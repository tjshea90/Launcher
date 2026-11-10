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
}
