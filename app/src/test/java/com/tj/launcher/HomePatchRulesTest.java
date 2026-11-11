package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * BI1: the rules of the real-pixel patch on the unlocked home screen ({@link HomePatchRules}). Each test pins one way a wrong answer would show Tj the stock Screenshot button on the home screen, a patch
 * that does not match what is under it, or a camera flash at a bad moment.
 */
public class HomePatchRulesTest {
    private static HomePatchRules.Facts good() {
        HomePatchRules.Facts f = new HomePatchRules.Facts();
        f.switchOn = true; f.coverMode = true; f.customMode = true; f.unlocked = true; f.screenOn = true; f.homeShowing = true; f.rectKnown = true; f.cropValid = true;
        return f;
    }

    private static HomePatchRules.CalState calGood() {
        HomePatchRules.CalState s = new HomePatchRules.CalState();
        s.switchOn = true; s.coverMode = true; s.customMode = true; s.unlocked = true; s.screenOn = true; s.homeAtRest = true; s.rectKnown = true; s.permission = true;
        return s;
    }

    // ---------------------------------------------------------------- the window is wanted

    @Test public void everythingInOrderWantsThePatch() {
        assertEquals("", HomePatchRules.whyNot(good()));
        assertTrue(HomePatchRules.wanted(good()));
    }

    @Test public void eachConditionAloneTakesThePatchAway() {
        HomePatchRules.Facts f;
        f = good(); f.switchOn = false; assertEquals("switch off", HomePatchRules.whyNot(f));
        f = good(); f.coverMode = false; assertEquals("power menu mode is not 'hide Screenshot'", HomePatchRules.whyNot(f));
        f = good(); f.automationOff = true; assertEquals("automation paused", HomePatchRules.whyNot(f));
        f = good(); f.customMode = false; assertEquals("Quickstep", HomePatchRules.whyNot(f));
        f = good(); f.unlocked = false; assertEquals("lock screen", HomePatchRules.whyNot(f));
        f = good(); f.screenOn = false; assertEquals("screen off", HomePatchRules.whyNot(f));
        f = good(); f.homeShowing = false; assertEquals("not on the plain home screen", HomePatchRules.whyNot(f));
        f = good(); f.floatingViewOpen = true; assertEquals("a folder or popup is open", HomePatchRules.whyNot(f));
        f = good(); f.busy = true; assertEquals("dragging or changing screen", HomePatchRules.whyNot(f));
        f = good(); f.calibrating = true; assertEquals("calibrating", HomePatchRules.whyNot(f));
        f = good(); f.rectKnown = false; assertTrue(HomePatchRules.whyNot(f).startsWith("open the power menu once"));
        f = good(); f.cropValid = false; assertEquals("not calibrated for this wallpaper and page yet", HomePatchRules.whyNot(f));
    }

    @Test public void theFirstFailingConditionIsTheOneReported() {
        HomePatchRules.Facts f = new HomePatchRules.Facts();   // everything false
        assertEquals("switch off", HomePatchRules.whyNot(f));
        f.switchOn = true;
        assertEquals("power menu mode is not 'hide Screenshot'", HomePatchRules.whyNot(f));
        f.coverMode = true; f.automationOff = true;
        assertEquals("automation paused", HomePatchRules.whyNot(f));
    }

    @Test public void theSwitchShipsOnInReleaseAndOffInDebug() {
        assertTrue(HomePatchRules.defaultOn(false));
        assertFalse(HomePatchRules.defaultOn(true));
    }

    // ---------------------------------------------------------------- fresh, due, visible

    @Test public void aPatchBuiltAfterTheLastDrawIsFreshAndOneBuiltBeforeItIsNot() {
        assertTrue(HomePatchRules.compositeFresh(1000, 900, true));
        assertTrue("built in the same millisecond as the last draw: the draw may have come first, but the request was made after the frame", HomePatchRules.compositeFresh(1000, 1000, true));
        assertFalse(HomePatchRules.compositeFresh(1000, 1001, true));
    }

