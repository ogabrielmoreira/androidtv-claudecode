package com.mihir.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Botoes do controle Xiaomi:
 *  - Apps (⋮⋮⋮, scancode 580 / KEYCODE_ALL_APPS) -> menu "Mais apps" do launcher (tecla chega aqui).
 *  - Prime Video: o WindowManager da Xiaomi consome a tecla e abre um componente fixo do Prime, entao
 *    nao da para desviar a tecla. Em vez disso o servico observa so as janelas do Prime
 *    (packageNames no XML) e, quando o Prime abre, troca pelo YouTube e encerra o Prime em segundo plano.
 *  - Netflix e tratado pela NetflixKeyActivity.
 * Nao le o conteudo da tela.
 */
public class RemoteButtonsService extends AccessibilityService {

    private static final String TAG = "RemoteButtons";
    private static final String APPS_MENU = "apps_menu";
    static final String PRIME_PKG = "com.amazon.amazonvideo.livingroom";
    private static final String PRIME_TARGET = "com.google.android.youtube.tv";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastRedirect;

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        String target = targetFor(e);
        if (target == null) return false;
        // Consome o apertar e o soltar; age so ao soltar.
        if (e.getAction() == KeyEvent.ACTION_UP) {
            Log.i(TAG, "botao scan=" + e.getScanCode() + " key=" + e.getKeyCode() + " -> " + target);
            open(target);
        }
        return true;
    }

    private static String targetFor(KeyEvent e) {
        int scan = e.getScanCode();
        if (scan == 580 || e.getKeyCode() == KeyEvent.KEYCODE_ALL_APPS) return APPS_MENU;
        return null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (!PRIME_PKG.contentEquals(String.valueOf(event.getPackageName()))) return;
        // O Prime abre varias janelas em sequencia; um redirecionamento por abertura basta.
        long now = SystemClock.elapsedRealtime();
        if (now - lastRedirect < 3000) return;
        lastRedirect = now;
        Log.i(TAG, "Prime Video abriu -> " + PRIME_TARGET);
        open(PRIME_TARGET);
        handler.postDelayed(this::killPrime, 1500);
    }

    /** Libera a memoria do Prime depois que ele vai para segundo plano. */
    private void killPrime() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) am.killBackgroundProcesses(PRIME_PKG);
    }

    private void open(String target) {
        Intent i;
        if (APPS_MENU.equals(target)) {
            i = new Intent(this, MainActivity.class).putExtra(MainActivity.EXTRA_OPEN_APPS_MENU, true);
        } else {
            i = getPackageManager().getLeanbackLaunchIntentForPackage(target);
            if (i == null) i = getPackageManager().getLaunchIntentForPackage(target);
            if (i == null) return; // app nao instalado: nao faz nada
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (RuntimeException ex) {
            Log.w(TAG, "nao abriu " + target, ex);
        }
    }

    @Override
    public void onInterrupt() {
    }
}
