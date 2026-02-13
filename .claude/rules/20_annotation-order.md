---
description: Standard annotation ordering for Spring applications (Framework -> Lombok)
globs: "*.{java,kt,kts}"
alwaysApply: false
---

# Annotation Order

## Priority Rule
Spring/JPA annotations first, Lombok last. Core framework before configuration.

| Priority | Category | Examples |
|----------|----------|----------|
| 1st | Core Framework | `@Entity`, `@Service`, `@RestController` |
| 2nd | Configuration | `@Table`, `@RequestMapping`, `@Transactional` |
| 3rd | Validation/Constraints | `@NotNull`, `@Size`, `@Valid` |
| 4th | Lombok | `@Builder`, `@NoArgsConstructor`, `@Getter`, `@ToString` |

## Class-Level Order
- **Entity**: `@Entity` -> `@Table` -> `@Builder` -> `@AllArgsConstructor` -> `@NoArgsConstructor` -> `@Getter`
- **Controller**: `@RestController` -> `@RequestMapping` -> `@RequiredArgsConstructor`
- **Service**: `@Service` -> `@Transactional` -> `@RequiredArgsConstructor`
- **Configuration**: `@Configuration` -> `@Enable*` -> `@RequiredArgsConstructor`
- **DTO**: `@Builder` -> `@AllArgsConstructor` -> `@Getter` -> `@ToString`

## Field-Level Order
- **ID**: `@Id` -> `@GeneratedValue`
- **Column**: `@Column` -> validation (`@NotBlank`, `@Size`)
- **Enum**: `@Enumerated(EnumType.STRING)` -> `@Column`
- **Relationship**: `@ManyToOne` -> `@JoinColumn`
- **Audit**: `@CreatedDate` -> `@Column(updatable = false)`

## Method-Level Order
- **Controller**: `@GetMapping` -> `@PreAuthorize` -> `@Cacheable`
- **Service**: `@Transactional` -> `@CacheEvict`
- **Repository**: `@Query` -> `@Lock` -> `@EntityGraph`
- **Event**: `@Async` -> `@EventListener` or `@TransactionalEventListener`

## Parameter-Level Order
- `@Valid` -> `@RequestBody`

## Lombok Internal Order
1. `@Builder` (object creation)
2. `@NoArgsConstructor` / `@AllArgsConstructor` / `@RequiredArgsConstructor`
3. `@Getter` / `@Setter`
4. `@ToString`
5. `@EqualsAndHashCode`

## Important Rules
- `@ToString` and `@EqualsAndHashCode` are **prohibited on entities** (LazyInitializationException risk)
- `@AllArgsConstructor` is **required** when using `@Builder`

> For detailed examples, see skill: `annotation-order`
