package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CB2 + CE1: when the launcher turns the VPN off (the quiet world: screen dark, lock screen up, custom launcher in front, either launcher locked), and what it says. */
public class VpnOffRulesTest {
    private static final long FAR = Long.MAX_VALUE / 4;   // "no stop before"

    private static String why(boolean sw, boolean ok, boolean quiet, boolean up, int recent) { return VpnOffRules.why(sw, ok, quiet, up, recent, false, FAR); }

    @Test public void theVpnGoesOffInTheQuietWorld() {   // the world (screen dark, lock screen up, custom launcher, PIN not decided yet) is NotifQuietPolicy's: VpnOff asks it, this rule only reads the answer
        assertNull(why(true, true, true, true, 0));
        assertNull(why(true, true, true, true, 2));     // a VPN that reconnected twice is still stopped
    }

    @Test public void nothingHappensInAnUnlockedQuickstepSessionOrWithoutAVpnOrWithTheSwitchOff() {
        assertEquals(VpnOffRules.UNLOCKED, why(true, true, false, true, 0));      // CE1: "Never automatically turn off fast wifi mode ... inside quickstep launcher": the VPN is Tj's to use
        assertEquals(VpnOffRules.UNLOCKED, why(true, true, false, true, 5));      // even a VPN that was stopped often before
        assertEquals(VpnOffRules.NO_VPN, why(true, true, true, false, 0));        // nothing to do: no call, no log noise
        assertEquals(VpnOffRules.SWITCHED_OFF, why(false, true, true, true, 0));  // the switch wins over everything
        assertTrue(VpnOffRules.UNLOCKED.contains("Quickstep"));
    }

    @Test public void withoutAndroidsPermissionItSaysSoOnlyWhenAVpnIsUp() {
        assertEquals(VpnOffRules.NOT_ALLOWED, why(true, false, true, true, 0));
        assertEquals(VpnOffRules.NO_VPN, why(true, false, true, false, 0));       // no VPN: no "not allowed" line every lock
        assertTrue(VpnOffRules.worthALine(VpnOffRules.NOT_ALLOWED));
        assertFalse(VpnOffRules.worthALine(VpnOffRules.NO_VPN));
        assertFalse(VpnOffRules.worthALine(VpnOffRules.SETTLING));
    }

    @Test public void aVpnThatKeepsReconnectingIsLeftAloneAfterThreeStopsWithinTenMinutes() {
        long[] stops = new long[VpnOffRules.MAX_STOPS];
        long t = 1_000_000L;
        for (int i = 0; i < VpnOffRules.MAX_STOPS; i++) {
            assertNull(why(true, true, true, true, VpnOffRules.recentStops(stops, t)));
            VpnOffRules.recordStop(stops, t);
            t += 60_000L;   // a reconnect every minute
        }
        assertEquals(VpnOffRules.GAVE_UP, why(true, true, true, true, VpnOffRules.recentStops(stops, t)));
        assertTrue(VpnOffRules.GAVE_UP.contains("reconnected 3 times"));   // the emulator section vo greps this
    }

    @Test public void stopsHoursApartNeverReachTheCap() {   // review LOW4: overnight reconnects (Wi-Fi changes) are each stopped
        long[] stops = new long[VpnOffRules.MAX_STOPS];
        long t = 1_000_000L;
        for (int i = 0; i < 10; i++) {
            assertNull(why(true, true, true, true, VpnOffRules.recentStops(stops, t)));
            VpnOffRules.recordStop(stops, t);
            t += 2 * 60 * 60_000L;
        }
        assertEquals(0, VpnOffRules.recentStops(stops, t + VpnOffRules.WINDOW_MS));
    }

    @Test public void aSecondTriggerRightAfterAStopWaitsForTheTeardown() {   // review LOW5: one VPN must not be counted as two stops
        assertEquals(VpnOffRules.SETTLING, VpnOffRules.why(true, true, true, true, 1, false, 500));
        assertNull(VpnOffRules.why(true, true, true, true, 1, false, VpnOffRules.SETTLE_MS));
    }