    @Test public void aPatchBuiltWithAnotherWallpaperCropIsNeverFresh() {
        assertFalse(HomePatchRules.compositeFresh(1000, 0, false));
    }

    @Test public void noPatchBuiltYetIsNotFresh() {
        assertFalse(HomePatchRules.compositeFresh(0, 0, true));
    }

    @Test public void aBuildIsDueOnlyWhenNothingFreshIsThereNothingIsBuildingAndTheHomeIsQuiet() {
        assertTrue(HomePatchRules.buildDue(10_000, 9_000, false, false));
        assertFalse("a fresh one exists", HomePatchRules.buildDue(10_000, 9_000, true, false));
        assertFalse("one is being built", HomePatchRules.buildDue(10_000, 9_000, false, true));
        assertFalse("the home drew 100 ms ago", HomePatchRules.buildDue(10_000, 9_900, false, false));
        assertTrue("exactly the quiet time", HomePatchRules.buildDue(10_000, 10_000 - HomePatchRules.DRAW_QUIET_MS, false, false));
        assertTrue("never drew", HomePatchRules.buildDue(10_000, 0, false, false));
    }

    @Test public void thePatchIsVisibleOnlyWhenRestableAndBothTimersAreOver() {
        long now = 100_000;
        assertTrue(HomePatchRules.visible(true, now, now - HomePatchRules.TOUCH_AWAY_MS, now - HomePatchRules.DRAW_QUIET_MS));
        assertFalse("not restable", HomePatchRules.visible(false, now, 0, 0));
        assertFalse("a finger went down 300 ms ago", HomePatchRules.visible(true, now, now - 300, 0));
        assertFalse("the home drew 100 ms ago", HomePatchRules.visible(true, now, 0, now - 100));
        assertTrue("never touched, never drew", HomePatchRules.visible(true, now, 0, 0));
    }

    @Test public void waitMsIsTheLongerOfTheTwoRemainingTimers() {
        long now = 100_000;
        assertEquals(0, HomePatchRules.waitMs(now, 0, 0));
        assertEquals(HomePatchRules.TOUCH_AWAY_MS, HomePatchRules.waitMs(now, now, 0));
        assertEquals(HomePatchRules.DRAW_QUIET_MS - 50, HomePatchRules.waitMs(now, 0, now - 50));
        assertEquals(HomePatchRules.TOUCH_AWAY_MS - 100, HomePatchRules.waitMs(now, now - 100, now - 50));
        assertEquals(0, HomePatchRules.waitMs(now, now - 5000, now - 5000));
    }

    @Test public void aClockThatWentBackwardsNeverHidesThePatchForEver() {
        assertTrue(HomePatchRules.visible(true, 1000, 5000, 6000));
        assertEquals(0, HomePatchRules.waitMs(1000, 5000, 6000));
        assertTrue(HomePatchRules.buildDue(1000, 6000, false, false));
    }

    // ---------------------------------------------------------------- which wallpaper crop

    @Test public void theWallpaperOffsetIsAStepNumber() {
        assertEquals(0, HomePatchRules.step(0f));
        assertEquals(500, HomePatchRules.step(0.5f));
        assertEquals(333, HomePatchRules.step(1f / 3f));
        assertEquals(1000, HomePatchRules.step(1f));
        assertEquals("out of range values are clamped", 1000, HomePatchRules.step(1.7f));
        assertEquals(0, HomePatchRules.step(-0.2f));
        assertEquals(0, HomePatchRules.step(Float.NaN));
    }

    @Test public void offsetsThatDifferInTheLastDigitsAreTheSamePosition() {
        assertTrue(HomePatchRules.sameStep(333, 334));
        assertTrue(HomePatchRules.sameStep(333, 331));
        assertFalse(HomePatchRules.sameStep(333, 336));
        assertFalse(HomePatchRules.sameStep(0, 333));
    }

    @Test public void theSignatureHasTheSameShapeAsTheLockScreensAndNamesTheFilePerRotationAndStep() {
        assertEquals("42|r0|600x1024", HomePatchRules.sig(42, 0, 600, 1024));
        assertEquals("homeshot_r0_x0.png", HomePatchRules.fileName(0, 0));
        assertEquals("homeshot_r1_x333.png", HomePatchRules.fileName(1, 333));
        assertEquals("home_shot_r0_x500", HomePatchRules.metaKey(0, 500));
    }

