---
title: "Layer Architecture"
description: "4-Layer Architecture structure, responsibilities and rules for each layer, dependency injection rules"
category: "architecture"
order: 2
last_updated: "2026-02-14"
---

# Layer Architecture

## Overview

This document describes the 4-Layer Architecture pattern, responsibilities for each layer, and dependency injection rules. Each layer has clear responsibilities, and only upper layers depend on lower layers.

---

## Core Principles

> **Dependency Direction Rule**: Only upper layers depend on lower layers. Do not create reverse dependencies.

```
Bootstrap (Controller → Facade)
    ↓
Domain Application (Query/Command)
    ↓
Domain Service (Business Logic)
    ↓
Domain Repository (JPA/QueryDSL)
    ↓
JPA Entity
```

### Layer Responsibility Summary

| Layer | Module | Responsibility | Transaction |
|-------|------|------|---------|
| **Controller** | Bootstrap | HTTP handling, Facade calls | None |
| **Facade** | Bootstrap | DTO conversion, Application composition | None |
| **Application** | Domain | Transaction boundaries, Service delegation | `@Transactional` |
| **Service** | Domain | Business logic, Repository calls | None |
| **Repository** | Domain | Data access | None |

## Layer 1: Bootstrap (Controller & Facade)

**Module**: `modules/bootstrap/{app-name}/`

### Controller

Controller defines HTTP endpoints and routes requests to Facade.

#### Rules

| Item | Rule |
|------|------|
| Location | `{appname}/api/{Feature}Controller.kt` |
| Dependency Injection | **Facade only** |
| Return Type | `ResponseEntity<ApiResource<T>>` |

#### Implementation Example

```kotlin
@RestController
@RequestMapping("/api/holidays")
class HolidayController(
    private val holidayFacade: HolidayFacade,  // Inject Facade only
) {
    @GetMapping("/{year}")
    fun getByYear(
        @PathVariable year: Int,
        pageable: Pageable,
    ): ResponseEntity<ApiResource<List<HolidayDto>>> {
        return ApiResource.ofPage(holidayFacade.findPageByYear(year, pageable))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateHolidayApiRequest
    ): ResponseEntity<ApiResource<HolidayDto>> {
        // API Request → Domain Request conversion
        val holiday = holidayFacade.create(
            CreateHolidayRequest(request.holidayDate, request.name)
        )
        return ApiResource.success(holiday)
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResource<String>> {
        holidayFacade.delete(id)
        return ApiResource.success()
    }
}
```

### Facade

Facade converts between API DTOs and Domain DTOs.

#### Rules

| Item | Rule |
|------|------|
| Location | `{appname}/facade/{Feature}Facade.kt` |
| Dependency Injection | `QueryApplication`, `CommandApplication` |
| Annotation | `@Component` |

#### Implementation Example

```kotlin
@Component
class HolidayFacade(
    private val holidayQueryApplication: HolidayQueryApplication,
    private val holidayCommandApplication: HolidayCommandApplication,
) {
    // Domain DTO → API DTO
    fun findPageByYear(year: Int, pageable: Pageable): Page<HolidayDto> {
        return holidayQueryApplication.findPageByYear(year, pageable)
            .map { HolidayDto.from(it) }
    }

    fun create(request: CreateHolidayRequest): HolidayDto {
        val holiday = holidayCommandApplication.create(request)
        return HolidayDto.from(holiday)
    }
}
```

## Layer 2: Domain Application

**Module**: `modules/domain/`, **Package**: `domain.{feature}.application`

Application is a thin delegation layer that manages transaction boundaries. Separate Query and Command operations (CQRS-lite).

### QueryApplication (Query)

#### Rules

| Item | Rule |
|------|------|
| Annotation | `@Service`, `@Transactional(readOnly = true)` |
| Dependency Injection | Service only |
| Return Type | `{Feature}Info` or `Page<{Feature}Info>` |

#### Implementation Example

```kotlin
@Service
@Transactional(readOnly = true)  // Slave DB routing
class HolidayQueryApplication(
    private val holidayService: HolidayService,
) {
    fun findPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> =
        holidayService.findPageByYear(year, pageable)

    fun findById(id: Long): HolidayInfo =
        holidayService.findById(id)
}
```

### CommandApplication (Create/Update/Delete)

#### Rules

| Item | Rule |
|------|------|
| Annotation | `@Service`, `@Transactional` |
| Dependency Injection | Service only |

#### Implementation Example

