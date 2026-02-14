---
name: Common Codes & Enums
description: Enum conventions for domain codes including CommonCode interface, EnumType.STRING, and categorization patterns
last-verified: 2026-02-14
---

# Common codes & enums

## Overview

This document defines rules for categorized domain codes as enums. All domain enums that represent classifiable codes must implement the `CommonCode` interface from the common module.

> **Key Principle**: All categorized domain codes must implement `CommonCode`. Always persist enums as `STRING` in JPA entities — never use `ORDINAL`.

## CommonCode interface

`CommonCode` in `com.myrealtrip.common.codes` defines the contract for all domain code enums.

```kotlin
interface CommonCode {
    val code: String        // Machine-readable code value
    val description: String // Human-readable description
    val name: String        // Enum constant name (provided by Kotlin enum)
}
```

> **IMPORTANT**: All enums that represent categorized business codes must implement `CommonCode`. This approach ensures consistent code/description access across the project and enables automatic REST Docs generation via `CommonCodeDocsTest`.

---

## Enum definition

### Standard pattern

```kotlin
import com.myrealtrip.common.codes.CommonCode

enum class OrderStatus(
    override val code: String,
    override val description: String,
) : CommonCode {
    PENDING("PENDING", "Order placed, awaiting payment"),
    PAID("PAID", "Payment confirmed"),
    SHIPPED("SHIPPED", "Order shipped"),
    DELIVERED("DELIVERED", "Order delivered"),
    CANCELLED("CANCELLED", "Order cancelled"),
    REFUNDED("REFUNDED", "Order refunded"),
    ;

    companion object {
        fun fromCode(code: String): OrderStatus =
            entries.find { it.code == code }
                ?: throw IllegalArgumentException("Unknown OrderStatus code: $code")
    }
}
```

### Naming conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Enum class | PascalCase, descriptive noun | `OrderStatus`, `PaymentMethod`, `BookingType` |
| Enum constants | SCREAMING_SNAKE_CASE | `PENDING`, `IN_PROGRESS`, `CREDIT_CARD` |
| `code` value | Matches constant name or domain-specific code | `"PENDING"`, `"CC"` |
| `description` | Clear English description | `"Order placed, awaiting payment"` |

### Trailing semicolon

> **IMPORTANT**: Add a trailing semicolon (`;`) after the last enum constant when the enum has a body (companion object, methods, or properties).

```kotlin
// Good: Trailing semicolon before companion object
enum class UserRole(
    override val code: String,
    override val description: String,
) : CommonCode {
    ADMIN("ADMIN", "System administrator"),
    USER("USER", "Regular user"),
    GUEST("GUEST", "Guest user"),
    ;  // Required when enum has a body

    companion object { ... }
}

// Bad: Missing semicolon
enum class UserRole(...) : CommonCode {
    ADMIN("ADMIN", "System administrator"),
    GUEST("GUEST", "Guest user")  // Compilation error if companion follows
    companion object { ... }
}
```

---

## JPA entity usage

> **IMPORTANT**: Use `@Enumerated(EnumType.STRING)` for enum fields in JPA entities. Never use `EnumType.ORDINAL` — it breaks silently when you reorder or remove enum constants.

### Correct: EnumType.STRING

```kotlin
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.PENDING,
) : BaseTimeEntity()
```

### Incorrect: EnumType.ORDINAL

```kotlin
// Bad: ORDINAL stores position index — breaks when enum order changes
@Enumerated(EnumType.ORDINAL)
@Column(nullable = false)
var status: OrderStatus = OrderStatus.PENDING
// DB stores: 0, 1, 2... If PENDING moves to position 2, all data corrupts
```

### Column length

Set `@Column(length = N)` to match the longest enum constant name. This setting prevents truncation and documents the expected range.

