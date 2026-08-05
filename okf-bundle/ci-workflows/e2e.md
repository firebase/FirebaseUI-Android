---
type: Reference
title: E2E CI workflow
description: Shape and triage notes for .github/workflows/e2e_test.yml.
tags: [ci, e2e, emulator, auth]
timestamp: 2026-07-31T00:00:00Z
---

# E2E CI

Workflow: [.github/workflows/e2e_test.yml](../../.github/workflows/e2e_test.yml)

## Triggers

- `pull_request` only (not `push`)

## Job shape

| Step | Detail |
|------|--------|
| Runner | `ubuntu-latest`, 30m timeout |
| Checkout | `actions/checkout` (pinned) |
| Caches | Gradle caches/wrapper; `~/.cache/firebase/emulators` |
| Node | **20** |
| JDK | Temurin **21** |
| Tools | `npm i -g firebase-tools` |
| Emulator | `./scripts/start-firebase-emulator.sh` (`CI` set → background + readiness poll) |
| Tests | `./gradlew e2eTest` |
| On failure | `./scripts/print_build_logs.sh` |

## Agent notes

- Local procedure and suite semantics: [running e2e](../testing/running-e2e.md).
- Auth emulator only — do not invent multi-emulator CI.
- Unit CI excludes `:e2eTest`; this job is the canonical e2e gate for Auth UI changes.
