---
title: "Error Handling Strategy"
description: "Exception hierarchy, GlobalExceptionHandler, custom exception patterns, Precondition validation utilities"
category: "architecture"
order: 7
last_updated: "2026-02-14"
---

# Error Handling Strategy

## Overview

This document describes the project's error handling strategy. The project provides consistent error handling through a layered exception structure and standardized error responses. `GlobalExceptionHandlerV2` converts all exceptions to `ApiResource<T>` format.

## Exception Hierarchy

### BizExceptionInfo Interface

The `BizExceptionInfo` interface defines the contract for all business exceptions:

```kotlin
interface BizExceptionInfo {
    val code: ResponseCode
    val message: String
    val cause: Throwable?
    val logStackTrace: Boolean
}
```

### Exception Type Classification

The following diagram shows the exception hierarchy:

```
Exception
├── BizException (Checked)
│   └── Recoverable business error
└── RuntimeException
    └── BizRuntimeException (Unchecked)
        ├── Unrecoverable business error
        └── KnownException (Unchecked)
            └── Expected error (no stack trace)
```

### 1. KnownException (Recommended)

Use `KnownException` for expected or known error conditions that do not require stack trace logging.

```kotlin
open class KnownException @JvmOverloads constructor(
    override val code: ResponseCode,
    override val message: String = code.message,
    cause: Throwable? = null,
) : BizRuntimeException(
    code = code,
    message = message,
    cause = cause,
    logStackTrace = false,
)
```

**Use Cases:**
- Resource not found
- Validation failure
- User input error
- Business rule violation

**Example:**

```kotlin
class UserNotFoundException(userId: Long) : KnownException(
    code = ErrorCode.DATA_NOT_FOUND,
    message = "User not found: $userId"
)
```

**Log Output:**

```
INFO  c.m.c.h.GlobalExceptionHandlerV2 - [GET /api/users/999] DATA_NOT_FOUND: User not found: 999
```

### 2. BizRuntimeException

Use `BizRuntimeException` for unrecoverable business errors that require stack trace logging.

```kotlin
open class BizRuntimeException @JvmOverloads constructor(
    override val code: ResponseCode,
    override val message: String = code.message,
    override val cause: Throwable? = null,
    override val logStackTrace: Boolean = false,
) : RuntimeException(message, cause), BizExceptionInfo
```

**Use Cases:**
- Bugs or unexpected conditions
- Unrecoverable situations
- Errors requiring developer fixes

**Example:**

```kotlin
throw BizRuntimeException(
    code = ErrorCode.ILLEGAL_STATE,
    message = "Unexpected state: payment already completed but status is PENDING"
)
```

### 3. BizException (Checked)

Use `BizException` for recoverable business errors that the caller must handle.

**Use Cases:**
- Caller expected to handle the error
- Error is part of normal business flow
- Recovery is possible

### Exception Type Selection

| Situation | Exception Type | Log Level | Stack Trace |
|------|-----------|-----------|---------------|
| Expected error (validation, not found) | `KnownException` | INFO | No |
| Unrecoverable business error | `BizRuntimeException` | ERROR | Yes |
| Recoverable business error (checked) | `BizException` | ERROR | Optional |
| Unexpected error | `RuntimeException` | ERROR | Yes |

## GlobalExceptionHandlerV2

### Role

`GlobalExceptionHandlerV2` catches all exceptions and converts them to error responses in `ApiResource<T>` format.

```kotlin
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class GlobalExceptionHandlerV2 {
    // ...
}
```

### Processing Flow

The following diagram shows the exception handling flow:

```
Exception in Controller
    ↓
GlobalExceptionHandlerV2 handles by type
    ↓
Map to appropriate ErrorCode
    ↓
Log (with/without stack trace)
    ↓
Return ApiResource<T> error response
```

### Exception Handling Methods

#### 1. KnownException

GlobalExceptionHandlerV2 catches `KnownException` and logs at INFO level without stack traces:

```kotlin
@ExceptionHandler(KnownException::class)
fun handleKnownException(
    request: HttpServletRequest,
    e: KnownException,
): ResponseEntity<ApiResource<Any>> =
    createApiResource(request, e.code, e, log = false)
```

