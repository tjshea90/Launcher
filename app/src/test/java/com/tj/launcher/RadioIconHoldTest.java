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
        assertEquals("rotate,headset,wifi,bluetooth,vpn", RadioIconHold.with(null));
        assertTrue(RadioIconHold.hides(null, "rotate"));
        assertTrue(RadioIconHold.hides(null, "headset"));
        assertFalse(RadioIconHold.hides(null, "wifi"));
        assertFalse(RadioIconHold.hides(null, "bluetooth"));
        assertFalse(RadioIconHold.hides(null, "vpn"));
    }

    @Test public void anExistingListKeepsEveryEntryAndGetsAllOfOurs() {
        assertEquals("a,b,wifi,bluetooth,vpn", RadioIconHold.with("a,b"));
        assertEquals("wifi,bluetooth,vpn", RadioIconHold.with(""));
        assertEquals("a,wifi,bluetooth,vpn", RadioIconHold.with("a,,"));                // empty entries are no-ops for SystemUI and are dropped
        assertEquals("a,wifi,bluetooth,vpn", RadioIconHold.with("a,wifi,bluetooth,vpn")); // already there: not twice
        assertEquals("a,wifi,bluetooth,vpn", RadioIconHold.with("a,wifi,bluetooth"));     // two already there: not twice, only the third is added
    }

    @Test public void onlyTheMissingSlotsAreAddedWhenOthersAreHiddenAlready() {
        assertEquals("bluetooth,x,wifi,vpn", RadioIconHold.with("bluetooth,x"));
        assertEquals("wifi,x,bluetooth,vpn", RadioIconHold.with("wifi,x"));
        assertEquals("vpn,x,wifi,bluetooth", RadioIconHold.with("vpn,x"));               // a VPN key Tj hides himself is not added twice
        assertEquals("vpn,bluetooth,x,wifi", RadioIconHold.with("vpn,bluetooth,x"));     // the others hidden already: only the one missing slot is added
        assertEquals(Arrays.asList("wifi", "vpn"), RadioIconHold.missing("bluetooth"));
        assertEquals(Arrays.asList("wifi", "bluetooth", "vpn"), RadioIconHold.missing(null));
        assertTrue(RadioIconHold.missing("wifi,bluetooth,vpn").isEmpty());
    }

    @Test public void hidesAllNeedsEverySlot() {
        assertTrue(RadioIconHold.hidesAll("rotate,wifi,bluetooth,vpn"));
        assertFalse(RadioIconHold.hidesAll("rotate,wifi,bluetooth"));   // the VPN key would still show
        assertFalse(RadioIconHold.hidesAll("wifi"));
        assertFalse(RadioIconHold.hidesAll("bluetooth"));
        assertFalse(RadioIconHold.hidesAll("vpn"));
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
        assertEquals("nfc,cast", RadioIconHold.decodeMarker(RadioIconHold.encodeMarker("nfc,cast")));
        assertTrue(RadioIconHold.encodeMarker(null).length() > 0);   // a marker is never empty: "present" is what means "held"
    }

    @Test public void anUnreadableMarkerCountsAsTheSystemDefault() {
        assertNull(RadioIconHold.decodeMarker(null));
        assertNull(RadioIconHold.decodeMarker(""));
        assertNull(RadioIconHold.decodeMarker("garbage"));
    }

    // ---------------------------------------------------------------- the radio rule

    @Test public void radiosStayOnOnlyWhileTheSymbolsCanReallyBeHidden() {
        // arguments: switchOn, grant, automationOff, quickstepMode, helperUp
        assertTrue(RadioIconHold.keepsRadiosOn(true, true, false, false, false));    // the custom launcher needs no helper
        assertTrue(RadioIconHold.keepsRadiosOn(true, true, false, true, true));      // Quickstep with the helper: the lock screen is next
        assertFalse(RadioIconHold.keepsRadiosOn(true, true, false, true, false));    // Quickstep without the helper (a lock screen right after a boot): the symbols cannot be hidden yet, so the radios go off like before
        assertFalse(RadioIconHold.keepsRadiosOn(false, true, false, false, true));   // switch off: today's behaviour
        assertFalse(RadioIconHold.keepsRadiosOn(true, false, false, false, true));   // no adb permission: the symbols would show on the lock screen, so the radios go off like before
        assertFalse(RadioIconHold.keepsRadiosOn(true, true, true, false, true));     // paused / breaker tripped
    }

    /** The radios are kept exactly when the policy wants the hold on a LOCK screen with the screen on, for every combination of the five facts. A dark screen and a lock screen are the same moment for both rules; this fails when the policy ever treats them differently (review of BS1, finding 1). */
    @Test public void theRadioRuleIsTheHoldRuleOnALockScreen() {
        for (int m = 0; m < 32; m++) {
            boolean sw = (m & 1) != 0, grant = (m & 2) != 0, off = (m & 4) != 0, qs = (m & 8) != 0, helper = (m & 16) != 0;
            NotifQuietPolicy.Facts f = new NotifQuietPolicy.Facts();
            f.switchOn = sw; f.grant = grant; f.automationOff = off; f.quickstepMode = qs; f.helperUp = helper; f.screenOn = true; f.lockUp = true;
            assertEquals("combination " + m, NotifQuietPolicy.wanted(f), RadioIconHold.keepsRadiosOn(sw, grant, off, qs, helper));
        }
    }

    /** Review of BS2, finding 2: the helper goes away while the lock screen is up -> the hold ends (symbols would show) -> the radios must go off. Nothing else ends a hold with a radio switch-off. */
    @Test public void theRadiosGoOffOnlyWhenTheHelperIsGoneOnADarkOrLockedScreen() {
        assertTrue(RadioIconHold.radiosGoOffWhenHoldEnds(NotifQuietPolicy.WHY_NO_HELPER, true));
        assertFalse("an unlocked Quickstep session without the helper: the symbols are meant to show, nothing to hide", RadioIconHold.radiosGoOffWhenHoldEnds(NotifQuietPolicy.WHY_NO_HELPER, false));
        assertFalse("the normal end of the hold", RadioIconHold.radiosGoOffWhenHoldEnds("Quickstep session", true));
        assertFalse("paused on purpose: the launcher does nothing then", RadioIconHold.radiosGoOffWhenHoldEnds("automation paused", true));
        assertFalse("without the permission nothing can be switched or hidden by us", RadioIconHold.radiosGoOffWhenHoldEnds("no adb permission", true));
        assertFalse("the switch is handled by the Control handler", RadioIconHold.radiosGoOffWhenHoldEnds("switch off", true));
        assertFalse(RadioIconHold.radiosGoOffWhenHoldEnds("", true));
        assertFalse(RadioIconHold.radiosGoOffWhenHoldEnds(null, true));
    }

    /**
     * Third review of BS2, finding 1: the symbols must not come back BEFORE the radios are off. The release of the list is deferred exactly when the radios have to go off, and never twice in a row (the worker's own
     * final sync, and any sync inside the window, release at once: a deferral can not loop).
     */
    @Test public void theReleaseIsDeferredWhenTheRadiosMustGoOffAndHeldBackWhileTheDeferralRuns() {
        final String gone = NotifQuietPolicy.WHY_NO_HELPER;
        final int idle = RadioIconHold.DEFER_IDLE, running = RadioIconHold.DEFER_RUNNING, finishing = RadioIconHold.DEFER_FINISHING;
        assertEquals("the helper left on a dark or locked screen, nothing running: start the deferral", RadioIconHold.START_DEFERRAL, RadioIconHold.releaseDecision(gone, true, idle, 1_000, 0));
        assertEquals("run #522: a second trigger 6 ms later must NOT release", RadioIconHold.HOLD_BACK, RadioIconHold.releaseDecision(gone, true, running, 1_006, 10_000));
        assertEquals("... whatever the screen looks like a moment later: the worker releases within seconds", RadioIconHold.HOLD_BACK, RadioIconHold.releaseDecision(gone, false, running, 1_006, 10_000));
        assertEquals("the worker's own final sync releases (no loop)", RadioIconHold.RELEASE, RadioIconHold.releaseDecision(gone, true, finishing, 5_000, 10_000));
        assertEquals("a deferral that never finished blocks nothing for ever", RadioIconHold.START_DEFERRAL, RadioIconHold.releaseDecision(gone, true, running, 10_000, 10_000));
        assertEquals("a lit, unlocked screen: the symbols are meant to show", RadioIconHold.RELEASE, RadioIconHold.releaseDecision(gone, false, idle, 1_000, 0));
        for (int st : new int[] { idle, running, finishing }) {
            assertEquals("every other reason ends the hold at once", RadioIconHold.RELEASE, RadioIconHold.releaseDecision("Quickstep session", true, st, 1_000, 10_000));
            assertEquals(RadioIconHold.RELEASE, RadioIconHold.releaseDecision("switch off", true, st, 1_000, 10_000));
            assertEquals(RadioIconHold.RELEASE, RadioIconHold.releaseDecision("", true, st, 1_000, 10_000));
            assertEquals(RadioIconHold.RELEASE, RadioIconHold.releaseDecision(null, true, st, 1_000, 10_000));
        }
    }

    /** The words come from the policy itself: a changed text cannot silently disable the rule above. */
    @Test public void theHelperReasonIsThePolicysOwnText() {
        NotifQuietPolicy.Facts f = new NotifQuietPolicy.Facts();
        f.switchOn = true; f.grant = true; f.quickstepMode = true; f.helperUp = false;
        assertEquals(NotifQuietPolicy.WHY_NO_HELPER, NotifQuietPolicy.why(f));
    }

    @Test public void theSwitchIsOnInTheShippedAppAndOffInDebug() {
        assertTrue(RadioIconHold.defaultOn(false));
        assertFalse(RadioIconHold.defaultOn(true));
    }

    // ---------------------------------------------------------------- taking the hold

    @Test public void theOriginalIsOnDiskBeforeTheFirstWrite() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc,cast");
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,cast,wifi,bluetooth,vpn", s.value);
        assertTrue(st.held);
        assertEquals("nfc,cast", st.original);
        assertEquals("[save(true,nfc,cast), put(nfc,cast,wifi,bluetooth,vpn)]", events.toString());
    }

    @Test public void aMissingSettingIsARealOriginalAndTheDefaultListIsKept() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting(null);
        assertTrue(set(st, s, true, true));
        assertEquals("rotate,headset,wifi,bluetooth,vpn", s.value);
        assertTrue(st.held);
        assertNull(st.original);
        assertEquals("[save(true,null), put(rotate,headset,wifi,bluetooth,vpn)]", events.toString());
    }

    @Test public void aListThatAlreadyHidesEverySymbolIsLeftAloneAndNothingIsHeld() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("rotate,bluetooth,vpn,wifi");
        assertFalse(set(st, s, true, true));
        assertFalse(st.held);
        assertEquals("rotate,bluetooth,vpn,wifi", s.value);
        assertTrue(events.isEmpty());
    }

    @Test public void aListThatHidesOnlyOneSymbolGetsTheOthersAndItsOriginalKeepsTheFirst() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("rotate,wifi");
        assertTrue(set(st, s, true, true));
        assertEquals("rotate,wifi,bluetooth,vpn", s.value);
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
        assertEquals("nfc,wifi,bluetooth,vpn", s.value);
    }

    @Test public void aHoldThatLostOneSlotAddsItBackAndKeepsItsOriginal() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        s.value = "nfc,wifi,vpn";   // something removed the bluetooth entry
        events.clear();
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,wifi,vpn,bluetooth", s.value);
        assertEquals("nfc", st.original);   // NOT the list with our wifi entry in it: that would leave wifi hidden for good
        assertEquals("[put(nfc,wifi,vpn,bluetooth)]", events.toString());
        assertFalse(set(st, s, true, false));
        assertEquals("nfc", s.value);
    }

    @Test public void aHoldWhoseValueWasResetIsTakenAgainAndStillEndsCleanly() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        assertTrue(set(st, s, true, true));
        s.value = "nfc,cast";   // something rewrote the list without our entries
        assertTrue(set(st, s, true, true));
        assertEquals("nfc,cast,wifi,bluetooth,vpn", s.value);
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,cast", s.value);   // his newer list, minus only what we added
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
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc,cast");
        set(st, s, true, true);
        assertEquals("nfc,cast,wifi,bluetooth,vpn", s.value);   // premise: the hold really changed the list
        events.clear();
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,cast", s.value);
        assertFalse(st.held);
        assertEquals("[put(nfc,cast), save(false,null)]", events.toString());   // the hold is cleared only AFTER the write
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
        s.value = "nfc,wifi,cast,bluetooth,vpn";   // Tj (or a tool) added cast while we held
        assertFalse(set(st, s, true, false));
        assertEquals("nfc,cast", s.value);
        assertFalse(st.held);
    }

    @Test public void aListThatNoLongerHidesAnySymbolIsNotTouchedWhenTheHoldEnds() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.value = "cast";   // somebody replaced the list: nothing of ours is left in it
        events.clear();
        assertFalse(set(st, s, true, false));
        assertEquals("cast", s.value);
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
        assertEquals("nfc,wifi,bluetooth,vpn", s.value);
        assertTrue(events.isEmpty());
    }

    @Test public void aFailedWriteWhileEndingKeepsTheHoldSoTheNextTriggerTriesAgain() {
        FakeState st = new FakeState(); FakeSetting s = new FakeSetting("nfc");
        set(st, s, true, true);
        s.failNextPut = true;
        assertTrue(set(st, s, true, false));
        assertTrue(st.held);
        assertEquals("nfc,wifi,bluetooth,vpn", s.value);
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
