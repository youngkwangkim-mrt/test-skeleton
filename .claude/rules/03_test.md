---
description: Test generation rules using AssertJ and Kotest with given-when-then structure
globs: "*.{kt,kts}"
alwaysApply: false
---

# Test generation rules

## Critical rules

- **Never modify production code to make tests pass** -- tests validate existing behavior
- **Always run and verify tests** after writing (`./gradlew test`)
- **No boilerplate tests** -- never write tests solely for coverage metrics
- Ask: "Would this test catch a real bug?" -- if no, do not write it

## Test format

- Use backtick method names: `` `should calculate total when discount applied` ``
- Explicit `: Unit` return type
- **given-when-then** structure with section comments
- Blank lines between sections

## Libraries

- **AssertJ** (primary): `assertThat`, `assertThatThrownBy`, fluent assertions
- **Kotest** (when simpler): `shouldBe`, data-driven tests, property-based testing

## Best practices

- Focus on business logic, edge cases, boundary conditions, error paths
- Do NOT test trivial getters/setters or framework behavior
- Use `@ParameterizedTest` or Kotest `withData` for multiple similar cases
- Group related scenarios when context setup is identical
- Use `@Nested` with `@DisplayName` for organizing related tests
- Mock only what is necessary -- avoid over-mocking
- Use `assertSoftly` when verifying multiple fields on one object

## Naming

- Pattern: `should [expected behavior] when [condition]`
- Alternative: `[method] - [scenario] - [expected result]`

> For detailed examples and patterns, see skill: `test-rules`
