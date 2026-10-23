#!/usr/bin/env bash
# Troca a tela inicial entre a original do Android TV e a customizada.
#   scripts/switch-interface.sh android-tv   -> volta para a tela original
#   scripts/switch-interface.sh nova         -> volta para a customizada
set -euo pipefail
source "$(dirname "$0")/common.sh"
connect
case "${1:-}" in
  android-tv)
    adb shell pm enable "$STOCK_LAUNCHER"
    ;;
  nova)
    adb shell pm disable-user --user 0 "$STOCK_LAUNCHER"
    adb shell cmd package set-home-activity "$LAUNCHER_HOME"
    ;;
  *) echo "uso: $0 android-tv | nova"; exit 1 ;;
esac
adb shell input keyevent KEYCODE_HOME
sleep 2
echo "Tela atual: $(adb shell dumpsys window | grep mCurrentFocus | tr -d '\r')"
