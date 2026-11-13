package com.tj.launcher;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.List;

/**
 * V2: stock keyboard while the custom launcher is on, Gboard while Quickstep is on (BX1, 2026-11-12; it was Microsoft SwiftKey before). Needs WRITE_SECURE_SETTINGS, which only adb can grant
 * (`bash tools/adb-grant.sh keyboard`); without it nothing changes and Launcher control says so. Default OFF. Snapshot first (RestoreStore), write, read back, undo on mismatch.
 * Never touches anything lock-screen related; only switches to a keyboard that is already enabled.
 */
public final class KeyboardSwitch {
    private static final String TAG = "TjKeyboard";
    public static final String KEY = "keyboard_switch";
    public static final String STATUS_KEY = "keyboard_status";
    private static final String DEFAULT_IME = Settings.Secure.DEFAULT_INPUT_METHOD;

    private KeyboardSwitch() {}

    public static boolean enabled(Context c) { return Features.get(c, KEY, false); }
    public static boolean permitted(Context c) { return c.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED; }
    public static String status(Context c) { return Features.getString(c, STATUS_KEY, "(not run yet)"); }

    private static void say(Context c, String s) { Features.setStringAsyncIfChanged(c, STATUS_KEY, s); Log.i(TAG, s); }   // BY1a C6/C7: a status line: async and only when it changed (it was a commit + fsync on the unlock path at every mode change)

    public static void apply(Context c, String mode) {
        try {
            if (!enabled(c)) return;
            if (!permitted(c)) { say(c, "needs the adb grant: adb shell pm grant com.tj.launcher android.permission.WRITE_SECURE_SETTINGS"); return; }
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<KeyboardPolicy.Ime> enabledImes = new ArrayList<>();
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                boolean system = i.getServiceInfo() != null && i.getServiceInfo().applicationInfo != null
                        && (i.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                enabledImes.add(new KeyboardPolicy.Ime(i.getId(), i.getPackageName(), system));
            }
            String current = Settings.Secure.getString(c.getContentResolver(), DEFAULT_IME);
            String original = c.getSharedPreferences("tj_kb", Context.MODE_PRIVATE).getString("original", null);
            if (original == null && current != null) {
                original = current;
                c.getSharedPreferences("tj_kb", Context.MODE_PRIVATE).edit().putString("original", original).commit();
                RestoreStore.snapshotSecure(c, DEFAULT_IME);   // "Restore everything" puts this back
            }
            String target = KeyboardPolicy.target(mode, enabledImes, original);
            if (target == null) {
                say(c, KeyboardPolicy.noTargetStatus(mode, enabledImes));   // BX1: names what IS enabled, so the line alone shows why Gboard was not found
                return;
            }
            if (target.equals(current)) { say(c, mode + ": already " + target); return; }
            Settings.Secure.putString(c.getContentResolver(), DEFAULT_IME, target);
            String now = Settings.Secure.getString(c.getContentResolver(), DEFAULT_IME);
            if (target.equals(now)) say(c, mode + ": keyboard -> " + target + " (verified)");
            else {
                Settings.Secure.putString(c.getContentResolver(), DEFAULT_IME, current);
                say(c, "write did not stick, put the old keyboard back");
            }
        } catch (Throwable t) {
            Log.e(TAG, "apply failed", t);
            try { say(c, "error: " + t.getClass().getSimpleName()); } catch (Throwable ignored) { }
        }
    }
}
