package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import org.junit.Test;

/**
 * BN1: the Storage page rules. Every expected value below was worked out by hand from the AOSP 10 sources (android.text.format.Formatter, Settings FileSizeFormatter, StorageItemPreference,
 * StorageAsyncLoader, StorageItemPreferenceController, FileUtils.roundStorageSize) and cross-checked with a separate float32 re-implementation in Python, never by running the class under test.
 * Exact binary ties (0.125 GB and the like) are pinned only where HALF_EVEN is the point: the JVM's and Android's String.format are not guaranteed to agree on them.
 */
public class SsStorageRulesTest {
    private static final Locale US = Locale.US;
    private static final long Q = SsStorageRules.DEFAULT_CACHE_QUOTA;

    // ------------------------------------------------------------------ order, labels, texts

    @Test public void rowsAreInTheStockOrderWithTheStockLabels() {
        String[] want = { "Photos & videos", "Music & audio", "Games", "Movie & TV apps", "Other apps", "Files", "System" };   // Tj's Storage screenshot, top to bottom
        SsStorageRules.Row[] rows = SsStorageRules.Row.values();
        assertEquals(want.length, rows.length);
        for (int i = 0; i < want.length; i++) assertEquals("row " + i, want[i], rows[i].label);
    }

    @Test public void fixedTexts() {
        assertEquals("Storage", SsStorageRules.TITLE);
        assertEquals("Calculating…", SsStorageRules.CALCULATING);
        assertEquals("used", SsStorageRules.DONUT_LABEL);
        assertEquals("95%, used", SsStorageRules.donutDescription("95%"));
        assertEquals("System includes files used to run Android version 10", SsStorageRules.systemInfo("10"));
    }

    @Test public void usedOfUsesTheShortFileSize() {
        assertEquals("Used of 32 GB", SsStorageRules.usedOf(32_000_000_000L, US));   // Tj's screenshot
        assertEquals("Used of 16 GB", SsStorageRules.usedOf(16_000_000_000L, US));
        assertEquals("Used of 64 GB", SsStorageRules.usedOf(64_000_000_000L, US));
        assertEquals("Used of 8.0 GB", SsStorageRules.usedOf(8_000_000_000L, US));   // shorter: one decimal below 10
        assertEquals("Used of 128 GB", SsStorageRules.usedOf(128_000_000_000L, US));
    }

    // ------------------------------------------------------------------ android.text.format.Formatter

    @Test public void fileSizeClimbsAUnitAbove900() {
        assertEquals("0 B", SsStorageRules.formatFileSize(0, US));
        assertEquals("1 B", SsStorageRules.formatFileSize(1, US));
        assertEquals("900 B", SsStorageRules.formatFileSize(900, US));      // 900 itself stays in bytes
        assertEquals("0.90 kB", SsStorageRules.formatFileSize(901, US));    // ... 901 does not
        assertEquals("1.00 kB", SsStorageRules.formatFileSize(999, US));    // 0.999 prints as 1.00 (the platform's own quirk)
        assertEquals("1.00 kB", SsStorageRules.formatFileSize(1000, US));
        assertEquals("1.50 kB", SsStorageRules.formatFileSize(1500, US));
        assertEquals("1.00 MB", SsStorageRules.formatFileSize(999_999, US));
        assertEquals("1.57 GB", SsStorageRules.formatFileSize(1_568_000_000L, US));   // "95% used - 1.57 GB free" on Tj's tablet
        assertEquals("30.28 GB", SsStorageRules.formatFileSize(30_280_000_000L, US));
        assertEquals("32.00 GB", SsStorageRules.formatFileSize(32_000_000_000L, US));
        assertEquals("128 GB", SsStorageRules.formatFileSize(128_000_000_000L, US));   // 100 and up: no decimals
        assertEquals("-1.50 kB", SsStorageRules.formatFileSize(-1500, US));
    }

    @Test public void shortFileSizeDropsDecimals() {
        assertEquals("1.0 kB", SsStorageRules.formatShortFileSize(1000, US));
        assertEquals("1.5 kB", SsStorageRules.formatShortFileSize(1500, US));
        assertEquals("1.6 GB", SsStorageRules.formatShortFileSize(1_568_000_000L, US));
        assertEquals("8.0 GB", SsStorageRules.formatShortFileSize(8_000_000_000L, US));
        assertEquals("30 GB", SsStorageRules.formatShortFileSize(30_280_000_000L, US));
        assertEquals("32 GB", SsStorageRules.formatShortFileSize(32_000_000_000L, US));   // the header's "Used of 32 GB"
        assertEquals("128 GB", SsStorageRules.formatShortFileSize(128_000_000_000L, US));
    }

