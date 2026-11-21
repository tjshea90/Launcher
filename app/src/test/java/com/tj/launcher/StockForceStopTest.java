package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** CT1 / SR2: every rule of the helper-free Force stop of the stock launcher that can be decided without a device. */
public class StockForceStopTest {

    private static final long H = 3_600_000L;

    private static String block(boolean sw, int fails, boolean helper, int debt, boolean custom, boolean home, boolean unlocked, boolean autoOff, long last, long now) {
        return StockForceStop.blocker(sw, fails, helper, debt, 12, custom, home, unlocked, autoOff, last, now, 4 * H);
    }

    // ---------------------------------------------------------------- defaults and names

    @Test public void defaultIsOnInReleaseAndOffInDebug() {
        assertTrue(StockForceStop.defaultOn(false));
        assertFalse(StockForceStop.defaultOn(true));
        assertEquals("stock_force_stop", StockForceStop.KEY);   // stored in the preferences: renaming it would reset Tj's choice
        assertEquals("com.android.launcher3", StockForceStop.STOCK_PKG);
        assertEquals("com.android.settings", StockForceStop.SETTINGS_PKG);
    }

    @Test public void coverOutlivesTheWholeRun() {
        assertTrue(StockForceStop.COVER_MS > StockForceStop.CAP_MS);
        assertTrue(StockForceStop.COVER_MS > 4500);   // the cover's own failsafe is 4.5 s: a Settings drive needs longer
    }

    // ---------------------------------------------------------------- the debt

    @Test public void onlyAChangeOfTheSaverIsAFlip() {
        assertFalse(StockForceStop.isFlip(Integer.MIN_VALUE, 1));   // the first reading is no change
        assertFalse(StockForceStop.isFlip(1, 1));                   // an observer that reports one write twice
        assertTrue(StockForceStop.isFlip(0, 1));
        assertTrue(StockForceStop.isFlip(1, 0));
    }

    @Test public void debtAddsAndCaps() {
        assertEquals(5, StockForceStop.addDebt(4, 1));
        assertEquals(1, StockForceStop.addDebt(-7, 1));
        assertEquals(3, StockForceStop.addDebt(3, -2));
        assertEquals(100_000, StockForceStop.addDebt(99_999, 50));
        assertEquals(100_000, StockForceStop.addDebt(Integer.MAX_VALUE, 5));
    }

    // ---------------------------------------------------------------- whether to start

    @Test public void goesWhenEverythingHolds() {
        assertEquals("", block(true, 0, false, 12, true, true, true, false, 0, 100 * H));
        assertEquals("", block(true, 1, false, 40, true, true, true, false, 90 * H, 100 * H));   // one earlier failure still allows a try
    }

    @Test public void everyBlockerNamesItsReason() {
        assertEquals("switch off", block(false, 0, false, 99, true, true, true, false, 0, 100 * H));
        assertTrue(block(true, 2, false, 99, true, true, true, false, 0, 100 * H).startsWith("gave up after 2 failed tries"));
        assertTrue(block(true, 0, true, 99, true, true, true, false, 0, 100 * H).contains("shell helper"));
        assertTrue(block(true, 0, false, 11, true, true, true, false, 0, 100 * H).startsWith("not enough has piled up yet (11 of 12)"));
        assertTrue(block(true, 0, false, 12, false, true, true, false, 0, 100 * H).contains("Quickstep"));
        assertTrue(block(true, 0, false, 12, true, false, true, false, 0, 100 * H).contains("custom home"));
        assertTrue(block(true, 0, false, 12, true, true, false, false, 0, 100 * H).contains("screen"));
        assertTrue(block(true, 0, false, 12, true, true, true, true, 0, 100 * H).contains("automation"));
    }

