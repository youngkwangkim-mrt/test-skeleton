---
name: layer-architecture
description: Layer structure, responsibilities, DTO flow, and conventions based on Holiday feature reference implementation
triggers:
  - layer
  - architecture
  - facade
  - application
  - dto flow
  - cqrs
argument-hint: ""
---

# Layer Architecture

## Overview

This project follows a 4-Layer Architecture.

> **Key Principle**: Upper layers depend on lower layers only. Reverse dependencies are prohibited.

```
Bootstrap (Controller → Facade)
    ↓
Domain Application (QueryApplication / CommandApplication)
    ↓
Domain Service (Business Logic)
    ↓
Domain Repository (JpaRepository / QueryRepository)
    ↓
Domain Entity (JPA Entity)
```

---

## Layer 1: Bootstrap (HTTP entry point)

> **Module**: `modules/bootstrap/{app-name}/`

The entry point for handling HTTP requests and responses. Responsible only for API DTO conversion and routing.

### Controller

| Rule | Description |
|------|-------------|
| Location | `{appname}/api/{Feature}Controller.kt` |
| Responsibility | Define HTTP endpoints, route requests |
| Dependency | Inject Facade only (direct injection of Service, Application, or Repository is prohibited) |
| Return | `ResponseEntity<ApiResource<T>>` |
| Conversion | API Request DTO to Domain Request DTO (simple conversion only) |

```kotlin
@RestController
@RequestMapping("/api/holidays")
class HolidayController(
    private val holidayFacade: HolidayFacade,  // Inject Facade only
) {
    @GetMapping("/{year}")
    fun getByYear(@PathVariable year: Int): ResponseEntity<ApiResource<HolidaysResponse>> =
        ApiResource.success(holidayFacade.findByYear(year))

    @PostMapping
    fun create(@RequestBody request: CreateHolidayApiRequest): ResponseEntity<ApiResource<HolidayDto>> {
        val createRequest = CreateHolidayRequest(request.holidayDate, request.name)
        return ApiResource.success(holidayFacade.create(createRequest))
    }
}
```

### Facade

| Rule | Description |
|------|-------------|
| Location | `{appname}/facade/{Feature}Facade.kt` |
| Responsibility | Convert between API DTO and Domain DTO, orchestrate Application calls |
| Dependency | Inject QueryApplication and CommandApplication |
| Annotation | `@Component` |

```kotlin
@Component
class HolidayFacade(
    private val holidayQueryApplication: HolidayQueryApplication,
    private val holidayCommandApplication: HolidayCommandApplication,
) {
    // Convert Domain DTO to API DTO
    fun findByYear(year: Int): HolidaysResponse {
        val holidays = holidayQueryApplication.findByYear(year)
        return HolidaysResponse.from(holidays)
    }

    fun create(request: CreateHolidayRequest): HolidayDto {
        val holiday = holidayCommandApplication.create(request)
        return HolidayDto.from(holiday)
    }
}
```

### API DTOs

| Rule | Description |
|------|-------------|
| Request location | `{appname}/dto/request/{Feature}ApiRequest.kt` |
| Response location | `{appname}/dto/response/{Feature}ApiResponse.kt` |
| Naming | Request: `{Action}{Feature}ApiRequest`, Response: `{Feature}Dto`, `{Feature}sResponse` |

---

## Layer 2: Domain application (orchestration)

> **Module**: `modules/domain/`
> **Package**: `domain.{feature}.application`

A thin delegation layer that manages transaction boundaries. Separates Query and Command (CQRS-light).

### QueryApplication (query)

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/application/{Feature}QueryApplication.kt` |
| Annotation | `@Service`, `@Transactional(readOnly = true)` (class-level) |
| Dependency | Inject Service only |
| Return | Domain DTO (`{Feature}Info`) |

```kotlin
@Service
@Transactional(readOnly = true)
class HolidayQueryApplication(
    private val holidayService: HolidayService,
) {
    fun findByYear(year: Int): List<HolidayInfo> =
        holidayService.findByYear(year)

    fun findById(id: Long): HolidayInfo =
        holidayService.findById(id)
}
```

### CommandApplication (create/update/delete)

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/application/{Feature}CommandApplication.kt` |
| Annotation | `@Service`, `@Transactional` (class-level) |
| Dependency | Inject Service only |
| Return | Domain DTO (`{Feature}Info`) |

