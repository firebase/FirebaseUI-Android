# Basic Usage of FirebaseUI for Authentication

Make sure you've already setup [FirebaseUI Auth](./setup.md) in your app!

Before invoking the FirebaseUI authentication flow, your app should check
whether a
[user is already signed in](https://firebase.google.com/docs/auth/android/manage-users#get_the_currently_signed-in_user) from a previous session:

```java
FirebaseAuth auth = FirebaseAuth.getInstance();
if (auth.getCurrentUser() != null) {
  // already signed in
} else {
  // not signed in
}
```

The entry point to the authentication flow is the
`com.firebase.ui.auth.AuthUI` class.
If your application uses the default `FirebaseApp` instance, an AuthUI
instance can be retrieved simply by calling `AuthUI.getInstance()`.
If an alternative app instance is required, call
`AuthUI.getInstance(app)` instead, passing the appropriate FirebaseApp instance.

## Sign in

If a user is not currently signed in, as can be determined by checking
`auth.getCurrentUser() != null`, where auth is the FirebaseAuth instance
associated with your FirebaseApp, then the sign-in process can be started by
creating a sign-in intent using `AuthUI.SignInIntentBuilder`. A builder instance
can be retrieved by calling `createSignInIntentBuilder()` on the retrieved
AuthUI instance.

The builder provides the following customization options for the authentication flow:

- The set of authentication providers can be specified.
- The terms of service URL for your app can be specified, which is included as
  a link in the small-print of the account creation step for new users. If no
  terms of service URL is provided, the associated small-print is omitted.

- A custom theme can be specified for the flow, which is applied to all the
  activities in the flow for consistent customization of colors and typography.

### 1. Start FirebaseUI Auth Activity

If no customization is required, and only email authentication is required, the sign-in flow
can be started as follows:

```java
startActivityForResult(
    // Get an instance of AuthUI based on the default app
    AuthUI.getInstance().createSignInIntentBuilder().build(),
    RC_SIGN_IN);
```

You can enable sign-in providers like Google Sign-In or Facebook Log In by calling the
`setProviders` method:

```java
startActivityForResult(
    AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setProviders(
            AuthUI.EMAIL_PROVIDER,
            AuthUI.GOOGLE_PROVIDER,
            AuthUI.FACEBOOK_PROVIDER)
        .build(),
    RC_SIGN_IN);
```

If a terms of service URL and a custom theme are required:

```java
startActivityForResult(
    AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setProviders(...)
        .setTosUrl("https://superapp.example.com/terms-of-service.html")
        .setTheme(R.style.SuperAppTheme)
        .build(),
    RC_SIGN_IN);
```

By default, FirebaseUI uses [Smart Lock for Passwords](https://developers.google.com/identity/smartlock-passwords/android/)
to store the user's credentials and automatically sign users into your app on subsequent attempts.
Using SmartLock is recommended to provide the best user experience, but in some cases you may want
to disable SmartLock for testing or development.  To disable SmartLock, you can use the
`setIsSmartLockEnabled` method when building your sign-in Intent:

```java
startActivityForResult(
    AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setIsSmartLockEnabled(false)
        .build(),
    RC_SIGN_IN);
```

It is often desirable to disable SmartLock in development but enable it in production. To achieve
this, you can use the `BuildConfig.DEBUG` flag to control SmartLock:

```java
startActivityForResult(
    AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
        .build(),
    RC_SIGN_IN);
```

### 2. Handle the Activity Result

The authentication flow provides only two response codes:
`Activity.RESULT_OK` if a user is signed in, and `Activity.RESULT_CANCELLED` if
sign in failed. No further information on failure is provided as it is not
typically useful; the only recourse for most apps if sign in fails is to ask
the user to sign in again later, or proceed with an anonymous account if
supported.

```java
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
   super.onActivityResult(requestCode, resultCode, data);
   if (requestCode == RC_SIGN_IN) {
     if (resultCode == RESULT_OK) {
       // user is signed in!
       startActivity(new Intent(this, WelcomeBackActivity.class));
       finish();
     } else {
       // user is not signed in. Maybe just wait for the user to press
       // "sign in" again, or show a message
     }
   }
 }
```

Alternatively, you can register a listener for authentication state changes;
see the
[Firebase Auth documentation](https://firebase.google.com/docs/auth/android/manage-users#get_the_currently_signed-in_user)
for more information.

## Sign out

With the integrations provided by AuthUI, signing out a user is a multi-stage process:

1. The user must be signed out of the FirebaseAuth instance.
2. Smart Lock for Passwords must be instructed to disable automatic sign-in, in
   order to prevent an automatic sign-in loop that prevents the user from
   switching accounts.
3. If the current user signed in using either Google or Facebook, the user must
   also be signed out using the associated API for that authentication method.
   This typically ensures that the user will not be automatically signed-in
   using the current account when using that authentication method again from
   the authentication method picker, which would also prevent the user from
   switching between accounts on the same provider.

In order to make this process easier, AuthUI provides a simple `signOut` method
to encapsulate this behavior. The method returns a `Task` which is marked
completed once all necessary sign-out operations are completed:

```java
public void onClick(View v) {
  if (v.getId() == R.id.sign_out) {
      AuthUI.getInstance()
          .signOut(this)
          .addOnCompleteListener(new OnCompleteListener<Void>() {
            public void onComplete(@NonNull Task<Void> task) {
              // user is now signed out
              startActivity(new Intent(MyActivity.this, SignInActivity.class));
              finish();
            }
          });
  }
}
```

### Authentication flow chart

The authentication flow implemented on Android is more complex than on other
platforms, due to the availability of Smart Lock for Passwords. It is
represented in the following diagram:

![FirebaseUI authentication flow on Android](/auth/flow.png)
