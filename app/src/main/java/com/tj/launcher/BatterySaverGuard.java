package com.tj.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * AC3: turns battery saver off the moment the custom launcher becomes active. See {@link BatterySaverPolicy} for the why and for AZ2 (the reason this class no longer switches the saver ON and heals instead).
 * This class is the Android side: a ContentObserver on {@code Settings.Global low_power} (log + the opt-in strict mode), a ContentObserver on the "manual activation" counter (the self-heal), the one-time
 * unstick, and the check whenever the custom launcher becomes active. All registered once per process from {@link TjApplication} / the accessibility service. Cheap, exception-safe, never throws.
 * Quickstep is never touched. Needs the WRITE_SECURE_SETTINGS grant (already part of the adb setup): without it the guard does nothing and says so in its status line.
 */
public final class BatterySaverGuard {
    private static final String TAG = "TjBatSaver";
    private static final long[] writes = new long[BatterySaverPolicy.MAX_WRITES];
    private static int writeIdx = 0;
    private static final long[] heals = new long[BatterySaverPolicy.MAX_HEALS];
    private static int healIdx = 0;
    private static int lastCount = -1;          // the last value of the "manual activation" counter we saw (-1 = not read yet)
    private static ContentObserver observer;    // held strongly: the content resolver only keeps a weak reference
    private static ContentObserver countObserver;
    private static Handler main;
    /** Review of v0.8.1: when low_power last CHANGED, and when the last accepted turn-ON request was counted. A request that Android really carried out changes low_power (even if the person turns it off again 100 ms later); a SWALLOWED one changes nothing, so only that one is healed. */
    private static volatile long lowPowerChangedAt = 0, bumpAt = 0;

    private BatterySaverGuard() {}

