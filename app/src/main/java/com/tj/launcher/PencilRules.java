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

    /** The cover goes now: the followed pencil was lost, or a whole-tree search ran and found none. Never because nothing could be checked (no node and no search allowed): that leaves the cover as it is. */
    public static boolean hideCover(int followed, boolean searched, boolean foundByFullSearch) {
        if (foundByFullSearch) return false;
        return followed == LOST || searched;
    }

    /** While the cover is up it is re-checked this often even without an event (the events stop when the panel stops moving, and the last one may come before the pencil is hidden). One node refresh each: cheap. */
    public static final long WATCH_MS = 150;

    /** A whole-tree search is not held back by the spacing gap right after a followed pencil was lost (it may be a transient failure; the cover is already hidden, the pencil must be re-covered promptly). */
    public static boolean searchGapWaived(int followed) { return followed == LOST; }
}
