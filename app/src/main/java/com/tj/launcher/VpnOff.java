package com.tj.launcher;

import android.app.AppOpsManager;
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
import android.util.Log;

/**
 * CB2: the Android side of {@link VpnOffRules} (Tj, 2026-11-13: "make the launcher automatically turn off my VPN any time the screen turns off or the lock
 * screen is on"; CB2e "And any time I lock the screen from quickstep": no mode condition). Mechanism and its proof in AOSP 10:
 * research/VPN_FOOTER_2026-11-13.md. All decisions and all state live on one small worker thread; the facts "screen dark / lock screen up" are read on the
 * MAIN thread through {@link Look} (the service's keyguard read is not thread-safe), every time, so nothing here can act on a stale "locked" while Tj is
 * using the tablet. The screen-off path of the accessibility service only posts (no binder call, no disk write on the main thread). Never logs or stores the
 * VPN app's name (the tablet report goes into a public repository).
 */
public final class VpnOff {
    private static final String TAG = "TjVpnOff";
    private VpnOff() { }

    /** How long after the call the VPN network is looked for again (the teardown is asynchronous). */
    private static final long LOOK_AGAIN_MS = 1500;

    // ---- worker-thread state (written only on the worker)
    private static final long[] stopTimes = new long[VpnOffRules.MAX_STOPS];   // elapsedRealtime of the last stops, 0 = empty
    private static long lastPrepareAt = 0;          // elapsedRealtime of the last prepare call
    private static boolean refusedThisLock = false; // Always-on / lockdown: not asked again until the next unlock (review LOW6)
    private static String loggedThisLock = null;    // the reason / refusal already written into the lock log since the last unlock (one line, not one per trigger)
    private static volatile boolean lockedWorld = false;   // a screen-off or a lock screen was seen and no unlock since: the 30 s look runs (review MED2)

    private static Handler worker;
    private static ConnectivityManager.NetworkCallback callback;
    private static volatile Handler main;
    private static volatile Look look;
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

    /**
     * A trigger (any thread): the screen went off, the lock screen came up, a VPN connected while locked. {@code dark} / {@code lockUp} are the caller's FRESH
     * facts (read on the main thread). Cheap when the switch is off (one preference read, nothing posted).
     */
    public static void check(Context c, final boolean dark, final boolean lockUp, final String trigger) {
        final Context app = c.getApplicationContext();
        if (!switchOn(app)) return;
        worker().post(() -> run(app, dark, lockUp, trigger));
    }

    /** The screen went off / the lock screen is up: from now on a VPN is also looked for every {@link VpnOffRules#POLL_MS} until the next unlock. */
    public static void onLockedWorld(Context c) {
        final Context app = c.getApplicationContext();
        if (!switchOn(app)) return;
        lockedWorld = true;
        worker().post(() -> { if (allowed(app)) schedulePoll(); });   // nothing could be done without Android's OK: no look at all then
    }

    /** The tablet is unlocked (USER_PRESENT, or a wake inside the "lock after" delay with no lock screen: review MED1): the next lock starts from zero. */
    public static void onUnlocked() {
        lockedWorld = false;
        worker().post(() -> {
            java.util.Arrays.fill(stopTimes, 0L); refusedThisLock = false; loggedThisLock = null;
            worker().removeCallbacks(poll);
        });
    }

    private static final Runnable poll = new Runnable() { @Override public void run() {
        Context app = appCtx;
        if (!lockedWorld || app == null || !switchOn(app)) return;
        recheck(app, "a look while locked", 0);
        schedulePoll();
    } };
    private static void schedulePoll() { Handler w = worker(); w.removeCallbacks(poll); w.postDelayed(poll, VpnOffRules.POLL_MS); }

    /** Read the facts on the MAIN thread (after {@code delayMs}) and check if the screen is dark or the lock screen up; an unlocked answer ends the locked world. */
    private static void recheck(final Context app, final String trigger, long delayMs) {
        Handler m = main; final Look l = look;
        if (m == null || l == null) return;
        m.postDelayed(() -> {
            try { boolean d = l.dark(), k = l.lockUp(); if (d || k) check(app, d, k, trigger); else if (lockedWorld) onUnlocked(); }
            catch (Throwable t) { Log.w(TAG, "VPN look failed: " + t); }
        }, delayMs);
    }

    private static void run(final Context app, boolean dark, boolean lockUp, final String trigger) {
        try {
            boolean on = switchOn(app);
            boolean up = on && vpnUp(app);
            final boolean ok = up && allowed(app);   // the app-op is read only when there is something to turn off
            long now = SystemClock.elapsedRealtime();
            long since = lastPrepareAt == 0 ? Long.MAX_VALUE / 4 : now - lastPrepareAt;
            String why = VpnOffRules.why(on, ok, dark, lockUp, up, VpnOffRules.recentStops(stopTimes, now), refusedThisLock, since);
            if (why != VpnOffRules.STOP) {
                if (VpnOffRules.SETTLING.equals(why)) { recheck(app, trigger, VpnOffRules.SETTLE_MS - since + 100); return; }   // level-triggered: look again (fresh facts) once the teardown had its time
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

    /** The service's facts, read on the MAIN thread. */
    public interface Look { boolean dark(); boolean lockUp(); }

    /** Hear a VPN connect while the tablet is dark or locked (a VPN app that reconnects by itself), and keep the facts for the 30 s look. Idempotent. */
    public static synchronized void watch(Context c, final Handler mainHandler, final Look l) {
        final Context app = c.getApplicationContext();
        appCtx = app; main = mainHandler; look = l;
        if (callback != null) return;
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            // API 29 narrows a listen to VPNs that route the CALLER's traffic: a VPN that leaves the launcher out is never heard here; the 30 s look covers it
            NetworkRequest req = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_VPN).removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build();
            ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network n) { if (switchOn(app)) recheck(app, "a VPN connected while locked", 0); }
            };
            cm.registerNetworkCallback(req, cb, worker());
            callback = cb;
        } catch (Throwable t) { Log.w(TAG, "VPN watch failed: " + t); }
    }

    public static synchronized void unwatch(Context c) {
        main = null; look = null;   // the 30 s look of a dead service instance does nothing (the next instance sets its own)
        ConnectivityManager.NetworkCallback cb = callback;
        callback = null;
        if (cb == null) return;
        try { ((ConnectivityManager) c.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(cb); } catch (Throwable ignored) { }
    }

    /** One line for Launcher control and the tablet report (no VPN name). */
    public static String describe(Context c) {
        boolean on = switchOn(c), ok = allowed(c), up = vpnUp(c);
        return (on ? "ON" : "OFF") + "; allowed by Android: " + (ok ? "yes" : "NO (Launcher control: Allow)")
                + "; accessibility helper running: " + (TjAccessibilityService.get() != null ? "yes" : "NO (nothing can turn the VPN off without it)")
                + "; a VPN is connected now: " + (up ? "yes" : "no") + "; stops in the last 10 min: " + VpnOffRules.recentStops(stopTimes, SystemClock.elapsedRealtime())
                + "; last: " + Features.getString(c, VpnOffRules.LAST_KEY, "(has not had to act yet)");
    }
}
