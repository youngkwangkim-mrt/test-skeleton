---
name: Common Module
description: Common module usage guide for Value Objects, Exceptions, DateTime utilities, and extensions
---

# Common Module Usage Guide

## Overview

`modules/common`은 프로젝트 전체에서 사용되는 공통 유틸리티를 제공합니다. 코드 작성 시 이 모듈의 컴포넌트를 우선적으로 사용하세요.

> **참고**: 자세한 사용법은 `modules/common/README.adoc` 참조

## Value Objects (필수 사용)

> **IMPORTANT**: 원시 타입(String, Long, BigDecimal) 대신 타입 안전한 값 객체를 사용하세요.

### Email

이메일 처리 시 `String` 대신 `Email` 사용

```kotlin
// BAD
data class User(
    val email: String  // 유효하지 않은 이메일 허용
)

// GOOD
import com.myrealtrip.common.values.Email

data class User(
    val email: Email  // 컴파일 타임 타입 안전성
)

// 생성
val email = Email.of("user@example.com")
val email = "user@example.com".asEmail

// 유효성 검사
Email.isValid("user@example.com")

// 마스킹
email.masked()  // "us**@example.com"
```

### PhoneNumber

전화번호 처리 시 `String` 대신 `PhoneNumber` 사용

```kotlin
// BAD
data class Contact(
    val phone: String  // 형식 불일치, 유효성 미검증
)

// GOOD
import com.myrealtrip.common.values.PhoneNumber

data class Contact(
    val phone: PhoneNumber  // 자동 파싱, 포맷팅, 유효성 검사
)

// 생성
val phone = PhoneNumber.of("010-1234-5678")
val phone = "01012345678".asPhoneNumber

// 포맷팅
phone.toE164()       // "+821012345678"
phone.toNational()   // "010-1234-5678"

// 유효성 검사
PhoneNumber.isValid("010-1234-5678")
PhoneNumber.isValidMobile("010-1234-5678")

// 마스킹 (toString() 기본 마스킹)
phone.masked()       // "***-****-5678"
phone.toString()     // "***-****-5678"
```

### Money

금액 처리 시 `BigDecimal` 또는 `Long` 대신 `Money` 사용

```kotlin
// BAD
data class Order(
    val amount: BigDecimal,     // 통화 정보 없음
    val currency: String        // 분리된 통화 정보
)

// GOOD
import com.myrealtrip.common.values.Money

data class Order(
    val amount: Money  // 통화 정보 포함, 타입 안전한 연산
)

// 생성
val price = Money.krw(10000)
val price = Money.usd(99.99)
val price = 10000L.krw

// 연산
val total = price1 + price2  // 동일 통화만 가능
val discounted = price * 0.9

// Rate 적용
val discountRate = Rate.ofPercent(10)
price.applyRate(discountRate)           // 할인 금액
price.remainderAfterRate(discountRate)  // 할인 후 금액

// 포맷팅
price.format()          // "₩10,000"
price.formatWithCode()  // "10,000 KRW"
```

### Rate

비율/퍼센트 처리 시 `Double` 또는 `BigDecimal` 대신 `Rate` 사용

```kotlin
// BAD
val discountPercent: Double = 0.15  // 15%인지 0.15%인지 모호

// GOOD
import com.myrealtrip.common.values.Rate

val discount = Rate.ofPercent(15)   // 명확하게 15%
val discount = 15.percent           // 확장 프로퍼티
val discount = Rate.of(0.15)        // 소수점 형태

// 적용
discount.applyTo(amount)     // amount * 0.15
discount.remainderOf(amount) // amount * 0.85

// 변환
discount.toPercent()       // 15.0000
discount.toDecimal()       // 0.1500
discount.toPercentString() // "15%"
```

## Exceptions (필수 사용)

### 예외 선택 기준

| 상황 | 예외 클래스 |
|------|-------------|
| 예상된 에러 (validation, not found) | `KnownException` |
| 비즈니스 에러 (복구 불가) | `BizRuntimeException` |
| 비즈니스 에러 (복구 가능) | `BizException` |

```kotlin
import com.myrealtrip.common.exceptions.KnownException
import com.myrealtrip.common.exceptions.BizRuntimeException
import com.myrealtrip.common.codes.response.ErrorCode

// 예상된 에러 - 스택 트레이스 없이 로깅
class UserNotFoundException(id: Long) : KnownException(
    ErrorCode.DATA_NOT_FOUND,
    "User not found: $id"
)

// 비즈니스 에러
throw BizRuntimeException(ErrorCode.ILLEGAL_STATE, "Invalid order state")
```

