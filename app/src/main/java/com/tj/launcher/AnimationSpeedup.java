package com.tj.launcher;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/**
 * The three global animation scales (window / transition / animator duration = what the Accessibility "Remove animations" switch writes). Needs WRITE_SECURE_SETTINGS (the same one-time adb grant as the keyboard
 * switch); without it nothing changes. Two independent REASONS may hold them at 0, kept as a bit mask ({@link AnimationPolicy#HOLD_RESET}, {@link AnimationPolicy#HOLD_QUICKSTEP}); the scales go back as soon as
 * the LAST reason ends:
 * <ul>
 * <li><b>The return reset</b> (Y2b, Tj 2026-11-03 "as fast as possible, instant if possible"): most of the return reset is animation time (shade opens, Recents opens, cards fly off); {@link #begin} / {@link #restoreLater}.</li>
 * <li><b>The Quickstep session</b> (BF1, Tj 2026-11-07 23:36 "turn off all animations ... Animations should only ever be turned off inside quickstep launcher ... turned back on automatically and immediately on both the
 * lock screen and in the custom launcher"): LEVEL-TRIGGERED. {@link #syncNow} re-reads the facts ({@link AnimationPolicy#why}) at every trigger and sets or clears the hold, so no missed event can leave the animations
 * off: {@code Mode.set}, screen off / on, the unlock decision, the helper connecting / going away, the start of the process, a window-change heartbeat ({@link #tick}) and "Restore everything".</li>
 * </ul>
 * Safety (L10, now for a LONG hold): the original values are written to our prefs BEFORE anything is changed; a scale is put back only if it is still at OUR zero (a value Tj changed by hand in Developer options
 * meanwhile is his), and only when its original was not zero itself (Tj's own permanent setting is never touched); the holds end (1) when their reason does, (2) by the reset's 10 s failsafe, (3) at the next start of the
 * main process (a reset never outlives its process; the Quickstep hold is re-decided from the facts AT ONCE, and once more after 2.5 s without the helper), (4) by "Restore everything" and the crash breaker.
 */
