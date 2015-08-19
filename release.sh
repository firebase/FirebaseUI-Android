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
#  exit 1
fi

##############################
#  VALIDATE CLIENT VERSIONS  #
##############################

VERSION=$(grep versionName library/build.gradle | awk '{print $2}' | awk '{split($0, a, "\"")}{print a[2]}')
read -p "We are releasing $VERSION, is this correct? (press enter to continue) " DERP
if [[ ! -z $DERP ]]; then
  echo "Cancelling release, please update library/build.gradle with the desired version"
fi

# TODO: Ensure this version is also on pom.xml

# Ensure there is not an existing git tag for the new version
# XXX this is wrong; needs to be semver sorted as my other scripts are
LAST_GIT_TAG="$(git tag --list | tail -1 | awk -F 'v' '{print $2}')"
if [[ $VERSION == $LAST_GIT_TAG ]]; then
  echo "Error: git tag v${VERSION} already exists. Make sure you are not releasing an already-released version."
  exit 1
fi

# Tag the release in git
# XXX this is wrong; needs to be semver sorted as my other scripts are
git tag -a v$VERSION

##########################
# GENERATE RELEASE BUILD #
##########################

#gradle clean assembleRelease assembleDebug bundleReleaseJavadoc
gradle clean :app:compileDebugSources :app:compileDebugAndroidTestSources :library:compileDebugSources :library:compileDebugAndroidTestSources bundleReleaseJavadoc

###################
# DEPLOY TO MAVEN #
###################
read -p "Next, make sure this repo is clean and up to date. We will be kicking off a deploy to maven." DERP
#the next line installs the output of build.gradle into (local) maven, but does not tag it in git
#mvn install:install-file -Dfile=library/build/outputs/aar/library-release.aar -DgroupId=com.firebase -DartifactId=firebase-ui -Dversion=$VERSION -Dpackaging=aar
#the next line signs and deploys the aar file to maven
#mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=ossrh -DpomFile=library/pom.xml -Dfile=library/build/outputs/aar/library-debug.aar -Dversion=$VERSION

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
  echo "Error: Failed to do 'git push --tags' from FirebaseUI-Android repo."
  exit 1
fi

################
# MANUAL STEPS #
################

echo "Manual steps:"
echo "  1) release maven repo at http://oss.sonatype.org/"
#echo "  2) Deploy new docs: $> firebase deploy"
echo "  3) Update the release notes for FirebaseUI-Android version ${VERSION} on GitHub and add aar for downloading"
#echo "  4) Update firebase-versions.json in the firebase-clients repo with the changelog information"
#echo "  5) Tweet @FirebaseRelease: 'v${VERSION} of FirebaseUI-Android is available https://github.com/firebase/FirebaseUI-Android"
echo ---
echo "Done! Woo!"