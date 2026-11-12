package com.tj.launcher;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import com.android.launcher3.notification.NotificationListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Tj (2026-11-02): double Home back to the custom launcher "automatically and immediately closes all apps and clears all notifications and turns off all
 * connections ... and automatically and immediately opens the solitaire app". Each step is isolated (a failure in one never blocks the next), uses only normal
 * permissions (no secure settings, no lock-screen code), and the whole thing has a master switch in Launcher control.
 */
public final class ReturnReset {
    private static final String TAG = "TjReset";
    public static final String KEY = "return_reset";            // master switch, default ON
    public static final String APP_KEY = "return_app_label";    // label substring of the app to open, default "solitaire"
    public static final String DEFAULT_APP = "solitaire";
    public static final String CLEAR_NOTIF_KEY = "reset_clear_notifications";   // default ON
    public static final String CLEAR_RECENTS_KEY = "reset_clear_recents";       // default ON
    public static final String COVER_KEY = "reset_black_cover";                  // default ON: black cover while the reset drives the shade / Recents

    /**
     * BE2 / BE3 (Tj, 2026-11-07: "If it is better, do not start the custom launcher with solitaire, but start it with no apps at all and completely clear the recent apps list every time the custom launcher starts" /
     * "the custom launcher must be immediate or as close as possible"). CLEAN START: a custom session begins with the custom home alone on screen and an EMPTY Recents list; no stand-in app is started (a cold
     * Solitaire start on this 2 GB tablet was what kept the black cover up for seconds, and its task was one more card). "Begins" = a Quickstep session ends (plain PIN, double Home, the return methods) or the
     * tablet booted; NOT every unlock of a custom-only session and NOT every restart of our process (the system kills it under memory pressure: clearing then would close what Tj is using).
     * Shipped default ON; debug builds default OFF so the older emulator sections (written for the Solitaire behaviour) keep testing it; the new section sets it explicitly. OFF = the old Solitaire behaviour exactly.
     */
    public static final String CLEAN_START_KEY = "clean_start";
    /** Set at a new boot (clean start only): the persisted Recents list of the last boot is cleared at the first unlock. */
    public static final String BOOT_CLEAN_KEY = "clean_pending_boot";

