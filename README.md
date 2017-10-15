# FirebaseUI for Android — UI Bindings for Firebase

[![Build Status](https://travis-ci.org/firebase/FirebaseUI-Android.svg?branch=master)](https://travis-ci.org/firebase/FirebaseUI-Android)

FirebaseUI is an open-source library for Android that allows you to
quickly connect common UI elements to [Firebase](https://firebase.google.com) APIs.

A compatible FirebaseUI client is also available for [iOS](https://github.com/firebase/firebaseui-ios).

## Table of Contents

  1. [Usage](#usage)
  1. [Installation](#installation)
  1. [Upgrading](#upgrading)
  1. [Dependencies](#dependencies)
  1. [Sample App](#sample-app)
  1. [Contributing](#contributing)

## Usage

FirebaseUI has separate modules for using Firebase Realtime Database, Cloud Firestore, 
Firebase Auth, and Cloud Storage. To get started, see the individual instructions for each module:

  * [firebase-ui-database](database/README.md)
  * [firebase-ui-firestore](firestore/README.md)
  * [firebase-ui-auth](auth/README.md)
  * [firebase-ui-storage](storage/README.md)

## Installation

FirebaseUI is published as a collection of libraries separated by the
Firebase API they target. Each FirebaseUI library has a transitive
dependency on the appropriate Firebase SDK so there is no need to include
those separately in your app.

In your `app/build.gradle` file add a dependency on one of the FirebaseUI
libraries.

```groovy
dependencies {
    // FirebaseUI for Firebase Realtime Database
    compile 'com.firebaseui:firebase-ui-database:3.0.0'
    
    // FirebaseUI for Cloud Firestore
    compile 'com.firebaseui:firebase-ui-firestore:3.0.0'

    // FirebaseUI for Firebase Auth
    compile 'com.firebaseui:firebase-ui-auth:0.4.0'

    // FirebaseUI for Cloud Storage
    compile 'com.firebaseui:firebase-ui-storage:3.0.0'
}
```

If you're including the `firebase-ui-auth` dependency, there's a little
[more setup](https://github.com/firebase/FirebaseUI-Android/tree/master/auth#configuration)
required.

After the project is synchronized, we're ready to start using Firebase functionality in our app.

## Upgrading

If you are using an old version of FirebaseUI and upgrading, please see the appropriate
migration guide:

  * [Upgrade from 2.3.0 to 3.x.x](./docs/upgrade-to-3.0.md)
  * [Upgrade from 1.2.0 to 2.x.x](./docs/upgrade-to-2.0.md)

## Dependencies

### Compatibility with Firebase / Google Play Services Libraries

FirebaseUI libraries have the following transitive dependencies on the Firebase SDK:
```
firebase-ui-auth
|--- com.google.firebase:firebase-auth
|--- com.google.android.gms:play-services-auth

firebase-ui-database
|--- com.google.firebase:firebase-database

firebase-ui-firestore
|--- com.google.firebase:firebase-firestore

firebase-ui-storage
|--- com.google.firebase:firebase-storage
```

Each version of FirebaseUI has dependency on a fixed version of these libraries, defined as the variable `firebase_version`
in `common/constants.gradle`.  If you are using any dependencies in your app of the form
`compile 'com.google.firebase:firebase-*:x.y.z'` or `compile 'com.google.android.gms:play-services-*:x.y.z'`
you need to make sure that you use the same version that your chosen version of FirebaseUI requires.

For convenience, here are some recent examples:

| FirebaseUI Version | Firebase/Play Services Version |
|--------------------|--------------------------------|
| 3.0.0              | 11.4.2                         |
| 2.4.0              | 11.4.0                         |
| 2.3.0              | 11.0.4                         |
| 2.2.0              | 11.0.4                         |
| 2.1.1              | 11.0.2                         |
| 2.0.1              | 11.0.1                         |
| 1.2.0              | 10.2.0                         |
| 1.1.1              | 10.0.0 or 10.0.1               |
| 1.0.1              | 10.0.0 or 10.0.1               |
| 1.0.0              | 9.8.0                          |


## Upgrading dependencies

If you would like to use a newer version of one of FirebaseUI's transitive dependencies, such
as Firebase, Play services, or the Android support libraries, you need to add explicit
`compile` declarations in your `build.gradle` for all of FirebaseUI's dependencies at the version
you want to use. For example if you want to use Play services/Firebase version `FOO` and support
libraries version `BAR` add the following extra lines for each FirebaseUI module you're using:

Auth:

```groovy
compile "com.google.firebase:firebase-auth:$FOO"
compile "com.google.android.gms:play-services-auth:$FOO"

compile "com.android.support:design:$BAR"
compile "com.android.support:customtabs:$BAR"
compile "com.android.support:cardview-v7:$BAR"
```

Realtime Database:

```groovy
compile "com.google.firebase:firebase-database:$FOO"

compile "com.android.support:recyclerview-v7:$BAR"
compile "com.android.support:support-v4:$BAR"
```

Firestore:

```groovy
compile "com.google.firebase:firebase-firestore:$FOO"

compile "com.android.support:recyclerview-v7:$BAR"
compile "com.android.support:support-v4:$BAR"
```

Storage:

```groovy
compile "com.google.firebase:firebase-storage:$FOO"

compile "com.android.support:appcompat-v7:$BAR"
compile "com.android.support:palette-v7:$BAR"
```

NOTE :
Starting version 25.4.0, support libraries are now available through [Google's Maven repository](https://developer.android.com/studio/build/dependencies.html#google-maven), so ensure that you have that added to your project's repositories.

Open the `build.gradle` file for your project and modify it as following,

```
allprojects {
    repositories {
        maven {
            url "https://maven.google.com"
        }
        jcenter()
    }
}
```

## Sample App

There is a sample app in the `app/` directory that demonstrates most
of the features of FirebaseUI. Load the project in Android Studio and
run it on your Android device to see a demonstration.

Before you can run the sample app, you must create a project in
the Firebase console. Add an Android app to the project, and copy
the generated google-services.json file into the `app/` directory.
Also enable [anonymous authentication](https://firebase.google.com/docs/auth/android/anonymous-auth)
for the Firebase project, since some components of the sample app
requires it.

If you encounter a version incompatibility error between Android Studio
and Gradle while trying to run the sample app, try disabling the Instant
Run feature of Android Studio. Alternatively, update Android Studio and
Gradle to their latest versions.

## Contributing

### Installing locally

You can download FirebaseUI and install it locally by cloning this
repository and running:

    ./gradlew :library:prepareArtifacts :library:publishAllToMavenLocal

###  Deployment

To deploy FirebaseUI to Bintray

  1. Set `BINTRAY_USER` and `BINTRAY_KEY` in your environment. You must
     be a member of the firebaseui Bintray organization.
  1. Run `./gradlew clean :library:prepareArtifacts :library:bintrayUploadAll`
  1. Go to the Bintray dashboard and click 'Publish'.
    1. In Bintray click the 'Maven Central' tab and publish the release.

### Tag a release on GitHub

* Ensure that all your changes are on master and that your local build is on master
* Ensure that the correct version number is in `common/constants.gradle`

### Contributor License Agreements

We'd love to accept your sample apps and patches! Before we can take them, we
have to jump a couple of legal hurdles.

Please fill out either the individual or corporate Contributor License Agreement
(CLA).

  * If you are an individual writing original source code and you're sure you
    own the intellectual property, then you'll need to sign an
    [individual CLA](https://developers.google.com/open-source/cla/individual).
  * If you work for a company that wants to allow you to contribute your work,
    then you'll need to sign a
    [corporate CLA](https://developers.google.com/open-source/cla/corporate).

Follow either of the two links above to access the appropriate CLA and
instructions for how to sign and return it. Once we receive it, we'll be able to
accept your pull requests.

### Contribution Process

1. Submit an issue describing your proposed change to the repo in question.
1. The repo owner will respond to your issue promptly.
1. If your proposed change is accepted, and you haven't already done so, sign a
   Contributor License Agreement (see details above).
1. Fork the desired repo, develop and test your code changes.
1. Ensure that your code adheres to the existing style of the library to which
   you are contributing.
1. Ensure that your code has an appropriate set of unit tests which all pass.
1. Submit a pull request and cc @puf or @samtstern
