package com.tj.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

/**
 * CH1 (Tj, 2026-11-18): "Pocket return", the glue. {@link PocketRules} decides, this class does. Called from {@link ProfileSession#returnToCustom} ONLY
 * when {@link PocketRules#active} said yes (switch ON, and the return came from the tile or the icon): with the switch OFF nothing in here ever runs.
 *
 * <p>Beside the plain return (which already switches to the custom launcher, clears notifications and Recents and, with the pocket flag, force-stops
 * every open app except LinkMeter and the keyboards, see {@code ReturnReset.run(c, pocket)}) two small threads run:
 * <ul>
 *   <li>{@code tj-pocket}: asks LinkMeter (same tablet; {@code content://com.tj.linkmeter.launcher}) to put the linked phone to sleep (hotspot and Wi-Fi off,
 *       airplane mode on, battery saver on, screen off), and switches the TABLET's Wi-Fi off when the phone has accepted its order (or after 25 s, or when LinkMeter
 *       does not answer). Nothing waits on the phone: a missing or failing LinkMeter only skips the phone steps.</li>
 *   <li>{@code tj-pocket-local}: the stock keyboard again (step 8), a look at the VPN (step 5) and the preload of the custom launcher (step 9).</li>
 * </ul>
 * Every step writes one {@code POCKET} line into the lock log (the persistent log the tablet report prints), so a run can be read afterwards.
 */
public final class PocketReturn {
    private static final String TAG = "TjPocket";
    private PocketReturn() {}

    /** The bridge in the tablet's LinkMeter ({@code LauncherBridge}); it answers only the Launcher's own signing key. */
    static final Uri BRIDGE = Uri.parse("content://com.tj.linkmeter.launcher");
    /** The one-line result of the last run (Launcher control and the tablet report). */
    public static final String LAST_KEY = "pocket_last";

    public static boolean on(Context c) { return Features.get(c, PocketRules.KEY, PocketRules.defaultOn()); }
    public static String last(Context c) { return Features.getString(c, LAST_KEY, "no pocket return yet"); }

    private static final Object LOCK = new Object();
    private static boolean running;
    private static long startedAt;
    /** A watcher that outlived its own caps (it cannot, but a stuck binder call could) does not block the next return for ever. */
    private static final long STALE_MS = PocketRules.TOTAL_CAP_MS + 60_000L;

    /** Starts the pocket work beside the plain return. Never blocks, never throws. */
    public static void begin(final Context c, final String via) {
        final Context app = c.getApplicationContext();
        try {
            synchronized (LOCK) {
                long now = SystemClock.elapsedRealtime();
                if (running && now - startedAt < STALE_MS) { LockLog.add(app, "POCKET", "a pocket return is already running (" + (now - startedAt) + " ms): this one only closes apps"); return; }
                running = true; startedAt = now;
            }
            LockLog.add(app, "POCKET", "start via " + via + " (switch on)");
            new Thread(new Runnable() { @Override public void run() { phoneAndWifi(app); } }, "tj-pocket").start();
            new Thread(new Runnable() { @Override public void run() { localSteps(app); } }, "tj-pocket-local").start();
        } catch (Throwable t) {
            Log.e(TAG, "begin failed", t);
            synchronized (LOCK) { running = false; }
        }
    }

    // ------------------------------------------------------------------------------------------------ steps 1-4: the phone (through LinkMeter) and the tablet's Wi-Fi

    /** Review CH1: a bridge call that does not answer (LinkMeter starting, frozen, stuck in a Bluetooth call) must not keep the tablet's Wi-Fi on: each call waits this long at most. */
    static final long CALL_MS = 6_000L;
    private static volatile boolean btHeld;

    /** True while a pocket run is going: the lock-screen rules must not switch Bluetooth off then (LinkMeter reaches the phone over it). Never true when the switch was never used. */
    public static boolean holdsBluetooth() { return btHeld; }

    private static void phoneAndWifi(Context app) {
        btHeld = true;
        try { phoneAndWifiBody(app); } finally { btHeld = false; }
    }

