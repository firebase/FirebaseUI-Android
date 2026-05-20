#!/usr/bin/env bash

set -euo pipefail

APP_ID="com.firebaseui.android.demo"
MAIN_ACTIVITY="com.firebaseui.android.demo.MainActivity"
APK_RELATIVE_PATH="app/build/outputs/apk/debug/app-debug.apk"
EMULATOR_LOG="${TMPDIR:-/tmp}/firebaseui-android-emulator.log"
EMULATOR_PID=""
LAUNCHED_EMULATOR=0
CONNECTED_DEVICES=()
AVAILABLE_AVDS=()
KNOWN_DEVICE_SERIALS=()

usage() {
  cat <<'EOF'
Usage: run-demo.sh [--help]

Builds, installs, and launches the FirebaseUI Android demo app.

The script lets you:
1. Use an already connected Android device or emulator.
2. Start an AVD selected from `emulator -list-avds`.
EOF
}

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

collect_connected_devices() {
  CONNECTED_DEVICES=()
  while IFS= read -r serial; do
    if [[ -n "$serial" ]]; then
      CONNECTED_DEVICES+=("$serial")
    fi
  done < <(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
}

collect_available_avds() {
  AVAILABLE_AVDS=()
  while IFS= read -r avd_name; do
    if [[ -n "$avd_name" ]]; then
      AVAILABLE_AVDS+=("$avd_name")
    fi
  done < <(emulator -list-avds 2>/dev/null)
}

prompt_for_choice() {
  local prompt="$1"
  shift
  local options=("$@")
  local selection
  local index=1

  echo "$prompt"
  for option in "${options[@]}"; do
    printf "  %d) %s\n" "$index" "$option"
    index=$((index + 1))
  done

  while true; do
    printf "Select an option [1-%d]: " "${#options[@]}"
    read -r selection

    if [[ "$selection" =~ ^[0-9]+$ ]] && (( selection >= 1 && selection <= ${#options[@]} )); then
      CHOICE_INDEX=$((selection - 1))
      return 0
    fi

    echo "Please enter a number between 1 and ${#options[@]}."
  done
}

is_known_device() {
  local candidate="$1"
  local known
  local index

  for (( index=0; index<${#KNOWN_DEVICE_SERIALS[@]}; index++ )); do
    known="${KNOWN_DEVICE_SERIALS[$index]}"
    if [[ "$known" == "$candidate" ]]; then
      return 0
    fi
  done

  return 1
}

cleanup_on_exit() {
  local exit_code="$?"

  if (( exit_code != 0 )) && (( LAUNCHED_EMULATOR == 1 )) && [[ -n "$EMULATOR_PID" ]]; then
    echo "Stopping emulator started by this script..." >&2
    kill "$EMULATOR_PID" 2>/dev/null || true
  fi

  exit "$exit_code"
}

wait_for_device_boot() {
  local serial="$1"
  local attempt
  local boot_completed

  adb -s "$serial" wait-for-device >/dev/null

  echo "Waiting for $serial to finish booting..."
  for (( attempt=1; attempt<=120; attempt++ )); do
    boot_completed="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot_completed" == "1" ]]; then
      echo "$serial is ready."
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for $serial to boot." >&2
  exit 1
}

start_selected_avd() {
  local avd_name="$1"
  local attempt
  local serial
  local index

  collect_connected_devices
  KNOWN_DEVICE_SERIALS=()
  for (( index=0; index<${#CONNECTED_DEVICES[@]}; index++ )); do
    KNOWN_DEVICE_SERIALS+=("${CONNECTED_DEVICES[$index]}")
  done

  echo "Starting emulator '$avd_name'..."
  echo "Emulator logs: $EMULATOR_LOG"
  emulator -avd "$avd_name" >"$EMULATOR_LOG" 2>&1 &
  EMULATOR_PID=$!
  LAUNCHED_EMULATOR=1

  for (( attempt=1; attempt<=120; attempt++ )); do
    sleep 2
    collect_connected_devices
    for (( index=0; index<${#CONNECTED_DEVICES[@]}; index++ )); do
      serial="${CONNECTED_DEVICES[$index]}"
      case "$serial" in
        emulator-*)
          if ! is_known_device "$serial"; then
            TARGET_SERIAL="$serial"
            wait_for_device_boot "$TARGET_SERIAL"
            return 0
          fi
          ;;
      esac
    done
  done

  echo "Failed to detect the new emulator for AVD '$avd_name'." >&2
  exit 1
}

choose_target_device() {
  local option_labels=()
  local option_types=()
  local option_values=()
  local serial
  local avd_name
  local index

  collect_connected_devices
  collect_available_avds

  for (( index=0; index<${#CONNECTED_DEVICES[@]}; index++ )); do
    serial="${CONNECTED_DEVICES[$index]}"
    option_labels+=("Use connected device: $serial")
    option_types+=("device")
    option_values+=("$serial")
  done

  for (( index=0; index<${#AVAILABLE_AVDS[@]}; index++ )); do
    avd_name="${AVAILABLE_AVDS[$index]}"
    option_labels+=("Start emulator: $avd_name")
    option_types+=("avd")
    option_values+=("$avd_name")
  done

  if (( ${#option_labels[@]} == 0 )); then
    cat >&2 <<'EOF'
No connected Android devices were found, and no AVDs are available.
Connect a device or create an emulator first, then rerun this script.
EOF
    exit 1
  fi

  prompt_for_choice "Choose a target for the demo app:" "${option_labels[@]}"

  case "${option_types[$CHOICE_INDEX]}" in
    device)
      TARGET_SERIAL="${option_values[$CHOICE_INDEX]}"
      ;;
    avd)
      start_selected_avd "${option_values[$CHOICE_INDEX]}"
      ;;
  esac
}

main() {
  local script_dir
  local repo_root
  local apk_path
  local launch_output

  if [[ "${1:-}" == "--help" ]]; then
    usage
    exit 0
  fi

  trap cleanup_on_exit EXIT

  require_command adb
  require_command emulator

  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  repo_root="$(cd "$script_dir/../.." && pwd)"
  apk_path="$repo_root/$APK_RELATIVE_PATH"

  if [[ ! -x "$repo_root/gradlew" ]]; then
    echo "gradlew not found or not executable at $repo_root/gradlew" >&2
    exit 1
  fi

  choose_target_device

  echo "Building debug APK..."
  "$repo_root/gradlew" -p "$repo_root" :app:assembleDebug

  if [[ ! -f "$apk_path" ]]; then
    echo "APK not found at $apk_path" >&2
    exit 1
  fi

  echo "Installing app on $TARGET_SERIAL..."
  adb -s "$TARGET_SERIAL" install -r "$apk_path"

  echo "Launching demo app on $TARGET_SERIAL..."
  launch_output="$(adb -s "$TARGET_SERIAL" shell am start -n "$APP_ID/$MAIN_ACTIVITY")"
  echo "$launch_output"
  if [[ "$launch_output" == *"Error:"* ]]; then
    echo "Failed to launch the demo app." >&2
    exit 1
  fi

  trap - EXIT
}

main "$@"
