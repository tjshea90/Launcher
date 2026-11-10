package com.tj.launcher;

/**
 * BR1 (Tj, 2026-11-10 19:27 UTC: "Make the settings gear icon in the custom launcher notification panel open the spoof settings app for the custom launcher instead of the regular android settings"): the pure
 * rules (JVM-testable) of the transparent overlay that sits over the gear ("Open settings.", {@code com.android.systemui:id/settings_button}) in the quick-settings footer. Design and AOSP facts:
 * research/GEAR_TO_LOOKALIKE_2026-11-10.md. The service wires these rules; the overlay rides on the pencil cover (it is shown exactly while the cover is shown).
 *
 * <p>Rectangles are {@code int[]{left, top, right, bottom}} so that nothing here needs the Android framework.
 */
public final class GearRules {
    private GearRules() {}

    /** The switch in Launcher control. */
    public static final String KEY = "gear_to_lookalike";
    /** Text preference (allow-listed in the tablet report): what the last learn / tap did. */
    public static final String STATUS_KEY = "gear_status";
    /** SystemUI's id of the gear in Android 10 (AOSP qs_footer_impl.xml). */
    public static final String VIEW_ID = "com.android.systemui:id/settings_button";
    /** The gear's accessibility description in Android 10 (AOSP strings.xml accessibility_quick_settings_settings: "Open settings."). Matched loosely, lower case. */
    public static final String DESCRIPTION_PART = "settings";
    /** A gear is a footer button: bigger than this is the wrong node. */
    public static final int MAX_PX = 400;
    /** Learning the gear's place relative to the pencil is tried this many times per panel visit, then left alone until the cover comes back. */
    public static final int MAX_LEARN_TRIES = 3;
    /** Delay before a learn (let the panel settle) and between two tries (ms). */
    public static final long LEARN_DELAY_MS = 80, LEARN_RETRY_MS = 600;

    /** What the overlay does at a look (see {@link #decide}). */
    public static final int HIDE = 0, SHOW = 1, LEARN = 2, REMOVE = 3;

    /**
     * ON in release builds, OFF in debug builds like every new Tj-facing behaviour (clean_start, settings_spoof): the emulator sections that do not test the gear must not find a window over the footer.
     * Tj can flip it in Launcher control.
     */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /**
     * The overlay may exist at all: the switch is on, the custom launcher is the active one, the tablet is UNLOCKED (on the lock screen the real gear stays, it asks for the PIN: the look-alike must never open over
     * a locked screen) and the Settings look-alike itself is on (it exists only in the custom mode, SettingsSpoof.aliasWanted).
     */
    public static boolean wanted(boolean switchOn, boolean customMode, boolean keyguardLocked, boolean lookAlikeOn) {
        return switchOn && customMode && !keyguardLocked && lookAlikeOn;
    }

    /** What to do at a look, from the facts: not wanted = remove the window; the cover is away = hide it (untouchable, kept warm); the offset to the pencil is unknown or was learned under another screen = learn it first; else show it. */
    public static int decide(boolean wanted, boolean coverShown, boolean offsetKnown, boolean offsetKeyMatches) {
        if (!wanted) return REMOVE;
        if (!coverShown) return HIDE;
        if (!offsetKnown || !offsetKeyMatches) return LEARN;
        return SHOW;
    }

    /** A button-sized, non-empty rectangle. */
    public static boolean boundsOk(int[] r) {
        if (r == null || r.length != 4) return false;
        int w = r[2] - r[0], h = r[3] - r[1];
        return w > 0 && h > 0 && w < MAX_PX && h < MAX_PX;
    }

    /** The gear's edges minus the pencil's, edge by edge: both are children of ONE footer row, so the difference stays the same while the panel moves. */
    public static int[] offset(int[] pencil, int[] gear) {
        return new int[] { gear[0] - pencil[0], gear[1] - pencil[1], gear[2] - pencil[2], gear[3] - pencil[3] };
    }

    /** Where the gear is when the pencil is at {@code pencil}. */
    public static int[] place(int[] pencil, int[] off) {
        return new int[] { pencil[0] + off[0], pencil[1] + off[1], pencil[2] + off[2], pencil[3] + off[3] };
    }

    /** The facts a learned offset depends on: the footer is laid out again when the screen turns or the density changes. Two keys differ = the offset is stale. */
    public static String key(int widthPx, int heightPx, int densityDpi, int rotation) {
        return widthPx + "x" + heightPx + "@" + densityDpi + "r" + rotation;
    }

    /** True when the finger went down and up close together (inside twice the touch slop): a tap. A swipe that starts on the gear is not one. */
    public static boolean isTap(float downX, float downY, float upX, float upY, float slopPx) {
        float dx = upX - downX, dy = upY - downY, lim = 2f * slopPx;
        return dx * dx + dy * dy <= lim * lim;
    }

    /** The two rectangles are the same place (the panel was at rest while something was read twice). */
    public static boolean same(int[] a, int[] b) {
        return a != null && b != null && a.length == 4 && b.length == 4 && a[0] == b[0] && a[1] == b[1] && a[2] == b[2] && a[3] == b[3];
    }

    /** One line for the tablet report. */
    public static String statusLine(String clock, String what) { return clock + "  " + what; }
}
