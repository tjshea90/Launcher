package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** BN1 M0: the gating rules of the Settings look-alike (drawer entry, real Settings kept out, page guard) and the page table. */
public class SettingsSpoofTest {
    private static final String SELF = "com.tj.launcher";

    @Test public void theAliasExistsOnlyInCustomModeWithTheSwitchOn() {
        assertTrue(SettingsSpoof.aliasWanted(true, true));
        assertFalse(SettingsSpoof.aliasWanted(true, false));    // Quickstep: the stock drawer keeps its single real Settings
        assertFalse(SettingsSpoof.aliasWanted(false, true));    // switch off
        assertFalse(SettingsSpoof.aliasWanted(false, false));
    }

    @Test public void theRealSettingsIsKeptOutOfTheDrawerOnlyWhileTheLookAlikeIsTheEntry() {
        String real = SettingsSpoof.REAL_SETTINGS_PACKAGE;
        assertTrue(SettingsSpoof.hidesRealSettings(true, true, real));
        assertFalse(SettingsSpoof.hidesRealSettings(true, false, real));   // Quickstep mode: the chooser shows the true state
        assertFalse(SettingsSpoof.hidesRealSettings(false, true, real));   // switch off: the real one is as allowed before
        assertFalse(SettingsSpoof.hidesRealSettings(true, true, "com.android.chrome"));
        assertFalse(SettingsSpoof.hidesRealSettings(true, true, null));
    }

    @Test public void anOpenPageClosesWhenTheLookAlikeIsNotWantedAnyMore() {
        assertTrue(SettingsSpoof.pageAllowed(true, true));
        assertFalse(SettingsSpoof.pageAllowed(true, false));
        assertFalse(SettingsSpoof.pageAllowed(false, true));
    }

    @Test public void theAliasIsTheOneOwnComponentTheCustomDrawerShows() {
        Set<String> allowed = new HashSet<>(Collections.singletonList("a"));
        assertTrue(AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS.contains(SettingsSpoof.ALIAS));
        assertEquals(1, AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS.size());
        assertTrue(AppVisibility.isShown(allowed, SELF, SettingsSpoof.ALIAS, SELF));
        assertFalse(AppVisibility.isShown(allowed, SELF, "com.tj.launcher.ControlActivity", SELF));   // our other icons stay hidden
        assertFalse(AppVisibility.isShown(allowed, SELF, "com.tj.launcher.BackToCustomActivity", SELF));
        assertFalse(AppVisibility.isShown(allowed, SELF, null, SELF));
    }

    @Test public void theEntrySetCannotBeChangedAtRunTime() {
        try { AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS.add("com.tj.launcher.ControlActivity"); throw new AssertionError("the set must be read-only"); }
        catch (UnsupportedOperationException expected) { /* a stray add() could put Launcher control into the custom drawer */ }
    }

    @Test public void aTaskRootedInTheAliasOrThePageActivityIsTheLookAlikesOwn() {
        assertTrue(SettingsSpoof.isLookAlikeComponent(SettingsSpoof.ALIAS));
        assertTrue(SettingsSpoof.isLookAlikeComponent("com.tj.launcher.SsPageActivity"));
        assertFalse(SettingsSpoof.isLookAlikeComponent("com.tj.launcher.ControlActivity"));
        assertFalse(SettingsSpoof.isLookAlikeComponent(null));
    }

    /** WORK IN PROGRESS pin: the look-alike is OFF by default until its gate is green; the finishing commit changes this test together with {@link SettingsSpoof#defaultOn}. */
    @Test public void wipDefaultIsOff() {
        assertFalse(SettingsSpoof.defaultOn(false));
        assertFalse(SettingsSpoof.defaultOn(true));
    }

    @Test public void pagesAreFoundByIdAndAnythingUnknownIsTheHomePage() {
        for (SsPage p : SsPage.values()) assertEquals(p, SsPage.fromId(p.id));
        assertEquals(SsPage.HOME, SsPage.fromId(null));
        assertEquals(SsPage.HOME, SsPage.fromId("no_such_page"));
        assertEquals(SsPage.HOME, SsPage.fromId(""));
    }

    @Test public void theHomeListIsInTjsOrder() {   // Tj, 2026-11-09 02:06 UTC: Battery / Display / Sound / Storage / Accessibility / System / About tablet
        assertArrayEquals(new SsPage[] { SsPage.BATTERY, SsPage.DISPLAY, SsPage.SOUND, SsPage.STORAGE, SsPage.ACCESSIBILITY, SsPage.SYSTEM, SsPage.ABOUT }, SsPage.homeEntries());
    }

    @Test public void pageIdsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (SsPage p : SsPage.values()) assertTrue("duplicate id " + p.id, seen.add(p.id));
    }
}
