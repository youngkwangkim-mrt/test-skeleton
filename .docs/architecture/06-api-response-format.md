---
title: "API Response Format"
description: "ApiResource standard response format, ResponseCode system, pagination, date/time format rules"
category: "architecture"
order: 6
last_updated: "2026-02-14"
---

# API Response Format

## Overview

This document describes the `ApiResource<T>` wrapper format that all API responses use in this project. The format provides consistent structure, standardized error handling, and automatic metadata inclusion for every API response.

## ApiResource Structure

### Basic Format

The following example shows the complete structure of an `ApiResource` response:

```json
{
  "status": {
    "status": 200,
    "code": "SUCCESS",
    "message": "Success"
  },
  "meta": {
    "x-b3-traceid": "507f1f77bcf86cd799439011",
    "appTraceId": "01932f7a-3b45-7000-8000-0242ac120002",
    "responseTs": 1707901234567,
    "size": 1
  },
  "data": {
    "id": 1,
    "name": "John Doe"
  }
}
```

### Components

#### 1. Status

The `status` object contains the HTTP status code and a human-readable response code.

```kotlin
data class Status(
    val status: Int,      // HTTP status code
    val code: String,     // Response code (Enum name)
    val message: String   // Response message
)
```

| Field | Type | Description | Example |
|------|------|------|------|
| `status` | Int | HTTP status code | `200`, `400`, `500` |
| `code` | String | Response code name | `SUCCESS`, `INVALID_ARGUMENT` |
| `message` | String | User-friendly message | `"Success"`, `"Invalid request argument"` |

#### 2. Meta (Metadata)

The `meta` object provides tracing, timing, and pagination metadata.

```kotlin
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Meta(
    val traceId: String = MdcUtil.getTraceId(),
    val appTraceId: String = MdcUtil.getAppTraceId(),
    val responseTs: Long = System.currentTimeMillis(),
    val size: Int? = null,
    val pageInfo: PageInfo? = null,
    val offsetInfo: OffsetInfo? = null,
)
```

| Field | Type | Required | Description |
|------|------|------|------|
| `traceId` | String | Yes | Zipkin B3 Trace ID (distributed tracing) |
| `appTraceId` | String | Yes | UUID v7-based application Trace ID |
| `responseTs` | Long | Yes | Response timestamp (Unix milliseconds) |
| `size` | Int? | No | Element count for collections |
| `pageInfo` | PageInfo? | No | Page information (offset-based pagination) |
| `offsetInfo` | OffsetInfo? | No | Offset information (cursor-based pagination) |

#### 3. Data (Payload)

The `data` field contains the actual response payload. The generic type `T` supports any response type.

```kotlin
data class ApiResource<T>(
    val status: Status,
    val meta: Meta,
    val data: T
)
```

## ResponseCode System

### ResponseCode Interface

The `ResponseCode` interface defines the contract for all success and error codes.

```kotlin
interface ResponseCode {
    val status: Int
    val message: String
    val name: String

    fun isSuccess(): Boolean = status in 200..299
    fun isClientError(): Boolean = status in 400..499
    fun isServerError(): Boolean = status in 500..599
    fun isError(): Boolean = isClientError() || isServerError()
    fun isBusinessError(): Boolean = status == 406
}
```

### SuccessCode

The following enum defines standard success response codes:

```kotlin
enum class SuccessCode(
    override val status: Int,
    override val message: String,
) : ResponseCode {
    SUCCESS(200, "Success"),
    CREATED(201, "Created successfully"),
    ACCEPTED(202, "Accepted successfully"),
}
```

### ErrorCode

The following enum defines standard error response codes:

```kotlin
enum class ErrorCode(
    override val status: Int,
    override val message: String,
) : ResponseCode {
    // Authentication/Authorization
    UNAUTHORIZED(401, "Authentication required"),
    UNAUTHORIZED_IP(401, "Unauthorized IP address"),
    FORBIDDEN(403, "Access forbidden"),
    NOT_FOUND(404, "Requested resource not found"),

    // Request validation
    INVALID_ARGUMENT(400, "Invalid request argument"),
    NOT_READABLE(400, "Invalid request message"),

    // Business errors (406)
    ILLEGAL_ARGUMENT(406, "Invalid argument"),
    ILLEGAL_STATE(406, "Invalid state"),
    DATA_NOT_FOUND(406, "Requested data not found"),
    UNSUPPORTED_OPERATION(406, "Unsupported operation"),
    DB_ACCESS_ERROR(406, "Database access error"),
    CALL_RESPONSE_ERROR(406, "Invalid state"),

    // Server errors
    SERVER_ERROR(500, "Temporary error. Please try again later"),
}
```

**Error Code Classification:**

| HTTP Status | Category | Description |
|-----------|-----------|------|
| 400 | Request validation | Client request format or parameter error |
| 401 | Authentication | Missing or expired authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not found | Endpoint or resource not found |
| 406 | Business error | Expected business logic error |
| 500 | Server error | Unexpected server error |

