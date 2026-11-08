package de.anker.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/** Gemeinsame Funktionen für App, Timer und Erinnerung. */
final class Native {
    private Native() { }

    static final String PREFS = "anker";
    static final int REQ_TIMER = 11;
    static final int REQ_REMIND = 12;

    static String clean(String n) { return n.replaceAll("[^0-9+]", ""); }

    static boolean sendSms(Context ctx, String number, String text) {
        if (ctx.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return false;
        try {
            SmsManager sms;
            if (Build.VERSION.SDK_INT >= 31) sms = ctx.getSystemService(SmsManager.class);
            else sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(text);
            sms.sendMultipartTextMessage(clean(number), null, parts, null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    static String lastLocation(Context ctx) {
        boolean fine = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return "";
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location best = null;
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
            if (best == null) return "";
            return String.format(Locale.US, "%.5f,%.5f", best.getLatitude(), best.getLongitude());
        } catch (Exception e) {
            return "";
        }
    }

    private static PendingIntent timerIntent(Context ctx) {
        Intent i = new Intent(ctx, TimerReceiver.class);
        return PendingIntent.getBroadcast(ctx, REQ_TIMER, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static boolean setTimer(Context ctx, long until, String numbersCsv, String message, boolean withLocation) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("timerUntil", until)
                .putString("timerNumbers", numbersCsv)
                .putString("timerMessage", message)
                .putBoolean("timerLocation", withLocation)
                .apply();
        return scheduleTimer(ctx, until);
    }

    static boolean scheduleTimer(Context ctx, long until) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            Intent show = new Intent(ctx, MainActivity.class);
            PendingIntent showPi = PendingIntent.getActivity(ctx, 13, show, PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(until, showPi), timerIntent(ctx));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void cancelTimer(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("timerUntil").apply();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(timerIntent(ctx));
    }

    private static PendingIntent remindIntent(Context ctx) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        return PendingIntent.getBroadcast(ctx, REQ_REMIND, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void setReminder(Context ctx, boolean on, int hour) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean("remind", on).putInt("remindHour", hour).apply();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(remindIntent(ctx));
        if (!on) return;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), AlarmManager.INTERVAL_DAY, remindIntent(ctx));
    }

    /** Nach einem Neustart Timer und Erinnerung wiederherstellen. */
    static void restore(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long until = p.getLong("timerUntil", 0);
        if (until > 0) {
            if (until <= System.currentTimeMillis()) until = System.currentTimeMillis() + 60_000;
            scheduleTimer(ctx, until);
        }
        if (p.getBoolean("remind", false)) setReminder(ctx, true, p.getInt("remindHour", 20));
    }
}
