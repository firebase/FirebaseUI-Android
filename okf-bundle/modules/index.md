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
| `:lint`, `:internal:lint`, `:internal:lintchecks` | Custom lint detectors — [what each is for](#custom-lint-modules) |
| `buildSrc` | Shared `Config` (version, SDK levels, submodule list) |

<a id="custom-lint-modules"></a>

### Custom lint modules

Three separate modules, and the difference is not cosmetic:

| Module | Detector | Reaches |
|--------|----------|---------|
| `:lint` | `FirestoreRecyclerAdapterLifecycleDetector` (2 issues) | `:firestore` only, via `lintChecks(project(":lint"))` in `firestore/build.gradle.kts` |
| `:internal:lint` | `NonGlobalIdDetector` (`NonGlobalIdInLayout`) | `:internal:lintchecks` only |
| `:internal:lintchecks` | none — an empty AAR (`<manifest />` is its whole source) | itself |

`:internal:lintchecks` exists only to declare `lintChecks(project(":internal:lint"))`, and `:auth` takes it as a `debugImplementation` dependency. That does **not** propagate the detector: `lintChecks` applies a check to the module that declares it, while `lintPublish` is the configuration that ships one to consumers. So `NonGlobalIdInLayout` runs against an empty module and reaches nothing.

It is dead wiring rather than a missed bug — `:auth` has no `"@id/` references left in its layouts after the Compose rewrite, so the detector would report nothing today even if it were wired correctly. Whether `:internal:lintchecks` should exist at all is unresolved.

Both registries declare a `Vendor`; omitting one makes lint print a `does not specify a vendor` warning on every run.

## Cross-cutting note (v10 Auth)

FirebaseUI Auth **10.x** is a Compose rewrite (breaking vs 9.x Views). User migration: [docs/upgrade-to-10.0.md](../../docs/upgrade-to-10.0.md). Firestore/database/storage remain View-based; there is no active Compose migration work-queue for those modules unless one is opened later ([documentation policy § work queues](../documentation-policy.md#work-queue-documents)).
