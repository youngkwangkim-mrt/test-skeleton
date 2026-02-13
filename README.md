---
name: Spring Boot Skeleton
description: Spring Boot 4.x + Kotlin 2.x multi-module skeleton project README
last-verified: 2026-02-14
---

# Spring Boot Skeleton

![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?logo=springboot&logoColor=white)

## Overview

Spring Boot 4.x and Kotlin 2.x based multi-module skeleton.
Modules are organized following dependency direction control and layer separation principles.

Clone this repository to start a new project.

### Tech stack

| Tech | Version | Notes |
|------|---------|-------|
| Spring Boot | 4.0.2 | |
| Kotlin | 2.3.10 | |
| Gradle | 9.2.1 | Version Catalog |
| JDK | 25 | Virtual Thread support |
| QueryDSL | 7.1 | Type-safe query builder |
| Redisson | 4.2.0 | Redis client (L2 cache, distributed lock) |

## Project structure

```
modules/
├── common/                     # Common utilities, exceptions, codes, Value Objects
├── common-web/                 # Web commons (Filter, Interceptor, Handler, ApiResource)
├── test-support/               # Test fixtures, REST Docs support
├── domain/                     # Domain models, business rules, JPA Entity, Repository
├── infrastructure/             # Config, Cache, HTTP Client, external services
├── bootstrap/                  # Spring Boot app modules
│   ├── common-api-app/         # Common API server (Controller, Facade)
│   ├── skeleton-api-app/       # API server
│   └── skeleton-worker-app/    # Worker server
└── docs/                       # REST Docs document generation
```

### Module dependency structure

```
┌──────────────────────────────────────────────────────────────┐
│                          Bootstrap                           │
│    (common-api-app, skeleton-api-app, skeleton-worker-app)   │
│              Controller → Facade → Application               │
├──────────────────────────────────────────────────────────────┤
│                         common-web                           │
│     (Filters, Interceptors, ExceptionHandler, ApiResource)   │
├────────────────────────────┬─────────────────────────────────┤
│       Infrastructure       │            Domain               │
│  (Config, Cache, Client)   │  (Entity, Repository, Service)  │
│  infrastructure → domain   │  Application (Query/Command)    │
├────────────────────────────┴─────────────────────────────────┤
│                           Common                             │
│         (Codes, Exceptions, Values, Utils, Extensions)       │
└──────────────────────────────────────────────────────────────┘

test-support ─── Test fixtures (depends on common, common-web)
docs ─────────── REST Docs document generation (Asciidoctor)
```

Layer roles:

- **Bootstrap**: Spring Boot executable modules. Controller → Facade (DTO conversion + orchestration)
- **common-web**: Web common infrastructure (filters, interceptors, exception handling, ApiResource response format)
- **Domain**: Domain models, business rules, Application (Query/Command), JPA Entity, Repository
- **Infrastructure**: JPA/QueryDSL Config, Cache, HTTP Client, Export, Slack
- **Common**: Shared code across all modules (Value Objects, exceptions, utilities)

## Module details

### common

Provides shared utilities and exception classes for all modules.

| Package | Description |
|---------|-------------|
| `codes` | ResponseCode, ErrorCode, SuccessCode |
| `exceptions` | KnownException, BizException, BizRuntimeException |
| `values` | Value Objects (Email, Money, PhoneNumber, Rate) |
| `utils/datetime` | Date/time Range, formatting, SearchDates |
| `utils/extensions` | String, Number, DateTime, UUID, Masking extensions |
| `utils/cipher` | AES, SEED encryption |
| `utils/codec` | URL encoding/decoding |
| `utils/coroutine` | MDC-preserving coroutine utilities (runBlockingWithMDC, asyncWithMDC, retry) |

[See detailed docs](.claude/rules/50_common-module.md)

### common-web

Provides web common infrastructure: filters, interceptors, and exception handling.

| Package | Description |
|---------|-------------|
| `filters` | ContentCachingFilter, AppTraceFilter (UUID v7 traceId) |
| `interceptors` | LogInterceptor, LogResponseBodyInterceptor |
| `handlers` | GlobalExceptionHandlerV2 |
| `response` | ApiResource standard response format (Status, Meta, PageResponse) |
| `aop` | LogTraceAspect, CheckIpAspect |

### domain