    @Test public void anAlwaysOnRefusalIsNotAskedAgainUntilTheNextUnlock() {   // review LOW6: no prepare call and no log line at every trigger
        assertTrue(VpnOffRules.refusesForThisLock(VpnOffRules.REFUSED));
        assertTrue(VpnOffRules.refusesForThisLock(VpnOffRules.LOCKDOWN));
        assertFalse(VpnOffRules.refusesForThisLock(VpnOffRules.FAILED));
        assertFalse(VpnOffRules.refusesForThisLock(VpnOffRules.PREPARED));
        assertEquals(VpnOffRules.REFUSED_BEFORE, VpnOffRules.why(true, true, true, true, 0, true, FAR));
        assertEquals(VpnOffRules.UNLOCKED, VpnOffRules.why(true, true, false, true, 0, true, FAR));
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

    @Test public void theWorldWordIsQuickstepOrQuietWithAReason() {   // CE1: the contract with LinkMeter's Fast Wi-Fi (FastRules.WORLD_*), a Settings.Global string
        assertEquals("quickstep", VpnOffRules.worldValue(false, ""));
        assertEquals("quickstep", VpnOffRules.worldValue(false, "custom launcher"));   // a reason of an open world is never published
        assertEquals("quiet:custom launcher", VpnOffRules.worldValue(true, "custom launcher"));
        assertEquals("quiet:screen off", VpnOffRules.worldValue(true, " screen off "));
        assertEquals("quiet:quiet", VpnOffRules.worldValue(true, ""));
        assertEquals("quiet:quiet", VpnOffRules.worldValue(true, null));
        assertEquals("tj_launcher_world", VpnOffRules.WORLD_KEY);
    }

    @Test public void onlyTheQuietWordsMeanNoVpn() {   // a reader must never stay down for a word it does not know
        assertTrue(VpnOffRules.isQuiet("quiet"));
        assertTrue(VpnOffRules.isQuiet("quiet:lock screen"));
        assertTrue(VpnOffRules.isQuiet(VpnOffRules.worldValue(true, "unlock not decided yet")));
        assertFalse(VpnOffRules.isQuiet("quickstep"));
        assertFalse(VpnOffRules.isQuiet(VpnOffRules.worldValue(false, "")));
        assertFalse(VpnOffRules.isQuiet(null));
        assertFalse(VpnOffRules.isQuiet(""));
        assertFalse(VpnOffRules.isQuiet("quietly"));      // not a prefix match on the bare word
        assertFalse(VpnOffRules.isQuiet("Quiet:x"));      // exact words
        assertFalse(VpnOffRules.isQuiet("something else"));
    }

    @Test public void theWholeWorldRuleFromTheFactsOfEachSituation() {   // NotifQuietPolicy's facts -> the word VpnOff publishes (the banners, the radio symbols and the VPN share ONE world)
        NotifQuietPolicy.Facts f = NotifQuietPolicy.Facts.quickstepSession();
        assertFalse(NotifQuietPolicy.wanted(f));   // an unlocked, decided Quickstep session: a VPN may run
        assertEquals("quickstep", VpnOffRules.worldValue(NotifQuietPolicy.wanted(f), NotifQuietPolicy.quietFor(f)));
        f.screenOn = false;
        assertEquals("quiet:screen off", VpnOffRules.worldValue(NotifQuietPolicy.wanted(f), NotifQuietPolicy.quietFor(f)));
        f.screenOn = true; f.lockUp = true;
        assertEquals("quiet:lock screen", VpnOffRules.worldValue(NotifQuietPolicy.wanted(f), NotifQuietPolicy.quietFor(f)));
        f.lockUp = false; f.unlockUndecided = true;   // a plain PIN was typed: the custom launcher may be what opens, so no VPN yet (it would be turned off a moment later)
        assertEquals("quiet:unlock not decided yet", VpnOffRules.worldValue(NotifQuietPolicy.wanted(f), NotifQuietPolicy.quietFor(f)));
        f.unlockUndecided = false; f.quickstepMode = false;
        assertEquals("quiet:custom launcher", VpnOffRules.worldValue(NotifQuietPolicy.wanted(f), NotifQuietPolicy.quietFor(f)));
        f = NotifQuietPolicy.Facts.quickstepSession(); f.automationOff = true;   // "Restore everything" / the crash breaker: the launcher is paused, it must not touch a VPN
        assertFalse(NotifQuietPolicy.wanted(f));
        f = NotifQuietPolicy.Facts.quickstepSession(); f.helperUp = false;       // Quickstep without the helper: nothing can hear the lock screen, so no hold is taken
        assertFalse(NotifQuietPolicy.wanted(f));
    }
}
