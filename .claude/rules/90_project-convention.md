---
name: Project Convention
description: Project conventions for API response format (ApiResource), date/time format, and JsonFormat usage
---

# Project Convention

## API Response

> **IMPORTANT**: All APIs except `GlobalController` and `HomeController` must respond with `ApiResource` format.

### Correct Examples

```kotlin
@GetMapping("/{id}")
fun getUser(@PathVariable id: Long): ResponseEntity<ApiResource<UserDto>> =
    ApiResource.success(userService.findById(id))

@PostMapping
fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<ApiResource<UserDto>> =
    ApiResource.success(userService.create(request))

@DeleteMapping("/{id}")
fun deleteUser(@PathVariable id: Long): ResponseEntity<ApiResource<String>> {
    userService.delete(id)
    return ApiResource.success()
}

@GetMapping
fun getUsers(pageable: Pageable): ResponseEntity<ApiResource<List<UserDto>>> =
    ApiResource.ofPage(userService.findAll(pageable))
```

### Incorrect Examples

```kotlin
// Bad: Not using ApiResource
@GetMapping("/{id}")
fun getUser(@PathVariable id: Long): UserDto = userService.findById(id)

// Bad: Using only ResponseEntity
@GetMapping("/{id}")
fun getUser(@PathVariable id: Long): ResponseEntity<UserDto> =
    ResponseEntity.ok(userService.findById(id))
```

## DTO Package Convention

> **IMPORTANT**: Bootstrap 모듈의 API DTO는 `dto/request/`와 `dto/response/` 패키지로 분리합니다.

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

## Default Timezone

> **IMPORTANT**: Bootstrap 앱은 기본 타임존으로 **UTC**를 사용합니다.

모든 Bootstrap `-app` 모듈의 `main()` 함수에서 `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))`를 설정합니다.

```kotlin
fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<MyApplication>(*args)
}
```

| 항목 | 규칙 |
|------|------|
| JVM 기본 타임존 | `UTC` |
| `LocalDateTime.now()` | UTC 기준으로 동작 |
| DB 저장 | UTC 기준 |
| KST 변환 | API 응답 시 필요한 경우에만 명시적으로 변환 |

### KST 변환

KST 변환이 필요한 경우 `DateTimeExtensions`의 `.kst()` 확장 함수를 사용합니다.

| 타입 | 메서드 | 반환 타입 |
|------|--------|-----------|
| `LocalDateTime` | `.kst()` | `LocalDateTime` |
| `ZonedDateTime` | `.kst()` | `ZonedDateTime` |

### Correct Examples

```kotlin
import com.myrealtrip.common.utils.extensions.kst

// UTC 기준 현재 시간 (JVM 기본 타임존이 UTC이므로 별도 Clock 불필요)
val now = LocalDateTime.now()

// KST 변환이 필요한 경우 .kst() 확장 함수 사용
val kstNow = now.kst()
```

### Incorrect Examples

```kotlin
// Bad: Bootstrap main()에서 타임존 설정 누락
fun main(args: Array<String>) {
    runApplication<MyApplication>(*args)  // JVM 기본 타임존(KST) 사용됨
}

// Bad: 직접 ZoneId를 사용한 변환
val kstNow = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))

// Bad: 직접 시간 더하기
val kstNow = LocalDateTime.now().plusHours(9)
```

## Date and Time Format

> **IMPORTANT**: Use ISO-8601 format for all date and time representations.

### Standard Formats

| Type | Format | Example |
|------|--------|---------|
| Date | `yyyy-MM-dd` | `2025-01-02` |
| Time | `HH:mm:ss` or `HH:mm:ss.SSS` | `14:30:00` or `14:30:00.123` |
| DateTime | `yyyy-MM-dd'T'HH:mm:ss` or `yyyy-MM-dd'T'HH:mm:ss.SSS` | `2025-01-02T14:30:00` or `2025-01-02T14:30:00.123` |
| DateTime with timezone (ZonedDateTime) | `yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'` or `yyyy-MM-dd'T'HH:mm:ss.SSSXXX'['VV']'` | `2025-01-02T14:30:00+09:00[Asia/Seoul]` or `2025-01-02T14:30:00.123+09:00[Asia/Seoul]` |
| DateTime UTC | `yyyy-MM-dd'T'HH:mm:ss'Z'` or `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` | `2025-01-02T05:30:00Z` or `2025-01-02T05:30:00.123Z` |

> **NOTE**: `.SSS` (milliseconds) is optional. Include when precision is required.

### Correct Examples

```kotlin
// API Response DTO
data class EventDto(
    val id: Long,
    val name: String,
    val startDate: LocalDate,        // Serialized as: "2025-01-02"
    val startTime: LocalTime,        // Serialized as: "14:30:00"
    val createdAt: LocalDateTime,    // Serialized as: "2025-01-02T14:30:00"
    val scheduledAt: ZonedDateTime   // Serialized as: "2025-01-02T14:30:00+09:00[Asia/Seoul]"
)

// Request parameter
@GetMapping("/events")
fun getEvents(
    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
): ResponseEntity<ApiResource<List<EventDto>>>
```

### Incorrect Examples

```kotlin
// Bad: Not using ISO-8601 format
data class EventDto(
    val date: String  // "01/02/2025" or "2025/01/02"
)

// Bad: Non-standard custom format
@GetMapping("/events")
fun getEvents(
    @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") date: LocalDate  // "20250102"
): ResponseEntity<ApiResource<List<EventDto>>>
```

### JsonFormat Annotation Usage

Use `@JsonFormat` with appropriate use-site targets for JSON serialization/deserialization.

| Annotation Target | Use Case | Description |
|-------------------|----------|-------------|
| `@param:JsonFormat` | Request (Deserialization) | Applied to constructor parameters for parsing incoming JSON |
| `@get:JsonFormat` | Response (Serialization) | Applied to getter for formatting outgoing JSON |
| `@field:JsonFormat` | Both directions | Applied to field for both request and response |

#### Correct Examples

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

#### Incorrect Examples

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
