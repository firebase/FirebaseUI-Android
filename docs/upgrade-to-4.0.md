# Upgrading to FirebaseUI 4.0

This document outlines the key _breaking_ changes made between versions `3.3.1` and `4.0.0` as well
as suggested upgrade paths. This is not a comprehensive list of all features or bug fixes, please
reference the [release notes][release-notes] for that information.

For a full, machine-generated compatibility report see the `docs/compat_reports` folder.

## Auth

* Removed all previously `@Deprecated` methods on `AuthUI.IdpConfig.Builder`, `AuthUI.IdpConfig`,
  `IdpResponse`, and `AuthUI.SignInIntentBuilder`. Most of the deprecated methods have to do
  with configuring the FirebaseUI sign-in Intent and the replacements can be seen in the `README`.

## Realtime Database

* No breaking changes.

## Cloud Firestore

* Adopt the breaking changes from the `firebase-firestore` library version `16.0.0`. The primary
  breaking change is the removal of `QueryListenOptions`, which has been replaced by the
  `MetadataChanges` enum. See the [firebase release notes][firebase-0502] for more information.

## Cloud Storage

* No breaking changes.

[firebase-0502]: https://firebase.google.com/support/release-notes/android#20180502
[release-notes]: https://github.com/firebase/FirebaseUI-Android/releases/tag/4.0.0
