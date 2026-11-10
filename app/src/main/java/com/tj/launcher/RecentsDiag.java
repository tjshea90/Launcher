package com.tj.launcher;

/**
 * BN8f1 (second BN8 review, 2026-11-10): the pure part of the Recents failure diagnostic. The diagnostic describes the stock launcher's window when the clean-up finds nothing, and the tablet report that carries it is
 * PUBLISHED, while Recents card text is app labels and conversation titles. So no node text is ever copied: a node's text is only matched against two FIXED words, and only counts leave the service
 * (JVM-tested: RecentsDiagTest).
 */
final class RecentsDiag {
    private RecentsDiag() {}

    static final int NONE = 0, CLEAR_ALL = 1, NO_ITEMS = 2;

    /** Which fixed Recents word this node text is: the button ("Clear all") or the empty-state line ("No recent items"). A long text is a message, not a button. */
    static int word(CharSequence text) {
        if (text == null || text.length() == 0 || text.length() > 80) return NONE;
        String s = text.toString().toLowerCase(java.util.Locale.ROOT);
        if (s.contains("clear all")) return CLEAR_ALL;
        if (s.contains("no recent items")) return NO_ITEMS;
        return NONE;
    }

    /** Is this the label of the accessibility action the clean-up presses on a Recents card ("Close", the same test as recentsEmpty)? The diagnostic reports only HOW MANY nodes carry it, never a label. */
    static boolean isCloseLabel(CharSequence label) {
        return label != null && "close".equalsIgnoreCase(label.toString().trim());
    }
}
