---
name: Coroutine Best Practices
description: Kotlin coroutine conventions including MDC propagation, dispatcher selection, retry patterns, and structured concurrency
last-verified: 2026-02-14
---

# Coroutine best practices

## Overview

This document defines rules for using Kotlin coroutines safely and effectively in this project. All coroutine utilities are provided by the `CoroutineUtils` in the common module.

> **Key Principle**: Always use the project's `CoroutineUtils` for coroutine operations. Never use raw `runBlocking`, `async`, or `launch` directly. MDC context is lost without the MDC-preserving wrappers.

## MDC context propagation (required)

> **IMPORTANT**: Always use MDC-preserving coroutine functions from `com.myrealtrip.common.utils.coroutine`. Raw coroutine builders lose MDC context, which breaks request tracing and logging.

### Available functions

| Function | Purpose | Returns |
|----------|---------|---------|
| `runBlockingWithMDC` | Bridge blocking code to coroutines with MDC | `T` (blocking) |
| `asyncWithMDC` | Launch concurrent coroutine with MDC | `Deferred<T>` |
| `launchWithMDC` | Launch fire-and-forget coroutine with MDC | `Job` |

### Usage

```kotlin
import com.myrealtrip.common.utils.coroutine.runBlockingWithMDC
import com.myrealtrip.common.utils.coroutine.asyncWithMDC
import com.myrealtrip.common.utils.coroutine.launchWithMDC

// Parallel execution with MDC preserved
fun fetchUserDashboard(userId: Long): DashboardInfo = runBlockingWithMDC {
    val user = asyncWithMDC { userClient.fetchUser(userId) }
    val orders = asyncWithMDC { orderClient.fetchOrders(userId) }
    DashboardInfo(user.await(), orders.await())
}

// Fire-and-forget with MDC preserved
fun processOrder(order: Order): Unit = runBlockingWithMDC {
    launchWithMDC { notificationService.sendConfirmation(order) }
    launchWithMDC { auditService.logOrderCreated(order) }
}
```

### Incorrect: raw coroutine builders

```kotlin
// Bad: MDC context lost — traceId and requestId disappear in child coroutines
fun fetchData(): Result = runBlocking {
    val a = async { fetchA() }  // Logs will have no traceId
    val b = async { fetchB() }  // Logs will have no traceId
    Result(a.await(), b.await())
}

// Good: MDC context preserved
fun fetchData(): Result = runBlockingWithMDC {
    val a = asyncWithMDC { fetchA() }  // traceId flows through
    val b = asyncWithMDC { fetchB() }  // traceId flows through
    Result(a.await(), b.await())
}
```

---

## Dispatcher selection

Choose the right dispatcher based on the workload type.

### Dispatcher functions

| Dispatcher | Functions | Use Case |
|------------|-----------|----------|
| **Default** (CPU) | `runBlockingWithMDC`, `asyncWithMDC`, `launchWithMDC` | CPU-bound computation |
| **Virtual Thread** | `runBlockingOnVirtualThread`, `asyncOnVirtualThread`, `launchOnVirtualThread` | Blocking I/O (preferred) |
| **IO** | `runBlockingOnIoThread`, `asyncOnIoThread`, `launchOnIoThread` | Blocking I/O (fallback) |

### Virtual Thread (preferred for blocking I/O)

> **Tip**: Prefer Virtual Thread functions over IO Dispatcher for blocking I/O operations. Virtual threads handle blocking calls more efficiently, with lower overhead.

```kotlin
import com.myrealtrip.common.utils.coroutine.runBlockingOnVirtualThread
import com.myrealtrip.common.utils.coroutine.asyncOnVirtualThread

// Good: Virtual threads for parallel blocking API calls
fun fetchExternalData(): AggregatedData = runBlockingOnVirtualThread {
    val flights = asyncWithMDC { flightClient.search(criteria) }
    val hotels = asyncWithMDC { hotelClient.search(criteria) }
    AggregatedData(flights.await(), hotels.await())
}
```

### IO Dispatcher (fallback)

```kotlin
import com.myrealtrip.common.utils.coroutine.runBlockingOnIoThread
import com.myrealtrip.common.utils.coroutine.asyncOnIoThread

// Use IO dispatcher when virtual threads are not suitable
fun readFiles(): List<String> = runBlockingOnIoThread {
    val file1 = asyncOnIoThread { readFile("path1.txt") }
    val file2 = asyncOnIoThread { readFile("path2.txt") }
    listOf(file1.await(), file2.await())
}
```

