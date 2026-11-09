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
        assertEquals(true, PencilRules.hideCover(PencilRules.LOST, false, false));
    }

    @Test public void aSearchThatFoundNothingHidesTheCover() {
        assertEquals(true, PencilRules.hideCover(PencilRules.NO_KNOWN, true, false));
    }

    @Test public void aPencilFoundByTheSearchKeepsTheCoverEvenAfterALoss() {
        assertEquals("lost, but the prompt search found it again: re-covered, not hidden", false, PencilRules.hideCover(PencilRules.LOST, true, true));
    }

    @Test public void whenNothingCouldBeCheckedTheCoverIsLeftAlone() {
        assertEquals("no node, no search allowed: no answer, no change (the old rule for a busy burst)", false, PencilRules.hideCover(PencilRules.NO_KNOWN, false, false));
        assertEquals(false, PencilRules.hideCover(PencilRules.FOUND, false, false));
    }

    @Test public void theSpacingGapIsWaivedOnlyAfterALoss() {
        assertEquals(true, PencilRules.searchGapWaived(PencilRules.LOST));
        assertEquals(false, PencilRules.searchGapWaived(PencilRules.FOUND));
        assertEquals(false, PencilRules.searchGapWaived(PencilRules.NO_KNOWN));
    }

    @Test public void theWatchdogIsFasterThanTheOldSearchGapPlusASearch() {
        assertEquals(true, PencilRules.WATCH_MS < EventNoise.minPencilSearchGapMs(0x00000800) + 50);   // 0x800 = TYPE_WINDOW_CONTENT_CHANGED
    }
}