```kotlin
@Service
@Transactional  // Master DB routing
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

## Layer 3: Domain Service

**Module**: `modules/domain/`, **Package**: `domain.{feature}.service`

Service implements core business logic.

### Rules

| Item | Rule |
|------|------|
| Annotation | `@Service` |
| Dependency Injection | Repository only |
| Return Type | `{Feature}Info` |
| Conversion | `{Feature}Info.from(entity)` |

### Implementation Example

```kotlin
@Service
class HolidayService(
    private val holidayJpaRepository: HolidayJpaRepository,
    private val holidayQueryRepository: HolidayQueryRepository,
) {
    fun findPageByYear(year: Int, pageable: Pageable): Page<HolidayInfo> =
        holidayQueryRepository.fetchPageByYear(year, pageable)

    fun findById(id: Long): HolidayInfo =
        holidayJpaRepository.findById(id)
            .map { HolidayInfo.from(it) }  // Entity → Domain DTO
            .orElseThrow { HolidayNotFoundException(id) }

    fun create(request: CreateHolidayRequest): HolidayInfo {
        val entity = Holiday.create(request.holidayDate, request.name)
        return HolidayInfo.from(holidayJpaRepository.save(entity))
    }

    fun update(id: Long, request: UpdateHolidayRequest): HolidayInfo {
        val entity = holidayJpaRepository.findById(id)
            .orElseThrow { HolidayNotFoundException(id) }
        entity.update(request.holidayDate, request.name)
        return HolidayInfo.from(entity)  // JPA dirty checking
    }

    fun delete(id: Long) {
        if (!holidayJpaRepository.existsById(id)) {
            throw HolidayNotFoundException(id)
        }
        holidayJpaRepository.deleteById(id)
    }
}
```

## Layer 4: Domain Repository

**Module**: `modules/domain/`, **Package**: `domain.{feature}.repository`

### JpaRepository

JpaRepository handles simple CRUD, derived queries, and custom `@Query` methods.

```kotlin
@Repository
interface HolidayJpaRepository : JpaRepository<Holiday, Long> {
    fun findByHolidayDate(holidayDate: LocalDate): List<Holiday>

    @Query("""
        select h from Holiday h
         where year(h.holidayDate) = :year
         order by h.holidayDate
    """)
    fun findByYear(year: Int): List<Holiday>
}
```

### QueryRepository

QueryRepository handles dynamic conditions, pagination, and complex joins. **Methods must use `fetch` prefix**.

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

## Entity & Domain DTO

### Entity

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

    @Column(nullable = false, length = 100)
    var name: String = name
        private set

    fun update(holidayDate: LocalDate, name: String) {
        this.holidayDate = holidayDate
        this.name = name
    }

    companion object {
        fun create(holidayDate: LocalDate, name: String) =
            Holiday(holidayDate, name)
    }
}
```

### Domain DTO (Info)

> **IMPORTANT**: Entity must not import DTO classes. Use `{Feature}Info.from(entity)` pattern.

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

data class CreateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)
```

## Dependency Injection Rules

Each layer injects **only the layer immediately below**.

```
Controller → Facade → Application → Service → Repository
```

| Layer | Injection Target | Prohibited |
|-------|-----------|------|
| Controller | Facade only | Service, Application, Repository |
| Facade | Application only | Service, Repository |
| Application | Service only | Repository |
| Service | Repository | - |

## Transaction Rules

| Layer | Transaction | DataSource |
|-------|---------|-----------|
| Controller | None | - |
| Facade | None | - |
| QueryApplication | `@Transactional(readOnly = true)` | **Slave** (Reader) |
| CommandApplication | `@Transactional` | **Master** (Writer) |
| Service | None (propagated) | - |

## Anti-Patterns

| Anti-Pattern | Problem | Correct Method |
|---------|------|-----------|
| Controller calls Service directly | Bypasses Facade | Controller → Facade → Application |
| Facade calls Repository | Skips layers | Facade → Application → Service → Repository |
| Service returns API DTO | Dependency inversion | Service returns Domain DTO only |
| Return Entity as API response | Exposes internal structure | Entity → Info → Dto conversion |
| Business logic in Application | Role confusion | Business logic in Service |
| `@Transactional` on Service | Duplicate management | Manage only at Application |
| `toInfo()` method in Entity | Dependency inversion | Use `{Feature}Info.from(entity)` |

## Checklist

When you add new features:

- [ ] Does Controller inject only Facade?
- [ ] Does Facade inject only Application?
- [ ] Does Application inject only Service?
- [ ] Does `@Transactional` appear only at Application level?
- [ ] Does QueryApplication use `readOnly = true`?
- [ ] Does Entity → Info conversion use `{Feature}Info.from(entity)`?
- [ ] Does Entity avoid importing DTO?

## Summary

**4-Layer Architecture** separates responsibilities across layers:

- **Controller**: HTTP handling, Facade calls
- **Facade**: DTO conversion, Application composition
- **Application**: Transaction boundaries, Service delegation
- **Service**: Business logic, Repository calls
- **Repository**: Data access

**Core Rules**:

1. Each layer depends only on the layer immediately below
2. System manages transactions only at Application level
3. Entity → DTO conversion uses `{Feature}Info.from(entity)` pattern

---

## Related Documents

- [00-overview.md](00-overview.md) - Project overview
- [01-module-dependency.md](01-module-dependency.md) - Module dependency structure
- [../../.claude/rules/92_layer-architecture.md](../../.claude/rules/92_layer-architecture.md) - Detailed layer architecture rules
