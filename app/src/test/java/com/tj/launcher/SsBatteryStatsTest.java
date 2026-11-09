package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.Test;

/** BN1: the dumpsys batterystats parser. The sample follows the text AOSP 10 BatteryStats.dumpLocked writes (formatTimeMs: zero leading parts left out, a trailing space). */
public class SsBatteryStatsTest {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final String DISCHARGING =
        "Discharge step durations:\n"
        + "  #0: +1h2m3s0ms to 83 (screen-on, power-save-off)\n"
        + "  Estimated discharge time remaining: +9h9m9s0ms\n"           // before the statistics block: must NOT be used
        + "\n"
        + "Statistics since last charge:\n"
        + "  System starts: 0, currently on battery: true\n"
        + "  Estimated battery capacity: 4000 mAh\n"
        + "  Time on battery: 2h 4m 5s 100ms (100.0%) realtime, 2h 4m 5s 100ms (100.0%) uptime\n"
        + "  Time on battery screen off: 48m 0s 0ms (38.7%) realtime, 48m 0s 0ms (38.7%) uptime\n"
        + "  Time on battery screen doze: 0ms (0.0%)\n"
        + "  Total run time: 2h 4m 6s 100ms realtime, 2h 4m 6s 100ms uptime\n"
        + "  Battery time remaining: 5h 3m 12s 345ms \n"
        + "  Discharge: 400 mAh\n"
        + "  Start clock time: 2026-11-09-03-14-11\n"
        + "  Screen on: 1h 16m 3s 322ms (61.3%) 7x, Interactive: 1h 16m 3s 322ms (61.3%)\n"
        + "  Screen brightnesses:\n"
        + "    dark 1h 16m 3s 322ms (100.0%)\n"
        + "  Screen on: 0ms (0.0%) 0x, Interactive: 0ms (0.0%)\n";   // a second block (a uid) must not overwrite the first

    private static final String CHARGING =
        "Statistics since last charge:\n"
        + "  System starts: 0, currently on battery: false\n"
        + "  Total run time: 10m 6s 100ms realtime, 10m 6s 100ms uptime\n"
        + "  Charge time remaining: 1h 35m 0s 0ms \n"
        + "  Start clock time: 2026-11-08-23-59-59\n"
        + "  Screen on: 0ms (0.0%) 0x, Interactive: 0ms (0.0%)\n";

    private static long utc(int y, int mo, int d, int h, int mi, int s) { return LocalDateTime.of(y, mo, d, h, mi, s).toInstant(ZoneOffset.UTC).toEpochMilli(); }

    @Test public void parseTimeReadsTheDumpFormat() {
        assertEquals(5 * 3_600_000L + 3 * 60_000L + 12_000L + 345, SsBatteryStats.parseTime(" 5h 3m 12s 345ms "));
        assertEquals(76 * 60_000L + 3_322, SsBatteryStats.parseTime(" 1h 16m 3s 322ms "));
        assertEquals(0, SsBatteryStats.parseTime(" 0ms "));
        assertEquals(86_400_000L + 2 * 3_600_000L, SsBatteryStats.parseTime(" 1d 2h 0m 0s 0ms "));
        assertEquals(48 * 60_000L, SsBatteryStats.parseTime(" 48m 0s 0ms "));
        assertEquals(-1, SsBatteryStats.parseTime("nothing here"));
        assertEquals(-1, SsBatteryStats.parseTime(null));
    }

    @Test public void parsesADischargingDump() {
        SsBatteryStats.Data d = SsBatteryStats.parse(DISCHARGING, UTC);
        assertEquals("ok", d.status);
        assertEquals(utc(2026, 11, 9, 3, 14, 11), d.startClockMs);
        assertEquals(76 * 60_000L + 3_322, d.screenOnMs);
        assertEquals(5 * 3_600_000L + 3 * 60_000L + 12_000L + 345, d.dischargeMs);   // from the statistics block, not the 9h9m line
        assertEquals(-1, d.chargeMs);
    }

