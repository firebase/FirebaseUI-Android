#!/usr/bin/env bash

./gradlew :library:prepareArtifacts
./gradlew :common:artifactoryPublish
./gradlew :auth:artifactoryPublish
./gradlew :database:artifactoryPublish
./gradlew :firestore:artifactoryPublish
./gradlew :storage:artifactoryPublish
