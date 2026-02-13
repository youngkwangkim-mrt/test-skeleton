---
description: Layer structure, responsibilities, DTO flow, and conventions based on Holiday feature reference implementation
globs: "*.{java,kt,kts}"
alwaysApply: false
---

# Layer Architecture

## 4-Layer Structure
```
Bootstrap (Controller -> Facade)
  -> Domain Application (QueryApplication / CommandApplication)
    -> Domain Service (Business Logic)
      -> Domain Repository (JpaRepository / QueryRepository)
        -> Domain Entity
```
Upper layers depend on lower layers only. Reverse dependencies are prohibited.

## Layer 1: Bootstrap (HTTP Entry Point)
- **Controller**: Define HTTP endpoints, inject Facade only (no Service/Application/Repository)
  - Returns `ResponseEntity<ApiResource<T>>`
  - Converts API Request DTO to Domain Request DTO
- **Facade** (`@Component`): Convert between API DTO and Domain DTO, orchestrate Application calls
  - Injects QueryApplication and CommandApplication
  - KST conversion happens here (`.toKst()`)

## Layer 2: Domain Application (Orchestration)
- **QueryApplication** (`@Service`, `@Transactional(readOnly = true)`): Read operations, injects Service only
- **CommandApplication** (`@Service`, `@Transactional`): Write operations, injects Service only
- Application can inject Services from multiple domains for atomic cross-domain operations
- Application must NOT inject another Application

## Layer 3: Domain Service (Business Logic)
- `@Service`, no `@Transactional` (propagated from Application)
- Injects Repository only (JpaRepository, QueryRepository)
- Converts Entity to Domain DTO via `{Feature}Info.from(entity)`
- Other Service injection is allowed (same layer)

## Layer 4: Domain Repository (Persistence)
- **JpaRepository**: Simple CRUD, derived queries, `@Query`
- **QueryRepository**: QueryDSL, dynamic conditions, pagination (`fetch` prefix)

## Domain Entity & DTO
- **Entity**: Extends `BaseTimeEntity`, private setter, mutations via `update()`, factory via `create()`
- **Entity must NOT import DTO** -- dependency direction: DTO depends on Entity
- **Domain DTOs**: `{Feature}Info` (with `from(entity)` factory), `Create{Feature}Request`, `Update{Feature}Request`
- **Exception**: `{Feature}NotFoundException` extends `KnownException`

## DTO Flow
```
API Request -> Controller converts -> Domain Request
  -> Service creates/queries Entity -> {Feature}Info.from(entity)
    -> Facade converts -> API Response DTO -> ApiResource.success()
```

## Dependency Injection Rules
| Layer | Injects | Prohibited |
|-------|---------|------------|
| Controller | Facade only | Service, Application, Repository |
| Facade | Application only | Service, Repository |
| Application | Service only | Repository, other Application |
| Service | Repository | (other Services OK) |

## Transaction Rules
| Layer | Transaction |
|-------|-------------|
| Controller / Facade | None |
| QueryApplication | `@Transactional(readOnly = true)` |
| CommandApplication | `@Transactional` |
| Service | None (propagated from Application) |

## Cross-Domain Orchestration
- **Facade -> multiple Applications**: Separate transactions per call (independent reads)
- **Application -> multiple Services**: Single atomic transaction (write operations)

## Key Anti-Patterns
- Calling Service directly from Controller (bypasses Facade)
- Returning Entity as API response (expose via Info -> Dto)
- Business logic in Application (belongs in Service)
- `@Transactional` on Service (manage in Application only)
- `entity.toInfo()` method (reversed dependency -- use `Info.from(entity)`)
- Application injecting another Application (use Services instead)

> For detailed examples, see skill: `layer-architecture`
