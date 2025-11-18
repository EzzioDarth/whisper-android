#!/bin/bash

# Whisper Android helper script
# Usage: ./whisper.sh [command]

# Hardcoded project root so you can run from anywhere
PROJECT_DIR="$HOME/whisper-android"
GRADLE="$PROJECT_DIR/gradlew"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
RUNAPP="$PROJECT_DIR/runApp.sh"
SERVE_DIR="$PROJECT_DIR/app/build/outputs/apk/debug"

COLOR_BLUE="\e[34m"
COLOR_GREEN="\e[32m"
COLOR_RED="\e[31m"
NC="\e[0m"

print() {
    echo -e "${COLOR_BLUE}==> $1${NC}"
}

error() {
    echo -e "${COLOR_RED}[ERROR] $1${NC}" >&2
}

case "$1" in

# -------------------------------------------------------
# Build / pipeline
# -------------------------------------------------------

build)
    if [ ! -x "$RUNAPP" ]; then
        error "runApp.sh not found or not executable at: $RUNAPP"
        echo "Make sure runApp.sh exists and run: chmod +x runApp.sh"
        exit 1
    fi

    print "Running full pipeline via runApp.sh (emulator + build + install + launch)..."
    "$RUNAPP"
    ;;

clean)
    print "Cleaning project..."
    (cd "$PROJECT_DIR" && $GRADLE clean)
    rm -rf ~/.gradle/caches/
    ;;

rebuild)
    print "Cleaning + rebuilding (assembleDebug only)..."
    (cd "$PROJECT_DIR" && $GRADLE clean assembleDebug)
    ;;

# -------------------------------------------------------
# Device / install helpers
# -------------------------------------------------------

install)
    print "Installing Debug APK..."
    adb install -r "$APK_PATH"
    ;;

run)
    print "Building + installing + running (no emulator start)..."
    (cd "$PROJECT_DIR" && $GRADLE installDebug)
    adb shell monkey -p com.whisper.whisperandroid 1
    ;;

devices)
    print "Connected devices:"
    adb devices -l
    ;;

kill-adb)
    print "Restarting adb server..."
    adb kill-server
    adb start-server
    ;;

kill-emulator)
    print "Killing all emulators..."
    adb -s emulator-5554 emu kill 2>/dev/null
    pkill qemu-system 2>/dev/null
    ;;

# -------------------------------------------------------
# Logs
# -------------------------------------------------------

logs)
    print "Filtering logcat for Whisper..."
    adb logcat | grep -i "Whisper"
    ;;

full-logs)
    print "Full logcat..."
    adb logcat
    ;;

clear-logs)
    print "Clearing logcat..."
    adb logcat -c
    ;;

# -------------------------------------------------------
# PocketBase helpers
# -------------------------------------------------------

pb)
    print "Checking PocketBase health..."
    curl -I http://localhost:8090/api/health
    ;;

pb-logs)
    print "Docker logs (PocketBase)"
    docker compose -f "$PROJECT_DIR/docker-compose.yml" logs -f
    ;;

pb-restart)
    print "Restarting docker..."
    docker compose -f "$PROJECT_DIR/docker-compose.yml" down
    docker compose -f "$PROJECT_DIR/docker-compose.yml" up -d
    ;;

# -------------------------------------------------------
# Serve APK to phone (NEW)
# -------------------------------------------------------

serve-apk)
    print "Building APK..."
    (cd "$PROJECT_DIR" && $GRADLE assembleDebug)

    if [ ! -d "$SERVE_DIR" ]; then
        error "APK output directory not found: $SERVE_DIR"
        exit 1
    fi

    print "Starting HTTP server on port 8000"
    PHONE_URL="http://$(hostname -I | awk '{print $1}'):8000/app-debug.apk"
    echo -e "${COLOR_GREEN}Download on your phone using this link:${NC}"
    echo -e "${COLOR_GREEN}$PHONE_URL${NC}"

    cd "$SERVE_DIR"
    python3 -m http.server 8000
    ;;

# -------------------------------------------------------
# Git helpers
# -------------------------------------------------------

git-status)
    (cd "$PROJECT_DIR" && git status)
    ;;

git-pull)
    print "Pulling main safely..."
    (cd "$PROJECT_DIR" && git stash push -m "auto")
    (cd "$PROJECT_DIR" && git pull origin main)
    (cd "$PROJECT_DIR" && git stash pop)
    ;;

git-push)
    print "Pushing dev branch..."
    (cd "$PROJECT_DIR" && git push origin dev)
    ;;

# -------------------------------------------------------
# Help
# -------------------------------------------------------

*)
    echo -e "${COLOR_GREEN}Whisper helpers available:${NC}"
    echo "
    Build / Pipeline:
      build               Full pipeline via runApp.sh (emulator + build + install + run)
      clean               Clean Gradle
      rebuild             Clean + assemble (no install)

    Install:
      install             Install debug APK
      run                 Build & run (no emulator)
      devices             List adb devices
      kill-adb            Restart adb
      kill-emulator       Kill emulator

    Logs:
      logs                Filtered whisper logs
      full-logs           Full logcat
      clear-logs          Clear logcat

    PocketBase:
      pb                  Health check
      pb-logs             Container logs
      pb-restart          Restart PB docker

    APK server (NEW):
      serve-apk           Build APK + start HTTP server (download on phone)

    Git:
      git-status          Status
      git-pull            Stash → pull main
      git-push            Push dev
    "
    ;;
esac

