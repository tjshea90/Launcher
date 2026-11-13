package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * BL1: the rules of the home curtain ({@link HomeCurtainRules}). Each test pins one way a wrong answer would hurt Tj: a picture of another wallpaper or screen shown as "the home", a camera-less capture at
 * a bad moment, a cover that swallows the retry of a wrong PIN, or a failed copy stored as the home screen.
 */
public class HomeCurtainRulesTest {
    private static HomeCurtainRules.UseFacts goodUse() {
        HomeCurtainRules.UseFacts f = new HomeCurtainRules.UseFacts();
        f.switchOn = true; f.hasPicture = true; f.pictureSig = "42|r0|600x1024"; f.currentSig = "42|r0|600x1024";
        f.picW = 600; f.picH = 1024; f.dispW = 600; f.dispH = 1024; f.picRotation = 0; f.rotation = 0; f.ageMs = 60_000;
        return f;
    }

    private static HomeCurtainRules.CaptureFacts goodCapture() {
        HomeCurtainRules.CaptureFacts f = new HomeCurtainRules.CaptureFacts();
        f.switchOn = true; f.customMode = true; f.unlocked = true; f.screenOn = true; f.homeAtRest = true; f.positionKnown = true; f.wallKnown = true;
        f.msSinceDraw = 5000; f.msSinceTouch = 5000; f.msSinceLastCapture = 60_000;
        return f;
    }

    // ---------------------------------------------------------------- the switch

    @Test public void shippedDefaultIsOnAndDebugBuildsKeepTheBlackCover() {
        assertTrue(HomeCurtainRules.defaultOn(false));
        assertFalse(HomeCurtainRules.defaultOn(true));
    }

    // ---------------------------------------------------------------- use

    @Test public void aMatchingFreshPictureIsUsed() { assertEquals("", HomeCurtainRules.whyNotUse(goodUse())); }

    @Test public void switchOffNeverUsesIt() { HomeCurtainRules.UseFacts f = goodUse(); f.switchOn = false; assertEquals("switch off", HomeCurtainRules.whyNotUse(f)); }

    @Test public void noPictureMeansTheBlackCover() { HomeCurtainRules.UseFacts f = goodUse(); f.hasPicture = false; assertEquals("no picture yet", HomeCurtainRules.whyNotUse(f)); }

    @Test public void anotherRotationIsNotUsed() { HomeCurtainRules.UseFacts f = goodUse(); f.rotation = 1; assertTrue(HomeCurtainRules.whyNotUse(f).contains("rotation")); }

    @Test public void anotherDisplaySizeIsNotUsed() {
        HomeCurtainRules.UseFacts f = goodUse(); f.dispW = 720;
        assertTrue(HomeCurtainRules.whyNotUse(f).contains("size"));
        f = goodUse(); f.picH = 0; f.dispH = 0;
        assertTrue("a zero-sized picture is never a picture", HomeCurtainRules.whyNotUse(f).contains("size"));
    }

    @Test public void aPictureOfAnotherWallpaperIsNotUsed() {
        HomeCurtainRules.UseFacts f = goodUse(); f.currentSig = "43|r0|600x1024";
        assertTrue(HomeCurtainRules.whyNotUse(f).contains("wallpaper"));
        f = goodUse(); f.pictureSig = "";
        assertTrue("an empty signature never matches", HomeCurtainRules.whyNotUse(f).contains("wallpaper"));
        f = goodUse(); f.currentSig = null;
        assertTrue("an unknown current signature never matches", HomeCurtainRules.whyNotUse(f).contains("wallpaper"));
    }

    @Test public void aTooOldOrUnknownAgePictureIsNotUsed() {
        HomeCurtainRules.UseFacts f = goodUse(); f.ageMs = HomeCurtainRules.MAX_AGE_MS + 1;
        assertTrue(HomeCurtainRules.whyNotUse(f).contains("old"));
        f.ageMs = Long.MAX_VALUE;
        assertTrue("an unknown age is treated as too old", HomeCurtainRules.whyNotUse(f).contains("old"));
        f.ageMs = -5;
        assertTrue("a picture from the future (clock jump) is not trusted", HomeCurtainRules.whyNotUse(f).contains("old"));
        f.ageMs = HomeCurtainRules.MAX_AGE_MS;
        assertEquals("exactly the limit is still fine", "", HomeCurtainRules.whyNotUse(f));
    }

