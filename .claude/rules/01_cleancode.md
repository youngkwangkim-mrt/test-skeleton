---
name: Clean Code
description: Clean code principles (KISS, DRY, YAGNI) for Java, Kotlin, and Spring applications
---

# Clean Code Rules for Claude Code

## Overview

These rules ensure Claude Code generates clean, maintainable, and human-readable code following industry best practices for Java, Kotlin, and Spring applications.

## Core Principles

### KISS (Keep It Simple, Stupid)

* Write the simplest solution that works
* Avoid premature optimization
* Do not add features "just in case"
* If a simpler solution exists, use it

### DRY (Don't Repeat Yourself)

* Extract repeated logic into reusable functions or classes
* Use inheritance or composition to share behavior
* Create utility classes for common operations
* Avoid copy-paste coding

### YAGNI (You Aren't Gonna Need It)

* Only implement what is currently required
* Do not build abstractions for hypothetical future needs
* Remove unused code immediately

## Human-Readable Code

### Meaningful Names

```kotlin
// BAD
val d: Int = 0  // elapsed time in days
fun calc(a: Int, b: Int): Int

// GOOD
val elapsedTimeInDays: Int = 0
fun calculateTotalPrice(quantity: Int, unitPrice: Int): Int
```

* Variable names should reveal intent
* Function names should describe what they do (use verbs)
* Class names should be nouns that describe their responsibility
* Avoid abbreviations unless they are universally understood (e.g., `id`, `url`)
* Boolean variables should read like questions: `isValid`, `hasPermission`, `canExecute`

### Consistent Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `UserService`, `OrderRepository` |
| Functions/Methods | camelCase | `findUserById()`, `calculateTotal()` |
| Variables | camelCase | `userName`, `orderCount` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT` |
| Packages | lowercase | `com.myrealtrip.common.utils` |

## Abstraction Levels

### Keep Consistent Abstraction Level

Each function should operate at a single level of abstraction:

```kotlin
// BAD - mixed abstraction levels
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

// GOOD - consistent abstraction level
fun processOrder(order: Order) {
    validateOrder(order)
    saveOrder(order)
    sendConfirmationEmail(order)
}
```

### Function Size

* Functions should do ONE thing
* Ideal function length: 5-20 lines
* If a function needs a comment to explain what a section does, extract that section

### Class Cohesion

* Classes should have a single responsibility
* All methods should relate to the class's core purpose
* If a class grows too large, split it into smaller focused classes

## Java/Kotlin Best Practices

### Prefer Immutability

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

### Null Safety (Kotlin)

```kotlin
// Use nullable types explicitly
fun findUserById(id: Long): User?

// Use safe calls
val userName = user?.name ?: "Unknown"

// Avoid !! operator unless absolutely necessary
```

### Use Kotlin Idioms

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

## Spring Best Practices

### Constructor Injection

```kotlin
// GOOD - constructor injection (testable, immutable)
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
)

// BAD - field injection (harder to test)
@Service
class UserService {
    @Autowired
    private lateinit var userRepository: UserRepository
}
```

### Layer Separation

* Controller: HTTP handling only, delegate to services
* Service: Business logic, transaction management
* Repository: Data access only
* Do not leak implementation details between layers

### Exception Handling

```kotlin
// Use custom exceptions for business errors
class UserNotFoundException(userId: Long) :
    BizRuntimeException(ErrorCode.USER_NOT_FOUND, "User not found: $userId")

// Handle exceptions at appropriate level
@ExceptionHandler(BizRuntimeException::class)
fun handleBizException(ex: BizRuntimeException): ResponseEntity<ErrorResponse>
```

## Do Not Overengineer

### Avoid

* Creating interfaces for classes that will only have one implementation
* Building complex abstractions for simple problems
* Adding configuration options that nobody will use
* Creating utility classes for one-off operations
* Premature generalization

### Signs of Overengineering

* More than 3 levels of inheritance
* Classes with only one method
* Excessive use of design patterns
* Configuration for everything
* "Flexible" code that is never flexed

## Testable Code

### Design for Testability

```kotlin
// GOOD - dependencies injected, easy to mock
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

// BAD - hard-coded dependencies, impossible to test
class OrderService {
    fun createOrder(request: OrderRequest): Order {
        val repo = OrderRepositoryImpl()  // Can't mock
        val now = LocalDateTime.now()      // Can't control time
        // ...
    }
}
```

### Pure Functions

* Prefer functions without side effects
* Same input should always produce same output
* Makes testing trivial

### Small, Focused Units

* Small classes and functions are easier to test
* Single responsibility = single test focus

## Code Organization

### File Structure

* One public class per file
* Related classes can be in the same file if small
* Group by feature, not by type

### Import Organization

* Remove unused imports
* Avoid wildcard imports
* Group imports logically

### Comments

```kotlin
// BAD - comment explains what (code should be self-explanatory)
// Loop through users and find active ones
val activeUsers = users.filter { it.isActive }

// GOOD - comment explains why (context that code can't express)
// Filter inactive users to comply with GDPR deletion requirements
val activeUsers = users.filter { it.isActive }
```

* Code should be self-documenting
* Comments should explain WHY, not WHAT
* Keep comments up-to-date or remove them
* Use KDoc/Javadoc for public APIs

## Summary Checklist

Before submitting code, verify:

- [ ] Names are meaningful and descriptive
- [ ] Functions do one thing and do it well
- [ ] Abstraction levels are consistent
- [ ] No duplicate code
- [ ] No unnecessary complexity
- [ ] Dependencies are injected (testable)
- [ ] Edge cases are handled
- [ ] Code is formatted consistently
