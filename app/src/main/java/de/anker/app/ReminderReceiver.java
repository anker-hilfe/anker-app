package de.anker.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

/** Tägliche Erinnerung an den Kurz-Check. */
public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL = "check";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33
                && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Täglicher Check", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("anker://check"), ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 21, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Wie geht's dir heute?")
                .setContentText("Ein Tipp genügt.")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify(1, n);
    }
}
