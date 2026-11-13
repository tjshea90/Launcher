package com.tj.launcher;

/**
 * BL1 (Tj, 2026-11-08 21:11 UTC: "Make the home looking curtain instead of the black screen. An identical screenshot of the current home screen"). The pure rules (no Android classes) of the HOME CURTAIN:
 * the full-screen picture that the no-helper return reset shows instead of the black cover while it clears Recents and the shade. Design, the map of the old cover and the reasons:
 * {@code research/BL1_HOME_CURTAIN_2026-11-08.md}.
 *
 * The picture is the home screen as it really looks: a real system screenshot of the plain wallpaper (kept per wallpaper position when the home calibration takes it) with the Launcher's own pixels laid
 * over it (the Launcher window's {@code PixelCopy}), composed into ONE stored picture per rotation. Everything here only decides; a doubt always means "no picture" and the plain black cover is shown as
 * before, so the curtain can never be worse than the cover it replaces.
 */
public final class HomeCurtainRules {
    private HomeCurtainRules() {}

    /** The switch in Launcher control (shipped default ON, debug builds OFF so the older emulator sections keep seeing the black cover, like clean_start / home_patch). */
    public static final String KEY = "home_curtain";

    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    // ---------------------------------------------------------------- may the stored picture be shown as the cover?

    /** A picture older than this is not used (the data does not live for ever; a changed home is re-captured long before). */
    public static final long MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000;

    /**
     * A capture that finds the home UNCHANGED confirms that the stored picture is still right: once it is older than this, its time stamp is renewed (the meta only, no picture is written). Without it a home that
     * is never rearranged would turn "too old" after {@link #MAX_AGE_MS} and the black cover would come back for good (code review 2026-11-08).
     */
    public static final long STAMP_AFTER_MS = 24L * 60 * 60 * 1000;

    public static boolean stampDue(long ageMs) { return ageMs > STAMP_AFTER_MS && ageMs <= MAX_AGE_MS * 4; }

    public static final class UseFacts {
        public boolean switchOn, hasPicture;
        /** The picture's own signature (wallpaper id | rotation | display size) and the signature of the screen as it is now. */
        public String pictureSig = "", currentSig = "";
        public int picW, picH, dispW, dispH, picRotation, rotation;
        /** ms since the picture was captured; a huge number = unknown. */
        public long ageMs = Long.MAX_VALUE;
    }

    /** Why the stored picture must NOT be used as the cover now (plain words, shown in Launcher control), or "" = use it. The first reason wins. */
    public static String whyNotUse(UseFacts f) {
        if (!f.switchOn) return "switch off";
        if (!f.hasPicture) return "no picture yet";
        if (f.picRotation != f.rotation) return "the picture is for another screen rotation";
        if (f.picW != f.dispW || f.picH != f.dispH || f.picW <= 0 || f.picH <= 0) return "the picture has another size than the screen";
        if (f.pictureSig == null || f.currentSig == null || f.pictureSig.isEmpty() || !f.pictureSig.equals(f.currentSig)) return "the wallpaper changed since the picture was taken";
        if (f.ageMs < 0 || f.ageMs > MAX_AGE_MS) return "the picture is too old";
        return "";
    }

    // ---------------------------------------------------------------- when may the picture be captured? (the home at rest, the Launcher's own pixels, no flash)

    /** The home must be quiet: no finger, no Launcher drawing for this long; and at most one capture per gap. */
    public static final long QUIET_DRAW_MS = 1500, QUIET_TOUCH_MS = 1500, MIN_GAP_MS = 15_000;

    public static final class CaptureFacts {
        public boolean switchOn, customMode, unlocked, screenOn, homeAtRest, positionKnown, calibrating, wallKnown, building;
        /** ms since the Launcher last drew / since a finger went down / since the last capture started; a huge number = never. */
        public long msSinceDraw = Long.MAX_VALUE, msSinceTouch = Long.MAX_VALUE, msSinceLastCapture = Long.MAX_VALUE;
    }

    /** Why the picture must NOT be captured now: the first reason in plain words, or "" = go. */
    public static String whyNotCapture(CaptureFacts f) {
        if (!f.switchOn) return "switch off";
        if (!f.customMode) return "Quickstep";
        if (!f.unlocked) return "lock screen";
        if (!f.screenOn) return "screen off";
        if (f.calibrating) return "a calibration is running";
        if (f.building) return "a capture is running";
        if (!f.positionKnown) return "the wallpaper position is not known yet";
        if (!f.wallKnown) return "no wallpaper picture for this page yet (the next calibration takes it)";
        if (!f.homeAtRest) return "the home screen is not at rest";
        if (f.msSinceTouch < QUIET_TOUCH_MS) return "touched a moment ago";
        if (f.msSinceDraw < QUIET_DRAW_MS) return "the home screen is still moving";
        if (f.msSinceLastCapture < MIN_GAP_MS) return "captured a moment ago";
        return "";
    }

