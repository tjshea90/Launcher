package com.tj.launcher;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherSettings;

/**
 * CV1: which profile our Launcher3 model holds (custom = today's locked-down home, unlocked = what Quickstep was) and the level-triggered rule that keeps it equal to the wanted one
 * ({@link Unified#wantedProfile}, pure rules in {@link UnifiedRules}). The profile is the pair "layout database + app filter":
 * <ul><li>the layout database is chosen by {@link #onLoadStart}, the FIRST step of every load, on the model thread;</li>
 * <li>the app filter ({@link HiddenApps#isShown}) follows the live mode, so it is right at once and the reload below only has to bring the DB and the lists in line.</li></ul>
 * Triggers of {@link #syncNow}: Mode.set, Launcher resume, switch change. A reload that is not needed is never started. Never throws.
 */
public final class HomeProfile {
    private static final String TAG = "TjProfile";
    private static final long MIN_SPACING_MS = 1500;

    /** What the model's layout database is right now (null: nothing loaded in this process yet). */
    private static volatile UnifiedRules.Profile loaded;
    private static volatile long lastReloadAt;
    private static volatile String lastWhy = "";
    private static volatile boolean again;
    /** The last switch the provider could not make (disk full, database error): which profile and when (review CV1-1: never reload for ever, never hold a cover for it). */
    private static volatile UnifiedRules.Profile switchFailedFor;
    private static volatile long switchFailedAt;

    private HomeProfile() {}

    public static UnifiedRules.Profile loaded() { return loaded; }

    /** For tests of the glue-free rules and the report. */
    static void resetForTest() { loaded = null; lastReloadAt = 0; lastWhy = ""; again = false; switchFailedFor = null; switchFailedAt = 0; }

    /**
     * LoaderTask, first step of every load, on the model worker thread (the thread of every layout write): select the wanted profile's layout database in the provider. On any failure the provider
     * keeps its previous database and {@link #loaded} keeps its previous value, so the next trigger tries again.
     */
    public static void onLoadStart(Context c) {
        UnifiedRules.Profile want = null;
        try {
            Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            want = Unified.wantedProfile(app);
            Bundle r = LauncherSettings.Settings.call(app.getContentResolver(), LauncherSettings.Settings.METHOD_SET_PROFILE_DB, UnifiedRules.dbName(want));
            String active = r == null ? null : r.getString(LauncherSettings.Settings.EXTRA_VALUE);
            if (active == null) throw new IllegalStateException("the provider gave no answer");
            UnifiedRules.Profile now = UnifiedRules.profileOfDb(active);
            UnifiedRules.Profile before = loaded;
            loaded = now;
            if (now == want) switchFailedFor = null;
            if (before != now) LockLog.add(app, "PROFILE", "model loads the " + now + " layout (" + active + "), wanted " + want);
        } catch (Throwable t) {
            Log.e(TAG, "selecting the layout database failed", t);
            // The provider keeps the database it had: in a process that has loaded nothing yet that is the custom one. Say so, otherwise "nothing loaded" never counts as a mismatch and the model could never be fixed (review CV1-9a).
            if (loaded == null) loaded = UnifiedRules.Profile.CUSTOM;
            switchFailedFor = want;
            switchFailedAt = System.currentTimeMillis();
            try { LockLog.add(c.getApplicationContext() != null ? c.getApplicationContext() : c, "PROFILE", "switching to the " + want + " layout FAILED (" + t + "); keeps " + loaded + ", next try in " + UnifiedRules.SWITCH_RETRY_MS / 1000 + " s"); } catch (Throwable ignored) { }
        }
    }

    /**
     * True once the unlocked layout database exists: pinned shortcuts (they belong to the launcher package, not to one layout) are then shared by both profiles, so a load of one profile must never unpin a shortcut
     * that only the other one lists (review CV1-3). Cheap: one file check. With no unlocked database the old behaviour is unchanged.
     */
    public static boolean sharedShortcutPins(Context c) {
        try { return c.getDatabasePath(UnifiedRules.DB_UNLOCKED).exists(); } catch (Throwable t) { return false; }
    }

