package com.tj.launcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** BY1c / F1: the lock patch stays away while SystemUI's notification panel / quick settings are open on the lock screen (pane titles, run #565). */
public class LockShadeTest {
    private static final int PT = LockShade.PANE_TITLE;

    @Test public void englishTitlesFromRun565() {
        assertEquals(LockShade.OPEN, LockShade.kind(true, PT, "Quick settings.", null, null));
        assertEquals(LockShade.CLOSED, LockShade.kind(true, PT, "Lock screen.", null, null));
        assertEquals(LockShade.OPEN, LockShade.kind(true, PT, "Notification shade.", null, null));
        assertEquals(LockShade.OPEN, LockShade.kind(true, PT, "Quick settings editor.", null, null));
    }

    @Test public void localizedTitlesFromSystemUiResources() {
        String[] open = { "Schnelleinstellungen.", "Benachrichtigungsleiste." };
        assertEquals(LockShade.OPEN, LockShade.kind(true, PT, "Schnelleinstellungen.", "Sperrbildschirm.", open));
        assertEquals(LockShade.CLOSED, LockShade.kind(true, PT, " Sperrbildschirm. ", "Sperrbildschirm.", open));
        assertEquals(LockShade.OPEN, LockShade.kind(true, PT, "Quick settings.", "Sperrbildschirm.", open));   // the English fallback still counts
    }

    @Test public void onlyPaneTitleChangesOfSystemUiCount() {
        assertEquals(LockShade.UNKNOWN, LockShade.kind(false, PT, "Quick settings.", null, null));   // another app
        assertEquals(LockShade.UNKNOWN, LockShade.kind(true, 0x10, "Pin unlock.", null, null));      // the PIN pad appeared (PANE_APPEARED), run #565
        assertEquals(LockShade.UNKNOWN, LockShade.kind(true, 0x0, "Phone options", null, null));     // the power menu's own window event
        assertEquals(LockShade.UNKNOWN, LockShade.kind(true, PT | 0x1, "Lock screen.".substring(0, 4), null, null));   // an unknown title
        assertEquals(LockShade.CLOSED, LockShade.kind(true, PT | 0x1, "Lock screen.", null, null));   // other change bits next to PANE_TITLE
        assertEquals(LockShade.UNKNOWN, LockShade.kind(true, PT, null, null, null));
        assertEquals(LockShade.UNKNOWN, LockShade.kind(true, PT, "  ", "", new String[] { "" }));    // empty titles never match
    }

    @Test public void holdWhileOpenAndForTheSettleAfterTheClose() {
        assertEquals(Long.MAX_VALUE, LockShade.holdUntil(true, 0));
        assertEquals(Long.MAX_VALUE, LockShade.holdUntil(true, 5000));
        assertEquals(5000 + LockShade.SETTLE_MS, LockShade.holdUntil(false, 5000));
        assertEquals(0, LockShade.holdUntil(false, 0));   // no close since the reset: no hold
    }

    @Test public void theCloseHoldFeedsTheOneReturnTime() {
        long closed = 10_000, now = 10_100;
        long until = LockShade.holdUntil(false, closed);
        assertEquals(LockShade.SETTLE_MS - 100, PatchRest.returnInMs(now, now - PatchRest.SETTLE_MS, 0, Math.max(0, until)));
    }
}
