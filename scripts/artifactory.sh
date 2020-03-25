#!/usr/bin/env bash
set -e

GRADLE_ARGS="--max-workers=2"

./gradlew $GRADLE_ARGS :library:prepareArtifacts
./gradlew $GRADLE_ARGS :common:assembleRelease :common:prepareArtifacts :common:artifactoryPublish
./gradlew $GRADLE_ARGS :auth:assembleRelease :auth:prepareArtifacts :auth:artifactoryPublish
./gradlew $GRADLE_ARGS :database:assembleRelease :database:prepareArtifacts :database:artifactoryPublish
./gradlew $GRADLE_ARGS :firestore:assembleRelease :firestore:prepareArtifacts :firestore:artifactoryPublish
./gradlew $GRADLE_ARGS :storage:assembleRelease :storage:prepareArtifacts :storage:artifactoryPublish
