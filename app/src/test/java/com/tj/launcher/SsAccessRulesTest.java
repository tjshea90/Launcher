package com.tj.launcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** BN1: the Accessibility page rules; the expected numbers come from the AOSP 10 sources (C_access_system_about.md) and from the slider positions on Tj's TTS screenshot. */
public class SsAccessRulesTest {
    @Test public void touchAndHoldDelay() {
        assertEquals("Short", SsAccessRules.longPressSummary(400));    // Tj's tablet
        assertEquals("Medium", SsAccessRules.longPressSummary(1000));
        assertEquals("Long", SsAccessRules.longPressSummary(1500));
        assertEquals("", SsAccessRules.longPressSummary(500));         // a value outside the list: nothing selected, no summary
        assertEquals(1, SsAccessRules.longPressIndex(1000));
        assertEquals(-1, SsAccessRules.longPressIndex(-1));
        assertEquals(SsAccessRules.LONG_PRESS_VALUES.length, SsAccessRules.LONG_PRESS_LABELS.length);
    }

    @Test public void magnificationSummaries() {
        assertEquals("Off", SsAccessRules.magnificationSummary(false, false));   // Tj's tablet
        assertEquals("Magnify with triple-tap", SsAccessRules.magnificationSummary(true, false));
        assertEquals("Magnify with shortcut", SsAccessRules.magnificationSummary(false, true));
        assertEquals("Magnify with shortcut & triple-tap", SsAccessRules.magnificationSummary(true, true));
        assertEquals("On", SsAccessRules.onOff(true));
        assertEquals("Off", SsAccessRules.onOff(false));
    }

    @Test public void audioBalanceMapping() {
        assertEquals(100, SsAccessRules.balanceProgress(0f));        // centre, as on Tj's screenshot (thumb in the middle)
        assertEquals(0, SsAccessRules.balanceProgress(-1f));
        assertEquals(200, SsAccessRules.balanceProgress(1f));
        assertEquals(150, SsAccessRules.balanceProgress(0.5f));
        assertEquals(200, SsAccessRules.balanceProgress(7f));        // never outside the bar
        assertEquals(0, SsAccessRules.balanceProgress(-7f));
        assertEquals(0f, SsAccessRules.balanceValue(100), 0f);
        assertEquals(-1f, SsAccessRules.balanceValue(0), 1e-6f);
        assertEquals(0.5f, SsAccessRules.balanceValue(150), 1e-6f);
    }

    @Test public void audioBalanceSnapsToTheMiddle() {
        assertEquals(100, SsAccessRules.snapBalance(100));
        assertEquals(100, SsAccessRules.snapBalance(95));            // within 6 of the middle
        assertEquals(100, SsAccessRules.snapBalance(105));
        assertEquals(94, SsAccessRules.snapBalance(94));             // exactly 6 away: stays (the range is exclusive)
        assertEquals(106, SsAccessRules.snapBalance(106));
        assertEquals(0, SsAccessRules.snapBalance(0));
        assertEquals(200, SsAccessRules.snapBalance(200));
    }

    @Test public void ttsSlidersMatchTheScreenshotPositions() {
        // Tj's screenshot (600 px wide, track 77..570): speech rate thumb at x 152, pitch thumb at x 175, both defaults of 100 %
        double rate = SsAccessRules.rateProgress(100) / (double) SsAccessRules.RATE_PROGRESS_MAX;
        double pitch = SsAccessRules.pitchProgress(100) / (double) SsAccessRules.PITCH_PROGRESS_MAX;
        assertEquals(152, 77 + rate * (570 - 77), 1.5);
        assertEquals(175, 77 + pitch * (570 - 77), 1.5);
        assertEquals(590, SsAccessRules.RATE_PROGRESS_MAX);
        assertEquals(375, SsAccessRules.PITCH_PROGRESS_MAX);
    }

    @Test public void ttsValuesRoundTripAndStayInRange() {
        for (int v = SsAccessRules.RATE_MIN; v <= SsAccessRules.RATE_MAX; v++) assertEquals(v, SsAccessRules.rateValue(SsAccessRules.rateProgress(v)));
        for (int v = SsAccessRules.PITCH_MIN; v <= SsAccessRules.PITCH_MAX; v++) assertEquals(v, SsAccessRules.pitchValue(SsAccessRules.pitchProgress(v)));
        assertEquals(0, SsAccessRules.rateProgress(-5));
        assertEquals(590, SsAccessRules.rateProgress(10_000));
        assertEquals(600, SsAccessRules.rateValue(10_000));
        assertEquals(25, SsAccessRules.pitchValue(-3));
    }
}
