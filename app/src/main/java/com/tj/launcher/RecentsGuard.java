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
    public static void excludeOurTasks(Context c) {
        try {
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = am == null ? null : am.getAppTasks();
            if (tasks == null) return;
            for (ActivityManager.AppTask t : tasks) {
                if (isSettingsLookAlikeTask(t)) continue;   // BN1: like the real Settings, the look-alike is a normal card in Recents
                t.setExcludeFromRecents(true);
            }
        } catch (Throwable t) { Log.e(TAG, "exclude failed", t); }
    }

    /** BN1: true for the task whose root is the Settings look-alike (entered through the alias, or its page activity). Never throws. */
    static boolean isSettingsLookAlikeTask(ActivityManager.AppTask t) {
        try {
            ActivityManager.RecentTaskInfo info = t.getTaskInfo();
            android.content.ComponentName cn = info == null ? null : (info.baseIntent != null && info.baseIntent.getComponent() != null ? info.baseIntent.getComponent() : info.baseActivity);
            return cn != null && SettingsSpoof.isLookAlikeComponent(cn.getClassName());
        } catch (Throwable ignored) { return false; }
    }

    private static volatile String cachedLabel;
    private static volatile Bitmap cachedIcon;

    /**
     * Makes any Recents card of ours look like the target app (name + icon). Applied on EVERY resume: the description belongs to the activity instance, so a recreated Launcher
     * would otherwise go back to "Launcher" (v0.3.2 did it once per process: that was a bug). The Launcher is now a HOME-type task (never listed), this is the second line of defence.
     */
    public static void describeAsTarget(final Activity a) {
        try {
            if (cachedLabel != null && cachedIcon != null) { a.setTaskDescription(new ActivityManager.TaskDescription(cachedLabel, cachedIcon)); return; }
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
                } catch (Throwable t) { Log.e(TAG, "describe failed", t); }
            } }, 300);
        } catch (Throwable t) { Log.e(TAG, "describe failed", t); }
    }
}
