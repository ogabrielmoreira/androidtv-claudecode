package com.gabriel.cleanhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.os.Build;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Launcher minimalista para TV: fundo "mesh" desfocado, painel de vidro com os apps,
 * foco que cresce (estilo tvOS) e uma assinatura discreta.
 *
 * Tokens de design: espacamento base 4dp; raios 16 (cards) e 30 (painel); texto primario 95% branco,
 * secundario 70%, terciario 55% (todos com contraste >= 4.5:1 sobre o fundo escuro).
 */
public class MainActivity extends Activity
        implements View.OnFocusChangeListener, View.OnClickListener {

    /** Apps que aparecem primeiro, nesta ordem. Os demais apps do usuario vem depois, em ordem alfabetica. */
    private static final String[] PRIORITY = {
            "com.global.unitviptv",
            "tv.pluto.android",
            "com.esaba.downloader",
    };
    private static final String SETTINGS_PKG = "com.android.tv.settings";
    private static final String GOOGLE_HOME_PKG = "com.google.android.tvlauncher";
    private static final String GOOGLE_HOME_ACTIVITY = "com.google.android.tvlauncher.MainActivity";

    private static final int COLS = 5;
    private static final int KIND_APP = 0;
    private static final int KIND_SETTINGS = 1;
    private static final int KIND_PREVIOUS = 2;

    private static final int TEXT_PRIMARY = 0xF2FFFFFF;
    private static final int TEXT_SECONDARY = 0xB3FFFFFF;
    private static final int TEXT_TERTIARY = 0x8CFFFFFF;

    /**
     * Personalizacao sem recompilar: arquivos enviados por adb para
     * /sdcard/Android/data/com.gabriel.cleanhome/files/ (wallpaper.jpg/.png e logo.png).
     */
    private static final String[] WALLPAPER_FILES = {"wallpaper.jpg", "wallpaper.jpeg", "wallpaper.png"};
    private static final String LOGO_FILE = "logo.png";

    private PackageManager pm;
    private float d;
    private FrameLayout root;
    private ImageView logo;
    private long wallpaperStamp = -1;
    private long logoStamp = -1;
    private ScrollView scroll;
    private LinearLayout grid;
    private LinearLayout panel;
    private TextView greeting;

    private static class Item {
        int kind;
        String pkg;
        CharSequence label;
        Drawable banner;
        Drawable icon;
        Intent intent;
    }

    /** Dados de cada tile, guardados na tag da view. */
    private static class Tile {
        Item item;
        TextView label;
        GradientDrawable ringBg;
        float elevation;
    }

    private static class FocusFirst implements Runnable {
        private final ViewGroup root;

        FocusFirst(ViewGroup root) {
            this.root = root;
        }

        @Override
        public void run() {
            View first = findTile(root);
            if (first != null) first.requestFocus();
        }

        private static View findTile(View v) {
            if (v.getTag() instanceof Tile) return v;
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) {
                    View r = findTile(g.getChildAt(i));
                    if (r != null) return r;
                }
            }
            return null;
        }
    }

    private static class RoundOutline extends ViewOutlineProvider {
        private final float radius;

        RoundOutline(float radius) {
            this.radius = radius;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        d = getResources().getDisplayMetrics().density;

        root = new FrameLayout(this);
        root.setBackground(new Art.Background());
        root.setClipChildren(false);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, dp(96), 0, dp(40));
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        grid.setClipChildren(false);
        scroll.addView(grid, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildGreeting(), greetingParams());
        root.addView(buildClock(), clockParams());
        setContentView(root);
    }

    // ---------------------------------------------------------------- cabecalho

    private View buildGreeting() {
        greeting = new TextView(this);
        greeting.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        greeting.setTextColor(TEXT_SECONDARY);
        greeting.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return greeting;
    }

    private FrameLayout.LayoutParams greetingParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.topMargin = dp(40);
        lp.leftMargin = dp(56);
        return lp;
    }

    private String greetingText() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String hello = h >= 5 && h < 12 ? "Bom dia" : (h >= 12 && h < 18 ? "Boa tarde" : "Boa noite");
        return hello + ", Gabriel";
    }

    private View buildClock() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.END);

        TextClock time = new TextClock(this);
        time.setFormat24Hour("HH:mm");
        time.setFormat12Hour("HH:mm");
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 34);
        time.setTextColor(TEXT_PRIMARY);
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setGravity(Gravity.END);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d 'de' MMMM");
        date.setFormat12Hour("EEEE, d 'de' MMMM");
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        date.setTextColor(TEXT_SECONDARY);
        date.setGravity(Gravity.END);

        box.addView(time);
        box.addView(date);
        box.addView(buildSignature());
        return box;
    }

    /** Logo + "Gabriel Tech Design", pequeno e discreto, logo abaixo da data. */
    private View buildSignature() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);

        TextView name = new TextView(this);
        name.setText("GABRIEL TECH DESIGN");
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        name.setLetterSpacing(0.14f);
        name.setTextColor(TEXT_TERTIARY);
        name.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        row.addView(name);

        logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImageDrawable(new Art.Logo());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(16), dp(16));
        lp.leftMargin = dp(8);
        row.addView(logo, lp);

        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = dp(10);
        rp.gravity = Gravity.END;
        row.setLayoutParams(rp);
        return row;
    }

    private FrameLayout.LayoutParams clockParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.topMargin = dp(32);
        lp.rightMargin = dp(56);
        return lp;
    }

    // ---------------------------------------------------------------- ciclo de vida

    @Override
    protected void onResume() {
        super.onResume();
        greeting.setText(greetingText());
        applyCustomFiles();
        populate();
    }

    /** Aplica papel de parede e logo enviados pelo usuario; volta ao padrao se o arquivo sumir. */
    private void applyCustomFiles() {
        File dir = getExternalFilesDir(null);
        if (dir == null) return;

        File wp = null;
        for (String name : WALLPAPER_FILES) {
            File f = new File(dir, name);
            if (f.exists()) {
                wp = f;
                break;
            }
        }
        long stamp = wp == null ? 0 : wp.lastModified() + wp.length();
        if (stamp != wallpaperStamp) {
            wallpaperStamp = stamp;
            Bitmap bmp = wp == null ? null : decodeScaled(wp, 1280);
            root.setBackground(bmp == null ? new Art.Background() : new Art.Photo(bmp));
        }

        File lf = new File(dir, LOGO_FILE);
        long lstamp = lf.exists() ? lf.lastModified() + lf.length() : 0;
        if (lstamp != logoStamp) {
            logoStamp = lstamp;
            Bitmap bmp = lstamp == 0 ? null : decodeScaled(lf, 128);
            if (bmp == null) logo.setImageDrawable(new Art.Logo());
            else logo.setImageBitmap(bmp);
        }
    }

    /** Decodifica reduzindo o tamanho (o stick tem 1 GB de RAM). */
    private static Bitmap decodeScaled(File f, int targetWidth) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        if (o.outWidth <= 0) return null;
        int sample = 1;
        while (o.outWidth / (sample * 2) >= targetWidth) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        scroll.scrollTo(0, 0);
        focusFirst();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Na tela inicial o botao voltar nao faz nada.
        if (keyCode == KeyEvent.KEYCODE_BACK) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // ignorado de proposito
    }

    private int dp(float v) {
        return Math.round(v * d);
    }

    private void focusFirst() {
        grid.post(new FocusFirst(grid));
    }

    // ---------------------------------------------------------------- dados

    private List<Item> loadItems() {
        Set<String> pkgs = new LinkedHashSet<String>();
        String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (p.equals(getPackageName())) continue;
                ApplicationInfo ai = ri.activityInfo.applicationInfo;
                boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (!system) pkgs.add(p);
            }
        }
        for (String p : PRIORITY) pkgs.add(p);

        List<Item> apps = new ArrayList<Item>();
        for (String p : pkgs) {
            Intent launch = pm.getLeanbackLaunchIntentForPackage(p);
            if (launch == null) launch = pm.getLaunchIntentForPackage(p);
            if (launch == null) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(p, 0);
                Item it = new Item();
                it.kind = KIND_APP;
                it.pkg = p;
                it.label = ai.loadLabel(pm);
                it.banner = ai.loadBanner(pm);
                it.icon = ai.loadIcon(pm);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                it.intent = launch;
                apps.add(it);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        sortItems(apps);

        List<Item> items = new ArrayList<Item>(apps);

        Item settings = new Item();
        settings.kind = KIND_SETTINGS;
        settings.pkg = SETTINGS_PKG;
        settings.label = "Configurações";
        settings.icon = new Art.Gear();
        settings.intent = new Intent(android.provider.Settings.ACTION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        items.add(settings);

        // So mostra o atalho quando o launcher do Google esta ativo: um botao que nao faz nada confunde.
        if (googleHomeEnabled()) {
            Item previous = new Item();
            previous.kind = KIND_PREVIOUS;
            previous.pkg = GOOGLE_HOME_PKG;
            previous.label = "Interface anterior";
            previous.icon = new Art.House();
            items.add(previous);
        }
        return items;
    }

    private boolean googleHomeEnabled() {
        try {
            if (!pm.getApplicationInfo(GOOGLE_HOME_PKG, 0).enabled) return false;
            int state = pm.getApplicationEnabledSetting(GOOGLE_HOME_PKG);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                    || state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } catch (Exception e) {
            return false;
        }
    }

    /** Ordena por prioridade e depois por nome (insercao: a lista e pequena). */
    private static void sortItems(List<Item> items) {
        Collator collator = Collator.getInstance();
        for (int i = 1; i < items.size(); i++) {
            Item cur = items.get(i);
            int j = i - 1;
            while (j >= 0 && isAfter(items.get(j), cur, collator)) {
                items.set(j + 1, items.get(j));
                j--;
            }
            items.set(j + 1, cur);
        }
    }

    private static boolean isAfter(Item a, Item b, Collator collator) {
        int pa = prio(a.pkg), pb = prio(b.pkg);
        if (pa != pb) return pa > pb;
        return collator.compare(a.label.toString(), b.label.toString()) > 0;
    }

    private static int prio(String pkg) {
        for (int i = 0; i < PRIORITY.length; i++) if (PRIORITY[i].equals(pkg)) return i;
        return PRIORITY.length;
    }

    // ---------------------------------------------------------------- interface

    private void populate() {
        grid.removeAllViews();
        List<Item> items = loadItems();

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int ring = dp(4);
        int pad = dp(28);
        int gap = dp(20);
        int available = dm.widthPixels - 2 * dp(48) - 2 * pad;
        int tileW = (available - (COLS - 1) * gap - COLS * 2 * ring) / COLS;
        int tileH = tileW * 9 / 16;

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClipChildren(false);
        panel.setClipToPadding(false);
        // Cantos concentricos: raio do painel = raio do card (16) + respiro interno (28).
        panel.setBackground(new Art.Glass(dp(44), dp(1), 0x26FFFFFF, 0x0DFFFFFF, 0x38FFFFFF));
        // Embaixo sobra menos porque o rotulo do item focado ja ocupa espaco ali.
        panel.setPadding(pad, dp(36), pad, dp(12));

        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                row.setClipChildren(false);
                panel.addView(row, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            View cell = buildCell(items.get(i), tileW, tileH, ring);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cp.leftMargin = gap / 2;
            cp.rightMargin = gap / 2;
            row.addView(cell, cp);

            // Entrada suave, um tile depois do outro.
            cell.setAlpha(0f);
            cell.setTranslationY(dp(14));
            cell.animate().alpha(1f).translationY(0f).setStartDelay(i * 50L).setDuration(320)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
        grid.addView(panel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        focusFirst();
    }

    private View buildCell(Item item, int tileW, int tileH, int ring) {
        float radius = dp(16);

        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setClipChildren(false);

        FrameLayout ringView = new FrameLayout(this);
        ringView.setPadding(ring, ring, ring, ring);
        ringView.setFocusable(true);
        ringView.setClickable(true);
        ringView.setClipChildren(false);
        ringView.setContentDescription(item.label);
        ringView.setAlpha(0.92f);
        if (Build.VERSION.SDK_INT >= 28) {
            // Sombra clara vira um brilho suave em volta do item focado (sombra escura some no fundo escuro).
            ringView.setOutlineSpotShadowColor(0xFFB9C6FF);
            ringView.setOutlineAmbientShadowColor(0xFFB9C6FF);
        }

        FrameLayout card = new FrameLayout(this);
        card.setOutlineProvider(new RoundOutline(radius));
        card.setClipToOutline(true);

        ImageView img = new ImageView(this);
        if (item.banner != null) {
            card.setBackground(new Art.Glass(radius, dp(1), 0x1FFFFFFF, 0x1FFFFFFF, 0x00000000));
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setImageDrawable(item.banner);
            card.addView(img, new FrameLayout.LayoutParams(tileW, tileH));
        } else {
            card.setBackground(new Art.Glass(radius, dp(1), 0x33FFFFFF, 0x14FFFFFF, 0x4DFFFFFF));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            img.setImageDrawable(item.icon);
            int g = tileH * 55 / 100;
            FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(g, g);
            ip.gravity = Gravity.CENTER;
            card.addView(img, ip);
        }
        ringView.addView(card, new FrameLayout.LayoutParams(tileW, tileH));

        TextView label = new TextView(this);
        label.setText(item.label);
        label.setTextColor(TEXT_PRIMARY);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        label.setSingleLine(true);
        label.setGravity(Gravity.CENTER);
        label.setAlpha(0f);

        GradientDrawable ringBg = new GradientDrawable();
        ringBg.setColor(0x14FFFFFF);
        ringBg.setStroke(dp(3), 0xFFFFFFFF);
        ringBg.setCornerRadius(radius + ring);

        Tile tile = new Tile();
        tile.item = item;
        tile.label = label;
        tile.ringBg = ringBg;
        tile.elevation = dp(16);
        ringView.setTag(tile);
        ringView.setOnFocusChangeListener(this);
        ringView.setOnClickListener(this);

        cell.addView(ringView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                tileW, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        cell.addView(label, lp);
        return cell;
    }

    // ---------------------------------------------------------------- acoes

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        Tile t = (Tile) v.getTag();
        float s = hasFocus ? 1.10f : 1f;
        v.animate().scaleX(s).scaleY(s).setDuration(220)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();
        v.setBackground(hasFocus ? t.ringBg : null);
        v.setElevation(hasFocus ? t.elevation : 0);
        v.setAlpha(hasFocus ? 1f : 0.92f);
        t.label.animate().alpha(hasFocus ? 1f : 0f).setDuration(160).start();
    }

    @Override
    public void onClick(View v) {
        Tile t = (Tile) v.getTag();
        if (t.item.kind == KIND_PREVIOUS) {
            openPreviousInterface();
            return;
        }
        try {
            startActivity(t.item.intent);
        } catch (Exception ignored) {
        }
    }

    /** Abre o launcher do Google, se ele estiver ativo no aparelho. */
    private void openPreviousInterface() {
        Intent i = new Intent();
        i.setComponent(new ComponentName(GOOGLE_HOME_PKG, GOOGLE_HOME_ACTIVITY));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "A interface anterior está desativada. Peça ao Claude para reativar.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
