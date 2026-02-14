# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language

- **Default**: Respond in professional, understandable, easy English
- **Korean**: If user asks to respond in Korean, use 존댓말 (formal/polite speech) as default

## Skills

> Before starting any task, **check if there is an appropriate skill available**. Use the Skill tool to invoke skills
> for
> common tasks.

## Editing Rules

- **Match file tone**: When editing or modifying existing files, always follow the file's existing tone, style, and formatting conventions. Do not introduce a different writing style into an established document.

## Markdown Front-Matter

> **IMPORTANT**: When creating or editing markdown files (`.md`), always include YAML front-matter at the top of the
> file.

```markdown
---
name: Document Title
description: Brief description of the document's purpose and contents
last-verified: 2026-02-14
---
```

| Field           | Required | Description                                                           |
|-----------------|----------|-----------------------------------------------------------------------|
| `name`          | Yes      | Document title                                                        |
| `description`   | Yes      | Brief description of purpose and contents                             |
| `last-verified` | Yes      | Date when the content was last verified to be accurate (`yyyy-MM-dd`) |

## Project Rules

All coding standards and guidelines are maintained in the `.claude/rules/` directory:

| File                                   | Description                                                                                     |
|----------------------------------------|-------------------------------------------------------------------------------------------------|
| **01-09: Code Quality & Language**     |                                                                                                 |
| `01_cleancode.md`                      | Clean code principles (KISS, DRY, readable code)                                                |
| `02_kotlin.md`                         | Kotlin best practices (nullability, immutability, idioms)                                       |
| `03_test.md`                           | Test generation rules (AssertJ, given-when-then, meaningful tests)                              |
| **10-19: API & Web Layer**             |                                                                                                 |
| `10_controller.md`                     | Controller design (RESTful URLs, /api/v1/ prefix, kebab-case, ApiResource, SearchCondition)     |
| `11_restdocs.md`                       | REST Docs rules (Field DSL, common response patterns, AsciiDoc generation)                      |
| `12_request-response.md`              | Request/Response DTO conventions (package structure, naming, JsonFormat)                         |
| **20-29: Spring Framework**            |                                                                                                 |
| `20_annotation-order.md`               | Annotation ordering (Framework → Lombok)                                                        |
| `21_transaction.md`                    | Transaction management (propagation, isolation, pitfalls)                                       |
| `22_coroutine.md`                      | Coroutine best practices (MDC propagation, dispatcher selection, retry, structured concurrency) |
| `23_async-event.md`                    | Async & event handling (@Async, @TransactionalEventListener, event design patterns)            |
| **30-39: Domain Modeling**             |                                                                                                 |
| `30_common-codes.md`                   | Common codes & enums (CommonCode interface, EnumType.STRING, categorization patterns)           |
| `31_datetime.md`                       | DateTime handling (UTC-first, KST conversion, common utilities)                                 |
| `32_domain-exception.md`               | Domain exception handling (feature Error enum, Exception hierarchy, Service-layer usage)        |
| **40-49: Database & Persistence**      |                                                                                                 |
| `40_sql.md`                            | SQL style guide (formatting, naming, no FK/index by default)                                    |
| `41_jpa.md`                            | JPA rules (no associations, unidirectional only, QueryDSL)                                      |
| `42_querydsl.md`                       | QueryDSL rules (QuerydslRepositorySupport, fetch prefix, @QueryProjection)                      |
| **50-59: Project Utilities**           |                                                                                                 |
| `50_common-module.md`                  | Common module usage (Value Objects, Exceptions, Utils)                                          |
| **90-99: Project Structure & Process** |                                                                                                 |
| `91_project-modules.md`                | Project modules & architecture                                                                  |
| `92_layer-architecture.md`             | Layer architecture (4-layer structure, DTO flow, DI rules)                                      |
| `95_git-strategy.md`                   | Git Flow branching strategy, Conventional Commits                                               |

Claude Code automatically reads rules from this directory.