    @Test public void aConfirmedPictureIsRestampedBeforeItCanExpire() {
        assertFalse("a fresh picture needs no new stamp", HomeCurtainRules.stampDue(60_000));
        assertFalse("exactly the stamp age is not yet due", HomeCurtainRules.stampDue(HomeCurtainRules.STAMP_AFTER_MS));
        assertTrue("older than a day and confirmed unchanged: renew the stamp", HomeCurtainRules.stampDue(HomeCurtainRules.STAMP_AFTER_MS + 1));
        assertTrue("even a picture that is already past the use limit is renewed when the home is confirmed unchanged", HomeCurtainRules.stampDue(HomeCurtainRules.MAX_AGE_MS + 1));
        assertFalse("a stamp from the future (clock jump) is not renewed", HomeCurtainRules.stampDue(-5));
        assertTrue("the renewal is far below the use limit, so a still-correct picture never expires", HomeCurtainRules.STAMP_AFTER_MS < HomeCurtainRules.MAX_AGE_MS / 10);
    }

    // ---------------------------------------------------------------- capture

    @Test public void aQuietHomeIsCaptured() { assertEquals("", HomeCurtainRules.whyNotCapture(goodCapture())); }

    @Test public void captureWaitsForEveryOtherThing() {
        HomeCurtainRules.CaptureFacts f = goodCapture(); f.switchOn = false;            assertEquals("switch off", HomeCurtainRules.whyNotCapture(f));
        f = goodCapture(); f.customMode = false;                                         assertEquals("Quickstep", HomeCurtainRules.whyNotCapture(f));
        f = goodCapture(); f.unlocked = false;                                           assertEquals("lock screen", HomeCurtainRules.whyNotCapture(f));
        f = goodCapture(); f.screenOn = false;                                           assertEquals("screen off", HomeCurtainRules.whyNotCapture(f));
        f = goodCapture(); f.calibrating = true;                                         assertTrue(HomeCurtainRules.whyNotCapture(f).contains("calibration"));
        f = goodCapture(); f.building = true;                                            assertTrue(HomeCurtainRules.whyNotCapture(f).contains("capture is running"));
        f = goodCapture(); f.positionKnown = false;                                      assertTrue(HomeCurtainRules.whyNotCapture(f).contains("position"));
        f = goodCapture(); f.wallKnown = false;                                          assertTrue(HomeCurtainRules.whyNotCapture(f).contains("wallpaper picture"));
        f = goodCapture(); f.homeAtRest = false;                                         assertTrue(HomeCurtainRules.whyNotCapture(f).contains("at rest"));
        f = goodCapture(); f.msSinceTouch = HomeCurtainRules.QUIET_TOUCH_MS - 1;         assertTrue(HomeCurtainRules.whyNotCapture(f).contains("touched"));
        f = goodCapture(); f.msSinceDraw = HomeCurtainRules.QUIET_DRAW_MS - 1;           assertTrue(HomeCurtainRules.whyNotCapture(f).contains("moving"));
        f = goodCapture(); f.msSinceLastCapture = HomeCurtainRules.MIN_GAP_MS - 1;       assertTrue(HomeCurtainRules.whyNotCapture(f).contains("moment ago"));
    }

    @Test public void theQuietLimitsAreInclusive() {
        HomeCurtainRules.CaptureFacts f = goodCapture();
        f.msSinceTouch = HomeCurtainRules.QUIET_TOUCH_MS; f.msSinceDraw = HomeCurtainRules.QUIET_DRAW_MS; f.msSinceLastCapture = HomeCurtainRules.MIN_GAP_MS;
        assertEquals("", HomeCurtainRules.whyNotCapture(f));
    }

    @Test public void aLockScreenOrQuickstepIsNeverCapturedEvenIfEverythingElseIsQuiet() {
        HomeCurtainRules.CaptureFacts f = goodCapture(); f.customMode = false; f.unlocked = false;
        assertEquals("the first reason wins (Quickstep before the lock screen)", "Quickstep", HomeCurtainRules.whyNotCapture(f));
    }

    // ---------------------------------------------------------------- touches

    @Test public void aWrongPinStaysRetypableUntilTheUnlockIsConfirmed() {
        assertFalse("never confirmed", HomeCurtainRules.unlockConfirmed(100_000, 0));
        assertFalse("a USER_PRESENT from a much earlier unlock does not confirm this one", HomeCurtainRules.unlockConfirmed(100_000, 100_000 - HomeCurtainRules.CONFIRMED_WITHIN_MS));
        assertTrue("just confirmed", HomeCurtainRules.unlockConfirmed(100_000, 99_999));
        assertTrue("confirmed a moment ago", HomeCurtainRules.unlockConfirmed(100_000, 100_000 - HomeCurtainRules.CONFIRMED_WITHIN_MS + 1));
        assertFalse("a clock that went backwards confirms nothing", HomeCurtainRules.unlockConfirmed(100_000, 100_500));
    }

    // ---------------------------------------------------------------- hash / blank

    private static int hashOf(int[][] rows, int[] opaque) {
        int h = 17;
        for (int[] r : rows) h = HomeCurtainRules.foldRow(h, r, r.length, opaque);
        return h;
    }

