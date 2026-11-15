package com.tj.launcher;

/**
 * CB2 (Tj, 2026-11-13: "If it can't be removed, make the launcher automatically turn off my VPN any time the screen turns off or the lock screen is on";
 * CB2e: "And any time I lock the screen from quickstep"). The quick-settings row "Device connected to <VPN>" is SystemUI's own and cannot be removed or
 * covered well (research/VPN_FOOTER_2026-11-13.md), so the VPN itself is turned off whenever the screen is dark or the lock screen is up, in EITHER launcher.
 *
 * CE1 (Tj, 2026-11-15: "The VPN bar in the notification panel should never show up at any time on any lock screen or in custom launcher ... Never automatically
 * turn off fast wifi mode if I turned it on and I'm inside quickstep launcher"): the world is now the SAME one the pop-up banners (BH1) and the radio symbols
 * (BS1) use, {@link NotifQuietPolicy}: the custom launcher, a dark screen, the lock screen (any launcher) and a plain PIN still being decided are "quiet" = no
 * VPN; an unlocked, decided Quickstep session is the one place a VPN may run. The launcher also PUBLISHES that world ({@link #WORLD_KEY}) so the VPN's own app
 * (LinkMeter's Fast Wi-Fi) can pause itself instead of fighting the launcher: before, LinkMeter put its VPN back two seconds after every stop, three times, and
 * then {@link #GAVE_UP} left the VPN (and its row on the lock screen) alone.
 *
 * Pure rules (no Android), pinned by VpnOffRulesTest; VpnOff is the Android side.
 *
 * How the VPN is turned off: {@code VpnService.prepare(launcher)} once Android has allowed the launcher to use the VPN service (the app-op ACTIVATE_VPN, given
 * by Android's own one-time "Connection request" dialog or by adb): Android then makes the launcher the VPN app and revokes the running VPN (its app gets
 * onRevoke and disconnects). The launcher never sets up a VPN of its own, and nothing here ever turns a VPN on.
 *
 * Level-triggered: every trigger (screen off, the lock screen coming up, a wake to the lock screen, a VPN that connects while the tablet is dark or locked,
 * a look every 30 s while locked for a VPN the network callback cannot hear) re-reads the facts. A VPN app that reconnects by itself is stopped at most
 * {@link #MAX_STOPS} times within {@link #WINDOW_MS} (no ping-pong all night); an unlock starts from zero.
 */
public final class VpnOffRules {
    private VpnOffRules() { }

    /** The switch in Launcher control. */
    public static final String KEY = "vpn_off_when_locked";
    /** The last thing the rule did or refused (status line in Launcher control and the tablet report; never names the VPN app). */
    public static final String LAST_KEY = "vpn_off_last";
    /** At most this many stops within {@link #WINDOW_MS} (review of CB2, LOW4: a cap per LOCK used up its stops on reconnects hours apart). */
    public static final int MAX_STOPS = 3;
    public static final long WINDOW_MS = 10 * 60_000L;
    /** Android tears a revoked VPN down asynchronously: a second trigger this soon after a stop waits (review LOW5: one VPN counted as two stops). */
    public static final long SETTLE_MS = 2_000L;
    /** While the tablet is dark / locked the VPN is looked at this often (only while the CPU is awake anyway: review MED2, a VPN that does not route the launcher is never heard by the network callback). */
    public static final long POLL_MS = 30_000L;

    /** Release builds ON (Tj asked for it), debug builds OFF so the older emulator sections never see a VPN taken over (the section vo turns it on). */
    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    // ---------------------------------------------------------------- the published world (the contract with LinkMeter's Fast Wi-Fi)

    /**
     * {@code Settings.Global} key the launcher writes (it holds WRITE_SECURE_SETTINGS; no other normal app can write it, every app can read it). Value
     * {@link #WORLD_QUICKSTEP} = an unlocked, decided Quickstep session is on screen: a VPN may run. {@code quiet:<why>} = no VPN now (the custom launcher, the screen
     * is off, the lock screen is up, the unlock is not decided yet). Missing = the launcher says nothing (switch off, no adb grant, app removed): the reader acts alone.
     * LinkMeter's tablet app has the same two words (FastRules.WORLD_*), keep them in step.
     */
    public static final String WORLD_KEY = "tj_launcher_world";
    public static final String WORLD_QUICKSTEP = "quickstep";
    public static final String WORLD_QUIET = "quiet";