    @Test public void aStoredSignatureBelongsToThePictureAndTheDisplayWhateverTheRotation() {
        assertTrue(HomePatchRules.sigMatches("42|r0|600x1024", 42, 600, 1024));
        assertTrue(HomePatchRules.sigMatches("42|r1|600x1024", 42, 600, 1024));
        assertFalse("another wallpaper picture", HomePatchRules.sigMatches("41|r0|600x1024", 42, 600, 1024));
        assertTrue("the same display in the other rotation (the metrics swap): its crop must survive", HomePatchRules.sigMatches("42|r1|1024x600", 42, 600, 1024));
        assertFalse("another display size", HomePatchRules.sigMatches("42|r0|800x1280", 42, 600, 1024));
        assertFalse(HomePatchRules.sigMatches(null, 42, 600, 1024));
        assertFalse(HomePatchRules.sigMatches("garbage", 42, 600, 1024));
    }

    // ---------------------------------------------------------------- the calibration by itself

    @Test public void everythingQuietStartsTheCalibration() {
        assertEquals("", HomePatchRules.calDecision(calGood()));
    }

    @Test public void eachVetoStopsTheCalibrationAndTheFirstOneIsReported() {
        HomePatchRules.CalState s;
        s = calGood(); s.switchOn = false; assertEquals("switch off", HomePatchRules.calDecision(s));
        s = calGood(); s.coverMode = false; assertEquals("power menu mode is not 'hide Screenshot'", HomePatchRules.calDecision(s));
        s = calGood(); s.automationOff = true; assertEquals("automation paused", HomePatchRules.calDecision(s));
        s = calGood(); s.customMode = false; assertEquals("Quickstep", HomePatchRules.calDecision(s));
        s = calGood(); s.unlocked = false; assertEquals("lock screen", HomePatchRules.calDecision(s));
        s = calGood(); s.screenOn = false; assertEquals("screen off", HomePatchRules.calDecision(s));
        s = calGood(); s.permission = false; assertTrue(HomePatchRules.calDecision(s).startsWith("needs 'Allow wallpaper access'"));
        s = calGood(); s.running = true; assertEquals("a calibration is running", HomePatchRules.calDecision(s));
        s = calGood(); s.otherCalibration = true; assertEquals("the lock-screen calibration is running", HomePatchRules.calDecision(s));
        s = calGood(); s.hasCrop = true; assertEquals("already calibrated for this wallpaper and page", HomePatchRules.calDecision(s));
        s = calGood(); s.positionKnown = false; assertEquals("the wallpaper position is not known yet", HomePatchRules.calDecision(s));
        s = calGood(); s.positionKnown = false; s.hasCrop = true; assertEquals("said before 'already calibrated': nothing can be said for a position nobody reported", "the wallpaper position is not known yet", HomePatchRules.calDecision(s));
        s = calGood(); s.rectKnown = false; assertTrue(HomePatchRules.calDecision(s).startsWith("open the power menu once"));
        s = calGood(); s.homeAtRest = false; assertEquals("the home screen is not at rest", HomePatchRules.calDecision(s));
        s = calGood(); s.menuOpen = true; assertEquals("the power menu is open", HomePatchRules.calDecision(s));
    }

    @Test public void theCalibrationWaitsForTheFingerTheDrawsAndTheWake() {
        HomePatchRules.CalState s;
        s = calGood(); s.msSinceTouch = HomePatchRules.CAL_TOUCH_QUIET_MS - 1; assertEquals("touched a moment ago", HomePatchRules.calDecision(s));
        s = calGood(); s.msSinceTouch = HomePatchRules.CAL_TOUCH_QUIET_MS; assertEquals("", HomePatchRules.calDecision(s));
        s = calGood(); s.msSinceDraw = HomePatchRules.CAL_DRAW_QUIET_MS - 1; assertEquals("the home screen is still moving", HomePatchRules.calDecision(s));
        s = calGood(); s.msSinceScreenOn = HomePatchRules.CAL_SCREEN_ON_MS - 1; assertEquals("the screen came on a moment ago", HomePatchRules.calDecision(s));
    }

