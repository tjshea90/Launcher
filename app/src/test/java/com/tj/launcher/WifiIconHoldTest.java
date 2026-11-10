package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * BS1: the Wi-Fi symbol hold ({@link WifiIconHold}) over in-memory stores. Wrong, these cases hide the wrong icons on Tj's tablet for good, leave his symbol hidden in Quickstep, or overwrite a list he edited:
 * each rule of the class comment is one test. {@code events} records the ORDER of disk and settings writes (the original must be on disk before the first change).
 */
public class WifiIconHoldTest {
    private final List<String> events = new ArrayList<>();

    private final class FakeState implements WifiIconHold.State {
        boolean held; String original; boolean failNextSave;
        @Override public boolean held() { return held; }
        @Override public String original() { return original; }
        @Override public void save(boolean h, String o) {
            if (failNextSave) { failNextSave = false; throw new IllegalStateException("disk full"); }
            held = h; original = o; events.add("save(" + h + "," + o + ")");
        }
    }

    private final class FakeSetting implements WifiIconHold.Setting {
        String value; boolean failNextPut;
        FakeSetting(String v) { value = v; }
        @Override public String get() { return value; }
        @Override public void put(String v) {
            if (failNextPut) { failNextPut = false; throw new SecurityException("permission revoked"); }
            value = v; events.add("put(" + v + ")");
        }
    }

    private static final WifiIconHold.Sink QUIET = new WifiIconHold.Sink() { @Override public void log(String m) { } };

    private boolean set(FakeState st, FakeSetting s, boolean permitted, boolean want) { return WifiIconHold.set(st, s, permitted, want, QUIET); }

    // ---------------------------------------------------------------- the list helpers

    @Test public void aMissingSettingStartsFromTheSystemDefaultNotFromNothing() {
        assertEquals("rotate,headset,wifi", WifiIconHold.with(null, "wifi"));
        assertTrue(WifiIconHold.hides(null, "rotate"));
        assertTrue(WifiIconHold.hides(null, "headset"));
        assertFalse(WifiIconHold.hides(null, "wifi"));
    }

    @Test public void anExistingListKeepsEveryEntryAndGetsOurs() {
        assertEquals("a,b,wifi", WifiIconHold.with("a,b", "wifi"));
        assertEquals("wifi", WifiIconHold.with("", "wifi"));
        assertEquals("a,wifi", WifiIconHold.with("a,,", "wifi"));      // empty entries are no-ops for SystemUI and are dropped
        assertEquals("a,wifi", WifiIconHold.with("a,wifi", "wifi"));   // already there: not twice
    }

    @Test public void slotNamesAreComparedExactlyLikeSystemUIDoes() {
        assertFalse(WifiIconHold.hides("wifi2,mywifi", "wifi"));
        assertFalse(WifiIconHold.hides("WIFI", "wifi"));
        assertFalse(WifiIconHold.hides(" wifi", "wifi"));   // no trimming in SystemUI's split
        assertTrue(WifiIconHold.hides("a,wifi,b", "wifi"));
    }

    @Test public void takingOurTokenOutLeavesTheRestAndANullStaysNull() {
        assertEquals("a,b", WifiIconHold.without("a,wifi,b", "wifi"));
        assertEquals("a", WifiIconHold.without("a,wifi,wifi", "wifi"));
        assertEquals("", WifiIconHold.without("wifi", "wifi"));
        assertNull(WifiIconHold.without(null, "wifi"));
    }

    // ---------------------------------------------------------------- the marker kept in Settings.Global

    @Test public void theMarkerRoundTripsEveryKindOfOriginal() {
        assertNull(WifiIconHold.decodeMarker(WifiIconHold.encodeMarker(null)));
        assertEquals("", WifiIconHold.decodeMarker(WifiIconHold.encodeMarker("")));
        assertEquals("rotate,headset", WifiIconHold.decodeMarker(WifiIconHold.encodeMarker("rotate,headset")));
        assertEquals("nfc,vpn", WifiIconHold.decodeMarker(WifiIconHold.encodeMarker("nfc,vpn")));
        assertTrue(WifiIconHold.encodeMarker(null).length() > 0);   // a marker is never empty: "present" is what means "held"
    }

    @Test public void anUnreadableMarkerCountsAsTheSystemDefault() {
        assertNull(WifiIconHold.decodeMarker(null));
        assertNull(WifiIconHold.decodeMarker(""));
        assertNull(WifiIconHold.decodeMarker("garbage"));
    }

    // ---------------------------------------------------------------- the radio rule

    @Test public void wifiStaysOnOnlyWhileTheSymbolCanReallyBeHidden() {
        assertTrue(WifiIconHold.keepsWifiOn(true, true, false));
        assertFalse(WifiIconHold.keepsWifiOn(false, true, false));   // switch off: today's behaviour
        assertFalse(WifiIconHold.keepsWifiOn(true, false, false));   // no adb permission: the symbol would show on the lock screen, so Wi-Fi goes off like before
        assertFalse(WifiIconHold.keepsWifiOn(true, true, true));     // paused / breaker tripped
    }

    @Test public void theSwitchIsOnInTheShippedAppAndOffInDebug() {
        assertTrue(WifiIconHold.defaultOn(false));
        assertFalse(WifiIconHold.defaultOn(true));
    }

