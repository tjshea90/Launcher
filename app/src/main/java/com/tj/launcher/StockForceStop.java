package com.tj.launcher;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CT1 / SR2 (Tj, 2026-11-21: "There is already a fix to force stop quickstep, but it requires the adb helper which i don't have"): the same cure as SR1 ({@link StockRefresh}), WITHOUT the shell helper.
 * The accessibility service opens Settings' App info page of the stock launcher behind a cover, presses "Force stop" and the confirm "OK" (exactly what Tj does by hand), and goes back to the custom home.
 *
 * <p>Why: research/CG1F_QUICKSTEP_AOSP_ANGLE_2026-11-17.md (verified in the code): the Go {@code IconRecentsView} pins every NEW Activity of the stock launcher for the life of its process;
 * research/CG1F_CONFIG_CHANGES_2026-11-17.md: each battery-saver flip (uiMode night) re-creates the visible stock Activity, and the launcher flips the saver around every lock. An app cannot kill that
 * process (adj 100) and, without the helper, cannot even READ its size (an app uid is refused {@code dumpsys meminfo}). So the trigger here is a DEBT counter, an estimate: every change of
 * {@code low_power} and every ended Quickstep session adds one; at {@link #MIN_DEBT} the next quiet moment on the custom home restarts the process. The helper path (SR1) keeps its exact reading and wins when the helper runs.
 *
 * <p>No Android here except the thin preference glue (tested in StockForceStopTest). The step-by-step drive lives in TjAccessibilityService ({@code stockForceStop}) and follows {@link #step}.
 */
final class StockForceStop {
    private StockForceStop() {}

    static final String KEY = "stock_force_stop";
    static final String DEBT_KEY = "sf_debt", LAST_AT_KEY = "sf_last_at", FAILS_KEY = "sf_fails", STATUS_KEY = "sf_status";
    /** Test levers (the emulator section lowers them; nothing in the app writes them). Both builds read them. */
    static final String MIN_DEBT_KEY = "sf_min_debt", DELAY_KEY = "sf_delay_ms", COOLDOWN_MIN_KEY = "sf_cooldown_min";
    static final String STOCK_PKG = "com.android.launcher3";
    static final String SETTINGS_PKG = "com.android.settings";
    private static final String TAG = "TjStockForceStop";

    /** Debt that justifies a restart: one lock + gesture unlock with the saver on is two flips, so about six such cycles (or twelve Quickstep sessions). */
    static final int MIN_DEBT = 12;
    static final long COOLDOWN_MS = 4L * 60 * 60 * 1000;
    /** A drive that failed is tried again after this, not after the full cooldown (still never in a loop, see {@link #MAX_FAILS}). */
    static final long RETRY_AFTER_FAIL_MS = 30L * 60 * 1000;
    static final int MAX_FAILS = 2;
    /** After the return to custom wait this long (the clean-up of the return is done, nobody is in Quickstep) before asking whether to act. */
    static final long AFTER_RETURN_DELAY_MS = 45_000L;
    /** The whole drive (from the cover going up) is over by then; the cover's failsafe is re-armed to {@link #COVER_MS}. */
    static final long CAP_MS = 14_000L;
    static final long LEAVE_STEP_MS = 1_000L;   // more than HomeDoublePress's 900 ms: two HOME presses of one leave are never one double Home
    static final int LEAVE_TRIES = 4;
    /** The cover outlives the whole run: the cap, every leave press, and a margin (review: a shorter failsafe showed Settings while BACK / HOME were still being pressed). */
    static final long COVER_MS = CAP_MS + LEAVE_TRIES * LEAVE_STEP_MS + 2_000L;
    /** Not before this long without a touch on the home screen (a tap that lands on an invisible App info page would be Tj's wrong tap, not ours). */
    static final long TOUCH_QUIET_MS = 2_500L;
    static final long TICK_MS = 120L;
    static final long START_DELAY_MS = 600L;
    static final long RECHECK_MS = 400L;
    static final long START_WAIT_MAX_MS = 12_000L;
    /** The App info page: how long for the page, for a Force stop button that is still greyed (it is enabled by an answer to a broadcast), for the dialog, for the dialog to go. */
    static final long PAGE_WAIT_MS = 7_000L, GREY_WAIT_MS = 2_500L, DIALOG_WAIT_MS = 4_000L, GONE_WAIT_MS = 4_000L, RETRY_CLICK_MS = 1_500L;
    /** Not again within this time of the last drive's end, whatever the cooldown says. */
    static final long MIN_GAP_MS = 60_000L;

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final AtomicBoolean PENDING = new AtomicBoolean(false);
    private static int lastLow = Integer.MIN_VALUE;

    static boolean defaultOn(boolean debuggable) { return !debuggable; }

    static boolean switchOn(Context c) {
        try { return Features.get(c, KEY, defaultOn(ReturnReset.isDebuggable(c))); } catch (Throwable t) { return false; }
    }

    // ------------------------------------------------------------------------------------------------ the debt

    /** Pure: the debt after one more event (never negative, capped so a flood cannot overflow). */
    static int addDebt(int debt, int n) { long v = (long) Math.max(0, debt) + Math.max(0, n); return (int) Math.min(v, 100_000L); }

    /** Pure: does this reading of {@code low_power} count as a flip? Only a CHANGE of the value does (a ContentObserver can report one write twice). {@code prev} MIN_VALUE = first reading, never counted. */
    static boolean isFlip(int prev, int now) { return prev != Integer.MIN_VALUE && now != prev; }

    /** SR1 (the helper path) or a finished drive took the debt away. Under the same lock as the additions, so a flip cannot be overwritten. */
    static synchronized void setDebt(Context app, int v) { try { Features.setIntAsync(app, DEBT_KEY, v); } catch (Throwable ignored) { } }

    /** The first reading of low_power in this process (BatterySaverGuard.start): a Go tablet kills the process often, and the first flip after each start must still count. */
    static void seedLowPower(int now) { synchronized (StockForceStop.class) { if (lastLow == Integer.MIN_VALUE) lastLow = now; } }

    private static synchronized void addDebt(Context app, int n) {
        try { Features.setIntAsync(app, DEBT_KEY, addDebt(Features.getInt(app, DEBT_KEY, 0), n)); } catch (Throwable ignored) { }
    }

    /** BatterySaverGuard's low_power observer: one more change of the saver (ours or the person's: each is a uiMode flip for the stock launcher). Cheap; never throws; nothing when the switch is off. */
    static void noteLowPower(Context c, int now) {
        try {
            final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            boolean flip;
            synchronized (StockForceStop.class) { flip = isFlip(lastLow, now); lastLow = now; }
            if (flip && switchOn(app)) addDebt(app, 1);
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------------------------------------ whether to start

    /**
     * Pure: "" = go, else the reason in plain words (status line, lock log). Order = the questions a reader asks: switch, gave-up, helper (SR1 does it better), debt, the safe moment, the cooldown.
     * {@code lastAtMs} 0 = never; a clock that went backwards allows it.
     */
    static String blocker(boolean switchOn, int fails, boolean helperRunning, int debt, int minDebt, boolean customMode, boolean customHomeInFront, boolean screenOnUnlocked,
                          boolean automationOff, long lastAtMs, long nowMs, long cooldownMs) {
        if (!switchOn) return "switch off";
        if (fails >= MAX_FAILS) return "gave up after " + fails + " failed tries (switch it off and on to try again)";
        if (helperRunning) return "the shell helper runs: its exact reading decides";
        if (debt < minDebt) return "not enough has piled up yet (" + debt + " of " + minDebt + ")";
        String safe = safeMoment(customMode, customHomeInFront, screenOnUnlocked, automationOff);
        if (!safe.isEmpty()) return safe;
        if (lastAtMs > 0 && nowMs >= lastAtMs && nowMs - lastAtMs < cooldownMs) return "restarted " + ((nowMs - lastAtMs) / 60_000L) + " min ago";
        return "";
    }

    /** Pure: the start decision of a waiting drive: "" = go now, "WAIT:..." = look again, "STOP:..." = give this run up (nothing was opened). {@code stop} = a permanent reason, {@code wait} = a transient one. */
    static String startVerdict(String stop, String wait, long waitedMs) {
        if (stop != null && !stop.isEmpty()) return "STOP:" + stop;
        if (wait == null || wait.isEmpty()) return "";
        if (waitedMs >= START_WAIT_MAX_MS) return "STOP:gave up after " + (waitedMs / 1000) + " s: " + wait;
        return "WAIT:" + wait;
    }

    /** Pure: the part every start shares (also the button): the custom launcher is the screen in front and nothing else is going on. */
    static String safeMoment(boolean customMode, boolean customHomeInFront, boolean screenOnUnlocked, boolean automationOff) {
        if (automationOff) return "automation is off (crash guard or pause)";
        if (!customMode) return "Quickstep is the active launcher";
        if (!screenOnUnlocked) return "the screen is off or locked";
        if (!customHomeInFront) return "the custom home is not on screen";
        return "";
    }

    /** Pure: the time stored as "last restart". A drive that worked starts the full cooldown; one that did not is tried again after {@link #RETRY_AFTER_FAIL_MS}. */
    static long lastAtAfter(boolean worked, long nowMs, long cooldownMs) { return worked ? nowMs : nowMs - cooldownMs + RETRY_AFTER_FAIL_MS; }

    /** Pure: the status line must keep the result of the last drive while the cooldown is what blocks the next one. */
    static boolean keepsLastResult(String blockerText) { return blockerText != null && (blockerText.startsWith("restarted ") || blockerText.startsWith("not enough")); }

    // ------------------------------------------------------------------------------------------------ the drive (pure: what to do at each look)

    /** Phases of a running drive. */
    static final int IDLE = 0, WAITING = 1, OPENING = 2, PRESSING = 3, CONFIRMING = 4, LEAVING = 5;
    /** What the service does now. */
    static final int ACT_NONE = 0, ACT_CLICK_FORCE = 1, ACT_CLICK_OK = 2, ACT_BACK = 3, ACT_HOME = 4, ACT_END = 5;
    /** How the drive ended. */
    static final int OUT_NONE = 0, OUT_RESTARTED = 1, OUT_NOT_RUNNING = 2, OUT_FAILED = 3, OUT_ABORTED = 4;
    /** The Force stop button as the last look found it. */
    static final int BTN_NONE = 0, BTN_GREY = 1, BTN_ENABLED = 2;

    /** What the service saw this tick. */
    static final class Obs {
        boolean safe;            // plain, custom mode, automation on
        boolean plain;           // screen on, unlocked, no PIN pad (BACK / HOME may be pressed)
        boolean settingsFront;   // the active window is com.android.settings
        int button = BTN_NONE;   // the Force stop button of the App info page
        boolean confirmUp;       // the "Force stop?" dialog with its OK button
        boolean homeFront;       // the custom launcher is the screen in front
    }

    static final class Step {
        final int phase, action, outcome; final String why;
        Step(int phase, int action, int outcome, String why) { this.phase = phase; this.action = action; this.outcome = outcome; this.why = why; }
    }

    /**
     * One look of a running drive. phase / phaseAgeMs: where it is and for how long; runMs: since the cover went up; pageAgeMs: how long the App info page has shown its Force stop button (-1 = not yet:
     * a cold-starting Settings builds the button greyed, so "grey" counts from the first look at the page, not from the start of the intent); clicks: Force stop presses so far; leaveTries: BACK / HOME
     * presses so far; pending: the outcome decided in an earlier phase (the LEAVING phase carries it to the end).
     */
    static Step step(int phase, long phaseAgeMs, long runMs, long pageAgeMs, int clicks, int leaveTries, int pending, Obs o) {
        if (phase == LEAVING) {
            if (!o.plain) return new Step(IDLE, ACT_END, pending, "the screen is not plain: nothing is pressed, the rest is left to the system");   // never BACK / HOME on a lock screen
            if (o.homeFront) return new Step(IDLE, ACT_END, pending, "back on the custom home");
            if (leaveTries >= LEAVE_TRIES) return new Step(IDLE, ACT_END, keepsOutcomeWhenStuck(pending) ? pending : OUT_FAILED, "could not get back to the custom home after " + leaveTries + " presses");
            if (phaseAgeMs >= LEAVE_STEP_MS || leaveTries == 0) return new Step(LEAVING, leaveTries < 2 ? ACT_BACK : ACT_HOME, pending, "going back");
            return new Step(LEAVING, ACT_NONE, pending, "");
        }
        if (phase != OPENING && phase != PRESSING && phase != CONFIRMING) return new Step(IDLE, ACT_END, OUT_NONE, "not running");
        if (!o.safe) return new Step(LEAVING, ACT_NONE, OUT_ABORTED, "the screen is not plain any more (locked, off or another launcher): the run stops");
        if (runMs >= CAP_MS) return new Step(LEAVING, ACT_NONE, OUT_FAILED, "took longer than " + (CAP_MS / 1000) + " s");
        switch (phase) {
            case OPENING:
                if (o.settingsFront && o.button == BTN_ENABLED) return new Step(PRESSING, ACT_CLICK_FORCE, OUT_NONE, "Force stop pressed");
                if (o.settingsFront && o.button == BTN_GREY && pageAgeMs >= GREY_WAIT_MS) return new Step(LEAVING, ACT_NONE, OUT_NOT_RUNNING, "the Force stop button stays grey: the stock launcher is not running");
                if (phaseAgeMs >= PAGE_WAIT_MS) return new Step(LEAVING, ACT_NONE, OUT_FAILED, o.settingsFront ? "the App info page shows no Force stop button" : "the App info page did not come up");
                return new Step(OPENING, ACT_NONE, OUT_NONE, "");
            case PRESSING:
                if (o.confirmUp) return new Step(CONFIRMING, ACT_CLICK_OK, OUT_NONE, "OK pressed");
                if (clicks < 2 && o.button == BTN_ENABLED && phaseAgeMs >= RETRY_CLICK_MS) return new Step(PRESSING, ACT_CLICK_FORCE, OUT_NONE, "Force stop pressed again");
                if (phaseAgeMs >= DIALOG_WAIT_MS) return new Step(LEAVING, ACT_NONE, OUT_FAILED, "no confirm dialog came after Force stop");
                return new Step(PRESSING, ACT_NONE, OUT_NONE, "");
            default: // CONFIRMING
                if (!o.confirmUp && (o.settingsFront || phaseAgeMs >= 1_500L)) return new Step(LEAVING, ACT_NONE, OUT_RESTARTED, "the dialog closed after OK");   // a moment with no Settings window at all (a transition) is not yet "closed"
                if (phaseAgeMs >= GONE_WAIT_MS) return new Step(LEAVING, ACT_NONE, OUT_FAILED, "the dialog did not close after OK");
                return new Step(CONFIRMING, ACT_NONE, OUT_NONE, "");
        }
    }

    /** Pure: a decided outcome that stays what it was when the way back to the home is stuck (a stuck way home is no reason to call a verified restart, a verified "not running" or a safety stop a failure). */
    static boolean keepsOutcomeWhenStuck(int outcome) { return outcome == OUT_RESTARTED || outcome == OUT_NOT_RUNNING || outcome == OUT_ABORTED; }

    /** What a finished drive writes (pure, tested). {@code debt} -1 = leave it; {@code fails}: -1 = leave it, else the new count; {@code lastAt}: the new "last drive" time. */
    static final class After {
        final int debt, fails; final long lastAt;
        After(int debt, int fails, long lastAt) { this.debt = debt; this.fails = fails; this.lastAt = lastAt; }
    }

    static After afterOutcome(int outcome, int failsNow, long nowMs, long cooldownMs) {
        if (clearsDebt(outcome)) return new After(0, 0, nowMs);                                                   // done: nothing piled up, the full cooldown starts
        if (outcome == OUT_FAILED) return new After(-1, failsNow + 1, lastAtAfter(false, nowMs, cooldownMs));    // failed: debt kept, one more failure, retry later
        return new After(-1, -1, lastAtAfter(false, nowMs, cooldownMs));                                          // aborted / nothing: debt kept, no failure counted, retry later
    }

    // ------------------------------------------------------------------------------------------------ text

    private static String clock() { return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date()); }

    /** Pure: the one-sentence status of a finished drive. */
    static String resultText(String clock, int outcome, String why, int debtBefore, long ms, String via) {
        String base = clock + "  " + (via == null || via.isEmpty() ? "" : "(" + via + ") ");
        switch (outcome) {
            case OUT_RESTARTED: return base + "restarted the stock launcher through Settings in " + ms + " ms (debt was " + debtBefore + ")";
            case OUT_NOT_RUNNING: return base + "the stock launcher was not running: nothing to restart (debt was " + debtBefore + ")";
            case OUT_ABORTED: return base + "stopped, nothing restarted: " + why;
            default: return base + "could not restart the stock launcher through Settings: " + why;
        }
    }

    /** Pure: did the drive take the debt away? A restart or a stock launcher that was not running both leave nothing to give back; a failure keeps the debt. */
    static boolean clearsDebt(int outcome) { return outcome == OUT_RESTARTED || outcome == OUT_NOT_RUNNING; }

    // ------------------------------------------------------------------------------------------------ glue

    /** Mode.set hook: a Quickstep session just ended (one more piece of debt); 45 s later the custom home is quiet and {@link #consider} decides. One wait at most; nothing when the switch is off. Never throws, never blocks. */
    static void afterQuickstepSession(final Context c) {
        try {
            final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            if (!switchOn(app)) return;
            addDebt(app, 1);
            if (!PENDING.compareAndSet(false, true)) return;
            final long delay = Features.getInt(app, DELAY_KEY, (int) AFTER_RETURN_DELAY_MS);
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        Thread.sleep(Math.max(0L, delay));
                    } catch (Throwable ignored) {
                    } finally { PENDING.set(false); }
                    try { consider(app, "after a Quickstep session"); } catch (Throwable ignored) { }
                }
            }, "TjStockForceStopWait");
            t.setDaemon(true);
            t.start();
        } catch (Throwable t) { PENDING.set(false); }
    }

    /** Decides ({@link #blocker}) and, on go, asks the accessibility service to drive Settings. Off the main thread. The drive itself ends in {@link #finished}. */
    static void consider(Context app, String why) {
        try {
            long now = System.currentTimeMillis();
            int minDebt = Features.getInt(app, MIN_DEBT_KEY, MIN_DEBT);
            long cooldown = Features.getInt(app, COOLDOWN_MIN_KEY, (int) (COOLDOWN_MS / 60_000L)) * 60_000L;
            int debt = Features.getInt(app, DEBT_KEY, 0);
            TjAccessibilityService svc = TjAccessibilityService.get();
            String block = svc == null ? "the accessibility service is not running" : blocker(switchOn(app), Features.getInt(app, FAILS_KEY, 0), ShellHelper.running(), debt, minDebt,
                    Mode.isCustom(app), TjHooks.launcherInFront(), screenOnUnlocked(app), CrashGuard.automationOff(app), Features.getLong(app, LAST_AT_KEY, 0L), now, cooldown);
            if (!block.isEmpty()) {
                if (debt >= minDebt && !keepsLastResult(block)) Features.setStringAsync(app, STATUS_KEY, clock() + "  the stock launcher is due (debt " + debt + "), left alone: " + block);
                return;
            }
            start(app, svc, why, false);
        } catch (Throwable t) { Log.e(TAG, "consider failed", t); }
    }

    /** The button in Launcher control: restart now, with no debt or cooldown rule (the quiet-moment rules still hold). Returns "" when the drive was asked for, else the reason. */
    static String fromButton(Context app) {
        try {
            if (!switchOn(app)) return "the switch is off";
            TjAccessibilityService svc = TjAccessibilityService.get();
            if (svc == null) return "the accessibility service is not running";
            String block = safeMoment(Mode.isCustom(app), true, screenOnUnlocked(app), CrashGuard.automationOff(app));
            if (!block.isEmpty()) return block;
            start(app, svc, "button", true);
            return "";
        } catch (Throwable t) { return "failed: " + t; }
    }

    private static void start(Context app, TjAccessibilityService svc, String why, boolean manual) {
        if (!RUNNING.compareAndSet(false, true)) return;
        long now = System.currentTimeMillis();
        long cooldown = Features.getInt(app, COOLDOWN_MIN_KEY, (int) (COOLDOWN_MS / 60_000L)) * 60_000L;
        LockLog.add(app, "SF", "restarting the stock launcher through Settings (" + why + ", debt " + Features.getInt(app, DEBT_KEY, 0) + ")");
        Features.setLongAsync(app, LAST_AT_KEY, now);   // before the drive: a drive that hangs must not be asked again in a loop
        if (!svc.stockForceStop(why, manual, Features.getInt(app, DEBT_KEY, 0), cooldown)) { RUNNING.set(false); Features.setLongAsync(app, LAST_AT_KEY, lastAtAfter(false, now, cooldown)); }
    }

    /** The service reports the end of a drive (any thread). Stores the result; a failure counts toward {@link #MAX_FAILS}. */
    static void finished(Context app, int outcome, String why, int debtBefore, long ms, long cooldownMs, String via) {
        try {
            long now = System.currentTimeMillis();
            After af = afterOutcome(outcome, Features.getInt(app, FAILS_KEY, 0), now, cooldownMs);
            if (af.debt >= 0) setDebt(app, af.debt);
            if (af.fails >= 0) Features.setIntAsync(app, FAILS_KEY, af.fails);
            Features.setLongAsync(app, LAST_AT_KEY, af.lastAt);
            String text = resultText(clock(), outcome, why, debtBefore, ms, via);
            Features.setStringAsync(app, STATUS_KEY, text);
            LockLog.add(app, "SF", text.length() > 190 ? text.substring(0, 190) : text);
            Log.i(TAG, text);
        } catch (Throwable t) { Log.e(TAG, "finished failed", t); }
        finally { RUNNING.set(false); }
    }

    /** The switch was turned ON in Launcher control: a fresh start (the "gave up" state is cleared). */
    static void reset(Context app) {
        try { Features.setIntAsync(app, FAILS_KEY, 0); Features.setStringAsync(app, STATUS_KEY, ""); } catch (Throwable ignored) { }
    }

    private static boolean screenOnUnlocked(Context c) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
            android.app.KeyguardManager km = (android.app.KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
            return pm != null && pm.isInteractive() && km != null && !km.isKeyguardLocked();
        } catch (Throwable t) { return false; }
    }

    /** One sentence for Launcher control and the tablet report. */
    static String describe(Context c) {
        try {
            String st = Features.getString(c, STATUS_KEY, "");
            return "switch " + (switchOn(c) ? "ON" : "OFF") + "; debt " + Features.getInt(c, DEBT_KEY, 0) + " of " + MIN_DEBT + "; "
                    + (st.isEmpty() ? "has not had to act yet (it acts only when the debt reaches " + MIN_DEBT + ", the custom home is quiet on screen, the shell helper is not running, at most every " + (COOLDOWN_MS / 3_600_000L) + " h)" : st);
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
