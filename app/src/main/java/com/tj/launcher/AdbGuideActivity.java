package com.tj.launcher;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * C2 (Tj, 2026-11-04): "in the app tell me a step by step guide of what adb commands to enter". Every adb line (same list as research/ADB_COMMANDS.txt), a tick for what is
 * already granted, a Copy button per step and one for everything. Android 10 has no way for an app to give itself these permissions, so a computer + USB cable is needed once.
 * Only opens in Quickstep mode (like the rest of Launcher control).
 */
public class AdbGuideActivity extends Activity {
    private static final String PKG = "com.tj.launcher";
    private LinearLayout list;

    private static final class Step {
        final String title, why; final String[] cmds;
        Step(String title, String why, String... cmds) { this.title = title; this.why = why; this.cmds = cmds; }
    }

    private final Step[] steps = {
        new Step("1. One time, on the tablet", "Settings > About tablet > tap Build number 7 times; Settings > System > Developer options > USB debugging ON; plug in USB and tap Allow on the tablet.", "adb devices"),
        new Step("2. The important one", "Keyboard switch, animations off during the double-Home reset, later: different quick-settings tiles per launcher.", "adb shell pm grant " + PKG + " android.permission.WRITE_SECURE_SETTINGS"),
        new Step("3. Wallpaper patch for the power menu", "Same as the 'Allow wallpaper access' button.", "adb shell pm grant " + PKG + " android.permission.READ_EXTERNAL_STORAGE"),
        new Step("4. Keep the helper alive on this tablet", "Stops Android from freezing the accessibility helper.", "adb shell dumpsys deviceidle whitelist +" + PKG, "adb shell cmd appops set " + PKG + " RUN_IN_BACKGROUND allow", "adb shell cmd appops set " + PKG + " RUN_ANY_IN_BACKGROUND allow"),
        new Step("5. Later features (read-only / settings)", "DUMP reads what is running; the others are for features added later.", "adb shell pm grant " + PKG + " android.permission.DUMP", "adb shell pm grant " + PKG + " android.permission.CHANGE_CONFIGURATION", "adb shell pm grant " + PKG + " android.permission.PACKAGE_USAGE_STATS", "adb shell cmd appops set " + PKG + " GET_USAGE_STATS allow", "adb shell cmd appops set " + PKG + " WRITE_SETTINGS allow"),
        new Step("6. If Android shows 'Choose a Home app'", "Happens after a launcher crash. Puts the launcher back as the default Home app.", "adb shell cmd package set-home-activity " + PKG + "/com.tj.launcher.RouterActivity"),
        new Step("7. Check what is granted", "Each granted line shows granted=true.", "adb shell dumpsys package " + PKG + " | grep -E \"WRITE_SECURE_SETTINGS|READ_EXTERNAL_STORAGE|DUMP|CHANGE_CONFIGURATION|PACKAGE_USAGE_STATS\""),
        new Step("8. Undo (any time)", "Takes a permission away again.", "adb shell pm revoke " + PKG + " android.permission.WRITE_SECURE_SETTINGS", "adb shell pm revoke " + PKG + " android.permission.READ_EXTERNAL_STORAGE", "adb shell pm revoke " + PKG + " android.permission.DUMP", "adb shell pm revoke " + PKG + " android.permission.CHANGE_CONFIGURATION", "adb shell pm revoke " + PKG + " android.permission.PACKAGE_USAGE_STATS"),
        new Step("10. Invisible clean-up helper (again after every tablet restart)", "Starts a tiny helper as the shell user so the return to the custom launcher force-stops Quickstep's apps with no black cover. Mac/Linux: the one line. Windows: the two steps (put the path from step 1 into step 2). Stop it with the last line.", ShellProtocol.START_ONE_LINER, ShellProtocol.START_STEP1, ShellProtocol.START_STEP2, ShellProtocol.STOP_LINE),
        new Step("11. Check the user profiles", "Decides whether a real second Android user is possible (N = 1 means no). Send the output to Claude.", "adb shell pm get-max-users", "adb shell pm list users", "adb shell settings get global user_switcher_enabled"),
        new Step("12. Read-only probe of the tablet", "Changes nothing. Facts for the power menu, restarts and the Settings clone (full script: tools/adb-probe.sh in the repo, or research/ADB_COMMANDS.txt section 12).", "adb shell getprop ro.build.fingerprint", "adb shell getprop ro.crypto.state", "adb shell getprop ro.crypto.type", "adb shell getprop service.adb.tcp.port", "adb shell getprop persist.adb.tcp.port", "adb shell settings get secure power_menu_actions", "adb shell pm path com.android.settings", "adb shell cmd overlay list"),
        new Step("13. Keyboards", "Both the stock keyboard and SwiftKey must be listed, or the keyboard switch has nothing to switch to.", "adb shell ime list -s"),
        new Step("9. Optional: the old Browser app", "Disables com.android.browser (reversible with: adb shell pm enable com.android.browser).", "adb shell pm disable-user --user 0 com.android.browser"),
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Mode.isCustom(this)) { finish(); return; }
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(40, 80, 40, 160);
        sv.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(sv);
    }

    @Override protected void onResume() { super.onResume(); RecentsGuard.excludeOurTasks(this); RecentsGuard.describeAsTarget(this); if (Mode.isCustom(this)) { finish(); return; } build(); }

    private boolean granted(String perm) { return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED; }

    private boolean appOp(String op) {
        try { AppOpsManager a = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE); return a.checkOpNoThrow(op, android.os.Process.myUid(), PKG) == AppOpsManager.MODE_ALLOWED; }
        catch (Throwable t) { return false; }
    }

    private String tick(boolean ok) { return ok ? "[done] " : "[not yet] "; }

    private void build() {
        list.removeAllViews();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        add(text("adb setup guide", 22, Color.BLACK));
        add(text("Run these on a computer with the tablet plugged in. Nothing is needed to use the launcher: each feature that needs one says so. All lines are reversible (step 8). The same list is in research/ADB_COMMANDS.txt in the repo.", 14, Color.DKGRAY));
        add(text("Status now:\n"
                + tick(granted("android.permission.WRITE_SECURE_SETTINGS")) + "WRITE_SECURE_SETTINGS\n"
                + tick(granted("android.permission.READ_EXTERNAL_STORAGE")) + "READ_EXTERNAL_STORAGE (wallpaper patch)\n"
                + tick(granted("android.permission.DUMP")) + "DUMP\n"
                + tick(granted("android.permission.CHANGE_CONFIGURATION")) + "CHANGE_CONFIGURATION\n"
                + tick(granted("android.permission.PACKAGE_USAGE_STATS")) + "PACKAGE_USAGE_STATS (Settings look-alike: battery numbers)\n"
                + tick(appOp("android:get_usage_stats")) + "usage access\n"
                + tick(Settings.System.canWrite(this)) + "modify system settings\n"
                + tick(pm != null && pm.isIgnoringBatteryOptimizations(PKG)) + "battery whitelist\n"
                + tick(TjAccessibilityService.get() != null) + "accessibility helper is on", 14, Color.BLACK));
        add(button("Copy ALL commands (one per line)", new Runnable() { public void run() { copy(allCommands()); } }));
        for (final Step s : steps) {
            add(text(s.title, 17, Color.BLACK));
            add(text(s.why, 13, Color.DKGRAY));
            StringBuilder sb = new StringBuilder();
            for (String c : s.cmds) sb.append(c).append('\n');
            TextView cmd = text(sb.toString().trim(), 13, Color.BLACK);
            cmd.setTypeface(android.graphics.Typeface.MONOSPACE);
            cmd.setTextIsSelectable(true);
            cmd.setBackgroundColor(Color.parseColor("#F1F3F4"));
            cmd.setPadding(16, 12, 16, 12);
            add(cmd);
            final String joined = sb.toString().trim();
            add(button("Copy this step", new Runnable() { public void run() { copy(joined); } }));
        }
    }

    private String allCommands() {
        StringBuilder sb = new StringBuilder();
        for (Step s : steps) for (String c : s.cmds) sb.append(c).append('\n');
        return sb.toString();
    }

    private void copy(String t) {
        try {
            ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("adb commands", t));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) { Toast.makeText(this, "Could not copy", Toast.LENGTH_SHORT).show(); }
    }

    private void add(View v) { list.addView(v); }

    private TextView text(String t, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color); v.setPadding(0, 20, 0, 0);
        return v;
    }

    private Button button(String label, final Runnable r) {
        Button bt = new Button(this);
        bt.setText(label); bt.setAllCaps(false); bt.setGravity(Gravity.CENTER);
        bt.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { r.run(); } });
        return bt;
    }
}
