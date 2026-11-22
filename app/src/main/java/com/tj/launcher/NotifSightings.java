package com.tj.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * BH1 (Tj, 2026-11-08 02:29 UTC: "When I unlocked to the custom launcher from the lock screen, there was a notification from a quickstep app"). This Go tablet has no notification listener, so nothing could ever
 * say WHICH app posted a notification or WHEN. Android does send one thing to an accessibility helper: an event {@code TYPE_NOTIFICATION_STATE_CHANGED} (class android.app.Notification) when a notification is
 * first posted. This class notes who (the package, never the title or text), in which state (custom / Quickstep, dark / locked / on) and whether the app is in the custom drawer, in a short ring buffer for the
 * tablet report, and raises a flag "something was posted since the shade was last cleared" that the start-of-session clean-up reads: a SILENT (low-importance) notification has no status-bar icon, so the icon
 * scan alone would skip the shade although it lists it. Limits, said plainly (code review 2026-11-08): Android announces a notification only when its importance is ABOVE the lowest level, so a minimized one
 * (no icon, collapsed in the shade) sends no event and is still missed; and the flag is cleared by the first shade step after it was raised, so it costs one extra step per batch of arrivals, not one per return.
 *
 * The event arrives on the shared Launcher / accessibility main thread, so only the cheap package check happens there; the state lookup, the drawer lookup and every write run on a worker.
 *
 * Privacy: the event also carries the notification itself; nothing of it is read except the package name.
 */
public final class NotifSightings {
    private static final String TAG = "TjNotifSeen";
    private static final String PREFS = "tj_notif_seen";
    static final int MAX_LINES = 40;
    /** One lock-log line per package and state at most this often (a chat app can post dozens in a minute); the ring buffer and the counters see every one. */
    static final long LOG_GAP_MS = 60_000;
    private static final String ANDROID = "android", SYSTEM_UI = "com.android.systemui";

    private NotifSightings() {}

    // ================================================================ pure rules (JVM-tested)

    /**
     * Does a notification from this package count? Not our own (the keep-alive service's), and not the system's own (USB, charging, screen recording: ongoing, not Quickstep's and nothing "Clear all" would
     * remove).
     */
    public static boolean counts(String pkg, String ownPackage) {
        if (pkg == null || pkg.isEmpty()) return false;
        return !pkg.equals(ownPackage) && !ANDROID.equals(pkg) && !SYSTEM_UI.equals(pkg);
    }

    /**
     * Pure (BH1e, found 2026-11-08 while reading runReset): does the clean-up open the shade and press "Clear all"? {@code notifOn} = the "Clear notifications" switch; {@code icons} = whether the status bar
     * shows a notification icon (null = unknown); {@code seen} = a notification was posted since the shade was last cleared. A SILENT notification has no icon, so the icons alone would skip the shade
     * although it lists the notification: the flag decides then. Used by BOTH {@code ReturnReset} (the plan) and {@code TjAccessibilityService.runReset} (the step): v0.8.8 had the rule only in the first, and the
     * second skipped the step again.
     */
    public static boolean shadeStepWanted(boolean notifOn, Boolean icons, boolean seen) {
        return notifOn && (icons == null || icons.booleanValue() || seen);
    }

    /** Pure: should this sighting also get a lock-log line? The first one of its kind, or {@link #LOG_GAP_MS} after the last. A clock that went backwards counts as "log it". */
    public static boolean shouldLog(long lastLoggedMs, long nowMs) {
        return lastLoggedMs <= 0 || nowMs < lastLoggedMs || nowMs - lastLoggedMs >= LOG_GAP_MS;
    }

    /** Pure: the words for the state at the time: "custom" or "quickstep", plus "/dark" (screen off) or "/lock" (lock screen up). */
    public static String stateWords(boolean quickstep, boolean screenOn, boolean lockUp) {
        return (quickstep ? "quickstep" : "custom") + (!screenOn ? "/dark" : lockUp ? "/lock" : "");
    }

    /** Pure: {@code recent} with {@code line} added at the end, only the newest {@code max} lines kept. */
    public static String append(String recent, String line, int max) {
        StringBuilder b = new StringBuilder(recent == null ? "" : recent);
        if (b.length() > 0 && b.charAt(b.length() - 1) != '\n') b.append('\n');
        b.append(line).append('\n');
        String all = b.toString();
        int lines = 0;
        for (int i = 0; i < all.length(); i++) if (all.charAt(i) == '\n') lines++;
        if (lines <= max) return all;
        int cut = 0;
        for (int drop = lines - max; drop > 0; drop--) cut = all.indexOf('\n', cut) + 1;
        return all.substring(cut);
    }

