---
description: Clean code principles (KISS, DRY, YAGNI) for Java, Kotlin, and Spring applications
globs: "*.{java,kt,kts}"
alwaysApply: false
---

# Clean Code Rules

## Core Principles
- **KISS**: Write the simplest solution that works. No premature optimization or "just in case" features
- **DRY**: Extract repeated logic into reusable functions/classes. No copy-paste coding
- **YAGNI**: Implement only what is currently required. Remove unused code immediately

## Naming
- Variables/functions: `camelCase`, Classes: `PascalCase`, Constants: `SCREAMING_SNAKE_CASE`
- Names must reveal intent -- no abbreviations unless universally understood (e.g., `id`, `url`)
- Booleans: `isValid`, `hasPermission`, `canExecute`
- Function names use verbs, class names use nouns

## Functions & Classes
- Functions do ONE thing, 5-20 lines ideal
- Maintain consistent abstraction level within each function
- Single responsibility per class -- split if growing too large
- If a section needs a comment to explain, extract it into a function

## Java/Kotlin
- Prefer `val` over `var`, use immutable collections
- Use `data class` for DTOs
- Avoid `!!` -- use safe calls and `?:`
- Use `when` instead of complex `if-else`
- Use scope functions appropriately (`let`, `apply`, `also`)

## Spring
- **Constructor injection only** -- no `@Autowired` field injection
- Layer separation: Controller (HTTP) -> Service (business logic) -> Repository (data access)
- Use custom exceptions extending `BizRuntimeException` or `KnownException`

## Do NOT Overengineer
- No interfaces for single implementations
- No abstractions for hypothetical future needs
- No utility classes for one-off operations
- No excessive design patterns or configuration for everything

## Testability
- Inject dependencies (including `Clock` for time)
- Prefer pure functions without side effects
- Small, focused units are easier to test

## Code Organization
- One public class per file, group by feature not by type
- Remove unused imports, avoid wildcard imports
- Comments explain WHY, not WHAT -- keep them up-to-date or remove

> For detailed examples, see skill: `cleancode`
