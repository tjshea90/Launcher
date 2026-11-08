package com.tj.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;

/**
 * BL1: the stored pictures of the HOME CURTAIN (the home-looking cover of the no-helper return reset; rules in {@link HomeCurtainRules}, design in {@code research/BL1_HOME_CURTAIN_2026-11-08.md}), the
 * counterpart of {@link HomeShot} (which keeps only a small CROP of the wallpaper for the power-menu patch).
 *
 * Two kinds of file in the app's private files: (1) the plain-WALLPAPER picture of the whole display, one per rotation and wallpaper offset step, kept by the home calibration instead of being thrown away
 * after the crop was cut (a real system screenshot taken with the Launcher's own views hidden, so it is exactly what the compositor shows there); (2) the composed CURTAIN, one per rotation: that wallpaper
 * picture with the Launcher's own window pixels laid over it (the same SRC_OVER the patch uses), i.e. what the home screen really looks like. Metadata lives in preferences.
 *
 * The newest curtain is also kept in MEMORY, as ONE immutable entry (rotation, signature, saved-at, bitmap): the cover is created at the ENTER tap, on the main thread, and must never wait for a disk read
 * there, and it can tell from the entry alone whether the picture fits the screen as it is now. A bitmap that was put in the cache may be on screen in a cover right now, so a replaced or dropped
 * bitmap is never recycled (the garbage collector frees it once the cover is gone). Everything here is best effort and never throws: a doubt means "no picture".
 */
public final class HomeCurtain {
    private static final String TAG = "TjHomeCurtain";
    /** Which wallpaper pictures exist: "r0x0,r0x333". */
    private static final String WALL_INDEX_KEY = "home_wall_idx";
    /** The last wallpaper picture that could NOT be kept ("signature#rotation#step"): the calibration is not repeated for the same page and wallpaper because of it (code review 2026-11-08: a full disk must not cost six flashes a day). */
    private static final String WALL_FAIL_KEY = "home_wall_fail";

    private HomeCurtain() {}

    private static File wallFile(Context c, int rot, int step) { return new File(c.getFilesDir(), HomeCurtainRules.wallFile(rot, step)); }
    private static File curtainFile(Context c, int rot) { return new File(c.getFilesDir(), HomeCurtainRules.curtainFile(rot)); }
    private static String entry(int rot, int step) { return "r" + rot + "x" + step; }

    /** Writes the PNG to a temp file and renames it: a failed or cut-short write must never become "the picture" (the same care as {@link HomeShot#save}). */
    private static boolean writePng(Bitmap b, File f) {
        File tmp = new File(f.getPath() + ".tmp");
        try {
            FileOutputStream o = new FileOutputStream(tmp);
            boolean written;
            try { written = b.compress(Bitmap.CompressFormat.PNG, 100, o); } finally { o.close(); }
            if (!written || tmp.length() <= 0) { tmp.delete(); Log.e(TAG, "the picture could not be written (disk full?)"); return false; }
            if (!tmp.renameTo(f)) { tmp.delete(); return false; }
            return true;
        } catch (Throwable t) { tmp.delete(); Log.e(TAG, "writing the picture failed", t); return false; }
    }

    // ================================================================ the plain-wallpaper pictures

    /** Keeps the whole calibration screenshot (the plain wallpaper, display size) for this rotation and step. Worker thread (a PNG write). Then forgets the pictures of an older wallpaper. */
    public static boolean saveWall(Context c, int rot, int step, Bitmap full, String sig, long wallpaperId, int dispW, int dispH) {
        try {
            if (full == null || full.isRecycled() || full.getWidth() != dispW || full.getHeight() != dispH || !writePng(full, wallFile(c, rot, step))) { markWallFailed(c, rot, step, sig); return false; }
            Features.setString(c, HomeCurtainRules.wallMeta(rot, step), sig);
            addToWallIndex(c, rot, step);
            pruneWalls(c, wallpaperId, dispW, dispH);
            if (wallFailed(c, rot, step, sig)) Features.remove(c, WALL_FAIL_KEY);
            return true;
        } catch (Throwable t) { Log.e(TAG, "saving the wallpaper picture failed", t); markWallFailed(c, rot, step, sig); return false; }
    }

    private static void markWallFailed(Context c, int rot, int step, String sig) {
        try { Features.setString(c, WALL_FAIL_KEY, sig + "#" + rot + "#" + step); } catch (Throwable ignored) { }
    }

    /** Did the calibration for this very page and wallpaper already fail to keep its wallpaper picture? Then it is not run again just for the curtain (the black cover stays). */
    public static boolean wallFailed(Context c, int rot, int step, String sig) {
        try {
            String[] p = Features.getString(c, WALL_FAIL_KEY, "").split("#");
            return p.length == 3 && sig != null && p[0].equals(sig) && Integer.parseInt(p[1]) == rot && HomePatchRules.sameStep(Integer.parseInt(p[2]), step);
        } catch (Throwable t) { return false; }
    }

