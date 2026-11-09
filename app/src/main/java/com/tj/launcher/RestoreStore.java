package com.tj.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import java.util.Map;

/**
 * Every system setting this app ever changes is recorded here FIRST (its previous value), so one call puts Android back exactly as it was.
 * Today nothing writes to system settings (the permission-based features arrive later, each default OFF), so restoreAll() mainly turns the
 * launcher's own switches off. The secure-setting restore is real and unit-tested-by-design: it only writes values we snapshotted, only when
 * the permission was granted, and never writes any lock-screen SETTING (the lock-screen number overlay is the helper's own switch and is turned off here).
 */
public final class RestoreStore {
    private static final String PREFS = "tj_restore";
    private RestoreStore() {}

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** Records the current value of a Settings.Secure key once (never overwrites an existing snapshot). */
    public static synchronized void snapshotSecure(Context c, String key) {   // v0.7.6 (review of AK1): synchronized: two threads start at every process start, a second read after the first write would store the CHANGED value as the original
        SharedPreferences sp = p(c);
        if (sp.contains("secure:" + key)) return;
        String cur = Settings.Secure.getString(c.getContentResolver(), key);
        sp.edit().putString("secure:" + key, cur == null ? "\u0000null" : cur).commit();
    }

    /** Same as {@link #snapshotSecure} for Settings.Global (the value the system had before we first changed it). */
    public static void snapshotGlobal(Context c, String key) {
        SharedPreferences sp = p(c);
        if (sp.contains("global:" + key)) return;
        String cur = Settings.Global.getString(c.getContentResolver(), key);
        sp.edit().putString("global:" + key, cur == null ? "\u0000null" : cur).commit();
    }

    /**
     * Replaces the snapshot with what the system has NOW. Only for a moment when the launcher is NOT holding a changed value (the caller checks): then whatever is there IS the original,
     * also when another tool changed it since the first snapshot (a stale snapshot would overwrite that newer value on the next restore).
     */
    public static void resnapshotGlobal(Context c, String key) {
        String cur = Settings.Global.getString(c.getContentResolver(), key);
        p(c).edit().putString("global:" + key, cur == null ? "\u0000null" : cur).commit();
    }

    public static boolean hasGlobalSnapshot(Context c, String key) { return p(c).contains("global:" + key); }

    /** The snapshotted Settings.Global value, or {@code fallback} if none (or it was unset). */
    public static String globalSnapshot(Context c, String key, String fallback) {
        String v = p(c).getString("global:" + key, null);
        return v == null || "\u0000null".equals(v) ? fallback : v;
    }

    public static boolean hasSnapshot(Context c) { return !p(c).getAll().isEmpty(); }

    /** AK1: puts ONE snapshotted secure setting back and forgets that snapshot (a switch that turns a single feature off). True when something was restored. Never throws. */
    public static synchronized boolean restoreSecure(Context c, String key) {
        try {
            SharedPreferences sp = p(c);
            String v = sp.getString("secure:" + key, null);
            if (v == null) return false;
            Settings.Secure.putString(c.getContentResolver(), key, "\u0000null".equals(v) ? null : v);
            sp.edit().remove("secure:" + key).commit();
            return true;
        } catch (Throwable t) { android.util.Log.e("TjRestore", "could not restore " + key, t); return false; }
    }

    /** Restores every snapshotted secure setting that we can write, forgets the snapshot, and turns every launcher feature switch off. */
    public static void restoreAll(Context c) {
        SharedPreferences sp = p(c);
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            String k = e.getKey();
            if ((!k.startsWith("secure:") && !k.startsWith("global:")) || !(e.getValue() instanceof String)) continue;
            String v = (String) e.getValue();
            try {
                if (k.startsWith("global:")) Settings.Global.putString(c.getContentResolver(), k.substring(7), "\u0000null".equals(v) ? null : v);
                else Settings.Secure.putString(c.getContentResolver(), k.substring(7), "\u0000null".equals(v) ? null : v);
            } catch (Throwable t) {
                android.util.Log.e("TjRestore", "could not restore " + k, t); // no permission: nothing was changed by us either
            }
        }
        try { AnimationSpeedup.restoreIfPending(c); } catch (Throwable ignored) { }
        try { NotifQuiet.restoreIfHeld(c); } catch (Throwable ignored) { }   // BH1: the pop-up banner setting goes back too (our own saved original, not a snapshot in this store)
        sp.edit().clear().commit();
        Features.allOff(c);
        Features.set(c, LockNumber.KEY, false);
        Features.set(c, PickerGuard.KEY, false);   // AO1: the hider of 'My photos' is a customisation too
        Features.set(c, PatchRest.KEY, false);   // AM1: the resting patch is ON by default now: restore = off
        Features.set(c, RecentsFilter.KEY, false);   // AP1: so is the Recents filter
        Features.set(c, LockQuiet.KEY, false);   // AK1: restore = the lock screen shows notifications again (the two settings were put back above) and nothing of ours switches Wi-Fi / Bluetooth off at the lock
        Features.set(c, LockQuiet.RESTORE_KEY, false);
        // AC1 (v0.7.3): the Two-profiles switches (pre-arm, radios, pre-clean) are NOT written false here any more. The old code did, so a breaker trip silently disabled the very things Tj depends on and he had to find and
        // re-enable them by hand. "Nothing of ours switches launchers for you" is now the PAUSE ({@link CrashGuard#automationOff}), which every one of those rules reads; Restore and the breaker set it, and the next deliberate
        // way back to the custom launcher clears it.
        Features.set(c, KeepAlivePolicy.KEY, false);   // AA2: restore = the process is no longer held in memory by us
        KeepAliveService.stop(c);
        Features.set(c, ImmersiveProfile.KEY, false);   // AA1: the policy_control value was put back above (snapshot); the switch itself is off
        Features.set(c, DemoModeProfile.KEY, false);   // (the retired retail-demo option: the flag, if it was ever ours, was put back above)   // O4: restore = nothing of ours closes apps for you
        Features.set(c, AnimationPolicy.KEY, false);   // BF1: the Quickstep animation switch is ON by default in the shipped app, so restore = off (the scales themselves were put back above)
        Features.set(c, NotifQuietPolicy.KEY, false);   // BH1: the pop-up banner switch is ON by default in the shipped app, so restore = off (the setting itself was put back above)
        Features.set(c, SettingsSpoof.KEY, false);   // BN1: restore = the real Settings is the only Settings again (the look-alike's drawer entry goes, the real one is no longer kept out)
        try { SettingsSpoof.syncSoon(c, "restore everything"); } catch (Throwable ignored) { }
        try { AnimationSpeedup.syncNow(c, "restore everything"); } catch (Throwable ignored) { }   // BF1: allOff() above made the switch read its default (ON) for a few ms, long enough for the window heartbeat to take the animations off again: this closing look sees the switch OFF and puts them back for good
        Features.set(c, RouterActivity.HOME_TYPE_KEY, false);   // restore = the plain launch (Android keeps treating us as an ordinary home app)   // defaults to ON, so switch it off explicitly: restore means nothing of ours stays on the lock screen
    }
}
