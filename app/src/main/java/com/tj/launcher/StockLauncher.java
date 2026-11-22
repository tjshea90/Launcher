package com.tj.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;

/** Finds the device's own launcher (Quickstep) at runtime. Nothing is hard-coded except a preference for com.android.launcher3. */
public final class StockLauncher {
    private StockLauncher() {}

    /** Pure choice rule, testable on the JVM: skip ourselves, prefer a system app, prefer com.android.launcher3, else first. */
    public static ActivityInfo choose(List<ActivityInfo> homes, String selfPackage) {
        ActivityInfo best = null;
        int bestScore = -1;
        for (ActivityInfo a : homes) {
            if (a == null || selfPackage.equals(a.packageName)) continue;
            int score = 1;
            if (a.applicationInfo != null && (a.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) score += 2;
            if ("com.android.launcher3".equals(a.packageName)) score += 4;
            if (score > bestScore) { bestScore = score; best = a; }
        }
        return best;
    }

    /** A9 (CW1): how long {@link #cached} keeps an answer (the service's own {@code stockCached()} uses the same minute). */
    static final long CACHE_MS = 60_000;

    /** Pure (A9): is an answer taken at {@code atMs} still good at {@code nowMs}? Only a FOUND answer is kept (a failed lookup is retried at once); a clock that went backwards never keeps one. */
    static boolean cacheFresh(boolean haveAnswer, long atMs, long nowMs) { return haveAnswer && nowMs >= atMs && nowMs - atMs <= CACHE_MS; }

    private static volatile ComponentName cachedAnswer;
    private static volatile long cachedAt;

    /**
     * A9 (CW1): {@link #find} at most once a minute, for the planning code that asks again and again (return clean-up, usage marks, the wallpaper picker). A stale answer only matters if another launcher was installed or
     * removed within the last minute. NOT for the Router's start or any emergency exit: those use {@link #find} (fresh).
     */
    public static ComponentName cached(Context c) {
        long now = android.os.SystemClock.elapsedRealtime();
        ComponentName have = cachedAnswer;
        if (cacheFresh(have != null, cachedAt, now)) return have;
        ComponentName fresh = find(c);
        if (fresh != null) { cachedAnswer = fresh; cachedAt = now; }
        return fresh;
    }

    public static ComponentName find(Context c) {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> ris = c.getPackageManager().queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY);
        java.util.ArrayList<ActivityInfo> infos = new java.util.ArrayList<>();
        for (ResolveInfo ri : ris) infos.add(ri.activityInfo);
        ActivityInfo a = choose(infos, c.getPackageName());
        return a == null ? null : new ComponentName(a.packageName, a.name);
    }
}
