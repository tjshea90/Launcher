package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * CW1 (Tj, 2026-11-22): the pure rules of the speed / resources / hider / stock-launcher-sleep changes
 * (research/CW1_FINDINGS_2026-11-22.md). One file, one section per package, so a later reader finds what pins which finding.
 */
public class Cw1RulesTest {

    // ================================================================ D4: the Recents clear-all clock (RecentsClock)

    @Test public void clockDeadlineFollowsTheFirstSignAndNeverLeavesTheCap() {
        // no sign yet: wait up to the cap
        assertEquals(1000 + 6000, RecentsClock.deadline(1000, 2500, true, 0));
        // the key was refused: the old fixed rule, whatever happens
        assertEquals(1000 + 2500, RecentsClock.deadline(1000, 2500, false, 0));
        assertEquals(1000 + 2500, RecentsClock.deadline(1000, 2500, false, 4000));
        // a sign: the budget starts there
        assertEquals(4500, RecentsClock.deadline(1000, 2500, true, 2000));
        // a very early sign never gives LESS than the old rule (a sign at the press itself equals it, a sign "before" it cannot shorten it)
        assertEquals(1000 + 2500, RecentsClock.deadline(1000, 2500, true, 1000));
        assertEquals(1000 + 2500, RecentsClock.deadline(1000, 2500, true, 500));
        assertEquals(3501, RecentsClock.deadline(1000, 2500, true, 1001));
        // a very late sign never runs past the cap
        assertEquals(1000 + 6000, RecentsClock.deadline(1000, 2500, true, 6500));
    }

    @Test public void clockInstanceMovesTheDeadlineOnlyAtTheFirstSign() {
        RecentsClock c = new RecentsClock(1000, 2500, true);
        assertFalse(c.signSeen());
        assertEquals(-1L, c.coldStartMs());
        assertFalse(c.over(6999));
        assertTrue(c.over(7000));
        c.sign(3000);
        assertTrue(c.signSeen());
        assertEquals(2000L, c.coldStartMs());
        assertEquals(5500, c.deadline());
        c.sign(1500);   // later calls change nothing
        assertEquals(2000L, c.coldStartMs());
        assertFalse(c.over(5499));
        assertTrue(c.over(5500));
    }

    @Test public void clockKeepsTheCoverUpOnceAndOnlyWhenItRunsPastTheOldBudget() {
        RecentsClock c = new RecentsClock(1000, 2500, true);
        assertFalse(c.takeCoverKeep(3499));
        assertTrue(c.takeCoverKeep(3500));
        assertFalse(c.takeCoverKeep(3600));   // once
        assertEquals(RecentsClock.COVER_MARGIN_MS + (c.deadline() - 3500), c.coverFor(3500));
        assertEquals(RecentsClock.COVER_MARGIN_MS, c.coverFor(c.deadline() + 99_000));   // never negative
        RecentsClock refused = new RecentsClock(1000, 2500, false);
        assertFalse(refused.takeCoverKeep(99_000));   // a refused key never extends anything
    }

    @Test public void clockFallbackIsTheOldFixedRuleFromNow() {
        RecentsClock c = new RecentsClock(1000, 2500, true).fallback(5000, 1800);
        assertEquals(6800, c.deadline());
        assertFalse(c.takeCoverKeep(99_000));
        assertTrue(new RecentsClock(1000, 2500, true).describe(1500).startsWith("no stock window seen in 500 ms"));
        RecentsClock s = new RecentsClock(1000, 2500, true); s.sign(1700);
        assertEquals("cold start +700 ms", s.describe(9999));
    }

    // ================================================================ D3 / D5: the stock-launcher debt counts PINS in the one-launcher world

    @Test public void sessionsAndSaverFlipsWeighNothingInTheOneLauncherWorld() {
        assertEquals(0, StockForceStop.sessionWeight(true));
        assertEquals(1, StockForceStop.sessionWeight(false));
        assertEquals(0, StockForceStop.flipWeight(true));
        assertEquals(1, StockForceStop.flipWeight(false));
        assertEquals(StockForceStop.PIN_THRESHOLD, StockForceStop.minDebtFor(true));
        assertEquals(StockForceStop.MIN_DEBT, StockForceStop.minDebtFor(false));
    }

