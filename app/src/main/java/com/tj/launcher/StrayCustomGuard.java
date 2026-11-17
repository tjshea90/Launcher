package com.tj.launcher;

/**
 * CG1a (Tj, 2026-11-17: "Sometimes the launcher randomly shows the custom launcher, even though i was in quickstep"): the mirror of {@link StrayHomeGuard}.
 *
 * Why it happens (research/CG1A_MODE_PATHS_2026-11-17.md, path B1; the code is read, the framework half is inferred): a Quickstep session that was locked with an app open gets the custom home
 * put in front behind the keyguard (the lock-screen preparation). The gesture unlock then starts the remembered app ABOVE it. The stored mode stays Quickstep and no router runs, so when that
 * app is left with Back (or ends, or crashes) Android resumes the top HOME task of the home stack, which is our custom home, in the middle of a Quickstep session. Nothing logged it and nothing
 * took it away again.
 *
 * What this does: the accessibility helper sees OUR Launcher window appear while the stored mode is Quickstep, the tablet is unlocked and lit, and no mode change, unlock or return reset is under
 * way. It writes a lock-log line saying exactly that (always) and starts the stock home through the router with the prearm extra "stock" (NOT a Home press, so it can never be read as a double
 * Home). The same stand-down rule as the stray stock home guard: at most {@link #MAX} redirects inside {@link #WINDOW_MS}, then {@link #COOLDOWN_MS} of silence, so a stock launcher that cannot
 * start is never fought in a loop. Pure rules here (JVM tests); the service does the starting.
 */
public final class StrayCustomGuard {
    public static final String KEY = "guard_stray_custom";
    public static final String COUNT_KEY = "guard_custom_count";
    public static final String LAST_KEY = "guard_custom_last";
    /** The activity of the custom home (our fork of Launcher3). Control, Setup and the look-alike screens have other class names and never count. */
    public static final String CUSTOM_HOME_CLASS = "com.android.launcher3.Launcher";
    public static final int MAX = StrayHomeGuard.MAX;
    public static final long WINDOW_MS = StrayHomeGuard.WINDOW_MS, COOLDOWN_MS = StrayHomeGuard.COOLDOWN_MS;
    /** The stored mode must be this old (a deliberate change, the unlock choice and the return reset all move it) before a custom home in Quickstep counts as stray. */
    public static final long MODE_QUIET_MS = 2500;

    private StrayCustomGuard() {}

    /** New Tj-facing behaviour: ON in the shipped build, OFF in a debug build (older emulator sections open our Launcher in Quickstep mode on purpose; the section that tests this switches it on). */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** The window that just appeared is OUR custom home activity (not Control, not a dialog, not another app's window). */
    public static boolean isCustomHome(String pkg, String cls, String ownPkg) {
        return pkg != null && cls != null && ownPkg != null && pkg.equals(ownPkg) && cls.equals(CUSTOM_HOME_CLASS);
    }

    /**
     * @param lockLeftover the window is what was UNDERNEATH the lock screen or appeared in the moments after an unlock ({@link ProfileSession#windowIsLockLeftover})
     * @param pausedUntil  a return reset / Recents clean-up is running until this time ({@code guardPausedUntil}): never fight it
     * @param recent       times (ms) of earlier redirects, 0 = unused slot
     */
    public static boolean shouldRedirect(boolean enabled, boolean quickstepMode, boolean keyguardLocked, boolean screenOn, boolean customHomeShown, boolean lockLeftover,
                                         long now, long modeChangedAt, long pausedUntil, long failOpenUntil, long standDownUntil, long[] recent) {
        if (!enabled || !quickstepMode || keyguardLocked || !screenOn || !customHomeShown || lockLeftover) return false;
        if (now - modeChangedAt < MODE_QUIET_MS) return false;
        if (now < pausedUntil || now < failOpenUntil || now < standDownUntil) return false;
        return StrayHomeGuard.countInWindow(recent, now) < MAX;
    }

    /** What the lock-log line says about the decision (one word group, so the tablet report names the reason a redirect did or did not happen). */
    public static String why(boolean enabled, boolean keyguardLocked, boolean screenOn, boolean lockLeftover, long now, long modeChangedAt, long pausedUntil, long failOpenUntil, long standDownUntil, long[] recent) {
        if (!enabled) return "switch off: only logged";
        if (keyguardLocked) return "lock screen up: the pre-armed home behind it, left alone";
        if (!screenOn) return "screen dark: left alone";
        if (lockLeftover) return "just after an unlock or a handshake: left alone";
        if (now - modeChangedAt < MODE_QUIET_MS) return "mode changed " + Math.max(0, now - modeChangedAt) + " ms ago: left alone";
        if (now < pausedUntil) return "a return reset is running: left alone";
        if (now < failOpenUntil) return "fail-open window: left alone";
        if (now < standDownUntil || StrayHomeGuard.countInWindow(recent, now) >= MAX) return "stood down after " + MAX + " redirects: left alone";
        return "redirecting to the stock home";
    }

    /**
     * CG1 review C (privacy): what the lock log and the tablet report (both end up in a PUBLIC repository) say about the window that was in front before the custom home appeared. The system's own packages are kept
     * (they say WHICH screen it was: the stock launcher, Settings, SystemUI); any other app is only "another app".
     */
    public static String beforeLabel(String pkg) {
        if (pkg == null || pkg.isEmpty()) return "?";
        if ("android".equals(pkg) || pkg.startsWith("com.android.")) return pkg;
        return "another app";
    }
}
