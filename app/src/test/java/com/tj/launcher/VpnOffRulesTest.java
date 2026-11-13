package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CB2: when the launcher turns the VPN off (screen dark or lock screen up, either launcher), and what it says. */
public class VpnOffRulesTest {
    private static final long FAR = Long.MAX_VALUE / 4;   // "no stop before"

    private static String why(boolean sw, boolean ok, boolean dark, boolean lock, boolean up, int recent) { return VpnOffRules.why(sw, ok, dark, lock, up, recent, false, FAR); }

    @Test public void theVpnGoesOffWhenTheScreenIsDarkOrTheLockScreenIsUp() {
        assertNull(why(true, true, true, false, true, 0));    // screen off (the keyguard has not locked yet: Tj said "any time the screen turns off")
        assertNull(why(true, true, false, true, true, 0));    // lock screen showing on a lit screen (a wake, a boot)
        assertNull(why(true, true, true, true, true, 2));     // a VPN that reconnected twice is still stopped
    }

    @Test public void nothingHappensUnlockedOrWithoutAVpnOrWithTheSwitchOff() {
        assertEquals(VpnOffRules.UNLOCKED, why(true, true, false, false, true, 0));      // in use: the VPN is Tj's to use
        assertEquals(VpnOffRules.NO_VPN, why(true, true, true, true, false, 0));         // nothing to do: no call, no log noise
        assertEquals(VpnOffRules.SWITCHED_OFF, why(false, true, true, true, true, 0));   // the switch wins over everything
    }

    @Test public void withoutAndroidsPermissionItSaysSoOnlyWhenAVpnIsUp() {
        assertEquals(VpnOffRules.NOT_ALLOWED, why(true, false, true, true, true, 0));
        assertEquals(VpnOffRules.NO_VPN, why(true, false, true, true, false, 0));        // no VPN: no "not allowed" line every lock
        assertTrue(VpnOffRules.worthALine(VpnOffRules.NOT_ALLOWED));
        assertFalse(VpnOffRules.worthALine(VpnOffRules.NO_VPN));
        assertFalse(VpnOffRules.worthALine(VpnOffRules.SETTLING));
    }

    @Test public void aVpnThatKeepsReconnectingIsLeftAloneAfterThreeStopsWithinTenMinutes() {
        long[] stops = new long[VpnOffRules.MAX_STOPS];
        long t = 1_000_000L;
        for (int i = 0; i < VpnOffRules.MAX_STOPS; i++) {
            assertNull(why(true, true, true, true, true, VpnOffRules.recentStops(stops, t)));
            VpnOffRules.recordStop(stops, t);
            t += 60_000L;   // a reconnect every minute
        }
        assertEquals(VpnOffRules.GAVE_UP, why(true, true, true, true, true, VpnOffRules.recentStops(stops, t)));
        assertTrue(VpnOffRules.GAVE_UP.contains("reconnected 3 times"));   // the emulator section vo greps this
    }

    @Test public void stopsHoursApartNeverReachTheCap() {   // review LOW4: overnight reconnects (Wi-Fi changes) are each stopped
        long[] stops = new long[VpnOffRules.MAX_STOPS];
        long t = 1_000_000L;
        for (int i = 0; i < 10; i++) {
            assertNull(why(true, true, true, true, true, VpnOffRules.recentStops(stops, t)));
            VpnOffRules.recordStop(stops, t);
            t += 2 * 60 * 60_000L;
        }
        assertEquals(0, VpnOffRules.recentStops(stops, t + VpnOffRules.WINDOW_MS));
    }

    @Test public void aSecondTriggerRightAfterAStopWaitsForTheTeardown() {   // review LOW5: one VPN must not be counted as two stops
        assertEquals(VpnOffRules.SETTLING, VpnOffRules.why(true, true, true, true, true, 1, false, 500));
        assertNull(VpnOffRules.why(true, true, true, true, true, 1, false, VpnOffRules.SETTLE_MS));
    }

    @Test public void anAlwaysOnRefusalIsNotAskedAgainUntilTheNextUnlock() {   // review LOW6: no prepare call and no log line at every trigger
        assertTrue(VpnOffRules.refusesForThisLock(VpnOffRules.REFUSED));
        assertTrue(VpnOffRules.refusesForThisLock(VpnOffRules.LOCKDOWN));
        assertFalse(VpnOffRules.refusesForThisLock(VpnOffRules.FAILED));
        assertFalse(VpnOffRules.refusesForThisLock(VpnOffRules.PREPARED));
        assertEquals(VpnOffRules.REFUSED_BEFORE, VpnOffRules.why(true, true, true, true, true, 0, true, FAR));
        assertEquals(VpnOffRules.UNLOCKED, VpnOffRules.why(true, true, false, false, true, 0, true, FAR));
    }

    @Test public void theStatusLineSaysWhatHappenedAndNeverNamesTheVpnApp() {
        assertEquals("turned the VPN off", VpnOffRules.outcome(VpnOffRules.PREPARED, true, false));
        assertTrue(VpnOffRules.outcome(VpnOffRules.PREPARED, true, true).contains("still connected"));
        assertTrue(VpnOffRules.outcome(VpnOffRules.REFUSED, true, true).contains("Always-on VPN"));
        assertEquals(VpnOffRules.NOT_ALLOWED, VpnOffRules.outcome(VpnOffRules.REFUSED, false, true));
        assertTrue(VpnOffRules.outcome(VpnOffRules.LOCKDOWN, true, true).contains("lockdown"));
    }

    @Test public void aLateLookOfASleepingCpuIsNotBelieved() {   // review LOW11
        assertTrue(VpnOffRules.lateLookMeaningful(1_500));
        assertFalse(VpnOffRules.lateLookMeaningful(5 * 60_000L));
    }

    @Test public void releaseBuildsHaveItOnDebugBuildsOff() {
        assertTrue(VpnOffRules.defaultOn(false));
        assertFalse(VpnOffRules.defaultOn(true));
    }
}
