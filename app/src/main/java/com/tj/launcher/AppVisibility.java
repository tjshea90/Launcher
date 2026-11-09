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
}
