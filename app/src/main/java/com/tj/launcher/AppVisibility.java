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
     * app drawer"): Eleven, the LineageOS music app that ships beside the Launcher (module eleven/). They are allowed ONCE, automatically ({@link #autoAllowDue}); if Tj hides one
     * later it stays hidden, and a reinstall counts as new again ({@link #withoutDone}).
     */
    public static final Set<String> AUTO_ALLOWED_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("org.lineageos.eleven")));

    /** True when {@code pkg} is an auto-allowed package that has not been handled yet ({@code done} = the ones already allowed automatically once). */
    public static boolean autoAllowDue(String pkg, Set<String> done) {
        return pkg != null && AUTO_ALLOWED_PACKAGES.contains(pkg) && (done == null || !done.contains(pkg));
    }

    /** {@code done} with {@code pkg} added (the automatic allowing has happened, or Tj decided by hand: never repeat it). */
    public static Set<String> withDone(Set<String> done, String pkg) {
        Set<String> s = done == null ? new HashSet<String>() : new HashSet<>(done);
        if (pkg != null) s.add(pkg);
        return s;
    }

    /** {@code done} without {@code pkg}: the app was uninstalled, so a reinstall is auto-allowed again. */
    public static Set<String> withoutDone(Set<String> done, String pkg) {
        Set<String> s = done == null ? new HashSet<String>() : new HashSet<>(done);
        s.remove(pkg);
        return s;
    }
}