```kotlin
@Service
@Transactional
class HolidayCommandApplication(
    private val holidayService: HolidayService,
) {
    fun create(request: CreateHolidayRequest): HolidayInfo =
        holidayService.create(request)

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo =
        holidayService.update(id, request)

    fun delete(id: Long) =
        holidayService.delete(id)
}
```

---

## Layer 3: Domain service (business logic)

> **Module**: `modules/domain/`
> **Package**: `domain.{feature}.service`

Core business logic. Responsible for Entity manipulation and Domain DTO conversion.

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/service/{Feature}Service.kt` |
| Annotation | `@Service` |
| Dependency | Inject Repository only (JpaRepository, QueryRepository) |
| Return | Domain DTO (`{Feature}Info`) |
| Conversion | Entity to Domain DTO (`{Feature}Info.from(entity)`) |

```kotlin
@Service
class HolidayService(
    private val holidayJpaRepository: HolidayJpaRepository,
) {
    fun findByYear(year: Int): List<HolidayInfo> =
        holidayJpaRepository.findByYear(year).map { HolidayInfo.from(it) }

    fun create(request: CreateHolidayRequest): HolidayInfo {
        val holiday = Holiday.create(request.holidayDate, request.name)
        return HolidayInfo.from(holidayJpaRepository.save(holiday))
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo {
        val holiday = holidayJpaRepository.findById(id)
            .orElseThrow { HolidayNotFoundException(id) }
        holiday.update(request.holidayDate, request.name)
        return HolidayInfo.from(holiday)
    }

    fun delete(id: Long) {
        val holiday = holidayJpaRepository.findById(id)
            .orElseThrow { HolidayNotFoundException(id) }
        holidayJpaRepository.delete(holiday)
    }
}
```

---

## Layer 4: Domain repository (persistence)

> **Module**: `modules/domain/`
> **Package**: `domain.{feature}.repository`

Data access layer. Two approaches: Spring Data JPA and QueryDSL.

### JpaRepository

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/repository/{Feature}JpaRepository.kt` |
| Approach | Spring Data JPA interface |
| Purpose | Simple CRUD, derived queries, custom `@Query` |

```kotlin
interface HolidayJpaRepository : JpaRepository<Holiday, Long> {
    fun findByHolidayDate(holidayDate: LocalDate): List<Holiday>

    @Query("select h from Holiday h where year(h.holidayDate) = :year order by h.holidayDate")
    fun findByYear(@Param("year") year: Int): List<Holiday>
}
```

### QueryRepository

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/repository/{Feature}QueryRepository.kt` |
| Approach | QueryDSL, extends `QuerydslRepositorySupport` |
| Purpose | Dynamic conditions, pagination, complex joins |
| Naming | `fetch` prefix required (see skill: `querydsl`) |

```kotlin
@Repository
class HolidayQueryRepository : QuerydslRepositorySupport(Holiday::class.java) {

    fun fetchPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> {
        return applyPagination(
            pageable,
            contentQuery = { queryFactory ->
                queryFactory.selectFrom(holiday)
                    .where(holiday.holidayDate.year().eq(year))
                    .orderBy(holiday.holidayDate.asc())
            },
            countQuery = { queryFactory ->
                queryFactory.select(holiday.count()).from(holiday)
                    .where(holiday.holidayDate.year().eq(year))
            },
        ).map { HolidayInfo.from(it) }
    }
}
```

---

## Domain entity & DTO

> **Module**: `modules/domain/`

### Entity

> **IMPORTANT**: Entity must not know about DTOs. Entity-to-DTO conversion uses the DTO's `companion object { fun from(entity) }` factory method.

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/entity/{Feature}.kt` |
| Inheritance | `BaseTimeEntity` or `BaseEntity` |
| Immutability | Private setter, mutations via `update()` method |
| Factory | `create()` method in `companion object` |
| Dependency direction | Entity must not import DTO (DTO imports Entity) |

```kotlin
@Entity
@Table(name = "holidays")
class Holiday(
    holidayDate: LocalDate,
    name: String,
    id: Long? = null,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = id

    @Column(nullable = false)
    var holidayDate: LocalDate = holidayDate
        private set

    @Column(nullable = false, length = 50)
    var name: String = name
        private set

    fun update(holidayDate: LocalDate, name: String) {
        this.holidayDate = holidayDate
        this.name = name
    }

    companion object {
        fun create(holidayDate: LocalDate, name: String) = Holiday(holidayDate = holidayDate, name = name)
    }
}
```

### Domain DTOs

> **IMPORTANT**: Domain DTOs convert from Entity using a `companion object { fun from(entity) }` factory method. Dependency direction: DTO depends on Entity (DTO knows Entity).

| Rule | Description |
|------|-------------|
| Location | `domain/{feature}/dto/` |
| Query DTO | `{Feature}Info` -- includes `companion object { fun from(entity) }` factory method |
| Create request | `Create{Feature}Request` |
| Update request | `Update{Feature}Request` |
| Exception | `{Feature}NotFoundException` (extends `KnownException`) |

```kotlin
data class HolidayInfo(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
) {
    companion object {
        fun from(entity: Holiday) = HolidayInfo(
            id = entity.id!!,
            holidayDate = entity.holidayDate,
            name = entity.name,
        )
    }
}

data class CreateHolidayRequest(val holidayDate: LocalDate, val name: String)

data class UpdateHolidayRequest(val holidayDate: LocalDate, val name: String)

class HolidayNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND, "Holiday not found: $id"
)
```

---

## DTO flow (data conversion points)

```
[HTTP Request JSON]
    ↓ deserialize
CreateHolidayApiRequest (Bootstrap: dto/request/)
    ↓ Convert in Controller
CreateHolidayRequest (Domain: dto/)
    ↓ Facade → Application → Service
Holiday Entity (Domain: entity/)
    ↓ HolidayInfo.from(entity)
HolidayInfo (Domain: dto/)
    ↓ Convert in Facade (HolidayDto.from())
HolidayDto (Bootstrap: dto/response/)
    ↓ ApiResource.success() wrapping
[HTTP Response JSON]
```

### DTO Conversion Rules

| Conversion | Where | Method |
|------------|-------|--------|
| API Request to Domain Request | Controller | Direct constructor call |
| Entity to Domain DTO | Service | `{Feature}Info.from(entity)` |
| Domain DTO to API Response | Facade | `ResponseDto.from(domainDto)` |

> **IMPORTANT**: Controller must not handle Entity directly. Facade must not handle Entity directly.

### Dependency direction rule (within domain)

> **IMPORTANT**: Dependencies must be unidirectional. Entity importing DTO is prohibited.

```
Correct (unidirectional): DTO(HolidayInfo) --imports--> Entity(Holiday)
Violation (reversed):     Entity(Holiday)  --imports--> DTO(HolidayInfo)  -- Prohibited!
```

| Pattern | Direction | Allowed |
|---------|-----------|---------|
| `HolidayInfo.from(entity)` | DTO depends on Entity | Yes (correct) |
| `entity.toInfo()` | Entity depends on DTO | No (reversed) |
| `HolidayDto.from(info)` | API DTO depends on Domain DTO | Yes (correct) |

---

## Dependency injection rules

| Layer | Injection Target | Prohibited |
|-------|------------------|------------|
| Controller | Facade only | Direct injection of Service, Application, or Repository |
| Facade | QueryApplication, CommandApplication | Direct injection of Service or Repository |
| Application | Service only | Direct injection of Repository |
| Service | JpaRepository, QueryRepository | Other Service injection is allowed (same layer) |

```
Controller → Facade → Application → Service → Repository
    (Each layer injects only the layer directly below it)
```

---

## Transaction rules

| Layer | Transaction | Reason |
|-------|-------------|--------|
| Controller | None | HTTP layer, does not manage transactions |
| Facade | None | DTO conversion only, no transaction needed |
| QueryApplication | `@Transactional(readOnly = true)` | Read-only optimization |
| CommandApplication | `@Transactional` | Write transaction |
| Service | None (propagated from Application) | Prevents duplicate transactions |

---

## Cross-domain orchestration

### Facade: multi-domain composition (separate transactions)

Facade can call multiple domain Applications to compose results. Since Facade has no `@Transactional`, each Application call runs in its own transaction.

```kotlin
// Good: Compose multiple domain Applications (each in a separate transaction)
@Component
class BookingFacade(
    private val bookingCommandApplication: BookingCommandApplication,
    private val holidayQueryApplication: HolidayQueryApplication,
    private val userQueryApplication: UserQueryApplication,
) {
    fun create(request: CreateBookingApiRequest): BookingDto {
        val user = userQueryApplication.findById(request.userId)        // Transaction 1
        val holidays = holidayQueryApplication.findByYear(request.year) // Transaction 2
        val booking = bookingCommandApplication.create(request.toDomainRequest()) // Transaction 3
        return BookingDto.from(booking, user, holidays)
    }
}
```

| Allowed | Not Allowed |
|---------|-------------|
| Call multiple domain Applications | Business logic (validation, calculation) |
| API DTO ↔ Domain DTO conversion | Direct Repository access |
| Response data assembly | Direct Service injection |

> **Note**: Each Application call from Facade runs in a separate transaction. This is suitable for independent read compositions where atomicity is not required.

### Cross-domain Application: single transaction across domains

When multiple domain write operations must be atomic, create a cross-domain Application that injects Services from different domains.

```kotlin
// Good: Multiple domain Services in a single transaction
@Service
@Transactional
class BookingCommandApplication(
    private val bookingService: BookingService,
    private val paymentService: PaymentService,
    private val inventoryService: InventoryService,
) {
    fun createWithPayment(request: CreateBookingRequest): BookingInfo {
        val booking = bookingService.create(request)
        paymentService.charge(booking.id, request.amount)
        inventoryService.decreaseStock(request.productId)
        return booking
        // If any step fails, the entire transaction rolls back
    }
}
```

| Approach | Transaction | Use Case |
|----------|-------------|----------|
| Facade → multiple Applications | Separate per call | Independent read compositions |
| Application → multiple Services | Single (atomic) | Write operations requiring atomicity |

> **IMPORTANT**: Application can only inject Services. Inject Services from other domains directly to execute within a single `@Transactional`. Injecting another Application from an Application is prohibited.

---

## Anti-patterns

| Anti-Pattern | Problem | Correct |
|--------------|---------|---------|
| Calling Service directly from Controller | Bypasses Facade, misses DTO conversion | Controller → Facade → Application |
| Calling Repository from Facade | Skips layers | Facade → Application → Service → Repository |
| Returning API DTO from Service | Reverses dependency on Bootstrap | Service returns Domain DTO only |
| Returning Entity directly as API response | Exposes internal structure | Convert Entity → Info → Dto |
| Implementing business logic in Application | Application delegates only | Business logic belongs in Service |
| Adding `@Transactional` to Service | Duplicates with Application | Manage transactions in Application only |
| Defining `toInfo()` method on Entity | Entity depends on DTO (dependency reversal) | Use `{Feature}Info.from(entity)` factory |
| Adding `@Transactional` to Facade for atomicity | Facade is not a transaction boundary, race conditions possible | Create a cross-domain Application with multiple Services |
| Application injecting another Application | Tangled transaction boundaries, circular risk | Application injects Services only |
| Putting business validation in Facade | No transaction protection, domain logic leaks to Bootstrap | Validation belongs in Service |

---

## Summary checklist

Verify when adding a new feature:

- [ ] Controller injects Facade only
- [ ] Facade injects Application only
- [ ] Application injects Service only
- [ ] `@Transactional` exists at the Application level only
- [ ] QueryApplication uses `readOnly = true`
- [ ] Entity-to-Info conversion uses `{Feature}Info.from(entity)` pattern
- [ ] Entity class has no DTO imports
- [ ] Info-to-API DTO conversion is performed in Facade
- [ ] API Request-to-Domain Request conversion is performed in Controller
- [ ] Domain DTOs do not depend on Bootstrap module