    @Test public void aStockWindowUnderAnotherConfigurationIsOnePin() {
        String a = StockForceStop.signature(32, 1.2f, 170, "en-US"), b = StockForceStop.signature(16, 1.2f, 170, "en-US");
        assertEquals("n32|f1.2|d170|en-US", a);
        // the first window since a restart: the process's floor, no pin
        StockForceStop.Window first = StockForceStop.onStockWindow("", a, true);
        assertEquals(a, first.sig); assertEquals(0, first.pins);
        // the same configuration: nothing new
        assertEquals(0, StockForceStop.onStockWindow(a, a, true).pins);
        // a different one: a new Activity, one pin (only counted in the one-launcher world; the signature is kept either way)
        StockForceStop.Window w = StockForceStop.onStockWindow(a, b, true);
        assertEquals(b, w.sig); assertEquals(1, w.pins);
        StockForceStop.Window old = StockForceStop.onStockWindow(a, b, false);
        assertEquals(b, old.sig); assertEquals(0, old.pins);
        // an unreadable configuration changes nothing (and a null store reads as none)
        StockForceStop.Window unreadable = StockForceStop.onStockWindow(a, "", true);
        assertEquals(a, unreadable.sig); assertEquals(0, unreadable.pins);
        assertEquals("", StockForceStop.onStockWindow(null, null, true).sig);
        assertEquals("", StockForceStop.sigAfterRestart());
    }

    @Test public void onlyAWindowStateChangeOfTheStockPackageIsAStockWindow() {
        int stateChanged = android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        int windowsChanged = android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        assertTrue(StockForceStop.isStockWindow(stateChanged, "com.android.launcher3", null));
        assertTrue(StockForceStop.isStockWindow(stateChanged, "other.stock", "other.stock"));   // the lookup's answer on an image with another package
        assertFalse(StockForceStop.isStockWindow(windowsChanged, "com.android.launcher3", null));   // noise: no package, no class
        assertFalse(StockForceStop.isStockWindow(stateChanged, "com.tj.launcher", "com.android.launcher3"));
        assertFalse(StockForceStop.isStockWindow(stateChanged, null, null));
    }

    @Test public void theSeedRunsOnceAndNeverLowersTheDebt() {
        assertTrue(StockForceStop.seedWanted(true, true, false));
        assertFalse(StockForceStop.seedWanted(true, true, true));    // already seeded on this install
        assertFalse(StockForceStop.seedWanted(false, true, false));  // the emergency exit never seeds
        assertFalse(StockForceStop.seedWanted(true, false, false));  // the switch is off
        assertEquals(5, StockForceStop.seededDebt(0, 5));
        assertEquals(9, StockForceStop.seededDebt(9, 5));   // never lower than what is there
        assertEquals(1, StockForceStop.seededDebt(-2, 0));
        assertEquals("S", StockForceStop.seededSig("S", "X"));
        assertEquals("X", StockForceStop.seededSig("", "X"));
        assertEquals("seed", StockForceStop.seededSig("", ""));   // never empty: empty means "no stock window since the last restart"
        assertEquals("seed", StockForceStop.seededSig(null, null));
    }

    @Test public void nothingToGiveBackWhenNoStockWindowHasShownSinceTheLastRestart() {
        long H = 3_600_000L;
        // one launcher, no stock window since the restart: blocked right after the helper question, before the debt
        String b = StockForceStop.blocker(true, 0, false, 99, 5, true, true, true, false, 0L, 10 * H, 4 * H, true, false);
        assertTrue(b, b.startsWith("nothing to give back"));
        assertTrue(StockForceStop.keepsLastResult(b));
        // a window was seen: the usual questions (debt first)
        String lowDebt = StockForceStop.blocker(true, 0, false, 2, 5, true, true, true, false, 0L, 10 * H, 4 * H, true, true);
        assertTrue(lowDebt, lowDebt.startsWith("not enough has piled up"));
        assertEquals("", StockForceStop.blocker(true, 0, false, 5, 5, true, true, true, false, 0L, 10 * H, 4 * H, true, true));
        // the emergency exit (not unified) never asks the question: v0.9.16 behaviour
        assertEquals("", StockForceStop.blocker(true, 0, false, 12, 12, true, true, true, false, 0L, 10 * H, 4 * H, false, false));
        // the helper question still comes first
        assertTrue(StockForceStop.blocker(true, 0, true, 99, 5, true, true, true, false, 0L, 10 * H, 4 * H, true, false).startsWith("the shell helper runs"));
        // the older 12-argument form is the emergency exit's
        assertEquals("", StockForceStop.blocker(true, 0, false, 12, 12, true, true, true, false, 0L, 10 * H, 4 * H));
    }

