package com.tj.launcher;

/**
 * BA1 (Tj, 2026-11-07: "make the brightness of the square patch match the darkened lock screen after the side key press 100% identical"): WHEN and HOW the helper measures the stock menu's real scrim on the tablet
 * (the maths is {@link ScrimFit}). The sequence, once per lock screen (wallpaper / rotation / size / theme): a bare frame of the plain lock screen, the stock power menu opened by the helper itself
 * ({@code GLOBAL_ACTION_POWER_DIALOG}) until its fade is complete, a second frame, the menu closed again ({@code GLOBAL_ACTION_BACK}, retried until it is gone). It only ever starts on a PLAIN lock screen (screen on,
 * no PIN pad, no menu, no gesture in progress) and gives up at the first sign of anything else: the lock screen is never blocked. This class is the pure part (JVM-tested): the decision, the limits and the sampling grid.
 * {@link TjAccessibilityService} does the Android side.
 */
public final class ScrimMeasure {
    /** Launcher control switch. Default ON in the app, OFF in a debuggable build (the emulator sections that do not test this must not see a power menu open by itself). */
    public static final String KEY = "scrim_measure";
    /** The epoch ms of the recent starts, comma separated (the cap per day; {@link AutoCalibration#addTry} keeps the list). */
    public static final String TRIES_KEY = "scrim_tries";
    /** What happened last, in plain words (Launcher control, the tablet report). */
    public static final String NOTE_KEY = "scrim_note";
    /** The stored measurement of one rotation: {@code VALUE_PREFIX + rotation} ({@link ScrimFit#encode}). */
    public static final String VALUE_PREFIX = "patch_scrim_r";

    public static final long MIN_GAP_MS = 45_000;
    /**
     * BQ1a (Tj, 2026-11-10, v0.9.1: "It did two or three screenshots of the lock screen again, one time on each of the three last times I went to the lock screen"): ONE run per lock, because every run starts with a REAL
     * system screenshot (shutter flash, thumbnail) the moment it begins, however it ends.
     */
    public static final int MAX_PER_LOCK = 1, MAX_PER_DAY = 4;
    /**
     * The screen has to have been on this long before a run starts: the lock screen settles (clock, wallpaper fade-in) AND the THEME settles. After a Quickstep session the battery saver is ON, so the lock screen wakes in
     * the DARK theme; the lock rule switches the saver off, but Android applies that several seconds after the wake on Tj's slow tablet (2026-11-10: the setting changed 1.0 s after the rule's write, the theme later still).
     * At the old 3.5 s the decision fell into that dark moment, started a run for the DARK slot, took its first real screenshot, and the theme flip or the person's swipe then stopped it: the slot was never stored, so
     * the next lock from Quickstep started it again (BQ1a).
     */
    public static final long SETTLE_AFTER_WAKE_MS = 10_000;
    /** How long to wait for a screenshot to appear in the media store, for the menu to open, and the steps of closing it again. */
    public static final long SHOT_WAIT_MS = 8_000, MENU_WAIT_MS = 4_000, CLOSE_STEP_MS = 900;
    public static final int CLOSE_TRIES = 5;
    /** Between the bare frame and the menu: the screenshot's own animation (flash, shrink, thumbnail in the corner) must be over before the next frame. */
    public static final long SHOT_GAP_MS = 2_400;
    /**
     * After the menu opened: the stock fade (300 ms) is complete, plus a LARGE margin: the helper's detection fires on the dialog's window event, but a slow tablet may draw the dialog's first frame (= the start
     * of the fade) a few hundred ms later, and the screenshot service adds its own latency. A frame taken in the middle of the fade would be stored for good (review of BA1, finding 6), so wait longer rather than less.
     */
    public static final long OPEN_SETTLE_MS = PatchFade.SETTLE_MS + 870;
    /** The whole sequence gives up after this. */
    public static final long STAGE_LIMIT_MS = 45_000;

    private ScrimMeasure() {}

