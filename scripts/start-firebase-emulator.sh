#!/bin/bash
if ! [ -x "$(command -v firebase)" ]; then
  echo "❌ Firebase tools CLI is missing."
  exit 1
fi

if ! [ -x "$(command -v node)" ]; then
  echo "❌ Node.js is missing."
  exit 1
fi

if ! [ -x "$(command -v npm)" ]; then
  echo "❌ NPM is missing."
  exit 1
fi

# Extract project ID from .firebaserc
export FIREBASE_PROJECT_ID=$(cat library/.firebaserc | jq -r '.projects.default')

# Extract auth port from firebase.json
AUTH_PORT=$(cat library/firebase.json | jq -r '.emulators.auth.port')
export FIREBASE_AUTH_EMULATOR_URL="http://127.0.0.1:${AUTH_PORT}"

# Starts firebase auth emulator only
EMU_START_COMMAND="firebase emulators:start --only auth --project ${FIREBASE_PROJECT_ID}"

MAX_RETRIES=3
MAX_CHECKATTEMPTS=60
CHECKATTEMPTS_WAIT=1

RETRIES=1
while [ $RETRIES -le $MAX_RETRIES ]; do

  if [[ -z "${CI}" ]]; then
    echo "Starting Firebase Emulator Suite in foreground."
    $EMU_START_COMMAND
    exit 0
  else
    echo "Starting Firebase Emulator Suite in background."
    $EMU_START_COMMAND &
    CHECKATTEMPTS=1
    while [ $CHECKATTEMPTS -le $MAX_CHECKATTEMPTS ]; do
      sleep $CHECKATTEMPTS_WAIT
      if curl --output /dev/null --silent --fail ${FIREBASE_AUTH_EMULATOR_URL}; then
        # Check again since it can exit before the emulator is ready.
        sleep 15
        if curl --output /dev/null --silent --fail ${FIREBASE_AUTH_EMULATOR_URL}; then
          echo "Firebase Emulator Suite is online!"
          exit 0
        else
          echo "❌ Firebase Emulator exited after startup."
          exit 1
        fi
      fi
      echo "Waiting for Firebase Emulator Suite to come online, check $CHECKATTEMPTS of $MAX_CHECKATTEMPTS..."
      ((CHECKATTEMPTS = CHECKATTEMPTS + 1))
    done
  fi

  echo "Firebase Emulator Suite did not come online in $MAX_CHECKATTEMPTS checks. Try $RETRIES of $MAX_RETRIES."
  ((RETRIES = RETRIES + 1))

done
echo "Firebase Emulator Suite did not come online after $MAX_RETRIES attempts."
exit 1