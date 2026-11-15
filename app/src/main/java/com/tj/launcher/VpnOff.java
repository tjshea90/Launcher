package com.tj.launcher;

import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/**
 * CB2: the Android side of {@link VpnOffRules} (Tj, 2026-11-13: "make the launcher automatically turn off my VPN any time the screen turns off or the lock
 * screen is on"; CB2e "And any time I lock the screen from quickstep"). Mechanism and its proof in AOSP 10: research/VPN_FOOTER_2026-11-13.md.
 *
 * CE1 (Tj, 2026-11-15: "The VPN bar in the notification panel should never show up at any time on any lock screen or in custom launcher ... Never automatically
 * turn off fast wifi mode if I turned it on and I'm inside quickstep launcher"): the rule follows the SAME world as the pop-up banners and the radio symbols
 * ({@link NotifQuietPolicy}), decided by the SAME level-triggered pipeline ({@link AnimationSpeedup#syncNow}: mode changes, screen off / on, the unlock decision,
 * the 2 Hz window heartbeat in Quickstep, process start, helper connect): quiet = the custom launcher, a dark screen, the lock screen (any launcher), a plain
 * PIN not decided yet. In a quiet world a running VPN is turned off; in an unlocked, decided Quickstep session nothing here ever touches a VPN. The world is also
 * PUBLISHED ({@link VpnOffRules#WORLD_KEY}) so LinkMeter's Fast Wi-Fi can pause itself and come back by itself in Quickstep instead of fighting the launcher.
 *
 * All decisions and all state live on one small worker thread; the facts are read by {@link #sync} inside the pipeline (so a stale "locked" can never act while
 * Tj is using the tablet, and the "unlock not decided yet" fact is the same one the other holds use). Nothing here waits on the main thread, writes a file or
 * calls a binder on it: {@link #sync} only posts. Never logs or stores the VPN app's name (the tablet report goes into a public repository).
 */
public final class VpnOff {
    private static final String TAG = "TjVpnOff";
    private VpnOff() { }

    /** How long after the call the VPN network is looked for again (the teardown is asynchronous). */
    private static final long LOOK_AGAIN_MS = 1500;
    /** The pipeline's heartbeat trigger (AnimationSpeedup.tick): it re-reads the world but must not start a VPN look twice a second. */
    private static final String HEARTBEAT = "window change";

    // ---- worker-thread state (written only on the worker)
    private static final long[] stopTimes = new long[VpnOffRules.MAX_STOPS];   // elapsedRealtime of the last stops, 0 = empty
    private static long lastPrepareAt = 0;          // elapsedRealtime of the last prepare call
    private static boolean refusedThisLock = false; // Always-on / lockdown: not asked again until the next unlock (review LOW6)
    private static String loggedThisLock = null;    // the reason / refusal already written into the lock log since the last unlock (one line, not one per trigger)

    // ---- written by the pipeline (serialised), read anywhere
    private static volatile boolean quietWorld = false;   // the last sync said "quiet" and nothing since said otherwise: the 30 s look runs (review MED2)
    private static volatile String published = null;      // the last world word this process wrote (null: none, or the write failed)
    private static volatile boolean retractChecked = false;   // the switch is off and the word was looked for (and removed) once in this process

    private static Handler worker;
    private static ConnectivityManager.NetworkCallback callback;
    private static volatile Context appCtx;

    private static synchronized Handler worker() {
        if (worker == null) { HandlerThread t = new HandlerThread("tj-vpn-off"); t.start(); worker = new Handler(t.getLooper()); }
        return worker;
    }

    public static boolean switchOn(Context c) { return Features.get(c, VpnOffRules.KEY, VpnOffRules.defaultOn(ReturnReset.isDebuggable(c))); }

    /** Android allowed the launcher to use the VPN service (app-op ACTIVATE_VPN): a read, it changes nothing. */
    public static boolean allowed(Context c) {
        try {
            AppOpsManager ao = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            return ao != null && ao.unsafeCheckOpNoThrow("android:activate_vpn", android.os.Process.myUid(), c.getPackageName()) == AppOpsManager.MODE_ALLOWED;
        } catch (Throwable t) { return false; }
    }

