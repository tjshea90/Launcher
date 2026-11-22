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

    /** Debt that justifies a restart: one lock + gesture unlock with the saver on is two flips, so about six such cycles (or twelve Quickstep sessions). The v0.9.16 world, kept for the emergency exit (see {@link #minDebtFor}). */
    static final int MIN_DEBT = 12;
    /**
     * CW1 D3 (research/CW1_AUDIT_D_STOCK_SLEEP_2026-11-22.md): with the one launcher in force the debt counts PINS only, one per stock window that came up under a different configuration than the one before it
     * (each such window is a new Activity the Go Recents view pins for the life of the process). Four to six is a real leak; five.
     */
    static final int PIN_THRESHOLD = 5;
    /** CW1 D5: set once (the first time the one launcher is in force with this switch on): the seeded cleanup of what the v0.9.16 era left in the stock process. */
    static final String SEED_KEY = "sf_unified_seed";
    /** CW1 D3: the configuration signature of the last stock window seen since the stock process was last restarted; EMPTY = no stock window since then (nothing can have been pinned). */
    static final String SIG_KEY = "sf_sig";
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

    // ---- CW1 D3: what counts as debt depends on the world. Emergency exit (the stock launcher is the Quickstep home again) = the v0.9.16 weights; the one launcher in force = pins only.

    /** Pure: debt of one ended Quickstep session. In the one-launcher world a session creates nothing in the stock process (its HOME activity is not started any more), so it weighs nothing. */
    static int sessionWeight(boolean unifiedActive) { return unifiedActive ? 0 : 1; }

    /** Pure: debt of one change of {@code low_power}. In the one-launcher world a flip pins nothing by itself (a stopped Recents activity is re-created once, at its next show, and that show is what is counted). */
    static int flipWeight(boolean unifiedActive) { return unifiedActive ? 0 : 1; }

    /** Pure: the debt that justifies a restart in this world. */
    static int minDebtFor(boolean unifiedActive) { return unifiedActive ? PIN_THRESHOLD : MIN_DEBT; }

    /** Pure: the configuration a stock window was created under, as one string. Night bit (already masked), font scale, density, locale tags: the facts whose change makes the stock process re-create the visible Activity. */
    static String signature(int nightBits, float fontScale, int densityDpi, String localeTags) {
        return "n" + nightBits + "|f" + fontScale + "|d" + densityDpi + "|" + (localeTags == null ? "" : localeTags);
    }

    /** What one stock window means: the signature to store and the pins to add. */
    static final class Window {
        final String sig; final int pins;
        Window(String sig, int pins) { this.sig = sig; this.pins = pins; }
    }

    /**
     * Pure: a window of the stock launcher came up under {@code nowSig}. {@code storedSig} EMPTY = none since the last restart: this window is the process's first Activity (the floor), it pins nothing yet. The same
     * signature as the stored one = a second window of the same show or a show under the same configuration: nothing new. A different one = the stock process built a NEW Activity for it, and the Go Recents view
     * pins every new one: one pin (counted only in the one-launcher world; in the emergency exit the old weights decide and only the signature is kept).
     */
    static Window onStockWindow(String storedSig, String nowSig, boolean unifiedActive) {
        String now = nowSig == null ? "" : nowSig;
        if (now.isEmpty()) return new Window(storedSig == null ? "" : storedSig, 0);   // an unreadable configuration changes nothing
        if (storedSig == null || storedSig.isEmpty()) return new Window(now, 0);
        if (storedSig.equals(now)) return new Window(storedSig, 0);
        return new Window(now, unifiedActive ? 1 : 0);
    }

    /** Pure: the event that counts as "a window of the stock launcher came up" (a window-state change of its package; WINDOWS_CHANGED carries no package and is noise for this). */
    static boolean isStockWindow(int eventType, String pkg, String stockPkgOrNull) {
        if (eventType != android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || pkg == null) return false;
        return STOCK_PKG.equals(pkg) || (stockPkgOrNull != null && stockPkgOrNull.equals(pkg));
    }

    /** Pure: the stock process was restarted (or was not running) or the tablet booted: nothing it pinned exists any more, the next stock window is its first. */
    static String sigAfterRestart() { return ""; }

    /** Pure D5: seed the one cleanup? Only when the one launcher is in force, this switch is on and it was never seeded on this install. */
    static boolean seedWanted(boolean unifiedActive, boolean switchOn, boolean alreadySeeded) { return unifiedActive && switchOn && !alreadySeeded; }

    /** Pure D5: the seeded debt = the drive threshold (never lower than what is there). */
    static int seededDebt(int debt, int minDebt) { return Math.max(Math.max(0, debt), Math.max(1, minDebt)); }

    /** Pure D5: the stock process of the v0.9.16 era may hold pinned screens although no window was seen by THIS code: the seed counts as "a stock window was seen" so the blocker does not say "nothing to give back". */
    static String seededSig(String storedSig, String nowSig) {
        if (storedSig != null && !storedSig.isEmpty()) return storedSig;
        return nowSig == null || nowSig.isEmpty() ? "seed" : nowSig;   // never empty: empty means "no stock window since the last restart"
    }

    /** SR1 (the helper path) or a finished drive took the debt away. Under the same lock as the additions, so a flip cannot be overwritten. */
    static synchronized void setDebt(Context app, int v) { try { Features.setIntAsync(app, DEBT_KEY, v); } catch (Throwable ignored) { } }

    /** The first reading of low_power in this process (BatterySaverGuard.start): a Go tablet kills the process often, and the first flip after each start must still count. */
    static void seedLowPower(int now) { synchronized (StockForceStop.class) { if (lastLow == Integer.MIN_VALUE) lastLow = now; } }

    private static synchronized void addDebt(Context app, int n) {
        if (n <= 0) return;
        try { Features.setIntAsync(app, DEBT_KEY, addDebt(Features.getInt(app, DEBT_KEY, 0), n)); } catch (Throwable ignored) { }
    }

    /** BatterySaverGuard's low_power observer: one more change of the saver (ours or the person's: each is a uiMode flip for the stock launcher). Cheap; never throws; nothing when the switch is off. CW1 D3: it weighs nothing in the one-launcher world. */
    static void noteLowPower(Context c, int now) {
        try {
            final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            boolean flip;
            synchronized (StockForceStop.class) { flip = isFlip(lastLow, now); lastLow = now; }
            if (!flip || !switchOn(app)) return;
            boolean unified = Unified.active(app);
            seedIfNeeded(app, unified);
            addDebt(app, flipWeight(unified));
        } catch (Throwable ignored) { }
    }

    // ---- CW1 D3 / D5 glue: the stock windows, the signature, the seed, the restart / boot resets

    private static String sigCache = null;   // the stored signature, loaded once per process (guarded by the class monitor)
    private static volatile boolean seedChecked = false;

    private static String storedSigLocked(Context app) {
        if (sigCache == null) sigCache = Features.getString(app, SIG_KEY, "");
        return sigCache;
    }

    private static void storeSigLocked(Context app, String sig) {
        sigCache = sig;
        Features.setStringAsync(app, SIG_KEY, sig);
    }

    /** The configuration the stock process sees now, as the signature string ("" when it cannot be read). */
    private static String currentSignature(Context app) {
        try {
            android.content.res.Configuration cf = app.getResources().getConfiguration();
            return signature(cf.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK, cf.fontScale, cf.densityDpi, cf.getLocales().toLanguageTags());
        } catch (Throwable t) { return ""; }
    }

    /**
     * RecentsFilter.onEvent: a window of the stock launcher came up (CW1 D3). Records that the stock process has shown something since its last restart and, in the one-launcher world, adds one pin when the
     * configuration differs from the one of the previous stock window. Cheap (a signature compare); nothing when the switch is off; never throws. A debt that reaches the line asks for a look 45 s later.
     */
    static void noteStockWindow(Context c) {
        try {
            final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            if (!switchOn(app)) return;
            boolean unified = Unified.active(app);
            String now = currentSignature(app);
            boolean crossed = false;
            synchronized (StockForceStop.class) {
                String stored = storedSigLocked(app);
                Window w = onStockWindow(stored, now, unified);
                if (w.sig.equals(stored) && w.pins == 0) return;
                storeSigLocked(app, w.sig);
                if (w.pins > 0) {
                    int before = Features.getInt(app, DEBT_KEY, 0), after = addDebt(before, w.pins);
                    Features.setIntAsync(app, DEBT_KEY, after);
                    int line = Features.getInt(app, MIN_DEBT_KEY, minDebtFor(unified));
                    crossed = before < line && after >= line;
                }
            }
            if (crossed) scheduleConsider(app, "stock launcher pins piled up");
        } catch (Throwable ignored) { }
    }

    /** True when a window of the stock launcher has shown since its process was last restarted (or the seed said it may hold old screens). */
    private static boolean stockSeen(Context app) {
        synchronized (StockForceStop.class) { return !storedSigLocked(app).isEmpty(); }
    }

    /**
     * CW1 D5: the first time the one launcher is in force on this install (with this switch on), the stock process of the v0.9.16 era may still hold pinned screens, a model and a stale home task: set the debt to
     * the line ONCE, so the existing quiet-moment drive (after the next ended Quickstep session) cleans it. Guarded by {@link #SEED_KEY}: never twice, never while the switch is off. Cheap; never throws.
     */
    private static void seedIfNeeded(Context app, boolean unified) {
        try {
            if (seedChecked) return;
            boolean already = Features.get(app, SEED_KEY, false);
            if (already) { seedChecked = true; return; }
            if (!seedWanted(unified, switchOn(app), false)) return;
            synchronized (StockForceStop.class) {
                if (Features.get(app, SEED_KEY, false)) { seedChecked = true; return; }
                Features.setAsync(app, SEED_KEY, true);   // the flag first: a lost write below must never mean "twice"
                seedChecked = true;
                Features.setIntAsync(app, DEBT_KEY, seededDebt(Features.getInt(app, DEBT_KEY, 0), Features.getInt(app, MIN_DEBT_KEY, minDebtFor(true))));
                storeSigLocked(app, seededSig(storedSigLocked(app), currentSignature(app)));
            }
            LockLog.add(app, "SF", "the one launcher is in force for the first time: one cleanup of the stock launcher is due at the next quiet moment");
        } catch (Throwable ignored) { }
    }

    /** The stock process was restarted (SR1 through the helper, or a finished SR2 drive) or was not running: its debt is gone and so is everything it showed. */
    static void stockRestarted(Context app) {
        try {
            synchronized (StockForceStop.class) { storeSigLocked(app, sigAfterRestart()); }
            setDebt(app, 0);
        } catch (Throwable ignored) { }
    }

    /** ProfileSession.checkBoot: a new boot. The stock process is new: in the one-launcher world the pins of the dead one are forgotten (the old weights keep their debt: that world never reset it). */
    static void newBoot(Context c) {
        try {
            final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
            synchronized (StockForceStop.class) { storeSigLocked(app, sigAfterRestart()); }
            if (Unified.active(app)) setDebt(app, 0);
        } catch (Throwable ignored) { }
    }

    // ------------------------------------------------------------------------------------------------ whether to start

    /**
     * Pure: "" = go, else the reason in plain words (status line, lock log). Order = the questions a reader asks: switch, gave-up, helper (SR1 does it better), debt, the safe moment, the cooldown.
     * {@code lastAtMs} 0 = never; a clock that went backwards allows it.
     */
    static String blocker(boolean switchOn, int fails, boolean helperRunning, int debt, int minDebt, boolean customMode, boolean customHomeInFront, boolean screenOnUnlocked,
                          boolean automationOff, long lastAtMs, long nowMs, long cooldownMs) {
        return blocker(switchOn, fails, helperRunning, debt, minDebt, customMode, customHomeInFront, screenOnUnlocked, automationOff, lastAtMs, nowMs, cooldownMs, false, true);
    }

    /**
     * CW1 D3: the same with the one-launcher facts. {@code unifiedActive} + no stock window seen since the stock process was last restarted = "nothing to give back" (a process that showed nothing cannot have pinned
     * anything), asked right after the helper question and before the debt. The emergency exit (not unified) never asks it: the v0.9.16 behaviour is unchanged.
     */
    static String blocker(boolean switchOn, int fails, boolean helperRunning, int debt, int minDebt, boolean customMode, boolean customHomeInFront, boolean screenOnUnlocked,
                          boolean automationOff, long lastAtMs, long nowMs, long cooldownMs, boolean unifiedActive, boolean stockWindowSeen) {
        if (!switchOn) return "switch off";
        if (fails >= MAX_FAILS) return "gave up after " + fails + " failed tries (switch it off and on to try again)";
        if (helperRunning) return "the shell helper runs: its exact reading decides";
        if (unifiedActive && !stockWindowSeen) return "nothing to give back: the stock launcher has shown no window since it was last restarted";
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
    static boolean keepsLastResult(String blockerText) { return blockerText != null && (blockerText.startsWith("restarted ") || blockerText.startsWith("not enough") || blockerText.startsWith("nothing to give back")); }

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
