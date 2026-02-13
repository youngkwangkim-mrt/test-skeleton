---
name: test-rules
description: Test generation rules using AssertJ and Kotest with given-when-then structure
triggers:
  - test
  - kotest
  - assertj
  - given-when-then
  - unit test
argument-hint: ""
---

# Test generation rules

## Overview

Tests validate existing behavior. They must never drive changes to production code.

> **Key Principle**: Tests validate behavior, not drive code changes. Never modify production code to make tests pass.

## Critical rules

> **IMPORTANT**: Never modify production code to make tests pass.
>
> * Tests validate existing behavior, not drive code changes.
> * If a test fails, fix the test logic or setup, NOT the production code.
> * Production code changes require deliberate feature requests or bug fixes.
> * Write tests that work with the current implementation as-is.

> **IMPORTANT**: Always run and verify tests.
>
> * Run tests immediately after writing them.
> * Ensure ALL tests pass before considering the task complete.
> * If tests fail, debug and fix the TEST implementation.
> * Command: `./gradlew test` or specific test class

> **IMPORTANT**: Do not create boilerplate tests for coverage.
>
> * Never write tests solely to increase code coverage metrics.
> * Avoid trivial tests that only verify getters, setters, or simple pass-through methods.
> * Do not create tests that provide no meaningful validation of business logic.
> * Coverage numbers without meaningful assertions are misleading and add maintenance burden.
> * Ask: "Would this test catch a real bug?" -- if no, do not write it.

> **Tip**: Generate meaningful tests.
>
> * Focus on testing actual business logic and behavior.
> * Test edge cases, boundary conditions, and error handling paths.
> * Verify complex state transitions and conditional logic.
> * Write tests that would catch real bugs if the implementation changes incorrectly.
> * Prioritize tests for critical paths and high-risk code sections.
> * Each test must answer: "What behavior am I validating, and why does it matter?"

## Test libraries

### AssertJ (primary)

AssertJ provides fluent, readable assertions. Use AssertJ for all assertions.

```kotlin
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
```

### Kotest (when simpler)

Use Kotest when it provides cleaner syntax, especially for:

* Property-based testing
* Data-driven tests
* Complex matchers

```kotlin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldStartWith
```

## Test method format

### Standard JUnit format

```kotlin
@Test
fun `should return user when valid id is provided`(): Unit {
    // given
    val userId = 1L
    val expectedUser = User(id = userId, name = "John")

    // when
    val result = userService.findById(userId)

    // then
    assertThat(result).isNotNull
    assertThat(result.id).isEqualTo(userId)
    assertThat(result.name).isEqualTo("John")
}
```

### Format rules

| Element | Rule | Example |
|---------|------|---------|
| Method name | Backticks with descriptive name | `` `should calculate total price correctly` `` |
| Return type | Explicit `: Unit` | `fun test(): Unit` |
| Structure | given-when-then with comments | See examples below |
| Spacing | Blank line between sections | Improves readability |

## Given-when-then pattern

### Structure

```kotlin
@Test
fun `descriptive test name explaining scenario and expectation`(): Unit {
    // given - setup test data and preconditions
    val input = createTestInput()
    val expected = createExpectedResult()

    // when - execute the action being tested
    val result = systemUnderTest.execute(input)

    // then - verify the outcome
    assertThat(result).isEqualTo(expected)
}
```

### When sections can be combined

For simple tests, sections can be minimal:

```kotlin
@Test
fun `should return empty list when no users exist`(): Unit {
    // given
    val repository = InMemoryUserRepository()

    // when
    val result = repository.findAll()

    // then
    assertThat(result).isEmpty()
}
```

## Grouping related tests

### When to group

Group related scenarios in a single test when:

* Testing similar behavior with slight variations.
* Context setup is identical.
* Tests would be repetitive if separated.

```kotlin
@Test
fun `should validate email format correctly`(): Unit {
    // given
    val validator = EmailValidator()

    // when & then - valid emails
    assertThat(validator.isValid("user@example.com")).isTrue
    assertThat(validator.isValid("user.name@example.co.kr")).isTrue
    assertThat(validator.isValid("user+tag@example.com")).isTrue

    // when & then - invalid emails
    assertThat(validator.isValid("invalid")).isFalse
    assertThat(validator.isValid("@example.com")).isFalse
    assertThat(validator.isValid("user@")).isFalse
    assertThat(validator.isValid("")).isFalse
}
```

