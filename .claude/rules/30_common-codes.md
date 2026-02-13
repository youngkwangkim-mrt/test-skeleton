---
description: Enum conventions for domain codes including CommonCode interface, EnumType.STRING, and categorization patterns
globs: "*.{java,kt,kts}"
alwaysApply: false
---

# Common Codes & Enums

## Core Rule
All categorized domain codes must implement `CommonCode` from `com.myrealtrip.common.codes`. Always persist enums as `EnumType.STRING` -- never `ORDINAL`.

## CommonCode Interface
```kotlin
interface CommonCode {
    val code: String        // Machine-readable code value
    val description: String // Human-readable description
    val name: String        // Enum constant name (from Kotlin enum)
}
```

## Enum Definition Rules
- Implement `CommonCode` with `code` and `description` properties
- Enum class: `PascalCase` (`OrderStatus`, `PaymentMethod`)
- Constants: `SCREAMING_SNAKE_CASE` (`PENDING`, `IN_PROGRESS`)
- Add trailing semicolon (`;`) after last constant when enum has a body
- Provide `fromCode()` companion method for external code mapping

## JPA Entity Usage
- **Always** use `@Enumerated(EnumType.STRING)` -- `ORDINAL` breaks on reorder/removal
- Set `@Column(length = N)` to match longest constant name (e.g., 20 for safety)

## When to Use CommonCode
- **Yes**: Business status codes, category/type classifications, role/permission types
- **No**: Response codes (use `ResponseCode`), internal flags (plain enum), sealed classes

## Package Location
- Domain-specific: `domain/{feature}/entity/` (alongside Entity)
- Shared across features: `domain/common/codes/`
- Common module: `common/codes/`

## REST Docs Integration
`CommonCodeDocsTest` auto-documents all `CommonCode` enums, keeping API docs in sync.

## Common Pitfalls
- `EnumType.ORDINAL` -> data corruption on reorder
- Missing `@Column(length)` -> defaults to 255, wastes storage
- Hardcoded string comparisons -> use enum constants directly
- Missing trailing semicolon -> compilation error when body added

> For detailed examples, see skill: `common-codes`
