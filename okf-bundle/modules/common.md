---
type: Reference
title: Common module notes
description: Durable agent notes for shared firebase-ui-common adapter primitives.
tags: [common, modules]
timestamp: 2026-07-31T00:00:00Z
---

# `:common` — firebase-ui-common

Published shared Java primitives used by Firestore and Realtime Database UI adapters (`POM_ARTIFACT_ID=firebase-ui-common`). Not a primary consumer README surface like auth/firestore/database/storage — treat it as a shared library dependency those modules consume.

## Key types

| Type | Role |
|------|------|
| `BaseObservableSnapshotArray` | Base observable snapshot list |
| `BaseCachingSnapshotParser` / `BaseSnapshotParser` | Parser bases |
| `BaseChangeEventListener` / `ChangeEventType` | Change-event plumbing |
| `Preconditions` | Shared precondition helpers |

## Validation

No `src/test` / `androidTest` today — empty-suite trap: [agent command policy](../testing/agent-command-policy.md#empty-unit-suite-trap). Commands and tiers: [validation checklist](../testing/validation-checklist.md) (module matrix).

**Module-specific:** changes here can break `:firestore` and `:database`. Prefer also running `./gradlew :firestore:testDebugUnitTest` (real JVM suite) when the shared API is consumed there. Database has no JVM unit suite — [database.md](database.md).
