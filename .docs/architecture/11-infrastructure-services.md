---
title: "Infrastructure Services"
description: "Export system, Slack integration, and Common module utilities"
category: "architecture"
order: 11
last_updated: "2026-02-14"
---

# Infrastructure Services

## Overview

This document describes the modularized infrastructure features that provide reusable functionality across the application. The main services include the Export system for Excel and CSV generation, Slack integration with Kotlin DSL, and Common module utilities for value objects and data processing.

**Main Services**:
- **Export System**: Excel/CSV export with annotation-based configuration
- **Slack Integration**: Kotlin DSL message builder
- **Common Utilities**: Value Objects, DateTime, String extensions

## Export System

### Basic Usage

The export system uses annotations to configure Excel and CSV exports declaratively.

**1. Add annotations to DTO**

Define export configuration using `@ExportSheet` and `@ExportColumn` annotations.

```kotlin
@ExportSheet(name = "User List", freezeHeader = true, includeIndex = true)
data class UserExportDto(
    @ExportColumn(header = "ID", order = 1, width = 10)
    val id: Long,

    @ExportColumn(header = "Name", order = 2, width = 20)
    val name: String,

    @ExportColumn(
        header = "Email",
        order = 3,
        width = 30,
        bodyStyle = ExportCellStyle(preset = ExportStylePreset.BODY_LINK)
    )
    val email: String,

    @ExportColumn(header = "Join Date", order = 4, width = 15, format = "yyyy-MM-dd")
    val createdAt: LocalDate,

    @ExportColumn(
        header = "Points",
        order = 5,
        width = 15,
        format = "#,##0",
        bodyStyle = ExportCellStyle(preset = ExportStylePreset.BODY_CURRENCY)
    )
    val points: Long,
)
```

**2. Export from Controller**

Use the `ExcelExporter` or `CsvExporter` to generate files.

```kotlin
@GetMapping("/users/export")
fun exportUsers(@RequestParam format: ExportFormat = ExportFormat.EXCEL): ResponseEntity<ByteArray> {
    val users = userService.findAll()
    val exportDtos = users.map { UserExportDto.from(it) }

    val outputStream = ByteArrayOutputStream()
    val exporter = when (format) {
        ExportFormat.EXCEL -> ExcelExporter()
        ExportFormat.CSV -> CsvExporter()
    }

    exporter.export(exportDtos, UserExportDto::class, outputStream)

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=users.${format.extension}")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(outputStream.toByteArray())
}
```

### Annotations

#### @ExportSheet (Class Level)

The `@ExportSheet` annotation configures sheet-level properties.

```kotlin
annotation class ExportSheet(
    val name: String = "Sheet1",
    val freezeHeader: Boolean = true,
    val includeIndex: Boolean = true,
    val indexHeader: String = "No.",
    val overflowStrategy: OverflowStrategy = OverflowStrategy.MULTI_SHEET,
)
```

#### @ExportColumn (Field Level)

The `@ExportColumn` annotation configures column-level properties.

```kotlin
annotation class ExportColumn(
    val header: String,
    val order: Int = 0,
    val width: Int = -1,
    val format: String = "",
    val headerStyle: ExportCellStyle = ExportCellStyle(),
    val bodyStyle: ExportCellStyle = ExportCellStyle(),
)
```

**Format Examples**:

| Type | Format | Result |
|------|--------|--------|
| Date | `yyyy-MM-dd` | 2026-02-14 |
| DateTime | `yyyy-MM-dd HH:mm:ss` | 2026-02-14 10:30:00 |
| Number | `#,##0` | 1,000 |
| Currency | `#,##0.00` | 1,000.00 |

### Style Presets

The system provides predefined style presets for common formatting needs.

```kotlin
enum class ExportStylePreset {
    NONE,
    HEADER_DEFAULT,         // Bold, gray background, center
    HEADER_BLUE,            // Bold, blue background
    HEADER_DARK_BLUE,       // Bold, dark blue, white text
    BODY_DEFAULT,           // Left aligned
    BODY_CENTER,            // Center aligned
    BODY_RIGHT,             // Right aligned
    BODY_HIGHLIGHT,         // Bold, yellow background
    BODY_WARNING,           // Bold, orange background, red text
    BODY_CURRENCY,          // Bold, right aligned
}
```

### Large Dataset Processing

Process large datasets in chunks to avoid memory issues.

