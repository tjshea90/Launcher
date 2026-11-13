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
import android.util.Log;

/**
 * CB2: the Android side of {@link VpnOffRules} (Tj, 2026-11-13: "make the launcher automatically turn off my VPN any time the screen turns off or the lock
 * screen is on"). Mechanism and its proof in AOSP 10: research/VPN_FOOTER_2026-11-13.md. Everything runs on one small worker thread: the screen-off path of
 * the accessibility service only posts (no binder call, no disk write on the main thread). Never logs or stores the VPN app's name (the tablet report goes
 * into a public repository).
 */
public final class VpnOff {
    private static final String TAG = "TjVpnOff";
    private VpnOff() { }

    /** How long after the call the VPN network is looked for again (the teardown is asynchronous). */
    private static final long LOOK_AGAIN_MS = 1500;

    private static volatile int stopsThisLock = 0;
    private static volatile String loggedThisLock = null;   // the refusal already written into the lock log during this lock (one line per lock, not one per trigger)
    private static Handler worker;
    private static ConnectivityManager.NetworkCallback callback;

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

    /** A VPN network is connected (any app's; getAllNetworks lists every network). */
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
     * A trigger (main thread or any thread): the screen went off, the lock screen came up, a VPN connected while locked. {@code dark} / {@code lockUp} are the
     * caller's facts at the time of the trigger. Cheap when the switch is off (one preference read, nothing posted).
     */
    public static void check(Context c, final boolean dark, final boolean lockUp, final String trigger) {
        final Context app = c.getApplicationContext();
        if (!switchOn(app)) return;
        worker().post(() -> run(app, dark, lockUp, trigger));
    }

    /** The tablet was unlocked: a new lock starts counting from zero. */
    public static void onUnlocked() { stopsThisLock = 0; loggedThisLock = null; }

    private static void run(final Context app, boolean dark, boolean lockUp, final String trigger) {
        try {
            boolean on = switchOn(app);
            boolean up = on && vpnUp(app);
            final boolean ok = up && allowed(app);   // the app-op is read only when there is something to turn off
            String why = VpnOffRules.why(on, ok, dark, lockUp, up, stopsThisLock);
            if (why != VpnOffRules.STOP) {
                if ((VpnOffRules.NOT_ALLOWED.equals(why) || VpnOffRules.GAVE_UP.equals(why)) && !why.equals(loggedThisLock)) {
                    loggedThisLock = why;
                    note(app, why + " (" + trigger + ")");
                }
                return;
            }
            int result;
            try { Intent need = VpnService.prepare(app); result = need == null ? VpnOffRules.PREPARED : VpnOffRules.REFUSED; }
            catch (IllegalStateException e) { result = VpnOffRules.LOCKDOWN; }
            catch (Throwable t) { result = VpnOffRules.FAILED; Log.w(TAG, "prepare failed", t); }
            stopsThisLock = VpnOffRules.afterAttempt(stopsThisLock, result);
            final int r = result;
            Log.i(TAG, "VPN off: prepare result=" + r + " trigger=" + trigger + " stopsThisLock=" + stopsThisLock);
            worker().postDelayed(() -> note(app, VpnOffRules.outcome(r, ok, vpnUp(app)) + " (" + trigger + ")"), LOOK_AGAIN_MS);
        } catch (Throwable t) { Log.e(TAG, "VPN off failed", t); }
    }

    private static void note(Context app, String line) {
        Log.i(TAG, "VPN " + line);
        LockLog.add(app, "VPN", line);
        Features.setStringAsyncIfChanged(app, VpnOffRules.LAST_KEY, TimeFmt.hms(System.currentTimeMillis()) + " " + line);
    }

    /** The service's facts, read on the MAIN thread when a VPN connects (the keyguard read of the service is not thread-safe). */
    public interface Look { boolean dark(); boolean lockUp(); }

    /** Hear a VPN connect while the tablet is dark or locked (a VPN app that reconnects by itself). Idempotent. */
    public static synchronized void watch(Context c, final Handler main, final Look look) {
        if (callback != null) return;
        final Context app = c.getApplicationContext();
        try {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            NetworkRequest req = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_VPN).removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build();
            ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network n) {
                    if (!switchOn(app)) return;
                    main.post(() -> { try { boolean d = look.dark(), l = look.lockUp(); if (d || l) check(app, d, l, "a VPN connected while locked"); } catch (Throwable t) { Log.w(TAG, "VPN look failed: " + t); } });
                }
            };
            cm.registerNetworkCallback(req, cb, worker());
            callback = cb;
        } catch (Throwable t) { Log.w(TAG, "VPN watch failed: " + t); }
    }

    public static synchronized void unwatch(Context c) {
        ConnectivityManager.NetworkCallback cb = callback;
        callback = null;
        if (cb == null) return;
        try { ((ConnectivityManager) c.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(cb); } catch (Throwable ignored) { }
    }

    /** One line for Launcher control and the tablet report (no VPN name). */
    public static String describe(Context c) {
        boolean on = switchOn(c), ok = allowed(c), up = vpnUp(c);
        return (on ? "ON" : "OFF") + "; allowed by Android: " + (ok ? "yes" : "NO (Launcher control: Allow)") + "; a VPN is connected now: " + (up ? "yes" : "no")
                + "; turned off this lock: " + stopsThisLock + "; last: " + Features.getString(c, VpnOffRules.LAST_KEY, "(has not had to act yet)");
    }
}
