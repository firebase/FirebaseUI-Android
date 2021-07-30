# Upgrading to FirebaseUI 8.0

FirebaseUI version `8.0.0` has significant breaking API changes and also adopts new major versions of many critical dependencies. Below is a description of each breaking change.

## All - Update to Java 8.0 and Android Gradle Plugin 7.0

In order to use FirebaseUI v8.0 or higher your app must use Java 8.0 language featuresa and version 7.x of the Android Gradle Plugin.

### Java 8

Add the following to your app's `build.gradle`

```groovy
android {
    ...
    // Configure only for each module that uses Java 8
    // language features (either in its source code or
    // through dependencies).
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    // For Kotlin projects
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

For more information on using Java 8 language features in your app, see the [Android documentation](https://developer.android.com/studio/write/java8-support).

### AGP 7

In your root `build.gradle` file make sure you are using AGP 7 or higher:

```groovy
buildscript {
  // ...
  dependencies {
    // ...
    classpath 'com.android.tools.build:gradle:7.0.0'
  }
}
```

This will require installing Gradle 7 and JDK 11 on your machine. For more information on this relase see the [Android documentation](https://developer.android.com/studio/releases/gradle-plugin?buildsystem=ndk-build#7-0-0).

## All - Update to Firebase BOM 28

FirebaseUI now depends on the Firebase SDK at BOM major version `28.0.0`. You should update your app to use the same major version to avoid possible compilation errors or crashes.

For more information on this SDK release see the [Firebase Android SDK release notes](https://firebase.google.com/support/release-notes/android#bom_v28-0-0).

## Auth - Transition to Material Compnents

FirebaseUI Authentication now depends on the Material Design UI components rather than the older AppCompat components. This will require two breaking changes for existing users:

First, you must define a theme with the following minimal properties:

  * `colorPrimary`
  * `colorPrimaryVariant`
  * `colorAccent`
  * `android:statusBarColor` (API > 21) or `colorPrimaryDark` (API < 21)

Second, you must explicitly pass a reference to this theme when starting FirebaseUI using `setTheme()`:

```java
Intent signInIntent = 
    AuthUI.getInstance(this).createSignInIntentBuilder()
        // ...
        .setTheme(R.style.AppTheme)
        .build())
```

For more information on how to build a Material theme, see the [Material Design documentation](https://material.io/resources/build-a-material-theme).

## Auth - Remove offensive terms

The offensive terms "whitelist" and "blacklist" have been removed from the FirebaseUI public API. Specifically the `IdpConfig.PhoneBuilder` class has changed:

**Before**
```java
IdpConfig phoneConfigWithAllowedCountries = new IdpConfig.PhoneBuilder()
        .setWhitelistedCountries(allowedCountries)
        .build();

IdpConfig phoneConfigWithBlockedCountries = new IdpConfig.PhoneBuilder()
        .setBlacklistedCountries(blockedCountries)
        .build();
```

**After**
```java
IdpConfig phoneConfigWithAllowedCountries = new IdpConfig.PhoneBuilder()
        .setAllowedCountries(allowedCountries)
        .build();

IdpConfig phoneConfigWithBlockedCountries = new IdpConfig.PhoneBuilder()
        .setBlockedCountries(blockedCountries)
        .build();
```

## Firestore - Upgrade to AndroidX Paging 3

The `FirestorePagingAdapter` class now depends on AndroidX Paging version 3, which has a new API.

To see the new FirebaseUI API, see the [firestore/README.md](../firestore/README.md) file for an example.

For general information on upgrading your app to Paging 3, see [the Android documentation](https://developer.android.com/topic/libraries/architecture/paging/v3-migration).

## Database - Upgrade to AndroidX PAging 3

The `FirebaseRecyclerPagingAdapter` class now depends on AndroidX Paging version 3, which has a new API.

To see the new FirebaseUI API, see the [database/README.md](../database/README.md) file for an example.

For general information on upgrading your app to Paging 3, see [the Android documentation](https://developer.android.com/topic/libraries/architecture/paging/v3-migration).
