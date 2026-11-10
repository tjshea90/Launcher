package com.tj.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * B4 (Tj, 2026-11-03: "only close apps if there are apps to close"): has any app other than Solitaire been opened since the last time Recents was cleared?
 * The accessibility helper marks it when an unrelated app's window appears (while Quickstep is on, or when the launch-flag method is off, because only then do other
 * apps end up in Recents); a successful Recents "Clear all" resets it. If it is clean, the double-Home reset skips the Recents step entirely.
 * Unknown (never tracked) counts as dirty: the first reset always clears.
 */
public final class RecentsDirty {
    private static final String PREFS = "tj_dirty";
    private RecentsDirty() {}

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public static boolean isDirty(Context c) { return p(c).getBoolean("dirty", true); }
    public static void mark(Context c) { if (!isDirty(c)) p(c).edit().putBoolean("dirty", true).commit(); }
    public static void clear(Context c) { p(c).edit().putBoolean("dirty", false).commit(); }

    /** BN8e: at most one DIRTY line per package per minute in the lock log (ordinary use of Quickstep apps wrote about six a minute and pushed everything else out of the report's excerpt). */
    public static final long LOG_GAP_MS = 60_000L;

    /** Pure rule with the caller's own map: may the DIRTY line of {@code pkg} be written now? Bounded (32 packages), and a clock that went backwards logs again. */
    public static boolean shouldLog(java.util.Map<String, Long> last, String pkg, long nowMs, long gapMs) {
        Long at = last.get(pkg);
        if (at != null && nowMs >= at && nowMs - at < gapMs) return false;
        if (at == null && last.size() >= 32) last.clear();
        last.put(pkg, nowMs);
        return true;
    }

    /** BN8d, pure: is a window whose root belongs to {@code pkg} one of the stock launcher's (the Recents screen lives there)? {@code stockPkg} = the resolved stock launcher, may be null. */
    public static boolean isStockLauncherWindow(String pkg, String stockPkg) {
        if (pkg == null || pkg.isEmpty() || "-".equals(pkg)) return false;
        return pkg.startsWith("com.android.launcher3") || pkg.equals(stockPkg);
    }

    /**
     * BN8d, pure: the diagnosis line of a Recents clean-up that found nothing. The stock launcher's windows come FIRST and are never pushed out by the others: the line is cut to {@code max} characters from the END
     * (the report cuts every value at 600), so what is lost is the list of the other windows, not the node classes and texts of the Recents screen itself.
     */
    public static String diagLine(String head, String stock, String others, int max) {
        String s = (head == null ? "" : head) + "; stock launcher:" + (stock == null || stock.isEmpty() ? " NO WINDOW" : stock) + "; others:" + (others == null || others.isEmpty() ? " none" : others);
        return max > 0 && s.length() > max ? s.substring(0, max) : s;
    }

    /** Pure rule (JVM-testable): does a window of {@code pkg} appearing mean Recents may now hold another app? */
    public static boolean shouldMark(String pkg, String ourPkg, String stockLauncherPkg, String imePkg, String targetPkg, boolean otherAppsLandInRecents) {
        if (!otherAppsLandInRecents || pkg == null || pkg.isEmpty()) return false;
        if (pkg.equals(ourPkg) || pkg.equals(stockLauncherPkg) || pkg.equals(imePkg) || pkg.equals(targetPkg)) return false;
        return !(pkg.equals("android") || pkg.equals("com.android.systemui") || pkg.startsWith("com.android.inputmethod")
                || pkg.equals("com.android.permissioncontroller") || pkg.equals("com.google.android.permissioncontroller"));
    }
}
