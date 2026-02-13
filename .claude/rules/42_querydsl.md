---
description: QueryDSL rules for type-safe queries using QuerydslRepositorySupport, fetch prefix, and @QueryProjection
globs: "*.{kt,kts}"
alwaysApply: false
---

# QueryDSL Rules

## Core Principles
- Extend **QuerydslRepositorySupport** for all QueryDSL repositories
- Use **`QueryRepository` suffix** for class names (e.g., `OrderQueryRepository`)
- Prefix all select methods with **`fetch`** (`fetchById`, `fetchAllByXxx`, `fetchPageByXxx`)
- Use **`@QueryProjection`** on DTO constructors -- avoid `Projections.constructor`
- Use QueryDSL JOINs instead of entity associations

## Method Naming
- Single result: `fetchXxx` (e.g., `fetchById`)
- List result: `fetchAllXxx` (e.g., `fetchAllByUserId`)
- Paged result: `fetchPageXxx` (e.g., `fetchPageByStatus`)
- Count: `fetchCountXxx`
- Exists: `existsXxx`

## Pagination
- Always accept **`Pageable`** -- never raw `page`/`size` parameters
- Use `applyPagination` with separate content and count queries

## SearchCondition
- Encapsulate complex filters in **`{Feature}SearchCondition`** data class
- Use **`SearchDates`** from common module for date range fields (not raw `startDate`/`endDate`)

## Dynamic Conditions
- Use **`QuerydslExpressions`** for null-safe dynamic filtering
- Available: `eq`, `gt/gte/lt/lte`, `contains`, `containsIgnoreCase`, `startsWith`, `in`, `dateBetween`, `dateTimeBetween`, `isTrue/isFalse`
- All methods return `null` when value is null/empty (ignored by QueryDSL `where()`)

> For detailed examples and code patterns, see skill: `querydsl`
