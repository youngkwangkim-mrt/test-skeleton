---
title: "Persistence Patterns (JPA & QueryDSL)"
description: "JPA entity rules, QueryDSL patterns, association policy, fetch strategies, locking strategies"
category: "architecture"
order: 5
last_updated: "2026-02-14"
---

# Persistence Patterns (JPA & QueryDSL)

## Overview

This document explains the project's persistence layer patterns using JPA and QueryDSL. You will learn entity design rules, association policies, fetch strategies, QueryDSL naming conventions, and locking patterns.

## Core Principles

- Inherit BaseEntity or BaseTimeEntity and minimize associations
- Store foreign keys as Long ID values, not entity references
- Use QueryDSL for complex queries and DTO conversion
- Always use FetchType.LAZY for associations
- Disable OSIV (Open Session In View) for clear transaction boundaries

## Entity Rules

### BaseEntity vs BaseTimeEntity

All entities inherit BaseEntity or BaseTimeEntity for audit fields.

| Class | Fields | Use Case |
|-------|--------|----------|
| `BaseTimeEntity` | `createdAt`, `modifiedAt` | Only timestamp auditing needed |
| `BaseEntity` | `createdAt`, `modifiedAt`, `createdBy`, `modifiedBy` | Full auditing needed (who + when) |

#### BaseTimeEntity

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set

    @LastModifiedDate
    @Column(name = "modified_at", nullable = false)
    lateinit var modifiedAt: LocalDateTime
        protected set
}
```

#### BaseEntity

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity : BaseTimeEntity() {

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    lateinit var createdBy: String
        protected set

    @LastModifiedBy
    @Column(name = "modified_by", nullable = false, length = 50)
    lateinit var modifiedBy: String
        protected set
}
```

#### JpaConfig (Enable Auditing)

```kotlin
@Configuration
@EnableJpaAuditing
class JpaConfig(
    @param:Value("\${spring.application.name:system}")
    private val name: String = "",
) {
    @Bean
    @ConditionalOnMissingBean
    fun auditorProvider(): AuditorAware<String> =
        AuditorAware { Optional.of(name) }
}
```

`@EnableJpaAuditing` activates auditing. `AuditorAware` identifies the current user (default: application name).

### Standard Entity Pattern

```kotlin
@Entity
@Table(
    name = "holidays",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_holidays_01", columnNames = ["holiday_date", "name"])
    ]
)
class Holiday(
    holidayDate: LocalDate,
    name: String,
    id: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = id

    @Column(name = "holiday_date", nullable = false)
    var holidayDate: LocalDate = holidayDate
        private set

    @Column(name = "name", nullable = false, length = 100)
    var name: String = name
        private set

    fun update(holidayDate: LocalDate, name: String) {
        this.holidayDate = holidayDate
        this.name = name
    }

    companion object {
        fun create(holidayDate: LocalDate, name: String) = Holiday(
            holidayDate = holidayDate,
            name = name,
        )
    }
}
```

**Key Points:**

- Use `var` with `private set` for immutability. Change values only via the `update()` method.
- Provide a factory method `create()` in the `companion object`.
- Specify explicit column mapping with `@Column(name = "...")`.
- Use named constraints like `UniqueConstraint(name = "uk_holidays_01", ...)`.

### Entity Checklist

| Item | Rule | Example |
|------|------|---------|
| Inheritance | BaseEntity or BaseTimeEntity | `class Holiday : BaseTimeEntity()` |
| Table Name | Specify `@Table(name = "...")` | `@Table(name = "holidays")` |
| Column Name | Specify `@Column(name = "...")` | `@Column(name = "holiday_date")` |
| ID Strategy | `GenerationType.IDENTITY` | `@GeneratedValue(strategy = IDENTITY)` |
| Enum | `EnumType.STRING` (ORDINAL prohibited) | `@Enumerated(EnumType.STRING)` |
| Immutability | `var` + `private set` | `var name: String private set` |
| Update Method | Provide `update()` method | `fun update(name: String)` |
| Factory | `companion object { fun create() }` | `Holiday.create(...)` |

## Association Policy (No Associations)

### Core Rules

> **IMPORTANT**: Do not use entity associations by default.

