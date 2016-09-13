# Setting up FirebaseUI Auth

The first step to using FirebaseUI is setting up dependencies then configuring
identity providers.

## Setting up Dependencies

As a pre-requisite, ensure your application is configured for use with
Firebase: see the
[Firebase documentation](https://firebase.google.com/docs/android/setup).
Then, add the FirebaseUI auth library dependency. If your project uses
Gradle, add:

```groovy
dependencies {
  // ...
  compile 'com.firebaseui:firebase-ui-auth:0.5.3'
}
```

If instead your project uses Maven, add:

```xml
<dependency>
  <groupId>com.firebaseui</groupId>
  <artifactId>firebase-ui-auth</artifactId>
  <version>0.5.3</version>
</dependency>
```

## Setting up Identity Providers

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

## Next Steps

Check out the [basic usage docs](./basic_usage.md) for next steps!
