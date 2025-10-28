#!/usr/bin/env bash
set -euo pipefail

# === Config you can override when calling ===
AVD_NAME="${AVD_NAME:-Pixel_5_API_34}"
GPU_MODE="${GPU_MODE:-off}"            # host | swiftshader | off

# === Go to project root (folder of this script) ===
cd "$(dirname "$0")"

# === Ensure Android tools are on PATH (handles both common SDK locations) ===
if [ -d "$HOME/Android/Sdk/platform-tools" ]; then
  export PATH="$PATH:$HOME/Android/Sdk/platform-tools:$HOME/Android/Sdk/emulator"
elif [ -d "$HOME/android-sdk/platform-tools" ]; then
  export PATH="$PATH:$HOME/android-sdk/platform-tools:$HOME/android-sdk/emulator"
fi

# === Start emulator if none running ===
if ! adb devices 2>/dev/null | grep -q "emulator-"; then
  echo "Starting emulator: $AVD_NAME (gpu=$GPU_MODE)"
  nohup emulator -avd "$AVD_NAME" -gpu "$GPU_MODE" -no-snapshot-load >/dev/null 2>&1 &
fi

# === Wait for device to be ready ===
echo "Waiting for emulator to connect…"
adb wait-for-device

# Wait until Android reports boot completed
echo "Waiting for Android to finish booting…"
until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 3
done
adb shell input keyevent 82 || true   # wake/unlock just in case

# === Build APK ===
./gradlew assembleDebug

# === Install & run ===
APK="app/build/outputs/apk/debug/app-debug.apk"
echo "Installing $APK …"
adb install -r "$APK"

echo "Launching app…"
adb shell am start -n com.whisper.whisperandroid/.MainActivity

echo "✅ Done."