    @Test public void bigNumberAndUnitAreSeparate() {
        SsStorageRules.Bytes b = SsStorageRules.formatBytes(30_280_000_000L, false, US);   // the donut header: "30.28" over "GB" (flags 0)
        assertEquals("30.28", b.value);
        assertEquals("GB", b.units);
        SsStorageRules.Bytes s = SsStorageRules.formatBytes(512, false, US);
        assertEquals("512", s.value);
        assertEquals("B", s.units);
    }

    @Test public void decimalSeparatorFollowsTheLocale() {
        assertEquals("1,57 GB", SsStorageRules.formatFileSize(1_568_000_000L, Locale.GERMANY));
    }

    // ------------------------------------------------------------------ Settings FileSizeFormatter (the rows)

    @Test public void rowSizesAreAlwaysGigabytesWithTheStockDecimals() {
        assertEquals("0.00 GB", SsStorageRules.formatRowSize(0, US));
        assertEquals("0.01 GB", SsStorageRules.formatRowSize(12_000_000L, US));
        assertEquals("0.54 GB", SsStorageRules.formatRowSize(540_000_000L, US));
        assertEquals("1.0 GB", SsStorageRules.formatRowSize(999_999_999L, US));   // float arithmetic: 999999999 is 1.0E9f, so it is not below 1
        assertEquals("3.0 GB", SsStorageRules.formatRowSize(3_000_000_000L, US));
        assertEquals("9.9 GB", SsStorageRules.formatRowSize(9_900_000_000L, US));
        assertEquals("10 GB", SsStorageRules.formatRowSize(10_000_000_000L, US));
        assertEquals("15 GB", SsStorageRules.formatRowSize(15_360_000_000L, US));
        assertEquals("", SsStorageRules.formatRowSize(SsStorageRules.UNKNOWN, US));
    }

    // ------------------------------------------------------------------ percentages

    @Test public void percentIsWholeNumbersHalfEven() {
        assertEquals("95%", SsStorageRules.percent(0.95, US));
        assertEquals("0%", SsStorageRules.percent(0.0, US));
        assertEquals("100%", SsStorageRules.percent(1.0, US));
        assertEquals("95%", SsStorageRules.percent(0.954, US));
        assertEquals("96%", SsStorageRules.percent(0.956, US));
        assertEquals("12%", SsStorageRules.percent(0.125, US));   // exact tie: HALF_EVEN goes to the even 12 (HALF_UP would say 13)
        assertEquals("62%", SsStorageRules.percent(0.625, US));
    }

    @Test public void usedAndItsFraction() {
        assertEquals(30_432_000_000L, SsStorageRules.used(32_000_000_000L, 1_568_000_000L));
        assertEquals(0L, SsStorageRules.used(10, 20));   // never negative
        assertEquals(0.951, SsStorageRules.usedFraction(30_432_000_000L, 32_000_000_000L), 1e-9);
        assertEquals(0.0, SsStorageRules.usedFraction(0, 32_000_000_000L), 0.0);
        assertTrue(Double.isNaN(SsStorageRules.usedFraction(5, 0)));
        assertTrue(Double.isNaN(SsStorageRules.usedFraction(5, -1)));
    }

    @Test public void homeSummaryMatchesTheStockLine() {
        assertEquals("95% used - 1.57 GB free", SsStorageRules.homeSummary(32_000_000_000L, 1_568_000_000L, US));   // Tj's Settings home
        assertEquals("0% used - 32.00 GB free", SsStorageRules.homeSummary(32_000_000_000L, 32_000_000_000L, US));
        assertEquals("", SsStorageRules.homeSummary(0, 1_000, US));         // total unknown
        assertEquals("", SsStorageRules.homeSummary(32_000_000_000L, -1, US));   // free unknown
    }

    // ------------------------------------------------------------------ the total