    @Test public void cooldownBlocksUntilFourHoursPassed() {
        long now = 100 * H;
        assertTrue(block(true, 0, false, 12, true, true, true, false, now - 3 * H, now).startsWith("restarted 180 min ago"));
        assertEquals("", block(true, 0, false, 12, true, true, true, false, now - 4 * H, now));
        assertEquals("", block(true, 0, false, 12, true, true, true, false, now + H, now));   // a clock that went backwards allows it
    }

    @Test public void aFailedDriveIsRetriedAfterHalfAnHourNotFourHours() {
        long now = 100 * H;
        long last = StockForceStop.lastAtAfter(false, now, 4 * H);
        assertEquals(now - 4 * H + 30 * 60_000L, last);
        assertTrue(block(true, 1, false, 12, true, true, true, false, last, now + 29 * 60_000L).startsWith("restarted"));
        assertEquals("", block(true, 1, false, 12, true, true, true, false, last, now + 30 * 60_000L));
        assertEquals(now, StockForceStop.lastAtAfter(true, now, 4 * H));
    }

    @Test public void theStatusKeepsTheLastResultWhileOnlyTheCooldownOrTheDebtBlocks() {
        assertTrue(StockForceStop.keepsLastResult("restarted 12 min ago"));
        assertTrue(StockForceStop.keepsLastResult("not enough has piled up yet (3 of 12)"));
        assertFalse(StockForceStop.keepsLastResult("the custom home is not on screen"));
        assertFalse(StockForceStop.keepsLastResult(null));
    }

    @Test public void startVerdictWaitsThenGivesUp() {
        assertEquals("", StockForceStop.startVerdict("", "", 0));
        assertEquals("WAIT:the home screen is busy", StockForceStop.startVerdict("", "the home screen is busy", 5_000));
        assertTrue(StockForceStop.startVerdict("", "the home screen is busy", 12_000).startsWith("STOP:gave up after 12 s"));
        assertEquals("STOP:the screen is off or locked", StockForceStop.startVerdict("the screen is off or locked", "busy", 0));
    }

    // ---------------------------------------------------------------- the drive

    private static StockForceStop.Obs obs(boolean settings, int button, boolean confirm, boolean home) {
        StockForceStop.Obs o = new StockForceStop.Obs();
        o.safe = true; o.plain = true; o.settingsFront = settings; o.button = button; o.confirmUp = confirm; o.homeFront = home;
        return o;
    }

    private static StockForceStop.Step st(int phase, long age, long run, int clicks, int leave, int pending, StockForceStop.Obs o) { return StockForceStop.step(phase, age, run, clicks, leave, pending, o); }

    @Test public void happyPathPressesForceStopThenOkThenLeaves() {
        StockForceStop.Step s = st(StockForceStop.OPENING, 300, 300, 0, 0, 0, obs(true, StockForceStop.BTN_ENABLED, false, false));
        assertEquals(StockForceStop.PRESSING, s.phase); assertEquals(StockForceStop.ACT_CLICK_FORCE, s.action);
        s = st(StockForceStop.PRESSING, 400, 700, 1, 0, 0, obs(true, StockForceStop.BTN_ENABLED, true, false));
        assertEquals(StockForceStop.CONFIRMING, s.phase); assertEquals(StockForceStop.ACT_CLICK_OK, s.action);
        s = st(StockForceStop.CONFIRMING, 300, 1000, 1, 0, 0, obs(true, StockForceStop.BTN_GREY, false, false));
        assertEquals(StockForceStop.LEAVING, s.phase); assertEquals(StockForceStop.OUT_RESTARTED, s.outcome); assertEquals(StockForceStop.ACT_NONE, s.action);
        s = st(StockForceStop.LEAVING, 0, 1100, 1, 0, StockForceStop.OUT_RESTARTED, obs(true, StockForceStop.BTN_GREY, false, false));
        assertEquals(StockForceStop.ACT_BACK, s.action);
        s = st(StockForceStop.LEAVING, 300, 1400, 1, 1, StockForceStop.OUT_RESTARTED, obs(true, StockForceStop.BTN_GREY, false, false));
        assertEquals("a BACK just sent is given time to work", StockForceStop.ACT_NONE, s.action);
        s = st(StockForceStop.LEAVING, 800, 1900, 1, 1, StockForceStop.OUT_RESTARTED, obs(false, StockForceStop.BTN_NONE, false, true));
        assertEquals(StockForceStop.ACT_END, s.action); assertEquals(StockForceStop.OUT_RESTARTED, s.outcome);
    }