    /** The stored step that serves {@code step} (the same wallpaper position, +-2) for this signature, or -1. A cheap check: no decoding. */
    public static int findWallStep(Context c, int rot, int step, String signature) {
        try {
            if (signature == null || signature.isEmpty()) return -1;
            for (int d = 0; d <= 2; d++) {
                for (int sign = -1; sign <= 1; sign += 2) {
                    int s = step + sign * d;
                    if (s < 0 || (d == 0 && sign == 1)) continue;
                    if (signature.equals(Features.getString(c, HomeCurtainRules.wallMeta(rot, s), "")) && wallFile(c, rot, s).isFile()) return s;
                }
            }
        } catch (Throwable t) { Log.e(TAG, "looking for the wallpaper picture failed", t); }
        return -1;
    }

    public static boolean hasWall(Context c, int rot, int step, String signature) { return findWallStep(c, rot, step, signature) >= 0; }

    /** The wallpaper picture that serves {@code step}, decoded MUTABLE (the Launcher's layer is drawn straight onto it: one full-screen bitmap fewer on a low-RAM tablet), a worker thread, or null. */
    public static Bitmap loadWall(Context c, int rot, int step, String signature) {
        try {
            int s = findWallStep(c, rot, step, signature);
            if (s < 0) return null;
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inMutable = true;
            Bitmap b = BitmapFactory.decodeFile(wallFile(c, rot, s).getPath(), o);
            if (b != null && !b.isMutable()) { Bitmap m = b.copy(Bitmap.Config.ARGB_8888, true); b.recycle(); b = m; }
            return b;
        } catch (Throwable t) { Log.e(TAG, "loading the wallpaper picture failed", t); return null; }
    }

    /** Deletes the pictures whose signature is not this wallpaper and display size any more. */
    private static void pruneWalls(Context c, long wallpaperId, int dispW, int dispH) {
        try {
            StringBuilder keep = new StringBuilder();
            for (String e : wallIndex(c)) {
                int x = e.indexOf('x');
                if (x < 2) continue;
                int rot = Integer.parseInt(e.substring(1, x)), step = Integer.parseInt(e.substring(x + 1));
                String sig = Features.getString(c, HomeCurtainRules.wallMeta(rot, step), "");
                if (!sig.isEmpty() && HomePatchRules.sigMatches(sig, wallpaperId, dispW, dispH)) { if (keep.length() > 0) keep.append(','); keep.append(e); }
                else { wallFile(c, rot, step).delete(); Features.remove(c, HomeCurtainRules.wallMeta(rot, step)); }
            }
            Features.setString(c, WALL_INDEX_KEY, keep.toString());
        } catch (Throwable t) { Log.e(TAG, "pruning the wallpaper pictures failed", t); }
    }

