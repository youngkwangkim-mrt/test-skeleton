---
name: transaction
description: Transaction management rules including propagation, isolation, rollback, and common pitfalls
triggers:
  - transaction
  - propagation
  - isolation
  - rollback
argument-hint: ""
---

# Transaction management

## Overview

This document defines rules for managing transactions in Spring applications, including propagation, isolation, Master/Slave routing, and common pitfalls.

> **Key Principle**: Keep transactions small and fast. No external I/O. Watch for self-invocation.

For JPA-specific topics (Lazy Loading, N+1, Locking, Entity State), see skill: `jpa-rules`.

| Guideline | Description |
|-----------|-------------|
| **Keep it small** | Only include necessary DB operations |
| **No external I/O** | Avoid HTTP calls, file I/O, messaging inside transactions |
| **No self-invocation** | Same-class method calls bypass proxy |
| **Fail fast** | Validate before starting transaction |

---

## @Transactional defaults

| Property | Default | Description |
|----------|---------|-------------|
| Propagation | `REQUIRED` | Joins existing or creates new |
| Isolation | DB default | Usually `READ_COMMITTED` |
| Rollback | Unchecked only | RuntimeException, Error |
| Timeout | None | No limit |

```kotlin
@Transactional  // Uses all defaults
fun createUser(name: String): User = userRepository.save(User(name = name))
```

---

## Propagation levels

| Level | Behavior | Use Case |
|-------|----------|----------|
| `REQUIRED` | Join existing or create new | **Default**, most operations |
| `REQUIRES_NEW` | Always create new, suspend current | Audit logs, independent operations |
| `NESTED` | Savepoint within existing | Partial rollback support |
| `SUPPORTS` | Use if exists, else none | Read operations |
| `NOT_SUPPORTED` | Suspend current, run without | Non-transactional operations |

```kotlin
// REQUIRES_NEW - commits independently
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun auditLog(message: String) {
    logRepository.save(AuditLog(message))  // Persists even if parent rolls back
}
```

---

## Isolation levels

| Level | Prevents | Performance |
|-------|----------|-------------|
| `READ_UNCOMMITTED` | Nothing | Fastest |
| `READ_COMMITTED` | Dirty reads | **Recommended default** |
| `REPEATABLE_READ` | Dirty + non-repeatable reads | Slower |
| `SERIALIZABLE` | All anomalies | Slowest |

```kotlin
@Transactional(isolation = Isolation.REPEATABLE_READ)
fun transferFunds(fromId: Long, toId: Long, amount: BigDecimal) {
    // Repeated reads return same values
}
```

---

## Read-only transactions

```kotlin
@Transactional(readOnly = true)
fun getUsers(): List<User> = userRepository.findAll()
```

**Benefits:**
- JDBC driver optimizations
- Database query planning hints
- Prevents accidental writes
- **Master/Slave routing**: `readOnly = true` automatically routes to Slave (Reader)

---

## Master/Slave DataSource routing

> **IMPORTANT**: The system automatically routes connections to Master or Slave based on the `@Transactional(readOnly = true)` setting.

### Routing rules

| Transaction | DataSource | Purpose |
|-------------|------------|---------|
| `@Transactional` (default) | **Master** (Writer) | INSERT, UPDATE, DELETE |
| `@Transactional(readOnly = true)` | **Slave** (Reader) | SELECT (read-only) |
| No transaction | **Master** (default) | Default behavior |

### How it works

`DataSourceConfig` uses `AbstractRoutingDataSource` + `LazyConnectionDataSourceProxy` for routing:

```
@Transactional entry
  -> Spring sets the readOnly flag
    -> LazyConnectionDataSourceProxy defers connection acquisition until actual SQL execution
      -> AbstractRoutingDataSource checks readOnly flag to route to Master/Slave
```

