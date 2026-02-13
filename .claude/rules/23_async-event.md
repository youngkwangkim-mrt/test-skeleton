---
description: Rules for asynchronous processing, Spring event-driven communication, @Async, @TransactionalEventListener, and event design patterns
globs: "*.{kt,kts}"
alwaysApply: false
---

# Async & event handling

> **Key Principle**: Use `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` for side effects after transaction commit. Never perform external I/O inside a transaction.

## Async configuration

- Project uses `VirtualThreadTaskExecutor` with `ContextPropagatingTaskDecorator` for MDC propagation
- Never create custom `TaskExecutor` without applying the decorator

## @Async rules

- **Public methods only** — proxy cannot intercept private/protected methods
- **No self-invocation** — extract async methods to a separate `@Component`
- **Fire-and-forget** — return `Unit` for side effects, `CompletableFuture<T>` when caller needs result

## Standard event listener pattern

- Use `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`
- Always wrap listener logic in try-catch with explicit error logging
- Prefer `@TransactionalEventListener` over `@EventListener` for transactional events
- `@EventListener` fires before commit — may process rolled-back data

## Event publishing

- Publish events via `ApplicationEventPublisher` in Service or Application layer only
- Never publish events in Controller or Facade

## Event class conventions

- Use sealed interface/class hierarchy: `{Feature}Event.Created`, `{Feature}Event.Cancelled`
- Include only IDs and minimal context — never pass entities or DTOs
- Place in `domain/{feature}/event/` package

## Event listener location

- Name: `{Feature}EventListener`
- Group related handlers in one class per feature
- Place in `domain/{feature}/listener/` or `infrastructure/{channel}/`

## Transaction phases

- `AFTER_COMMIT` — default for side effects (notifications, external API)
- `AFTER_ROLLBACK` — compensating actions on failure
- `AFTER_COMPLETION` — actions regardless of outcome
- `BEFORE_COMMIT` — validation within transaction

## Common pitfalls

- External I/O inside `@Transactional` — use events to defer after commit
- Missing `fallbackExecution = true` — event not fired without active transaction
- Passing Entity in event — causes coupling and `LazyInitializationException`
- One listener class per event — group by feature instead

> For detailed examples, see skill: `async-event`
