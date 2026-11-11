package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** AE3 / AD7b: the lock-screen patch rests visible over the Screenshot slot, but never over the PIN pad, and never at home. */
public class PatchRestTest {
    @Test public void theSwitchKeyIsStable() { assertEquals("lock_patch_always", PatchRest.KEY); }

    @Test public void switchOffKeepsTheOldBehaviourInvisibleUntilTheMenuOpens() {
        assertFalse(PatchRest.visible(false, false, true, false));
        assertTrue(PatchRest.visible(true, false, true, false));
    }
    @Test public void switchOnRestsVisibleOnTheLockScreen() { assertTrue(PatchRest.visible(false, true, true, false)); }
    @Test public void neverRestsOverThePinPad() {
        assertFalse(PatchRest.visible(false, true, true, true));
        assertTrue("an open power menu still gets its patch over the pad", PatchRest.visible(true, true, true, true));
    }
    @Test public void theHomeScreenPatchNeverRests() {
        assertFalse(PatchRest.visible(false, true, false, false));
        assertTrue(PatchRest.visible(true, true, false, false));
    }

    @Test public void theRestingPatchPaintsOnlyBelowTheCard() {
        assertEquals(150, PatchRest.restTop(100, 250, 400));
        assertEquals(0, PatchRest.restTop(100, 100, 400));
    }
    @Test public void theOpenMenuScrimIsTheStockValueUntilTjNudgesIt() {
        assertEquals(0.79f, PatchRest.openScrim(0.79f, 0), 0.0001f);
        assertEquals(0.79f * 0.9f, PatchRest.openScrim(0.79f, -10), 0.0001f);
        assertEquals(1f, PatchRest.openScrim(0.79f, 40), 0.0001f);     // never brighter than the screen itself
        assertEquals(0.3f, PatchRest.openScrim(0.79f, -90), 0.0001f);  // never black
    }
    @Test public void aCardOutsideTheWindowIsClamped() {
        assertEquals(0, PatchRest.restTop(100, 40, 400));        // card bottom above the window: paint it all
        assertEquals(300, PatchRest.restTop(100, 900, 400));     // card bottom below the window: paint nothing
        assertEquals(0, PatchRest.restTop(400, 500, 300));       // an empty / inverted window never goes negative
    }

