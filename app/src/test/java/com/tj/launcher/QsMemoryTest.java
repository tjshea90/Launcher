package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CG1f: the recorder of the stock launcher's memory (parser, ring, reading). */
public class QsMemoryTest {
    /** The layout of Android 10's {@code dumpsys meminfo <package>} (shortened to the lines the parser reads, in their order). */
    static final String DUMP = ""
            + "Applications Memory Usage (in Kilobytes):\n"
            + "Uptime: 123456 Realtime: 234567\n"
            + "\n"
            + "** MEMINFO in pid 2345 [com.android.launcher3] **\n"
            + "                   Pss  Private  Private  SwapPss     Heap     Heap     Heap\n"
            + "                 Total    Dirty    Clean    Dirty     Size    Alloc     Free\n"
            + "                ------   ------   ------   ------   ------   ------   ------\n"
            + "  Native Heap    21000    20900        0     1200    32000    24000     6000\n"
            + "  Dalvik Heap    14000    13900        0      800    22000    14500     7000\n"
            + "        Stack     1400     1400        0        0\n"
            + "      Gfx dev    52000    52000        0        0\n"
            + "    Other dev       12        0       12        0\n"
            + "   EGL mtrack    30000    30000        0        0\n"
            + "    GL mtrack    41000    41000        0        0\n"
            + "      Unknown     2400     2400        0       10\n"
            + "        TOTAL   310000   300000      200     3000    54000    38500    13000\n"
            + "\n"
            + " App Summary\n"
            + "                       Pss(KB)\n"
            + "                        ------\n"
            + "           Java Heap:    14100\n"
            + "         Native Heap:    21000\n"
            + "                Code:    22000\n"
            + "               Stack:     1400\n"
            + "            Graphics:   123000\n"
            + "       Private Other:     9000\n"
            + "              System:     8000\n"
            + "\n"
            + "               TOTAL:   310000       TOTAL SWAP PSS:     3000\n"
            + "\n"
            + " Objects\n"
            + "               Views:      612         ViewRootImpl:        2\n"
            + "         AppContexts:        9           Activities:        4\n"
            + "              Assets:       11        AssetManagers:        0\n"
            + "       Local Binders:       40        Proxy Binders:       37\n"
            + "       Parcel memory:       12         Parcel count:       48\n"
            + "    Death Recipients:        1      OpenSSL Sockets:        0\n"
            + "            WebViews:        0\n";

    @Test public void aRealLayoutIsReadPartByPart() {
        QsMemory.Sample s = QsMemory.parse(DUMP, 1_700_000_000_000L);
        assertTrue(s.found);
        assertEquals(2345, s.pid);
        assertEquals(310000, s.pssKb);
        assertEquals(14100, s.javaKb);
        assertEquals(21000, s.nativeKb);
        assertEquals(22000, s.codeKb);
        assertEquals(1400, s.stackKb);
        assertEquals(123000, s.graphicsKb);
        assertEquals(9000, s.privOtherKb);
        assertEquals(8000, s.systemKb);
        assertEquals(30000, s.eglKb);
        assertEquals(41000, s.glKb);
        assertEquals(52000, s.gfxDevKb);
        assertEquals(2400, s.unknownKb);
        assertEquals(612, s.views);
        assertEquals(2, s.viewRoots);
        assertEquals(4, s.activities);
        assertEquals(9, s.appContexts);
        assertEquals(11, s.assets);
        assertEquals(40, s.binders);
    }

    @Test public void theTotalRowOfTheTableIsNotTakenForTheSummaryTotal() {
        // the table row "TOTAL   310000 ..." has no colon: only the App Summary line counts
        assertEquals(310000, QsMemory.parse(DUMP, 1).pssKb);
    }

    @Test public void aDumpOfNoRunningProcessIsNotAReading() {
        assertFalse(QsMemory.parse("No process found for: com.android.launcher3\n", 1).found);
        assertFalse(QsMemory.parse("", 1).found);
        assertFalse(QsMemory.parse(null, 1).found);
        assertFalse("a header without the total is not a reading", QsMemory.parse("** MEMINFO in pid 5 [x] **\n Objects\n Views: 3  ViewRootImpl: 1\n", 1).found);
    }

