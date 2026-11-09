package com.tj.launcher;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * S2 (Tj, 2026-11-04: "Make the adb probe an app ... Make it so i can do everything in one session with adb"): the whole adb to-do list as one screen. Each row shows whether it is DONE;
 * "Do everything" applies the missing ones through the shell helper (only ids on ShellProtocol's fixed list), "Collect report" writes the tablet report + copies the system apps
 * (Settings, SystemUI, stock launcher, framework) into the share folder for {@code adb pull}. The only adb line needed first is the one that starts the helper.
 * Opens only in Quickstep mode (like the rest of Launcher control).
 */
public class SetupActivity extends Activity {
    public static final String EXTRA_AUTORUN = "autorun";          // emulator test: do everything + collect, then write probe/autorun-done.txt (only if the pref below is set)
    public static final String PREF_ALLOW_AUTORUN = "allow_setup_autorun";
    /** Emulator test only (same guard as the autorun): sets a recognisable picture as the LOCK-screen wallpaper so the lock-screen patch can be compared against it. */
    public static final String EXTRA_TEST_LOCK_WALLPAPER = "test_lock_wallpaper";
    /** Emulator test only (same guard): sets a FINELY patterned picture (2-px checkerboard on a gradient, twice the screen wide like the launcher's scrolling wallpaper) as the HOME wallpaper, so a wrong or misplaced home-patch crop shows. BI1. */
    public static final String EXTRA_TEST_HOME_WALLPAPER = "test_home_wallpaper";

    private void setTestLockWallpaper() {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int w = dm.widthPixels, h = dm.heightPixels;
            android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = 0xFF000000 | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | (255 - x * 255 / w);
            b.setPixels(px, 0, w, 0, 0, w, h);
            android.app.WallpaperManager.getInstance(this).setBitmap(b, null, true, android.app.WallpaperManager.FLAG_LOCK);
            Features.setString(this, "test_lock_wallpaper_done", "yes " + w + "x" + h);
        } catch (Throwable t) { Features.setString(this, "test_lock_wallpaper_done", "FAILED " + t); }
    }
    private void setTestHomeWallpaper() {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int w = dm.widthPixels * 2, h = dm.heightPixels;
            android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = 0xFF000000 | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | ((((x / 2 + y / 2) & 1) * 150) + 50);
            b.setPixels(px, 0, w, 0, 0, w, h);
            android.app.WallpaperManager.getInstance(this).setBitmap(b, null, true, android.app.WallpaperManager.FLAG_SYSTEM);
            Features.setString(this, "test_home_wallpaper_done", "yes " + w + "x" + h);
        } catch (Throwable t) { Features.setString(this, "test_home_wallpaper_done", "FAILED " + t); }
    }
    private static final int ACCENT = Color.parseColor("#00796B"), INK = Color.parseColor("#202124"), MUTED = Color.parseColor("#5F6368"), BAD = Color.parseColor("#B3261E");

    private LinearLayout page;
    private TextView log;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private float dp;

    /** One adb to-do item: how to see it is done, and which helper job does it (null = nothing to run). */
    private static final class Item {
        final String title, why, job;
        Item(String title, String why, String job) { this.title = title; this.why = why; this.job = job; }
    }

    private final Item[] items = {
        new Item("Permission: change secure settings", "Keyboard switch, tiles per launcher, animation speed-up.", "GRANT_WSS"),
        new Item("Permission: read what is running (DUMP)", "Read-only look at what is open.", "GRANT_DUMP"),
        new Item("Permission: change configuration", "For later features (font size, orientation).", "GRANT_CFG"),
        new Item("Permission: read storage", "Only for the wallpaper-patch power-menu mode and the share folder.", "GRANT_STORAGE"),
        new Item("Permission: battery numbers (usage stats)", "Only for the Battery page of the Settings look-alike: last full charge and screen time.", "GRANT_USAGE"),
        new Item("Usage access", "Which app was used last (read-only).", "OP_USAGE"),
        new Item("Modify system settings", "For later features (brightness, screen timeout).", "OP_WRITE"),
        new Item("Allowed to run in the background", "So Android does not freeze the helper.", "OP_BG"),
        new Item("Allowed to run any time in the background", "Same.", "OP_ANYBG"),
        new Item("Battery: not frozen while the screen is off", "The helper keeps working with the screen off (lock-time clean-up).", "IDLE_WL"),
        new Item("Speed: compile the launcher ahead of time", "Sideloaded apps run half-optimised until Android has time to compile them; this does it now (about a minute) so the launcher starts faster after an unlock. Redo after every update.", "COMPILE_SELF"),
        new Item("Accessibility helper is ON", "Power menu, pencil, lock screen handshakes, return reset.", "A11YON"),
        new Item("Shell helper is running", "Invisible clean-up and everything on this screen. Start it with the adb line below (again after every restart).", null),
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Mode.isCustom(this)) { finish(); return; }
        dp = getResources().getDisplayMetrics().density;
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(px(16), px(36), px(16), px(96));
        sv.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
    }

    @Override protected void onResume() {
        super.onResume();
        RecentsGuard.excludeOurTasks(this);
        RecentsGuard.describeAsTarget(this);
        if (Mode.isCustom(this)) { finish(); return; }
        build();
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_TEST_LOCK_WALLPAPER, false) && Features.get(this, PREF_ALLOW_AUTORUN, false)) {
            getIntent().removeExtra(EXTRA_TEST_LOCK_WALLPAPER);
            setTestLockWallpaper();
        }
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_TEST_HOME_WALLPAPER, false) && Features.get(this, PREF_ALLOW_AUTORUN, false)) {
            getIntent().removeExtra(EXTRA_TEST_HOME_WALLPAPER);
            setTestHomeWallpaper();
        }
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_AUTORUN, false) && Features.get(this, PREF_ALLOW_AUTORUN, false)) {
            getIntent().removeExtra(EXTRA_AUTORUN);
            autorun();
        }
    }

    private int px(int v) { return Math.round(v * dp); }

    private TextView text(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    // ---------------------------------------------------------------- is it done?
    private boolean done(Item it) {
        try {
            String job = it.job;
            if (job == null) return ShellHelper.reachable();
            PackageManager pm = getPackageManager();
            switch (job) {
                case "GRANT_WSS": return checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED;
                case "GRANT_DUMP": return checkSelfPermission("android.permission.DUMP") == PackageManager.PERMISSION_GRANTED;
                case "GRANT_CFG": return checkSelfPermission("android.permission.CHANGE_CONFIGURATION") == PackageManager.PERMISSION_GRANTED;
                case "GRANT_STORAGE": return checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED;
                case "GRANT_USAGE": return checkSelfPermission("android.permission.PACKAGE_USAGE_STATS") == PackageManager.PERMISSION_GRANTED;
                case "OP_USAGE": return op("android:get_usage_stats");
                case "OP_WRITE": return android.provider.Settings.System.canWrite(this);
                case "OP_BG": return op("android:run_in_background");
                case "OP_ANYBG": return op("android:run_any_in_background");
                case "IDLE_WL": return ((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
                case "COMPILE_SELF": return Features.getInt(this, "compile_done_code", 0) == getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                case "A11YON": return TjAccessibilityService.get() != null;
                default: return false;
            }
        } catch (Throwable t) { return false; }
    }

    /** Runs one allow-listed job; remembers that the speed compile finished for THIS version (the shell cannot tell us later). */
    private String shellJob(String job) {
        String out = ShellHelper.run(job);
        try { if ("COMPILE_SELF".equals(job) && out != null && !out.toLowerCase().contains("error") && !out.toLowerCase().contains("fail"))
            Features.setInt(this, "compile_done_code", getPackageManager().getPackageInfo(getPackageName(), 0).versionCode); } catch (Throwable ignored) { }
        return out;
    }

    private boolean op(String name) {
        AppOpsManager a = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return a != null && a.checkOpNoThrow(name, android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    // ---------------------------------------------------------------- the page
    private void build() {
        page.removeAllViews();
        page.addView(text("Tablet setup & report", 26, INK, true));
        page.addView(text("Every adb to-do item on one screen. Green = done. The only adb line you ever need first starts the shell helper (below); everything else is a button here. Press 1, then 2 (then 3 and 4 only if asked).", 14, MUTED, false));
        boolean helper = ShellHelper.reachable();
        page.addView(text(helper ? "Shell helper: RUNNING" : "Shell helper: NOT running - start it with the adb line (tap Copy), then come back", 15, helper ? ACCENT : BAD, true));
        if (!helper) page.addView(text("Why some buttons below are grey (UX1): 1 (do everything), 3 (side-key test), 4 (collect everything) and \"Disable the old Browser\" need the shell helper. 2 and 2b (the tablet report) work without it; the report then marks the lines that needed the helper.", 13, MUTED, false));
        List<Item> missing = new ArrayList<>();
        for (Item it : items) {
            boolean ok = done(it);
            if (!ok && it.job != null) missing.add(it);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(px(4), px(8), px(4), px(8));
            row.addView(text((ok ? "[done]  " : "[todo]  ") + it.title, 16, ok ? ACCENT : INK, true));
            row.addView(text(it.why, 13, MUTED, false));
            page.addView(row);
        }
        page.addView(button("Copy the helper line for a PHONE / adb app (no 'adb shell')", new Runnable() { public void run() { copy(ShellProtocol.START_DEVICE_LINE); } }));
        page.addView(button("Copy it wrapped in sh -c (for adb apps that do not use a shell)", new Runnable() { public void run() { copy(ShellProtocol.START_DEVICE_SH_C); } }));
        page.addView(button("Copy the helper line for a COMPUTER (adb shell ...)", new Runnable() { public void run() { copy(ShellProtocol.START_ONE_LINER); } }));
        final boolean canRun = helper;
        Button all = button("1. Do everything that is missing (" + missing.size() + ")", new Runnable() { public void run() { doEverything(); } });
        all.setEnabled(canRun && !missing.isEmpty());
        page.addView(all);
        page.addView(button("2. Collect the tablet report (small, about a minute) - SEND THIS FILE", new Runnable() { public void run() { collect(false); } }));
        page.addView(button("2b. Share a SHORT summary as text (if you cannot attach the file)", new Runnable() { public void run() { shareSummary(); } }));
        Button key = button("3. Side-key test (optional, 15 seconds)", new Runnable() { public void run() { keyTest(); } });
        key.setEnabled(canRun);
        page.addView(key);
        final String kout = Features.getString(this, "keytest_output", "");
        if (!kout.isEmpty()) {
            String ksum = Features.getString(this, "keytest_summary", "");
            page.addView(text("Side-key test: " + (ksum.isEmpty() ? ShellProtocol.keyTestSummary(kout) : ksum), 13, INK, false));
            String ans = Features.getString(this, "keytest_menu", "");
            if (ans.isEmpty()) {
                page.addView(text("Did the NORMAL power menu (Power off / Restart / Screenshot) appear while you held the key?", 14, INK, true));
                page.addView(button("No, it did NOT appear", new Runnable() { public void run() { keyAnswer("not_shown"); } }));
                page.addView(button("Yes, it appeared (press Back to close it)", new Runnable() { public void run() { keyAnswer("shown"); } }));
            } else page.addView(text("Your answer: the normal power menu " + ("shown".equals(ans) ? "APPEARED" : "did NOT appear") + ". Tap 2 again so the report holds it.", 13, MUTED, false));
        }
        Button big = button("4. Collect EVERYTHING: report + Settings screenshots + system apps (big, 2-3 minutes, only if Claude asks)", new Runnable() { public void run() { collect(true); } });
        big.setEnabled(canRun);
        page.addView(big);
        Button browser = button("Disable the old Browser app (com.android.browser) - reversible", new Runnable() { public void run() { runJob("BROWSER_OFF"); } });
        browser.setEnabled(canRun);
        page.addView(browser);
        log = text("", 13, MUTED, false);
        log.setPadding(px(4), px(12), px(4), px(12));
        page.addView(log);
        page.addView(text("Everything is published to the folder  Downloads / launcher-probe  on the tablet: open it with any file manager and send me tablet-report.txt first (it is small); zip the whole folder only if you pressed 4. (From a computer: adb pull " + ShellProtocol.PUBLISH_DIR + " probe-out.) Nothing is sent anywhere by this app.", 12, MUTED, false));
    }

    private Button button(String label, final Runnable r) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { r.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56));
        lp.topMargin = px(10);
        b.setLayoutParams(lp);
        return b;
    }

    private void copy(String s) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("adb", s));
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
    }

    private void say(final String s) { ui.post(new Runnable() { public void run() { if (log != null) log.append(s + "\n"); } }); }

    private void runJob(final String job) {
        new Thread(new Runnable() { public void run() {
            String out = job.equals("A11YON") ? ShellHelper.a11yOn() : shellJob(job);
            say(job + ": " + (out == null ? "FAILED (is the helper running?)" : (out.trim().isEmpty() ? "done" : out.trim())));
            ui.post(new Runnable() { public void run() { build(); } });
        } }, "tj-setup-job").start();
    }

    /** Tj, 2026-11-05 (Power menu v2): can the shell read the side key here, and does the "no power menu" flag keep the normal menu away? The helper does the 13-second routine; the person holds the key. */
    /** One side-key test at a time: a second tap while it runs would start a second 13 s routine (two getevent readers and two flag holders racing for the same key press and for the stored verdict). */
    private final java.util.concurrent.atomic.AtomicBoolean keyTestRunning = new java.util.concurrent.atomic.AtomicBoolean(false);

    private void keyTest() {
        if (!keyTestRunning.compareAndSet(false, true)) { say("The side-key test is already running: wait for its answer (about 13 seconds)."); return; }
        Features.setString(this, "keytest_menu", "");
        say("SIDE-KEY TEST: within the next 8 seconds hold the SIDE key (power) for about 1.5 seconds, then let go. Never hold it longer than 3 seconds.");
        new Thread(new Runnable() { public void run() {
            try {
                String res = runKeyTest();
                say(res == null ? "side-key test FAILED (is the helper running?)" : "side-key test: " + ShellProtocol.keyTestSummary(res));
            } finally {
                keyTestRunning.set(false);
                ui.post(new Runnable() { public void run() { build(); } });   // success AND failure: the screen must not keep an old verdict next to a cleared answer
            }
        } }, "tj-setup-keytest").start();
    }

    /** The helper's KEYTEST (about 13 s, blocking: call off the main thread) and where its result is remembered, so the screen AND the tablet report show it. Returns the raw answer, or null if the helper did not answer. */
    private String runKeyTest() {
        String out = ShellHelper.keyTest();
        if (out == null) return null;
        String res = out.trim();
        // the one-line verdict is computed from the WHOLE answer and stored on its own: the report cuts long values, and the flag line comes after up to 64 device and key lines
        Features.setString(this, "keytest_summary", ShellProtocol.keyTestSummary(res));
        Features.setString(this, "keytest_output", res.length() > 1500 ? res.substring(0, 1500) : res);
        Features.setString(this, "keytest_at", new java.util.Date().toString());
        return res;
    }

    private void keyAnswer(String a) {
        Features.setString(this, "keytest_menu", a);
        build();
    }

    private void doEverything() {
        final List<Item> todo = new ArrayList<>();
        for (Item it : items) if (it.job != null && !done(it)) todo.add(it);
        new Thread(new Runnable() { public void run() {
            for (Item it : todo) {
                String out = it.job.equals("A11YON") ? ShellHelper.a11yOn() : shellJob(it.job);
                say(it.job + ": " + (out == null ? "FAILED" : "done"));
            }
            say("finished: " + todo.size() + " item(s) applied. The list above refreshes.");
            ui.post(new Runnable() { public void run() { build(); } });
        } }, "tj-setup-all").start();
    }

    /** The report (and with {@code everything}: the Settings screenshots and the system apps), published to Downloads/launcher-probe where a file manager can reach it. */
    private void collect(final boolean everything) {
        say("collecting... (the report takes about a minute" + (everything ? ", the screenshots and apps a few more; touch nothing meanwhile" : "") + ")");
        new Thread(new Runnable() { public void run() {
            File f = TabletReport.write(SetupActivity.this);
            say("report written (" + (f.length() / 1024) + " KB)");
            ShellHelper.run("MKDIR_PUBLISH");
            String pub = ShellHelper.run("PUBLISH_REPORT");
            say(pub == null ? "could not publish the report to Downloads (is the shell helper running?): it stays in " + f : "published: " + ShellProtocol.PUBLISH_DIR + "/tablet-report.txt");
            if (everything) {
                String shots = ShellHelper.shots();
                say(shots == null ? "screenshots FAILED (helper not running?)" : "screenshots: " + shots.trim().replace('\n', ' '));
                String apks = ShellHelper.apks();
                say(apks == null ? "system apps FAILED" : "system apps: " + apks.trim().replace('\n', ' '));
            }
            if (pub == null) say("DONE, but the copy to Downloads did NOT happen (no shell helper). The report is only here: Android > data > com.tj.launcher > files > probe > tablet-report.txt (in a file manager), or tap \"2b. Share a SHORT summary as text\" and send me that text.");
            else say("DONE. Open Downloads / launcher-probe with a file manager, zip it and send it to me.");
        } }, "tj-setup-report").start();
    }

    /** The short summary (a few KB of text) goes through Android's share sheet, so Tj picks where it goes (a message to himself, e-mail, a chat); it is also copied. Nothing is sent by this app itself. */
    private void shareSummary() {
        say("making the short summary...");
        new Thread(new Runnable() { public void run() {
            final String text = TabletReport.summary(SetupActivity.this, ShellHelper.reachable());
            ui.post(new Runnable() { public void run() {
                try {
                    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("tablet summary", text));
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_SUBJECT, "Tablet summary (launcher)");
                    send.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(send, "Send the short summary to Claude"));
                    say("short summary: " + text.length() + " characters, also copied to the clipboard");
                } catch (Throwable t) { say("could not open the share sheet (" + t.getClass().getSimpleName() + "); the summary is on the clipboard: paste it into the chat"); }
            } });
        } }, "tj-setup-summary").start();
    }

    /** Emulator test: everything, then the report, then a marker file the test reads. */
    private void autorun() {
        new Thread(new Runnable() { public void run() {
            StringBuilder b = new StringBuilder();
            for (Item it : items) if (it.job != null && !done(it)) {
                String out = it.job.equals("A11YON") ? ShellHelper.a11yOn() : shellJob(it.job);
                b.append(it.job).append(out == null ? " FAILED" : " ok").append('\n');
            }
            // the side-key test first, so the report written next holds its verdict (the emulator test reads both)
            String kt = runKeyTest();
            b.append("keytest: ").append(kt == null ? "FAILED" : kt.replace('\n', '|')).append('\n');
            File f = TabletReport.write(SetupActivity.this);
            b.append("report ").append(f.length()).append(" bytes\n");
            try (java.io.FileOutputStream os = new java.io.FileOutputStream(new File(TabletReport.shareDir(SetupActivity.this), "summary.txt"))) {
                os.write(TabletReport.summary(SetupActivity.this, ShellHelper.reachable()).getBytes("UTF-8"));
                b.append("summary written\n");
            } catch (Throwable t) { b.append("summary FAILED ").append(t).append('\n'); }
            ShellHelper.run("MKDIR_PUBLISH");
            b.append("publish report: ").append(ShellHelper.run("PUBLISH_REPORT") == null ? "FAILED" : "ok").append('\n');
            String shots = ShellHelper.shots();
            b.append("shots: ").append(shots == null ? "FAILED" : shots.trim().replace('\n', ' ')).append('\n');
            String apks = ShellHelper.apks();
            b.append("apks: ").append(apks == null ? "FAILED" : apks.trim().replace('\n', ' ')).append('\n');
            for (Item it : items) b.append("state ").append(it.title).append(": ").append(done(it) ? "done" : "TODO").append('\n');
            try (java.io.FileOutputStream os = new java.io.FileOutputStream(new File(TabletReport.shareDir(SetupActivity.this), "autorun-done.txt"))) { os.write(b.toString().getBytes("UTF-8")); } catch (Throwable ignored) { }
            ui.post(new Runnable() { public void run() { build(); } });
        } }, "tj-setup-autorun").start();
    }
}
