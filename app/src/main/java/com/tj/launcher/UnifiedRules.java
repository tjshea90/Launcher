package com.tj.launcher;

/**
 * CV1 (Tj, 2026-11-22): ONE launcher for both worlds. Pure rules (JVM-testable) behind {@link Unified} and {@link HomeProfile}; design: research/CV1_UNIFIED_LAUNCHER_DESIGN_2026-11-22.md.
 *
 * <p>{@link Mode} stays the one state variable. With the switch on, the home that answers the Home button in the QUICKSTEP world is OUR Launcher3 too, running in the UNLOCKED profile
 * (every launchable app, its own icon layout, no hidden-app logic, no patches). The stock launcher stays installed as Android's Recents host and as the emergency exit: the crash breaker
 * ({@link CrashGuard}) trips = every rule here answers exactly like version 0.9.16 (stock home in the QUICKSTEP world).
 */
public final class UnifiedRules {
    private UnifiedRules() {}

    /** Layout database file per profile (Launcher3's {@code LauncherFiles.LAUNCHER_DB} is the custom one). */
    public static final String DB_CUSTOM = "launcher.db";
    public static final String DB_UNLOCKED = "launcher_unlocked.db";

    /** Which home the Router starts. */
    public enum Home { OURS, STOCK }

    /** The state of the Launcher3 model: what it shows and which layout it holds. */
    public enum Profile { CUSTOM, UNLOCKED }

    /** The unified launcher is in force: the switch is on and the crash breaker has not tripped (a tripped breaker = emergency exit = the old two-launcher behaviour). */
    public static boolean active(boolean switchOn, boolean breakerTripped) { return switchOn && !breakerTripped; }

    /** Router: CUSTOM always answers with our Launcher; the QUICKSTEP world with ours only while the unified launcher is in force, else the stock home. */
    public static Home homeFor(boolean customMode, boolean active) { return customMode || active ? Home.OURS : Home.STOCK; }

    /** The profile our Launcher3 must hold: UNLOCKED only in the QUICKSTEP world while the unified launcher is in force; in every other state the custom profile (also when it is shown by accident). */
    public static Profile profile(boolean customMode, boolean active) { return !customMode && active ? Profile.UNLOCKED : Profile.CUSTOM; }

    public static String dbName(Profile p) { return p == Profile.UNLOCKED ? DB_UNLOCKED : DB_CUSTOM; }

    public static Profile profileOfDb(String db) { return DB_UNLOCKED.equals(db) ? Profile.UNLOCKED : Profile.CUSTOM; }

    /** The model must be reloaded when it holds a profile other than the wanted one. {@code loaded == null}: nothing loaded in this process yet, the first load picks the wanted profile itself. */
    public static boolean reloadNeeded(Profile wanted, Profile loaded) { return loaded != null && wanted != loaded; }

    /** The provider could not switch to {@code failedFor} less than {@link #SWITCH_RETRY_MS} ago: do not start another reload for it yet (a failing switch would reload for ever) and never hold a cover for it. */
    public static boolean holdOff(Profile wanted, Profile failedFor, long failedAt, long now) {
        return failedFor != null && failedFor == wanted && now >= failedAt && now - failedAt < SWITCH_RETRY_MS;
    }

    public static final long SWITCH_RETRY_MS = 60_000;

    /** The profile is READY (the cover may go, the unlock may proceed): the model holds the wanted profile AND its load has finished. */
    public static boolean ready(Profile wanted, Profile loaded, boolean modelLoaded) { return loaded == wanted && modelLoaded; }

    /**
     * CW1 A1: the same, where "the load has finished" may also be "the load has bound workspace and app list" ({@code appsBound}: what a person sees and can open is complete; the icon cache update, the deep shortcuts, the widgets and
     * three idle waits that follow only prepare things nobody can reach yet). The profile must still be the WANTED one: a bound list of the other profile is never ready.
     */
    public static boolean ready(Profile wanted, Profile loaded, boolean modelLoaded, boolean appsBound) { return loaded == wanted && (modelLoaded || appsBound); }

    /** The stray-custom-home guard ("our Launcher is in front in a Quickstep session = a mistake") only exists while the stock launcher is the Quickstep home. */
    public static boolean strayCustomApplies(boolean active) { return !active; }

    /** The stray-stock-home guard ("the stock HOME window is in front") fights in the custom world always, and in the QUICKSTEP world once our launcher is its home. */
    public static boolean strayStockApplies(boolean customMode, boolean active) { return customMode || active; }

    /**
     * After a handshake: is the right home in front? Stock launcher or the remembered app (old world), our launcher or the remembered app (unified world). The Solitaire stand-in and any other app are NOT
     * "Quickstep in front" (the caller passes {@code resumedIsRemembered}).
     */
    public static boolean quickstepFrontOk(boolean active, boolean stockInFront, boolean ownInFront, boolean resumedIsRemembered) {
        return resumedIsRemembered || (active ? ownInFront : stockInFront);
    }

    /** A6 (CW1): which window look the handshake front check needs. Pure counterpart of {@link #quickstepFrontOk}: it only evaluates the side whose answer is used (the other side's answer was thrown away). */
    public enum FrontLook { NONE, OWN, STOCK }

    /** A6: the remembered app is in front = nothing else to look at; else our home (unified world) or the stock home (old world). {@code quickstepFrontOk(a, s, o, r)} equals {@code quickstepFrontOk(a, look==STOCK && s, look==OWN && o, r)}. */
    public static FrontLook frontLook(boolean active, boolean resumedIsRemembered) { return resumedIsRemembered ? FrontLook.NONE : active ? FrontLook.OWN : FrontLook.STOCK; }

    /** A4 (CW1): how often the settle loop may fetch the accessibility tree while it only waits for the profile reload (ms). The one Home press (>= 200 ms) and the 1.5 s give-up keep their meaning. */
    public static final long SETTLE_LOOK_MS = 200;

    /**
     * A4: does this tick of the return-cover settle loop need the accessibility tree (a cross-process fetch on the thread that is binding the Launcher)? The deadline of the next look comes FIRST. Between looks, while the
     * profile reload is pending ({@code waitingForProfile}) and our Launcher is resumed ({@code launcherResumed}), the only possible answer is "keep waiting", so the tree is not fetched. Every other state looks every tick, as before.
     */
    public static boolean settleNeedsTree(boolean waitingForProfile, boolean launcherResumed, long dtMs, long nextLookAtMs) {
        if (dtMs >= nextLookAtMs) return true;
        return !(waitingForProfile && launcherResumed);
    }

    /** Apps started from our home belong to the custom world only while the custom profile is shown: in the unlocked profile they are Quickstep-session apps (cleaned at the return). */
    public static boolean marksCustomOrigin(Profile wanted) { return wanted == Profile.CUSTOM; }

    /** The unlocked drawer shows what the stock drawer shows: every launchable app and every launcher entry of our own package (Launcher control, Back to custom, ...) except the Settings look-alike. */
    public static boolean shownInUnlocked(String pkg, String className, String selfPackage) {
        if (pkg == null) return false;
        if (pkg.equals(selfPackage)) return className == null || !SettingsSpoof.ALIAS.equals(className);
        return true;
    }
}
