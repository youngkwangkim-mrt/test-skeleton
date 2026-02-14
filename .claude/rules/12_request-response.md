---
name: Request & Response
description: DTO package structure (request/response separation) and JsonFormat annotation use-site targets
last-verified: 2026-02-14
---

# Request & response conventions

## Overview

This document defines conventions for API request/response DTOs: package structure, naming, and `@JsonFormat` annotation usage.

> **Key Principle**: Separate API DTOs into `dto/request/` and `dto/response/` packages. Use correct `@JsonFormat` use-site targets.

## Never return entities

> **IMPORTANT**: Never return JPA entities directly as API responses. Always convert to a response DTO. Exposing entities leaks internal structure, bypasses timezone conversion, and couples the API contract to the database schema.

```kotlin
// Bad: Returning entity directly
@GetMapping("/{id}")
fun getById(@PathVariable id: Long): ResponseEntity<ApiResource<Holiday>> =
    ApiResource.success(holidayService.findById(id))

// Good: Return response DTO
@GetMapping("/{id}")
fun getById(@PathVariable id: Long): ResponseEntity<ApiResource<HolidayDto>> =
    ApiResource.success(holidayFacade.findById(id))
```

> **Note**: The full conversion flow is `Entity → {Feature}Info (domain DTO) → {Feature}Dto (API response DTO)`. See `92_layer-architecture.md` for details.

## DTO package convention

> **IMPORTANT**: API DTOs in bootstrap modules must be separated into `dto/request/` and `dto/response/` packages.

### Package Structure

```
com.myrealtrip.{appname}/
├── api/
│   └── {Feature}Controller.kt
├── dto/
│   ├── request/
│   │   └── {Feature}ApiRequest.kt     # API request DTOs
│   └── response/
│       └── {Feature}ApiResponse.kt    # API response DTOs
└── facade/
    └── {Feature}Facade.kt
```

### Naming Convention

| Type | Naming | Package | Example |
|------|--------|---------|---------|
| API Request | `{Action}{Feature}ApiRequest` | `dto/request/` | `CreateHolidayApiRequest` |
| API Response | `{Feature}Dto`, `{Feature}sResponse` | `dto/response/` | `HolidayDto`, `HolidaysResponse` |
| Domain DTO | `{Feature}Info` | domain `dto/` | `HolidayInfo` |
| Domain Request | `Create{Feature}Request` | domain `dto/` | `CreateHolidayRequest` |

### Correct Examples

```kotlin
// dto/request/HolidayApiRequest.kt
package com.myrealtrip.commonapiapp.dto.request

data class CreateHolidayApiRequest(
    val holidayDate: LocalDate,
    val name: String,
)

// dto/response/HolidayApiResponse.kt
package com.myrealtrip.commonapiapp.dto.response

data class HolidayDto(
    val id: Long,
    val holidayDate: LocalDate,
    val name: String,
)
```

### Incorrect Examples

```kotlin
// Bad: request/response in the same file
data class HolidayDto(...)
data class CreateHolidayRequest(...)

// Bad: DTOs inside api/ package
package com.myrealtrip.commonapiapp.api.dto
```

## JsonFormat annotation usage

Use `@JsonFormat` with appropriate use-site targets for JSON serialization/deserialization.

| Annotation Target | Use Case | Description |
|-------------------|----------|-------------|
| `@param:JsonFormat` | Request (Deserialization) | Applied to constructor parameters for parsing incoming JSON |
| `@get:JsonFormat` | Response (Serialization) | Applied to getter for formatting outgoing JSON |
| `@field:JsonFormat` | Both directions | Applied to field for both request and response |

### Correct Examples

```kotlin
// Response only - use @get:JsonFormat
data class EventResponse(
    val id: Long,
    @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")
    val scheduledAt: ZonedDateTime
)

// Request only - use @param:JsonFormat
data class CreateEventRequest(
    val name: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val startAt: LocalDateTime,
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")
    val scheduledAt: ZonedDateTime
)

// Both directions - use @field:JsonFormat
data class EventDto(
    val id: Long,
    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")
    val scheduledAt: ZonedDateTime
)
```

### Incorrect Examples

```kotlin
// Bad: Using @param for response serialization (won't work)
data class EventResponse(
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime  // Output format not applied
)

// Bad: Using @get for request deserialization (won't work)
data class CreateEventRequest(
    @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val startAt: LocalDateTime  // Input parsing not applied
)
```
