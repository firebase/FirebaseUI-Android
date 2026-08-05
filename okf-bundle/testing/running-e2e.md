---
type: Reference
title: Running e2e tests
description: Canonical Auth emulator + Gradle e2eTest commands, environment, and diagnosis for FirebaseUI-Android.
tags: [testing, e2e, emulator, auth, gradle]
timestamp: 2026-07-31T00:00:00Z
---

# Running e2e tests

Canonical e2e runbook for this repo. **Shell allowlist:** [agent command policy](agent-command-policy.md). Do not restate the full allowlist here.

<a id="e2e-agent-rule"></a>

## E2e agent rule (read first)

1. Start the Auth emulator with **`./scripts/start-firebase-emulator.sh`** only.
2. Run the suite with **`./gradlew e2eTest`** only (custom task in `e2eTest/build.gradle.kts`).
3. Do **not** use `connectedAndroidTest`, Espresso device runs, or bare `firebase emulators:start` with invented ports as substitutes.
4. On failure: read the full Gradle output (and `./scripts/print_build_logs.sh` when useful), fix product/test code or prerequisites, re-run the **same** commands.

## What this suite is

| Property | Value |
|----------|--------|
| Module | `:e2eTest` |
| Runner | Robolectric + Compose UI testing (`test` source set) |
| Backend | Firebase **Auth** Emulator only |
| Project ID | From `e2eTest/.firebaserc` (`fake-project-id`) |
| Auth port | From `e2eTest/firebase.json` emulators.auth.port (`9099`) |
| Custom task | `e2eTest` — mirrors the debug unit-test classpath; `doNotTrackState` so it always runs |

Tests live under `e2eTest/src/test/java/com/firebase/ui/auth/ui/` — mostly `screens/` (email, phone, Google, MFA, reauth, credential linking, anonymous) plus `AccessibilityTest.kt` beside that package.

This suite does **not** require a real Firebase project. Demo app / Play Services provider setup for the sample app is separate ([modules/auth](../modules/auth.md), [CONTRIBUTING.md](../../CONTRIBUTING.md)).

## Prerequisites

```bash
# Once per machine (CI installs firebase-tools each job)
npm install -g firebase-tools
```

Also needed: `node`, `npm`, `jq` (used to read `e2eTest/.firebaserc` / `firebase.json`; the script preflights only `firebase` / `node` / `npm`), and a free Auth emulator port.

## Canonical local run

**Terminal A** (foreground emulator locally):

```bash
./scripts/start-firebase-emulator.sh
```

**Terminal B**:

```bash
./gradlew e2eTest
```

CI order matches [.github/workflows/e2e_test.yml](../../.github/workflows/e2e_test.yml): install firebase-tools → start script (background when `CI` is set) → `./gradlew e2eTest`.

## Focused runs (diagnosis only)

Gradle supports narrowing to a test class while iterating. Prefer this during **`unit-focused`** diagnosis; revert to the full `e2eTest` task before **`area-focused`** / **`review`** gate closure.

```bash
./gradlew e2eTest --tests 'com.firebase.ui.auth.ui.screens.EmailAuthScreenTest'
```

Never commit temporary `.only`-style filters or leave the suite narrowed for review evidence.

<a id="when-e2e-is-required"></a>

## When e2e is required

| Change surface | E2e required? |
|----------------|---------------|
| `auth/` Compose screens, flow controller, credential manager, MFA | **Yes** — area-focused and full tiers |
| `e2eTest/` itself | **Yes** |
| `firestore/` / `database/` / `storage/` / `common/` only | **No** — unit/androidTest for that module |
| Docs / OKF only | **No** |

## Diagnosis

1. Confirm emulator is up: start script succeeded; Auth URL responds (`FIREBASE_AUTH_EMULATOR_URL`, default `http://127.0.0.1:9099`).
2. Confirm prerequisites: script exits non-zero if `firebase` / `node` / `npm` are missing; ensure `jq` is installed too (used without a dedicated preflight check).
3. Re-run **`./gradlew e2eTest`** after product/test fixes — same command.
4. On CI-shaped failures: `./scripts/print_build_logs.sh`.

Do not “fix” emulator connectivity by inventing alternate ports or starting Firestore/Database emulators unless a future suite documents them as required.

## Related docs

| Topic | Document |
|-------|----------|
| Allowlisted commands | [agent-command-policy.md](agent-command-policy.md) |
| Handoff checklist | [validation-checklist.md](validation-checklist.md) |
| E2e CI job | [ci-workflows/e2e.md](../ci-workflows/e2e.md) |
| Auth module notes | [modules/auth.md](../modules/auth.md) |