    /** Pure: the default of the clean-start switch. */
    static boolean cleanStartDefault(boolean debuggable) { return !debuggable; }
    static boolean isDebuggable(Context c) { return (c.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0; }
    public static boolean cleanStart(Context c) { return Features.get(c, CLEAN_START_KEY, cleanStartDefault(isDebuggable(c))); }
    /** Seconds since the epoch when the boot flag was set (it expires after a day: code review 2026-11-07, a flag that outlived its boot would have cleared a later custom-only session). */
    public static final String BOOT_CLEAN_AT_KEY = "clean_pending_boot_at";
    static final int BOOT_CLEAN_MAX_AGE_S = 24 * 3600;
    /** Pure: is a boot flag set at {@code atS} still valid at {@code nowS}? */
    static boolean bootCleanValid(boolean flag, int atS, int nowS) { return flag && atS > 0 && nowS >= atS && nowS - atS <= BOOT_CLEAN_MAX_AGE_S; }
    private static int nowS() { return (int) (System.currentTimeMillis() / 1000L); }
    public static boolean bootCleanPending(Context c) { return bootCleanValid(Features.get(c, BOOT_CLEAN_KEY, false), Features.getInt(c, BOOT_CLEAN_AT_KEY, 0), nowS()); }
    public static void markBootClean(Context c) { if (cleanStart(c)) { Features.setInt(c, BOOT_CLEAN_AT_KEY, nowS()); Features.set(c, BOOT_CLEAN_KEY, true); } }
    public static void clearBootClean(Context c) { if (Features.get(c, BOOT_CLEAN_KEY, false)) Features.set(c, BOOT_CLEAN_KEY, false); }

    /**
     * BE3: does the complete clear of Recents have anything to do? Not when nothing was used since the last clear: the dirty flag, the tracker, Android's own usage log (any app resumed since, whoever opened it) and
     * a fresh boot all say "yes"; the first run after an install and any doubt count as "yes" too.
     */
    static boolean recentsNeeded(Context c) { return recentsNeededCheap(c) || !AppOrigin.usedSinceClearCached(c).isEmpty(); }   // the usage log is read at the LOCK (AppOrigin.refreshUsedCacheAsync), not on the unlock path

    /** The same without the usage-log query (a few preference reads): for the Enter tap, which runs on the main thread while the cover must go up at once. */
    static boolean recentsNeededCheap(Context c) {
        return !AppOrigin.tracked(c) || !AppOrigin.pending(c).isEmpty() || RecentsDirty.isDirty(c) || bootCleanPending(c);
    }

    public static boolean clearNotifications(Context c) { return Features.get(c, CLEAR_NOTIF_KEY, true); }
    public static boolean clearRecents(Context c) { return Features.get(c, CLEAR_RECENTS_KEY, true); }

    private ReturnReset() {}

    public static boolean enabled(Context c) { return Features.get(c, KEY, true); }

    /**
     * AC2 / AC8 (Tj, 2026-11-05: "I turned off the feature to automatically open solitaire, yet I see it in the Quickstep launcher recent apps often"; "it opens solitaire also. I turned the solitaire setting off,
     * so it shouldn't load it under the lock screen"). ROOT CAUSE: the "Open Solitaire" switch ({@link TjHooks#STARTUP_KEY}) was read only when the launcher STARTS; the lock-screen preparation started the target
     * app on every locked wake whatever the switch said, and every wake left a Solitaire task that Quickstep's Recents then listed. The preparation now asks this first.
     * (The RETURN reset keeps its own controls, the app-name box and "Plain (no app)": Tj asked for that behaviour on 2026-11-02.)
     */
    public static boolean autoOpenWanted(Context c) { return !cleanStart(c) && Features.get(c, TjHooks.STARTUP_KEY, true); }   // BE3: a clean start never opens a stand-in app
    public static String appLabel(Context c) { return Features.getString(c, APP_KEY, DEFAULT_APP); }

    /**
     * N6: is the INVISIBLE path possible right now? Needs the shell helper (adb, once per boot) and the switch. Checked on the main thread: a refused local connection fails at once.
     * Cached for 4 s so the Enter-tap check and the unlock itself do not both pay for it.
     */
    private static volatile long invisAt = 0;
    private static volatile boolean invisVal = false;
    public static boolean invisibleReady(Context c) {
        if (!ShellHelper.enabled(c)) return false;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - invisAt < 4000) return invisVal;
        boolean v = ShellHelper.reachable();
        invisVal = v; invisAt = now;
        return v;
    }

