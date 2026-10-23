#!/usr/bin/env bash
# Compila o launcher sem Gradle (aapt2 + javac + d8 + apksigner).
# Requisitos: JDK 17+ e o SDK baixado por scripts/setup-sdk.sh (ou ANDROID_SDK apontando para um SDK seu).
# Saida: launcher/build/tvhome.apk
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
SDK="${ANDROID_SDK:-$ROOT/.sdk}"
# build-tools 35+ e obrigatorio: o d8 do 34 quebra com classes do JDK 21 (NullPointerException em classes anonimas).
BT="${BUILD_TOOLS:-$SDK/build-tools}"
JAR="${ANDROID_JAR:-$SDK/platforms/android-28/android.jar}"
[ -x "$BT/aapt2" ] || { echo "build-tools nao encontrado em $BT (rode scripts/setup-sdk.sh)"; exit 1; }
[ -f "$JAR" ] || { echo "android.jar (API 28) nao encontrado em $JAR (rode scripts/setup-sdk.sh)"; exit 1; }

# Pasta temporaria sem espacos: o wrapper do d8 quebra com caminhos como "Application Support".
OUT="$(mktemp -d /tmp/tvhome-build.XXXXXX)"
trap 'rm -rf "$OUT"' EXIT
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$HERE/build"

"$BT/aapt2" compile --dir "$HERE/res" -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest "$HERE/AndroidManifest.xml" --java "$OUT/gen" \
  -A "$HERE/assets" --min-sdk-version 28 --target-sdk-version 28 "$OUT/res.zip"
javac -nowarn -Xlint:-options --release 11 -classpath "$JAR" -d "$OUT/classes" \
  "$HERE"/src/com/mihir/tvhome/*.java $(find "$OUT/gen" -name '*.java')
"$BT/d8" --min-api 28 --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
(cd "$OUT/dex" && zip -q -u ../base.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/base.apk" "$OUT/aligned.apk"

KS="$HERE/debug.keystore"
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
  -alias tvhome -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=tvhome" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$HERE/build/tvhome.apk" "$OUT/aligned.apk"
echo "ok: $HERE/build/tvhome.apk ($(wc -c < "$HERE/build/tvhome.apk") bytes)"
