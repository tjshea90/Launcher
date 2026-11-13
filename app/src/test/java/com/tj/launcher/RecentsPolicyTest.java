package com.tj.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RecentsPolicyTest {
    @Test public void otherAppsAreExcludedFromRecents() { assertTrue(RecentsPolicy.shouldExclude(true, "com.brave.browser", "com.solitaire", "com.tj.launcher")); }
    @Test public void theTargetAppStaysInRecents() { assertFalse(RecentsPolicy.shouldExclude(true, "com.solitaire", "com.solitaire", "com.tj.launcher")); }
    @Test public void switchedOffMeansStock() { assertFalse(RecentsPolicy.shouldExclude(false, "com.brave.browser", "com.solitaire", "com.tj.launcher")); }
    @Test public void noTargetInstalledStillExcludesOthers() { assertTrue(RecentsPolicy.shouldExclude(true, "com.brave.browser", null, "com.tj.launcher")); }
    @Test public void unknownPackageIsLeftAlone() { assertFalse(RecentsPolicy.shouldExclude(true, null, "x", "com.tj.launcher")); }

    /** BY1a A4: the target lookup is skipped only where it cannot change the answer. */
    @Test public void theTargetIsLookedUpOnlyWhenItCanMatter() {
        assertTrue(RecentsPolicy.needsTarget(true, "com.brave.browser", "com.tj.launcher"));
        assertFalse(RecentsPolicy.needsTarget(false, "com.brave.browser", "com.tj.launcher"));   // the default: switch off
        assertFalse(RecentsPolicy.needsTarget(true, null, "com.tj.launcher"));
        assertFalse(RecentsPolicy.needsTarget(true, "", "com.tj.launcher"));
        assertFalse(RecentsPolicy.needsTarget(true, "com.tj.launcher", "com.tj.launcher"));
        String[] pkgs = { null, "", "com.tj.launcher", "com.brave.browser", "com.solitaire" };
        String[] targets = { null, "", "com.solitaire", "com.brave.browser", "com.tj.launcher" };
        for (boolean on : new boolean[] { true, false })
            for (String p : pkgs)
                for (String t : targets)
                    if (!RecentsPolicy.needsTarget(on, p, "com.tj.launcher"))
                        assertFalse(on + " " + p + " " + t, RecentsPolicy.shouldExclude(on, p, t, "com.tj.launcher"));
    }
}
