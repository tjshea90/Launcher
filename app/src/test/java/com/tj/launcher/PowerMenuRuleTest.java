package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PowerMenuRuleTest {
    private static final String H = Features.POWER_HIDE_SCREENSHOT, B = Features.POWER_BLOCK, O = Features.POWER_OFF;

    @Test public void offDoesNothing() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(O, true, false, true, true)); }
    @Test public void quickstepModeNeverTouchesTheMenu() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(B, false, false, true, true)); }
    @Test public void blockNeverActsOnTheLockScreen() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(B, true, true, true, true)); }
    @Test public void noMenuNoAction() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(H, true, false, false, false)); }
    @Test public void hideCoversTheScreenshotEntry() { assertEquals(PowerMenuRule.COVER_SCREENSHOT, PowerMenuRule.decide(H, true, false, true, true)); }
    @Test public void hideFailsOpenWhenEntryNotFound() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(H, true, false, true, false)); }
    @Test public void blockClosesTheMenu() { assertEquals(PowerMenuRule.CLOSE_MENU, PowerMenuRule.decide(B, true, false, true, false)); }
    @Test public void wallpaperModeCoversLikeHideMode() { assertEquals(PowerMenuRule.COVER_SCREENSHOT, PowerMenuRule.decide(Features.POWER_WALLPAPER, true, false, true, true)); }
    @Test public void coverWorksOnTheLockScreenToo() { assertEquals(PowerMenuRule.COVER_SCREENSHOT, PowerMenuRule.decide(H, true, true, true, true)); assertEquals(PowerMenuRule.COVER_SCREENSHOT, PowerMenuRule.decide(Features.POWER_WALLPAPER, true, true, true, true)); }
    // ---- AD7a (Tj, 2026-11-06): the cover works on EVERY lock screen, also one that began in Quickstep; unlocked Quickstep stays untouched
    @Test public void aLockScreenIsCoverWorldWhateverTheModeFlagSays() {
        assertEquals(true, PowerMenuRule.coverWorld(false, true));
        assertEquals(true, PowerMenuRule.coverWorld(true, true));
        assertEquals(true, PowerMenuRule.coverWorld(true, false));
        assertEquals(false, PowerMenuRule.coverWorld(false, false));
    }
    @Test public void aQuickstepSessionLockedFromQuicksteStillGetsTheCover() {
        assertEquals(PowerMenuRule.COVER_SCREENSHOT, PowerMenuRule.decide(H, PowerMenuRule.coverWorld(false, true), true, true, true));
        assertEquals(PowerMenuRule.COVER_SCREENSHOT, PowerMenuRule.decide(Features.POWER_WALLPAPER, PowerMenuRule.coverWorld(false, true), true, true, true));
    }
    @Test public void unlockedQuickstepIsStillNeverTouched() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(H, PowerMenuRule.coverWorld(false, false), false, true, true)); }
    @Test public void blockStillStaysOffTheLockScreenInTheCoverWorld() { assertEquals(PowerMenuRule.NOTHING, PowerMenuRule.decide(B, PowerMenuRule.coverWorld(false, true), true, true, true)); }
    @Test public void isCoverKnowsBothModes() { assertEquals(true, Features.isCover(Features.POWER_HIDE_SCREENSHOT) && Features.isCover(Features.POWER_WALLPAPER) && !Features.isCover(Features.POWER_BLOCK) && !Features.isCover(Features.POWER_OFF)); }

    // ---------------------------------------------------------------- BN2: the dying dialog (tablet report 2026-11-09: a scan 143 ms after the Home key drew the TALL card over the fading stock menu)
    @Test public void aDialogSeenRightAfterACloseIsTheDyingOneNotANewOpening() {
        assertEquals(true, PowerMenuRule.dyingDialog(1143, 1000));
        assertEquals(true, PowerMenuRule.dyingDialog(1000, 1000));
        assertEquals(true, PowerMenuRule.dyingDialog(1000 + PowerMenuRule.DYING_DIALOG_MS - 1, 1000));
    }
    @Test public void afterTheFadeOutHasHadItsTimeADialogIsBelievedAgain() {
        assertEquals(false, PowerMenuRule.dyingDialog(1000 + PowerMenuRule.DYING_DIALOG_MS, 1000));
        assertEquals(false, PowerMenuRule.dyingDialog(60_000, 1000));
    }
    @Test public void noCloseYetAndAClockThatWentBackwardsNeverBlockAnOpening() {
        assertEquals("no close seen yet", false, PowerMenuRule.dyingDialog(500, 0));
        assertEquals("the mark lies in the future: ignore it", false, PowerMenuRule.dyingDialog(500, 900));
    }
    @Test public void theFadeOutOfTheStockDialogIsShorterThanTheMarkSoTheMarkCoversItAll() {
        assertEquals(true, PowerMenuRule.DYING_DIALOG_MS >= PatchFade.CLOSE_MS);
    }
}
