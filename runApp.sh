#!/usr/bin/env bash
set -euo pipefail

# === Config you can override when calling ===
AVD_NAME="${AVD_NAME:-Pixel_7_API_34}"
GPU_MODE="${GPU_MODE:-off}"            # host | swiftshader_indirect | off
ADB_BIN="${ADB_BIN:-adb}"
EMULATOR_BIN="${EMULATOR_BIN:-emulator}"
GRADLE_BIN="./gradlew"

# Timeout (seconds) for emulator boot
BOOT_TIMEOUT="${BOOT_TIMEOUT:-180}"

# === Go to project root (folder of this script) ===
cd "$(dirname "$0")"

# === Ensure Android tools are on PATH ===
if [ -d "$HOME/Android/Sdk/platform-tools" ]; then
  export PATH="$PATH:$HOME/Android/Sdk/platform-tools:$HOME/Android/Sdk/emulator"
elif [ -d "$ANDROID_HOME/platform-tools" ]; then
  export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"
elif [ -d "$ANDROID_SDK_ROOT/platform-tools" ]; then
  export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator"
elif [ -d "$HOME/android-sdk/platform-tools" ]; then
  export PATH="$PATH:$HOME/android-sdk/platform-tools:$HOME/android-sdk/emulator"
fi

log() {
  echo -e "\e[34m==> $*\e[0m"
}

err() {
  echo -e "\e[31m[ERROR]\e[0m $*" >&2
}

# === Helpers: check devices ===
get_online_device() {
  # prints first "device" line, empty if none
  "$ADB_BIN" devices | awk 'NR>1 && $2=="device"{print $1; exit 0}'
}

wait_for_device() {
  local timeout="$1"
  local waited=0

  log "Waiting for an adb device (timeout ${timeout}s)…"

  while [ "$waited" -lt "$timeout" ]; do
    local dev
    dev="$(get_online_device || true)"
    if [ -n "$dev" ]; then
      log "Found online device: $dev"
      return 0
    fi
    sleep 3
    waited=$(( waited + 3 ))
  done

  err "No online device found after ${timeout}s."
  "$ADB_BIN" devices -l || true
  return 1
}

wait_for_boot_completed() {
  local timeout="$1"
  local waited=0

  log "Waiting for Android to finish booting (sys.boot_completed)…"

  while [ "$waited" -lt "$timeout" ]; do
    if "$ADB_BIN" shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
      log "Emulator boot completed."
      return 0
    fi
    sleep 3
    waited=$(( waited + 3 ))
  done

  err "sys.boot_completed never reached 1 within ${timeout}s."
  return 1
}

start_emulator_if_needed() {
  local dev
  dev="$(get_online_device || true)"

  if [ -n "$dev" ]; then
    log "Device already online: $dev (won't start emulator)."
    return 0
  fi

  log "No device online. Starting emulator: $AVD_NAME (GPU: $GPU_MODE)…"

  # Start emulator in the background and log its output
  "$EMULATOR_BIN" -avd "$AVD_NAME" -gpu "$GPU_MODE" -no-snapshot-load \
    >/tmp/emulator.log 2>&1 &

  EMU_PID=$!
  log "Emulator started with PID $EMU_PID (logging to /tmp/emulator.log)"

  # Give the emulator a few seconds to spawn and register with adb
  sleep 10

  # Wait until adb sees a 'device'
  if ! wait_for_device "$BOOT_TIMEOUT"; then
    err "adb never saw a device. Dumping last emulator log lines:"
    tail -n 40 /tmp/emulator.log || true
    err "Killing emulator PID $EMU_PID"
    kill "$EMU_PID" 2>/dev/null || true
    exit 1
  fi

  # Optional but nice: wait until Android finishes booting
  if ! wait_for_boot_completed "$BOOT_TIMEOUT"; then
    err "sys.boot_completed was never 1. Recent emulator log:"
    tail -n 40 /tmp/emulator.log || true
    exit 1
  fi
}


# === MAIN FLOW ===

log "Checking adb…"
"$ADB_BIN" start-server

log "Ensuring emulator/device is ready…"
start_emulator_if_needed

log "Building debug APK…"
$GRADLE_BIN assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
  err "APK not found at $APK"
  exit 1
fi

log "Installing $APK …"
"$ADB_BIN" install -r "$APK"

log "Launching Whisper app…"
"$ADB_BIN" shell am start -n com.whisper.whisperandroid/.MainActivity

log "✅ Done."

