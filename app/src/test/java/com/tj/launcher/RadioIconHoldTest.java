package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * BS1 + BS2: the Wi-Fi / Bluetooth symbol hold ({@link RadioIconHold}) over in-memory stores. Wrong, these cases hide the wrong icons on Tj's tablet for good, leave his symbols hidden in Quickstep, or overwrite a
 * list he edited: each rule of the class comment is one test. {@code events} records the ORDER of disk and settings writes (the original must be on disk before the first change).
 */
public class RadioIconHoldTest {
    private final List<String> events = new ArrayList<>();

    private final class FakeState implements RadioIconHold.State {
        boolean held; String original; boolean failNextSave;
        @Override public boolean held() { return held; }
        @Override public String original() { return original; }
        @Override public void save(boolean h, String o) {
            if (failNextSave) { failNextSave = false; throw new IllegalStateException("disk full"); }
            held = h; original = o; events.add("save(" + h + "," + o + ")");
        }
    }

    private final class FakeSetting implements RadioIconHold.Setting {
        String value; boolean failNextPut;
        FakeSetting(String v) { value = v; }
        @Override public String get() { return value; }
        @Override public void put(String v) {
            if (failNextPut) { failNextPut = false; throw new SecurityException("permission revoked"); }
            value = v; events.add("put(" + v + ")");
        }
    }

    private static final RadioIconHold.Sink QUIET = new RadioIconHold.Sink() { @Override public void log(String m) { } };

    private boolean set(FakeState st, FakeSetting s, boolean permitted, boolean want) { return RadioIconHold.set(st, s, permitted, want, QUIET); }

    // ---------------------------------------------------------------- the list helpers

    @Test public void aMissingSettingStartsFromTheSystemDefaultNotFromNothing() {
        assertEquals("rotate,headset,wifi,bluetooth", RadioIconHold.with(null));
        assertTrue(RadioIconHold.hides(null, "rotate"));
        assertTrue(RadioIconHold.hides(null, "headset"));
        assertFalse(RadioIconHold.hides(null, "wifi"));
        assertFalse(RadioIconHold.hides(null, "bluetooth"));
    }

    @Test public void anExistingListKeepsEveryEntryAndGetsBothOfOurs() {
        assertEquals("a,b,wifi,bluetooth", RadioIconHold.with("a,b"));
        assertEquals("wifi,bluetooth", RadioIconHold.with(""));
        assertEquals("a,wifi,bluetooth", RadioIconHold.with("a,,"));            // empty entries are no-ops for SystemUI and are dropped
        assertEquals("a,wifi,bluetooth", RadioIconHold.with("a,wifi,bluetooth")); // already there: not twice
    }

    @Test public void onlyTheMissingSlotIsAddedWhenTheOtherIsHiddenAlready() {
        assertEquals("bluetooth,x,wifi", RadioIconHold.with("bluetooth,x"));
        assertEquals("wifi,x,bluetooth", RadioIconHold.with("wifi,x"));
        assertEquals(Arrays.asList("wifi"), RadioIconHold.missing("bluetooth"));
        assertEquals(Arrays.asList("wifi", "bluetooth"), RadioIconHold.missing(null));
        assertTrue(RadioIconHold.missing("wifi,bluetooth").isEmpty());
    }

    @Test public void hidesAllNeedsBothSlots() {
        assertTrue(RadioIconHold.hidesAll("rotate,wifi,bluetooth"));
        assertFalse(RadioIconHold.hidesAll("wifi"));
        assertFalse(RadioIconHold.hidesAll("bluetooth"));
        assertFalse(RadioIconHold.hidesAll(null));
    }

    @Test public void slotNamesAreComparedExactlyLikeSystemUIDoes() {
        assertFalse(RadioIconHold.hides("wifi2,mywifi", "wifi"));
        assertFalse(RadioIconHold.hides("WIFI", "wifi"));
        assertFalse(RadioIconHold.hides(" wifi", "wifi"));   // no trimming in SystemUI's split
        assertFalse(RadioIconHold.hides("bluetooth_battery", "bluetooth"));
        assertTrue(RadioIconHold.hides("a,wifi,b", "wifi"));
    }

    @Test public void takingSlotsOutLeavesTheRestAndANullStaysNull() {
        assertEquals("a,b", RadioIconHold.without("a,wifi,b,bluetooth", Arrays.asList("wifi", "bluetooth")));
        assertEquals("a,bluetooth", RadioIconHold.without("a,wifi,wifi,bluetooth", Arrays.asList("wifi")));
        assertEquals("", RadioIconHold.without("wifi,bluetooth", Arrays.asList("wifi", "bluetooth")));
        assertNull(RadioIconHold.without(null, Arrays.asList("wifi", "bluetooth")));
    }

    // ---------------------------------------------------------------- the marker kept in Settings.Global

