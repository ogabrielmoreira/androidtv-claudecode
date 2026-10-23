# Clean Home (primeira versão)

O primeiro launcher do projeto, escrito do zero antes de adotar o [tv-home](../../upstream-tv-home/):
fundo "mesh" desfocado desenhado em código, painel de vidro com cantos concêntricos, saudação, relógio,
assinatura "Gabriel Tech Design" com monograma e foco no estilo tvOS. Também aceita `wallpaper.jpg` e
`logo.png` em `/sdcard/Android/data/com.gabriel.cleanhome/files/`.

Ficou como referência; o launcher em uso é o de [`launcher/`](../../launcher/).

```bash
extras/clean-home-launcher/build.sh
```

Para instalar no lugar do launcher principal, instale o APK e defina `com.gabriel.cleanhome/.MainActivity`
como Home (veja `scripts/install-launcher.sh`, que desativa este por padrão).
