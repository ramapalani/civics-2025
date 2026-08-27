#!/usr/bin/env bash
# Command-line verify build for the USCIS Civics 2025 Android app.
# Uses Android Studio JBR (or Homebrew JDK), Homebrew Gradle, and the local Android SDK.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Prefer Homebrew 17 for Kotlin/KSP. Studio JBR is 25 and breaks Room KSP.
if [[ -z "${JAVA_HOME_OVERRIDE:-}" ]]; then
  if [[ -x "/opt/homebrew/opt/openjdk@17/bin/java" ]]; then
    JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  elif [[ -x "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java" ]]; then
    JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  else
    echo "No usable JDK found." >&2
    exit 1
  fi
else
  JAVA_HOME="$JAVA_HOME_OVERRIDE"
fi
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "No usable JDK at $JAVA_HOME" >&2
  echo "Install Android Studio, or: brew install openjdk@17" >&2
  exit 1
fi

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
if [[ ! -d "$ANDROID_HOME/platforms" ]]; then
  echo "Android SDK not found at $ANDROID_HOME" >&2
  echo "Open Android Studio once and install SDK Platform 37." >&2
  exit 1
fi

if [[ ! -d "$ANDROID_HOME/platforms/android-37.0" && ! -d "$ANDROID_HOME/platforms/android-37" ]]; then
  echo "compileSdk 37 is not installed in $ANDROID_HOME/platforms" >&2
  echo "In Android Studio: Settings → Languages & Frameworks → Android SDK → SDK Platforms → API 37" >&2
  exit 1
fi

GRADLE_BIN="${GRADLE_BIN:-/opt/homebrew/bin/gradle}"
if [[ ! -x "$GRADLE_BIN" ]]; then
  GRADLE_BIN="$(command -v gradle || true)"
fi
if [[ -z "$GRADLE_BIN" ]]; then
  echo "Gradle not found. Install with: brew install gradle" >&2
  exit 1
fi

export JAVA_HOME
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME_OVERRIDE:-$HOME/.gradle}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
mkdir -p "$GRADLE_USER_HOME"

# Corporate SSL inspection (Zscaler) presents a custom CA. curl trusts
# /etc/ssl/certs/ca-bundle.pem, but Gradle's JDK does not unless we import it.
ensure_truststore() {
  local pem="${SSL_CERT_FILE:-${CURL_CA_BUNDLE:-/etc/ssl/certs/ca-bundle.pem}}"
  local store="$GRADLE_USER_HOME/uscis-civics-corp-cacerts.jks"
  local stamp="$GRADLE_USER_HOME/uscis-civics-corp-cacerts.stamp"
  if [[ ! -f "$pem" ]]; then
    return 0
  fi
  if [[ -f "$store" && -f "$stamp" && ! "$pem" -nt "$stamp" ]]; then
    echo "$store"
    return 0
  fi
  echo "Importing CA bundle into a Gradle truststore (once)..." >&2
  local tmp
  tmp="$(mktemp -d)"
  awk -v dir="$tmp" '
    /BEGIN CERTIFICATE/ { n++; fn = sprintf("%s/%03d.pem", dir, n) }
    n > 0 { print > fn }
  ' "$pem"
  rm -f "$store"
  local n=0
  local f
  for f in "$tmp"/*.pem; do
    n=$((n + 1))
    "$JAVA_HOME/bin/keytool" -importcert -noprompt \
      -alias "ca$n" \
      -file "$f" \
      -keystore "$store" \
      -storepass changeit >/dev/null 2>&1 || true
  done
  rm -rf "$tmp"
  date > "$stamp"
  echo "$store"
}

TRUST_STORE="$(ensure_truststore)"
EXTRA_JVM=()
if [[ -n "$TRUST_STORE" && -f "$TRUST_STORE" ]]; then
  EXTRA_JVM+=(
    "-Djavax.net.ssl.trustStore=$TRUST_STORE"
    "-Djavax.net.ssl.trustStorePassword=changeit"
  )
  echo "Using truststore $TRUST_STORE"
fi

if [[ -f "$ROOT/local.properties" ]]; then
  if grep -q '^sdk.dir=' "$ROOT/local.properties"; then
    sed -i.bak "s|^sdk.dir=.*|sdk.dir=$ANDROID_HOME|" "$ROOT/local.properties"
    rm -f "$ROOT/local.properties.bak"
  else
    echo "sdk.dir=$ANDROID_HOME" >> "$ROOT/local.properties"
  fi
else
  printf 'sdk.dir=%s\n' "$ANDROID_HOME" > "$ROOT/local.properties"
fi

echo "JAVA_HOME=$JAVA_HOME"
"$JAVA_HOME/bin/java" -version
echo "ANDROID_HOME=$ANDROID_HOME"
echo "GRADLE_USER_HOME=$GRADLE_USER_HOME"
echo "Gradle: $GRADLE_BIN ($("$GRADLE_BIN" -v | awk '/^Gradle /{print $2; exit}'))"
echo

JVMARGS="-Xmx2048m -Dfile.encoding=UTF-8"
if [[ -n "$TRUST_STORE" && -f "$TRUST_STORE" ]]; then
  JVMARGS="$JVMARGS -Djavax.net.ssl.trustStore=$TRUST_STORE -Djavax.net.ssl.trustStorePassword=changeit"
  export GRADLE_OPTS="${GRADLE_OPTS:-} -Djavax.net.ssl.trustStore=$TRUST_STORE -Djavax.net.ssl.trustStorePassword=changeit"
fi

"$GRADLE_BIN" -p "$ROOT" --no-daemon \
  -Dorg.gradle.java.home="$JAVA_HOME" \
  -Dorg.gradle.jvmargs="$JVMARGS" \
  :core:test \
  :app:assembleDebug \
  "$@"

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK" ]]; then
  echo
  echo "Build OK"
  echo "APK: $APK"
  ls -lh "$APK"
else
  echo "Build finished but APK was not found at $APK" >&2
  exit 1
fi
