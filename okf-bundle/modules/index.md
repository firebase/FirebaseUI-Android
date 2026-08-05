# Modules

Published library modules and supporting Gradle projects. Human-facing usage docs: module `README.md` files and [docs/](../../docs/).

## Published libraries

| Module | Artifact | Language / UI | OKF notes |
|--------|----------|---------------|-----------|
| `:auth` | `firebase-ui-auth` | Kotlin, Jetpack Compose (v10+) | [auth.md](auth.md) |
| `:firestore` | `firebase-ui-firestore` | Java, RecyclerView / Paging | [firestore.md](firestore.md) |
| `:database` | `firebase-ui-database` | Java, RecyclerView / ListView / Paging | [database.md](database.md) |
| `:storage` | `firebase-ui-storage` | Java, Glide | [storage.md](storage.md) |
| `:common` | `firebase-ui-common` | Java (shared adapter primitives) | [common.md](common.md) |

Version and SDK floors: `buildSrc/.../Config.kt` — [repo tooling](../repo-tooling/index.md).

## Supporting modules

| Module | Role |
|--------|------|
| `:library` | Umbrella / publish aggregation (`prepareArtifacts`) |
| `:app` | Demo app (Auth Compose sample) |
| `:e2eTest` | Auth emulator e2e (Robolectric + Compose UI test) |
| `:proguard-tests` | R8/ProGuard packaging checks (disabled in CI unit path — [Android CI](../ci-workflows/android.md)) |
| `:lint`, `:internal:lint`, `:internal:lintchecks` | Custom lint plumbing |
| `buildSrc` | Shared `Config` (version, SDK levels, submodule list) |

## Cross-cutting note (v10 Auth)

FirebaseUI Auth **10.x** is a Compose rewrite (breaking vs 9.x Views). User migration: [docs/upgrade-to-10.0.md](../../docs/upgrade-to-10.0.md). Firestore/database/storage remain View-based; there is no active Compose migration work-queue for those modules unless one is opened later ([documentation policy § work queues](../documentation-policy.md#work-queue-documents)).
