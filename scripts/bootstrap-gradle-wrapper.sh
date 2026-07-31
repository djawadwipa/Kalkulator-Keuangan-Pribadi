#!/bin/sh
set -eu

GRADLE_VERSION="9.3.1"
EXPECTED_SHA256="b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13"
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
WRAPPER_DIR="$PROJECT_DIR/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
DOWNLOAD_URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar"

mkdir -p "$WRAPPER_DIR"

verify_checksum() {
    file="$1"
    if command -v sha256sum >/dev/null 2>&1; then
        actual=$(sha256sum "$file" | awk '{print $1}')
    elif command -v shasum >/dev/null 2>&1; then
        actual=$(shasum -a 256 "$file" | awk '{print $1}')
    else
        echo "ERROR: sha256sum atau shasum diperlukan untuk memverifikasi Gradle Wrapper." >&2
        exit 1
    fi

    if [ "$actual" != "$EXPECTED_SHA256" ]; then
        echo "ERROR: SHA-256 Gradle Wrapper tidak cocok." >&2
        echo "Expected: $EXPECTED_SHA256" >&2
        echo "Actual:   $actual" >&2
        exit 1
    fi
}

if [ -f "$WRAPPER_JAR" ]; then
    verify_checksum "$WRAPPER_JAR"
    echo "Gradle Wrapper $GRADLE_VERSION terverifikasi."
    exit 0
fi

TMP_FILE="$WRAPPER_JAR.tmp.$$"
trap 'rm -f "$TMP_FILE"' EXIT INT TERM

echo "Mengunduh Gradle Wrapper $GRADLE_VERSION dari repository resmi Gradle..."
if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error \
        --proto '=https' --tlsv1.2 \
        --retry 3 --retry-delay 2 \
        --output "$TMP_FILE" "$DOWNLOAD_URL"
elif command -v wget >/dev/null 2>&1; then
    wget --https-only --tries=3 --output-document="$TMP_FILE" "$DOWNLOAD_URL"
else
    echo "ERROR: curl atau wget diperlukan untuk mengunduh Gradle Wrapper." >&2
    exit 1
fi

verify_checksum "$TMP_FILE"
mv "$TMP_FILE" "$WRAPPER_JAR"
trap - EXIT INT TERM
echo "Gradle Wrapper $GRADLE_VERSION berhasil dipasang dan SHA-256 terverifikasi."
