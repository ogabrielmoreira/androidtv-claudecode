#!/usr/bin/env bash
# Compila o primeiro launcher do projeto (feito do zero, antes de adotar o tv-home). Saida: build/cleanhome.apk
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
SDK="${ANDROID_SDK:-$ROOT/.sdk}"
BT="${BUILD_TOOLS:-$SDK/build-tools}"
JAR="${ANDROID_JAR:-$SDK/platforms/android-28/android.jar}"
OUT="$(mktemp -d /tmp/cleanhome-build.XXXXXX)"
trap 'rm -rf "$OUT"' EXIT
mkdir -p "$OUT/classes" "$HERE/build"

"$BT/aapt2" link -o "$OUT/base.apk" --manifest "$HERE/AndroidManifest.xml" -I "$JAR" \
  --min-sdk-version 21 --target-sdk-version 28 --version-code 1 --version-name 1.0
javac --release 8 -Xlint:-options -cp "$JAR" -d "$OUT/classes" "$HERE"/src/com/gabriel/cleanhome/*.java
"$BT/d8" --lib "$JAR" --min-api 21 --output "$OUT" $(find "$OUT/classes" -name '*.class')
(cd "$OUT" && zip -q -j base.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"
KS="$HERE/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
  -alias cleanhome -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=CleanHome" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$HERE/build/cleanhome.apk" "$OUT/aligned.apk"
echo "ok: $HERE/build/cleanhome.apk"
