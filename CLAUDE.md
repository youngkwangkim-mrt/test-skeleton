# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Skills

> Before starting any task, **check if there is an appropriate skill available**. Use the Skill tool to invoke skills for
common tasks.

## Language

- **Default**: Respond in professional, understandable, easy English
- **Korean**: If user asks to respond in Korean, use 존댓말 (formal/polite speech) as default

## Project Rules

All coding standards and guidelines are maintained in the `.claude/rules/` directory:

| File | Description |
|------|-------------|
| **01-09: Code Quality & Language** | |
| `01_cleancode.md` | Clean code principles (KISS, DRY, readable code) |
| `02_kotlin.md` | Kotlin best practices (nullability, immutability, idioms) |
| `03_test.md` | Test generation rules (AssertJ, given-when-then, meaningful tests) |
| **10-19: Spring Framework** | |
| `10_annotation-order.md` | Annotation ordering (Framework → Lombok) |
| `11_transaction.md` | Transaction management (propagation, isolation, pitfalls) |
| **20-29: Database & Persistence** | |
| `20_sql.md` | SQL style guide (formatting, naming, no FK/index by default) |
| `21_jpa.md` | JPA rules (no associations, unidirectional only, QueryDSL) |
| `22_querydsl.md` | QueryDSL rules (QuerydslRepositorySupport, fetch prefix, @QueryProjection) |
| **30-39: Project Utilities** | |
| `30_common-module.md` | Common module usage (Value Objects, Exceptions, Utils) |
| **90-99: Project Structure & Process** | |
| `90_project-convention.md` | Project conventions (ApiResource response format, DTO package convention) |
| `91_project-modules.md` | Project modules & architecture |
| `92_layer-architecture.md` | Layer architecture (4-layer structure, DTO flow, DI rules) |
| `95_git-strategy.md` | Git Flow branching strategy, Conventional Commits |

Claude Code automatically reads rules from this directory.

## Build Commands

```bash
# Build
./gradlew build

# Test
./gradlew test

# Test specific class
./gradlew test --tests "com.myrealtrip.common.utils.SomeTest"
```

## Documentation

Project documentation is maintained in the `.docs/` directory:

- `.docs/versioning-and-release.adoc` - Versioning and release guide
- `.docs/architecture/` - Architecture documentation
  - `00-overview.adoc` - Project overview
  - `01-module-dependency.adoc` - Module dependency structure
  - `02-request-lifecycle.adoc` - Request lifecycle
  - `03-error-handling.adoc` - Error handling strategy
  - `04-caching-strategy.adoc` - Caching strategy
  - `05-http-client-patterns.adoc` - HTTP client patterns
  - `06-cross-cutting-concerns.adoc` - Cross-cutting concerns (AOP, filters)
  - `07-api-response-format.adoc` - API response format and ResponseCode
  - `08-new-module-guide.adoc` - Guide for creating new modules

## Project Structure

- `modules/common` - Common utilities and shared code
- `modules/common-web` - Web common (Filter, Interceptor, Handler)
- `modules/domain` - Domain models, business rules, JPA Entity, Repository, Application services
- `modules/infrastructure` - Config, Cache, HTTP Client, external services
- `modules/bootstrap/*-app` - Spring Boot applications (Controller, Facade)