The handler uses `e.code` directly and sets the log level to INFO.

#### 2. BizRuntimeException

GlobalExceptionHandlerV2 catches `BizRuntimeException` and conditionally logs stack traces based on the `logStackTrace` flag:

```kotlin
@ExceptionHandler(BizRuntimeException::class)
fun handleBizRuntimeException(
    request: HttpServletRequest,
    e: BizRuntimeException,
): ResponseEntity<ApiResource<Any>> =
    createApiResource(request, e.code, e, e.logStackTrace)
```

Log level is ERROR when `logStackTrace=true`, INFO when `logStackTrace=false`.

#### 3. General Exceptions

GlobalExceptionHandlerV2 maps Spring and standard exceptions to appropriate `ErrorCode` values:

```kotlin
@ExceptionHandler(Exception::class)
fun handleException(
    request: HttpServletRequest,
    ex: Exception,
): ResponseEntity<ApiResource<Any>> = when (ex) {
    // 404 errors
    is NoResourceFoundException,
    is HttpRequestMethodNotSupportedException
    -> createApiResource(request, ErrorCode.NOT_FOUND, ex)

    // 400 errors - validation
    is MethodArgumentNotValidException -> {
        val errors = ex.bindingResult.allErrors
            .filterIsInstance<FieldError>()
            .associate { it.field to it.defaultMessage }
        createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex,
            printStackTrace = false, data = errors)
    }

    is MethodArgumentTypeMismatchException -> {
        val detail = "Type mismatch: '${ex.value}' in '${ex.propertyName}'"
        createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex,
            printStackTrace = false, data = detail)
    }

    is ConstraintViolationException -> {
        val errors = ex.constraintViolations
            .associate { it.propertyPath.toString() to it.message }
        createApiResource(request, ErrorCode.INVALID_ARGUMENT, ex,
            printStackTrace = false, data = errors)
    }

    // 403 errors
    is AccessDeniedException
    -> createApiResource(request, ErrorCode.FORBIDDEN, ex)

    // 406 errors - business
    is IllegalArgumentException
    -> createApiResource(request, ErrorCode.ILLEGAL_ARGUMENT, ex)

    is IllegalStateException
    -> createApiResource(request, ErrorCode.ILLEGAL_STATE, ex)

    is NoSuchElementException
    -> createApiResource(request, ErrorCode.DATA_NOT_FOUND, ex)

    // 500 errors
    else -> createApiResource(request, ErrorCode.SERVER_ERROR, ex,
        data = ErrorCode.SERVER_ERROR.message)
}
```

### Exception → ErrorCode Mapping

| Spring Exception | ErrorCode | HTTP | Stack Trace |
|-------------|-----------|-----------|---------------|
| `NoResourceFoundException` | `NOT_FOUND` | 404 | Yes |
| `MethodArgumentNotValidException` | `INVALID_ARGUMENT` | 400 | No |
| `MethodArgumentTypeMismatchException` | `INVALID_ARGUMENT` | 400 | No |
| `ConstraintViolationException` | `INVALID_ARGUMENT` | 400 | No |
| `AccessDeniedException` | `FORBIDDEN` | 403 | Yes |
| `IllegalArgumentException` | `ILLEGAL_ARGUMENT` | 406 | Yes |
| `IllegalStateException` | `ILLEGAL_STATE` | 406 | Yes |
| `NoSuchElementException` | `DATA_NOT_FOUND` | 406 | Yes |
| Other exceptions | `SERVER_ERROR` | 500 | Yes |

## Error Response Format

### Basic Error Response

The following example shows a standard error response:

```json
{
  "status": {
    "status": 406,
    "code": "DATA_NOT_FOUND",
    "message": "Requested data not found"
  },
  "meta": {
    "x-b3-traceid": "507f1f77bcf86cd799439011",
    "appTraceId": "01932f7a-3b45-7000-8000-0242ac120002",
    "responseTs": 1707901234567
  },
  "data": "User not found: 999"
}
```

### Validation Error Response

GlobalExceptionHandlerV2 includes field-level errors in the `data` field:

```json
{
  "status": {
    "status": 400,
    "code": "INVALID_ARGUMENT",
    "message": "Invalid request argument"
  },
  "meta": {...},
  "data": {
    "email": "Invalid email format",
    "age": "must be greater than or equal to 18",
    "name": "must not be blank"
  }
}
```

## Custom Exception Patterns

### Domain Exception Rules

Follow these rules when creating domain exceptions:

1. **Location:** `modules/domain/src/main/kotlin/com/myrealtrip/domain/{feature}/dto/`
2. **Naming:** `{Feature}NotFoundException`, `Invalid{Feature}StateException`
3. **Inheritance:** `KnownException` (recommended)
4. **Parameters:** Pass contextual information for exception message

### Example 1: Not Found Exception

The following example shows a standard not found exception:

```kotlin
package com.myrealtrip.domain.user.dto

import com.myrealtrip.common.codes.response.ErrorCode
import com.myrealtrip.common.exceptions.KnownException

class UserNotFoundException(userId: Long) : KnownException(
    code = ErrorCode.DATA_NOT_FOUND,
    message = "User not found: $userId"
)
```

**Usage:**

```kotlin
@Service
class UserService(private val userRepository: UserRepository) {
    fun findById(id: Long): UserInfo {
        val user = userRepository.findById(id)
            .orElseThrow { UserNotFoundException(id) }
        return user.toInfo()
    }
}
```

### Example 2: State Validation

The following example shows a state validation exception with context:

```kotlin
class InvalidOrderStateException(
    orderId: Long,
    currentState: OrderState,
    expectedStates: List<OrderState>
) : KnownException(
    code = ErrorCode.ILLEGAL_STATE,
    message = "Order $orderId is in state $currentState, expected one of $expectedStates"
)
```

### Example 3: Business Rule Violation

The following example shows a business rule violation exception with detailed context:

```kotlin
class InsufficientStockException(
    productId: Long,
    requested: Int,
    available: Int
) : KnownException(
    code = ErrorCode.ILLEGAL_STATE,
    message = "Insufficient stock for product $productId: requested=$requested, available=$available"
)
```

## Precondition Validation Utilities

### Standard vs Known Validation

| Standard Kotlin | Common Utility | Exception Type |
|-------------|-----------------|-----------|
| `require()` | `knownRequired()` | `IllegalArgumentException` → `KnownException` |
| `requireNotNull()` | `knownRequiredNotNull()` | `IllegalArgumentException` → `KnownException` |
| - | `knownNotBlank()` | `KnownException` |

### knownRequired()

Use `knownRequired()` to validate boolean conditions:

```kotlin
inline fun knownRequired(value: Boolean, lazyMessage: () -> Any)
```

**Example:**

```kotlin
fun createOrder(userId: Long, amount: Money) {
    knownRequired(amount.value > 0) {
        "Order amount must be positive: $amount"
    }
    knownRequired(userId > 0) {
        "Invalid user ID: $userId"
    }
}
```

**Exception Thrown:**

```kotlin
KnownException(
    code = ErrorCode.ILLEGAL_ARGUMENT,
    message = "Order amount must be positive: -1000 KRW"
)
```

### knownRequiredNotNull()

Use `knownRequiredNotNull()` to validate non-null values:

```kotlin
inline fun <T : Any> knownRequiredNotNull(value: T?, lazyMessage: () -> Any): T
```

**Example:**

```kotlin
fun processOrder(orderId: Long): OrderInfo {
    val order = knownRequiredNotNull(orderRepository.findById(orderId).orElse(null)) {
        "Order not found: $orderId"
    }
    return order.toInfo()
}
```

**Advantages:**
- Direct variable assignment
- Smart cast support
- Concise code

### knownNotBlank()

Use `knownNotBlank()` to validate non-blank strings:

```kotlin
inline fun knownNotBlank(value: String?, lazyMessage: () -> Any): String
```

**Example:**

```kotlin
fun createUser(request: CreateUserRequest) {
    val name = knownNotBlank(request.name) { "User name is required" }
    val email = knownNotBlank(request.email) { "User email is required" }
}
```

### Precondition Selection Guide

