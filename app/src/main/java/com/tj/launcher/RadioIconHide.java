package com.tj.launcher;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

/**
 * BS1 + BS2 (Tj, 2026-11-10 23:27 UTC: "Is it possible to keep wifi on when I turn the screen off and it goes to screen lock, but hide the wifi symbol in the lock screen and custom launcher"; 23:55 UTC: "Make it
 * also keep Bluetooth on but hide Bluetooth the same way it keeps wifi on but hides the symbol. Only keep them on if they are already on. Don't turn them on automatically. They should never, at any time, show up
 * on the lock screen or in the custom launcher"). The Android side of the Wi-Fi and Bluetooth SYMBOL hold: {@code Settings.Secure icon_blacklist} gets the slots {@code wifi} and {@code bluetooth} added whenever
 * {@link NotifQuietPolicy} says "quiet" (the custom launcher, a dark or locked screen in ANY launcher, a plain PIN still being decided) and is put back exactly as found for an unlocked Quickstep session. The
 * rules, with the proof from the AOSP 10 sources, are in {@link RadioIconHold}; THIS class reads the facts, adapts the two settings to its small interfaces and keeps the saved original. Needs
 * WRITE_SECURE_SETTINGS (the one-time adb grant the launcher already holds); without it nothing changes.
 *
 * The other half, "the radios stay ON": {@link #keepsRadiosOn} is asked by the places that switched Wi-Fi and Bluetooth off (AK1's lock-screen rule {@link LockQuiet}, the custom launcher's radio guard, the session
 * reset {@link ReturnReset#radiosOff}); they leave BOTH radios alone only while the symbols can really be hidden (switch on, grant, automation not paused, and in a Quickstep mode a running helper). NOTHING in this
 * class turns a radio on (Tj: "Only keep them on if they are already on. Don't turn them on automatically."); the one older path that can, AK1's put-back after a gesture unlock into Quickstep, restores only what
 * AK1 itself switched off at a lock screen that came up before the symbols could be hidden (right after a boot), exactly as v0.9.3 did.
 *
 * LEVEL-TRIGGERED, like {@link NotifQuiet}: {@link #sync} is called from {@link AnimationSpeedup#syncNow} with the same trigger list (mode changes, screen off / on, the unlock decision, helper connect / unbind, process
 * start, the 2 Hz window heartbeat in Quickstep, the switch, "Restore everything"), so no missed event can leave the symbols hidden in Quickstep.
 *
 * WHERE THE HOLD IS REMEMBERED: in {@code Settings.Global} itself, as one private key ({@link #MARKER}) that holds the value to put back, NOT in a preference file (a file dies with "Clear data" and would leave
 * the symbols hidden for good with nothing that knows it was ours; the key survives both, so the next start can still put the list back). KNOWN GAP: uninstalling the app while the hold is on leaves the symbols
 * hidden in every launcher until the list is reset ({@code adb shell settings delete secure icon_blacklist}) or the app is installed again (the first sync puts it back): switch the feature off first.
 *
 * UNPROVEN on Tj's tablet: the source says AOSP's SystemUI honours the list on the lock screen too; the tablet's OEM SystemUI may not. Launcher control and the tablet report show the facts.
 */
public final class RadioIconHide {
    private static final String TAG = "TjRadioIcon";
    private static final String PREFS = "tj_radio_icons";
    /** Settings.Global key that exists exactly while the hold is on; its value is {@link RadioIconHold#encodeMarker}. */
    public static final String MARKER = "tj_radio_icons_original";

