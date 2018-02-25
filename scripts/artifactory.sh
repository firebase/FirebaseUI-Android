#!/usr/bin/env bash

set -e

VERSION_BRANCH_RE="version-[0-9\.]+-dev"

if [[ ($CI == "true") && !("${TRAVIS_BRANCH}" =~ $VERSION_BRANCH_RE) ]]; then
    echo "Not triggering artifactory for branch: ${TRAVIS_BRANCH}"
    exit 0
fi

if [[ ($CI == "true") && ($TRAVIS_EVENT_TYPE != "push") ]]; then
    echo "Artifactory only triggered on 'push' builds."
    exit 0
fi


./gradlew :library:prepareArtifacts
./gradlew :common:assembleRelease :common:prepareArtifacts :common:artifactoryPublish
./gradlew :auth:assembleRelease :auth:prepareArtifacts :auth:artifactoryPublish
./gradlew :database:assembleRelease :database:prepareArtifacts :database:artifactoryPublish
./gradlew :firestore:assembleRelease :firestore:prepareArtifacts :firestore:artifactoryPublish
./gradlew :storage:assembleRelease :storage:prepareArtifacts :storage:artifactoryPublish
