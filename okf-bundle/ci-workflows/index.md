# CI workflows

GitHub Actions job shape and failure triage for FirebaseUI-Android.

## Jobs

* [Android CI](android.md) — `android.yml`: JDK 21, Gradle cache, `./scripts/build.sh`
* [E2E Tests](e2e.md) — `e2e_test.yml`: firebase-tools, Auth emulator, `./gradlew e2eTest`

## Shared dependencies

* [Agent command policy](../testing/agent-command-policy.md) — allowlisted commands for agents
* [Running e2e — e2e agent rule](../testing/running-e2e.md#e2e-agent-rule) — emulator + `e2eTest` only
* [Validation checklist](../testing/validation-checklist.md) — local handoff sequence matching CI

## Related

* [Running e2e tests](../testing/running-e2e.md) — local runbook
* [Repo tooling](../repo-tooling/index.md) — Gradle modules, versioning
