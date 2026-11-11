package com.tj.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

/**
 * BS1 (Tj, 2026-11-10 23:27 UTC: "Is it possible to keep wifi on when I turn the screen off and it goes to screen lock, but hide the wifi symbol in the lock screen and custom launcher"). The Android side of the
 * Wi-Fi SYMBOL hold: {@code Settings.Secure icon_blacklist} gets the slot {@code wifi} added whenever {@link NotifQuietPolicy} says "quiet" (the custom launcher, a dark or locked screen in ANY launcher, a plain
 * PIN still being decided) and is put back exactly as found for an unlocked Quickstep session. The rules, with the proof from the AOSP 10 sources, are in {@link WifiIconHold}; THIS class reads the facts, adapts the
 * two settings to its small interfaces and keeps the saved original. Needs WRITE_SECURE_SETTINGS (the one-time adb grant the launcher already holds); without it nothing changes.
 *
 * The other half, "Wi-Fi stays ON": {@link #keepsWifiOn} is asked by the places that switched Wi-Fi off (AK1's lock-screen rule {@link LockQuiet}, the custom launcher's radio guard, the session reset
 * {@link ReturnReset#radiosOff}); they leave Wi-Fi alone only while the symbol can really be hidden (switch on, grant, automation not paused). Bluetooth is unchanged.
 *
 * LEVEL-TRIGGERED, like {@link NotifQuiet}: {@link #sync} is called from {@link AnimationSpeedup#syncNow} with the same trigger list (mode changes, screen off / on, the unlock decision, helper connect / unbind, process
 * start, the 2 Hz window heartbeat in Quickstep, the switch, "Restore everything"), so no missed event can leave the symbol hidden in Quickstep.
 *
 * WHERE THE HOLD IS REMEMBERED: in {@code Settings.Global} itself, as one private key ({@link #MARKER}) that holds the value to put back, NOT in a preference file (a file dies with "Clear data" and would leave
 * the symbol hidden for good with nothing that knows it was ours; the key survives both, so the next start can still put the list back).
 *
 * UNPROVEN on Tj's tablet: the source says AOSP's SystemUI honours the list on the lock screen too; the tablet's OEM SystemUI may not. Launcher control and the tablet report show the facts.
 */
public final class WifiIconHide {
    private static final String TAG = "TjWifiIcon";
    private static final String PREFS = "tj_wifi_icon";
    /** Settings.Global key that exists exactly while the hold is on; its value is {@link WifiIconHold#encodeMarker}. */
    public static final String MARKER = "tj_wifi_icon_original";