**Note:** This project uses HTTP 406 for business errors as a project convention.

## ApiResource Creation Methods

The `ApiResource` companion object provides the following factory methods:

```kotlin
companion object {
    fun success(): ResponseEntity<ApiResource<String>>
    fun <T> success(data: T): ResponseEntity<ApiResource<T>>
    fun error(): ResponseEntity<ApiResource<String>>
    fun <T> error(data: T): ResponseEntity<ApiResource<T>>
    fun <T> of(code: ResponseCode, data: T): ResponseEntity<ApiResource<T>>
}
```

### Automatic Type Wrapping

The `of` method automatically detects the data type and applies appropriate wrapping:

```kotlin
when (data) {
    is Collection<*> -> ofCollection(data, code)
    is Map<*, *> -> ofMap(data, code)
    is Page<*> -> ofPage(data, code)
    else -> createResource(code, data).toResponseEntity()
}
```

## Usage Patterns

### 1. Single Object Response

Use `ApiResource.success()` for single object responses:

```kotlin
@GetMapping("/{id}")
fun findById(@PathVariable id: Long): ResponseEntity<ApiResource<UserDto>> =
    ApiResource.success(userService.findById(id))
```

### 2. List Response

Use `ApiResource.success()` for list responses. The response automatically includes `meta.size` with the element count.

```kotlin
@GetMapping
fun findAll(): ResponseEntity<ApiResource<List<UserDto>>> =
    ApiResource.success(userService.findAll())
```

### 3. Paginated Response (Offset-based)

Use `ApiResource.ofPage()` for offset-based paginated responses:

```kotlin
@GetMapping
fun findAll(pageable: Pageable): ResponseEntity<ApiResource<List<UserDto>>> =
    ApiResource.ofPage(userService.findAll(pageable))
```

**Response Example:**

```json
{
  "status": {"status": 200, "code": "SUCCESS", "message": "Success"},
  "meta": {
    "x-b3-traceid": "507f1f77bcf86cd799439011",
    "appTraceId": "01932f7a-3b45-7000-8000-0242ac120002",
    "responseTs": 1707901234567,
    "pageInfo": {
      "totalPages": 10,
      "totalElements": 95,
      "pageNumber": 0,
      "pageElements": 10,
      "isFirst": true,
      "isLast": false,
      "isEmpty": false
    }
  },
  "data": [...]
}
```

**PageInfo Fields:**

| Field | Type | Description |
|------|------|------|
| `totalPages` | Int | Total number of pages |
| `totalElements` | Long | Total number of elements |
| `pageNumber` | Int | Current page (0-indexed) |
| `pageElements` | Int | Elements in current page |
| `isFirst` | Boolean | Indicates whether this page is the first page |
| `isLast` | Boolean | Indicates whether this page is the last page |
| `isEmpty` | Boolean | Indicates whether this page is empty |

### 4. Cursor-based Pagination

Use `ApiResource.ofNoOffsetPage()` for cursor-based pagination:

```kotlin
@GetMapping("/cursor")
fun findByCursor(
    @RequestParam lastIndex: String?,
    pageable: Pageable
): ResponseEntity<ApiResource<List<UserDto>>> {
    val page = userService.findByCursor(lastIndex, pageable)
    val newLastIndex = page.content.lastOrNull()?.id?.toString() ?: "0"
    return ApiResource.ofNoOffsetPage(page, newLastIndex)
}
```

**OffsetInfo Fields:**

| Field | Type | Description |
|------|------|------|
| `lastIndex` | String | Cursor value for next request |
| `totalPages` | Int | Total pages |
| `totalElements` | Long | Total elements |
| `pageNumber` | Int | Current page |
| `pageElements` | Int | Elements in page |
| `isLast` | Boolean | Indicates whether this page is the last page |
| `isEmpty` | Boolean | Indicates whether this page is empty |

### 5. Creation Response

Use `ApiResource.of()` with `SuccessCode.CREATED` for resource creation:

```kotlin
@PostMapping
fun create(@RequestBody request: CreateUserRequest): ResponseEntity<ApiResource<UserDto>> =
    ApiResource.of(SuccessCode.CREATED, userService.create(request))
```

### 6. Deletion Response

Use `ApiResource.success()` without data for deletion operations. The method returns "Success" as the data value.

```kotlin
@DeleteMapping("/{id}")
fun delete(@PathVariable id: Long): ResponseEntity<ApiResource<String>> {
    userService.delete(id)
    return ApiResource.success()
}
```

### 7. Map Response

Use `ApiResource.success()` for map responses:

```kotlin
@GetMapping("/stats")
fun getStatistics(): ResponseEntity<ApiResource<Map<String, Int>>> =
    ApiResource.success(mapOf(
        "totalUsers" to 100,
        "activeUsers" to 85,
        "pendingUsers" to 15
    ))
```

