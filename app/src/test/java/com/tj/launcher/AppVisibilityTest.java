package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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

    // BV1 (2026-11-11): the music app (Eleven) must be in the custom drawer without a visit to Hidden apps. The rule is a pure state machine over two sets (allowed, done); done holds ONE entry per
    // INSTALL ("pkg@firstInstallTime"), so an update in place (same time) keeps Tj's choice and a reinstall (new time) is new, even when the removal event was missed (review of BV1, M1 and M2).
    private static final String MUSIC = "org.lineageos.eleven";
    private static final long T1 = 1_700_000_000_000L;       // when one install began
    private static final long T2 = T1 + 86_400_000L;         // a later reinstall

    private static AppVisibility.Marks start() { return new AppVisibility.Marks(AppVisibility.baseline(set("a", "b"), SELF), new HashSet<String>()); }
    private static boolean shown(AppVisibility.Marks m, String pkg) { return AppVisibility.isShown(m.allowed, pkg, "X", SELF); }
    private static boolean hasEntryOf(Set<String> done, String pkg) { for (String e : done) if (e.equals(pkg) || e.startsWith(pkg + "@")) return true; return false; }

    @Test public void onlyTheMusicAppIsAutoAllowed() {
        assertEquals(set(MUSIC), AppVisibility.AUTO_ALLOWED_PACKAGES);   // a new entry here is a decision: write it in TASKS.md first
        AppVisibility.Marks m = start();
        assertSame(m, AppVisibility.asked(m, "com.some.other", T1));
        assertSame(m, AppVisibility.asked(m, null, T1));
        assertFalse(shown(AppVisibility.asked(m, "com.some.other", T1), "com.some.other"));
    }
    @Test public void theFirstAskOfAnInstallAllowsTheMusicAppOnceAndWritesItDown() {
        AppVisibility.Marks m0 = start();
        assertFalse(shown(m0, MUSIC));                               // an app installed after the baseline: hidden
        AppVisibility.Marks m1 = AppVisibility.asked(m0, MUSIC, T1);
        assertTrue(shown(m1, MUSIC));
        assertEquals(set(AppVisibility.doneEntry(MUSIC, T1)), m1.done);
        assertSame(m1, AppVisibility.asked(m1, MUSIC, T1));          // the second ask of the same install changes nothing (no write)
        assertTrue(shown(m1, "a") && shown(m1, "b"));                // everything else untouched
    }
    @Test public void anUnreadableInstallTimeWritesNothing() {
        AppVisibility.Marks m = start();
        assertSame(m, AppVisibility.asked(m, MUSIC, 0));             // not installed any more (a late ask) or not readable
        assertSame(m, AppVisibility.asked(m, MUSIC, -5));
        assertFalse(shown(m, MUSIC));
    }
    @Test public void aHandHideSticksAcrossAsksAndUpdates() {
        AppVisibility.Marks m1 = AppVisibility.asked(start(), MUSIC, T1);
        AppVisibility.Marks m2 = AppVisibility.handSet(m1, MUSIC, false, T1);   // Tj hides it
        assertFalse(shown(m2, MUSIC));
        assertSame(m2, AppVisibility.asked(m2, MUSIC, T1));          // asked again, and after an update in place (the install time is the same): still hidden, nothing written
    }
    @Test public void aReinstallWithItsRemovalEventIsAutoAllowedAgain() {
        AppVisibility.Marks m2 = AppVisibility.handSet(AppVisibility.asked(start(), MUSIC, T1), MUSIC, false, T1);
        AppVisibility.Marks m3 = AppVisibility.removed(m2, MUSIC);   // Launcher3 told us the package is gone
        assertFalse(hasEntryOf(m3.done, MUSIC));
        assertFalse(shown(m3, MUSIC));
        assertTrue(shown(AppVisibility.asked(m3, MUSIC, T2), MUSIC));
    }
    @Test public void aReinstallWhoseRemovalEventWasMissedIsAutoAllowedAgain() {
        // review M1: the launcher process was dead (or its model not created) when the app was uninstalled, so onRemoved never ran; the new install time says it is a new app
        AppVisibility.Marks m2 = AppVisibility.handSet(AppVisibility.asked(start(), MUSIC, T1), MUSIC, false, T1);
        AppVisibility.Marks m3 = AppVisibility.asked(m2, MUSIC, T2);
        assertTrue(shown(m3, MUSIC));
        assertEquals(set(AppVisibility.doneEntry(MUSIC, T2)), m3.done);   // the old install's entry is gone: one entry per package
    }
    @Test public void aHandAllowIsRememberedLikeTheAutomaticOne() {
        AppVisibility.Marks m1 = AppVisibility.handSet(start(), MUSIC, true, T1);
        assertTrue(shown(m1, MUSIC));
        assertSame(m1, AppVisibility.asked(m1, MUSIC, T1));
        AppVisibility.Marks m2 = AppVisibility.handSet(m1, MUSIC, false, T1);
        assertFalse(shown(m2, MUSIC));
        assertSame(m2, AppVisibility.asked(m2, MUSIC, T1));
    }
    @Test public void aHandChoiceOfAnotherPackageTouchesNeitherSet() {
        AppVisibility.Marks m0 = new AppVisibility.Marks(AppVisibility.baseline(set("a", "b"), SELF), set("x@5"));
        AppVisibility.Marks m1 = AppVisibility.handSet(m0, "a", false, 0);
        assertFalse(shown(m1, "a"));
        assertSame(m0.done, m1.done);                                // the done set is not even copied (HiddenApps writes it only when it changed)
        assertTrue(shown(AppVisibility.handSet(m0, "newapp", true, 0), "newapp"));
    }
    @Test public void aRemovalForgetsTheMusicAppOnlyAndAlsoAnOldBareEntry() {
        AppVisibility.Marks m = new AppVisibility.Marks(set("a", MUSIC), set(MUSIC, "x@5", MUSIC + "@7"));   // a bare "pkg" is the format of the unreleased first build
        AppVisibility.Marks r = AppVisibility.removed(m, MUSIC);
        assertEquals(set("a"), r.allowed);
        assertEquals(set("x@5"), r.done);
        assertEquals(set(AppVisibility.doneEntry(MUSIC, T1)), AppVisibility.withDone(set(MUSIC), MUSIC, T1));
        assertEquals(new HashSet<String>(), AppVisibility.withoutDone(null, MUSIC));
    }
    @Test public void theMusicAppGetsNoHomeScreenIconOfItsOwnAccord() {
        assertTrue(AppVisibility.noHomeIconOnInstall(MUSIC));
        assertFalse(AppVisibility.noHomeIconOnInstall("com.some.other"));
        assertFalse(AppVisibility.noHomeIconOnInstall(null));
    }
}