| Policy | Content |
|--------|---------|
| **Default** | Do not map associations. Store foreign keys as Long type fields. |
| **Exception** | Only unidirectional associations allowed (only when absolutely necessary) |
| **Prohibited** | Bidirectional associations are **strictly forbidden** |
| **Query** | Perform JOIN operations with QueryDSL |

### Correct: No Association (Default)

```kotlin
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,  // ← Store FK as Long type

    @Column(nullable = false)
    var totalAmount: BigDecimal,
) : BaseEntity()
```

### Correct: JOIN with QueryDSL

```kotlin
@Repository
class OrderQueryRepository : QuerydslRepositorySupport(Order::class.java) {

    private val order = QOrder.order
    private val user = QUser.user

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
            .join(user).on(order.userId.eq(user.id))  // ← JOIN with QueryDSL
            .where(order.id.eq(orderId))
            .fetchOne()
    }
}
```

### Exception: Unidirectional Only (Extremely Exceptional)

```kotlin
// Unidirectional association - only when absolutely necessary
@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)  // ← LAZY required
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(nullable = false)
    var quantity: Int,
) : BaseEntity()
```

### Incorrect: Bidirectional (Prohibited)

```kotlin
// BAD: Bidirectional associations are strictly prohibited
@Entity
class Order(
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val items: MutableList<OrderItem> = mutableListOf(),  // ← Prohibited!
)

@Entity
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order,  // ← Other side of bidirectional
)
```

**Why:** Bidirectional associations increase complexity, cause LazyInitializationException, generate N+1 queries, and create testing difficulties.

## QueryDSL Patterns

### QuerydslRepositorySupport

All QueryDSL Repositories inherit `QuerydslRepositorySupport`.

```kotlin
@Repository
class HolidayQueryRepository : QuerydslRepositorySupport(Holiday::class.java) {

    fun fetchPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> {
        return applyPagination(
            pageable,
            contentQuery = { queryFactory ->
                queryFactory
                    .selectFrom(holiday)
                    .where(holiday.holidayDate.year().eq(year))
                    .orderBy(holiday.holidayDate.asc())
            },
            countQuery = { queryFactory ->
                queryFactory
                    .select(holiday.count())
                    .from(holiday)
                    .where(holiday.holidayDate.year().eq(year))
            },
        ).map { HolidayInfo.from(it) }
    }
}
```

**Features:** `select()`, `selectFrom()`, `applyPagination()` (separate content and count queries), `getQueryFactory()`, `getEntityManager()`

### Naming Rules

| Operation | Prefix | Example |
|-----------|--------|---------|
| Single Query | `fetchXxx` | `fetchById(id)` |
| List Query | `fetchAllXxx` | `fetchAllByUserId(userId)` |
| Paging Query | `fetchPageXxx` | `fetchPageByStatus(status, pageable)` |
| Count | `fetchCountXxx` | `fetchCountByStatus(status)` |
| Exists Check | `existsXxx` | `existsByEmail(email)` |

**Wrong:** `findById()`, `getOrdersByUser()` — **Correct:** `fetchById()`, `fetchAllByUser()`

### @QueryProjection DTO Pattern

Use `@QueryProjection` for **type-safe DTO projection**.

```kotlin
// DTO definition
data class HolidayDto @QueryProjection constructor(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
)

// Usage in Repository
@Repository
class HolidayQueryRepository : QuerydslRepositorySupport(Holiday::class.java) {

    fun fetchById(id: Long): HolidayDto? {
        return select(
            QHolidayDto(  // ← Q-class auto-generated
                holiday.id,
                holiday.holidayDate,
                holiday.name,
            )
        )
            .from(holiday)
            .where(holiday.id.eq(id))
            .fetchOne()
    }
}
```

**Wrong:** `Projections.constructor()` has no compile-time type safety and risks runtime errors.

### Pagination Handling

`applyPagination()` separates content and count queries for optimization.