## Date/Time Format Rules

### ISO-8601 Standard

All date and time fields use **ISO-8601** format.

| Type | Format | Example |
|------|------|------|
| `LocalDate` | `yyyy-MM-dd` | `2025-01-02` |
| `LocalTime` | `HH:mm:ss[.SSS]` | `14:30:00` |
| `LocalDateTime` | `yyyy-MM-dd'T'HH:mm:ss[.SSS]` | `2025-01-02T14:30:00` |
| `ZonedDateTime` | `yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'` | `2025-01-02T14:30:00+09:00[Asia/Seoul]` |
| `Instant` | `yyyy-MM-dd'T'HH:mm:ss'Z'` | `2025-01-02T05:30:00Z` |

**Note:** Milliseconds (`.SSS`) are optional.

### DTO Example

The following example shows proper date/time field usage in a DTO:

```kotlin
data class EventDto(
    val id: Long,
    val name: String,
    val startDate: LocalDate,        // "2025-01-02"
    val startTime: LocalTime,        // "14:30:00"
    val createdAt: LocalDateTime,    // "2025-01-02T14:30:00"
    val scheduledAt: ZonedDateTime   // "2025-01-02T14:30:00+09:00[Asia/Seoul]"
)
```

### @JsonFormat Annotation

Use `@JsonFormat` for explicit format specification.

| Annotation | Purpose | Applied To |
|------------|------|-----------|
| `@param:JsonFormat` | Request deserialization | Constructor parameters |
| `@get:JsonFormat` | Response serialization | Getter |
| `@field:JsonFormat` | Bidirectional | Field |

**Examples:**

```kotlin
// Request DTO
data class CreateEventRequest(
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val startAt: LocalDateTime
)

// Response DTO
data class EventResponse(
    @get:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime
)

// Bidirectional DTO
data class EventDto(
    @field:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime
)
```

### Timezone Handling

#### Default Timezone: UTC

All Bootstrap applications set UTC as the default timezone.

```kotlin
fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<MyApplication>(*args)
}
```

**Rules:**

| Item | Rule |
|------|------|
| JVM default timezone | `UTC` |
| `LocalDateTime.now()` | Operates in UTC |
| DB storage | UTC-based |
| KST conversion | Explicit conversion when needed for API responses |

#### KST Conversion

Use the `.toKst()` extension function when you need KST conversion:

```kotlin
import com.myrealtrip.common.utils.extensions.toKst

val now = LocalDateTime.now()  // UTC
val kstNow = now.toKst()       // UTC + 9 hours
```

## Controller Writing Rules

### Mandatory Rule

All APIs must respond in `ApiResource<T>` format.

**Exceptions:** `GlobalController` and `HomeController` (system-level controllers).

### Correct Examples

The following examples demonstrate correct controller implementation:

```kotlin
@RestController
@RequestMapping("/api/users")
class UserController(private val userFacade: UserFacade) {
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<ApiResource<UserDto>> =
        ApiResource.success(userFacade.findById(id))

    @GetMapping
    fun findAll(pageable: Pageable): ResponseEntity<ApiResource<List<UserDto>>> =
        ApiResource.ofPage(userFacade.findAll(pageable))

    @PostMapping
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<ApiResource<UserDto>> =
        ApiResource.of(SuccessCode.CREATED, userFacade.create(request))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResource<String>> {
        userFacade.delete(id)
        return ApiResource.success()
    }
}
```

### Incorrect Examples

The following examples show common mistakes to avoid:

```kotlin
// BAD: No ApiResource wrapping
@GetMapping("/{id}")
fun findById(@PathVariable id: Long): UserDto =
    userService.findById(id)

// BAD: Only ResponseEntity
@GetMapping("/{id}")
fun findById(@PathVariable id: Long): ResponseEntity<UserDto> =
    ResponseEntity.ok(userService.findById(id))
```

## Summary

### Core Principles

1. All APIs respond in `ApiResource<T>` format
2. Standardized status management via `ResponseCode`
3. Automatic metadata by type (size, pageInfo, offsetInfo)
4. ISO-8601 date/time format
5. UTC default timezone with explicit KST conversion when needed

### Checklist

- [ ] Return type is `ResponseEntity<ApiResource<T>>`
- [ ] Appropriate `SuccessCode` or `ErrorCode` used
- [ ] Paginated responses use `ofPage()` or `ofNoOffsetPage()`
- [ ] Date/time fields in ISO-8601 format
- [ ] `@JsonFormat` uses correct use-site target (`@param`, `@get`, `@field`)

### Related Documents

- `07-error-handling.md`: Exception handling and error responses
- `92_layer-architecture.md`: Controller → Facade → Application → Service flow
- `90_project-convention.md`: API response and DTO package conventions
