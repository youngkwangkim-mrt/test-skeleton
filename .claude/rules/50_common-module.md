---
name: Common Module
description: Common module usage guide for Value Objects, Exceptions, DateTime utilities, and extensions
last-verified: 2026-02-14
---

# Common module usage guide

## Overview

The `modules/common` module provides shared utilities for the entire project. Use components from this module when you write code.

> **Note**: For detailed usage, refer to `modules/common/README.adoc`.

## Value objects (required)

> **IMPORTANT**: Use type-safe value objects instead of primitive types (String, Long, BigDecimal).

### Email

Use `Email` instead of `String` for email handling.

```kotlin
// Bad
data class User(
    val email: String  // Allows invalid emails
)

// Good
import com.myrealtrip.common.values.Email

data class User(
    val email: Email  // Compile-time type safety
)

// Creation
val email = Email.of("user@example.com")
val email = "user@example.com".asEmail

// Validation
Email.isValid("user@example.com")

// Masking
email.masked()  // "us**@example.com"
```

### PhoneNumber

Use `PhoneNumber` instead of `String` for phone number handling.

```kotlin
// Bad
data class Contact(
    val phone: String  // Inconsistent format, no validation
)

// Good
import com.myrealtrip.common.values.PhoneNumber

data class Contact(
    val phone: PhoneNumber  // Auto-parsing, formatting, validation
)

// Creation
val phone = PhoneNumber.of("010-1234-5678")
val phone = "01012345678".asPhoneNumber

// Formatting
phone.toE164()       // "+821012345678"
phone.toNational()   // "010-1234-5678"

// Validation
PhoneNumber.isValid("010-1234-5678")
PhoneNumber.isValidMobile("010-1234-5678")

// Masking (toString() masks by default)
phone.masked()       // "***-****-5678"
phone.toString()     // "***-****-5678"
```

### Money

Use `Money` instead of `BigDecimal` or `Long` for monetary amount handling.

```kotlin
// Bad
data class Order(
    val amount: BigDecimal,     // No currency information
    val currency: String        // Separated currency information
)

// Good
import com.myrealtrip.common.values.Money

data class Order(
    val amount: Money  // Includes currency, type-safe operations
)

// Creation
val price = Money.krw(10000)
val price = Money.usd(99.99)
val price = 10000L.krw

// Operations
val total = price1 + price2  // Same currency only
val discounted = price * 0.9

// Rate application
val discountRate = Rate.ofPercent(10)
price.applyRate(discountRate)           // Discount amount
price.remainderAfterRate(discountRate)  // Amount after discount

// Formatting
price.format()          // "₩10,000"
price.formatWithCode()  // "10,000 KRW"
```

### Rate

Use `Rate` instead of `Double` or `BigDecimal` for ratio/percentage handling.

```kotlin
// Bad
val discountPercent: Double = 0.15  // Ambiguous: 15% or 0.15%?

// Good
import com.myrealtrip.common.values.Rate

val discount = Rate.ofPercent(15)   // Explicitly 15%
val discount = 15.percent           // Extension property
val discount = Rate.of(0.15)        // Decimal form

// Application
discount.applyTo(amount)     // amount * 0.15
discount.remainderOf(amount) // amount * 0.85

// Conversion
discount.toPercent()       // 15.0000
discount.toDecimal()       // 0.1500
discount.toPercentString() // "15%"
```

## Exceptions (required)

### Exception selection criteria

| Scenario | Exception Class |
|----------|-----------------|
| Expected error (validation, not found) | `KnownException` |
| Business error (unrecoverable) | `BizRuntimeException` |
| Business error (recoverable) | `BizException` |

```kotlin
import com.myrealtrip.common.exceptions.KnownException
import com.myrealtrip.common.exceptions.BizRuntimeException
import com.myrealtrip.common.codes.response.ErrorCode

// Expected error - logged without stack trace
class UserNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND,
    "User not found: $id"
)

// Business error
throw BizRuntimeException(ErrorCode.ILLEGAL_STATE, "Invalid order state")
```

### Precondition validation

Use `knownRequired` instead of `require` or `check`. This function throws `KnownException`.