    private static int[][] gray(int rows, int cols, int argb) {
        int[][] m = new int[rows][cols];
        for (int[] r : m) java.util.Arrays.fill(r, argb);
        return m;
    }

    @Test public void theSameLayerHashesTheSame() {
        assertEquals(hashOf(gray(5, 30, 0xFF336699), new int[1]), hashOf(gray(5, 30, 0xFF336699), new int[1]));
    }

    @Test public void aMovedIconChangesTheHash() {
        int[][] a = gray(5, 30, 0x00000000), b = gray(5, 30, 0x00000000);
        b[2][9] = 0xFFFFFFFF;   // column 9 is a sampled column (every 3rd)
        assertNotEquals(hashOf(a, new int[1]), hashOf(b, new int[1]));
    }

    @Test public void anAllTransparentLayerIsBlankAndNeverStored() {
        int[] opaque = new int[1];
        hashOf(gray(8, 60, 0x00000000), opaque);
        assertTrue(HomeCurtainRules.blank(opaque));
        int[][] m = gray(8, 60, 0x00000000); m[3][30] = 0x80FF0000;
        opaque = new int[1]; hashOf(m, opaque);
        assertFalse("one visible sample is enough to not be a failed copy", HomeCurtainRules.blank(opaque));
    }

    @Test public void theStoredPictureIsRemadeOnlyWhenSomethingChanged() {
        String sig = "42|r0|600x1024";
        assertTrue("nothing stored yet", HomeCurtainRules.changed(false, "", 0, 0, sig, 500, 7));
        assertFalse("same wallpaper, page and layer", HomeCurtainRules.changed(true, sig, 500, 7, sig, 500, 7));
        assertFalse("a page offset within the tolerance is the same page", HomeCurtainRules.changed(true, sig, 500, 7, sig, 502, 7));
        assertTrue("another page", HomeCurtainRules.changed(true, sig, 500, 7, sig, 0, 7));
        assertTrue("another layer (an icon moved)", HomeCurtainRules.changed(true, sig, 500, 7, sig, 500, 8));
        assertTrue("another wallpaper", HomeCurtainRules.changed(true, sig, 500, 7, "43|r0|600x1024", 500, 7));
        assertTrue("a stored signature that is null is never the same", HomeCurtainRules.changed(true, null, 500, 7, sig, 500, 7));
    }

    // ---------------------------------------------------------------- names and meta

    @Test public void fileAndPreferenceNamesCarryTheRotationAndTheStep() {
        assertEquals("homewall_r1_x333.png", HomeCurtainRules.wallFile(1, 333));
        assertEquals("home_wall_r1_x333", HomeCurtainRules.wallMeta(1, 333));
        assertEquals("homecurtain_r0.png", HomeCurtainRules.curtainFile(0));
        assertEquals("home_curtain_r0", HomeCurtainRules.curtainMeta(0));
        assertNotEquals("the curtain files never collide with the patch crops", HomePatchRules.fileName(0, 500), HomeCurtainRules.wallFile(0, 500));
        assertNotEquals(HomePatchRules.metaKey(0, 500), HomeCurtainRules.wallMeta(0, 500));
    }

    @Test public void metaRoundTripsAndGarbageIsRejected() {
        String m = HomeCurtainRules.metaString("42|r0|600x1024", 500, -123456, 1794200000000L);
        HomeCurtainRules.Meta p = HomeCurtainRules.parseMeta(m);
        assertNotNull(p);
        assertEquals("42|r0|600x1024", p.sig); assertEquals(500, p.step); assertEquals(-123456, p.hash); assertEquals(1794200000000L, p.savedAtMs);
        assertNull(HomeCurtainRules.parseMeta(null));
        assertNull(HomeCurtainRules.parseMeta(""));
        assertNull(HomeCurtainRules.parseMeta("a#b#c#d"));
        assertNull("three parts are not a curtain meta", HomeCurtainRules.parseMeta("sig#1#2"));
        assertNull("an empty signature is not one", HomeCurtainRules.parseMeta("#1#2#3"));
    }

    @Test public void theMemoryCopyIsKeptOnlyWhereACoverCanShowIt() {   // CA1 M1
        assertTrue(HomeCurtainRules.keepInMemory(true, false, false));    // Quickstep: the Enter tap that ends it shows the curtain
        assertFalse(HomeCurtainRules.keepInMemory(true, true, false));    // a plain custom session: nothing shows it
        assertTrue(HomeCurtainRules.keepInMemory(true, true, true));      // the first unlock after a boot cleans up behind the curtain
        assertFalse(HomeCurtainRules.keepInMemory(false, false, false));  // the switch is off
        assertFalse(HomeCurtainRules.keepInMemory(false, true, true));
    }
}
