---
name: REST Docs
description: Spring REST Docs test writing rules, Field DSL, common response patterns, and AsciiDoc generation
last-verified: 2026-02-14
---

# REST Docs rules

## Overview

This document defines rules for writing Spring REST Docs tests using the project's `RestDocsSupport` base class, Field DSL, and common response patterns.

> **Key Principle**: Extend `RestDocsSupport`, mock the Facade, and use `DocsFieldType` and Field DSL for all documentation tests.

> **Note**: Full documentation is available in the `.docs/restdocs/` directory.
>
> | Document | Contents |
> |----------|----------|
> | `.docs/restdocs/01-overview.adoc` | Directory structure, core components, DocsFieldType, Field DSL |
> | `.docs/restdocs/02-writing-tests.adoc` | Step-by-step DocsTest writing guide, common response patterns |
> | `.docs/restdocs/03-examples.adoc` | HolidayControllerDocsTest analysis, AsciiDoc document authoring |
> | `.docs/restdocs/04-reference.adoc` | Best practices, troubleshooting, checklist, build commands |

## Core rules

### 1. Extend RestDocsSupport

All DocsTests must extend `RestDocsSupport`.

```kotlin
class MyControllerDocsTest : RestDocsSupport() {

    private val myFacade: MyFacade = mock()

    override fun initController(): Any = MyController(myFacade)
}
```

### 2. Mock the Facade

Controllers depend on Facades. Mock the Facade in your DocsTests.

```kotlin
// Good: Mock the Facade
private val holidayFacade: HolidayFacade = mock()
override fun initController(): Any = HolidayController(holidayFacade)

// Bad: Mock the Service directly
private val holidayService: HolidayService = mock()
```

### 3. Use DocsFieldType

| Type | JSON Type | Auto format |
|------|-----------|-------------|
| `STRING` | String | - |
| `NUMBER` | Number | - |
| `BOOLEAN` | Boolean | - |
| `ARRAY` | Array | - |
| `OBJECT` | Object | - |
| `DATE` | String | `yyyy-MM-dd` |
| `DATETIME` | String | `yyyy-MM-dd'T'HH:mm:ss` |
| `ENUM(Class::class)` | String | Lists enum values |

### 4. Field DSL (infix functions)

```kotlin
"fieldName" type TYPE means "description"
"fieldName" type TYPE means "description" isOptional true
"fieldName" type TYPE means "description" example "sample value"
"fieldName" type TYPE means "description" withDefaultValue "default"
```

### 5. Response strategy

| Strategy | Method | Use case |
|----------|--------|----------|
| **Full fields** | `responseCommonFields()` + `fields()` | Few and simple fields |
| **Subsection** | `responseCommonFieldsSubsection()` + `dataResponseFields()` | Many or complex fields (recommended) |

```kotlin
// Subsection strategy (recommended)
responseFields(*responseCommonFieldsSubsection()),
dataResponseFields(
    "id" type NUMBER means "ID",
    "name" type STRING means "이름",
)
```

| Response type | Subsection method | Full fields method |
|---------------|-------------------|--------------------|
| Single object | `responseCommonFieldsSubsection()` | `responseCommonFields()` |
| Array | `responseArrayCommonFieldsSubsection()` | `responseArrayCommonFields()` |
| String (DELETE, etc.) | - | `responseStringCommonFields()` |

### 6. Pagination response

```kotlin
mockMvc.perform(
    get("/api/resources/{id}", 1)
        .param("page", "0")
        .param("size", "20")
)
    .andExpect(status().isOk)
    .andDo(
        restDocs.document(
            queryParameters(
                *pageRequestFormat().toTypedArray(),
            ),
            responseFields(
                *responseArrayCommonFieldsSubsection(),
                subsectionWithPath("meta.pageInfo").type(JsonFieldType.OBJECT)
                    .optional().description("페이지 정보"),
            ),
            responseFields(
                beneathPath("meta.pageInfo").withSubsectionId("data.page"),
                *pageCommonFormat().toTypedArray(),
            ),
            dataResponseFields(
                "id" type NUMBER means "ID",
            ),
        )
    )
```

### 7. Test method naming

Test method names become snippet directory names. Use clear English names.

```kotlin
// Good: Meaningful snippet directory names
fun `get holidays by year`(): Unit { }       // -> get-holidays-by-year/
fun `create holiday`(): Unit { }             // -> create-holiday/

// Bad: Unclear directory names
fun `should return holidays`(): Unit { }     // -> should-return-holidays/
```

### 8. Write field descriptions in Korean

> **IMPORTANT**: Write field descriptions in REST Docs in Korean. This is a project convention.

```kotlin
// Good
"data.name" type STRING means "공휴일 이름"

// Bad
"data.name" type STRING means "holiday name"
```

## Quick reference

```kotlin
// Single object response
responseFields(*responseCommonFieldsSubsection()),
dataResponseFields("id" type NUMBER means "ID")

// Array response
responseFields(*responseArrayCommonFieldsSubsection()),
dataResponseFields("id" type NUMBER means "ID")

// String response (DELETE, etc.)
responseFields(*responseStringCommonFields())

// Request fields
requestFields(*fields("name" type STRING means "이름"))

// Path parameters
pathParameters(parameterWithName("id").description("ID"))
```

## Build

```bash
# Generate HTML docs (test -> snippets -> HTML)
./gradlew clean :modules:docs:docs

# Run a specific test only
./gradlew test --tests "com.myrealtrip.commonapiapp.docs.HolidayControllerDocsTest"
```

## Summary checklist

- [ ] Extend `RestDocsSupport`
- [ ] Mock the Facade (not Service)
- [ ] Use `DocsFieldType` (`DATE`, `DATETIME`, `ENUM`, etc.)
- [ ] Use Field DSL: `type`, `means` infix functions
- [ ] Write field descriptions in Korean
- [ ] Use `pageRequestFormat()` and `pageCommonFormat()` for pagination responses
- [ ] Use clear English test method names
- [ ] Mark optional fields with `isOptional true`
