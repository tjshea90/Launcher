package com.tj.launcher;

import android.content.Context;

/**
 * N3 (Tj, 2026-11-04): the custom launcher and Quickstep behave like TWO USER ACCOUNTS. The custom launcher is the default one; Quickstep exists only behind a secret handshake on the
 * lock screen ({@link UnlockMethods}). Locking the tablet ENDS a Quickstep session (like logging out): a plain PIN afterwards is the custom account again.
 *
 * What makes the custom account open "like a regular unlock, no delay": all of the switching is done WHILE the lock screen covers the screen.
 *  - wake on the lock screen: the custom stack (custom home + Solitaire) is started behind the keyguard (Mode is left alone until the unlock decides),
 *  - handshake accepted: Quickstep's home is started behind the keyguard instead (and Mode flips, so tiles / keyboard follow),
 *  - plain unlock after a Quickstep session: Mode flips to custom and the return reset closes what Quickstep left behind (hidden by the black cover that went up at the Enter tap).
 * Pure rules here (JVM-testable); {@link TjAccessibilityService} feeds the events and does the starting.
 */
public final class ProfileSession {
    // AC1 (v0.7.3): "locking ends a Quickstep session" is no longer a switch (Tj: a plain PIN must ALWAYS open the custom launcher, without exception). Only the paused state ({@link CrashGuard#automationOff}) stops it.
    public static final String PREARM_KEY = "profile_prearm";                // lock screen: put the custom home (and Solitaire, only if "Open Solitaire automatically" is on) in front behind the keyguard, default ON (an OFF-ramp)
    public static final String RADIOS_KEY = "profile_radios_off";            // custom account: Wi-Fi and Bluetooth are switched off again if something turns them on, default ON
    public static final String PRECLEAN_KEY = "profile_preclean";            // O4: closing/locking from Quickstep closes its apps + notifications AT LOCK (needs the adb helper), default ON
    public static final String BUZZ_KEY = "unlock_buzz";                     // a short buzz when a handshake is accepted, default ON
    public static final String PREARM_REPORT_KEY = "profile_last";           // what the last lock-screen switch did (shown in Launcher control)

    private ProfileSession() {}

    public static boolean prearm(Context c) { return Features.get(c, PREARM_KEY, true); }
    public static boolean radiosOff(Context c) { return Features.get(c, RADIOS_KEY, true); }
    public static boolean preClean(Context c) { return Features.get(c, PRECLEAN_KEY, true); }
    public static boolean buzz(Context c) { return Features.get(c, BUZZ_KEY, true); }

    /**
     * Pure: wake-up on the LOCK screen (keyguard locked, screen on). Put the custom stack (the custom home; Solitaire only when {@link ReturnReset#autoOpenWanted}) in front behind the keyguard?
     * Never while a handshake is armed (the target is Quickstep then). {@code prearmEnabled} is the OFF-ramp switch (default on); {@code automationOff} = breaker tripped or paused.
     */
    public static boolean prearmCustomStackOnLockedWake(boolean prearmEnabled, boolean keyguardLocked, boolean handshakeArmed, boolean alreadyDoneThisLock, boolean automationOff) {
        return prearmEnabled && keyguardLocked && !handshakeArmed && !alreadyDoneThisLock && !automationOff;
    }

    /**
     * O4 (Tj, 2026-11-04: "when I am closing or locking the screen from the quickstep launcher, close all apps and notifications so that everything is already ready"):
     * the tablet is LOCKED while a Quickstep session is current, nobody has given a handshake, and the shell helper can do the clean-up with no screen at all
     * (force-stop + removing the Recents cards; the shade and Recents cannot be driven with the screen off) -> clean up NOW and switch to the custom account behind the lock screen.
     */
    public static boolean preCleanAtLock(boolean preCleanEnabled, boolean customMode, boolean automationOff, boolean keyguardLocked, boolean handshakeArmed, boolean helperReady, boolean alreadyDone) {
        return preCleanEnabled && !customMode && !automationOff && keyguardLocked && !handshakeArmed && helperReady && !alreadyDone;
    }

    /** Pure: a plain unlock (USER_PRESENT, no handshake): is the Quickstep session over, so the custom account takes the tablet? ALWAYS, unless automation is paused (AC1). */
    public static boolean plainUnlockEndsQuickstep(boolean customMode, boolean automationOff) {
        return !customMode && !automationOff;
    }

    /** Pure: the Enter tap of a plain PIN while a Quickstep session is still current: raise the black cover now, so the unlock animation never shows Quickstep's leftovers. */
    public static boolean coverAtEnter(boolean customMode, boolean automationOff, boolean handshakeGiven, boolean maybeWorkToDo, boolean coverEnabled) {
        return plainUnlockEndsQuickstep(customMode, automationOff) && !handshakeGiven && maybeWorkToDo && coverEnabled;
    }

    /** Pure: is this the first Home of a NEW boot (the boot counter changed since we last looked) while a Quickstep session is still marked? A restart kills every app, so the session is over. */
    public static boolean bootEndsQuickstep(int seenBootCount, int nowBootCount, boolean customMode, boolean automationOff) {
        return nowBootCount >= 0 && seenBootCount != nowBootCount && !customMode && !automationOff;
    }

    /**
     * Pure (AC9): a handshake was just accepted. Quickstep's home goes in front behind the lock screen AT ONCE, whatever the pre-arm switch says: the custom stack may already be there
     * (prepared at the wake) and must be replaced before the unlock reveals it.
     */
    public static boolean stockBehindLockOnHandshake() { return true; }

