package com.mihir.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Botao Netflix do controle Xiaomi: o sistema (WindowManager) consome a tecla antes de qualquer app
 * e dispara um VIEW para nflx://www.netflix.com/. Sem o Netflix instalado, esta tela invisivel e quem
 * responde, e abre o UniTV no lugar.
 */
public class NetflixKeyActivity extends Activity {

    private static final String TARGET = "com.global.unitviptv";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(TARGET);
        if (i == null) i = getPackageManager().getLaunchIntentForPackage(TARGET);
        if (i != null) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(i);
            } catch (RuntimeException ignored) {
            }
        }
        finish();
    }
}