```kotlin
// Routed to Slave (read-only)
@Service
@Transactional(readOnly = true)
class HolidayQueryApplication(
    private val holidayService: HolidayService,
) {
    fun findAll(): List<HolidayInfo> = holidayService.findAll()
}

// Routed to Master (write)
@Service
@Transactional
class HolidayCommandApplication(
    private val holidayService: HolidayService,
) {
    fun create(request: CreateHolidayRequest): HolidayInfo =
        HolidayInfo.from(holidayService.create(request))
}
```

### Configuration properties

```yaml
# Master/Slave routing activates only when the master jdbc-url property exists
# Without the property (e.g., embed profile), the default H2 auto-configuration is used
spring:
  datasource:
    master:
      hikari:
        jdbc-url: jdbc:mysql://master-host:3306/mydb
        username: user
        password: pass
    slave:
      hikari:
        jdbc-url: jdbc:mysql://slave-host:3306/mydb
        username: user
        password: pass
```

### Considerations

| Consideration | Description |
|---------------|-------------|
| **Missing readOnly** | Using only `@Transactional` routes read queries to Master as well |
| **QueryApplication requires readOnly** | Query Application classes must use `@Transactional(readOnly = true)` |
| **CommandApplication uses default** | Command Application classes use `@Transactional` (default) |
| **LazyConnection required** | Without `LazyConnectionDataSourceProxy`, the connection is acquired before the readOnly flag is set, causing routing failure |

---

## Transaction timeout

```kotlin
@Transactional(timeout = 5)  // 5 seconds
fun processLargeData() { ... }
```

**Global default:**

```yaml
spring:
  transaction:
    default-timeout: 30
```

---

## Rollback rules

| Exception Type | Default Behavior | Override |
|----------------|------------------|----------|
| `RuntimeException` | Rollback | `noRollbackFor` |
| `Error` | Rollback | `noRollbackFor` |
| Checked exception | No rollback | `rollbackFor` |

```kotlin
@Transactional(rollbackFor = [IOException::class])
fun loadData() { ... }

@Transactional(noRollbackFor = [ValidationException::class])
fun validate() { ... }
```

---

## External calls after commit

Move external calls outside transactions:

```kotlin
// Bad: HTTP call inside transaction
@Transactional
fun createUser(request: CreateUserRequest): User {
    val user = userRepository.save(User.from(request))
    paymentService.createCustomer(user.email)  // Holds DB connection
    return user
}

// Good: External calls after commit
fun createUser(request: CreateUserRequest): User {
    val user = saveUser(request)  // Transaction ends here
    paymentService.createCustomer(user.email)  // No DB connection held
    return user
}

@Transactional
fun saveUser(request: CreateUserRequest): User = userRepository.save(User.from(request))
```

### Using @TransactionalEventListener

```kotlin
@Transactional
fun createUser(request: CreateUserRequest): User {
    val user = userRepository.save(User.from(request))
    eventPublisher.publishEvent(UserCreatedEvent(user))
    return user
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
fun handleUserCreated(event: UserCreatedEvent) {
    emailService.sendWelcome(event.user.email)  // Runs after commit
}
```

---

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Self-invocation | `@Transactional` ignored | Make caller transactional, inject self, or extract to another service |
| Non-public methods | Proxy only works on public | Always use public methods |
| Checked exceptions | No automatic rollback | Use `rollbackFor` |
| External I/O in transaction | Connection held, inconsistent state | Move outside transaction |
| Long transactions | Lock contention, connection exhaustion | Keep transactions small |

### Self-invocation problem

```kotlin
@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun createUser(name: String): User = userRepository.save(User(name = name))

    fun register(name: String): User {
        return createUser(name)  // No transaction! Direct call bypasses proxy
    }
}
```

**Solutions:**

1. Make caller transactional (recommended)
2. Inject self: `@Autowired private lateinit var self: UserService`
3. Extract to separate service

---

## Testing

```kotlin
@DataJpaTest  // Includes @Transactional with auto-rollback
class UserRepositoryTest(
    @Autowired private val userRepository: UserRepository,
) {
    @Test
    fun `should save user`() {
        val user = userRepository.save(User(name = "John"))
        assertNotNull(user.id)
        // Automatically rolled back after test
    }
}
```
