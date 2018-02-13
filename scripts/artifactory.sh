#!/usr/bin/env bash

set -e

./gradlew :library:prepareArtifacts
./gradlew :common:artifactoryPublish
./gradlew :auth:artifactoryPublish
./gradlew :database:artifactoryPublish
./gradlew :firestore:artifactoryPublish
./gradlew :storage:artifactoryPublish