    @Test public void theCalibrationIsRateLimited() {
        HomePatchRules.CalState s = calGood();
        s.msSinceLastStart = HomePatchRules.CAL_MIN_GAP_MS - 1;
        assertEquals("tried a moment ago", HomePatchRules.calDecision(s));
        s = calGood(); s.startsToday = HomePatchRules.CAL_MAX_PER_DAY;
        assertEquals("tried " + HomePatchRules.CAL_MAX_PER_DAY + " times today", HomePatchRules.calDecision(s));
        s = calGood(); s.startsToday = HomePatchRules.CAL_MAX_PER_DAY - 1;
        assertEquals("", HomePatchRules.calDecision(s));
    }

    @Test public void aScreenshotIsOursWhenItIsNoOlderThanTheRequestMinusTheRoundingSlack() {
        long asked = 1_000_000_000_000L;
        assertTrue(HomePatchRules.shotIsOurs(asked / 1000, asked));
        assertTrue("a second early: the rounding", HomePatchRules.shotIsOurs(asked / 1000 - 1, asked));
        assertFalse("two seconds early = another screenshot (a hardware-key one just before): its icons must never be stored as 'wallpaper only'", HomePatchRules.shotIsOurs(asked / 1000 - 2, asked));
        assertTrue("a request in the middle of a second: the file is stamped with the second's start, 500 ms early", HomePatchRules.shotIsOurs(asked / 1000, asked + 500));
        assertFalse("an old one", HomePatchRules.shotIsOurs(asked / 1000 - 10, asked));
        assertFalse("no request made", HomePatchRules.shotIsOurs(asked / 1000, 0));
    }

    // ---------------------------------------------------------------- BJ1: the first window event of a power-menu burst (run #382: v0.8.9 treated it as NORMAL, the lock screen as START)

    @Test public void theFirstWindowEventOfABurstStartsAWaitWhenTheDialogsOwnEventWillOpenThePatch() {
        // this is the case v0.8.9 got wrong on the home screen: it did the node scan / layer refresh at once, on the thread the patch is drawn on, and the dialog's own event waited behind it
        assertEquals(HomePatchRules.BURST_START, HomePatchRules.burstTreatment(false, 1000, 0, true));
        assertEquals("a wait that ended long ago is no wait", HomePatchRules.BURST_START, HomePatchRules.burstTreatment(false, 1000, 400, true));
    }

    @Test public void anEventThatCannotOpenThePatchIsTreatedAsBefore() {
        assertEquals("not a window event, or the fast path is not possible (the patch is stale, the geometry unknown ...): the card path scans at once", HomePatchRules.BURST_NORMAL, HomePatchRules.burstTreatment(false, 1000, 0, false));
    }

    @Test public void everyEventInsideARunningWaitIsQuiet() {
        assertEquals("the wait the first event started", HomePatchRules.BURST_QUIET, HomePatchRules.burstTreatment(false, 1050, 1120, true));
        assertEquals("... also for an event that would not start one", HomePatchRules.BURST_QUIET, HomePatchRules.burstTreatment(false, 1050, 1120, false));
        assertEquals("the wait the open menu's own event set (the stock fade is being followed)", HomePatchRules.BURST_QUIET, HomePatchRules.burstTreatment(true, 1100, 1330, false));
    }

    @Test public void theWaitEndsExactlyAtItsEnd() {
        assertEquals(HomePatchRules.BURST_QUIET, HomePatchRules.burstTreatment(true, 1329, 1330, false));
        assertEquals(HomePatchRules.BURST_NORMAL, HomePatchRules.burstTreatment(true, 1330, 1330, false));
        assertEquals(HomePatchRules.BURST_START, HomePatchRules.burstTreatment(false, 1120, 1120, true));
    }