    @Test public void aGreyButtonMeansNothingIsRunningButOnlyAfterItHadTimeToTurnOn() {
        StockForceStop.Step s = st(StockForceStop.OPENING, 800, 800, 0, 0, 0, obs(true, StockForceStop.BTN_GREY, false, false));
        assertEquals("the button is enabled by an answer to a broadcast: wait", StockForceStop.OPENING, s.phase);
        s = st(StockForceStop.OPENING, StockForceStop.GREY_WAIT_MS, 2600, 0, 0, 0, obs(true, StockForceStop.BTN_GREY, false, false));
        assertEquals(StockForceStop.LEAVING, s.phase); assertEquals(StockForceStop.OUT_NOT_RUNNING, s.outcome);
        assertTrue(StockForceStop.clearsDebt(StockForceStop.OUT_NOT_RUNNING));
    }

    @Test public void aPageThatNeverComesIsAFailure() {
        StockForceStop.Step s = st(StockForceStop.OPENING, StockForceStop.PAGE_WAIT_MS, 7100, 0, 0, 0, obs(false, StockForceStop.BTN_NONE, false, false));
        assertEquals(StockForceStop.OUT_FAILED, s.outcome); assertTrue(s.why.contains("did not come up"));
        s = st(StockForceStop.OPENING, StockForceStop.PAGE_WAIT_MS, 7100, 0, 0, 0, obs(true, StockForceStop.BTN_NONE, false, false));
        assertTrue(s.why.contains("no Force stop button"));
        assertFalse(StockForceStop.clearsDebt(StockForceStop.OUT_FAILED));
    }

    @Test public void forceStopIsPressedAgainOnceThenGivenUp() {
        StockForceStop.Obs o = obs(true, StockForceStop.BTN_ENABLED, false, false);
        assertEquals(StockForceStop.ACT_NONE, st(StockForceStop.PRESSING, 800, 900, 1, 0, 0, o).action);
        StockForceStop.Step s = st(StockForceStop.PRESSING, StockForceStop.RETRY_CLICK_MS, 1600, 1, 0, 0, o);
        assertEquals(StockForceStop.ACT_CLICK_FORCE, s.action);
        assertEquals("only ONE retry", StockForceStop.ACT_NONE, st(StockForceStop.PRESSING, 2000, 2100, 2, 0, 0, o).action);
        s = st(StockForceStop.PRESSING, StockForceStop.DIALOG_WAIT_MS, 4100, 2, 0, 0, o);
        assertEquals(StockForceStop.OUT_FAILED, s.outcome); assertEquals(StockForceStop.LEAVING, s.phase);
    }

    @Test public void aMomentWithoutAnySettingsWindowIsNotYetADialogThatClosed() {
        StockForceStop.Obs o = obs(false, StockForceStop.BTN_NONE, false, false);
        assertEquals(StockForceStop.CONFIRMING, st(StockForceStop.CONFIRMING, 300, 2000, 1, 0, 0, o).phase);
        assertEquals(StockForceStop.OUT_RESTARTED, st(StockForceStop.CONFIRMING, 1500, 3000, 1, 0, 0, o).outcome);
    }

    @Test public void aDialogThatStaysIsAFailure() {
        StockForceStop.Obs o = obs(true, StockForceStop.BTN_NONE, true, false);
        assertEquals(StockForceStop.CONFIRMING, st(StockForceStop.CONFIRMING, 1000, 3000, 1, 0, 0, o).phase);
        StockForceStop.Step s = st(StockForceStop.CONFIRMING, StockForceStop.GONE_WAIT_MS, 6000, 1, 0, 0, o);
        assertEquals(StockForceStop.OUT_FAILED, s.outcome);
    }

