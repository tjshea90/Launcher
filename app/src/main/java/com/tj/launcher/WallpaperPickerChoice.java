package com.tj.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.util.Log;

import java.util.List;
import java.util.Locale;

/**
 * AO1 (Tj, 2026-11-06: "when i hold down on the home screen and select wallpapers, make sure it always uses the default android wallpaper picker and doesn't offer another app").
 * The long-press entry started {@code ACTION_SET_WALLPAPER} with no component, so Android asked which app to use whenever more than one answers it (a gallery app answers it too).
 * Now the picker is chosen HERE, once, as ONE explicit component: the one the stock launcher is configured to use ({@code wallpaper_picker_package} in the stock launcher's own resources), else the system's
 * wallpaper app by name. A user-installed app is never used, and when nothing fits the entry does nothing (a message) instead of showing a chooser.
 * The pure rule is JVM-tested; the Android side only gathers the candidates.
 */
public final class WallpaperPickerChoice {
    private static final String TAG = "TjWallpaperPick";

    private WallpaperPickerChoice() {}

    /** Packages that answer SET_WALLPAPER but are galleries / cameras / file managers: "another app", never the wallpaper picker. */
    public static boolean isMediaApp(String pkg) {
        if (pkg == null) return true;
        String p = pkg.toLowerCase(Locale.ROOT);
        return p.contains("gallery") || p.contains("photo") || p.contains("camera") || p.contains("documentsui") || p.contains("filemanager") || p.contains("files") || p.contains("downloads");
    }

