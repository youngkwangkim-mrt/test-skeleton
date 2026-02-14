---
name: Controller Design
description: RESTful controller conventions including URL design, ApiResource responses, and search patterns
last-verified: 2026-02-14
---

# Controller design rules

## Overview

This document defines rules for designing REST controllers with consistent URL patterns, response formatting, and search conventions.

> **Key Principle**: All API paths start with `/api/v1/`, use kebab-case and plural nouns, and always return `ApiResource`.

## URL design

### Base path

> **IMPORTANT**: Start all API paths with `/api/v1/`.

```kotlin
// Good
@RequestMapping("/api/v1/users")
@RequestMapping("/api/v1/order-items")

// Bad: Missing version prefix
@RequestMapping("/api/users")
@RequestMapping("/users")

// Bad: Missing /api prefix
@RequestMapping("/v1/users")
```

### Kebab-case

> **IMPORTANT**: Use kebab-case for all URL path segments. Do not use camelCase, snake_case, or PascalCase.

```kotlin
// Good: kebab-case
@RequestMapping("/api/v1/order-items")
@RequestMapping("/api/v1/flight-bookings")
@RequestMapping("/api/v1/user-profiles")

// Bad: camelCase
@RequestMapping("/api/v1/orderItems")

// Bad: snake_case
@RequestMapping("/api/v1/order_items")

// Bad: PascalCase
@RequestMapping("/api/v1/OrderItems")
```

### Plural resource names

Use plural nouns for resource collections.

```kotlin
// Good: Plural
@RequestMapping("/api/v1/users")
@RequestMapping("/api/v1/orders")
@RequestMapping("/api/v1/holidays")

// Bad: Singular
@RequestMapping("/api/v1/user")
@RequestMapping("/api/v1/order")
```

### RESTful URL patterns

| Operation | HTTP Method | URL Pattern | Example |
|-----------|-------------|-------------|---------|
| List / Search | `GET` | `/api/v1/{resources}` | `GET /api/v1/orders` |
| Get by ID | `GET` | `/api/v1/{resources}/{id}` | `GET /api/v1/orders/1` |
| Create | `POST` | `/api/v1/{resources}` | `POST /api/v1/orders` |
| Update | `PUT` | `/api/v1/{resources}/{id}` | `PUT /api/v1/orders/1` |
| Partial Update | `PATCH` | `/api/v1/{resources}/{id}` | `PATCH /api/v1/orders/1` |
| Delete | `DELETE` | `/api/v1/{resources}/{id}` | `DELETE /api/v1/orders/1` |
| Bulk Create | `POST` | `/api/v1/{resources}/bulk` | `POST /api/v1/orders/bulk` |
| Sub-resource | `GET` | `/api/v1/{resources}/{id}/{sub}` | `GET /api/v1/users/1/orders` |

### URL design guidelines

| Rule | Description |
|------|-------------|
| **Use nouns, not verbs** | `/api/v1/orders` not `/api/v1/get-orders` |
| **Hierarchy via nesting** | `/api/v1/users/{id}/orders` for user's orders |
| **Max 3 levels deep** | Avoid `/api/v1/a/{id}/b/{id}/c/{id}/d` |
| **No trailing slashes** | `/api/v1/orders` not `/api/v1/orders/` |
| **Actions as sub-paths** | `POST /api/v1/orders/{id}/cancel` for non-CRUD operations |

---

## Response format

> **IMPORTANT**: All controllers must return `ResponseEntity<ApiResource<T>>`. The only exceptions are `GlobalController` and `HomeController`.

### ApiResource methods

