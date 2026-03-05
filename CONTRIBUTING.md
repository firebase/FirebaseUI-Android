# Contributing to FirebaseUI-Android

<a href="https://github.com/firebase/FirebaseUI-Android/actions/workflows/android.yml">
  <img src="https://github.com/firebase/FirebaseUI-Android/workflows/Android%20CI/badge.svg" alt="Android CI GitHub Workflow Status"/>
</a>

_See also: [Firebase's code of conduct](https://firebase.google.com/support/guides/code-conduct)_

## 1. Things you will need

- Linux, Mac OS X, or Windows.
- [git](https://git-scm.com) (used for source version control).
- An ssh client (used to authenticate with GitHub).
- [Android Studio](https://developer.android.com/studio) or [IntelliJ IDEA](https://www.jetbrains.com/idea/).
- [JDK 21](https://adoptium.net/) or higher.
- [Android SDK](https://developer.android.com/studio) with minimum API level 21.

## 2. Forking & cloning the repository

- Ensure all the dependencies described in the previous section are installed.
- Fork `https://github.com/firebase/FirebaseUI-Android` into your own GitHub account. If
  you already have a fork, and are now installing a development environment on
  a new machine, make sure you've updated your fork so that you don't use stale
  configuration options from long ago.
- If you haven't configured your machine with an SSH key that's known to github, then
  follow [GitHub's directions](https://help.github.com/articles/generating-ssh-keys/)
  to generate an SSH key.
- `git clone git@github.com:<your_name_here>/FirebaseUI-Android.git`
- `git remote add upstream git@github.com:firebase/FirebaseUI-Android.git` (So that you
  fetch from the main repository, not your clone, when running `git fetch`
  et al.)

## 3. Environment Setup

FirebaseUI-Android uses Gradle to manage the project and dependencies.

The repository includes a `library/google-services.json` file that is copied to the app and test modules during builds. This is handled automatically by the build scripts.

To verify your environment is set up correctly, run:

```bash
./scripts/build.sh
```

This will:
- Copy the necessary `google-services.json` files
- Download all dependencies
- Build all modules
- Run checkstyle
- Run unit tests

> If you need to use your own Firebase project, replace `library/google-services.json` with your own configuration file from the [Firebase Console](https://console.firebase.google.com/).

## 4. Running an example

The project provides a demo app in the `app` module which showcases the main use-cases of FirebaseUI Auth.

To run the example app:

**Option 1: Android Studio**
- Open the project in Android Studio
- Select the `app` configuration
- Run on a device or emulator

**Option 2: Command line**
```bash
./gradlew :app:installDebug
```

Then launch the app on your device.

Any changes made to the library modules (auth, database, firestore, storage) locally will be automatically reflected in the example application.

## 5. Running tests

FirebaseUI-Android comprises of a number of tests, including unit tests and E2E tests.

### Unit tests

Unit tests are responsible for ensuring expected behavior whilst developing the library's Kotlin/Java code. Unit tests do not
interact with 3rd party Firebase services, and mock where possible. To run unit tests for all modules (excluding e2eTest), run the following command from the root directory:

```bash
./gradlew testDebugUnitTest -x :e2eTest:testDebugUnitTest
```

To run unit tests for a specific module (e.g., auth):

```bash
./gradlew :auth:testDebugUnitTest
```

### E2E tests

E2E tests run against Firebase Auth Emulator and test the full integration with Firebase services. To run e2e tests, you first need to start the Firebase Emulator:

```bash
# Install Firebase Tools (if not already installed)
npm install -g firebase-tools

# Start the Firebase Auth Emulator
./scripts/start-firebase-emulator.sh
```

Then in a separate terminal, run the e2e tests:

```bash
./gradlew e2eTest
```

> Note: E2E tests use Firebase Emulator Suite, so you don't need a real Firebase project to run them.

### Lint and Code Analysis

To run lint checks:

```bash
./gradlew checkstyle
```

## 6. Contributing code

We gladly accept contributions via GitHub pull requests.

Please peruse the
[Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and
[Android code style guide](https://developer.android.com/kotlin/style-guide) before
working on anything non-trivial. These guidelines are intended to
keep the code consistent and avoid common pitfalls.

To start working on a patch:

1. `git fetch upstream`
2. `git checkout upstream/master -b <name_of_your_branch>`
3. Hack away!

Once you have made your changes, ensure that it passes the internal analyzer & formatting checks. The following
commands can be run locally to highlight any issues before committing your code:

```bash
# Run the full CI build script
./scripts/build.sh
```

This script runs:
- `./gradlew clean`
- `./gradlew assembleDebug` - Build all modules
- `./gradlew checkstyle` - Run code style checks
- `./gradlew testDebugUnitTest -x :e2eTest:testDebugUnitTest` - Run unit tests

You can also run these commands individually if needed.

Assuming all is successful, commit and push your code:

1. `git commit -a -m "<your informative commit message>"`
2. `git push origin <name_of_your_branch>`

To send us a pull request:

- `git pull-request` (if you are using [Hub](http://github.com/github/hub/)) or
  go to `https://github.com/firebase/FirebaseUI-Android` and click the
  "Compare & pull request" button

Please make sure all your check-ins have detailed commit messages explaining the patch.

When naming the title of your pull request, please follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
guide. For example, for a fix to the FirebaseUI Auth module:

`fix(auth): fixed a bug with email sign-in!`

For a new feature:

`feat(auth): add support for passkey authentication`

Tests are run automatically on contributions using GitHub Actions. Depending on
your code contributions, various tests will be run against your updated code automatically.

Once you've gotten an LGTM from a project maintainer and once your PR has received
the green light from all our automated testing, wait for one of the package maintainers
to merge the pull request.

### Code style

FirebaseUI-Android follows standard Kotlin and Android conventions:

#### Kotlin

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable and function names
- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)

#### Jetpack Compose

- Follow [Compose API guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
- Composables should be stateless when possible
- Use `remember` for state that survives recomposition
- Hoist state when it needs to be shared

#### Example

```kotlin
@Composable
fun SignInScreen(
    configuration: AuthUIConfiguration,
    onSignInSuccess: (AuthResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

### Documentation

All public APIs should be documented with KDoc:

```kotlin
/**
 * Authenticates a user with email and password.
 *
 * @param email The user's email address
 * @param password The user's password
 * @return [AuthResult] containing the signed-in user
 * @throws AuthException.InvalidCredentialsException if credentials are invalid
 * @throws AuthException.NetworkException if network is unavailable
 *
 * Example usage:
 * ```kotlin
 * val result = authUI.signInWithEmailAndPassword(
 *     email = "user@example.com",
 *     password = "securePassword123"
 * )
 * ```
 */
suspend fun signInWithEmailAndPassword(
    email: String,
    password: String
): AuthResult
```

### Contributor License Agreement

You must complete the
[Contributor License Agreement](https://cla.developers.google.com/clas).
You can do this online, and it only takes a minute.
If you've never submitted code before, you must add your (or your
organization's) name and contact info to the [AUTHORS](AUTHORS) file.

### License Headers

If you create a new file, do not forget to add the license header:

```kotlin
/*
 * Copyright 2025 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

### The review process

Newly opened PRs first go through initial triage which results in one of:

- **Merging the PR** - if the PR can be quickly reviewed and looks good.
- **Closing the PR** - if the PR maintainer decides that the PR should not be merged.
- **Moving the PR to the backlog** - if the review requires non-trivial effort and the issue isn't a priority; in this case the maintainer will:
  - Make sure that the PR has an associated issue labeled with "auth", "database", "firestore", or "storage".
  - Add the "backlog" label to the issue.
  - Leave a comment on the PR explaining that the review is not trivial and that the issue will be looked at according to priority order.
- **Starting a non-trivial review** - if the review requires non-trivial effort and the issue is a priority; in this case the maintainer will:
  - Add the "in review" label to the issue.
  - Self assign the PR.
- **API Changes**
  - If a change or improvement will affect public API, the team will take longer in the review process.

### The release process

We push releases manually, using Gradle and Maven publishing.

Changelogs and version updates are managed by project maintainers. The new version is automatically
generated via the commit types and changelogs via the commit messages.

Some things to keep in mind before publishing the release:

- Has CI run on the main commit and gone green? Even if CI shows as green on
  the PR it's still possible for it to fail on merge, for multiple reasons.
  There may have been some bug in the merge that introduced new failures. CI
  runs on PRs as it's configured on their branch state, and not on tip of tree.
- [Publishing is
  forever.](https://central.sonatype.org/publish/publish-guide/#deployment)
  Hopefully any bugs or breaking changes in this PR have already been caught
  in PR review, but now's a second chance to revert before anything goes live.
- "Don't deploy on a Friday." Consider carefully whether or not it's worth
  immediately publishing an update before a stretch of time where you're going
  to be unavailable. There may be bugs with the release or questions about it
  from people that immediately adopt it, and uncovering and resolving those
  support issues will take more time if you're unavailable.

## 7. Contributing documentation

We gladly accept contributions to the SDK documentation. As our docs are also part of this repo,
see "Contributing code" above for how to prepare and submit a PR to the repo.

FirebaseUI-Android documentation lives in the README files for each module:
- `auth/README.md` - FirebaseUI Auth documentation
- `database/README.md` - FirebaseUI Realtime Database documentation
- `firestore/README.md` - FirebaseUI Firestore documentation
- `storage/README.md` - FirebaseUI Storage documentation

Firebase follows the [Google developer documentation style guide](https://developers.google.com/style),
which you should read before writing substantial contributions.

When updating documentation:

1. Ensure code samples are tested and working
2. Follow Markdown best practices
3. Include screenshots or GIFs for UI-related changes
4. Update table of contents if adding new sections
5. Ensure links are valid and point to the correct locations

## 8. Getting help

If you have questions about contributing:

- Check the module README files
- Check existing [issues](https://github.com/firebase/FirebaseUI-Android/issues) and [pull requests](https://github.com/firebase/FirebaseUI-Android/pulls)
- Ask on [Stack Overflow](https://stackoverflow.com/questions/tagged/firebaseui) with the `firebaseui` tag
- Create a new issue for discussion

## 9. Recognition

Contributors will be recognized in:
- Release notes
- GitHub contributors page
- Project AUTHORS file

Thank you for making FirebaseUI-Android better! 🎉
