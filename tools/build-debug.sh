#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_VERSION="9.5.0"
CACHE_ROOT="$PROJECT_ROOT/.gradle-local"
GRADLE_HOME="$CACHE_ROOT/gradle-$GRADLE_VERSION"
ZIP_PATH="$CACHE_ROOT/gradle-$GRADLE_VERSION-bin.zip"

if [[ ! -x "$GRADLE_HOME/bin/gradle" ]]; then
  mkdir -p "$CACHE_ROOT"
  if [[ ! -f "$ZIP_PATH" ]]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP_PATH"
  fi
  echo "Extracting Gradle $GRADLE_VERSION..."
  unzip -oq "$ZIP_PATH" -d "$CACHE_ROOT"
fi

echo "Building Blake Labs Guitar Tuner (debug)..."
"$GRADLE_HOME/bin/gradle" -p "$PROJECT_ROOT" :app:assembleDebug "$@"
echo
echo "APK: $PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
