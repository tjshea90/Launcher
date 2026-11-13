package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import org.junit.Test;

/** AB2: which accessibility events and windows the helper may skip on the slow tablet without ever missing the power menu, the shade or the PIN pad. */
public class EventNoiseTest {
    @Test public void theConstantsAreTheOnesAndroidUses() {
        // compile-time constants of android.jar: if Android ever changed them this fails instead of silently skipping the wrong events
        assertEquals(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, EventNoise.TYPE_WINDOW_CONTENT_CHANGED);
        assertEquals(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, EventNoise.TYPE_WINDOW_STATE_CHANGED);
        assertEquals(AccessibilityEvent.TYPE_WINDOWS_CHANGED, EventNoise.TYPE_WINDOWS_CHANGED);
        assertEquals(AccessibilityWindowInfo.TYPE_APPLICATION, EventNoise.WINDOW_TYPE_APPLICATION);
        assertEquals(AccessibilityWindowInfo.TYPE_INPUT_METHOD, EventNoise.WINDOW_TYPE_INPUT_METHOD);
    }

    @Test public void anotherAppsContentChangeIsNoise() {
        assertTrue(EventNoise.isNoise(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "com.example.game"));
        assertTrue(EventNoise.isNoise(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "com.google.android.apps.photosgo"));
        assertTrue(EventNoise.isNoise(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, null));
    }

    @Test public void systemUiContentChangesAreNeverNoise() {
        // the shade, the volume panel, the PIN pad and the power menu all live in SystemUI
        assertFalse(EventNoise.isNoise(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "com.android.systemui"));
        assertFalse(EventNoise.isNoise(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "android"));      // system_server's own windows (some ROMs host dialogs there): quiet, so kept
    }

    @Test public void everyOtherEventTypeIsNeverNoiseWhoeverSendsIt() {
        int[] types = { AccessibilityEvent.TYPE_VIEW_CLICKED, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED, AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED, AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED, AccessibilityEvent.TYPE_VIEW_FOCUSED };
        for (int t : types) {
            assertFalse("type " + t, EventNoise.isNoise(t, "com.example.game"));
            assertFalse("type " + t, EventNoise.isNoise(t, "com.android.systemui"));
            assertFalse("type " + t, EventNoise.isNoise(t, null));
        }
    }

    @Test public void aNewWindowIsRecognisedFromTheEventType() {
        assertTrue(EventNoise.windowsJustChanged(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED));
        assertTrue(EventNoise.windowsJustChanged(AccessibilityEvent.TYPE_WINDOWS_CHANGED));
        assertFalse(EventNoise.windowsJustChanged(AccessibilityEvent.TYPE_VIEW_CLICKED));
        assertFalse(EventNoise.windowsJustChanged(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED));
    }

    @Test public void onlyAppAndKeyboardWindowsAreNotSystemWindows() {
        assertFalse(EventNoise.isSystemWindow(AccessibilityWindowInfo.TYPE_APPLICATION));
        assertFalse(EventNoise.isSystemWindow(AccessibilityWindowInfo.TYPE_INPUT_METHOD));
        assertTrue(EventNoise.isSystemWindow(AccessibilityWindowInfo.TYPE_SYSTEM));
        assertTrue(EventNoise.isSystemWindow(AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY));
        assertTrue(EventNoise.isSystemWindow(AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER));
    }

    @Test public void withTheActionsDialogUpOnlyThatWindowIsSearched() {
        assertTrue(EventNoise.mayHoldPowerMenu(true, true, AccessibilityWindowInfo.TYPE_SYSTEM));
        assertFalse(EventNoise.mayHoldPowerMenu(true, false, AccessibilityWindowInfo.TYPE_SYSTEM));
        assertFalse(EventNoise.mayHoldPowerMenu(true, false, AccessibilityWindowInfo.TYPE_APPLICATION));
    }

    @Test public void onlySystemWindowsAreSearched() {   // BY1a B1
        assertFalse(EventNoise.mayHoldPowerMenu(false, false, AccessibilityWindowInfo.TYPE_APPLICATION));
        assertFalse(EventNoise.mayHoldPowerMenu(false, false, AccessibilityWindowInfo.TYPE_INPUT_METHOD));
        assertTrue(EventNoise.mayHoldPowerMenu(false, false, AccessibilityWindowInfo.TYPE_SYSTEM));
        assertTrue(EventNoise.mayHoldPowerMenu(false, false, AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER));
    }

