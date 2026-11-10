package com.tj.launcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** BN8a: the three fixed summaries on the look-alike's home page are the ones Tj asked for (2026-11-10). */
public class SsPagesHomeTest {
    @Test public void theThreeHomeSummariesAreTheOnesTjAsked() {
        assertEquals("Brightness, sleep, font size", SsPages.SUMMARY_DISPLAY);
        assertEquals("Volume", SsPages.SUMMARY_SOUND);
        assertEquals("Languages", SsPages.SUMMARY_SYSTEM);
    }
}
