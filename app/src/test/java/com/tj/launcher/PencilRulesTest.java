package com.tj.launcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** BN4: the cover over the quick-settings pencil must go the moment the pencil it follows is gone (tablet report 2026-11-09: a white box on the lock screen after the shade slid up). */
public class PencilRulesTest {
    @Test public void aVisiblePencilWithSaneBoundsIsFollowed() {
        assertEquals(PencilRules.FOUND, PencilRules.follow(true, true, true, true));
    }

    @Test public void eachWayTheFollowedNodeCanFailIsALoss() {
        assertEquals("an INVISIBLE view cannot be refreshed at all", PencilRules.LOST, PencilRules.follow(true, false, false, false));
        assertEquals("refresh failed", PencilRules.LOST, PencilRules.follow(true, false, true, true));
        assertEquals("alpha 0: alive but not visible to the user", PencilRules.LOST, PencilRules.follow(true, true, false, true));
        assertEquals("bounds of nothing / of a whole panel", PencilRules.LOST, PencilRules.follow(true, true, true, false));
    }

    @Test public void noKnownNodeIsNotALoss() {
        assertEquals(PencilRules.NO_KNOWN, PencilRules.follow(false, false, false, false));
        assertEquals(PencilRules.NO_KNOWN, PencilRules.follow(false, true, true, true));
    }

    @Test public void aLostPencilHidesTheCoverAtOnceWithoutAnySearch() {
        assertEquals(true, PencilRules.hideCover(PencilRules.LOST, false));
    }

    @Test public void aSearchThatFoundNothingHidesTheCover() {
        assertEquals(true, PencilRules.hideCover(PencilRules.NO_KNOWN, true));
        assertEquals(true, PencilRules.hideCover(PencilRules.LOST, true));
    }

    @Test public void whenNothingCouldBeCheckedTheCoverIsLeftAlone() {
        assertEquals("no node, no search allowed: no answer, no change (the old rule for a busy burst)", false, PencilRules.hideCover(PencilRules.NO_KNOWN, false));
        assertEquals(false, PencilRules.hideCover(PencilRules.FOUND, false));
    }

    @Test public void theSpacingGapIsWaivedOnlyAfterALossThatNoSearchAnswered() {
        assertEquals(true, PencilRules.searchGapWaived(PencilRules.LOST, false));
        assertEquals("a search that already found nothing in this call is the answer: no second one 30 ms later", false, PencilRules.searchGapWaived(PencilRules.LOST, true));
        assertEquals(false, PencilRules.searchGapWaived(PencilRules.FOUND, false));
        assertEquals(false, PencilRules.searchGapWaived(PencilRules.NO_KNOWN, false));
    }

