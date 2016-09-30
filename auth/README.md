# FirebaseUI for Android — Auth

FirebaseUI is an open-source library that offers simple,
customizable UI bindings on top of the core
[Firebase](https://firebase.google.com) SDKs. It aims to eliminate boilerplate
code and promote best practices (both user experience and security) for
authentication.

A simple API is provided for drop-in user authentication which handles
the flow of signing in users with Email/Password,
Google Sign-In, and Facebook Login. It is built on top of the
[Firebase Auth](https://firebase.google.com/docs/auth).

The best practices embodied in FirebaseUI aim to maximize sign-in
and sign-up conversion for your app. It integrates with
[Smart Lock for Passwords](https://developers.google.com/identity/smartlock-passwords/android/)
to store and retrieve credentials, enabling automatic and single-tap sign-in to
your app for returning users. It also handles tricky use cases like
account recovery and account linking that are security sensitive and
difficult to implement correctly using the base APIs provided by Firebase Auth.

FirebaseUI auth can be easily customized to fit with the rest of your app's
visual style. As it is open source, you are also free to modify it to exactly
fit your preferred user experience.

Equivalent FirebaseUI auth libraries are also available for
[iOS](https://github.com/firebase/firebaseui-ios/)
and [Web](https://github.com/firebase/firebaseui-web/).

## Table of Content

1. [Configuration](#configuration)
2. [Usage instructions](#using-firebaseui-for-authentication)
3. [Customization](#ui-customization)

## Configuration

As a pre-requisite, ensure your application is configured for use with
Firebase: see the
[Firebase documentation](https://firebase.google.com/docs/android/setup).
Then, add the FirebaseUI auth library dependency. If your project uses
Gradle, add:

```groovy
dependencies {
  // ...
  compile 'com.firebaseui:firebase-ui-auth:0.6.0'
}
```

If instead your project uses Maven, add:

```xml
<dependency>
  <groupId>com.firebaseui</groupId>
  <artifactId>firebase-ui-auth</artifactId>
  <version>0.6.0</version>
</dependency>
```

### Identity provider configuration

In order to use either Google or Facebook accounts with your app, ensure that
these authentication methods are first configured in the Firebase console.

FirebaseUI client-side configuration for Google sign-in is then provided
automatically by the
[google-services gradle plugin](https://developers.google.com/android/guides/google-services-plugin).
If support for Facebook Sign-in is also required, define the
resource string `facebook_application_id` to match the application ID in
the [Facebook developer dashboard](https://developers.facebook.com):

```xml
<resources>
  <!-- ... -->
  <string name="facebook_application_id" translatable="false">APPID</string>
</resources>
```

## Using FirebaseUI for Authentication

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

### Sign in

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

#### Sign-in examples

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

#### Handling the sign-in response

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

### Sign out

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

![FirebaseUI authentication flow on Android](flow.png)

### UI customization

To provide customization of the visual style of the activities that implement
the flow, a new theme can be declared. Standard material design color
and typography properties will take effect as expected. For example, to define
a green theme:

```xml
<style name="GreenTheme" parent="FirebaseUI">
    <item name="colorPrimary">@color/material_green_500</item>
    <item name="colorPrimaryDark">@color/material_green_700</item>
    <item name="colorAccent">@color/material_purple_a700</item>
    <item name="colorControlNormal">@color/material_green_500</item>
    <item name="colorControlActivated">@color/material_lime_a700</item>
    <item name="colorControlHighlight">@color/material_green_a200</item>
    <item name="android:windowBackground">@color/material_green_50</item>
</style>
```

With associated colors:

```xml
<color name="material_green_50">#E8F5E9</color>
<color name="material_green_500">#4CAF50</color>
<color name="material_green_700">#388E3C</color>
<color name="material_green_a200">#69F0AE</color>
<color name="material_lime_a700">#AEEA00</color>
<color name="material_purple_a700">#AA00FF</color>
```

This would then be used in the construction of the sign-in intent:

```java
startActivityForResult(
    AuthUI.getInstance(this).createSignInIntentBuilder()
        // ...
        .setTheme(R.style.GreenTheme)
        .build());
```

Your application theme could also simply be used, rather than defining a new
one.

If you wish to change the string messages, the existing strings can be
easily overridden by name in your application. See
[the built-in strings.xml](src/main/res/values/strings.xml) and simply
redefine a string to change it, for example:

```java
<resources>
  <!-- was "Signing up..." -->
  <string name="progress_dialog_signing_up">Creating your shiny new account...</string>
</resources>
```

### OAuth Scope Customization

#### Google
By default, FirebaseUI requests the `email` and `profile` scopes when using Google Sign In. If you
would like to request additional scopes from the user, add a string array resource named 
`google_permissions` to your `strings.xml` file like this:

```xml
<!--
    For a list of all scopes, see:
    https://developers.google.com/identity/protocols/googlescopes
-->
<string-array name="google_permissions">
    <!-- Request permission to read the user's Google Drive files -->
    <item>https://www.googleapis.com/auth/drive.readonly</item>
</string-array>
```


#### Facebook

By default, FirebaseUI requests the `email` and `public_profile` permissions when initiating
Facebook Login.  If you would like to override these scopes, a string array resource named 
`facebook_permissions` to your `strings.xml` file like this:

```xml
<!--
    See:
    https://developers.facebook.com/docs/facebook-login/android
    https://developers.facebook.com/docs/facebook-login/permissions
-->
<string-array name="facebook_permissions">
    <!-- Request permission to know the user's birthday -->
    <item>user_birthday</item>
</string-array>
```