    private RadioIconHide() {}

    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED; }

    /** The switch (shipped default ON, debug builds OFF: {@link RadioIconHold#defaultOn}). */
    public static boolean switchOn(Context c) { return Features.get(c, RadioIconHold.KEY, RadioIconHold.defaultOn(ReturnReset.isDebuggable(c))); }

    /**
     * Do the radio switch-offs leave Wi-Fi AND Bluetooth ON right now? ({@link RadioIconHold#keepsRadiosOn}.) The switch is read first (a preference read): with the feature off there is no binder call. Never throws;
     * false on any doubt = today's behaviour (the radios go off like before).
     */
    public static boolean keepsRadiosOn(Context c) {
        try {
            final Context app = c.getApplicationContext();
            if (!switchOn(app)) return false;
            final boolean quickstep = Mode.QUICKSTEP.equals(Mode.get(app));
            return RadioIconHold.keepsRadiosOn(true, permitted(app), CrashGuard.automationOff(app), quickstep, !quickstep || TjAccessibilityService.get() != null);
        } catch (Throwable t) { return false; }
    }

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** The saved state: the marker key in Settings.Global. {@code save} throws when the write did not happen, so a change is never made without its original saved. */
    private static final class MarkerState implements RadioIconHold.State {
        private final ContentResolver cr;
        MarkerState(ContentResolver cr) { this.cr = cr; }
        @Override public boolean held() { return Settings.Global.getString(cr, MARKER) != null; }
        @Override public String original() { return RadioIconHold.decodeMarker(Settings.Global.getString(cr, MARKER)); }
        @Override public void save(boolean held, String original) {
            if (!Settings.Global.putString(cr, MARKER, held ? RadioIconHold.encodeMarker(original) : null)) throw new IllegalStateException("could not save the Wi-Fi / Bluetooth symbol state");
        }
    }

    /** The one secure setting. */
    private static final class ListSetting implements RadioIconHold.Setting {
        private final ContentResolver cr;
        ListSetting(ContentResolver cr) { this.cr = cr; }
        @Override public String get() { return Settings.Secure.getString(cr, RadioIconHold.SETTING); }
        @Override public void put(String value) { if (!Settings.Secure.putString(cr, RadioIconHold.SETTING, value)) throw new IllegalStateException("could not write " + RadioIconHold.SETTING); }
    }

    private static final RadioIconHold.Sink SINK = new RadioIconHold.Sink() { @Override public void log(String message) { Log.i(TAG, message); } };

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
            final boolean after = RadioIconHold.set(new MarkerState(cr), new ListSetting(cr), f.grant, want, SINK);
            if (before != after) {
                String msg = after ? "Wi-Fi and Bluetooth symbols HIDDEN (" + NotifQuietPolicy.quietFor(f) + ", " + trigger + ")" : "Wi-Fi and Bluetooth symbols shown again: " + (why.isEmpty() ? "hold ended" : why) + " (" + trigger + ")";
                Log.i(TAG, msg);
                LockLog.addAsync(app, "SYMBOLS", msg);
                p(app).edit().putString("last", new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  " + msg).apply();
                // review of BS2, finding 2: the hold ended because the helper went away while the screen is dark or the lock screen is up. The symbols would show with the radios still on and nothing else would switch
                // them off (the radio receiver and the lock re-check live in the helper): run the lock rule now (keepsRadiosOn is false without the helper, so it switches both radios off and remembers them for a gesture unlock)
                if (before && !after && look != null && RadioIconHold.radiosGoOffWhenHoldEnds(why, !look.screenOn() || look.lockUp())) {
                    LockLog.addAsync(app, "SYMBOLS", "the helper is gone on a dark or locked screen: the lock rule switches the radios off");
                    LockQuiet.onLockAsync(app);
                }
            }
        } catch (Throwable t) { Log.e(TAG, "sync failed", t); }
    }

    /** "Restore everything" and the crash breaker: put the list back now, whatever the switch says. Never throws. */
    public static synchronized void restoreIfHeld(Context c) {
        try {
            final Context app = c.getApplicationContext();
            final ContentResolver cr = app.getContentResolver();
            RadioIconHold.set(new MarkerState(cr), new ListSetting(cr), permitted(app), false, SINK);
        } catch (Throwable t) { Log.e(TAG, "restore failed", t); }
    }

    // ================================================================ what Launcher control and the tablet report show

    private static String wifiState(Context c) {
        try {
            WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wm == null ? "?" : (wm.isWifiEnabled() ? "on" : "off");
        } catch (Throwable t) { return "?"; }
    }

    private static String bluetoothState() {
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            return a == null ? "none" : (a.isEnabled() ? "on" : "off");
        } catch (Throwable t) { return "?"; }
    }

    /** One line: the switch, the grant, whether the symbols are hidden right now, the list, what will be put back, both radios and the last change. Never throws. */
    public static String describe(Context c) {
        try {
            Context app = c.getApplicationContext();
            ContentResolver cr = app.getContentResolver();
            String marker = Settings.Global.getString(cr, MARKER);
            StringBuilder b = new StringBuilder();
            b.append("switch ").append(switchOn(app) ? "on" : "off").append(", adb permission ").append(permitted(app) ? "granted" : "MISSING");
            b.append(", radios kept on by us: ").append(keepsRadiosOn(app) ? "yes" : "no").append(", Wi-Fi radio now: ").append(wifiState(app)).append(", Bluetooth radio now: ").append(bluetoothState()).append(" | now: ");
            b.append(marker != null ? "Wi-Fi and Bluetooth symbols HIDDEN (held)" : "symbols as Android has them");
            b.append(" | ").append(RadioIconHold.SETTING).append(" = ").append(Settings.Secure.getString(cr, RadioIconHold.SETTING));
            if (marker != null) { String o = RadioIconHold.decodeMarker(marker); b.append(" | put back to: ").append(o == null ? "(system default)" : "\"" + o + "\""); }
            b.append(" | last change: ").append(p(app).getString("last", "(none yet)"));
            return b.toString();
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