    @Test public void theMarkerRoundTripsEveryKindOfOriginal() {
        assertNull(RadioIconHold.decodeMarker(RadioIconHold.encodeMarker(null)));
        assertEquals("", RadioIconHold.decodeMarker(RadioIconHold.encodeMarker("")));
        assertEquals("rotate,headset", RadioIconHold.decodeMarker(RadioIconHold.encodeMarker("rotate,headset")));
        assertEquals("nfc,vpn", RadioIconHold.decodeMarker(RadioIconHold.encodeMarker("nfc,vpn")));
        assertTrue(RadioIconHold.encodeMarker(null).length() > 0);   // a marker is never empty: "present" is what means "held"
    }

    @Test public void anUnreadableMarkerCountsAsTheSystemDefault() {
        assertNull(RadioIconHold.decodeMarker(null));
        assertNull(RadioIconHold.decodeMarker(""));
        assertNull(RadioIconHold.decodeMarker("garbage"));
    }

    // ---------------------------------------------------------------- the radio rule

    @Test public void radiosStayOnOnlyWhileTheSymbolsCanReallyBeHidden() {
        assertTrue(RadioIconHold.keepsRadiosOn(true, true, false));
        assertFalse(RadioIconHold.keepsRadiosOn(false, true, false));   // switch off: today's behaviour
        assertFalse(RadioIconHold.keepsRadiosOn(true, false, false));   // no adb permission: the symbols would show on the lock screen, so the radios go off like before
        assertFalse(RadioIconHold.keepsRadiosOn(true, true, true));     // paused / breaker tripped
    }

    @Test public void theSwitchIsOnInTheShippedAppAndOffInDebug() {
        assertTrue(RadioIconHold.defaultOn(false));
        assertFalse(RadioIconHold.defaultOn(true));
    }

    // ---------------------------------------------------------------- taking the hold

    @Test public void theOriginalIsOnDiskBeforeTheFirstWrite() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc,vpn");
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,vpn,wifi,bluetooth", s.value);
        assertTrue(st.held);
        assertEquals("nfc,vpn", st.original);
        assertEquals("[save(true,nfc,vpn), put(nfc,vpn,wifi,bluetooth)]", events.toString());
    }

    @Test public void aMissingSettingIsARealOriginalAndTheDefaultListIsKept() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting(null);
        assertTrue(set(st, s, true, true));
        assertEquals("rotate,headset,wifi,bluetooth", s.value);
        assertTrue(st.held);
        assertNull(st.original);
        assertEquals("[save(true,null), put(rotate,headset,wifi,bluetooth)]", events.toString());
    }

    @Test public void aListThatAlreadyHidesBothSymbolsIsLeftAloneAndNothingIsHeld() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("rotate,bluetooth,wifi");
        assertFalse(set(st, s, true, true));
        assertFalse(st.held);
        assertEquals("rotate,bluetooth,wifi", s.value);
        assertTrue(events.isEmpty());
    }

    @Test public void aListThatHidesOnlyOneSymbolGetsTheOtherAndItsOriginalKeepsTheFirst() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("rotate,wifi");
        assertTrue(set(st, s, true, true));
        assertEquals("rotate,wifi,bluetooth", s.value);
        assertEquals("rotate,wifi", st.original);
        assertFalse(set(st, s, true, false));
        assertEquals("rotate,wifi", s.value);   // the wifi entry was Tj's own: it stays
        assertFalse(st.held);
    }

    @Test public void aHoldThatIsAlreadyOnRewritesNothing() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        events.clear();
        assertTrue(set(st, s, true, true));
        assertTrue(events.isEmpty());
        assertEquals("nfc,wifi,bluetooth", s.value);
    }

    @Test public void aHoldThatLostOneSlotAddsItBackAndKeepsItsOriginal() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        s.value = "nfc,wifi";   // something removed the bluetooth entry
        events.clear();
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,wifi,bluetooth", s.value);
        assertEquals("nfc", st.original);   // NOT the list with our wifi entry in it: that would leave wifi hidden for good
        assertEquals("[put(nfc,wifi,bluetooth)]", events.toString());
        assertFalse(set(st, s, true, false));
        assertEquals("nfc", s.value);
    }

    @Test public void aHoldWhoseValueWasResetIsTakenAgainAndStillEndsCleanly() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        s.value = "nfc,vpn";   // something rewrote the list without our entries
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,vpn,wifi,bluetooth", s.value);
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,vpn", s.value);   // his newer list, minus only what we added
        assertFalse(st.held);
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

    @Test public void aListEditedMeanwhileLosesOnlyOurEntries() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.value = "nfc,wifi,vpn,bluetooth";   // Tj (or a tool) added vpn while we held
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,vpn", s.value);
        assertFalse(st.held);
    }

    @Test public void aListThatNoLongerHidesAnySymbolIsNotTouchedWhenTheHoldEnds() {
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
        assertEquals("nfc,wifi,bluetooth", s.value);
        assertTrue(events.isEmpty());
    }

    @Test public void aFailedWriteWhileEndingKeepsTheHoldSoTheNextTriggerTriesAgain() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.failNextPut = true;
        assertTrue(set(st, s, true, false));
        assertTrue(st.held);
        assertEquals("nfc,wifi,bluetooth", s.value);
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
            assertEquals("rotate,headset,wifi,bluetooth", s.value);
            assertFalse(set(st, s, true, false));
            assertNull(s.value);
        }
    }
}
