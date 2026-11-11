package com.tj.launcher;

/**
 * AE3 / AD7b (Tj, 2026-11-06: "any time I'm on the lock screen, there should be an accessibility overlay of the wallpaper patch exactly where the screenshot button shows up, so that when I hold the side key
 * to open the power menu, the screenshot button is hidden without delay"). Until now the lock-screen patch window stayed invisible and was shown only when the power menu had been DETECTED (a short delay in which
 * the stock Screenshot entry could be seen). With the switch on, the patch rests visible over the Screenshot slot the whole time the lock screen is up, except while the PIN pad is showing. Pure rules, JVM-tested;
 * {@link TjAccessibilityService} draws it.
 */
public final class PatchRest {
    /** Launcher control switch, default ON (since v0.8.0; OFF = the patch is only drawn while the menu is open). */
    public static final String KEY = "lock_patch_always";

    private PatchRest() {}

    /**
     * Is the patch window visible?
     * @param menuOpen   our cover is up for an open power menu (the full, dimmed patch)
     * @param always     the Control switch (and the lock-screen patch itself) is on
     * @param lockMode   this is the lock-screen patch (the home-screen patch never rests)
     * @param pinPadUp   the PIN / password / pattern pad is showing: the resting patch must never sit over it
     */
    public static boolean visible(boolean menuOpen, boolean always, boolean lockMode, boolean pinPadUp) {
        if (!lockMode) return menuOpen;      // home-screen patch: only while the menu is open, as before
        if (menuOpen) return true;           // an open menu always gets its patch (a power menu can also open over the PIN pad)
        return always && !pinPadUp;
    }

