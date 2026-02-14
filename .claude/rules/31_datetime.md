---
name: DateTime Handling
description: DateTime rules for UTC-first storage, KST conversion at response boundary, and common utilities
last-verified: 2026-02-14
---

# DateTime handling rules

## Overview

This document defines rules for handling date/time across all layers. UTC is the single source of truth for all internal operations.

> **Key Principle**: Store and process in UTC. Convert to KST only at the final display boundary. All controller inputs must be UTC.

## Core rules

| Rule | Description |
|------|-------------|
| **Internal timezone** | UTC everywhere (JVM, DB, domain logic) |
| **Controller input** | Must be UTC. If KST arrives, convert to UTC immediately |
| **Controller output** | UTC by default. Convert to KST only when display requires it |
| **KST conversion** | Use `.toKst()` extension only at the response boundary |
| **Utilities** | Use `com.myrealtrip.common.utils.datetime` for all datetime operations |

---

## JVM timezone configuration

Set UTC as the JVM default timezone in every bootstrap `-app` module.

```kotlin
fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    runApplication<MyApplication>(*args)
}
```

> **IMPORTANT**: Without this configuration, `LocalDateTime.now()` returns system-local time (KST on Korean servers), causing inconsistencies.

---

## Controller input: always UTC

> **IMPORTANT**: All datetime inputs in controllers must be UTC. If a client sends KST, convert to UTC immediately in the Controller or Facade layer before you pass it to domain.

### Correct: UTC input

```kotlin
@PostMapping("/events")
fun createEvent(
    @Valid @RequestBody request: CreateEventApiRequest,
): ResponseEntity<ApiResource<EventDto>> {
    // request.startAt is already UTC — pass directly
    val domainRequest = CreateEventRequest(
        name = request.name,
        startAt = request.startAt,
    )
    return ApiResource.success(eventFacade.create(domainRequest))
}
```

### Correct: KST input with immediate UTC conversion

Convert at the entry point when an external client or legacy system sends KST.

```kotlin
@PostMapping("/events")
fun createEvent(
    @Valid @RequestBody request: CreateEventApiRequest,
): ResponseEntity<ApiResource<EventDto>> {
    // Client sends KST — convert to UTC immediately
    val utcStartAt = request.startAt.toUtc()
    val domainRequest = CreateEventRequest(
        name = request.name,
        startAt = utcStartAt,
    )
    return ApiResource.success(eventFacade.create(domainRequest))
}
```

### Using ZonedDateTime for explicit timezone

Use `ZonedDateTime` and normalize to UTC when timezone-aware input is needed.

```kotlin
data class CreateEventApiRequest(
    val name: String,
    @param:JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")
    val startAt: ZonedDateTime,
)

// In Controller or Facade
val utcStartAt = request.startAt
    .withZoneSameInstant(ZoneOffset.UTC)
    .toLocalDateTime()
```

### Incorrect: passing KST to domain

```kotlin
// Bad: KST datetime leaks into domain layer
@PostMapping("/events")
fun createEvent(@RequestBody request: CreateEventApiRequest) {
    // request.startAt is KST but passed as-is — domain stores KST as UTC
    eventFacade.create(CreateEventRequest(startAt = request.startAt))
}
```

---

## Controller output: KST conversion at response boundary

> **IMPORTANT**: Convert to KST only when you build the API response DTO. Domain and Service layers must never perform KST conversion.

### Correct: convert in Facade or Response DTO

```kotlin
import com.myrealtrip.common.utils.extensions.toKst

// Option 1: Convert in Facade
@Component
class EventFacade(
    private val eventQueryApplication: EventQueryApplication,
) {
    fun findById(id: Long): EventDto {
        val event = eventQueryApplication.findById(id)
        return EventDto(
            id = event.id,
            name = event.name,
            startAt = event.startAt.toKst(),      // UTC → KST at response boundary
            createdAt = event.createdAt.toKst(),   // UTC → KST at response boundary
        )
    }
}

// Option 2: Convert in Response DTO factory
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
            startAt = info.startAt.toKst(),
            createdAt = info.createdAt.toKst(),
        )
    }
}
```

### Incorrect: KST conversion in domain

```kotlin
// Bad: KST conversion inside Service
@Service
class EventService {
    fun findById(id: Long): EventInfo {
        val entity = eventRepository.findById(id)
        return EventInfo(
            startAt = entity.startAt.toKst(),  // WRONG — domain must stay UTC
        )
    }
}
```

---

## DateTime lifecycle

```
[Client Request]
  UTC datetime input (or KST → convert to UTC immediately)
    ↓
[Controller / Facade]
  Ensure UTC before passing to domain
    ↓
[Domain (Application → Service → Repository)]
  All operations in UTC. No KST awareness.
    ↓
[Database]
  Stored as UTC
    ↓
[Domain → Facade / Response DTO]
  Convert to KST with .toKst() only if display requires it
    ↓
[Client Response]
  KST for display, or UTC for machine-to-machine
```

---

## Utility classes

> **IMPORTANT**: Use `com.myrealtrip.common.utils.datetime` for all datetime operations. Do not use raw `java.time` formatting or manual calculations.

### DateFormatter (parsing & formatting)

