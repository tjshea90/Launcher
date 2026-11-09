package com.tj.launcher;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

/**
 * S2 (Tj, 2026-11-04: "an app ... that I can plug in the tablet ... to read the tablet settings and everything needed to help this project"): the facts the app can see by itself
 * (no adb), plus, when the shell helper runs, the whole read-only probe list. Written to the app's share folder, where {@code adb pull} (or any file manager) can take it.
 * Reads names and public facts only: no accounts, serial number, Wi-Fi names, lock settings or setting values (except the few listed in probe-commands.txt).
 */
public final class TabletReport {
    private TabletReport() {}

    public static File shareDir(Context c) {
        File base = c.getExternalFilesDir(null);
        if (base == null) base = c.getFilesDir();
        File d = new File(base, "probe");
        //noinspection ResultOfMethodCallIgnored
        d.mkdirs();
        return d;
    }

    private static String prop(String k) {
        try { return String.valueOf(Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class).invoke(null, k, "")); }
        catch (Throwable t) { return "(unreadable)"; }
    }

    private static void line(StringBuilder b, String k, Object v) { b.append(k).append(": ").append(v).append('\n'); }

    private static void names(StringBuilder b, Context c, String which, android.net.Uri uri) {
        b.append("\n===== setting NAMES: ").append(which).append('\n');
        try (Cursor cur = c.getContentResolver().query(uri, new String[] { "name" }, null, null, "name")) {
            int n = 0;
            while (cur != null && cur.moveToNext()) { b.append(cur.getString(0)).append('\n'); n++; }
            b.append("(").append(n).append(" names)\n");
        } catch (Throwable t) { b.append("(not readable: ").append(t.getClass().getSimpleName()).append(")\n"); }
    }

    private static void pkg(StringBuilder b, PackageManager pm, String p) {
        try {
            PackageInfo pi = pm.getPackageInfo(p, 0);
            b.append(p).append("  v").append(pi.versionName).append(" (").append(pi.versionCode).append(")  apk ").append(pi.applicationInfo.sourceDir)
                    .append("  system=").append((pi.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0)
                    .append("  uid=").append(pi.applicationInfo.uid).append('\n');
        } catch (PackageManager.NameNotFoundException e) { b.append(p).append("  (not installed)\n"); }
    }

    /** The device facts at the top of the report: model, Android, build, the few properties that matter, memory, screen. */
    private static void deviceFacts(StringBuilder b, Context c) {
        line(b, "model", Build.MANUFACTURER + " " + Build.MODEL + " / " + Build.DEVICE);
        line(b, "android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), patch " + Build.VERSION.SECURITY_PATCH);
        line(b, "fingerprint", Build.FINGERPRINT);
        line(b, "display id", Build.DISPLAY);
        for (String k : new String[] { "ro.lineage.version", "ro.modversion", "ro.crypto.state", "ro.crypto.type", "ro.boot.flash.locked", "ro.boot.verifiedbootstate", "ro.config.low_ram", "fw.max_users", "fw.show_multiuserui", "persist.adb.tcp.port", "service.adb.tcp.port", "ro.sf.lcd_density" })
            line(b, k, prop(k));
        try { line(b, "UserManager.getMaxSupportedUsers (hidden API)", UserManager.class.getMethod("getMaxSupportedUsers").invoke(null)); } catch (Throwable t) { line(b, "getMaxSupportedUsers", t); }
        try { line(b, "UserManager.supportsMultipleUsers", UserManager.supportsMultipleUsers()); } catch (Throwable t) { line(b, "supportsMultipleUsers", t); }
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        line(b, "low-RAM (Go) device", am != null && am.isLowRamDevice());
        line(b, "memory class MB", am == null ? "?" : am.getMemoryClass());
        android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
        line(b, "screen", dm.widthPixels + "x" + dm.heightPixels + " @" + dm.densityDpi + " dpi, font scale " + c.getResources().getConfiguration().fontScale);
    }

    /** What the app can read by itself. */
    public static String appSide(Context c) {
        StringBuilder b = new StringBuilder();
        b.append("===== TABLET REPORT (app side) ").append(new java.util.Date()).append('\n');
        deviceFacts(b, c);
        PackageManager pm = c.getPackageManager();
        b.append("\n===== system features that matter\n");
        for (android.content.pm.FeatureInfo f : pm.getSystemAvailableFeatures()) {
            if (f.name != null && (f.name.contains("multiuser") || f.name.contains("managed") || f.name.contains("device_admin") || f.name.contains("freeform") || f.name.contains("picture") || f.name.contains("fingerprint") || f.name.contains("sensor.accelerometer") || f.name.contains("sensor.proximity") || f.name.contains("sensor.gyroscope")))
                b.append(f.name).append('\n');
        }
        b.append("\n===== the system apps this project depends on\n");
        for (String p : new String[] { "com.android.settings", "com.android.systemui", "com.android.launcher3", "android", "com.android.browser", "com.android.inputmethod.latin", "com.touchtype.swiftkey", c.getPackageName() }) pkg(b, pm, p);
        b.append("\n===== which Settings pages answer the public 'android.settings.*' actions (the front-door targets)\n");
        for (String a : new String[] { "SETTINGS", "WIFI_SETTINGS", "BLUETOOTH_SETTINGS", "DISPLAY_SETTINGS", "SOUND_SETTINGS", "APPLICATION_SETTINGS", "MANAGE_ALL_APPLICATIONS_SETTINGS", "INTERNAL_STORAGE_SETTINGS", "BATTERY_SAVER_SETTINGS", "DATE_SETTINGS", "LOCALE_SETTINGS", "ACCESSIBILITY_SETTINGS", "SECURITY_SETTINGS", "PRIVACY_SETTINGS", "DEVICE_INFO_SETTINGS", "INPUT_METHOD_SETTINGS", "NOTIFICATION_SETTINGS", "ZEN_MODE_SETTINGS", "WIRELESS_SETTINGS", "AIRPLANE_MODE_SETTINGS", "NFC_SETTINGS", "ADD_ACCOUNT_SETTINGS", "SYNC_SETTINGS", "USER_SETTINGS", "HOME_SETTINGS", "VOICE_INPUT_SETTINGS", "CAPTIONING_SETTINGS", "HARD_KEYBOARD_SETTINGS", "MEMORY_CARD_SETTINGS" }) {
            List<ResolveInfo> ris = pm.queryIntentActivities(new Intent("android.settings." + a), 0);
            b.append("android.settings.").append(a).append(" -> ");
            if (ris.isEmpty()) b.append("(nothing)");
            for (ResolveInfo ri : ris) b.append(ri.activityInfo.packageName).append('/').append(ri.activityInfo.name).append(' ');
            b.append('\n');
        }
        b.append("\n===== every activity of the Settings app (exported flag)\n");
        try {
            PackageInfo pi = pm.getPackageInfo("com.android.settings", PackageManager.GET_ACTIVITIES);
            if (pi.activities != null) for (ActivityInfo ai : pi.activities) b.append(ai.exported ? "exported  " : "private   ").append(ai.name).append('\n');
        } catch (Throwable t) { b.append("(not readable: ").append(t).append(")\n"); }
        names(b, c, "secure", Settings.Secure.CONTENT_URI);
        names(b, c, "global", Settings.Global.CONTENT_URI);
        names(b, c, "system", Settings.System.CONTENT_URI);
        permissions(b, c);
        return b.toString();
    }

    private static void permissions(StringBuilder b, Context c) {
        b.append("\n===== this launcher's permissions\n");
        for (String p : new String[] { "WRITE_SECURE_SETTINGS", "DUMP", "CHANGE_CONFIGURATION", "READ_EXTERNAL_STORAGE", "PACKAGE_USAGE_STATS", "INTERNET", "VIBRATE" })
            b.append(p).append(": ").append(c.checkSelfPermission("android.permission." + p) == PackageManager.PERMISSION_GRANTED ? "granted" : "NOT granted").append('\n');
    }

    /** Copies the tablet's own Settings / SystemUI / stock launcher / framework APKs into the share folder (readable by the app: system apps are world-readable). Returns a summary. */
    public static String copyApks(Context c) {
        StringBuilder b = new StringBuilder();
        File dir = new File(shareDir(c), "apk");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        PackageManager pm = c.getPackageManager();
        for (String p : new String[] { "com.android.settings", "com.android.systemui", "com.android.launcher3", "android" }) {
            try {
                String src = pm.getApplicationInfo(p, 0).sourceDir;
                File out = new File(dir, p + ".apk");
                try (FileInputStream in = new FileInputStream(src); FileOutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[64 * 1024]; int n; long total = 0;
                    while ((n = in.read(buf)) > 0) { os.write(buf, 0, n); total += n; }
                    b.append("copied ").append(p).append(" (").append(total / 1024).append(" KB)\n");
                }
            } catch (Throwable t) { b.append("could not copy ").append(p).append(": ").append(t.getClass().getSimpleName()).append('\n'); }
        }
        return b.toString();
    }

    /**
     * The launcher's OWN readers run on this ROM's real output. If a vendor ROM prints Recents or notifications in another shape the clean-up would silently do less; this shows it at once
     * (Tj, 2026-11-05: the report must hold everything Claude needs, so nothing has to be copied by hand).
     */
    static String selfCheck(boolean helper) {
        StringBuilder b = new StringBuilder("\n===== PARSER SELF-CHECK (the launcher's own readers, run on THIS ROM's real output)\n");
        if (!helper) return b.append("(the shell helper is not running: start it, then collect again)\n").toString();
        java.util.LinkedHashMap<Integer, String> tasks = ShellHelper.listTasks();
        if (tasks == null) b.append("Recents tasks parsed: (the helper did not answer)\n");
        else {
            b.append("Recents tasks parsed: ").append(tasks.size()).append('\n');
            for (java.util.Map.Entry<Integer, String> e : tasks.entrySet()) b.append("  task ").append(e.getKey()).append("  ").append(e.getValue()).append('\n');
        }
        java.util.Set<String> n = ShellHelper.notifPackages();
        b.append("Apps with a notification now (as the clean-up sees them): ").append(n == null ? "(the helper did not answer)" : n.isEmpty() ? "(none)" : n.toString()).append('\n');
        return b.toString();
    }

    /** The launcher's own log lines, as far as the app itself may read them (an app sees only its own lines; the shell helper's probe adds the rest when it runs). */
    private static String ownLog(int maxLines) { return ownLog(maxLines, Integer.MAX_VALUE); }

    /** PG3 (gate A of v0.7.3: the short summary was over 12 KB once in two runs): the short summary promises to stay under 12 KB, so the log gets whatever is left of the budget, newest lines first. */
    static final int SUMMARY_MAX = 11_000;
    private static final String LOG_CUT_NOTE = "(older log lines left out: the short summary is capped; the full report file has them)\n";

    /** Pure: the newest lines of {@code lines} (oldest first) whose lengths, one newline each, add up to at most {@code room}; none when even the last line does not fit. */
    static List<String> newestWithin(List<String> lines, int room) {
        int total = 0, from = lines.size();
        while (from > 0) {
            int add = lines.get(from - 1).length() + 1;
            if (total + add > room) break;
            total += add; from--;
        }
        return new java.util.ArrayList<>(lines.subList(from, lines.size()));
    }

    private static String ownLog(int maxLines, int roomChars) {
        StringBuilder b = new StringBuilder("\n\n===== THE LAUNCHER'S OWN LOG (what the app itself may read)\n");
        Process p = null;
        try {
            p = new ProcessBuilder("logcat", "-d", "-v", "time", "-s", "TjReset:I", "TjTasks:I", "TjA11y:I", "TjKeepAlive:I", "TjImmersive:I", "TjRouter:I", "TjHooks:I", "TjKeyboard:I", "TjRecentsGuard:I").redirectErrorStream(true).start();
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            java.util.ArrayDeque<String> last = new java.util.ArrayDeque<>();
            String l;
            while ((l = r.readLine()) != null) { last.add(l); if (last.size() > maxLines) last.removeFirst(); }
            List<String> fit = newestWithin(new java.util.ArrayList<>(last), roomChars - b.length() - LOG_CUT_NOTE.length());
            if (fit.size() < last.size()) b.append(LOG_CUT_NOTE);
            for (String s : fit) b.append(s).append('\n');
        } catch (Throwable t) { b.append("(not readable: ").append(t).append(")\n"); }
        finally { if (p != null) p.destroy(); }   // a read that failed half way must not leave the logcat child running
        return b.toString();
    }

    /**
     * A SHORT version of the report (a few KB, no shell probe): small enough to paste into a chat or send as a text message when the file cannot be attached. It uses the SAME section titles as the full
     * report, so {@code tools/tablet-intake.sh -} reads it too (its probe rows then say "(not in the report)"). Blocking when the helper runs (the parser self-check asks it): call off the main thread.
     */
    public static String summary(Context c, boolean helper) {
        StringBuilder b = new StringBuilder();
        b.append("===== TABLET SUMMARY (short: the full report is the file tablet-report.txt) ").append(new java.util.Date()).append('\n');
        deviceFacts(b, c);
        permissions(b, c);
        // PG3: the 11 000-character budget is spent in the order of importance: the facts above, the parser self-check, the launcher's state (its TAIL goes first: the power-menu dump, then old lock-log lines),
        // then the launcher's own log, newest lines first. The output order stays: facts, permissions, state, self-check, log.
        String check = selfCheck(helper);
        b.append(cutAtLine(ReportState.section(c, helper, false), SUMMARY_MAX - b.length() - check.length() - LOG_MIN));
        b.append(check);
        b.append(ownLog(40, SUMMARY_MAX - b.length()));
        return b.toString();
    }

    /** The room kept for the launcher's own log in the short summary (about five lines). */
    private static final int LOG_MIN = 700;

    /** Pure: {@code s} unchanged when it fits in {@code max} characters, else cut at the last line end that leaves room for a one-line note saying so (empty when even the note does not fit). */
    static String cutAtLine(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        String note = "(cut: the short summary is capped; the full report file has the rest)\n";
        int room = max - note.length();
        if (room <= 0) return "";
        int nl = s.lastIndexOf('\n', room - 1);
        return (nl < 0 ? "" : s.substring(0, nl + 1)) + note;
    }

    /** Builds the whole report (app side + state + parser self-check + helper probe when it runs) and writes it. Blocking: call off the main thread. Returns the file. */
    public static File write(Context c) {
        boolean helper = ShellHelper.reachable();
        StringBuilder b = new StringBuilder(appSide(c));
        b.append(ReportState.section(c, helper));
        b.append(selfCheck(helper));
        b.append("\n\n===== SHELL PROBE (read-only commands run by the helper)\n");
        String probe = helper ? ShellHelper.probe() : null;
        b.append(probe == null ? "(the shell helper is not running: start it with the adb line, then collect again for the full probe)\n" : probe);
        if (probe == null) b.append(ownLog(200));
        File f = new File(shareDir(c), "tablet-report.txt");
        try (FileOutputStream os = new FileOutputStream(f)) { os.write(b.toString().getBytes("UTF-8")); } catch (Throwable ignored) { }
        return f;
    }
}
