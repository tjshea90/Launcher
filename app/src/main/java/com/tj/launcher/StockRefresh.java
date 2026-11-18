package com.tj.launcher;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CI1 / SR1 (Tj, 2026-11-18: "the quickstep launcher ram building problem"): gives the stock launcher's memory back after a Quickstep session, by restarting its process through the shell helper, but ONLY when
 * the process has grown big and only where nobody is using it.
 *
 * <p>Why: research/CG1F_QUICKSTEP_AOSP_ANGLE_2026-11-17.md (code read, VERIFIED): the Go {@code IconRecentsView} adds a listener to the process-wide {@code RecentsModel} in its constructor and nothing removes it, so
 * every NEW Activity of the stock launcher stays pinned for the life of the process; research/CG1F_CONFIG_CHANGES_2026-11-17.md: a battery-saver flip (uiMode night) re-creates the visible stock Activity, and the launcher
 * flips the saver around every lock. Tj's own force stops (CD1g / CD1j: 450 -> 280 -> 130 MB, 3-hour average) are the same cure by hand. The process cannot be stopped by an app (it lives at adj 100); the shell helper can.
 *
 * <p>When it acts (all of them, {@link #blocker}): the switch is on (release default ON, debug OFF like every new Tj-facing behaviour), the reading came THROUGH THE HELPER (a number the recorder trusts), the stock
 * process is big ({@link #MIN_PSS_KB} or {@link #MIN_ACTIVITIES} pinned screens), the stored mode is the custom launcher, the custom Launcher is the screen in front (so the stock Recents / home are not being
 * looked at), and the last refresh is more than {@link #COOLDOWN_MS} ago. The usual trigger is the end of a Quickstep session ({@link #afterQuickstepSession}: a reading 45 s after the return); the recorder's own
 * readings (every 25 min while the tablet is in use) catch the rest. A healthy stock launcher is never touched. Without the helper nothing happens at all.
 *
 * <p>What it costs: the next Recents opening or the next Quickstep entry starts the stock process cold (about one second once), SystemUI re-binds its overview service by itself. Never throws.
 */
final class StockRefresh {
    private StockRefresh() {}

    static final String KEY = "stock_refresh";
    static final String LAST_AT_KEY = "sr_last_at";
    static final String STATUS_KEY = "sr_status";
    /** Test levers (the emulator section lowers them; nothing in the app writes them). Both builds read them. */
    static final String MIN_MB_KEY = "sr_min_mb", MIN_ACT_KEY = "sr_min_act", DELAY_KEY = "sr_delay_ms";
    private static final String TAG = "TjStockRefresh";

    /** "Big": the whole process holds this much (a healthy Go launcher is 50-130 MB; Tj's was 280-450). */
    static final int MIN_PSS_KB = 200 * 1024;
    /** ... or this many Activity objects are alive (one launcher + one Recents is normal; each pinned re-creation adds one). */
    static final int MIN_ACTIVITIES = 6;
    static final long COOLDOWN_MS = 6L * 60 * 60 * 1000;
    /** After the return to custom, wait this long (the clean-up of the return is finished and nobody is in Quickstep any more) before the reading that decides. */
    static final long AFTER_RETURN_DELAY_MS = 45_000L;
    /** After the restart, wait this long for the new process before the "after" reading. */
    static final long AFTER_RESTART_MS = 12_000L;

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicBoolean PENDING = new AtomicBoolean(false);

    static boolean defaultOn(boolean debuggable) { return !debuggable; }

    static boolean switchOn(Context c) {
        try { return Features.get(c, KEY, defaultOn(ReturnReset.isDebuggable(c))); } catch (Throwable t) { return false; }
    }

    /** Pure: is the reading big enough to act on? A missing number (-1) counts as "not big" on its own side. */
    static boolean big(int pssKb, int activities, int minPssKb, int minActivities) {
        return (pssKb >= 0 && pssKb >= minPssKb) || (activities >= 0 && activities >= minActivities);
    }

    /**
     * Pure: "" = go, else the reason in plain words (it goes into the status line and the lock log). The order is the order of the questions a reader asks: is the feature on, is there a trustworthy
     * reading, is it big, is it a safe moment (custom mode, the custom home in front), is it not too soon after the last one. {@code lastAtMs} 0 = never; a clock that went backwards allows it.
     */
    static String blocker(boolean switchOn, boolean found, boolean viaHelper, int pssKb, int activities, boolean customMode, boolean customHomeInFront,
                          long lastAtMs, long nowMs, int minPssKb, int minActivities) {
        if (!switchOn) return "switch off";
        if (!found) return "no reading";
        if (!viaHelper) return "reading not taken by the shell helper";
        if (!big(pssKb, activities, minPssKb, minActivities)) return "not big";
        if (!customMode) return "Quickstep is the active launcher";
        if (!customHomeInFront) return "the custom home is not on screen";
        if (lastAtMs > 0 && nowMs >= lastAtMs && nowMs - lastAtMs < COOLDOWN_MS) return "restarted " + ((nowMs - lastAtMs) / 60_000L) + " min ago";
        return "";
    }

    private static int mb(int kb) { return kb < 0 ? -1 : Math.round(kb / 1024f); }

    /** Pure: the one-sentence status of a finished refresh (before and after readings; -1 = unknown). */
    static String resultText(String clock, int beforeKb, int beforeAct, int afterKb, int afterAct, boolean restarted, String error) {
        if (!restarted) return clock + "  wanted to restart the stock launcher (" + fmt(beforeKb, beforeAct) + ") but the helper could not: " + (error == null || error.isEmpty() ? "no answer" : error);
        return clock + "  restarted the stock launcher: before " + fmt(beforeKb, beforeAct) + ", about " + (AFTER_RESTART_MS / 1000) + " s later " + (afterKb < 0 && afterAct < 0 ? "not running yet (it starts again when it is needed)" : fmt(afterKb, afterAct));
    }

    private static String fmt(int kb, int act) { return (kb < 0 ? "? MB" : mb(kb) + " MB") + ", " + (act < 0 ? "?" : String.valueOf(act)) + " Activities"; }

    private static String clock() { return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date()); }

    /** Mode.set hook: a Quickstep session just ended. A reading 45 s later decides ({@link #consider}). One wait at most; nothing when the switch is off. Never throws, never blocks. */
    static void afterQuickstepSession(final Context c) {
        try {
            final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            if (!switchOn(app)) return;
            if (!PENDING.compareAndSet(false, true)) return;
            final long delay = Features.getLong(app, DELAY_KEY, AFTER_RETURN_DELAY_MS);
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        Thread.sleep(Math.max(0L, delay));
                    } catch (Throwable ignored) {
                    } finally { PENDING.set(false); }
                    try { QsMemory.refreshAsync(app, true); } catch (Throwable ignored) { }
                }
            }, "TjStockRefreshWait");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) { PENDING.set(false); }
    }

    /**
     * Called by the memory recorder's worker thread right after it stored a reading. Decides ({@link #blocker}) and, when it says go, restarts the stock launcher through the helper and writes what happened.
     * Runs on that worker (never on the main thread). Never throws.
     */
    static void consider(final Context app, QsMemory.Sample s, boolean viaHelper) {
        try {
            if (s == null) return;
            long now = System.currentTimeMillis();
            int minKb = Features.getInt(app, MIN_MB_KEY, MIN_PSS_KB / 1024) * 1024;
            int minAct = Features.getInt(app, MIN_ACT_KEY, MIN_ACTIVITIES);
            String why = blocker(switchOn(app), s.found, viaHelper, s.pssKb, s.activities, Mode.isCustom(app), TjHooks.launcherInFront(),
                    Features.getLong(app, LAST_AT_KEY, 0L), now, minKb, minAct);
            if (!why.isEmpty()) {
                if (big(s.pssKb, s.activities, minKb, minAct)) Features.setStringAsync(app, STATUS_KEY, clock() + "  the stock launcher is big (" + fmt(s.pssKb, s.activities) + "), left alone: " + why);
                return;
            }
            if (!RUNNING.compareAndSet(false, true)) return;
            try {
                LockLog.add(app, "SR", "restarting the stock launcher (" + fmt(s.pssKb, s.activities) + ", custom home in front)");
                Log.i(TAG, "restarting the stock launcher: " + fmt(s.pssKb, s.activities));
                Features.setLongAsync(app, LAST_AT_KEY, now);   // before the call: a helper that hangs must not be asked again in a loop
                boolean ok = ShellHelper.stockRestart();
                String err = ShellHelper.lastError;
                int afterKb = -1, afterAct = -1;
                if (ok) {
                    try { Thread.sleep(AFTER_RESTART_MS); } catch (InterruptedException ignored) { }
                    String d = ShellHelper.stockMeminfo();
                    QsMemory.Sample a = QsMemory.parse(d == null ? "" : d, System.currentTimeMillis());
                    if (a.found) { afterKb = a.pssKb; afterAct = a.activities; }
                }
                String text = resultText(clock(), s.pssKb, s.activities, afterKb, afterAct, ok, err);
                Features.setStringAsync(app, STATUS_KEY, text);
                LockLog.add(app, "SR", text.length() > 180 ? text.substring(0, 180) : text);
                Log.i(TAG, text);
            } finally { RUNNING.set(false); }
        } catch (Throwable t) { Log.e(TAG, "consider failed", t); }
    }

    /** One sentence for Launcher control and the tablet report. */
    static String describe(Context c) {
        try {
            String st = Features.getString(c, STATUS_KEY, "");
            String sw = switchOn(c) ? "ON" : "OFF";
            return "switch " + sw + "; " + (st.isEmpty() ? "has not had to act yet (it acts only when the stock launcher holds " + (MIN_PSS_KB / 1024) + " MB or " + MIN_ACTIVITIES + " Activities, the custom home is on screen, the shell helper runs, at most every " + (COOLDOWN_MS / 3_600_000L) + " h)" : st);
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