### Custom dispatcher

Pass a custom dispatcher to `runBlockingWithMDC` when needed.

```kotlin
// Custom dispatcher with MDC
runBlockingWithMDC(myCustomDispatcher) {
    asyncWithMDC { process() }
}
```

### Selection guide

| Workload | Dispatcher | Example |
|----------|------------|---------|
| CPU computation | Default | Data transformation, calculation |
| HTTP/API calls | Virtual Thread | REST client calls, gRPC |
| File I/O | IO or Virtual Thread | File read/write, stream processing |
| Database queries | Virtual Thread | JDBC calls outside JPA transaction |
| Mixed workload | Virtual Thread | Combined I/O operations |

---

## Retry pattern

Use the project's `retry` and `retryBlocking` functions for resilient operations. Do not implement custom retry logic.

### Suspend retry

```kotlin
import com.myrealtrip.common.utils.coroutine.retry

// Default: 3 attempts, 500ms delay, no backoff
val result = retry { externalApi.call() }

// Custom: 5 attempts with exponential backoff (100ms → 200ms → 400ms → 800ms)
val result = retry(
    maxAttempts = 5,
    delay = 100.milliseconds,
    backoffMultiplier = 2.0,
) {
    externalApi.call()
}

// Selective: Retry only on specific exceptions
val result = retry(
    retryOn = { it is IOException || it is TimeoutException },
) {
    externalApi.call()
}
```

### Blocking retry

```kotlin
import com.myrealtrip.common.utils.coroutine.retryBlocking

// Blocking version — wraps retry in runBlockingWithMDC
val result = retryBlocking(maxAttempts = 3) {
    externalApi.call()
}
```

### Retry parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `maxAttempts` | `3` | Total number of attempts (must be >= 1) |
| `delay` | `500ms` | Initial delay between retries |
| `backoffMultiplier` | `1.0` | Multiplier for exponential backoff (1.0 = fixed delay) |
| `retryOn` | All exceptions | Predicate to filter retryable exceptions |

### Incorrect: custom retry logic

```kotlin
// Bad: Hand-rolled retry with no MDC, no backoff support
fun callWithRetry(): Result {
    var lastException: Exception? = null
    repeat(3) {
        try {
            return apiClient.call()
        } catch (e: Exception) {
            lastException = e
            Thread.sleep(500)
        }
    }
    throw lastException!!
}

// Good: Use project's retry utility
fun callWithRetry(): Result = retryBlocking(maxAttempts = 3) {
    apiClient.call()
}
```

---

## Debug logging

Use `withLogging` to trace coroutine execution with thread IDs for debugging.

```kotlin
import com.myrealtrip.common.utils.coroutine.withLogging

suspend fun fetchUserData(userId: Long): UserData = withLogging("fetchUserData") {
    val user = asyncWithMDC { userClient.fetch(userId) }
    val orders = asyncWithMDC { orderClient.fetchByUser(userId) }
    UserData(user.await(), orders.await())
}
// Logs: # >>> fetchUserData, start thread: 42
// Logs: # <<< fetchUserData, end thread: 43
```

> **Note**: Use `withLogging` only for debugging. Remove or guard with log-level checks in production-critical paths to avoid unnecessary log noise.

---

## Structured concurrency

### Use structured concurrency

> **IMPORTANT**: Always use structured concurrency. Never launch coroutines in `GlobalScope` or unstructured scopes. Bind child coroutines to a parent scope for proper lifecycle management and cancellation.

```kotlin
// Bad: Unstructured — coroutine leaks if parent fails
fun process(): Unit {
    GlobalScope.launch { sendNotification() }  // Leaks, no cancellation
}

// Good: Structured — child cancels with parent
fun process(): Unit = runBlockingWithMDC {
    launchWithMDC { sendNotification() }  // Bound to parent scope
}
```

### Cancellation handling

Respect cancellation by checking `isActive` in long-running loops, and using cancellable suspension points.

```kotlin
// Good: Cooperative cancellation
suspend fun processItems(items: List<Item>) = coroutineScope {
    for (item in items) {
        ensureActive()  // Check cancellation before each item
        processItem(item)
    }
}
```