    // ---- AH3 (v0.7.6): the slot-only patch with soft edges
    @Test public void theSlotWindowIsThePanelPlusTheMarginFromOneFeatherAboveTheCardBottom() {
        int[] w = PatchRest.slotWindow(480, 700, 585, 830, 2, 8, 600, 1024);   // learned panel x 480..585, card bottom 700, panel bottom 830
        assertEquals(470, w[0]); assertEquals(692, w[1]); assertEquals(595, w[2]); assertEquals(840, w[3]);
    }
    @Test public void theSlotWindowIsClampedToTheDisplay() {
        int[] w = PatchRest.slotWindow(495, 700, 600, 1020, 2, 8, 600, 1024);   // panel flush with the right edge, close to the bottom
        assertEquals(600, w[2]); assertEquals(1024, w[3]);
        int[] z = PatchRest.slotWindow(4, 3, 100, 200, 2, 8, 600, 1024);        // close to the top left corner: never negative
        assertEquals(0, z[0]); assertEquals(0, z[1]);
    }
    // ---- BB1 (v0.8.3): the opaque core reaches further DOWN (the stock panel's shadow), within what the stored crop covers
    @Test public void theExtendedWindowIsTallerAtTheBottomOnly() {
        int[] old = PatchRest.slotWindow(480, 700, 585, 830, 2, 8, 600, 1024);
        int[] ext = PatchRest.slotWindow(480, 700, 585, 830, 2, 8, 20, 8, 600, 1024);
        assertEquals(old[0], ext[0]); assertEquals(old[1], ext[1]); assertEquals(old[2], ext[2]);   // left, top, right: untouched
        assertEquals(old[3] + 20, ext[3]);                                                                // 830 + 2 + 20 + 8
        assertEquals(860, ext[3]);
    }
    @Test public void theOldSignatureIsTheNoExtensionGeometry() {
        for (int[] a : new int[][] { { 480, 700, 585, 830 }, { 495, 700, 600, 1020 }, { 4, 3, 100, 200 } })
            assertArrayEquals(PatchRest.slotWindow(a[0], a[1], a[2], a[3], 2, 8, 600, 1024), PatchRest.slotWindow(a[0], a[1], a[2], a[3], 2, 8, 0, 8, 600, 1024));
    }
    @Test public void theExtendedWindowIsStillClampedToTheDisplay() {
        assertEquals(1024, PatchRest.slotWindow(495, 700, 600, 1015, 2, 8, 20, 8, 600, 1024)[3]);
    }
    @Test public void theExtensionNeverLeavesTheStoredCrop() {
        // the crop was cut for the OLD window + margin; whatever the wish (and the bottom fringe), the new window's bottom stays inside it with the slack to spare
        int margin = 24, e = 2, f = 9;
        for (int wish = -5; wish <= 60; wish++) for (int bf = 0; bf <= 24; bf++) {
            int x = PatchRest.clampExtra(wish, bf, f, margin);
            assertTrue("never negative", x >= 0);
            int oldBottom = 830 + e + f, crop = oldBottom + margin, newBottom = 830 + e + x + bf;
            if (x > 0) assertTrue("wish " + wish + " bf " + bf + " -> " + x, newBottom <= crop - PatchRest.CROP_SLACK_PX);
        }
    }
    @Test public void theDefaultExtensionFitsTheDefaultFringeAtTheTabletsDensity() {
        int px = Math.round(PatchRest.EXTRA_BOTTOM_DP * 1.0625f), bf = Math.round(PatchRest.BOTTOM_FEATHER_DP * 1.0625f), f = Math.round(PatchRest.FEATHER_DP * 1.0625f);
        assertEquals(26, px); assertEquals(4, bf); assertEquals(9, f);
        assertEquals(px, PatchRest.clampExtra(px, bf, f, 24));  // 26 px: not clamped at 170 dpi (the shorter bottom fringe pays for it)
        assertEquals(Math.max(0, 2 - PatchRest.CROP_SLACK_PX + (f - bf)), PatchRest.clampExtra(px, bf, f, 2));   // a tiny crop margin: only what the shorter fringe frees (margin - slack + (fringe - bottom fringe))
        int[] w = PatchRest.slotWindow(480, 700, 585, 830, 2, f, px, bf, 600, 1024);
        assertEquals(830 + 2 + 26 + 4, w[3]);                    // the window bottom: 32 px under the panel, inside the crop (35 allowed, 3 slack)
        assertTrue(w[3] <= 830 + 2 + f + 24 - PatchRest.CROP_SLACK_PX);
    }
    @Test public void aDenserOrSparserScreenStillStaysInsideTheCrop() {
        for (float d : new float[] { 0.75f, 1.0f, 1.0625f, 1.25f, 1.5f, 2.0f }) {
            int e = Math.round(PatchRest.EDGE_DP * d), f = Math.round(PatchRest.FEATHER_DP * d), bf = Math.round(PatchRest.BOTTOM_FEATHER_DP * d);
            int x = PatchRest.clampExtra(Math.round(PatchRest.EXTRA_BOTTOM_DP * d), bf, f, 24);
            int[] w = PatchRest.slotWindow(480, 700, 585, 830, e, f, x, bf, 600, 1024);
            assertTrue("density " + d, w[3] <= 830 + e + f + 24 - PatchRest.CROP_SLACK_PX || x == 0);
        }
    }
    @Test public void insideTheCoreThePatchIsFullyOpaque() { assertEquals(255, PatchRest.alphaAt(50, 50, 10, 10, 90, 90, 8, 8, 8, 8)); assertEquals(255, PatchRest.alphaAt(10, 10, 10, 10, 90, 90, 8, 8, 8, 8)); assertEquals(255, PatchRest.alphaAt(89, 89, 10, 10, 90, 90, 8, 8, 8, 8)); }
    @Test public void aHardEdgeShowsNothingOutsideTheCore() {
        assertEquals(0, PatchRest.alphaAt(9, 50, 10, 10, 90, 90, 0, 0, 0, 0));
        assertEquals(0, PatchRest.alphaAt(90, 50, 10, 10, 90, 90, 0, 0, 0, 0));
        assertEquals(0, PatchRest.alphaAt(50, 9, 10, 10, 90, 90, 0, 0, 0, 0));
        assertEquals(0, PatchRest.alphaAt(50, 90, 10, 10, 90, 90, 0, 0, 0, 0));
    }
    @Test public void theSoftFringeFadesOutwardAndEndsAtZero() {
        int prev = 255;
        for (int d = 1; d <= 8; d++) { int a = PatchRest.alphaAt(10 - d, 50, 10, 10, 90, 90, 8, 8, 8, 8); assertTrue("fade must not grow outward at d=" + d, a <= prev); assertTrue(a > 0 && a < 255); prev = a; }
        assertEquals(0, PatchRest.alphaAt(10 - 9, 50, 10, 10, 90, 90, 8, 8, 8, 8));
        assertEquals(0, PatchRest.alphaAt(0, 50, 10, 10, 90, 90, 8, 8, 8, 8));
    }
    @Test public void aCornerFadesInBothDirections() {
        int edge = PatchRest.alphaAt(5, 50, 10, 10, 90, 90, 8, 8, 8, 8), corner = PatchRest.alphaAt(5, 5, 10, 10, 90, 90, 8, 8, 8, 8);
        assertTrue(corner < edge && corner > 0);
        assertEquals(PatchRest.alphaAt(5, 50, 10, 10, 90, 90, 8, 8, 8, 8), PatchRest.alphaAt(94, 50, 10, 10, 90, 90, 8, 8, 8, 8));   // symmetric left / right
    }
    @Test public void theOpenPatchHasAHardTopSoTheStockPanelNeverShowsThroughIt() {
        // the OPEN mask: top fringe 0 -> everything above the core is transparent, everything inside is fully opaque
        assertEquals(0, PatchRest.alphaAt(50, 9, 10, 10, 90, 90, 8, 0, 8, 8));
        assertEquals(255, PatchRest.alphaAt(50, 10, 10, 10, 90, 90, 8, 0, 8, 8));
    }
    @Test public void aWobbleOfAFewPixelsDoesNotRebuildThePatch() {
        int[] a = { 470, 692, 595, 840 };
        assertTrue(PatchRest.sameWindow(a, new int[] { 470, 693, 595, 840 }, 2));
        assertTrue(PatchRest.sameWindow(a, new int[] { 472, 690, 593, 842 }, 2));
        assertFalse(PatchRest.sameWindow(a, new int[] { 470, 695, 595, 840 }, 2));   // a real change (3 px) rebuilds it
        assertFalse(PatchRest.sameWindow(a, null, 2));
        assertFalse(PatchRest.sameWindow(a, new int[] { 1, 2, 3 }, 2));
    }
    @Test public void theMaskOfTheRealTabletWindowIsAsymmetricAsDesigned() {
        // v0.7.6 (review): the slot window on the 600 x 1024 tablet is 126 x 115: opaque core x 9..123, hard top at row 6 (open menu), fringes left 9 / right 2 (the panel is 4 px from the screen edge) / bottom 9
        byte[] m = PatchRest.mask(126, 115, 9, 6, 124, 106, 9, 0, 2, 9);
        assertEquals(126 * 115, m.length);
        assertEquals(255, m[60 * 126 + 60] & 0xff);            // inside the core
        assertEquals(0, m[5 * 126 + 60] & 0xff);               // above the hard top: nothing (the stock panel must never show through a fade)
        assertEquals(189, m[60 * 126 + 124] & 0xff);           // first column of the short right fringe
        assertEquals(66, m[60 * 126 + 125] & 0xff);            // last column = the screen edge
        assertTrue((m[60 * 126 + 4] & 0xff) > 0 && (m[60 * 126 + 4] & 0xff) < 255);   // the long left fringe fades
        assertTrue((m[112 * 126 + 60] & 0xff) > 0 && (m[112 * 126 + 60] & 0xff) < 255); // and so does the bottom fringe
    }
    @Test public void theMaskHasOneByteForEveryPixelAndMatchesAlphaAt() {
        byte[] m = PatchRest.mask(100, 100, 10, 10, 90, 90, 8, 8, 8, 8);
        assertEquals(100 * 100, m.length);
        assertEquals(PatchRest.alphaAt(7, 40, 10, 10, 90, 90, 8, 8, 8, 8), m[40 * 100 + 7] & 0xff);
        assertEquals(255, m[50 * 100 + 50] & 0xff);
    }