```kotlin
import com.myrealtrip.common.utils.knownRequired
import com.myrealtrip.common.utils.knownRequiredNotNull
import com.myrealtrip.common.utils.knownNotBlank

// Boolean validation
knownRequired(amount > 0) { "Amount must be positive" }

// Null validation
val user = knownRequiredNotNull(repository.findById(id)) {
    "User not found: $id"
}

// Blank validation
val name = knownNotBlank(request.name) { "Name is required" }
```

## DateTime utilities

### DateFormatter

The DateFormatter parses and formats date and time values.

```kotlin
import com.myrealtrip.common.utils.datetime.DateFormatter.toDate
import com.myrealtrip.common.utils.datetime.DateFormatter.toDateTime
import com.myrealtrip.common.utils.datetime.DateFormatter.toStr
import com.myrealtrip.common.utils.datetime.DateFormatter.toKorean

// Parsing
"2025-01-24".toDate()           // LocalDate
"20250124".numericToDate()      // LocalDate
"2025-01-24T14:30:00".toDateTime() // LocalDateTime

// Formatting
date.toStr()         // "2025-01-24"
date.toNumericStr()  // "20250124"
date.toKorean()      // "2025년 01월 24일"
```

### SearchDates

SearchDates handles date range searches.

```kotlin
import com.myrealtrip.common.utils.datetime.SearchDates

val dates = SearchDates.lastMonth()
val dates = SearchDates.of(startDate, endDate)

// Containment check
LocalDate.now() in dates
```

### Range classes

```kotlin
import com.myrealtrip.common.utils.datetime.LocalDateRange

val range = LocalDateRange.from(start, end)
date in range           // Containment check
range.overlaps(other)   // Overlap check
range.daysBetween()     // Day count
```

## String extensions

### Masking

```kotlin
import com.myrealtrip.common.utils.extensions.mask
import com.myrealtrip.common.utils.extensions.maskName
import com.myrealtrip.common.utils.extensions.maskDigits
import com.myrealtrip.common.utils.extensions.maskEmail

"홍길동".maskName()              // "홍*동"
"010-1234-5678".maskDigits()    // "***-****-5678"
"user@example.com".maskEmail()  // "us**@example.com"
```

### String utilities

```kotlin
import com.myrealtrip.common.utils.extensions.ifNullOrBlank
import com.myrealtrip.common.utils.extensions.removeAllSpaces

value.ifNullOrBlank("default")
"hello world".removeAllSpaces()  // "helloworld"
```

## DateTime extensions

```kotlin
import com.myrealtrip.common.utils.extensions.isToday
import com.myrealtrip.common.utils.extensions.isPast
import com.myrealtrip.common.utils.extensions.getAge
import com.myrealtrip.common.utils.extensions.getKoreanAge

date.isToday()
date.isPast()
birthDate.getAge()        // International age
birthDate.getKoreanAge()  // Korean age
```

## Other utilities

### StopWatch

```kotlin
import com.myrealtrip.common.utils.stopWatch

val (elapsedMs, result) = stopWatch("API Call") {
    apiClient.call()
}
```

### Coroutine (MDC Preservation)

```kotlin
import com.myrealtrip.common.utils.coroutine.runBlockingWithMDC
import com.myrealtrip.common.utils.coroutine.asyncWithMDC
import com.myrealtrip.common.utils.coroutine.launchWithMDC

runBlockingWithMDC { /* MDC context preserved */ }
```

### AES encryption

```kotlin
import com.myrealtrip.common.utils.cipher.AesCipher
import com.myrealtrip.common.utils.cipher.AesMode

val encrypted = AesCipher.encrypt(plainText, key, iv, AesMode.CBC)
val decrypted = AesCipher.decrypt(encrypted, key, iv, AesMode.CBC)
```

## Summary checklist

Before you submit code, verify the following items:

- [ ] Email fields use the `Email` Value Object
- [ ] Phone number fields use the `PhoneNumber` Value Object
- [ ] Monetary amount fields use the `Money` Value Object
- [ ] Ratio/percentage fields use the `Rate` Value Object
- [ ] Expected errors use `KnownException`
- [ ] Precondition validations use `knownRequired*` functions
- [ ] Date/time parsing and formatting use `DateFormatter` extension functions
- [ ] Personal data masking uses masking extension functions
