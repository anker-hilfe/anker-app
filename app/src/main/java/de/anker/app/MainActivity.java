package de.anker.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.print.PrintManager;
import android.speech.tts.TextToSpeech;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String launch = "";
    private ValueCallback<Uri[]> fileCallback;
    private PermissionRequest pendingPermission;
    private String pendingSave;
    static final int REQ_FILE = 21, REQ_SAVE = 22, REQ_MIC = 23;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launch = launchFrom(getIntent());
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new Bridge(this), "AnkerNative");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("file:")) return false;
                openExternal(url);
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            // Mikrofon für "Stimme zum Festhalten"
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.getResources());
                    } else {
                        pendingPermission = request;
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
                    }
                });
            }

            // Datei auswählen für "Daten wiederherstellen"
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Sicherung auswählen"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");

        tts = new TextToSpeech(this, status -> ttsReady = (status == TextToSpeech.SUCCESS));
        askPermissions();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String w = launchFrom(intent);
        if (!w.isEmpty() && web != null) {
            web.evaluateJavascript("window.ankerLaunch && window.ankerLaunch('" + w + "')", null);
        }
    }

    static String launchFrom(Intent i) {
        if (i == null || i.getData() == null) return "";
        String host = i.getData().getHost();
        if ("hilfe".equals(host) || "check".equals(host) || "zeigen".equals(host)) return host;
        return "";
    }

    void askPermissions() {
        ArrayList<String> want = new ArrayList<>();
        want.add(Manifest.permission.SEND_SMS);
        want.add(Manifest.permission.CALL_PHONE);
        want.add(Manifest.permission.ACCESS_FINE_LOCATION);
        want.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) want.add(Manifest.permission.POST_NOTIFICATIONS);
        ArrayList<String> missing = new ArrayList<>();
        for (String p : want) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
        }
        if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MIC && pendingPermission != null) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermission.grant(pendingPermission.getResources());
            } else {
                pendingPermission.deny();
            }
            pendingPermission = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri uri = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
            fileCallback.onReceiveValue(uri != null ? new Uri[]{uri} : null);
            fileCallback = null;
        }
        if (requestCode == REQ_SAVE) {
            boolean ok = false;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingSave != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    out.write(pendingSave.getBytes(StandardCharsets.UTF_8));
                    ok = true;
                } catch (Exception ignored) { }
            }
            pendingSave = null;
            web.evaluateJavascript("window.ankerSaved && window.ankerSaved(" + ok + ")", null);
        }
    }

    void openExternal(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) { }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.ankerBack ? window.ankerBack() : 'false'", value -> {
            if (value == null || value.contains("false")) MainActivity.super.onBackPressed();
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }

    /** Wird von der Web-Oberfläche über window.AnkerNative aufgerufen. */
    public class Bridge {
        private final Context ctx;
        Bridge(Context c) { ctx = c; }

        @JavascriptInterface
        public boolean isAvailable() { return true; }

        @JavascriptInterface
        public String getLaunch() {
            String w = launch;
            launch = "";
            return w;
        }

        @JavascriptInterface
        public boolean sendSms(String number, String text) {
            return Native.sendSms(ctx, number, text);
        }

        @JavascriptInterface
        public boolean call(String number) {
            if (ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) return false;
            try {
                Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Native.clean(number)));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                runOnUiThread(() -> startActivity(i));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void openUrl(String url) { runOnUiThread(() -> openExternal(url)); }

        @JavascriptInterface
        public void requestPermissions() { runOnUiThread(MainActivity.this::askPermissions); }

        @JavascriptInterface
        public String getLocation() { return Native.lastLocation(ctx); }

        @JavascriptInterface
        public boolean vibratePattern(String csv) {
            try {
                String[] parts = csv.split(",");
                long[] timings = new long[parts.length + 1];
                timings[0] = 0;
                for (int i = 0; i < parts.length; i++) timings[i + 1] = Long.parseLong(parts[i].trim());
                Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                if (vib == null || !vib.hasVibrator()) return false;
                vib.vibrate(VibrationEffect.createWaveform(timings, -1));
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean speak(String text, String lang) {
            if (!ttsReady) return false;
            try {
                tts.setLanguage(Locale.forLanguageTag(lang));
                tts.setSpeechRate(0.92f);
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "anker");
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void stopSpeak() { if (tts != null) tts.stop(); }

        @JavascriptInterface
        public void keepAwake(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public boolean setTimer(String untilMillis, String numbersCsv, String message, boolean withLocation) {
            return Native.setTimer(ctx, Long.parseLong(untilMillis), numbersCsv, message, withLocation);
        }

        @JavascriptInterface
        public void cancelTimer() { Native.cancelTimer(ctx); }

        @JavascriptInterface
        public void setReminder(boolean on, int hour) { Native.setReminder(ctx, on, hour); }

        @JavascriptInterface
        public void printPage() {
            runOnUiThread(() -> {
                PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                pm.print("Anker-Bericht", web.createPrintDocumentAdapter("Anker-Bericht"), null);
            });
        }

        @JavascriptInterface
        public void share(String text) {
            runOnUiThread(() -> {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, "Anker-Bericht");
                i.putExtra(Intent.EXTRA_TEXT, text);
                startActivity(Intent.createChooser(i, "Bericht teilen"));
            });
        }

        /** Sicherung als Datei speichern (Nutzer wählt den Ort). */
        @JavascriptInterface
        public void saveFile(String name, String content) {
            runOnUiThread(() -> {
                pendingSave = content;
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, name);
                try {
                    startActivityForResult(i, REQ_SAVE);
                } catch (Exception e) {
                    pendingSave = null;
                    web.evaluateJavascript("window.ankerSaved && window.ankerSaved(false)", null);
                }
            });
        }

        @JavascriptInterface
        public void setDiscreet(boolean on) {
            PackageManager pm = ctx.getPackageManager();
            ComponentName normal = new ComponentName(ctx, "de.anker.app.Launcher");
            ComponentName discreet = new ComponentName(ctx, "de.anker.app.LauncherDiscreet");
            pm.setComponentEnabledSetting(on ? discreet : normal,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(on ? normal : discreet,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }
}