public final class AnimationSpeedup {
    private static final String TAG = "TjAnim";
    public static final String KEY = "reset_animations_off";   // the RESET's switch, default ON (only does anything when the adb grant exists)
    private static final String PREFS = "tj_anim";
    private static final String[] NAMES = { "window_animation_scale", "transition_animation_scale", "animator_duration_scale" };
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AnimationSpeedup() {}

    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED; }
    public static boolean enabled(Context c) { return Features.get(c, KEY, true); }
    /** BF1: the Quickstep switch (shipped default ON, debug builds OFF: {@link AnimationPolicy#defaultOn}). */
    public static boolean quickstepSwitch(Context c) { return Features.get(c, AnimationPolicy.KEY, AnimationPolicy.defaultOn(ReturnReset.isDebuggable(c))); }
    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    // ================================================================ the holds and the writes (the only code that touches the scales)

    /**
     * Sets or clears ONE reason and does what follows from the mask (the rules live in {@link AnimationHolds}, JVM-tested): the first reason saves the originals (disk first) and writes the zeros; further reasons
     * rewrite nothing; the last one ending puts the originals back. Returns the mask afterwards. Never throws. Serialised with everything else in this class.
     */
    private static synchronized int setHold(Context c, int bit, boolean on) {
        Context app = c.getApplicationContext();
        try {
            return AnimationHolds.set(new PrefsState(p(app)), new GlobalScales(app.getContentResolver()), NAMES, permitted(app), bit, on, SINK);
        } catch (Throwable t) {
            Log.e(TAG, "animation hold change failed", t);
            return holds(app);
        }
    }

    private static final AnimationHolds.Sink SINK = new AnimationHolds.Sink() { @Override public void log(String message) { Log.i(TAG, message); } };

    /** The saved state, in our preferences: the mask, `pending`, and the three originals. {@code save} throws when the commit failed, so a change is never made without its originals on disk. */
    private static final class PrefsState implements AnimationHolds.State {
        private final SharedPreferences sp;
        PrefsState(SharedPreferences sp) { this.sp = sp; }
        @Override public int holds() { return sp.getInt("holds", 0); }
        @Override public boolean pending() { return sp.getBoolean("pending", false); }
        @Override public boolean hasOriginal(String scale) { return sp.contains(scale); }
        @Override public String original(String scale) { return sp.getString(scale, "1.0"); }
        @Override public void save(int holds, boolean pending, java.util.Map<String, String> originals) {
            SharedPreferences.Editor e = sp.edit();
            if (originals != null) for (java.util.Map.Entry<String, String> en : originals.entrySet()) e.putString(en.getKey(), en.getValue());
            e.putBoolean("pending", pending).putInt("holds", holds);
            if (!e.commit()) throw new IllegalStateException("could not save the animation state");
        }
    }

    /** The three global scales. */
    private static final class GlobalScales implements AnimationHolds.Scales {
        private final android.content.ContentResolver cr;
        GlobalScales(android.content.ContentResolver cr) { this.cr = cr; }
        @Override public String get(String scale) { return Settings.Global.getString(cr, scale); }
        @Override public void put(String scale, String value) { if (!Settings.Global.putString(cr, scale, value)) throw new IllegalStateException("could not write " + scale); }
    }

    private static int holds(Context c) { return p(c).getInt("holds", 0); }

    // ================================================================ the RETURN RESET's hold (Y2b)

    /** Set when THIS process began a reset: the start-of-process look must not end it (review BF1). */
    private static volatile boolean resetStartedHere = false;

    /** Turns the animations off for the reset. Returns true if they are now off (so the caller can shorten its waits). */
    public static synchronized boolean begin(final Context c) {
        try {
            if (!enabled(c) || !permitted(c)) return false;
            resetStartedHere = true;
            int after = setHold(c, AnimationPolicy.HOLD_RESET, true);
            if (!AnimationPolicy.has(after, AnimationPolicy.HOLD_RESET)) return false;
            MAIN.postDelayed(new Runnable() { @Override public void run() { releaseReset(c, "failsafe"); } }, 10_000);   // failsafe
            Log.i(TAG, "animations off for the reset");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "could not turn animations off", t);
            try { releaseReset(c, "error"); } catch (Throwable ignored) { }
            return false;
        }
    }

    public static void restoreLater(final Context c, long delayMs) {
        MAIN.postDelayed(new Runnable() { @Override public void run() { releaseReset(c, "reset finished"); } }, delayMs);
    }

    /** BF1: the reset no longer needs the animations off (the custom home is in front, or it finished): its reason ends; the scales return unless the Quickstep session still holds them. Safe to repeat. */
    public static void releaseReset(Context c, String why) {
        try { setHold(c, AnimationPolicy.HOLD_RESET, false); } catch (Throwable t) { Log.e(TAG, "release failed", t); }
    }

    /** "Restore everything" and every last-resort path: ALL reasons end, the scales go back (if they are still ours). Safe to call any time, from any process of ours. */
    public static synchronized void restoreIfPending(Context c) {
        setHold(c, AnimationPolicy.HOLD_RESET | AnimationPolicy.HOLD_QUICKSTEP, false);
    }

    // ================================================================ the QUICKSTEP SESSION's hold (BF1)

    /** The lock screen was (or may be) up since the last unlock decision, so the stored mode can still be the OLD session's. Set at screen off, cleared when the unlock choice is applied (or no lock came). */
    private static volatile boolean lockCycle = false;
    /** Uptime at which the keyguard was first seen GONE during a lock cycle: the safety valve for an unlock handler that never answers. */
    private static volatile long unlockSeenAt = 0;

    /** The screen went off: the lock screen is next. The animations come back NOW, before it can show (the screen is dark: nothing timing-critical waits for this). */
    public static void onScreenOff(Context c) { lockCycle = true; unlockSeenAt = 0; syncNow(c, "screen off", Boolean.FALSE, null, false); }

    /** The screen came on. {@code lockUp}: the lock screen is what it shows (stays real); otherwise the session simply continues. The caller's answer is the one used (no second look at the keyguard). */
    public static void onScreenOn(Context c, boolean lockUp) {
        if (!lockUp) { lockCycle = false; unlockSeenAt = 0; }
        syncNow(c, lockUp ? "screen on (lock screen)" : "screen on", Boolean.TRUE, Boolean.valueOf(lockUp), false);
    }

    /** The unlock choice was applied (plain PIN = custom, gesture = Quickstep): from here the stored mode is the truth. */
    public static void onUnlockDecided(Context c) {
        lockCycle = false; unlockSeenAt = 0;
        syncNow(c, "unlock decided", null, null, false);
        // Android reports the keyguard gone a moment after the unlock handler can run: two cheap follow-up looks (the window heartbeat is the third) so a gesture unlock never waits for the next window change
        final Context app = c.getApplicationContext();
        Worker.H.postDelayed(new Runnable() { @Override public void run() { try { syncNow(app, "after the unlock", null, null, false); } catch (Throwable t) { Log.e(TAG, "sync failed", t); } } }, 400);
        Worker.H.postDelayed(new Runnable() { @Override public void run() { try { syncNow(app, "after the unlock", null, null, false); } catch (Throwable t) { Log.e(TAG, "sync failed", t); } } }, 1500);
    }

    /** Like {@link #syncNow} but on a background thread (coalesced: the run that follows reads the newest facts). For triggers on the app's start path. */
    public static void syncSoon(final Context c, final String trigger) {
        final Context app = c.getApplicationContext();
        if (!SOON_PENDING.compareAndSet(false, true)) return;
        Worker.H.post(new Runnable() { @Override public void run() { SOON_PENDING.set(false); try { syncNow(app, trigger, null, null, false); } catch (Throwable t) { Log.e(TAG, "sync failed", t); } } });
    }
    private static final java.util.concurrent.atomic.AtomicBoolean SOON_PENDING = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final class Worker { static final Handler H; static { HandlerThread t = new HandlerThread("tj-anim"); t.start(); H = new Handler(t.getLooper()); } }

    /**
     * The heartbeat (called for every accessibility event; at most twice a second it looks at anything): heals a missed trigger, e.g. a lock screen that appeared after the screen came on. Costs a volatile read and a
     * time compare in all but two events a second, and after that only preference reads: nothing starts (not even the worker thread) unless the hold is on or could start (Quickstep mode AND the switch on).
     */
    public static void tick(Context c) {
        long now = SystemClock.uptimeMillis();
        if (now - tickAt < 500) return;
        tickAt = now;
        Context app = c.getApplicationContext();
        if (!AnimationPolicy.has(holds(app), AnimationPolicy.HOLD_QUICKSTEP) && !(Mode.QUICKSTEP.equals(Mode.get(app)) && (quickstepSwitch(app) || NotifQuiet.switchOn(app) || RadioIconHide.switchOn(app)))) return;   // BH1: the banner quiet is level-triggered in Quickstep too (a lock screen that came up without its broadcast); still nothing at all in the custom launcher
        syncSoon(app, "window change");
    }
    private static volatile long tickAt = 0;

    /**
     * Main process start. (1) AT ONCE, on the worker: a reset never outlives its process, so a reset hold left by the process that died ends (unless THIS process has begun one); the Quickstep hold is decided from the facts
     * with the helper ASSUMED present, so everything that needs no helper (lock screen, screen off, custom launcher, switch off, paused) is acted on without delay: a crash loop that dies in a second still ends up with
     * real animations on the lock screen. (2) After 2.5 s, with the helper as it really is: a helper that never came back cannot hear the screen go off, so the hold ends.
     */
    public static void reconcileAsync(final Context c) {
        final Context app = c.getApplicationContext();
        // a short-lived thread creates the worker, so Application.onCreate (whose cost AD8 measures on the slow tablet) never waits for a new thread's looper
        new Thread(new Runnable() { @Override public void run() {
            Worker.H.post(new Runnable() { @Override public void run() {
                try { if (!resetStartedHere) setHold(app, AnimationPolicy.HOLD_RESET, false); syncNow(app, "process start", null, null, true); } catch (Throwable t) { Log.e(TAG, "reconcile failed", t); }
            } });
            Worker.H.postDelayed(new Runnable() { @Override public void run() {
                try { syncNow(app, "process start + 2.5 s", null, null, false); } catch (Throwable t) { Log.e(TAG, "reconcile failed", t); }
            } }, 2500);
        } }, "tj-anim-start").start();
    }

    public static void syncNow(Context c, String trigger) { syncNow(c, trigger, null, null, false); }

    /**
     * THE decision. Reads the facts, asks {@link AnimationPolicy#why}, sets or clears the Quickstep reason, and logs a line (lock log + logcat) only when something changed. {@code screenOnNow} / {@code lockUpNow}: what
     * the caller knows better than a service query (the broadcast that says so, the answer it already used), or null to ask. {@code assumeHelper}: the process has just started and its helper is about to connect.
     * Cheap when there is nothing to do: no binder call unless the hold is on or could start.
     */
    static synchronized void syncNow(Context c, String trigger, Boolean screenOnNow, Boolean lockUpNow, boolean assumeHelper) {
        final Context app = c.getApplicationContext();
        final Look look = new Look(app, screenOnNow, lockUpNow);   // asked at most once, whichever rule needs it first
        // BH1: the pop-up banners follow the same triggers and the same "which session is this" facts (their rule is the complement of the animation rule); first, so nothing below can keep them from being decided
        try { NotifQuiet.sync(app, trigger, look, assumeHelper); } catch (Throwable t) { Log.e(TAG, "banner quiet sync failed", t); }
        // BS1: the Wi-Fi symbol hold is decided in exactly the same world (custom launcher, dark or locked screen, undecided PIN), from the same triggers and the same look
        try { RadioIconHide.sync(app, trigger, look, assumeHelper); } catch (Throwable t) { Log.e(TAG, "Wi-Fi, Bluetooth and VPN symbol sync failed", t); }
        try {
            final int before = holds(app);
            final boolean holding = AnimationPolicy.has(before, AnimationPolicy.HOLD_QUICKSTEP);
            AnimationPolicy.Facts f = new AnimationPolicy.Facts();
            f.switchOn = quickstepSwitch(app);
            f.grant = permitted(app);
            f.helperUp = assumeHelper || TjAccessibilityService.get() != null;
            f.quickstepMode = Mode.QUICKSTEP.equals(Mode.get(app));
            f.automationOff = CrashGuard.automationOff(app);
            // stage 1, no binder call: with the screen and the lock assumed fine, is anything else against it? (this runs at every screen off and every mode change)
            f.screenOn = true; f.lockUp = false; f.unlockUndecided = false;
            String why = AnimationPolicy.why(f);
            if (!why.isEmpty() && !holding) return;   // nothing held and nothing could start
            if (why.isEmpty()) {   // stage 2: the screen and the lock screen
                f.screenOn = look.screenOn();
                f.lockUp = look.lockUp();
                f.unlockUndecided = unlockUndecided(f.screenOn, f.lockUp);
                why = AnimationPolicy.why(f);
            }
            final boolean want = why.isEmpty();
            final int after = setHold(app, AnimationPolicy.HOLD_QUICKSTEP, want);
            if (AnimationPolicy.has(before, AnimationPolicy.HOLD_QUICKSTEP) != AnimationPolicy.has(after, AnimationPolicy.HOLD_QUICKSTEP)) {
                String msg = want ? "OFF in the Quickstep session (" + trigger + ")" : "ON again: " + why + " (" + trigger + ")";
                Log.i(TAG, "animations " + msg);
                LockLog.addAsync(app, "ANIM", msg);
                p(app).edit().putString("last", new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  " + msg).apply();
            }
        } catch (Throwable t) { Log.e(TAG, "sync failed", t); }
    }

    /**
     * The "lock cycle" fact shared by every level-triggered rule that reads the stored mode (the animations here, the pop-up banners in {@link NotifQuiet}): true while the lock screen was up since the last unlock
     * decision, the screen is on, the lock screen is gone and the unlock handler has not answered yet (the stored mode can still be the OLD session's). Also runs the safety valve: after
     * {@link AnimationPolicy#UNDECIDED_MAX_MS} without an answer the cycle is over. Only ever called from {@link #syncNow} (serialised), so the volatile clock needs no more than that.
     */
    static boolean unlockUndecided(boolean screenOn, boolean lockUp) {
        if (!lockCycle) return false;
        if (!screenOn || lockUp) { unlockSeenAt = 0; return false; }   // still dark or locked: the unlock is still to come
        long now = SystemClock.uptimeMillis();
        if (unlockSeenAt == 0) unlockSeenAt = now;
        if (now - unlockSeenAt > AnimationPolicy.UNDECIDED_MAX_MS) { lockCycle = false; unlockSeenAt = 0; return false; }   // the handler never answered: stop waiting for it
        return true;
    }

    /**
     * One {@link #syncNow}'s look at the screen and the lock screen: each binder call at most once, shared by every rule that reads them (the animations here, the banners in {@link NotifQuiet}); the
     * caller's own answer, when it has one, is used instead of asking (it is the broadcast that says so). Same results as asking directly: the lock is "up" only on a lit screen.
     */
    static final class Look {
        private final Context app;
        private final Boolean screenOnGiven, lockUpGiven;
        private Boolean screenOn, lockUp;
        Look(Context app, Boolean screenOnGiven, Boolean lockUpGiven) { this.app = app; this.screenOnGiven = screenOnGiven; this.lockUpGiven = lockUpGiven; }
        boolean screenOn() { if (screenOn == null) screenOn = screenOnGiven != null ? screenOnGiven : Boolean.valueOf(interactive(app)); return screenOn.booleanValue(); }
        boolean lockUp() { if (lockUp == null) lockUp = Boolean.valueOf(screenOn() && (lockUpGiven != null ? lockUpGiven.booleanValue() : keyguardUp(app))); return lockUp.booleanValue(); }
    }

    static boolean interactive(Context app) {
        try { PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE); return pm != null && pm.isInteractive(); } catch (Throwable t) { return false; }
    }

    /** Unknown counts as "lock screen up": the safe side (the animations stay real). */
    static boolean keyguardUp(Context app) {
        try { KeyguardManager km = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE); return km == null || km.isKeyguardLocked(); } catch (Throwable t) { return true; }
    }

    // ================================================================ what Launcher control and the tablet report show

    /** One line: the switch, the grant, whether the animations are held right now (and why), the three scales, what will be put back, and the last change. Never throws. */
    public static String describe(Context c) {
        try {
            Context app = c.getApplicationContext();
            SharedPreferences sp = p(app);
            int h = sp.getInt("holds", 0);
            android.content.ContentResolver cr = app.getContentResolver();
            StringBuilder b = new StringBuilder();
            b.append("Quickstep switch ").append(quickstepSwitch(app) ? "on" : "off").append(", adb permission ").append(permitted(app) ? "granted" : "MISSING").append(" | now: ");
            if (h == 0) b.append("animations ON (normal)");
            else b.append("animations OFF, held for ").append(AnimationPolicy.has(h, AnimationPolicy.HOLD_QUICKSTEP) ? "the Quickstep session" : "").append(AnimationPolicy.has(h, AnimationPolicy.HOLD_QUICKSTEP) && AnimationPolicy.has(h, AnimationPolicy.HOLD_RESET) ? " and " : "").append(AnimationPolicy.has(h, AnimationPolicy.HOLD_RESET) ? "the return reset" : "");
            b.append(" | scales (window / transition / animator): ");
            for (int i = 0; i < NAMES.length; i++) b.append(i == 0 ? "" : " / ").append(Settings.Global.getString(cr, NAMES[i]));
            if (sp.getBoolean("pending", false)) {
                b.append(" | put back to: ");
                for (int i = 0; i < NAMES.length; i++) b.append(i == 0 ? "" : " / ").append(sp.getString(NAMES[i], "1.0"));
            }
            b.append(" | last change: ").append(sp.getString("last", "(none yet)"));
            return b.toString();
        } catch (Throwable t) { return "(unreadable)"; }
    }
}
