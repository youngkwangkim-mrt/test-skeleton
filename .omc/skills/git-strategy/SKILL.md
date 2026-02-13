---
name: git-strategy
description: Git Flow branching model, Conventional Commits format, and merge strategy
triggers:
  - git flow
  - branch
  - commit message
  - conventional commit
  - merge strategy
argument-hint: ""
---

# Git branching strategy

## Overview

This project follows the **Git Flow** branching model. All branching, merging, and release workflows must adhere to these rules.

> **Key Principle**: Use Git Flow branches, Conventional Commits, and merge commits for all integrations. Rebase only when updating feature branches from develop.

> **Note**: Use [git-flow-next](https://github.com/gittower/git-flow-next) for automated branch management.

## Branch structure

```
main (production)
 |
 +-- hotfix/xxx -------------- merge back to main + develop
 |
 develop (integration)
  |
  +-- feature/xxx ------------- merge back to develop
  |
  +-- release/x.x.x ---------- merge back to main + develop
  |
  +-- support/x.x ------------ long-term support for older versions
```

## Branch types

| Branch | Base | Merges Into | Naming | Purpose |
|--------|------|-------------|--------|---------|
| `main` | - | - | `main` | Production-ready code |
| `develop` | `main` | - | `develop` | Integration branch for features |
| `feature/*` | `develop` | `develop` | `feature/{description}` | New features and enhancements |
| `release/*` | `develop` | `main` + `develop` | `release/{version}` | Release preparation |
| `hotfix/*` | `main` | `main` + `develop` | `hotfix/{description}` | Critical production fixes |
| `support/*` | `main` | - | `support/{version}` | Long-term support for older versions |

## Branch naming convention

### Format

```
{type}/{description}
```

### Rules

| Rule | Example | Note |
|------|---------|------|
| Use lowercase | `feature/add-login` | Never `Feature/Add-Login` |
| Use hyphens as separator | `feature/user-authentication` | Never underscores or spaces |
| Keep it short and descriptive | `hotfix/fix-null-pointer` | Describe what, not how |
| Include Jira ticket when available | `feature/PROJ-123-add-login` | Ticket number before description |

### Examples

```bash
# Feature
feature/user-authentication
feature/PROJ-123-add-export-api

# Release
release/1.2.0

# Hotfix
hotfix/fix-payment-timeout
hotfix/PROJ-456-null-pointer

# Support
support/1.0
```

## Workflow

### Feature development

```bash
# 1. Start feature from develop
git flow feature start user-authentication

# 2. Work on feature (commit frequently)
git add . && git commit -m "feat: add user authentication"

# 3. Publish feature for collaboration (optional)
git flow feature publish user-authentication

# 4. Finish feature (merges into develop)
git flow feature finish user-authentication
```

### Release

```bash
# 1. Start release from develop
git flow release start 1.2.0

# 2. Final adjustments (version bump, changelog)
git commit -m "chore: bump version to 1.2.0"

# 3. Finish release (merges into main + develop, creates tag)
git flow release finish 1.2.0
```

### Hotfix

```bash
# 1. Start hotfix from main
git flow hotfix start fix-payment-timeout

# 2. Fix the issue
git commit -m "fix: resolve payment timeout issue"

# 3. Finish hotfix (merges into main + develop, creates tag)
git flow hotfix finish fix-payment-timeout
```

## Commit message convention

Follow [Conventional Commits](https://www.conventionalcommits.org/) format:

```
{type}({scope}): {description}

{body}

{footer}
```

### Types

| Type | Description | Example |
|------|-------------|---------|
| `feat` | New feature | `feat(auth): add JWT authentication` |
| `fix` | Bug fix | `fix(payment): resolve timeout issue` |
| `docs` | Documentation | `docs: update API documentation` |
| `style` | Formatting (no logic change) | `style: fix indentation` |
| `refactor` | Code refactoring | `refactor(user): extract validation logic` |
| `perf` | Performance improvement | `perf(query): optimize search query` |
| `test` | Add or fix tests | `test(auth): add login test cases` |
| `chore` | Build, CI, tooling | `chore: update gradle dependencies` |
| `build` | Build system changes | `build: upgrade spring boot to 4.0.2` |
| `ci` | CI/CD changes | `ci: add deployment workflow` |

### Rules

- Use imperative mood: "add" not "added" or "adds"
- Do not capitalize first letter after type
- No period at the end of subject line
- Keep subject line under 72 characters
- Use body to explain "what" and "why", not "how"

### Examples

```
feat(export): add Excel multi-sheet support

Add automatic sheet splitting when row count exceeds Excel limit.
Support MULTI_SHEET and EXCEPTION overflow strategies.

Closes PROJ-789
```

```
fix(cache): resolve cache eviction race condition

The two-tier cache (Caffeine + Redis) had a race condition
where L1 eviction could serve stale data before L2 update.
```

## Merge strategy

| Scenario | Strategy | Reason |
|----------|----------|--------|
| Feature to Develop | **Merge commit** | Preserve feature history |
| Release to Main | **Merge commit** | Clear release boundary |
| Release to Develop | **Merge commit** | Sync release fixes |
| Hotfix to Main | **Merge commit** | Clear hotfix boundary |
| Hotfix to Develop | **Merge commit** | Sync hotfix |
| Update feature from develop | **Rebase** | Keep feature history linear |

## Tag convention

| Format | Example | When |
|--------|---------|------|
| `v{major}.{minor}.{patch}` | `v1.2.0` | Release finish |
| `v{major}.{minor}.{patch}` | `v1.2.1` | Hotfix finish |

Follow [Semantic Versioning](https://semver.org/):

- **Major**: Breaking changes
- **Minor**: New features (backward compatible)
- **Patch**: Bug fixes (backward compatible)

## Protected branches

| Branch | Push | Force Push | Delete |
|--------|------|------------|--------|
| `main` | PR only | Prohibited | Prohibited |
| `develop` | PR only | Prohibited | Prohibited |
| `release/*` | Direct | Prohibited | After merge |
| `feature/*` | Direct | Allowed | After merge |
| `hotfix/*` | Direct | Allowed | After merge |

## Summary checklist

Before merging, verify:

- [ ] Branch follows naming convention (`{type}/{description}`)
- [ ] Commits follow Conventional Commits format
- [ ] Feature branches are based on `develop`
- [ ] Hotfix branches are based on `main`
- [ ] Release branches are tagged with semantic version
- [ ] No direct pushes to `main` or `develop`
- [ ] Branch is deleted after merge
