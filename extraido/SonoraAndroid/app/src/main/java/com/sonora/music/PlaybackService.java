package com.sonora.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class PlaybackService extends Service {
    public static final String ACTION_PLAY = "com.sonora.music.PLAY";
    private static final String CHANNEL = "sonora_playback";
    private static final int NOTIFICATION_ID = 41;

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Reproducción de SONORA", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Controles de reproducción musical");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String title = intent == null ? "Tu música" : intent.getStringExtra("title");
        if (title == null || title.trim().isEmpty()) title = "Tu música";
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setContentTitle("SONORA")
                .setContentText("Reproduciendo · " + title)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pending)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        startForeground(NOTIFICATION_ID, builder.build());
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