    public static boolean enabled(Context c) { return Features.get(c, BatterySaverPolicy.KEY, true); }
    /** AN1: strict = also undo a saver that is turned ON while the custom launcher is active (the old v0.7.3 - v0.7.6 behaviour); default OFF so the tile in the shade always answers. */
    public static boolean strict(Context c) { return Features.get(c, BatterySaverPolicy.STRICT_KEY, false); }
    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == android.content.pm.PackageManager.PERMISSION_GRANTED; }

    /** Registers the observers once per process (idempotent), clears a saver an older version left stuck, and checks once. Called from {@link TjApplication#onCreate} and the accessibility service. */
    public static synchronized void start(Context c) {
        try {
            if (observer != null) return;
            final Context app = c.getApplicationContext();
            main = new Handler(Looper.getMainLooper());
            observer = new ContentObserver(main) {
                @Override public void onChange(boolean selfChange) {
                    lowPowerChangedAt = System.currentTimeMillis();
                    try { StockForceStop.noteLowPower(app, Settings.Global.getInt(app.getContentResolver(), BatterySaverPolicy.SETTING, 0)); } catch (Throwable ignored) { }   // CT1 (SR2): every change of the saver is a uiMode flip for the stock launcher (debt for the helper-free Force stop)
                    // AG2 (Tj, 2026-11-06: the battery saver TILE in Quickstep does not work): every change of low_power is written to the lock log with the mode and the guard's switches, so a report says WHY
                    try { LockLog.add(app, "BATSAVER", "low_power is now " + Settings.Global.getString(app.getContentResolver(), BatterySaverPolicy.SETTING) + ", mode=" + Mode.get(app) + ", off-in-custom=" + enabled(app) + ", strict=" + strict(app) + ", power=" + powerSource(app) + (CrashGuard.automationOff(app) ? " PAUSED" : "")); } catch (Throwable ignored) { }
                    async("tj-bs-changed", new Runnable() { @Override public void run() { onSettingChanged(app); } });   // BA2: Quickstep's own state is remembered; a saver turned on while the lock screen is up is switched off again (off the main thread: it writes preferences)
                    check(app, "setting changed", true);   // AN1: does nothing unless "strict" is on: a change made on the tile is left alone
                }
            };
            app.getContentResolver().registerContentObserver(Settings.Global.getUriFor(BatterySaverPolicy.SETTING), false, observer);
            try { StockForceStop.seedLowPower(Settings.Global.getInt(app.getContentResolver(), BatterySaverPolicy.SETTING, 0)); } catch (Throwable ignored) { }   // CT1: the first flip after a process start counts too
            // AZ2: the self-heal. Android raises this counter for every request to turn the saver ON that it ACCEPTED (even one its state machine then ignores because a direct write put it out of step).
            lastCount = readCount(app);
            countObserver = new ContentObserver(main) { @Override public void onChange(boolean selfChange) { onActivationCount(app); } };
            app.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(BatterySaverPolicy.COUNT_SETTING), false, countObserver);
            unstickOnce(app);
            // AN1: the rule is re-applied at a process start only in the first minutes after a boot; a restart of our process later (a Go device kills background processes often) must never undo a tap on the tile
            if (BatterySaverPolicy.startCheckWanted(strict(app), android.os.SystemClock.elapsedRealtime())) check(app, "process start");
        } catch (Throwable t) { Log.e(TAG, "could not watch battery saver", t); }
    }

    /** Switches battery saver off now if the rules say so. {@code why} is for the log / status line only. A DELIBERATE check (a mode change, the process start): the switch-off that follows is never capped. */
    public static void check(Context c, String why) { check(c, why, false, false); }

    static void check(Context c, String why, boolean reactive) { check(c, why, reactive, false); }

    /**
     * {@code reactive}: called because the setting CHANGED (something turned the saver on): only these switch-offs count toward the cap that protects against a tight loop.
     * {@code fromQuickstep} (BA2): a Quickstep session has just ended, so the saver's state RIGHT NOW is Quickstep's state: it is remembered before the switch-off so a later gesture unlock can bring it back.
     */
    static void check(Context c, String why, boolean reactive, boolean fromQuickstep) {
        try {
            if (!BatterySaverPolicy.mayAct(reactive, strict(c))) return;   // AN1: a change behind our back (the tile, Android) is left alone unless "strict" is on
            ContentResolver cr = c.getContentResolver();
            boolean on = BatterySaverPolicy.isOn(Settings.Global.getString(cr, BatterySaverPolicy.SETTING));
            if (fromQuickstep) rememberQuickstep(c, on);   // BA2: before anything is written, also when it is off (a stale "on" memory is cleared)
            if (!on) return;   // nothing to do (one settings read); AZ2: the saver is never switched ON from here (Quickstep is stock)
            long now = System.currentTimeMillis();
            synchronized (BatterySaverGuard.class) {
                if (!BatterySaverPolicy.shouldTurnOff(enabled(c), Mode.isCustom(c), CrashGuard.automationOff(c), true, now, writes)) return;
                if (!permitted(c)) { Features.setString(c, BatterySaverPolicy.LAST_KEY, stamp(now) + "  battery saver is on but the adb permission (WRITE_SECURE_SETTINGS) is missing"); return; }
                writes[writeIdx++ % writes.length] = now;
            }
            writeOff(c, cr, now);
            Features.setString(c, BatterySaverPolicy.LAST_KEY, stamp(now) + "  switched battery saver off in the custom launcher (" + why + ")");
            Log.i(TAG, "BATSAVER off (" + why + ")");
            LockLog.add(c, "BATSAVER", "guard switched it OFF (custom launcher active; " + why + ")");   // AG2
        } catch (Throwable t) { Log.e(TAG, "battery saver check failed", t); }
    }

    /** The one place that writes low_power = 0 (and the sticky flag): stamps {@link #selfWriteAt} (the observer must not take the echo for the person's choice) and starts / keeps the hold. */
    private static void writeOff(Context c, ContentResolver cr, long now) {
        selfWriteAt = now;
        Settings.Global.putInt(cr, BatterySaverPolicy.SETTING, 0);
        if (BatterySaverPolicy.isOn(Settings.Global.getString(cr, BatterySaverPolicy.STICKY_SETTING))) Settings.Global.putInt(cr, BatterySaverPolicy.STICKY_SETTING, 0);
        markHeld(c, now);
    }

    // ---------------------------------------------------------------- BA2: the lock screen keeps the saver OFF; a gesture unlock into Quickstep brings Quickstep's own state back

    /** When we last wrote low_power ourselves (the observer ignores the echo of that), and when the last restore was done (the keep-off rule leaves the unlock alone for a few seconds). */
    private static volatile long selfWriteAt = 0, restoredAt = 0;
    private static final long[] lockWrites = new long[BatterySaverPolicy.MAX_LOCK_WRITES];
    private static int lockWriteIdx = 0;
    private static final long[] restores = new long[BatterySaverPolicy.MAX_RESTORES];
    private static int restoreIdx = 0;

    /** Launcher control: the lock screen always has the saver off (any launcher). Default ON. */
    public static boolean lockOffEnabled(Context c) { return Features.get(c, BatterySaverPolicy.LOCK_OFF_KEY, true); }
    /** Launcher control: a gesture unlock into Quickstep brings Quickstep's own saver state back. Default ON. */
    public static boolean lockRestoreEnabled(Context c) { return Features.get(c, BatterySaverPolicy.LOCK_RESTORE_KEY, true); }

    private static long bootEpoch() { return System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime(); }

    /** The battery level in percent, -1 when it cannot be read. */
    static int batteryLevel(Context c) {
        try {
            android.content.Intent i = c.registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return -1;
            int level = i.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1), scale = i.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            return level < 0 || scale <= 0 ? -1 : Math.round(level * 100f / scale);
        } catch (Throwable t) { return -1; }
    }

    private static boolean saverOn(Context c) { return BatterySaverPolicy.isOn(Settings.Global.getString(c.getContentResolver(), BatterySaverPolicy.SETTING)); }

    /** BA2: a lock or unlock event: has the battery level risen since the last one? Then the tablet was charged meanwhile, and (while the guard holds the saver off) Android's own state was reset. */
    private static void noteLevel(Context c) {
        try {
            int last = -1;
            try { last = Integer.parseInt(Features.getString(c, BatterySaverPolicy.LEVEL_KEY, "-1")); } catch (NumberFormatException ignored) { }
            int now = batteryLevel(c);
            if (Features.get(c, BatterySaverPolicy.HELD_KEY, false) && BatterySaverPolicy.levelRose(last, now)) Features.setString(c, BatterySaverPolicy.PLUG_SEEN_KEY, "1");
            if (now >= 0) Features.setString(c, BatterySaverPolicy.LEVEL_KEY, String.valueOf(now));
        } catch (Throwable ignored) { }
    }

    /** BA2: starts a hold (the saver is off BECAUSE of the guard) unless one is running: the facts the restore will check are recorded now. */
    private static synchronized void markHeld(Context c, long now) {
        if (Features.get(c, BatterySaverPolicy.HELD_KEY, false)) return;
        // asynchronous writes: this can run on the main thread (Mode.set at a plain unlock); the in-memory values are visible at once
        Features.setAsync(c, BatterySaverPolicy.HELD_KEY, true);
        Features.setStringAsync(c, BatterySaverPolicy.HELD_AT_KEY, String.valueOf(now));
        Features.setStringAsync(c, BatterySaverPolicy.HELD_BOOT_KEY, String.valueOf(bootEpoch()));
        Features.setStringAsync(c, BatterySaverPolicy.PLUG_SEEN_KEY, "0");
        int lv = batteryLevel(c);
        if (lv >= 0) Features.setStringAsync(c, BatterySaverPolicy.LEVEL_KEY, String.valueOf(lv));
    }

    private static long heldAt(Context c) { try { return Long.parseLong(Features.getString(c, BatterySaverPolicy.HELD_AT_KEY, "0")); } catch (NumberFormatException e) { return 0; } }
    private static long realQsStart(Context c) { try { return Long.parseLong(Features.getString(c, BatterySaverPolicy.QS_REAL_START_KEY, "0")); } catch (NumberFormatException e) { return 0; } }

    /**
     * Review of BA2 (finding 1): called by {@link Mode#set} when Quickstep begins. Only a session that begins while the tablet is UNLOCKED counts as a new Quickstep session (a secret entrance, a return method):
     * the handshake flips the mode while the lock screen is still up, and when that handshake is armed and then abandoned the hold (and Quickstep's memory) must survive it.
     */
    public static void noteQuickstepBegins(Context c) {
        try { if (!TileProfile.keyguardLocked(c)) Features.setStringAsync(c, BatterySaverPolicy.QS_REAL_START_KEY, String.valueOf(System.currentTimeMillis())); } catch (Throwable ignored) { }
    }

    /** Review of BA2 (nit): a thread that cannot be started (out of memory) must never throw into the main thread or the lock path. */
    private static void async(String name, Runnable r) {
        try { new Thread(r, name).start(); } catch (Throwable t) { Log.e(TAG, "could not start " + name, t); }
    }

    /**
     * BA2: remember what Quickstep's saver state is at the moment the guard is about to switch it off (or finds it off). A hold that began before the current Quickstep session is not this session's
     * ({@link BatterySaverPolicy#holdStillValid}): it is dropped first, so the live state is what counts.
     */
    private static synchronized void rememberQuickstep(Context c, boolean on) {
        // both callers (a lock from Quickstep, the end of a Quickstep session) are about a Quickstep session, so the session question is always "yes": did THAT session begin after the hold?
        boolean held = BatterySaverPolicy.holdStillValid(Features.get(c, BatterySaverPolicy.HELD_KEY, false), heldAt(c), realQsStart(c), true);
        if (!held && Features.get(c, BatterySaverPolicy.HELD_KEY, false)) Features.setAsync(c, BatterySaverPolicy.HELD_KEY, false);   // a Quickstep session began after that hold: start over
        String stored = Features.getString(c, BatterySaverPolicy.QS_ON_KEY, "");
        String next = BatterySaverPolicy.qsStateAtHold(true, held, on, stored);
        if (!next.equals(stored)) Features.setStringAsync(c, BatterySaverPolicy.QS_ON_KEY, next);
    }

    /**
     * BA2: THE LOCK MOMENT (screen off while locked, or the first locked wake): whichever launcher it came from, the lock screen has battery saver OFF. Quickstep's own state is remembered first. A saver that is
     * already off needs no write. Blocking settings calls: call from a worker thread ({@link #onLockAsync}). Never throws; does nothing when switched off, paused, or without the adb permission.
     */
    public static void onLock(Context c, String why) {
        try {
            noteLevel(c);
            if (!lockOffEnabled(c) || CrashGuard.automationOff(c)) return;
            boolean locked = TileProfile.keyguardLocked(c);
            if (!locked) return;
            boolean on = saverOn(c);
            boolean qs = Mode.QUICKSTEP.equals(Mode.get(c));
            if (qs) rememberQuickstep(c, on);   // locked from Quickstep: the state right now is Quickstep's (also when it is off: a stale "on" is cleared)
            if (!BatterySaverPolicy.lockOffWanted(true, locked, false, on)) return;
            if (!permitted(c)) { Features.setString(c, BatterySaverPolicy.NOTE_KEY, stamp(System.currentTimeMillis()) + "  the lock screen has battery saver on: the adb permission (WRITE_SECURE_SETTINGS) is missing"); return; }
            switchOffForLock(c, "lock screen: " + why, false);
        } catch (Throwable t) { Log.e(TAG, "lock battery saver failed", t); }
    }

    public static void onLockAsync(final Context c, final String why) {
        final Context app = c.getApplicationContext();
        async("tj-bs-lock", new Runnable() { @Override public void run() { onLock(app, why); } });
    }

    /** One switch-off for the lock screen. {@code reactive}: something turned the saver on while the lock screen is up (capped, see {@link BatterySaverPolicy#keepOffWanted}). True when it wrote. */
    private static boolean switchOffForLock(Context c, String why, boolean reactive) {
        long now = System.currentTimeMillis();
        synchronized (BatterySaverGuard.class) {
            if (reactive) {
                if (!BatterySaverPolicy.keepOffWanted(true, true, false, true, now, restoredAt, lockWrites)) return false;
                lockWrites[lockWriteIdx++ % lockWrites.length] = now;
            }
        }
        writeOff(c, c.getContentResolver(), now);
        Features.setString(c, BatterySaverPolicy.NOTE_KEY, stamp(now) + "  battery saver switched off (" + why + ")");
        Log.i(TAG, "BATSAVER lock off (" + why + ")");
        LockLog.add(c, "BATSAVER", "lock rule: switched OFF (" + why + "); " + BatterySaverPolicy.describe(Features.getString(c, BatterySaverPolicy.QS_ON_KEY, ""), true, false));
        return true;
    }

    /**
     * BA2: the observer saw {@code low_power} change. (1) While Quickstep has the tablet and the person made the change, that is Quickstep's state now (and the guard's hold is over). (2) While the lock screen is
     * up and the saver came on again (Android's low-battery trigger, a notification button): switch it off again (capped). Never on our own echo.
     */
    private static void onSettingChanged(final Context app) {
        try {
            long ts = System.currentTimeMillis();
            boolean own = ts - selfWriteAt < 1500;
            boolean on = saverOn(app);
            boolean locked = TileProfile.keyguardLocked(app);
            boolean quickstep = Mode.QUICKSTEP.equals(Mode.get(app));
            String stored = Features.getString(app, BatterySaverPolicy.QS_ON_KEY, "");
            String next = BatterySaverPolicy.qsStateLive(quickstep, locked, own, on, stored);
            if (!next.equals(stored)) Features.setString(app, BatterySaverPolicy.QS_ON_KEY, next);
            // Review of BA2 (finding 2): while the guard holds the saver off, a change that is not ours and arrives while the tablet is UNLOCKED (the person's "on" / "off" in Settings, Android's own trigger) moves Android's
            // state machine on (a manual "off" = OFF). Switching the saver on from outside after that is the AZ2 bug again, so the hold ends here: nothing is restored any more.
            if (!own && !locked && Features.get(app, BatterySaverPolicy.HELD_KEY, false)) {
                Features.set(app, BatterySaverPolicy.HELD_KEY, false);
                LockLog.add(app, "BATSAVER", "the saver was changed by the person (or Android) while the launcher held it off: nothing will be switched back on (low_power=" + (on ? 1 : 0) + ")");
            }
            if (!own && locked && on && lockOffEnabled(app) && !CrashGuard.automationOff(app) && permitted(app)) {
                async("tj-bs-keepoff", new Runnable() { @Override public void run() { try { switchOffForLock(app, "something turned it on while the lock screen is up", true); } catch (Throwable t) { Log.e(TAG, "keep-off failed", t); } } });
            }
        } catch (Throwable t) { Log.e(TAG, "battery saver change failed", t); }
    }

    /**
     * BA2: AN UNLOCK was handled. Only a GESTURE unlock into Quickstep brings the saver back, and only when Android's own state cannot have been reset (see {@link BatterySaverPolicy#restoreRefusal}: not plugged in
     * now, not plugged in or charged meanwhile, the same boot): the guard's switch-off left Android in "on" (MANUAL_ON / AUTOMATIC_ON), so one write of low_power = 1 puts the saver and Android's state in line again
     * and the tile / Settings can turn it off. A plain unlock (the custom launcher) leaves the saver off and keeps the memory for the next gesture unlock. Whatever happens, what is on afterwards is Quickstep's state.
     * Blocking: worker thread ({@link #onUnlockAsync}).
     */
    public static void onUnlock(Context c, boolean quickstepGesture) {
        try {
            final long begin = System.currentTimeMillis();
            noteLevel(c);
            if (CrashGuard.automationOff(c)) return;
            if (!Features.get(c, BatterySaverPolicy.HELD_KEY, false)) return;
            if (!quickstepGesture) return;   // plain unlock = the custom launcher: the saver stays off; the hold and the memory wait for a later gesture unlock
            for (int i = 0; i < 5 && TileProfile.keyguardLocked(c); i++) Thread.sleep(400);   // the keyguard can report "locked" for a moment after the unlock broadcast
            long now = System.currentTimeMillis();
            BatterySaverPolicy.Restore s = new BatterySaverPolicy.Restore();
            s.switchOn = lockOffEnabled(c); s.restoreOn = lockRestoreEnabled(c); s.automationOff = false; s.permitted = permitted(c);
            s.quickstepGesture = Mode.QUICKSTEP.equals(Mode.get(c));
            s.held = true;
            s.qsOn = Features.getString(c, BatterySaverPolicy.QS_ON_KEY, "");
            s.saverOnNow = saverOn(c);
            s.keyguardLocked = TileProfile.keyguardLocked(c);
            s.chargingOrUnknown = !"no".equals(powerSource(c));
            s.plugSeen = "1".equals(Features.getString(c, BatterySaverPolicy.PLUG_SEEN_KEY, "0"));
            try { s.heldBootEpoch = Long.parseLong(Features.getString(c, BatterySaverPolicy.HELD_BOOT_KEY, "0")); } catch (NumberFormatException e) { s.heldBootEpoch = 0; }
            s.nowBootEpoch = bootEpoch(); s.now = now;
            synchronized (BatterySaverGuard.class) { s.recentRestores = restores.clone(); }
            String why = BatterySaverPolicy.restoreRefusal(s);
            if (why.isEmpty()) {
                synchronized (BatterySaverGuard.class) { restores[restoreIdx++ % restores.length] = now; }
                selfWriteAt = now; restoredAt = now;
                Settings.Global.putInt(c.getContentResolver(), BatterySaverPolicy.SETTING, 1);
                Features.setInt(c, BatterySaverPolicy.RESTORES_KEY, Features.getInt(c, BatterySaverPolicy.RESTORES_KEY, 0) + 1);
                Features.setString(c, BatterySaverPolicy.NOTE_KEY, stamp(now) + "  gesture unlock into Quickstep: battery saver switched back on (it was on the last time in Quickstep)");
                Log.i(TAG, "BATSAVER restored for Quickstep");
                LockLog.add(c, "BATSAVER", "gesture unlock into Quickstep: low_power set to 1 (Quickstep had it on)");
            } else {
                Features.setString(c, BatterySaverPolicy.NOTE_KEY, stamp(now) + "  gesture unlock into Quickstep: battery saver left off (" + why + ")");
                Log.i(TAG, "BATSAVER not restored: " + why);
                LockLog.add(c, "BATSAVER", "gesture unlock into Quickstep: NOT switched back on (" + why + ")");
            }
            Thread.sleep(300);
            if (heldAt(c) > begin) return;   // review nit: the person locked the tablet again within these moments and a NEW hold began: it is not ours to wipe
            // Quickstep has the tablet again: whatever happened above, what is on now is Quickstep's state and the guard holds nothing
            Features.set(c, BatterySaverPolicy.HELD_KEY, false);
            Features.setString(c, BatterySaverPolicy.QS_ON_KEY, saverOn(c) ? "1" : "0");
        } catch (Throwable t) { Log.e(TAG, "unlock battery saver failed", t); }
    }

    public static void onUnlockAsync(final Context c, final boolean quickstepGesture) {
        final Context app = c.getApplicationContext();
        async("tj-bs-unlock", new Runnable() { @Override public void run() { onUnlock(app, quickstepGesture); } });
    }

    /** BA2: a charger was plugged in: while the guard holds the saver off, Android's own state is reset by that, so the saver is never switched back on from outside. */
    public static void onPowerEvent(Context c, boolean connected) {
        try { if (connected && Features.get(c, BatterySaverPolicy.HELD_KEY, false)) Features.setStringAsync(c, BatterySaverPolicy.PLUG_SEEN_KEY, "1"); } catch (Throwable ignored) { }
    }

    /** One line for Launcher control and the tablet report. */
    public static String lockStatus(Context c) {
        try {
            return BatterySaverPolicy.describe(Features.getString(c, BatterySaverPolicy.QS_ON_KEY, ""), Features.get(c, BatterySaverPolicy.HELD_KEY, false), "1".equals(Features.getString(c, BatterySaverPolicy.PLUG_SEEN_KEY, "0")))
                    + "; last: " + Features.getString(c, BatterySaverPolicy.NOTE_KEY, "(nothing yet)") + "; restores so far " + Features.getInt(c, BatterySaverPolicy.RESTORES_KEY, 0);
        } catch (Throwable t) { return "?"; }
    }

    // ---------------------------------------------------------------- AZ2: the self-heal

    private static int readCount(Context c) {
        try { return Settings.Secure.getInt(c.getContentResolver(), BatterySaverPolicy.COUNT_SETTING, 0); } catch (Throwable t) { return -1; }
    }

    /** The "manual activation" counter changed: somebody asked (tile, Settings) to turn the saver ON and Android accepted the request. A moment later see whether it really is on. */
    private static void onActivationCount(final Context app) {
        try {
            int now = readCount(app), before = lastCount;
            lastCount = now;
            if (!BatterySaverPolicy.activationRequested(before, now) || main == null) return;
            bumpAt = System.currentTimeMillis();
            main.postDelayed(new Runnable() { @Override public void run() { healIfSwallowed(app); } }, BatterySaverPolicy.HEAL_DELAY_MS);
        } catch (Throwable t) { Log.e(TAG, "activation count failed", t); }
    }

    /**
     * Android accepted a request to turn the saver ON but {@code low_power} is still 0: its state machine is out of step (our own switch-off in the custom launcher left it in MANUAL_ON / AUTOMATIC_ON) and
     * ignored the request. Carry it out ourselves: one write of low_power = 1 (Android switches the saver on and the state machine agrees again, so the next "turn off" works). Deliberately NOT tied to the
     * guard's switches or to "automation paused": this only completes what the person asked for, and "Restore everything" must leave the tile working.
     */
    static void healIfSwallowed(Context c) {
        try {
            if (lowPowerChangedAt >= bumpAt - 20) { Log.i(TAG, "BATSAVER no heal: low_power changed after the request (it was carried out, then possibly undone)"); return; }
            ContentResolver cr = c.getContentResolver();
            boolean on = BatterySaverPolicy.isOn(Settings.Global.getString(cr, BatterySaverPolicy.SETTING));
            String src = powerSource(c);
            long now = System.currentTimeMillis();
            synchronized (BatterySaverGuard.class) {
                if (!BatterySaverPolicy.healWanted(on, BatterySaverPolicy.charging(src), now, heals)) return;
                if (!permitted(c)) { Features.setString(c, BatterySaverPolicy.LAST_KEY, stamp(now) + "  your tap to turn battery saver ON was ignored by Android but the adb permission (WRITE_SECURE_SETTINGS) is missing, so it cannot be completed"); return; }
                heals[healIdx++ % heals.length] = now;
            }
            Settings.Global.putInt(cr, BatterySaverPolicy.SETTING, 1);
            Features.setInt(c, BatterySaverPolicy.HEALS_KEY, Features.getInt(c, BatterySaverPolicy.HEALS_KEY, 0) + 1);
            Features.setString(c, BatterySaverPolicy.LAST_KEY, stamp(now) + "  your tap to turn battery saver ON was ignored by Android (its internal state was out of step after the launcher switched it off); switched it on for you");
            Log.i(TAG, "BATSAVER healed: a swallowed turn-ON was carried out");
            LockLog.add(c, "BATSAVER", "healed: the turn-ON request was swallowed by Android's state machine; low_power set to 1 (power=" + src + ")");
        } catch (Throwable t) { Log.e(TAG, "battery saver heal failed", t); }
    }

    /**
     * AZ2: v0.7.5 - v0.8.0 forced the saver ON when Quickstep began (a direct write). Android's state machine then thinks it is OFF, so the tile and Settings cannot turn it off ("it says it turned off but it
     * never does"). Once, after the update: if the guard's own last note says it forced the saver ON and it is still on, switch it off (OFF/OFF is a consistent pair again; turning it on by hand then works).
     */
    private static void unstickOnce(Context c) {
        try {
            if (Features.get(c, BatterySaverPolicy.UNSTICK_DONE_KEY, false)) return;
            ContentResolver cr = c.getContentResolver();
            boolean on = BatterySaverPolicy.isOn(Settings.Global.getString(cr, BatterySaverPolicy.SETTING));
            String last = Features.getString(c, BatterySaverPolicy.LAST_KEY, "");
            if (BatterySaverPolicy.unstickWanted(false, on, last)) {
                if (!permitted(c)) return;   // try again when the permission is there
                Settings.Global.putInt(cr, BatterySaverPolicy.SETTING, 0);
                if (BatterySaverPolicy.isOn(Settings.Global.getString(cr, BatterySaverPolicy.STICKY_SETTING))) Settings.Global.putInt(cr, BatterySaverPolicy.STICKY_SETTING, 0);
                long now = System.currentTimeMillis();
                Features.setString(c, BatterySaverPolicy.LAST_KEY, stamp(now) + "  switched off a battery saver that an older version had forced on (Android could no longer turn it off by hand)");
                Log.i(TAG, "BATSAVER unstuck: cleared a saver forced on by an older version");
                LockLog.add(c, "BATSAVER", "unstuck: cleared a saver forced on by an older version");
            }
            Features.set(c, BatterySaverPolicy.UNSTICK_DONE_KEY, true);
        } catch (Throwable t) { Log.e(TAG, "battery saver unstick failed", t); }
    }

    /** "ac" / "usb" / "wireless" / "no" (not plugged in) / "?": the plugged-in state for the log and the report, because Android switches the saver off by itself while charging. Never throws. */
    static String powerSource(Context c) {
        try {
            android.content.Intent i = c.registerReceiver(null, new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return "?";
            int p = i.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0);
            if (p == 0) return "no";
            if ((p & android.os.BatteryManager.BATTERY_PLUGGED_AC) != 0) return "ac";
            if ((p & android.os.BatteryManager.BATTERY_PLUGGED_USB) != 0) return "usb";
            if ((p & android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0) return "wireless";
            return "other";
        } catch (Throwable t) { return "?"; }
    }

    private static String stamp(long ms) { return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date(ms)); }
}