    /**
     * Review CV1-1: the install queue (icons for new apps) is flushed into whichever layout the model HOLDS, which can differ from the live mode for a moment. With the unlocked layout present, an entry for an app the
     * CUSTOM drawer hides is dropped at the flush when the model holds the custom profile. Without an unlocked layout nothing changes (the base behaviour).
     */
    public static boolean queuedInstallAllowed(Context c, String pkg) {
        try {
            if (pkg == null || pkg.isEmpty() || !sharedShortcutPins(c)) return true;
            if (loaded == UnifiedRules.Profile.UNLOCKED) return true;
            return HiddenApps.isPackageShownInCustom(c, pkg);
        } catch (Throwable t) { return true; }
    }

    /** Level-triggered: reload the model when it holds another profile than the wanted one. Cheap when nothing is to do (two memory reads and the mode preference). */
    public static void syncNow(Context c, String why) {
        try {
            Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            UnifiedRules.Profile want = Unified.wantedProfile(app);
            UnifiedRules.Profile have = loaded;
            if (!UnifiedRules.reloadNeeded(want, have)) return;
            long now = System.currentTimeMillis();
            if (UnifiedRules.holdOff(want, switchFailedFor, switchFailedAt, now)) return;   // the provider could not switch to it a moment ago: not again at once
            if (now - lastReloadAt < MIN_SPACING_MS) {   // a reload was just started: look again right after it (a quick second flip must never be dropped)
                if (!again) {
                    again = true;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() { @Override public void run() { again = false; syncNow(app, why + " (again)"); } },
                            MIN_SPACING_MS - (now - lastReloadAt) + 60);
                }
                return;
            }
            lastReloadAt = now;
            lastWhy = why;
            LockLog.add(app, "PROFILE", "reload " + have + " -> " + want + " (" + why + ")");
            HiddenApps.reloadLauncher(app);
            // Review CV1-2: a flip on a VISIBLE home (double Home, tile, Back to custom, volume code, Control) is bridged by a cover until the model holds the wanted profile, so the person never sees the other
            // profile's layout or app list. Not on the lock screen (nothing visible; the lock-time return has its own cover rules).
            try { TjAccessibilityService svc = TjAccessibilityService.get(); if (svc != null) svc.profileCover("flip " + have + " -> " + want); } catch (Throwable ignored) { }
        } catch (Throwable t) {
            Log.e(TAG, "sync failed", t);
        }
    }

    /** True when the model holds the wanted profile and its load has finished: the cover may go, the unlock may proceed. */
    public static boolean ready(Context c) {
        try {
            LauncherAppState st = LauncherAppState.getInstanceNoCreate();
            boolean modelLoaded = st != null && st.getModel().isModelLoaded();
            UnifiedRules.Profile want = Unified.wantedProfile(c);
            if (UnifiedRules.holdOff(want, switchFailedFor, switchFailedAt, System.currentTimeMillis())) return true;   // the switch is failing: never hold a cover for it
            return UnifiedRules.ready(want, loaded, modelLoaded);
        } catch (Throwable t) {
            return true;   // never hold a cover on a failing check
        }
    }

    /** One line for the tablet report and the lock log. */
    public static String status(Context c) {
        try {
            return "switch " + (Unified.switchOn(c) ? "on" : "off") + ", breaker " + (CrashGuard.wasTripped(c) ? "TRIPPED (stock home is the Quickstep home)" : "ok")
                    + ", wanted " + Unified.wantedProfile(c) + ", model holds " + (loaded == null ? "nothing yet" : loaded.toString()) + ", ready " + ready(c)
                    + (lastWhy.isEmpty() ? "" : ", last reload by " + lastWhy);
        } catch (Throwable t) { return "unreadable"; }
    }
}