    // ---- BC1 (Tj, 2026-11-07, v0.8.3, with a video): "a very small vertical sliver all the way to the right ... cover all the way to the right edge of the screen"
    @Test public void aWindowOnTheDisplayEdgeRunsItsCoreToTheEdgeWithoutAFringe() {
        // the 600 x 1024 tablet: window x 474..600 (w 126, h 115): core right 124, fringe right 2 = 74 % / 26 % of the stock card's edge line showed in the last two columns (the mask test above)
        int[] c = PatchRest.coverDisplayEdges(474, 600, 653, 600, 1024, 126, 115, 9, 124, 106, 9, 2, 9);
        assertEquals(126, c[1]);                    // right: the core reaches the display edge
        assertEquals(0, c[4]);                      // and has no fringe
        assertEquals(9, c[0]); assertEquals(106, c[2]); assertEquals(9, c[3]); assertEquals(9, c[5]);   // the left and bottom sides do not touch the display edge: unchanged
        byte[] m = PatchRest.mask(126, 115, c[0], 6, c[1], c[2], c[3], 0, c[4], c[5]);
        assertEquals(255, m[60 * 126 + 123] & 0xff);
        assertEquals(255, m[60 * 126 + 124] & 0xff);   // was 189
        assertEquals(255, m[60 * 126 + 125] & 0xff);   // the screen's last column: was 66
        assertEquals(255, m[100 * 126 + 125] & 0xff);  // down the whole slot, not only at one row
    }
    @Test public void aWindowThatDoesNotTouchTheDisplayEdgeKeepsItsFringes() {
        assertArrayEquals(new int[] { 9, 100, 140, 9, 9, 9 }, PatchRest.coverDisplayEdges(400, 520, 700, 600, 1024, 120, 160, 9, 100, 140, 9, 9, 9));
    }
    @Test public void everySideThatLiesOnTheDisplayEdgeIsSnapped() {
        assertArrayEquals(new int[] { 0, 600, 80, 0, 0, 0 }, PatchRest.coverDisplayEdges(0, 600, 1024, 600, 1024, 600, 80, 9, 590, 70, 9, 9, 9));
    }
    @Test public void theWindowOfTheRealTabletReachesTheDisplayEdgeAtTheDefaultGeometry() {
        // panel [485,339][596,642] on the 600 x 1024 tablet at density 1.0625 (EDGE 2, FEATHER 9): the window is clamped at 600, so the rule above applies to the real window
        int[] w = PatchRest.slotWindow(485, 547, 596, 642, 2, 9, 26, 4, 600, 1024);
        assertEquals(600, w[2]);
    }