```kotlin
fun fetchPageByCondition(
    condition: HolidaySearchCondition,
    pageable: Pageable,
): Page<HolidayInfo> {
    return applyPagination(
        pageable,
        contentQuery = { queryFactory ->
            queryFactory
                .selectFrom(holiday)
                .where(
                    QuerydslExpressions.eq(holiday.name, condition.name),
                    QuerydslExpressions.dateBetween(
                        holiday.holidayDate,
                        condition.startDate,
                        condition.endDate,
                    ),
                )
                .orderBy(holiday.holidayDate.asc())
        },
        countQuery = { queryFactory ->
            queryFactory
                .select(holiday.count())
                .from(holiday)
                .where(
                    QuerydslExpressions.eq(holiday.name, condition.name),
                    QuerydslExpressions.dateBetween(
                        holiday.holidayDate,
                        condition.startDate,
                        condition.endDate,
                    ),
                )
        },
    ).map { HolidayInfo.from(it) }
}
```

**Why separate queries?** The content query needs `ORDER BY` and `JOIN`. The count query does not, enabling performance optimization.

### QuerydslExpressions (Dynamic Conditions)

QuerydslExpressions provides utilities for null-safe dynamic conditions.

```kotlin
fun fetchAllByCondition(condition: HolidaySearchCondition): List<HolidayInfo> {
    return selectFrom(holiday)
        .where(
            QuerydslExpressions.containsIgnoreCase(holiday.name, condition.name),
            QuerydslExpressions.dateBetween(
                holiday.holidayDate,
                condition.startDate,
                condition.endDate,
            ),
        )
        .fetch()
        .map { HolidayInfo.from(it) }
}
```

**Provided Methods:**

| Method | Description |
|--------|-------------|
| `eq(path, value)` | Equality comparison (String, Boolean, Enum, Number) |
| `gt`, `gte`, `lt`, `lte` | Number comparisons |
| `contains(path, value)` | String contains |
| `containsIgnoreCase(path, value)` | Case-insensitive contains |
| `containsIgnoreCaseAndSpace(path, value)` | Ignore case and whitespace |
| `startsWith(path, value)` | String starts with |
| `in(path, collection)` | IN clause |
| `inIgnoreCase(path, collection)` | Case-insensitive IN |
| `dateBetween(path, start, end)` | Date range (partial allowed) |
| `dateTimeBetween(path, start, end)` | DateTime range (partial allowed) |
| `isTrue(path)`, `isFalse(path)` | Boolean checks |

These methods return `null` if the value is `null` or empty. QueryDSL's `where()` ignores `null` conditions, enabling clean dynamic queries.

## Fetch Strategies

### LAZY vs EAGER

| Strategy | Behavior | Recommended |
|----------|----------|-------------|
| `LAZY` | Load on access | **Always use** |
| `EAGER` | Immediate load | **Never use** |

```kotlin
// GOOD: Specify LAZY
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
val user: User

// BAD: EAGER or default (ManyToOne, OneToOne default to EAGER!)
@ManyToOne  // ← Works as EAGER (dangerous!)
@JoinColumn(name = "user_id")
val user: User
```

### Resolving LazyInitializationException

**Problem:**

```kotlin
@Transactional(readOnly = true)
fun findOrder(id: Long): OrderDto {
    val order = orderRepository.findById(id)
    return OrderDto(
        id = order.id,
        userName = order.user.name  // ← LazyInitializationException!
    )
}
```

**Solution: Convert to DTO with QueryDSL**

```kotlin
@Transactional(readOnly = true)
fun findOrder(id: Long): OrderDto {
    return orderQueryRepository.fetchById(id)  // ← Single query to DTO
}

// QueryRepository
fun fetchById(id: Long): OrderDto? {
    return select(
        QOrderDto(
            order.id,
            user.name,  // ← Fetched at once with JOIN
        )
    )
        .from(order)
        .join(user).on(order.userId.eq(user.id))
        .where(order.id.eq(id))
        .fetchOne()
}
```

## Locking Strategies

### Optimistic Locking (@Version)

**Use Case:** High read traffic with low concurrent update conflicts.

```kotlin
@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var stock: Int,

    @Version
    var version: Long = 0,  // ← Optimistic Lock
) : BaseEntity()
```

On update: `UPDATE ... SET stock = ?, version = 6 WHERE id = ? AND version = 5`. If another transaction updated the row first, JPA throws `OptimisticLockingFailureException`.

