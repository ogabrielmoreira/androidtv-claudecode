#!/usr/bin/env bash
# Instala o launcher (launcher/build/tvhome.apk) e o deixa como tela inicial:
#  1. instala/atualiza o APK
#  2. desativa (nao apaga) o launcher original, que tem prioridade de sistema no botao Home
#  3. liga o servico "Botoes do controle" (botao Apps -> Mais apps; Prime Video -> YouTube)
#  4. aperta Home
# Desfazer: scripts/switch-interface.sh android-tv
set -euo pipefail
source "$(dirname "$0")/common.sh"
APK="${1:-$ROOT/launcher/build/tvhome.apk}"
[ -f "$APK" ] || { echo "APK nao encontrado: $APK (rode launcher/build.sh)"; exit 1; }
connect
if ! OUT="$(adb install -r "$APK" 2>&1)"; then
  # APK assinado com outra chave (ex.: compilado em outro computador): so da para instalar do zero.
  if echo "$OUT" | grep -q -E "UPDATE_INCOMPATIBLE|signatures do not match"; then
    echo "Assinatura diferente da versao instalada: reinstalando do zero."
    echo "Depois rode scripts/push-branding.sh de novo (logo e papeis de parede sao apagados)."
    adb uninstall "$LAUNCHER_PKG"
    adb install "$APK"
  else
    echo "$OUT"; exit 1
  fi
fi
adb shell pm disable-user --user 0 "$STOCK_LAUNCHER" || true
# Primeiro launcher do projeto (extras/clean-home-launcher), se estiver instalado.
adb shell pm disable-user --user 0 com.gabriel.cleanhome >/dev/null 2>&1 || true
adb shell cmd package set-home-activity "$LAUNCHER_HOME"
enable_buttons_service
adb shell am force-stop com.android.tv.settings
adb shell input keyevent KEYCODE_HOME
sleep 3
echo "Tela atual: $(adb shell dumpsys window | grep mCurrentFocus | tr -d '\r')"
