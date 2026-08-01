#!/usr/bin/env bash
set -euo pipefail

SDKMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/avdmanager"
EMULATOR="${ANDROID_HOME}/emulator/emulator"

if [[ ! -x "$SDKMANAGER" ]]; then
  SDKMANAGER="$(command -v sdkmanager)"
fi
if [[ ! -x "$AVDMANAGER" ]]; then
  AVDMANAGER="$(command -v avdmanager)"
fi

SYSTEM_IMAGE="system-images;android-35;google_apis;x86_64"
AVD_NAME="kkp-ci-api-35"

set +o pipefail
yes | "$SDKMANAGER" \
  "platform-tools" \
  "emulator" \
  "platforms;android-35" \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "$SYSTEM_IMAGE"
set -o pipefail

if [[ -e /dev/kvm ]]; then
  sudo chmod 666 /dev/kvm
fi

mkdir -p "$HOME/.android"
touch "$HOME/.android/repositories.cfg"
echo "no" | "$AVDMANAGER" create avd \
  --force \
  --name "$AVD_NAME" \
  --package "$SYSTEM_IMAGE" \
  --device "pixel_6"

"$EMULATOR" \
  -avd "$AVD_NAME" \
  -no-window \
  -noaudio \
  -no-boot-anim \
  -no-snapshot \
  -wipe-data \
  -camera-back none \
  -camera-front none \
  -gpu swiftshader_indirect \
  > emulator.log 2>&1 &
EMULATOR_PID=$!

cleanup() {
  adb emu kill >/dev/null 2>&1 || true
  kill "$EMULATOR_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

adb wait-for-device
BOOTED=false
for _ in $(seq 1 180); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)" == "1" ]]; then
    BOOTED=true
    break
  fi
  sleep 2
done

if [[ "$BOOTED" != "true" ]]; then
  echo "Android emulator tidak selesai boot dalam enam menit." >&2
  tail -n 200 emulator.log >&2 || true
  exit 1
fi

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell input keyevent 82
adb shell cmd package list packages >/dev/null

./gradlew --no-daemon --stacktrace connectedDebugAndroidTest
