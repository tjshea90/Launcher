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

    // ---- BN1p D1: where a page starts from, and what it keeps after a read
    @Test public void aPageStartsFromTheNewestGoodReadingUntilItsEstimateRunsOut() {
        SsBatteryStats.forget();
        SsBatteryStats.Data blank = SsBatteryStats.seed(0);
        assertFalse(blank.any());
        assertFalse(blank.finished());                               // no read yet: the page must not show a status word
        SsBatteryStats.Data good = SsBatteryStats.parse(DISCHARGING, UTC);   // Battery time remaining: 5 h 3 min
        assertTrue(good.dischargeMs > 0);
        good.readAtElapsed = 10_000;
        SsBatteryStats.remember(good, 10_000);
        assertNull(SsBatteryStats.recent(30_000, 10_000_000));       // far too old to skip a new read ...
        assertSame(good, SsBatteryStats.seed(10_000_000));           // ... but still the right first frame (2.8 h on, the 5 h estimate has not run out)
        assertFalse(SsBatteryStats.seed(10_000 + good.dischargeMs + 1).any());   // run out: a blank start, the refresh a second later has the real one
        assertFalse(SsBatteryStats.seed(10_000 + good.dischargeMs + 1).finished());
        SsBatteryStats.forget();
        assertFalse(SsBatteryStats.seed(0).any());
    }

    /** BN1p D1: a seeded reading may be hours old; "now + its figure" would put the end time that much too late. The figure counts down from the read, the END time stays where Android put it. */
    @Test public void anEstimateCountsDownFromTheTimeOfTheRead() {
        SsBatteryStats.Data d = new SsBatteryStats.Data();
        d.dischargeMs = 3_600_000;
        assertEquals(3_600_000, d.drainAt(5_000));                   // read time unknown (a parse in a test): as read
        assertEquals(-1, d.chargeAt(5_000));                         // unknown stays unknown
        d.readAtElapsed = 1_000;
        assertEquals(3_600_000, d.drainAt(1_000));
        assertEquals(3_000_000, d.drainAt(601_000));                 // ten minutes later
        assertEquals(3_600_000, d.drainAt(500));                     // a clock that went backwards: no negative age
        assertEquals(1, d.drainAt(3_600_999));
        assertEquals(-1, d.drainAt(3_601_000));                      // used up
        assertFalse(d.expired(601_000));
        assertTrue(d.expired(3_601_000));
        d.chargeMs = 7_200_000;
        assertEquals(6_600_000, d.chargeAt(601_000));
        d.dischargeMs = -1;
        assertFalse(d.expired(3_601_000));                           // a charge estimate left that has not run out
        assertTrue(d.expired(7_201_000));
        d.chargeMs = -1;
        assertFalse(d.expired(99_999_999));                          // no estimate was ever there: nothing can run out
        d.dischargeMs = 0;
        assertFalse(d.expired(99_999_999));                          // 0 = no estimate either (the header treats it so)
        assertEquals(-1, d.drainAt(1_000));
    }

    /** Review finding 1: a seed from a charging session has no discharge figure; shown unplugged, the header would fall back to the status word in the first frame. */
    @Test public void aSeedIsUsableOnlyForThePlugStateItHasAnEstimateFor() {
        SsBatteryStats.Data d = new SsBatteryStats.Data();
        d.dischargeMs = 3_600_000; d.readAtElapsed = 1_000;
        assertTrue(d.usableFor(false, 1_000));
        assertFalse(d.usableFor(true, 1_000));            // charging now: this reading has no charge time
        d.chargeMs = 600_000;
        assertTrue(d.usableFor(true, 1_000));
        assertFalse(d.usableFor(true, 601_000));          // the charge estimate has run out
        assertTrue(d.usableFor(false, 601_000));          // the discharge one has not
        assertFalse(new SsBatteryStats.Data().usableFor(false, 0));   // a blank seed is never usable
        assertFalse(new SsBatteryStats.Data().usableFor(true, 0));
    }

    @Test public void aFinishedReadIsKnownEvenWhenItFoundNothing() {
        assertFalse(new SsBatteryStats.Data().finished());
        assertTrue(SsBatteryStats.parse(DISCHARGING, UTC).finished());
        SsBatteryStats.Data empty = SsBatteryStats.parse("nothing useful\n", UTC);
        assertFalse(empty.any());
        assertTrue(empty.finished());                                // "empty": the status word may show now
    }

    @Test public void aPageKeepsNumbersOverAnEmptyRead() {
        SsBatteryStats.Data good = SsBatteryStats.parse(DISCHARGING, UTC);
        SsBatteryStats.Data empty = SsBatteryStats.parse("nothing useful\n", UTC);
        SsBatteryStats.Data blank = new SsBatteryStats.Data();
        assertSame(good, SsBatteryStats.keep(blank, good));          // the first good read replaces the blank start
        assertSame(good, SsBatteryStats.keep(good, empty));          // a failed refresh must not wipe what the page shows
        assertSame(empty, SsBatteryStats.keep(blank, empty));        // nothing to keep: the finished, empty read lets the status word show
        assertSame(empty, SsBatteryStats.keep(null, empty));
        SsBatteryStats.Data newer = SsBatteryStats.parse(DISCHARGING, UTC);
        assertSame(newer, SsBatteryStats.keep(good, newer));         // a newer good read wins
    }

    @Test public void aTimeThatWentBackwardsIsNotShared() {
        SsBatteryStats.forget();
        SsBatteryStats.remember(SsBatteryStats.parse(DISCHARGING, UTC), 100_000);
        assertNull(SsBatteryStats.recent(30_000, 50_000));           // never negative ages
        SsBatteryStats.forget();
    }
}
