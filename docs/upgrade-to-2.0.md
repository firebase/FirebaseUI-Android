# Upgrading to FirebaseUI 2.0

This document outlines the _breaking_ changes made between versions 1.2.0 and 2.0.0 as well as
suggested upgrade paths. This is not a comprehensive list of all features or bug fixes, please
reference the release note for that information.

For a full, machine-generated compatibility report see the `docs/compat_reports` folder.

## Auth

* **Facebook and Twitter dependencies** - FirebaseUI no longer directly depends on the Facebook
  or Twitter SDKs. If you plan to enable either of these providers you should include the
  appropriate SDK in your app to avoid a crash. This saves space for developers only using
  Google/Email/Phone authentication.
* **Sign out and delete** - The `AuthUI#delete(Activity)` and `AuthUI#signOut(Activity)`
  methods have been removed, please use the versions that accept `FragmentActivity`.
* **Error codes** - `ResultCodes.RESULT_NO_NETWORK` has been removed. Instead you should use
  `IdpResponse.fromResultIntent(data)` to get the response from the `AuthUI` intent and then
  check if `IdpResponse#getErrorCode()` is `ErrorCodes.NO_NETWORK`.
* **Choosing providers** - `setProviders` has been deprecated, please use `setAvailableProviders`
  which now respects the order in which you provide them. The varargs version of `setProviders`
  has been removed completely.
* **Facebook and Google scopes** - setting scopes via `string` resources is no longer supported,
  please set scopes in code using `IdpConfig`.
* **Themes** - AppCompat theme properties such as `colorPrimary` and `colorAccent` are now used
  to style FirebaseUI automatically without any need for customization. Unless your auth UI
  needs a different theme than the rest of your app, please remove
  `AuthUI.SignInIntentBuilder#setTheme(int)` and its related xml theme from your auth intent
  builder and check to make sure that the auth UI has been themed correctly.
* **Smart Lock for passwords** - `setIsSmartLockEnabled` has added a two-argument overload.
  There are now separate flags for enabling the hint select and enabling
  the saving/retrieving of full credentials from the API. Setting the same value for each flag
  will emulate the previous single-flag behavior.

## Database

* **Change events** - the signature of `ChangeEventListener#onChildChanged` has been modified
  to include a `DataSnapshot` argument.
* **Snapshot parsing** - the `parseSnapshot` method of the adapters has been removed. Instead,
  you should pass a custom `SnapshotParser` to the adapter constructor.
* **Method visibility** - the `onDataChanged` method has changed from `protected` to `public`,
  which will require a change in your code if you are overriding this method.

## Storage

No breaking changes.
