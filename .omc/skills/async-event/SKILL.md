---
name: async-event
description: Rules for asynchronous processing, Spring event-driven communication, @Async, @TransactionalEventListener, and event design patterns
triggers:
  - event
  - async
  - listener
  - transactional event
argument-hint: ""
---

# Async & event handling

## Overview

This document defines rules for asynchronous processing and Spring event-driven communication. The project uses Virtual Thread executors with MDC propagation for `@Async` methods, and `@TransactionalEventListener` for transactional side effects.

> **Key Principle**: Use `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` for side effects after transaction commit. Never perform external I/O inside a transaction. Always preserve MDC context in async operations.

## Async configuration

The project uses Virtual Thread-based async execution configured in `AsyncConfig` (`common-web` module).

| Setting | Value | Description |
|---------|-------|-------------|
| Executor | `VirtualThreadTaskExecutor` | Lightweight virtual threads for async tasks |
| Thread prefix | `async-vt-` | Identifies async virtual threads in logs |
| Task decorator | `ContextPropagatingTaskDecorator` | Preserves MDC (traceId, requestId) across threads |
| Exception handler | `GlobalSimpleAsyncUncaughtExceptionHandler` | Logs `KnownException` at DEBUG level |

> **IMPORTANT**: The `ContextPropagatingTaskDecorator` ensures MDC context flows into async threads. Never create a custom `TaskExecutor` without applying this decorator — traceId and requestId will be lost.

---

## @Async methods

### Basic usage

Mark a method with `@Async` to run it on a separate virtual thread. The caller returns immediately without waiting for the method to complete.

```kotlin
@Component
class NotificationSender(
    private val slackClient: SlackClient,
) {

    @Async
    fun sendSlackNotification(channel: String, message: String) {
        slackClient.send(channel, message)
    }
}
```

### Rules

| Rule | Description |
|------|-------------|
| **Public methods only** | `@Async` requires a proxy — private/protected methods are ignored |
| **No self-invocation** | Calling an `@Async` method from the same class bypasses the proxy |
| **Fire-and-forget** | Return `void`/`Unit` for side effects. Use `CompletableFuture<T>` when the caller needs a result |
| **No return value reliance** | Do not expect the caller to wait for a `Unit`-returning `@Async` method |

### Incorrect: self-invocation

```kotlin
// Bad: @Async ignored — direct call bypasses proxy
@Service
class OrderService {

    fun createOrder(request: CreateOrderRequest): OrderInfo {
        val order = save(request)
        sendNotification(order)  // Runs synchronously! Proxy is bypassed
        return OrderInfo.from(order)
    }

    @Async
    fun sendNotification(order: Order) {
        slackClient.send("#orders", "New order: ${order.id}")
    }
}

// Good: Separate class — proxy intercepts correctly
@Service
class OrderService(
    private val orderNotifier: OrderNotifier,
) {
    fun createOrder(request: CreateOrderRequest): OrderInfo {
        val order = save(request)
        orderNotifier.sendNotification(order.id)  // Async via proxy
        return OrderInfo.from(order)
    }
}

@Component
class OrderNotifier(
    private val slackClient: SlackClient,
) {
    @Async
    fun sendNotification(orderId: Long) {
        slackClient.send("#orders", "New order: $orderId")
    }
}
```

---

## Spring event system

### Event lifecycle

```
Service (within @Transactional)
  ↓ applicationEventPublisher.publishEvent(event)
Spring ApplicationEventPublisher
  ↓ transaction commits
@TransactionalEventListener(AFTER_COMMIT)
EventListener.handle(event)
  ↓
Side effect (notification, external API, audit log, etc.)
```

### Publishing events

Use Spring's `ApplicationEventPublisher` to publish events.

```kotlin
@Service
class OrderService(
    private val orderRepository: OrderJpaRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun create(request: CreateOrderRequest): OrderInfo {
        val order = orderRepository.save(Order.create(request))

        // Publish event — listener fires after transaction commits
        applicationEventPublisher.publishEvent(
            OrderEvent.Created(orderId = order.id!!)
        )

        return OrderInfo.from(order)
    }
}
```

### Where to publish

| Layer | Allowed | Reason |
|-------|---------|--------|
| Service | Yes | Business logic decides when events occur |
| Application | Yes | Orchestration may trigger events after multi-service calls |
| Controller / Facade | No | HTTP layer must not publish domain events |

---

## Listening to events

### Standard pattern: @Async + @TransactionalEventListener

> **IMPORTANT**: Use `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` as the default pattern for event-driven side effects.

```kotlin
@Component
class OrderEventListener(
    private val slackNotificationService: SlackNotificationService,
    private val emailService: EmailService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handleOrderCreated(event: OrderEvent.Created) {
        try {
            slackNotificationService.notifyInfo(
                channel = "#order-alerts",
                title = "신규 주문",
                message = "주문 ID: ${event.orderId}",
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to handle OrderCreated event: ${event.orderId}" }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun sendOrderConfirmationEmail(event: OrderEvent.Created) {
        try {
            emailService.sendConfirmation(event.orderId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to send confirmation email: ${event.orderId}" }
        }
    }
}
```