    @Test public void aLineRoundTrips() {
        QsMemory.Sample s = QsMemory.parse(DUMP, 1_700_000_123_456L);
        String line = QsMemory.toLine(s);
        assertTrue(line, line.startsWith("t=1700000123;pid=2345;pss=310000;"));
        QsMemory.Sample r = QsMemory.fromLine(line);
        assertTrue(r.found);
        assertEquals(1_700_000_123_000L, r.atMs);
        assertEquals(s.pssKb, r.pssKb);
        assertEquals(s.activities, r.activities);
        assertEquals(s.views, r.views);
        assertEquals(s.graphicsKb, r.graphicsKb);
        assertEquals(s.eglKb, r.eglKb);
    }

    @Test public void aMissingPartIsLeftOutOfTheLineAndComesBackAsMinusOne() {
        QsMemory.Sample s = new QsMemory.Sample();
        s.found = true; s.atMs = 5000; s.pid = 7; s.pssKb = 100;
        String line = QsMemory.toLine(s);
        assertEquals("t=5;pid=7;pss=100", line);
        assertEquals(-1, QsMemory.fromLine(line).activities);
        assertFalse(QsMemory.fromLine("garbage").found);
        assertFalse(QsMemory.fromLine(null).found);
        assertFalse(QsMemory.fromLine("t=5;pss=abc").found);
    }

    @Test public void theRingKeepsTheNewestAndDropsTheOldest() {
        String ring = "";
        for (int i = 1; i <= 5; i++) ring = QsMemory.addLine(ring, "t=" + i + ";pss=" + i, 3);
        assertEquals("t=3;pss=3\nt=4;pss=4\nt=5;pss=5", ring);
        assertEquals("t=9;pss=9", QsMemory.addLine("", "t=9;pss=9", 3));
        assertEquals("t=2;pss=2", QsMemory.addLine("t=1;pss=1", "t=2;pss=2", 1));
    }

    @Test public void aSampleIsDueAfterTheGapAndWhenTheClockWentBackwards() {
        long now = 10_000_000L;
        assertTrue(QsMemory.due(0, now, false));
        assertFalse(QsMemory.due(now - QsMemory.GAP_MS + 1, now, false));
        assertTrue(QsMemory.due(now - QsMemory.GAP_MS, now, false));
        assertTrue(QsMemory.due(now + 5, now, false));                                   // the clock went backwards
        assertFalse(QsMemory.due(now - QsMemory.FORCED_GAP_MS + 1, now, true));          // opening Control twice in a row: one dumpsys
        assertTrue(QsMemory.due(now - QsMemory.FORCED_GAP_MS, now, true));
    }

    private static final long BASE_S = 1_700_000_000L;
    private static long at(long tMin) { return (BASE_S + tMin * 60) * 1000L; }

    private static String line(long tMin, int pid, int pss, int java, int nat, int gfx, int act, int views) {
        return "t=" + (BASE_S + tMin * 60) + ";pid=" + pid + ";pss=" + pss + ";java=" + java + ";nat=" + nat + ";gfx=" + gfx + ";act=" + act + ";views=" + views;
    }

    @Test public void noSampleYetSaysHowItStarts() {
        assertTrue(QsMemory.describe("", 0).startsWith("no sample yet"));
        assertTrue(QsMemory.describe(null, 0).startsWith("no sample yet"));
    }

    @Test public void aGrowthWithMoreActivitiesPointsAtThePinnedActivities() {
        String ring = line(0, 10, 100_000, 10_000, 20_000, 30_000, 1, 200) + "\n" + line(600, 10, 180_000, 20_000, 30_000, 60_000, 6, 900);
        String d = QsMemory.describe(ring, at(600));
        assertTrue(d, d.contains("2 samples"));
        assertTrue(d, d.contains("PSS +80000 kB"));
        assertTrue(d, d.contains("Activities +5, Views +700"));
        assertTrue(d, d.contains("R1"));
    }

