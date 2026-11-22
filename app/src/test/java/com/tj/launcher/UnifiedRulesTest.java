package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CV1: the pure rules of the one launcher (which home answers, which profile the model holds, when it reloads, what the unlocked drawer shows). */
public class UnifiedRulesTest {
    private static final String SELF = "com.tj.launcher";

    @Test public void activeNeedsTheSwitchAndAnUntrippedBreaker() {
        assertTrue(UnifiedRules.active(true, false));
        assertFalse(UnifiedRules.active(false, false));
        assertFalse("a tripped breaker is the emergency exit: the old two-launcher behaviour", UnifiedRules.active(true, true));
        assertFalse(UnifiedRules.active(false, true));
    }

    @Test public void customAlwaysAnswersWithOurLauncher() {
        assertEquals(UnifiedRules.Home.OURS, UnifiedRules.homeFor(true, true));
        assertEquals(UnifiedRules.Home.OURS, UnifiedRules.homeFor(true, false));
    }

    @Test public void theQuickstepWorldIsOursOnlyWhileTheOneLauncherIsInForce() {
        assertEquals(UnifiedRules.Home.OURS, UnifiedRules.homeFor(false, true));
        assertEquals("switch off or breaker tripped = the stock home, exactly as before", UnifiedRules.Home.STOCK, UnifiedRules.homeFor(false, false));
    }

    @Test public void theUnlockedProfileExistsOnlyInTheQuickstepWorldWhileInForce() {
        assertEquals(UnifiedRules.Profile.UNLOCKED, UnifiedRules.profile(false, true));
        assertEquals(UnifiedRules.Profile.CUSTOM, UnifiedRules.profile(true, true));
        assertEquals(UnifiedRules.Profile.CUSTOM, UnifiedRules.profile(true, false));
        assertEquals("not in force: our launcher in front by accident is still the custom profile", UnifiedRules.Profile.CUSTOM, UnifiedRules.profile(false, false));
    }

    @Test public void eachProfileHasItsOwnLayoutDatabase() {
        assertEquals("launcher.db", UnifiedRules.dbName(UnifiedRules.Profile.CUSTOM));
        assertEquals("launcher_unlocked.db", UnifiedRules.dbName(UnifiedRules.Profile.UNLOCKED));
        assertEquals(UnifiedRules.Profile.UNLOCKED, UnifiedRules.profileOfDb("launcher_unlocked.db"));
        assertEquals(UnifiedRules.Profile.CUSTOM, UnifiedRules.profileOfDb("launcher.db"));
        assertEquals("anything unknown is the custom database", UnifiedRules.Profile.CUSTOM, UnifiedRules.profileOfDb(null));
        assertEquals(UnifiedRules.Profile.CUSTOM, UnifiedRules.profileOfDb("other.db"));
    }

    @Test public void theCustomDatabaseNameIsLaunchers3sOwn() {
        assertEquals(com.android.launcher3.LauncherFiles.LAUNCHER_DB, UnifiedRules.DB_CUSTOM);
        assertEquals(com.android.launcher3.LauncherFiles.LAUNCHER_UNLOCKED_DB, UnifiedRules.DB_UNLOCKED);
    }

    @Test public void reloadOnlyWhenTheModelHoldsAnotherProfile() {
        assertTrue(UnifiedRules.reloadNeeded(UnifiedRules.Profile.UNLOCKED, UnifiedRules.Profile.CUSTOM));
        assertTrue(UnifiedRules.reloadNeeded(UnifiedRules.Profile.CUSTOM, UnifiedRules.Profile.UNLOCKED));
        assertFalse(UnifiedRules.reloadNeeded(UnifiedRules.Profile.CUSTOM, UnifiedRules.Profile.CUSTOM));
        assertFalse(UnifiedRules.reloadNeeded(UnifiedRules.Profile.UNLOCKED, UnifiedRules.Profile.UNLOCKED));
        assertFalse("nothing loaded yet: the first load picks the wanted profile itself", UnifiedRules.reloadNeeded(UnifiedRules.Profile.UNLOCKED, null));
    }

