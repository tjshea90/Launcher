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

    // ---- BN8d: the diagnosis of a clean-up that found nothing
    @Test public void theStockLaunchersWindowsAreRecognised() {
        assertEquals(true, RecentsDirty.isStockLauncherWindow("com.android.launcher3", null));
        assertEquals(true, RecentsDirty.isStockLauncherWindow("com.android.launcher3.go", "com.android.launcher3"));
        assertEquals(true, RecentsDirty.isStockLauncherWindow("com.vendor.home", "com.vendor.home"));   // a vendor's own launcher: the resolved stock package counts
        assertEquals(false, RecentsDirty.isStockLauncherWindow("com.android.systemui", "com.android.launcher3"));
        assertEquals(false, RecentsDirty.isStockLauncherWindow("com.tj.launcher", "com.android.launcher3"));
        assertEquals(false, RecentsDirty.isStockLauncherWindow("-", "-"));                              // a window without a root never counts
        assertEquals(false, RecentsDirty.isStockLauncherWindow(null, null));
    }

    @Test public void theDiagnosisPutsTheRecentsScreenFirstAndFitsTheReport() {
        String stock = " [type 1 layer 2 active com.android.launcher3 nodes{FrameLayoutx3,RecyclerViewx1,Buttonx1} texts{Clear all* | Maps}]";
        StringBuilder others = new StringBuilder();
        for (int i = 0; i < 40; i++) others.append(" [type 3 layer ").append(i).append(" com.android.systemui]");
        String d = RecentsDirty.diagLine("recents action accepted; 41 window(s)", stock, others.toString(), 590);
        assertEquals(590, d.length());                                             // cut to the report's limit ...
        assertEquals(true, d.contains("stock launcher:" + stock + "; others:"));   // ... and the Recents screen is whole: only the list of the other windows was cut
        assertEquals(true, d.startsWith("recents action accepted; 41 window(s); stock launcher: [type 1"));
    }

    @Test public void theDiagnosisSaysSoWhenTheStockLauncherHasNoWindow() {
        assertEquals("recents action REFUSED; 2 window(s); stock launcher: NO WINDOW; others: [type 3 layer 1 com.android.systemui]",
                RecentsDirty.diagLine("recents action REFUSED; 2 window(s)", "", " [type 3 layer 1 com.android.systemui]", 590));
        assertEquals("x; stock launcher: NO WINDOW; others: none", RecentsDirty.diagLine("x", null, null, 590));
    }
}
