package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** AC1/AC7: the persistent lock-screen decision log. The Android side only appends to a file; every rule worth testing is pure. */
public class LockLogTest {
    @Test public void aLineIsOneLineWithTimeEventAndDetail() {
        String l = LockLog.line(0, "UNLOCK", "plain, mode quickstep -> custom");
        assertTrue(l.endsWith("\n"));
        assertEquals(1, l.split("\n").length);
        assertTrue(l.contains("UNLOCK plain, mode quickstep -> custom"));
        assertTrue(l.matches("\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d .*\n"));
    }
    @Test public void newlinesInDetailsCannotSplitALine() {
        String l = LockLog.line(0, "X", "a\nb\rc");
        assertEquals(1, l.split("\n").length);
    }
    @Test public void aLongDetailIsCut() { assertTrue(LockLog.line(0, "X", new String(new char[500]).replace('\0', 'a')).length() <= 201); }
    @Test public void aMissingEventAndDetailStillGiveALine() { assertTrue(LockLog.line(0, null, null).contains(" ?")); }

    @Test public void trimKeepsTheNewestWholeLines() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 100; i++) b.append("line").append(i).append("-xxxxxxxxxx\n");
        String t = LockLog.trim(b.toString(), 500, 300);
        assertTrue(t.length() <= 300);
        assertTrue(t.startsWith("line"));                 // starts at a line boundary
        assertTrue(t.endsWith("line99-xxxxxxxxxx\n"));    // the newest line survives
        assertFalse(t.contains("line0-"));                // the oldest is gone
    }
    @Test public void trimLeavesAShortLogAlone() { assertEquals("a\nb\n", LockLog.trim("a\nb\n", 500, 300)); assertEquals("", LockLog.trim(null, 500, 300)); }

    // ---- BN8e: the report's excerpt folds runs of DIRTY lines (Tj's report of 2026-11-10 had 40 of them and nothing else)
    private static String ev(String time, String event, String detail) { return time + " " + event + " " + detail + "\n"; }

    @Test public void aRunOfDirtyLinesBecomesOneLineAndTheRestIsUntouched() {
        String all = ev("19:24:31.000", "LOCK", "screen off")
                + ev("19:27:42.167", "DIRTY", "com.a was marked as opened in Quickstep (window x)")
                + ev("19:27:42.945", "DIRTY", "com.a was marked as opened in Quickstep (window y)")
                + ev("19:27:44.173", "DIRTY", "com.b was marked as opened in Quickstep (window x)")
                + ev("19:27:45.000", "DIRTY", "com.a was marked as opened in Quickstep (window x)")
                + ev("19:32:49.842", "ROUTER", "home press, mode quickstep");
        String[] d = LockLog.digest(all, 50).split("\n");
        assertEquals(3, d.length);
        assertTrue(d[0].contains("LOCK screen off"));
        assertEquals("19:27:42.167 DIRTY 4 lines (com.a x3, com.b x1; newest 19:27:45.000)", d[1]);
        assertTrue(d[2].contains("ROUTER home press"));
    }
    @Test public void aSingleDirtyLineAndTwoSeparateRunsStayApart() {
        String all = ev("10:00:00.000", "DIRTY", "com.a was marked as opened in Quickstep (window x)")
                + ev("10:00:01.000", "UNLOCK", "plain")
                + ev("10:00:02.000", "DIRTY", "com.a was marked as opened in Quickstep (window x)")
                + ev("10:00:03.000", "DIRTY", "com.a was marked as opened in Quickstep (window x)");
        String[] d = LockLog.digest(all, 50).split("\n");
        assertEquals(3, d.length);
        assertTrue(d[0].contains("DIRTY com.a was marked"));       // a run of one is the original line
        assertTrue(d[1].contains("UNLOCK plain"));
        assertTrue(d[2].contains("DIRTY 2 lines (com.a x2; newest 10:00:03.000)"));
    }
    @Test public void theDigestKeepsTheNewestLinesAndNeverOverflowsALine() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 10; i++) b.append(ev("10:00:0" + i + ".000", "EV", "n" + i));
        assertEquals(ev("10:00:08.000", "EV", "n8") + ev("10:00:09.000", "EV", "n9"), LockLog.digest(b.toString(), 2));
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 60; i++) many.append(ev("10:01:00.000", "DIRTY", "com.example.application.number" + i + " was marked as opened in Quickstep (window x)"));
        String folded = LockLog.digest(many.toString(), 5);
        assertEquals(1, folded.split("\n").length);
        assertTrue(folded.length() <= 201);
        assertEquals("", LockLog.digest(null, 5));
        assertEquals("", LockLog.digest("", 5));
        assertEquals("", LockLog.digest("10:00:00.000 EV x\n", 0));
    }

    @Test public void tailGivesTheLastLinesInOrder() {
        String all = "1\n2\n3\n4\n5\n";
        assertEquals("4\n5\n", LockLog.tailLines(all, 2));
        assertEquals("1\n2\n3\n4\n5\n", LockLog.tailLines(all, 9));
        assertEquals("5\n", LockLog.tailLines(all, 1));
        assertEquals("", LockLog.tailLines("", 3));
        assertEquals("", LockLog.tailLines(all, 0));
        assertEquals("only\n", LockLog.tailLines("only", 3));
    }
    @Test public void theSizesAreSane() { assertTrue(LockLog.KEEP_BYTES < LockLog.MAX_BYTES); assertTrue(LockLog.MAX_BYTES <= 64 * 1024); }
}
