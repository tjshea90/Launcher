package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ProfileSessionTest {
    @Test public void prearmOnlyOnTheLockScreenAndOncePerLock() {
        assertTrue(ProfileSession.prearmCustomStackOnLockedWake(true, true, false, false, false));
        assertFalse(ProfileSession.prearmCustomStackOnLockedWake(true, false, false, false, false));   // not locked: never yank an app away
        assertFalse(ProfileSession.prearmCustomStackOnLockedWake(true, true, false, true, false));     // already done this lock
        assertFalse(ProfileSession.prearmCustomStackOnLockedWake(false, true, false, false, false));   // switched off
    }
    @Test public void aHandshakeOrACrashTripSuppressesThePrearm() {
        assertFalse(ProfileSession.prearmCustomStackOnLockedWake(true, true, true, false, false));
        assertFalse(ProfileSession.prearmCustomStackOnLockedWake(true, true, false, false, true));
    }
    @Test public void plainUnlockEndsQuickstepAlwaysUnlessAutomationIsPaused() {
        assertTrue(ProfileSession.plainUnlockEndsQuickstep(false, false));
        assertFalse(ProfileSession.plainUnlockEndsQuickstep(true, false));    // already custom
        assertFalse(ProfileSession.plainUnlockEndsQuickstep(false, true));    // paused (Restore everything / the crash breaker): stay on the safe stock launcher
    }
    @Test public void coverAtEnterNeedsAPlainPinAndWorkToDo() {
        assertTrue(ProfileSession.coverAtEnter(false, false, false, true, true));
        assertFalse(ProfileSession.coverAtEnter(false, false, true, true, true));    // handshake: going to Quickstep, nothing to hide
        assertFalse(ProfileSession.coverAtEnter(false, false, false, false, true));  // nothing to clean
        assertFalse(ProfileSession.coverAtEnter(false, false, false, true, false));  // cover switched off
        assertFalse(ProfileSession.coverAtEnter(true, false, false, true, true));    // already custom
        assertFalse(ProfileSession.coverAtEnter(false, true, false, true, true));    // automation paused
    }
    @Test public void radiosAreEnforcedInCustomOnlyAndNeverInALoop() {
        long now = 100_000;
        assertTrue(ProfileSession.enforceRadios(true, true, now, new long[] {0, 0, 0}));
        assertFalse(ProfileSession.enforceRadios(true, false, now, new long[] {0, 0, 0}));
        assertFalse(ProfileSession.enforceRadios(false, true, now, new long[] {0, 0, 0}));
        assertFalse(ProfileSession.enforceRadios(true, true, now, new long[] {now - 1000, now - 2000, now - 3000}));
        assertTrue(ProfileSession.enforceRadios(true, true, now, new long[] {now - 70_000, now - 2000, now - 3000}));
    }
    @Test public void homeFailsafeOnlyWhenTheHelperIsOffAndThreeQuickPresses() {
        assertTrue(ProfileSession.homeFailsafe(false, true, 2400, 1000, 1700));     // three presses, 700 ms apart, helper off
        assertFalse(ProfileSession.homeFailsafe(true, true, 2400, 1000, 1700));     // helper ON: the handshake is the only way out
        assertFalse(ProfileSession.homeFailsafe(false, false, 2400, 1000, 1700));   // already Quickstep
        assertFalse(ProfileSession.homeFailsafe(false, true, 2400, 1000, 1200 + 1000)); // too slow between the last two
        assertFalse(ProfileSession.homeFailsafe(false, true, 5000, 1000, 1700));    // the last press is old
        assertFalse(ProfileSession.homeFailsafe(false, true, 1500, 0, 1000));       // only two presses so far
    }
    @Test public void aNewBootEndsAQuickstepSessionUnlessAutomationIsPaused() {
        assertTrue(ProfileSession.bootEndsQuickstep(41, 42, false, false));
        assertFalse(ProfileSession.bootEndsQuickstep(42, 42, false, false));    // same boot
        assertFalse(ProfileSession.bootEndsQuickstep(41, 42, true, false));     // already custom
        assertFalse(ProfileSession.bootEndsQuickstep(41, 42, false, true));     // paused (Restore everything / crash breaker): stay stock
        assertFalse(ProfileSession.bootEndsQuickstep(41, -1, false, false));    // counter unreadable
    }
    @Test public void preCleanAtLockNeedsEverythingToLineUp() {
        assertTrue(ProfileSession.preCleanAtLock(true, false, false, true, false, true, false));
        assertFalse(ProfileSession.preCleanAtLock(false, false, false, true, false, true, false));   // switched off
        assertFalse(ProfileSession.preCleanAtLock(true, true, false, true, false, true, false));      // already custom: nothing of Quickstep to clean
        assertFalse(ProfileSession.preCleanAtLock(true, false, true, true, false, true, false));      // automation paused: stay on the stock launcher
        assertFalse(ProfileSession.preCleanAtLock(true, false, false, false, false, true, false));    // not locked: a plain wake must keep Quickstep's apps
        assertFalse(ProfileSession.preCleanAtLock(true, false, false, true, true, true, false));      // a handshake is armed: the target is Quickstep
        assertFalse(ProfileSession.preCleanAtLock(true, false, false, true, false, false, false));    // no shell helper: the cover path at unlock
        assertFalse(ProfileSession.preCleanAtLock(true, false, false, true, false, true, true));      // done already this lock
    }

    /**
     * CY1 B2 / B3 (2026-11-24): TjAccessibilityService.lockPrepare and coverAtEnter skip the shell-helper ping (the expensive ARGUMENT of these two rules) when the mode is custom.
     * That is only equivalent if the rules are false for EVERY other input in custom mode. Exhaustive truth table; plainUnlockEndsQuickstep is the first term of coverAtEnter.
     */
    @Test public void customModeMakesPreCleanAndCoverFalseForEveryOtherInput_soTheHelperPingMaySkip() {
        for (int m = 0; m < 64; m++) {
            boolean enabled = (m & 1) != 0, paused = (m & 2) != 0, locked = (m & 4) != 0, armed = (m & 8) != 0, ready = (m & 16) != 0, done = (m & 32) != 0;
            assertFalse("preCleanAtLock must be false in custom mode, m=" + m, ProfileSession.preCleanAtLock(enabled, true, paused, locked, armed, ready, done));
        }
        for (int m = 0; m < 8; m++) {
            boolean paused = (m & 1) != 0, handshake = (m & 2) != 0, cover = (m & 4) != 0;
            assertFalse(ProfileSession.plainUnlockEndsQuickstep(true, paused));
            for (int w = 0; w < 2; w++) assertFalse("coverAtEnter must be false in custom mode, m=" + m, ProfileSession.coverAtEnter(true, paused, handshake, w == 1, cover));
        }
        // and the guard is exactly the first term: whenever plainUnlockEndsQuickstep is false, coverAtEnter is false whatever the rest says
        for (int m = 0; m < 32; m++) {
            boolean custom = (m & 1) != 0, paused = (m & 2) != 0, handshake = (m & 4) != 0, work = (m & 8) != 0, cover = (m & 16) != 0;
            if (!ProfileSession.plainUnlockEndsQuickstep(custom, paused)) assertFalse(ProfileSession.coverAtEnter(custom, paused, handshake, work, cover));
        }
    }

    // ---- AC9 (v0.7.3): the gesture's choice always wins
    @Test public void aHandshakeAlwaysStartsQuickstepBehindTheLockWhateverThePrearmSwitchSays() { assertTrue(ProfileSession.stockBehindLockOnHandshake()); }
    @Test public void quickstepIsStartedAgainOnlyWhenOurLauncherOrTheStandInIsInFront() {
        assertTrue(ProfileSession.restartQuickstepAfterUnlock(true, false, 0));    // the custom home came up late
        assertTrue(ProfileSession.restartQuickstepAfterUnlock(false, true, 0));    // the Solitaire stand-in came up late
        assertFalse(ProfileSession.restartQuickstepAfterUnlock(false, false, 0));  // Quickstep (or any other app Tj opened) is in front: leave it alone
        assertTrue(ProfileSession.restartQuickstepAfterUnlock(true, false, 1));    // second look
        assertFalse(ProfileSession.restartQuickstepAfterUnlock(true, true, 2));    // never more than two restarts
        assertEquals(2, ProfileSession.VERIFY_DELAYS_MS.length);
    }
    @Test public void theOldEndOnLockSwitchIsGone() throws Exception {
        for (java.lang.reflect.Field f : ProfileSession.class.getDeclaredFields()) org.junit.Assert.assertFalse(f.getName(), f.getName().equals("END_ON_LOCK_KEY"));
    }

    @Test public void retiredDemoFlagCleanupOnlyUndoesWhatWasOurs() {
        assertEquals("0", DemoModeProfile.undoValue("1", true, null));          // it was unset before: back to 0
        assertEquals("0", DemoModeProfile.undoValue("1", true, "0"));
        assertEquals("2", DemoModeProfile.undoValue("1", true, "2"));           // whatever it was before
        org.junit.Assert.assertNull(DemoModeProfile.undoValue("1", false, null));   // not ours: never touched
        org.junit.Assert.assertNull(DemoModeProfile.undoValue("0", true, null));    // already off
        org.junit.Assert.assertNull(DemoModeProfile.undoValue(null, true, null));
        org.junit.Assert.assertNull(DemoModeProfile.undoValue("1", true, "1"));     // it was on before we ever touched it: leave it
    }

    // ---- AC2 follow-up (2026-11-06, run 143): what appears under the lock screen is not "opened in Quickstep"
    @Test public void aWindowUnderTheLockScreenIsNotAnAppTjOpened() {
        assertTrue(ProfileSession.windowIsLockLeftover(true, false, 10_000, 0));      // the lock screen is up
        assertTrue(ProfileSession.windowIsLockLeftover(false, true, 10_000, 0));      // a handshake is armed: the unlock is about to happen
    }

    @Test public void theUnlockAnimationStillRevealsItForAMoment() {
        assertTrue(ProfileSession.windowIsLockLeftover(false, false, 10_300, 10_000));                                   // 300 ms after the unlock
        assertTrue(ProfileSession.windowIsLockLeftover(false, false, 10_000 + ProfileSession.UNLOCK_QUIET_MS - 1, 10_000));
        assertFalse(ProfileSession.windowIsLockLeftover(false, false, 10_000 + ProfileSession.UNLOCK_QUIET_MS, 10_000));   // after that an app is an app Tj opened
        assertFalse(ProfileSession.windowIsLockLeftover(false, false, 17_000, 10_000));                                  // 7 s later (the emulator test's Quickstep app)
    }

    @Test public void anAppOpenedInQuickstepWithNoLockInvolvedIsCounted() {
        assertFalse(ProfileSession.windowIsLockLeftover(false, false, 10_000, 0));    // no unlock ever happened, nothing armed
        assertFalse(ProfileSession.windowIsLockLeftover(false, false, 5_000, 9_000)); // the clock went backwards: never block forever
    }
}
