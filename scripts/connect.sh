#!/usr/bin/env bash
# Conecta na TV por ADB pela rede e mostra modelo, Android e memoria.
# Na 1a vez a TV mostra "Permitir depuracao USB?": marque "Sempre permitir" e confirme.
set -euo pipefail
source "$(dirname "$0")/common.sh"
"$ADB_BIN" connect "$TV"
sleep 2
"$ADB_BIN" devices
echo "modelo:   $(adb shell getprop ro.product.model | tr -d '\r')"
echo "android:  $(adb shell getprop ro.build.version.release | tr -d '\r')"
adb shell "grep -E 'MemTotal|MemAvailable' /proc/meminfo"
echo "launcher: $(adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1 | tr -d '\r')"
