---
type: Reference
title: Auth module notes
description: Durable agent notes for firebase-ui-auth (Compose, configuration DSL, MFA, Credential Manager, tests).
tags: [auth, compose, modules]
timestamp: 2026-07-31T00:00:00Z
---

# `:auth` — firebase-ui-auth

Primary Auth UI library. **v10+** is Kotlin + Jetpack Compose + Material 3 (breaking vs 9.x Views).

## Layout

| Path | Role |
|------|------|
| `com.firebase.ui.auth` | Core entrypoints (`FirebaseAuthUI`, flow/state types) |
| `…/configuration` | `authUIConfiguration {}` DSL, `AuthProvider.*`, theme, validators, string provider |
| `…/ui/screens` | Compose screens (`FirebaseAuthScreen`, email/phone/MFA, etc.) |
| `…/ui/components` | Shared Compose widgets |
| `…/ui/method_picker` | Provider picker |
| `…/credentialmanager` | Android Credential Manager / password credentials |
| `…/mfa` | MFA enrollment/challenge support |
| `…/data`, `…/util` | Supporting types/helpers |

Unit tests: `auth/src/test/…`. E2e: `e2eTest/src/test/…` — [running e2e](../testing/running-e2e.md).

## Public integration surfaces

Agents changing Auth should know both consumer APIs (do not break without migration notes):

1. **High-level** — `FirebaseAuthScreen` + `authUIConfiguration { }` (Compose-first).
2. **Low-level** — `AuthFlowController` for Activity-hosted flows.

Consumer setup skill (outside this repo’s product code): `.agents/skills/firebaseui-android-getting-started/SKILL.md`.

Human docs: [auth/README.md](../../auth/README.md), [docs/upgrade-to-10.0.md](../../docs/upgrade-to-10.0.md).

## Validation

Tier commands and handoff sequence: [validation checklist](../testing/validation-checklist.md). Auth UI / emulator path requires e2e — [running e2e § when required](../testing/running-e2e.md#when-e2e-is-required). Allowlist: [agent command policy](../testing/agent-command-policy.md).

## Architectural facts (durable)

- Configuration is Kotlin DSL (`AuthProvider.Email()`, etc.), not 9.x `IdpConfig` builders.
- Theming uses `AuthUITheme` / Material 3, not XML Auth themes as the primary path.
- State is reactive (`Flow`-oriented Auth state), not only `AuthStateListener` callbacks.
- Credential Manager integration lives under `credentialmanager/` — treat password-save/retrieve as Auth-critical surface.
- MFA (SMS/TOTP) has dedicated screens and e2e coverage (`MfaEnrollmentScreenTest`, `MfaChallengeScreenTest`, …).

## Related

* [Modules index](index.md)
* [E2e CI](../ci-workflows/e2e.md)
* [Change authoring](../testing/change-authoring-workflow.md)
