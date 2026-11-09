package com.tj.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

/**
 * BN1 (Tj, 2026-11-09): the look-alike of the stock Settings app, built into this APK (pages: {@link SsPage}; plan: research/SETTINGS_BUILD_PLAN_2026-11-09.md).
 *
 * <p>How it gets into the custom drawer: the manifest has an {@code activity-alias} {@link #ALIAS} (label "Settings", the stock icon, LAUNCHER filter, disabled by default). The drawer lists only
 * components with a LAUNCHER filter, and {@link AppVisibility#ALWAYS_SHOWN_OWN_COMPONENTS} lets that one own component through. The alias is ENABLED only in the custom mode and only while the switch
 * {@link #KEY} is on ({@link #aliasWanted}), so the stock (Quickstep) drawer keeps its single real Settings. The real Settings is kept out of the custom drawer while the look-alike is the entry
 * ({@link #hidesRealSettings}).
 *
 * <p>The switch is LEVEL-triggered: {@link #syncSoon} re-reads the facts (switch, mode) at process start, at every mode change and when the switch is flipped, and only calls the package manager when the alias
 * is not already in the wanted state. It runs a few seconds late on a background thread, OFF the unlock path (an unlock must stay fast, BK1; the drawer is not opened in that second).
 */
public final class SettingsSpoof {
    /** The switch in Launcher control. */
    public static final String KEY = "settings_spoof";
    /** Name of the manifest activity-alias (no class behind it: the target is {@link SsPageActivity}). */
    public static final String ALIAS = "com.tj.launcher.SettingsAlias";
    /** The stock Settings app: hidden from the custom drawer while the look-alike replaces it. */
    public static final String REAL_SETTINGS_PACKAGE = "com.android.settings";
    /** Text preference (allow-listed in the tablet report): what the last alias sync did. */
    public static final String STATUS_KEY = "ss_alias_status";
    /** Delay of the background sync (ms). */
    static final long SYNC_DELAY_MS = 3000;
    private static final String TAG = "TjSettingsSpoof";

    private SettingsSpoof() {}

    /**
     * The default of {@link #KEY}. WORK IN PROGRESS: OFF everywhere until the look-alike is finished and its gate is green (a release made mid-way must be harmless); the finished build switches release builds
     * ON (debug builds stay OFF like clean_start: the emulator sections that do not test the spoof must not see a second Settings in the drawer; section "ss" turns it on).
     */
    public static boolean defaultOn(boolean debuggable) { return false; }

    public static boolean switchOn(Context c) { return Features.get(c, KEY, defaultOn(ReturnReset.isDebuggable(c))); }

    /** Pure: the alias (the drawer entry) exists only in the custom mode and only while the switch is on. */
    public static boolean aliasWanted(boolean switchOn, boolean customMode) { return switchOn && customMode; }

    /** Pure: the real Settings is out of the custom drawer exactly while the look-alike is the entry. In Quickstep mode nothing is hidden (the chooser there shows the true state). */
    public static boolean hidesRealSettings(boolean switchOn, boolean customMode, String pkg) {
        return REAL_SETTINGS_PACKAGE.equals(pkg) && aliasWanted(switchOn, customMode);
    }

    /** Pure: an open look-alike page may stay on screen (the custom mode, the switch on); otherwise it closes itself on resume (a stale task in a Quickstep session must not show our pages). */
    public static boolean pageAllowed(boolean switchOn, boolean customMode) { return aliasWanted(switchOn, customMode); }

    /** Pure: the class name is the look-alike's alias or its page activity (the two names a task of it can be rooted in). */
    public static boolean isLookAlikeComponent(String className) {
        return ALIAS.equals(className) || SsPageActivity.class.getName().equals(className);
    }

    public static boolean hidesRealSettings(Context c, String pkg) {
        if (!REAL_SETTINGS_PACKAGE.equals(pkg)) return false;   // the common case first: the drawer asks this for every app
        return hidesRealSettings(switchOn(c), Mode.isCustom(c), pkg);
    }

    public static boolean pageAllowed(Context c) { return pageAllowed(switchOn(c), Mode.isCustom(c)); }

    // ---------------------------------------------------------------- the level-triggered alias switch

    private static HandlerThread thread;
    private static Handler bg;
    private static volatile Context appCtx;
    private static volatile String whyNext = "";
    private static final Runnable RUN = new Runnable() { @Override public void run() { Context a = appCtx; if (a != null) syncNow(a, whyNext); } };

    private static synchronized Handler bg() {
        if (bg == null) { thread = new HandlerThread("TjSsSync"); thread.start(); bg = new Handler(thread.getLooper()); }
        return bg;
    }

    /** Re-reads the facts a few seconds from now on a background thread (one pending run at most: a burst of calls is one sync). Never throws. */
    public static void syncSoon(Context c, String why) {
        try {
            appCtx = c.getApplicationContext();
            whyNext = why;
            Handler h = bg();
            if (!h.hasCallbacks(RUN)) h.postDelayed(RUN, SYNC_DELAY_MS);
        } catch (Throwable t) { Log.e(TAG, "syncSoon failed", t); }
    }

    /**
     * Puts the alias in the wanted state NOW (blocking: one package-manager call, only when it differs). True = it changed. When it changed the launcher's app list is rebuilt at once (the package manager's own
     * "component changed" broadcast reaches the launcher too, but a forced reload is certain).
     */
    public static synchronized boolean syncNow(Context c, String why) {
        try {
            Context app = c.getApplicationContext();
            boolean want = aliasWanted(switchOn(app), Mode.isCustom(app));
            PackageManager pm = app.getPackageManager();
            ComponentName cn = new ComponentName(app.getPackageName(), ALIAS);
            boolean is = pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;   // DEFAULT = the manifest value = disabled
            if (is == want) { Log.i(TAG, "alias already " + (want ? "on" : "off") + " (" + why + ")"); return false; }
            pm.setComponentEnabledSetting(cn, want ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            String line = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  Settings entry " + (want ? "ON" : "OFF") + " (" + why + ")";
            Log.i(TAG, line);
            Features.setStringAsync(app, STATUS_KEY, line);
            new Handler(Looper.getMainLooper()).post(new Runnable() { @Override public void run() { try { HiddenApps.reloadLauncher(appCtx); } catch (Throwable ignored) { } } });
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "sync failed", t);
            return false;
        }
    }
}
