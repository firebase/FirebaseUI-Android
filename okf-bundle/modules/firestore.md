---
type: Reference
title: Firestore module notes
description: Durable agent notes for firebase-ui-firestore adapters and paging.
tags: [firestore, modules]
timestamp: 2026-07-31T00:00:00Z
---

# `:firestore` — firebase-ui-firestore

Java library binding Cloud Firestore queries to RecyclerView (and AndroidX Paging).

## Key types

| Type | Role |
|------|------|
| `FirestoreRecyclerAdapter` | Lifecycle-aware RecyclerView adapter |
| `FirestoreRecyclerOptions` | Query/options builder for the adapter |
| `FirestoreArray` / `ObservableSnapshotArray` | Observable snapshot list |
| `SnapshotParser` / `ClassSnapshotParser` / `CachingSnapshotParser` | Document → model mapping |
| `paging/*` (`FirestorePagingAdapter`, `FirestorePagingSource`, …) | AndroidX Paging integration |

Human docs: [firestore/README.md](../../firestore/README.md).

## Tests / validation

- JVM unit suite: `firestore/src/test`
- Instrumented: `firestore/src/androidTest` — human/device only (not CI / not allowlisted)
- Tier commands and evidence: [validation checklist](../testing/validation-checklist.md)
- Auth emulator e2e is **not** required for Firestore-only changes — [running e2e § when required](../testing/running-e2e.md#when-e2e-is-required)

## Related

* Shared adapter primitives: [common.md](common.md)
* Parallel Realtime Database adapters: [database.md](database.md)
