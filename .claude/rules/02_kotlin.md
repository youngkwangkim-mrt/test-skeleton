---
name: Kotlin
description: Kotlin coding conventions, idioms, null safety, and best practices
---

# Kotlin Style & Best Practices

## Overview

A pragmatic guide for writing clean, readable, and maintainable Kotlin code.

> **Core Philosophy**: Simple is best. Write code that humans can understand. Don't over-engineer.

Based on [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

## Naming Conventions

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

### Packages

```kotlin
// lowercase, no underscores
package org.example.project
package com.company.feature.domain
```

### Classes & Interfaces

```kotlin
// UpperCamelCase
class UserRepository
class OrderProcessingService
interface PaymentGateway
```

### Functions & Variables

```kotlin
// lowerCamelCase
fun processOrder() { }
fun calculateTotalPrice(): BigDecimal { }

val userName = "John"
var orderCount = 0
```

### Constants

```kotlin
// SCREAMING_SNAKE_CASE for true constants
const val MAX_RETRY_COUNT = 3
val DEFAULT_TIMEOUT = Duration.ofSeconds(30)

companion object {
    private const val TAG = "UserService"
}
```

### Backing Properties

```kotlin
private val _items = mutableListOf<Item>()
val items: List<Item> get() = _items
```

### Acronyms

```kotlin
// Two letters: both uppercase
class IOStream

// Longer: capitalize first letter only
class XmlParser
class HttpClient
```

## Code Organization

### Class Layout Order

Follow this order for consistent, readable classes:

```kotlin
class UserService(
    private val userRepository: UserRepository,  // 1. Constructor properties
) {
    // 2. Properties & initializer blocks
    private val cache = mutableMapOf<Long, User>()

    init {
        // initialization logic
    }

    // 3. Secondary constructors
    constructor() : this(DefaultUserRepository())

    // 4. Public methods
    fun findUser(id: Long): User? { }

    fun saveUser(user: User): User { }

    // 5. Internal/Protected methods
    internal fun clearCache() { }

    // 6. Private methods
    private fun validateUser(user: User) { }

    // 7. Companion object
    companion object {
        private const val CACHE_SIZE = 100
        fun create(): UserService = UserService()
    }

    // 8. Nested/Inner classes
    data class CacheEntry(val user: User, val timestamp: Long)
}
```

### Group Related Members

```kotlin
class OrderService(
    private val orderRepository: OrderRepository,
    private val paymentService: PaymentService,
) {
    // Group: Order creation
    fun createOrder(request: CreateOrderRequest): Order { }
    private fun validateOrderRequest(request: CreateOrderRequest) { }
    private fun calculateOrderTotal(items: List<OrderItem>): Money { }

    // Group: Order retrieval
    fun findOrder(id: Long): Order? { }
    fun findOrdersByUser(userId: Long): List<Order> { }
    private fun enrichOrderWithDetails(order: Order): Order { }

    // Group: Order status management
    fun cancelOrder(id: Long): Order { }
    fun completeOrder(id: Long): Order { }
    private fun updateOrderStatus(id: Long, status: OrderStatus): Order { }
}
```

### Visibility Order Within Groups

```kotlin
class PaymentProcessor {
    // Public first
    fun processPayment(payment: Payment): Receipt { }

    // Then internal
    internal fun retryPayment(paymentId: Long): Receipt { }

    // Then protected (if open class)
    protected fun validatePayment(payment: Payment) { }

    // Then private last
    private fun logPaymentAttempt(payment: Payment) { }
}
```

### Property Organization

```kotlin
class UserViewModel(
    // 1. Injected dependencies
    private val userRepository: UserRepository,
    private val analytics: Analytics,
) : ViewModel() {

    // 2. Public observable state
    val user: StateFlow<User?> = _user.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 3. Private mutable state
    private val _user = MutableStateFlow<User?>(null)
    private val _isLoading = MutableStateFlow(false)

    // 4. Private helpers
    private val scope = viewModelScope
    private var currentJob: Job? = null
}
```

### Companion Object Placement

```kotlin
class ApiClient {
    // Always place companion object at the end
    fun get(url: String): Response { }
    fun post(url: String, body: Any): Response { }

    companion object {
        // Factory methods
        fun create(config: Config): ApiClient = ApiClient()

        // Constants
        private const val DEFAULT_TIMEOUT = 30_000L
        private const val MAX_RETRIES = 3
    }
}
```

### Keep Classes Focused

```kotlin
// Bad: mixed responsibilities
class UserService {
    fun createUser() { }
    fun sendWelcomeEmail() { }    // Email responsibility
    fun generateUserReport() { }  // Reporting responsibility
    fun validateUserData() { }
    fun exportUsersToCsv() { }    // Export responsibility
}

// Good: single responsibility per class
class UserService {
    fun createUser() { }
    fun findUser() { }
    fun updateUser() { }
    fun deleteUser() { }
}

class UserNotificationService {
    fun sendWelcomeEmail() { }
    fun sendPasswordReset() { }
}

class UserReportService {
    fun generateReport() { }
    fun exportToCsv() { }
}
```

### Data Class Layout

```kotlin
data class User(
    // 1. Required properties first
    val id: Long,
    val email: String,

    // 2. Optional properties with defaults
    val name: String = "",
    val role: Role = Role.USER,
    val isActive: Boolean = true,

    // 3. Timestamps last
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
) {
    // Computed properties
    val displayName: String
        get() = name.ifBlank { email.substringBefore("@") }

    // Validation in init block if needed
    init {
        require(email.contains("@")) { "Invalid email format" }
    }

    // Helper methods
    fun isAdmin() = role == Role.ADMIN
}
```

### Sealed Class Layout

```kotlin
sealed class Result<out T> {
    // Success case first (most common)
    data class Success<T>(val data: T) : Result<T>()

    // Error cases
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : Result<Nothing>()

    // State cases
    data object Loading : Result<Nothing>()

    // Common operations in parent
    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw IllegalStateException(message, cause)
        is Loading -> throw IllegalStateException("Still loading")
    }
}
```

### Interface Implementation Order

```kotlin
class UserRepositoryImpl(
    private val database: Database,
) : UserRepository, Closeable {

    // 1. Interface methods (in interface declaration order)
    override fun findById(id: Long): User? { }
    override fun save(user: User): User { }
    override fun delete(id: Long) { }

    // 2. Additional interface methods (Closeable)
    override fun close() { }

    // 3. Own public methods
    fun findByEmail(email: String): User? { }

    // 4. Private methods
    private fun mapToEntity(user: User): UserEntity { }
}
```

### File Naming

```kotlin
// Single class: match class name
UserService.kt

// Multiple related declarations: descriptive name
UserExtensions.kt
OrderValidations.kt
```

## Formatting

### Indentation & Braces

```kotlin
// 4 spaces, opening brace at end of line
if (user != null) {
    processUser(user)
} else {
    handleMissingUser()
}
```

### Function Signatures

```kotlin
// Short: single line
fun findById(id: Long): User? = repository.find(id)

// Long: break parameters
fun createOrder(
    userId: Long,
    items: List<OrderItem>,
    shippingAddress: Address,
    paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
): Order {
    // ...
}
```

### Chained Calls

```kotlin
val result = users
    .filter { it.isActive }
    .map { it.toDto() }
    .sortedBy { it.name }
```

### Trailing Commas

```kotlin
// Use trailing commas for cleaner diffs
data class User(
    val id: Long,
    val name: String,
    val email: String,
)
```

## Clean Code Principles

### Write Self-Documenting Code

```kotlin
// Bad
fun calc(a: Int, b: Int) = a * b + a * 0.1

// Good
fun calculateTotalWithTax(price: Int, quantity: Int): Double {
    val subtotal = price * quantity
    val tax = subtotal * TAX_RATE
    return subtotal + tax
}
```

### Keep Functions Small & Focused

```kotlin
// Bad: doing too much
fun processOrder(order: Order) {
    // validate (20 lines)
    // calculate (15 lines)
    // save (10 lines)
    // notify (15 lines)
}

// Good: single responsibility
fun processOrder(order: Order): ProcessedOrder {
    val validated = validateOrder(order)
    val priced = calculateTotals(validated)
    val saved = orderRepository.save(priced)
    notificationService.sendConfirmation(saved)
    return saved
}
```

### Avoid Deep Nesting

```kotlin
// Bad
fun processUser(user: User?) {
    if (user != null) {
        if (user.isActive) {
            if (user.hasPermission("admin")) {
                // logic here
            }
        }
    }
}

// Good: early returns
fun processUser(user: User?) {
    if (user == null) return
    if (!user.isActive) return
    if (!user.hasPermission("admin")) return

    // logic here
}
```

### Comments: Explain Why, Not What

```kotlin
// Bad: explains what (obvious)
// Increment counter
counter++

// Good: explains why (not obvious)
// Reset after successful connection to prevent accumulated retries
retryCount = 0
```

## OOP & SOLID (Pragmatically)

> Apply when they add value. Don't create abstractions for hypothetical future needs.

### Single Responsibility

```kotlin
// Good: focused class
class OrderPriceCalculator {
    fun calculate(order: Order): Money { }
}

// Simple case: function is fine
fun calculateOrderPrice(order: Order): Money =
    order.items.sumOf { it.price * it.quantity }
```

### Dependency Injection

```kotlin
// Good: injectable dependencies
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
)

// Simple case: default parameters are fine
class SimpleService(
    private val client: HttpClient = HttpClient(),
)
```

### When NOT to Over-Engineer

```kotlin
// Bad: interface for single implementation
interface UserRepository { }
class UserRepositoryImpl : UserRepository { }

// Good: just use the class
class UserRepository { }

// Rule: Wait for 2-3 real use cases before abstracting
```

## Functional Programming (Balanced)

### Prefer Immutability

```kotlin
val users = listOf(user1, user2)  // immutable
fun getActiveUsers(): List<User>  // returns immutable

// Mutable only when necessary
private val _cache = mutableMapOf<Long, User>()
```

### Collection Operations

```kotlin
// Good: readable chain
val activeAdminEmails = users
    .filter { it.isActive }
    .filter { it.role == Role.ADMIN }
    .map { it.email }
```

### Sequences for Large Collections

```kotlin
// Use sequences for large collections with multiple operations
val result = hugeList.asSequence()
    .filter { it.isValid }
    .map { it.transform() }
    .take(10)
    .toList()
```

### Avoid Over-Functional Code

```kotlin
// Bad: too clever
val result = items
    .groupBy { it.category }
    .mapValues { (_, v) -> v.sortedByDescending { it.date }.take(5) }
    .flatMap { (k, v) -> v.map { k to it } }
    .toMap()

// Good: clear steps
val grouped = items.groupBy { it.category }
val topByCategory = grouped.mapValues { (_, items) ->
    items.sortedByDescending { it.date }.take(5)
}
```

## Kotlin Idioms

### Null Safety

```kotlin
val length = name?.length ?: 0
user?.let { saveToDatabase(it) }

requireNotNull(user) { "User cannot be null" }
```

### Data Classes

```kotlin
data class User(
    val id: Long,
    val name: String,
    val email: String,
)

val updated = user.copy(name = "New Name")
```

### Value Objects (from common module)

> **IMPORTANT**: 원시 타입 대신 `com.myrealtrip.common.values` 패키지의 값 객체를 사용하세요.

```kotlin
// Bad: 원시 타입 사용
data class User(
    val email: String,      // 유효성 미검증
    val phone: String,      // 형식 불일치
    val balance: BigDecimal // 통화 정보 없음
)

// Good: 값 객체 사용
import com.myrealtrip.common.values.Email
import com.myrealtrip.common.values.PhoneNumber
import com.myrealtrip.common.values.Money

data class User(
    val email: Email,       // 유효성 검증, 마스킹
    val phone: PhoneNumber, // 자동 파싱/포맷팅
    val balance: Money      // 통화 포함, 타입 안전 연산
)

// 생성
val email = "user@example.com".asEmail
val phone = "010-1234-5678".asPhoneNumber
val money = 10000L.krw
val rate = 15.percent
```

**사용 가능한 값 객체:**

| 타입 | 용도 | 예시 |
|------|------|------|
| `Email` | 이메일 | `Email.of("a@b.com")`, `"a@b.com".asEmail` |
| `PhoneNumber` | 전화번호 | `PhoneNumber.of("010-1234-5678")` |
| `Money` | 금액 | `Money.krw(10000)`, `10000L.krw` |
| `Rate` | 비율/퍼센트 | `Rate.ofPercent(15)`, `15.percent` |

> 자세한 사용법: `modules/common/README.adoc` 또는 `.claude/rules/20_common-module.md` 참조

### Sealed Classes

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

fun handle(result: Result<User>) = when (result) {
    is Result.Success -> showUser(result.data)
    is Result.Error -> showError(result.message)
    is Result.Loading -> showLoading()
}
```

### Extension Functions

```kotlin
fun String.toSlug() = lowercase().replace(" ", "-")

fun User.isEligibleForDiscount() =
    accountAge > Duration.ofDays(365) && totalPurchases > 1000
```

### Scope Functions

```kotlin
// let: null checks
val result = nullableValue?.let { transform(it) }

// apply: object configuration
val user = User().apply {
    name = "John"
    email = "john@example.com"
}

// also: side effects
return user.also { logger.info("Created: ${it.id}") }
```

### Default & Named Arguments

```kotlin
fun createUser(
    name: String,
    email: String,
    role: Role = Role.USER,
    active: Boolean = true,
): User { }

createUser(
    name = "John",
    email = "john@example.com",
    role = Role.ADMIN,
)
```

## Error Handling

> **IMPORTANT**: 예외 처리 시 `com.myrealtrip.common.exceptions` 패키지의 예외 클래스를 사용하세요.

### Nullable for Expected Absence

```kotlin
fun findUser(id: Long): User? = userRepository.findById(id)
```

### Exceptions (from common module)

```kotlin
import com.myrealtrip.common.exceptions.KnownException
import com.myrealtrip.common.exceptions.BizRuntimeException
import com.myrealtrip.common.codes.response.ErrorCode

// 예상된 에러 (validation, not found) - 스택 트레이스 없음
class UserNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND,
    "User not found: $id"
)

