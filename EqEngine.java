package com.marshall.eqdime;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.DynamicsProcessing;
import android.os.Build;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aplica un ecualizador de 10 bandas a las sesiones de audio que anuncian otras apps (Spotify, etc). */
public class EqEngine {
    private static final String TAG = "EqDime";
    private static final float[] FREQS = {31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f};
    private static EqEngine inst;

    private final SharedPreferences prefs;
    private final Map<Integer, DynamicsProcessing> fx = new HashMap<>();
    private final Map<Integer, String> apps = new HashMap<>();
    private final float[] gains = new float[10];
    private float pre = 0f;
    private boolean bypass = false;
    private boolean limiter = false;

    public static synchronized EqEngine get(Context c) {
        if (inst == null) inst = new EqEngine(c.getApplicationContext());
        return inst;
    }

    private EqEngine(Context c) {
        prefs = c.getSharedPreferences("eqdime", Context.MODE_PRIVATE);
        String[] p = prefs.getString("g", "").split(",");
        if (p.length == 10) {
            try {
                for (int i = 0; i < 10; i++) gains[i] = Float.parseFloat(p[i]);
            } catch (NumberFormatException e) {
                for (int i = 0; i < 10; i++) gains[i] = 0f;
            }
        }
        pre = prefs.getFloat("pre", 0f);
        bypass = prefs.getBoolean("bypass", false);
        limiter = prefs.getBoolean("lim", false);
    }

    public synchronized void open(int session, String pkg) {
        if (Build.VERSION.SDK_INT < 28 || session <= 0) return;
        close(session);
        try {
            DynamicsProcessing.Config cfg = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
                    true, 10, false, 0, false, 0, true).build();
            DynamicsProcessing dp = new DynamicsProcessing(0, session, cfg);
            fx.put(session, dp);
            apps.put(session, pkg == null ? "desconocida" : pkg);
            applyTo(dp);
            dp.setEnabled(true);
        } catch (Throwable t) {
            Log.w(TAG, "No se pudo abrir la sesion " + session, t);
        }
    }

    public synchronized void close(int session) {
        DynamicsProcessing dp = fx.remove(session);
        apps.remove(session);
        if (dp != null) {
            try { dp.release(); } catch (Throwable t) { Log.w(TAG, "release", t); }
        }
    }

    public synchronized void releaseAll() {
        for (DynamicsProcessing dp : fx.values()) {
            try { dp.release(); } catch (Throwable t) { Log.w(TAG, "release", t); }
        }
        fx.clear();
        apps.clear();
    }

    public synchronized void update(float[] g, float p, boolean b, boolean l) {
        for (int i = 0; i < 10 && i < g.length; i++) gains[i] = g[i];
        pre = p;
        bypass = b;
        limiter = l;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) sb.append(',');
            sb.append(gains[i]);
        }
        prefs.edit().putString("g", sb.toString()).putFloat("pre", pre)
                .putBoolean("bypass", bypass).putBoolean("lim", limiter).apply();
        for (DynamicsProcessing dp : fx.values()) applyTo(dp);
    }

    public synchronized List<String> apps() {
        return new ArrayList<>(apps.values());
    }

    private void applyTo(DynamicsProcessing dp) {
        try {
            for (int i = 0; i < 10; i++) {
                float hi = i < 9 ? (float) Math.sqrt(FREQS[i] * FREQS[i + 1]) : 20000f;
                float g = bypass ? 0f : gains[i];
                dp.setPreEqBandAllChannelsTo(i, new DynamicsProcessing.EqBand(true, hi, g));
            }
            dp.setInputGainAllChannelsTo(bypass ? 0f : pre);
            dp.setLimiterAllChannelsTo(new DynamicsProcessing.Limiter(
                    true, limiter && !bypass, 0, 1f, 60f, 10f, -2f, 0f));
        } catch (Throwable t) {
            Log.w(TAG, "applyTo", t);
        }
    }
}
