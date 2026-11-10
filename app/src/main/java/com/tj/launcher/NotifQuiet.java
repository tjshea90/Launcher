package com.tj.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

/**
 * BH1 (Tj, 2026-11-08 02:29 UTC: "... Turn all notifications off for both the lock screen and the custom launcher ... should never show any notification at any time ..."). The pop-up banner (heads-up) half:
 * {@code Settings.Global heads_up_notifications_enabled} is held at 0 whenever {@link NotifQuietPolicy} says so (the custom launcher, a dark or locked screen in ANY launcher, a plain PIN still being decided)
 * and is put back exactly as found for an unlocked Quickstep session. Needs WRITE_SECURE_SETTINGS (the one-time adb grant the launcher already holds); without it nothing changes.
 *
 * LEVEL-TRIGGERED, like the animations: {@link #sync} is called from {@link AnimationSpeedup#syncNow} (mode changes, screen off / on, the unlock decision, helper connect / unbind, process start, the 2 Hz
 * window heartbeat in Quickstep, "Restore everything", the switch), re-reads the facts and sets or clears the hold, so no missed event can leave the banners off in Quickstep. The original value is saved before
 * the first write; the rules are in {@link NotifQuietHold} (JVM-tested).
 *
 * WHERE THE HOLD IS REMEMBERED (code review 2026-11-08): in {@code Settings.Global} itself, as one private key ({@link #MARKER}) that holds the value to put back, NOT in a preference file. A file dies with
 * "Clear data" (and a reinstall that clears), which would leave the setting at 0 for good with nothing that remembers it was ours; the key survives both, so the next start can still put the banners back.
 *
 * What this does NOT do (said plainly in TASKS BH1a): it hides no notification that is already in the shade, no status-bar icon and no shade entry. The lock screen's own list is {@link LockQuiet}'s (AK1), the
 * clean-up at the start of a custom session is {@code ReturnReset}'s, and this tablet is a Go device (no notification listener, so nothing can see or cancel a notification before SystemUI shows it).
 */
public final class NotifQuiet {
    private static final String TAG = "TjNotifQuiet";
    private static final String PREFS = "tj_notif_quiet";
    /** Settings.Global key that exists exactly while the hold is on; its value is what to put back. */
    public static final String MARKER = "tj_banners_original";

    private NotifQuiet() {}

    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED; }

    /** The switch (shipped default ON, debug builds OFF: {@link NotifQuietPolicy#defaultOn}). */
    public static boolean switchOn(Context c) { return Features.get(c, NotifQuietPolicy.KEY, NotifQuietPolicy.defaultOn(ReturnReset.isDebuggable(c))); }

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** The saved state: the marker key in Settings.Global. {@code save} throws when the write did not happen, so a change is never made without its original saved. */
    private static final class MarkerState implements NotifQuietHold.State {
        private final ContentResolver cr;
        MarkerState(ContentResolver cr) { this.cr = cr; }
        @Override public boolean held() { return Settings.Global.getString(cr, MARKER) != null; }
        @Override public String original() { return Settings.Global.getString(cr, MARKER); }
        @Override public void save(boolean held, String original) {
            if (!Settings.Global.putString(cr, MARKER, held ? (original == null ? "1" : original) : null)) throw new IllegalStateException("could not save the banner state");
        }
    }

    /** The one global setting. */
    private static final class GlobalSetting implements NotifQuietHold.Setting {
        private final ContentResolver cr;
        GlobalSetting(ContentResolver cr) { this.cr = cr; }
        @Override public String get() { return Settings.Global.getString(cr, NotifQuietPolicy.SETTING); }
        @Override public void put(String value) { if (!Settings.Global.putString(cr, NotifQuietPolicy.SETTING, value)) throw new IllegalStateException("could not write " + NotifQuietPolicy.SETTING); }
    }

    private static final NotifQuietHold.Sink SINK = new NotifQuietHold.Sink() { @Override public void log(String message) { Log.i(TAG, message); } };

    public static boolean held(Context c) {
        try { return Settings.Global.getString(c.getApplicationContext().getContentResolver(), MARKER) != null; } catch (Throwable t) { return false; }
    }

    /**
     * THE decision. Reads the facts, asks {@link NotifQuietPolicy#why}, sets or clears the hold, and logs a line (lock log + logcat) only when something changed. {@code look}: the screen and lock facts of this
     * sync (asked of Android at most once, shared with the animation rule); {@code assumeHelper}: the process has just started and its helper is about to connect. Cheap when there is nothing to do: no binder
     * call unless the hold is on or could start. Only called from {@link AnimationSpeedup#syncNow} (serialised); never throws.
     */
    static synchronized void sync(Context c, String trigger, AnimationSpeedup.Look look, boolean assumeHelper) {
        try {
            final Context app = c.getApplicationContext();
            final boolean before = held(app);
            NotifQuietPolicy.Facts f = new NotifQuietPolicy.Facts();
            f.switchOn = switchOn(app);
            if (!f.switchOn && !before) return;   // nothing held and nothing wanted
            fillFacts(app, f, look, assumeHelper);
            final String why = NotifQuietPolicy.why(f);
            final boolean want = why.isEmpty();
            final boolean after = NotifQuietHold.set(new MarkerState(app.getContentResolver()), new GlobalSetting(app.getContentResolver()), f.grant, want, SINK);
            if (before != after) {
                String msg = after ? "banners OFF (" + NotifQuietPolicy.quietFor(f) + ", " + trigger + ")" : "banners ON again: " + (why.isEmpty() ? "hold ended" : why) + " (" + trigger + ")";
                Log.i(TAG, msg);
                LockLog.addAsync(app, "QUIET", msg);
                p(app).edit().putString("last", new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  " + msg).apply();
            }
        } catch (Throwable t) { Log.e(TAG, "sync failed", t); }
    }

    /** "Restore everything" and the crash breaker: put the setting back now, whatever the switch says. Never throws. */
    public static synchronized void restoreIfHeld(Context c) {
        try {
            final Context app = c.getApplicationContext();
            NotifQuietHold.set(new MarkerState(app.getContentResolver()), new GlobalSetting(app.getContentResolver()), permitted(app), false, SINK);
        } catch (Throwable t) { Log.e(TAG, "restore failed", t); }
    }

    // ================================================================ what Launcher control and the tablet report show

    /** One line: the switch, the grant, whether the banners are held off right now, the setting, what will be put back, and the last change. Never throws. */
    public static String describe(Context c) {
        try {
            Context app = c.getApplicationContext();
            ContentResolver cr = app.getContentResolver();
            String marker = Settings.Global.getString(cr, MARKER);
            StringBuilder b = new StringBuilder();
            b.append("switch ").append(switchOn(app) ? "on" : "off").append(", adb permission ").append(permitted(app) ? "granted" : "MISSING").append(" | now: ");
            b.append(marker != null ? "pop-up banners OFF (held)" : "pop-up banners as Android has them");
            b.append(" | ").append(NotifQuietPolicy.SETTING).append(" = ").append(Settings.Global.getString(cr, NotifQuietPolicy.SETTING));
            if (marker != null) b.append(" | put back to: ").append(marker);
            b.append(" | last change: ").append(p(app).getString("last", "(none yet)"));
            return b.toString();
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