    @Test public void parsesAChargingDump() {
        SsBatteryStats.Data d = SsBatteryStats.parse(CHARGING, UTC);
        assertEquals("ok", d.status);
        assertEquals(utc(2026, 11, 8, 23, 59, 59), d.startClockMs);
        assertEquals(0, d.screenOnMs);
        assertEquals(-1, d.dischargeMs);
        assertEquals(95 * 60_000L, d.chargeMs);
    }

    @Test public void theClockIsReadInTheGivenZone() {
        TimeZone plus2 = TimeZone.getTimeZone("GMT+02:00");
        SsBatteryStats.Data d = SsBatteryStats.parse(DISCHARGING, plus2);
        assertEquals(utc(2026, 11, 9, 1, 14, 11), d.startClockMs);
    }

    @Test public void incompleteOrEmptyDumpsAreMarked() {
        SsBatteryStats.Data none = SsBatteryStats.parse("", UTC);
        assertEquals("empty", none.status);
        assertFalse(none.any());
        SsBatteryStats.Data denied = SsBatteryStats.parse("Permission Denial: can't dump BatteryStats\n", UTC);
        assertFalse(denied.any());
        SsBatteryStats.Data partial = SsBatteryStats.parse("Statistics since last charge:\n  Start clock time: 2026-11-09-03-14-11\n", UTC);
        assertEquals("partial", partial.status);
        assertTrue(partial.any());
        assertEquals(-1, partial.screenOnMs);
    }

    @Test public void linesOutsideTheChargedBlockAreIgnored() {
        String t = "Statistics since last unplugged:\n  Start clock time: 2000-01-01-00-00-00\n  Screen on: 5m 0s 0ms (1.0%) 1x, Interactive: 0ms (0.0%)\n";
        assertFalse(SsBatteryStats.parse(t, UTC).any());
    }

    @Test public void theParserStopsAfterTheScreenLine() {
        SsBatteryStats.Parser p = new SsBatteryStats.Parser(UTC);
        assertFalse(p.done());
        for (String l : DISCHARGING.split("\n")) { p.line(l); if (p.done()) break; }
        assertTrue(p.done());
    }

    // ---- the shared reading (review A #3 / B #4): a page started soon after a good read takes it instead of spawning another dumpsys
    @Test public void aGoodReadIsSharedWithinTheLimitOnly() {
        SsBatteryStats.forget();
        SsBatteryStats.Data d = SsBatteryStats.parse(DISCHARGING, UTC);
        assertTrue(d.any());
        assertNull(SsBatteryStats.recent(30_000, 1_000));            // nothing read yet
        SsBatteryStats.remember(d, 10_000);
        assertSame(d, SsBatteryStats.recent(30_000, 10_000));        // the same instant
        assertSame(d, SsBatteryStats.recent(30_000, 39_999));        // 29.999 s later: still shared
        assertNull(SsBatteryStats.recent(30_000, 40_000));           // exactly the limit: read again
        assertSame(d, SsBatteryStats.recent(60_000, 40_000));        // the home page's longer limit still takes it
        SsBatteryStats.forget();
    }

    @Test public void aFailedReadIsNeverShared() {
        SsBatteryStats.forget();
        SsBatteryStats.Data good = SsBatteryStats.parse(DISCHARGING, UTC);
        SsBatteryStats.remember(good, 5_000);
        SsBatteryStats.remember(SsBatteryStats.parse("nothing useful\n", UTC), 6_000);   // empty: must not replace the good one nor extend its life
        SsBatteryStats.remember(null, 7_000);
        assertSame(good, SsBatteryStats.recent(30_000, 6_000));
        assertNull(SsBatteryStats.recent(30_000, 35_000));           // the good one is still dated 5 s: expired at 35 s
        SsBatteryStats.forget();
    }

    @Test public void aTimeThatWentBackwardsIsNotShared() {
        SsBatteryStats.forget();
        SsBatteryStats.remember(SsBatteryStats.parse(DISCHARGING, UTC), 100_000);
        assertNull(SsBatteryStats.recent(30_000, 50_000));           // never negative ages
        SsBatteryStats.forget();
    }
}