| Method | Return Type | Use Case |
|--------|-------------|----------|
| `ApiResource.success()` | `ResponseEntity<ApiResource<String>>` | DELETE or void operations |
| `ApiResource.success(data)` | `ResponseEntity<ApiResource<T>>` | Single object |
| `ApiResource.of(data)` | `ResponseEntity<ApiResource<T>>` | Auto-detects Collection/Map/Page |
| `ApiResource.ofPage(page)` | `ResponseEntity<ApiResource<List<T>>>` | Paginated results |
| `ApiResource.ofCollection(list)` | `ResponseEntity<ApiResource<Collection<T>>>` | Collection with size meta |

### Pageable default

> **IMPORTANT**: Set `Pageable.ofSize(100)` as the default for all paginated endpoints.

```kotlin
// Good: Default page size of 100
@GetMapping
fun getAll(
    pageable: Pageable = Pageable.ofSize(100),
): ResponseEntity<ApiResource<List<OrderDto>>>

// Bad: No default (Spring default is 20)
@GetMapping
fun getAll(
    pageable: Pageable,
): ResponseEntity<ApiResource<List<OrderDto>>>
```

### Standard CRUD examples

```kotlin
@RestController
@RequestMapping("/api/v1/orders")
@Validated
class OrderController(
    private val orderFacade: OrderFacade,
) {

    // GET single
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<ApiResource<OrderDto>> =
        ApiResource.success(orderFacade.findById(id))

    // GET paginated list
    @GetMapping
    fun getAll(
        pageable: Pageable = Pageable.ofSize(100),
    ): ResponseEntity<ApiResource<List<OrderDto>>> =
        ApiResource.ofPage(orderFacade.findAll(pageable))

    // POST create
    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateOrderApiRequest,
    ): ResponseEntity<ApiResource<OrderDto>> =
        ApiResource.success(orderFacade.create(request.toDomainRequest()))

    // PUT update
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateOrderApiRequest,
    ): ResponseEntity<ApiResource<OrderDto>> =
        ApiResource.success(orderFacade.update(id, request.toDomainRequest()))

    // DELETE
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResource<String>> {
        orderFacade.delete(id)
        return ApiResource.success()
    }
}
```

---

## Search endpoints

### Simple filtering

For 1-2 filter parameters, use `@RequestParam` directly.

```kotlin
@GetMapping
fun getByStatus(
    @RequestParam(required = false) status: OrderStatus?,
    pageable: Pageable = Pageable.ofSize(100),
): ResponseEntity<ApiResource<List<OrderDto>>> =
    ApiResource.ofPage(orderFacade.findByStatus(status, pageable))
```

### Complex search with SearchCondition

> **IMPORTANT**: If you have 3 or more filter parameters, encapsulate them in a `{Feature}SearchCondition` object. Use `SearchDates` for date range fields.

```kotlin
// Search condition DTO (in dto/request/)
data class OrderSearchApiRequest(
    val status: OrderStatus? = null,
    val customerName: String? = null,
    val minAmount: BigDecimal? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
) {
    fun toSearchCondition(): OrderSearchCondition = OrderSearchCondition(
        status = status,
        customerName = customerName,
        minAmount = minAmount,
        searchDates = SearchDates.of(
            startDate = startDate ?: LocalDate.now().minusMonths(1),
            endDate = endDate ?: LocalDate.now(),
        ),
    )
}

// Controller
@GetMapping("/search")
fun search(
    @ModelAttribute condition: OrderSearchApiRequest,
    pageable: Pageable,
): ResponseEntity<ApiResource<List<OrderDto>>> =
    ApiResource.ofPage(orderFacade.search(condition.toSearchCondition(), pageable))
```

### SearchDates in search endpoints

The `SearchDates` class from the common module validates and constrains date ranges. It provides built-in safeguards against invalid or excessively wide ranges.

```kotlin
import com.myrealtrip.common.utils.datetime.SearchDates

// SearchDates auto-adjusts invalid ranges in non-strict mode:
// - If startDate > endDate, adjusts startDate to endDate - searchPeriod
// - If range exceeds maxSearchPeriod, adjusts startDate to endDate - maxSearchPeriod

val dates = SearchDates.of(startDate, endDate)               // Custom range
val dates = SearchDates.of(startDate, endDate, strict = true) // Throws on invalid range
val dates = SearchDates.lastMonth()                           // Previous full month
val dates = SearchDates.lastDays(7)                           // Last 7 days
val dates = SearchDates.thisWeek()                            // Current week to today
```

