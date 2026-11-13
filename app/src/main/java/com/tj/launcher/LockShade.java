package com.tj.launcher;

/**
 * BY1c / F1 (Tj, 2026-11-12: "It should cover the screenshot button without flicker or delay ... after pulling down the notification panel ... It should not flicker or flash either the screenshot button or the
 * wallpaper patch at any time"). Emulator film {@code shade} of runs #564 and #565: the finger's DOWN hid the resting lock patch, the finger hold (1.5 s) ended while the panel was still OPEN and the patch came
 * back ON TOP of it (a block of lock-screen wallpaper over the white quick settings for 2.3 s). Nothing in the lock flavour knew about the panel.
 * <p>
 * SystemUI's notification panel is an accessibility PANE: every change of its title arrives as TYPE_WINDOW_STATE_CHANGED from {@code com.android.systemui} with CONTENT_CHANGE_TYPE_PANE_TITLE (0x8) and the title
 * as the event text (run #565: "Quick settings." when the quick settings are open, "Lock screen." at the END of the collapse). The titles are SystemUI's own strings
 * ({@code accessibility_desc_quick_settings}, {@code _notification_shade}, {@code _quick_settings_edit}, {@code _lock_screen}); the service reads them from SystemUI's resources (any language) and these English
 * ones are the fallback. Pure rules, JVM-tested (LockShadeTest).
 */
final class LockShade {
    private LockShade() {}

    static final int UNKNOWN = 0, OPEN = 1, CLOSED = 2;
    /** AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE (API 28). */
    static final int PANE_TITLE = 0x8;
    /** After "Lock screen." the panel's scrim is still fading out (run #565: the film settled about 90 ms after the event); the patch waits this long before it rests visibly again. */
    static final long SETTLE_MS = 400;

    static final String CLOSED_EN = "Lock screen.";
    static final String[] OPEN_EN = { "Quick settings.", "Notification shade.", "Quick settings editor." };
    /** SystemUI resource names of the titles (string resources of com.android.systemui). */
    static final String CLOSED_RES = "accessibility_desc_lock_screen";
    static final String[] OPEN_RES = { "accessibility_desc_quick_settings", "accessibility_desc_notification_shade", "accessibility_desc_quick_settings_edit" };

    /** What a SystemUI pane-title event (or a pane title read from the panel's node) says about the panel: OPEN, CLOSED or UNKNOWN (not a pane-title change, not SystemUI, an unknown title). */
    static int kind(boolean fromSystemUi, int contentChangeTypes, CharSequence title, String closedTitle, String[] openTitles) {
        if (!fromSystemUi || (contentChangeTypes & PANE_TITLE) == 0 || title == null) return UNKNOWN;
        String t = title.toString().trim();
        if (t.isEmpty()) return UNKNOWN;
        if (same(t, closedTitle) || same(t, CLOSED_EN)) return CLOSED;
        if (openTitles != null) for (String o : openTitles) if (same(t, o)) return OPEN;
        for (String o : OPEN_EN) if (same(t, o)) return OPEN;
        return UNKNOWN;
    }

    private static boolean same(String t, String title) {
        if (title == null) return false;
        String b = title.trim();
        return !b.isEmpty() && t.equalsIgnoreCase(b);
    }

    /**
     * Uptime ms until which the lock patch must stay away for the panel: {@link Long#MAX_VALUE} while it is open (its close event brings the patch back, no timer), the end of {@link #SETTLE_MS} after a close,
     * 0 = no hold. {@code closedAtMs} 0 = no close seen since the last reset.
     */
    static long holdUntil(boolean open, long closedAtMs) {
        if (open) return Long.MAX_VALUE;
        return closedAtMs > 0 ? closedAtMs + SETTLE_MS : 0;
    }
}
