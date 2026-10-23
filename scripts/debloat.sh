#!/usr/bin/env bash
# Remove os apps de uma lista SO para o usuario 0 (pm uninstall --user 0). Nada e apagado do sistema:
# scripts/restore.sh <mesma lista> traz tudo de volta, e um reset de fabrica tambem.
# Uso: scripts/debloat.sh devices/xiaomi-mi-tv-stick/debloat.txt
set -euo pipefail
source "$(dirname "$0")/common.sh"
LIST="${1:?uso: $0 <lista.txt>}"
connect
mkdir -p "$ROOT/backup"
adb shell pm list packages > "$ROOT/backup/pacotes-antes-$(date +%Y%m%d-%H%M%S).txt"
grep -v -E '^\s*(#|$)' "$LIST" | awk '{print $1}' | while read -r pkg; do
  # Um comando por chamada: encadear varios "pm" no mesmo shell as vezes para no primeiro.
  printf '%-55s ' "$pkg"
  adb shell pm uninstall -k --user 0 "$pkg" | tr -d '\r'
done
echo "Pronto. Lista de pacotes de antes salva em backup/."
