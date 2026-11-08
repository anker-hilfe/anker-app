package de.anker.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Läuft der Melde-dich-Timer ab, bekommen die gewählten Vertrauenspersonen eine SMS. */
public class TimerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        SharedPreferences p = ctx.getSharedPreferences(Native.PREFS, Context.MODE_PRIVATE);
        if (p.getLong("timerUntil", 0) == 0) return;
        String numbers = p.getString("timerNumbers", "");
        String msg = p.getString("timerMessage", "");
        if (p.getBoolean("timerLocation", false)) {
            String loc = Native.lastLocation(ctx);
            if (!loc.isEmpty()) msg = msg + " Mein letzter Standort: https://maps.google.com/?q=" + loc;
        }
        for (String n : numbers.split(",")) {
            if (!n.trim().isEmpty()) Native.sendSms(ctx, n.trim(), msg);
        }
        p.edit().remove("timerUntil").putBoolean("timerFired", true).apply();
    }
}
