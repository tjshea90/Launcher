package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** CS2: every rule of the automatic power-menu prime that can be decided without a device. */
public class PowerMenuPrimeTest {

    private static PowerMenuPrime.Facts quiet() {
        PowerMenuPrime.Facts f = new PowerMenuPrime.Facts();
        f.wanted = true; f.switchOn = true; f.coverMode = true; f.customMode = true; f.homeInFront = true; f.screenOn = true;
        return f;
    }

    // ---------------------------------------------------------------- when it starts

    @Test public void quietCustomHomeGoes() {
        assertEquals(PowerMenuPrime.GO, PowerMenuPrime.start(quiet(), 1300).verdict);
    }

    @Test public void defaultIsOnInReleaseAndOffInDebug() {
        assertTrue(PowerMenuPrime.defaultOn(false));
        assertFalse(PowerMenuPrime.defaultOn(true));
        assertEquals("prime_menu", PowerMenuPrime.KEY);   // stored in the preferences: renaming it would reset Tj's choice
    }

    @Test public void permanentReasonsStopTheRun() {
        for (int i = 0; i < 9; i++) {
            PowerMenuPrime.Facts f = quiet();
            switch (i) {
                case 0: f.wanted = false; break;
                case 1: f.switchOn = false; break;
                case 2: f.automationOff = true; break;
                case 3: f.coverMode = false; break;
                case 4: f.customMode = false; break;
                case 5: f.keyguardLocked = true; break;
                case 6: f.pinPad = true; break;
                case 7: f.screenOn = false; break;
                default: f.msSinceLastEnd = PowerMenuPrime.MIN_GAP_MS - 1; break;
            }
            PowerMenuPrime.Start s = PowerMenuPrime.start(f, 0);
            assertEquals("case " + i + ": " + s.why, PowerMenuPrime.STOP, s.verdict);
        }
    }

    @Test public void gapOfAMinuteIsEnough() {
        PowerMenuPrime.Facts f = quiet();
        f.msSinceLastEnd = PowerMenuPrime.MIN_GAP_MS;
        assertEquals(PowerMenuPrime.GO, PowerMenuPrime.start(f, 0).verdict);
    }

    @Test public void transientReasonsWaitThenGiveUp() {
        for (int i = 0; i < 5; i++) {
            PowerMenuPrime.Facts f = quiet();
            switch (i) {
                case 0: f.menuOpen = true; break;
                case 1: f.coverUp = true; break;
                case 2: f.otherStage = true; break;
                case 3: f.homeInFront = false; break;
                default: f.homeBusy = true; break;
            }
            assertEquals("case " + i, PowerMenuPrime.WAIT, PowerMenuPrime.start(f, PowerMenuPrime.START_WAIT_MAX_MS - 1).verdict);
            PowerMenuPrime.Start late = PowerMenuPrime.start(f, PowerMenuPrime.START_WAIT_MAX_MS);
            assertEquals("case " + i, PowerMenuPrime.STOP, late.verdict);
            assertTrue(late.why, late.why.startsWith("gave up"));
        }
    }

    @Test public void anOpenMenuIsNeverOpenedOver() {
        PowerMenuPrime.Facts f = quiet();
        f.menuOpen = true;
        assertFalse(PowerMenuPrime.start(f, 0).verdict == PowerMenuPrime.GO);
    }

    // ---------------------------------------------------------------- the run: open, hold, close

    @Test public void waitsForTheMenuThenHolds() {
        PowerMenuPrime.Step s = PowerMenuPrime.step(PowerMenuPrime.OPENING, 300, 300, false, true, 0);
        assertEquals(PowerMenuPrime.OPENING, s.phase);
        assertEquals(PowerMenuPrime.ACT_NONE, s.action);
        s = PowerMenuPrime.step(PowerMenuPrime.OPENING, 400, 400, true, true, 0);
        assertEquals(PowerMenuPrime.HOLDING, s.phase);
        assertEquals(PowerMenuPrime.ACT_NONE, s.action);
    }

    @Test public void aMenuThatNeverComesEndsTheRunWithoutAnyBack() {
        PowerMenuPrime.Step s = PowerMenuPrime.step(PowerMenuPrime.OPENING, PowerMenuPrime.OPEN_WAIT_MS, PowerMenuPrime.OPEN_WAIT_MS, false, true, 0);
        assertEquals(PowerMenuPrime.ACT_END, s.action);
    }

    @Test public void holdsThenPressesBackOnce() {
        PowerMenuPrime.Step s = PowerMenuPrime.step(PowerMenuPrime.HOLDING, PowerMenuPrime.HOLD_MS - 1, 1500, true, true, 0);
        assertEquals(PowerMenuPrime.ACT_NONE, s.action);
        s = PowerMenuPrime.step(PowerMenuPrime.HOLDING, PowerMenuPrime.HOLD_MS, 1500, true, true, 0);
        assertEquals(PowerMenuPrime.CLOSING, s.phase);
        assertEquals(PowerMenuPrime.ACT_BACK, s.action);
    }

