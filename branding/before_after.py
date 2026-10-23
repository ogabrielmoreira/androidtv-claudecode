"""Monta a imagem "antes x depois" para o LinkedIn (retrato 4:5, 1080x1350).

Uso: python3 before_after.py <antes.png> <depois.png> <saida.png>
"""
import sys

from PIL import Image, ImageDraw, ImageFont

before_path, after_path, out_path = sys.argv[1:4]
W, H = 1080, 1350
BG = (11, 15, 23)
MARGIN = 60
IMG_W = W - 2 * MARGIN
IMG_H = round(IMG_W * 9 / 16)
RADIUS = 22

FONT = "/System/Library/Fonts/Helvetica.ttc"
label_font = ImageFont.truetype(FONT, 30, index=1)  # bold
foot_font = ImageFont.truetype(FONT, 22, index=0)


def rounded(img):
    img = img.convert("RGB").resize((IMG_W, IMG_H), Image.LANCZOS)
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, IMG_W, IMG_H), RADIUS, fill=255)
    out = Image.new("RGBA", img.size)
    out.paste(img, (0, 0), mask)
    return out


def pill(draw, x, y, text, fill, color):
    pad_x, pad_y = 18, 9
    tw = draw.textlength(text, font=label_font)
    box = (x, y, x + tw + 2 * pad_x, y + 30 + 2 * pad_y)
    draw.rounded_rectangle(box, radius=(box[3] - box[1]) // 2, fill=fill)
    draw.text((x + pad_x, y + pad_y - 2), text, font=label_font, fill=color)
    return box[3]


canvas = Image.new("RGBA", (W, H), BG)
d = ImageDraw.Draw(canvas)

y = 50
y = pill(d, MARGIN, y, "ANTES", (120, 28, 36), (255, 214, 214)) + 16
canvas.alpha_composite(rounded(Image.open(before_path)), (MARGIN, y))
y += IMG_H + 40
y = pill(d, MARGIN, y, "DEPOIS", (22, 92, 58), (210, 255, 228)) + 16
canvas.alpha_composite(rounded(Image.open(after_path)), (MARGIN, y))
y += IMG_H

foot = "Xiaomi Mi TV Stick  ·  sem root  ·  sem formatar"
d.text(((W - d.textlength(foot, font=foot_font)) / 2, y + (H - y) / 2 - 14), foot,
       font=foot_font, fill=(150, 160, 178))

canvas.convert("RGB").save(out_path, quality=95)
print(out_path, canvas.size)
