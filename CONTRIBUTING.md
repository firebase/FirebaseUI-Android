Want to contribute? Great! This document includes information on both the [technical process](#technical-process)
and the [legal process](#legal-process) for contributing. Make sure to read both!

# Technical Process

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

# Legal Process

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

### Before you contribute

Before we can use your code, you must sign the [Google Individual Contributor
License Agreement](https://cla.developers.google.com/about/google-individual)
(CLA), which you can do online. The CLA is necessary mainly because you own the
copyright to your changes, even after your contribution becomes part of our
codebase, so we need your permission to use and distribute your code. We also
need to be sure of various other things—for instance that you'll tell us if you
know that your code infringes on other people's patents. You don't have to sign
the CLA until after you've submitted your code for review and a member has
approved it, but you must do it before we can put your code into our codebase.

### Adding new features

Before you start working on a larger contribution, you should get in touch with
us first through the issue tracker with your idea so that we can help out and
possibly guide you. Coordinating up front makes it much easier to avoid
frustration later on.

If this has been discussed in an issue, make sure to mention the issue number.
If not, go file an issue about this to make sure this is a desirable change.

If this is a new feature please co-ordinate with someone on [FirebaseUI-iOS](https://github.com/firebase/FirebaseUI-iOS)
to make sure that we can implement this on both platforms and maintain feature parity.
Feature parity (where it makes sense) is a strict requirement for feature development in FirebaseUI.

### Code reviews

All submissions, including submissions by project members, require review. We
use Github pull requests for this purpose. We adhere to the
[Google Java style guide](https://google.github.io/styleguide/javaguide.html).

### The small print

Contributions made by corporations are covered by a different agreement than the
one above, the [Software Grant and Corporate Contributor License
Agreement](https://cla.developers.google.com/about/google-corporate).
