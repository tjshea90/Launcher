package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CG1a: the six newest mode changes kept for the tablet report (Mode.FLIPS_KEY). */
public class ModeFlipsTest {
    @Test public void theFirstEntryStandsAlone() { assertEquals("a", Mode.pushFlip("", "a", 6)); assertEquals("a", Mode.pushFlip(null, "a", 6)); }

    @Test public void theNewestComesFirstAndOldOnesFollow() { assertEquals("c ; b ; a", Mode.pushFlip("b ; a", "c", 6)); }

    @Test public void onlyTheNewestSixAreKept() {
        String s = "";
        for (int i = 1; i <= 9; i++) s = Mode.pushFlip(s, "e" + i, 6);
        assertEquals("e9 ; e8 ; e7 ; e6 ; e5 ; e4", s);
        assertEquals(6, s.split(" ; ").length);
    }

    @Test public void aListShorterThanTheLimitKeepsEverything() { assertEquals("c ; b ; a", Mode.pushFlip(Mode.pushFlip("a", "b", 6), "c", 6)); }

    @Test public void theKeyIsOnTheReportAllowList() { assertTrue(ReportState.SAFE_VALUES.contains(Mode.FLIPS_KEY)); }
}
