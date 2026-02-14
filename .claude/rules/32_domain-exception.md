---
name: Domain Exception Handling
description: Rules for domain-specific error codes, exception classes, and exception hierarchy per feature
last-verified: 2026-02-14
---

# Domain exception handling

## Overview

This document defines rules for structuring domain-specific exceptions. Each domain feature defines its own error codes and exception hierarchy under an `exception` package.

> **Key Principle**: Each domain feature has its own `{Feature}Error` enum, `{Feature}Exception` base class, and specific exception subclasses. Use `KnownException` for expected errors, `BizRuntimeException` for unexpected errors.

## Exception hierarchy

```
BizRuntimeException (common module, unchecked)
  └── KnownException (common module, no stack trace logged)
        └── {Feature}Exception (domain feature, open class)
              ├── {Feature}NotFoundException
              ├── {Feature}AlreadyExistsException
              └── ... (other feature-specific exceptions)
```

| Class | Module | Purpose | Status Code | Stack Trace |
|-------|--------|---------|-------------|-------------|
| `BizRuntimeException` | common | Unrecoverable business errors | varies | Yes |
| `BizException` | common | Recoverable checked business errors | varies | Yes |
| `KnownException` | common | Expected errors (validation, not found) | varies | No |
| `{Feature}Exception` | domain | Feature-specific base exception | 406 | No (inherits KnownException) |
| `{Feature}NotFoundException` | domain | Resource not found | 406 | No |

> **IMPORTANT**: All domain business exceptions use HTTP status code `406`. This is the project convention for business errors. The `{Feature}Error` enum's `status` property must return `406`.

---

## Package structure

> **IMPORTANT**: Each domain feature must have its own `exception` package containing error codes and exception classes.

```
domain/{feature}/
├── application/
├── dto/
├── entity/
├── repository/
├── service/
└── exception/                          # Feature-specific exceptions
    ├── {Feature}Error.kt               # Error code enum
    ├── {Feature}Exception.kt           # Base exception + specific exceptions
    └── ...
```

---

## Error code enum

Each feature defines a `{Feature}Error` enum implementing `ResponseCode`. This enum contains all error codes specific to the feature.

```kotlin
package com.myrealtrip.domain.holiday.exception

import com.myrealtrip.common.codes.ResponseCode

enum class HolidayError(
    override val message: String,
) : ResponseCode {
    HOLIDAY_NOT_FOUND("공휴일을 찾을 수 없습니다."),
    HOLIDAY_ALREADY_EXISTS("이미 등록된 공휴일입니다."),
    INVALID_HOLIDAY_DATE("공휴일 날짜가 올바르지 않습니다."),
    ;

    override val status: Int
        get() = 406
}
```

### Naming conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Enum class | `{Feature}Error` | `HolidayError`, `OrderError`, `PaymentError` |
| Constants | `SCREAMING_SNAKE_CASE` | `HOLIDAY_NOT_FOUND`, `ORDER_ALREADY_CANCELLED` |
| Message | Korean description | `"공휴일을 찾을 수 없습니다."` |

### When to use feature error vs common ErrorCode

| Scenario | Error Code | Example |
|----------|-----------|---------|
| Feature-specific business error | `{Feature}Error` | `HolidayError.HOLIDAY_ALREADY_EXISTS` |
| Generic infrastructure error | `ErrorCode` (common) | `ErrorCode.SERVER_ERROR` |
| Generic validation error | `ErrorCode` (common) | `ErrorCode.INVALID_ARGUMENT` |

> **Tip**: If the error is specific to one domain feature, define it in `{Feature}Error`. If it applies across features, use the common `ErrorCode`.

---

## Exception classes

### Base exception (open class)

Each feature defines an `open class` base exception that extends `KnownException` and accepts the feature's error code enum.

```kotlin
package com.myrealtrip.domain.holiday.exception

import com.myrealtrip.common.exceptions.KnownException

open class HolidayException(
    code: HolidayError,
    message: String = code.message,
) : KnownException(
    code = code,
    message = message,
)
```

| Rule | Description |
|------|-------------|
| Modifier | `open class` (allows subclassing) |
| Extends | `KnownException` |
| Constructor | Accepts `{Feature}Error` and optional custom message |
| Default message | Falls back to the error code's message |

### Specific exceptions (subclasses)

Define specific exception classes for common error scenarios. Place them in the same file as the base exception.

```kotlin
class HolidayNotFoundException(id: Long) : HolidayException(
    code = HolidayError.HOLIDAY_NOT_FOUND,
    message = "Holiday not found: $id",
)
```

### Common specific exception patterns

| Exception | Error Code | Use Case |
|-----------|-----------|----------|
| `{Feature}NotFoundException` | `{FEATURE}_NOT_FOUND` | Resource lookup fails |
| `{Feature}AlreadyExistsException` | `{FEATURE}_ALREADY_EXISTS` | Duplicate creation attempt |
| `{Feature}InvalidStateException` | `{FEATURE}_INVALID_STATE` | Invalid state transition |

### Using base exception directly

For cases where a dedicated subclass is unnecessary, use `{Feature}Exception` directly with the appropriate error code.

```kotlin
// Direct usage — no subclass needed
throw HolidayException(
    HolidayError.INVALID_HOLIDAY_DATE,
    "Holiday date cannot be in the past: $date",
)

// Subclass usage — when it carries specific context (e.g., id)
throw HolidayNotFoundException(id)
```

---

## Usage in Service

Exceptions are thrown in the Service layer where business logic resides.

```kotlin
@Service
class HolidayService(
    private val holidayJpaRepository: HolidayJpaRepository,
) {
    fun findById(id: Long): HolidayInfo {
        return holidayJpaRepository.findById(id)
            .map { HolidayInfo.from(it) }
            .orElseThrow { HolidayNotFoundException(id) }
    }

    fun create(request: CreateHolidayRequest): HolidayInfo {
        if (holidayJpaRepository.existsByHolidayDateAndName(request.holidayDate, request.name)) {
            throw HolidayException(
                HolidayError.HOLIDAY_ALREADY_EXISTS,
                "Holiday already exists: ${request.holidayDate} ${request.name}",
            )
        }
        val entity = Holiday.create(request.holidayDate, request.name)
        return HolidayInfo.from(holidayJpaRepository.save(entity))
    }
}
```

---

## Common pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Using common `ErrorCode` for feature-specific errors | Loses domain context, vague error reporting | Define `{Feature}Error` enum |
| Throwing raw `KnownException` with `ErrorCode` | No feature-level error taxonomy | Use `{Feature}Exception` or its subclasses |
| Throwing exceptions in Application layer | Business logic leaks into delegation layer | Throw exceptions in Service only |
| Throwing exceptions in Facade | No transaction protection, domain logic leaks to Bootstrap | Throw exceptions in Service only |
| Missing `open` modifier on base exception | Cannot create specific subclasses | Always declare `{Feature}Exception` as `open class` |
| Hardcoded error messages without context | Difficult to debug | Include relevant identifiers (id, date, name) in message |

---

## Summary checklist

Before submitting exception-related code, verify:

- [ ] Feature has an `exception` package under `domain/{feature}/`
- [ ] `{Feature}Error` enum implements `ResponseCode` with Korean descriptions
- [ ] `{Feature}Exception` is an `open class` extending `KnownException`
- [ ] Specific exceptions (e.g., `NotFoundException`) extend `{Feature}Exception`
- [ ] Exceptions are thrown in the Service layer, not in Application or Facade
- [ ] Error messages include relevant context (id, date, name)
- [ ] Generic errors use common `ErrorCode`, feature-specific errors use `{Feature}Error`
