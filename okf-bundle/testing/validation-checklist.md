---
type: Reference
title: Validation checklist
description: Canonical command sequence for validating FirebaseUI-Android changes and handoff.
tags: [testing, validation, gradle, checkstyle, e2e]
timestamp: 2026-07-31T00:00:00Z
---

# Validation checklist

Validation commands for development/handoff. Other docs/skills link here; do not restate.

**Agents:** [agent command policy](agent-command-policy.md) — only allowlisted invocations.

## When to run what

Work types and tiers: [change authoring workflow](change-authoring-workflow.md). Term ids: [iteration vocabulary](iteration-vocabulary.md).

| Work type | Scope | Shortcuts |
|-----------|-------|-----------|
| `gap-analysis` | Read APIs, module READMEs, upgrade guides | n/a |
| `baseline-capture` | Touched module unit suite (+ e2e if Auth) | **area-focused** tier |
| `implementation` | Module unit tests; e2e when Auth UI/emulator path changed | **unit-focused** tier; optional `--tests` narrowing for diagnosis |
| `independent-review` | Area checklist on [frozen tree](change-authoring-workflow.md#frozen-tree) | **area-focused**; no temporary narrowing left in place |
| `pre-merge-validation` | CI unit path + e2e when Auth in PR | **full** tier — once per branch |

## Build and unit tests

Repo root. Full CI unit path (what `build.sh` runs): [Android CI](../ci-workflows/android.md).

```bash
./scripts/build.sh
```

Module-scoped while iterating — **only modules with a real `src/test` suite** count as unit evidence:

```bash
./gradlew :auth:testDebugUnitTest
./gradlew :firestore:testDebugUnitTest
./gradlew :storage:testDebugUnitTest
```

<a id="module-validation-matrix"></a>

| Module | JVM `src/test`? | Unit-focused evidence | Area-focused / full |
|--------|-----------------|----------------------|---------------------|
| `:auth` | Yes | `:auth:testDebugUnitTest`; optional focused e2e — [running e2e](running-e2e.md) | Full auth unit + e2e; `./scripts/build.sh` at full |
| `:firestore`, `:storage` | Yes | `:<module>:testDebugUnitTest` | Same + `./scripts/build.sh` at full |
| `:database` | **No** (only `androidTest`) | `:database:assembleDebug` + checkstyle — **not** `:database:testDebugUnitTest` ([empty-suite trap](agent-command-policy.md#empty-unit-suite-trap)) | `./scripts/build.sh` |
| `:common` | **No** | `:common:assembleDebug` + checkstyle + `:firestore:testDebugUnitTest` when shared API consumed — **not** `:common:testDebugUnitTest` ([trap](agent-command-policy.md#empty-unit-suite-trap)) | `./scripts/build.sh` |

Instrumented `androidTest` (database/firestore) is **not** in CI or the agent allowlist — human/device only.

<a id="lint-and-formatting"></a>

## Lint and formatting

**Blocking before `implementation` handoff and on the frozen tree for `independent-review`** when Java/Kotlin style-relevant sources changed.

```bash
./gradlew checkstyle
```

Config: `library/quality/checkstyle.xml` (wired from root `build.gradle.kts`). There is **no** separate agent entrypoint for ktlint/detekt — do not invent one.

Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html), [Android Kotlin style](https://developer.android.com/kotlin/style-guide), and [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md) as described in [CONTRIBUTING.md](../../CONTRIBUTING.md).

## E2e

When Auth UI / `e2eTest` is in scope — [running e2e](running-e2e.md):

```bash
./scripts/start-firebase-emulator.sh   # separate terminal locally
./gradlew e2eTest
```

## Coverage

This repo does **not** currently configure Jacoco/Codecov (or equivalent) in CI. Do not invent coverage tasks. Prefer meaningful unit + e2e assertions over synthetic coverage gates until tooling is added and documented here.

## OKF bundle review

Before handoff, follow [OKF policy](../documentation-policy.md#okf-update-contract):

1. Update relevant `okf-bundle/modules/` docs with durable learnings.
2. Check `okf-bundle/testing/` for conflicts with verified behavior; fix drift.
3. Run independent scan for canonical ownership, DRY refs, link hygiene, durability.

<a id="validation-evidence-package"></a>

## Validation evidence package (blocking)

Before closing **`implementation_gate`**, **`review_gate`**, **`commit_gate`**, or publishing (`git push` / PR update), record evidence per [change authoring § validation evidence](change-authoring-workflow.md#validation-evidence-blocking). Minimum template:

```markdown
| Step        | Command                                                                 | Exit | Evidence                                      |
|-------------|-------------------------------------------------------------------------|------|-----------------------------------------------|
| unit CI     | ./scripts/build.sh                                                      | 0    | —                                             |
| module unit | ./gradlew :<module>:testDebugUnitTest                                   | 0    | N/N tests — only if module has `src/test`     |
| assemble    | ./gradlew :<module>:assembleDebug                                       | 0    | when module has no JVM unit suite             |
| checkstyle  | ./gradlew checkstyle                                                    | 0    | when style-relevant sources                   |
| e2e         | ./gradlew e2eTest                                                       | 0    | when Auth UI — /tmp/...log                    |
```

**History rewrite invalidates** prior rows — re-run and replace the table after amend/rebase.

## Handoff checklist

- [ ] `./scripts/build.sh` (or equivalent assemble + checkstyle + unit exclusion path) exit 0
- [ ] Module evidence per [module validation matrix](#module-validation-matrix)
- [ ] `./gradlew checkstyle` when style-relevant sources changed
- [ ] E2e green when Auth UI / `e2eTest` changed ([running e2e](running-e2e.md))
- [ ] [Validation evidence package](#validation-evidence-package) recorded
- [ ] OKF bundle reviewed/updated per § above
- [ ] Conventional Commit subject prepared ([documentation policy](../documentation-policy.md)); single-commit PR title will match

## Related docs

| Topic | Document |
|-------|----------|
| Allowlist | [agent-command-policy.md](agent-command-policy.md) |
| Change loop | [change-authoring-workflow.md](change-authoring-workflow.md) |
| E2e | [running-e2e.md](running-e2e.md) |
| CI | [ci-workflows/index.md](../ci-workflows/index.md) |
