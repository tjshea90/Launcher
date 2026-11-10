package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** BA1: when the helper measures the stock menu's scrim, how often, and where it samples. The Android side is proved by an emulator section (pt phase C). */
public class ScrimMeasureTest {
    private static ScrimMeasure.State go() {
        ScrimMeasure.State s = new ScrimMeasure.State();
        s.switchOn = true; s.coverOn = true; s.lockPatchOn = true; s.permitted = true; s.geometryKnown = true; s.validCrop = true;
        s.interactive = true; s.keyguardLocked = true; s.now = 1_000_000; s.screenOnMs = 900_000; s.lastStartMs = 0;
        return s;
    }

    // ---- the decision
    @Test public void aPlainLockScreenWithACalibratedPatchStarts() { assertTrue(ScrimMeasure.shouldStart(go())); assertEquals("", ScrimMeasure.decision(go())); }
    @Test public void nothingStartsOnceThereIsAMeasurement() { ScrimMeasure.State s = go(); s.haveMeasurement = true; assertFalse(ScrimMeasure.shouldStart(s)); }
    @Test public void theSwitchAndThePauseWin() {
        ScrimMeasure.State s = go(); s.switchOn = false; assertFalse(ScrimMeasure.shouldStart(s)); assertTrue(ScrimMeasure.decision(s).contains("switched off"));
        s = go(); s.paused = true; assertFalse(ScrimMeasure.shouldStart(s));
        s = go(); s.coverOn = false; assertFalse(ScrimMeasure.shouldStart(s));
        s = go(); s.lockPatchOn = false; assertFalse(ScrimMeasure.shouldStart(s));
        s = go(); s.permitted = false; assertFalse(ScrimMeasure.shouldStart(s));
    }
    @Test public void itWaitsForTheCalibrationAndTheMenuGeometry() {
        ScrimMeasure.State s = go(); s.validCrop = false; assertTrue(ScrimMeasure.decision(s).contains("calibration"));
        s = go(); s.geometryKnown = false; assertTrue(ScrimMeasure.decision(s).contains("side key"));
    }
    @Test public void itNeverStartsOnAnythingButAPlainLockScreen() {
        ScrimMeasure.State s = go(); s.interactive = false; assertTrue(ScrimMeasure.decision(s).contains("screen is off"));
        s = go(); s.keyguardLocked = false; assertFalse(ScrimMeasure.shouldStart(s));
        s = go(); s.pinPadUp = true; assertTrue(ScrimMeasure.decision(s).contains("PIN pad"));
        s = go(); s.menuOpen = true; assertTrue(ScrimMeasure.decision(s).contains("menu is open"));
        s = go(); s.handshakeArmed = true; assertTrue(ScrimMeasure.decision(s).contains("gesture"));
        s = go(); s.calibrating = true; assertFalse(ScrimMeasure.shouldStart(s));
        s = go(); s.stageRunning = true; assertFalse(ScrimMeasure.shouldStart(s));
    }
    @Test public void theLockScreenSettlesFirst() {
        ScrimMeasure.State s = go(); s.screenOnMs = s.now - 1000; assertFalse(ScrimMeasure.shouldStart(s));
        s.screenOnMs = s.now - ScrimMeasure.SETTLE_AFTER_WAKE_MS; assertTrue(ScrimMeasure.shouldStart(s));
        s.screenOnMs = 0; assertTrue(ScrimMeasure.shouldStart(s));   // unknown: not a reason to wait
    }
    @Test public void triesAreLimited() {
        ScrimMeasure.State s = go(); s.lastStartMs = s.now - 10_000; assertTrue(ScrimMeasure.decision(s).contains("just now"));
        s.lastStartMs = s.now - ScrimMeasure.MIN_GAP_MS; assertTrue(ScrimMeasure.shouldStart(s));
        s = go(); s.startsThisLock = ScrimMeasure.MAX_PER_LOCK; assertTrue(ScrimMeasure.decision(s).contains("this lock"));
        s = go(); s.startsToday = ScrimMeasure.MAX_PER_DAY; assertTrue(ScrimMeasure.decision(s).contains("today"));
    }
    // ---- review of BA1: a lock screen that cannot be fitted is not retried every day
    @Test public void aLockScreenThatCannotBeFittedIsGivenUpAfterTwoTries() {
        ScrimMeasure.State s = go(); s.failures = 1; assertTrue(ScrimMeasure.shouldStart(s));
        s.failures = ScrimMeasure.MAX_FAILURES; assertFalse(ScrimMeasure.shouldStart(s)); assertTrue(ScrimMeasure.decision(s).contains("gave up for this lock screen"));
        s.haveMeasurement = true; assertEquals("", ScrimMeasure.decision(s));   // a measurement that exists always wins
    }
    @Test public void failuresAreCountedPerLockScreenAndTheme() {
        String sig = PatchShot.sig(7, 0, 600, 1024), other = PatchShot.sig(8, 0, 600, 1024);
        assertEquals(0, ScrimMeasure.failures("", sig, false)); assertEquals(0, ScrimMeasure.failures(null, sig, false));
        String one = ScrimMeasure.addFailure("", sig, false); assertEquals(1, ScrimMeasure.failures(one, sig, false));
        String two = ScrimMeasure.addFailure(one, sig, false); assertEquals(2, ScrimMeasure.failures(two, sig, false));
        assertEquals(0, ScrimMeasure.failures(two, other, false));       // another wallpaper starts afresh
        assertEquals(0, ScrimMeasure.failures(two, sig, true));          // the dark theme is another lock screen
        assertEquals(1, ScrimMeasure.failures(ScrimMeasure.addFailure(two, other, false), other, false));   // and a record of another lock screen is replaced, not added to
        assertEquals(0, ScrimMeasure.failures("garbage", sig, false)); assertEquals(0, ScrimMeasure.failures(sig + "#d#abc", sig, false));
    }
    // ---- BQ1a (Tj, 2026-11-10, v0.9.1: "It did two or three screenshots of the lock screen again, one time on each of the three last times I went to the lock screen")
    @Test public void aSlotThatNeverFinishesFlashesAtMostTwiceNoMatterHowOftenTheTabletIsLocked() {
        // the 2026-11-10 night: the slot never stored a value, every run was interrupted and nothing was counted: up to four flashes a day, for ever. Review of 2026-11-10: a run is counted WHEN IT STARTS
        // (maybeMeasureScrim), so a service that is torn down or a process that dies in the middle of a run (it never reaches the end of the run) cannot lose the count either.
        String sig = PatchShot.sig(30, 0, 600, 1024), record = ""; int starts = 0;
        for (int lock = 0; lock < 12; lock++) {
            ScrimMeasure.State s = go(); s.failures = ScrimMeasure.failures(record, sig, false); s.startsToday = 0; s.lastStartMs = 0; s.startsThisLock = 0;
            if (!ScrimMeasure.shouldStart(s)) continue;
            starts++; record = ScrimMeasure.addFailure(record, sig, false);   // counted before the first screenshot is asked for; the run then never ends at all
        }
        assertEquals(ScrimMeasure.MAX_FAILURES, starts);
        assertEquals(2, ScrimMeasure.failures(record, sig, false));
        ScrimMeasure.State s = go(); s.failures = ScrimMeasure.failures(record, PatchShot.sig(31, 0, 600, 1024), false); assertTrue("a new wallpaper starts afresh", ScrimMeasure.shouldStart(s));
        s = go(); s.night = true; s.failures = 0; assertFalse("the dark look is never measured at all (BQ2), whatever its record says", ScrimMeasure.shouldStart(s));
        assertEquals("scrim_fail_r0", ScrimMeasure.failKey(0));
    }
    @Test public void aRunThatStoredAMeasurementIsNoFailureAndAStoredMeasurementAlwaysWins() {
        String sig = PatchShot.sig(30, 0, 600, 1024);
        String counted = ScrimMeasure.addFailure("", sig, false);          // counted at the start of the run ...
        assertEquals(1, ScrimMeasure.failures(counted, sig, false));
        String afterMeasured = "";                                          // ... and cleared by scrimIngest when it stored the value
        assertEquals(0, ScrimMeasure.failures(afterMeasured, sig, false));
        ScrimMeasure.State s = go(); s.failures = 2; s.haveMeasurement = true;
        assertEquals("a stale count can never switch a stored measurement off", "", ScrimMeasure.decision(s));
        s.haveMeasurement = false; assertTrue("without a stored value two counted runs end it", ScrimMeasure.decision(s).contains("gave up for this lock screen"));
    }
    @Test public void theThemeHasSettledBeforeARunStarts() {
        // the tablet wakes in the DARK theme after a Quickstep session (battery saver ON) and only flips to light a few seconds later
        ScrimMeasure.State s = go();
        for (long ms : new long[] { 0, 500, 3_500, 4_000, 6_000, 9_999 }) { s.screenOnMs = s.now - ms; assertFalse("at " + ms + " ms", ScrimMeasure.shouldStart(s)); assertTrue(ScrimMeasure.decision(s).contains("only just come on")); }
        s.screenOnMs = s.now - 10_000; assertTrue(ScrimMeasure.shouldStart(s));
        assertTrue(ScrimMeasure.SETTLE_AFTER_WAKE_MS >= 10_000);
    }
    @Test public void aFreshlyConnectedServiceSettlesFromItsConnectNotFromAnOldWake() {
        long now = 1_000_000_000L, elapsed = 50_000_000L;
        assertEquals("the wake was 3 s ago, connected an hour ago: the wake counts", now - 3_000, ScrimMeasure.settleFromMs(now - 3_000, now, elapsed, elapsed - 3_600_000));
        assertEquals("the wake was a minute ago but the service connected 2 s ago", now - 2_000, ScrimMeasure.settleFromMs(now - 60_000, now, elapsed, elapsed - 2_000));
        assertEquals("never connected: only the wake", now - 5_000, ScrimMeasure.settleFromMs(now - 5_000, now, elapsed, 0));
        assertEquals("no wake seen yet", now - 1_000, ScrimMeasure.settleFromMs(0, now, elapsed, elapsed - 1_000));
        assertEquals("a clock that went backwards is ignored", now - 7_000, ScrimMeasure.settleFromMs(now - 7_000, now, 100, 200));
        ScrimMeasure.State s = go(); s.now = now; s.screenOnMs = ScrimMeasure.settleFromMs(now - 60_000, now, elapsed, elapsed - 2_000);
        assertFalse("2 s after a reconnect no run starts", ScrimMeasure.shouldStart(s));
        s.now = now + ScrimMeasure.SETTLE_AFTER_WAKE_MS; assertTrue(ScrimMeasure.shouldStart(s));
    }
    // ---- BQ2 (Tj, 2026-11-10: "Why doesnt it just measure/calibrate one time and never screenshot again? I'm not changing the wallpaper")
    @Test public void theDarkLookIsNeverMeasuredItWaitsForTheLightLook() {
        ScrimMeasure.State s = go(); s.night = true;
        assertFalse(ScrimMeasure.shouldStart(s));
        assertTrue("a 'waiting' reason: the service looks again in a moment, the theme flips to light a few seconds after the wake", ScrimMeasure.decision(s).startsWith("waiting"));
        assertTrue(ScrimMeasure.decision(s).contains("dark look"));
        s.night = false; assertTrue(ScrimMeasure.shouldStart(s));
    }
    @Test public void aMeasurementThatExistsAnswersInBothLooks() {
        ScrimMeasure.State s = go(); s.haveMeasurement = true;
        s.night = false; assertEquals("", ScrimMeasure.decision(s)); assertFalse(ScrimMeasure.shouldStart(s));
        s.night = true; assertEquals("", ScrimMeasure.decision(s)); assertFalse(ScrimMeasure.shouldStart(s));
    }
    @Test public void theDarkLookUsesItsOwnOldSlotFirstAndThenTheLightOne() {
        boolean[] dark = ScrimMeasure.slotOrder(true), light = ScrimMeasure.slotOrder(false);
        assertEquals(2, dark.length); assertTrue(dark[0]); assertFalse(dark[1]);
        assertEquals(1, light.length); assertFalse(light[0]);
    }
    @Test public void aLightMeasurementServesTheDarkLookAndNeverNeedsADarkScreenshot() {
        ScrimFit.Transform t = new ScrimFit.Transform(new float[] { 0.8f, 0.8f, 0.8f }, new float[] { 6f, 7f, 7f }, 30000, 0.3);
        String sig = "30|r0|600x1024", day = ScrimFit.encode(t, sig, false, 1000L);
        // the service walks ScrimMeasure.slotOrder(night) and takes the first slot that decodes for its own look
        for (boolean night : new boolean[] { false, true }) {
            ScrimFit.Transform got = null;
            for (boolean slot : ScrimMeasure.slotOrder(night)) { got = ScrimFit.decode(slot ? "" : day, sig, slot); if (got != null) break; }
            assertNotNull("the light slot answers in the " + (night ? "dark" : "light") + " look", got);
        }
        assertNull("a different wallpaper: nothing answers any more", ScrimFit.decode(day, "31|r0|600x1024", false));
    }
    @Test public void withAnUnchangedWallpaperNoLockEverStartsARunAgain() {
        // 500 lock visits in every combination of look, wake time and counters: once the light measurement exists (haveMeasurement) nothing starts
        for (int i = 0; i < 500; i++) {
            ScrimMeasure.State s = go(); s.haveMeasurement = true; s.night = (i % 2 == 0); s.screenOnMs = s.now - (i % 40) * 1000L; s.startsThisLock = i % 3; s.startsToday = i % 5; s.failures = i % 4; s.lastStartMs = i % 7 == 0 ? 0 : s.now - i * 1000L;
            assertFalse("visit " + i, ScrimMeasure.shouldStart(s));
        }
    }
    @Test public void aNewWallpaperIsMeasuredOnceInTheLightLookAfterTheWakeSettled() {
        // the tablet wakes DARK after a Quickstep session and is light a few seconds later: no run in the dark seconds, one run once the light look has settled
        ScrimMeasure.State s = go(); s.haveMeasurement = false;
        s.night = true; s.screenOnMs = s.now - 2_000; assertFalse(ScrimMeasure.shouldStart(s));
        s.night = true; s.screenOnMs = s.now - 20_000; assertFalse("still dark after 20 s: no dark run either", ScrimMeasure.shouldStart(s));
        s.night = false; s.screenOnMs = s.now - 4_000; assertFalse("light, but the wake has not settled", ScrimMeasure.shouldStart(s));
        s.screenOnMs = s.now - ScrimMeasure.SETTLE_AFTER_WAKE_MS; assertTrue(ScrimMeasure.shouldStart(s));
    }
    @Test public void onlyOneRunPerLockBecauseEachOneFlashesAtOnce() {
        assertEquals(1, ScrimMeasure.MAX_PER_LOCK);
        ScrimMeasure.State s = go(); s.startsThisLock = 1; assertFalse(ScrimMeasure.shouldStart(s)); assertEquals("gave up for this lock after 1 try", ScrimMeasure.decision(s));
    }
    @Test public void theLastRunIsOneReadableLine() {
        assertEquals("11-10 03:11:44 dark theme: stopped: the PIN pad came up", ScrimMeasure.runLine("11-10 03:11:44", true, "stopped: the PIN pad came up"));
        assertEquals("11-10 03:11:44 light theme: MEASURED x", ScrimMeasure.runLine("11-10 03:11:44", false, " MEASURED x "));
        assertEquals("s dark theme: ", ScrimMeasure.runLine("s", true, null));
        assertEquals("a run that never finishes still reads as one", "11-10 09:12:03 light theme: started (timer), no end recorded yet", ScrimMeasure.runLine("11-10 09:12:03", false, "started (timer), no end recorded yet"));
        assertTrue(ScrimMeasure.runLine("s", false, new String(new char[500]).replace('\0', 'x')).length() <= 160);
        assertEquals("scrim_last_run", ScrimMeasure.RUN_KEY);
    }
    @Test public void theSecondFrameWaitsForTheWholeFadeAndMore() { assertTrue(ScrimMeasure.OPEN_SETTLE_MS >= PatchFade.OPEN_MS + 800); }
    @Test public void aDebugBuildDoesNotMeasureUnlessAskedTo() { assertFalse(ScrimMeasure.defaultOn(true)); assertTrue(ScrimMeasure.defaultOn(false)); }
    @Test public void theValueKeyIsPerRotation() { assertEquals("patch_scrim_r0", ScrimMeasure.valueKey(0)); assertEquals("patch_scrim_r3", ScrimMeasure.valueKey(3)); }
    // ---- BB2 (Tj, 2026-11-07: "the lock screen screenshot itself again, not sure why"): one slot per THEME, so a theme flip never invalidates the other theme's measurement
    @Test public void eachThemeHasItsOwnSlotAndTheLightOneIsTheOldKey() {
        assertEquals(ScrimMeasure.valueKey(0), ScrimMeasure.valueKey(0, false));            // v0.8.2's stored value stays valid
        assertEquals("patch_scrim_r0n", ScrimMeasure.valueKey(0, true));
        assertEquals("scrim_fail_r0", ScrimMeasure.failKey(0)); assertEquals("scrim_fail_r0n", ScrimMeasure.failKey(0, true));
        for (int rot = 0; rot < 4; rot++) { assertFalse(ScrimMeasure.valueKey(rot, true).equals(ScrimMeasure.valueKey(rot, false))); assertFalse(ScrimMeasure.failKey(rot, true).equals(ScrimMeasure.failKey(rot, false))); }
    }
    @Test public void aThemeFlipKeepsBothMeasurementsValid() {
        ScrimFit.Transform t = new ScrimFit.Transform(new float[] { 0.8f, 0.8f, 0.8f }, new float[] { 6f, 7f, 7f }, 30000, 0.3);
        String sig = "30|r0|600x1024";
        String day = ScrimFit.encode(t, sig, false, 1000L), night = ScrimFit.encode(t, sig, true, 2000L);
        assertNotNull(ScrimFit.decode(day, sig, false)); assertNotNull(ScrimFit.decode(night, sig, true));
        assertNull("a slot never answers for the other theme", ScrimFit.decode(day, sig, true));
        assertNull(ScrimFit.decode(night, sig, false));
    }
    @Test public void theReasonIsInPlainWords() {
        ScrimFit.Transform t = new ScrimFit.Transform(new float[] { 0.8f, 0.8f, 0.8f }, new float[] { 6f, 7f, 7f }, 30000, 0.3);
        String sig = "30|r0|600x1024", other = "31|r0|600x1024";
        assertTrue(ScrimFit.reasonMissing("", sig, false).contains("nothing is stored yet") && ScrimFit.reasonMissing("", sig, false).contains("light"));
        assertTrue(ScrimFit.reasonMissing(null, sig, true).contains("dark theme"));
        assertTrue(ScrimFit.reasonMissing(ScrimFit.encode(t, other, false, 1L), sig, false).contains("lock screen changed"));
        assertTrue(ScrimFit.reasonMissing(ScrimFit.encode(t, sig, true, 1L), sig, false).contains("other theme"));
        assertTrue(ScrimFit.reasonMissing("garbage", sig, false).contains("could not be read"));
    }

