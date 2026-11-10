package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** BR1: the overlay over the quick-settings gear (Tj, 2026-11-10: the gear opens the Settings look-alike). */
public class GearRulesTest {
    @Test public void releaseDefaultsOnAndDebugDefaultsOff() {
        assertTrue(GearRules.defaultOn(false));
        assertFalse(GearRules.defaultOn(true));
    }

    @Test public void theOverlayIsWantedOnlyInTheCustomLauncherOnAnUnlockedTabletWithBothSwitchesOn() {
        assertTrue(GearRules.wanted(true, true, false, true));
        assertFalse("switch off", GearRules.wanted(false, true, false, true));
        assertFalse("Quickstep keeps the real gear", GearRules.wanted(true, false, false, true));
        assertFalse("the lock screen keeps the real gear (it asks for the PIN)", GearRules.wanted(true, true, true, true));
        assertFalse("no look-alike, nothing to open", GearRules.wanted(true, true, false, false));
        assertFalse("lock screen in Quickstep", GearRules.wanted(true, false, true, true));
    }

    @Test public void notWantedRemovesTheWindowWhateverElseIsTrue() {
        assertEquals(GearRules.REMOVE, GearRules.decide(false, true, true, true));
        assertEquals(GearRules.REMOVE, GearRules.decide(false, false, false, false));
    }

    @Test public void aCoverThatIsAwayHidesTheOverlay() {
        assertEquals(GearRules.HIDE, GearRules.decide(true, false, true, true));
        assertEquals("even before anything was learned", GearRules.HIDE, GearRules.decide(true, false, false, false));
    }

    @Test public void anUnknownOrStaleOffsetIsLearnedBeforeTheOverlayShows() {
        assertEquals(GearRules.LEARN, GearRules.decide(true, true, false, false));
        assertEquals("learned under another screen (rotation, density)", GearRules.LEARN, GearRules.decide(true, true, true, false));
    }

    @Test public void aKnownOffsetUnderTheSameScreenShowsTheOverlay() {
        assertEquals(GearRules.SHOW, GearRules.decide(true, true, true, true));
    }

    @Test public void boundsOfAButtonAreOkAndOthersAreNot() {
        assertTrue(GearRules.boundsOk(new int[] {1500, 600, 1596, 696}));
        assertFalse("empty", GearRules.boundsOk(new int[] {10, 10, 10, 40}));
        assertFalse("inverted", GearRules.boundsOk(new int[] {50, 10, 40, 40}));
        assertFalse("a whole panel", GearRules.boundsOk(new int[] {0, 0, 1200, 800}));
        assertFalse("null", GearRules.boundsOk(null));
        assertFalse("wrong length", GearRules.boundsOk(new int[] {1, 2, 3}));
    }

    @Test public void theOffsetSurvivesTheFooterMoving() {
        int[] pencil = {40, 700, 136, 796};
        int[] gear = {1000, 700, 1096, 796};
        int[] off = GearRules.offset(pencil, gear);
        assertArrayEquals(new int[] {960, 0, 960, 0}, off);
        assertArrayEquals("same place", gear, GearRules.place(pencil, off));
        int[] movedPencil = {40, 520, 136, 616};   // the panel slid up by 180 px
        assertArrayEquals(new int[] {1000, 520, 1096, 616}, GearRules.place(movedPencil, off));
    }

    @Test public void aDifferentSizedGearKeepsItsOwnSize() {
        int[] pencil = {40, 700, 136, 796};
        int[] gear = {1010, 706, 1090, 790};   // smaller, inside the same row
        int[] back = GearRules.place(pencil, GearRules.offset(pencil, gear));
        assertArrayEquals(gear, back);
    }

    @Test public void theKeyChangesWithTheScreen() {
        String a = GearRules.key(1200, 1920, 213, 0);
        assertEquals(a, GearRules.key(1200, 1920, 213, 0));
        assertFalse("rotation", a.equals(GearRules.key(1920, 1200, 213, 1)));
        assertFalse("display size (density)", a.equals(GearRules.key(1200, 1920, 160, 0)));
    }

    @Test public void aTapStaysCloseAndASwipeDoesNot() {
        assertTrue(GearRules.isTap(100, 100, 100, 100, 8));
        assertTrue("a little finger roll", GearRules.isTap(100, 100, 108, 104, 8));
        assertFalse("a swipe up from the gear", GearRules.isTap(100, 100, 100, 40, 8));
        assertTrue("exactly twice the slop is still a tap", GearRules.isTap(0, 0, 16, 0, 8));
        assertFalse("just over it", GearRules.isTap(0, 0, 17, 0, 8));
    }

    @Test public void sameComparesAllFourEdges() {
        assertTrue(GearRules.same(new int[] {1, 2, 3, 4}, new int[] {1, 2, 3, 4}));
        assertFalse(GearRules.same(new int[] {1, 2, 3, 4}, new int[] {1, 2, 3, 5}));
        assertFalse(GearRules.same(null, new int[] {1, 2, 3, 4}));
    }

    @Test public void theStatusLineHasAClockAndAText() {
        assertEquals("19:30:01  opened the look-alike", GearRules.statusLine("19:30:01", "opened the look-alike"));
    }
}
