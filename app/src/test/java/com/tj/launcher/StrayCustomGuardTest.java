package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CG1a: the custom home that shows up in the middle of a Quickstep session (research/CG1A_MODE_PATHS_2026-11-17.md, B1), and CG1 review A: a "left alone" verdict is looked at again. */
public class StrayCustomGuardTest {
    private static final long NOW = 1_000_000_000L;
    private static final long[] NONE = new long[StrayCustomGuard.MAX];

    /** The one case that must redirect: Quickstep, unlocked, lit, not after an unlock, mode old, nothing running. */
    private static boolean redirect(boolean enabled, boolean quickstep, boolean locked, boolean screenOn, boolean home, boolean leftover,
                                    long modeAgeMs, long pausedFor, long failOpenFor, long standDownFor, long[] recent) {
        return StrayCustomGuard.shouldRedirect(enabled, quickstep, locked, screenOn, home, leftover, false, 0, 0, NOW, NOW - modeAgeMs, NOW + pausedFor, NOW + failOpenFor, NOW + standDownFor, recent);
    }

    private static int reasonOf(boolean enabled, boolean locked, boolean screenOn, boolean leftover, boolean undecided, long resumingSince,
                                long modeAgeMs, long pausedFor, long failOpenFor, long standDownFor, long[] recent) {
        return reasonWithRouter(enabled, locked, screenOn, leftover, undecided, resumingSince, 0, modeAgeMs, pausedFor, failOpenFor, standDownFor, recent);
    }

    private static int reasonWithRouter(boolean enabled, boolean locked, boolean screenOn, boolean leftover, boolean undecided, long resumingSince, long routerStartedAt,
                                        long modeAgeMs, long pausedFor, long failOpenFor, long standDownFor, long[] recent) {
        return StrayCustomGuard.reason(enabled, true, locked, screenOn, true, leftover, undecided, resumingSince, routerStartedAt, NOW, NOW - modeAgeMs, NOW + pausedFor, NOW + failOpenFor, NOW + standDownFor, recent);
    }

    private static boolean plain() { return redirect(true, true, false, true, true, false, 60_000, 0, 0, 0, NONE); }

    @Test public void theCustomHomeInAQuickstepSessionIsRedirected() { assertTrue(plain()); }

    @Test public void onlyOurHomeActivityCounts() {
        assertTrue(StrayCustomGuard.isCustomHome("com.tj.launcher", "com.android.launcher3.Launcher", "com.tj.launcher"));
        assertFalse(StrayCustomGuard.isCustomHome("com.tj.launcher", "com.tj.launcher.ControlActivity", "com.tj.launcher"));
        assertFalse(StrayCustomGuard.isCustomHome("com.tj.launcher", "com.tj.launcher.SetupActivity", "com.tj.launcher"));
        assertFalse(StrayCustomGuard.isCustomHome("com.android.launcher3", "com.android.launcher3.Launcher", "com.tj.launcher"));   // the STOCK launcher has the same class name
        assertFalse(StrayCustomGuard.isCustomHome(null, "com.android.launcher3.Launcher", "com.tj.launcher"));
        assertFalse(StrayCustomGuard.isCustomHome("com.tj.launcher", null, "com.tj.launcher"));
        assertFalse(StrayCustomGuard.isCustomHome("com.tj.launcher", "com.android.launcher3.Launcher", null));
    }

    @Test public void theCustomModeIsTheNormalCaseAndNeverRedirects() { assertFalse(redirect(true, false, false, true, true, false, 60_000, 0, 0, 0, NONE)); }

    @Test public void theLockScreenAndTheDarkScreenAreLeftAlone() {
        assertFalse(redirect(true, true, true, true, true, false, 60_000, 0, 0, 0, NONE));    // the pre-armed home behind the keyguard is by design
        assertFalse(redirect(true, true, false, false, true, false, 60_000, 0, 0, 0, NONE));
    }

    @Test public void theMomentsAfterAnUnlockAreLeftAlone() { assertFalse(redirect(true, true, false, true, true, true, 60_000, 0, 0, 0, NONE)); }

