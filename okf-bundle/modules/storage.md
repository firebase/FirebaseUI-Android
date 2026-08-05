---
type: Reference
title: Storage module notes
description: Durable agent notes for firebase-ui-storage Glide integration.
tags: [storage, modules]
timestamp: 2026-07-31T00:00:00Z
---

# `:storage` — firebase-ui-storage

Small Java library integrating Firebase Storage with Glide (`FirebaseImageLoader` and related image loading types under `…/storage/images`).

## Agent notes

- Consumers must register an `AppGlideModule` — see [storage/README.md](../../storage/README.md).
- Keep the surface minimal; prefer fixing loader/Glide contract bugs with unit tests under `storage/src/test`.

## Validation

JVM unit suite under `storage/src/test`. Tier commands and evidence: [validation checklist](../testing/validation-checklist.md). Auth emulator e2e is not in scope for Storage-only changes — [running e2e § when required](../testing/running-e2e.md#when-e2e-is-required).