---

## Controller structure

### Method size

> **IMPORTANT**: Keep controller methods to **7 lines or fewer**. Controllers handle HTTP routing only. All business logic belongs in the Facade or lower layers.

If a method exceeds 7 lines, extract the conversion logic into the request DTO or the Facade.

```kotlin
// Good: 3 lines — delegate everything to Facade
@PostMapping
fun create(
    @Valid @RequestBody request: CreateOrderApiRequest,
): ResponseEntity<ApiResource<OrderDto>> =
    ApiResource.success(orderFacade.create(request.toDomainRequest()))

// Bad: 10+ lines — conversion logic in controller
@PostMapping
fun create(
    @Valid @RequestBody request: CreateOrderApiRequest,
): ResponseEntity<ApiResource<OrderDto>> {
    val items = request.items.map { OrderItem(it.productId, it.quantity) }
    val shippingAddress = Address(request.street, request.city, request.zipCode)
    val domainRequest = CreateOrderRequest(
        customerId = request.customerId,
        items = items,
        shippingAddress = shippingAddress,
    )
    return ApiResource.success(orderFacade.create(domainRequest))
}
```

> **Tip**: Use `toDomainRequest()` extension or method on the API request DTO to keep controllers thin.

### Annotation order

Follow the annotation ordering from `20_annotation-order.md`:

```kotlin
@RestController                          // 1. Spring core
@RequestMapping("/api/v1/orders")        // 2. Spring configuration
@Validated                               // 3. Validation (when using @Min, @Max, etc. on parameters)
class OrderController(
    private val orderFacade: OrderFacade, // Inject Facade only
)
```

### Method annotation order

```kotlin
@GetMapping("/{id}")                     // 1. HTTP method mapping
@PreAuthorize("hasRole('ADMIN')")        // 2. Security (if needed)
@Cacheable("orders")                     // 3. Caching (if needed)
fun getById(@PathVariable id: Long): ResponseEntity<ApiResource<OrderDto>>
```

### Parameter annotation order

```kotlin
fun create(
    @Valid                               // 1. Validation trigger
    @RequestBody                         // 2. Binding source
    request: CreateOrderApiRequest,
): ResponseEntity<ApiResource<OrderDto>>
```

### Dependencies

> **IMPORTANT**: Controllers inject Facade only. Direct injection of Service or Repository is prohibited. See `92_layer-architecture.md`.

> **Note**: For trivially simple endpoints where the Facade would be a pure pass-through with no DTO conversion, you can inject Domain Application (QueryApplication / CommandApplication) directly. This exception applies only when the API DTO and Domain DTO are identical, or no conversion is needed.

```kotlin
// Standard: Controller → Facade (recommended)
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderFacade: OrderFacade,
)

// Exception: Simple case where Facade adds no value
@RestController
@RequestMapping("/api/v1/health-checks")
class HealthCheckController(
    private val healthCheckQueryApplication: HealthCheckQueryApplication,
) {
    @GetMapping
    fun getStatus(): ResponseEntity<ApiResource<HealthCheckDto>> =
        ApiResource.success(healthCheckQueryApplication.getStatus())
}
```

---

## DateTime input/output

> **IMPORTANT**: All datetime inputs must be UTC. KST conversion happens only at the response boundary. See `31_datetime.md` for full rules.

### Input: Ensure UTC

Controllers must receive all `LocalDateTime` and `ZonedDateTime` parameters as UTC. If a client sends KST, convert to UTC immediately before passing to the domain layer.