    /** The default of the switch: ON in the app, OFF in a debuggable (emulator test) build. */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** Everything the decision depends on; the service fills it in. */
    public static final class State {
        public boolean switchOn, coverOn, lockPatchOn, paused, permitted, geometryKnown, validCrop, haveMeasurement, calibrating, stageRunning;
        public boolean interactive, keyguardLocked, pinPadUp, menuOpen, handshakeArmed;
        public long now, screenOnMs, lastStartMs;
        public int startsThisLock, startsToday, failures;
    }

    // ---------------------------------------------------------------- failures that will not go away by trying again (review of BA1, finding 5)

    /** Per rotation: how many measurements of THIS lock screen (wallpaper | rotation | size | theme) were taken but could not be fitted: {@code sig#d|n#count}. */
    public static final String FAIL_PREFIX = "scrim_fail_r";
    /** After this many runs of the same lock screen that stored nothing (frames that could not be fitted, OR runs that were interrupted: BQ1a) the helper stops until the wallpaper, rotation, size or theme changes (or "Measure again" is tapped): a flat wallpaper, a scrim that is not a flat layer or a lock screen the person always unlocks within seconds would otherwise flash a screenshot and open the menu by itself again and again. */
    public static final int MAX_FAILURES = 2;
    public static String failKey(int rotation) { return failKey(rotation, false); }
    /** The failure record of one rotation in one theme (light = the v0.8.2 key, dark = the same with an "n"): see {@link #valueKey(int, boolean)}. */
    public static String failKey(int rotation, boolean night) { return FAIL_PREFIX + rotation + (night ? "n" : ""); }

    /**
     * Pure: did a run end WITHOUT storing a measurement? BQ1a: every run costs the person a real screenshot (the bare frame is asked for the moment the run starts), so EVERY run that does not end in a stored value counts,
     * whether the frames could not be fitted ("not measured: ...") or it was interrupted ("stopped: the PIN pad came up / the theme changed / the screen went off ..."). Before v0.9.2 an interruption counted for nothing
     * and was tried again at the next lock, up to four flashes a day for a slot that could never finish. After {@link #MAX_FAILURES} such runs the slot waits for a new wallpaper, rotation or theme, or for "Measure again".
     */
    public static boolean measurementFailure(String outcome) {
        return outcome != null && !outcome.isEmpty() && !outcome.startsWith("MEASURED");
    }

    /** The last run, in plain words, with the time and the theme it ran in (Launcher control, the tablet report): the 2026-11-10 report could not say WHICH run took the screenshots because only the last START was kept. */
    public static final String RUN_KEY = "scrim_last_run";

    /** Pure: one line for {@link #RUN_KEY}: {@code stamp + " " + theme + ": " + outcome}, the outcome cut to 140 characters. */
    public static String runLine(String stamp, boolean night, String outcome) {
        String o = outcome == null ? "" : outcome.trim();
        if (o.length() > 140) o = o.substring(0, 140);
        return stamp + (night ? " dark theme: " : " light theme: ") + o;
    }

    /** Pure: the number of recorded failures for this lock screen (0 for another lock screen or an unreadable record). */
    public static int failures(String stored, String sig, boolean night) {
        try {
            if (stored == null || stored.isEmpty()) return 0;
            String[] p = stored.split("#");
            if (p.length != 3 || !p[0].equals(sig) || !p[1].equals(night ? "n" : "d")) return 0;
            return Math.max(0, Integer.parseInt(p[2]));
        } catch (Throwable t) { return 0; }
    }

    /** Pure: the record with one more failure for this lock screen (a record of another lock screen starts again at 1). */
    public static String addFailure(String stored, String sig, boolean night) {
        return sig + "#" + (night ? "n" : "d") + "#" + (failures(stored, sig, night) + 1);
    }