    @Test public void anOpenMenuNeverStartsAnotherWait() {
        assertEquals("once our patch is up the stock dialog's event has been handled: nothing to wait for", HomePatchRules.BURST_NORMAL, HomePatchRules.burstTreatment(true, 2000, 0, true));
    }

    // ---------------------------------------------------------------- BN2: the keyguard's own window at the unlock is not a shade (tablet report 2026-11-09: the 2.5 s guard it started made the first press uncovered)
    @Test public void onlyTheStatusBarWindowCanBeTheKeyguardGoingAway() {
        assertEquals(true, HomePatchRules.isKeyguardWindow("com.android.systemui", "android.widget.FrameLayout"));
        assertEquals("the volume panel and every other SystemUI Dialog announce themselves with their own class", false, HomePatchRules.isKeyguardWindow("com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl$CustomDialog"));
        assertEquals(false, HomePatchRules.isKeyguardWindow("com.android.settings", "android.widget.FrameLayout"));
        assertEquals(false, HomePatchRules.isKeyguardWindow("com.android.systemui", ""));
    }
    @Test public void theKeyguardWindowRightAfterTheKeyguardWasUpGetsAProvisionalGuard() {
        assertEquals(HomePatchRules.FOREIGN_UNLOCK_CHECK, HomePatchRules.foreignTreatment(true, 0));
        assertEquals(HomePatchRules.FOREIGN_UNLOCK_CHECK, HomePatchRules.foreignTreatment(true, 600));
        assertEquals(HomePatchRules.FOREIGN_UNLOCK_CHECK, HomePatchRules.foreignTreatment(true, HomePatchRules.UNLOCK_GRACE_MS - 1));
    }
    @Test public void theSameWindowLongAfterTheUnlockIsTheShadeAndGuardsForGood() {
        assertEquals(HomePatchRules.FOREIGN_NOW, HomePatchRules.foreignTreatment(true, HomePatchRules.UNLOCK_GRACE_MS));
        assertEquals(HomePatchRules.FOREIGN_NOW, HomePatchRules.foreignTreatment(true, 60_000));
        assertEquals("the keyguard was never seen up in this process", HomePatchRules.FOREIGN_NOW, HomePatchRules.foreignTreatment(true, Long.MAX_VALUE));
    }
    @Test public void aDialogOrAnotherAppsWindowIsAlwaysGuardedForGoodEvenRightAfterTheUnlock() {
        assertEquals(HomePatchRules.FOREIGN_NOW, HomePatchRules.foreignTreatment(false, 0));
        assertEquals(HomePatchRules.FOREIGN_NOW, HomePatchRules.foreignTreatment(false, 1000));
    }
    @Test public void theRecheckLiftsTheGuardOnlyWhenTheLauncherKeptTheFocusAndNoOtherWindowTouchedTheGuard() {
        assertEquals("the Launcher still has the focus: it was the keyguard going away", HomePatchRules.RECHECK_LIFT, HomePatchRules.recheck(true, 5000, 5000));
        assertEquals("the shade took the focus: a real window, the guard stays", HomePatchRules.RECHECK_KEEP, HomePatchRules.recheck(false, 5000, 5000));
    }
    @Test public void theRecheckNeverLiftsAGuardAnotherWindowSetOrExtended() {
        assertEquals("a later window moved the guard end", HomePatchRules.RECHECK_NOT_OURS, HomePatchRules.recheck(true, 5300, 5000));
        assertEquals("the guard is final (no provisional event)", HomePatchRules.RECHECK_NOT_OURS, HomePatchRules.recheck(true, 5000, 0));
        assertEquals(HomePatchRules.RECHECK_NOT_OURS, HomePatchRules.recheck(false, 5300, 5000));
    }
    @Test public void aRecheckWithoutFocusLooksAgainUntilTheGuardEndsThenCountsTheWindow() {
        assertEquals("a slow resume after the unlock: the focus may come at +900 ms", true, HomePatchRules.recheckAgain(1400, 3500));
        assertEquals(true, HomePatchRules.recheckAgain(3500 - HomePatchRules.FOREIGN_RECHECK_MS - 1, 3500));
        assertEquals("less than one more look fits before the guard ends: it is a real window now", false, HomePatchRules.recheckAgain(3500 - HomePatchRules.FOREIGN_RECHECK_MS, 3500));
        assertEquals(false, HomePatchRules.recheckAgain(4000, 3500));
    }
    @Test public void theGraceRecheckIsShorterThanTheGuardItMayLift() {
        assertEquals(true, HomePatchRules.FOREIGN_RECHECK_MS < 2500);
    }