    @Test public void aGrowthInGraphicsWithFlatObjectsPointsAtGraphicsMemory() {
        QsMemory.Sample a = QsMemory.fromLine(line(0, 10, 100_000, 10_000, 20_000, 30_000, 2, 300));
        QsMemory.Sample b = QsMemory.fromLine(line(600, 10, 180_000, 11_000, 21_000, 100_000, 2, 310));
        assertTrue(QsMemory.reading(a, b), QsMemory.reading(a, b).contains("R2"));
    }

    @Test public void aGrowthInTheHeapWithFlatObjectsPointsAtTheHeap() {
        QsMemory.Sample a = QsMemory.fromLine(line(0, 10, 100_000, 10_000, 20_000, 30_000, 2, 300));
        QsMemory.Sample b = QsMemory.fromLine(line(600, 10, 160_000, 40_000, 40_000, 31_000, 2, 305));
        assertTrue(QsMemory.reading(a, b), QsMemory.reading(a, b).contains("R3"));
    }

    @Test public void aSmallChangeIsNotCalledGrowth() {
        QsMemory.Sample a = QsMemory.fromLine(line(0, 10, 100_000, 10_000, 20_000, 30_000, 2, 300));
        QsMemory.Sample b = QsMemory.fromLine(line(600, 10, 102_000, 10_500, 20_500, 30_500, 2, 300));
        assertTrue(QsMemory.reading(a, b).startsWith("no real growth"));
    }

    @Test public void aNewProcessStartsTheCountAgain() {
        String ring = line(0, 10, 400_000, 1, 1, 1, 9, 2000) + "\n" + line(300, 11, 60_000, 1, 1, 1, 1, 200) + "\n" + line(360, 11, 62_000, 1, 1, 1, 1, 210);
        String d = QsMemory.describe(ring, at(400));
        assertTrue(d, d.contains("3 samples"));
        assertTrue(d, d.contains("stock launcher process 11"));
        assertTrue(d, d.contains("PSS +2000 kB"));            // measured from the first sample of pid 11, not of pid 10
        assertFalse(d, d.contains("R1"));
        String only = QsMemory.describe(line(0, 10, 400_000, 1, 1, 1, 9, 2000) + "\n" + line(300, 11, 60_000, 1, 1, 1, 1, 200), at(400));
        assertTrue(only, only.contains("restarted"));
    }

    @Test public void thePerHourRateNeedsAtLeastHalfAnHour() {
        String shortRing = line(0, 10, 100_000, 1, 1, 1, 1, 1) + "\n" + line(20, 10, 110_000, 1, 1, 1, 1, 1);
        assertFalse(QsMemory.describe(shortRing, at(30)).contains("per hour"));
        String longRing = line(0, 10, 100_000, 1, 1, 1, 1, 1) + "\n" + line(120, 10, 110_000, 1, 1, 1, 1, 1);
        assertTrue(QsMemory.describe(longRing, at(130)).contains("+5000 kB per hour"));
    }

    @Test public void theSwitchIsOnInTheShippedBuildAndOffInDebug() {
        assertTrue(QsMemory.defaultOn(false));
        assertFalse(QsMemory.defaultOn(true));
    }

    @Test public void theProcessListFallbackReadsTheStockLaunchersLastPss() {
        String dump = "  *APP* UID 10001 ProcessRecord{1a2b3c4 1234:com.other/u0a1}\n    lastPss=12MB lastSwapPss=0\n"
                + "  *APP* UID 10010 ProcessRecord{9f8e7d6 2515:com.android.launcher3/u0a10}\n    pssProcState=3 lastPssTime=-5m lastPss=48MB lastSwapPss=0 lastCachedPss=0\n";
        QsMemory.Sample s = QsMemory.parseProcesses(dump, 5000);
        assertTrue(s.found);
        assertEquals(2515, s.pid);
        assertEquals(48 * 1024, s.pssKb);
        assertEquals(-1, s.activities);
    }

