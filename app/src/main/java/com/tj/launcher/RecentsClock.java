package com.tj.launcher;

/**
 * CW1 D4 (research/CW1_AUDIT_D_STOCK_SLEEP_2026-11-22.md section 4): the clock of the Recents clear-all of a return reset. Until now every clear-all loop gave itself a fixed 2.5 s (1.8 s for the plain Clear all press)
 * from the moment it pressed the Recents key, so a COLD start of the stock launcher's Recents screen (its process at the floor after a restart, a new Activity to inflate) came out of the same budget; a miss left
 * the cards of apps opened in Quickstep in the custom Recents (BN8d history: "All of my quickstep apps were in the recent apps list in the custom launcher").
 *
 * <p>Now the budget starts at the FIRST SIGN of the stock Recents (a window of the stock package in the loop's own look, or, for the Close-by-label loop, in its card search), with a hard cap from the key press
 * ({@link #CAP_MS}). A key press the system refused ({@code keyAccepted} false) gets the old rule: nothing will come, so nothing is waited for. Every loop still checks {@link #over} FIRST on every look (the BE3 rule:
 * a loop that returns early after pressing something and never reaches its deadline starves the main thread); this class only moves the deadline, it never removes it. Pure, uptime-clock milliseconds in, no Android.
 */
final class RecentsClock {
    /** The longest a clear-all loop may run from the key press, whatever it sees. */
    static final long CAP_MS = 6_000L;
    /** How long the reset cover's failsafe is kept beyond the clock's deadline when the clock outran the cover's own 4.5 s. */
    static final long COVER_MARGIN_MS = 2_500L;

    private final long startMs, budgetMs;
    private final boolean extend;
    private long signMs = 0;      // 0 = no stock window seen yet
    private boolean coverKept = false;

    /** {@code startMs}: the uptime of the Recents key press. {@code budgetMs}: the old fixed budget. {@code keyAccepted}: the press was accepted (else nothing is waited for beyond the old rule). */
    RecentsClock(long startMs, long budgetMs, boolean keyAccepted) { this.startMs = startMs; this.budgetMs = budgetMs; this.extend = keyAccepted; }

    /** Pure: the deadline of a loop. {@code signMs} 0 = no stock window seen yet. */
    static long deadline(long startMs, long budgetMs, boolean extend, long signMs) {
        long old = startMs + budgetMs;
        if (!extend) return old;                                     // the key was refused (or this is a fallback after the cap): the old rule
        long cap = startMs + CAP_MS;
        if (signMs <= 0) return Math.max(old, cap);                  // nothing yet: wait for the first sign, up to the cap
        return Math.min(Math.max(old, signMs + budgetMs), Math.max(old, cap));   // the budget starts at the sign; never later than the cap (and never earlier than the old rule)
    }

    /** The first look that saw a window of the stock launcher. Later calls change nothing. */
    void sign(long nowMs) { if (signMs <= 0) signMs = Math.max(1L, nowMs); }

    boolean signSeen() { return signMs > 0; }

    long deadline() { return deadline(startMs, budgetMs, extend, signMs); }

    /** The deadline check every look starts with. */
    boolean over(long nowMs) { return nowMs >= deadline(); }

    /** The old fixed deadline has passed: the clock is (or may be) running longer than the loops used to. */
    boolean pastOldBudget(long nowMs) { return nowMs >= startMs + budgetMs; }

    /** True once, the first time it is asked after {@link #pastOldBudget}: the caller keeps the cover up. */
    boolean takeCoverKeep(long nowMs) {
        if (coverKept || !extend || !pastOldBudget(nowMs)) return false;
        coverKept = true;
        return true;
    }

    /** How long a cover must still live for this clock to finish behind it (the clock's remaining time plus a margin). */
    long coverFor(long nowMs) { return Math.max(0L, deadline() - nowMs) + COVER_MARGIN_MS; }

    /** The Recents key press to the first sign of the stock Recents, in ms; -1 = none seen. */
    long coldStartMs() { return signMs > 0 ? signMs - startMs : -1L; }

    /** For the lock log line of the clear-all. */
    String describe(long nowMs) {
        return signMs > 0 ? "cold start +" + (signMs - startMs) + " ms" : "no stock window seen in " + Math.max(0L, nowMs - startMs) + " ms";
    }

    /** The plain Clear all press after the card search found nothing: the screen is (or was waited for) already, so the old fixed rule from now. */
    RecentsClock fallback(long nowMs, long budgetMs) { return new RecentsClock(nowMs, budgetMs, false); }
}
