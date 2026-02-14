---
name: QueryDSL
description: QueryDSL rules for type-safe queries using QuerydslRepositorySupport, fetch prefix, and @QueryProjection
last-verified: 2026-02-14
---

# QueryDSL Rules

## Overview

QueryDSL usage rules for type-safe query construction in this project.

## Core principles

> **Key Principle**: Use QueryDSL for all complex queries. Extend QuerydslRepositorySupport. Use `@QueryProjection` for DTOs. Prefix select methods with `fetch`.

| Guideline | Description |
|-----------|-------------|
| **Extend QuerydslRepositorySupport** | All QueryDSL repositories must extend the project's support class |
| **`QueryRepository` suffix** | All QueryDSL repository classes must use `QueryRepository` suffix (e.g., `OrderQueryRepository`) |
| **`fetch` prefix** | All QueryDSL select methods must be prefixed with `fetch` |
| **`@QueryProjection`** | Use `@QueryProjection` on DTO constructors for type-safe projections |
| **No associations** | Use QueryDSL JOINs instead of entity associations (see `41_jpa.md`) |
| **`Pageable` for pagination** | Always accept `Pageable` as a parameter for paginated queries |
| **`SearchCondition` for complex filters** | Encapsulate multiple search parameters in a dedicated `{Feature}SearchCondition` object |

## Repository structure

### Standard QueryDSL repository

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

### Naming convention

| Operation | Prefix | Example |
|-----------|--------|---------|
| Single result | `fetchXxx` | `fetchById(id)`, `fetchByEmail(email)` |
| List result | `fetchAllXxx` | `fetchAllByUserId(userId)` |
| Paged result | `fetchPageXxx` | `fetchPageByStatus(status, pageable)` |
| Count | `fetchCountXxx` | `fetchCountByStatus(status)` |
| Exists check | `existsXxx` | `existsByEmail(email)` |

### Incorrect naming

```kotlin
// Bad: Missing fetch prefix
fun findById(id: Long): OrderDto?
fun getOrdersByUser(userId: Long): List<OrderDto>
fun searchOrders(condition: OrderSearchCondition): List<OrderDto>

// Good: fetch prefix
fun fetchById(id: Long): OrderDto?
fun fetchAllByUser(userId: Long): List<OrderDto>
fun fetchAllByCondition(condition: OrderSearchCondition): List<OrderDto>
```

## DTO projection with @QueryProjection

### Standard DTO pattern

```kotlin
data class OrderDto @QueryProjection constructor(
    val id: Long,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
)
```

### DTO with joined data

```kotlin
data class OrderWithUserDto @QueryProjection constructor(
    val orderId: Long,
    val totalAmount: BigDecimal,
    val userName: String,
    val userEmail: String,
)
```

### Usage in repository

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

### Incorrect: Projections.constructor (avoid)

```kotlin
// Bad: Not type-safe, breaks silently if constructor changes
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

// Good: Type-safe with @QueryProjection
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

> **IMPORTANT**: Always use `Pageable` for paginated queries. Do not pass raw `page`/`size` parameters directly.

```kotlin
// Bad: Raw pagination parameters
fun fetchPageByStatus(status: OrderStatus, page: Int, size: Int): List<OrderDto>

// Good: Use Pageable
fun fetchPageByStatus(status: OrderStatus, pageable: Pageable): Page<OrderDto>
```

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

## SearchCondition objects

> **IMPORTANT**: Encapsulate complex search parameters in a dedicated `{Feature}SearchCondition` data class. Do not pass multiple filter parameters individually.

```kotlin
// Bad: Multiple individual parameters
fun fetchAllByCondition(
    name: String?,
    status: UserStatus?,
    startDate: LocalDate?,
    endDate: LocalDate?,
): List<UserDto>