    @Test public void anUnsafeScreenStopsTheRunAtOnceAndNeverPressesAnythingOnALockScreen() {
        StockForceStop.Obs o = obs(true, StockForceStop.BTN_ENABLED, false, false);
        o.safe = false;   // e.g. the stored mode changed
        StockForceStop.Step s = st(StockForceStop.OPENING, 100, 100, 0, 0, 0, o);
        assertEquals(StockForceStop.LEAVING, s.phase); assertEquals(StockForceStop.OUT_ABORTED, s.outcome); assertEquals(StockForceStop.ACT_NONE, s.action);
        o.plain = false;   // the screen went dark or locked: leaving presses nothing
        s = st(StockForceStop.LEAVING, 900, 1000, 0, 0, StockForceStop.OUT_ABORTED, o);
        assertEquals(StockForceStop.ACT_END, s.action);
        assertEquals(StockForceStop.OUT_ABORTED, s.outcome);
    }

    @Test public void theCapEndsAnyRun() {
        StockForceStop.Step s = st(StockForceStop.PRESSING, 1000, StockForceStop.CAP_MS, 1, 0, 0, obs(true, StockForceStop.BTN_ENABLED, false, false));
        assertEquals(StockForceStop.LEAVING, s.phase); assertEquals(StockForceStop.OUT_FAILED, s.outcome);
    }

    @Test public void leavingPressesBackTwiceThenHomeThenGivesUp() {
        StockForceStop.Obs o = obs(true, StockForceStop.BTN_GREY, false, false);
        assertEquals(StockForceStop.ACT_BACK, st(StockForceStop.LEAVING, 0, 0, 1, 0, StockForceStop.OUT_RESTARTED, o).action);
        assertEquals(StockForceStop.ACT_BACK, st(StockForceStop.LEAVING, 800, 0, 1, 1, StockForceStop.OUT_RESTARTED, o).action);
        assertEquals(StockForceStop.ACT_HOME, st(StockForceStop.LEAVING, 800, 0, 1, 2, StockForceStop.OUT_RESTARTED, o).action);
        StockForceStop.Step s = st(StockForceStop.LEAVING, 800, 0, 1, StockForceStop.LEAVE_TRIES, StockForceStop.OUT_RESTARTED, o);
        assertEquals("a restart that happened stays a restart even when the way home is stuck", StockForceStop.OUT_RESTARTED, s.outcome);
        s = st(StockForceStop.LEAVING, 800, 0, 1, StockForceStop.LEAVE_TRIES, StockForceStop.OUT_FAILED, o);
        assertEquals(StockForceStop.OUT_FAILED, s.outcome);
    }

    @Test public void anIdlePhaseDoesNothing() {
        assertEquals(StockForceStop.ACT_END, st(StockForceStop.IDLE, 0, 0, 0, 0, 0, obs(false, 0, false, false)).action);
    }

    // ---------------------------------------------------------------- text

    @Test public void resultTextsSayWhatHappened() {
        String t = StockForceStop.resultText("11-21 22:00", StockForceStop.OUT_RESTARTED, "", 14, 3200, "after a Quickstep session");
        assertTrue(t, t.contains("restarted the stock launcher through Settings in 3200 ms (debt was 14)"));
        assertTrue(StockForceStop.resultText("c", StockForceStop.OUT_NOT_RUNNING, "", 12, 1, "").contains("was not running"));
        assertTrue(StockForceStop.resultText("c", StockForceStop.OUT_ABORTED, "the screen is off", 12, 1, "").contains("nothing restarted: the screen is off"));
        assertTrue(StockForceStop.resultText("c", StockForceStop.OUT_FAILED, "the App info page did not come up", 12, 1, "button").contains("could not restart the stock launcher through Settings: the App info page did not come up"));
    }
}
