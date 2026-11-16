package com.marshall.eqdime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/** Servicio en primer plano: escucha las sesiones de audio de otras apps y mantiene vivo el ecualizador. */
public class EqService extends Service {
    private BroadcastReceiver rx;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("eq", "EQ Dime", NotificationManager.IMPORTANCE_LOW));
        }
        Notification n = new NotificationCompat.Builder(this, "eq")
                .setContentTitle("EQ Dime activo")
                .setContentText("Ecualizando el audio de Spotify y otras apps")
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, n);
        }
        if (rx == null) {
            rx = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent i) {
                    int session = i.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1);
                    String pkg = i.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME);
                    if (AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION.equals(i.getAction())) {
                        EqEngine.get(ctx).open(session, pkg);
                    } else {
                        EqEngine.get(ctx).close(session);
                    }
                }
            };
            IntentFilter f = new IntentFilter();
            f.addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            f.addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
            ContextCompat.registerReceiver(this, rx, f, ContextCompat.RECEIVER_EXPORTED);
        }
        EqEngine.get(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (rx != null) {
            unregisterReceiver(rx);
            rx = null;
        }
        EqEngine.get(this).releaseAll();
        super.onDestroy();
    }
}
