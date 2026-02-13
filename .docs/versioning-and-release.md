---
title: Versioning and Release Guide
description: Structured versioning and release process using Semantic Versioning, Git tags, GitHub milestones, and automated releases
last_verified: 2026-02-13
---

# Versioning and Release Guide

## Overview

This project follows a structured versioning and release process. The application version, Git tags, GitHub milestones, and GitHub releases are all kept in sync to maintain a clear and traceable release history.

## Version Format

All versions use plain [Semantic Versioning](https://semver.org/) **with** a `v` prefix:

```
vMAJOR.MINOR.PATCH
```

Examples:

```
v0.0.1
v0.1.0
v1.0.0
```

> **IMPORTANT**: Always use the `v` prefix. The correct format is `v0.1.0`, not `0.1.0`.

## Application Version

The application version (project version) is defined in `gradle.properties`:

```properties
projectVersion=0.0.1-SNAPSHOT
```

- During development, the version uses the `-SNAPSHOT` suffix (e.g., `0.1.0-SNAPSHOT`).
- On release, the version is set to the release version without the suffix (e.g., `0.1.0`).

File location:

```
/gradle.properties  -> projectVersion=x.x.x-SNAPSHOT
```

## Release Workflow

### Prerequisites

Before creating a release, ensure the following are aligned:

| Item | Requirement |
|------|-------------|
| `gradle.properties` | `projectVersion` matches the release version (without `-SNAPSHOT`) |
| Git Tag | Tag name matches the release version exactly (e.g., `v0.1.0`) |
| GitHub Milestone | Milestone name matches the release version without `v` prefix (e.g., `0.1.0`) |

### Step-by-Step Release Process

#### 1. Prepare the release version

Update `projectVersion` in `gradle.properties` to remove the `-SNAPSHOT` suffix:

```diff
-projectVersion=0.1.0-SNAPSHOT
+projectVersion=0.1.0
```

#### 2. Commit the version change

```bash
git add gradle.properties
git commit -m "release: v0.1.0"
```

#### 3. Create a Git tag

Create an annotated tag matching the release version:

```bash
git tag -a v0.1.0 -m "v0.1.0"
```

#### 4. Push the tag

```bash
git push origin v0.1.0
```

Pushing the tag triggers the automated release workflow.

#### 5. Prepare next development version

After tagging, bump the version to the next snapshot:

```diff
-projectVersion=0.1.0
+projectVersion=0.2.0-SNAPSHOT
```

```bash
git add gradle.properties
git commit -m "chore: prepare next development version 0.2.0-SNAPSHOT"
git push origin master
```

## Automated GitHub Release

When a tag matching the pattern `v[0-9]+\.[0-9]+\.[0-9]+` is pushed, the GitHub Actions workflow (`.github/workflows/release.yml`) automatically:

1. **Extracts the version** from the tag (strips `v` prefix) to find the matching GitHub milestone
2. **Generates a changelog** using [spring-io/github-changelog-generator](https://github.com/spring-io/github-changelog-generator) based on the milestone
3. **Appends a full changelog link** comparing the previous tag to the current tag
4. **Creates a GitHub Release** with the generated release notes

### Related Files

| File | Description |
|------|-------------|
| `.github/workflows/release.yml` | Release workflow definition (tag trigger, changelog generation, GitHub Release creation) |
| `.github/changelog-generator.yml` | Changelog section/label mapping configuration for [spring-io/github-changelog-generator](https://github.com/spring-io/github-changelog-generator) |

### Changelog Sections

Issues are categorized by label into the following sections:

| Section | Labels |
|---------|--------|
| Noteworthy Changes | `for: team-attention`, `type: blocker` |
| New Features | `type: feature`, `type: enhancement` |
| Bug Fixes | `type: bug` |
| Dependency Upgrades | `type: dependency-upgrade` |
| Improvements | `theme: datasource`, `theme: error-handling`, `theme: modularization`, `theme: observability`, `theme: performance`, `theme: testing` |
| Documentation | `type: documentation` |

> **Note**: Issues labeled `type: task` are excluded from the changelog.

### Tag Pattern

The release workflow is triggered only by tags that match the exact pattern:

```
v[0-9]+\.[0-9]+\.[0-9]+
```

Tags without the `v` prefix (e.g., `0.1.0`) or other formats will **not** trigger the release.

## GitHub Milestone

Each release must have a corresponding GitHub milestone with the same name as the version.

- Milestone name: `0.1.0` (without `v` prefix)
- All issues and pull requests included in the release should be assigned to this milestone
- Close the milestone after the release is published

The changelog generator reads closed issues from the milestone to build the release notes.

## Version Alignment Checklist

Use this checklist before every release:

- [ ] `gradle.properties` `projectVersion` set to release version (no `-SNAPSHOT`)
- [ ] GitHub milestone created with matching version name
- [ ] All relevant issues assigned to the milestone
- [ ] Milestone closed
- [ ] Git tag created with matching version (with `v` prefix)
- [ ] Tag pushed to remote (triggers automated release)
- [ ] GitHub Release created automatically
- [ ] `projectVersion` bumped to next `-SNAPSHOT` version after release

## Example Release History

| Version | Git Tag | Milestone | GitHub Release |
|---------|---------|-----------|----------------|
| v0.0.1 | `v0.0.1` | `0.0.1` | [v0.0.1](https://github.com/{repo}/releases/tag/v0.0.1) |
| v0.0.2 | `v0.0.2` | `0.0.2` | [v0.0.2](https://github.com/{repo}/releases/tag/v0.0.2) |
| v0.0.3 | `v0.0.3` | `0.0.3` | [v0.0.3](https://github.com/{repo}/releases/tag/v0.0.3) |
