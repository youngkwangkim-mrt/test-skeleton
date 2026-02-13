---
description: Project module structure, architecture, dependencies, and conventions for creating new modules
globs: "*.gradle.kts"
alwaysApply: false
---

# Project Modules & Architecture

## Module Structure
```
modules/
├── common/              # Codes, Exceptions, Values, Utils, Extensions
├── common-web/          # Filters, Interceptors, ExceptionHandler, ApiResource
├── test-support/        # Test fixtures, REST Docs support
├── domain/              # Entity, Repository, Service, Application, DTO
├── infrastructure/      # JPA Config, Cache, Redis, RestClient, Export, Slack
├── bootstrap/
│   ├── common-api-app/  # Common API server (Controller, Facade)
│   ├── skeleton-api-app/# API server
│   └── skeleton-worker-app/ # Worker server
└── docs/                # REST Docs generation
```

## Dependency Direction (unidirectional only)
- `bootstrap` -> `domain`, `infrastructure`, `common-web`
- `infrastructure` -> `domain`, `common`
- `domain` -> `common` (only)
- `common` depends on nothing
- Within domain: `DTO -> Entity` (Entity must NOT import DTO)

## Request Flow
```
Controller (bootstrap) -> Facade (bootstrap)
  -> QueryApplication / CommandApplication (domain)
    -> Service (domain)
      -> JpaRepository / QueryRepository (domain)
```

## Package Convention
- Domain: `com.myrealtrip.domain.{feature}/{dto,entity,repository,service,application}/`
- Bootstrap: `com.myrealtrip.{appname}/{api,dto/request,dto/response,facade,config}/`

## Module Naming
- `-app` suffix: Spring Boot executable (bootJar enabled)
- No suffix: Library module (jar only)

## Response Format
All APIs use `ApiResource<T>` wrapping with `status`, `meta`, `data` fields.

## Exception Types
| Exception | Usage | Log Level |
|-----------|-------|-----------|
| `KnownException` | Expected errors (validation, not found) | INFO |
| `BizRuntimeException` | Business errors (unrecoverable) | ERROR |
| `BizException` | Checked business exceptions | ERROR |

## DataSource Routing
- `embed/local`: H2 in-memory (single pool)
- `dev/test/stage/prod`: MySQL Master-Slave via `RoutingDataSource`
- `@Transactional(readOnly = true)` routes to Slave, otherwise Master

## Caching (Two-Tier)
- L1: Caffeine (local, 200 items, 30min TTL)
- L2: Redis (distributed, configurable TTL)
- Cache names: `SHORT_LIVED` (10min), `DEFAULT` (30min), `MID_LIVED` (1h), `LONG_LIVED` (24h)

## HTTP Client Pattern
Use `@HttpExchange` interfaces with `@ImportHttpServices` configuration.

> For detailed examples, see skill: `project-modules`
