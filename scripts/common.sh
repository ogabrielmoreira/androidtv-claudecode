#!/usr/bin/env bash
# Funcoes comuns aos scripts. Use: export TV=192.168.1.44:5555 (ou salve o endereco no arquivo .tv na raiz).
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [ -z "${TV:-}" ] && [ -f "$ROOT/.tv" ]; then TV="$(cat "$ROOT/.tv")"; fi
if [ -z "${TV:-}" ]; then echo "Defina o IP da TV: export TV=192.168.1.X:5555"; exit 1; fi
case "$TV" in *:*) ;; *) TV="$TV:5555" ;; esac

if [ -x "$ROOT/.sdk/platform-tools/adb" ]; then ADB_BIN="$ROOT/.sdk/platform-tools/adb"
elif command -v adb >/dev/null; then ADB_BIN="$(command -v adb)"
else echo "adb nao encontrado (rode scripts/setup-sdk.sh)"; exit 1; fi

LAUNCHER_PKG=com.mihir.tvhome
LAUNCHER_HOME=$LAUNCHER_PKG/.MainActivity
BUTTONS_SVC=$LAUNCHER_PKG/$LAUNCHER_PKG.RemoteButtonsService
# Launcher original do Android TV (Google TV usa com.google.android.apps.tv.launcherx).
STOCK_LAUNCHER="${STOCK_LAUNCHER:-com.google.android.tvlauncher}"

adb() { "$ADB_BIN" -s "$TV" "$@"; }
connect() { "$ADB_BIN" connect "$TV" >/dev/null 2>&1 || true; }

# Liga o servico "Botoes do controle". Zerar e gravar de novo forca o Android a religar o servico,
# que ele desliga sempre que o launcher e reinstalado.
enable_buttons_service() {
  local cur new
  cur="$(adb shell settings get secure enabled_accessibility_services | tr -d '\r')"
  case "$cur" in
    null|"") new="$BUTTONS_SVC" ;;
    *"$BUTTONS_SVC"*) new="$cur" ;;
    *) new="$cur:$BUTTONS_SVC" ;;
  esac
  adb shell settings put secure enabled_accessibility_services '""'
  sleep 1
  adb shell settings put secure enabled_accessibility_services "$new"
  adb shell settings put secure accessibility_enabled 1
}
