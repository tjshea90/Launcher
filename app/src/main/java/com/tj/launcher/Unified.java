package com.tj.launcher;

import android.content.Context;

/**
 * CV1: the one-launcher switch and its facts (rules: {@link UnifiedRules}). Switch {@code unified_launcher}: release default ON, debug default OFF (same pattern as clean_start / the Settings look-alike:
 * the older emulator sections keep testing the two-launcher path; the new sections switch it on). Never throws.
 */
public final class Unified {
    public static final String KEY = "unified_launcher";

    private Unified() {}

    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    public static boolean switchOn(Context c) {
        try { return Features.get(c, KEY, defaultOn(ReturnReset.isDebuggable(c))); } catch (Throwable t) { return false; }
    }

    /** The unified launcher is in force: the switch is on and the crash breaker has not tripped (emergency exit = the old behaviour). */
    public static boolean active(Context c) {
        try { return UnifiedRules.active(switchOn(c), CrashGuard.wasTripped(c)); } catch (Throwable t) { return false; }
    }

    /** Which home the Router starts for the current mode. */
    public static UnifiedRules.Home home(Context c) { return UnifiedRules.homeFor(Mode.isCustom(c), active(c)); }

    /** The profile our Launcher3 must hold right now. */
    public static UnifiedRules.Profile wantedProfile(Context c) { return UnifiedRules.profile(Mode.isCustom(c), active(c)); }

    /** True while our own launcher is the Quickstep world's home (the unlocked profile is wanted). */
    public static boolean unlockedHome(Context c) { return wantedProfile(c) == UnifiedRules.Profile.UNLOCKED; }

    /** The switch was changed by hand (Launcher control): a deliberate choice also ends an emergency, like every deliberate way back (CrashGuard.clearTripped). */
    public static void setSwitch(Context c, boolean on) {
        Features.set(c, KEY, on);
        try { CrashGuard.clearTripped(c); } catch (Throwable ignored) { }
        try { HomeProfile.syncNow(c, "switch " + (on ? "on" : "off")); } catch (Throwable ignored) { }
    }
}