### Parameterized tests (preferred for many cases)

```kotlin
@ParameterizedTest
@CsvSource(
    "user@example.com, true",
    "user.name@domain.co.kr, true",
    "invalid, false",
    "@example.com, false",
    "'', false"
)
fun `should validate email format`(email: String, expected: Boolean): Unit {
    // given
    val validator = EmailValidator()

    // when
    val result = validator.isValid(email)

    // then
    assertThat(result).isEqualTo(expected)
}
```

### Kotest data-driven tests

```kotlin
class EmailValidatorTest : FunSpec({
    context("email validation") {
        withData(
            "user@example.com" to true,
            "user.name@domain.co.kr" to true,
            "invalid" to false,
            "@example.com" to false
        ) { (email, expected) ->
            EmailValidator().isValid(email) shouldBe expected
        }
    }
})
```

## AssertJ best practices

### Basic assertions

```kotlin
// Equality
assertThat(actual).isEqualTo(expected)
assertThat(actual).isNotEqualTo(other)

// Null checks
assertThat(result).isNotNull
assertThat(result).isNull()

// Boolean
assertThat(condition).isTrue
assertThat(condition).isFalse

// Comparisons
assertThat(value).isGreaterThan(5)
assertThat(value).isLessThanOrEqualTo(10)
assertThat(value).isBetween(1, 100)
```

### String assertions

```kotlin
assertThat(text).isEqualTo("expected")
assertThat(text).contains("substring")
assertThat(text).startsWith("prefix")
assertThat(text).endsWith("suffix")
assertThat(text).matches("regex.*pattern")
assertThat(text).isBlank()
assertThat(text).isNotEmpty
assertThat(text).hasSize(10)
```

### Collection assertions

```kotlin
assertThat(list).isEmpty()
assertThat(list).isNotEmpty
assertThat(list).hasSize(3)
assertThat(list).contains(element)
assertThat(list).containsExactly(a, b, c)
assertThat(list).containsExactlyInAnyOrder(c, a, b)
assertThat(list).containsOnly(a, b)
assertThat(list).doesNotContain(x)

// Extract and verify
assertThat(users)
    .extracting("name")
    .containsExactly("Alice", "Bob", "Charlie")

assertThat(users)
    .filteredOn { it.isActive }
    .hasSize(2)
```

### Exception assertions

```kotlin
// Verify exception is thrown
assertThatThrownBy { service.process(invalidInput) }
    .isInstanceOf(IllegalArgumentException::class.java)
    .hasMessage("Input cannot be null")

// With message containing
assertThatThrownBy { service.findById(-1) }
    .isInstanceOf(BizRuntimeException::class.java)
    .hasMessageContaining("Invalid ID")

// Verify specific exception type
assertThatExceptionOfType(UserNotFoundException::class.java)
    .isThrownBy { service.findById(999) }
    .withMessage("User not found: 999")

// Verify no exception
assertThatCode { service.process(validInput) }
    .doesNotThrowAnyException()
```

### Object assertions

```kotlin
// Field by field comparison
assertThat(actual)
    .usingRecursiveComparison()
    .isEqualTo(expected)

// Ignoring fields
assertThat(actual)
    .usingRecursiveComparison()
    .ignoringFields("id", "createdAt")
    .isEqualTo(expected)

// Specific field checks
assertThat(user)
    .extracting("name", "email", "active")
    .containsExactly("John", "john@example.com", true)
```

### Soft assertions (multiple checks)

```kotlin
import org.assertj.core.api.SoftAssertions.assertSoftly

@Test
fun `should create user with all fields populated`(): Unit {
    // given & when
    val user = userService.create(request)

    // then - all assertions run even if some fail
    assertSoftly { softly ->
        softly.assertThat(user.id).isNotNull
        softly.assertThat(user.name).isEqualTo("John")
        softly.assertThat(user.email).isEqualTo("john@example.com")
        softly.assertThat(user.createdAt).isNotNull
        softly.assertThat(user.active).isTrue
    }
}
```

## Kotest matchers

### When to use Kotest

```kotlin
// Simple equality - cleaner with Kotest
result shouldBe expected
result shouldNotBe other

// Collections
list shouldHaveSize 3
list shouldContain element
list shouldContainExactly listOf(a, b, c)

// Strings
text shouldStartWith "prefix"
text shouldContain "substring"
text shouldMatch "regex.*"

// Nullability
result.shouldNotBeNull()
result.shouldBeNull()

// Types
result.shouldBeInstanceOf<User>()
```