Defines domain models, business rules, JPA Entity, and Repository. Depends only on the common module.

Feature package structure:

```
domain/{feature}/
├── dto/                  # Info, Request, Exception
├── entity/               # JPA Entity
├── exception/            # {Feature}Error enum, {Feature}Exception
├── repository/           # JpaRepository, QueryRepository
├── service/              # Business logic
└── application/          # QueryApplication, CommandApplication
```

Key rules:

- `QueryApplication` — `@Transactional(readOnly = true)` → Slave DB routing
- `CommandApplication` — `@Transactional` → Master DB routing
- Entity → DTO conversion uses `{Feature}Info.from(entity)` pattern (DTO references Entity)
- No entity associations by default. Unidirectional only when necessary; bidirectional is prohibited

### infrastructure

Integrates with databases, caches, and external services.

| Feature | Description |
|---------|-------------|
| Persistence | JpaConfig, QuerydslConfig, DataSourceConfig (Master-Slave routing) |
| Cache | Caffeine (L1) + Redis (L2) two-tier cache |
| Redis | Distributed lock (RedisLockAspect), Cache Aspect |
| HTTP Client | RestClient + @HttpExchange logging |
| Slack | Slack SDK-based notifications (Kotlin DSL message builder) |
| Export | Excel/CSV file export (annotation-based, style presets) |

### bootstrap

Spring Boot executable modules. Modules ending with `-app` automatically apply the Spring Boot plugin.

| Module | Purpose |
|--------|---------|
| `common-api-app` | Common API server (Holiday API, data initialization) |
| `skeleton-api-app` | REST API server |
| `skeleton-worker-app` | Background Worker server |

### test-support

Provides test fixtures and REST Docs support.

| Component | Description |
|-----------|-------------|
| `IntegratedTestSupport` | Integration test base class |
| `EndPointTestSupport` | API endpoint test base class |
| `RestDocsSupport` | Spring REST Docs Kotlin DSL support |
| `TestTimeRunner` | Test execution time measurement utility |

### docs

Generates API documents based on Spring REST Docs.

- Generates HTML documents via Asciidoctor plugin
- Collects snippets from `common-api-app` and `skeleton-api-app` tests
- Generate documents with `./gradlew :modules:docs:docs`

## Quick start

### Build

```bash
./gradlew build
```

### Run

```bash
# Common API server
./gradlew :modules:bootstrap:common-api-app:bootRun

# Run with profile
./gradlew :modules:bootstrap:common-api-app:bootRun --args='--spring.profiles.active=embed'
```

### Test

```bash
# All tests
./gradlew test

# Specific module
./gradlew :modules:domain:test

# Specific class
./gradlew test --tests "com.myrealtrip.domain.holiday.HolidayServiceTest"

# Integrated test report
./gradlew testReport
```

### Generate docs

```bash
./gradlew :modules:docs:docs
```

## Settings

### gradle.properties

Defines project group and version.

```properties
projectGroup=com.myrealtrip
projectVersion=0.0.1-SNAPSHOT
```

### Version Catalog

Library versions are managed in `gradle/libs.versions.toml`.

- Libraries managed by Spring BOM omit version
- Only third-party libraries specify version explicitly

### Profile settings

| Profile | Database | DDL Mode | Purpose |
|---------|----------|----------|---------|
| `embed` | H2 In-Memory | `create-drop` | Local development |
| `local` | | | Local development |
| `dev`, `dev01`, `dev02` | Master-Slave | `validate` | Dev server |
| `test`, `test01`, `test02` | Master-Slave | `validate` | Test server |
| `stage` | Master-Slave | `none` | Staging |
| `prod` | Master-Slave | `none` | Production |

## New module creation

### Add a domain feature

Add a new feature to the Domain module.

```
modules/domain/src/main/kotlin/com/myrealtrip/domain/myfeature/
├── dto/
│   ├── MyFeatureInfo.kt               # Domain DTO
│   └── CreateMyFeatureRequest.kt      # Domain request
├── entity/
│   └── MyFeature.kt                   # JPA Entity (extends BaseEntity)
├── exception/
│   ├── MyFeatureError.kt              # Feature error code enum
│   └── MyFeatureException.kt          # Feature exception classes
├── repository/
│   ├── MyFeatureJpaRepository.kt      # Spring Data JPA
│   └── MyFeatureQueryRepository.kt    # QueryDSL (fetch prefix)
├── service/
│   └── MyFeatureService.kt            # Business logic
└── application/
    ├── MyFeatureQueryApplication.kt   # Query use cases (readOnly)
    └── MyFeatureCommandApplication.kt # Command use cases
```