    // ---- BT1c / BT1d (Tj, 2026-11-11): the stock Screenshot entry is never left uncovered when the patch is not fresh at the press
    private static HomePatchRules.OpenFacts open(boolean window, boolean restable, boolean kept, boolean step, boolean focus, boolean rest, boolean cal) {
        HomePatchRules.OpenFacts f = new HomePatchRules.OpenFacts();
        f.patchWindow = window; f.restable = restable; f.compositeKept = kept; f.stepMatches = step; f.launcherFocus = focus; f.homeAtRest = rest; f.calibrating = cal;
        return f;
    }
    @Test public void aFreshPatchOpensTheFastWayAsEver() {
        assertEquals(HomePatchRules.OPEN_FRESH, HomePatchRules.openTreatment(open(true, true, true, true, true, true, false)));
    }
    @Test public void aStalePatchIsShownAsItIsWhenTheLauncherHasTheFocusOnThePlainHome() {
        // emulator run 529: a draw since the build, no shade at all, the press found "the patch is not resting" and the Screenshot entry showed
        assertEquals(HomePatchRules.OPEN_STALE, HomePatchRules.openTreatment(open(true, false, true, true, true, true, false)));
    }
    @Test public void withoutTheFocusAFolderOrADragTheCardCoversTheWholePanel() {
        assertEquals("the shade (or another window) holds the focus: the Launcher's pixels are not what is under the slot", HomePatchRules.OPEN_FULL_CARD, HomePatchRules.openTreatment(open(true, false, true, true, false, true, false)));
        assertEquals("a folder / popup / drag / All Apps", HomePatchRules.OPEN_FULL_CARD, HomePatchRules.openTreatment(open(true, false, true, true, true, false, false)));
        assertEquals("the focus decides even for a restable patch's neighbours: no window, no patch", HomePatchRules.OPEN_FULL_CARD, HomePatchRules.openTreatment(open(false, false, true, true, true, true, false)));
    }
    @Test public void aCompositeForAnotherWallpaperPositionOrNoCompositeOrACalibrationNeverShows() {
        assertEquals(HomePatchRules.OPEN_FULL_CARD, HomePatchRules.openTreatment(open(true, false, true, false, true, true, false)));
        assertEquals(HomePatchRules.OPEN_FULL_CARD, HomePatchRules.openTreatment(open(true, false, false, true, true, true, false)));
        assertEquals("the Launcher is hidden for the screenshot", HomePatchRules.OPEN_FULL_CARD, HomePatchRules.openTreatment(open(true, true, true, true, true, true, true)));
    }
    @Test public void theLogWordsNameTheFirstReasonAndTheNumbers() {
        assertEquals("the Launcher drew 700 ms after the build [built 1000 ms ago, last draw 300 ms ago, guard 0 ms left, focus true]", HomePatchRules.notFreshWhy(true, true, 1000, 300, 0, true, false));
        assertEquals("a window guard is running [built 1000 ms ago, last draw 5000 ms ago, guard 1800 ms left, focus true]", HomePatchRules.notFreshWhy(true, true, 1000, 5000, 1800, true, false));
        assertEquals("the Launcher has no window focus [built 10 ms ago, last draw never, guard 0 ms left, focus false]", HomePatchRules.notFreshWhy(true, true, 10, -1, 0, false, false));
        assertEquals("no composite built yet [built never, last draw never, guard 0 ms left, focus true]", HomePatchRules.notFreshWhy(false, false, -1, -1, -5, true, false));
        assertEquals("calibrating [built never, last draw never, guard 0 ms left, focus true]", HomePatchRules.notFreshWhy(false, false, -1, -1, 0, true, true));
    }
}
