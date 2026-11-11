package com.tj.launcher;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;

/**
 * AK1 (Tj, 2026-11-06: "make it so wifi, Bluetooth, and all notifications are cleared and off whenever the tablet is on the lock screen, from any launcher. The lock screen should never, at any time, show wifi,
 * Bluetooth, or notifications from apps"). Two halves, both independent of which launcher is active:
 *  - NOTIFICATIONS: without the shell helper this app cannot delete other apps' notifications, but Android has its own lock-screen setting: {@code Settings.Secure lock_screen_show_notifications = 0} (and
 *    {@code lock_screen_allow_private_notifications = 0}) makes the lock screen show NO app notifications and no notification icons, at every moment, with no flicker. The original values are recorded first
 *    ({@link RestoreStore#snapshotSecure}) so "Restore everything", the crash breaker and the switch itself put them back. The notifications stay in the shade until cleared.
 *  - RADIOS: when the tablet is on the lock screen Wi-Fi and Bluetooth are switched off and kept off (a radio that comes on again is switched off again, at most 3 times per minute). What was on before the
 *    lock is remembered; a GESTURE unlock into Quickstep puts it back (second switch), a plain unlock into the custom launcher leaves it off (the custom launcher's own rule).
 * Never touches the lock method, never blocks an unlock, every step is exception-safe and does nothing when automation is paused (breaker / "Restore everything").
 */
public final class LockQuiet {
    private static final String TAG = "TjLockQuiet";
    public static final String KEY = "lock_quiet";                      // switch in Launcher control, default ON (Tj asked)
    public static final String RESTORE_KEY = "lock_quiet_restore";      // switch: a gesture unlock into Quickstep puts the radios back, default ON
    public static final String PREV_KEY = "lq_prev";                    // what was on before this lock, e.g. "w1b0"
    public static final String LAST_KEY = "lq_last";                    // what it did last (Launcher control, report)
    public static final String SHOW = "lock_screen_show_notifications";
    public static final String PRIVATE = "lock_screen_allow_private_notifications";
    /**
     * AW3 (Tj, 2026-11-06 evening: "no wifi or Bluetooth ... ever" on the lock screen). v0.8.0 switched a radio that came back on off again at most 3 times per minute and then GAVE UP (the radio stayed on until the
     * next broadcast). Now it never gives up while the lock screen is up: a radio is switched off again as soon as {@link #MIN_GAP_MS} have passed since the last switch-off, and checked again after that, until it
     * stays off. Only a lock that has already spent {@link #MAX_PER_LOCK} switch-offs stops (a ROM that really insists must not drain the battery in a tight loop); the count starts again at every lock.
     */
    public static final long MIN_GAP_MS = 4_000;
    public static final int MAX_PER_LOCK = 40;

    private LockQuiet() {}