    // ---------------------------------------------------------------- taking the hold

    @Test public void theOriginalIsOnDiskBeforeTheFirstWrite() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc,vpn");
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,vpn,wifi", s.value);
        assertTrue(st.held);
        assertEquals("nfc,vpn", st.original);
        assertEquals("[save(true,nfc,vpn), put(nfc,vpn,wifi)]", events.toString());
    }

    @Test public void aMissingSettingIsARealOriginalAndTheDefaultListIsKept() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting(null);
        assertTrue(set(st, s, true, true));
        assertEquals("rotate,headset,wifi", s.value);
        assertTrue(st.held);
        assertNull(st.original);
        assertEquals("[save(true,null), put(rotate,headset,wifi)]", events.toString());
    }

    @Test public void aListThatAlreadyHidesTheSymbolIsLeftAloneAndNothingIsHeld() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("rotate,wifi");
        assertFalse(set(st, s, true, true));
        assertFalse(st.held);
        assertEquals("rotate,wifi", s.value);
        assertTrue(events.isEmpty());
    }

    @Test public void aHoldThatIsAlreadyOnRewritesNothing() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        events.clear();
        assertTrue(set(st, s, true, true));
        assertTrue(events.isEmpty());
        assertEquals("nfc,wifi", s.value);
    }

    @Test public void aHoldWhoseValueWasResetIsTakenAgainWithTheNewestOriginal() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        s.value = "nfc,vpn";   // something rewrote the list without our token
        events.clear();
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,vpn,wifi", s.value);
        assertEquals("nfc,vpn", st.original);   // the newest value without our token is the original now
        assertEquals("[save(true,nfc,vpn), put(nfc,vpn,wifi)]", events.toString());
    }

    @Test public void withoutThePermissionNothingIsTakenAndAHeldHoldStays() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertFalse(set(st, s, false, true));
        assertEquals("nfc", s.value);
        assertTrue(events.isEmpty());
        assertTrue(set(st, s, true, true));
        events.clear();
        assertTrue(set(st, s, false, true));   // permission gone while held: the hold is reported as still on
        assertTrue(events.isEmpty());
    }

    @Test public void aFailedWriteWhileTakingTheHoldIsUndoneSoNothingLooksHeld() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        s.failNextPut = true;
        assertFalse(set(st, s, true, true));
        assertFalse(st.held);
        assertEquals("nfc", s.value);
        assertEquals("[save(true,nfc), save(false,null)]", events.toString());
    }

    @Test public void aFailedSaveMeansNoWriteAtAll() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        st.failNextSave = true;
        assertFalse(set(st, s, true, true));
        assertEquals("nfc", s.value);
        assertFalse(st.held);
        assertEquals("[save(false,null)]", events.toString());   // only the clean-up of a possibly half-written marker; the setting itself was never written
    }

    // ---------------------------------------------------------------- ending the hold

    @Test public void endingTheHoldPutsTheExactOriginalBack() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc,vpn");
        set(st, s, true, true);
        events.clear();
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,vpn", s.value);
        assertFalse(st.held);
        assertEquals("[put(nfc,vpn), save(false,null)]", events.toString());   // the hold is cleared only AFTER the write
    }

    @Test public void aMissingOriginalIsRestoredByDeletingTheSetting() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting(null);
        set(st, s, true, true);
        assertFalse(set(st, s, true, false));
        assertNull(s.value);
        assertFalse(st.held);
    }

    @Test public void aListEditedMeanwhileLosesOnlyOurToken() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.value = "nfc,wifi,vpn";   // Tj (or a tool) added vpn while we held
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,vpn", s.value);
        assertFalse(st.held);
    }

    @Test public void aListThatNoLongerHidesTheSymbolIsNotTouchedWhenTheHoldEnds() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.value = "vpn";   // somebody replaced the list: nothing of ours is left in it
        events.clear();
        assertFalse(set(st, s, true, false));
        assertEquals("vpn", s.value);
        assertFalse(st.held);
        assertEquals("[save(false,null)]", events.toString());
    }

    @Test public void withoutThePermissionAHoldCannotEndAndItsOriginalWaits() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        events.clear();
        assertTrue(set(st, s, false, false));
        assertTrue(st.held);
        assertEquals("nfc", st.original);
        assertEquals("nfc,wifi", s.value);
        assertTrue(events.isEmpty());
    }

    @Test public void aFailedWriteWhileEndingKeepsTheHoldSoTheNextTriggerTriesAgain() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.failNextPut = true;
        assertTrue(set(st, s, true, false));
        assertTrue(st.held);
        assertEquals("nfc,wifi", s.value);
        assertFalse(set(st, s, true, false));   // the next trigger succeeds
        assertEquals("nfc", s.value);
        assertFalse(st.held);
    }

    @Test public void nothingHeldAndNothingWantedDoesNothing() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertFalse(set(st, s, true, false));
        assertFalse(set(st, s, false, false));
        assertTrue(events.isEmpty());
        assertEquals("nfc", s.value);
    }

    @Test public void takeThenEndThenTakeAgainIsStable() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting(null);
        for (int i = 0; i < 3; i++) {
            assertTrue(set(st, s, true, true));
            assertEquals("rotate,headset,wifi", s.value);
            assertFalse(set(st, s, true, false));
            assertNull(s.value);
        }
    }
}