    /** The packages to force-stop on return: what Quickstep opened (or, before anything is tracked, every launchable app except the custom ones). Never us, the stock launcher, the keyboard, the target app. */
    private static java.util.List<String> invisibleTargets(Context c) {
        ComponentName stock = StockLauncher.find(c);
        String ime = null;
        String cur = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (cur != null && cur.contains("/")) ime = cur.substring(0, cur.indexOf('/'));
        String target = cleanStart(c) ? null : targetPackage(c);   // BE3: a clean start has no stand-in: Solitaire is an app like any other
        java.util.Set<String> pkgs = new java.util.LinkedHashSet<>();
        if (AppOrigin.tracked(c)) pkgs.addAll(AppOrigin.pending(c));
        else {
            java.util.Set<String> keep = AppOrigin.custom(c);
            for (ResolveInfo ri : c.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))
                if (!keep.contains(ri.activityInfo.packageName)) pkgs.add(ri.activityInfo.packageName);
        }
        java.util.List<String> out = new ArrayList<>();
        for (String p : pkgs) {
            if (p.equals(target)) continue;
            if (ResetPolicy.shouldKill(p, c.getPackageName(), stock == null ? null : stock.getPackageName(), ime)) out.add(p);
        }
        return out;
    }

    /** BE2: the same guard for the Recents sweep. */
    static TaskOrigin.Guard guardFor(Context c) { return guard(c); }

    /** AA3: what may be closed at all (never this app, the stock launcher, the keyboard, the system). */
    private static TaskOrigin.Guard guard(final Context c) {
        ComponentName stock = StockLauncher.find(c);
        final String stockPkg = stock == null ? null : stock.getPackageName();
        String cur = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        final String ime = (cur != null && cur.contains("/")) ? cur.substring(0, cur.indexOf('/')) : null;
        final String self = c.getPackageName();
        return new TaskOrigin.Guard() { @Override public boolean mayClose(String pkg) { return ResetPolicy.shouldKill(pkg, self, stockPkg, ime); } };
    }

    /**
     * Solitaire (or the target app) at once; the Quickstep apps are force-stopped BEHIND it: Recents cards, notifications and processes vanish with no shade, no Recents screen, no cover.
     * BE3: in a clean start nothing is opened at all: the custom home is already the screen, and every Recents task is removed by id behind it (complete clear).
     */
    private static void runInvisible(final Context app, final boolean clean) {
        final long t0 = android.os.SystemClock.uptimeMillis();
        final java.util.List<String> pkgs = invisibleTargets(app);
        final boolean first = !AppOrigin.tracked(app);
        final String target = clean ? null : targetPackage(app);
        final TaskOrigin.Guard guard = guard(app);
        new Thread(new Runnable() { @Override public void run() {
            try { radiosOff(app); } catch (Throwable t) { Log.e(TAG, "radios failed", t); }
            TaskOrigin.Outcome oc = TaskOrigin.finish(app, pkgs, target, guard, clean);   // AA3: ground truth from the helper's task list when a start-of-session snapshot exists
            int n = ShellHelper.forceStop(oc.stop);
            int m = (n >= 0 && !oc.removeIds.isEmpty()) ? ShellHelper.removeTasks(oc.removeIds) : 0;
            if (n >= 0) { AppOrigin.clearQuickstep(app); AppOrigin.setTracked(app); }
            if (clean && n >= 0 && oc.listed && m >= 0) { RecentsDirty.clear(app); clearBootClean(app); }   // BE3: the list was read and every task removed (no cap: the helper client sends them in chunks): the next return has nothing to redo
            invisAt = 0;
            String line = "INVISIBLE: " + (n < 0 ? "the shell helper stopped answering, " + oc.stop.size() + " apps NOT stopped (next return uses the cover method)" : "force-stopped " + n + " of " + oc.stop.size() + " apps"
                    + (first ? " (first run: every non-custom app)" : "") + " in " + (android.os.SystemClock.uptimeMillis() - t0) + " ms; no cover, no shade, no Recents screen; " + oc.note + (m != 0 ? " [task removal answered " + m + "]" : ""));
            report(app, line);
        } }, "tj-reset-invisible").start();
        try { if (clean) settleHome(app); else openApp(app); } catch (Throwable t) { Log.e(TAG, "open app failed", t); }
    }

    /**
     * O4: the clean-up WITHOUT opening anything: force-stop Quickstep's apps (their notifications go with them), remove their Recents cards, radios off. Used at LOCK, so the unlock has nothing left to do.
     * Only with the shell helper (nothing here touches the screen). Returns false (and does nothing) when the helper is not reachable. BE3: with a clean start EVERY Recents task goes (complete clear).
     */
    public static boolean cleanAtLock(final Context c) {
        final Context app = c.getApplicationContext();
        if (!enabled(app) || !invisibleReady(app)) return false;
        final long t0 = android.os.SystemClock.uptimeMillis();
        final boolean clean = cleanStart(app);
        final java.util.List<String> pkgs = invisibleTargets(app);
        final boolean first = !AppOrigin.tracked(app);
        final String target = clean ? null : targetPackage(app);
        final TaskOrigin.Guard guard = guard(app);
        new Thread(new Runnable() { @Override public void run() {
            try { radiosOff(app); } catch (Throwable t) { Log.e(TAG, "radios failed", t); }
            TaskOrigin.Outcome oc = TaskOrigin.finish(app, pkgs, target, guard, clean);   // AA3
            int n = ShellHelper.forceStop(oc.stop);
            int m = (n >= 0 && !oc.removeIds.isEmpty()) ? ShellHelper.removeTasks(oc.removeIds) : 0;
            if (n >= 0) { AppOrigin.clearQuickstep(app); AppOrigin.setTracked(app); }
            if (clean && n >= 0 && oc.listed && m >= 0) { RecentsDirty.clear(app); clearBootClean(app); }
            invisAt = 0;
            String line = "AT LOCK: " + (n < 0 ? "the shell helper stopped answering, " + oc.stop.size() + " apps NOT stopped" : "force-stopped " + n + " of " + oc.stop.size() + " apps" + (first ? " (first run: every non-custom app)" : "") + " in " + (android.os.SystemClock.uptimeMillis() - t0) + " ms; " + oc.note + (m != 0 ? " [task removal answered " + m + "]" : "") + " ")
                    + "; the unlock has nothing left to close";
            report(app, line);
            LockLog.add(app, "CLEANLOCK", (clean ? "complete clear: " : "") + oc.note);   // BE3: the persistent log shows what the lock-time clean-up did (it was only in the report before)
        } }, "tj-lock-clean").start();
        return true;
    }

    public static void run(final Context c) {
        if (!enabled(c)) return;
        final Context app = c.getApplicationContext();
        final boolean clean = cleanStart(app);
        if (invisibleReady(app)) { runInvisible(app, clean); return; }
        // W1: the slow, blocking parts (a binder call per app, Wi-Fi/Bluetooth) run on their own thread, so the main thread starts pressing "Clear all" at once.
        new Thread(new Runnable() { @Override public void run() {
            try { killApps(app); } catch (Throwable t) { Log.e(TAG, "kill failed", t); }
            try { radiosOff(app); } catch (Throwable t) { Log.e(TAG, "radios failed", t); }
        } }, "tj-reset-bg").start();
        final boolean notif = clearNotifications(app), recents = clearRecents(app);
        TjAccessibilityService svc = TjAccessibilityService.get();
        if (svc != null && (notif || recents)) {
            // B4: look first. Nothing in the status bar = no shade step; Recents untouched since the last clear = no Recents step. Both skipped = the home at once, nothing else on screen.
            Boolean icons = notif ? svc.hasNotificationIcons() : Boolean.FALSE;
            final boolean seen = notif && NotifSightings.dirty(app);   // BH1: something was posted since the shade was last cleared; a SILENT notification has no status-bar icon but the shade lists it
            final boolean doShade = NotifSightings.shadeStepWanted(notif, icons, seen);
            if (doShade) NotifSightings.clearDirty(app);   // whatever arrives from here on sets it again
            // D5: only apps opened from QUICKSTEP are closed; apps opened from the custom launcher stay. First run after install (untracked) clears everything once.
            // BE3 (clean start): complete clear = Recents "Clear all", whenever anything was used since the last clear (no label matching: nothing can be mis-matched or missed)
            final boolean tracked = AppOrigin.tracked(app);
            final java.util.Set<String> pendPkgs = AppOrigin.pending(app);
            final boolean doRecents = recents && (clean ? recentsNeeded(app) : (!tracked || !pendPkgs.isEmpty()));
            final java.util.Set<String> labels = (!clean && tracked && !AppOrigin.custom(app).isEmpty()) ? labelsOf(app, pendPkgs) : null;   // selective dismissal only when something must be preserved
            // AI1: why an app was (not) closed is now in the lock log. v0.7.6 (review): LockLog cuts a line at 200 characters, so the decision flags and the COUNTS come first and the unbounded lists last
            LockLog.add(app, "RESET", "plan: " + (clean ? "clean start, " : "") + "tracked=" + tracked + ", Recents step=" + doRecents + ", shade step=" + doShade + " (icons=" + icons + ", seen=" + seen + "), pending=" + pendPkgs.size() + ", kept(custom)=" + AppOrigin.custom(app).size() + ", cards to close=" + labels + ", opened in Quickstep=" + pendPkgs);
            if (!doShade && !doRecents) {
                fastActive = false;
                svc.appendReport("nothing to clear (status bar shows no notifications" + (recents ? ", no app was opened from Quickstep since the last clear" : "") + "): " + (clean ? "the custom home stays" : "Solitaire at once"));
                Log.i(TAG, "RESET nothing to clear: " + (clean ? "the custom home stays" : "Solitaire at once"));
                try { if (clean) settleHome(app); else openApp(app); } catch (Throwable t) { Log.e(TAG, "open app failed", t); }
                return;
            }
            final boolean fast = AnimationSpeedup.begin(app);   // Y2b: no animations while the reset runs (restored below, and by three failsafes)
            fastActive = fast;
            if (Features.get(app, COVER_KEY, true)) svc.showResetCover();   // the shade / Recents are driven behind a black cover: you never see them flicker
            // Android Go has no Notification access, and killBackgroundProcesses is not enough: press "Clear all" in the real shade / Recents, THEN open the app (BE3: then show the home).
            try { svc.runReset(doShade, doRecents, labels, seen, new Runnable() { @Override public void run() {
                try { if (clean) settleHome(app); else openApp(app); } catch (Throwable t) { Log.e(TAG, "open app failed", t); }   // the flags (dirty, boot) are cleared by the Recents step ITSELF, and only when it really finished (code review 2026-11-07)
                AnimationSpeedup.restoreLater(app, clean ? 2000 : (fast ? 700 : 0)); } }); return; }   // BF1: a clean start releases the animations the moment the custom home is in front (TjAccessibilityService.settleHome, or its own 1.5 s timeout); this timer is only the backstop. The Solitaire flow keeps its 700 ms
            catch (Throwable t) { Log.e(TAG, "accessibility reset failed, using the simple reset", t); svc.hideResetCover(); }
        } else if (svc == null && (notif || recents)) {
            Features.setString(app, TjAccessibilityService.RESET_REPORT_KEY, "Accessibility helper is OFF: notifications and Recents were NOT cleared (turn it on in Accessibility settings).");
        } else {
            Features.setString(app, TjAccessibilityService.RESET_REPORT_KEY, "Both Clear all switches are OFF.");
        }
        if (notif) { try { clearNotifications(); } catch (Throwable t) { Log.e(TAG, "notifications failed", t); } }
        try { if (clean) settleHome(app); else openApp(app); } catch (Throwable t) { Log.e(TAG, "open app failed", t); }
        AnimationSpeedup.restoreLater(app, 0);
    }

    /**
     * BE3 (clean start): the custom home is the screen. After the clean-up (or when there was nothing to clean) the accessibility helper makes sure the custom launcher is the front window (one Home press if
     * Recents or the shade is still up) and takes the black cover away THE MOMENT it is: the cover used to stay until a cold Solitaire start was verified, which on this tablet took seconds.
     */
    static void settleHome(Context c) {
        TjAccessibilityService svc = TjAccessibilityService.get();
        if (svc != null) { svc.settleHome(); return; }
        try {   // no accessibility helper: nothing to press and nothing to watch, so start the custom home itself (quiet: the router's pre-arm start, no Home press, no stand-in app)
            Context app = c.getApplicationContext();
            app.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(app.getPackageName()).putExtra(RouterActivity.EXTRA_PREARM, "custom").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (Throwable t) { Log.e(TAG, "settle home without the helper failed", t); }
    }

    /** True while this reset runs with the animations off: the waits can be shorter. */
    public static volatile boolean fastActive = false;

    /** The app labels of these packages (what a Recents card says). */
    private static java.util.Set<String> labelsOf(Context c, java.util.Set<String> pkgs) {
        java.util.Set<String> out = new java.util.HashSet<>();
        PackageManager pm = c.getPackageManager();
        for (String p : pkgs) {
            try { out.add(String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(p, 0)))); } catch (Throwable ignored) { }
        }
        return out;
    }

    private static void killApps(Context c) {
        PackageManager pm = c.getPackageManager();
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName stock = StockLauncher.find(c);
        String ime = null;
        String cur = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        if (cur != null && cur.contains("/")) ime = cur.substring(0, cur.indexOf('/'));
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        java.util.Set<String> keep = AppOrigin.custom(c);
        int n = 0;
        for (ResolveInfo ri : pm.queryIntentActivities(launcher, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (keep.contains(pkg)) continue;   // D5: apps opened from the custom launcher stay open
            if (ResetPolicy.shouldKill(pkg, c.getPackageName(), stock == null ? null : stock.getPackageName(), ime)) {
                am.killBackgroundProcesses(pkg);
                n++;
            }
        }
        Log.i(TAG, "asked Android to close " + n + " apps");
    }

    private static void clearNotifications() {
        NotificationListener l = NotificationListener.getInstanceIfConnected();
        if (l == null) { Log.i(TAG, "notifications NOT cleared: notification access is not enabled for the launcher"); return; }
        l.cancelAllNotifications();
        Log.i(TAG, "cleared notifications");
    }

    static void radiosOff(Context c) {
        final boolean keep = RadioIconHide.keepsRadiosOn(c);   // BS1 + BS2 (Tj): Wi-Fi and Bluetooth stay on (only if they already are: nothing here turns one on), only their symbols go
        WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null && wm.isWifiEnabled()) {
            if (keep) Log.i(TAG, "wifi left on (BS1: its symbol is hidden instead)");
            else { boolean ok = wm.setWifiEnabled(false); Log.i(TAG, "wifi off -> " + ok); }
        }
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null && bt.isEnabled()) {
            if (keep) Log.i(TAG, "bluetooth left on (BS2: its symbol is hidden instead)");
            else { boolean ok = bt.disable(); Log.i(TAG, "bluetooth off -> " + ok); }
        }
    }

    private static volatile String cachedLabel, cachedPkg;
    private static volatile long cachedAt = 0;
    private static volatile boolean refreshing = false;
    /** Review LOW2 (2026-11-12): a stored "no app" ("") was taken on trust in this process and its background re-check has not landed yet ({@link #targetForAction}). */
    private static volatile boolean noneUnchecked = false;
    private static final long TARGET_TTL_MS = 10 * 60_000;
    private static final long NO_TARGET_TTL_MS = 30_000;       // "no such app installed" is re-checked soon: it may be installed a minute later

    /**
     * Package of the app we open / keep in Recents (e.g. Solitaire), or null. AB2 (efficiency): finding it means asking Android for the label of EVERY launchable app (a resource lookup in each APK: on
     * the MT8168 that is 100+ ms and it ran on the main thread every 30 s, on every return to the custom launcher and on every app launch). Now the answer is kept for 10 minutes, REMEMBERED in
     * the preferences across process restarts (checked to still be installed), and re-checked on a background thread; only the very first lookup for a label scans on the calling thread.
     */
    public static String targetPackage(Context c) {
        final Context app = c.getApplicationContext();
        final String label = appLabel(app);
        long now = android.os.SystemClock.elapsedRealtime();
        if (cachedLabel != null && cachedLabel.equals(label)) {
            String known = cachedPkg;
            if (known != null && !isInstalled(app, known)) return scanTarget(app, label);          // uninstalled since: look again now (the rare case; one cheap package lookup otherwise)
            if (now - cachedAt > (known == null ? NO_TARGET_TTL_MS : TARGET_TTL_MS) && !refreshing) refreshTargetAsync(app, label);
            return known;
        }
        String saved = Features.getString(app, "target_pkg_for_" + label.toLowerCase(java.util.Locale.ROOT), null);
        int kind = savedKind(saved);
        if (kind == SAVED_NO_APP) {                     // BY1a A2: the last full scan found no app for this label: a known "none", re-checked off the calling thread (it was a full label scan on the calling thread, often main, in every new process)
            cachedPkg = null; cachedAt = now; cachedLabel = label;
            noneUnchecked = true;
            refreshTargetAsync(app, label);
            return null;
        }
        if (kind == SAVED_PACKAGE && isInstalled(app, saved)) {
            cachedLabel = label; cachedPkg = saved; cachedAt = now;
            refreshTargetAsync(app, label);            // is there a better match now? (off the calling thread)
            return saved;
        }
        return scanTarget(app, label);                  // the first time ever for this label
    }

    /** BY1a A2 (pure): what a stored answer of {@link #scanTarget} means: nothing stored (never scanned for this label), "" (the scan found no app), or a package (still to be checked as installed). */
    static final int SAVED_NOTHING = 0, SAVED_NO_APP = 1, SAVED_PACKAGE = 2;
    static int savedKind(String saved) { return saved == null ? SAVED_NOTHING : saved.isEmpty() ? SAVED_NO_APP : SAVED_PACKAGE; }

    private static boolean isInstalled(Context c, String pkg) {
        try { c.getPackageManager().getApplicationInfo(pkg, 0); return true; } catch (PackageManager.NameNotFoundException e) { return false; }
    }

    /** The slow, complete search; stores the answer in memory and in the preferences. */
    private static String scanTarget(Context app, String label) {
        long t0 = android.os.SystemClock.elapsedRealtime();
        List<String> labels = new ArrayList<>();
        List<ResolveInfo> ris = launchables(app, labels);
        int i = ResetPolicy.pickLabel(labels, label);
        String pkg = i < 0 ? null : ris.get(i).activityInfo.packageName;
        cachedPkg = pkg; cachedLabel = label; cachedAt = android.os.SystemClock.elapsedRealtime();
        Features.setString(app, "target_pkg_for_" + label.toLowerCase(java.util.Locale.ROOT), pkg == null ? "" : pkg);
        noneUnchecked = false;
        Log.i(TAG, "target app for '" + label + "' = " + pkg + " (full scan of " + ris.size() + " apps took " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms)");
        return pkg;
    }

    /**
     * The target for a path that ACTS on the answer (starts the app, or goes Home because there is none). Review LOW2 (2026-11-12): a stored "no app" taken on trust in a new process while its background
     * re-check is still running could send the startup Home although the app was installed since (clean start off, Solitaire reinstalled, process restarted): such a "none" is checked now, once (the
     * scan these paths always ran before BY1a A2; never on the common paths).
     */
    private static String targetForAction(Context c) {
        String pkg = targetPackage(c);
        if (pkg == null && noneUnchecked) { Context app = c.getApplicationContext(); pkg = scanTarget(app, appLabel(app)); }
        return pkg;
    }

    private static void refreshTargetAsync(final Context app, final String label) {
        if (refreshing) return;
        refreshing = true;
        new Thread(new Runnable() { @Override public void run() {
            try { scanTarget(app, label); } catch (Throwable t) { Log.e(TAG, "target refresh failed", t); } finally { refreshing = false; }
        } }, "tj-target-scan").start();
    }

    /** The launchable activities of ONE package (a handful, not the whole phone) and the index of the one whose label best matches ours (0 if none does). */
    private static List<ResolveInfo> launchablesOf(Context c, String pkg, List<String> labelsOut) {
        PackageManager pm = c.getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg), 0);
        for (ResolveInfo ri : ris) labelsOut.add(String.valueOf(ri.loadLabel(pm)));
        return ris;
    }

    /** Label of the app we would open, or null if none is installed (used by Launcher control's status line). */
    public static String findAppLabel(Context c) {
        String pkg = targetPackage(c);
        if (pkg == null) return null;
        List<String> labels = new ArrayList<>();
        launchablesOf(c, pkg, labels);
        int i = ResetPolicy.pickLabel(labels, appLabel(c));
        return labels.isEmpty() ? null : labels.get(i < 0 ? 0 : i);
    }

    private static List<ResolveInfo> launchables(Context c, List<String> labelsOut) {
        PackageManager pm = c.getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        for (ResolveInfo ri : ris) labelsOut.add(String.valueOf(ri.loadLabel(pm)));
        return ris;
    }

    /** N3: start the target app once, with no verification loop and no Home press: used while the lock screen still covers everything (the accessibility check cannot see behind it). */
    public static void startTargetBehindLock(Context c) {
        Context app = c.getApplicationContext();
        if (!autoOpenWanted(app)) { Log.i(TAG, "lock-screen preparation: the target app is NOT started (\"Open Solitaire automatically\" is off)"); return; }   // AC2/AC8
        String pkg = targetForAction(app);
        if (pkg == null) return;
        List<String> labels = new ArrayList<>();
        List<ResolveInfo> ris = launchablesOf(app, pkg, labels);
        if (ris.isEmpty()) return;
        int i = ResetPolicy.pickLabel(labels, appLabel(app));
        launchOnce(app, ris.get(i < 0 ? 0 : i), 0);
    }

    /** N3: might the return reset have anything to do (used to decide whether the black cover goes up at the Enter tap)? */
    public static boolean mayHaveWork(Context c) {
        if (!enabled(c) || invisibleReady(c)) return false;   // the invisible path needs no cover
        return clearNotifications(c) || (clearRecents(c) && (cleanStart(c) ? recentsNeededCheap(c) : (!AppOrigin.tracked(c) || !AppOrigin.pending(c).isEmpty())));
    }

    /** Puts the target app in front because the custom launcher just STARTED (boot / restart). Same safe, verified launch as the return reset; nothing else is touched. */
    public static void openStartupApp(Context c) { openApp(c.getApplicationContext()); }

    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());

    /** Starts the target app and makes sure it ends up in FRONT: re-starts it (harmless if already on top) up to 3 more times, checking with the accessibility helper when it is on. */
    private static void openApp(final Context c) {
        if (cleanStart(c)) { settleHome(c); return; }   // BE3: a clean start never starts a stand-in app
        final long t0 = android.os.SystemClock.elapsedRealtime();
        final List<String> labels = new ArrayList<>();
        String targetPkg = targetForAction(c);                     // AB2: cached, no label loading for every installed app (LOW2: an unchecked stored "none" is checked once)
        List<ResolveInfo> ris = targetPkg == null ? new ArrayList<ResolveInfo>() : launchablesOf(c, targetPkg, labels);
        int i = ris.isEmpty() ? -1 : Math.max(0, ResetPolicy.pickLabel(labels, appLabel(c)));
        Log.i(TAG, "openApp: target resolved in " + (android.os.SystemClock.elapsedRealtime() - t0) + " ms");
        if (i < 0) {
            Log.i(TAG, "no app matching '" + appLabel(c) + "' found"); report(c, "app: no app matching '" + appLabel(c) + "' is installed");
            // Nothing to put in front, so leave Recents / the shade ourselves (found by the E1 emulator test: Recents stayed open). No Solitaire start can race with this Home press.
            TjAccessibilityService svc = TjAccessibilityService.get();
            if (svc != null) { svc.hideResetCover(); svc.goHome(); }
            return;
        }
        final ResolveInfo ri = ris.get(i);
        final String label = labels.get(i);
        launchOnce(c, ri, 0);
        scheduleVerify(c, ri, label, 1);
    }

    private static void launchOnce(Context c, ResolveInfo ri, int attempt) {
        Intent it = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | (attempt > 0 ? Intent.FLAG_ACTIVITY_REORDER_TO_FRONT : 0));
        c.startActivity(it);
        Log.i(TAG, "start app attempt " + (attempt + 1));
    }

    private static void scheduleVerify(final Context c, final ResolveInfo ri, final String label, final int attempt) {
        MAIN.postDelayed(new Runnable() { @Override public void run() {
            try {
                TjAccessibilityService svc = TjAccessibilityService.get();
                boolean front = svc != null && svc.isForeground(ri.activityInfo.packageName);
                if (front) { report(c, "app: " + label + " is on screen" + (attempt > 1 ? " (needed " + attempt + " starts)" : "")); if (svc != null) svc.hideResetCover(); return; }
                if (attempt >= 5) { if (svc != null) svc.hideResetCover(); report(c, "app: " + label + " was started " + attempt + " times but could not be confirmed on screen" + (svc == null ? " (accessibility helper off, cannot check)" : "")); return; }
                launchOnce(c, ri, attempt);                 // not in front yet (e.g. the home screen came up late): bring it forward again
                scheduleVerify(c, ri, label, attempt + 1);
            } catch (Throwable t) { Log.e(TAG, "verify failed", t); }
        } }, attempt == 1 ? (fastActive ? 300 : 450) : 500);
    }

    private static void report(Context c, String line) {
        TjAccessibilityService svc = TjAccessibilityService.get();
        if (svc != null) svc.appendReport(line);
        else Features.setString(c, TjAccessibilityService.RESET_REPORT_KEY, line);
        Log.i(TAG, line);
    }
}
