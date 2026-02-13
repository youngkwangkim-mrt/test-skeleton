---
description: Git Flow branching model, Conventional Commits format, and merge strategy
alwaysApply: false
---

# Git Branching Strategy

## Branch Structure (Git Flow)
| Branch | Base | Merges Into | Purpose |
|--------|------|-------------|---------|
| `main` | - | - | Production-ready code |
| `develop` | `main` | - | Integration branch |
| `feature/*` | `develop` | `develop` | New features |
| `release/*` | `develop` | `main` + `develop` | Release preparation |
| `hotfix/*` | `main` | `main` + `develop` | Critical production fixes |
| `support/*` | `main` | - | Long-term support |

## Branch Naming
- Format: `{type}/{description}` (lowercase, hyphens)
- Include Jira ticket when available: `feature/PROJ-123-add-login`

## Conventional Commits
Format: `{type}({scope}): {description}`

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation |
| `style` | Formatting (no logic change) |
| `refactor` | Code refactoring |
| `perf` | Performance improvement |
| `test` | Add or fix tests |
| `chore` | Build, CI, tooling |
| `build` | Build system changes |
| `ci` | CI/CD changes |

Rules: imperative mood, no capitalize after type, no period, under 72 chars.

## Merge Strategy
- All integrations: **merge commit** (preserve history)
- Update feature from develop: **rebase** (keep linear)

## Release Process
- Update `projectVersion` in `gradle.properties` (remove `-SNAPSHOT` suffix)
- Tag format: `v{major}.{minor}.{patch}` (Semantic Versioning)

## Protected Branches
- `main` / `develop`: PR only, no force push, no delete
- `feature/*` / `hotfix/*`: direct push allowed, delete after merge

> For detailed examples, see skill: `git-strategy`
