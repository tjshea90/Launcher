package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.text.NumberFormat;
import java.util.Locale;
import org.junit.Test;

/** BN1: the Display page rules. Expected numbers were computed from the AOSP 10 formulas independently (see research/aosp10-settings/A_battery_display.md sec. 4). */
public class SsDisplayRulesTest {
    private static String pct(int raw) { return NumberFormat.getPercentInstance(Locale.US).format(SsDisplayRules.brightnessFraction(raw, 10, 255)); }

    @Test public void brightnessSummaryFollowsTheHlgCurve() {
        assertEquals("0%", pct(10));
        assertEquals("25%", pct(15));    // Tj's tablet reads 25 %: a raw value of about 15
        assertEquals("35%", pct(20));
        assertEquals("49%", pct(30));    // 49.5 rounds half-even to 49 (NumberFormat)
        assertEquals("59%", pct(40));
        assertEquals("82%", pct(102));
        assertEquals("100%", pct(255));
    }

    @Test public void gammaRoundTripsAndKnownPoints() {
        assertEquals(0, SsDisplayRules.toGamma(10, 10, 255));
        assertEquals(253, SsDisplayRules.toGamma(15, 10, 255));
        assertEquals(836, SsDisplayRules.toGamma(102, 10, 255));
        assertEquals(1023, SsDisplayRules.toGamma(255, 10, 255));
        assertEquals(10, SsDisplayRules.fromGamma(0, 10, 255));
        assertEquals(15, SsDisplayRules.fromGamma(256, 10, 255));
        assertEquals(30, SsDisplayRules.fromGamma(512, 10, 255));
        assertEquals(75, SsDisplayRules.fromGamma(768, 10, 255));
        assertEquals(255, SsDisplayRules.fromGamma(1023, 10, 255));
        for (int raw = 10; raw <= 255; raw++) assertEquals("raw " + raw, raw, SsDisplayRules.fromGamma(SsDisplayRules.toGamma(raw, 10, 255), 10, 255), 1);
    }

    @Test public void timeoutSummary() {
        assertEquals("After 1 minute of inactivity", SsDisplayRules.timeoutSummary(60_000));   // Tj's screenshot
        assertEquals("After 15 seconds of inactivity", SsDisplayRules.timeoutSummary(15_000));
        assertEquals("After 30 minutes of inactivity", SsDisplayRules.timeoutSummary(1_800_000));
        assertEquals("", SsDisplayRules.timeoutSummary(45_000));
        assertEquals(2, SsDisplayRules.timeoutIndex(60_000));
        assertEquals(-1, SsDisplayRules.timeoutIndex(-1));
        assertEquals(SsDisplayRules.TIMEOUT_VALUES.length, SsDisplayRules.TIMEOUT_LABELS.length);
    }

    @Test public void fontIndexUsesTheMidpoints() {
        assertEquals(1, SsDisplayRules.fontIndex(1.2f));      // the tablet's own scale is "Default"
        assertEquals(1, SsDisplayRules.fontIndex(1.1999817f));
        assertEquals(0, SsDisplayRules.fontIndex(1.0f));
        assertEquals(0, SsDisplayRules.fontIndex(1.1249f));
        assertEquals(1, SsDisplayRules.fontIndex(1.125f));    // exactly the midpoint of the first two steps: the upper step
        assertEquals(2, SsDisplayRules.fontIndex(1.35f));
        assertEquals(3, SsDisplayRules.fontIndex(1.5f));
        assertEquals(3, SsDisplayRules.fontIndex(2.0f));
        assertEquals("Default", SsDisplayRules.FONT_LABELS[SsDisplayRules.fontIndex(1.2f)]);
    }

    @Test public void densityStopsOfTheTablet() {
        SsDisplayRules.Density d = SsDisplayRules.densityStops(170, 600, 1024, 170);
        assertArrayEquals(new int[] { 144, 170, 198, 226, 254 }, d.values);
        assertArrayEquals(new String[] { "Small", "Default", "Large", "Larger", "Largest" }, d.labels);
        assertEquals(1, d.index);
        assertEquals(4, SsDisplayRules.densityStops(170, 600, 1024, 254).index);
        assertEquals(0, SsDisplayRules.densityStops(170, 600, 1024, 144).index);
    }

    @Test public void otherScreens() {
        assertArrayEquals(new int[] { 136, 160, 186, 212, 240 }, SsDisplayRules.densityStops(160, 1080, 1920, 160).values);
        assertArrayEquals(new int[] { 238, 280, 306, 332, 360 }, SsDisplayRules.densityStops(280, 720, 1280, 280).values);
    }

    @Test public void anUnknownDensityIsACustomEntry() {
        SsDisplayRules.Density d = SsDisplayRules.densityStops(170, 600, 1024, 180);
        assertEquals(6, d.values.length);
        assertEquals(180, d.values[5]);
        assertEquals("Custom (180)", d.labels[5]);
        assertEquals(5, d.index);
    }
}
