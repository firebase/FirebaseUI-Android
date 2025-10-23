# Releasing

**Note**: This guide does not yet cover the full process for releasing a major version, which is
more complex.

## 0 - Decide when to release

All releases should have a milestone. A release is ready to go when all issues/PRs in the milestone
are either closed or have the `fix-implemented` label.

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

The library is published to Maven Central by the `firebase-sonatype` account, Googlers can find the
password for this account in [Valentine](http://go/valentine/).

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

You might receive an email from `keyserver@keys.openpgp.org` to verify your email.
Click the link in the email to complete the verification before proceeding.

<details>

<summary>gpg: keyserver send failed: Server indicated a failure</summary>

If you run into the `gpg: keyserver send failed: Server indicated a failure` error when trying to
 upload the key from a macOS machine, you can try the solution proposed [here](https://github.com/asdf-vm/asdf-nodejs/issues/192#issuecomment-797448073):

```shell
echo "standard-resolver" >  ~/.gnupg/dirmngr.conf
```

and then:

```shell
sudo pkill dirmngr
```

</details>

### Local Properties

1. Navigate to https://central.sonatype.com/ and **Log In** using the credentials from [Valentine](http://go/valentine).
1. Go to [View user tokens](https://central.sonatype.com/usertoken) and generate a new token.
    You should see an XML that looks like this:
    ```xml
    <server>
        <id>${server}</id>
        <username>tokenuser</username>
        <password>tokenkey-dlghnfgh8+4LfXmg5Hsd8jd</password>
    </server>
    ```
1. Open your `$HOME/.gradle/gradle.properties` file and fill in the values:

```
signing.keyId=<KEY ID>
signing.password=<PASSWORD YOU CHOSE>
signing.secretKeyRingFile=<FULL PATH TO YOUR GPG FILE>
mavenCentralUsername=<USERNAME FROM THE XML ABOVE (eg. tokenuser)>
mavenCentralPassword=<PASSWORD FROM THE XML ABOVE (eg. tokenkey-dlghnfgh8+4LfXmg5Hsd8jd)>
```

## 2b - Publish and release

Once you are sure the release branch is healthy, run the following commands:

```shell
./gradlew clean :library:prepareArtifacts
./gradlew --no-daemon --no-parallel publishAllPublicationsToMavenRepository
```

Follow [the instructions here](https://central.sonatype.org/pages/releasing-the-deployment.html):

  1. Navigate to https://central.sonatype.com/ and **Log In**, if you haven't already.
  1. Click on [View deployments](https://central.sonatype.com/publishing/deployments) and look for
  the `com.firebaseui-firebase-ui-$VERSION` deployment that you started.
  1. Wait for the artifacts to be verified, and when ready, click **Publish**.

## 3 - Update issues, milestones, and release notes

First, go to the milestone for the released version and for each open issue:

  * Close the issue.
  * Comment to let anyone watching know that the fix/feature has been released.

Next, create a git tag for the latest version. In GitHub, create release notes based on the
template from the previous version and assign the release notes to the tag.

## 4 - Prepare next development branch

Check out a new `version-x.y.z-dev` branch from `master`. Change the version to be the next
`SNAPSHOT` version and then push the new dev branch up.

When possible, change all outstanding PRs to target the new dev branch and then delete the old
`dev` branch.
