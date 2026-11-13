package com.tj.testnotifier;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * CI-only (TASKS CB2, emulator section vo): a REAL VPN, so the scenario can prove that the launcher turns a running VPN off when the screen goes off / the lock
 * screen is up. It routes only the unused test network 10.111.0.0/24 (the emulator's own traffic is not touched) and moves no packets: Android counts it as a
 * connected VPN all the same (the quick-settings row "Device connected to ...", a VPN network in ConnectivityManager). Started by MainActivity with
 * {@code --ez vpn true}; consent is given by the scenario with {@code appops set com.tj.testnotifier ACTIVATE_VPN allow}. Every state change is one logcat line
 * with the tag TJVPN, which the scenario reads.
 */
public class TestVpnService extends VpnService {
    private static final String TAG = "TJVPN";
    private ParcelFileDescriptor tun;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (tun == null) {
            try {
                tun = new Builder().setSession("ci-test").addAddress("10.111.0.2", 32).addRoute("10.111.0.0", 24).establish();
                Log.i(TAG, tun != null ? "established" : "establish returned null (not prepared)");
            } catch (Throwable t) { Log.i(TAG, "establish failed: " + t); }
        } else Log.i(TAG, "already up");
        return START_NOT_STICKY;
    }

    /** Android calls this when another app takes the VPN over (the launcher's VpnService.prepare): the VPN is already torn down by the system; close our end. */
    @Override public void onRevoke() {
        Log.i(TAG, "revoked");
        close();
        stopSelf();
    }

    @Override public void onDestroy() { close(); super.onDestroy(); }

    private void close() {
        ParcelFileDescriptor t = tun; tun = null;
        if (t != null) try { t.close(); } catch (Throwable ignored) { }
    }
}