### Annotation rules

| Annotation | Required | Description |
|------------|----------|-------------|
| `@Async` | Yes | Runs on a virtual thread, does not block the caller |
| `@TransactionalEventListener` | Yes | Fires after transaction commits |
| `phase = AFTER_COMMIT` | Yes | Default phase for side effects |
| `fallbackExecution = true` | Recommended | Fires even without active transaction context |

### Annotation order

Follow the annotation ordering from skill: `annotation-order`:

```kotlin
@Async                                                                      // 1. Execution mode
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT,          // 2. Event handling
                            fallbackExecution = true)
fun handleEvent(event: MyEvent) { }
```

### @TransactionalEventListener vs @EventListener

| Annotation | Timing | Use Case |
|------------|--------|----------|
| `@TransactionalEventListener` | After transaction phase | Side effects that depend on committed data |
| `@EventListener` | Immediately when published | Non-transactional contexts, startup events |

> **IMPORTANT**: Prefer `@TransactionalEventListener` over `@EventListener` for events published within `@Transactional` methods. `@EventListener` fires immediately — if the transaction rolls back, the listener has already processed data that does not exist.

```kotlin
// Bad: @EventListener fires before commit — may process rolled-back data
@EventListener
fun handle(event: OrderEvent.Created) {
    sendNotification(event.orderId)  // Order may not exist if transaction rolls back
}

// Good: @TransactionalEventListener fires after commit
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
fun handle(event: OrderEvent.Created) {
    sendNotification(event.orderId)  // Order is guaranteed to exist
}
```

### When to use each phase

| Phase | Use Case | Example |
|-------|----------|---------|
| `AFTER_COMMIT` | Side effects that must not fire on rollback | Notifications, external API calls |
| `AFTER_ROLLBACK` | Compensating actions on failure | Cleanup, alerting on failure |
| `AFTER_COMPLETION` | Actions regardless of outcome | Metric recording, audit logging |
| `BEFORE_COMMIT` | Validation within transaction | Cross-aggregate consistency checks |

> **IMPORTANT**: Use `AFTER_COMMIT` as the default phase. Use other phases only when you have a specific reason.

### When NOT to use @Async

Use synchronous `@TransactionalEventListener` (without `@Async`) only when:
- The listener must complete before the caller returns
- The listener result affects the response

```kotlin
// Synchronous: validation that must happen within the same transaction
@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
fun validateCrossAggregate(event: OrderCreatedEvent) {
    // Runs synchronously within the publishing transaction
}
```

---

## Event listener location

### Naming convention

| Pattern | Example |
|---------|---------|
| `{Feature}EventListener` | `OrderEventListener`, `ReservationEventListener` |

### Package location

| Module | Location | Description |
|--------|----------|-------------|
| Same domain feature | `domain/{feature}/listener/` | Intra-domain event handling |
| Cross-domain | `domain/{target-feature}/listener/` | Place in the consuming feature's package |
| Infrastructure side effects | `infrastructure/{channel}/` | Notification, external API, audit |
| Bootstrap-specific | `{appname}/listener/` | App-specific event handling |

### One listener class per feature

Group related event handlers in a single listener class per feature. Do not create one class per event.

```kotlin
// Good: One listener class handles all order-related events
@Component
class OrderEventListener(
    private val slackNotificationService: SlackNotificationService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handleCreated(event: OrderEvent.Created) { ... }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun handleCancelled(event: OrderEvent.Cancelled) { ... }
}

// Bad: Separate class for each event
class OrderCreatedEventListener { ... }
class OrderCancelledEventListener { ... }
```

---

## Defining events

### Event class conventions

| Convention | Rule | Example |
|------------|------|---------|
| Structure | Sealed interface/class hierarchy | Type-safe `when` exhaustiveness |
| Naming | `{Feature}Event` with nested subtypes | `OrderEvent.Created`, `OrderEvent.Cancelled` |
| Data | Include only IDs and minimal context | `data class Created(val orderId: Long)` |
| Location | `domain/{feature}/event/` | `domain/order/event/OrderEvent.kt` |

### Correct: sealed hierarchy with minimal data

```kotlin
// Good: Event carries only IDs — listener fetches what it needs
sealed class OrderEvent {
    data class Created(val orderId: Long) : OrderEvent()
    data class Cancelled(val orderId: Long, val reason: String) : OrderEvent()
    data class Completed(val orderId: Long) : OrderEvent()
}
```

### Incorrect: fat event with entity or DTO

```kotlin
// Bad: Event carries the entire entity — coupling, LazyInitializationException
sealed class OrderEvent {
    data class Created(val order: Order) : OrderEvent()
}

// Bad: Event carries DTO — unnecessary coupling between publisher and listener
sealed class OrderEvent {
    data class Created(val orderInfo: OrderInfo) : OrderEvent()
}
```

