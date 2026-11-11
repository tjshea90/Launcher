package com.tj.launcher;

/**
 * BH1 (Tj, 2026-11-08 02:29 UTC: "When I unlocked to the custom launcher from the lock screen, there was a notification from a quickstep app. Turn all notifications off for both the lock screen and the custom
 * launcher. The lock screen (no matter which launcher it was on) and the custom launcher should never show any notification at any time, unless it can safely and without delay or flicker only include
 * notifications from apps that are in the custom launcher app drawer and not hidden").
 *
 * The pure rules (JVM-tested, no Android classes) for the POP-UP BANNER half. The lock screen's own list is {@link LockQuiet}'s job (AK1, held all the time, from any launcher); the clean-up at the start of a custom
 * session is {@code ReturnReset}'s. What neither did: a notification that ARRIVES while the custom launcher (or the dark / locked screen) is in front pops up as a banner at the top. Android's switch for that
 * is the global setting {@link #SETTING}; {@link NotifQuiet} writes it (adb grant WRITE_SECURE_SETTINGS) and puts it back; THIS class decides WHEN it must be off.
 *
 * It is the exact complement of {@link AnimationPolicy}: banners are quiet EVERYWHERE except inside an unlocked, decided Quickstep session (the stock home and the apps opened there). The lock screen counts as the
 * custom world (a Quickstep session that is locked or dark is quiet too: "the lock screen, no matter which launcher it was on"). The custom launcher needs no accessibility helper: without it nothing can change
 * the mode, so a hold taken there stays right, and a process restart must not flip the banners on for the minute or two the helper needs to bind. A QUICKSTEP session does need it (code review 2026-11-08): it is
 * what hears the lock screen go away, so with the helper gone nothing could ever end a hold taken at the lock, and the safe direction is "banners as Android has them".
 */
public final class NotifQuietPolicy {
    private NotifQuietPolicy() {}

    /** The switch in Launcher control (shipped default ON, debug builds OFF so the older emulator sections keep their premises, like clean_start and quickstep_animations_off). */
    public static final String KEY = "quiet_banners";

    /** Settings.Global: any non-zero number = pop-up (heads-up) notifications on, 0 = off. SystemUI reads a missing or unreadable value as off. */
    public static final String SETTING = "heads_up_notifications_enabled";

    /** The default of {@link #KEY}. */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** The {@link #why} text for "a Quickstep mode and the accessibility helper is not running" (also asked by {@link RadioIconHold#radiosGoOffWhenHoldEnds}, so the words live in one place). */
    public static final String WHY_NO_HELPER = "accessibility helper not running";

    /** Everything the decision reads, by name. All false = nothing is wanted ("switch off"). */
    public static final class Facts {
        /** the switch in Launcher control */
        public boolean switchOn;
        /** WRITE_SECURE_SETTINGS (the one-time adb grant) */
        public boolean grant;
        /** crash breaker tripped, or "Restore everything" paused the launcher ({@link CrashGuard#automationOff}) */
        public boolean automationOff;
        /** the stored mode says Quickstep (false = the custom launcher) */
        public boolean quickstepMode;
        /** the accessibility helper is connected: it is what hears the screen go off and the unlock, so a Quickstep session without it can never end a hold in time (only asked in Quickstep mode) */
        public boolean helperUp;
        /** the display is on (interactive) */
        public boolean screenOn;
        /** the lock screen is up (keyguard locked) */
        public boolean lockUp;
        /** the lock screen was up and the unlock choice (plain PIN = custom, gesture = Quickstep) has not been applied yet: the stored mode can still be the OLD session's */
        public boolean unlockUndecided;

        /** The facts of an ordinary unlocked Quickstep session with everything granted: the one situation that is NOT quiet. */
        public static Facts quickstepSession() {
            Facts f = new Facts();
            f.switchOn = true; f.grant = true; f.quickstepMode = true; f.helperUp = true; f.screenOn = true;
            return f;
        }
    }

    /**
     * Why the quiet hold is NOT wanted right now: the FIRST failing condition in plain words, or "" when it is wanted. One function for the decision and for the words (log, Launcher control), so they can
     * never disagree.
     */
    public static String why(Facts f) {
        if (!f.switchOn) return "switch off";
        if (!f.grant) return "no adb permission";
        if (f.automationOff) return "automation paused";
        if (!f.quickstepMode) return "";        // the custom launcher and everything opened from it
        if (!f.helperUp) return WHY_NO_HELPER;   // Quickstep without the helper: nothing could end a hold when the lock screen goes
        if (!f.screenOn) return "";             // dark screen: the lock screen is next, in any launcher
        if (f.lockUp) return "";                // the lock screen
        if (f.unlockUndecided) return "";       // a plain PIN is still being decided: the custom launcher may be what opens
        return "Quickstep session";
    }

    public static boolean wanted(Facts f) { return why(f).isEmpty(); }

    /** For the log: what the banners are quiet FOR, when {@link #wanted}. */
    public static String quietFor(Facts f) {
        if (!f.quickstepMode) return "custom launcher";
        if (!f.screenOn) return "screen off";
        if (f.lockUp) return "lock screen";
        if (f.unlockUndecided) return "unlock not decided yet";
        return "";
    }

    // ---------------------------------------------------------------- the one setting (a Settings.Global string)

    /** True when the stored value means "no banners", exactly as SystemUI reads it: "0", a missing value, and a value that is not a number (its default is off). */
    public static boolean isOff(String v) {
        if (v == null) return true;
        try { return Integer.parseInt(v.trim()) == 0; } catch (NumberFormatException e) { return true; }
    }

    /** Does this value need a write to switch the banners off? Not when they already are (a permanent setting of Tj's own is left alone). */
    public static boolean needsZero(String current) { return !isOff(current); }

    /**
     * What to write back when the hold ends, or null for "write nothing": only a value that is STILL our zero is put back, and only when the original was a real "on". A value changed by hand (or by "Restore
     * everything") meanwhile is not ours.
     */
    public static String restoreValue(String current, String original) {
        if (current == null || !"0".equals(current.trim())) return null;
        return original == null || isOff(original) ? null : original;
    }
}
