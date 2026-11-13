package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** AW5 + review AU2: when the helper calibrates the lock-screen patch by itself, and which screenshots it refuses. */
public class AutoCalibrationTest {
    private static final long NOW = 5_000_000;

    /** A plain lock screen with no crop yet: everything ready. */
    private static AutoCalibration.State ready() {
        AutoCalibration.State s = new AutoCalibration.State();
        s.switchOn = true; s.coverOn = true; s.lockPatchOn = true; s.permitted = true; s.geometryKnown = true; s.patchWindow = true;
        s.interactive = true; s.keyguardLocked = true; s.now = NOW;
        return s;
    }

    @Test public void startsOnAPlainLockScreenWithoutACrop() { assertTrue(AutoCalibration.shouldStart(ready())); assertEquals("", AutoCalibration.decision(ready())); }
    @Test public void neverWhenACropIsValid() { AutoCalibration.State s = ready(); s.validCrop = true; assertFalse(AutoCalibration.shouldStart(s)); }
    @Test public void neverWhenSwitchedOffOrThePatchIsOff() {
        AutoCalibration.State s = ready(); s.switchOn = false; assertFalse(AutoCalibration.shouldStart(s)); assertTrue(AutoCalibration.decision(s).contains("switched off"));
        s = ready(); s.coverOn = false; assertFalse(AutoCalibration.shouldStart(s));
        s = ready(); s.lockPatchOn = false; assertFalse(AutoCalibration.shouldStart(s));
    }
    @Test public void neverWhilePausedOrWithoutThePermission() {
        AutoCalibration.State s = ready(); s.paused = true; assertFalse(AutoCalibration.shouldStart(s));
        s = ready(); s.permitted = false; assertFalse(AutoCalibration.shouldStart(s)); assertTrue(AutoCalibration.decision(s).contains("permission"));
    }
    @Test public void waitsUntilTheMenuPlaceIsKnown() {
        AutoCalibration.State s = ready(); s.geometryKnown = false;
        assertFalse(AutoCalibration.shouldStart(s)); assertTrue(AutoCalibration.decision(s).contains("side key"));
        s = ready(); s.patchWindow = false; assertFalse(AutoCalibration.shouldStart(s));
    }
    @Test public void onlyOnAPlainLockScreen() {
        AutoCalibration.State s = ready(); s.interactive = false; assertFalse(AutoCalibration.shouldStart(s));
        s = ready(); s.keyguardLocked = false; assertFalse(AutoCalibration.shouldStart(s));
        s = ready(); s.pinPadUp = true; assertFalse(AutoCalibration.shouldStart(s)); assertTrue(AutoCalibration.decision(s).contains("PIN pad"));
        s = ready(); s.menuOpen = true; assertFalse(AutoCalibration.shouldStart(s)); assertTrue(AutoCalibration.decision(s).contains("power menu"));
        s = ready(); s.calibrating = true; assertFalse(AutoCalibration.shouldStart(s));
    }
    @Test public void backsOffBetweenTriesAndCapsPerLockAndPerDay() {
        AutoCalibration.State s = ready(); s.lastStartMs = NOW - 5_000; assertFalse(AutoCalibration.shouldStart(s));
        s.lastStartMs = NOW - AutoCalibration.MIN_GAP_MS; assertTrue(AutoCalibration.shouldStart(s));
        s = ready(); s.startsThisLock = AutoCalibration.MAX_PER_LOCK; assertFalse(AutoCalibration.shouldStart(s));
        s.startsThisLock = AutoCalibration.MAX_PER_LOCK - 1; assertTrue(AutoCalibration.shouldStart(s));
        s = ready(); s.startsToday = AutoCalibration.MAX_PER_DAY; assertFalse(AutoCalibration.shouldStart(s));
        s = ready(); s.lastStartMs = NOW + 10_000; assertTrue("a clock that went back never blocks for long", AutoCalibration.shouldStart(s));
    }
    @Test public void theDailyRecordCountsOnlyTheLastDay() {
        String t = (NOW - 100) + "," + (NOW - AutoCalibration.DAY_MS - 1) + "," + (NOW - 3_600_000) + ",garbage,";
        assertEquals(2, AutoCalibration.startsWithinDay(t, NOW));
        assertEquals(0, AutoCalibration.startsWithinDay("", NOW));
        assertEquals(0, AutoCalibration.startsWithinDay(null, NOW));
        String added = AutoCalibration.addTry(t, NOW);
        assertEquals(3, AutoCalibration.startsWithinDay(added, NOW));
        assertTrue("old entries are dropped", !added.contains(String.valueOf(NOW - AutoCalibration.DAY_MS - 1)));
        assertTrue(added.endsWith(String.valueOf(NOW)));
        assertEquals(String.valueOf(NOW), AutoCalibration.addTry(null, NOW));
    }
    @Test public void theRecordStaysSmall() {
        String t = "";
        for (int i = 0; i < 50; i++) t = AutoCalibration.addTry(t, NOW + i);
        assertTrue(t.split(",").length <= AutoCalibration.MAX_PER_DAY + 2);
    }

