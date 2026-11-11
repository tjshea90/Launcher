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
    /** Packages of {@link AppVisibility#AUTO_ALLOWED_PACKAGES} that were already allowed automatically once (BV1), so a later hide by Tj is never undone. */
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

    /**
     * BV1: a package of {@link AppVisibility#AUTO_ALLOWED_PACKAGES} (the music app) is allowed the first time anyone asks about it, once; asking means it is installed. Level-triggered on
     * purpose (no install broadcast to miss, nothing to run at boot). Call inside LOCK. The first test ends almost every call at once; the one commit() happens once per install.
     */
    private static void autoAllowIfDue(Context c, String pkg) {
        if (pkg == null || !AppVisibility.AUTO_ALLOWED_PACKAGES.contains(pkg)) return;
        SharedPreferences sp = p(c);
        Set<String> done = new HashSet<>(sp.getStringSet(AUTO_DONE, new HashSet<String>()));
        if (!AppVisibility.autoAllowDue(pkg, done)) return;
        Set<String> a = AppVisibility.withShown(allowed(c), pkg, true);
        sp.edit().putStringSet(ALLOWED, a).putStringSet(AUTO_DONE, AppVisibility.withDone(done, pkg)).commit();
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
            synchronized (LOCK) { return AppVisibility.isShown(allowed(c), pkg, null, c.getPackageName()); }
        } catch (Throwable t) { return true; }
    }

    public static void setShown(Context c, String pkg, boolean shown) {
        synchronized (LOCK) {
            Set<String> a = AppVisibility.withShown(allowed(c), pkg, shown);
            p(c).edit().putStringSet(ALLOWED, a).commit();
        }
    }

    public static void onRemoved(Context c, String pkg) {
        try {
            synchronized (LOCK) {
                Set<String> a = AppVisibility.withoutRemoved(allowed(c), pkg);
                p(c).edit().putStringSet(ALLOWED, a).commit();
            }
        } catch (Throwable ignored) { }
    }

    /** Ask Launcher3 to rebuild its app list so a change shows up at once. */
    public static void reloadLauncher(Context c) {
        try { com.android.launcher3.LauncherAppState.getInstance(c).getModel().forceReload(); }
        catch (Throwable t) { android.util.Log.e("TjHidden", "reload failed", t); }
    }
}
