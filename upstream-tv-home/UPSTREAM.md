# Projeto original: tv-home

Esta pasta guarda o projeto [tv-home](https://github.com/mihirgupta0900/tv-home), de Mihir Gupta (licença MIT),
como veio no `tv-home-main.zip`, menos a pasta `launcher/`, que foi adaptada e hoje vive em
[`../launcher/`](../launcher/).

O que tem aqui e não é usado pelo fluxo principal deste repositório:

- `tools/tv.py` e scripts: cliente ADB em Python (`adb-shell`), alternativa quando o macOS bloqueia o `adb`.
- `examples/tcl-google-tv-g07/`: lista de debloat de uma TV TCL com Google TV.
- `PROMPT.md`: o prompt original para fazer tudo com o Claude Code.

O fluxo deste repositório usa o `adb` oficial (`scripts/setup-sdk.sh`) e os scripts de [`../scripts/`](../scripts/).