    @Test public void roundStorageSizeIsTheSmallestStepNotBelowTheSize() {
        assertEquals(1L, SsStorageRules.roundStorageSize(0));
        assertEquals(1L, SsStorageRules.roundStorageSize(1));
        assertEquals(2L, SsStorageRules.roundStorageSize(2));
        assertEquals(4L, SsStorageRules.roundStorageSize(3));
        assertEquals(8L, SsStorageRules.roundStorageSize(5));
        assertEquals(512L, SsStorageRules.roundStorageSize(512));
        assertEquals(1_000L, SsStorageRules.roundStorageSize(513));   // after 512 comes 1 x 1000
        assertEquals(2_000_000_000L, SsStorageRules.roundStorageSize(1_000_000_001L));
        assertEquals(32_000_000_000L, SsStorageRules.roundStorageSize(28_400_000_000L));
        assertEquals(32_000_000_000L, SsStorageRules.roundStorageSize(32_000_000_000L));
        assertEquals(64_000_000_000L, SsStorageRules.roundStorageSize(32_000_000_001L));
        assertEquals(512_000_000_000L, SsStorageRules.roundStorageSize(512_000_000_000L));
        assertEquals(1_000_000_000_000L, SsStorageRules.roundStorageSize(513_000_000_000L));
    }

    @Test public void marketingTotalRoundsDataPlusSystem() {
        assertEquals(32_000_000_000L, SsStorageRules.marketingTotal(26_000_000_000L, 3_000_000_000L));
        assertEquals(64_000_000_000L, SsStorageRules.marketingTotal(60_000_000_000L, 3_000_000_000L));
    }

    // ------------------------------------------------------------------ bars

    @Test public void progressIsTheTruncatedPercentOfTheTotal() {
        long total = 32_000_000_000L;
        assertEquals(10, SsStorageRules.progress(3_290_000_000L, total));
        assertEquals(1, SsStorageRules.progress(540_000_000L, total));
        assertEquals(0, SsStorageRules.progress(0, total));
        assertEquals(48, SsStorageRules.progress(15_360_000_000L, total));
        assertEquals(100, SsStorageRules.progress(total, total));
        assertEquals(0, SsStorageRules.progress(3_290_000_000L, 0));          // no total: empty bar
        assertEquals(0, SsStorageRules.progress(SsStorageRules.UNKNOWN, total));   // no size: empty bar
    }

    @Test public void barFillMatchesTheMeasuredStockBars() {
        int[] progress = { 10, 1, 9, 0, 11, 48, 13 };
        int[] fill = { 51, 6, 46, 0, 56, 243, 66 };   // pixels of the teal part in the 506 px wide bars of Tj's screenshot
        for (int i = 0; i < progress.length; i++) assertEquals("progress " + progress[i], fill[i], SsStorageRules.barFillPx(506, progress[i]));
        assertEquals(506, SsStorageRules.barFillPx(506, 100));
        assertEquals(253, SsStorageRules.barFillPx(506, 50));
        assertEquals(0, SsStorageRules.barFillPx(506, -5));      // out of range is clamped like ProgressBar does
        assertEquals(506, SsStorageRules.barFillPx(506, 250));
    }

    @Test public void barLevelIsComputedInFloatLikeProgressBar() {
        int[] progress = { 0, 1, 10, 39, 53, 59, 78, 100 };
        int[] level = { 0, 100, 1000, 3899, 5299, 5899, 7799, 10000 };   // (int) ((float) p / 100f * 10000): the four odd ones lose 1 to float rounding
        for (int i = 0; i < progress.length; i++) assertEquals("progress " + progress[i], level[i], SsStorageRules.barLevel(progress[i]));
    }

    /** The seven rows of Tj's screenshot from bytes to text, bar and pixels: if one of these moves, the page no longer looks like the stock one. */
    @Test public void stockScreenshotGolden() {
        long total = 32_000_000_000L;
        long[] size = { 3_290_000_000L, 540_000_000L, 3_000_000_000L, 0L, 3_700_000_000L, 15_360_000_000L, 4_300_000_000L };
        String[] text = { "3.3 GB", "0.54 GB", "3.0 GB", "0.00 GB", "3.7 GB", "15 GB", "4.3 GB" };
        int[] progress = { 10, 1, 9, 0, 11, 48, 13 };
        int[] fill = { 51, 6, 46, 0, 56, 243, 66 };
        for (int i = 0; i < size.length; i++) {
            String row = SsStorageRules.Row.values()[i].label;
            assertEquals(row + " text", text[i], SsStorageRules.formatRowSize(size[i], US));
            assertEquals(row + " progress", progress[i], SsStorageRules.progress(size[i], total));
            assertEquals(row + " fill", fill[i], SsStorageRules.barFillPx(506, progress[i]));
        }
    }

