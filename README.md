# FirebaseUI for Android — UI Bindings for Firebase

[![FirebaseOpensource.com](https://img.shields.io/badge/Docs-firebaseopensource.com-orange.svg)](
https://firebaseopensource.com/projects/firebase/firebaseui-android
)
[![Build Status](https://travis-ci.org/firebase/FirebaseUI-Android.svg?branch=master)](https://travis-ci.org/firebase/FirebaseUI-Android)

FirebaseUI is an open-source library for Android that allows you to
quickly connect common UI elements to [Firebase](https://firebase.google.com) APIs.

A compatible FirebaseUI client is also available for [iOS](https://github.com/firebase/firebaseui-ios).

## Table of contents

1. [Usage](#usage)
1. [Installation](#installation)
   1. [Upgrading](#upgrading)
1. [Dependencies](#dependencies)
   1. [Compatability](#compatibility-with-firebase--google-play-services-libraries)
   1. [Upgrading dependencies](#upgrading-dependencies)
1. [Sample App](#sample-app)
1. [Snapshot Builds](#snapshot-builds)
1. [Contributing](#contributing)
   1. [Installing](#installing-locally)
   1. [Deploying](#deployment)
   1. [Tagging](#tag-a-release-on-github)
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
    implementation 'com.firebaseui:firebase-ui-database:4.2.1'

    // FirebaseUI for Cloud Firestore
    implementation 'com.firebaseui:firebase-ui-firestore:4.2.1'

    // FirebaseUI for Firebase Auth
    implementation 'com.firebaseui:firebase-ui-auth:4.2.1'

    // FirebaseUI for Firebase Auth (GitHub provider)
    implementation 'com.firebaseui:firebase-ui-auth-github:4.2.1'

    // FirebaseUI for Cloud Storage
    implementation 'com.firebaseui:firebase-ui-storage:4.2.1'
}
```

If you're including the `firebase-ui-auth` dependency, there's a little
[more setup](auth/README.md#configuration) required.

After the project is synchronized, we're ready to start using Firebase functionality in our app.

### Upgrading

If you are using an old version of FirebaseUI and upgrading, please see the appropriate
migration guide:

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

As of version `15.0.0`, Firebase and Google Play services libraries have independent, semantic
versions. This means that FirebaseUI has independent dependencies on each of the libraries above.
For best results, your app should depend on a version of each dependency with the same major
version number as the version used by FirebaseUI.

As of version `4.2.1`, FirebaseUI has the following dependency versions:

| Library              | Version                        |
|----------------------|--------------------------------|
| `firebase-auth`      | 16.0.5                         |
| `play-services-auth` | 16.0.1                         |
| `firebase-database`  | 16.0.3                         |
| `firebase-firestore` | 17.1.1                         |
| `firebase-storage`   | 16.0.3                         |

### Upgrading dependencies

If you would like to use a newer version of one of FirebaseUI's transitive dependencies, such
as Firebase, Play services, or the Android support libraries, you need to add explicit
`implementation` declarations in your `build.gradle` for all of FirebaseUI's dependencies at the version
you want to use. For example if you want to use Play services/Firebase version `FOO` and support
libraries version `BAR` add the following extra lines for each FirebaseUI module you're using:

#### Auth

```groovy
implementation "com.google.firebase:firebase-auth:$FOO"
implementation "com.google.android.gms:play-services-auth:$FOO"

implementation "com.android.support:design:$BAR"
implementation "com.android.support:customtabs:$BAR"
implementation "com.android.support:cardview-v7:$BAR"
```

#### Firestore

```groovy
implementation "com.google.firebase:firebase-firestore:$FOO"

implementation "com.android.support:recyclerview-v7:$BAR"
implementation "com.android.support:support-v4:$BAR"
```

#### Realtime Database

```groovy
implementation "com.google.firebase:firebase-database:$FOO"

implementation "com.android.support:recyclerview-v7:$BAR"
implementation "com.android.support:support-v4:$BAR"
```

#### Storage

```groovy
implementation "com.google.firebase:firebase-storage:$FOO"

implementation "com.android.support:appcompat-v7:$BAR"
implementation "com.android.support:palette-v7:$BAR"
```

#### Note

Starting version 25.4.0, support libraries are now available through
[Google's Maven repository](https://developer.android.com/studio/build/dependencies.html#google-maven),
so ensure that you have that added to your project's repositories.

Open the `build.gradle` file for your project and modify it as following,

```
allprojects {
    repositories {
        google()
        jcenter()
    }
}
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

Like to live on the cutting edge?  Want to try the next release of FirebaseUI before anyone else? As of version `3.2.2`
FirebaseUI hosts "snapshot" builds on oss.jfrog.org.

Just add the following to your `build.gradle`:

```groovy
repositories {
  maven { url "https://oss.jfrog.org/artifactory/oss-snapshot-local" }
}
```

Then you can depend on snapshot versions:

```groovy
implementation 'com.firebaseui:firebase-ui-auth:x.y.z-SNAPSHOT'
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
