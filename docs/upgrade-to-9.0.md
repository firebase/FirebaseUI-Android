# Upgrading to FirebaseUI 9.0

FirebaseUI version `9.0.0` has significant breaking API changes and also adopts new major versions
of many critical dependencies. Below is a description of each breaking change.

## All - Update to Firebase BoM 33

FirebaseUI now depends on the Firebase SDK at BoM major version `33.9.0`. You should update your
app to use the same major version to avoid possible compilation errors or crashes.

For more information on this SDK release see the
[Firebase Android SDK release notes](https://firebase.google.com/support/release-notes/android#bom_v33-9-0).

Release Notes for other BoM versions with breaking changes:
- [Firebase Android SDK BoM 32.0.0](https://firebase.google.com/support/release-notes/android#bom_v32-0-0)
- [Firebase Android SDK BoM 31.0.0](https://firebase.google.com/support/release-notes/android#bom_v31-0-0)
- [Firebase Android SDK BoM 30.0.0](https://firebase.google.com/support/release-notes/android#bom_v30-0-0)
- [Firebase Android SDK BoM 29.0.0](https://firebase.google.com/support/release-notes/android#bom_v29-0-0)

## Auth - Remove Smart Lock

[Smart Lock for Passwords](https://developers.google.com/identity/smartlock-passwords/android/overview)
, which was deprecated in 2022, is now removed from the Google Play Services Auth SDK
(`com.google.android.gms:play-services-auth`).
FirebaseUI Android has been updated to use [Credential Manager](https://developer.android.com/training/sign-in/passkeys)
instead.

Due to this change, some APIs have changed:

- The `AuthUI#setIsSmartLockEnabled(boolean enableCredentials)` method has been replaced with the new
 `setCredentialManagerEnabled(Boolean)` method.
- The `AuthUI#setIsSmartLockEnabled(boolean enableCredentials, boolean enableHints)` method has been
 removed with no replacement (for now).
- The `AuthUI#silentSignIn()` method has been removed with no replacement.

## Auth - (behavior change) new Email authentication flow

Versions 8.x and older of FirebaseUI relied on methods like `fetchSignInForEmail`, which
now fail with the introduction of
[Email Enumeration Protection](https://cloud.google.com/identity-platform/docs/admin/email-enumeration-protection).

Version 9.0 removed those methods and now shows a different flow for email sign in and sign up.

## Auth - Removed SafetyNet

[Firebase Auth v22.0.0](https://firebase.google.com/support/release-notes/android#auth_v22-0-0)
removed SafetyNet support for app verification during phone number authentication.
