package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class AppVisibilityTest {
    private static Set<String> set(String... s) { return new HashSet<>(Arrays.asList(s)); }
    private static final String SELF = "com.tj.launcher";

    @Test public void baselineKeepsEverythingInstalledButNotUs() {
        Set<String> b = AppVisibility.baseline(set("a", "b", SELF), SELF);
        assertEquals(set("a", "b"), b);
    }
    @Test public void appsInstalledAfterTheBaselineAreHidden() {
        Set<String> allowed = AppVisibility.baseline(set("a", "b"), SELF);
        assertTrue(AppVisibility.isShown(allowed, "a", "A", SELF));
        assertFalse(AppVisibility.isShown(allowed, "newapp", "N", SELF));
    }
    @Test public void tjCanAllowOneAndHideAnother() {
        Set<String> allowed = AppVisibility.baseline(set("a", "b"), SELF);
        allowed = AppVisibility.withShown(allowed, "newapp", true);
        allowed = AppVisibility.withShown(allowed, "b", false);
        assertTrue(AppVisibility.isShown(allowed, "newapp", "N", SELF));
        assertFalse(AppVisibility.isShown(allowed, "b", "B", SELF));
        assertTrue(AppVisibility.isShown(allowed, "a", "A", SELF));
    }
    @Test public void ourOwnIconsNeverShowInTheCustomDrawer() {
        assertFalse(AppVisibility.isShown(set(SELF, "a"), SELF, "com.tj.launcher.ControlActivity", SELF));
        assertFalse(AppVisibility.isShown(set(SELF), SELF, null, SELF));
    }
    @Test public void aReinstalledAppCountsAsNewAndIsHidden() {
        Set<String> allowed = AppVisibility.baseline(set("a", "b"), SELF);
        allowed = AppVisibility.withoutRemoved(allowed, "b");   // uninstalled
        assertFalse(AppVisibility.isShown(allowed, "b", "B", SELF));   // reinstalled: hidden until allowed
    }

    // BV1 (2026-11-11): the music app (Eleven) must be in the custom drawer without a visit to Hidden apps.
    private static final String MUSIC = "org.lineageos.eleven";

    @Test public void theMusicAppIsAutoAllowedAndNothingElseIs() {
        assertTrue(AppVisibility.autoAllowDue(MUSIC, new HashSet<String>()));
        assertTrue(AppVisibility.autoAllowDue(MUSIC, null));
        assertFalse(AppVisibility.autoAllowDue("com.some.other", new HashSet<String>()));
        assertFalse(AppVisibility.autoAllowDue(null, new HashSet<String>()));
        assertEquals(set(MUSIC), AppVisibility.AUTO_ALLOWED_PACKAGES);   // a new entry here is a decision: write it in TASKS.md first
    }
    @Test public void autoAllowingMakesTheMusicAppShownLikeAnyAllowedApp() {
        Set<String> allowed = AppVisibility.baseline(set("a"), SELF);
        assertFalse(AppVisibility.isShown(allowed, MUSIC, "H", SELF));   // before: an app installed after the baseline, hidden
        allowed = AppVisibility.withShown(allowed, MUSIC, true);          // what HiddenApps does when autoAllowDue says so
        assertTrue(AppVisibility.isShown(allowed, MUSIC, "H", SELF));
    }
    @Test public void aHandledMusicAppIsNeverAutoAllowedAgain() {
        Set<String> done = AppVisibility.withDone(new HashSet<String>(), MUSIC);
        assertFalse(AppVisibility.autoAllowDue(MUSIC, done));   // Tj hid it by hand after the first allowing: it stays hidden
        assertEquals(set(MUSIC), done);
    }
    @Test public void aReinstalledMusicAppIsAutoAllowedAgain() {
        Set<String> done = AppVisibility.withDone(null, MUSIC);
        done = AppVisibility.withoutDone(done, MUSIC);   // uninstalled
        assertTrue(AppVisibility.autoAllowDue(MUSIC, done));
        assertEquals(new HashSet<String>(), AppVisibility.withoutDone(null, MUSIC));
    }
}