### Add a bootstrap module

Modules with `-app` suffix automatically apply the Spring Boot plugin.

```
modules/bootstrap/my-api-app/
├── build.gradle.kts
└── src/main/kotlin/com/myrealtrip/myapiapp/
    ├── api/
    │   └── MyController.kt
    ├── dto/
    │   ├── request/              # API request DTOs
    │   └── response/             # API response DTOs
    ├── facade/
    │   └── MyFacade.kt
    └── MyApiApplication.kt
```

### Update settings.gradle.kts

Register the module in `settings.gradle.kts` after adding it.

```kotlin
include(
    // Foundation
    "modules:common",
    "modules:common-web",
    "modules:test-support",

    // Infrastructure
    "modules:infrastructure",

    // Core Business
    "modules:domain",

    // Runtime
    "modules:bootstrap:common-api-app",
    "modules:bootstrap:skeleton-api-app",
    "modules:bootstrap:skeleton-worker-app",
    "modules:bootstrap:my-api-app",        // Added

    // Documentation
    "modules:docs",
)
```

## Documentation

### Architecture docs

Detailed documents describing project architecture and design decisions.

| Document | Description |
|----------|-------------|
| [00-overview](.docs/architecture/00-overview.md) | Project purpose, tech stack, architecture overview |
| [01-module-dependency](.docs/architecture/01-module-dependency.md) | Multi-module dependency rules, module roles, package structure |
| [02-layer-architecture](.docs/architecture/02-layer-architecture.md) | 4-Layer Architecture structure, layer responsibilities, DI rules |
| [03-dto-flow](.docs/architecture/03-dto-flow.md) | DTO conversion strategy, data flow between layers |
| [04-datasource-routing](.docs/architecture/04-datasource-routing.md) | Master-Slave DataSource routing, LazyConnection strategy |
| [05-persistence-patterns](.docs/architecture/05-persistence-patterns.md) | JPA Entity policies, QueryDSL patterns, pagination |
| [06-api-response-format](.docs/architecture/06-api-response-format.md) | ApiResource response format, common response structure |
| [07-error-handling](.docs/architecture/07-error-handling.md) | Exception handling strategy, ErrorCode, KnownException hierarchy |
| [08-http-client-patterns](.docs/architecture/08-http-client-patterns.md) | @HttpExchange patterns, RestClient configuration |
| [09-caching-strategy](.docs/architecture/09-caching-strategy.md) | Two-tier cache (Caffeine L1 + Redis L2), cache naming conventions |
| [10-cross-cutting-concerns](.docs/architecture/10-cross-cutting-concerns.md) | AOP, Filter, Interceptor, cross-cutting concerns |
| [11-infrastructure-services](.docs/architecture/11-infrastructure-services.md) | Excel/CSV Export, Slack notifications, infrastructure services |

### REST Docs guide

Guides for writing Spring REST Docs tests and generating API documentation.

| Document | Description |
|----------|-------------|
| [01-overview](.docs/restdocs/01-overview.adoc) | REST Docs introduction, directory structure, core components |
| [02-writing-tests](.docs/restdocs/02-writing-tests.adoc) | DocsTest writing guide, Field DSL, common response patterns |
| [03-examples](.docs/restdocs/03-examples.adoc) | HolidayControllerDocsTest analysis, AsciiDoc document authoring |
| [04-reference](.docs/restdocs/04-reference.adoc) | Best Practices, troubleshooting, checklist, build commands |

### Technical docs

| Document | Description |
|----------|-------------|
| [excel-download-feature](.docs/technical/excel-download-feature.md) | Excel/CSV download feature details |
| [slack-notification-feature](.docs/technical/slack-notification-feature.md) | Slack notification feature details |

### Other docs

| Document | Description |
|----------|-------------|
| [versioning-and-release](.docs/versioning-and-release.md) | Semantic Versioning, Git tags, GitHub release workflow |
| [common module guide](.claude/rules/50_common-module.md) | Value Objects, exceptions, utilities detailed usage |
| [docs module README](modules/docs/README.md) | REST Docs build guide |

