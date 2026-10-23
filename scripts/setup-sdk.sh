#!/usr/bin/env bash
# Baixa so o necessario do SDK oficial do Google para .sdk/ (nada e instalado no sistema):
#   platform-tools (adb), build-tools 36.1 (aapt2, d8, apksigner) e a plataforma Android 9 (API 28).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$ROOT/.sdk"
BASE=https://dl.google.com/android/repository
case "$(uname -s)" in
  Darwin) PT=platform-tools-latest-darwin.zip; BTZ=build-tools_r36.1_macosx.zip ;;
  Linux)  PT=platform-tools-latest-linux.zip;  BTZ=build-tools_r36.1_linux.zip ;;
  *) echo "Sistema nao suportado: $(uname -s)"; exit 1 ;;
esac
command -v javac >/dev/null || { echo "Instale um JDK 17+ antes (ex.: Adoptium Temurin)."; exit 1; }

mkdir -p "$SDK/platforms"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cd "$TMP"

if [ ! -x "$SDK/platform-tools/adb" ]; then
  echo "baixando platform-tools..."; curl -fsSL -o pt.zip "$BASE/$PT"; unzip -q pt.zip
  rm -rf "$SDK/platform-tools"; mv platform-tools "$SDK/"
fi
if [ ! -x "$SDK/build-tools/aapt2" ]; then
  echo "baixando build-tools 36.1..."; curl -fsSL -o bt.zip "$BASE/$BTZ"; mkdir bt; unzip -q bt.zip -d bt
  rm -rf "$SDK/build-tools"; mv bt/* "$SDK/build-tools"
fi
if [ ! -f "$SDK/platforms/android-28/android.jar" ]; then
  echo "baixando plataforma Android 9 (API 28)..."; curl -fsSL -o p28.zip "$BASE/platform-28_r06.zip"; mkdir p28; unzip -q p28.zip -d p28
  rm -rf "$SDK/platforms/android-28"; mv p28/* "$SDK/platforms/android-28"
fi
echo "SDK pronto em $SDK"
echo "adb: $SDK/platform-tools/adb"