    // ---- the sampling grid
    @Test public void theGridSkipsTheExcludedRectangles() {
        int[][] ex = { { 0, 0, 600, 36 }, { 300, 300, 500, 500 } };
        int[] g = ScrimMeasure.grid(600, 1024, 4, ex);
        assertTrue(g.length > 10_000);
        for (int v : g) {
            int x = v & 0xFFFF, y = v >>> 16;
            assertTrue(y >= 36);
            assertFalse(x >= 300 && x < 500 && y >= 300 && y < 500);
        }
    }
    @Test public void theGridDecodesToValidPixels() {
        int[] g = ScrimMeasure.grid(600, 1024, 4, null);
        for (int v : g) { int x = v & 0xFFFF, y = v >>> 16; assertTrue(x >= 1 && x < 599 && y >= 1 && y < 1023); }
    }
    @Test public void theExclusionRectanglesCoverTheBarsTheNumberThePanelAndThePatch() {
        int[][] r = ScrimMeasure.excludeRects(600, 1024, new int[] { 300, 100, 560, 400 }, new int[] { 300, 330, 560, 420 }, 20);
        assertEquals(5, r.length);
        assertEquals(280, r[3][0]); assertEquals(80, r[3][1]); assertEquals(580, r[3][2]); assertEquals(420, r[3][3]);   // panel + 20 px shadow
        assertEquals(294, r[4][0]);                                                                                      // patch + 6 px
        assertEquals(3, ScrimMeasure.excludeRects(600, 1024, null, null, 20).length);
    }
    @Test public void theNumberRectScalesWithTheScreen() {
        int[] a = ScrimMeasure.numberRect(600, 1024), b = ScrimMeasure.numberRect(1200, 2048);
        assertEquals(a[0] * 2, b[0], 1); assertEquals(a[3] * 2, b[3], 1);
        assertTrue(a[0] < 205 && a[2] > 396 && a[1] < 907 && a[3] > 924);   // it contains the measured bbox
    }
    @Test public void theBareFramePicksFlatPixelsOutsideTheExclusions() {
        int w = 200, h = 300;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = 0xFF000000 | ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | 90;   // a smooth gradient: every pixel is flat
        int[][] ex = { { 0, 0, w, 10 }, { 100, 100, 150, 150 } };
        int[] pts = ScrimMeasure.pickPoints(px, w, h, ex);
        assertTrue(pts.length > 1000);
        for (int v : pts) { int x = v & 0xFFFF, y = v >>> 16; assertTrue(y >= 10); assertFalse(x >= 100 && x < 150 && y >= 100 && y < 150); }
        // a chessboard of 1-pixel squares has no flat pixel at all
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) px[y * w + x] = ((x + y) & 1) == 0 ? 0xFFFFFFFF : 0xFF000000;
        assertEquals(0, ScrimMeasure.pickPoints(px, w, h, null).length);
        assertEquals(0, ScrimMeasure.pickPoints(null, w, h, null).length);
    }
    @Test public void aChannelIsReadAtThePoints() {
        int w = 10;
        int[] px = new int[w * 10];
        px[3 * w + 4] = 0xFF112233;
        int[] pts = { (3 << 16) | 4 };
        assertEquals(0x11, ScrimMeasure.channel(px, w, pts, 16)[0]);
        assertEquals(0x22, ScrimMeasure.channel(px, w, pts, 8)[0]);
        assertEquals(0x33, ScrimMeasure.channel(px, w, pts, 0)[0]);
    }
    @Test public void aFlatPixelHasSmallNeighbourDifferences() {
        assertTrue(ScrimMeasure.flat(101, 100, 104, 98, 101));
        assertFalse(ScrimMeasure.flat(100, 20, 200, 100, 100));    // an edge through the pixel
        assertFalse(ScrimMeasure.flat(255, 0, 0, 0, 0));           // a bright dot / a one-pixel stroke: both sides equal, the pixel itself is not
        assertFalse(ScrimMeasure.flat(0, 255, 255, 255, 255));     // and a dark one
        assertEquals(255, ScrimMeasure.lum(0xFFFFFF)); assertEquals(0, ScrimMeasure.lum(0x000000));
        assertTrue(ScrimMeasure.lum(0x00FF00) > ScrimMeasure.lum(0xFF0000));   // green counts more than red
    }
}
