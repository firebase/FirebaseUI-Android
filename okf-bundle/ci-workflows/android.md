---
type: Reference
title: Android CI workflow
description: Shape and triage notes for .github/workflows/android.yml.
tags: [ci, gradle, android]
timestamp: 2026-07-31T00:00:00Z
---

# Android CI

Workflow: [.github/workflows/android.yml](../../.github/workflows/android.yml)

## Triggers

- `pull_request`
- `push`

## Job shape

| Step | Detail |
|------|--------|
| Runner | `ubuntu-latest`, 30m timeout |
| Checkout | `actions/checkout` (pinned) |
| Cache | `~/.gradle/caches`, `~/.gradle/wrapper` keyed on Gradle files |
| JDK | Temurin **21** |
| Build | `./scripts/build.sh` |
| On failure | `./scripts/print_build_logs.sh` |

<a id="what-buildsh-runs"></a>

## What `build.sh` runs

Canonical owner for the CI unit-path step list. Script: [scripts/build.sh](../../scripts/build.sh).

1. Copy `library/google-services.json` → `app/` and `proguard-tests/`
2. `./gradlew --max-workers=2 clean`
3. `./gradlew --max-workers=2 assembleDebug`
4. `./gradlew --max-workers=2 checkstyle`
5. `./gradlew --max-workers=2 testDebugUnitTest -x :e2eTest:testDebugUnitTest`

`proguard-tests:build` is currently commented out (re-enable before release). Green Android CI does **not** prove ProGuard/R8 packaging.

## Agent notes

- Match this path locally with `./scripts/build.sh` — [agent command policy](../testing/agent-command-policy.md).
- Do not treat this job as e2e coverage; that is [e2e.md](e2e.md).
