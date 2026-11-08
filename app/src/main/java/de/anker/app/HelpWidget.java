package de.anker.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/** Widget auf dem Startbildschirm: ein Tipp öffnet sofort den Hilfe-Dialog. */
public class HelpWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("anker://hilfe"), ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 31, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_help);
            rv.setOnClickPendingIntent(R.id.widget_root, pi);
            mgr.updateAppWidget(id, rv);
        }
    }
}
