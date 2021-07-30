# Releasing

**Note**: This guide does not yet cover the full process for releasing a major version, which is
more complex.

## 0 - Decide when to release

All releases should have a milestone. A release is ready to go when all issues/PRs in the milestone
are either closed or have the `fix-implemented` lablel.

FirebaseUI does not have strict guidance on release frequency. Minor and patch releases can go out
as soon as they are ready. Major releases with breaking changes should happen as infrequently as
possibly, preferable not more than 1-2 a year unless required by the underlying SDKs.

## 1 - Create the release branch

First, check out the latest `version-x.y.z-dev` branch. Branch from the `HEAD` of that branch
to create a new `version-x.y.z` branch:

```shell
$ VERSION=1.2.3
$ git checkout version-$VERSION-dev && git pull origin version-$VERSION-dev
$ git checkout -b version-$VERSION
```

Next, make the following changes on the release branch:

  * Update `Config.kt` and `gradle.properties` to remove the `SNAPSHOT` from the version name and set the release version.
  * Update `README.md` and `auth/README.md` to point to the latest version of the library
    and to have the correct descriptions of transitive dependencies.
  * Empty `CHANGELOG.md`

Commit the changes with a generic message:

```shell
$ git commit -am "Version x.y.z"
```

Next, upload the release branch to GitHub and create a pull request against `master`:

```shell
$ git push -u origin HEAD:version-$VERSION
```

When ready, merge the pull request.

## 2a - Set up publishing environment

### Credentials

The library is published to Maven Central by the firebase-sonatype account, Googlers can find the
password for this account in [Valentine](http://valentine/)

### GPG Key

You will need to create a private GPG keyring on your machine, if you don't have one do the
following steps:

  1. Run `gpg --full-generate-key`
  1. Choose `RSA and RSA` for the key type
  1. Use `4096` for the key size
  1. Use `0` for the expiration (never)
  1. Use any name, email address, and password
  
This creates your key in `~/.gnupg/openpgp-revocs.d/` with `.rev` format. The last 8 characters
before the `.rev` extension are your **Key ID**.

To export the key, run:

```
gpg --export-secret-keys -o $HOME/sonatype.gpg
```

Finally upload your key to the keyserver:

```
gpg --keyserver hkp://keys.openpgp.org --send-keys <YOUR KEY ID>
```

### Local Properties

Open your `$HOME/.gradle/gradle.properties` file at and fill in the values:

```
signing.keyId=<KEY ID>
signing.password=<PASSWORD YOU CHOSE>
signing.secretKeyRingFile=<FULL PATH TO YOUR GPG FILE>
mavenCentralRepositoryUsername=firebase-sonatype
mavenCentralRepositoryUsername=<PASSWORD FROM VALENTINE>
```

## 2b - Publish and Release

### Publish

Once you are sure the release branch is healthy, run the following command:

```shell
./gradlew clean :library:prepareArtifacts
./gradlew --no-daemon --no-parallel publishAllPublicationsToMavenRepository
```

### Release

Follow [the instructions here](https://central.sonatype.org/pages/releasing-the-deployment.html):

  1. Navigate to https://oss.sonatype.org/ and **Log In**
  1. On the left side click **Staging Repositories** and look for the `com.firebaseui` repo.
  1. Click **Close** ... wait a few minutes (you can check status with **Refresh**)
  1. Click **Release**

## 3 - Update issues, milestones, and release notes

First, go to the milestone for the released version and for each open issue:

  * Close the issue.
  * Comment to let anyone watching know that the fix/feature has been released.

Next, create a git tag for the latest version. In GitHub, create release notes based on the
template from the previous version and assign the release ntoes to the tag.

## 4 - Prepare next development branch

Check out a new `version-x.y.z-dev` branch from `master`. Change the version to be the next
`SNAPSHOT` version and then push the new dev branch up.

When possible, change all outstanding PRs to target the new dev branch and then delete the old
`dev` branch.