**Pros:** No database lock, high performance.
**Cons:** Requires retry logic.

### Pessimistic Locking (@Lock)

**Use Case:** High concurrent update conflicts that must process sequentially.

```kotlin
interface ProductJpaRepository : JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    fun findByIdForUpdate(id: Long): Product?
}
```

This query executes `SELECT ... FOR UPDATE`. Other transactions wait until the lock releases.

**Pros:** Definite concurrency control.
**Cons:** Database lock, wait time, deadlock risk.

## JPA Configuration

### Recommended Settings

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # none in production, create-drop/validate locally
    properties:
      hibernate:
        default_batch_fetch_size: 500  # Prevent N+1
        order_updates: true            # Sort UPDATE order (prevent Deadlock)
        order_inserts: true            # Sort INSERT order
        jdbc:
          batch_size: 500              # Batch Insert/Update
    open-in-view: false                # Disable OSIV (required!)
```

### OSIV (Open Session In View)

| Setting | Recommended | Reason |
|---------|-------------|--------|
| `true` (default) | **Prohibited** | Holds database connection too long, hides N+1 issues |
| `false` | **Required** | Provides clear transaction boundaries, enables early detection |

**OSIV enabled:** The Hibernate session stays open until view rendering completes, hiding N+1 queries.
**OSIV disabled (recommended):** The Service returns a DTO. QueryDSL fetches only needed data within the transaction.

### ddl-auto Strategy

| Profile | Setting | Description |
|---------|---------|-------------|
| embed, local | `create-drop` | Generate DDL on start, drop on shutdown |
| dev, test | `validate` | Only verify entity-schema match |
| stage, prod | `none` | Do nothing (use Flyway or Liquibase) |

## Dirty Checking

JPA automatically detects changes to Managed entities. You do not need to call `save()`. JPA executes UPDATE on commit.

**Entity States:** Transient (new), Managed (tracked), Detached (no tracking), Removed (deletion pending).

## Common Pitfalls

### 1. N+1 Queries

**Bad:** Call `orderRepository.findAll()` and then lazy load associations in a map → generates N+1 queries.
**Good:** Use QueryDSL JOIN to fetch all data in a single query.

### 2. EAGER Anywhere

**Bad:** EAGER fetches data on every query.
**Good:** Store foreign keys as Long values and use QueryDSL JOIN only when needed.

### 3. Bidirectional Associations

**Bad:** Use bidirectional associations.
**Good:** Avoid associations and query with QueryDSL.

### 4. @Enumerated(ORDINAL)

**Bad:** ORDINAL stores the positional index (data corruption if enum order changes).
**Good:** Always use STRING.

### 5. Missing flush/clear in Large Batch

**Bad:** Large batches accumulate in the persistence context → OutOfMemoryError.
**Good:** Use `chunked(500)` and call `flush()` and `clear()` periodically.

## Summary

### Entity

- [ ] Inherits BaseEntity or BaseTimeEntity
- [ ] Specifies `@Table(name = "...")`
- [ ] Specifies `@Column(name = "...")`
- [ ] Uses `@Enumerated(EnumType.STRING)`
- [ ] Does not use associations (foreign keys stored as Long type)
- [ ] Implements immutability with `var` + `private set` + `update()` method

### QueryDSL

- [ ] Inherits QuerydslRepositorySupport
- [ ] Uses `fetch` prefix for query methods
- [ ] Uses `@QueryProjection` DTO pattern
- [ ] Uses `applyPagination()` (separate content and count queries)
- [ ] Uses QuerydslExpressions for dynamic conditions

### JPA Configuration

- [ ] Sets `open-in-view: false`
- [ ] Sets `default_batch_fetch_size: 500`
- [ ] Enables Batch Insert and Update
- [ ] Sets profile-specific `ddl-auto` strategy

### Performance

- [ ] Prevents N+1 queries (uses QueryDSL JOIN)
- [ ] Prohibits EAGER usage
- [ ] Calls flush and clear in large batches
- [ ] Chooses appropriate locking strategy

## Related Documents

- [DTO Flow and Conversion](03-dto-flow.md)
- [JPA Rules](../rules/41_jpa.md)
- [QueryDSL Rules](../rules/42_querydsl.md)
