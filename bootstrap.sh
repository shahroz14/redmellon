#!/usr/bin/env bash
# Run once to download the Gradle wrapper jar. Requires curl and a JDK 17+.
set -euo pipefail

GRADLE_VERSION=8.5
WRAPPER_JAR=gradle/wrapper/gradle-wrapper.jar
WRAPPER_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

if [ -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper jar already present."
  exit 0
fi

echo "Downloading Gradle ${GRADLE_VERSION}..."
TMP_ZIP=$(mktemp /tmp/gradle-XXXX.zip)
curl -L "$WRAPPER_URL" -o "$TMP_ZIP"

TMP_DIR=$(mktemp -d)
unzip -q "$TMP_ZIP" -d "$TMP_DIR"
GRADLE_BIN="${TMP_DIR}/gradle-${GRADLE_VERSION}/bin/gradle"

echo "Generating wrapper..."
"$GRADLE_BIN" wrapper --gradle-version "$GRADLE_VERSION"

rm -rf "$TMP_DIR" "$TMP_ZIP"
echo "Done. You can now use ./gradlew"
