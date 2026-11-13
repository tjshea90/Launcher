package com.tj.launcher;

/**
 * CB2 (Tj, 2026-11-13: "If it can't be removed, make the launcher automatically turn off my VPN any time the screen turns off or the lock screen is on").
 * The quick-settings row "Device connected to <VPN>" is SystemUI's own and cannot be removed or covered well (research/VPN_FOOTER_2026-11-13.md), so the VPN
 * itself is turned off whenever the screen is dark or the lock screen is up. Pure rules (no Android), pinned by VpnOffRulesTest; VpnOff is the Android side.
 *
 * How the VPN is turned off: {@code VpnService.prepare(launcher)} once Android has allowed the launcher to use the VPN service (the app-op ACTIVATE_VPN, given
 * by Android's own one-time "Connection request" dialog or by adb): Android then makes the launcher the VPN app and revokes the running VPN (its app gets
 * onRevoke and disconnects). The launcher never sets up a VPN of its own, and nothing here ever turns a VPN on.
 *
 * Level-triggered: every trigger (screen off, the lock screen coming up, a wake to the lock screen, a VPN that connects while the tablet is dark or locked)
 * re-reads the facts. A VPN app that reconnects by itself is stopped again at most {@link #MAX_STOPS_PER_LOCK} times per lock (no ping-pong all night).
 */
public final class VpnOffRules {
    private VpnOffRules() { }

    /** The switch in Launcher control. */
    public static final String KEY = "vpn_off_when_locked";
    /** The last thing the rule did or refused (status line in Launcher control and the tablet report; never names the VPN app). */
    public static final String LAST_KEY = "vpn_off_last";
    /** A VPN that keeps reconnecting while the tablet stays locked is stopped this many times per lock, then left alone (and the status says so). */
    public static final int MAX_STOPS_PER_LOCK = 3;

    /** Release builds ON (Tj asked for it), debug builds OFF so the older emulator sections never see a VPN taken over (the section vo turns it on). */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** The reasons for doing nothing; {@link #STOP} = turn the VPN off now. */
    public static final String STOP = null;
    public static final String SWITCHED_OFF = "the switch is off";
    public static final String UNLOCKED = "the screen is on and unlocked";
    public static final String NO_VPN = "no VPN is connected";
    public static final String NOT_ALLOWED = "Android has not allowed the launcher to turn a VPN off yet (Launcher control: Allow)";
    public static final String GAVE_UP = "the VPN reconnected " + MAX_STOPS_PER_LOCK + " times during this lock: left alone until the next unlock";

    /**
     * Should the running VPN be turned off now?
     * @param screenDark the screen is off
     * @param lockUp the lock screen (keyguard) is showing
     * @param vpnUp a VPN network is connected
     * @param stopsThisLock how often it was turned off since the last unlock
     * @return {@link #STOP} (null) or the reason to leave it
     */
    public static String why(boolean switchOn, boolean allowed, boolean screenDark, boolean lockUp, boolean vpnUp, int stopsThisLock) {
        if (!switchOn) return SWITCHED_OFF;
        if (!screenDark && !lockUp) return UNLOCKED;
        if (!vpnUp) return NO_VPN;
        if (!allowed) return NOT_ALLOWED;
        if (stopsThisLock >= MAX_STOPS_PER_LOCK) return GAVE_UP;
        return STOP;
    }

    /** What the prepare call said. */
    public static final int PREPARED = 0, REFUSED = 1, LOCKDOWN = 2, FAILED = 3;

    /**
     * The status line after an attempt.
     * @param result PREPARED (prepare returned null: the launcher is the VPN app now), REFUSED (prepare returned the consent Intent: not allowed, or the
     *               running VPN is Android's "Always-on VPN", which no other app may replace), LOCKDOWN (IllegalStateException), FAILED (anything else)
     * @param vpnStillUp a VPN network is still connected a moment after the call
     */
    public static String outcome(int result, boolean allowed, boolean vpnStillUp) {
        switch (result) {
            case PREPARED: return vpnStillUp ? "asked Android to turn the VPN off, but a VPN is still connected (another VPN, or it reconnected at once)" : "turned the VPN off";
            case REFUSED: return allowed ? "Android refused: the VPN is set as \"Always-on VPN\" (Settings > Network > VPN); that one cannot be turned off by another app" : NOT_ALLOWED;
            case LOCKDOWN: return "Android refused: a lockdown VPN is active";
            default: return "failed (see the lock log)";
        }
    }

    /** The per-lock count after an attempt (only a real stop counts, so a refusal is retried at the next trigger and still logged once per trigger). */
    public static int afterAttempt(int stopsThisLock, int result) { return result == PREPARED ? stopsThisLock + 1 : stopsThisLock; }
}
