---
name: jpa-rules
description: JPA entity mapping rules - no associations by default, unidirectional only, use QueryDSL for joins
triggers:
  - jpa
  - entity
  - hibernate
  - mapping
argument-hint: ""
---

# JPA & Hibernate Rules

## Overview

JPA entity mapping, fetch strategies, and persistence context management rules for this project.

## Core principles

> **Key Principle**: Keep entities simple. Extend BaseEntity or BaseTimeEntity. Use LAZY fetching. Avoid associations.

| Guideline | Description |
|-----------|-------------|
| **Extend BaseEntity or BaseTimeEntity** | Inherit audit columns |
| **Enum as STRING** | Always `@Enumerated(EnumType.STRING)`, never ORDINAL |
| **LAZY by default** | Use `FetchType.LAZY` for all associations |
| **No associations** | Do not use entity associations by default |

### BaseEntity vs BaseTimeEntity

| Class | Fields | Use Case |
|-------|--------|----------|
| `BaseTimeEntity` | `createdAt`, `modifiedAt` | Entities that only need timestamp auditing |
| `BaseEntity` | `createdAt`, `modifiedAt`, `createdBy`, `modifiedBy` | Entities that need full auditing (who + when) |

## Association policy

> **IMPORTANT**: Do not use entity associations by default.

| Rule | Description |
|------|-------------|
| **Default** | Do not map entity associations |
| **Exception** | Unidirectional only, when absolutely necessary |
| **Prohibited** | Bidirectional associations are strictly forbidden |
| **Querying** | Use QueryDSL for joining related data |

### Correct: No association (default)

```kotlin
@Entity
@Table(name = "orders")
class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,  // Store FK as plain ID value

    @Column(nullable = false)
    var totalAmount: BigDecimal,
) : BaseEntity()
```

### Correct: QueryDSL for joins

```kotlin
// Use QueryDSL to join related data
fun findOrderWithUser(orderId: Long): OrderWithUserDto? {
    return queryFactory
        .select(
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

### Exception: Unidirectional only (when absolutely necessary)

```kotlin
// Unidirectional allowed only when strictly necessary
@Entity
@Table(name = "order_items")
class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @Column(nullable = false)
    var quantity: Int,
) : BaseEntity()
```

### Incorrect: Bidirectional (prohibited)

```kotlin
// Bad: Bidirectional associations are prohibited
@Entity
class Order(
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val items: MutableList<OrderItem> = mutableListOf(),  // PROHIBITED
)

@Entity
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    val order: Order,  // Opposite side of bidirectional
)
```

## Entity structure

### Standard entity pattern

```kotlin
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,
) : BaseEntity()
```

### Entity checklist

| Annotation | Purpose | Required |
|------------|---------|----------|
| `@Entity` | Mark as JPA entity | Yes |
| `@Table(name = "xxx")` | Specify table name | Yes |
| `extends BaseEntity` or `BaseTimeEntity` | Inherit audit columns | Yes |

### BaseTimeEntity (timestamps only)

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

### BaseEntity (full auditing: timestamps + author)

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

## Enum mapping

> **IMPORTANT**: Always use `@Enumerated(EnumType.STRING)` for enum fields. Never use `EnumType.ORDINAL` — it stores the positional index, which silently corrupts data when enum constants are reordered, inserted, or removed.

```kotlin
// Good: STRING — stores the enum constant name
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
var status: OrderStatus = OrderStatus.PENDING
// DB stores: "PENDING", "PAID", "SHIPPED" — safe to reorder

// Bad: ORDINAL — stores the positional index
@Enumerated(EnumType.ORDINAL)
@Column(nullable = false)
var status: OrderStatus = OrderStatus.PENDING
// DB stores: 0, 1, 2 — breaks if enum order changes
```

> **Note**: All categorized domain enums must implement `CommonCode`. See skill: `common-codes` for full enum conventions.

---

## Fetch strategies

### LAZY vs EAGER

| Type | Behavior | Recommendation |
|------|----------|----------------|
| `LAZY` | Load on access | **Always use** |
| `EAGER` | Load immediately | **Never use** |

> **IMPORTANT**: Always specify LAZY for `@ManyToOne` and `@OneToOne` - the defaults are EAGER.

### Solving lazy loading issues

| Problem | Solution |
|---------|----------|
| LazyInitializationException | Fetch required data as DTO via QueryDSL |
| N+1 queries | Use QueryDSL JOIN to fetch in a single query |
| Need data outside transaction | Convert to DTO within the transaction boundary |

## Locking strategies

| Type | Use Case | Trade-off |
|------|----------|-----------|
| **Optimistic** (`@Version`) | Low contention, read-heavy | Retries on conflict |
| **Pessimistic** (`@Lock`) | High contention, critical sections | Blocks other transactions |

### Optimistic locking

```kotlin
@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var stock: Int,

    @Version
    var version: Long = 0,
) : BaseEntity()
```

### Pessimistic locking

```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
fun findByIdForUpdate(id: Long): Product?
```

## Entity state & dirty checking

| State | Description | Tracked by JPA |
|-------|-------------|----------------|
| **New/Transient** | Not yet persisted | No |
| **Managed** | In persistence context | Yes (dirty checking) |
| **Detached** | Outside transaction | No |
| **Removed** | Marked for deletion | Yes |

```kotlin
@Transactional
fun updateUserName(userId: Long, newName: String) {
    val user = userRepository.findById(userId)
        ?: throw KnownException(ErrorCode.DATA_NOT_FOUND, "User not found: $userId")
    user.name = newName
    // No save() needed - JPA detects change automatically
}
```

## Configuration

### Recommended settings

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none              # Never auto-generate DDL in production
    properties:
      hibernate:
        default_batch_fetch_size: 500
        order_updates: true
        order_inserts: true
        jdbc:
          batch_size: 500
    open-in-view: false           # Disable OSIV
```

### OSIV (Open Session In View)

| OSIV | Recommendation |
|------|----------------|
| `true` (default) | **Prohibited** - Holds DB connection too long, hides N+1 issues |
| `false` | **Recommended** - Clear transaction boundaries, fetch only needed data via QueryDSL |

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| N+1 queries | 1 + N queries for lazy associations | Use QueryDSL JOIN to fetch in a single query |
| EAGER anywhere | Loads unnecessary data | Always use LAZY |
| Bidirectional mapping | Complex state management | Unidirectional only; prefer no associations with QueryDSL |
| `@Enumerated(ORDINAL)` | Breaks if enum order changes | Always use `STRING` |
| Missing `@Version` | Lost updates in concurrent scenarios | Add optimistic locking for mutable entities |
| Large batch without flush | OutOfMemoryError | Flush and clear every N items |
| OSIV enabled | DB connection held during view rendering | Set `open-in-view: false` |

## Summary checklist

Before submitting code, verify:

- [ ] Entity extends BaseEntity or BaseTimeEntity
- [ ] Enums use `@Enumerated(EnumType.STRING)`
- [ ] No entity associations used (FK stored as plain ID value)
- [ ] If association is necessary, only unidirectional is used
- [ ] No bidirectional associations exist
- [ ] All associations specify `FetchType.LAZY`
- [ ] Related data is queried via QueryDSL
- [ ] `open-in-view: false` is configured
