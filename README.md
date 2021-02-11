# FirebaseUI for Android — UI Bindings for Firebase

[![FirebaseOpensource.com](https://img.shields.io/badge/Docs-firebaseopensource.com-orange.svg)](
https://firebaseopensource.com/projects/firebase/firebaseui-android
)
[![Actions Status][gh-actions-badge]][gh-actions]

FirebaseUI is an open-source library for Android that allows you to
quickly connect common UI elements to [Firebase](https://firebase.google.com) APIs.

A compatible FirebaseUI client is also available for [iOS](https://github.com/firebase/firebaseui-ios).

## Table of contents

1. [Usage](#usage)
1. [Installation](#installation)
   1. [Upgrading](#upgrading)
1. [Dependencies](#dependencies)
   1. [Compatibility](#compatibility-with-firebase--google-play-services-libraries)
   1. [Upgrading dependencies](#upgrading-dependencies)
1. [Sample App](#sample-app)
1. [Snapshot Builds](#snapshot-builds)
1. [Contributing](#contributing)
   1. [Installing](#installing-locally)
   1. [License agreements](#contributor-license-agreements)
   1. [Process](#contribution-process)

## Usage

FirebaseUI has separate modules for using Firebase Realtime Database, Cloud Firestore,
Firebase Auth, and Cloud Storage. To get started, see the individual instructions for each module:

* [FirebaseUI Auth](auth/README.md)
* [FirebaseUI Firestore](firestore/README.md)
* [FirebaseUI Database](database/README.md)
* [FirebaseUI Storage](storage/README.md)

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
    implementation 'com.firebaseui:firebase-ui-database:7.1.1'

    // FirebaseUI for Cloud Firestore
    implementation 'com.firebaseui:firebase-ui-firestore:7.1.1'

    // FirebaseUI for Firebase Auth
    implementation 'com.firebaseui:firebase-ui-auth:7.1.1'

    // FirebaseUI for Cloud Storage
    implementation 'com.firebaseui:firebase-ui-storage:7.1.1'
}
```

If you're including the `firebase-ui-auth` dependency, there's a little
[more setup](auth/README.md#configuration) required.

After the project is synchronized, we're ready to start using Firebase functionality in our app.

### Upgrading

If you are using an old version of FirebaseUI and upgrading, please see the appropriate
migration guide:

* [Upgrade from 6.4.0 to 7.x.x](./docs/upgrade-to-7.0.md)
* [Upgrade from 5.1.0 to 6.x.x](./docs/upgrade-to-6.0.md)
* [Upgrade from 4.3.2 to 5.x.x](./docs/upgrade-to-5.0.md)
* [Upgrade from 3.3.1 to 4.x.x](./docs/upgrade-to-4.0.md)
* [Upgrade from 2.3.0 to 3.x.x](./docs/upgrade-to-3.0.md)
* [Upgrade from 1.2.0 to 2.x.x](./docs/upgrade-to-2.0.md)

## Dependencies

### Compatibility with Firebase / Google Play Services libraries

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

You can see the specific dependencies associated with each release on the 
[Releases page](https://github.com/firebase/FirebaseUI-Android/releases).

### Upgrading dependencies

If you would like to use a newer version of one of FirebaseUI's transitive dependencies, such
as Firebase, Play services, or the Android support libraries, you need to add explicit
`implementation` declarations in your `build.gradle` for all of FirebaseUI's dependencies at the version
you want to use. Here are some examples listing all of the critical dependencies:

#### Auth

```groovy
implementation "com.google.firebase:firebase-auth:$X.Y.Z"
implementation "com.google.android.gms:play-services-auth:$X.Y.Z"

implementation "androidx.lifecycle:lifecycle-extensions:$X.Y.Z"
implementation "androidx.browser:browser:$X.Y.Z"
implementation "androidx.cardview:cardview:$X.Y.Z"
implementation "androidx.constraintlayout:constraintlayout:$X.Y.Z"
implementation "androidx.legacy:legacy-support-v4:$X.Y.Z"
implementation "com.google.android.material:material:$X.Y.Z"
```

#### Firestore

```groovy
implementation "com.google.firebase:firebase-firestore:$X.Y.Z"

implementation "androidx.legacy:legacy-support-v4:$X.Y.Z"
implementation "androidx.recyclerview:recyclerview:$X.Y.Z"
```

#### Realtime Database

```groovy
implementation "com.google.firebase:firebase-database:$X.Y.Z"

implementation "androidx.legacy:legacy-support-v4:$X.Y.Z"
implementation "androidx.recyclerview:recyclerview:$X.Y.Z"
```

#### Storage

```groovy
implementation "com.google.firebase:firebase-storage:$X.Y.Z"

implementation "androidx.legacy:legacy-support-v4:$X.Y.Z"
```


## Sample app

There is a sample app in the [`app/`](app) directory that demonstrates most
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

A note on importing the project using Android Studio: Using 'Project from 
Version Control' will not automatically link the project with Gradle 
(issue [#1349](https://github.com/firebase/FirebaseUI-Android/issues/1349)). 
When doing so and opening any `build.gradle.kts` file, an error shows up: 
`Project 'FirebaseUI-Android' isn't linked with Gradle`. To resolve this 
issue, please `git checkout` the project manually and import with `Import 
from external model`.

## Snapshot builds

Like to live on the cutting edge?  Want to try the next release of FirebaseUI before anyone else?
FirebaseUI hosts "snapshot" builds on oss.jfrog.org.

Just add the following to your `build.gradle`:

```groovy
repositories {
  maven { url "https://oss.jfrog.org/artifactory/oss-snapshot-local" }
}
```

Then you can depend on snapshot versions:

```groovy
implementation 'com.firebaseui:firebase-ui-auth:$X.Y.Z-SNAPSHOT'
```

You can see which `SNAPSHOT` builds are avaiable here:
https://oss.jfrog.org/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/com/firebaseui

Snapshot builds come with absolutely no guarantees and we will close any issues asking to troubleshoot
a snapshot report unless they identify a bug that should block the release launch. Experiment
at your own risk!

## Contributing

### Installing locally

You can download FirebaseUI and install it locally by cloning this
repository and running:

```sh
./gradlew :library:prepareArtifacts :library:publishAllToMavenLocal
```

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

### Contribution process

1. Submit an issue describing your proposed change to the repo in question.
1. The repo owner will respond to your issue promptly.
1. If your proposed change is accepted, and you haven't already done so, sign a
   Contributor License Agreement (see details above).
1. Fork the desired repo, develop, and then test your code changes **on the latest dev branch**.
1. Ensure that your code adheres to the existing style of the library to which
   you are contributing.
1. Ensure that your code has an appropriate set of unit tests which all pass.
1. Submit a pull request targeting the latest dev branch.

[gh-actions]: https://github.com/firebase/FirebaseUI-Android/actions
[gh-actions-badge]: https://github.com/firebase/FirebaseUI-Android/workflows/Android%20CI/badge.svg
