package com.tj.launcher;

/**
 * CG1a (Tj, 2026-11-17: "Sometimes the launcher randomly shows the custom launcher, even though i was in quickstep"): the mirror of {@link StrayHomeGuard}.
 *
 * Why it happens (research/CG1A_MODE_PATHS_2026-11-17.md, path B1; the code is read, the framework half is inferred): a Quickstep session that was locked with an app open gets the custom home
 * put in front behind the keyguard (the lock-screen preparation). The gesture unlock then starts the remembered app ABOVE it. The stored mode stays Quickstep and no router runs, so when that
 * app is left with Back (or ends, or crashes) Android resumes the top HOME task of the home stack, which is our custom home, in the middle of a Quickstep session. Nothing logged it and nothing
 * took it away again.
 *
 * What this does: the Launcher's own onResume (the one signal that cannot be missed; an own-package window event never reaches the guard) asks the accessibility helper whether OUR custom home is in
 * front while the stored mode is Quickstep, the tablet is unlocked and lit, and no mode change, unlock, app resume or return reset is under way. It writes a lock-log line saying exactly that (always,
 * the verdict FIRST) and starts the stock home through the router with the prearm extra "stock" (NOT a Home press, so it can never be read as a double Home). The same stand-down rule as the stray stock
 * home guard: at most {@link #MAX} redirects inside {@link #WINDOW_MS}, then {@link #COOLDOWN_MS} of silence, so a stock launcher that cannot start is never fought in a loop. Pure rules here (JVM
 * tests); the service does the starting.
 *
 * CG1 review A (2026-11-17): a verdict is NOT final. A look that says "left alone" because of something that only TIME changes (the moments after an unlock, an unlock still being decided, the app the
 * gesture started still launching, a young mode change, a running reset, the fail-open window) is repeated by the service while the Launcher stays in front ({@link #retryInMs}), for at most
 * {@link #RETRY_BUDGET_MS}; a Launcher that pauses in between cancels it. All times are wall-clock milliseconds (System.currentTimeMillis, the clock Mode.changedAt writes): a clock that steps
 * BACKWARDS makes "N ms ago" negative = "too young" = left alone (fails safe: the custom home stays, nothing is started), and a retry step is capped so a far-away end time can never schedule a long timer.
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
    /**
     * The app a gesture unlock brings back (AD5, {@code startQuickstepFront}) is given this long to cover the custom home before the custom home counts as stray: sending the stock home while that launch is in flight
     * would put the stock home ABOVE the app. Same 15 s as the "the resumed app counts for this unlock" rule in the service; a launch that has been SEEN (its window appeared) ends the wait at once.
     */
    public static final long RESUME_QUIET_MS = 15_000;
    /**
     * The service started the router itself (the stock home after a gesture unlock, a redirect of this guard) this recently: what it started is on its way and the custom home is only still in front until its
     * window is ready (slow on a Go tablet). Looking again 1 s later and starting the stock home a SECOND time would not help and would eat the stand-down budget ({@link #MAX} redirects).
     */
    public static final long ROUTER_QUIET_MS = 3_000;
    /** Longest a "left alone" look is repeated for one appearance (the app launch above gets its 15 s inside it), the shortest and longest single step, and the margin added to a computed end time. */
    public static final long RETRY_BUDGET_MS = 20_000, RETRY_MIN_MS = 250, RETRY_STEP_MAX_MS = 2_000, RETRY_MARGIN_MS = 150;

    /** Why a custom home in a Quickstep session is, or is not, sent back. The FIRST reason that applies wins; the numbers are only names (never stored). */
    public static final int REDIRECT = 0, SWITCH_OFF = 1, LOCKED = 2, DARK = 3, LEFTOVER = 4, UNDECIDED = 5, RESUMING = 6, ROUTER_BUSY = 7, MODE_YOUNG = 8, PAUSED = 9, FAIL_OPEN = 10, STOOD_DOWN = 11, NOT_STRAY = 12;

    private StrayCustomGuard() {}

    /** New Tj-facing behaviour: ON in the shipped build, OFF in a debug build (older emulator sections open our Launcher in Quickstep mode on purpose; the section that tests this switches it on). */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** The window that just appeared is OUR custom home activity (not Control, not a dialog, not another app's window). */
    public static boolean isCustomHome(String pkg, String cls, String ownPkg) {
        return pkg != null && cls != null && ownPkg != null && pkg.equals(ownPkg) && cls.equals(CUSTOM_HOME_CLASS);
    }

    /**
     * THE decision, as a reason code.
     * @param lockLeftover   the window is what was UNDERNEATH the lock screen or appeared in the moments after an unlock ({@link ProfileSession#windowIsLockLeftover})
     * @param unlockUndecided the lock cycle is still open: the unlock was seen but the unlock choice has not been decided yet (a plain PIN flips the stored mode a moment AFTER the Launcher resumed: {@link AnimationSpeedup#lockCycleOpen})
     * @param resumingSince  when the app the gesture started was requested, while it has not appeared yet; 0 = none in flight
     * @param routerStartedAt when the service last started the router (0 = never): {@link #ROUTER_QUIET_MS}
     * @param pausedUntil    a return reset / Recents clean-up is running until this time ({@code guardPausedUntil}): never fight it
     * @param recent         times (ms) of earlier redirects, 0 = unused slot
     */
    public static int reason(boolean enabled, boolean quickstepMode, boolean keyguardLocked, boolean screenOn, boolean customHomeShown, boolean lockLeftover, boolean unlockUndecided, long resumingSince,
                             long routerStartedAt, long now, long modeChangedAt, long pausedUntil, long failOpenUntil, long standDownUntil, long[] recent) {
        if (!quickstepMode || !customHomeShown) return NOT_STRAY;
        if (!enabled) return SWITCH_OFF;
        if (keyguardLocked) return LOCKED;
        if (!screenOn) return DARK;
        if (lockLeftover) return LEFTOVER;
        if (unlockUndecided) return UNDECIDED;
        if (resumingSince > 0 && now - resumingSince < RESUME_QUIET_MS) return RESUMING;   // a negative difference (the clock stepped back) counts as "still launching": fails safe
        if (routerStartedAt > 0 && now - routerStartedAt < ROUTER_QUIET_MS) return ROUTER_BUSY;
        if (now - modeChangedAt < MODE_QUIET_MS) return MODE_YOUNG;
        if (now < pausedUntil) return PAUSED;
        if (now < failOpenUntil) return FAIL_OPEN;
        if (now < standDownUntil || StrayHomeGuard.countInWindow(recent, now) >= MAX) return STOOD_DOWN;
        return REDIRECT;
    }

    public static boolean shouldRedirect(boolean enabled, boolean quickstepMode, boolean keyguardLocked, boolean screenOn, boolean customHomeShown, boolean lockLeftover, boolean unlockUndecided, long resumingSince,
                                         long routerStartedAt, long now, long modeChangedAt, long pausedUntil, long failOpenUntil, long standDownUntil, long[] recent) {
        return reason(enabled, quickstepMode, keyguardLocked, screenOn, customHomeShown, lockLeftover, unlockUndecided, resumingSince, routerStartedAt, now, modeChangedAt, pausedUntil, failOpenUntil, standDownUntil, recent) == REDIRECT;
    }

    /** What the lock-log line says about the decision (one word group, so the tablet report names the reason a redirect did or did not happen). */
    public static String why(int reason, long now, long modeChangedAt) {
        switch (reason) {
            case REDIRECT: return "redirecting to the stock home";
            case SWITCH_OFF: return "switch off: only logged";
            case LOCKED: return "lock screen up: the pre-armed home behind it, left alone";
            case DARK: return "screen dark: left alone";
            case LEFTOVER: return "just after an unlock or a handshake: left alone";
            case UNDECIDED: return "the unlock is still being decided: left alone";
            case RESUMING: return "the app of the gesture is still launching: left alone";
            case ROUTER_BUSY: return "the stock home was just started: left alone";
            case MODE_YOUNG: return "mode changed " + Math.max(0, now - modeChangedAt) + " ms ago: left alone";
            case PAUSED: return "a return reset is running: left alone";
            case FAIL_OPEN: return "fail-open window: left alone";
            case STOOD_DOWN: return "stood down after " + MAX + " redirects: left alone";
            default: return "not a custom home in a Quickstep session";
        }
    }

    /**
     * How long to wait before looking AGAIN at a "left alone" verdict that time alone can change; -1 = do not look again (the switch is off, the screen is dark, the guard stood down, nothing to wait for, or
     * {@code waitedMs} - how long this appearance has been looked at already, 0 at the first look - reached {@link #RETRY_BUDGET_MS}). The budget bounds the NUMBER of looks: the look a step lands on always
     * decides. The service looks again only while the Launcher is still in front and cancels the wait when it pauses.
     */
    public static long retryInMs(int reason, long now, long lastUnlockAt, long resumingSince, long routerStartedAt, long modeChangedAt, long pausedUntil, long failOpenUntil, long waitedMs) {
        if (waitedMs >= RETRY_BUDGET_MS) return -1;
        long want;
        switch (reason) {
            case LOCKED: want = 1000; break;                                                   // the lock screen goes when the unlock is done
            case LEFTOVER: {
                long since = now - lastUnlockAt;
                want = lastUnlockAt > 0 && since >= 0 && since < ProfileSession.UNLOCK_QUIET_MS ? ProfileSession.UNLOCK_QUIET_MS - since + RETRY_MARGIN_MS : 700;   // 700: a handshake is armed, the unlock has not happened yet
                break;
            }
            case UNDECIDED: want = 500; break;                                                 // the unlock handler answers within milliseconds; its own valve ends the cycle after 5 s
            case RESUMING: want = resumingSince + RESUME_QUIET_MS - now + RETRY_MARGIN_MS; break;
            case ROUTER_BUSY: want = routerStartedAt + ROUTER_QUIET_MS - now + RETRY_MARGIN_MS; break;
            case MODE_YOUNG: want = MODE_QUIET_MS - Math.max(0, now - modeChangedAt) + RETRY_MARGIN_MS; break;
            case PAUSED: want = pausedUntil - now + RETRY_MARGIN_MS; break;
            case FAIL_OPEN: want = failOpenUntil - now + RETRY_MARGIN_MS; break;
            default: return -1;
        }
        return Math.max(RETRY_MIN_MS, Math.min(want, RETRY_STEP_MAX_MS));
    }

    /**
     * The lock-log text of one look. The verdict comes FIRST: the lock log cuts a line at 200 characters (LockLog.line) and the first version put it last, so exactly the part that answers "why" was the part
     * that got cut. Everything after it is short context with capped numbers (the whole text stays under 170 characters, pinned by a test).
     * @param sinceUnlockMs how long ago the last unlock was handled, negative = none; @param modeAgeMs how old the stored mode is, negative = unknown
     */
    public static String logText(String verdict, String via, boolean locked, boolean screenOn, long sinceUnlockMs, long modeAgeMs, String before) {
        return verdict + " | via " + via + ", lock=" + (locked ? "y" : "n") + ", screen=" + (screenOn ? "on" : "dark")
                + ", unlock=" + (sinceUnlockMs < 0 ? "none" : sinceUnlockMs > 99_999 ? ">99s" : sinceUnlockMs + "ms")
                + ", mode=" + (modeAgeMs < 0 ? "?" : modeAgeMs > 9_999_000 ? ">9999s" : (modeAgeMs / 1000) + "s") + ", before=" + before;
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