    public static boolean enabled(Context c) { return Features.get(c, KEY, true); }
    public static boolean restoreEnabled(Context c) { return Features.get(c, RESTORE_KEY, true); }
    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == android.content.pm.PackageManager.PERMISSION_GRANTED; }

    // ---------------------------------------------------------------- pure rules (JVM-tested)

    /**
     * Pure (AW3): switch a radio that is on OFF now (return 0), later (return the wait in ms; the caller checks again then) or not at all (-1)? Only while the keyguard is up, only with the switch on, never while
     * automation is paused, {@link #MIN_GAP_MS} apart, at most {@link #MAX_PER_LOCK} per lock. {@code lastWriteMs} 0 = none yet this lock.
     */
    public static long retryDelayMs(boolean switchOn, boolean keyguardLocked, boolean automationOff, long now, long lastWriteMs, int writesThisLock) {
        if (!switchOn || !keyguardLocked || automationOff) return -1;
        if (writesThisLock >= MAX_PER_LOCK) return -1;
        if (lastWriteMs <= 0 || now < lastWriteMs) return 0;
        long since = now - lastWriteMs;
        return since >= MIN_GAP_MS ? 0 : MIN_GAP_MS - since;
    }

    /** Pure: the memory of which radios were on before the lock. */
    public static String encodePrev(boolean wifi, boolean bluetooth) { return "w" + (wifi ? 1 : 0) + "b" + (bluetooth ? 1 : 0); }
    public static boolean prevWifi(String prev) { return prev != null && prev.contains("w1"); }
    public static boolean prevBluetooth(String prev) { return prev != null && prev.contains("b1"); }

    /** Pure: put the radios back now? Only for a GESTURE unlock into Quickstep, with both switches on, automation not paused, and something was on before the lock. */
    public static boolean shouldRestore(boolean switchOn, boolean restoreOn, boolean quickstepUnlock, boolean automationOff, String prev) {
        return switchOn && restoreOn && quickstepUnlock && !automationOff && (prevWifi(prev) || prevBluetooth(prev));
    }

    /** Pure: remember the radios at the lock moment? The first lock moment of a lock, or whenever a radio is on right now (that is the freshest truth). */
    public static boolean shouldRecordPrev(String stored, boolean wifiOnNow, boolean btOnNow) { return stored == null || stored.isEmpty() || wifiOnNow || btOnNow; }

    /**
     * Pure (review of BS2, finding 1): is the stored memory of "what was on before the lock" a leftover? Only an unlock the helper handled clears it ({@link #onUnlock}); a boot unlock before the helper bound (or a
     * helper that crashed during a lock) leaves it behind, and with the radios now kept on at every later lock nothing rewrites it, so a much later gesture unlock would switch on a radio that was off. When the helper
     * (re)connects while the screen is ON and the keyguard is NOT up no lock is in progress: whatever is stored belongs to a lock that has ended. (A lock that is just starting has the screen off or the keyguard up.)
     */
    public static boolean shouldForgetPrev(boolean keyguardLocked, boolean screenOn, String prev) { return !keyguardLocked && screenOn && prev != null && !prev.isEmpty(); }

    // ---------------------------------------------------------------- Android side

    private static String stamp() { return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()); }

    /** The notification half: hide (switch on) or put the original values back (switch off). Cheap when nothing changes. Never throws. */
    public static void applyNotifications(Context c) {
        try {
            ContentResolver cr = c.getContentResolver();
            if (enabled(c) && !CrashGuard.automationOff(c)) {
                if (!permitted(c)) { Features.setString(c, LAST_KEY, stamp() + "  the lock screen still shows notifications: the adb permission (WRITE_SECURE_SETTINGS) is missing"); return; }
                boolean changed = false;
                for (String k : new String[] { SHOW, PRIVATE }) {
                    RestoreStore.snapshotSecure(c, k);                       // the original, once
                    if (!"0".equals(Settings.Secure.getString(cr, k))) { Settings.Secure.putInt(cr, k, 0); changed = true; }
                }
                if (changed) { Features.setString(c, LAST_KEY, stamp() + "  lock screen: app notifications hidden"); Log.i(TAG, "lock-screen notifications hidden"); }
            } else {
                boolean any = RestoreStore.restoreSecure(c, SHOW) | RestoreStore.restoreSecure(c, PRIVATE);
                if (any) { Features.setString(c, LAST_KEY, stamp() + "  lock-screen notification setting put back"); Log.i(TAG, "lock-screen notification setting restored"); }
            }
        } catch (Throwable t) { Log.e(TAG, "notification setting failed", t); }
    }

    public static void applyNotificationsAsync(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { @Override public void run() { applyNotifications(app); } }, "tj-lockquiet-notif").start();
    }

    /** AW3: is Wi-Fi or Bluetooth on right now? Binder calls: from a worker thread or the main thread, never under a lock. */
    public static boolean anyRadioOn(Context c) { return (wifiOn(c) || btOn()) && !RadioIconHide.keepsRadiosOn(c); }   // BS1 + BS2: a radio we leave on (its symbol is hidden instead) is not "a radio that must go"

    private static boolean wifiOn(Context c) {
        try { WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE); return wm != null && wm.isWifiEnabled(); } catch (Throwable t) { return false; }
    }
    private static boolean btOn() {
        try { BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter(); return a != null && a.isEnabled(); } catch (Throwable t) { return false; }
    }

    /** The lock moment: remember what is on, switch Wi-Fi and Bluetooth off. Blocking binder calls: call from a worker thread (see {@link #onLockAsync}). */
    public static void onLock(Context c) {
        try {
            if (!enabled(c) || CrashGuard.automationOff(c)) return;
            final boolean keep = RadioIconHide.keepsRadiosOn(c);   // BS1 + BS2: while the symbols are hidden instead, the radios are not ours to switch off (and not ours to put back); nothing here ever switches one ON
            boolean w = !keep && wifiOn(c), b = !keep && btOn();
            if (shouldRecordPrev(Features.getString(c, PREV_KEY, ""), w, b)) Features.setString(c, PREV_KEY, encodePrev(w, b));
            if (w || b) {
                ReturnReset.radiosOff(c);
                Features.setString(c, LAST_KEY, stamp() + "  lock screen: " + (w ? "Wi-Fi " : "") + (b ? "Bluetooth " : "") + "switched off");
                Log.i(TAG, "lock screen: radios off (wifi=" + w + " bt=" + b + ")");
            }
        } catch (Throwable t) { Log.e(TAG, "lock radios failed", t); }
    }

    /**
     * AW2 / AW3: the app process starts early after a boot (it is the HOME app), long before the accessibility helper binds. If the lock screen is already up then, the radios are switched off and the
     * rotation-only tile list is written at once, and looked at again after a few seconds (the keyguard may not have settled when the process was forked). Worker thread, exception-safe, silent when the tablet
     * is not locked, the switches are off, the adb permission is missing or automation is paused.
     */
    public static void applyLockRulesEarly(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { @Override public void run() {
            for (int i = 0; i < 3; i++) {
                try {
                    if (i > 0) Thread.sleep(i == 1 ? 3000 : 9000);
                    if (CrashGuard.automationOff(app) || !TileProfile.keyguardLocked(app)) continue;
                    if (enabled(app)) onLock(app);
                    if (TileProfile.enabled(app) && TileProfile.permitted(app)) TileProfile.apply(app, Mode.get(app));
                    Log.i(TAG, "early lock rules applied (look " + (i + 1) + ")");
                } catch (Throwable t) { Log.e(TAG, "early lock rules failed", t); }
            }
        } }, "tj-lockquiet-early").start();
    }

    public static void onLockAsync(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { @Override public void run() { onLock(app); } }, "tj-lockquiet-lock").start();
    }

    /** An unlock was handled: forget the memory of this lock; after a gesture unlock into Quickstep put the radios back. Blocking: call from a worker thread (see {@link #onUnlockAsync}). */
    public static void onUnlock(Context c, boolean quickstep) {
        try {
            String prev = Features.getString(c, PREV_KEY, "");
            if (!prev.isEmpty()) Features.setString(c, PREV_KEY, "");
            if (!shouldRestore(enabled(c), restoreEnabled(c), quickstep, CrashGuard.automationOff(c), prev)) return;
            if (prevWifi(prev)) { WifiManager wm = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE); if (wm != null && !wm.isWifiEnabled()) wm.setWifiEnabled(true); }
            if (prevBluetooth(prev)) { BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter(); if (a != null && !a.isEnabled()) a.enable(); }
            Features.setString(c, LAST_KEY, stamp() + "  gesture unlock into Quickstep: " + (prevWifi(prev) ? "Wi-Fi " : "") + (prevBluetooth(prev) ? "Bluetooth " : "") + "put back");
            Log.i(TAG, "radios put back after a gesture unlock (" + prev + ")");
        } catch (Throwable t) { Log.e(TAG, "unlock restore failed", t); }
    }

    /** Called when the helper (re)connects: forgets a leftover lock memory ({@link #shouldForgetPrev}). Worker thread (binder reads and a disk write), exception-safe, silent when there is nothing to forget. */
    public static void forgetStalePrevAsync(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { @Override public void run() {
            try {
                String prev = Features.getString(app, PREV_KEY, "");
                if (prev.isEmpty()) return;
                android.os.PowerManager pm = (android.os.PowerManager) app.getSystemService(Context.POWER_SERVICE);
                if (shouldForgetPrev(TileProfile.keyguardLocked(app), pm != null && pm.isInteractive(), prev)) {
                    Features.setString(app, PREV_KEY, "");
                    Log.i(TAG, "stale lock memory forgotten (" + prev + "): the helper connected while no lock was in progress");
                }
            } catch (Throwable t) { Log.e(TAG, "forgetting the stale lock memory failed", t); }
        } }, "tj-lockquiet-forget").start();
    }

    public static void onUnlockAsync(final Context c, final boolean quickstep) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() { @Override public void run() { onUnlock(app, quickstep); } }, "tj-lockquiet-unlock").start();
    }
}
