package com.tj.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import java.util.List;

/**
 * W2 (Tj, 2026-11-03): "Launcher still shows in the recent apps menu. I want it to only show solitaire."
 * Manifest and launch flags only apply when a task is CREATED, so a task that already existed (made by an earlier version, or by Android itself) kept showing.
 * This sets the flag on our tasks at runtime (AppTask.setExcludeFromRecents works on an existing task), every time one of our screens is resumed.
 * Hedge (Tj's own fallback): should a card ever still appear, it carries the target app's name and icon instead of "Launcher".
 */
public final class RecentsGuard {
    private static final String TAG = "TjRecentsGuard";

    private RecentsGuard() {}

    /** Marks every task of this app as excluded from Recents. Cheap, never throws. */
    public static void excludeOurTasks(Context c) { excludeOurTasks(c, false); }

    /**
     * BY1a A2/C13: {@code onlyNew} skips the exclude call for a task id this process already flagged (see {@link FlaggedIds}). Returns false when the pass failed, so the caller tries again later. Never throws.
     */
    static boolean excludeOurTasks(Context c, boolean onlyNew) {
        try {
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = am == null ? null : am.getAppTasks();
            if (tasks == null) return false;
            for (ActivityManager.AppTask t : tasks) {
                ActivityManager.RecentTaskInfo info = taskInfo(t);
                if (isSettingsLookAlike(info)) continue;   // BN1: like the real Settings, the look-alike is a normal card in Recents
                int id = info == null ? -1 : info.taskId;
                if (!FLAGGED.needsFlag(id, onlyNew)) continue;
                t.setExcludeFromRecents(true);
                FLAGGED.flagged(id);
            }
            return true;
        } catch (Throwable t) { Log.e(TAG, "exclude failed", t); return false; }
    }

    private static ActivityManager.RecentTaskInfo taskInfo(ActivityManager.AppTask t) {
        try { return t.getTaskInfo(); } catch (Throwable ignored) { return null; }
    }

    /** BN1: true for the task whose root is the Settings look-alike (entered through the alias, or its page activity). Never throws. */
    private static boolean isSettingsLookAlike(ActivityManager.RecentTaskInfo info) {
        try {
            android.content.ComponentName cn = info == null ? null : (info.baseIntent != null && info.baseIntent.getComponent() != null ? info.baseIntent.getComponent() : info.baseActivity);
            return cn != null && SettingsSpoof.isLookAlikeComponent(cn.getClassName());
        } catch (Throwable ignored) { return false; }
    }

    /**
     * BY1a A2/C13 (pure, JVM-tested): the task ids this process already flagged. The flag sits on the task's base intent for the life of the task and a new task gets a new id, so a known id needs no
     * second call. An unknown id (no task info) is always flagged. Bounded: past {@link #MAX} ids it starts over (every task is flagged once more, harmless).
     */
    static final class FlaggedIds {
        static final int MAX = 64;
        private final java.util.Set<Integer> ids = new java.util.HashSet<>();
        synchronized boolean needsFlag(int id, boolean onlyNew) { return !onlyNew || id < 0 || !ids.contains(id); }
        synchronized void flagged(int id) { if (id < 0) return; if (ids.size() >= MAX && !ids.contains(id)) ids.clear(); ids.add(id); }
        synchronized int size() { return ids.size(); }
    }
    private static final FlaggedIds FLAGGED = new FlaggedIds();

    /** BY1a A2/C13 (pure): is {@code o} the instance {@code ref} points to? */
    static boolean sameInstance(java.lang.ref.WeakReference<?> ref, Object o) { return o != null && ref != null && ref.get() == o; }

    private static volatile String cachedLabel;
    private static volatile Bitmap cachedIcon;
    /** BY1a A2: the activity instance that got the description last (main thread). */
    private static volatile java.lang.ref.WeakReference<Activity> describedFor;

    /**
     * Makes any Recents card of ours look like the target app (name + icon). The description belongs to the activity instance, so a recreated Launcher would otherwise go back to "Launcher" (v0.3.2 did
     * it once per process: that was a bug). BY1a A2: once per instance (it was sent again at EVERY resume: a new TaskDescription, a bitmap scale and a binder call); nothing else in the app sets one, and
     * an instance without it (no target app found yet) keeps asking at each resume as before. The Launcher is now a HOME-type task (never listed), this is the second line of defence.
     */
    public static void describeAsTarget(final Activity a) {
        try {
            if (sameInstance(describedFor, a)) return;
            if (cachedLabel != null && cachedIcon != null) { a.setTaskDescription(new ActivityManager.TaskDescription(cachedLabel, cachedIcon)); describedFor = new java.lang.ref.WeakReference<>(a); return; }
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() { @Override public void run() {
                try {
                    String pkg = ReturnReset.targetPackage(a);
                    if (pkg == null) return;
                    PackageManager pm = a.getPackageManager();
                    CharSequence label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                    Drawable d = pm.getApplicationIcon(pkg);
                    int px = (int) (48 * a.getResources().getDisplayMetrics().density);
                    Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
                    Canvas cv = new Canvas(bmp);
                    d.setBounds(0, 0, px, px);
                    d.draw(cv);
                    cachedLabel = String.valueOf(label); cachedIcon = bmp;
                    a.setTaskDescription(new ActivityManager.TaskDescription(cachedLabel, cachedIcon));
                    describedFor = new java.lang.ref.WeakReference<>(a);
                } catch (Throwable t) { Log.e(TAG, "describe failed", t); }
            } }, 300);
        } catch (Throwable t) { Log.e(TAG, "describe failed", t); }
    }
}