    @Test public void aFreshModeChangeIsLeftAlone() {
        assertFalse(redirect(true, true, false, true, true, false, 0, 0, 0, 0, NONE));
        assertFalse(redirect(true, true, false, true, true, false, StrayCustomGuard.MODE_QUIET_MS - 1, 0, 0, 0, NONE));
        assertTrue(redirect(true, true, false, true, true, false, StrayCustomGuard.MODE_QUIET_MS, 0, 0, 0, NONE));
    }

    @Test public void aRunningResetAndTheFailOpenWindowAreNeverFought() {
        assertFalse(redirect(true, true, false, true, true, false, 60_000, 5_000, 0, 0, NONE));
        assertFalse(redirect(true, true, false, true, true, false, 60_000, 0, 5_000, 0, NONE));
    }

    @Test public void theSwitchOffOnlyLogs() { assertFalse(redirect(false, true, false, true, true, false, 60_000, 0, 0, 0, NONE)); }

    @Test public void aWindowThatIsNotTheCustomHomeNeverRedirects() { assertFalse(redirect(true, true, false, true, false, false, 60_000, 0, 0, 0, NONE)); }

    @Test public void aLauncherThatCannotStartIsNeverFoughtInALoop() {
        long[] hist = new long[StrayCustomGuard.MAX];
        for (int i = 0; i < StrayCustomGuard.MAX; i++) hist[i] = NOW - 1000L * (i + 1);                 // four redirects inside the window
        assertFalse(redirect(true, true, false, true, true, false, 60_000, 0, 0, 0, hist));
        long[] old = new long[StrayCustomGuard.MAX];
        for (int i = 0; i < StrayCustomGuard.MAX; i++) old[i] = NOW - StrayCustomGuard.WINDOW_MS - 1000L * (i + 1);   // all older than the window: allowed again
        assertTrue(redirect(true, true, false, true, true, false, 60_000, 0, 0, 0, old));
        assertFalse(redirect(true, true, false, true, true, false, 60_000, 0, 0, StrayCustomGuard.COOLDOWN_MS, NONE));   // the cool-down after a stand-down
    }

    @Test public void theMirrorSharesTheStockGuardsLimits() {
        assertEquals(StrayHomeGuard.MAX, StrayCustomGuard.MAX);
        assertEquals(StrayHomeGuard.WINDOW_MS, StrayCustomGuard.WINDOW_MS);
        assertEquals(StrayHomeGuard.COOLDOWN_MS, StrayCustomGuard.COOLDOWN_MS);
    }

    @Test public void theNewSwitchIsOnInTheShippedBuildAndOffInDebug() {
        assertTrue(StrayCustomGuard.defaultOn(false));
        assertFalse(StrayCustomGuard.defaultOn(true));
    }

    // ---------------------------------------------------------------- CG1 review A: the reason code, the unlock being decided, the app that is still launching

    @Test public void anUnlockThatIsStillBeingDecidedIsLeftAlone() {   // a plain PIN flips the stored mode a moment AFTER the Launcher resumed: until then the mode still reads Quickstep
        assertEquals(StrayCustomGuard.UNDECIDED, reasonOf(true, false, true, false, true, 0, 60_000, 0, 0, 0, NONE));
        assertFalse(StrayCustomGuard.shouldRedirect(true, true, false, true, true, false, true, 0, 0, NOW, NOW - 60_000, 0, 0, 0, NONE));
        assertEquals(StrayCustomGuard.LOCKED, reasonOf(true, true, true, false, true, 0, 60_000, 0, 0, 0, NONE));      // the older reasons come first
        assertEquals(StrayCustomGuard.LEFTOVER, reasonOf(true, false, true, true, true, 0, 60_000, 0, 0, 0, NONE));
    }

