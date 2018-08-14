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
$ git checkout -b version-$VERSIOn
```

Next, make the following changes on the release branch:

  * Update `Config.kt` to remove the `SNAPSHOT` from the version name.
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

## 2 - Upload to Bintray

Once you are sure the release branch is healthy, run the following command:

```shell
$ ./gradlew clean :library:prepareArtifacts :library:bintrayUploadAll
```

This will upload the release to Bintray as a draft. You will need to go to this page and click
**Publish** to release the draft artifacts. When done correctly you should see 8 pending draft
artifacts per library:

https://bintray.com/firebaseui/firebase-ui

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
