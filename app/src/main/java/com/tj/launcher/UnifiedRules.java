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

    /** The profile is READY (the cover may go, the unlock may proceed): the model holds the wanted profile AND its load has finished. */
    public static boolean ready(Profile wanted, Profile loaded, boolean modelLoaded) { return loaded == wanted && modelLoaded; }

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

    /** Apps started from our home belong to the custom world only while the custom profile is shown: in the unlocked profile they are Quickstep-session apps (cleaned at the return). */
    public static boolean marksCustomOrigin(Profile wanted) { return wanted == Profile.CUSTOM; }

    /** The unlocked drawer shows what the stock drawer shows: every launchable app and every launcher entry of our own package (Launcher control, Back to custom, ...) except the Settings look-alike. */
    public static boolean shownInUnlocked(String pkg, String className, String selfPackage) {
        if (pkg == null) return false;
        if (pkg.equals(selfPackage)) return className == null || !SettingsSpoof.ALIAS.equals(className);
        return true;
    }
}