    // ---- BT1a (Tj, 2026-11-11): a patch that only BECOMES restable during a pull-up must not appear over the darkening wallpaper
    @Test public void aPatchRestsVisiblyOnlyAfterItHasBeenRestableForTheSettleTime() {
        assertFalse("just became restable", PatchRest.settled(1000, 1000));
        assertFalse(PatchRest.settled(1000 + PatchRest.SETTLE_MS - 1, 1000));
        assertTrue(PatchRest.settled(1000 + PatchRest.SETTLE_MS, 1000));
        assertTrue(PatchRest.settled(60_000, 1000));
    }
    @Test public void aPatchThatIsNotRestableIsNeverSettled() {
        assertFalse(PatchRest.settled(60_000, 0));
        assertEquals(0, PatchRest.settleLeftMs(60_000, 0));
    }
    @Test public void theSettleTimerSaysHowLongIsLeft() {
        assertEquals(PatchRest.SETTLE_MS, PatchRest.settleLeftMs(5000, 5000));
        assertEquals(PatchRest.SETTLE_MS - 400, PatchRest.settleLeftMs(5400, 5000));
        assertEquals(0, PatchRest.settleLeftMs(5000 + PatchRest.SETTLE_MS, 5000));
        assertEquals(0, PatchRest.settleLeftMs(99_999, 5000));
    }

    // ---- review B #1 (BT1): ONE return time, re-derived from the books every time the patch is left invisible
    @Test public void theReturnTimeIsTheLongestOfSettleHoldAndFinger() {
        assertEquals("nothing holds it", 0, PatchRest.returnInMs(60_000, 1000, 0, 0));
        assertEquals("settle only", PatchRest.SETTLE_MS - 400, PatchRest.returnInMs(5400, 5000, 0, 0));
        assertEquals("PIN-pad hold longer than the settle", 2500, PatchRest.returnInMs(5000, 1000, 7500, 0));
        assertEquals("finger hold longer than the rest", 1500, PatchRest.returnInMs(5000, 1000, 5200, 6500));
        assertEquals("a hold in the past is nothing", 0, PatchRest.returnInMs(9000, 1000, 8000, 7000));
    }
    @Test public void aPadSeenForUnderNinetyMillisecondsStillGetsItsReturnAfterTheHold() {
        // the tablet's short pull-up: last sign S = 10000 holds the patch away until 12500; the pad leaves at L = 10700 and the old padBack ran at L + 1600 = 12300, 200 ms BEFORE the hold ended, and nothing looked again
        long sign = 10_000, holdUntil = sign + 2500, left = 10_700, padBackRan = left + 1600;
        assertTrue("the old timer ran inside the hold", padBackRan < holdUntil);
        long in = PatchRest.returnInMs(padBackRan, 1000, holdUntil, 0);
        assertEquals(200, in);
        assertTrue("settled by then", PatchRest.settled(padBackRan + in, 1000));
        assertTrue("at that moment nothing holds it any more", padBackRan + in >= holdUntil);
    }
    @Test public void atTheReturnTimeNothingHoldsThePatchAnyMore() {
        for (long since : new long[]{1, 1000, 4800})
            for (long hold : new long[]{0, 4000, 5000, 7000})
                for (long touch : new long[]{0, 4500, 5900})
                    for (long now : new long[]{1500, 4999, 5000, 5001, 6000, 6500}) {
                        long at = now + PatchRest.returnInMs(now, since, hold, touch);
                        assertTrue(PatchRest.settled(at, since));
                        assertTrue(at >= hold);
                        assertTrue(at >= touch);
                    }
    }
}
