---
description: Transaction management rules including propagation, isolation, rollback, and common pitfalls
globs: "*.{kt,kts}"
alwaysApply: false
---

# Transaction management

> **Key Principle**: Keep transactions small and fast. No external I/O. Watch for self-invocation.

## Core rules

- **Keep it small**: Only include necessary DB operations in a transaction
- **No external I/O**: Never perform HTTP calls, file I/O, or messaging inside transactions
- **No self-invocation**: Same-class `@Transactional` method calls bypass proxy — extract to another service or make caller transactional
- **Fail fast**: Validate before starting a transaction
- **Public methods only**: `@Transactional` proxy only works on public methods

## @Transactional defaults

- Propagation: `REQUIRED` (joins existing or creates new)
- Isolation: DB default (usually `READ_COMMITTED`)
- Rollback: `RuntimeException` and `Error` only — use `rollbackFor` for checked exceptions

## Master/Slave routing

- `@Transactional(readOnly = true)` routes to **Slave** (Reader)
- `@Transactional` (default) routes to **Master** (Writer)
- QueryApplication classes must use `readOnly = true`
- CommandApplication classes use default `@Transactional`
- `LazyConnectionDataSourceProxy` is required for correct routing

## Propagation levels

- `REQUIRED` — default, join existing or create new
- `REQUIRES_NEW` — always create new (audit logs, independent operations)
- `NESTED` — savepoint within existing (partial rollback)
- `SUPPORTS` / `NOT_SUPPORTED` — read operations / non-transactional operations

## External calls after commit

- Move external calls (HTTP, email, messaging) outside the transaction
- Use `@TransactionalEventListener(AFTER_COMMIT)` for post-commit side effects

## Common pitfalls

- Self-invocation bypasses proxy — extract to separate service
- Checked exceptions don't trigger rollback — use `rollbackFor`
- Long transactions cause lock contention and connection exhaustion
- Missing `readOnly = true` routes read queries to Master

> For detailed examples, see skill: `transaction`
