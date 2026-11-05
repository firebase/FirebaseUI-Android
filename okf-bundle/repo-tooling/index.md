---
type: Reference
title: Repo tooling
description: Gradle multi-module layout, versioning, and release/branch pointers for FirebaseUI-Android.
tags: [gradle, tooling, release]
timestamp: 2026-07-31T00:00:00Z
---

# Repo tooling

## Gradle layout

- Root build: `build.gradle.kts`, `settings.gradle`, `gradle.properties`, Wrapper via `./gradlew`
- Version catalog: `gradle/libs.versions.toml`
- Shared constants: `buildSrc/src/main/kotlin/Config.kt`
  - `Config.version` (e.g. `10.0.0-beta04`)
  - `Config.submodules` = `auth`, `common`, `firestore`, `database`, `storage`
  - `Config.SdkVersions` — compile / target / min
- Checkstyle: `library/quality/checkstyle.xml`
- Included modules: see root `settings.gradle` and [modules/index](../modules/index.md)

## Canonical scripts

| Script | Role |
|--------|------|
| `scripts/build.sh` | CI unit path — step list in [Android CI](../ci-workflows/android.md#what-buildsh-runs) |
| `scripts/start-firebase-emulator.sh` | Auth emulator for e2e — [running e2e](../testing/running-e2e.md) |
| `scripts/print_build_logs.sh` | CI failure log dump |

Command policy: [agent command policy](../testing/agent-command-policy.md). Note: PR template still says `./gradlew check`; agents use `./scripts/build.sh` — [PR template trap](../testing/agent-command-policy.md#pr-template-gradlew-check).

## Branching / release (pointers)

Human-maintained process docs (do not duplicate procedures here):

* [docs/internal/branching.md](../../docs/internal/branching.md)
* [docs/internal/releasing.md](../../docs/internal/releasing.md)

Agents must **not** publish to Maven Central / Sonatype unless the user explicitly requests release work. **Release procedure owner:** [docs/internal/releasing.md](../../docs/internal/releasing.md) (includes `:library:prepareArtifacts` for remote publish). Optional **local-only** verify (not the release procedure): `./gradlew :library:prepareArtifacts publishToMavenLocal`.

## Demo / config files

- Template Firebase config: `library/google-services.json` (copied by `build.sh` into `app/` and `proguard-tests/`)
- E2e emulator config: `e2eTest/firebase.json`, `e2eTest/.firebaserc`
