package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** AK1: the lock screen shows no Wi-Fi, no Bluetooth and no app notifications, from any launcher. The Android side is proven by emulator section lq; these are the pure rules. */
public class LockQuietTest {
    private static final long NOW = 1_000_000;

    // ---- AW3: while the keyguard is up a radio is switched off again, spaced MIN_GAP_MS apart, and never given up on (v0.8.0 stopped after 3 a minute)
    @Test public void aRadioThatComesOnWhileLockedIsSwitchedOffAtOnce() { assertEquals(0, LockQuiet.retryDelayMs(true, true, false, NOW, 0, 0)); }
    @Test public void neverWhileTheTabletIsUnlocked() { assertEquals(-1, LockQuiet.retryDelayMs(true, false, false, NOW, 0, 0)); }
    @Test public void neverWithTheSwitchOff() { assertEquals(-1, LockQuiet.retryDelayMs(false, true, false, NOW, 0, 0)); }
    @Test public void neverWhileAutomationIsPaused() { assertEquals(-1, LockQuiet.retryDelayMs(true, true, true, NOW, 0, 0)); }
    @Test public void aRadioThatKeepsComingBackIsSwitchedOffAgainAfterTheGapNeverGivenUpOn() {
        assertEquals("1 s after the last switch-off: wait for the rest of the gap", LockQuiet.MIN_GAP_MS - 1000, LockQuiet.retryDelayMs(true, true, false, NOW, NOW - 1000, 1));
        assertEquals("the gap has passed: now", 0, LockQuiet.retryDelayMs(true, true, false, NOW, NOW - LockQuiet.MIN_GAP_MS, 1));
        assertEquals(0, LockQuiet.retryDelayMs(true, true, false, NOW, NOW - 59_000, 12));   // v0.8.0 would have given up here (3 per minute)
        assertEquals(0, LockQuiet.retryDelayMs(true, true, false, NOW, NOW - 5_000, LockQuiet.MAX_PER_LOCK - 1));
    }
    @Test public void aRomThatReallyInsistsEndsAtTheCapOfOneLock() {
        assertEquals(-1, LockQuiet.retryDelayMs(true, true, false, NOW, NOW - 60_000, LockQuiet.MAX_PER_LOCK));
        assertTrue("a whole minute of switch-offs is allowed", LockQuiet.MAX_PER_LOCK * LockQuiet.MIN_GAP_MS >= 60_000);
    }
    @Test public void aClockThatWentBackNeverWaitsForLong() { assertEquals(0, LockQuiet.retryDelayMs(true, true, false, NOW, NOW + 50_000, 3)); }

    @Test public void theMemoryOfWhatWasOnRoundTrips() {
        assertEquals("w1b0", LockQuiet.encodePrev(true, false));
        assertEquals("w0b1", LockQuiet.encodePrev(false, true));
        assertTrue(LockQuiet.prevWifi("w1b0")); assertFalse(LockQuiet.prevBluetooth("w1b0"));
        assertTrue(LockQuiet.prevBluetooth("w0b1")); assertFalse(LockQuiet.prevWifi("w0b1"));
        assertFalse(LockQuiet.prevWifi(null)); assertFalse(LockQuiet.prevBluetooth(""));
    }

    @Test public void aGestureUnlockIntoQuickstepPutsWhatWasOnBack() { assertTrue(LockQuiet.shouldRestore(true, true, true, false, "w1b0")); }
    @Test public void aPlainUnlockIntoCustomLeavesThemOff() { assertFalse(LockQuiet.shouldRestore(true, true, false, false, "w1b1")); }
    @Test public void nothingToRestoreWhenNothingWasOn() { assertFalse(LockQuiet.shouldRestore(true, true, true, false, "w0b0")); assertFalse(LockQuiet.shouldRestore(true, true, true, false, "")); }
    @Test public void restoreNeedsBothSwitchesAndNoPause() {
        assertFalse(LockQuiet.shouldRestore(false, true, true, false, "w1b0"));
        assertFalse(LockQuiet.shouldRestore(true, false, true, false, "w1b0"));
        assertFalse(LockQuiet.shouldRestore(true, true, true, true, "w1b0"));
    }

    @Test public void theFirstLockMomentRecordsAndARadioThatIsOnNowRefreshesIt() {
        assertTrue(LockQuiet.shouldRecordPrev("", false, false));
        assertTrue(LockQuiet.shouldRecordPrev(null, false, false));
        assertFalse("the second lock moment of the same lock keeps the first memory", LockQuiet.shouldRecordPrev("w1b0", false, false));
        assertTrue("a radio that is on right now is the freshest truth", LockQuiet.shouldRecordPrev("w0b0", true, false));
    }

    /** Review of BS2, finding 1: a lock memory left over from an unlock the helper never saw must not survive a helper connect on an UNLOCKED, lit screen (it would put a radio back much later). */
    @Test public void aLockMemoryIsForgottenOnlyWhenNoLockIsInProgress() {
        assertTrue("the helper connected on a lit, unlocked tablet: the memory is a leftover", LockQuiet.shouldForgetPrev(false, true, "w1b1"));
        assertTrue(LockQuiet.shouldForgetPrev(false, true, "w0b1"));
        assertFalse("on the lock screen the memory belongs to this lock", LockQuiet.shouldForgetPrev(true, true, "w1b1"));
        assertFalse("a dark screen may be the start of a lock", LockQuiet.shouldForgetPrev(false, false, "w1b1"));
        assertFalse("a dark screen with the keyguard up", LockQuiet.shouldForgetPrev(true, false, "w1b1"));
        assertFalse("nothing stored: nothing to forget", LockQuiet.shouldForgetPrev(false, true, ""));
        assertFalse(LockQuiet.shouldForgetPrev(false, true, null));
    }

    @Test public void theNamesAreAndroidsOwnAndTheSwitchKeysAreStable() {
        assertEquals("lock_screen_show_notifications", LockQuiet.SHOW);
        assertEquals("lock_screen_allow_private_notifications", LockQuiet.PRIVATE);
        assertEquals("lock_quiet", LockQuiet.KEY);
        assertEquals("lock_quiet_restore", LockQuiet.RESTORE_KEY);
    }
}