### Precondition 검증

`require`/`check` 대신 `knownRequired` 사용 (KnownException 발생)

```kotlin
import com.myrealtrip.common.utils.knownRequired
import com.myrealtrip.common.utils.knownRequiredNotNull
import com.myrealtrip.common.utils.knownNotBlank

// boolean 검증
knownRequired(amount > 0) { "Amount must be positive" }

// null 검증
val user = knownRequiredNotNull(repository.findById(id)) {
    "User not found: $id"
}

// blank 검증
val name = knownNotBlank(request.name) { "Name is required" }
```

## DateTime Utilities

### DateFormatter

날짜/시간 파싱 및 포맷팅

```kotlin
import com.myrealtrip.common.utils.datetime.DateFormatter.toDate
import com.myrealtrip.common.utils.datetime.DateFormatter.toDateTime
import com.myrealtrip.common.utils.datetime.DateFormatter.toStr
import com.myrealtrip.common.utils.datetime.DateFormatter.toKorean

// 파싱
"2025-01-24".toDate()           // LocalDate
"20250124".numericToDate()      // LocalDate
"2025-01-24T14:30:00".toDateTime() // LocalDateTime

// 포맷팅
date.toStr()         // "2025-01-24"
date.toNumericStr()  // "20250124"
date.toKorean()      // "2025년 01월 24일"
```

### SearchDates

날짜 범위 검색

```kotlin
import com.myrealtrip.common.utils.datetime.SearchDates

val dates = SearchDates.lastMonth()
val dates = SearchDates.of(startDate, endDate)

// 포함 여부
LocalDate.now() in dates
```

### Range Classes

```kotlin
import com.myrealtrip.common.utils.datetime.LocalDateRange

val range = LocalDateRange.from(start, end)
date in range           // 포함 여부
range.overlaps(other)   // 겹침 확인
range.daysBetween()     // 일수 계산
```

## String Extensions

### 마스킹

```kotlin
import com.myrealtrip.common.utils.extensions.mask
import com.myrealtrip.common.utils.extensions.maskName
import com.myrealtrip.common.utils.extensions.maskDigits
import com.myrealtrip.common.utils.extensions.maskEmail

"홍길동".maskName()              // "홍*동"
"010-1234-5678".maskDigits()    // "***-****-5678"
"user@example.com".maskEmail()  // "us**@example.com"
```

### 문자열 유틸리티

```kotlin
import com.myrealtrip.common.utils.extensions.ifNullOrBlank
import com.myrealtrip.common.utils.extensions.removeAllSpaces

value.ifNullOrBlank("default")
"hello world".removeAllSpaces()  // "helloworld"
```

## DateTime Extensions

```kotlin
import com.myrealtrip.common.utils.extensions.isToday
import com.myrealtrip.common.utils.extensions.isPast
import com.myrealtrip.common.utils.extensions.getAge
import com.myrealtrip.common.utils.extensions.getKoreanAge

date.isToday()
date.isPast()
birthDate.getAge()        // 만 나이
birthDate.getKoreanAge()  // 한국 나이
```

## Other Utilities

### StopWatch

```kotlin
import com.myrealtrip.common.utils.stopWatch

val (elapsedMs, result) = stopWatch("API Call") {
    apiClient.call()
}
```

### Coroutine (MDC 유지)

```kotlin
import com.myrealtrip.common.utils.coroutine.runBlockingWithMDC
import com.myrealtrip.common.utils.coroutine.asyncWithMDC
import com.myrealtrip.common.utils.coroutine.launchWithMDC

runBlockingWithMDC { /* MDC 컨텍스트 유지 */ }
```

### AES 암호화

```kotlin
import com.myrealtrip.common.utils.cipher.AesCipher
import com.myrealtrip.common.utils.cipher.AesMode

val encrypted = AesCipher.encrypt(plainText, key, iv, AesMode.CBC)
val decrypted = AesCipher.decrypt(encrypted, key, iv, AesMode.CBC)
```

## Summary Checklist

코드 작성 시 확인:

- [ ] 이메일 → `Email` 값 객체 사용
- [ ] 전화번호 → `PhoneNumber` 값 객체 사용
- [ ] 금액 → `Money` 값 객체 사용
- [ ] 비율/퍼센트 → `Rate` 값 객체 사용
- [ ] 예상된 에러 → `KnownException` 사용
- [ ] 전제조건 검증 → `knownRequired*` 함수 사용
- [ ] 날짜/시간 파싱/포맷팅 → `DateFormatter` 확장 함수 사용
- [ ] 개인정보 마스킹 → 마스킹 확장 함수 사용