// Good: SearchCondition object
data class UserSearchCondition(
    val name: String? = null,
    val status: UserStatus? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

fun fetchAllByCondition(condition: UserSearchCondition): List<UserDto>
```

### Naming Convention

| Pattern | Example |
|---------|---------|
| `{Feature}SearchCondition` | `OrderSearchCondition`, `UserSearchCondition` |

### Use SearchDates for Date Range Fields

> **Tip**: Use `SearchDates` from the common module instead of raw `startDate`/`endDate` fields when the SearchCondition involves date range filtering. `SearchDates` provides built-in safeguards against invalid or excessively wide date ranges.

```kotlin
import com.myrealtrip.common.utils.datetime.SearchDates

// Bad: Raw date fields with no validation
data class OrderSearchCondition(
    val status: OrderStatus? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)

// Good: Use SearchDates for date range with built-in safeguards
data class OrderSearchCondition(
    val status: OrderStatus? = null,
    val searchDates: SearchDates = SearchDates.lastMonth(),
)
```

**Available factory methods:**

| Method | Range | Description |
|--------|-------|-------------|
| `SearchDates.of(start, end)` | Custom | Custom date range with auto-adjustment |
| `SearchDates.today()` | Today | Single day (today) |
| `SearchDates.yesterday()` | Yesterday | Single day (yesterday) |
| `SearchDates.lastDays(n)` | Last N days | From N days ago to today |
| `SearchDates.lastWeeks(n)` | Last N weeks | From N weeks ago to today |
| `SearchDates.lastMonths(n)` | Last N months | From N months ago to today |
| `SearchDates.thisWeek()` | Current week | Week start to today |
| `SearchDates.lastWeek()` | Previous week | Previous full week |
| `SearchDates.thisMonth()` | Current month | 1st of month to today |
| `SearchDates.lastMonth()` | Previous month | Previous full month |

**Usage in repository:**

```kotlin
fun fetchPageByCondition(
    condition: OrderSearchCondition,
    pageable: Pageable,
): Page<OrderDto> {
    return applyPagination(
        pageable,
        contentQuery = { queryFactory ->
            queryFactory
                .select(QOrderDto(order.id, order.totalAmount, order.status))
                .from(order)
                .where(
                    QuerydslExpressions.eq(order.status, condition.status),
                    QuerydslExpressions.dateBetween(
                        order.createdAt,
                        condition.searchDates.startDate,
                        condition.searchDates.endDate,
                    ),
                )
        },
        countQuery = { queryFactory ->
            queryFactory
                .select(order.count())
                .from(order)
                .where(
                    QuerydslExpressions.eq(order.status, condition.status),
                    QuerydslExpressions.dateBetween(
                        order.createdAt,
                        condition.searchDates.startDate,
                        condition.searchDates.endDate,
                    ),
                )
        },
    )
}
```

### SearchCondition with Pagination

Combine `SearchCondition` with `Pageable` for paginated search queries.

```kotlin
fun fetchPageByCondition(
    condition: OrderSearchCondition,
    pageable: Pageable,
): Page<OrderDto>
```

## Dynamic conditions with QuerydslExpressions

### Using Null-Safe Expressions

Pass `SearchCondition` fields to `QuerydslExpressions` methods for null-safe dynamic filtering.

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

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Using `Projections.constructor` | Not type-safe, fails at runtime | Use `@QueryProjection` |
| Missing `fetch` prefix | Inconsistent naming | Always prefix with `fetch` |
| Not extending QuerydslRepositorySupport | Loses pagination and utility methods | Always extend the support class |
| Separate count query missing in pagination | Incorrect total count | Always provide both content and count queries |
| Hardcoded conditions | Not reusable | Use `QuerydslExpressions` for null-safe conditions |
| Raw `page`/`size` parameters | Bypasses Spring pagination | Always use `Pageable` |
| Multiple individual filter parameters | Hard to extend, cluttered signatures | Use a `{Feature}SearchCondition` object |
| Raw `startDate`/`endDate` in SearchCondition | No validation, no max period guard | Use `SearchDates` from the common module |

## Summary checklist

Before submitting code, verify:

- [ ] Repository extends `QuerydslRepositorySupport`
- [ ] Repository class name uses `QueryRepository` suffix
- [ ] All select methods are prefixed with `fetch`
- [ ] DTOs use `@QueryProjection` constructor
- [ ] Pagination uses `applyPagination` with separate content and count queries
- [ ] Dynamic conditions use `QuerydslExpressions`
- [ ] Paginated queries accept `Pageable` (not raw `page`/`size` parameters)
- [ ] Complex search filters use a `{Feature}SearchCondition` object
- [ ] Date range fields in SearchCondition use `SearchDates` instead of raw `startDate`/`endDate`
- [ ] Q-classes are used for type-safe path references