    // ------------------------------------------------------------------ the donut

    @Test public void percentSignRange() {
        assertArrayEquals(new int[] { 2, 3 }, SsStorageRules.percentSignRange("95%", "%"));
        assertArrayEquals(new int[] { 3, 4 }, SsStorageRules.percentSignRange("95 %", "%"));   // a no-break space before the sign (German)
        assertArrayEquals(new int[] { 0, 1 }, SsStorageRules.percentSignRange("%95", "%"));          // the sign first (Turkish)
        assertArrayEquals(new int[] { 0, 2 }, SsStorageRules.percentSignRange("95", "%"));           // no sign found: the whole text is the small part
    }

    // ------------------------------------------------------------------ what the apps are blamed for

    @Test public void appsAreBlamedForDataAndCodeOnce() {
        SsStorageRules.Tally t = new SsStorageRules.Tally();
        t.addApp("a.b", SsStorageRules.CAT_UNDEFINED, false, 1000, 0, Q, 500);
        assertEquals(1500, t.other);
        t.addApp("a.b", SsStorageRules.CAT_UNDEFINED, false, 200, 0, Q, 500);   // the same package again (another user): its code is already counted
        assertEquals(1700, t.other);
        assertEquals(2, t.count);
    }

    @Test public void cacheAboveTheQuotaCountsAsFree() {
        SsStorageRules.Tally t = new SsStorageRules.Tally();
        t.addApp("over", SsStorageRules.CAT_UNDEFINED, false, 10_000, 9_000, 4_000, 0);
        assertEquals(5_000, t.other);                      // 10000 - 9000 + 4000
        t.addApp("equal", SsStorageRules.CAT_UNDEFINED, false, 10_000, 4_000, 4_000, 0);
        assertEquals(15_000, t.other);                     // at the quota nothing is taken off
        t.addApp("under", SsStorageRules.CAT_UNDEFINED, false, 10_000, 1_000, 4_000, 0);
        assertEquals(25_000, t.other);
        t.addApp("noquota", SsStorageRules.CAT_UNDEFINED, false, 10_000, 100, 0, 0);
        assertEquals(34_900, t.other);                     // 10000 - 100 + 0
    }

    @Test public void appsAreSortedByCategoryThenByTheGameFlag() {
        SsStorageRules.Tally t = new SsStorageRules.Tally();
        t.addApp("game", SsStorageRules.CAT_GAME, false, 1, 0, Q, 0);
        t.addApp("audio", SsStorageRules.CAT_AUDIO, false, 20, 0, Q, 0);
        t.addApp("video", SsStorageRules.CAT_VIDEO, false, 300, 0, Q, 0);
        t.addApp("image", SsStorageRules.CAT_IMAGE, false, 4_000, 0, Q, 0);
        t.addApp("oldgame", SsStorageRules.CAT_UNDEFINED, true, 50_000, 0, Q, 0);    // the deprecated FLAG_IS_GAME: a game
        t.addApp("tool", SsStorageRules.CAT_UNDEFINED, false, 600_000, 0, Q, 0);
        t.addApp("flaggedaudio", SsStorageRules.CAT_AUDIO, true, 7_000_000, 0, Q, 0);   // a category wins over the flag
        assertEquals(50_001, t.games);
        assertEquals(7_000_020, t.music);
        assertEquals(300, t.video);
        assertEquals(4_000, t.photos);
        assertEquals(600_000, t.other);
        assertEquals(7_654_321, t.attributed());
        assertEquals(7, t.count);
    }

    // ------------------------------------------------------------------ the seven sizes

    private static SsStorageRules.Tally sampleApps() {
        SsStorageRules.Tally t = new SsStorageRules.Tally();
        t.addApp("g", SsStorageRules.CAT_GAME, false, 2_000_000_000L, 0, Q, 0);
        t.addApp("m", SsStorageRules.CAT_AUDIO, false, 100, 0, Q, 0);
        t.addApp("v", SsStorageRules.CAT_VIDEO, false, 300, 0, Q, 0);
        t.addApp("p", SsStorageRules.CAT_IMAGE, false, 400, 0, Q, 0);
        t.addApp("o", SsStorageRules.CAT_UNDEFINED, false, 5_000_000_000L, 0, Q, 0);
        return t;
    }

