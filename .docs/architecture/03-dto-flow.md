---
title: "DTO Flow and Conversion"
description: "DTO types, conversion points, dependency direction rules, request/response lifecycle"
category: "architecture"
order: 3
last_updated: "2026-02-14"
---

# DTO Flow and Conversion

## Overview

This document explains how data transforms from HTTP request to HTTP response. You will learn about DTO (Data Transfer Object) types, their roles in each layer, and the rules for converting between them.

## What Is a DTO?

A DTO (Data Transfer Object) transfers data between layers. DTOs separate concerns across layers and prevent exposing internal implementation details (Entity) to external clients.

## DTO Types

### 1. API Request DTO

**Location**: `modules/bootstrap/{app-name}/dto/request/`
**Naming**: `{Action}{Feature}ApiRequest`

The API Request DTO receives the HTTP request body. Jackson deserializes JSON into this DTO and validates its fields.

```kotlin
package com.myrealtrip.commonapiapp.dto.request

data class CreateHolidayApiRequest(
    @field:NotNull
    val holidayDate: LocalDate,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)
```

### 2. API Response DTO

**Location**: `modules/bootstrap/{app-name}/dto/response/`
**Naming**: `{Feature}Dto`, `{Feature}sResponse`

The API Response DTO sends data as the HTTP response body. Jackson serializes this DTO into JSON.

```kotlin
package com.myrealtrip.commonapiapp.dto.response

data class HolidayDto(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
) {
    companion object {
        fun from(holiday: HolidayInfo) = HolidayDto(
            id = holiday.id,
            holidayDate = holiday.holidayDate,
            name = holiday.name,
        )
    }
}

data class HolidaysResponse(
    val holidays: List<HolidayItem>,
) {
    companion object {
        fun from(holidays: List<HolidayInfo>) = HolidaysResponse(
            holidays = holidays.map { HolidayItem(it.holidayDate, it.name) }
        )
    }
}
```

### 3. Domain DTO (Info)

**Location**: `modules/domain/.../dto/`
**Naming**: `{Feature}Info`

The Domain DTO (Info) represents read-only data used internally within the domain layer. The Service layer converts Entity to this DTO.

```kotlin
package com.myrealtrip.domain.holiday.dto

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
```

### 4. Domain Request DTO

**Location**: `modules/domain/.../dto/`
**Naming**: `Create{Feature}Request`, `Update{Feature}Request`

The Domain Request DTO contains data needed for create and update operations in the domain layer.

```kotlin
data class CreateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)

data class UpdateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)
```

### 5. Entity

**Location**: `modules/domain/.../entity/`

The JPA Entity maps to database tables and manages persistence state.

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

## DTO Dependency Direction Rules

### Core Principle

> **IMPORTANT**: Dependencies must be **unidirectional**. Entity classes must not import DTO classes.

```
Correct (unidirectional): DTO(HolidayInfo) --imports--> Entity(Holiday)
Violation (reversed):     Entity(Holiday)  --imports--> DTO(HolidayInfo)  ← Prohibited!
```

### Allowed vs Prohibited Patterns

| Pattern | Direction | Allowed | Reason |
|---------|-----------|---------|--------|
| `HolidayInfo.from(entity)` | DTO → Entity | ✅ | DTO knows Entity |
| `entity.toInfo()` | Entity → DTO | ❌ | Entity knows DTO |
| `HolidayDto.from(info)` | API DTO → Domain DTO | ✅ | Upper layer knows lower layer |

### Correct Conversion

```kotlin
// ✅ Service: Entity → Domain DTO
fun findById(id: Long): HolidayInfo {
    return holidayJpaRepository.findById(id)
        .map { HolidayInfo.from(it) }  // DTO factory method
        .orElseThrow { HolidayNotFoundException(id) }
}

// ✅ Facade: Domain DTO → API Response DTO
fun findById(id: Long): HolidayDto {
    val holiday = holidayQueryApplication.findById(id)
    return HolidayDto.from(holiday)
}
```

### Prohibited Conversion