```kotlin
import com.myrealtrip.common.utils.datetime.DateFormatter.toDate
import com.myrealtrip.common.utils.datetime.DateFormatter.toDateTime
import com.myrealtrip.common.utils.datetime.DateFormatter.toStr
import com.myrealtrip.common.utils.datetime.DateFormatter.toKorean

// Parsing
"2025-01-24".toDate()                    // LocalDate
"20250124".numericToDate()               // LocalDate
"2025-01-24T14:30:00".toDateTime()       // LocalDateTime
"20250124143000".numericToDateTime()     // LocalDateTime

// Formatting
date.toStr()            // "2025-01-24"
date.toNumericStr()     // "20250124"
date.toKorean()         // "2025년 1월 24일"
dateTime.toStr()        // "2025-01-24T14:30:00"
dateTime.toKorean()     // "2025년 1월 24일 14시 30분"
```

### Timezone conversion

```kotlin
import com.myrealtrip.common.utils.extensions.toKst
import com.myrealtrip.common.utils.extensions.toUtc

// UTC → KST (for display)
val utcNow = LocalDateTime.now()     // UTC (JVM default)
val kstNow = utcNow.toKst()         // KST LocalDateTime

val utcZoned = ZonedDateTime.now()   // UTC
val kstZoned = utcZoned.toKst()     // KST ZonedDateTime

// KST → UTC (for storage/processing)
val kstInput = request.startAt       // KST from client
val utcInput = kstInput.toUtc()      // UTC LocalDateTime

val kstZonedInput = request.scheduledAt  // KST ZonedDateTime
val utcZonedInput = kstZonedInput.toUtc() // UTC ZonedDateTime
```

| Method | Direction | Use Case |
|--------|-----------|----------|
| `LocalDateTime.toKst()` | UTC → KST | Display in Facade / Response DTO |
| `ZonedDateTime.toKst()` | UTC → KST | Display in Facade / Response DTO |
| `LocalDateTime.toUtc()` | KST → UTC | Normalize KST input at controller boundary |
| `ZonedDateTime.toUtc()` | KST → UTC | Normalize KST input at controller boundary |

### SearchDates (date range for queries)

```kotlin
import com.myrealtrip.common.utils.datetime.SearchDates

val dates = SearchDates.lastMonth()
val dates = SearchDates.of(startDate, endDate)
val dates = SearchDates.lastDays(7)
val dates = SearchDates.thisWeek()

// Use in SearchCondition
data class OrderSearchCondition(
    val status: OrderStatus? = null,
    val searchDates: SearchDates = SearchDates.lastMonth(),
)
```

### Range classes

```kotlin
import com.myrealtrip.common.utils.datetime.LocalDateRange
import com.myrealtrip.common.utils.datetime.LocalDateTimeRange

val range = LocalDateRange(startDate, endDate)
date in range                    // containment check
range.overlaps(otherRange)       // overlap check
range.daysBetween()              // day count

val dtRange = LocalDateTimeRange(startDt, endDt)
dateTime in dtRange
dtRange.hoursBetween()
```

### Date extensions

```kotlin
import com.myrealtrip.common.utils.extensions.isToday
import com.myrealtrip.common.utils.extensions.isPast
import com.myrealtrip.common.utils.extensions.getAge

date.isToday()
date.isPast()
birthDate.getAge()           // International age
birthDate.getKoreanAge()     // Korean age
```

---

## ISO-8601 formats

| Type | Format | Example |
|------|--------|---------|
| Date | `yyyy-MM-dd` | `2025-01-02` |
| Time | `HH:mm:ss` | `14:30:00` |
| DateTime | `yyyy-MM-dd'T'HH:mm:ss` | `2025-01-02T14:30:00` |
| DateTime UTC | `yyyy-MM-dd'T'HH:mm:ss'Z'` | `2025-01-02T05:30:00Z` |
| ZonedDateTime | `yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'` | `2025-01-02T14:30:00+09:00[Asia/Seoul]` |

---

## Anti-patterns

| Anti-Pattern | Problem | Correct |
|--------------|---------|---------|
| Missing `TimeZone.setDefault(UTC)` in main() | `LocalDateTime.now()` returns KST | Set UTC in every bootstrap main() |
| KST input passed to domain as-is | UTC/KST mixed in DB | Convert to UTC at controller/facade |
| `.toKst()` in Service or Application | Domain polluted with display concern | `.toKst()` only in Facade or Response DTO |
| `ZonedDateTime.now(ZoneId.of("Asia/Seoul"))` | Bypasses UTC-first rule | `LocalDateTime.now()` + `.toKst()` when needed |
| `plusHours(9)` / `minusHours(9)` for conversion | Fragile, ignores DST edge cases | Use `.toKst()` / `.toUtc()` extensions |
| Raw `DateTimeFormatter` for parsing | Inconsistent formats | Use `DateFormatter` from common module |
| `String` type for date fields | No validation, format ambiguity | Use `LocalDate`, `LocalDateTime`, `ZonedDateTime` |
| `@DateTimeFormat(pattern = "yyyyMMdd")` | Non-ISO format in API | Use `@DateTimeFormat(iso = ISO.DATE)` |

---

## Summary checklist

Before submitting code, verify:

- [ ] Bootstrap main() sets `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))`
- [ ] All controller datetime inputs are UTC
- [ ] KST inputs are converted to UTC at the controller/facade boundary
- [ ] Domain and Service layers have no KST conversion logic
- [ ] `.toKst()` is used only in Facade or Response DTO for display
- [ ] `DateFormatter` from common module is used for parsing/formatting
- [ ] `SearchDates` is used for date range queries
- [ ] All datetime fields use proper types (`LocalDate`, `LocalDateTime`, `ZonedDateTime`), not `String`
- [ ] ISO-8601 format is used for all API date/time representations