    private static void phoneAndWifiBody(Context app) {
        final long t0 = SystemClock.elapsedRealtime();
        boolean wifiOff = false;
        String verdictText = "";
        String lastLook = "";
        try {
            PocketRules.Verdict v = PocketRules.helloVerdict(parseHello(callBridge(app, "hello", CALL_MS)));
            verdictText = v.text;
            LockLog.add(app, "POCKET", "phone: " + v.text + " (+" + (SystemClock.elapsedRealtime() - t0) + " ms)");
            if (v.start) {
                PocketRules.Look look = parseLook(callBridge(app, "pocket_start", CALL_MS));
                String lastLogged = "";
                while (true) {   // the deadline is the first thing PocketRules.next looks at; every round either ends or waits a positive time
                    long el = SystemClock.elapsedRealtime() - t0;
                    PocketRules.Step st = PocketRules.next(look, wifiOff, el);
                    lastLook = look.answered ? look.state + (look.sent ? " (sleep order sent)" : "") : "no answer";   // state words only: what the phone said stays out of the public report
                    if (!lastLook.equals(lastLogged) || st.finished || st.wifiOffNow) { LockLog.add(app, "POCKET", "phone job +" + el + " ms: " + lastLook + " -> " + st.why); lastLogged = lastLook; }   // one line per CHANGE, not per poll (the lock log is a small ring)
                    if (st.wifiOffNow) wifiOff = wifiOff(app, st.why);
                    if (st.finished) break;
                    try { Thread.sleep(st.nextMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    look = parseLook(callBridge(app, "pocket_state", CALL_MS));
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "phone steps failed", t);
            LockLog.add(app, "POCKET", "phone steps stopped on an error: " + t);
        }
        if (!wifiOff) wifiOff = wifiOff(app, verdictText.isEmpty() ? "the phone steps ended" : "no phone job (" + verdictText + ")");   // the tablet step never depends on the phone
        Features.setStringAsyncIfChanged(app, LAST_KEY, PocketRules.summary(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()), verdictText, lastLook, wifiOff));
        synchronized (LOCK) { running = false; }
    }

    /** Step 4: the tablet's Wi-Fi off (normal permission, the same call the old reset used). True = it is off now. */
    private static boolean wifiOff(Context app, String why) {
        try {
            WifiManager wm = (WifiManager) app.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) { LockLog.add(app, "POCKET", "tablet Wi-Fi: no Wi-Fi service"); return false; }
            if (!wm.isWifiEnabled()) { LockLog.add(app, "POCKET", "tablet Wi-Fi was already off (" + why + ")"); return true; }
            boolean ok = wm.setWifiEnabled(false);
            LockLog.add(app, "POCKET", "tablet Wi-Fi off -> " + ok + " (" + why + ")");
            return ok;
        } catch (Throwable t) {
            Log.e(TAG, "wifi off failed", t);
            LockLog.add(app, "POCKET", "tablet Wi-Fi off failed: " + t);
            return false;
        }
    }

    // ------------------------------------------------------------------------------------------------ the bridge client

    /** One call into LinkMeter's bridge; null = no answer (not installed, refused by the system, its process died). Blocking: never on the main thread. */
    private static Bundle callBridge(final Context app, final String method, long timeoutMs) {
        final Bundle[] out = { null };
        Thread t = new Thread(new Runnable() { @Override public void run() {
            try {
                ContentResolver cr = app.getContentResolver();
                Bundle b = cr.call(BRIDGE, method, null, null);
                synchronized (out) { out[0] = b; }
            } catch (Throwable e) { Log.w(TAG, "bridge " + method + " failed: " + e); }
        } }, "tj-pocket-call");
        t.setDaemon(true);
        t.start();
        try { t.join(timeoutMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        synchronized (out) {
            if (out[0] == null && t.isAlive()) Log.w(TAG, "bridge " + method + " did not answer in " + timeoutMs + " ms");
            return out[0];   // null = no answer (the abandoned thread ends whenever the call does)
        }
    }

    static PocketRules.Hello parseHello(Bundle b) {
        if (b == null) return PocketRules.Hello.none();
        return new PocketRules.Hello(true, b.getBoolean("ok", false), b.getBoolean("tablet", false), b.getBoolean("linked", false), b.getString("why", ""));
    }

    static PocketRules.Look parseLook(Bundle b) {
        if (b == null) return PocketRules.Look.none();
        return new PocketRules.Look(true, b.getBoolean("ok", false), b.getString("state", ""), b.getBoolean("sent", false), b.getString("text", ""));
    }

    // ------------------------------------------------------------------------------------------------ steps 5, 8, 9: the tablet's own steps

    private static void localSteps(Context app) {
        try {   // step 8: the stock Android keyboard (Mode.set already did it; this is the explicit, idempotent second look)
            KeyboardSwitch.apply(app, Mode.CUSTOM);
            LockLog.add(app, "POCKET", "keyboard: " + KeyboardSwitch.status(app));
        } catch (Throwable t) { Log.e(TAG, "keyboard step failed", t); LockLog.add(app, "POCKET", "keyboard step failed: " + t); }
        try {   // step 5: the VPN. The level-triggered pipeline stops it in the quiet world (custom launcher); this forces a look NOW instead of at the next trigger
            AnimationSpeedup.syncNow(app, "pocket return");
            LockLog.add(app, "POCKET", "VPN: " + (VpnOff.vpnUp(app) ? "still up (the VPN-off rule decides; see the VPN lines)" : "none up"));
        } catch (Throwable t) { Log.e(TAG, "vpn step failed", t); LockLog.add(app, "POCKET", "VPN step failed: " + t); }
        try {   // step 9: load what can be loaded now (the patch / pencil layers, the Settings look-alike)
            TjAccessibilityService svc = TjAccessibilityService.get();
            if (svc != null) svc.pocketPreload("pocket return");
            else LockLog.add(app, "POCKET", "preload: the accessibility helper is off, nothing to preload");
            SettingsSpoof.syncSoon(app, "pocket return");
        } catch (Throwable t) { Log.e(TAG, "preload step failed", t); LockLog.add(app, "POCKET", "preload step failed: " + t); }
    }
}