```kotlin
// ❌ Defining toInfo() method on Entity
@Entity
class Holiday {
    fun toInfo() = HolidayInfo(...)  // ❌ Entity imports DTO
}

// ❌ Using toInfo() method in Service
fun findById(id: Long): HolidayInfo {
    return holidayJpaRepository.findById(id)
        .map { it.toInfo() }  // ❌ Entity → DTO dependency
        .orElseThrow { HolidayNotFoundException(id) }
}
```

## Request/Response Flow

### Query Request (GET /api/holidays/{year})

```
1. HTTP Request
   GET /api/holidays/2025
      ↓
2. Controller
   holidayFacade.findPageByYear(year, pageable)
      ↓
3. Facade
   holidayQueryApplication.findPageByYear(year, pageable)
      ↓
4. Application (transaction starts)
   @Transactional(readOnly = true)
   holidayService.findPageByYear(year, pageable)
      ↓
5. Service
   holidayQueryRepository.fetchPageByYear(year, pageable)
      ↓
6. Repository
   SELECT * FROM holidays WHERE year(holiday_date) = 2025
      ↓
   Page<Holiday>
      ↓
   .map { HolidayInfo.from(it) }  // Entity → Domain DTO
      ↓
   Page<HolidayInfo>
      ↓
7. Service → Application → Facade
   .map { HolidayDto.from(it) }  // Domain DTO → API DTO
      ↓
   Page<HolidayDto>
      ↓
8. Controller
   ApiResource.ofPage(page)
      ↓
9. HTTP Response JSON
```

### Create Request (POST /api/holidays)

```
1. HTTP Request JSON
   { "holidayDate": "2025-12-25", "name": "Christmas" }
      ↓ JSON deserialization
   CreateHolidayApiRequest
      ↓
2. Controller
   CreateHolidayRequest(api.holidayDate, api.name)  // API → Domain
      ↓
   holidayFacade.create(domainRequest)
      ↓
3. Facade → Application (transaction starts) → Service
      ↓
4. Service
   val entity = Holiday.create(request.holidayDate, request.name)
   val saved = holidayJpaRepository.save(entity)
      ↓
   HolidayInfo.from(saved)  // Entity → Domain DTO
      ↓
   HolidayInfo
      ↓
5. Service → Application → Facade
   HolidayDto.from(holidayInfo)  // Domain DTO → API DTO
      ↓
   HolidayDto
      ↓
6. Controller
   ApiResource.success(holidayDto)
      ↓
7. HTTP Response JSON
```

### Update Request (PUT /api/holidays/{id})

```
1. HTTP Request
   PUT /api/holidays/42
   { "holidayDate": "2025-12-25", "name": "Christmas Day" }
      ↓
2. Controller → Facade → Application → Service
      ↓
3. Service
   val entity = holidayJpaRepository.findById(id)
       .orElseThrow { HolidayNotFoundException(id) }
   entity.update(request.holidayDate, request.name)
      ↓ JPA Dirty Checking
   HolidayInfo.from(entity)
      ↓
4. Service → Application (transaction commit) → Facade
   HolidayDto.from(info)
      ↓
5. Controller → HTTP Response
```

### Delete Request (DELETE /api/holidays/{id})

```
1. HTTP Request
   DELETE /api/holidays/42
      ↓
2. Controller → Facade → Application → Service
      ↓
3. Service
   if (!holidayJpaRepository.existsById(id)) {
       throw HolidayNotFoundException(id)
   }
   holidayJpaRepository.deleteById(id)
      ↓
4. Service → Application (commit) → Facade → Controller
   ApiResource.success()
      ↓
5. HTTP Response
   { "status": {...}, "data": "success" }
```

## Conversion Point Summary

### Where DTO Conversions Happen

| Conversion | Location | Method | Example |
|------------|----------|--------|---------|
| API Request → Domain Request | Controller | Constructor | `CreateHolidayRequest(api.holidayDate, api.name)` |
| Entity → Domain DTO | Service | `{Feature}Info.from(entity)` | `HolidayInfo.from(entity)` |
| Domain DTO → API Response | Facade | `{Feature}Dto.from(domainDto)` | `HolidayDto.from(info)` |

### Conversion Responsibilities