```kotlin
// Good: UTC input passed directly
@PostMapping
fun create(
    @Valid @RequestBody request: CreateEventApiRequest,
): ResponseEntity<ApiResource<EventDto>> =
    ApiResource.success(eventFacade.create(request.toDomainRequest()))

// Good: KST input converted to UTC in request DTO
data class CreateEventApiRequest(
    val name: String,
    val startAt: LocalDateTime,  // Client sends KST
) {
    fun toDomainRequest() = CreateEventRequest(
        name = name,
        startAt = startAt.toUtc(),  // KST → UTC
    )
}

// Good: ZonedDateTime with explicit timezone normalization
data class CreateEventApiRequest(
    val name: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")
    val startAt: ZonedDateTime,
) {
    fun toDomainRequest() = CreateEventRequest(
        name = name,
        startAt = startAt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
    )
}
```

### Output: KST at response boundary

Convert UTC to KST only in the Facade or Response DTO using `.toKst()`.

```kotlin
import com.myrealtrip.common.utils.extensions.kst

// Convert in Response DTO factory
data class EventDto(
    val id: Long,
    val name: String,
    val startAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(info: EventInfo) = EventDto(
            id = info.id,
            name = info.name,
            startAt = info.startAt.toKst(),      // UTC → KST
            createdAt = info.createdAt.toKst(),   // UTC → KST
        )
    }
}
```

### Incorrect

```kotlin
// Bad: KST leaks into domain — stored as-is in UTC column
@PostMapping
fun create(@RequestBody request: CreateEventApiRequest) {
    eventFacade.create(CreateEventRequest(startAt = request.startAt))  // KST treated as UTC
}
```

---

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Missing `/api/v1/` prefix | Inconsistent API versioning | Always start paths with `/api/v1/` |
| camelCase or snake_case URLs | Violates REST conventions | Use kebab-case for all URL paths |
| Singular resource names | Inconsistent with REST conventions | Use plural nouns |
| Returning raw `ResponseEntity` | Missing standard response format | Always wrap with `ApiResource` |
| Passing many filter params individually | Hard to maintain, cluttered signatures | Use `SearchCondition` object |
| Raw `startDate`/`endDate` without validation | Unbounded queries, potential DB overload | Use `SearchDates` for date range constraints |
| Pageable without default size | Relies on Spring default (20) | Set `Pageable.ofSize(100)` as default |
| Injecting Service directly | Bypasses Facade layer | Inject Facade, or Application for trivially simple cases |
| Using verbs in URLs | Non-RESTful design | Use nouns; actions via HTTP methods or sub-paths |
| Controller methods exceeding 7 lines | Business logic leaking into controller | Extract conversion to DTO's `toDomainRequest()` or Facade |
| KST datetime passed to domain as-is | UTC/KST mixed in DB | Convert KST to UTC at controller/DTO boundary |
| `.toKst()` in Service or domain layer | Display concern leaks into domain | `.toKst()` only in Facade or Response DTO |

## Summary checklist

Before submitting a controller, verify:

- [ ] Base path starts with `/api/v1/`
- [ ] URL paths use kebab-case
- [ ] Resource names are plural nouns
- [ ] All endpoints return `ResponseEntity<ApiResource<T>>`
- [ ] Paginated endpoints use `Pageable` with default `Pageable.ofSize(100)` and return `ApiResource.ofPage()`
- [ ] Complex searches (3+ filters) use a `SearchCondition` object
- [ ] Date range filters use `SearchDates` from the common module
- [ ] Controller methods are 7 lines or fewer
- [ ] Controller injects Facade (or Application for trivially simple pass-through cases)
- [ ] `@Validated` is present when using constraint annotations on parameters
- [ ] Request DTOs use `@Valid @RequestBody`
- [ ] All datetime inputs are UTC (KST inputs converted to UTC in request DTO)
- [ ] KST conversion (`.toKst()`) is only in Facade or Response DTO, not in domain