### Kotest for exceptions

```kotlin
shouldThrow<IllegalArgumentException> {
    service.process(invalidInput)
}.message shouldBe "Input cannot be null"

shouldNotThrowAny {
    service.process(validInput)
}
```

## Test quality guidelines

### DO write tests that

* Validate actual business logic and behavior.
* Test edge cases and boundary conditions.
* Verify error handling paths.
* Cover complex state transitions.
* Would catch real bugs if implementation changes.

### DO NOT write tests that

* Only increase coverage numbers.
* Test trivial getters/setters.
* Verify simple pass-through methods.
* Provide no meaningful validation.
* Test framework behavior (Spring, Hibernate, etc.).

### Test design principles

| Principle | Description |
|-----------|-------------|
| **Keep tests simple** | Patterns are meant to simplify, not overcomplicate. Avoid excessive mocking or overly complex setups. |
| **Focus on behavior** | Test the "what," not the "how." Verify outcomes rather than implementation details. |
| **Avoid over-mocking** | Mock only what is necessary to keep tests focused and reliable. Excessive mocking leads to brittle tests. |
| **Maintain consistency** | Adopt a pattern that works well for the team and apply it consistently across the codebase. |

### Example: Meaningful vs boilerplate

```kotlin
// Bad - boilerplate test, no real value
@Test
fun `should get name`(): Unit {
    val user = User(name = "John")
    assertThat(user.name).isEqualTo("John")
}

// Good - tests actual business logic
@Test
fun `should calculate discount based on membership tier`(): Unit {
    // given
    val goldMember = User(tier = Tier.GOLD)
    val regularMember = User(tier = Tier.REGULAR)
    val order = Order(totalPrice = Money.of(10000))

    // when
    val goldDiscount = discountService.calculate(goldMember, order)
    val regularDiscount = discountService.calculate(regularMember, order)

    // then
    assertThat(goldDiscount).isEqualTo(Money.of(2000))  // 20%
    assertThat(regularDiscount).isEqualTo(Money.of(500)) // 5%
}
```

## Test organization

### Nested tests for context

```kotlin
@Nested
@DisplayName("UserService.create")
inner class CreateTests {

    @Test
    fun `should create user with valid input`(): Unit {
        // ...
    }

    @Test
    fun `should throw exception when email is duplicate`(): Unit {
        // ...
    }

    @Nested
    @DisplayName("when user is admin")
    inner class WhenAdmin {

        @Test
        fun `should assign admin role`(): Unit {
            // ...
        }
    }
}
```

### Test naming conventions

```kotlin
// Pattern: should [expected behavior] when [condition]
`should return empty list when no users exist`
`should throw exception when id is negative`
`should calculate correct total when discount applied`

// Pattern: [method name] - [scenario] - [expected result]
`findById - existing user - returns user`
`findById - non existing user - throws NotFoundException`
```

## Mocking

### Mockito-Kotlin

```kotlin
import org.mockito.kotlin.*

@Test
fun `should send email when user is created`(): Unit {
    // given
    val emailService = mock<EmailService>()
    val userRepository = mock<UserRepository> {
        on { save(any()) } doReturn User(id = 1L, name = "John")
    }
    val userService = UserService(userRepository, emailService)

    // when
    userService.create(CreateUserRequest(name = "John"))

    // then
    verify(emailService).sendWelcomeEmail(any())
}
```

### MockK (Kotlin native)

```kotlin
import io.mockk.*

@Test
fun `should send email when user is created`(): Unit {
    // given
    val emailService = mockk<EmailService>(relaxed = true)
    val userRepository = mockk<UserRepository> {
        every { save(any()) } returns User(id = 1L, name = "John")
    }
    val userService = UserService(userRepository, emailService)

    // when
    userService.create(CreateUserRequest(name = "John"))

    // then
    verify { emailService.sendWelcomeEmail(any()) }
}
```

## Summary checklist

Before submitting tests:

- [ ] Tests run and pass (`./gradlew test`)
- [ ] No production code was modified to make tests pass
- [ ] AssertJ or Kotest used for assertions
- [ ] Given-when-then structure followed
- [ ] Test names are descriptive
- [ ] Tests validate meaningful behavior (not boilerplate)
- [ ] Edge cases and error paths covered
- [ ] Similar scenarios grouped when appropriate