    private static SsStorageRules.External sampleExternal() {
        return new SsStorageRules.External(20_000_000_000L, 1_000_000_000L, 2_000_000_000L, 3_000_000_000L, 4_000_000_000L);
    }

    @Test public void rowSizesFollowStorageItemPreferenceController() {
        long[] r = SsStorageRules.rowSizes(sampleApps(), sampleExternal(), 30_000_000_000L);
        // Photos & videos = photo apps 400 + images 3e9 + videos 2e9
        assertEquals(5_000_000_400L, r[SsStorageRules.Row.PHOTOS.ordinal()]);
        // Music & audio = music apps 100 + audio 1e9
        assertEquals(1_000_000_100L, r[SsStorageRules.Row.MUSIC.ordinal()]);
        assertEquals(2_000_000_000L, r[SsStorageRules.Row.GAMES.ordinal()]);
        assertEquals(300L, r[SsStorageRules.Row.MOVIES.ordinal()]);
        assertEquals(5_000_000_000L, r[SsStorageRules.Row.OTHER_APPS.ordinal()]);
        // Files = total 20e9 - audio 1e9 - video 2e9 - image 3e9 - app 4e9
        assertEquals(10_000_000_000L, r[SsStorageRules.Row.FILES.ordinal()]);
        // System = used 30e9 - (apps 7_000_000_800 + shared storage without its app part 16e9)
        assertEquals(6_999_999_200L, r[SsStorageRules.Row.SYSTEM.ordinal()]);
    }

    @Test public void systemNeverFallsBelowOneGibibyte() {
        long[] r = SsStorageRules.rowSizes(sampleApps(), sampleExternal(), 10_000_000_000L);   // less used than attributed
        assertEquals(1_073_741_824L, r[SsStorageRules.Row.SYSTEM.ordinal()]);
        assertEquals(1_073_741_824L, SsStorageRules.SYSTEM_FLOOR);
        assertEquals(67_108_864L, SsStorageRules.DEFAULT_CACHE_QUOTA);
    }

    @Test public void rowsThatNeedAMissingPartStayUnknown() {
        int files = SsStorageRules.Row.FILES.ordinal();
        int system = SsStorageRules.Row.SYSTEM.ordinal();
        int photos = SsStorageRules.Row.PHOTOS.ordinal();
        int games = SsStorageRules.Row.GAMES.ordinal();
        // no app numbers: only the shared-storage row is known
        long[] noApps = SsStorageRules.rowSizes(null, sampleExternal(), 30_000_000_000L);
        for (int i = 0; i < noApps.length; i++) {
            if (i == files) assertEquals(10_000_000_000L, noApps[i]); else assertFalse("row " + i, SsStorageRules.isKnown(noApps[i]));
        }
        // no shared-storage numbers: only the three plain app rows are known
        long[] noExt = SsStorageRules.rowSizes(sampleApps(), null, 30_000_000_000L);
        assertEquals(2_000_000_000L, noExt[games]);
        assertEquals(300L, noExt[SsStorageRules.Row.MOVIES.ordinal()]);
        assertEquals(5_000_000_000L, noExt[SsStorageRules.Row.OTHER_APPS.ordinal()]);
        assertFalse(SsStorageRules.isKnown(noExt[photos]));
        assertFalse(SsStorageRules.isKnown(noExt[SsStorageRules.Row.MUSIC.ordinal()]));
        assertFalse(SsStorageRules.isKnown(noExt[files]));
        assertFalse(SsStorageRules.isKnown(noExt[system]));
        // neither
        long[] none = SsStorageRules.rowSizes(null, null, 30_000_000_000L);
        for (int i = 0; i < none.length; i++) assertFalse("row " + i, SsStorageRules.isKnown(none[i]));
        // no used bytes (the volume could not be read): System stays blank, the rest is real
        long[] noUsed = SsStorageRules.rowSizes(sampleApps(), sampleExternal(), SsStorageRules.UNKNOWN);
        assertFalse(SsStorageRules.isKnown(noUsed[system]));
        assertEquals(5_000_000_400L, noUsed[photos]);
        assertEquals(10_000_000_000L, noUsed[files]);
    }

    @Test public void unknownIsNotAnyRealSize() {
        assertFalse(SsStorageRules.isKnown(SsStorageRules.UNKNOWN));
        assertTrue(SsStorageRules.isKnown(0));
        assertTrue(SsStorageRules.isKnown(-1));
    }
}