    /** Pure (AC9): after an unlock that chose Quickstep, are we still looking at OUR launcher or the Solitaire stand-in (a late start from the lock-screen preparation)? Then Quickstep is started again. */
    public static boolean restartQuickstepAfterUnlock(boolean ourLauncherInFront, boolean targetInFront, int attempt) {
        return attempt < VERIFY_DELAYS_MS.length && (ourLauncherInFront || targetInFront);
    }

    /** AC9: when (ms after the unlock) the helper looks again whether Quickstep really is in front, at most this many times. */
    public static final long[] VERIFY_DELAYS_MS = { 600, 900 };

    /** AC2 follow-up: how long (ms) after an unlock a window that appears is still the lock screen's leftover (the unlock animation reveals what was underneath), not an app Tj opened. */
    public static final long UNLOCK_QUIET_MS = 800;

    /**
     * Pure (AC2 follow-up, 2026-11-06, emulator run 143): is a window that appears NOW what was UNDERNEATH the lock screen, rather than an app Tj opened in Quickstep? While the lock screen is up, while a
     * handshake is armed (the unlock is about to happen) and for a moment after the unlock, Android shows the app that was last in front behind the keyguard (the custom account's own app). It used to be the
     * Solitaire stand-in, which is the target and is never counted; since the lock-screen preparation no longer starts Solitaire when its switch is off, it is the custom account's last app, and counting it as
     * "opened in Quickstep" got that app (and its notification) closed at the next lock.
     */
    public static boolean windowIsLockLeftover(boolean keyguardLocked, boolean handshakeArmed, long now, long lastUnlockAt) {
        if (keyguardLocked || handshakeArmed) return true;
        long since = now - lastUnlockAt;
        return lastUnlockAt > 0 && since >= 0 && since < UNLOCK_QUIET_MS;
    }

    /** Pure: switch Wi-Fi / Bluetooth off again? Custom account only, at most 3 attempts per 60 s (a radio that refuses to go off is never fought in a loop). */
    public static boolean enforceRadios(boolean enabled, boolean customMode, long now, long[] recent) {
        if (!enabled || !customMode) return false;
        int n = 0;
        for (long t : recent) if (t > 0 && now - t <= 60_000) n++;
        return n < 3;
    }

    /**
     * Pure failsafe (L10): with the accessibility helper OFF no handshake can be seen, so the custom launcher would lock Tj in for good. Three Home presses inside 1.6 s then let him out
     * to Quickstep. With the helper ON this is ignored: the only way out is the handshake.
     */
    public static boolean homeFailsafe(boolean helperRunning, boolean customMode, long now, long p1, long p2) {
        if (helperRunning || !customMode) return false;
        return p1 > 0 && p2 > 0 && now - p2 <= 800 && p2 - p1 <= 800 && now >= p2 && p2 >= p1;
    }

    /** Called at every Home (the router): notices a new boot, remembers it, and ends a Quickstep session that outlived the restart. Cheap: one settings read. */
    public static void checkBoot(Context c) {
        try {
            int now = android.provider.Settings.Global.getInt(c.getContentResolver(), android.provider.Settings.Global.BOOT_COUNT, -1);
            int seen = Features.getInt(c, "boot_count_seen", -2);
            if (now == seen) return;
            Features.setInt(c, "boot_count_seen", now);
            if (seen == -2) return;   // first look after install
            ReturnReset.markBootClean(c);   // BE2/BE3: a new boot (clean start only): the first unlock clears the Recents list the last boot left behind
            if (bootEndsQuickstep(seen, now, Mode.isCustom(c), CrashGuard.automationOff(c))) {
                Mode.set(c, Mode.CUSTOM);
                AppOrigin.clearQuickstep(c);
                Features.setString(c, PREARM_REPORT_KEY, "boot: a new boot ended the Quickstep session (custom launcher)");
            }
        } catch (Throwable t) { android.util.Log.e("TjBoot", "boot check failed", t); }
    }

    // ------------------------------------------------------------------ the two actions every method ends in
    /** Quickstep -> custom (any "kill switch" method): same as the double-Home return. Safe to call from any thread with a context. */
    public static void returnToCustom(Context c, String why) { returnToCustom(c, why, null); }

    /**
     * CH1: {@code pocketVia} is PocketRules.VIA_TILE / VIA_ICON when the return came from the tile or the icon, else null. With the "Pocket return" switch ON that return also puts the phone to
     * sleep through LinkMeter, switches the tablet's Wi-Fi off and force-stops every open app but LinkMeter and the keyboards; with the switch OFF (or null) this is the old method, unchanged.
     */
    public static void returnToCustom(Context c, String why, String pocketVia) {
        Context app = c.getApplicationContext();
        if (Mode.isCustom(app)) return;
        final boolean pocket = PocketRules.active(PocketReturn.on(app), pocketVia);
        if (pocket) PocketReturn.begin(app, pocketVia);   // CH1: the phone steps start FIRST and run beside the return (nothing waits on the phone)
        CrashGuard.clearTripped(app);
        Mode.set(app, Mode.CUSTOM);
        Features.setString(app, FailsafeKeys.LAST_KEY, "back to custom via " + why + " at " + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
        app.startActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME).setPackage(app.getPackageName())
                .putExtra(RouterActivity.EXTRA_PREARM, "custom").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
        ReturnReset.run(app, pocket);
    }

    /** Custom -> Quickstep (the master failsafe). Not a Home press: the router must not read it as a double-Home. */
    public static void goToQuickstep(Context c, String why) {
        Context app = c.getApplicationContext();
        Mode.set(app, Mode.QUICKSTEP);
        Features.setString(app, FailsafeKeys.LAST_KEY, "Quickstep via " + why + " at " + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
        app.startActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME).setPackage(app.getPackageName())
                .putExtra(RouterActivity.EXTRA_PREARM, "stock").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }
}