    @Test public void theFallbackIsNotAReadingWhenTheStockLauncherIsMissingOrHasNoPss() {
        assertFalse(QsMemory.parseProcesses("ProcessRecord{1a2 1234:com.other/u0a1}\n lastPss=9MB", 1).found);
        assertFalse(QsMemory.parseProcesses("ProcessRecord{9f8 2515:com.android.launcher3/u0a10}\n nothing here", 1).found);
        assertFalse(QsMemory.parseProcesses(null, 1).found);
    }

    // ---- CG1f (v0.9.12): the helper route and the failure backoff ----

    @Test public void aLookThatGaveNothingWaitsTwoHoursBeforeTheNextTry() {
        long now = 50_000_000L;
        String none = "no reading: the shell helper is not running ...";
        assertTrue("a good or unknown last look: the normal gap decides", QsMemory.retryDue("ok", now - 1, now, false));
        assertTrue(QsMemory.retryDue("", now - 1, now, false));
        assertTrue(QsMemory.retryDue(null, now - 1, now, false));
        assertFalse("a failed look 10 min ago is not repeated on the next screen-on", QsMemory.retryDue(none, now - 10 * 60_000L, now, false));
        assertFalse(QsMemory.retryDue(none, now - QsMemory.FAIL_GAP_MS + 1, now, false));
        assertTrue(QsMemory.retryDue(none, now - QsMemory.FAIL_GAP_MS, now, false));
        assertTrue("never tried: try", QsMemory.retryDue(none, 0, now, false));
        assertTrue("the clock went backwards: try", QsMemory.retryDue(none, now + 5, now, false));
        assertFalse("opening Launcher control twice in a row is still one try", QsMemory.retryDue(none, now - QsMemory.FORCED_GAP_MS + 1, now, true));
        assertTrue("... but Launcher control may retry after the short gap", QsMemory.retryDue(none, now - QsMemory.FORCED_GAP_MS, now, true));
    }

    @Test public void theStatusSaysWhatHappenedAndWhatWouldHelp() {
        assertEquals("ok via the shell helper", QsMemory.statusText(true, "the shell helper", true, true, "", -1));
        assertEquals("ok", QsMemory.statusText(true, "meminfo", false, true, "", -1));
        assertEquals("ok via activity processes (PSS only)", QsMemory.statusText(true, "activity processes (PSS only)", false, true, "", 900));
        String noHelper = QsMemory.statusText(false, "meminfo", false, true, "Can't find service: meminfo", 32);
        assertTrue(noHelper, noHelper.startsWith("no reading: the shell helper is not running"));
        assertTrue(noHelper, noHelper.contains("Can't find service: meminfo") && noHelper.contains("32 chars") && noHelper.contains("start the helper"));
        String noGrant = QsMemory.statusText(false, "meminfo", false, false, "", -1);
        assertTrue(noGrant, noGrant.contains("no DUMP grant") && noGrant.startsWith("no reading"));
        String empty = QsMemory.statusText(false, "the shell helper", true, true, "No process found", -1);
        assertTrue(empty, empty.startsWith("no reading: the shell helper answered") && empty.contains("No process found"));
        assertTrue("every failure text starts with 'no reading' (retryDue keys on it)", noHelper.startsWith("no reading") && noGrant.startsWith("no reading") && empty.startsWith("no reading"));
    }

    @Test public void theHelperCommandIsAConstantAndAcceptsNothingElse() {
        assertTrue(ShellProtocol.isStockMem("STOCKMEM"));
        assertTrue(ShellProtocol.isStockMem(" STOCKMEM \n"));
        assertFalse(ShellProtocol.isStockMem("STOCKMEM com.android.systemui"));
        assertFalse(ShellProtocol.isStockMem("stockmem"));
        assertFalse(ShellProtocol.isStockMem("STOCKMEM; id"));
        assertFalse(ShellProtocol.isStockMem(null));
        assertEquals("com.android.launcher3", ShellProtocol.STOCK_PKG);
    }
}
