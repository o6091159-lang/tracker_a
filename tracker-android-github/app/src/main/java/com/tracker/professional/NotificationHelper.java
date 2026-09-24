package com.tracker.professional;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class NotificationHelper {
    static final String CHANNEL_ID = "tracker_alerts";

    private NotificationHelper() {}

    static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    ctx.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }
    }

    static boolean canPost(Context ctx) {
        if (Build.VERSION.SDK_INT >= 33
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return false;
        if (Build.VERSION.SDK_INT >= 24) return nm.areNotificationsEnabled();
        return true;
    }

    /** Shows a system notification. Returns true if it was posted. */
    static boolean show(Context ctx, String title, String body, int id) {
        try {
            if (!canPost(ctx)) return false;
            ensureChannel(ctx);

            Intent open = new Intent(ctx, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(
                    ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) {
                builder = new Notification.Builder(ctx, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(ctx);
            }
            builder.setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(new Notification.BigTextStyle().bigText(body))
                    .setContentIntent(pi)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return false;
            nm.notify(id, builder.build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
