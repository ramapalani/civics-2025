#!/usr/bin/env bash
# Signed release APK for installing on a personal phone.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ -z "${JAVA_HOME_OVERRIDE:-}" ]]; then
  if [[ -x "/opt/homebrew/opt/openjdk@17/bin/java" ]]; then
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  elif [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]]; then
    JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  fi
else
  JAVA_HOME="$JAVA_HOME_OVERRIDE"
fi
export JAVA_HOME ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME_OVERRIDE:-$HOME/.gradle}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

STORE="$HOME/.android/uscis-civics-release.jks"
ALIAS="uscis"
PROPS="$ROOT/local.properties"

mkdir -p "$HOME/.android"

if [[ -f "$PROPS" ]] && grep -q '^RELEASE_STORE_PASSWORD=' "$PROPS"; then
  STORE_PASS="$(awk -F= '/^RELEASE_STORE_PASSWORD=/{print substr($0, index($0,$2))}' "$PROPS")"
  KEY_PASS="$(awk -F= '/^RELEASE_KEY_PASSWORD=/{print substr($0, index($0,$2))}' "$PROPS")"
  ALIAS="$(awk -F= '/^RELEASE_KEY_ALIAS=/{print substr($0, index($0,$2))}' "$PROPS")"
  EXISTING_STORE="$(awk -F= '/^RELEASE_STORE_FILE=/{print substr($0, index($0,$2))}' "$PROPS")"
  if [[ -n "$EXISTING_STORE" ]]; then
    STORE="$EXISTING_STORE"
  fi
else
  STORE_PASS="$(openssl rand -base64 18 | tr -d '/+=' | head -c 24)"
  KEY_PASS="$STORE_PASS"
fi

if [[ ! -f "$STORE" ]]; then
  echo "Creating release keystore at $STORE"
  keytool -genkeypair -v \
    -keystore "$STORE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS" \
    -dname "CN=Civics 2025, OU=Personal, O=Personal, L=Unknown, ST=Unknown, C=US"
fi

touch "$PROPS"
if [[ -f "$PROPS" ]] && grep -q '^sdk.dir=' "$PROPS"; then
  sed -i.bak "s|^sdk.dir=.*|sdk.dir=$ANDROID_HOME|" "$PROPS"
  rm -f "$PROPS.bak"
else
  echo "sdk.dir=$ANDROID_HOME" >> "$PROPS"
fi

upsert() {
  local key="$1" value="$2"
  if grep -q "^${key}=" "$PROPS"; then
    sed -i.bak "s|^${key}=.*|${key}=${value}|" "$PROPS"
    rm -f "$PROPS.bak"
  else
    echo "${key}=${value}" >> "$PROPS"
  fi
}
upsert RELEASE_STORE_FILE "$STORE"
upsert RELEASE_STORE_PASSWORD "$STORE_PASS"
upsert RELEASE_KEY_ALIAS "$ALIAS"
upsert RELEASE_KEY_PASSWORD "$KEY_PASS"

TRUST="$GRADLE_USER_HOME/uscis-civics-corp-cacerts.jks"
JVMARGS="-Xmx2048m -Dfile.encoding=UTF-8"
if [[ -f "$TRUST" ]]; then
  JVMARGS="$JVMARGS -Djavax.net.ssl.trustStore=$TRUST -Djavax.net.ssl.trustStorePassword=changeit"
  export GRADLE_OPTS="${GRADLE_OPTS:-} -Djavax.net.ssl.trustStore=$TRUST -Djavax.net.ssl.trustStorePassword=changeit"
fi

GRADLE_BIN="${GRADLE_BIN:-/opt/homebrew/bin/gradle}"
echo "Building signed release APK and Play App Bundle..."
"$GRADLE_BIN" -p "$ROOT" --no-daemon \
  -Dorg.gradle.java.home="$JAVA_HOME" \
  -Dorg.gradle.jvmargs="$JVMARGS" \
  :app:assembleRelease \
  :app:bundleRelease

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
AAB="$ROOT/app/build/outputs/bundle/release/app-release.aab"
if [[ ! -f "$APK" ]]; then
  echo "Release APK was not produced." >&2
  exit 1
fi
echo
echo "Release APK (sideload): $APK"
ls -lh "$APK"
if [[ -f "$AAB" ]]; then
  echo "Play App Bundle (upload this): $AAB"
  ls -lh "$AAB"
else
  echo "Play App Bundle was not produced." >&2
  exit 1
fi
MAPPING="$ROOT/app/build/outputs/mapping/release/mapping.txt"
if [[ -f "$MAPPING" ]]; then
  echo "R8 mapping (upload in Play Console with the release for readable crashes): $MAPPING"
fi

ADB="$ANDROID_HOME/platform-tools/adb"
if [[ -x "$ADB" ]]; then
  echo
  echo "Connected devices:"
  "$ADB" devices -l
  PHONES="$("$ADB" devices | awk 'NR>1 && $2=="device" && $1 !~ /^emulator-/{print $1}')"
  if [[ -n "$PHONES" ]]; then
    echo "Installing on phone(s)..."
    for serial in $PHONES; do
      "$ADB" -s "$serial" install -r "$APK" || {
        echo "Uninstalling previous builds (old and new package ids)..."
        "$ADB" -s "$serial" uninstall com.ramapalani.usciscivics2025 || true
        "$ADB" -s "$serial" uninstall com.ramapalani.civics2025 || true
        "$ADB" -s "$serial" install "$APK"
      }
    done
  else
    echo "No physical phone attached. Plug it in with USB debugging on, then:"
    echo "  $ADB install -r $APK"
  fi
fi
