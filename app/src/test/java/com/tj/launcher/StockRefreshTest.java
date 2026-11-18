package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CI1 / SR1: when the stock launcher may be restarted (pure rules), the helper command's shape, the status words. */
public class StockRefreshTest {
    private static final int MIN_KB = StockRefresh.MIN_PSS_KB, MIN_ACT = StockRefresh.MIN_ACTIVITIES;
    private static final long NOW = 1_000_000_000_000L;
    private static final long H = 3_600_000L;

    private static String go(boolean sw, boolean found, boolean helper, int pss, int act, boolean custom, boolean front, long last) {
        return StockRefresh.blocker(sw, found, helper, pss, act, custom, front, last, NOW, MIN_KB, MIN_ACT);
    }

    @Test public void everythingRightGoes() {
        assertEquals("", go(true, true, true, 300 * 1024, 2, true, true, 0));
    }

    @Test public void bigByMemoryOrByActivityCount() {
        assertEquals("", go(true, true, true, MIN_KB, 1, true, true, 0));          // exactly the memory line
        assertEquals("", go(true, true, true, 90 * 1024, MIN_ACT, true, true, 0)); // exactly the activity line
        assertEquals("not big", go(true, true, true, MIN_KB - 1, MIN_ACT - 1, true, true, 0));
        assertEquals("not big", go(true, true, true, -1, -1, true, true, 0));      // unknown numbers are never "big"
        assertEquals("", go(true, true, true, -1, MIN_ACT, true, true, 0));        // one known number is enough
    }

    @Test public void aHealthyStockLauncherIsNeverTouched() {
        assertEquals("not big", go(true, true, true, 130 * 1024, 2, true, true, 0));   // Tj's 130 MB after his own restart
        assertEquals("not big", go(true, true, true, 49 * 1024, 1, true, true, 0));    // his friend's 48 MB
    }

    @Test public void theSwitchAndTheReadingComeFirst() {
        assertEquals("switch off", go(false, true, true, 400 * 1024, 9, true, true, 0));
        assertEquals("no reading", go(true, false, true, 400 * 1024, 9, true, true, 0));
        assertEquals("reading not taken by the shell helper", go(true, true, false, 400 * 1024, 9, true, true, 0));
    }

    @Test public void neverWhileQuickstepIsActiveOrTheCustomHomeIsNotOnScreen() {
        assertEquals("Quickstep is the active launcher", go(true, true, true, 400 * 1024, 9, false, true, 0));
        assertEquals("the custom home is not on screen", go(true, true, true, 400 * 1024, 9, true, false, 0));
        assertEquals("Quickstep is the active launcher", go(true, true, true, 400 * 1024, 9, false, false, 0));
    }

    @Test public void atMostOncePerCooldown() {
        assertTrue(go(true, true, true, 400 * 1024, 9, true, true, NOW - 5 * H).startsWith("restarted 300 min ago"));
        assertEquals("", go(true, true, true, 400 * 1024, 9, true, true, NOW - 6 * H));         // exactly 6 h = due
        assertEquals("", go(true, true, true, 400 * 1024, 9, true, true, NOW - 30 * H));
        assertEquals("", go(true, true, true, 400 * 1024, 9, true, true, NOW + 5 * H));         // a clock that went backwards never blocks for ever
        assertEquals("", go(true, true, true, 400 * 1024, 9, true, true, 0));                    // never done
    }

    @Test public void theDefaultIsOnInReleaseAndOffInDebug() {
        assertTrue(StockRefresh.defaultOn(false));
        assertFalse(StockRefresh.defaultOn(true));
    }

    @Test public void theHelperCommandIsAConstantWithNoArgument() {
        assertTrue(ShellProtocol.isStockRestart("STOCKRESTART"));
        assertTrue(ShellProtocol.isStockRestart(" STOCKRESTART \n"));
        assertFalse(ShellProtocol.isStockRestart("STOCKRESTART com.android.systemui"));   // a package can never be named
        assertFalse(ShellProtocol.isStockRestart("STOCKRESTART; id"));
        assertFalse(ShellProtocol.isStockRestart("stockrestart"));
        assertFalse(ShellProtocol.isStockRestart("STOCKMEM"));
        assertFalse(ShellProtocol.isStockRestart(null));
        // the general force-stop still refuses the stock launcher (the dedicated command is the only way)
        assertFalse(ShellProtocol.allowed(ShellProtocol.STOCK_PKG));
        assertTrue(ShellProtocol.forceStopTargets("FORCESTOP " + ShellProtocol.STOCK_PKG).isEmpty());
    }

    @Test public void theResultIsToldInPlainWords() {
        String ok = StockRefresh.resultText("11-18 23:10", 312 * 1024, 7, 58 * 1024, 1, true, "");
        assertTrue(ok, ok.contains("restarted the stock launcher: before 312 MB, 7 Activities"));
        assertTrue(ok, ok.contains("58 MB, 1 Activities"));
        String none = StockRefresh.resultText("11-18 23:10", 312 * 1024, 7, -1, -1, true, "");
        assertTrue(none, none.contains("not running yet"));
        String bad = StockRefresh.resultText("11-18 23:10", 312 * 1024, 7, -1, -1, false, "refused: ERR unknown command");
        assertTrue(bad, bad.contains("the helper could not: refused: ERR unknown command"));
        assertTrue(StockRefresh.resultText("t", 1, 1, 1, 1, false, "").contains("no answer"));
    }
}