    @Test public void aRestartClearsTheDebtOnlyWhenTheProcessIsNew() {
        assertTrue(StockForceStop.clearsDebt(StockForceStop.OUT_RESTARTED));
        assertTrue(StockForceStop.clearsDebt(StockForceStop.OUT_NOT_RUNNING));
        assertFalse(StockForceStop.clearsDebt(StockForceStop.OUT_FAILED));
        assertFalse(StockForceStop.clearsDebt(StockForceStop.OUT_ABORTED));
    }

    // ================================================================ D7 / A9: no refused dumpsys, a one-minute stock lookup

    @Test public void theMemoryRecorderSpawnsNothingWithoutTheHelper() {
        assertTrue(QsMemory.mayRunDumpsys(true));
        assertFalse(QsMemory.mayRunDumpsys(false));
        assertTrue(QsMemory.STATUS_NEEDS_HELPER.startsWith("no reading"));   // retryDue keys on it
    }

    @Test public void theStockLookupAnswerIsKeptForAMinuteButNeverAFailure() {
        assertFalse(StockLauncher.cacheFresh(false, 100, 200));                          // a failed lookup is retried at once
        assertTrue(StockLauncher.cacheFresh(true, 100, 100));
        assertTrue(StockLauncher.cacheFresh(true, 100, 100 + StockLauncher.CACHE_MS));
        assertFalse(StockLauncher.cacheFresh(true, 100, 100 + StockLauncher.CACHE_MS + 1));
        assertFalse(StockLauncher.cacheFresh(true, 100, 50));                            // a clock that went backwards never keeps one
    }

    // ================================================================ A4 / A6: fewer cross-process looks on the unlock path

    @Test public void theFrontLookEvaluatesOnlyTheSideWhoseAnswerIsUsed() {
        assertEquals(UnifiedRules.FrontLook.NONE, UnifiedRules.frontLook(true, true));
        assertEquals(UnifiedRules.FrontLook.NONE, UnifiedRules.frontLook(false, true));
        assertEquals(UnifiedRules.FrontLook.OWN, UnifiedRules.frontLook(true, false));
        assertEquals(UnifiedRules.FrontLook.STOCK, UnifiedRules.frontLook(false, false));
    }

    @Test public void theReducedLooksGiveExactlyTheOldDecisionInEveryCase() {
        boolean[] tf = {false, true};
        for (boolean active : tf) for (boolean stock : tf) for (boolean own : tf) for (boolean remembered : tf) {
            UnifiedRules.FrontLook look = UnifiedRules.frontLook(active, remembered);
            boolean reduced = UnifiedRules.quickstepFrontOk(active, look == UnifiedRules.FrontLook.STOCK && stock, look == UnifiedRules.FrontLook.OWN && own, remembered);
            assertEquals("active=" + active + " stock=" + stock + " own=" + own + " remembered=" + remembered, UnifiedRules.quickstepFrontOk(active, stock, own, remembered), reduced);
        }
    }

    @Test public void theSettleLoopFetchesTheTreeOnlyWhenItCanChangeTheAnswer() {
        long look = UnifiedRules.SETTLE_LOOK_MS;
        // waiting for the profile with our Launcher resumed: only "keep waiting" is possible, so no tree until the next look time
        assertFalse(UnifiedRules.settleNeedsTree(true, true, 100, 100 + look));
        assertTrue(UnifiedRules.settleNeedsTree(true, true, 100 + look, 100 + look));   // the deadline of the next look comes FIRST
        assertTrue(UnifiedRules.settleNeedsTree(true, true, 5000, 100));
        // every other state looks at every tick, as before
        assertTrue(UnifiedRules.settleNeedsTree(false, true, 100, 100 + look));
        assertTrue(UnifiedRules.settleNeedsTree(true, false, 100, 100 + look));
        assertTrue(UnifiedRules.settleNeedsTree(false, false, 100, 100 + look));
    }

