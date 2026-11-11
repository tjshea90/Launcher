package com.tj.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import java.util.HashSet;
import java.util.Set;

/** Storage + Android glue for {@link AppVisibility}. Plain app preferences (no secure settings). Every method is exception-safe: on any failure apps stay SHOWN (never hide by accident). */
public final class HiddenApps {
    private static final String PREFS = "tj_apps";
    private static final String ALLOWED = "allowed";
    private static final String BASELINE = "baseline_done";
    /** One entry "pkg@firstInstallTime" per install of a package of {@link AppVisibility#AUTO_ALLOWED_PACKAGES} that was already handled (BV1): allowed once by the rule, or decided by Tj, so a later hide is never undone. */
    private static final String AUTO_DONE = "auto_done";
    private static final Object LOCK = new Object();

    private HiddenApps() {}

    private static SharedPreferences p(Context c) { return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static Set<String> installedLaunchablePackages(Context c) {
        Set<String> s = new HashSet<>();
        for (ResolveInfo ri : c.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)) {
            s.add(ri.activityInfo.packageName);
        }
        return s;
    }

    private static Set<String> allowed(Context c) {
        SharedPreferences sp = p(c);
        if (!sp.getBoolean(BASELINE, false)) {
            Set<String> base = AppVisibility.baseline(installedLaunchablePackages(c), c.getPackageName());
            sp.edit().putStringSet(ALLOWED, base).putBoolean(BASELINE, true).commit();
            return base;
        }
        return new HashSet<>(sp.getStringSet(ALLOWED, new HashSet<String>()));
    }

    private static Set<String> done(Context c) { return new HashSet<>(p(c).getStringSet(AUTO_DONE, new HashSet<String>())); }

    /** The time this install of {@code pkg} began ({@code PackageInfo.firstInstallTime}: kept by an update in place, new after an uninstall + reinstall); 0 when it cannot be read, e.g. the app is not installed. */
    private static long installStamp(Context c, String pkg) {
        try { return c.getPackageManager().getPackageInfo(pkg, 0).firstInstallTime; } catch (Throwable t) { return 0; }
    }

    /**
     * BV1: a package of {@link AppVisibility#AUTO_ALLOWED_PACKAGES} is allowed the first time anyone asks about THIS install of it, once; asking means it is installed. Level-triggered on
     * purpose (no install broadcast to miss, nothing to run at boot) and keyed to the install time, so an uninstall whose event this process missed cannot leave a stale mark. Call inside LOCK.
     * Every other package ends on the first line (one set lookup). The write is {@code apply()}: memory at once, no disk wait on the main thread, and the rule is idempotent if a kill loses it.
     */
    private static void autoAllowIfDue(Context c, String pkg) {
        if (pkg == null || !AppVisibility.AUTO_ALLOWED_PACKAGES.contains(pkg)) return;
        long stamp = installStamp(c, pkg);
        if (stamp <= 0) return;   // not installed (a late ask after an uninstall) or unreadable: write nothing
        AppVisibility.Marks now = new AppVisibility.Marks(allowed(c), done(c));
        AppVisibility.Marks next = AppVisibility.asked(now, pkg, stamp);
        if (next != now) p(c).edit().putStringSet(ALLOWED, next.allowed).putStringSet(AUTO_DONE, next.done).apply();
    }

    public static boolean isShown(Context c, ComponentName app) {
        try {
            if (SettingsSpoof.hidesRealSettings(c, app.getPackageName())) return false;   // BN1: the look-alike is the Settings entry of the custom drawer; the real one stays out while it is
            synchronized (LOCK) { autoAllowIfDue(c, app.getPackageName()); return AppVisibility.isShown(allowed(c), app.getPackageName(), app.getClassName(), c.getPackageName()); }
        } catch (Throwable t) {
            android.util.Log.e("TjHidden", "isShown failed, showing the app", t);
            return true;
        }
    }

    public static boolean isPackageShown(Context c, String pkg) {
        try {
            if (SettingsSpoof.hidesRealSettings(c, pkg)) return false;   // BN1: same rule as isShown (the Recents sweep asks by package)
            synchronized (LOCK) { autoAllowIfDue(c, pkg); return AppVisibility.isShown(allowed(c), pkg, null, c.getPackageName()); }
        } catch (Throwable t) { return true; }
    }

    public static void setShown(Context c, String pkg, boolean shown) {
        synchronized (LOCK) {
            AppVisibility.Marks now = new AppVisibility.Marks(allowed(c), done(c));
            AppVisibility.Marks next = AppVisibility.handSet(now, pkg, shown, AppVisibility.AUTO_ALLOWED_PACKAGES.contains(pkg) ? installStamp(c, pkg) : 0);   // Tj decided by hand: the automatic allowing must never override it
            SharedPreferences.Editor e = p(c).edit().putStringSet(ALLOWED, next.allowed);
            if (next.done != now.done) e.putStringSet(AUTO_DONE, next.done);
            e.commit();
        }
    }

    public static void onRemoved(Context c, String pkg) {
        try {
            synchronized (LOCK) {
                AppVisibility.Marks next = AppVisibility.removed(new AppVisibility.Marks(allowed(c), done(c)), pkg);
                SharedPreferences.Editor e = p(c).edit().putStringSet(ALLOWED, next.allowed);
                if (AppVisibility.AUTO_ALLOWED_PACKAGES.contains(pkg)) e.putStringSet(AUTO_DONE, next.done);   // a reinstall of the music app is auto-allowed again
                e.commit();
            }
        } catch (Throwable ignored) { }
    }

    /** Ask Launcher3 to rebuild its app list so a change shows up at once. */
    public static void reloadLauncher(Context c) {
        try { com.android.launcher3.LauncherAppState.getInstance(c).getModel().forceReload(); }
        catch (Throwable t) { android.util.Log.e("TjHidden", "reload failed", t); }
    }
}
