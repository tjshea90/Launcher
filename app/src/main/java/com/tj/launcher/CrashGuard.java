package com.tj.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Crash-loop breaker: if the launcher process dies of an uncaught exception 3 times within 2 minutes, switch to QUICKSTEP and
 * restore every saved modification, so a bug can never trap Tj in a crash loop. Always hands the exception on to the previous handler.
 *
 * KA1 (v0.7.2): one kind of crash is taken by a FUSE instead: Android crashing us over the keep-alive notification ("Bad notification ... Couldn't create icon"; it crash-looped the real tablet on 2026-11-05 and the
 * breaker then wiped Tj's switches). That first crash only switches keep-alive off ({@link KeepAliveService#blowFuse}) and is NOT counted here; a repeat while the fuse is already blown counts like any crash.
 * CBR1 (v0.7.2): a tripped breaker is cleared by the first start of a NEWER version (a fair try; three new crashes trip it again). The mode is left as it is: the way back to custom stays Tj's choice.
 */
public final class CrashGuard {
    private static final String PREFS = "tj_crash";
    static final int LIMIT = 3;
    static final long WINDOW_MS = 120_000;
    /** CBR1: the versionCode that tripped the breaker (absent = tripped by a version older than this logic). */
    static final String TRIP_VERSION_KEY = "trip_version";

    /** CBR1: shown in Launcher control after an automatic clear (kept in the crash preferences, which "Restore everything" does not touch). */
    static final String CLEARED_NOTE_KEY = "cleared_note";

    private CrashGuard() {}

    /** Pure decision (JVM-testable): given previous crash times (ms) and now, how many are inside the window, including this one. */
    public static int countInWindow(long[] previous, long now) {
        int n = 1;
        for (long t : previous) if (t > 0 && now - t <= WINDOW_MS) n++;
        return n;
    }

    /** Pure (CBR1): clear a tripped breaker at start-up? Only when it is tripped and the installed versionCode is newer than the one that tripped it (0 = tripped before this logic existed). */
    public static boolean shouldAutoClear(boolean tripped, int tripVersion, int installedVersion) {
        return tripped && installedVersion > tripVersion;
    }

    /** Pure (CBR1): the version to remember as "the one that tripped the breaker": the installed one, or "never clear automatically" when it could not be read (0 is reserved for "tripped before this logic existed"). */
    public static int tripVersionToStore(int installed) { return installed > 0 ? installed : Integer.MAX_VALUE; }

    public static void install(final Context app) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try { record(app, e); } catch (Throwable ignored) { }
                if (prev != null) prev.uncaughtException(t, e);
            }
        });
    }

    static void record(Context app, Throwable e) {
        // BN1 (review C #3): a crash that began in a Settings look-alike page is the look-alike's fuse, not the breaker's (only the first one: see SsFuse.fuseHandles).
        if (SsFuse.fuseHandles(e, SsFuse.blocked(app))) { SsFuse.blowFuse(app, e); return; }
        // KA1: the keep-alive notification crash is the fuse's, not the breaker's (only the first one: see KeepAlivePolicy.fuseHandles).
        if (KeepAlivePolicy.fuseHandles(e, KeepAliveService.blocked(app))) { KeepAliveService.blowFuse(app, e); return; }
        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        long[] prev = { p.getLong("t1", 0), p.getLong("t2", 0), p.getLong("t3", 0) };
        int n = countInWindow(prev, now);
        p.edit().putLong("t3", prev[1]).putLong("t2", prev[0]).putLong("t1", now).commit();
        if (n >= LIMIT) {
            // v0.7.6 (review of v0.7.5): mark the breaker tripped FIRST. Mode.set(QUICKSTEP) runs BatterySaverGuard.check, which reads CrashGuard.automationOff: with the flag still false the guard would switch
            // the battery saver ON in the middle of a crash loop, and restoreAll takes no snapshot of it. A Throwable inside Mode.set must not leave the breaker untripped either.
            p.edit().putBoolean("tripped", true).putInt(TRIP_VERSION_KEY, tripVersionToStore(installedVersion(app))).commit();
            Mode.set(app, Mode.QUICKSTEP);
            RestoreStore.restoreAll(app);
        }
    }

    public static boolean wasTripped(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("tripped", false);
    }

    /**
     * AC1 (v0.7.3): "automation" = everything the launcher does BY ITSELF around the lock screen (plain unlock ends a Quickstep session, the custom home is prepared behind the lock, the battery saver
     * guard ...). Those rules no longer depend on switches that a breaker wipe can leave off, so this is what pauses them: the breaker tripped, or Tj pressed "Restore everything", or a newer version
     * cleared the breaker (CBR1: the mode is left alone, so the launcher must not switch it back by itself). Cleared by every DELIBERATE way back to the custom launcher ({@link #clearTripped}).
     */
    static final String PAUSED_KEY = "paused";

    /**
     * Pure: is the launcher's own automation off? The breaker tripped, or the launcher is paused AND in Quickstep. A pause that is still set while the custom launcher is active (code review of v0.7.3: some route
     * to custom that does not call {@link #clearTripped}) is stale and must not silently switch off the pre-arm, the battery saver guard and the rest.
     */
    public static boolean automationOff(boolean tripped, boolean paused, boolean customMode) { return tripped || (paused && !customMode); }

    public static boolean isPaused(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PAUSED_KEY, false);
    }

    /** The one answer the automation rules read (breaker tripped OR paused). */
    public static boolean automationOff(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return automationOff(p.getBoolean("tripped", false), p.getBoolean(PAUSED_KEY, false), Mode.isCustom(c));
    }

    /** "Restore everything": nothing of ours switches launchers for you until you go back to the custom launcher on purpose. */
    public static void pause(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PAUSED_KEY, true).commit();
    }

    /** A deliberate way back to the custom launcher (double Home, a return method, the Control button): the breaker AND the pause are over. */
    public static void clearTripped(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("tripped", false).putBoolean(PAUSED_KEY, false)
                .putLong("t1", 0).putLong("t2", 0).putLong("t3", 0).putInt(TRIP_VERSION_KEY, 0).remove(CLEARED_NOTE_KEY).commit();
        try { LockQuiet.applyNotificationsAsync(c); } catch (Throwable ignored) { }   // v0.7.6 (review of AK1): the pause is over: the lock-screen notification setting follows the switch again
    }

    /** CBR1: called once per process start (TjApplication). Cheap: one preference read unless the breaker is tripped. Never throws. */
    public static void clearIfNewer(Context app) {
        try {
            SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!p.getBoolean("tripped", false)) return;
            int installed = installedVersion(app);
            if (shouldAutoClear(true, p.getInt(TRIP_VERSION_KEY, 0), installed)) {
                clearTripped(app);
                p.edit().putBoolean(PAUSED_KEY, true).commit();   // AC1: the breaker is cleared (a fair try) but the launcher still does not switch the MODE by itself: Tj decides when to go back to custom
                p.edit().putString(CLEARED_NOTE_KEY, "The crash breaker was cleared when version " + installed + " was installed: the new version gets a fair try (three more crashes within two minutes would trip it again). If the launcher is still in Quickstep, switch to the custom launcher above when you want it.").commit();
                android.util.Log.i("TjCrash", "crash breaker cleared at start-up: version " + installed + " is newer than the one that tripped it");
            }
        } catch (Throwable ignored) { }
    }

    /** CBR1: the note to show after an automatic clear, or "". */
    public static String clearedNote(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLEARED_NOTE_KEY, ""); }

    /** The installed versionCode, or 0 if it cannot be read (then nothing is ever auto-cleared). */
    static int installedVersion(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode; }
        catch (Throwable t) { return 0; }
    }
}
