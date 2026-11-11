package com.tj.launcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Pure rule for which apps the custom launcher shows (JVM-testable).
 * Model: an "allowed" set of packages. At the very first run the allowed set is every app already installed ("baseline"), so nothing Tj already has disappears;
 * after that an app is shown ONLY if it is allowed, so anything installed later stays hidden until Tj allows it, one by one. Hiding = removing from the set.
 * Our own package is never shown (Launcher control must not be visible in the custom launcher), except components in {@link #ALWAYS_SHOWN_OWN_COMPONENTS}.
 */
public final class AppVisibility {
    /** Own components that DO appear in the custom drawer: the Settings look-alike's alias (BN1; the alias itself is enabled only while {@link SettingsSpoof#aliasWanted} says so). */
    public static final Set<String> ALWAYS_SHOWN_OWN_COMPONENTS = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(SettingsSpoof.ALIAS)));

    private AppVisibility() {}

    /** Baseline = everything installed now except ourselves. */
    public static Set<String> baseline(Set<String> installedLaunchablePackages, String selfPackage) {
        Set<String> s = new HashSet<>(installedLaunchablePackages);
        s.remove(selfPackage);
        return s;
    }

    public static boolean isShown(Set<String> allowed, String pkg, String className, String selfPackage) {
        if (pkg == null) return false;
        if (pkg.equals(selfPackage)) return className != null && ALWAYS_SHOWN_OWN_COMPONENTS.contains(className);
        return allowed.contains(pkg);
    }

    public static Set<String> withShown(Set<String> allowed, String pkg, boolean shown) {
        Set<String> s = new HashSet<>(allowed);
        if (shown) s.add(pkg); else s.remove(pkg);
        return s;
    }

    /** An uninstalled app is forgotten, so reinstalling it later counts as a NEW app (hidden until allowed). */
    public static Set<String> withoutRemoved(Set<String> allowed, String pkg) { return withShown(allowed, pkg, false); }

    /**
     * Packages Tj wants in the custom drawer from the moment they are installed, without a visit to Hidden apps (BV1, 2026-11-11: "The music app should be in the custom launcher
     * app drawer"): Eleven, the LineageOS music app that ships beside the Launcher (module eleven/). They are allowed ONCE per INSTALL, automatically ({@link #asked}); if Tj hides one
     * later it stays hidden (an update keeps its install time), and a reinstall counts as new again ({@link #removed}, and for a missed removal event the new install time).
     */
    public static final Set<String> AUTO_ALLOWED_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("org.lineageos.eleven")));

    /**
     * The two sets of the drawer rule, always changed together: {@code allowed} (what the drawer shows) and {@code done} (one entry per INSTALL of an auto-allowed package that was
     * already handled, either allowed by the rule or decided by Tj: {@code "pkg@firstInstallTime"}, see {@link #doneEntry}).
     */
    public static final class Marks {
        public final Set<String> allowed;
        public final Set<String> done;
        public Marks(Set<String> allowed, Set<String> done) {
            this.allowed = allowed == null ? new HashSet<String>() : allowed;
            this.done = done == null ? new HashSet<String>() : done;
        }
    }

    /**
     * One "done" entry names the INSTALL, not just the package (review of BV1, 2026-11-11: when the launcher process missed the uninstall event, a package-only mark outlived the app and a
     * reinstall stayed hidden). {@code PackageInfo.firstInstallTime} is kept by an update in place and is new after an uninstall + reinstall.
     */
    public static String doneEntry(String pkg, long installStamp) { return pkg + "@" + installStamp; }

    /**
     * Someone asks whether {@code pkg} is shown. The first ask about THIS install of an auto-allowed package allows it once and writes the install down; anything else (another package,
     * an install time that could not be read, an install already handled) returns the very same {@code m}: nothing to write.
     */
    public static Marks asked(Marks m, String pkg, long installStamp) {
        if (pkg == null || installStamp <= 0 || !AUTO_ALLOWED_PACKAGES.contains(pkg)) return m;
        if (m.done.contains(doneEntry(pkg, installStamp))) return m;
        return new Marks(withShown(m.allowed, pkg, true), withDone(m.done, pkg, installStamp));
    }

    /** Tj decided by hand (Hidden apps screen). For an auto-allowed package the current install is written down too, so the rule never undoes his choice. */
    public static Marks handSet(Marks m, String pkg, boolean shown, long installStamp) {
        Set<String> done = m.done;
        if (pkg != null && installStamp > 0 && AUTO_ALLOWED_PACKAGES.contains(pkg)) done = withDone(m.done, pkg, installStamp);
        return new Marks(withShown(m.allowed, pkg, shown), done);
    }

    /** The package was uninstalled (Launcher3's removal event): forgotten completely, so a reinstall counts as a new app. */
    public static Marks removed(Marks m, String pkg) {
        return new Marks(withoutRemoved(m.allowed, pkg), withoutDone(m.done, pkg));
    }

    /** {@code done} with this install of {@code pkg} written down; older installs of the same package are dropped (one entry per package). */
    public static Set<String> withDone(Set<String> done, String pkg, long installStamp) {
        Set<String> s = withoutDone(done, pkg);
        if (pkg != null) s.add(doneEntry(pkg, installStamp));
        return s;
    }

    /** {@code done} without any entry of {@code pkg} (also a bare package name of an older format). */
    public static Set<String> withoutDone(Set<String> done, String pkg) {
        Set<String> s = new HashSet<>();
        if (done != null) for (String e : done) if (!isEntryOf(e, pkg)) s.add(e);
        return s;
    }

    private static boolean isEntryOf(String entry, String pkg) { return entry != null && pkg != null && (entry.equals(pkg) || entry.startsWith(pkg + "@")); }

    /** The custom home screen gets no icon of its own accord for these (review of BV1, L3): Tj asked for the DRAWER; the stock "add icon to home on install" must not put the music app on his home screen. */
    public static boolean noHomeIconOnInstall(String pkg) { return pkg != null && AUTO_ALLOWED_PACKAGES.contains(pkg); }
}
