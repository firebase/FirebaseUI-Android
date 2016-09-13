# Setting up FirebaseUI Database

The first step to using FirebaseUI is setting up dependencies.

## Setting up Dependencies

As a pre-requisite, ensure your application is configured for use with
Firebase: see the
[Firebase documentation](https://firebase.google.com/docs/android/setup).
Then, add the FirebaseUI auth library dependency. If your project uses
Gradle, add:

```groovy
dependencies {
  // ...
  compile 'com.firebaseui:firebase-ui-database:0.5.3'
}
```

If instead your project uses Maven, add:

```xml
<dependency>
  <groupId>com.firebaseui</groupId>
  <artifactId>firebase-ui-database</artifactId>
  <version>0.5.3</version>
</dependency>
```

## Next Steps

Check out the [usage docs](./usage.md) for next steps!