    /**
     * Where the RESTING patch starts to paint, in the patch window's own coordinates: the top of the Screenshot slot = the bottom of our two-button card. The part above it (where the card will appear) is left
     * alone so the resting patch covers as little of the lock screen (clock, notifications) as possible. A card bottom above the window paints everything; below the window paints nothing.
     */
    public static int restTop(int windowTop, int cardBottom, int windowBottom) {
        int h = Math.max(0, windowBottom - windowTop);
        int t = cardBottom - windowTop;
        return Math.max(0, Math.min(t, h));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // AH3 (Tj, 2026-11-06, with side-by-side photos: our card is the right size and sits flush, but a flat bluish block beside / below it shows where the wallpaper patch does not match the live lock screen).
    // The old patch covered the WHOLE menu rectangle plus a 12 px ring on every side with hard edges. The new one covers ONLY the Screenshot slot (below our card), reaches just past the stock panel's edge and
    // fades out softly beyond it, so any residual mismatch is a gentle gradient instead of a visible frame. Pure geometry + mask, JVM-tested; {@link TjAccessibilityService} draws it.
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /** dp: how far the opaque core reaches beyond the stock panel's edge (covers its antialiased edge), the soft fringe outside that, and how far the OPEN patch reaches up under our card (hidden by it). */
    public static final float EDGE_DP = 2f, FEATHER_DP = 8f, OVERLAP_DP = 3f;

    /**
     * The slot window {left, top, right, bottom} in display px: the learned panel's left / right / bottom plus the margin (edge + feather), starting one feather width ABOVE the card's bottom edge (room for the soft
     * top edge of the RESTING patch; while the menu is open that strip is transparent and the card covers the rest). Clamped to the display.
     */
    public static int[] slotWindow(int panelLeft, int cardBottom, int panelRight, int panelBottom, int edge, int feather, int dispW, int dispH) {
        return slotWindow(panelLeft, cardBottom, panelRight, panelBottom, edge, feather, 0, feather, dispW, dispH);   // the v0.8.2 geometry: no extension, the bottom fringe as wide as the others
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // BB1 (Tj, 2026-11-07, v0.8.2, after the square itself became invisible: "a very slight tinted line about where the bottom of the card would be. Like a faded tint"). The stock panel casts an ELEVATION SHADOW downward
    // (a dark band 6 to 10 levels deep right under its real bottom edge that fades out over ~15 to 30 px; measured in Tj's photos 2026-11-07 00:20 / 00:21, TASKS BB1a). The opaque core reached only EDGE_DP past the panel and
    // then faded out, so the shadow showed through the fringe and below the window. The core now reaches EXTRA_BOTTOM_DP further DOWN (the sides keep their margin: the shadow is cast downward) and the bottom fringe may be
    // shorter. The stored real-pixel crop is the window + PatchShot.MARGIN_PX on every side, so the extension is clamped to stay inside it (no new calibration screenshot for existing installs).
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * dp: how much further down than the old window the opaque core reaches (default; the hidden preference {@code patch_extra_dp} overrides it, 0 = the v0.8.2 geometry), and the width of the BOTTOM fringe (the
     * hidden preference {@code patch_bfeather_dp}; the sides keep FEATHER_DP). The slot interior now matches to about half a level (BA1), so a short bottom fringe is enough, and the deeper core covers more of the
     * stock shadow inside the same stored crop: at 170 dpi the core reaches 2 + 26 = 28 px below the panel, the window 32 px (the crop allows 35, minus the slack).
     */
    public static final float EXTRA_BOTTOM_DP = 24f, BOTTOM_FEATHER_DP = 4f;
    /** The hidden preferences (dp): the extension and the width of the bottom fringe. Not in Launcher control: the emulator section {@code pt} (phase D) sets them to compare geometries. */
    public static final String EXTRA_KEY = "patch_extra_dp", BOTTOM_FEATHER_KEY = "patch_bfeather_dp";
    /** px of slack kept inside the stored crop: the learned panel's bottom can wobble by a pixel between scans, and the crop must still cover the window. */
    public static final int CROP_SLACK_PX = 3;

    /** The slot window with a taller bottom: the core reaches {@code extraBottom} px past the usual edge, then a fringe of {@code bottomFeather} px. */
    public static int[] slotWindow(int panelLeft, int cardBottom, int panelRight, int panelBottom, int edge, int feather, int extraBottom, int bottomFeather, int dispW, int dispH) {
        int m = edge + feather;
        int mb = edge + Math.max(0, extraBottom) + Math.max(0, bottomFeather);
        return new int[] { Math.max(0, panelLeft - m), Math.max(0, cardBottom - feather), Math.min(dispW, panelRight + m), Math.min(dispH, panelBottom + mb) };
    }

    /**
     * The extension in px for a wish in dp: never negative, and never more than the stored crop's margin minus the slack, so the window stays inside the crop that was cut for the OLD window (margin = the crop's
     * extra rows on every side). The bottom fringe eats into the same margin: pass what is left after it ({@code extra + bottomFeather - oldFeather <= margin - slack}).
     */
    public static int clampExtra(int wantedPx, int bottomFeatherPx, int oldFeatherPx, int cropMarginPx) {
        int room = cropMarginPx - CROP_SLACK_PX - (bottomFeatherPx - oldFeatherPx);
        return Math.max(0, Math.min(wantedPx, room));
    }

    /**
     * 0..255: how much of the patch shows at (x, y) of its window (window coordinates). The core [cl, cr) x [ct, cb) is fully opaque; fl / ft / fr / fb are the widths of the soft fringe OUTSIDE it on each side
     * (0 = a hard edge: nothing shows beyond it). The two directions multiply, so a corner fades in both; the ramp is a smoothstep so there is no visible slope break.
     */
    public static int alphaAt(int x, int y, int cl, int ct, int cr, int cb, int fl, int ft, int fr, int fb) {
        float ax = 1f, ay = 1f;
        if (x < cl) ax = ramp(cl - x, fl); else if (x >= cr) ax = ramp(x - cr + 1, fr);
        if (y < ct) ay = ramp(ct - y, ft); else if (y >= cb) ay = ramp(y - cb + 1, fb);
        return Math.round(255f * ax * ay);
    }

    /**
     * BC1 (Tj, 2026-11-07, v0.8.3: "a very small vertical sliver all the way to the right ... Can the wallpaper patch be made to cover all the way to the right edge of the screen?"). The stock card sits 4 px from the
     * display edge (596 of 600) and the stock menu draws a thin light line in that gap along the card's whole height. The patch core ended 2 px past the card (598) and the window is clamped at the display edge (600),
     * so the fringe was {@code min(FEATHER, 600 - 598)} = 2 px wide: 74 % / 26 % of the stock line showed through the last two columns of the hidden slot's rows (the sliver in Tj's video). A fringe exists to blend into
     * the REAL lock screen next to the patch; on a side that IS the display edge there is nothing next to the patch, so that side's core runs to the edge and has no fringe.
     * Returns {cl, cr, cb, fl, fr, fb} (window coordinates): the left, right and bottom sides of the window that lie on the display edge are snapped; the others are returned unchanged. (The top side belongs to the card.)
     *
     * @param winLeft / winRight / winBottom the window's edges in screen px; {@code w}, {@code h} its size
     */
    public static int[] coverDisplayEdges(int winLeft, int winRight, int winBottom, int dispW, int dispH, int w, int h, int cl, int cr, int cb, int fl, int fr, int fb) {
        if (winLeft <= 0) { cl = 0; fl = 0; }
        if (winRight >= dispW) { cr = w; fr = 0; }
        if (winBottom >= dispH) { cb = h; fb = 0; }
        return new int[] { cl, cr, cb, fl, fr, fb };
    }

    private static float ramp(int dist, int width) {
        if (width <= 0) return 0f;
        float t = 1f - dist / (float) (width + 1);
        if (t <= 0f) return 0f;
        return t * t * (3f - 2f * t);
    }

    /**
     * Are two patch windows the same for all practical purposes? The slot window follows the card's bottom edge, which is measured on every menu scan and can wobble by a pixel; rebuilding the patch for that
     * would itself flicker. Each side may differ by up to {@code tol} px.
     */
    public static boolean sameWindow(int[] a, int[] b, int tol) {
        if (a == null || b == null || a.length != 4 || b.length != 4) return false;
        for (int i = 0; i < 4; i++) if (Math.abs(a[i] - b[i]) > tol) return false;
        return true;
    }

    /** The mask (w * h bytes, row-major, 0..255 as unsigned) for {@link #alphaAt}. */
    public static byte[] mask(int w, int h, int cl, int ct, int cr, int cb, int fl, int ft, int fr, int fb) {
        byte[] m = new byte[Math.max(0, w) * Math.max(0, h)];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) m[y * w + x] = (byte) alphaAt(x, y, cl, ct, cr, cb, fl, ft, fr, fb);
        return m;
    }

    /**
     * AH1: brightness the patch keeps while the stock power dialog is open. The dialog dims everything behind it ({@code stock} = the share of brightness that is kept, 0.79 measured on the emulator); the patch is drawn
     * ABOVE that scrim, so it must be dimmed the same or it shows as a lighter block (Tj: the patch "obviously doesn't blend in on the home screen"). {@code nudgePct} is Tj's own correction for his tablet.
     */
    public static float openScrim(float stock, int nudgePct) {
        float k = stock * (1f + nudgePct / 100f);
        return Math.max(0.3f, Math.min(1f, k));
    }

    /**
     * BT1a (Tj, 2026-11-11: "the first time I pull up to enter the pin from the lock screen, the patch for the power menu shows"). The finger's DOWN hides a RESTING patch at once, but a patch that only BECOMES
     * restable during the pull-up (its window was created late, the lock screen came up in Quickstep mode and the custom mode took over, the crop was loaded, the PIN pad went away) was never hidden by anything: it
     * appeared over the darkening wallpaper. It now rests visibly only after it has been restable for this long. Not a loss: the stock menu's own window event shows a restable patch that is merely not visible yet
     * in the same frame (the fast path, BE1), and nobody opens the menu within a second of the lock screen coming up.
     */
    public static final long SETTLE_MS = 1200;

    /** The patch has been restable for {@link #SETTLE_MS}. {@code restableSinceMs} = uptime ms of the moment it became restable, 0 = it is not restable. */
    public static boolean settled(long nowMs, long restableSinceMs) {
        return restableSinceMs > 0 && nowMs - restableSinceMs >= SETTLE_MS;
    }

    /** Milliseconds until the patch counts as settled (0 = now, or not restable). For the timer that looks again. */
    public static long settleLeftMs(long nowMs, long restableSinceMs) {
        if (restableSinceMs <= 0) return 0;
        return Math.max(0, SETTLE_MS - (nowMs - restableSinceMs));
    }

    /**
     * Review B #1 (BT1, 2026-11-11): ONE return time for a lock patch that is restable but not visible: the longest of the settle time, the PIN-pad hold and the finger hold (0 = it may be visible now). The three
     * return runnables (settle, padBack, touchBack) each knew only their own deadline; a pad that was up for less than 0.9 s let padBack run BEFORE the hold ended and nothing looked again (the patch stayed away until
     * the next finger). patchShow() posts this one time every time it leaves a restable patch invisible, so the answer is re-derived from the books, never trusted from an earlier timer.
     */
    public static long returnInMs(long nowMs, long restableSinceMs, long holdUntilMs, long touchUntilMs) {
        long left = settleLeftMs(nowMs, restableSinceMs);
        left = Math.max(left, holdUntilMs - nowMs);
        left = Math.max(left, touchUntilMs - nowMs);
        return Math.max(0, left);
    }
}