```kotlin
// Good: length matches longest constant ("CANCELLED" = 9 chars, use 20 for safety)
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
var status: OrderStatus = OrderStatus.PENDING

// Bad: No length specified — defaults to 255, wastes storage
@Enumerated(EnumType.STRING)
@Column(nullable = false)
var status: OrderStatus = OrderStatus.PENDING
```

---

## Lookup methods

### fromCode factory

Provide a `fromCode` companion method when external systems send code values that must be mapped to enum constants.

```kotlin
companion object {
    fun fromCode(code: String): PaymentMethod =
        entries.find { it.code == code }
            ?: throw IllegalArgumentException("Unknown PaymentMethod code: $code")
}
```

### Nullable lookup

Use a nullable variant when the code may not exist and `null` is an acceptable result.

```kotlin
companion object {
    fun fromCodeOrNull(code: String): PaymentMethod? =
        entries.find { it.code == code }
}
```

---

## When to use CommonCode

| Scenario | Use CommonCode | Example |
|----------|---------------|---------|
| Business status codes | Yes | `OrderStatus`, `BookingStatus` |
| Category/type classifications | Yes | `PaymentMethod`, `BookingType` |
| Role/permission types | Yes | `UserRole`, `MembershipTier` |
| Configuration flags | No | Simple `Boolean` or constant |
| Internal-only markers | No | Private sealed class or plain enum |
| Response codes | No | Use `ResponseCode` interface instead |

### When not to implement CommonCode

Do not implement `CommonCode` for:
- **Response codes** — Use `ResponseCode` (e.g., `ErrorCode` and `SuccessCode`)
- **Internal flags** — Simple enums without `code`/`description` that are never exposed externally
- **Sealed classes** — Use sealed classes for complex state with data

```kotlin
// Good: ResponseCode for HTTP response codes (not CommonCode)
enum class ErrorCode(
    override val status: Int,
    override val message: String,
) : ResponseCode {
    DATA_NOT_FOUND(406, "Data not found"),
}

// Good: Simple enum without CommonCode (internal use only)
enum class SortDirection {
    ASC, DESC
}
```

---

## Package location

| Module | Location | Example |
|--------|----------|---------|
| Domain-specific codes | `domain/{feature}/entity/` (alongside Entity) | `domain/order/entity/OrderStatus.kt` |
| Shared across features | `domain/common/codes/` | `domain/common/codes/Gender.kt` |
| Common module codes | `common/codes/` | `common/codes/response/ErrorCode.kt` |

---

## REST Docs integration

`CommonCodeDocsTest` automatically documents enums implementing `CommonCode`. The test generates REST Docs snippets listing all `code` and `description` pairs, ensuring API documentation stays in sync with the codebase.

---

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| `EnumType.ORDINAL` | Data corruption on reorder/removal | Always use `EnumType.STRING` |
| Missing `CommonCode` | Inconsistent code/description access | Implement `CommonCode` for all domain codes |
| No `@Column(length)` on enum fields | Defaults to 255, wastes storage | Set length to match longest constant |
| Missing `fromCode` method | No safe mapping from external code values | Add `companion object { fun fromCode() }` |
| Enum without trailing semicolon | Compilation error when body is added later | Always add `;` after last constant |
| Hardcoded string comparisons | Fragile, bypasses type safety | Use enum constants directly |

---

## Summary checklist

Before submitting code with enums, verify:

- [ ] All categorized domain codes implement `CommonCode`
- [ ] Enum has `code` and `description` properties
- [ ] JPA entity fields use `@Enumerated(EnumType.STRING)` — never `ORDINAL`
- [ ] `@Column(length = N)` is set to a reasonable value for enum fields
- [ ] Trailing semicolon is present after the last constant when the enum has a body
- [ ] `fromCode` companion method exists when external code mapping is needed
- [ ] Enum is placed in the correct package (`domain/{feature}/entity/` or `domain/common/codes/`)
- [ ] Response codes use `ResponseCode`, not `CommonCode`
