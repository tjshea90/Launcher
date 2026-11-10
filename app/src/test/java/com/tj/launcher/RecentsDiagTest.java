package com.tj.launcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecentsDiagTest {
    @Test public void onlyTheTwoFixedWordsAreRecognised() {
        assertEquals(RecentsDiag.CLEAR_ALL, RecentsDiag.word("Clear all"));
        assertEquals(RecentsDiag.CLEAR_ALL, RecentsDiag.word("CLEAR ALL"));
        assertEquals(RecentsDiag.NO_ITEMS, RecentsDiag.word("No recent items"));
    }

    @Test public void cardTextIsNeverRecognisedSoItNeverReachesThePublishedReport() {
        assertEquals(RecentsDiag.NONE, RecentsDiag.word("Telegram: Alice, 3 new messages"));
        assertEquals(RecentsDiag.NONE, RecentsDiag.word("Claude"));
    }

    @Test public void oddInputIsHarmless() {
        assertEquals(RecentsDiag.NONE, RecentsDiag.word(null));
        assertEquals(RecentsDiag.NONE, RecentsDiag.word(""));
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 1000; i++) big.append("clear all ");
        assertEquals("a long text is a message, not a button", RecentsDiag.NONE, RecentsDiag.word(big));
    }
}
