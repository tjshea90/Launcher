package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import org.junit.Test;

/** BN1: the Battery page text rules (AOSP 10 PowerUtil / StringUtil / BatteryInfo) and the two duration formats in SsFormat. */
public class SsBatteryTextTest {
    private static final long MIN = 60_000L, HOUR = 60 * MIN, DAY = 24 * HOUR;
    private final SsBatteryText.TimeOfDay clock = new SsBatteryText.TimeOfDay() { @Override public String format(long ms) { return "T" + ms; } };

    @Test public void elapsedFormatsLikeIcuShortEnglish() {
        assertEquals("0 min", SsFormat.elapsed(0));
        assertEquals("0 min", SsFormat.elapsed(29_999));      // under 30 s rounds down to nothing
        assertEquals("1 min", SsFormat.elapsed(30_000));      // 30 s rounds up
        assertEquals("59 min", SsFormat.elapsed(59 * MIN));
        assertEquals("1 hr, 3 min", SsFormat.elapsed(HOUR + 3 * MIN));
        assertEquals("1 hr, 16 min", SsFormat.elapsed(76 * MIN + 3322));   // Tj's screenshot: "1 hr, 16 min"
        assertEquals("2 hr", SsFormat.elapsed(2 * HOUR));
        assertEquals("1 day, 3 hr", SsFormat.elapsed(DAY + 3 * HOUR));
        assertEquals("1 day", SsFormat.elapsed(DAY));
        assertEquals("2 days", SsFormat.elapsed(2 * DAY));
        assertEquals("2 days, 1 hr, 5 min", SsFormat.elapsed(2 * DAY + HOUR + 5 * MIN));
        assertEquals("0 min", SsFormat.elapsed(-5 * MIN));
    }

    @Test public void agoFormatsLikeRelativeDateTimeLong() {
        assertEquals("0 minutes ago", SsFormat.ago(0));
        assertEquals("1 minute ago", SsFormat.ago(MIN));
        assertEquals("109 minutes ago", SsFormat.ago(109 * MIN));   // Tj's screenshot
        assertEquals("113 minutes ago", SsFormat.ago(113 * MIN));
        assertEquals("2 hours ago", SsFormat.ago(2 * HOUR));        // the first value that switches to hours
        assertEquals("2 hours ago", SsFormat.ago(2 * HOUR + 20 * MIN));
        assertEquals("40 hours ago", SsFormat.ago(40 * HOUR));
        assertEquals("2 days ago", SsFormat.ago(2 * DAY));
        assertEquals("3 days ago", SsFormat.ago(3 * DAY));
    }

    @Test public void roundingFollowsPowerUtil() {
        assertEquals(15 * MIN, SsBatteryText.roundToNearest(16 * MIN, 15 * MIN));
        assertEquals(15 * MIN, SsBatteryText.roundToNearest(22 * MIN + 29_000, 15 * MIN));   // remainder just under 7.5 min: down
        assertEquals(30 * MIN, SsBatteryText.roundToNearest(22 * MIN + 30_000, 15 * MIN));   // exactly half: up
        assertEquals(HOUR, SsBatteryText.roundToNearest(HOUR + 29 * MIN, HOUR));
        assertEquals(2 * HOUR, SsBatteryText.roundToNearest(HOUR + 30 * MIN, HOUR));
    }

    @Test public void dischargeThresholds() {
        assertNull(SsBatteryText.discharge(0, 0, clock));
        assertNull(SsBatteryText.discharge(-1, 0, clock));
        assertEquals("Tablet may shutdown soon", SsBatteryText.discharge(MIN, 0, clock));
        assertEquals("Tablet may shutdown soon", SsBatteryText.discharge(7 * MIN, 0, clock));
        assertEquals("Less than 15 min remaining", SsBatteryText.discharge(7 * MIN + 1, 0, clock));
        assertEquals("Less than 15 min remaining", SsBatteryText.discharge(15 * MIN, 0, clock));
        assertEquals("Should last until about T900000", SsBatteryText.discharge(15 * MIN + 1, 0, clock));
        assertEquals("Should last until about T1800000", SsBatteryText.discharge(23 * MIN, 0, clock));   // 23 min -> nearest 15 = 30
        assertEquals("Should last until about T" + (DAY - 15 * MIN > 0 ? 23 * HOUR + 45 * MIN : 0), SsBatteryText.discharge(DAY - 1, 0, clock));
        assertEquals("About 1 day left", SsBatteryText.discharge(DAY, 0, clock));
        assertEquals("About 1 day, 3 hr left", SsBatteryText.discharge(DAY + 3 * HOUR + 20 * MIN, 0, clock));
        assertEquals("About 1 day, 23 hr left", SsBatteryText.discharge(47 * HOUR, 0, clock));
        assertEquals("More than 2 days remaining", SsBatteryText.discharge(2 * DAY, 0, clock));
        assertEquals("More than 2 days remaining", SsBatteryText.discharge(10 * DAY, 0, clock));
    }

    @Test public void statusLabels() {
        assertEquals("Charging", SsBatteryText.status(2));
        assertEquals("Not charging", SsBatteryText.status(3));
        assertEquals("Plugged in, can’t charge right now", SsBatteryText.status(4));
        assertEquals("Full", SsBatteryText.status(5));
        assertEquals("Unknown", SsBatteryText.status(1));
        assertEquals("Unknown", SsBatteryText.status(0));
    }

    @Test public void headerWhileDischarging() {
        assertEquals("Should last until about T900000", SsBatteryText.header(0, 3, 16 * MIN, -1, 0, clock));
        assertEquals("Not charging", SsBatteryText.header(0, 3, -1, -1, 0, clock));        // no estimate yet: the status label
        assertEquals("Not charging", SsBatteryText.header(0, 3, 0, 5 * HOUR, 0, clock));   // a charge time is ignored while unplugged
    }

    @Test public void headerWhilePluggedIn() {
        assertEquals("1 hr, 35 min left until fully charged", SsBatteryText.header(1, 2, -1, 95 * MIN, 0, clock));
        assertEquals("Charging", SsBatteryText.header(1, 2, -1, -1, 0, clock));
        assertEquals("Full", SsBatteryText.header(2, 5, -1, 40 * MIN, 0, clock));          // full: never "left until fully charged"
        assertEquals("Plugged in, can’t charge right now", SsBatteryText.header(1, 4, -1, -1, 0, clock));
    }

    @Test public void joinAndStillWorks() {
        assertEquals("A, B and C", SsFormat.joinAnd(Arrays.asList("A", "B", "C")));
    }
}
