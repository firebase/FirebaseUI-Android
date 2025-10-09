#!/usr/bin/env bash
set -e

GRADLE_ARGS="--max-workers=2"

cp library/google-services.json app/google-services.json
cp library/google-services.json proguard-tests/google-services.json
cp library/google-services.json composeapp/google-services.json

./gradlew $GRADLE_ARGS clean
./gradlew $GRADLE_ARGS assembleDebug
# TODO(thatfiredev): re-enable before release
# ./gradlew $GRADLE_ARGS proguard-tests:build
./gradlew $GRADLE_ARGS checkstyle 
./gradlew $GRADLE_ARGS testDebugUnitTest
