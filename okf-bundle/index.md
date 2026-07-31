---
okf_version: '0.1'
---

# FirebaseUI-Android knowledge bundle

Agent-oriented knowledge for [firebase/FirebaseUI-Android](https://github.com/firebase/FirebaseUI-Android). Human-facing docs stay in `README.md`, `CONTRIBUTING.md`, module READMEs, and `docs/`.

- [Documentation/commit policy](/documentation-policy.md) — durable vs ephemeral, commits as documentation, PR titles, OKF consistency

# CI workflows

- [CI workflows](/ci-workflows/index.md) — GitHub Actions job shape (`android.yml`, `e2e_test.yml`), failure log triage

# Testing

- [Agent command policy](/testing/agent-command-policy.md) — allowlisted shell commands for agents (Gradle, scripts, emulator, validation)
- [Change authoring workflow](/testing/change-authoring-workflow.md) — verified product change loop (unit-focused → area-focused review → commit); [§ validation evidence (blocking)](/testing/change-authoring-workflow.md#validation-evidence-blocking)
- [Iteration vocabulary](/testing/iteration-vocabulary.md) — work type, tier, and queue field identifiers
- [Running e2e tests](/testing/running-e2e.md) — Auth emulator + `./gradlew e2eTest`
- [Validation checklist](/testing/validation-checklist.md) — assemble, checkstyle, unit tests, e2e

# Modules

- [Modules](/modules/index.md) — auth, firestore, database, storage, common, and supporting Gradle modules

# Repo tooling

- [Repo tooling](/repo-tooling/index.md) — Gradle layout, versioning (`Config.kt`), release/branch pointers