    // ---------------------------------------------------------------- touches

    /** A cover raised at the ENTER tap must not swallow touches until the unlock is confirmed: a wrong PIN has to stay retypable (the black cover never swallowed any touch). */
    public static final long CONFIRMED_WITHIN_MS = 8000;

    /** Is the unlock confirmed (USER_PRESENT seen) recently enough that a cover created now belongs to it? {@code lastUnlockUpMs <= 0} = never. */
    public static boolean unlockConfirmed(long nowMs, long lastUnlockUpMs) {
        return lastUnlockUpMs > 0 && nowMs >= lastUnlockUpMs && nowMs - lastUnlockUpMs < CONFIRMED_WITHIN_MS;
    }

    // ---------------------------------------------------------------- the content hash of the Launcher's layer (changed? blank?)

    /** Every this-many rows / columns is sampled (a full compare of 600x1024 pixels is not needed to notice a moved icon: an icon is far bigger than this). */
    public static final int SAMPLE_STEP_ROW = 4, SAMPLE_STEP_COL = 3;

    /** Folds one sampled row of ARGB pixels into the running hash. Returns the new hash; {@code opaque[0]} counts the samples whose alpha is not 0. */
    public static int foldRow(int acc, int[] row, int len, int[] opaque) {
        int h = acc;
        for (int x = 0; x < len; x += SAMPLE_STEP_COL) {
            int p = row[x];
            if ((p >>> 24) != 0) opaque[0]++;
            h = h * 31 + p;
        }
        return h;
    }

    /** A layer with no visible sample at all is a failed copy (a window that has not drawn yet), never "the home screen". */
    public static boolean blank(int[] opaque) { return opaque[0] == 0; }

    /**
     * CA1 M1: is the in-memory curtain (600x1024 ARGB = 2.4 MB) worth keeping? Only where a cover can show it: the Enter tap that ENDS a Quickstep session (stored mode Quickstep) and the first unlock
     * after a boot (the boot clean-up, also in the custom mode). In a plain custom session nothing shows it, so it is dropped and read back from its file when the mode turns Quickstep (long before
     * the Enter tap that ends that session). The picture on disk is kept and refreshed exactly as before.
     */
    public static boolean keepInMemory(boolean switchOn, boolean customMode, boolean bootCleanPending) { return switchOn && (!customMode || bootCleanPending); }

    /** Must the stored picture be re-made? Different layer content, or a different page / wallpaper / size, or none stored. */
    public static boolean changed(boolean hadStored, String storedSig, int storedStep, int storedHash, String sig, int step, int hash) {
        if (!hadStored) return true;
        if (storedSig == null || !storedSig.equals(sig)) return true;
        if (!HomePatchRules.sameStep(storedStep, step)) return true;
        return storedHash != hash;
    }

    // ---------------------------------------------------------------- file and preference names

    public static String wallFile(int rotation, int step) { return "homewall_r" + rotation + "_x" + step + ".png"; }
    public static String wallMeta(int rotation, int step) { return "home_wall_r" + rotation + "_x" + step; }
    public static String curtainFile(int rotation) { return "homecurtain_r" + rotation + ".png"; }
    public static String curtainMeta(int rotation) { return "home_curtain_r" + rotation; }

    /** The curtain's stored meta: {@code signature#step#layerHash#savedAtMs}. */
    public static String metaString(String sig, int step, int hash, long savedAtMs) { return sig + "#" + step + "#" + hash + "#" + savedAtMs; }

    /** Parsed meta of a stored curtain, or null when the string is not one. */
    public static final class Meta {
        public final String sig; public final int step, hash; public final long savedAtMs;
        Meta(String sig, int step, int hash, long savedAtMs) { this.sig = sig; this.step = step; this.hash = hash; this.savedAtMs = savedAtMs; }
    }

    public static Meta parseMeta(String s) {
        try {
            if (s == null) return null;
            String[] p = s.split("#");
            if (p.length != 4 || p[0].isEmpty()) return null;
            return new Meta(p[0], Integer.parseInt(p[1]), Integer.parseInt(p[2]), Long.parseLong(p[3]));
        } catch (NumberFormatException e) { return null; }
    }
}
