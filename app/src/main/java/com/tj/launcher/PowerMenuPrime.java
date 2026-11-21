package com.tj.launcher;

/**
 * CS2 (Tj, 2026-11-21): "when I press the back button in either the app drawer or notification quick settings tile, the launcher in addition to what it already
 * does should prepare as much as possible the power menu screenshot button cover for both the home screen and lock screen, so that even on the first press of
 * the side key, the button is hidden with no flicker at all. This may need the launcher to automatically open then close the power menu to prep it".
 *
 * <p>No Android in here (tested in PowerMenuPrimeTest). What the first side-key press after a return pays for: the stock dialog's first inflation and draw in
 * SystemUI (the same dialog class shows on the lock screen), our patch window's first draw with the open look, and the first fast-path pass through our own
 * service. One automatic open + close of the stock power menu, done once right after "Back to custom" while a cover (the picture of the home screen, else
 * black) hides it, pays all of that BEFORE Tj's first press. The lock screen cannot be primed by opening the menu there (the screen is unlocked at that moment,
 * and a menu opened over a dark or lit lock screen is a different, untested risk): its patch window is pre-created at the next screen-off as before, and the
 * stock dialog it will meet is already warm.
 *
 * <p>The rules that matter: it never starts unless everything is quiet and plain (custom home in front, unlocked, screen on, no other calibration, no menu
 * already open, no cover of the reset up); it presses BACK only on a menu it opened itself and that is verifiably still there; it ends by itself after at most
 * {@link #CAP_MS}; and a switch (release ON, debug OFF, like every new behaviour) turns it off.
 */
public final class PowerMenuPrime {
    private PowerMenuPrime() {}

    public static final String KEY = "prime_menu";
    /** The one-line result of the last run (Launcher control and the tablet report). */
    public static final String LAST_KEY = "prime_last";

    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** The return's own clean-up (apps, notifications, Recents, the cover) needs a moment first. */
    public static final long START_DELAY_MS = 1200;
    /** How long the start conditions may stay "not yet" before the run is given up (nothing is opened then). */
    public static final long START_WAIT_MAX_MS = 12_000;
    /** Looking again for a start condition. */
    public static final long RECHECK_MS = 400;
    /** After asking Android for the menu: how long to wait for its window. */
    public static final long OPEN_WAIT_MS = 2_500;
    /** The menu stays open this long: its first frames are drawn and our patch has drawn its open look. */
    public static final long HOLD_MS = 700;
    /** After a BACK: how long before the next one. */
    public static final long CLOSE_STEP_MS = 600;
    public static final int CLOSE_TRIES = 3;
    /** The whole run (from the cover going up) is over by then: the cover's own failsafe is 4.5 s. */
    public static final long CAP_MS = 4_000;
    /** Not again within this time of the last run's end. */
    public static final long MIN_GAP_MS = 60_000;
    /** The polling step of a running prime. */
    public static final long TICK_MS = 100;

    /** Everything the start decision depends on; the service fills it in. */
    public static final class Facts {
        public boolean switchOn, automationOff, coverMode, customMode, homeInFront, homeBusy, screenOn, keyguardLocked, pinPad, menuOpen, otherStage, coverUp, wanted;
        public long msSinceLastEnd = Long.MAX_VALUE;
    }

    /** Start now / look again / give this run up. */
    public static final int GO = 0, WAIT = 1, STOP = 2;

    public static final class Start {
        public final int verdict; public final String why;
        Start(int verdict, String why) { this.verdict = verdict; this.why = why; }
    }

