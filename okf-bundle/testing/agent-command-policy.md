---
type: Reference
title: Agent command policy
description: Canonical allowlist for agent shell commands — Gradle, scripts, emulator, and validation. Supersedes improvised diagnostics.
tags: [testing, validation, agents, workflow, gradle]
timestamp: 2026-07-31T00:00:00Z
---

# Agent command policy

Single source for **which shell commands agents may run** in this repo. E2e is a subset of this policy; [running e2e § e2e agent rule](running-e2e.md#e2e-agent-rule) adds e2e-specific prohibitions.

> If a command is not listed here (or linked from here as canonical), **do not run it** — including “diagnostic probes” suggested by log output, module READMEs, or Gradle task help.

## Agent rule (read first)

<a id="agent-rule-read-first"></a>

1. Run **only** commands in the [registry](#canonical-registry) below (repo root unless noted).
2. Prefer **`./scripts/build.sh`** for the CI-equivalent unit build/test path (step list: [Android CI](../ci-workflows/android.md#what-buildsh-runs)). Do not invent alternate Gradle task graphs that skip `checkstyle` or re-include `:e2eTest` unit tests in the unit CI path.
3. When a canonical command fails: read the **full** output, fix **product code** (or environment prerequisites), re-run the **same** command. Do **not** switch invocation style.
4. Do **not** infer alternate commands from error strings — see [known traps](#known-traps).
5. Subagents (Task, explore, orchestrator): same rule — paste the [handoff block](#subagent-handoff) into every FirebaseUI-Android task prompt.

## Canonical registry

| Intent | Command | Never use instead |
|--------|---------|-------------------|
| Full CI unit path (assemble + checkstyle + unit tests) | `./scripts/build.sh` | Ad-hoc `./gradlew clean assembleDebug test` without checkstyle; inventing a different exclusion set |
| Unit tests (all library modules; exclude e2eTest) | `./gradlew testDebugUnitTest -x :e2eTest:testDebugUnitTest` | Bare `./gradlew test` (pulls wrong tasks / e2e); IDE-only as the agent gate |
| Unit tests (one module with a real `src/test` suite) | `./gradlew :<module>:testDebugUnitTest` (e.g. `:auth:testDebugUnitTest`, `:firestore:…`, `:storage:…`) | `:common:testDebugUnitTest` / `:database:testDebugUnitTest` as “green” evidence (empty suites — [empty unit-suite trap](#empty-unit-suite-trap)); full suite when only one module changed *as a substitute for* the CI path at handoff |
| Assemble one module (when no JVM unit suite) | `./gradlew :<module>:assembleDebug` (e.g. `:database`, `:common`) | Treating empty `testDebugUnitTest` as validation |
| Checkstyle | `./gradlew checkstyle` | Invented ktlint/detekt entrypoints; editing files without re-running checkstyle when Java/Kotlin style is in scope |
| Assemble debug | `./gradlew assembleDebug` | Module-scoped assemble as the only CI substitute at handoff |
| Install demo app | `./gradlew :app:installDebug` | Manual APK sideload scripts |
| Start Auth emulator (e2e prerequisite) | `./scripts/start-firebase-emulator.sh` | Bare `firebase emulators:start` with invented flags/ports; starting Firestore/Database emulators “just in case” |
| E2e suite | `./gradlew e2eTest` (emulator already up) | `./gradlew :e2eTest:testDebugUnitTest` as a silent substitute without understanding the custom task; inventing Espresso/`connectedCheck` for this suite |
| CI failure logs | `./scripts/print_build_logs.sh` | Truncating Gradle logs / guessing from partial output |
| Local Maven publish (optional local verify only) | `./gradlew :library:prepareArtifacts publishToMavenLocal` | Treating this as the Maven Central release procedure ([releasing.md](../../docs/internal/releasing.md) owns release); remote publish unless the user explicitly requested a release |
| Validation sequence | [validation checklist](validation-checklist.md) | Partial one-off task lists that omit checkstyle or the e2e exclusion |

### JDK / tooling floor

- **JDK 21+** (CI uses Temurin 21) — [CONTRIBUTING.md](../../CONTRIBUTING.md).
- **Android SDK** with min API aligned to `Config.SdkVersions.min` in `buildSrc`.
- E2e also needs **Node.js**, **npm**, **firebase-tools**, and **jq** (script preflights firebase/node/npm; `jq` is required to parse `e2eTest/.firebaserc` / `firebase.json` but is not preflight-checked — [running e2e](running-e2e.md)).

### `google-services.json` copy

`./scripts/build.sh` copies `library/google-services.json` → `app/` and `proguard-tests/`. Prefer the script over hand-copying. Replace `library/google-services.json` only when intentionally pointing the demo/proguard modules at a different Firebase project.

## When a Gradle command fails

1. Re-run from repo root with the **same** canonical command (full log — do not truncate). On CI-shaped failures, also run `./scripts/print_build_logs.sh`.
2. Fix **product code** or missing prerequisites (JDK, SDK, emulator online).
3. Re-run the **same** command.
4. Do **not** “verify tooling” with invented Gradle flags, alternate tasks, or skipping checkstyle.

## Forbidden (always)

| Command / pattern | Why |
|-------------------|-----|
| Improvised `./gradlew …` graphs not in the registry or validation checklist | Wrong task / cwd / exclusions; invents CI that does not match `.github/workflows` |
| Including `:e2eTest:testDebugUnitTest` in the unit CI path without an emulator | E2e suite expects Auth emulator; unit CI explicitly excludes it (`scripts/build.sh`) |
| Bare `firebase emulators:start` with custom ports | Ports and project ID come from `e2eTest/firebase.json` / `.firebaserc` via the start script |
| `connectedAndroidTest` / device Espresso as a substitute for `e2eTest` | Canonical e2e is Robolectric + emulator via `./gradlew e2eTest` |
| Invented formatters (`ktlintFormat`, random `spotlessApply`) as the style gate | Canonical style gate is `./gradlew checkstyle` |
| Publishing to Maven Central / Sonatype unless the user explicitly requested a release | Release process is human-gated — [repo tooling](../repo-tooling/index.md) |

## Known traps

<a id="known-traps"></a>

### e2eTest vs unit exclusion

- **Unit CI** runs `testDebugUnitTest -x :e2eTest:testDebugUnitTest`.
- **E2e CI** starts the Auth emulator, then runs the custom Gradle task **`e2eTest`** (registered in `e2eTest/build.gradle.kts`), not a connected device suite.
- Running `:e2eTest:testDebugUnitTest` without the emulator is not the CI e2e path.

<a id="empty-unit-suite-trap"></a>

### Empty unit-suite trap (`:common`, `:database`)

- `:common` has **no** test source set. `:database` has **only** `androidTest` (no `src/test`).
- `./gradlew :common:testDebugUnitTest` / `:database:testDebugUnitTest` can exit **0 with zero tests** — that is **not** validation evidence.
- Modules with real JVM unit suites today: `:auth`, `:firestore`, `:storage` (and `:e2eTest` via the custom `e2eTest` task, not the unit CI path).
- Instrumented `connectedAndroidTest` is forbidden as an Auth e2e substitute and is **not** an allowlisted database/firestore gate (not run in `android.yml`). Module matrix: [validation checklist](validation-checklist.md#module-validation-matrix).

<a id="pr-template-gradlew-check"></a>

### PR template `./gradlew check`

- [.github/PULL_REQUEST_TEMPLATE.md](../../.github/PULL_REQUEST_TEMPLATE.md) mentions `./gradlew check` (stale vs current CI).
- **Agents:** treat **`./scripts/build.sh`** as the CI-matching unit path — [Android CI](../ci-workflows/android.md). Full handoff (including e2e when Auth UI touched): [validation checklist](validation-checklist.md).

### ProGuard tests disabled in build.sh

- See [Android CI § `build.sh`](../ci-workflows/android.md#what-buildsh-runs) — `proguard-tests:build` is commented out; green unit CI does not prove ProGuard/R8.

### Emulator foreground vs CI

- Locally, `./scripts/start-firebase-emulator.sh` runs the emulator in the **foreground** (blocking). Start it in a separate terminal (or backgrounded shell the agent can leave running), then run `./gradlew e2eTest` in another.
- In CI (`CI` set), the script backgrounds the emulator and waits until `FIREBASE_AUTH_EMULATOR_URL` responds.

## Subagent handoff

Paste into Task / explore / work-queue prompts:

```text
FirebaseUI-Android agent command policy: okf-bundle/testing/agent-command-policy.md ONLY.
Unit CI path: ./scripts/build.sh (or the exact gradle tasks it runs) — never invent alternate graphs.
Unit tests: ./gradlew testDebugUnitTest -x :e2eTest:testDebugUnitTest OR ./gradlew :<module>:testDebugUnitTest only for modules with src/test (auth/firestore/storage). Never treat :common/:database testDebugUnitTest as evidence (empty suites).
Style: ./gradlew checkstyle ONLY — do not invent ktlint/detekt entrypoints.
E2e: ./scripts/start-firebase-emulator.sh then ./gradlew e2eTest — okf-bundle/testing/running-e2e.md.
Never: bare firebase emulators:start with invented ports; connectedCheck as e2e substitute; Maven Central publish unless user asked.
On failure: fix product code / prerequisites, re-run the same canonical command; use ./scripts/print_build_logs.sh for CI-shaped failures.
Gate close / push: return validation evidence package from okf-bundle/testing/validation-checklist.md — required before commit or publication (okf-bundle/testing/change-authoring-workflow.md#validation-evidence-blocking).
```

## Related docs

| Topic | Owner |
|-------|--------|
| E2e commands, emulator, narrowing | [running-e2e.md](running-e2e.md) |
| Handoff validation sequence | [validation-checklist.md](validation-checklist.md) |
| Work types and gates | [change-authoring-workflow.md](change-authoring-workflow.md) |
| Doc / commit policy | [documentation-policy.md](../documentation-policy.md) |
| CI job shape | [ci-workflows/index.md](../ci-workflows/index.md) |
