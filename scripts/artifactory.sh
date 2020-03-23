#!/usr/bin/env bash
set -e

./gradlew :library:prepareArtifacts
./gradlew :common:assembleRelease :common:prepareArtifacts :common:artifactoryPublish
./gradlew :auth:assembleRelease :auth:prepareArtifacts :auth:artifactoryPublish
./gradlew :database:assembleRelease :database:prepareArtifacts :database:artifactoryPublish
./gradlew :firestore:assembleRelease :firestore:prepareArtifacts :firestore:artifactoryPublish
./gradlew :storage:assembleRelease :storage:prepareArtifacts :storage:artifactoryPublish
