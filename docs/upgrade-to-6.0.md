# Upgrading to FirebaseUI 6.0

FirebaseUI version `6.0.0` has no breaking API changes from version `5.1.0` but updates
critical dependencies to new major versions.

There are two major groups of changes:

 * Convert all Android Support Library dependencies to AndroidX or Jetpack dependencies. For
   information on migrating to AndroidX see [this guide][androidx-migrate].
 * Update all Firebase and Google Play services dependencies to their latest major versions. For
   information on changes included in these SDKs visit the [release notes][firebase-relnotes].


Below is a comprehensive list of all of the relevant dependencies for each module of FirebaseUI.


**Auth**

```
android.arch.lifecycle:extensions:1.1.1
androidx.browser:browser:1.0.0
androidx.cardview:cardview:1.0.0
androidx.constraintlayout:constraintlayout:1.1.3
androidx.legacy:legacy-support-v4:1.0.0
com.google.android.material:material:1.0.0
com.google.android.gms:play-services-auth:17.0.0
com.google.firebase:firebase-auth:19.0.0
```

**Common**

```
android.arch.lifecycle:runtime:1.1.1
android.arch.lifecycle:viewmodel:1.1.1
androidx.annotation:annotation:1.1.0
```

**Database**

```
androidx.legacy:legacy-support-v4:1.0.0
androidx.recyclerview:recyclerview:1.0.0
com.google.firebase:firebase-database:19.1.0
```

**Firestore**

```
androidx.legacy:legacy-support-v4:1.0.0
androidx.recyclerview:recyclerview:1.0.0
com.google.firebase:firebase-firestore:21.1.0
```

**Storage**

```
androidx.legacy:legacy-support-v4:1.0.0
com.google.firebase:firebase-storage:19.0.1
```

[androidx-migrate]: https://developer.android.com/jetpack/androidx/migrate
[firebase-relnotes]: https://firebase.google.com/support/release-notes/android
