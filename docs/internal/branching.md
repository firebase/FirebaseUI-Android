# Branching Strategy

## Basics

FirebaseUI organizes upcoming features and bugfixes via a branching system.
The basic idea is:

  * `master` - this branch contains the latest **released** code. The only
    commits newer than the last release should be documentation fixes.
  * `version-x.y.z-dev` - this branch contains ongoing work for version
    `x.y.z`. Normally there is only one of these branches alive at a given
    time, but sometimes you will see two if we are working on long-term
    and short-term changes.
  * `version-x.y.z` - these branches basically act as "tags", you can use
    them to easily browse the code from any past version.

## Dev Branches

Dev branches are created by picking a base branch (normally `master`) and
creating a commit that modifies `constants.gradle` to use the desired `SNAPSHOT`
version.

All unreleased work should be done as Pull Requests that target a `dev` branch.
Merging a PR into a `dev` branch means it will be included in that release.

Whenever a commit is merged into a `dev` branch, a build on Travis CI is kicked
off. If the build passes, a `SNAPSHOT` artifact is uploaded to
[artifactory][artifactory].

[artifactory]: https://oss.jfrog.org/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/com/firebaseui