    private static java.util.List<String> wallIndex(Context c) {
        java.util.List<String> out = new java.util.ArrayList<>();
        String v = Features.getString(c, WALL_INDEX_KEY, "");
        if (v.isEmpty()) return out;
        for (String s : v.split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    private static void addToWallIndex(Context c, int rot, int step) {
        java.util.List<String> idx = wallIndex(c);
        String e = entry(rot, step);
        if (!idx.contains(e)) idx.add(e);
        StringBuilder b = new StringBuilder();
        for (String s : idx) { if (b.length() > 0) b.append(','); b.append(s); }
        Features.setString(c, WALL_INDEX_KEY, b.toString());
    }

    /** How many wallpaper pictures are stored (Launcher control and the tablet report). */
    public static int wallCount(Context c) { return wallIndex(c).size(); }

    // ================================================================ the composed curtain

    /** Stores the composed curtain for this rotation with its meta. Worker thread (a PNG write). */
    public static boolean saveCurtain(Context c, int rot, Bitmap composed, String sig, int step, int layerHash, long nowMs) {
        try {
            if (composed == null || composed.isRecycled()) return false;
            if (!writePng(composed, curtainFile(c, rot))) return false;
            Features.setString(c, HomeCurtainRules.curtainMeta(rot), HomeCurtainRules.metaString(sig, step, layerHash, nowMs));
            return true;
        } catch (Throwable t) { Log.e(TAG, "saving the curtain failed", t); return false; }
    }

    /** The stored curtain's meta for this rotation, or null (none, a malformed meta, or the file is gone). A cheap check: no decoding. NOT for the Enter-tap path (it stats the file). */
    public static HomeCurtainRules.Meta meta(Context c, int rot) {
        try {
            HomeCurtainRules.Meta m = HomeCurtainRules.parseMeta(Features.getString(c, HomeCurtainRules.curtainMeta(rot), ""));
            return m != null && curtainFile(c, rot).isFile() ? m : null;
        } catch (Throwable t) { return null; }
    }

    /**
     * A capture found the home UNCHANGED: the stored picture is confirmed, so its time stamp is renewed (the meta only, no picture is written) and the memory copy is re-keyed to it. Without this a home that is
     * never rearranged would expire after 30 days. Returns the new stamp, or 0.
     */
    public static long refreshStamp(Context c, int rot) {
        try {
            HomeCurtainRules.Meta m = meta(c, rot);
            if (m == null) return 0;
            long now = System.currentTimeMillis();
            Features.setString(c, HomeCurtainRules.curtainMeta(rot), HomeCurtainRules.metaString(m.sig, m.step, m.hash, now));
            Entry e = cache;
            if (e != null && e.rot == rot && e.sig.equals(m.sig) && !e.bmp.isRecycled()) cache = new Entry(rot, m.sig, now, e.bmp);
            return now;
        } catch (Throwable t) { Log.e(TAG, "renewing the stamp failed", t); return 0; }
    }

    // ================================================================ the in-memory copy (the Enter tap never reads the disk)

    /** The newest curtain in memory: ONE immutable entry, so rotation, signature, stamp and bitmap are always read together. */
    private static final class Entry {
        final int rot; final String sig; final long savedAt; final Bitmap bmp;
        Entry(int rot, String sig, long savedAt, Bitmap bmp) { this.rot = rot; this.sig = sig; this.savedAt = savedAt; this.bmp = bmp; }
    }

    private static volatile Entry cache;

    /** The in-memory curtain for exactly this rotation and signature, or null. NO disk access: this runs on the main thread at the Enter tap. */
    public static Bitmap cachedFor(int rot, String sig) {
        try {
            Entry e = cache;
            return e != null && sig != null && e.rot == rot && e.sig.equals(sig) && !e.bmp.isRecycled() ? e.bmp : null;
        } catch (Throwable t) { return null; }
    }

    /** The rotation of the curtain that is in memory, or -1: the rules compare it with the screen as it is now (a real check, not a copy of the current rotation). */
    public static int cachedRot() { Entry e = cache; return e == null || e.bmp.isRecycled() ? -1 : e.rot; }

    /** The signature of the curtain in memory, or "". */
    public static String cachedSig() { Entry e = cache; return e == null || e.bmp.isRecycled() ? "" : e.sig; }

    /** When the curtain in memory was captured (the stamp), or 0. */
    public static long cachedSavedAt() { Entry e = cache; return e == null ? 0 : e.savedAt; }

    /** Puts a curtain in the memory cache. The replaced bitmap is NOT recycled (a cover may be showing it right now). */
    public static void setCache(int rot, String sig, long savedAtMs, Bitmap b) {
        cache = b == null || sig == null ? null : new Entry(rot, sig, savedAtMs, b);
    }

    public static void dropCache() { cache = null; }

    /** Reads the stored curtain into the memory cache if it is for this signature and exactly the display size. Worker thread (a PNG decode). True = it is cached now. */
    public static boolean preload(Context c, int rot, String sig, int dispW, int dispH) {
        try {
            HomeCurtainRules.Meta m = meta(c, rot);
            if (m == null || sig == null || !m.sig.equals(sig)) return false;
            Entry e = cache;
            if (e != null && e.rot == rot && e.sig.equals(sig) && e.savedAt == m.savedAtMs && !e.bmp.isRecycled()) return true;   // already there
            Bitmap b = BitmapFactory.decodeFile(curtainFile(c, rot).getPath());
            if (b == null) return false;
            if (b.getWidth() != dispW || b.getHeight() != dispH) { b.recycle(); return false; }
            setCache(rot, m.sig, m.savedAtMs, b);
            return true;
        } catch (Throwable t) { Log.e(TAG, "loading the curtain failed", t); return false; }
    }

    /** Forgets every stored picture (the wallpaper pictures, the curtains, the memory copy, the failure marker): "Calibrate the home patch again" and a changed wallpaper start over. */
    public static void invalidateAll(Context c) {
        try {
            for (String e : wallIndex(c)) {
                int x = e.indexOf('x');
                if (x < 2) continue;
                int rot = Integer.parseInt(e.substring(1, x)), step = Integer.parseInt(e.substring(x + 1));
                wallFile(c, rot, step).delete();
                Features.remove(c, HomeCurtainRules.wallMeta(rot, step));
            }
            Features.remove(c, WALL_INDEX_KEY);
            Features.remove(c, WALL_FAIL_KEY);
            for (int rot = 0; rot < 4; rot++) { curtainFile(c, rot).delete(); Features.remove(c, HomeCurtainRules.curtainMeta(rot)); }
        } catch (Throwable t) { Log.e(TAG, "forgetting the pictures failed", t); }
        dropCache();
    }
}
