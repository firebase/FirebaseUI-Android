---
type: Reference
title: Database module notes
description: Durable agent notes for firebase-ui-database adapters and paging.
tags: [database, modules]
timestamp: 2026-07-31T00:00:00Z
---

# `:database` — firebase-ui-database

Java library binding Realtime Database references to RecyclerView / ListView (and AndroidX Paging).

## Key types

| Type | Role |
|------|------|
| `FirebaseRecyclerAdapter` | RecyclerView adapter |
| `FirebaseListAdapter` | ListView adapter |
| `FirebaseRecyclerOptions` / `FirebaseListOptions` | Options builders |
| `FirebaseArray` / `FirebaseIndexArray` | Observable arrays (including indexed keys/values) |
| `SnapshotParser` family | Snapshot → model mapping |
| `paging/*` | Paging adapters/sources |

Human docs: [database/README.md](../../database/README.md).

## Tests / validation

- No JVM `src/test`. Instrumented tests live under `database/src/androidTest` (human/device only — not CI / not allowlisted).
- Empty-suite trap: [agent command policy](../testing/agent-command-policy.md#empty-unit-suite-trap).
- Commands and tiers: [validation checklist](../testing/validation-checklist.md) (module matrix).

## Related

* Shared primitives: [common.md](common.md)
* Firestore counterpart: [firestore.md](firestore.md)
