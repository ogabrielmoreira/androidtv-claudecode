package com.gabriel.cleanhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;

/**
 * Elementos visuais desenhados em codigo: fundo "mesh" desfocado, vidro, icones e logo.
 * Tudo em classes nomeadas (o d8 antigo do build-tools nao compila classes anonimas).
 */
final class Art {
    private Art() {
    }

    /** Base comum: nada de filtro de cor/alpha, so o necessario para ser um Drawable. */
    private abstract static class Base extends Drawable {
        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** Fundo estilo iOS: manchas de cor suaves num bitmap pequeno, ampliado com filtro (da o desfoque). */
    static class Background extends Base {
        private final Bitmap bmp;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        Background() {
            int w = 320, h = 180;
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawColor(0xFF060913);
            blob(c, w, h, 0.10f, 0.02f, 0.65f, 0x3B4FE0, 0.80f); // azul indigo, canto superior esquerdo
            blob(c, w, h, 0.92f, 0.12f, 0.50f, 0x8A3FD1, 0.60f); // violeta, canto superior direito
            blob(c, w, h, 0.78f, 1.02f, 0.62f, 0x1F8FA8, 0.50f); // verde-agua, canto inferior direito
            blob(c, w, h, 0.05f, 0.98f, 0.45f, 0xB03A8C, 0.34f); // magenta discreto, canto inferior esquerdo
            // escurece a parte de baixo para dar profundidade e manter o contraste dos textos
            Paint p = new Paint();
            p.setShader(new LinearGradient(0, h * 0.45f, 0, h, 0x00000000, 0x88000000, Shader.TileMode.CLAMP));
            c.drawRect(0, h * 0.45f, w, h, p);
        }

        private static void blob(Canvas c, int w, int h, float cx, float cy, float r, int rgb, float alpha) {
            float radius = r * w;
            int a = (int) (alpha * 255);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setShader(new RadialGradient(cx * w, cy * h, radius, (a << 24) | rgb, rgb,
                    Shader.TileMode.CLAMP));
            c.drawCircle(cx * w, cy * h, radius, p);
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(bmp, null, getBounds(), paint);
        }
    }

    /**
     * Papel de parede a partir de uma foto: preenche a tela (center-crop) e escurece em cima e
     * embaixo para manter relogio, assinatura e rotulos legiveis.
     */
    static class Photo extends Base {
        private final Bitmap bmp;
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        private final Paint scrim = new Paint();
        private final Rect src = new Rect();

        Photo(Bitmap bmp) {
            this.bmp = bmp;
        }

        @Override
        protected void onBoundsChange(Rect b) {
            float scale = Math.max(b.width() / (float) bmp.getWidth(), b.height() / (float) bmp.getHeight());
            int w = Math.round(b.width() / scale), h = Math.round(b.height() / scale);
            int x = (bmp.getWidth() - w) / 2, y = (bmp.getHeight() - h) / 2;
            src.set(x, y, x + w, y + h);
            scrim.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{0x8C000000, 0x33000000, 0x40000000, 0xA6000000},
                    new float[]{0f, 0.35f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(bmp, src, getBounds(), paint);
            canvas.drawRect(getBounds(), scrim);
        }
    }

    /** Painel de "vidro": degrade branco translucido com borda fina clara. */
    static class Glass extends Base {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float radius;
        private final int top;
        private final int bottom;

        /** Brilho na borda de cima, como a luz batendo no vidro. */
        private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean hasEdge;

        Glass(float radius, float strokePx, int top, int bottom, int strokeColor) {
            this.radius = radius;
            this.top = top;
            this.bottom = bottom;
            this.hasEdge = (strokeColor >>> 24) != 0;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(strokePx);
            stroke.setColor(strokeColor);
            sheen.setStyle(Paint.Style.STROKE);
            sheen.setStrokeWidth(strokePx * 1.5f);
        }

        @Override
        protected void onBoundsChange(Rect b) {
            fill.setShader(new LinearGradient(0, b.top, 0, b.bottom, top, bottom, Shader.TileMode.CLAMP));
            sheen.setShader(new LinearGradient(0, b.top, 0, b.top + b.height() * 0.45f,
                    0x80FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            float half = stroke.getStrokeWidth() / 2f;
            rect.inset(half, half);
            canvas.drawRoundRect(rect, radius, radius, fill);
            if (hasEdge) {
                canvas.drawRoundRect(rect, radius, radius, stroke);
                canvas.drawRoundRect(rect, radius, radius, sheen);
            }
        }

        @Override
        public void getOutline(Outline outline) {
            outline.setRoundRect(getBounds(), radius);
        }
    }

    private abstract static class Glyph extends Base {
        protected final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

        Glyph() {
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStrokeJoin(Paint.Join.ROUND);
            line.setColor(0xEBFFFFFF);
        }

        protected float size() {
            Rect b = getBounds();
            return Math.min(b.width(), b.height());
        }
    }

    /** Engrenagem: corpo em anel, 8 dentes curtos e grossos e furo central. */
    static class Gear extends Glyph {
        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float s = size();
            float cx = b.exactCenterX(), cy = b.exactCenterY();
            line.setStrokeWidth(s * 0.10f);
            canvas.drawCircle(cx, cy, s * 0.24f, line);
            line.setStrokeWidth(s * 0.13f);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i;
                float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
                canvas.drawLine(cx + cos * s * 0.34f, cy + sin * s * 0.34f,
                        cx + cos * s * 0.38f, cy + sin * s * 0.38f, line);
            }
        }
    }

    /** Casinha de contorno, para o botao "interface anterior". */
    static class House extends Glyph {
        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float s = size();
            float ox = b.exactCenterX() - s / 2f, oy = b.exactCenterY() - s / 2f;
            line.setStrokeWidth(s * 0.055f);
            Path p = new Path();
            p.moveTo(ox + s * 0.20f, oy + s * 0.50f);
            p.lineTo(ox + s * 0.50f, oy + s * 0.24f);
            p.lineTo(ox + s * 0.80f, oy + s * 0.50f);
            p.moveTo(ox + s * 0.28f, oy + s * 0.45f);
            p.lineTo(ox + s * 0.28f, oy + s * 0.76f);
            p.lineTo(ox + s * 0.72f, oy + s * 0.76f);
            p.lineTo(ox + s * 0.72f, oy + s * 0.45f);
            p.moveTo(ox + s * 0.44f, oy + s * 0.76f);
            p.lineTo(ox + s * 0.44f, oy + s * 0.60f);
            p.lineTo(ox + s * 0.56f, oy + s * 0.60f);
            p.lineTo(ox + s * 0.56f, oy + s * 0.76f);
            canvas.drawPath(p, line);
        }
    }

    /** Monograma "GT" num quadrado arredondado com degrade. Troque por um PNG proprio quando houver. */
    static class Logo extends Base {
        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();

        Logo() {
            text.setColor(0xFFFFFFFF);
            text.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            text.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        protected void onBoundsChange(Rect b) {
            bg.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, 0xFF6F7DFF, 0xFFB56CFF,
                    Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            rect.set(b);
            float r = b.height() * 0.30f;
            canvas.drawRoundRect(rect, r, r, bg);
            text.setTextSize(b.height() * 0.46f);
            float y = b.exactCenterY() - (text.ascent() + text.descent()) / 2f;
            canvas.drawText("GT", b.exactCenterX(), y, text);
        }

        @Override
        public int getIntrinsicWidth() {
            return -1;
        }
    }
}