    @Test public void theHoldIsCutShortWhenTheRunIsLate() {
        PowerMenuPrime.Step s = PowerMenuPrime.step(PowerMenuPrime.HOLDING, 10, PowerMenuPrime.CAP_MS - 1000, true, true, 0);
        assertEquals(PowerMenuPrime.ACT_BACK, s.action);
    }

    @Test public void backNeverGoesToAScreenThatIsNotPlain() {
        PowerMenuPrime.Step s = PowerMenuPrime.step(PowerMenuPrime.HOLDING, PowerMenuPrime.HOLD_MS, 1500, true, false, 0);
        assertEquals(PowerMenuPrime.ACT_END, s.action);
        s = PowerMenuPrime.step(PowerMenuPrime.CLOSING, PowerMenuPrime.CLOSE_STEP_MS, 2500, true, false, 1);
        assertEquals(PowerMenuPrime.ACT_END, s.action);
    }

    @Test public void aMenuSomethingElseClosedNeedsNoBack() {
        assertEquals(PowerMenuPrime.ACT_END, PowerMenuPrime.step(PowerMenuPrime.HOLDING, 100, 800, false, true, 0).action);
        assertEquals(PowerMenuPrime.ACT_END, PowerMenuPrime.step(PowerMenuPrime.CLOSING, 100, 1800, false, true, 1).action);
    }

    @Test public void closesWithAtMostThreeBacks() {
        assertEquals(PowerMenuPrime.ACT_NONE, PowerMenuPrime.step(PowerMenuPrime.CLOSING, PowerMenuPrime.CLOSE_STEP_MS - 1, 2000, true, true, 1).action);
        assertEquals(PowerMenuPrime.ACT_BACK, PowerMenuPrime.step(PowerMenuPrime.CLOSING, PowerMenuPrime.CLOSE_STEP_MS, 2000, true, true, 1).action);
        PowerMenuPrime.Step s = PowerMenuPrime.step(PowerMenuPrime.CLOSING, PowerMenuPrime.CLOSE_STEP_MS, 3500, true, true, PowerMenuPrime.CLOSE_TRIES);
        assertEquals(PowerMenuPrime.ACT_END, s.action);
        assertTrue(s.why, s.why.contains("tap outside"));
    }

    /** A whole run on a clock, the menu coming after 250 ms and closing 150 ms after the first BACK: the run ends well inside the cover's own 4.5 s failsafe. */
    @Test public void aNormalRunEndsInsideTheCap() {
        int phase = PowerMenuPrime.OPENING; long phaseAt = 0, openAt = 250, closeAfterBack = 150; int backs = 0; long backAt = -1;
        for (long t = 0; t < 10_000; t += PowerMenuPrime.TICK_MS) {
            boolean present = t >= openAt && (backAt < 0 || t < backAt + closeAfterBack);
            PowerMenuPrime.Step s = PowerMenuPrime.step(phase, t - phaseAt, t, present, true, backs);
            if (s.action == PowerMenuPrime.ACT_BACK) { backs++; backAt = t; phaseAt = t; }
            else if (s.phase != phase) phaseAt = t;
            phase = s.phase;
            if (s.action == PowerMenuPrime.ACT_END) {
                assertTrue("ended at " + t, t < PowerMenuPrime.CAP_MS);
                assertEquals(1, backs);
                assertEquals("closed", s.why);
                return;
            }
        }
        throw new AssertionError("the run never ended");
    }

    /** The worst case: a menu that ignores every BACK still ends the run in bounded time, with exactly CLOSE_TRIES presses. */
    @Test public void aStubbornMenuStillEndsTheRun() {
        int phase = PowerMenuPrime.OPENING; long phaseAt = 0; int backs = 0;
        for (long t = 0; t < 20_000; t += PowerMenuPrime.TICK_MS) {
            PowerMenuPrime.Step s = PowerMenuPrime.step(phase, t - phaseAt, t, true, true, backs);
            if (s.action == PowerMenuPrime.ACT_BACK) { backs++; phaseAt = t; }
            else if (s.phase != phase) phaseAt = t;
            phase = s.phase;
            if (s.action == PowerMenuPrime.ACT_END) {
                assertEquals(PowerMenuPrime.CLOSE_TRIES, backs);
                assertTrue("ended at " + t, t <= PowerMenuPrime.CAP_MS + 1500);
                return;
            }
        }
        throw new AssertionError("the run never ended");
    }

    @Test public void summaryLine() {
        assertEquals("12:00:01 opened and closed the stock menu in 1400 ms", PowerMenuPrime.summary("12:00:01", "closed", true, 1400));
        assertEquals("12:00:01 asked for the stock menu, ended after 2600 ms: no menu window came within 2500 ms; nothing to close", PowerMenuPrime.summary("12:00:01", "no menu window came within 2500 ms; nothing to close", true, 2600));
        assertEquals("12:00:01 did not open the menu: the switch is off", PowerMenuPrime.summary("12:00:01", "the switch is off", false, 0));
    }
}
