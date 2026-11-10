package com.tj.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RecentsDirtyTest {
    private static boolean m(String pkg, boolean other) { return RecentsDirty.shouldMark(pkg, "com.tj.launcher", "com.android.launcher3", "com.swiftkey", "com.solitaire", other); }

    @Test public void anotherAppMarksRecentsDirty() { assertTrue(m("com.brave.browser", true)); }
    @Test public void solitaireIsExpectedInRecents() { assertFalse(m("com.solitaire", true)); }
    @Test public void systemAndLauncherWindowsDoNotMark() {
        assertFalse(m("com.android.systemui", true)); assertFalse(m("com.android.launcher3", true)); assertFalse(m("com.tj.launcher", true));
        assertFalse(m("com.swiftkey", true)); assertFalse(m("android", true));
    }
    @Test public void appsLaunchedFromTheCustomLauncherAreExcludedSoTheyDoNotMark() { assertFalse(m("com.brave.browser", false)); }
    @Test public void emptyPackageDoesNotMark() { assertFalse(m(null, true)); assertFalse(m("", true)); }

    /** BN8e: at most one DIRTY line per package per minute in the lock log. */
    @Test public void aDirtyLineIsLoggedOncePerPackagePerGap() {
        java.util.HashMap<String, Long> last = new java.util.HashMap<String, Long>();
        long gap = RecentsDirty.LOG_GAP_MS;
        assertTrue(RecentsDirty.shouldLog(last, "com.a", 1_000, gap));
        assertFalse(RecentsDirty.shouldLog(last, "com.a", 1_001, gap));           // the same package a moment later
        assertTrue(RecentsDirty.shouldLog(last, "com.b", 1_002, gap));            // another package is its own
        assertFalse(RecentsDirty.shouldLog(last, "com.a", 1_000 + gap - 1, gap));
        assertTrue(RecentsDirty.shouldLog(last, "com.a", 1_000 + gap, gap));      // a full gap later: again
        assertTrue(RecentsDirty.shouldLog(last, "com.a", 5, gap));                // a clock that went backwards logs again and restarts the gap
        assertFalse(RecentsDirty.shouldLog(last, "com.a", 6, gap));
    }

    @Test public void theLogGateStaysSmall() {
        java.util.HashMap<String, Long> last = new java.util.HashMap<String, Long>();
        for (int i = 0; i < 200; i++) assertTrue(RecentsDirty.shouldLog(last, "com.p" + i, 1_000 + i, RecentsDirty.LOG_GAP_MS));
        assertTrue(last.size() <= 32);
    }

}