    @Test public void readyMeansTheWantedProfileIsLoadedAndTheLoadFinished() {
        assertTrue(UnifiedRules.ready(UnifiedRules.Profile.CUSTOM, UnifiedRules.Profile.CUSTOM, true));
        assertFalse("loading", UnifiedRules.ready(UnifiedRules.Profile.CUSTOM, UnifiedRules.Profile.CUSTOM, false));
        assertFalse("the other profile is still shown", UnifiedRules.ready(UnifiedRules.Profile.CUSTOM, UnifiedRules.Profile.UNLOCKED, true));
        assertFalse("never loaded", UnifiedRules.ready(UnifiedRules.Profile.UNLOCKED, null, true));
    }

    @Test public void theStrayGuardsFollowWhoseHomeItIs() {
        assertTrue("old world: our home in a Quickstep session is a mistake", UnifiedRules.strayCustomApplies(false));
        assertFalse("one launcher: nothing is stray", UnifiedRules.strayCustomApplies(true));
        assertTrue(UnifiedRules.strayStockApplies(true, false));
        assertTrue(UnifiedRules.strayStockApplies(true, true));
        assertTrue("one launcher: a stock HOME window is stray in the Quickstep world too", UnifiedRules.strayStockApplies(false, true));
        assertFalse("old world: the stock home IS the Quickstep home", UnifiedRules.strayStockApplies(false, false));
    }

    @Test public void afterAHandshakeTheRightHomeOrTheRememberedAppIsInFront() {
        assertTrue(UnifiedRules.quickstepFrontOk(false, true, false, false));
        assertFalse("old world: our own home is the wrong one", UnifiedRules.quickstepFrontOk(false, false, true, false));
        assertTrue(UnifiedRules.quickstepFrontOk(true, false, true, false));
        assertFalse("one launcher: the stock home is the wrong one", UnifiedRules.quickstepFrontOk(true, true, false, false));
        assertTrue("the remembered app always counts", UnifiedRules.quickstepFrontOk(true, false, false, true));
        assertTrue(UnifiedRules.quickstepFrontOk(false, false, false, true));
        assertFalse("nothing in front", UnifiedRules.quickstepFrontOk(true, false, false, false));
    }

    @Test public void appsStartedFromTheUnlockedHomeAreQuickstepSessionApps() {
        assertTrue(UnifiedRules.marksCustomOrigin(UnifiedRules.Profile.CUSTOM));
        assertFalse(UnifiedRules.marksCustomOrigin(UnifiedRules.Profile.UNLOCKED));
    }

    @Test public void theUnlockedDrawerShowsEverythingTheStockDrawerShowedExceptTheLookAlike() {
        assertTrue(UnifiedRules.shownInUnlocked("com.android.settings", "com.android.settings.Settings", SELF));
        assertTrue(UnifiedRules.shownInUnlocked("org.lineageos.eleven", null, SELF));
        assertTrue("Launcher control must be in the unlocked drawer", UnifiedRules.shownInUnlocked(SELF, "com.tj.launcher.ControlActivity", SELF));
        assertTrue("Back to custom too", UnifiedRules.shownInUnlocked(SELF, "com.tj.launcher.BackToCustomActivity", SELF));
        assertFalse("the Settings look-alike exists only in the custom world", UnifiedRules.shownInUnlocked(SELF, SettingsSpoof.ALIAS, SELF));
        assertFalse(UnifiedRules.shownInUnlocked(null, null, SELF));
    }

    @Test public void theCustomRuleIsUntouched() {
        // the custom profile still hides our own icons (except the alias) and everything not allowed
        java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Collections.singletonList("com.a"));
        assertTrue(AppVisibility.isShown(allowed, "com.a", null, SELF));
        assertFalse(AppVisibility.isShown(allowed, "com.b", null, SELF));
        assertFalse(AppVisibility.isShown(allowed, SELF, "com.tj.launcher.ControlActivity", SELF));
        assertTrue(AppVisibility.isShown(allowed, SELF, SettingsSpoof.ALIAS, SELF));
    }
}