    /**
     * Pure: the index of the picker to use, or -1 (= none: do nothing).
     * 1. The package the STOCK launcher is configured to use ({@code hint}), whatever it is called (highest priority first).
     * 2. Otherwise only SYSTEM apps count (a user-installed app is never offered); gallery / camera / file apps are skipped; a package whose name contains "wallpaper" wins,
     *    highest intent-filter priority first, then the alphabetically first component (so the answer never depends on the order Android lists them in).
     * 3. Otherwise a single remaining system app; several unknown ones = none (guessing could open the wrong app).
     */
    public static int choose(String[] pkgs, String[] classes, boolean[] system, int[] priority, String hint, String ownPkg) {
        if (pkgs == null || classes == null || system == null || priority == null) return -1;
        int n = Math.min(Math.min(pkgs.length, classes.length), Math.min(system.length, priority.length));
        if (hint != null && !hint.trim().isEmpty()) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (pkgs[i] == null || !pkgs[i].equals(hint.trim()) || pkgs[i].equals(ownPkg)) continue;
                if (best < 0 || better(i, best, pkgs, classes, priority)) best = i;
            }
            if (best >= 0) return best;
        }
        int wp = -1, only = -1, others = 0;
        for (int i = 0; i < n; i++) {
            if (pkgs[i] == null || classes[i] == null || pkgs[i].equals(ownPkg) || !system[i] || isMediaApp(pkgs[i])) continue;
            others++; only = i;
            if (pkgs[i].toLowerCase(Locale.ROOT).contains("wallpaper") && (wp < 0 || better(i, wp, pkgs, classes, priority))) wp = i;
        }
        if (wp >= 0) return wp;
        return others == 1 ? only : -1;
    }

    private static boolean better(int a, int b, String[] pkgs, String[] classes, int[] priority) {
        if (priority[a] != priority[b]) return priority[a] > priority[b];
        return (pkgs[a] + "/" + classes[a]).compareTo(pkgs[b] + "/" + classes[b]) < 0;
    }

    /**
     * Pure (AO1b): the packages, in the order to look, for the wallpapers the STOCK picker shows on this tablet: AOSP's own picker app, the package the stock launcher is configured to use, the system apps that answer
     * SET_WALLPAPER (names with "wallpaper" first; galleries, cameras and file apps never), then the stock launcher itself. Never our own package.
     */
    public static java.util.List<String> wallpaperPackages(String[] handlerPkgs, boolean[] system, String hint, String stockPkg, String ownPkg) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        out.add("com.android.wallpaperpicker");
        if (hint != null && !hint.trim().isEmpty()) out.add(hint.trim());
        int n = handlerPkgs == null || system == null ? 0 : Math.min(handlerPkgs.length, system.length);
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                String p = handlerPkgs[i];
                if (p == null || !system[i] || isMediaApp(p)) continue;
                boolean wp = p.toLowerCase(Locale.ROOT).contains("wallpaper");
                if ((pass == 0) == wp) out.add(p);
            }
        }
        if (stockPkg != null && !stockPkg.isEmpty()) out.add(stockPkg);
        if (ownPkg != null) out.remove(ownPkg);
        return new java.util.ArrayList<>(out);
    }

    // ---------------------------------------------------------------- Android side

    /** What {@link #resolve} found, for the long-press entry and for the report. */
    public static final class Result {
        public final ComponentName component;   // null = nothing fits
        public final String why;                // "the stock launcher's setting" / "the system wallpaper app" / "the only system app" / "none"
        public final String candidates;         // every app that answers SET_WALLPAPER, system or not
        Result(ComponentName c, String w, String all) { component = c; why = w; candidates = all; }
    }

    /** The package the stock launcher is configured to use for its Wallpapers entry (its own {@code wallpaper_picker_package}); "" when none or unknown. Never throws. */
    static String stockHint(Context c) {
        try {
            ComponentName stock = StockLauncher.cached(c);   // A9 (CW1)
            if (stock == null) return "";
            Resources r = c.getPackageManager().getResourcesForApplication(stock.getPackageName());
            int id = r.getIdentifier("wallpaper_picker_package", "string", stock.getPackageName());
            if (id == 0) return "";
            String v = r.getString(id);
            return v == null ? "" : v.trim();
        } catch (Throwable t) { return ""; }
    }

    /** Resolves the picker. Cheap (one package-manager query). Never throws; a failure is "nothing fits". */
    public static Result resolve(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(new Intent(Intent.ACTION_SET_WALLPAPER), PackageManager.MATCH_DEFAULT_ONLY);
            int n = list == null ? 0 : list.size();
            String[] pk = new String[n], cl = new String[n]; boolean[] sys = new boolean[n]; int[] pr = new int[n];
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < n; i++) {
                ResolveInfo ri = list.get(i);
                pk[i] = ri.activityInfo.packageName; cl[i] = ri.activityInfo.name; pr[i] = ri.priority;
                sys[i] = (ri.activityInfo.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                if (all.length() > 0) all.append("; ");
                all.append(pk[i]).append('/').append(cl[i]).append(sys[i] ? " [system]" : " [user]").append(" prio ").append(pr[i]);
            }
            String hint = stockHint(c);
            int i = choose(pk, cl, sys, pr, hint, c.getPackageName());
            if (i < 0) return new Result(null, "none", all.length() == 0 ? "(nothing answers SET_WALLPAPER)" : all.toString());
            String why = !hint.isEmpty() && hint.equals(pk[i]) ? "the stock launcher's own setting (" + hint + ")" : pk[i].toLowerCase(Locale.ROOT).contains("wallpaper") ? "the system wallpaper app" : "the only system app";
            return new Result(new ComponentName(pk[i], cl[i]), why, all.toString());
        } catch (Throwable t) {
            Log.e(TAG, "could not resolve the wallpaper picker", t);
            return new Result(null, "error: " + t.getClass().getSimpleName(), "");
        }
    }

    /** One line for Launcher control and the tablet report. */
    public static String summary(Context c) {
        Result r = resolve(c);
        return (r.component == null ? "NONE (the long-press entry shows a message and opens nothing)" : r.component.flattenToShortString() + "  <- " + r.why) + "   [answering SET_WALLPAPER: " + r.candidates + "]";
    }

    /**
     * AO1b: where the wallpapers of the stock picker live on this device: (that app's ApplicationInfo, the resource id of its "wallpapers" string-array), or null when no installed app has a non-empty one.
     * AOSP's picker ships an EMPTY array: every device build fills it in with an overlay, so the real set can only be read from the installed app. Never throws.
     */
    public static android.util.Pair<ApplicationInfo, Integer> bundledWallpapers(Context c) {
        try {
            PackageManager pm = c.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(new Intent(Intent.ACTION_SET_WALLPAPER), PackageManager.MATCH_DEFAULT_ONLY);
            int n = list == null ? 0 : list.size();
            String[] pk = new String[n]; boolean[] sys = new boolean[n];
            for (int i = 0; i < n; i++) {
                pk[i] = list.get(i).activityInfo.packageName;
                sys[i] = (list.get(i).activityInfo.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            }
            ComponentName stock = StockLauncher.cached(c);   // A9 (CW1)
            for (String pkg : wallpaperPackages(pk, sys, stockHint(c), stock == null ? null : stock.getPackageName(), c.getPackageName())) {
                try {
                    Resources res = pm.getResourcesForApplication(pkg);
                    int id = res.getIdentifier("wallpapers", "array", pkg);
                    if (id != 0 && res.getStringArray(id).length > 0) return new android.util.Pair<>(pm.getApplicationInfo(pkg, 0), id);
                } catch (Throwable ignored) { /* not installed / no such array: next */ }
            }
        } catch (Throwable t) { Log.e(TAG, "could not find the stock wallpapers", t); }
        return null;
    }

    /** The picker package last started from the long-press entry (read by the helper that hides "My photos"). */
    private static volatile String lastPackage = "";
    public static String lastPackage() { return lastPackage; }

    /** Called by the long-press entry right before the picker starts. */
    public static void noteLaunched(ComponentName picker) { lastPackage = picker == null ? "" : picker.getPackageName(); }
}