    /** Pure: why NOT to start now ("" = go). The reasons are shown in Launcher control, so they are plain words. */
    public static String decision(State s) {
        if (!s.switchOn) return "measuring the menu dimming is switched off";
        if (s.paused) return "paused (Restore everything / the crash breaker)";
        if (!s.coverOn || !s.lockPatchOn) return "the lock-screen patch is switched off";
        if (!s.permitted) return "needs permission: Launcher control > Allow wallpaper access";
        if (s.haveMeasurement) return "";   // nothing to do; callers treat haveMeasurement separately
        if (s.failures >= MAX_FAILURES) return "gave up for this lock screen after " + s.failures + " runs that stored nothing (see the last run); a new wallpaper, rotation or theme, or 'Measure again' in Launcher control, tries again";
        if (s.calibrating || s.stageRunning) return "a calibration or a measurement is already running";
        if (!s.validCrop) return "waiting: the patch has no real-pixel picture yet (the automatic calibration comes first)";
        if (!s.geometryKnown) return "waiting: hold the side key once on the lock screen so the menu's place is known";
        if (!s.interactive) return "waiting: the screen is off";
        if (!s.keyguardLocked) return "waiting for the lock screen";
        if (s.pinPadUp) return "waiting: the PIN pad is up";
        if (s.menuOpen) return "waiting: the power menu is open";
        if (s.handshakeArmed) return "waiting: an unlock gesture is in progress";
        if (s.screenOnMs > 0 && s.now >= s.screenOnMs && s.now - s.screenOnMs < SETTLE_AFTER_WAKE_MS) return "waiting: the screen has only just come on";
        if (s.lastStartMs > 0 && s.now >= s.lastStartMs && s.now - s.lastStartMs < MIN_GAP_MS) return "waiting: the last measurement was just now";
        if (s.startsThisLock >= MAX_PER_LOCK) return "gave up for this lock after " + MAX_PER_LOCK + (MAX_PER_LOCK == 1 ? " try" : " tries");
        if (s.startsToday >= MAX_PER_DAY) return "gave up for today after " + MAX_PER_DAY + " tries";
        return "";
    }

    /** Pure: start now? Only when there is no measurement yet for this lock screen AND {@link #decision} says go. */
    public static boolean shouldStart(State s) { return !s.haveMeasurement && decision(s).isEmpty(); }

    /** Pure: the preference key of the measurement of one rotation in the LIGHT theme (the v0.8.2 key: an existing measurement stays valid). */
    public static String valueKey(int rotation) { return valueKey(rotation, false); }

    /**
     * Pure: the key of the measurement of one rotation in one theme. v0.8.2 had ONE slot for both themes: every flip of the theme (battery saver switches the dark theme on and off) made the stored value invalid, the helper
     * measured again (two camera flashes, the stock menu opening by itself) and overwrote the other theme's value, so it flip-flopped up to 4 times a day (Tj, 2026-11-07, while testing the battery saver: "the lock screen
     * screenshot itself again, not sure why"). Each theme now has its own slot: light = the old key, dark = the same key with an "n". Each is measured once.
     */
    public static String valueKey(int rotation, boolean night) { return VALUE_PREFIX + rotation + (night ? "n" : ""); }

    /** Why the helper last started a measurement, in plain words (written when it starts; shown in Launcher control and in the tablet report). */
    public static final String WHY_KEY = "scrim_why";

    // ---------------------------------------------------------------- the sampling grid

    /**
     * The rectangles (left, top, right, bottom) that must NOT be sampled: the status bar and navigation bar bands (icons, clock), the stock panel with room for its shadow, the patch window, and our own lock number.
     * {@code panel} / {@code patch} may be null (unknown). Everything else on the lock screen is either plain wallpaper (the scrim applied to it) or small clutter that the robust fit throws out.
     */
    public static int[][] excludeRects(int w, int h, int[] panel, int[] patch, int shadowPx) {
        int top = Math.round(h * 0.035f), bottomBand = Math.round(h * 0.065f);
        int[] num = numberRect(w, h);
        int n = 3 + (panel != null ? 1 : 0) + (patch != null ? 1 : 0), i = 0;
        int[][] r = new int[n][];
        r[i++] = new int[] { 0, 0, w, top };
        r[i++] = new int[] { 0, h - bottomBand, w, h };
        r[i++] = num;
        if (panel != null) r[i++] = new int[] { panel[0] - shadowPx, panel[1] - shadowPx, panel[2] + shadowPx, panel[3] + shadowPx };
        if (patch != null) r[i++] = new int[] { patch[0] - 6, patch[1] - 6, patch[2] + 6, patch[3] + 6 };
        return r;
    }

