"""Transforma a logo (PNG/WebP com fundo transparente) numa marca d'agua para fundo escuro.

Uso: python3 make_watermark.py <entrada> <saida.png>
- Avatar (lado esquerdo): mantem as cores e a borda do adesivo.
- Texto (lado direito): preto vira branco para aparecer sobre a foto; o amarelo continua amarelo.
"""
import sys

from PIL import Image

src, out = sys.argv[1], sys.argv[2]
im = Image.open(src).convert("RGBA")
W, H = im.size
px = im.load()
split = int(W * 0.47)  # avatar a esquerda; barra e texto a direita

for y in range(H):
    for x in range(split, W):
        r, g, b, a = px[x, y]
        if a == 0:
            continue
        mn, mx = min(r, g, b), max(r, g, b)
        sat = (mx - mn) / (mx or 1)
        if sat <= 0.45:
            px[x, y] = (255, 255, 255, a)

im = im.crop(im.getbbox())
h = 120
im = im.resize((round(im.width * h / im.height), h), Image.LANCZOS)
im.save(out)

prev = Image.new("RGBA", (im.width + 40, im.height + 40), (40, 48, 60, 255))
prev.alpha_composite(im, (20, 20))
prev.save(out.replace(".png", "_preview.png"))
print(im.size)
