package com.tj.launcher;

/** Pure decision logic for the power-menu helper (JVM-testable). */
public final class PowerMenuRule {
    public static final int NOTHING = 0, COVER_SCREENSHOT = 1, CLOSE_MENU = 2;

    private PowerMenuRule() {}

    /**
     * AD7a (Tj, 2026-11-06: "when I lock the screen from quickstep, the power menu overlay doesn't work. I need it to always work any time I'm on the lock screen"): the world in which the power menu is covered.
     * The custom launcher, AND every lock screen: a plain PIN always ends a Quickstep session in the custom launcher (AC1), so a lock screen is custom territory even while the mode flag still says Quickstep (it flips
     * only at the unlock, or at the lock after a successful shell-helper clean-up, which this tablet never gets). Unlocked Quickstep stays untouched.
     */
    public static boolean coverWorld(boolean customMode, boolean keyguardLocked) { return customMode || keyguardLocked; }

    /**
     * @param customMode      our launcher is the active one (Quickstep mode never touches the power menu)
     * @param keyguardLocked  the device is on the lock screen: the replacement card works there too (Tj, 2026-11-03), blocking does not
     * @param menuOpen        the window shows both "Power off" and "Screenshot"
     * @param screenshotFound we located the Screenshot entry's bounds
     */
    public static int decide(String powerMode, boolean customMode, boolean keyguardLocked, boolean menuOpen, boolean screenshotFound) {
        if (!customMode || !menuOpen) return NOTHING;
        if (Features.POWER_BLOCK.equals(powerMode)) return keyguardLocked ? NOTHING : CLOSE_MENU;   // closing the menu stays off the lock screen
        if (Features.isCover(powerMode)) return screenshotFound ? COVER_SCREENSHOT : NOTHING;
        return NOTHING;
    }

    /**
     * BN2 (Tj, 2026-11-09 03:02 UTC, tablet report): after the stock menu was closed with the Home key, a scan 143 ms later still found the dialog's window in the window list, WITH its entries, and drew the TALL
     * card over it for 250 ms ("it flashed the old custom power menu you made which is tall"). The stock dialog fades out for about 250 ms and stays in the list until its window is removed. For this long after a
     * close, "the dialog is up" is not believed unless the dialog announced itself again (a new TYPE_WINDOW_STATE_CHANGED of class ActionsDialog = a real new opening, which clears the mark).
     */
    public static final long DYING_DIALOG_MS = 450;

    /** @param menuClosedAtMs uptime ms of the last close (0 = none yet) */
    public static boolean dyingDialog(long now, long menuClosedAtMs) {
        return menuClosedAtMs > 0 && now >= menuClosedAtMs && now - menuClosedAtMs < DYING_DIALOG_MS;
    }

    /**
     * CX1c (emulator runs #771 / #772, sections hm and gm): a scan 0.5-0.7 s after the stock menu opened answered "the card cannot be located" although BOTH menu texts were found ("menuOpen=true found=false"; the dialog is
     * mid-layout, the tree answers from a half-built state) and the patch went away while the stock menu was still open = the real Screenshot entry for the rest of it. {@code AZ1} already doubted ONE "no menu text" answer
     * within 2 s of the opening (look again 250 ms later); this rule also doubts "text found, card not located", up to {@link #HOLD_MISSES_TEXT_SEEN} times in a row (the text IS the evidence that the menu is open).
     * @param menuTextSeen both texts were found (only the card's bounds are unresolved); false = no menu text at all
     * @param holdMisses   doubted answers in a row so far
     */
    public static boolean holdOnMiss(boolean customMode, boolean overlayShown, boolean coverMode, boolean menuTextSeen, long sinceOpenMs, int holdMisses) {
        if (!customMode || !overlayShown || !coverMode || sinceOpenMs >= HOLD_WINDOW_MS) return false;
        return holdMisses < (menuTextSeen ? HOLD_MISSES_TEXT_SEEN : 1);
    }

    /** AZ1: an answer "not open" is doubted only this long after the opening. */
    public static final long HOLD_WINDOW_MS = 2000;
    /** CX1c: how many times in a row "menu text found, card not located" is doubted (each look is 250 ms after the one before). */
    public static final int HOLD_MISSES_TEXT_SEEN = 3;
}
