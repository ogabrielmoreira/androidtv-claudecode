#!/usr/bin/env bash
# Tira um print da tela da TV e salva em shots/.
set -euo pipefail
source "$(dirname "$0")/common.sh"
connect
mkdir -p "$ROOT/shots"
OUT="$ROOT/shots/tv-$(date +%Y%m%d-%H%M%S).png"
adb exec-out screencap -p > "$OUT"
echo "$OUT"
