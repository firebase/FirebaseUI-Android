#!/bin/bash
set -e

cd $(dirname $0)

###########################
#  VALIDATE GEOFIRE REPO  #
###########################
# Ensure the checked out geofire branch is master
CHECKED_OUT_BRANCH="$(git branch | grep "*" | awk -F ' ' '{print $2}')"
if [[ $CHECKED_OUT_BRANCH != "master" ]]; then
  echo "Error: Your FirebaseUI-Android repo is not on the master branch."
  exit 1
fi

# Make sure the geofire branch does not have existing changes
if ! git --git-dir=".git" diff --quiet; then
  echo "Error: Your FirebaseUI-Android repo has existing changes on the master branch. Make sure you commit and push the new version before running this release script."
  exit 1
fi

##############################
#  VALIDATE CLIENT VERSIONS  #
##############################

VERSION=$(grep version pom.xml |head -2|tail -1|awk -F '>' '{print $2}'|awk -F '<' '{print $1}'|awk -F '-' '{print $1}')
read -p "We are releasing $VERSION, is this correct? (press enter to continue) " DERP
if [[ ! -z $DERP ]]; then
  echo "Cancelling release, please update pom.xml to desired version"
fi

# Ensure there is not an existing git tag for the new version
# XXX this is wrong; needs to be semver sorted as my other scripts are
LAST_GIT_TAG="$(git tag --list | tail -1 | awk -F 'v' '{print $2}')"
if [[ $VERSION == $LAST_GIT_TAG ]]; then
  echo "Error: git tag v${VERSION} already exists. Make sure you are not releasing an already-released version."
  exit 1
fi

# Create docs
./create-docs.sh
if [[ $? -ne 0 ]]; then
  echo "error: There was an error creating the docs."
  exit 1
fi

###################
# DEPLOY TO MAVEN #
###################
read -p "Next, make sure this repo is clean and up to date. We will be kicking off a deploy to maven." DERP
mvn clean
mvn release:clean release:prepare release:perform -Dtag=v$VERSION

if [[ $? -ne 0 ]]; then
  echo "error: Error building and releasing to maven."
  exit 1
fi

##############
# UPDATE GIT #
##############

# Push the new git tag created by Maven
git push --tags
if [[ $? -ne 0 ]]; then
  echo "Error: Failed to do 'git push --tags' from geofire repo."
  exit 1
fi

################
# MANUAL STEPS #
################

echo "Manual steps:"
echo "  1) release maven repo at http://oss.sonatype.org/"
echo "  2) Deploy new docs: $> firebase deploy"
echo "  3) Update the release notes for FirebaseUI-Android version ${VERSION} on GitHub and add jars for downloading"
echo "  4) Update firebase-versions.json in the firebase-clients repo with the changelog information"
echo "  5) Tweet @FirebaseRelease: 'v${VERSION} of FirebaseUI-Android is available https://github.com/firebase/FirebaseUI-Android"
echo ---
echo "Done! Woo!"