    // ---- BN8b: the cover must not trail a panel that slides up
    @Test public void theCoverIsShownAndFollowedWhileThePanelIsAtRestOrOpening() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        assertEquals("the first look of a pull", true, g.allowCover(300, 1000));
        assertEquals("opening: the pencil goes down, the box trails inside the white panel", true, g.allowCover(380, 1040));
        assertEquals(true, g.allowCover(460, 1080));
        assertEquals("at rest", true, g.allowCover(460, 1120));
        assertEquals("jitter of a pixel or two is not a slide", true, g.allowCover(458, 1160));
        assertEquals(false, g.holding());
    }

    @Test public void thePencilGoingUpTakesTheCoverAwayAndItComesBackWhenTheLooksAgree() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        assertEquals(true, g.allowCover(460, 1000));
        assertEquals("the panel starts to slide up: no cover", false, g.allowCover(430, 1040));
        assertEquals(true, g.holding());
        assertEquals("still moving up", false, g.allowCover(380, 1080));
        assertEquals("the finger lets go: two looks agree -> back at the new rest place", true, g.allowCover(380, 1140));
        assertEquals(false, g.holding());
        assertEquals(true, g.allowCover(380, 1300));
    }

    @Test public void aReversalWaitsUntilThePanelHasSettled() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(460, 1000);
        assertEquals(false, g.allowCover(400, 1040));      // up
        assertEquals("now it goes down again: still moving, still no cover", false, g.allowCover(420, 1080));
        assertEquals(false, g.allowCover(450, 1120));
        assertEquals(true, g.allowCover(450, 1180));        // at rest
    }

    @Test public void aLongGapMeansANewPullAndANewGuard() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(460, 1000);
        assertEquals(false, g.allowCover(400, 1040));      // sliding up, held
        assertEquals("the next pull of the panel (no look for more than STALE_MS) starts clean, even higher up", true, g.allowCover(300, 1040 + PencilRules.SlideGuard.STALE_MS + 1));
        assertEquals(false, g.holding());
    }

    @Test public void aTransientLossDoesNotForgetTheSlide() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(460, 1000);
        assertEquals(false, g.allowCover(420, 1040));
        // the pencil node is lost for 200 ms (a failed refresh) and found again, higher: still a slide, still no cover
        assertEquals(false, g.allowCover(330, 1240));
        assertEquals(true, g.holding());
    }

    @Test public void theCoverIsNeverKeptAwayForLongerThanTheSafetyValve() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(900, 0);
        int top = 900; long t = 0; boolean shownAgain = false;
        for (int i = 0; i < 100 && !shownAgain; i++) { top -= 8; t += 40; shownAgain = g.allowCover(top, t); }   // a pencil that creeps up for ever
        assertEquals("released by the valve, not by stillness", true, shownAgain);
        assertEquals(true, t <= PencilRules.SlideGuard.HOLD_MAX_MS + 40);
    }

    @Test public void resetForgetsEverything() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(460, 1000);
        g.allowCover(400, 1040);
        assertEquals(true, g.holding());
        g.reset();
        assertEquals(false, g.holding());
        assertEquals(true, g.allowCover(100, 1050));
    }

    // ---- BN8f2: two looks closer than MIN_LOOK_GAP_MS can read the same SystemUI frame (the 60 ms re-look can land 10 ms after an event look)
    @Test public void aLookRightAfterAnotherDoesNotConfirmThatThePanelIsAtRest() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(460, 1000);
        assertEquals(false, g.allowCover(400, 1040));      // up: held, anchor (400, 1040)
        assertEquals("10 ms later, the same top: probably the same frame, not a second look", false, g.allowCover(400, 1050));
        assertEquals("30 ms after the anchor: still too close", false, g.allowCover(400, 1070));
        assertEquals(true, g.holding());
        assertEquals("40 ms after the anchor: a real second look, the panel is at rest", true, g.allowCover(400, 1080));
        assertEquals(false, g.holding());
    }

    @Test public void aCloseLookThatShowsAMoveUpStillHoldsAndMovesTheAnchor() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        g.allowCover(460, 1000);
        assertEquals(false, g.allowCover(400, 1040));
        assertEquals("20 px higher 10 ms later: still sliding", false, g.allowCover(380, 1050));
        assertEquals("the anchor is now (380, 1050): 30 ms later is too close to count", false, g.allowCover(380, 1080));
        assertEquals("and 50 ms after the new anchor the looks agree", true, g.allowCover(380, 1100));
    }

    @Test public void aCloseLookWhileThePanelOpensIsStillFollowed() {
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        assertEquals(true, g.allowCover(300, 1000));
        assertEquals("opening, 10 ms later: followed as before", true, g.allowCover(320, 1010));
        assertEquals(false, g.holding());
    }

    @Test public void theWatchdogIsFasterThanTheOldSearchGapPlusASearch() {
        assertEquals(true, PencilRules.WATCH_MS < EventNoise.minPencilSearchGapMs(0x00000800) + 50);   // 0x800 = TYPE_WINDOW_CONTENT_CHANGED
    }

    // ---- BN8b, second half: the fast watch
    @Test public void theWatchIsFastOnlyInsideTheFastWindow() {
        assertEquals(PencilRules.WATCH_MS, PencilRules.watchGapMs(1000, 0));                                  // never asked
        assertEquals(PencilRules.FAST_WATCH_MS, PencilRules.watchGapMs(1000, 1000 + PencilRules.FAST_FOR_MS)); // just asked
        assertEquals(PencilRules.FAST_WATCH_MS, PencilRules.watchGapMs(1599, 1600));                          // the last millisecond of the window
        assertEquals(PencilRules.WATCH_MS, PencilRules.watchGapMs(1600, 1600));                               // the window is over
        assertEquals(PencilRules.WATCH_MS, PencilRules.watchGapMs(5000, 1600));
    }

    @Test public void aDeadlineThatMakesNoSenseCountsAsSlow() {
        assertEquals(PencilRules.WATCH_MS, PencilRules.watchGapMs(1000, 1000 + PencilRules.FAST_FOR_MS + 1));   // further away than a window can be: a clock jump, not a request
        assertEquals(PencilRules.WATCH_MS, PencilRules.watchGapMs(0, Long.MAX_VALUE));
    }

    @Test public void theFastWatchSeesASlideWithinTwoFramesAndIsMuchFasterThanAnEvent() {
        assertEquals(true, PencilRules.FAST_WATCH_MS <= 34);                           // two 60 Hz frames
        assertEquals(true, PencilRules.FAST_WATCH_MS * 4 <= 100);                      // SystemUI sends content events at most every 100 ms
        assertEquals(true, PencilRules.FAST_FOR_MS >= 2 * PencilRules.WATCH_MS);       // a finger that goes down and waits a moment before it drags is still watched fast
    }

    @Test public void aSlowStartOfASlideIsCaughtAtTheNextFastLook() {
        // the first look sees the pencil only 2 px higher (jitter: the cover follows); 25 ms later it is 14 px higher: the cover goes
        PencilRules.SlideGuard g = new PencilRules.SlideGuard();
        assertEquals(true, g.allowCover(700, 1000));
        assertEquals(true, g.allowCover(698, 1000 + PencilRules.FAST_WATCH_MS));
        assertEquals(false, g.allowCover(684, 1000 + 2 * PencilRules.FAST_WATCH_MS));
        assertEquals(true, g.holding());
    }

    // ---- BQ1b (Tj, 2026-11-10, v0.9.1, the fifth report: "The pencil white cover shows when I slide the notification panel back up")
    @Test public void withNoFingerTheCoverIsNeverHeldByTheFingerGuard() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        assertEquals(true, g.allowCover(1000)); assertEquals(false, g.waiting());
        g.systemUiEvent(1000);                       // an event with no finger before it counts for nothing
        assertEquals(true, g.allowCover(1001));
    }

    @Test public void aFingerKeepsTheCoverAwayForTheMinimumWait() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        g.fingerDown(10_000);
        assertEquals(true, g.waiting());
        for (long dt : new long[] { 0, 1, 100, 200, PencilRules.FingerGuard.HOLD_MS - 1 }) assertEquals("at +" + dt, false, g.allowCover(10_000 + dt));
        assertEquals("the wait is not over until a look says so", true, g.waiting());
        assertEquals(true, g.allowCover(10_000 + PencilRules.FingerGuard.HOLD_MS));
        assertEquals("the first true ends the wait", false, g.waiting());
        assertEquals(true, g.allowCover(10_000 + PencilRules.FingerGuard.HOLD_MS + 5));
    }

    @Test public void systemUiRedrawingKeepsTheCoverAwayUntilItHasBeenQuiet() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        g.fingerDown(10_000);
        long t = 10_000;
        for (int i = 0; i < 20; i++) { t += 100; g.systemUiEvent(t); }                 // a drag: SystemUI keeps redrawing for 2 s
        assertEquals(false, g.allowCover(t + 1));
        assertEquals(false, g.allowCover(t + PencilRules.FingerGuard.QUIET_MS - 1));
        assertEquals(true, g.allowCover(t + PencilRules.FingerGuard.QUIET_MS));
    }

    @Test public void aFingerParkedOnThePanelNeverKeepsTheCoverAwayForEver() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        g.fingerDown(10_000);
        g.systemUiEvent(10_000 + PencilRules.FingerGuard.MAX_MS - 50);                  // still redrawing right at the end
        assertEquals(false, g.allowCover(10_000 + PencilRules.FingerGuard.MAX_MS - 10));
        assertEquals(true, g.allowCover(10_000 + PencilRules.FingerGuard.MAX_MS));      // the safety valve
        assertEquals(false, g.waiting());
    }

    @Test public void aSecondFingerRestartsTheWait() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        g.fingerDown(10_000);
        assertEquals(false, g.allowCover(10_300));
        g.fingerDown(10_300);                                                             // another touch on the screen
        assertEquals(false, g.allowCover(10_000 + PencilRules.FingerGuard.HOLD_MS + 10));     // would have been over after the first finger
        assertEquals(true, g.allowCover(10_300 + PencilRules.FingerGuard.HOLD_MS + PencilRules.FingerGuard.QUIET_MS));
    }

    @Test public void aClockThatWentBackwardsNeverHoldsTheCover() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        g.fingerDown(10_000);
        assertEquals(true, g.allowCover(5_000));
        assertEquals(false, g.waiting());
    }

    @Test public void resetForgetsTheFinger() {
        PencilRules.FingerGuard g = new PencilRules.FingerGuard();
        g.fingerDown(10_000); g.reset();
        assertEquals(true, g.allowCover(10_001));
    }

    @Test public void theMinimumWaitIsLongerThanOneFastLookAndShorterThanASecond() {
        assertEquals(true, PencilRules.FingerGuard.HOLD_MS > PencilRules.FAST_WATCH_MS * 4);       // the first look comes after the panel has had time to start moving
        assertEquals(true, PencilRules.FingerGuard.HOLD_MS + PencilRules.FingerGuard.QUIET_MS < 1000);   // the pencil is exposed for well under a second after a tap
        assertEquals(true, PencilRules.FingerGuard.MAX_MS >= PencilRules.SlideGuard.HOLD_MAX_MS);
    }

    @Test public void theTwoGuardsTogetherKeepAColdSlideAwayAndLetARestingPanelBack() {
        // the service feeds BOTH guards at every look: a slide up keeps the cover away through the SlideGuard even after the finger's wait is over; a panel at rest gets it back
        PencilRules.SlideGuard slide = new PencilRules.SlideGuard(); PencilRules.FingerGuard finger = new PencilRules.FingerGuard();
        long t = 20_000;
        assertEquals(true, slide.allowCover(700, t) && finger.allowCover(t));                 // the panel is open and at rest, the cover is up
        finger.fingerDown(t + 10);                                                            // the finger goes down: the service hides the cover without any look
        t += 400;                                                                             // the first look after the minimum wait: the pencil is 60 px higher
        boolean rest = slide.allowCover(640, t), fing = finger.allowCover(t);
        assertEquals(false, rest && fing);
        t += 60; rest = slide.allowCover(560, t); fing = finger.allowCover(t); assertEquals(false, rest && fing);       // still sliding
        t += 60; rest = slide.allowCover(560, t); fing = finger.allowCover(t); assertEquals("two looks agree: at rest", true, rest && fing);
    }
}
