---
name: Clean Code
description: Clean code principles (KISS, DRY, YAGNI) for Java, Kotlin, and Spring applications
last-verified: 2026-02-14
---

# Clean code rules

## Overview

This document provides rules for generating clean, maintainable, and human-readable code following industry best practices for Java, Kotlin, and Spring applications.

> **Key Principle**: Write the simplest code that works, make it human-readable, and don't over-engineer.

## Core principles

### KISS (Keep it simple, stupid)

* Write the simplest solution that works.
* Avoid premature optimization.
* Do not add features "just in case".
* If a simpler solution exists, use it.

### DRY (Don't repeat yourself)

* Extract repeated logic into reusable functions or classes.
* Use inheritance or composition to share behavior.
* Create utility classes for common operations.
* Avoid copy-paste coding.

### YAGNI (You aren't gonna need it)

* Implement only what is currently required.
* Do not build abstractions for hypothetical future needs.
* Remove unused code immediately.

## Human-readable code

### Meaningful names

```kotlin
// Bad
val d: Int = 0  // elapsed time in days
fun calc(a: Int, b: Int): Int

// Good
val elapsedTimeInDays: Int = 0
fun calculateTotalPrice(quantity: Int, unitPrice: Int): Int
```

* Variable names must reveal intent.
* Function names must describe what they do (use verbs).
* Class names must be nouns that describe their responsibility.
* Avoid abbreviations unless universally understood (e.g., `id`, `url`).
* Boolean variables must read like questions: `isValid`, `hasPermission`, `canExecute`.

### Consistent naming conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `UserService`, `OrderRepository` |
| Functions/Methods | camelCase | `findUserById()`, `calculateTotal()` |
| Variables | camelCase | `userName`, `orderCount` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| Packages | lowercase | `com.myrealtrip.common.utils` |

## Abstraction levels

### Keep consistent abstraction level

Each function must operate at a single level of abstraction:

```kotlin
// Bad - mixed abstraction levels
fun processOrder(order: Order) {
    // High level
    validateOrder(order)

    // Low level details mixed in
    val connection = dataSource.getConnection()
    val stmt = connection.prepareStatement("INSERT INTO orders...")
    stmt.setString(1, order.id)
    stmt.executeUpdate()

    // High level again
    sendConfirmationEmail(order)
}

// Good - consistent abstraction level
fun processOrder(order: Order) {
    validateOrder(order)
    saveOrder(order)
    sendConfirmationEmail(order)
}
```

### Function size

* Functions must do ONE thing.
* Ideal function length: 5-20 lines.
* If a function needs a comment to explain what a section does, extract that section.

### Class cohesion

* Classes must have a single responsibility.
* All methods must relate to the class's core purpose.
* If a class grows too large, split it into smaller focused classes.

## Java/Kotlin best practices

### Prefer immutability

```kotlin
// Prefer val over var
val userName: String = "John"

// Use immutable collections
val users: List<User> = listOf(user1, user2)

// Use data classes for DTOs
data class UserDto(
    val id: Long,
    val name: String,
    val email: String
)
```

### Null safety (Kotlin)

```kotlin
// Use nullable types explicitly
fun findUserById(id: Long): User?

// Use safe calls
val userName = user?.name ?: "Unknown"

// Avoid !! operator unless absolutely necessary
```

### Use Kotlin idioms

```kotlin
// Use scope functions appropriately
user?.let { saveUser(it) }

// Use extension functions for utility methods
fun String.toSlug(): String = this.lowercase().replace(" ", "-")

// Use when expressions instead of complex if-else
val result = when (status) {
    Status.ACTIVE -> handleActive()
    Status.PENDING -> handlePending()
    Status.INACTIVE -> handleInactive()
}
```

## Spring best practices

### Constructor injection

```kotlin
// Good - constructor injection (testable, immutable)
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
)

// Bad - field injection (harder to test)
@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository
}
```

### Layer separation

* Controller: HTTP handling only, delegate to services.
* Service: Business logic, transaction management.
* Repository: Data access only.
* Do not leak implementation details between layers.

### Exception handling

```kotlin
// Use custom exceptions for business errors
class UserNotFoundException(userId: Long) :
    BizRuntimeException(ErrorCode.USER_NOT_FOUND, "User not found: $userId")

// Handle exceptions at appropriate level
@ExceptionHandler(BizRuntimeException::class)
fun handleBizException(ex: BizRuntimeException): ResponseEntity<ErrorResponse>
```

## Do not overengineer

### Avoid

* Creating interfaces for classes that have only one implementation.
* Building complex abstractions for simple problems.
* Adding configuration options that nobody uses.
* Creating utility classes for one-off operations.
* Premature generalization.

### Signs of overengineering

* More than 3 levels of inheritance.
* Classes with only one method.
* Excessive use of design patterns.
* Configuration for everything.
* "Flexible" code that is never flexed.

## Testable code

### Design for testability

```kotlin
// Good - dependencies injected, easy to mock
class OrderService(
    private val orderRepository: OrderRepository,
    private val paymentGateway: PaymentGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun createOrder(request: OrderRequest): Order {
        val now = LocalDateTime.now(clock)
        // ...
    }
}

// Bad - hard-coded dependencies, impossible to test
class OrderService {
    fun createOrder(request: OrderRequest): Order {
        val repo = OrderRepositoryImpl()  // Can't mock
        val now = LocalDateTime.now()      // Can't control time
        // ...
    }
}
```

### Pure functions

* Prefer functions without side effects.
* Same input must always produce same output.
* Pure functions make testing trivial.

### Small, focused units

* Small classes and functions are easier to test.
* Single responsibility equals single test focus.

## Code organization

### File structure

* One public class per file.
* Related classes can be in the same file if small.
* Group by feature, not by type.

### Import organization

* Remove unused imports.
* Avoid wildcard imports.
* Group imports logically.

### Comments

```kotlin
// Bad - comment explains what (code should be self-explanatory)
// Loop through users and find active ones
val activeUsers = users.filter { it.isActive }

// Good - comment explains why (context that code can't express)
// Filter inactive users to comply with GDPR deletion requirements
val activeUsers = users.filter { it.isActive }
```

* Code must be self-documenting.
* Comments must explain WHY, not WHAT.
* Keep comments up-to-date or remove them.
* Use KDoc/Javadoc for public APIs.

## Summary checklist

Before submitting code, verify:

- [ ] Names are meaningful and descriptive
- [ ] Functions do one thing and do it well
- [ ] Abstraction levels are consistent
- [ ] No duplicate code exists
- [ ] No unnecessary complexity exists
- [ ] Dependencies are injected (testable)
- [ ] Edge cases are handled
- [ ] Code is formatted consistently