    /** The start decision. waitedMs = time since the return. Permanent reasons stop the run; transient ones wait (bounded by {@link #START_WAIT_MAX_MS}). */
    public static Start start(Facts f, long waitedMs) {
        if (!f.wanted) return new Start(STOP, "nothing asked for it");
        if (!f.switchOn) return new Start(STOP, "the switch is off");
        if (f.automationOff) return new Start(STOP, "automation is off (crash guard or pause)");
        if (!f.coverMode) return new Start(STOP, "the power menu mode has no patch to prime");
        if (!f.customMode) return new Start(STOP, "not the custom launcher any more");
        if (f.keyguardLocked || f.pinPad) return new Start(STOP, "the screen is locked");
        if (!f.screenOn) return new Start(STOP, "the screen is off");
        if (f.msSinceLastEnd < MIN_GAP_MS) return new Start(STOP, "the last run ended " + (f.msSinceLastEnd / 1000) + " s ago");
        String wait = "";
        if (f.menuOpen) wait = "a power menu is open";
        else if (f.coverUp) wait = "the return's cover is still up";
        else if (f.otherStage) wait = "a calibration or measurement is running";
        else if (!f.homeInFront) wait = "the custom home is not in front yet";
        else if (f.homeBusy) wait = "the home screen is busy";
        if (wait.isEmpty()) return new Start(GO, "everything is quiet");
        if (waitedMs >= START_WAIT_MAX_MS) return new Start(STOP, "gave up after " + (waitedMs / 1000) + " s: " + wait);
        return new Start(WAIT, wait);
    }

    /** Phases of a running prime. */
    public static final int IDLE = 0, WAITING = 1, OPENING = 2, HOLDING = 3, CLOSING = 4;

    /** What to do now: nothing, press BACK, or the run is over. */
    public static final int ACT_NONE = 0, ACT_BACK = 1, ACT_END = 2;

    public static final class Step {
        public final int phase, action; public final String why;
        Step(int phase, int action, String why) { this.phase = phase; this.action = action; this.why = why; }
    }

    /**
     * One look of a running prime (the menu has been asked for). phase / phaseAgeMs: where it is and for how long; runMs: since the cover went up;
     * dialogPresent: the stock menu's window is in the window list; safeToBack: the screen is on, unlocked, the custom launcher is the screen under the menu;
     * backTries: BACKs sent so far.
     */
    public static Step step(int phase, long phaseAgeMs, long runMs, boolean dialogPresent, boolean safeToBack, int backTries) {
        switch (phase) {
            case OPENING:
                if (dialogPresent) return new Step(HOLDING, ACT_NONE, "the menu is up");
                if (phaseAgeMs >= OPEN_WAIT_MS) return new Step(IDLE, ACT_END, "no menu window came within " + OPEN_WAIT_MS + " ms; nothing to close");
                return new Step(OPENING, ACT_NONE, "");
            case HOLDING:
                if (!dialogPresent) return new Step(IDLE, ACT_END, "the menu was closed by something else");
                if (phaseAgeMs < HOLD_MS && runMs < CAP_MS - 1500) return new Step(HOLDING, ACT_NONE, "");
                if (!safeToBack) return new Step(IDLE, ACT_END, "the screen is not plain any more: the menu is left to the system");
                return new Step(CLOSING, ACT_BACK, "closing it");
            case CLOSING:
                if (!dialogPresent) return new Step(IDLE, ACT_END, "closed");
                if (!safeToBack) return new Step(IDLE, ACT_END, "the screen is not plain any more: the menu is left to the system");
                if (backTries >= CLOSE_TRIES) return new Step(IDLE, ACT_END, "the menu would not close after " + backTries + " BACK presses: tap outside it");
                if (phaseAgeMs >= CLOSE_STEP_MS) return new Step(CLOSING, ACT_BACK, "BACK again");
                return new Step(CLOSING, ACT_NONE, "");
            default:
                return new Step(IDLE, ACT_END, "not running");
        }
    }

    /** The line for Launcher control / the tablet report. */
    public static String summary(String clock, String why, boolean opened, long ms) {
        return clock + " " + (opened ? "opened and closed the stock menu in " + ms + " ms" : "did not open the menu") + ": " + why;
    }
}