    // ---- review AU2
    @Test public void onlyTheSystemsOwnScreenshotsCount() {
        assertTrue(AutoCalibration.ownerOk("com.android.systemui"));
        assertTrue(AutoCalibration.ownerOk(null)); assertTrue(AutoCalibration.ownerOk(""));
        assertFalse(AutoCalibration.ownerOk("com.some.game"));
    }
    @Test public void aScreenshotDuringTheMenuOrThePadIsRefused() {
        long shot = 100_000;
        assertTrue(AutoCalibration.refusal(shot, shot - 1_000, false, 0, false, 0).contains("power menu"));
        assertTrue(AutoCalibration.refusal(shot, 0, true, 0, false, 0).contains("power menu"));
        assertTrue(AutoCalibration.refusal(shot, 0, false, shot + 2_000, false, 0).contains("PIN pad"));
        assertTrue(AutoCalibration.refusal(shot, 0, false, 0, true, 0).contains("PIN pad"));
        assertTrue(AutoCalibration.refusal(shot, 0, false, 0, false, shot + 800).contains("unlocked"));
    }
    @Test public void aScreenshotAroundTheOpenNotificationPanelIsRefused() {   // BY1 review LOW5 (2026-11-12)
        long shot = 100_000;
        assertTrue(AutoCalibration.refusal(shot, 0, false, 0, false, 0, 0, true).contains("notification panel"));
        assertTrue(AutoCalibration.refusal(shot, 0, false, 0, false, 0, shot - 1_000, false).contains("notification panel"));
        assertTrue(AutoCalibration.refusal(shot, 0, false, 0, false, 0, shot + 2_000, false).contains("notification panel"));
        assertEquals("a panel closed long before does not matter", "", AutoCalibration.refusal(shot, 0, false, 0, false, 0, shot - AutoCalibration.GUARD_MS - 1_000, false));
        assertEquals("no panel seen: the old rules only", "", AutoCalibration.refusal(shot, 0, false, 0, false, 0, 0, false));
        assertTrue("the menu still comes first", AutoCalibration.refusal(shot, 0, true, 0, false, 0, 0, true).contains("power menu"));
    }
    @Test public void aPlainLockScreenScreenshotIsAccepted() {
        long shot = 100_000;
        assertEquals("", AutoCalibration.refusal(shot, 0, false, 0, false, 0));
        assertEquals("a menu seen 10 s earlier does not matter", "", AutoCalibration.refusal(shot, shot - 10_000, false, shot - 9_000, false, shot - 20_000));
        assertEquals("an unlock long after is fine", "", AutoCalibration.refusal(shot, 0, false, 0, false, shot + 30_000));
        assertEquals("an unlock BEFORE the shot is fine", "", AutoCalibration.refusal(shot, 0, false, 0, false, shot - 5_000));
    }
    @Test public void theNearRuleIgnoresMomentsThatNeverHappened() {
        assertFalse(AutoCalibration.near(0, 1_000, 5_000)); assertFalse(AutoCalibration.near(1_000, 0, 5_000));
        assertTrue(AutoCalibration.near(1_000, 3_000, 2_500)); assertFalse(AutoCalibration.near(1_000, 4_000, 2_500));
    }
}
