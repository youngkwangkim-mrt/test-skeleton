---
description: JPA entity mapping rules - no associations by default, unidirectional only, use QueryDSL for joins
globs: "*.{kt,kts}"
alwaysApply: false
---

# JPA & Hibernate Rules

## Core Principles
- Extend **BaseEntity** (full auditing) or **BaseTimeEntity** (timestamps only)
- Always use `@Enumerated(EnumType.STRING)` -- never ORDINAL
- Always use `FetchType.LAZY` for all associations
- Always specify `@Table(name = "xxx")`

## Association Policy
- **Default**: Do NOT map entity associations -- store FK as plain ID value
- **Exception**: Unidirectional only, when absolutely necessary
- **Prohibited**: Bidirectional associations are strictly forbidden
- **Querying**: Use QueryDSL for joining related data

## Entity Structure
- `@Entity` + `@Table(name = "xxx")` + extend `BaseEntity`/`BaseTimeEntity`
- Use `val` for immutable fields (`id`), `var` for mutable fields
- Domain enums must implement `CommonCode`

## Fetch Strategies
- **LAZY**: Always use -- load on access
- **EAGER**: Never use -- loads unnecessary data
- Solve LazyInitializationException by fetching as DTO via QueryDSL
- Solve N+1 with QueryDSL JOIN

## Locking
- **Optimistic** (`@Version`): Low contention, read-heavy scenarios
- **Pessimistic** (`@Lock`): High contention, critical sections

## Configuration
- `ddl-auto: none` -- never auto-generate DDL in production
- `open-in-view: false` -- disable OSIV (mandatory)
- `default_batch_fetch_size: 500`, `batch_size: 500`

## Dirty Checking
- Managed entities are auto-tracked -- no explicit `save()` needed for updates within `@Transactional`

> For detailed examples and code patterns, see skill: `jpa-rules`
