package com.tj.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Tj (2026-11-02): "permanently only show solitaire in the recent apps menu". Every app the custom launcher starts gets FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS (the task never enters Recents)
 * except the target app (Solitaire). No automation, no flicker. Only affects launches made from the custom launcher; Quickstep mode is stock.
 */
public final class RecentsPolicy {
    public static final String KEY = "recents_only_target";   // default OFF since 2026-11-04 (D5: apps opened from the custom launcher stay in Recents normally)
    private RecentsPolicy() {}

    /** Pure rule (JVM-testable). */
    public static boolean shouldExclude(boolean enabled, String pkg, String targetPkg, String selfPkg) {
        if (!enabled || pkg == null || pkg.isEmpty()) return false;
        return !pkg.equals(targetPkg) && !pkg.equals(selfPkg);
    }

    public static boolean enabled(Context c) { return Features.get(c, KEY, false); }

    /** BY1a A4 (pure): does this launch need the target app at all? Without it {@link #shouldExclude} is false whatever the target is. */
    static boolean needsTarget(boolean enabled, String pkg, String selfPkg) {
        return enabled && pkg != null && !pkg.isEmpty() && !pkg.equals(selfPkg);
    }

    /** Called by the patched Launcher3 right before it starts an app. Never throws; on any problem the launch stays exactly as stock. */
    public static void apply(Context c, Intent intent) {
        try {
            ComponentName cn = intent.getComponent();
            String pkg = cn != null ? cn.getPackageName() : intent.getPackage();
            if (pkg != null && !pkg.equals(c.getPackageName())) AppOrigin.markCustom(c, pkg);   // D5: started from the custom launcher = the custom launcher's own app
            if (!needsTarget(enabled(c), pkg, c.getPackageName())) return;   // BY1a A4: switch off (the default): no target lookup (a package-manager call, sometimes a full label scan) on every app launch
            if (shouldExclude(true, pkg, ReturnReset.targetPackage(c), c.getPackageName())) {
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            }
        } catch (Throwable t) {
            Log.e("TjRecents", "apply failed", t);
        }
    }
}
