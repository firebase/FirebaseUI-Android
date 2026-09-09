#!/usr/bin/env bash
set -e

GRADLE_ARGS="--max-workers=2"

cp library/google-services.json app/google-services.json
cp library/google-services.json proguard-tests/google-services.json

./gradlew $GRADLE_ARGS clean
./gradlew $GRADLE_ARGS assembleDebug
# TODO(thatfiredev): re-enable before release
# ./gradlew $GRADLE_ARGS proguard-tests:build
./gradlew $GRADLE_ARGS checkstyle
# Android Lint is the Kotlin-capable gate, but it runs in its own workflow
# (.github/workflows/lint.yml) so it runs in parallel with this path rather than
# adding ~5 minutes to it, and so a lint failure does not mask unit-test results.
./gradlew $GRADLE_ARGS testDebugUnitTest -x :e2eTest:testDebugUnitTest
