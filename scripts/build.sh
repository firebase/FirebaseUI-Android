#!/usr/bin/env bash
set -e

cp library/google-services.json app/google-services.json
cp library/google-services.json proguard-tests/google-services.json

./gradlew clean
./gradlew assembleDebug proguard-tests:build check