### Coding rules

Coding rules for maintaining code quality and consistency are managed in the `.claude/rules/` directory.

**01-09: Code Quality & Language**

| File | Description |
|------|-------------|
| [01_cleancode](.claude/rules/01_cleancode.md) | Clean code principles (KISS, DRY, readability first) |
| [02_kotlin](.claude/rules/02_kotlin.md) | Kotlin coding conventions (Null Safety, immutability, idioms) |
| [03_test](.claude/rules/03_test.md) | Test writing rules (AssertJ, given-when-then, meaningful tests) |

**10-19: API & Web Layer**

| File | Description |
|------|-------------|
| [10_controller](.claude/rules/10_controller.md) | Controller design (RESTful URL, /api/v1/, kebab-case, ApiResource) |
| [11_restdocs](.claude/rules/11_restdocs.md) | REST Docs rules (Field DSL, common response patterns) |
| [12_request-response](.claude/rules/12_request-response.md) | Request/Response DTO conventions (package structure, naming, JsonFormat) |

**20-29: Spring Framework**

| File | Description |
|------|-------------|
| [20_annotation-order](.claude/rules/20_annotation-order.md) | Annotation order (Framework → Lombok) |
| [21_transaction](.claude/rules/21_transaction.md) | Transaction management (propagation, isolation, Master-Slave routing) |
| [22_coroutine](.claude/rules/22_coroutine.md) | Coroutines (MDC propagation, dispatcher selection, retry, structured concurrency) |

**30-39: Domain Modeling**

| File | Description |
|------|-------------|
| [30_common-codes](.claude/rules/30_common-codes.md) | Common codes & Enum (CommonCode interface, EnumType.STRING) |
| [31_datetime](.claude/rules/31_datetime.md) | DateTime handling (UTC-first, KST conversion, common utilities) |
| [32_domain-exception](.claude/rules/32_domain-exception.md) | Domain exceptions (per-feature Error enum, Exception hierarchy) |

**40-49: Database & Persistence**

| File | Description |
|------|-------------|
| [40_sql](.claude/rules/40_sql.md) | SQL style guide (formatting, naming, no FK/index by default) |
| [41_jpa](.claude/rules/41_jpa.md) | JPA rules (no associations, unidirectional only, QueryDSL) |
| [42_querydsl](.claude/rules/42_querydsl.md) | QueryDSL rules (QuerydslRepositorySupport, fetch prefix, @QueryProjection) |

**50-59: Project Utilities**

| File | Description |
|------|-------------|
| [50_common-module](.claude/rules/50_common-module.md) | Common module usage (Value Objects, exceptions, utilities) |

**90-99: Project Structure & Process**

| File | Description |
|------|-------------|
| [91_project-modules](.claude/rules/91_project-modules.md) | Project module structure & architecture |
| [92_layer-architecture](.claude/rules/92_layer-architecture.md) | 4-Layer architecture (DTO flow, DI rules) |
| [95_git-strategy](.claude/rules/95_git-strategy.md) | Git Flow branching strategy, Conventional Commits |

## Version management

Follows [Semantic Versioning](https://semver.org/) (`vMAJOR.MINOR.PATCH`), managed in `gradle.properties`.

Release workflow:

1. Remove `-SNAPSHOT` from `gradle.properties`
2. `git tag -a v0.1.0 -m "v0.1.0"` → `git push origin v0.1.0`
3. GitHub Actions automatically creates a Release
4. Restore `-SNAPSHOT` for next development version

See [Versioning and Release Guide](.docs/versioning-and-release.md) for details.

## Git branching strategy

Follows [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/) with [Conventional Commits](https://www.conventionalcommits.org/) format.

| Branch type | Naming | Purpose |
|-------------|--------|---------|
| `main` | `main` | Production code |
| `develop` | `develop` | Integration branch |
| `feature/*` | `feature/{description}` | New features |
| `release/*` | `release/{version}` | Release preparation |
| `hotfix/*` | `hotfix/{description}` | Critical production fixes |

Commit message format: `{type}({scope}): {description}`

Key types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `build`, `ci`

See [Git Strategy](.claude/rules/95_git-strategy.md) for details.