    @Test public void theAppTheGestureStartedGetsItsTimeToCoverTheCustomHome() {   // sending the stock home while it launches would put the stock home ABOVE the app
        assertEquals(StrayCustomGuard.RESUMING, reasonOf(true, false, true, false, false, NOW - 1_000, 60_000, 0, 0, 0, NONE));
        assertEquals(StrayCustomGuard.RESUMING, reasonOf(true, false, true, false, false, NOW - StrayCustomGuard.RESUME_QUIET_MS + 1, 60_000, 0, 0, 0, NONE));
        assertEquals(StrayCustomGuard.REDIRECT, reasonOf(true, false, true, false, false, NOW - StrayCustomGuard.RESUME_QUIET_MS, 60_000, 0, 0, 0, NONE));
        assertEquals(StrayCustomGuard.REDIRECT, reasonOf(true, false, true, false, false, 0, 60_000, 0, 0, 0, NONE));       // 0 = none in flight (it appeared, or there was none)
        assertEquals(StrayCustomGuard.RESUMING, reasonOf(true, false, true, false, false, NOW + 50_000, 60_000, 0, 0, 0, NONE));   // the clock stepped back: fails safe
    }

    @Test public void theReasonsComeInTheOrderOfTheLogText() {
        assertEquals(StrayCustomGuard.REDIRECT, reasonOf(true, false, true, false, false, 0, 60_000, 0, 0, 0, NONE));
        assertEquals(StrayCustomGuard.SWITCH_OFF, reasonOf(false, true, false, true, true, 0, 0, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.LOCKED, reasonOf(true, true, false, true, true, 0, 0, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.DARK, reasonOf(true, false, false, true, true, 0, 0, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.LEFTOVER, reasonOf(true, false, true, true, true, NOW - 1, 0, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.UNDECIDED, reasonOf(true, false, true, false, true, NOW - 1, 0, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.RESUMING, reasonOf(true, false, true, false, false, NOW - 1, 0, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.MODE_YOUNG, reasonOf(true, false, true, false, false, 0, 1_000, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.PAUSED, reasonOf(true, false, true, false, false, 0, 60_000, 5_000, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.FAIL_OPEN, reasonOf(true, false, true, false, false, 0, 60_000, 0, 5_000, 0, NONE));
        assertEquals(StrayCustomGuard.STOOD_DOWN, reasonOf(true, false, true, false, false, 0, 60_000, 0, 0, 5_000, NONE));
        assertEquals(StrayCustomGuard.NOT_STRAY, StrayCustomGuard.reason(true, false, false, true, true, false, false, 0, 0, NOW, NOW - 60_000, 0, 0, 0, NONE));   // the custom mode is the normal case
        assertEquals(StrayCustomGuard.NOT_STRAY, StrayCustomGuard.reason(true, true, false, true, false, false, false, 0, 0, NOW, NOW - 60_000, 0, 0, 0, NONE));    // not the custom home window
    }

    @Test public void theLogLineNamesTheReasonInTheSameOrderAsTheRule() {
        assertEquals("redirecting to the stock home", StrayCustomGuard.why(StrayCustomGuard.REDIRECT, NOW, NOW - 60_000));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.SWITCH_OFF, NOW, NOW - 60_000).startsWith("switch off"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.LOCKED, NOW, NOW - 60_000).startsWith("lock screen up"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.DARK, NOW, NOW - 60_000).startsWith("screen dark"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.LEFTOVER, NOW, NOW - 60_000).startsWith("just after an unlock"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.UNDECIDED, NOW, NOW - 60_000).startsWith("the unlock is still being decided"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.RESUMING, NOW, NOW - 60_000).startsWith("the app of the gesture is still launching"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.MODE_YOUNG, NOW, NOW - 1_000).startsWith("mode changed 1000 ms ago"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.MODE_YOUNG, NOW, NOW + 9_000).startsWith("mode changed 0 ms ago"));   // a clock that stepped back never prints a negative age
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.PAUSED, NOW, NOW - 60_000).startsWith("a return reset"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.FAIL_OPEN, NOW, NOW - 60_000).startsWith("fail-open"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.STOOD_DOWN, NOW, NOW - 60_000).startsWith("stood down"));
        assertTrue(StrayCustomGuard.why(StrayCustomGuard.NOT_STRAY, NOW, NOW - 60_000).startsWith("not a custom home"));
    }

    // ---------------------------------------------------------------- CG1 review A: a "left alone" is looked at again

    @Test public void onlyWhatTimeCanChangeIsLookedAtAgain() {
        for (int r : new int[]{StrayCustomGuard.REDIRECT, StrayCustomGuard.SWITCH_OFF, StrayCustomGuard.DARK, StrayCustomGuard.STOOD_DOWN, StrayCustomGuard.NOT_STRAY})
            assertEquals("reason " + r, -1, StrayCustomGuard.retryInMs(r, NOW, NOW - 100, NOW - 100, 0, NOW - 100, NOW + 100, NOW + 100, 0));
        for (int r : new int[]{StrayCustomGuard.LOCKED, StrayCustomGuard.LEFTOVER, StrayCustomGuard.UNDECIDED, StrayCustomGuard.RESUMING, StrayCustomGuard.MODE_YOUNG, StrayCustomGuard.PAUSED, StrayCustomGuard.FAIL_OPEN})
            assertTrue("reason " + r, StrayCustomGuard.retryInMs(r, NOW, NOW - 100, NOW - 100, 0, NOW - 100, NOW + 100, NOW + 100, 0) > 0);
    }

    @Test public void theStepsFollowTheEndOfTheWaitedForWindow() {
        assertEquals(650, StrayCustomGuard.retryInMs(StrayCustomGuard.LEFTOVER, NOW, NOW - 300, 0, 0, NOW - 60_000, 0, 0, 0));        // 800 - 300 + 150
        assertEquals(700, StrayCustomGuard.retryInMs(StrayCustomGuard.LEFTOVER, NOW, NOW - 5_000, 0, 0, NOW - 60_000, 0, 0, 0));      // an armed handshake: the unlock is still to come
        assertEquals(700, StrayCustomGuard.retryInMs(StrayCustomGuard.LEFTOVER, NOW, 0, 0, 0, NOW - 60_000, 0, 0, 0));                // no unlock handled yet
        assertEquals(1000, StrayCustomGuard.retryInMs(StrayCustomGuard.LOCKED, NOW, 0, 0, 0, NOW - 60_000, 0, 0, 0));
        assertEquals(500, StrayCustomGuard.retryInMs(StrayCustomGuard.UNDECIDED, NOW, 0, 0, 0, NOW - 60_000, 0, 0, 0));
        assertEquals(1650, StrayCustomGuard.retryInMs(StrayCustomGuard.MODE_YOUNG, NOW, 0, 0, 0, NOW - 1_000, 0, 0, 0));              // 2500 - 1000 + 150
        assertEquals(250, StrayCustomGuard.retryInMs(StrayCustomGuard.RESUMING, NOW, 0, NOW - 14_900, 0, NOW - 60_000, 0, 0, 0));     // 15000 - 14900 + 150
        assertEquals(250, StrayCustomGuard.retryInMs(StrayCustomGuard.PAUSED, NOW, 0, 0, 0, NOW - 60_000, NOW + 100, 0, 0));          // never shorter than the minimum step
    }

    @Test public void aFarAwayEndTimeOrAClockThatStoppedBackNeverSchedulesALongTimer() {
        long max = StrayCustomGuard.RETRY_STEP_MAX_MS;
        assertEquals(max, StrayCustomGuard.retryInMs(StrayCustomGuard.PAUSED, NOW, 0, 0, 0, NOW - 60_000, NOW + 3_600_000, 0, 0));
        assertEquals(max, StrayCustomGuard.retryInMs(StrayCustomGuard.FAIL_OPEN, NOW, 0, 0, 0, NOW - 60_000, 0, NOW + 3_600_000, 0));
        assertEquals(max, StrayCustomGuard.retryInMs(StrayCustomGuard.MODE_YOUNG, NOW, 0, 0, 0, NOW + 3_600_000, 0, 0, 0));          // the mode was "changed" in the future
        assertEquals(max, StrayCustomGuard.retryInMs(StrayCustomGuard.RESUMING, NOW, 0, NOW + 3_600_000, 0, NOW - 60_000, 0, 0, 0));
        assertEquals(max, StrayCustomGuard.retryInMs(StrayCustomGuard.RESUMING, NOW, 0, NOW - 1_000, 0, NOW - 60_000, 0, 0, 0));
    }

    @Test public void theLooksEndAtTheBudget() {
        assertTrue(StrayCustomGuard.retryInMs(StrayCustomGuard.MODE_YOUNG, NOW, 0, 0, 0, NOW, 0, 0, StrayCustomGuard.RETRY_BUDGET_MS - 1) > 0);
        assertEquals(-1, StrayCustomGuard.retryInMs(StrayCustomGuard.MODE_YOUNG, NOW, 0, 0, 0, NOW, 0, 0, StrayCustomGuard.RETRY_BUDGET_MS));
        assertEquals(-1, StrayCustomGuard.retryInMs(StrayCustomGuard.LOCKED, NOW, 0, 0, 0, NOW - 60_000, 0, 0, StrayCustomGuard.RETRY_BUDGET_MS + 1));
        assertTrue("the launch window of the app fits in the budget", StrayCustomGuard.RETRY_BUDGET_MS >= StrayCustomGuard.RESUME_QUIET_MS);
    }

    /** The service's loop, with a clock: look, and while the answer is a retry, move the clock and look again. Returns the clock offset (ms after the first look) at which the verdict became REDIRECT, or -1 (gave up). */
    private static long firstRedirectAfter(long lastUnlockOffset, long resumingOffset, long modeAgeAtStart, boolean undecidedUntil1s) {
        long t = 0, waited = 0;
        int looks = 0;
        while (looks++ < 200) {
            long now = NOW + t;
            long lastUnlockAt = lastUnlockOffset == Long.MIN_VALUE ? 0 : NOW + lastUnlockOffset;
            boolean leftover = lastUnlockAt > 0 && now - lastUnlockAt >= 0 && now - lastUnlockAt < ProfileSession.UNLOCK_QUIET_MS;
            boolean undecided = undecidedUntil1s && t < 1_000;
            long resumingSince = resumingOffset == Long.MIN_VALUE ? 0 : NOW + resumingOffset;
            long modeAt = NOW - modeAgeAtStart;
            int reason = StrayCustomGuard.reason(true, true, false, true, true, leftover, undecided, resumingSince, now, modeAt, 0, 0, 0, NONE);
            if (reason == StrayCustomGuard.REDIRECT) return t;
            long again = StrayCustomGuard.retryInMs(reason, now, lastUnlockAt, resumingSince, 0, modeAt, 0, 0, waited);
            if (again <= 0) return -1;
            t += again; waited += again;
        }
        throw new AssertionError("the looks never ended");
    }

    @Test public void aCustomHomeRevealedJustAfterAnUnlockIsSentBackOnceTheMomentIsOver() {
        long at = firstRedirectAfter(-100, Long.MIN_VALUE, 60_000, false);          // unlock handled 100 ms before the look
        assertTrue("redirected after " + at, at > 0 && at <= 1_000);
        assertEquals("control: nothing waits when nothing is wrong, the first look redirects", 0, firstRedirectAfter(Long.MIN_VALUE, Long.MIN_VALUE, 60_000, false));
    }

    @Test public void aYoungModeChangeIsWaitedOut() {
        long at = firstRedirectAfter(Long.MIN_VALUE, Long.MIN_VALUE, 500, false);   // the mode was written 500 ms ago: 2.5 s of quiet are needed
        assertTrue("redirected after " + at, at >= 2_000 && at <= 2_700);
    }

    @Test public void theUnlockDecisionIsWaitedOutAndThenTheAppLaunchToo() {
        long at = firstRedirectAfter(Long.MIN_VALUE, -200, 60_000, true);           // the gesture asked for an app 200 ms before the look
        assertTrue("redirected after " + at, at >= StrayCustomGuard.RESUME_QUIET_MS - 200 && at <= StrayCustomGuard.RESUME_QUIET_MS + 2_200);
    }

    @Test public void aLaunchThatNeverAppearsIsNotWaitedForForEver() {
        long at = firstRedirectAfter(Long.MIN_VALUE, -200, 60_000, false);
        assertTrue(at > 0 && at < StrayCustomGuard.RETRY_BUDGET_MS + StrayCustomGuard.RETRY_STEP_MAX_MS);
        assertEquals("a window that never ends is given up on (the custom home stays, nothing is started)", -1,
                firstRedirectAfterForever());
    }

    private static long firstRedirectAfterForever() {   // MODE_YOUNG for ever (the mode "changed" in the future): the chain must end without a redirect
        long t = 0, waited = 0;
        for (int looks = 0; looks < 200; looks++) {
            long now = NOW + t;
            int reason = StrayCustomGuard.reason(true, true, false, true, true, false, false, 0, now, NOW + 10_000_000L, 0, 0, 0, NONE);
            if (reason == StrayCustomGuard.REDIRECT) return t;
            long again = StrayCustomGuard.retryInMs(reason, now, 0, 0, 0, NOW + 10_000_000L, 0, 0, waited);
            if (again <= 0) return -1;
            t += again; waited += again;
        }
        throw new AssertionError("the looks never ended");
    }

    // ---------------------------------------------------------------- CG1 review A: the verdict is the first thing in the line (the lock log cuts a line at 200 characters)

    @Test public void theVerdictComesFirstAndTheLineStaysShort() {
        for (int r = 0; r <= StrayCustomGuard.NOT_STRAY; r++) {
            String verdict = StrayCustomGuard.why(r, NOW, NOW - 1_000);
            String line = StrayCustomGuard.logText(verdict, "re-check +19999ms", true, false, 99_999_999L, 99_999_999_000L, "com.android.permissioncontroller");
            assertTrue("reason " + r + ": " + line, line.startsWith(verdict + " | "));
            assertTrue("reason " + r + " is " + line.length() + " long: " + line, line.length() <= 170);   // 200 minus "HH:mm:ss.SSS STRAYCUSTOM " (25) and some room
        }
    }

    @Test public void theContextOfTheLineIsReadableAndCapped() {
        assertEquals("redirecting to the stock home | via onResume, lock=n, screen=on, unlock=none, mode=?, before=com.android.launcher3",
                StrayCustomGuard.logText("redirecting to the stock home", "onResume", false, true, -1, -1, "com.android.launcher3"));
        assertEquals("x | via v, lock=y, screen=dark, unlock=1500ms, mode=61s, before=?", StrayCustomGuard.logText("x", "v", true, false, 1_500, 61_000, "?"));
        assertEquals("x | via v, lock=n, screen=on, unlock=>99s, mode=>9999s, before=?", StrayCustomGuard.logText("x", "v", false, true, 100_000, 10_000_000, "?"));
    }

    @Test public void onlySystemPackagesAreNamedInTheLogAndTheReport() {   // CG1 review C: both go to a public repository
        assertEquals("?", StrayCustomGuard.beforeLabel(null));
        assertEquals("?", StrayCustomGuard.beforeLabel(""));
        assertEquals("com.android.launcher3", StrayCustomGuard.beforeLabel("com.android.launcher3"));
        assertEquals("com.android.settings", StrayCustomGuard.beforeLabel("com.android.settings"));
        assertEquals("android", StrayCustomGuard.beforeLabel("android"));
        assertEquals("another app", StrayCustomGuard.beforeLabel("org.telegram.messenger"));
        assertEquals("another app", StrayCustomGuard.beforeLabel("com.anthropic.claude"));
        assertEquals("another app", StrayCustomGuard.beforeLabel("com.google.android.inputmethod.latin"));
    }
}
