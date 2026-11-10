package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/** BN1p D2: a summary that is not known yet is ONE blank line (a no-break space), so the row has its final height from the first frame. */
public class SsRowsTest {
    @Test public void anUnknownSummaryIsOneBlankLine() {
        assertEquals(" ", SsRows.BLANK);
        assertEquals(SsRows.BLANK, SsRows.orBlank(null));
        assertEquals(SsRows.BLANK, SsRows.orBlank(""));
        assertEquals("95% used - 4.1 GB free", SsRows.orBlank("95% used - 4.1 GB free"));
        assertEquals(1, SsRows.BLANK.length());
        assertNotEquals(' ', SsRows.BLANK.charAt(0));          // not a plain space: a layout may trim that
    }
}
