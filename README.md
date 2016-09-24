# FirebaseUI for Android — UI Bindings for Firebase

[![Build Status](https://travis-ci.org/firebase/FirebaseUI-Android.svg?branch=master)](https://travis-ci.org/firebase/FirebaseUI-Android)

FirebaseUI is an open-source library for Android that allows you to
quickly connect common UI elements to [Firebase](https://firebase.google.com)
APIs like the Realtime Database or Firebase Authentication.

A compatible FirebaseUI client is also available for [iOS](https://github.com/firebase/firebaseui-ios).

## Table of Content

  1. [Installation](#installation)
  1. [Usage](#usage)
  1. [Sample App](#sample-app)
  1. [Contributing](#contributing)

## Installation

FirebaseUI is published as a collection of libraries separated by the
Firebase API they target. Each FirebaseUI library has a transitive
dependency on the appropriate Firebase SDK so there is no need to include
those separately in your app.

In your `app/build.gradle` file add a dependency on one of the FirebaseUI
libraries.

```groovy
dependencies {
    // Single target that includes all FirebaseUI libraries
    compile 'com.firebaseui:firebase-ui:0.6.0'

    // FirebaseUI Database only
    compile 'com.firebaseui:firebase-ui-database:0.6.0'

    // FirebaseUI Auth only
    compile 'com.firebaseui:firebase-ui-auth:0.6.0'

    // FirebaseUI Storage only
    compile 'com.firebaseui:firebase-ui-storage:0.6.0'
}
```

After the project is synchronized, we're ready to start using Firebase functionality in our app.

### Compatibility with Firebase / Google Play Services Libraries

FirebaseUI libraries have the following transitive dependencies on the Firebase SDK:
```
firebase-ui-auth
|--- com.google.firebase:firebase-auth
|--- com.google.android.gms:play-services-auth

firebase-ui-database
|--- com.google.firebase:firebase-database

firebase-ui-storage
|--- com.google.firebase:firebase-storage
```

Each version of FirebaseUI has dependency on a fixed version of these libraries, defined as the variable `firebase_version`
in `common/constants.gradle`.  If you are using any dependencies in your app of the form 
`compile 'com.google.firebase:firebase-*:x.y.z'` or `compile 'com.google.android.gms:play-services-*:x.y.z'`
you need to make sure that you use the same version that your chosen version of FirebaseUI requires.

For convenience, here are some examples:

| FirebaseUI Version | Firebase/Play Services Version |
|--------------------|--------------------------------|
| 0.6.0              | 9.6.0                          |
| 0.5.3              | 9.4.0                          |
| 0.4.4              | 9.4.0                          |
| 0.4.3              | 9.2.1                          |
| 0.4.2              | 9.2.0                          |
| 0.4.1              | 9.0.2                          |
| 0.4.0              | 9.0.0                          |

## Usage

  * [firebase-ui-database](database/README.md)
  * [firebase-ui-auth](auth/README.md)
  * [firebase-ui-storage](storage/README.md)

## Sample App

There is a sample app in the `app/` directory that demonstrates most
of the features of FirebaseUI. Load the project in Android Studio and
run it on your Android device to see a demonstration.

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