// 비즈니스 에러 - 스택 트레이스 포함
throw BizRuntimeException(ErrorCode.ILLEGAL_STATE, "Invalid state")
```

### Precondition 검증 (from common module)

```kotlin
import com.myrealtrip.common.utils.knownRequired
import com.myrealtrip.common.utils.knownRequiredNotNull

// require 대신 knownRequired 사용 (KnownException 발생)
knownRequired(amount > 0) { "Amount must be positive" }

val user = knownRequiredNotNull(repository.findById(id)) {
    "User not found: $id"
}
```

### Result Pattern for Expected Failures

```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
}

fun processPayment(payment: Payment): Result<Receipt> {
    return try {
        Result.Success(paymentGateway.process(payment))
    } catch (e: InsufficientFundsException) {
        Result.Failure(AppError.InsufficientFunds)
    }
}
```

## Testing

### Descriptive Names

```kotlin
@Test
fun `should return empty list when no users match criteria`() { }

@Test
fun `should throw exception when user id is negative`() { }
```

### Arrange-Act-Assert

```kotlin
@Test
fun `should calculate correct total with discount`() {
    // Arrange
    val order = Order(items = listOf(Item(price = 100)), discountPercent = 10)

    // Act
    val total = calculator.calculateTotal(order)

    // Assert
    assertEquals(90.0, total)
}
```

## Anti-Patterns to Avoid

### Over-Engineering

```kotlin
// Bad
interface StringProcessor { fun process(s: String): String }
class UpperCaseProcessor : StringProcessor { ... }
class ProcessorFactory { fun create(): StringProcessor = ... }

// Good
fun toUpperCase(s: String) = s.uppercase()
```

### God Classes

```kotlin
// Bad: class does everything
class UserManager {
    fun createUser() { }
    fun sendEmail() { }
    fun generateReport() { }
    // ... 50 more methods
}

// Good: split by responsibility
class UserService { }
class EmailService { }
class ReportGenerator { }
```

### Copy-Paste Code

```kotlin
// Bad: duplicated validation
fun validateUser(user: User) { /* same logic */ }
fun validateAdmin(admin: Admin) { /* same logic */ }

// Good: reusable
interface HasContactInfo { val name: String; val email: String }

fun HasContactInfo.validateContactInfo() {
    require(name.isNotBlank()) { "Name required" }
    require(email.isNotBlank()) { "Email required" }
}
```

## Summary

| Do | Don't |
|----|-------|
| Write readable code | Write clever code |
| Use Kotlin idioms | Fight the language |
| Keep it simple | Over-engineer |
| Abstract when needed | Abstract preemptively |
| Name things clearly | Use abbreviations |

> **Remember**: Code is read more often than written. Optimize for readability.
