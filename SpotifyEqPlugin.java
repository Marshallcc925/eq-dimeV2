package com.marshall.eqdime;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SpotifyEq")
public class SpotifyEqPlugin extends Plugin {

    @PluginMethod
    public void start(PluginCall call) {
        Context c = getContext();
        ContextCompat.startForegroundService(c, new Intent(c, EqService.class));
        JSObject r = new JSObject();
        r.put("supported", Build.VERSION.SDK_INT >= 28);
        call.resolve(r);
    }

    @PluginMethod
    public void update(PluginCall call) {
        JSArray arr = call.getArray("gains");
        float[] g = new float[10];
        try {
            for (int i = 0; i < 10; i++) g[i] = (float) arr.getDouble(i);
        } catch (Exception e) {
            call.reject("gains invalidos");
            return;
        }
        Float pre = call.getFloat("pre", 0f);
        Boolean bypass = call.getBoolean("bypass", false);
        Boolean limiter = call.getBoolean("limiter", false);
        EqEngine.get(getContext()).update(g, pre, bypass, limiter);
        call.resolve();
    }

    @PluginMethod
    public void status(PluginCall call) {
        JSObject r = new JSObject();
        r.put("apps", new JSArray(EqEngine.get(getContext()).apps()));
        call.resolve(r);
    }

    @PluginMethod
    public void getVolume(PluginCall call) {
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int max = Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        JSObject r = new JSObject();
        r.put("value", am.getStreamVolume(AudioManager.STREAM_MUSIC) / (double) max);
        call.resolve(r);
    }

    @PluginMethod
    public void setVolume(PluginCall call) {
        AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Float v = call.getFloat("value", 0.5f);
        am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(v * max), 0);
        call.resolve();
    }
}