    // ================================================================ B3 / B1a / B9 / B2 / B4: the hider's pure rules (HomePatchRules, CW1 block)

    @Test public void onlyAFullSearchForAnUnknownPencilWaitsForTheBurstsQuietWork() {
        assertEquals(HomePatchRules.PENCIL_AFTER_FRAME, HomePatchRules.pencilTreatment(false, false, true));
        assertEquals(HomePatchRules.PENCIL_NOW, HomePatchRules.pencilTreatment(true, false, true));    // a cover is up: it follows the panel exactly as before
        assertEquals(HomePatchRules.PENCIL_NOW, HomePatchRules.pencilTreatment(false, true, true));    // a known pencil node: one node refresh
        assertEquals(HomePatchRules.PENCIL_NOW, HomePatchRules.pencilTreatment(false, false, false));  // not a SystemUI event
        assertTrue(HomePatchRules.PENCIL_DEFER_MAX_MS >= TjAccessibilityService.FAST_WAIT_MS + 20);   // the burst's quiet work runs at FAST_WAIT_MS + 20 at the latest
    }

    @Test public void theFirstCompositeOfAWindowIsBuiltAfterAShorterQuietAndNeverShownStale() {
        assertEquals(HomePatchRules.FIRST_BUILD_QUIET_MS, HomePatchRules.buildQuietMs(false));
        assertEquals(HomePatchRules.DRAW_QUIET_MS, HomePatchRules.buildQuietMs(true));
        assertTrue(HomePatchRules.FIRST_BUILD_QUIET_MS < HomePatchRules.DRAW_QUIET_MS);
        long first = HomePatchRules.FIRST_BUILD_QUIET_MS;
        assertFalse(HomePatchRules.buildDue(1000 + first - 1, 1000, false, false, first));
        assertTrue(HomePatchRules.buildDue(1000 + first, 1000, false, false, first));
        assertFalse(HomePatchRules.buildDue(1000 + first, 1000, false, false, HomePatchRules.DRAW_QUIET_MS));   // a rebuild keeps 250 ms
        assertFalse(HomePatchRules.buildDue(5000, 1000, true, false, first));    // fresh: nothing to build
        assertFalse(HomePatchRules.buildDue(5000, 1000, false, true, first));    // a build is running
        assertTrue(HomePatchRules.buildDue(5000, 0, false, false, first));       // no draw seen yet
        // the old four-argument forms are the 250 ms rule
        assertEquals(HomePatchRules.buildDue(1100, 1000, false, false, HomePatchRules.DRAW_QUIET_MS), HomePatchRules.buildDue(1100, 1000, false, false));
        assertEquals(HomePatchRules.waitMs(1100, 0, 1000, HomePatchRules.DRAW_QUIET_MS), HomePatchRules.waitMs(1100, 0, 1000));
        // the wait until the build is the remaining part of the quiet, and the finger wait is unchanged
        assertEquals(60, HomePatchRules.waitMs(1040, 0, 1000, first));
        assertEquals(0, HomePatchRules.waitMs(1200, 0, 1000, first));
        assertEquals(HomePatchRules.TOUCH_AWAY_MS - 10, HomePatchRules.waitMs(1210, 1200, 1000, first));
    }

    private static HomePatchRules.Facts good() {
        HomePatchRules.Facts f = new HomePatchRules.Facts();
        f.switchOn = true; f.coverMode = true; f.customMode = true; f.unlocked = true; f.screenOn = true; f.homeShowing = true; f.rectKnown = true; f.cropValid = true;
        return f;
    }

