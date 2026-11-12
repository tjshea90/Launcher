package com.tj.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Which launcher answers the Home button: our ported Launcher3 (CUSTOM) or the stock Quickstep (QUICKSTEP).
 * Stored in plain app preferences (no secure settings, no lock-screen state). CUSTOM is the default.
 */
public final class Mode {
    public static final String CUSTOM = "custom";
    public static final String QUICKSTEP = "quickstep";
    private static final String PREFS = "tj_mode";
    private static final String KEY = "mode";

    private Mode() {}

    public static long lastHomePressMs(Context c) { return prefs(c).getLong("lastHome", 0); }

    /** apply(): called on every Home press, must stay cheap. */
    public static void recordHomePress(Context c, long now) {
        SharedPreferences p = prefs(c);
        p.edit().putLong("lastHome2", p.getLong("lastHome", 0)).putLong("lastHome", now).apply();
    }

    /** The Home press before the last one (the L10 failsafe needs three in a row). */
    public static long homePressBeforeLastMs(Context c) { return prefs(c).getLong("lastHome2", 0); }

    /** AI1: when the current / last Quickstep session began and ended (ms; 0 = unknown, e.g. a session that began before this version). */
    public static long qsStartAt(Context c) { return prefs(c).getLong("qsStartAt", 0); }
    public static long qsEndAt(Context c) { return prefs(c).getLong("qsEndAt", 0); }

    /** When the mode last changed (ms): the stray-Quickstep guard waits a moment after a deliberate change (late window events from the screen we just left). */
    public static long changedAt(Context c) { return prefs(c).getLong("changedAt", 0); }

    /** AC1: when the last unlock that CONFIRMED a Quickstep session (a handshake) happened (ms). With {@link #changedAt} it dates the start of the current Quickstep session. */
    public static long confirmedAt(Context c) { return prefs(c).getLong("qsConfirmedAt", 0); }
    public static void confirmQuickstep(Context c, long now) { prefs(c).edit().putLong("qsConfirmedAt", now).commit(); }

    /** AC1: the start of the current Quickstep session = the later of the mode change and the last handshake unlock; 0 = unknown (a test helper wrote the mode without a time). */
    public static long sessionRefMs(Context c) { return Math.max(changedAt(c), confirmedAt(c)); }

    /** W5: while a fail-open window is active (the router had to fall back to the stock launcher because ours would not start) the stray-Quickstep guard stands down. */
    public static long failOpenUntil(Context c) { return prefs(c).getLong("failOpenUntil", 0); }
    public static void markFailOpen(Context c, long untilMs) { prefs(c).edit().putLong("failOpenUntil", untilMs).commit(); }

    // AB2: one handle per process (see Features.p)
    private static volatile SharedPreferences cached;
    private static SharedPreferences prefs(Context c) {
        SharedPreferences s = cached;
        if (s == null) { s = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); cached = s; }
        return s;
    }

    public static String get(Context c) {
        String m = prefs(c).getString(KEY, CUSTOM);
        return QUICKSTEP.equals(m) ? QUICKSTEP : CUSTOM;
    }

    public static boolean isCustom(Context c) { return CUSTOM.equals(get(c)); }

    /** commit() (not apply): the router reads this a moment later, possibly after our process is killed. */
    public static void set(Context c, String mode) {
        String m = QUICKSTEP.equals(mode) ? QUICKSTEP : CUSTOM;
        boolean quickstepBegins = QUICKSTEP.equals(m) && CUSTOM.equals(get(c));
        boolean quickstepEnds = CUSTOM.equals(m) && QUICKSTEP.equals(get(c));
        long nowMs = System.currentTimeMillis();
        SharedPreferences.Editor ed = prefs(c).edit().putString(KEY, m).putLong("changedAt", nowMs);
        if (quickstepBegins) ed.putLong("qsStartAt", nowMs);   // AI1: the Quickstep session's own start and end (changedAt moves at EVERY change): the clean-up asks Android's usage log what was used in between
        if (quickstepEnds) ed.putLong("qsEndAt", nowMs);
        ed.commit();
        try { AnimationSpeedup.syncNow(c, "mode " + m); } catch (Throwable ignored) { }   // BF1 (Tj: animations off ONLY inside Quickstep, back on "immediately ... in the custom launcher"): first thing after the mode is stored, before the slower profile switches below
        try { SettingsSpoof.syncSoon(c, "mode " + m); } catch (Throwable ignored) { }   // BN1: the Settings look-alike's drawer entry exists only in the custom mode (level-triggered, a few seconds late on a background thread: never on the unlock path)
        if (quickstepBegins) BatterySaverGuard.noteQuickstepBegins(c);   // review of BA2: only a session that begins while the tablet is UNLOCKED counts for the saver memory
        if (quickstepBegins) AppOrigin.invalidateUsedCache();   // BE3: apps used in this session are not in a cache made before it
        if (quickstepBegins) TaskOrigin.sessionStart(c);   // AA3: remember which tasks are the custom account's BEFORE Quickstep opens anything (needs the shell helper; otherwise a no-op)
        KeyboardSwitch.apply(c, m);
        TileProfile.apply(c, m);
        ImmersiveProfile.apply(c, m);  // AA1: status bar hidden while the custom launcher is active (opt-in, needs the adb grant)
        if (BatterySaverPolicy.modeSetChecks(CUSTOM.equals(m), quickstepEnds, BatterySaverGuard.strict(c))) BatterySaverGuard.check(c, "the custom launcher became active", false, quickstepEnds);   // BA2: a Quickstep session ending = the saver's state right now is Quickstep's: remembered before the switch-off.  AC3: battery saver turned on in Quickstep is switched off the moment custom begins; AN1: only at a REAL change ("strict" keeps the old way); AZ2: Quickstep is never touched (the v0.7.5 switch-ON is gone: it left Android's state machine out of step)
        // (Q3's retail-demo flag was REMOVED by the AB1 safety audit: see DemoModeProfile)       // D6: one quick-settings tile in custom mode, Tj's three in Quickstep (needs the adb grant)   // V2: stock keyboard in custom mode, Gboard in Quickstep (BX1; only if switched on and adb-granted)
    }
}
