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

Step 4 is **Java-only** (`include("**/*.java")`), so it inspects zero files in the Kotlin modules — [Kotlin blind spot](../testing/agent-command-policy.md#checkstyle-kotlin-blind-spot). Android Lint covers that gap and runs in its own workflow, **not** in `build.sh` — see below.

<a id="lint-workflow"></a>

## Android Lint (`lint.yml`)

Separate workflow, `pull_request` only, running `./gradlew --max-workers=2 lintAll`.

`lintAll` is registered in the root `build.gradle.kts` and gates all 10 modules that configure a `lint { }` block.

It is a separate workflow rather than a step in `build.sh` for two reasons. Lint measured **~4-5 minutes** on this repo — the `build` job went from 3-6 min to 8-11 min when it was inline — so running it in parallel roughly halves PR feedback time at about the same total runner cost, since the extra compile the lint job pays is the one `build.sh` stops paying (`lintAnalyze` depends on `compileDebugKotlin`, and there is no remote build cache: Develocity here is configured for build scans only). And under `set -e` an inline lint failure aborted the run **before** `testDebugUnitTest`, so one new finding cost you every test result for that run.

The workflow copies `library/google-services.json` into `app/` and `proguard-tests/` before running, because `lintAll` gates `:proguard-tests:lintRelease` and that module applies the `google-services` plugin.

`proguard-tests:build` is currently commented out (re-enable before release). Green Android CI does **not** prove ProGuard/R8 packaging.

## Agent notes

- Match this path locally with `./scripts/build.sh` — [agent command policy](../testing/agent-command-policy.md).
- Do not treat this job as e2e coverage; that is [e2e.md](e2e.md).
