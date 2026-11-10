package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertEquals(2, SsDisplayRules.timeoutIndex(60_000));
        assertEquals(-1, SsDisplayRules.timeoutIndex(-1));
        assertEquals(SsDisplayRules.TIMEOUT_VALUES.length, SsDisplayRules.TIMEOUT_LABELS.length);
    }

    /** BN1p R4: a value that is not in the list reads as the nearest entry; the line is never empty (a tie takes the shorter entry). */
    @Test public void anOffListTimeoutReadsAsTheNearestEntry() {
        assertEquals("After 30 seconds of inactivity", SsDisplayRules.timeoutSummary(45_000));     // exactly between 30 s and 1 min: the shorter
        assertEquals("After 1 minute of inactivity", SsDisplayRules.timeoutSummary(46_000));
        assertEquals("After 15 seconds of inactivity", SsDisplayRules.timeoutSummary(0));
        assertEquals("After 15 seconds of inactivity", SsDisplayRules.timeoutSummary(-1));          // "never" written by a vendor tool
        assertEquals("After 15 seconds of inactivity", SsDisplayRules.timeoutSummary(Long.MIN_VALUE));   // review: no overflow in the distance
        assertEquals("After 30 minutes of inactivity", SsDisplayRules.timeoutSummary(Integer.MAX_VALUE));
        assertEquals("After 5 minutes of inactivity", SsDisplayRules.timeoutSummary(420_000));      // 7 min: closer to 5 than to 10
        for (int i = 0; i < SsDisplayRules.TIMEOUT_VALUES.length; i++) assertEquals(i, SsDisplayRules.nearestTimeoutIndex(SsDisplayRules.TIMEOUT_VALUES[i]));
        assertEquals(-1, SsDisplayRules.timeoutIndex(45_000));                                      // the dialog still selects nothing for it
    }

    /** BN1p R3: only a sane device range replaces the AOSP defaults. */
    @Test public void aBrightnessRangeIsTrustedOnlyWhenSane() {
        assertTrue(SsDisplayRules.rangeOk(10, 255));
        assertTrue(SsDisplayRules.rangeOk(1, 255));
        assertTrue(SsDisplayRules.rangeOk(0, 255));
        assertFalse(SsDisplayRules.rangeOk(255, 255));
        assertFalse(SsDisplayRules.rangeOk(100, 50));
        assertFalse(SsDisplayRules.rangeOk(-1, 255));
        assertFalse(SsDisplayRules.rangeOk(0, 0));
        assertFalse(SsDisplayRules.rangeOk(10, 1023));
        // the same raw value reads differently with another range: why the device's own one matters
        assertEquals(0.0, SsDisplayRules.brightnessFraction(1, 1, 255), 0.0);
        assertTrue(SsDisplayRules.brightnessFraction(15, 1, 255) > SsDisplayRules.brightnessFraction(15, 10, 255));
    }

    @Test public void fontIndexUsesTheMidpoints() {
        assertEquals(1, SsDisplayRules.fontIndex(1.2f));      // the tablet's own scale is "Default"
        assertEquals(1, SsDisplayRules.fontIndex(1.1999817f));
        assertEquals(0, SsDisplayRules.fontIndex(1.0f));
        assertEquals(0, SsDisplayRules.fontIndex(1.09f));     // below the midpoint 1.1 of the first two steps
        assertEquals(1, SsDisplayRules.fontIndex(1.11f));
        assertEquals(1, SsDisplayRules.fontIndex(1.24f));     // below the midpoint 1.25 of Default and Large
        assertEquals(2, SsDisplayRules.fontIndex(1.26f));
        assertEquals(2, SsDisplayRules.fontIndex(1.3f));
        assertEquals(2, SsDisplayRules.fontIndex(1.34f));     // below the midpoint 1.35 of Large and Largest
        assertEquals(3, SsDisplayRules.fontIndex(1.36f));
        assertEquals(3, SsDisplayRules.fontIndex(1.4f));
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

    /** BN1m (Tj, 2026-11-09 07:16): his Display size screenshots never say "Custom"; a density that is no stop becomes the Default of a rebuilt table. */
    @Test public void aDensityThatIsNoStopBecomesTheDefault() {
        // the CI emulator: stable density 160, the run forces 170 -> exactly his tablet's table and "Default"
        SsDisplayRules.Density d = SsDisplayRules.densityStops(160, 600, 1024, 170);
        assertArrayEquals(new int[] { 144, 170, 198, 226, 254 }, d.values);
        assertArrayEquals(new String[] { "Small", "Default", "Large", "Larger", "Largest" }, d.labels);
        assertEquals(1, d.index);
        // an odd value on his tablet: the table is built around it
        SsDisplayRules.Density e = SsDisplayRules.densityStops(170, 600, 1024, 180);
        assertEquals(5, e.values.length);
        assertEquals(180, e.values[e.index]);
        assertEquals("Default", e.labels[e.index]);
        // a value that IS a stop of the stable density keeps the stable table (the normal case)
        assertArrayEquals(new int[] { 136, 160, 186, 212, 240 }, SsDisplayRules.densityStops(160, 600, 1024, 160).values);
        assertEquals(2, SsDisplayRules.densityStops(160, 600, 1024, 186).index);
    }

    @Test public void noDensityEverReadsCustomOrFallsOffTheTable() {
        for (int stable : new int[] { 120, 160, 170, 213, 280, 420 }) {
            for (int cur = 0; cur <= 640; cur += 3) {
                SsDisplayRules.Density d = SsDisplayRules.densityStops(stable, 600, 1024, cur);
                assertTrue("index in range (stable " + stable + ", current " + cur + ")", d.index >= 0 && d.index < d.values.length);
                assertEquals(d.values.length, d.labels.length);
                for (String l : d.labels) assertTrue(l, l.equals("Small") || l.equals("Default") || l.equals("Large") || l.equals("Larger") || l.equals("Largest"));
                if (cur > 0) assertEquals("the selected stop is the current density (stable " + stable + ", current " + cur + ")", cur, d.values[d.index]);
            }
        }
    }
}