    @Test public void aTransientWallpaperStepKeepsTheWindowOnlyWhenNothingElseIsWrong() {
        HomePatchRules.Facts f = good(); f.cropValid = false;
        assertTrue(HomePatchRules.transientStepOnly(f));
        assertTrue(HomePatchRules.keepWindowForStep(true, true, true, true, f));
        assertFalse(HomePatchRules.keepWindowForStep(false, true, true, true, f));   // no window to keep
        assertFalse(HomePatchRules.keepWindowForStep(true, false, true, true, f));   // not the home flavour
        assertFalse(HomePatchRules.keepWindowForStep(true, true, false, true, f));   // another rotation
        assertFalse(HomePatchRules.keepWindowForStep(true, true, true, false, f));   // another place
        assertFalse(HomePatchRules.transientStepOnly(good()));                       // the crop is there: nothing is transient
        HomePatchRules.Facts notHome = good(); notHome.cropValid = false; notHome.homeShowing = false;
        assertFalse(HomePatchRules.transientStepOnly(notHome));                      // another reason as well: the window goes as before
        HomePatchRules.Facts noRect = good(); noRect.cropValid = false; noRect.rectKnown = false;
        assertFalse(HomePatchRules.transientStepOnly(noRect));
        HomePatchRules.Facts locked = good(); locked.cropValid = false; locked.unlocked = false;
        assertFalse(HomePatchRules.transientStepOnly(locked));
    }

    @Test public void theFastCardNeedsEveryPremiseAtOnce() {
        assertTrue(HomePatchRules.cardFastReady(true, true, false, false, false, true, true, true, true));
        assertFalse(HomePatchRules.cardFastReady(false, true, false, false, false, true, true, true, true));  // switch off
        assertFalse(HomePatchRules.cardFastReady(true, false, false, false, false, true, true, true, true));  // no card window attached
        assertFalse(HomePatchRules.cardFastReady(true, true, true, false, false, true, true, true, true));    // a menu is already open
        assertFalse(HomePatchRules.cardFastReady(true, true, false, true, false, true, true, true, true));    // the lock screen keeps its own paths
        assertFalse(HomePatchRules.cardFastReady(true, true, false, false, true, true, true, true, true));    // the PIN pad
        assertFalse(HomePatchRules.cardFastReady(true, true, false, false, false, false, true, true, true));  // cover mode off
        assertFalse(HomePatchRules.cardFastReady(true, true, false, false, false, true, false, true, true));  // not the custom world
        assertFalse(HomePatchRules.cardFastReady(true, true, false, false, false, true, true, false, true));  // geometry not learned
        assertFalse(HomePatchRules.cardFastReady(true, true, false, false, false, true, true, true, false));  // card not wanted
    }

    @Test public void theCardIsMadeTouchableAfterTheStockFadeLikeThePatch() {
        assertEquals(PatchFade.SETTLE_MS + 20, HomePatchRules.CARD_TOUCHABLE_MS);
        assertEquals(HomePatchRules.CARD_AT_ONCE, HomePatchRules.cardLayoutTreatment(false, false));
        assertEquals(HomePatchRules.CARD_AT_ONCE, HomePatchRules.cardLayoutTreatment(false, true));
        assertEquals(HomePatchRules.CARD_NOTHING, HomePatchRules.cardLayoutTreatment(true, true));
        assertEquals(HomePatchRules.CARD_VISIBLE_FIRST, HomePatchRules.cardLayoutTreatment(true, false));
    }

    // ================================================================ C6: idle executors end their thread

    @Test public void anIdleExecutorKeepsOrderAndLetsItsThreadGoAndStartsAnotherForTheNextTask() throws Exception {
        final ThreadPoolExecutor e = IdleExecutor.single("cw1-test-idle", 50);
        final List<Integer> out = Collections.synchronizedList(new ArrayList<Integer>());
        final CountDownLatch done = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) { final int n = i; e.execute(new Runnable() { @Override public void run() { out.add(n); done.countDown(); } }); }
        assertTrue(done.await(5, TimeUnit.SECONDS));
        List<Integer> want = new ArrayList<Integer>();
        for (int i = 0; i < 20; i++) want.add(i);
        assertEquals(want, new ArrayList<Integer>(out));   // one worker, FIFO
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (e.getPoolSize() > 0 && System.nanoTime() < end) Thread.sleep(20);
        assertEquals("the worker thread ends after the idle time", 0, e.getPoolSize());
        final CountDownLatch again = new CountDownLatch(1);
        e.execute(new Runnable() { @Override public void run() { again.countDown(); } });
        assertTrue("the next task starts a new worker", again.await(5, TimeUnit.SECONDS));
        e.shutdown();
    }
}
