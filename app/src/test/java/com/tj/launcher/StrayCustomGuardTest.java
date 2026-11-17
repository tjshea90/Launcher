package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CG1a: the custom home that shows up in the middle of a Quickstep session (research/CG1A_MODE_PATHS_2026-11-17.md, B1). */
public class StrayCustomGuardTest {
    private static final long NOW = 1_000_000_000L;
    private static final long[] NONE = new long[StrayCustomGuard.MAX];

    /** The one case that must redirect: Quickstep, unlocked, lit, not after an unlock, mode old, nothing running. */
    private static boolean redirect(boolean enabled, boolean quickstep, boolean locked, boolean screenOn, boolean home, boolean leftover,
                                    long modeAgeMs, long pausedFor, long failOpenFor, long standDownFor, long[] recent) {
        return StrayCustomGuard.shouldRedirect(enabled, quickstep, locked, screenOn, home, leftover, NOW, NOW - modeAgeMs, NOW + pausedFor, NOW + failOpenFor, NOW + standDownFor, recent);
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

    @Test public void theLogLineNamesTheReasonInTheSameOrderAsTheRule() {
        assertEquals("redirecting to the stock home", StrayCustomGuard.why(true, false, true, false, NOW, NOW - 60_000, 0, 0, 0, NONE));
        assertTrue(StrayCustomGuard.why(false, false, true, false, NOW, NOW - 60_000, 0, 0, 0, NONE).startsWith("switch off"));
        assertTrue(StrayCustomGuard.why(true, true, true, false, NOW, NOW - 60_000, 0, 0, 0, NONE).startsWith("lock screen up"));
        assertTrue(StrayCustomGuard.why(true, false, false, false, NOW, NOW - 60_000, 0, 0, 0, NONE).startsWith("screen dark"));
        assertTrue(StrayCustomGuard.why(true, false, true, true, NOW, NOW - 60_000, 0, 0, 0, NONE).startsWith("just after an unlock"));
        assertTrue(StrayCustomGuard.why(true, false, true, false, NOW, NOW - 1_000, 0, 0, 0, NONE).startsWith("mode changed 1000 ms ago"));
        assertTrue(StrayCustomGuard.why(true, false, true, false, NOW, NOW - 60_000, NOW + 1, 0, 0, NONE).startsWith("a return reset"));
        assertTrue(StrayCustomGuard.why(true, false, true, false, NOW, NOW - 60_000, 0, NOW + 1, 0, NONE).startsWith("fail-open"));
        long[] hist = new long[StrayCustomGuard.MAX];
        for (int i = 0; i < StrayCustomGuard.MAX; i++) hist[i] = NOW - 1000L * (i + 1);
        assertTrue(StrayCustomGuard.why(true, false, true, false, NOW, NOW - 60_000, 0, 0, 0, hist).startsWith("stood down"));
    }
}
