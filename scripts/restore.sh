#!/usr/bin/env bash
# Desfaz o debloat: reinstala para o usuario 0 os apps de fabrica da lista.
# Uso: scripts/restore.sh devices/xiaomi-mi-tv-stick/debloat.txt   (ou um pacote: scripts/restore.sh com.netflix.ninja)
set -euo pipefail
source "$(dirname "$0")/common.sh"
ARG="${1:?uso: $0 <lista.txt | pacote>}"
connect
if [ -f "$ARG" ]; then PKGS="$(grep -v -E '^\s*(#|$)' "$ARG" | awk '{print $1}')"; else PKGS="$ARG"; fi
for pkg in $PKGS; do
  printf '%-55s ' "$pkg"
  adb shell cmd package install-existing "$pkg" | tr -d '\r'
done
