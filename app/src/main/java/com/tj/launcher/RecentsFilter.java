package com.tj.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * AP1 (Tj, 2026-11-06: "The custom launcher occasionally had apps in the recent apps list which were from quickstep. If it makes it any more effective, make it so the only apps that can ever show up in the recent
 * apps list for the custom launcher are those apps in the custom launcher app drawer. No hidden apps should ever show up in the recent apps list for the custom launcher.")
 * The return clean-up ({@link ReturnReset}) removes what it KNOWS was opened in Quickstep; an app it missed (an event that never arrived, a usage log that lagged) stayed in Recents. This is the second, independent
 * line: whenever the Recents screen opens in the custom launcher, the cards of every app that is NOT in the custom drawer (the hidden apps, and apps opened in Quickstep that have no drawer entry) are closed by
 * their own "Close" action, whatever the tracking says. Cards are matched by the APP LABEL of known non-drawer apps only (a card that matches nothing is left alone: a browser card is titled with the page, and
 * closing it by mistake would be worse than leaving it). Fail-open: any problem leaves Recents as it is.
 */
final class RecentsFilter {
    private static final String TAG = "TjRecentsFilter";
    static final String KEY = "recents_drawer_only";        // switch, default ON
    static final String STATUS_KEY = "recents_guard_status"; // what the guard did last (Launcher control and the tablet report)
    static final long LABELS_TTL_MS = 60_000;

    private final TjAccessibilityService svc;
    private final android.os.Handler handler;
    // BY1a C1: the label set is built on a background thread (it was built on the main thread when Recents opened); these fields are touched on the main thread only, the worker posts its result
    private Set<String> blocked = null;   // null = never built
    private long blockedAt = 0;           // uptime when the build that made it started
    private boolean building = false;     // a build runs
    private boolean lookWaits = false;    // a look waits for that build (it counts as pending)
    private boolean pending = false;
    private int rounds = 0, looks = 0;
    /** BE2: the first look comes 60 ms after the Recents window opened (it was 150 ms); the cards are closed in ONE pass (it was one card per 70 ms), so a hidden app's card is on screen for as short a time as the tree allows. */
    static final long FIRST_LOOK_MS = 60;
    private static final long[] LOOK_DELAYS_MS = { 450, 700, 1200, 1500 };   // after the first look: looks at about 0.5 s, 1.2 s, 2.4 s and 3.9 s
    private long closedTotal = 0;

    RecentsFilter(TjAccessibilityService svc, android.os.Handler handler) { this.svc = svc; this.handler = handler; }

    // ---------------------------------------------------------------- pure rules (JVM-tested)

    /** Pure: does the guard work now? Only in the custom launcher, with its switch on, the tablet unlocked, the crash breaker not tripped and no return reset driving Recents itself. */
    static boolean wanted(boolean custom, boolean switchOn, boolean locked, boolean automationOff, boolean resetRunning) {
        return custom && switchOn && !locked && !automationOff && !resetRunning;
    }

    /**
     * Pure: the labels whose Recents cards must go = the hidden apps' labels plus the labels of apps opened in Quickstep that are not in the drawer, MINUS every label a drawer app also has (a card titled like a drawer
     * app is never closed).
     */
    static Set<String> blockedLabels(Set<String> hiddenLabels, Set<String> quickstepLabels, Set<String> drawerLabels) {
        Set<String> out = new HashSet<>();
        if (hiddenLabels != null) out.addAll(lower(hiddenLabels));
        if (quickstepLabels != null) out.addAll(lower(quickstepLabels));
        if (drawerLabels != null) out.removeAll(lower(drawerLabels));
        return out;
    }

    private static Set<String> lower(Set<String> in) {
        Set<String> r = new HashSet<>();
        for (String s : in) if (s != null && !s.trim().isEmpty()) r.add(s.trim().toLowerCase(Locale.ROOT));
        return r;
    }

    /** Pure: which blocked label does this card description start with (the same matching as the return clean-up), or null. */
    static String matchingLabel(String description, Set<String> blockedLower) {
        if (description == null || blockedLower == null) return null;
        String d = description.trim().toLowerCase(Locale.ROOT);
        if (d.isEmpty()) return null;
        String best = null;
        for (String x : blockedLower) {
            if (x == null || x.isEmpty()) continue;
            if (d.equals(x) || d.startsWith(x + ",") || d.startsWith(x + " ")) { if (best == null || x.length() > best.length()) best = x; }
        }
        return best;
    }

    /**
     * Pure: can this event mean "the Recents screen just opened"? A window change of the stock launcher (it hosts Quickstep's Recents), or of SystemUI ONLY when the window says "Recents": SystemUI also owns the
     * shade and the quick settings, and a scan there (a tree walk run by SystemUI's own UI thread) is exactly what must not happen while Tj taps a tile (AN1).
     */
    static boolean mayOpenRecents(int eventType, String pkg, String stockPkg, CharSequence className) {
        if (pkg == null) return false;
        boolean windowChange = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        if (!windowChange) return false;
        if (pkg.equals(stockPkg) || pkg.startsWith("com.android.launcher3")) return true;
        return "com.android.systemui".equals(pkg) && className != null && className.toString().toLowerCase(Locale.ROOT).contains("recents");
    }

    /** BY1a C1 (pure): may a look use the set built at {@code builtAtMs}? Never built = no; older than the TTL = no (the same 60 s rule as before). */
    static boolean labelsFresh(boolean built, long builtAtMs, long nowMs) { return built && nowMs - builtAtMs <= LABELS_TTL_MS; }

    /** BY1a C1 (pure): start a background build? Only for a set that is not fresh, and never a second one at a time. */
    static boolean buildNeeded(boolean fresh, boolean building) { return !fresh && !building; }

    /** BY1a C1 (pure): the end of a build runs the look that waited for it, unless the build failed (that look ends, as a failed scan ended it before; the next Recents open tries again). */
    static boolean lookAfterBuild(boolean lookWaited, boolean built) { return lookWaited && built; }

    // ---------------------------------------------------------------- Android side

    /** Called for every accessibility event; cheap (a few comparisons) unless a window of the stock launcher / SystemUI changed. Never throws. */
    void onEvent(AccessibilityEvent e) {
        try {
            if (e == null || pending) return;
            CharSequence pkg = e.getPackageName();
            if (pkg == null || !mayOpenRecents(e.getEventType(), pkg.toString(), svc.stockPackageOrNull(), e.getClassName())) return;
            // BE2: with the shell helper the cards of non-drawer apps are removed by id (no animation, no card to see); the label-based closing below is the fallback when no helper answers
            if (Mode.isCustom(svc) && !svc.keyguardLockedNow() && !svc.resetDrivesRecents()) RecentsSweep.requestAsync(svc.getApplicationContext(), "the Recents screen opened", RecentsSweep.HIDDEN_ONLY, null, 2500);
            if (!wanted(Mode.isCustom(svc), Features.get(svc, KEY, true), svc.keyguardLockedNow(), CrashGuard.automationOff(svc), svc.resetDrivesRecents())) return;
            schedule(FIRST_LOOK_MS);
            rounds = 0; looks = 0;
            if (buildNeeded(labelsFresh(blocked != null, blockedAt, android.os.SystemClock.uptimeMillis()), building)) build();   // BY1a C1: start at the event; the first look finds the set ready or waits for it
        } catch (Throwable t) { Log.e(TAG, "recents guard event failed", t); }
    }

    private void schedule(long delayMs) {
        pending = true;
        handler.postDelayed(run, delayMs);
    }

    private final Runnable run = new Runnable() { @Override public void run() { look(false); } };

    /** BY1a C1: builds the label set on the worker; the main thread gets it with {@link #built}. Never throws. */
    private void build() {
        building = true;
        final long startedAt = android.os.SystemClock.uptimeMillis();
        try {
            Labels.EX.execute(new Runnable() { @Override public void run() {
                Set<String> s = null;
                try { s = blockedNow(svc); } catch (Throwable t) { Log.e(TAG, "could not build the labels", t); }
                if (s != null) Log.i(TAG, "labels built in " + (android.os.SystemClock.uptimeMillis() - startedAt) + " ms on " + Thread.currentThread().getName() + ", " + s.size() + " blocked");
                final Set<String> result = s;
                handler.post(new Runnable() { @Override public void run() { built(result, startedAt); } });
            } });
        } catch (Throwable t) { Log.e(TAG, "could not start the label build", t); built(null, startedAt); }
    }

    /** BY1a C1: the end of a build (main thread). */
    private void built(Set<String> s, long startedAt) {
        building = false;
        if (s != null) { blocked = s; blockedAt = startedAt; }
        boolean waited = lookWaits;
        lookWaits = false;
        if (lookAfterBuild(waited, s != null)) look(true);
        else if (waited) pending = false;
    }

    /** One look at the Recents cards (main thread). {@code justBuilt}: the set was built for this look and is used whatever its age. */
    private void look(boolean justBuilt) {
        pending = false;
        try {
            if (!wanted(Mode.isCustom(svc), Features.get(svc, KEY, true), svc.keyguardLockedNow(), CrashGuard.automationOff(svc), svc.resetDrivesRecents())) return;
            boolean fresh = labelsFresh(blocked != null, blockedAt, android.os.SystemClock.uptimeMillis());
            if (!justBuilt && !fresh) {   // BY1a C1: missing or older than the TTL: never used; the worker builds it and this look runs again with it (the set the scan here gave before)
                pending = true; lookWaits = true;
                if (buildNeeded(false, building)) build();
                return;
            }
            Set<String> b = blocked;
            if (b == null || b.isEmpty()) return;
            java.util.List<String> closedNow = svc.closeRecentsCards(b);
            if (!closedNow.isEmpty()) {
                closedTotal += closedNow.size();
                String what = closedNow.size() == 1 ? "closed a Recents card of '" + closedNow.get(0) + "'" : "closed " + closedNow.size() + " Recents cards, '" + closedNow.get(0) + "' and " + (closedNow.size() - 1) + " more";
                String line = stamp() + "  " + what + " (not in the drawer); " + closedTotal + " card(s) closed since the helper connected";
                Features.setString(svc, STATUS_KEY, line);
                LockLog.add(svc.getApplicationContext(), "RECENTS", "closed " + closedNow.size() + " card(s), '" + closedNow.get(0) + "'" + (closedNow.size() > 1 ? " and " + (closedNow.size() - 1) + " more" : "") + ": not an app of the custom drawer");
                if (++rounds < 12) schedule(70);          // the cards animate away; look again for late ones
            } else if (looks < LOOK_DELAYS_MS.length) {
                schedule(LOOK_DELAYS_MS[looks++]);        // the cards of a freshly opened Recents screen can arrive late (a busy tablet / emulator): a few more looks over about 4 s, then stop
            }
        } catch (Throwable t) { Log.e(TAG, "recents guard run failed", t); }
    }

    /** BY1a C1: the one thread that builds the label set (a PackageManager query and labels for every app); it ends when idle. */
    private static final class Labels {
        static final java.util.concurrent.ThreadPoolExecutor EX = make();
        private static java.util.concurrent.ThreadPoolExecutor make() {
            java.util.concurrent.ThreadPoolExecutor e = new java.util.concurrent.ThreadPoolExecutor(1, 1, 10, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>(),
                    new java.util.concurrent.ThreadFactory() { @Override public Thread newThread(Runnable r) { Thread t = new Thread(r, "tj-recents-labels"); t.setDaemon(true); return t; } });
            e.allowCoreThreadTimeOut(true);
            return e;
        }
    }

    /** The labels (app label and every launcher-activity label) of the apps that are NOT in the custom drawer, never a drawer app's own label. Built on the worker (BY1a C1), at most once a minute. */
    static Set<String> blockedNow(Context c) {
        Set<String> hidden = new HashSet<>(), drawer = new HashSet<>();
        try {
            PackageManager pm = c.getPackageManager();
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(c.getPackageName())) continue;
                Set<String> into = HiddenApps.isPackageShown(c, pkg) ? drawer : hidden;
                into.add(String.valueOf(ri.loadLabel(pm)));
                try { into.add(String.valueOf(pm.getApplicationLabel(ri.activityInfo.applicationInfo))); } catch (Throwable ignored) { }
            }
        } catch (Throwable t) { Log.e(TAG, "could not list the apps", t); }
        // BN1 (review C #2): our own package is skipped above, so the look-alike's label ("Settings", the word of the real Settings that is hidden now) never reached the drawer set and this closer pressed Close on the
        // look-alike's own Recents card. While the look-alike is the wanted entry its label belongs to the drawer.
        if (SettingsSpoof.pageAllowed(c)) drawer.add(SettingsSpoof.LABEL);
        Set<String> qs = new HashSet<>();
        try {
            PackageManager pm = c.getPackageManager();
            for (String p : AppOrigin.quickstep(c)) {
                try { qs.add(String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(p, 0)))); } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        return blockedLabels(hidden, qs, drawer);
    }

    private static String stamp() { return new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new java.util.Date()); }
}
