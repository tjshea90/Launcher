package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.view.accessibility.AccessibilityEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** AP1: the pure rules of the Recents filter (only apps of the custom drawer may stay in the custom launcher's Recents). */
public class RecentsFilterTest {
    private static Set<String> set(String... s) { return new HashSet<>(Arrays.asList(s)); }

    @Test public void theGuardWorksOnlyInTheCustomLauncherUnlockedWithItsSwitchOn() {
        assertTrue(RecentsFilter.wanted(true, true, false, false, false));
        assertFalse(RecentsFilter.wanted(false, true, false, false, false));   // Quickstep keeps its own Recents
        assertFalse(RecentsFilter.wanted(true, false, false, false, false));   // switched off
        assertFalse(RecentsFilter.wanted(true, true, true, false, false));     // lock screen
        assertFalse(RecentsFilter.wanted(true, true, false, true, false));     // crash breaker tripped
        assertFalse(RecentsFilter.wanted(true, true, false, false, true));     // the return reset is driving Recents itself
    }
    @Test public void hiddenAndQuickstepOnlyAppsAreBlockedButNeverADrawerLabel() {
        Set<String> b = RecentsFilter.blockedLabels(set("Calendar", "Portfolio"), set("Chess", "Contacts"), set("Contacts", "Clock"));
        assertEquals(set("calendar", "portfolio", "chess"), b);   // Contacts was opened in Quickstep but is also a drawer app: it may stay
    }
    @Test public void aLabelThatAHiddenAndAShownAppShareIsNotBlocked() {
        assertTrue(RecentsFilter.blockedLabels(set("Gallery"), set(), set("gallery")).isEmpty());
    }
    @Test public void emptyAndNullInputsGiveNothingToClose() {
        assertTrue(RecentsFilter.blockedLabels(null, null, null).isEmpty());
        assertTrue(RecentsFilter.blockedLabels(set("", "  "), set((String) null), set()).isEmpty());
    }
    @Test public void aCardIsMatchedByTheLabelItStartsWith() {
        Set<String> b = set("calendar", "portfolio");
        assertEquals("calendar", RecentsFilter.matchingLabel("Calendar", b));
        assertEquals("calendar", RecentsFilter.matchingLabel("  CALENDAR  ", b));
        assertEquals("portfolio", RecentsFilter.matchingLabel("Portfolio, My holdings", b));
        assertEquals("portfolio", RecentsFilter.matchingLabel("Portfolio Tracker", b));
    }
    @Test public void aCardThatMatchesNothingKnownIsLeftAlone() {
        Set<String> b = set("calendar");
        assertNull(RecentsFilter.matchingLabel("Wikipedia", b));            // a browser card is titled with the page
        assertNull(RecentsFilter.matchingLabel("Calendars of the world", b)); // not the label followed by a separator
        assertNull(RecentsFilter.matchingLabel("", b));
        assertNull(RecentsFilter.matchingLabel(null, b));
        assertNull(RecentsFilter.matchingLabel("Calendar", null));
        assertNull(RecentsFilter.matchingLabel("Calendar", set()));
    }
    @Test public void theLongestMatchingLabelWins() {
        assertEquals("my files", RecentsFilter.matchingLabel("My Files, Downloads", set("my", "my files")));
    }
    @Test public void onlyWindowChangesOfTheStockLauncherOrSystemUiCanOpenRecents() {
        assertTrue(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "com.android.launcher3", "com.android.launcher3", "com.android.quickstep.RecentsActivity"));
        assertTrue(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOWS_CHANGED, "com.vendor.home", "com.vendor.home", null));
        assertTrue(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "com.android.systemui", null, "com.android.systemui.recents.RecentsActivity"));   // an old-style SystemUI Recents
        assertFalse(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "com.android.systemui", null, "android.widget.FrameLayout"));   // the shade / quick settings: never scanned (AN1)
        assertFalse(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "com.android.systemui", null, null));
        assertFalse(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, "com.android.launcher3", "com.android.launcher3", "x"));   // a redraw is no new screen
        assertFalse(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_VIEW_CLICKED, "com.android.systemui", null, "recents"));
        assertFalse(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "com.android.calendar", "com.android.launcher3", "Recents"));    // an app's own window
        assertFalse(RecentsFilter.mayOpenRecents(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, null, "com.android.launcher3", null));
    }

    /** BY1a C1: a look uses the label set only while it is fresh (the old 60 s rule), else it waits for the background build. */
    @Test public void theLabelSetIsUsedOnlyWhileFresh() {
        long t = 1_000_000;
        assertFalse("never built", RecentsFilter.labelsFresh(false, 0, t));
        assertTrue(RecentsFilter.labelsFresh(true, t, t));
        assertTrue("rebuilt only when OLDER than the TTL, as before", RecentsFilter.labelsFresh(true, t - RecentsFilter.LABELS_TTL_MS, t));
        assertFalse(RecentsFilter.labelsFresh(true, t - RecentsFilter.LABELS_TTL_MS - 1, t));
    }

    @Test public void aBuildStartsOnlyForASetThatIsNotFreshAndNeverTwiceAtATime() {
        assertTrue(RecentsFilter.buildNeeded(false, false));
        assertFalse("one build at a time", RecentsFilter.buildNeeded(false, true));
        assertFalse(RecentsFilter.buildNeeded(true, false));
        assertFalse(RecentsFilter.buildNeeded(true, true));
    }

    @Test public void theLookThatWaitedRunsOnlyWithASet() {
        assertTrue(RecentsFilter.lookAfterBuild(true, true));
        assertFalse("the build failed: that look ends, as a failed scan ended it before", RecentsFilter.lookAfterBuild(true, false));
        assertFalse("nobody waited (a build started at the event and ended before the first look)", RecentsFilter.lookAfterBuild(false, true));
        assertFalse(RecentsFilter.lookAfterBuild(false, false));
    }
}