    /** A VPN network is connected (any app's, also one that does not route the launcher: getAllNetworks lists every network). */
    public static boolean vpnUp(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            for (Network n : cm.getAllNetworks()) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                if (nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true;
            }
        } catch (Throwable t) { Log.w(TAG, "network look failed: " + t); }
        return false;
    }

    // ================================================================ the world (called by the pipeline)

    /**
     * The world facts of a VPN-free decision: {@link NotifQuietPolicy#why} without the banner switch and without the adb grant (turning a VPN off needs neither;
     * only PUBLISHING the word needs the grant). Same fact gathering as {@link NotifQuiet#fillFacts}: the helper, the screen and the lock only matter inside a
     * Quickstep mode (the custom launcher is quiet whatever they say), so no binder call there.
     */
    static void fillWorld(Context app, NotifQuietPolicy.Facts f, AnimationSpeedup.Look look, boolean assumeHelper) {
        f.switchOn = true; f.grant = true;
        f.automationOff = CrashGuard.automationOff(app);
        f.quickstepMode = Mode.QUICKSTEP.equals(Mode.get(app));
        f.helperUp = true; f.screenOn = true; f.lockUp = false; f.unlockUndecided = false;
        if (f.quickstepMode && !f.automationOff) {
            f.helperUp = assumeHelper || TjAccessibilityService.get() != null;
            if (f.helperUp) {
                f.screenOn = look.screenOn();
                f.lockUp = look.lockUp();
                f.unlockUndecided = AnimationSpeedup.unlockUndecided(f.screenOn, f.lockUp);
            }
        }
    }

    /**
     * THE decision, one more consumer of {@link AnimationSpeedup#syncNow} (serialised; called on whatever thread the trigger came from, the main thread at a mode
     * change: so this only reads facts and POSTS). Quiet: publish the word, turn a running VPN off, keep looking every {@link VpnOffRules#POLL_MS}. Not quiet:
     * publish "quickstep", end the quiet world (the next lock counts its stops from zero). Switch off: the word is removed. Never throws.
     */
    static void sync(Context c, String trigger, AnimationSpeedup.Look look, boolean assumeHelper) {
        try {
            final Context app = c.getApplicationContext();
            appCtx = app;
            if (!switchOn(app)) {
                endQuiet();
                if (!retractChecked) { retractChecked = true; published = null; worker().post(() -> removeWord(app)); }   // once per process: a word left by an earlier run
                return;
            }
            retractChecked = false;
            watch(app);
            NotifQuietPolicy.Facts f = new NotifQuietPolicy.Facts();
            fillWorld(app, f, look, assumeHelper);
            final boolean quiet = NotifQuietPolicy.wanted(f);
            publish(app, VpnOffRules.worldValue(quiet, quiet ? NotifQuietPolicy.quietFor(f) : ""));
            if (!quiet) { endQuiet(); return; }
            final boolean began = !quietWorld;
            quietWorld = true;
            if (began) worker().post(() -> { if (allowed(app)) schedulePoll(); });   // nothing could be done without Android's OK: no look at all then
            if (began || !HEARTBEAT.equals(trigger)) check(app, true, trigger);      // the heartbeat only re-reads the world; a look at the networks costs binder calls
        } catch (Throwable t) { Log.e(TAG, "VPN world sync failed", t); }
    }

    /** The quiet world is over (an unlocked Quickstep session, or the switch went off): the poll stops and the next quiet world counts its stops from zero. */
    private static void endQuiet() {
        if (!quietWorld) return;
        quietWorld = false;
        worker().post(VpnOff::resetCounters);
    }

    private static void resetCounters() {
        java.util.Arrays.fill(stopTimes, 0L); refusedThisLock = false; loggedThisLock = null;
        worker().removeCallbacks(poll);
    }

    /** The tablet is unlocked (USER_PRESENT): the next lock counts its VPN stops from zero (nothing turns a VPN back on). The world itself is decided by {@link #sync}. */
    public static void onUnlocked() {
        quietWorld = false;
        worker().post(VpnOff::resetCounters);
    }

    // ================================================================ the published word

    /** Writes the word when it changed (on the worker: never a binder call on the caller's thread). Needs the adb grant; without it the word is not published (Launcher control says so). */
    private static void publish(final Context app, final String value) {
        if (value.equals(published)) return;
        if (!AnimationSpeedup.secureSettingsGranted(app)) { published = null; return; }
        published = value;
        worker().post(() -> {
            try {
                if (!Settings.Global.putString(app.getContentResolver(), VpnOffRules.WORLD_KEY, value)) throw new IllegalStateException("the settings provider refused the write");
                LockLog.add(app, "VPN", "world word: " + value);
            } catch (Throwable t) {
                Log.w(TAG, "publishing the world failed: " + t);
                if (value.equals(published)) published = null;   // the next sync writes it again
            }
        });
    }

    private static void removeWord(Context app) {
        try {
            ContentResolver cr = app.getContentResolver();
            if (Settings.Global.getString(cr, VpnOffRules.WORLD_KEY) != null) { Settings.Global.putString(cr, VpnOffRules.WORLD_KEY, null); LockLog.add(app, "VPN", "world word removed (the switch is off)"); }
        } catch (Throwable t) { Log.w(TAG, "removing the world word failed: " + t); }
    }

    /** "Restore everything" and the like: the launcher says nothing any more (a reader then acts alone). */
    public static void retract(Context c) {
        final Context app = c.getApplicationContext();
        published = null; retractChecked = true;
        worker().post(() -> removeWord(app));
    }

    // ================================================================ turning a VPN off

    /** A trigger (any thread): the world is quiet. Cheap when the switch is off (one preference read, nothing posted). */
    static void check(Context c, final boolean quiet, final String trigger) {
        final Context app = c.getApplicationContext();
        if (!switchOn(app)) return;
        worker().post(() -> run(app, quiet, trigger));
    }

    private static final Runnable poll = new Runnable() { @Override public void run() {
        Context app = appCtx;
        if (!quietWorld || app == null || !switchOn(app)) return;
        AnimationSpeedup.syncSoon(app, "a look while quiet");   // level-triggered: the facts are read again by the pipeline, a VPN that appeared is turned off
        schedulePoll();
    } };
    private static void schedulePoll() { Handler w = worker(); w.removeCallbacks(poll); w.postDelayed(poll, VpnOffRules.POLL_MS); }

    private static void run(final Context app, boolean quiet, final String trigger) {
        try {
            boolean on = switchOn(app);
            boolean up = on && vpnUp(app);
            final boolean ok = up && allowed(app);   // the app-op is read only when there is something to turn off
            long now = SystemClock.elapsedRealtime();
            long since = lastPrepareAt == 0 ? Long.MAX_VALUE / 4 : now - lastPrepareAt;
            String why = VpnOffRules.why(on, ok, quiet, up, VpnOffRules.recentStops(stopTimes, now), refusedThisLock, since);
            if (why != VpnOffRules.STOP) {
                if (VpnOffRules.SETTLING.equals(why)) {   // level-triggered: look again (fresh facts) once the teardown had its time
                    worker().postDelayed(() -> AnimationSpeedup.syncSoon(app, trigger), VpnOffRules.SETTLE_MS - since + 100);
                    return;
                }
                if (VpnOffRules.worthALine(why) && !why.equals(loggedThisLock)) { loggedThisLock = why; note(app, why + " (" + trigger + ")"); }
                return;
            }
            int result;
            try { Intent need = VpnService.prepare(app); result = need == null ? VpnOffRules.PREPARED : VpnOffRules.REFUSED; }
            catch (IllegalStateException e) { result = VpnOffRules.LOCKDOWN; }
            catch (Throwable t) { result = VpnOffRules.FAILED; Log.w(TAG, "prepare failed", t); }
            lastPrepareAt = now;
            Log.i(TAG, "VPN off: prepare result=" + result + " trigger=" + trigger);
            if (result == VpnOffRules.PREPARED) {
                VpnOffRules.recordStop(stopTimes, now);
                note(app, VpnOffRules.outcome(result, ok, false) + " (" + trigger + ")");
                final long t0 = now;
                worker().postDelayed(() -> {   // review LOW11: a timer of a sleeping CPU can run minutes later: only a prompt look is believed
                    if (VpnOffRules.lateLookMeaningful(SystemClock.elapsedRealtime() - t0) && vpnUp(app)) note(app, VpnOffRules.outcome(VpnOffRules.PREPARED, ok, true) + " (" + trigger + ")");
                }, LOOK_AGAIN_MS);
                return;
            }
            if (VpnOffRules.refusesForThisLock(result)) refusedThisLock = true;
            String line = VpnOffRules.outcome(result, ok, true);
            if (!line.equals(loggedThisLock)) { loggedThisLock = line; note(app, line + " (" + trigger + ")"); }
        } catch (Throwable t) { Log.e(TAG, "VPN off failed", t); }
    }

    private static void note(Context app, String line) {
        Log.i(TAG, "VPN " + line);
        LockLog.add(app, "VPN", line);
        Features.setStringAsyncIfChanged(app, VpnOffRules.LAST_KEY, TimeFmt.hms(System.currentTimeMillis()) + " " + line);
    }

    // ================================================================ hearing a VPN connect

    /**
     * Hear a VPN connect (a VPN app that reconnects by itself): the pipeline looks again at once. Idempotent; registered by the first {@link #sync}, so it needs
     * no accessibility helper (the custom launcher needs none: without it nothing can change the mode). A VPN that connects in an unlocked Quickstep session is
     * looked at too (one cheap pipeline run) and left alone.
     */
    static synchronized void watch(Context c) {
        final Context app = c.getApplicationContext();
        appCtx = app;
        if (callback != null) return;
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            // API 29 narrows a listen to VPNs that route the CALLER's traffic: a VPN that leaves the launcher out is never heard here; the 30 s look covers it
            NetworkRequest req = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_VPN).removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build();
            ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network n) { if (switchOn(app)) AnimationSpeedup.syncSoon(app, "a VPN connected"); }
            };
            cm.registerNetworkCallback(req, cb, worker());
            callback = cb;
        } catch (Throwable t) { Log.w(TAG, "VPN watch failed: " + t); }
    }

    /** The helper's service went away: nothing here depends on it (the pipeline and the callback live in the process), the callback is kept. Kept for the service's calls. */
    public static synchronized void unwatch(Context c) { }

    /** One line for Launcher control and the tablet report (no VPN name). */
    public static String describe(Context c) {
        boolean on = switchOn(c), ok = allowed(c), up = vpnUp(c);
        String word;
        try { word = Settings.Global.getString(c.getContentResolver(), VpnOffRules.WORLD_KEY); } catch (Throwable t) { word = "(unreadable)"; }
        return (on ? "ON" : "OFF") + "; allowed by Android: " + (ok ? "yes" : "NO (Launcher control: Allow)")
                + "; accessibility helper running: " + (TjAccessibilityService.get() != null ? "yes" : "NO (nothing can turn the VPN off in Quickstep without it)")
                + "; world word for LinkMeter: " + (word != null ? word : on ? (AnimationSpeedup.secureSettingsGranted(c) ? "(not written yet)" : "NOT PUBLISHED (no adb permission)") : "(none: the switch is off)")
                + "; a VPN is connected now: " + (up ? "yes" : "no") + "; stops in the last 10 min: " + VpnOffRules.recentStops(stopTimes, SystemClock.elapsedRealtime())
                + "; last: " + Features.getString(c, VpnOffRules.LAST_KEY, "(has not had to act yet)");
    }
}
