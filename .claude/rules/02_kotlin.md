---
description: Kotlin coding conventions, idioms, null safety, and best practices
globs: "*.{kt,kts}"
alwaysApply: false
---

# Kotlin Style & Best Practices

## Naming Conventions
- Packages: `lowercase`, no underscores
- Classes/interfaces: `UpperCamelCase`
- Functions/variables: `lowerCamelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Backing properties: `_items` (underscore prefix)
- Acronyms: 2-letter = both uppercase (`IOStream`), longer = capitalize first only (`XmlParser`, `HttpClient`)

## Class Layout Order
1. Constructor properties
2. Properties & initializer blocks
3. Secondary constructors
4. Public methods
5. Internal/protected methods
6. Private methods
7. Companion object (always last)
8. Nested/inner classes

## Data Class Layout
1. Required properties first
2. Optional properties with defaults
3. Timestamps last
- Use `init` block for validation, computed properties via `get()`

## Formatting
- 4-space indentation, opening brace at end of line
- Long function signatures: break parameters, trailing comma
- Chained calls: each on new line with `.` indentation
- Use trailing commas for cleaner diffs

## Null Safety
- Use `?.` safe calls and `?:` elvis operator
- `requireNotNull()` for preconditions
- Avoid `!!` operator

## Value Objects
- Use Value Objects from `com.myrealtrip.common.values` instead of primitive types
- Available: `Email`, `PhoneNumber`, `Money`, `Rate`

## Error Handling
- Use `KnownException` for expected errors (no stack trace) -- validation, not found
- Use `BizRuntimeException` for business errors (with stack trace)
- Use `knownRequired` / `knownRequiredNotNull` instead of `require`
- Nullable return for expected absence: `fun findUser(id: Long): User?`

## Functional Programming
- Prefer immutability: `val`, immutable collections
- Use `asSequence()` for large collections with multiple operations
- Avoid over-functional chains -- break into clear steps if complex

## Scope Functions
- `let`: null checks (`value?.let { ... }`)
- `apply`: object configuration
- `also`: side effects (logging)

## Kotlin Idioms
- Use `when` for exhaustive matching on sealed classes
- Use extension functions for utility methods
- Use default & named arguments instead of overloads
- Use `sealed class` for restricted type hierarchies

## Anti-Patterns to Avoid
- Over-engineering (interfaces for single implementations, factories for simple cases)
- God classes (split by responsibility)
- Copy-paste code (extract reusable functions)
- Deep nesting (use early returns)
- Comments that explain what, not why

> For detailed examples, see skill: `kotlin-style`
