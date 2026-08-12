#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Building Blake Labs Guitar Tuner (debug)..."
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon :app:assembleDebug "$@"
echo
echo "APK: $PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
