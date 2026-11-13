package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CB2: when the launcher turns the VPN off (screen dark or lock screen up), and what it says. */
public class VpnOffRulesTest {

    @Test public void theVpnGoesOffWhenTheScreenIsDarkOrTheLockScreenIsUp() {
        assertNull(VpnOffRules.why(true, true, true, false, true, 0));    // screen off (the keyguard has not locked yet: Tj said "any time the screen turns off")
        assertNull(VpnOffRules.why(true, true, false, true, true, 0));    // lock screen showing on a lit screen (a wake, a boot)
        assertNull(VpnOffRules.why(true, true, true, true, true, 2));     // a VPN that reconnected twice is still stopped
    }

    @Test public void nothingHappensUnlockedOrWithoutAVpnOrWithTheSwitchOff() {
        assertEquals(VpnOffRules.UNLOCKED, VpnOffRules.why(true, true, false, false, true, 0));      // in use: the VPN is Tj's to use
        assertEquals(VpnOffRules.NO_VPN, VpnOffRules.why(true, true, true, true, false, 0));         // nothing to do: no call, no log noise
        assertEquals(VpnOffRules.SWITCHED_OFF, VpnOffRules.why(false, true, true, true, true, 0));   // the switch wins over everything
    }

    @Test public void withoutAndroidsPermissionItSaysSoOnlyWhenAVpnIsUp() {
        assertEquals(VpnOffRules.NOT_ALLOWED, VpnOffRules.why(true, false, true, true, true, 0));
        assertEquals(VpnOffRules.NO_VPN, VpnOffRules.why(true, false, true, true, false, 0));        // no VPN: no "not allowed" line every lock
    }

    @Test public void aVpnThatKeepsReconnectingIsLeftAloneAfterTheCapUntilTheNextUnlock() {
        int n = 0;
        for (int i = 0; i < VpnOffRules.MAX_STOPS_PER_LOCK; i++) {
            assertNull(VpnOffRules.why(true, true, true, true, true, n));
            n = VpnOffRules.afterAttempt(n, VpnOffRules.PREPARED);
        }
        assertEquals(VpnOffRules.GAVE_UP, VpnOffRules.why(true, true, true, true, true, n));
        assertTrue(VpnOffRules.GAVE_UP.contains(String.valueOf(VpnOffRules.MAX_STOPS_PER_LOCK)));
    }

    @Test public void onlyARealStopCountsTowardTheCap() {
        assertEquals(0, VpnOffRules.afterAttempt(0, VpnOffRules.REFUSED));
        assertEquals(0, VpnOffRules.afterAttempt(0, VpnOffRules.LOCKDOWN));
        assertEquals(0, VpnOffRules.afterAttempt(0, VpnOffRules.FAILED));
        assertEquals(1, VpnOffRules.afterAttempt(0, VpnOffRules.PREPARED));
    }

    @Test public void theStatusLineSaysWhatHappenedAndNeverNamesTheVpnApp() {
        assertEquals("turned the VPN off", VpnOffRules.outcome(VpnOffRules.PREPARED, true, false));
        assertTrue(VpnOffRules.outcome(VpnOffRules.PREPARED, true, true).contains("still connected"));
        assertTrue(VpnOffRules.outcome(VpnOffRules.REFUSED, true, true).contains("Always-on VPN"));
        assertEquals(VpnOffRules.NOT_ALLOWED, VpnOffRules.outcome(VpnOffRules.REFUSED, false, true));
        assertTrue(VpnOffRules.outcome(VpnOffRules.LOCKDOWN, true, true).contains("lockdown"));
    }

    @Test public void releaseBuildsHaveItOnDebugBuildsOff() {
        assertTrue(VpnOffRules.defaultOn(false));
        assertFalse(VpnOffRules.defaultOn(true));
    }
}