    /**
     * Pure: "package x3 (custom drawer), package x1 (quickstep/lock hidden)": the lines grouped by package + state + drawer, newest group first, at most {@code maxGroups} of them. A line is
     * {@code "MM-dd HH:mm:ss <package> <state> <drawer|hidden>"}; one that does not parse is skipped.
     */
    public static String summarize(String recent, int maxGroups) {
        if (recent == null || recent.trim().isEmpty()) return "(none)";
        String[] lines = recent.split("\n");
        Map<String, Integer> groups = new LinkedHashMap<>();
        for (int i = lines.length - 1; i >= 0; i--) {
            String[] w = lines[i].trim().split(" ");
            if (w.length < 5) continue;   // date, time, package, state, drawer
            String key = w[2] + " (" + w[3] + " " + w[4] + ")";
            Integer n = groups.get(key);
            groups.put(key, n == null ? 1 : n + 1);
        }
        if (groups.isEmpty()) return "(none)";
        StringBuilder b = new StringBuilder();
        int shown = 0;
        for (Map.Entry<String, Integer> e : groups.entrySet()) {
            if (shown++ >= maxGroups) { b.append(", ..."); break; }
            if (b.length() > 0) b.append(", ");
            b.append(e.getKey().substring(0, e.getKey().indexOf(' '))).append(" x").append(e.getValue()).append(e.getKey().substring(e.getKey().indexOf(' ')));
        }
        return b.toString();
    }

    // ================================================================ Android side

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    private static final Map<String, Long> LAST_LOGGED = new HashMap<>();

    /** One worker, in order: the drawer lookup can query the package manager and commit a preference the first time, which the main thread (the Launcher's first frame) must never wait for. */
    private static final java.util.concurrent.ExecutorService BG = IdleExecutor.single("tj-notif-seen");   // C6 (CW1): the thread ends after 10 s idle

    /**
     * The accessibility helper calls this for every TYPE_NOTIFICATION_STATE_CHANGED event whose class is android.app.Notification (delivered on the main thread): a package check, then everything else on the
     * worker with the time of the event. Never throws.
     */
    public static void onNotification(final Context c, CharSequence packageName) {
        try {
            final Context app = c.getApplicationContext();
            final String pkg = packageName == null ? null : packageName.toString();
            if (!counts(pkg, app.getPackageName())) return;
            final long now = System.currentTimeMillis();
            BG.execute(new Runnable() { @Override public void run() { record(app, pkg, now); } });
        } catch (Throwable t) { Log.e(TAG, "sighting failed", t); }
    }

    /** The worker's half: the state of the tablet at the time, the drawer membership, the ring buffer, the flag and the (rate-limited) lock-log line. */
    private static void record(Context app, String pkg, long now) {
        try {
            final boolean quickstep = Mode.QUICKSTEP.equals(Mode.get(app));
            final boolean on = AnimationSpeedup.interactive(app);
            final boolean lock = on && AnimationSpeedup.keyguardUp(app);
            final String state = stateWords(quickstep, on, lock);
            final boolean drawer = HiddenApps.isPackageShown(app, pkg);
            final String line = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date(now)) + " " + pkg + " " + state + " " + (drawer ? "drawer" : "hidden");
            SharedPreferences sp = p(app);
            synchronized (NotifSightings.class) {
                sp.edit().putString("recent", append(sp.getString("recent", ""), line, MAX_LINES)).putBoolean("dirty", true).putLong("total", sp.getLong("total", 0) + 1).apply();
                String key = pkg + " " + state;
                Long last = LAST_LOGGED.get(key);
                if (shouldLog(last == null ? 0 : last, now)) {
                    if (LAST_LOGGED.size() > 60) LAST_LOGGED.clear();
                    LAST_LOGGED.put(key, now);
                    LockLog.addAsync(app, "NOTIF", pkg + " posted a notification (" + state + ", " + (drawer ? "in the custom drawer" : "NOT in the custom drawer") + ")");
                }
            }
        } catch (Throwable t) { Log.e(TAG, "sighting failed", t); }
    }

    /** True when a notification was posted since the shade was last cleared by the clean-up (a silent one has no status-bar icon). */
    public static boolean dirty(Context c) { try { return p(c).getBoolean("dirty", false); } catch (Throwable t) { return false; } }

    /** The clean-up is about to clear the shade: whatever arrives from now on sets the flag again. */
    public static void clearDirty(Context c) { try { p(c).edit().putBoolean("dirty", false).apply(); } catch (Throwable ignored) { } }

    /** One line for the tablet report. Never throws. */
    public static String report(Context c) {
        try {
            SharedPreferences sp = p(c);
            String recent = sp.getString("recent", "");
            StringBuilder b = new StringBuilder();
            b.append(sp.getLong("total", 0)).append(" posted since this feature came in (the last ").append(MAX_LINES).append(" kept), clean-up flag ").append(sp.getBoolean("dirty", false) ? "SET" : "clear");
            b.append(" | by app: ").append(summarize(recent, 8));
            String[] lines = recent.isEmpty() ? new String[0] : recent.split("\n");
            if (lines.length > 0) {
                b.append(" | newest:");
                for (int i = Math.max(0, lines.length - 3); i < lines.length; i++) b.append(i == Math.max(0, lines.length - 3) ? " " : " || ").append(lines[i].trim());
            }
            return b.toString();
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