    /** The value to publish: {@code quickstep}, or {@code quiet:<why>} ({@code why} in plain words: {@link NotifQuietPolicy#quietFor}). */
    public static String worldValue(boolean quiet, String why) {
        if (!quiet) return WORLD_QUICKSTEP;
        return WORLD_QUIET + ":" + (why == null || why.trim().isEmpty() ? "quiet" : why.trim());
    }

    /** Does a published value say "no VPN now"? (Anything else, also null and unknown words, says "may run": the reader must never stay down for a word it does not know.) */
    public static boolean isQuiet(String value) {
        return value != null && (value.equals(WORLD_QUIET) || value.startsWith(WORLD_QUIET + ":"));
    }

    /** The reasons for doing nothing; {@link #STOP} = turn the VPN off now. */
    public static final String STOP = null;
    public static final String SWITCHED_OFF = "the switch is off";
    public static final String UNLOCKED = "an unlocked Quickstep session is on screen (a VPN may run there)";
    public static final String NO_VPN = "no VPN is connected";
    public static final String NOT_ALLOWED = "Android has not allowed the launcher to turn a VPN off yet (Launcher control: Allow)";
    public static final String REFUSED_BEFORE = "Android refused earlier during this lock: not asked again until the next unlock";
    public static final String SETTLING = "a VPN was turned off a moment ago: looked at again in a moment";
    public static final String GAVE_UP = "the VPN reconnected " + MAX_STOPS + " times within " + (WINDOW_MS / 60_000L) + " minutes: left alone for now (next unlock, or when the 10 minutes are over)";

    /**
     * Should the running VPN be turned off now?
     * @param quietWorld the custom launcher is in front, the screen is off, the lock screen is up, or a plain PIN is not decided yet ({@link NotifQuietPolicy#wanted})
     * @param vpnUp a VPN network is connected
     * @param recentStops stops within the last {@link #WINDOW_MS} ({@link #recentStops})
     * @param refusedThisLock Android refused (Always-on VPN, lockdown) since the last unlock
     * @param msSinceLastStop time since the last prepare call (a large number when there was none)
     * @return {@link #STOP} (null) or the reason to leave it
     */
    public static String why(boolean switchOn, boolean allowed, boolean quietWorld, boolean vpnUp, int recentStops, boolean refusedThisLock, long msSinceLastStop) {
        if (!switchOn) return SWITCHED_OFF;
        if (!quietWorld) return UNLOCKED;
        if (!vpnUp) return NO_VPN;
        if (!allowed) return NOT_ALLOWED;
        if (refusedThisLock) return REFUSED_BEFORE;
        if (msSinceLastStop >= 0 && msSinceLastStop < SETTLE_MS) return SETTLING;
        if (recentStops >= MAX_STOPS) return GAVE_UP;
        return STOP;
    }

    /** A reason worth ONE lock-log line per lock (the others are silent: they happen at every trigger by design). */
    public static boolean worthALine(String why) { return NOT_ALLOWED.equals(why) || GAVE_UP.equals(why); }

    /** How many of {@code stopTimes} (elapsed-realtime ms, 0 = empty slot) lie within the window before {@code now}. */
    public static int recentStops(long[] stopTimes, long now) {
        int n = 0;
        for (long t : stopTimes) if (t > 0 && now - t >= 0 && now - t < WINDOW_MS) n++;
        return n;
    }

    /** Record a stop at {@code now}: the oldest slot is replaced (the array keeps the last {@code stopTimes.length} stops). */
    public static void recordStop(long[] stopTimes, long now) {
        int oldest = 0;
        for (int i = 1; i < stopTimes.length; i++) if (stopTimes[i] < stopTimes[oldest]) oldest = i;
        stopTimes[oldest] = now;
    }

    /** What the prepare call said. */
    public static final int PREPARED = 0, REFUSED = 1, LOCKDOWN = 2, FAILED = 3;

    /** After this result no further prepare call is made until the next unlock (it would only be refused again and fill the lock log). */
    public static boolean refusesForThisLock(int result) { return result == REFUSED || result == LOCKDOWN; }

    /**
     * The status line after an attempt.
     * @param result PREPARED (prepare returned null: the launcher is the VPN app now), REFUSED (prepare returned the consent Intent although allowed: the running
     *               VPN is Android's "Always-on VPN", which no other app may replace), LOCKDOWN (IllegalStateException), FAILED (anything else)
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

    /** The late look ("still connected?") is only believed when it really ran soon after the call (review LOW11: a timer of a sleeping CPU can run minutes later, after Tj reconnected). */
    public static boolean lateLookMeaningful(long msAfterCall) { return msAfterCall >= 0 && msAfterCall <= 10_000L; }
}
