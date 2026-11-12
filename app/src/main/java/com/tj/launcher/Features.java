package com.tj.launcher;

import android.content.Context;
import android.content.SharedPreferences;

/** The launcher's own feature switches. Everything that is risky or needs a permission defaults to OFF. */
public final class Features {
    private static final String PREFS = "tj_features";

    /** Power menu: OFF | HIDE_SCREENSHOT | BLOCK (close the whole menu). Default OFF. */
    public static final String POWER_MODE = "power_mode";
    public static final String POWER_OFF = "off", POWER_HIDE_SCREENSHOT = "hide_screenshot", POWER_BLOCK = "block", POWER_WALLPAPER = "wallpaper";

    /** Modes that put our replacement card over the system card (W3b adds the wallpaper patch under it). */
    public static boolean isCover(String m) { return POWER_HIDE_SCREENSHOT.equals(m) || POWER_WALLPAPER.equals(m); }

    private Features() {}

    // AB2: one handle per process. Context.getSharedPreferences takes a global lock and a map lookup on every call, and the accessibility helper reads these settings several times per event.
    private static volatile SharedPreferences sp;
    private static SharedPreferences p(Context c) {
        SharedPreferences s = sp;
        if (s == null) { s = c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); sp = s; }
        return s;
    }

    public static boolean get(Context c, String key, boolean def) { return p(c).getBoolean(key, def); }
    public static void set(Context c, String key, boolean on) { p(c).edit().putBoolean(key, on).commit(); }
    public static String getString(Context c, String key, String def) { return p(c).getString(key, def); }
    public static void setString(Context c, String key, String v) { p(c).edit().putString(key, v).commit(); }
    /** For diagnostics only: the in-memory value is updated at once, the disk write happens later on SharedPreferences' own thread (never fsync on the UI thread inside a time-critical path). */
    public static void setStringAsync(Context c, String key, String v) { p(c).edit().putString(key, v).apply(); }
    /** BY1a C6/C7 (pure): would storing {@code v} change the stored value {@code stored} (null = nothing stored)? */
    static boolean differs(String stored, String v) { return stored == null ? v != null : !stored.equals(v); }
    /** BY1a C6/C7: a status / diagnostic string, written only when it changed and like {@link #setStringAsync} (no whole-file write + fsync on the caller's thread for a repeated line). */
    public static void setStringAsyncIfChanged(Context c, String key, String v) { SharedPreferences s = p(c); if (differs(s.getString(key, null), v)) s.edit().putString(key, v).apply(); }
    /** BY1a C6/C7: like {@link #setString} (commit) but only when the value changed: for bookkeeping a later decision reads back. */
    public static void setStringIfChanged(Context c, String key, String v) { SharedPreferences s = p(c); if (differs(s.getString(key, null), v)) s.edit().putString(key, v).commit(); }
    /** Like {@link #set} but the disk write happens later on SharedPreferences' own thread (BA2: bookkeeping on the unlock path must not fsync on the UI thread). The in-memory value is visible at once. */
    public static void setAsync(Context c, String key, boolean on) { p(c).edit().putBoolean(key, on).apply(); }
    public static int getInt(Context c, String key, int def) { return p(c).getInt(key, def); }
    public static void setInt(Context c, String key, int v) { p(c).edit().putInt(key, v).commit(); }
    public static void allOff(Context c) { p(c).edit().clear().commit(); }
    /** Removes a stored value (the code default applies again). */
    public static void remove(Context c, String key) { p(c).edit().remove(key).commit(); }
    /** Every stored preference. Only {@link ReportState} reads this, and only through its allow-list. */
    static java.util.Map<String, ?> all(Context c) { return p(c).getAll(); }   // package-private on purpose: only ReportState may read every preference (code review, 2026-11-05)

    public static String powerMode(Context c) {
        String m = getString(c, POWER_MODE, POWER_OFF);
        return POWER_HIDE_SCREENSHOT.equals(m) || POWER_BLOCK.equals(m) || POWER_WALLPAPER.equals(m) ? m : POWER_OFF;
    }
}
