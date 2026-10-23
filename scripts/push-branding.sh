#!/usr/bin/env bash
# Envia a logo (marca d'agua) e, opcionalmente, seus papeis de parede para o launcher, sem recompilar.
# Uso: scripts/push-branding.sh [pasta-com-fotos]
#  - branding/logo.png             -> aparece no canto superior esquerdo
#  - fotos .jpg/.png da pasta       -> substituem as 12 fotos de natureza (trocam a cada 10 min)
# Sem pasta, mantem os papeis de parede atuais.
set -euo pipefail
source "$(dirname "$0")/common.sh"
DIR=/sdcard/Android/data/$LAUNCHER_PKG/files
connect
adb shell mkdir -p "$DIR/wallpapers"
[ -f "$ROOT/branding/logo.png" ] && adb push "$ROOT/branding/logo.png" "$DIR/logo.png"
if [ -n "${1:-}" ]; then
  adb shell "rm -f $DIR/wallpapers/*"
  for f in "$1"/*.jpg "$1"/*.jpeg "$1"/*.png; do
    [ -f "$f" ] && adb push "$f" "$DIR/wallpapers/"
  done
fi
adb shell am force-stop $LAUNCHER_PKG
adb shell input keyevent KEYCODE_HOME
echo "Pronto. Para voltar as fotos de natureza: adb shell rm -r $DIR/wallpapers"
