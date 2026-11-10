package com.tj.launcher;

/**
 * BN4 (Tj, 2026-11-09 03:02 UTC, about v0.8.12: "on the lock screen, the pencil hiding works but as soon as I slide the notification panel back up I see a white box on the lock screen"): the rules for the cover
 * over the quick-settings "Edit" pencil (JVM-testable; the service only wires them).
 *
 * The cover is an OPAQUE white window at the pencil's bounds. It follows ONE known accessibility node. When SystemUI hides the pencil (the quick settings collapse: alpha 0 or INVISIBLE; an INVISIBLE view
 * cannot be refreshed at all) the node is lost; the old code then kept the cover where it was until the next allowed whole-tree search (200 ms gap + the search, up to ~500 ms on the tablet): invisible on the
 * white panel, a white box on the lock screen once the panel had slid away. A lost pencil now takes the cover away AT ONCE and the search that may bring it back runs promptly.
 */
public final class PencilRules {
    private PencilRules() {}

    /** What following the known node gave. */
    public static final int NO_KNOWN = 0, FOUND = 1, LOST = 2;

    /**
     * @param hadKnown   a node was being followed
     * @param refreshed  {@code refresh()} succeeded
     * @param visible    the refreshed node is visible to the user
     * @param boundsOk   its bounds are those of a pencil (non-empty, smaller than 400 px)
     */
    public static int follow(boolean hadKnown, boolean refreshed, boolean visible, boolean boundsOk) {
        if (!hadKnown) return NO_KNOWN;
        return refreshed && visible && boundsOk ? FOUND : LOST;
    }

    /** The cover goes now: the followed pencil was lost, or a whole-tree search ran and found none. Never because nothing could be checked (no node and no search allowed): that leaves the cover as it is. (Called only when nothing was found.) */
    public static boolean hideCover(int followed, boolean searched) {
        return followed == LOST || searched;
    }

    /** While the cover is up it is re-checked this often even without an event (the events stop when the panel stops moving, and the last one may come before the pencil is hidden). One node refresh each: cheap. */
    public static final long WATCH_MS = 150;

    /**
     * A whole-tree search is not held back by the spacing gap right after a followed pencil was lost (it may be a transient failure; the cover is already hidden, the pencil must be re-covered promptly) - but only when
     * that call did NOT already search: a search that found nothing is the answer, a second one 30 ms later (up to ~500 ms of main thread on the tablet) cannot find more (code review 2026-11-09).
     */
    public static boolean searchGapWaived(int followed, boolean fullSearchRan) { return followed == LOST && !fullSearchRan; }

    /**
     * BN8b (Tj, 2026-11-10, about v0.9.0: "The lock screen still flashed a white box after I pulled down [and] returned the notification panel for the pencil hider"). The BN4 fix above takes the cover away when the pencil is LOST,
     * but while the panel slides UP the pencil is still found, a little higher at every look, and the cover FOLLOWED it with the lag of the look (an event every 40 ms or a whole-tree search every 200 ms, then a relayout and a
     * frame): a white box trailing behind the retreating panel, over the lock screen (the tablet report counts 355 whole-tree searches against 26 one-node follow-ups). A cover cannot hide a pencil that moves that fast, so while the
     * pencil goes UP the cover stays away, and it comes back when two looks in a row agree (the panel is at rest, say half open). Going DOWN (the panel opening) is followed as before: there the box trails INSIDE the white panel.
     */
    public static final class SlideGuard {
        public static final int NONE = Integer.MIN_VALUE;
        public static final int SLIDE_UP_PX = 3;          // less than this is jitter, not a slide
        public static final long HOLD_MAX_MS = 3000;      // a safety valve: the cover is never kept away for longer than this in one go
        public static final long STALE_MS = 800;          // no look for this long = a new pull of the panel, nothing is known about it yet
        public static final long MIN_LOOK_GAP_MS = 40;    // BN8f2: two looks closer than this can read the SAME SystemUI frame: they do not count as "the looks agree"
        public static final long RELOOK_MS = 60;          // while the cover is held away the next look is due this soon (the events stop when the panel stops)
        private int lastTop = NONE;
        private long lastMs, holdSince;
        private boolean holding;

        /** A look found the pencil with its top edge at {@code top} at {@code nowMs}. True = the cover may be shown or followed now; false = keep it away (and look again soon). */
        public boolean allowCover(int top, long nowMs) {
            if (lastTop != NONE && nowMs - lastMs > STALE_MS) reset();
            boolean up = lastTop != NONE && top < lastTop - SLIDE_UP_PX;
            boolean close = lastTop != NONE && nowMs - lastMs < MIN_LOOK_GAP_MS;   // BN8f2: a look right after another one can read the SAME frame ...
            boolean still = lastTop == top && !close;                              // ... so it does not confirm that the panel is at rest ...
            if (up || !close) { lastTop = top; lastMs = nowMs; }                  // ... and it does not move the anchor (a move UP it still shows)
            if (holding && nowMs - holdSince >= HOLD_MAX_MS) { holding = false; return true; }
            if (up) { if (!holding) { holding = true; holdSince = nowMs; } return false; }
            if (holding) { if (still) { holding = false; return true; } return false; }
            return true;
        }

        public boolean holding() { return holding; }
        public void reset() { lastTop = NONE; holding = false; }
    }

    /**
     * BN8b, second half: the FAST watch. The guard above only acts at a LOOK, and the looks were too far apart for the START of a slide: SystemUI sends content events at most every 100 ms, the slow watch runs every
     * {@link #WATCH_MS}, so the cover could sit at its old place for about 0.1 s after the panel had begun to leave (and a first look that saw the pencil only 1-3 px higher "followed" it and then waited for the next event).
     * While something may be moving the panel (a finger went down anywhere: ACTION_OUTSIDE on the cover; SystemUI is redrawing; the pencil was just found at a new place) the covered pencil is looked at every
     * {@link #FAST_WATCH_MS} for {@link #FAST_FOR_MS}: a slide is then noticed within about two frames. One node refresh per look, and only while the cover is up.
     */
    public static final long FAST_WATCH_MS = 25;
    public static final long FAST_FOR_MS = 600;

    /** When the next look at a shown cover is due. Fast while {@code nowMs} is before {@code fastUntilMs}; a deadline that makes no sense (further away than {@link #FAST_FOR_MS}: a clock jump) counts as slow. Pure. */
    public static long watchGapMs(long nowMs, long fastUntilMs) {
        long left = fastUntilMs - nowMs;
        return left > 0 && left <= FAST_FOR_MS ? FAST_WATCH_MS : WATCH_MS;
    }
}