```kotlin
fun exportLargeDataset(outputStream: OutputStream) {
    val exporter = ExcelExporter()

    exporter.exportWithChunks(
        clazz = UserExportDto::class,
        outputStream = outputStream
    ) { consumer ->
        var offset = 0
        val chunkSize = 1000

        while (true) {
            val chunk = userRepository.findChunk(offset, chunkSize)
            if (chunk.isEmpty()) break

            val dtos = chunk.map { UserExportDto.from(it) }
            consumer(dtos)
            offset += chunkSize
        }
    }
}
```

## Slack Integration

### Basic Configuration

Configure Slack integration in the application properties.

```yaml
slack:
  enabled: true
  bot-token: ${SLACK_BOT_TOKEN}
  default-channel: "#alerts"
```

### Simple Usage

The SlackNotificationService provides convenience methods for common notification types.

```kotlin
@Service
class DeploymentService(
    private val slackNotificationService: SlackNotificationService,
) {
    fun deployApplication(version: String) {
        // Deployment logic...

        slackNotificationService.notifySuccess(
            channel = "#deploy",
            title = "Deployment Complete",
            message = "Version $version deployed successfully"
        )
    }
}
```

**Convenience Methods**:

Use these methods for common notification scenarios.

```kotlin
// Success notification (green)
slackNotificationService.notifySuccess("#alerts", "Task Complete", "message")

// Error notification (red, stack trace)
slackNotificationService.notifyError("#errors", "Error Occurred", exception)

// Warning notification (yellow)
slackNotificationService.notifyWarning("#monitoring", "Low Stock", "message")

// Info notification (blue)
slackNotificationService.notifyInfo("#general", "Daily Report", "message")
```

### Slack DSL

Build complex messages using the Kotlin DSL.

```kotlin
val message = slackMessage {
    text("Urgent Alert")

    header("System Maintenance Notice")

    section {
        markdown("""
            *Maintenance Time:* 2026-02-14 02:00 ~ 04:00
            *Affected Services:* All services
        """.trimIndent())
    }

    divider()

    context {
        text("Contact: Infrastructure Team")
    }

    actions {
        button("Check", "https://status.example.com", ButtonStyle.PRIMARY)
    }
}

slackNotificationService.notify("#announcements", message)
```

**Attachment Colors**:

Use predefined colors to indicate message severity.

```kotlin
val message = slackMessage {
    text("Order Processing Complete")

    attachment(SlackColor.SUCCESS) {  // Green
        section {
            markdown("*Order Number:* #12345\n*Status:* Payment Complete")
        }
    }

    attachment(SlackColor.INFO) {  // Blue
        section { markdown("Estimated Delivery: 2026-02-15") }
    }
}
```

| Color | Usage | Code |
|-------|-------|------|
| SUCCESS | Success | `#36a64f` |
| DANGER | Error | `#ff0000` |
| WARNING | Warning | `#ffcc00` |
| INFO | Information | `#3AA3E3` |

### Thread Reply

Send replies in threads to organize related messages.

```kotlin
// Original message
val response = slackNotificationService.notify("#deploy", slackMessage {
    text("Deployment Started")
})

// Reply in thread
slackNotificationService.reply("#deploy", response.ts, slackMessage {
    text("DB Migration Complete")
})

slackNotificationService.reply("#deploy", response.ts, slackMessage {
    text("Deployment Complete")
    attachment(SlackColor.SUCCESS) {
        section { markdown("All tasks completed") }
    }
})
```

### Async Sending

Choose synchronous or asynchronous sending based on requirements.

```kotlin
// Synchronous: wait for response
val response = slackNotificationService.notify(channel, message)

// Asynchronous: return immediately
slackNotificationService.notifyAsync(channel, message)
slackNotificationService.notifySuccessAsync("#alerts", "Complete", "message")
```

## Common Module: Value Objects

### Email

The Email value object provides validated email handling with masking support.

```kotlin
import com.myrealtrip.common.values.Email

// Creation
val email = Email.of("user@example.com")
val email = "user@example.com".asEmail

// Validation
Email.isValid("user@example.com")  // true

// Properties
email.localPart      // "user"
email.domain         // "example.com"

// Masking
email.masked()       // "us******@example.com"
```

### PhoneNumber

The PhoneNumber value object handles international phone number formatting and validation.

```kotlin
import com.myrealtrip.common.values.PhoneNumber

// Creation
val phone = PhoneNumber.of("010-1234-5678")
val phone = "010-1234-5678".asPhoneNumber

// Formatting
phone.toE164()          // "+821012345678"
phone.toNational()      // "010-1234-5678"
phone.toInternational() // "+82 10-1234-5678"

// Properties
phone.countryCode      // 82
phone.regionCode       // "KR"
phone.isMobile         // true

// Masking
phone.masked()         // "***-****-5678"
phone.toString()       // "***-****-5678"
```