    /** Our own lock number (v0.3.3: bbox x 205-396, y 907-924 on a 600 x 1024 screen) scaled to this screen, with room around it. */
    public static int[] numberRect(int w, int h) {
        float sx = w / 600f, sy = h / 1024f;
        return new int[] { Math.round(190 * sx), Math.round(895 * sy), Math.round(410 * sx), Math.round(938 * sy) };
    }

    private static boolean inside(int x, int y, int[] r) { return x >= r[0] && x < r[2] && y >= r[1] && y < r[3]; }

    /**
     * Pure: the sample points, every {@code step} pixels, as {@code (y << 16) | x}, skipping the excluded rectangles and a margin of 1 pixel at the screen edge (the flatness test looks at the neighbours).
     */
    public static int[] grid(int w, int h, int step, int[][] exclude) {
        int cap = ((w / step) + 1) * ((h / step) + 1);
        int[] out = new int[cap];
        int n = 0;
        for (int y = 1; y < h - 1; y += step) {
            for (int x = 1; x < w - 1; x += step) {
                boolean skip = false;
                if (exclude != null) for (int[] r : exclude) if (r != null && inside(x, y, r)) { skip = true; break; }
                if (!skip) out[n++] = (y << 16) | x;
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    /**
     * Pure: is a pixel flat enough to sample? Neither an edge through it (the opposite neighbours differ) nor a one-pixel line or dot on it (it differs from the average of its four neighbours: a central
     * difference alone is blind to a thin stroke, whose two sides are equal), so a text stroke or a one-pixel shift cannot spoil the pair.
     */
    public static boolean flat(int lumCenter, int lumLeft, int lumRight, int lumUp, int lumDown) {
        return Math.abs(lumRight - lumLeft) + Math.abs(lumDown - lumUp) <= FLAT_MAX
                && Math.abs(4 * lumCenter - (lumLeft + lumRight + lumUp + lumDown)) <= FLAT_MAX;
    }
    public static final int FLAT_MAX = 60;

    /** Pure: the luminance (0..255) of a packed RGB value. */
    public static int lum(int rgb) { return (((rgb >> 16) & 255) * 77 + ((rgb >> 8) & 255) * 150 + (rgb & 255) * 29) >> 8; }

    /**
     * Pure: the sample points of the BARE frame: the grid (every 4th pixel, every 2nd when that leaves too few), minus the excluded rectangles, keeping only flat pixels. {@code px} = the frame's pixels
     * ({@code Bitmap.getPixels}, row-major, w * h). The same points are then read from the menu frame.
     */
    public static int[] pickPoints(int[] px, int w, int h, int[][] exclude) {
        if (px == null || px.length < w * h || w < 4 || h < 4) return new int[0];
        int[] kept = new int[0];
        for (int step : new int[] { 4, 2 }) {
            int[] g = grid(w, h, step, exclude);
            kept = new int[g.length];
            int n = 0;
            for (int v : g) {
                int x = v & 0xFFFF, y = v >>> 16, i = y * w + x;
                if (flat(lum(px[i]), lum(px[i - 1]), lum(px[i + 1]), lum(px[i - w]), lum(px[i + w]))) kept[n++] = v;
            }
            kept = java.util.Arrays.copyOf(kept, n);
            if (n >= 2 * ScrimFit.MIN_SAMPLES) break;
        }
        return kept;
    }

    /** Pure: one colour channel (0..255) of the frame at the points; {@code shift} 16 = red, 8 = green, 0 = blue. */
    public static int[] channel(int[] px, int w, int[] points, int shift) {
        int[] out = new int[points.length];
        for (int k = 0; k < points.length; k++) out[k] = (px[(points[k] >>> 16) * w + (points[k] & 0xFFFF)] >> shift) & 255;
        return out;
    }
}