    private WifiIconHide() {}

    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED; }

    /** The switch (shipped default ON, debug builds OFF: {@link WifiIconHold#defaultOn}). */
    public static boolean switchOn(Context c) { return Features.get(c, WifiIconHold.KEY, WifiIconHold.defaultOn(ReturnReset.isDebuggable(c))); }

    /** Do the Wi-Fi switch-offs leave Wi-Fi ON right now? ({@link WifiIconHold#keepsWifiOn}.) One binder call (the permission); never throws; false on any doubt = today's behaviour. */
    public static boolean keepsWifiOn(Context c) {
        try {
            final Context app = c.getApplicationContext();
            return WifiIconHold.keepsWifiOn(switchOn(app), permitted(app), CrashGuard.automationOff(app));
        } catch (Throwable t) { return false; }
    }

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** The saved state: the marker key in Settings.Global. {@code save} throws when the write did not happen, so a change is never made without its original saved. */
    private static final class MarkerState implements WifiIconHold.State {
        private final ContentResolver cr;
        MarkerState(ContentResolver cr) { this.cr = cr; }
        @Override public boolean held() { return Settings.Global.getString(cr, MARKER) != null; }
        @Override public String original() { return WifiIconHold.decodeMarker(Settings.Global.getString(cr, MARKER)); }
        @Override public void save(boolean held, String original) {
            if (!Settings.Global.putString(cr, MARKER, held ? WifiIconHold.encodeMarker(original) : null)) throw new IllegalStateException("could not save the Wi-Fi symbol state");
        }
    }

    /** The one secure setting. */
    private static final class ListSetting implements WifiIconHold.Setting {
        private final ContentResolver cr;
        ListSetting(ContentResolver cr) { this.cr = cr; }
        @Override public String get() { return Settings.Secure.getString(cr, WifiIconHold.SETTING); }
        @Override public void put(String value) { if (!Settings.Secure.putString(cr, WifiIconHold.SETTING, value)) throw new IllegalStateException("could not write " + WifiIconHold.SETTING); }
    }

    private static final WifiIconHold.Sink SINK = new WifiIconHold.Sink() { @Override public void log(String message) { Log.i(TAG, message); } };

    public static boolean held(Context c) {
        try { return Settings.Global.getString(c.getApplicationContext().getContentResolver(), MARKER) != null; } catch (Throwable t) { return false; }
    }

    /**
     * THE decision. Reads the facts (shared with the pop-up banners: {@link NotifQuiet#fillFacts}), asks {@link NotifQuietPolicy#why}, sets or clears the hold, and logs a line (lock log + logcat) only when
     * something changed. Cheap when there is nothing to do: no binder call unless the hold is on or the switch is on. Only called from {@link AnimationSpeedup#syncNow} (serialised); never throws.
     */
    static synchronized void sync(Context c, String trigger, AnimationSpeedup.Look look, boolean assumeHelper) {
        try {
            final Context app = c.getApplicationContext();
            final boolean before = held(app);
            NotifQuietPolicy.Facts f = new NotifQuietPolicy.Facts();
            f.switchOn = switchOn(app);
            if (!f.switchOn && !before) return;   // nothing held and nothing wanted
            NotifQuiet.fillFacts(app, f, look, assumeHelper);
            final String why = NotifQuietPolicy.why(f);
            final boolean want = why.isEmpty();
            final ContentResolver cr = app.getContentResolver();
            final boolean after = WifiIconHold.set(new MarkerState(cr), new ListSetting(cr), f.grant, want, SINK);
            if (before != after) {
                String msg = after ? "Wi-Fi symbol HIDDEN (" + NotifQuietPolicy.quietFor(f) + ", " + trigger + ")" : "Wi-Fi symbol shown again: " + (why.isEmpty() ? "hold ended" : why) + " (" + trigger + ")";
                Log.i(TAG, msg);
                LockLog.addAsync(app, "WIFI", msg);
                p(app).edit().putString("last", new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  " + msg).apply();
            }
        } catch (Throwable t) { Log.e(TAG, "sync failed", t); }
    }

    /** "Restore everything" and the crash breaker: put the list back now, whatever the switch says. Never throws. */
    public static synchronized void restoreIfHeld(Context c) {
        try {
            final Context app = c.getApplicationContext();
            final ContentResolver cr = app.getContentResolver();
            WifiIconHold.set(new MarkerState(cr), new ListSetting(cr), permitted(app), false, SINK);
        } catch (Throwable t) { Log.e(TAG, "restore failed", t); }
    }

    // ================================================================ what Launcher control and the tablet report show

    private static String wifiState(Context c) {
        try {
            WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wm == null ? "?" : (wm.isWifiEnabled() ? "on" : "off");
        } catch (Throwable t) { return "?"; }
    }

    /** One line: the switch, the grant, whether the symbol is hidden right now, the list, what will be put back, the Wi-Fi radio and the last change. Never throws. */
    public static String describe(Context c) {
        try {
            Context app = c.getApplicationContext();
            ContentResolver cr = app.getContentResolver();
            String marker = Settings.Global.getString(cr, MARKER);
            StringBuilder b = new StringBuilder();
            b.append("switch ").append(switchOn(app) ? "on" : "off").append(", adb permission ").append(permitted(app) ? "granted" : "MISSING");
            b.append(", Wi-Fi radio kept on by us: ").append(keepsWifiOn(app) ? "yes" : "no").append(", Wi-Fi radio now: ").append(wifiState(app)).append(" | now: ");
            b.append(marker != null ? "Wi-Fi symbol HIDDEN (held)" : "Wi-Fi symbol as Android has it");
            b.append(" | ").append(WifiIconHold.SETTING).append(" = ").append(Settings.Secure.getString(cr, WifiIconHold.SETTING));
            if (marker != null) { String o = WifiIconHold.decodeMarker(marker); b.append(" | put back to: ").append(o == null ? "(system default)" : "\"" + o + "\""); }
            b.append(" | last change: ").append(p(app).getString("last", "(none yet)"));
            return b.toString();
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