    @Test public void ourOwnOverlaysAreNeverSearched() {   // BY1a B1: type 4 is always one of our windows
        assertFalse(EventNoise.mayHoldPowerMenu(false, false, AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY));
        assertFalse(EventNoise.mayHoldPowerMenu(true, true, AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY));
    }

    @Test public void aClickStartsTheScanOnlyFromTheSystemOrWithTheMenuUp() {   // BY1a B1
        assertFalse(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false, false));   // a click inside an app
        assertFalse(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, true, false));    // ... also right after a window change
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false, true));     // a click in SystemUI
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, true, false, false));     // the menu is up: follow it
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, false, false, false));   // window changes still always scan
        assertTrue(EventNoise.systemPackage("com.android.systemui"));
        assertTrue(EventNoise.systemPackage("android"));
        assertFalse(EventNoise.systemPackage("com.android.chrome"));
        assertFalse(EventNoise.systemPackage(null));
        assertEquals(AccessibilityEvent.TYPE_VIEW_CLICKED, EventNoise.TYPE_VIEW_CLICKED);
    }

    @Test public void aPinPadKeyStartsNoPowerMenuScan() {   // CA1 U1
        assertFalse(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false, true, true));   // a PIN key (a SystemUI click) while the pad is up
        assertFalse(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, true, true, true));    // ... also inside the full-scan window
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, true, false, true, true));     // the menu is up over the pad: a click in it is followed
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false, true, false));   // no pad: a SystemUI click scans as before
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, false, false, true, true));   // the menu opening over the pad: its window event scans
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOWS_CHANGED, false, false, false, true));
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, false, true, true, true));   // late content right after a window change: as before
        assertFalse(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, false, false, true, true));
        assertEquals(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false, true),
                EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false, true, false));   // the 4-argument form = no pad
    }

    @Test public void aPinPadKeyStartsNoFullPencilSearch() {   // CA1 U1
        assertFalse(EventNoise.pencilSearchOnEvent(AccessibilityEvent.TYPE_VIEW_CLICKED, true, false));
        assertTrue(EventNoise.pencilSearchOnEvent(AccessibilityEvent.TYPE_VIEW_CLICKED, true, true));     // a cover is up: follow it
        assertTrue(EventNoise.pencilSearchOnEvent(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false));   // no pad: as before
        assertTrue(EventNoise.pencilSearchOnEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, true, false));   // the panel coming down is a content burst / window change: still searched
        assertTrue(EventNoise.pencilSearchOnEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, true, false));
        assertTrue(EventNoise.pencilSearchOnEvent(0, true, false));   // an event-less follow-up
    }

    @Test public void onlyTallWindowsCanHoldTheKeyguardOrThePanel() {   // BY1a B2 / B3
        assertTrue(EventNoise.tallEnoughForPanel(1024, 1024));   // the keyguard / expanded panel: full height
        assertTrue(EventNoise.tallEnoughForPanel(342, 1024));
        assertFalse(EventNoise.tallEnoughForPanel(24, 1024));    // the collapsed status bar
        assertFalse(EventNoise.tallEnoughForPanel(48, 1024));    // the navigation bar
        assertTrue(EventNoise.tallEnoughForPanel(10, 0));        // unknown display: search as before
    }

    // ---- AN1: the shade must not be searched on every redraw
    @Test public void aContentChangeOfTheShadeDoesNotStartAScanUnlessAMenuIsUp() {
        assertFalse(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, false, false));   // a tile redraws, the clock ticks: nothing to look for
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, true, false));     // the menu is up: follow its changes
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, false, true));     // the 300 ms right after a window change: late content of a new window
    }
    @Test public void windowChangesAndClicksAlwaysStartTheScan() {
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, false, false));
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_WINDOWS_CHANGED, false, false));
        assertTrue(EventNoise.needsPowerMenuScan(AccessibilityEvent.TYPE_VIEW_CLICKED, false, false));
        assertTrue(EventNoise.needsPowerMenuScan(0, false, false));   // an event of no known type (null event): scan as before
    }
    @Test public void pencilSearchesInABurstOfContentChangesAreSpaced() {
        assertEquals(200L, EventNoise.minPencilSearchGapMs(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED));
        assertEquals(40L, EventNoise.minPencilSearchGapMs(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED));
        assertEquals(40L, EventNoise.minPencilSearchGapMs(AccessibilityEvent.TYPE_VIEW_CLICKED));
    }
}