### Money

The Money value object provides type-safe monetary operations with currency support.

```kotlin
import com.myrealtrip.common.values.Money

// Creation
val price = Money.krw(10000)
val price = 10000L.krw
val usd = 99.99.usd

// Operations (same currency only)
val total = price1 + price2
val diff = price1 - price2
val doubled = price1 * 2
val half = price1 / 2

// Apply Rate
val discount = price * Rate.ofPercent(15)
price.applyRate(discountRate)           // Discount amount
price.remainderAfterRate(discountRate)  // After discount

// Comparison
price1 > price2
price1.isPositive()

// Formatting
price.format()          // "₩10,000"
price.formatWithCode()  // "10,000 KRW"
```

### Rate

The Rate value object represents percentages and ratios with type-safe operations.

```kotlin
import com.myrealtrip.common.values.Rate

// Creation
val discount = Rate.ofPercent(15)   // 15%
val discount = 15.percent
val discount = Rate.of(0.15)

// Apply
val amount = 10000.krw
rate.applyTo(amount)      // Money(1500 KRW)
rate.remainderOf(amount)  // Money(8500 KRW)

// Conversion
rate.toPercent()       // 15.0000
rate.toDecimal()       // 0.1500
rate.toPercentString() // "15%"
```

## Common Module: DateTime

### DateFormatter

The DateFormatter provides standardized date and time parsing and formatting.

```kotlin
import com.myrealtrip.common.utils.datetime.DateFormatter.*

// Parsing
"2026-02-14".toDate()           // LocalDate
"20260214".numericToDate()      // LocalDate
"2026-02-14T10:30:00".toDateTime()  // LocalDateTime

// Formatting
date.toStr()         // "2026-02-14"
date.toNumericStr()  // "20260214"
date.toKorean()      // "2026년 02월 14일"
```

### SearchDates

The SearchDates utility provides convenient date range creation for queries.

```kotlin
import com.myrealtrip.common.utils.datetime.SearchDates

val today = SearchDates.today()
val lastWeek = SearchDates.lastWeek()
val thisMonth = SearchDates.thisMonth()
val custom = SearchDates.of(startDate, endDate)

// Usage
if (date in thisMonth) {
    // Within this month
}
```

### LocalDateRange

The LocalDateRange provides date range operations and queries.

```kotlin
import com.myrealtrip.common.utils.datetime.LocalDateRange

val range = LocalDateRange.from(start, end)

date in range           // Contains check
range.overlaps(other)   // Overlap check
range.daysBetween()     // Days calculation
```

## Common Module: String Extensions

### Masking

The masking extensions protect sensitive information in logs and displays.

```kotlin
import com.myrealtrip.common.utils.extensions.*

// Name masking
"홍길동".maskName()              // "홍*동"
"Alexander".maskName()          // "A*******r"

// Phone number masking
"010-1234-5678".maskDigits()    // "***-****-5678"

// Email masking
"user@example.com".maskEmail()  // "us**@example.com"

// Generic masking
"sensitive-data".mask(4, 8)     // "sens****data"
```

### Utilities

The string utilities provide common string operations.

```kotlin
// Null/blank handling
val value: String? = null
value.ifNullOrBlank("default")  // "default"

// Remove spaces
"hello world".removeAllSpaces()  // "helloworld"

// Extract numbers
"abc123def456".extractNumbers()  // "123456"
```

## Summary

| Category | Key Features | Location |
|----------|--------------|----------|
| **Export** | Excel/CSV export | `infrastructure/export` |
| **Slack** | Kotlin DSL message builder | `infrastructure/slack` |
| **Value Objects** | Email, PhoneNumber, Money, Rate | `common/values` |
| **DateTime** | DateFormatter, SearchDates | `common/utils/datetime` |
| **String** | Masking, utilities | `common/utils/extensions` |

**Best Practices**:
1. Use Value Objects instead of primitive types for domain values
2. Declare export configurations with annotations
3. Structure Slack messages with DSL for complex notifications
4. Utilize DateTime utilities for consistent date handling
5. Apply masking to protect personal information in logs

## Related Documents

- [Caching Strategy](09-caching-strategy.md)
- [Cross-Cutting Concerns](10-cross-cutting-concerns.md)