| Situation | Function | Example |
|------|-------------|------|
| Boolean condition | `knownRequired()` | `knownRequired(amount > 0) { ... }` |
| Null check | `knownRequiredNotNull()` | `knownRequiredNotNull(user) { ... }` |
| String blank check | `knownNotBlank()` | `knownNotBlank(name) { ... }` |
| Complex business validation | Custom exception | `throw InvalidOrderStateException(...)` |

## Error Logging

### Log Level Rules

| Exception Type | Log Level | Stack Trace | Reason |
|-----------|-----------|---------------|------|
| `KnownException` | INFO | No | Expected error |
| `BizRuntimeException` (logStackTrace=false) | INFO | No | Business error, no trace needed |
| `BizRuntimeException` (logStackTrace=true) | ERROR | Yes | Unexpected business error |
| Spring validation | INFO | No | Client input error |
| Other exceptions | ERROR | Yes | Unexpected server error |

## Best Practices

### DO: Use KnownException for Expected Errors

```kotlin
// GOOD
fun findUser(id: Long): UserInfo {
    val user = userRepository.findById(id)
        .orElseThrow { UserNotFoundException(id) }
    return user.toInfo()
}

class UserNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND,
    "User not found: $id"
)
```

### DO: Provide Meaningful Error Messages

```kotlin
// GOOD
throw InvalidOrderStateException(
    orderId = order.id,
    currentState = order.state,
    expectedStates = listOf(OrderState.PENDING, OrderState.CONFIRMED)
)

// BAD
throw KnownException(ErrorCode.ILLEGAL_STATE, "Invalid state")
```

### DO: Use Explicit Exceptions for Business Validation

```kotlin
// GOOD
if (order.amount < minimumAmount) {
    throw OrderAmountTooSmallException(order.id, order.amount, minimumAmount)
}

// BAD
require(order.amount >= minimumAmount) { "Amount too small" }
```

### DO: Use knownRequired* for Preconditions

```kotlin
// GOOD
fun createOrder(userId: Long, items: List<OrderItem>) {
    knownRequired(items.isNotEmpty()) {
        "Order must have at least one item"
    }
    val user = knownRequiredNotNull(userRepository.findById(userId).orElse(null)) {
        "User not found: $userId"
    }
}

// BAD (standard require)
fun createOrder(userId: Long, items: List<OrderItem>) {
    require(items.isNotEmpty()) { "Order must have at least one item" }
    // -> Throws IllegalArgumentException
}
```

### DON'T: Throw Generic RuntimeException

```kotlin
// BAD
throw RuntimeException("Something went wrong")

// GOOD
throw BizRuntimeException(ErrorCode.SERVER_ERROR, "Unexpected error in payment processing")
```

### DON'T: Include Sensitive Information

```kotlin
// BAD
throw KnownException(
    ErrorCode.UNAUTHORIZED,
    "Login failed for user ${user.email} with password ${password}"
)

// GOOD
throw KnownException(
    ErrorCode.UNAUTHORIZED,
    "Login failed: invalid credentials"
)
```

### DON'T: Use Exceptions for Flow Control

```kotlin
// BAD
fun findUserOrNull(id: Long): UserInfo? {
    return try {
        findUser(id)
    } catch (e: UserNotFoundException) {
        null
    }
}

// GOOD
fun findUserOrNull(id: Long): UserInfo? {
    return userRepository.findById(id)
        .map { it.toInfo() }
        .orElse(null)
}
```

## Summary

### Core Principles

1. Use `KnownException` for expected errors (INFO logging without stack trace)
2. Use `BizRuntimeException` for unexpected errors (ERROR logging with stack trace)
3. Use `knownRequired*()` for precondition validation (concise and consistent)
4. Provide meaningful error messages (easier debugging)
5. GlobalExceptionHandler converts all exceptions to `ApiResource` format

### Checklist

- [ ] Expected error? Use `KnownException`
- [ ] Unrecoverable error? Use `BizRuntimeException`
- [ ] Error message sufficiently specific?
- [ ] Using `knownRequired*()` for preconditions?
- [ ] Custom exception in domain package?
- [ ] No sensitive information in error messages?

### Related Documents

- `06-api-response-format.md`: ApiResource response format
- `30_common-module.md`: Common module utilities
- `92_layer-architecture.md`: Error handling by layer
