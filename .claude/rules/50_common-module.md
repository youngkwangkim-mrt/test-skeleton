---
description: Common module usage guide for Value Objects, Exceptions, DateTime utilities, and extensions
globs: "*.{kt,kts}"
alwaysApply: false
---

# Common Module Usage Guide

## Value Objects (required)
- **Email**: Use `Email` instead of `String` -- `Email.of()`, `.asEmail`, `.masked()`
- **PhoneNumber**: Use `PhoneNumber` instead of `String` -- `PhoneNumber.of()`, `.asPhoneNumber`, `.toE164()`, `.toNational()`
- **Money**: Use `Money` instead of `BigDecimal`/`Long` -- `Money.krw()`, `Money.usd()`, arithmetic operators, `.format()`
- **Rate**: Use `Rate` instead of `Double`/`BigDecimal` -- `Rate.ofPercent()`, `.percent`, `.applyTo()`, `.remainderOf()`

## Exceptions (required)
- **KnownException**: Expected errors (validation, not found) -- logged without stack trace
- **BizRuntimeException**: Unrecoverable business errors
- **BizException**: Recoverable business errors
- Use `knownRequired`, `knownRequiredNotNull`, `knownNotBlank` for precondition validation

## DateTime Utilities
- **DateFormatter**: Parse/format dates -- `.toDate()`, `.toDateTime()`, `.toStr()`, `.toKorean()`
- **SearchDates**: Date range searches -- `SearchDates.lastMonth()`, `SearchDates.of(start, end)`
- **LocalDateRange**: Range operations -- `from(start, end)`, `in`, `.overlaps()`, `.daysBetween()`

## String Extensions
- Masking: `.maskName()`, `.maskDigits()`, `.maskEmail()`
- Utilities: `.ifNullOrBlank()`, `.removeAllSpaces()`

## DateTime Extensions
- `.isToday()`, `.isPast()`, `.getAge()`, `.getKoreanAge()`

## Other Utilities
- **stopWatch**: Measure execution time
- **Coroutine MDC**: `runBlockingWithMDC`, `asyncWithMDC`, `launchWithMDC`
- **AesCipher**: AES encryption/decryption

> For detailed examples and API reference, see skill: `common-module`
