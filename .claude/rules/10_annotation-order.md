---
name: Annotation Order
description: Standard annotation ordering for Spring applications (Framework → Lombok)
---

# Annotation Order

Standard ordering for annotations in Spring applications. Framework annotations first, then Lombok.

## Key Principles

> **Spring → Lombok.** Core framework annotations come first, utility annotations (Lombok) come last.

| Priority | Category | Examples |
|----------|----------|----------|
| 1st | Core Framework | `@Entity`, `@Service`, `@RestController` |
| 2nd | Configuration | `@Table`, `@RequestMapping`, `@Transactional` |
| 3rd | Validation/Constraints | `@NotNull`, `@Size`, `@Valid` |
| 4th | Lombok | `@Builder`, `@NoArgsConstructor`, `@Getter`, `@ToString` |

---

## Class-Level Annotations

### Entity

```kotlin
@Entity                                    // 1. JPA core
@Table(name = "users")                     // 2. JPA configuration
@Builder                                   // 3. Lombok - builder
@AllArgsConstructor                        // 4. Lombok - @Builder requires this
@NoArgsConstructor(access = PROTECTED)     // 5. Lombok - JPA requires this
@Getter                                    // 6. Lombok - getter
class User
```

> **Note:** Entity에는 `@ToString`, `@EqualsAndHashCode` 사용을 피한다. 연관관계 필드에서 LazyInitializationException 또는 무한 루프 발생 위험.

### Controller

```kotlin
@RestController                            // 1. Spring core
@RequestMapping("/api/v1/users")           // 2. Spring configuration
@RequiredArgsConstructor                   // 3. Lombok
class UserController
```

### Service

```kotlin
@Service                                   // 1. Spring core
@Transactional(readOnly = true)            // 2. Spring configuration
@RequiredArgsConstructor                   // 3. Lombok
class UserService
```

### Repository

```kotlin
@Repository                                // 1. Spring core (optional for JpaRepository)
interface UserRepository
```

### Configuration

```kotlin
@Configuration                             // 1. Spring core
@EnableJpaAuditing                         // 2. Enable features
@RequiredArgsConstructor                   // 3. Lombok
class JpaConfig
```

### Component

```kotlin
@Component                                 // 1. Spring core
@ConditionalOnProperty(...)                // 2. Spring conditions
@RequiredArgsConstructor                   // 3. Lombok
class MyComponent
```

---

## Field-Level Annotations

### Entity Field (ID)

```kotlin
@Id                                        // 1. JPA core
@GeneratedValue(strategy = IDENTITY)       // 2. JPA configuration
val id: Long
```

### Entity Field (Column)

```kotlin
@Column(nullable = false, length = 100)    // 1. JPA mapping
@NotBlank                                  // 2. Validation
var name: String
```

### Entity Field (Enum)

```kotlin
@Enumerated(EnumType.STRING)               // 1. JPA mapping
@Column(length = 20)                       // 2. JPA configuration
var status: UserStatus
```

### Entity Field (Relationship)

```kotlin
@ManyToOne(fetch = FetchType.LAZY)         // 1. JPA relationship
@JoinColumn(name = "user_id")             // 2. JPA join configuration
val user: User
```

### Audit Fields

```kotlin
@CreatedDate                               // 1. Spring Data Audit
@Column(updatable = false)                 // 2. JPA configuration
var createdAt: LocalDateTime
```

### Dependency Injection

```kotlin
@Autowired                                 // Spring injection (prefer constructor)
@Qualifier("primaryDataSource")            // Spring qualifier
val dataSource: DataSource
```

---

## Method-Level Annotations

### Controller Method

```kotlin
@GetMapping("/{id}")                       // 1. HTTP method mapping
@PreAuthorize("hasRole('ADMIN')")          // 2. Security
@Cacheable("users")                        // 3. Caching
fun getUser(@PathVariable id: Long)
```

### Service Method

```kotlin
@Transactional                             // 1. Transaction
@CacheEvict("users", allEntries = true)    // 2. Cache management
fun updateUser(...)
```

### Repository Method

```kotlin
@Query("SELECT u FROM User u WHERE ...")   // 1. Query definition
@Lock(LockModeType.PESSIMISTIC_WRITE)      // 2. Locking
@EntityGraph(attributePaths = ["orders"])  // 3. Fetch strategy
fun findByIdForUpdate(id: Long): User?
```

### Async/Event

```kotlin
// 일반 이벤트 리스너
@Async                                     // 1. Execution mode (optional)
@EventListener                             // 2. Event handling
fun handleEvent(event: UserCreatedEvent)

// 트랜잭션 이벤트 리스너 (둘 중 하나 선택)
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
fun handleAfterCommit(event: UserCreatedEvent)
```

---

## Parameter-Level Annotations

### Controller Parameters

```kotlin
fun createUser(
    @Valid                                 // 1. Validation trigger
    @RequestBody                           // 2. Binding source
    request: CreateUserRequest
)

fun getUsers(
    @RequestParam(defaultValue = "0")      // Query parameter
    page: Int,
    @PathVariable                          // Path variable
    id: Long,
)
```

---

## DTO/Request Classes

```kotlin
@Builder                                   // 1. Lombok - builder
@AllArgsConstructor                        // 2. Lombok - constructor
@Getter                                    // 3. Lombok - getter
@ToString                                  // 4. Lombok - toString (DTO는 OK)
data class CreateUserRequest(
    @field:NotBlank                        // Validation
    @field:Size(max = 100)                 // Validation constraint
    val name: String,

    @field:NotNull                         // Validation
    @field:Email                           // Format validation
    val email: String,
)
```

---

## Quick Reference

| Layer | Order |
|-------|-------|
| **Entity** | `@Entity` → `@Table` → `@Builder` → `@AllArgsConstructor` → `@NoArgsConstructor` → `@Getter` |
| **Controller** | `@RestController` → `@RequestMapping` → `@RequiredArgsConstructor` |
| **Service** | `@Service` → `@Transactional` → `@RequiredArgsConstructor` |
| **DTO** | `@Builder` → `@AllArgsConstructor` → `@Getter` → `@ToString` |
| **Field (ID)** | `@Id` → `@GeneratedValue` |
| **Field (Column)** | `@Column` → `@NotBlank` |
| **Field (Relation)** | `@ManyToOne` → `@JoinColumn` |
| **Method** | `@GetMapping` → `@PreAuthorize` → `@Cacheable` |
| **Parameter** | `@Valid` → `@RequestBody` |

---

## Lombok Order

| Order | Annotation | Purpose |
|-------|------------|---------|
| 1st | `@Builder` | Object creation |
| 2nd | `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor` | Constructors |
| 3rd | `@Getter`, `@Setter` | Accessors |
| 4th | `@ToString` | String representation |
| 5th | `@EqualsAndHashCode` | Equality |

---

## Summary

| Rule | Description |
|------|-------------|
| **Framework First** | Spring/JPA annotations before Lombok |
| **Core Before Config** | `@Entity` before `@Table`, `@Service` before `@Transactional` |
| **Lombok Order** | Builder → Constructor → Getter → ToString |
| **Entity 주의** | `@ToString`, `@EqualsAndHashCode` 사용 금지 |
| **@Builder 사용 시** | `@AllArgsConstructor` 필수 |
