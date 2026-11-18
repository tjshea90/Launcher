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
    /** The alias's label (res/values/ss_strings.xml: ss_label): the same word as the real Settings', so a Recents card of either is titled "Settings". */
    public static final String LABEL = "Settings";
    /** The stock Settings app: hidden from the custom drawer while the look-alike replaces it. */
    public static final String REAL_SETTINGS_PACKAGE = "com.android.settings";
    /** Text preference (allow-listed in the tablet report): what the last alias sync did. */
    public static final String STATUS_KEY = "ss_alias_status";
    /** Delay of the background sync (ms). */
    static final long SYNC_DELAY_MS = 3000;
    private static final String TAG = "TjSettingsSpoof";

    private SettingsSpoof() {}

    /**
     * The default of {@link #KEY}: ON in release builds (the look-alike is finished and gated, v0.9.0), OFF in debug builds like clean_start: the emulator sections that do not test the spoof must not see a second
     * Settings in the drawer (section "ss" turns it on). Tj can flip it in Launcher control; the crash fuse ({@link SsFuse}) writes an explicit OFF.
     */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

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

    /** Its OWN lock: {@link #syncNow} holds the class monitor across package-manager binder calls, and {@link #syncSoon} runs on the unlock path (review C #5, BK1: an unlock must stay fast). */
    private static final Object BG_LOCK = new Object();

    private static Handler bg() {
        synchronized (BG_LOCK) {
            if (bg == null) { thread = new HandlerThread("TjSsSync"); thread.start(); bg = new Handler(thread.getLooper()); }
            return bg;
        }
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
    /** CA1 S2: a forced reload was skipped while Quickstep was on; the next sync in the custom mode does it even when the alias does not change then (the switch went off meanwhile). */
    private static volatile boolean reloadOwed = false;

    private static void postReload(final Context app) { new Handler(Looper.getMainLooper()).post(new Runnable() { @Override public void run() { try { HiddenApps.reloadLauncher(app); } catch (Throwable ignored) { } } }); }

    /** CI1 (G1): how long after an alias change the drawer model is looked at (the system's own package-changed callback reaches the model within tens of ms). */
    static final long MODEL_CHECK_DELAY_MS = 1200;

    /**
     * CI1 (G1), pure: does the drawer model still need a FULL reload after the alias was switched ON? A full reload rebuilds the whole workspace and all-apps list on the main thread (est. 0.3-0.7 s of CPU on the
     * tablet, the home icons blink about 3 s after every return to custom) and is only certain, never necessary, when the model already holds exactly the right rows: the alias flip itself reaches Launcher3
     * incrementally through the package-changed callback ({@code PackageUpdatedTask} OP_UPDATE adds / drops the alias row), and the real Settings row is out of a model that was loaded in the custom mode.
     * Reload when the model is not loaded (a loader is pending / was dropped: today's path), when the alias is NOT wanted (the real Settings must come back: kept as before), when the real Settings row is still
     * in the model (the process loaded it in Quickstep) or when the alias row is missing (the callback has not arrived).
     */
    public static boolean reloadNeeded(boolean modelLoaded, boolean wantAlias, boolean realSettingsInModel, boolean aliasInModel) {
        if (!modelLoaded) return true;
        if (!wantAlias) return true;
        return realSettingsInModel || !aliasInModel;
    }

    /**
     * CI1 (G1): the checked reload. Looks at the model's own list on its worker thread {@link #MODEL_CHECK_DELAY_MS} later and reloads (the old, certain path) only when {@link #reloadNeeded}. Any failure = reload.
     * A line in the log says which way it went (emulator section ss reads it).
     */
    private static void postReloadChecked(final Context app, final boolean want, final String why) {
        if (!want) { postReload(app); return; }   // review LOW 5: the real Settings must come back at once (the old path); only the alias-ON flip has a model that may already be right
        try {
            final com.android.launcher3.LauncherModel model = com.android.launcher3.LauncherAppState.getInstance(app).getModel();
            bg().postDelayed(new Runnable() {
                @Override public void run() {
                    try {
                        model.enqueueModelUpdateTask(new com.android.launcher3.LauncherModel.ModelUpdateTask() {
                            private com.android.launcher3.LauncherModel m;
                            private com.android.launcher3.AllAppsList list;
                            @Override public void init(com.android.launcher3.LauncherAppState a, com.android.launcher3.LauncherModel mm, com.android.launcher3.model.BgDataModel d,
                                                       com.android.launcher3.AllAppsList l, java.util.concurrent.Executor ui) { m = mm; list = l; }
                            @Override public void run() {
                                boolean needed = true;
                                String why2 = "failed";
                                if (!Mode.isCustom(app)) { reloadOwed = true; Log.i(TAG, "G1 check dropped: Quickstep came on meanwhile (the next custom sync reloads if it still needs to; " + why + ")"); return; }   // review LOW 5: no full reload while Quickstep is on (CA1 S2)
                                try {
                                    boolean loaded = m.isModelLoaded();
                                    boolean real = false, alias = false;
                                    if (loaded) {
                                        for (int i = list.data.size() - 1; i >= 0; i--) {
                                            android.content.ComponentName cn = list.data.get(i).componentName;
                                            if (cn == null) continue;
                                            if (REAL_SETTINGS_PACKAGE.equals(cn.getPackageName())) real = true;
                                            if (ALIAS.equals(cn.getClassName())) alias = true;
                                        }
                                    }
                                    needed = reloadNeeded(loaded, want, real, alias);
                                    why2 = "loaded=" + loaded + " real=" + real + " alias=" + alias;
                                } catch (Throwable t) { needed = true; }
                                Log.i(TAG, "G1 drawer model " + (needed ? "needs the full reload" : "already right: NO full reload") + " (" + why2 + "; " + why + ")");
                                if (needed) postReload(app);
                            }
                        });
                    } catch (Throwable t) { Log.e(TAG, "G1 check failed, full reload", t); postReload(app); }
                }
            }, MODEL_CHECK_DELAY_MS);
        } catch (Throwable t) { Log.e(TAG, "G1 check not possible, full reload", t); postReload(app); }
    }

    public static synchronized boolean syncNow(Context c, String why) {
        try {
            Context app = c.getApplicationContext();
            boolean want = aliasWanted(switchOn(app), Mode.isCustom(app));
            PackageManager pm = app.getPackageManager();
            ComponentName cn = new ComponentName(app.getPackageName(), ALIAS);
            boolean is = pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;   // DEFAULT = the manifest value = disabled
            if (is == want) {
                Log.i(TAG, "alias already " + (want ? "on" : "off") + " (" + why + ")");
                if (reloadOwed && Mode.isCustom(app)) { reloadOwed = false; postReloadChecked(app, want, why); Log.i(TAG, "the reload skipped in Quickstep is looked at now (" + why + ")"); }   // CA1 S2 review MED1: the switch went OFF during Quickstep (no alias change at the return), the drawer still needs the real Settings back
                return false;
            }
            pm.setComponentEnabledSetting(cn, want ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            String line = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  Settings entry " + (want ? "ON" : "OFF") + " (" + why + ")";
            Log.i(TAG, line);
            Features.setStringAsync(app, STATUS_KEY, line);
            if (want || Mode.isCustom(app)) { reloadOwed = false; postReloadChecked(app, want, why); }   // CI1 (G1): only when the model is not already right
            else { reloadOwed = true; Log.i(TAG, "no forced reload: Quickstep (the custom drawer is not on screen; the system's component-changed update drops the entry, the return to custom re-enables the alias and reloads)"); }   // CA1 S2: a full Launcher3 load + bind (est. 0.5-1.5 s of CPU on the tablet) ran at every switch to Quickstep, while Quickstep itself was starting; reloadOwed pays it at the next custom sync if the alias then does not change
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "sync failed", t);
            return false;
        }
    }
}