| Layer | Input | Output | Conversion Responsibility |
|-------|-------|--------|---------------------------|
| Controller | API Request DTO | - | API Request → Domain Request |
| Facade | Domain DTO | API Response DTO | Domain DTO → API Response |
| Application | Domain Request | Domain DTO | None (delegates to Service) |
| Service | Domain Request | Domain DTO | Entity → Domain DTO |
| Repository | Conditions | Entity or Domain DTO | Entity → Domain DTO (QueryRepository) |

## Package Structure

### Bootstrap Module

```
com.myrealtrip.{appname}/
├── api/
│   └── HolidayController.kt
├── dto/
│   ├── request/
│   │   └── CreateHolidayApiRequest.kt
│   └── response/
│       └── HolidayApiResponse.kt
└── facade/
    └── HolidayFacade.kt
```

### Domain Module

```
com.myrealtrip.domain.holiday/
├── dto/
│   ├── HolidayDto.kt                 # HolidayInfo
│   ├── CreateHolidayRequest.kt
│   └── HolidayNotFoundException.kt
├── entity/
│   └── Holiday.kt
├── repository/
│   ├── HolidayJpaRepository.kt
│   └── HolidayQueryRepository.kt
├── service/
│   └── HolidayService.kt
└── application/
    ├── HolidayQueryApplication.kt
    └── HolidayCommandApplication.kt
```

## DTO Design Guidelines

### API Request DTO

```kotlin
// ✅ Include validation annotations
data class CreateHolidayApiRequest(
    @field:NotNull
    val holidayDate: LocalDate,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)
```

**Include:**
- Validation annotations
- JSON deserialization fields

**Exclude:**
- ID fields (on create)
- Business logic
- Entity references

### API Response DTO

```kotlin
// ✅ Provide from factory method
data class HolidayDto(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
) {
    companion object {
        fun from(holiday: HolidayInfo) = HolidayDto(...)
    }
}
```

**Include:**
- `companion object { fun from(domainDto) }`
- Client-facing fields

**Exclude:**
- Full Entity exposure
- Circular reference fields
- Sensitive data

### Domain DTO (Info)

```kotlin
// ✅ Immutable, with from factory method
data class HolidayInfo(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
) {
    companion object {
        fun from(entity: Holiday) = HolidayInfo(...)
    }
}
```

**Include:**
- `companion object { fun from(entity) }`
- Immutable fields (`val`)

**Exclude:**
- JPA annotations
- Mutable fields (`var`)
- Entity references

### Domain Request DTO

```kotlin
// ✅ Simple data holder
data class CreateHolidayRequest(
    val holidayDate: LocalDate,
    val name: String,
)
```

**Include:**
- Only fields needed for create or update

**Exclude:**
- Validation annotations (validated at API layer)
- ID fields
- Timestamp fields

## Checklist

### Controller
- [ ] Converts API Request DTO to Domain Request DTO
- [ ] Does not handle Entity directly

### Facade
- [ ] Converts Domain DTO to API Response DTO
- [ ] Uses `{Feature}Dto.from(domainDto)` pattern

### Service
- [ ] Converts Entity to Domain DTO
- [ ] Uses `{Feature}Info.from(entity)` pattern
- [ ] Does not import API DTOs

### Entity
- [ ] Does not import DTOs
- [ ] Does not have conversion methods like `toInfo()`

### DTO Classes
- [ ] API Response DTOs provide `companion object { fun from() }`
- [ ] Domain DTOs provide `companion object { fun from(entity) }`
- [ ] DTO dependency direction follows upper → lower

## Summary

**Core Principles of DTO Flow**

1. **Dependency Direction**: DTO imports Entity. Entity does not import DTO.
2. **Conversion Points**:
   - Controller: API Request → Domain Request
   - Service: Entity → Domain DTO (`{Feature}Info.from(entity)`)
   - Facade: Domain DTO → API Response (`{Feature}Dto.from(domainDto)`)
3. **Factory Methods**: Use `companion object { fun from() }` pattern
4. **Layer Separation**: Each layer knows only its own DTOs and does not import upper layer DTOs

Follow these rules to reduce coupling between layers and improve maintainability and testability.

## Related Documents

- [Layer Architecture](92_layer-architecture.md)
- [Persistence Patterns (JPA & QueryDSL)](05-persistence-patterns.md)