### When minimal data is not enough

If the listener needs more than an ID (e.g., a snapshot of state at the time of the event), include only the required primitive values:

```kotlin
// Acceptable: Snapshot of specific values needed by listeners
data class StatusChanged(
    val orderId: Long,
    val previousStatus: OrderStatus,
    val newStatus: OrderStatus,
) : OrderEvent()
```

---

## Error handling

### Listener exception handling

> **IMPORTANT**: Always wrap listener logic in try-catch. Unhandled exceptions in `@Async` listeners are logged by `GlobalSimpleAsyncUncaughtExceptionHandler` but may be missed in monitoring.

```kotlin
// Good: Explicit error handling with context
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
fun handle(event: OrderEvent.Created) {
    try {
        processOrder(event.orderId)
    } catch (e: Exception) {
        logger.error(e) { "Failed to handle OrderCreated: orderId=${event.orderId}" }
    }
}

// Bad: No error handling — exception silently logged by global handler
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
fun handle(event: OrderEvent.Created) {
    processOrder(event.orderId)  // Unhandled exception — lost in async thread
}
```

### GlobalSimpleAsyncUncaughtExceptionHandler

The project provides a global async exception handler as a safety net:

| Exception Type | Log Level | Description |
|----------------|-----------|-------------|
| `KnownException` | DEBUG | Expected errors (not found, validation) |
| Other | ERROR | Unexpected errors (default Spring behavior) |

> **Note**: This handler is a safety net. Always prefer explicit try-catch in listeners for better observability and control.

---

## External I/O and transactions

> **IMPORTANT**: Never perform external I/O (HTTP calls, messaging, file operations) inside a `@Transactional` method. Use events to move I/O after the transaction commits.

```kotlin
// Bad: HTTP call inside transaction — holds DB connection, inconsistent on failure
@Transactional
fun createOrder(request: CreateOrderRequest): OrderInfo {
    val order = orderRepository.save(Order.create(request))
    slackClient.send("#orders", "New order: ${order.id}")  // Blocks transaction
    return OrderInfo.from(order)
}

// Good: Event-driven — I/O happens after commit on a separate thread
@Transactional
fun createOrder(request: CreateOrderRequest): OrderInfo {
    val order = orderRepository.save(Order.create(request))
    applicationEventPublisher.publishEvent(OrderEvent.Created(order.id!!))
    return OrderInfo.from(order)
}

// Listener handles I/O after commit
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
fun handleOrderCreated(event: OrderEvent.Created) {
    try {
        slackClient.send("#orders", "New order: ${event.orderId}")
    } catch (e: Exception) {
        logger.error(e) { "Failed to notify: ${event.orderId}" }
    }
}
```

---

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| External I/O inside `@Transactional` | Holds DB connection, inconsistent state on failure | Publish event, handle I/O in `@TransactionalEventListener(AFTER_COMMIT)` |
| Missing `@Async` on listener | Blocks the publishing thread until listener completes | Always add `@Async` for non-critical side effects |
| Missing `fallbackExecution = true` | Event not fired when published without active transaction | Add `fallbackExecution = true` unless you explicitly require a transaction |
| No try-catch in async listener | Unhandled exception silently logged by global handler | Wrap listener logic in try-catch with explicit logging |
| Passing Entity in event | Coupling, LazyInitializationException outside transaction | Pass only IDs and primitive values |
| `@Async` self-invocation | Direct call bypasses proxy — runs synchronously | Extract async method to a separate `@Component` |
| `@Async` on private method | Proxy cannot intercept — annotation ignored | Use public methods only |
| Using `@EventListener` for transactional events | Fires before commit — may process rolled-back data | Use `@TransactionalEventListener(AFTER_COMMIT)` |
| Creating custom executor without `ContextPropagatingTaskDecorator` | MDC context (traceId) lost in async threads | Always apply the decorator |
| Publishing events in Controller/Facade | Side effects escape domain boundary | Publish events in Service or Application layer only |
| One listener class per event | Class proliferation, hard to navigate | Group by feature in a single `{Feature}EventListener` |

---

## Summary checklist

Before submitting event-related code, verify:

- [ ] Side effects use `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`
- [ ] Events are published via `ApplicationEventPublisher.publishEvent()` in Service or Application layer
- [ ] Events are NOT published in Controller or Facade
- [ ] Event classes use sealed interface/class hierarchy for type safety
- [ ] Events carry only IDs and minimal context, not entities or DTOs
- [ ] Listener methods include try-catch with explicit error logging
- [ ] No external I/O (HTTP, Slack, Email) inside `@Transactional` methods
- [ ] `@Async` methods are in a separate class from the caller (no self-invocation)
- [ ] `@Async` methods are public
- [ ] Custom executors apply `ContextPropagatingTaskDecorator` for MDC propagation
- [ ] Listeners are grouped in `{Feature}EventListener` classes, not one class per event