### Exception handling

Handle exceptions within coroutine scopes to prevent unintended cancellation of sibling coroutines.

```kotlin
// Good: supervisorScope prevents one failure from cancelling siblings
suspend fun fetchAll(): AggregatedResult = supervisorScope {
    val users = asyncWithMDC {
        try {
            userClient.fetchAll()
        } catch (e: Exception) {
            logger.warn { "User fetch failed: ${e.message}" }
            emptyList()
        }
    }
    val orders = asyncWithMDC {
        try {
            orderClient.fetchAll()
        } catch (e: Exception) {
            logger.warn { "Order fetch failed: ${e.message}" }
            emptyList()
        }
    }
    AggregatedResult(users.await(), orders.await())
}
```

---

## Integration with Spring

### Service layer usage

Use coroutines in the Service layer for parallel I/O operations. Keep the Application layer as the transaction boundary.

```kotlin
@Service
class ProductService(
    private val inventoryClient: InventoryClient,
    private val pricingClient: PricingClient,
) {
    fun enrichProducts(products: List<Product>): List<EnrichedProduct> =
        runBlockingOnVirtualThread {
            products.map { product ->
                asyncWithMDC {
                    val inventory = inventoryClient.getStock(product.id)
                    val pricing = pricingClient.getPrice(product.id)
                    EnrichedProduct(product, inventory, pricing)
                }
            }.awaitAll()
        }
}
```

### Transaction boundary

> **IMPORTANT**: Do not start coroutines that perform database operations outside the transaction boundary. JPA operations must remain within the `@Transactional` scope that the Application layer manages.

```kotlin
// Bad: Database operations in separate coroutines lose transaction context
@Transactional
fun createOrder(request: CreateOrderRequest): OrderInfo = runBlockingWithMDC {
    val order = asyncWithMDC { orderRepository.save(Order.from(request)) }  // Wrong!
    val items = asyncWithMDC { itemRepository.saveAll(request.items) }      // Wrong!
    OrderInfo.from(order.await())
}

// Good: Database operations in transaction, parallel I/O outside
fun createOrderWithNotification(request: CreateOrderRequest): OrderInfo {
    val orderInfo = createOrder(request)  // Transaction handled by Application
    runBlockingWithMDC {
        launchWithMDC { notificationService.sendConfirmation(orderInfo) }
        launchWithMDC { auditService.logCreation(orderInfo) }
    }
    return orderInfo
}
```

---

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Raw `runBlocking`/`async`/`launch` | MDC context (traceId, requestId) lost | Use `runBlockingWithMDC`, `asyncWithMDC`, `launchWithMDC` |
| `GlobalScope.launch` | Coroutine leaks, no lifecycle management | Use structured concurrency with parent scope |
| `Thread.sleep` in coroutines | Blocks the thread, wastes resources | Use `delay()` for suspension |
| Custom retry loops | No MDC, inconsistent backoff | Use `retry` or `retryBlocking` from `CoroutineUtils` |
| DB operations in parallel coroutines | Transaction context lost | Keep DB operations sequential within `@Transactional` |
| Catching `CancellationException` | Breaks cooperative cancellation | Rethrow `CancellationException` or use `ensureActive()` |
| Missing exception handling in `async` | Exception silently deferred until `await()` | Handle exceptions at the `await()` call site or inside the `async` block |
| IO Dispatcher for blocking API calls | Limited thread pool, potential exhaustion | Use Virtual Thread dispatcher instead |

---

## Summary checklist

Before submitting code with coroutines, verify:

- [ ] All coroutine builders use MDC-preserving functions from `CoroutineUtils`
- [ ] No raw `runBlocking`, `async`, or `launch` calls exist
- [ ] No `GlobalScope` usage — all coroutines use structured concurrency
- [ ] Virtual Thread dispatcher is used for blocking I/O operations
- [ ] Retry logic uses `retry` or `retryBlocking` from `CoroutineUtils`
- [ ] Database operations remain within `@Transactional` scope (not in parallel coroutines)
- [ ] `CancellationException` is not swallowed
- [ ] Long-running loops check `ensureActive()` for cooperative cancellation
- [ ] `withLogging` is used only for debugging, not in hot paths
