---
name: QueryDSL
description: QueryDSL rules for type-safe queries using QuerydslRepositorySupport, fetch prefix, and @QueryProjection
---

# QueryDSL Rules

## Overview

QueryDSL usage rules for type-safe query construction in this project.

## Core Principles

> **Use QueryDSL for all complex queries.** Extend QuerydslRepositorySupport. Use `@QueryProjection` for DTOs. Prefix select methods with `fetch`.

| Guideline | Description |
|-----------|-------------|
| **Extend QuerydslRepositorySupport** | All QueryDSL repositories must extend the project's support class |
| **`QueryRepository` suffix** | All QueryDSL repository classes must use `QueryRepository` suffix (e.g., `OrderQueryRepository`) |
| **`fetch` prefix** | All QueryDSL select methods must be prefixed with `fetch` |
| **`@QueryProjection`** | Use `@QueryProjection` on DTO constructors for type-safe projections |
| **No associations** | Use QueryDSL JOINs instead of entity associations (see `21_jpa.md`) |

## Repository Structure

### Standard QueryDSL Repository

```kotlin
@Repository
class OrderQueryRepository(
) : QuerydslRepositorySupport(Order::class.java) {

    private val order = QOrder.order
    private val user = QUser.user

    fun fetchById(id: Long): OrderDto? {
        return select(
            QOrderDto(
                order.id,
                order.totalAmount,
                order.status,
            )
        )
            .from(order)
            .where(order.id.eq(id))
            .fetchOne()
    }

    fun fetchAllByUserId(userId: Long): List<OrderDto> {
        return select(
            QOrderDto(
                order.id,
                order.totalAmount,
                order.status,
            )
        )
            .from(order)
            .where(order.userId.eq(userId))
            .orderBy(order.createdAt.desc())
            .fetch()
    }
}
```

### Naming Convention

| Operation | Prefix | Example |
|-----------|--------|---------|
| Single result | `fetchXxx` | `fetchById(id)`, `fetchByEmail(email)` |
| List result | `fetchAllXxx` | `fetchAllByUserId(userId)` |
| Paged result | `fetchPageXxx` | `fetchPageByStatus(status, pageable)` |
| Count | `fetchCountXxx` | `fetchCountByStatus(status)` |
| Exists check | `existsXxx` | `existsByEmail(email)` |

### Incorrect Naming

```kotlin
// BAD: Missing fetch prefix
fun findById(id: Long): OrderDto?
fun getOrdersByUser(userId: Long): List<OrderDto>
fun searchOrders(condition: OrderSearchCondition): List<OrderDto>

// GOOD: fetch prefix
fun fetchById(id: Long): OrderDto?
fun fetchAllByUser(userId: Long): List<OrderDto>
fun fetchAllByCondition(condition: OrderSearchCondition): List<OrderDto>
```

## DTO Projection with @QueryProjection

### Standard DTO Pattern

```kotlin
data class OrderDto @QueryProjection constructor(
    val id: Long,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
)
```

### DTO with Joined Data

```kotlin
data class OrderWithUserDto @QueryProjection constructor(
    val orderId: Long,
    val totalAmount: BigDecimal,
    val userName: String,
    val userEmail: String,
)
```

### Usage in Repository

```kotlin
fun fetchWithUser(orderId: Long): OrderWithUserDto? {
    return select(
        QOrderWithUserDto(
            order.id,
            order.totalAmount,
            user.name,
            user.email,
        )
    )
        .from(order)
        .join(user).on(order.userId.eq(user.id))
        .where(order.id.eq(orderId))
        .fetchOne()
}
```

### Incorrect: Projections.constructor (Avoid)

```kotlin
// BAD: Not type-safe, breaks silently if constructor changes
fun fetchById(id: Long): OrderDto? {
    return select(
        Projections.constructor(
            OrderDto::class.java,
            order.id,
            order.totalAmount,
            order.status,
        )
    )
        .from(order)
        .where(order.id.eq(id))
        .fetchOne()
}

// GOOD: Type-safe with @QueryProjection
fun fetchById(id: Long): OrderDto? {
    return select(
        QOrderDto(
            order.id,
            order.totalAmount,
            order.status,
        )
    )
        .from(order)
        .where(order.id.eq(id))
        .fetchOne()
}
```

## Pagination

### Using applyPagination

```kotlin
fun fetchPageByCondition(
    condition: OrderSearchCondition,
    pageable: Pageable,
): Page<OrderDto> {
    return applyPagination(
        pageable,
        contentQuery = { queryFactory ->
            queryFactory
                .select(
                    QOrderDto(
                        order.id,
                        order.totalAmount,
                        order.status,
                    )
                )
                .from(order)
                .where(
                    QuerydslExpressions.eq(order.status, condition.status),
                    QuerydslExpressions.dateTimeBetween(
                        order.createdAt, condition.startDate, condition.endDate,
                    ),
                )
                .orderBy(order.createdAt.desc())
        },
        countQuery = { queryFactory ->
            queryFactory
                .select(order.count())
                .from(order)
                .where(
                    QuerydslExpressions.eq(order.status, condition.status),
                    QuerydslExpressions.dateTimeBetween(
                        order.createdAt, condition.startDate, condition.endDate,
                    ),
                )
        },
    )
}
```

## Dynamic Conditions with QuerydslExpressions

### Using Null-Safe Expressions

```kotlin
fun fetchAllByCondition(condition: UserSearchCondition): List<UserDto> {
    return select(
        QUserDto(
            user.id,
            user.name,
            user.email,
        )
    )
        .from(user)
        .where(
            QuerydslExpressions.containsIgnoreCase(user.name, condition.name),
            QuerydslExpressions.eq(user.status, condition.status),
            QuerydslExpressions.dateBetween(user.createdAt, condition.startDate, condition.endDate),
        )
        .fetch()
}
```

### Available Expressions

| Method | Description |
|--------|-------------|
| `eq(path, value)` | Equals (String, Boolean, Enum, Number) |
| `gt`, `gte`, `lt`, `lte` | Number comparisons |
| `contains(path, value)` | String contains |
| `containsIgnoreCase(path, value)` | Case-insensitive contains |
| `containsIgnoreCaseAndSpace(path, value)` | Ignore case and whitespace |
| `startsWith(path, value)` | String starts with |
| `in(path, collection)` | In collection |
| `inIgnoreCase(path, collection)` | Case-insensitive in |
| `dateBetween(path, start, end)` | Date range (supports partial) |
| `dateTimeBetween(path, start, end)` | DateTime range (supports partial) |
| `isTrue(path)`, `isFalse(path)` | Boolean checks |

All expression methods return `null` when the value is null or empty, which QueryDSL ignores in `where()` clauses.

## Common Pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Using `Projections.constructor` | Not type-safe, fails at runtime | Use `@QueryProjection` |
| Missing `fetch` prefix | Inconsistent naming | Always prefix with `fetch` |
| Not extending QuerydslRepositorySupport | Loses pagination and utility methods | Always extend the support class |
| Separate count query missing in pagination | Incorrect total count | Always provide both content and count queries |
| Hardcoded conditions | Not reusable | Use `QuerydslExpressions` for null-safe conditions |

## Summary Checklist

Before submitting code, verify:

- [ ] Repository extends `QuerydslRepositorySupport`
- [ ] Repository class name uses `QueryRepository` suffix
- [ ] All select methods are prefixed with `fetch`
- [ ] DTOs use `@QueryProjection` constructor
- [ ] Pagination uses `applyPagination` with separate content and count queries
- [ ] Dynamic conditions use `QuerydslExpressions`
- [ ] Q-classes are used for type-safe path references
