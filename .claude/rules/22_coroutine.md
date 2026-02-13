---
description: Kotlin coroutine conventions including MDC propagation, dispatcher selection, retry patterns, and structured concurrency
globs: "*.{kt,kts}"
alwaysApply: false
---

# Coroutine best practices

> **Key Principle**: Always use the project's `CoroutineUtils` for coroutine operations. Never use raw `runBlocking`, `async`, or `launch` directly.

## MDC context propagation (required)

- Always use MDC-preserving functions from `com.myrealtrip.common.utils.coroutine`
- `runBlockingWithMDC` — bridge blocking code to coroutines with MDC
- `asyncWithMDC` — launch concurrent coroutine with MDC
- `launchWithMDC` — launch fire-and-forget coroutine with MDC
- Raw coroutine builders lose MDC context (traceId, requestId)

## Dispatcher selection

- **Default** (CPU): `runBlockingWithMDC`, `asyncWithMDC`, `launchWithMDC` — CPU-bound computation
- **Virtual Thread** (preferred for I/O): `runBlockingOnVirtualThread`, `asyncOnVirtualThread` — blocking I/O
- **IO** (fallback): `runBlockingOnIoThread`, `asyncOnIoThread` — when virtual threads not suitable
- Prefer Virtual Thread over IO Dispatcher for blocking I/O operations

## Retry pattern

- Use `retry` (suspend) and `retryBlocking` from `CoroutineUtils`
- Never implement custom retry loops
- Parameters: `maxAttempts` (3), `delay` (500ms), `backoffMultiplier` (1.0), `retryOn`

## Structured concurrency

- Never use `GlobalScope` — all coroutines must use structured concurrency
- Use `ensureActive()` in long-running loops for cooperative cancellation
- Never catch `CancellationException` — rethrow it
- Use `supervisorScope` when sibling failures should not cancel each other

## Transaction boundary

- Do not start coroutines that perform DB operations outside `@Transactional` scope
- Keep DB operations sequential within transactions
- Use coroutines for parallel I/O outside transaction boundaries

## Common pitfalls

- Raw `runBlocking`/`async`/`launch` — MDC context lost
- `GlobalScope.launch` — coroutine leaks
- `Thread.sleep` in coroutines — blocks thread, use `delay()`
- DB operations in parallel coroutines — transaction context lost
- IO Dispatcher for blocking API calls — use Virtual Thread instead

> For detailed examples, see skill: `coroutine`
