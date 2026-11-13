package com.tj.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Launcher control" (D2, Tj 2026-11-04: "I can't tell what is on or off. make toggles and improve the overall settings ui"). Sections, a real on/off Switch for every option
 * (green = on, grey = off), one-line explanations, a status card at the top. Only opens while Quickstep is on (Tj: the settings are part of the "other profile").
 * Plain framework views on purpose: nothing that can fail to inflate on a Go device.
 */
public class ControlActivity extends Activity {
    private static final int ACCENT = Color.parseColor("#00796B"), INK = Color.parseColor("#202124"), MUTED = Color.parseColor("#5F6368"), CARD = Color.parseColor("#F1F3F4");
    private boolean unlockOpen = false;    // the lock-screen method drop-down
    private LinearLayout page;
    private LinearLayout box;            // AR1: where the rows of the current group go (the page itself outside any group)
    private TextView groupSummary;       // the header of the current group: how many of its switches are on
    private int groupOn, groupTotal;
    private ScrollView scroll;
    private float dp;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Tj: the launcher's settings are only available while the stock launcher (Quickstep) is on ("lock" the user into the custom launcher).
        if (Mode.isCustom(this)) { finish(); return; }
        dp = getResources().getDisplayMetrics().density;
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(px(16), px(36), px(16), px(96));
        scroll.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        RecentsGuard.excludeOurTasks(this);
        RecentsGuard.describeAsTarget(this);
        if (Mode.isCustom(this)) { finish(); return; }
        build();
    }

    private int px(int v) { return Math.round(v * dp); }

    /** Rebuilds every row from the stored state (keeps the scroll position). */
    private void rebuild() {
        final int y = scroll.getScrollY();
        build();
        scroll.post(new Runnable() { @Override public void run() { scroll.scrollTo(0, y); } });
    }

    // ---------------------------------------------------------------- the page
    /** AD8: the whole page is rebuilt from the stored state on every change; how long that takes on THIS tablet goes into the report, so the cost is known before anyone optimises it. */
    static volatile long lastBuildMs = -1;

    private void build() {
        long t0 = android.os.SystemClock.uptimeMillis();
        buildPage();
        lastBuildMs = android.os.SystemClock.uptimeMillis() - t0;
    }

    private void buildPage() {
        page.removeAllViews();
        box = page;
        groupSummary = null;
        title("Launcher control");

        // status card
        boolean helper = TjAccessibilityService.get() != null;
        boolean grant = AnimationSpeedup.permitted(this);
        LinearLayout card = card();
        card.addView(chipLine("Active launcher", "Quickstep (stock)  -  settings only open here", ACCENT));
        int helperState = HelperStatus.now(this);   // ACC1: "enabled but NOT connected" is its own answer
        card.addView(chipLine("Accessibility helper", HelperStatus.describe(helperState), helperState == HelperStatus.CONNECTED ? ACCENT : helperState == HelperStatus.ENABLED_NOT_CONNECTED ? Color.parseColor("#B26A00") : Color.parseColor("#B3261E")));
        card.addView(chipLine("adb permission", grant ? "granted" : "not granted  -  keyboard / tiles / faster reset wait for it (see Advanced > adb setup guide)", grant ? ACCENT : Color.parseColor("#B26A00")));
        if (Features.isCover(Features.powerMode(this)) && Features.get(this, TjAccessibilityService.LOCK_PATCH_KEY, true)) {   // AM1: say plainly whether the lock-screen patch can be invisible yet
            String cal = PatchShot.summary(this);
            boolean never = cal.startsWith("not calibrated");
            boolean valid = cal.contains("(valid for");   // AU7: a stored crop that no longer matches the wallpaper / screen is NOT green
            boolean auto = Features.get(this, AutoCalibration.KEY, true);
            card.addView(chipLine("Lock-screen patch", valid ? cal : never ? "not calibrated yet  -  " + (auto ? "the launcher takes one screenshot of your lock screen by itself the next time it shows plainly (hold the side key once first, so it knows where the menu is); until then the Screenshot button still shows in the lock-screen power menu" : "the Screenshot button still shows in the lock-screen power menu (group Power menu > Calibrate the patch)") : cal, valid ? ACCENT : Color.parseColor("#B26A00")));
        }
        box.addView(card);
        box = page;
        // problems first: what the launcher did on its own (crash breaker, paused automation)
        if (CrashGuard.wasTripped(this)) note("The launcher crashed 3 times in a row, so it switched itself back to the stock launcher and restored everything.");
        if (!CrashGuard.clearedNote(this).isEmpty()) note(CrashGuard.clearedNote(this));
        if (!Features.getString(this, SwitchMigration.NOTE_KEY, "").isEmpty()) note(Features.getString(this, SwitchMigration.NOTE_KEY, ""));
        if (CrashGuard.isPaused(this)) note("Automatic switching is PAUSED: while you are in Quickstep nothing switches back to the custom launcher by itself (after \"Restore everything\", or after an update cleared the crash breaker). Press \"Switch to the custom launcher\" (Everyday) when you want it back.");
        LinearLayout both = row();
        both.addView(smallButton("Open all groups", new Runnable() { public void run() { setAllGroups(true); } }));
        both.addView(smallButton("Close all groups", new Runnable() { public void run() { setAllGroups(false); } }));
        box.addView(both);

        group("everyday", "Everyday", true);
        action("Switch to the custom launcher", "Also closes the apps you opened from Quickstep.", new Runnable() { public void run() {
            CrashGuard.clearTripped(ControlActivity.this);
            Mode.set(ControlActivity.this, Mode.CUSTOM);
            goHome();
            ReturnReset.run(ControlActivity.this);
        }});
        action("Choose the apps shown in the custom launcher", "Everything else stays hidden there (new installs start hidden).", new Runnable() { public void run() {
            startActivity(new Intent(ControlActivity.this, HiddenAppsActivity.class));
        }});
        toggle("Start the custom launcher clean (no Solitaire)", "ON (recommended): when the custom launcher starts (after a Quickstep session, after the tablet was restarted) the home screen is alone on screen and the Recents list is completely emptied; nothing is opened for you, so the unlock is as fast as it can be. OFF: the old behaviour (Solitaire in front, only the apps opened in Quickstep closed); the two Solitaire settings below only matter then.", ReturnReset.cleanStart(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ReturnReset.CLEAN_START_KEY, v); rebuild(); } });
        toggle("Open Solitaire automatically", "ON: Solitaire is put in front after boot / a launcher restart AND behind the lock screen when the tablet wakes. OFF: it is never opened for you (it then also stays out of Quickstep's Recents). The return from Quickstep has its own group: \"Plain (no app)\".", Features.get(this, TjHooks.STARTUP_KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, TjHooks.STARTUP_KEY, v); } });
        final EditText appBox = new EditText(this);
        appBox.setSingleLine(true);
        appBox.setText(ReturnReset.appLabel(this));
        appBox.setHint("app to open (blank = none, plain home screen)");
        box.addView(appBox);
        LinearLayout abtn = row();
        abtn.addView(smallButton("Save app name", new Runnable() { public void run() { Features.setString(ControlActivity.this, ReturnReset.APP_KEY, appBox.getText().toString().trim()); rebuild(); } }));
        abtn.addView(smallButton("Plain (no app)", new Runnable() { public void run() { Features.setString(ControlActivity.this, ReturnReset.APP_KEY, ""); rebuild(); } }));
        box.addView(abtn);
        action("Accessibility helper settings", "Turn the helper on or off (the power menu patch, the lock screen, the reset and the Recents filter need it).", new Runnable() { public void run() { startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)); } });

        toggle("Animations off in Quickstep (faster)", AnimationSpeedup.permitted(this) ? "ON: while you use Quickstep (its home screen and the apps you open there) every animation is off, so the tablet feels faster. They come back the moment the tablet locks (the lock screen and its power menu need real animations) and the moment the custom launcher takes over, put back exactly as they were; a value you set yourself in Developer options in the meantime is left alone. Nothing is ever switched off in the custom launcher or on the lock screen. Needs the accessibility helper to be running." : "Needs the adb permission (not granted yet).", AnimationSpeedup.quickstepSwitch(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, AnimationPolicy.KEY, v); AnimationSpeedup.syncNow(ControlActivity.this, "switched in Launcher control"); rebuild(); } });   // BF1
        note("Animations: " + AnimationSpeedup.describe(this));
        toggle("No pop-up notifications outside Quickstep", NotifQuiet.permitted(this) ? "ON: a notification that arrives while the custom launcher is in front, or while the tablet is dark or locked (in any launcher), does not pop up at the top of the screen. In an unlocked Quickstep session pop-ups work as usual: the setting is put back exactly as it was, and a value you set yourself meanwhile is left alone. This hides only the pop-up banner: the lock screen's own list is the lock-screen switch above, and what is already in the shade is cleared when the custom launcher starts. In a Quickstep session this needs the accessibility helper to be running (without it the pop-ups simply stay as Android has them)." : "Needs the adb permission (not granted yet).", NotifQuiet.switchOn(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, NotifQuietPolicy.KEY, v); AnimationSpeedup.syncNow(ControlActivity.this, "switched in Launcher control"); rebuild(); } });   // BH1
        note("Pop-up notifications: " + NotifQuiet.describe(this));
        toggle("Keep Wi-Fi and Bluetooth on, hide their symbols and the VPN key", RadioIconHide.permitted(this) ? "ON: Wi-Fi and Bluetooth are no longer switched off on the lock screen or in the custom launcher, and their symbols and the VPN key are hidden at the top of the lock screen, the custom launcher and the notification panel. Nothing here ever turns Wi-Fi or Bluetooth ON: a radio that is off stays off. The VPN itself is never switched on or off, only its key is hidden. In an unlocked Quickstep session the symbols show as usual: the list of hidden symbols is put back exactly as it was, and a list you edited yourself meanwhile only loses the entries this switch added. OFF: Wi-Fi and Bluetooth are switched off on the lock screen and in the custom launcher like before. Without the adb permission the symbols cannot be hidden, so the radios are switched off like before; in a Quickstep session the same happens on a lock screen that comes up before the helper is running (right after a restart) and when the helper goes away while the screen is dark or locked, because the symbols cannot be hidden then. In a Quickstep session this needs the accessibility helper to be running. Switch this OFF before you uninstall the launcher, otherwise the symbols stay hidden in every launcher. Whether your tablet's own lock screen obeys the hide is not proven yet: tell me if you still see a symbol." : "Needs the adb permission (not granted yet): until then Wi-Fi and Bluetooth are switched off on the lock screen and in the custom launcher like before.", RadioIconHide.switchOn(this), new Chg() { public void on(boolean v) {
            Features.set(ControlActivity.this, RadioIconHold.KEY, v);
            if (!v && Mode.isCustom(ControlActivity.this) && ProfileSession.radiosOff(ControlActivity.this)) {   // review of BS1: OFF means "like before", so the radio guard acts now, not only at the next lock or return; the symbols come back only AFTER the radios are off (one worker does both, in that order)
                final android.content.Context app = getApplicationContext();
                new Thread(new Runnable() { @Override public void run() { RadioIconHide.offThenSync(app, new Runnable() { @Override public void run() { ReturnReset.radiosOff(app); } }, "switched off in Launcher control", false); } }, "tj-radios-off").start();
            } else AnimationSpeedup.syncNow(ControlActivity.this, "switched in Launcher control");
            rebuild();
        } });   // BS1 + BS2
        note("Wi-Fi, Bluetooth and VPN symbols: " + RadioIconHide.describe(this));

        group("lock", "Lock screen", false);
        toggle("Show my number at the bottom", "Disappears when you pull up the PIN pad.", LockNumber.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, LockNumber.KEY, v); } });
        final EditText numberBox = new EditText(this);
        numberBox.setSingleLine(true);
        numberBox.setText(LockNumber.text(this));
        numberBox.setHint("lock-screen number");
        box.addView(numberBox);
        LinearLayout nbtns = row();
        nbtns.addView(smallButton("Save number", new Runnable() { public void run() { Features.setString(ControlActivity.this, LockNumber.TEXT_KEY, LockNumber.sanitize(numberBox.getText().toString())); rebuild(); } }));
        box.addView(nbtns);
        // v0.7.6: nudges on top of the measured look (applied the next time the lock screen shows the number); 0 = as measured
        stepper("Number size", LockNumber.SIZE_KEY, 2, -30, 30, " %");
        stepper("Number weight (0 = measured, +1 bolder, -1 lighter)", LockNumber.WEIGHT_KEY, 1, -2, 4, " step");
        stepper("Number brightness", LockNumber.ALPHA_KEY, 5, -40, 40, " %");
        toggle("Lock screen: no Wi-Fi, no Bluetooth, no app notifications (every launcher)", LockQuiet.permitted(this) ? "The moment the tablet is on the lock screen, from ANY launcher: Wi-Fi and Bluetooth are switched off and kept off (a radio that comes on again is switched off again, at most 3 times a minute; NOT while \"Keep Wi-Fi and Bluetooth on, hide their symbols and the VPN key\" above is on and can hide them: then the radios stay as they are), and the lock screen shows no app notifications and no notification icons (Android's own lock-screen setting, put back by this switch, \"Restore everything\" and the crash breaker). The notifications themselves stay in the shade until they are cleared." : "Needs the adb permission (not granted yet): without it only Wi-Fi and Bluetooth are switched off.",
                LockQuiet.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, LockQuiet.KEY, v); LockQuiet.applyNotificationsAsync(ControlActivity.this); rebuild(); } });   // v0.7.6 (review): do not wait for a bound accessibility helper to put the setting back
        toggle("A gesture unlock into Quickstep puts Wi-Fi and Bluetooth back", "Whatever the lock screen rule itself switched off during that lock comes back after the lock-screen gesture opens Quickstep. A plain PIN into the custom launcher leaves them off (the custom launcher's own rule).", LockQuiet.restoreEnabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, LockQuiet.RESTORE_KEY, v); } });
        note("Lock screen quiet: " + Features.getString(this, LockQuiet.LAST_KEY, "(has not had to act yet)") + "   (lock_screen_show_notifications = " + android.provider.Settings.Secure.getString(getContentResolver(), LockQuiet.SHOW) + ")");
        toggle("Prepare the custom launcher behind the lock screen", "On waking to the lock screen the custom home screen is put in front behind it, so the unlock opens it at once with no delay (Solitaire too, only if \"Open Solitaire automatically\" is on). A handshake puts Quickstep there instead, at once.", ProfileSession.prearm(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ProfileSession.PREARM_KEY, v); } });
        toggle("A gesture unlock comes back to the app I had open", "ON: after the lock-screen gesture, Quickstep opens the app that was in front when you locked the tablet, exactly as you left it (like tapping its icon). OFF: it opens the Quickstep home screen instead, with the apps minimised. With no app open it is always the home screen.", Features.get(this, ResumeTarget.KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ResumeTarget.KEY, v); } });

        group("handshakes", "Unlock into Quickstep (secret handshakes)", false);
        note("Locking the tablet ALWAYS ends a Quickstep session (no switch any more): a plain PIN or fingerprint gives the custom launcher again and closes what Quickstep left open. Only the crash breaker and \"Restore everything\" pause this, until you switch to the custom launcher yourself.");
        note("The custom launcher is the default: a plain PIN or fingerprint always opens it. Quickstep opens only after one of the handshakes you SELECT below, done on the lock screen BEFORE the PIN. Select as many as you like; at least one always stays selected, so you can never lock yourself out. Which digits you type is never read.");
        final UnlockMethods.Enabled stored = new UnlockMethods.Enabled() { @Override public boolean on(UnlockMethods.Method m) { return Features.get(ControlActivity.this, m.prefKey(), m.defaultOn); } };
        final java.util.List<UnlockMethods.Method> chosen = UnlockMethods.selected(stored);
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < chosen.size(); i++) { if (i == 2 && chosen.size() > 3) { names.append(", +").append(chosen.size() - 2).append(" more"); break; } if (i > 0) names.append(", "); names.append(chosen.get(i).title); }
        // the drop-down: a field that opens / closes the list of methods
        LinearLayout dd = row();
        dd.setBackground(fieldBackground());
        LinearLayout ddText = new LinearLayout(this);
        ddText.setOrientation(LinearLayout.VERTICAL);
        ddText.addView(text("Lock-screen methods: " + chosen.size() + " of " + UnlockMethods.Method.values().length + " selected", 16, INK, true));
        ddText.addView(text(names.toString(), 13, MUTED, false));
        dd.addView(ddText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dd.addView(text(unlockOpen ? "^" : "v", 22, ACCENT, true));
        dd.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { unlockOpen = !unlockOpen; rebuild(); } });
        box.addView(dd);
        if (unlockOpen) {
            for (final UnlockMethods.Method m : UnlockMethods.Method.values()) {
                boolean on = chosen.contains(m);
                toggle(m.title, m.how, on, new Chg() { public void on(boolean v) {
                    Features.set(ControlActivity.this, m.prefKey(), v);
                    if (!v) {
                        boolean any = false;
                        for (UnlockMethods.Method x : UnlockMethods.Method.values()) if (stored.on(x)) any = true;
                        if (!any) {   // the last one was just switched off: put it back
                            Features.set(ControlActivity.this, m.prefKey(), true);
                            android.widget.Toast.makeText(ControlActivity.this, "At least one method must stay selected", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                } });
            }
        }
        toggle("Buzz when a handshake is accepted", "A 40 ms vibration so you know it counted.", ProfileSession.buzz(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ProfileSession.BUZZ_KEY, v); } });
        note("The volume / Back methods need the helper's key-event access (it asks for it only while one of them is selected; turn the helper off and on once after selecting one). Shake and knock read the accelerometer only while the lock screen is showing.");
        note("Last unlock: " + Features.getString(this, UnlockPick.LAST_KEY, "(none yet)") + "\nLast lock-screen switch: " + Features.getString(this, ProfileSession.PREARM_REPORT_KEY, "(none yet)"));

        group("power", "Power menu (side key)", false);
        final String mode = Features.powerMode(this);
        radio("When you hold the side key in the custom launcher", new String[] { "Leave it alone", "Hide the Screenshot button with a wallpaper patch (the stock Power off / Restart stay completely stock on the LOCK screen and, once calibrated, on the HOME screen; where the patch cannot be shown a card of mine covers them)", "Hide it under a wallpaper patch + my card (home screen only)", "Block it completely" },
                new String[] { Features.POWER_OFF, Features.POWER_HIDE_SCREENSHOT, Features.POWER_WALLPAPER, Features.POWER_BLOCK }, mode, new Str() { public void on(String v) { Features.setString(ControlActivity.this, Features.POWER_MODE, v); } });
        action("Allow wallpaper access", checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED ? "Granted. The patch and its calibration read the wallpaper and the screenshot." : "NOT granted yet. Needed by the wallpaper patch and its calibration (it reads the wallpaper and the screenshot).", new Runnable() { public void run() { requestPermissions(new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE }, 7); } });
        if (Features.isCover(mode)) {
            toggle("Lock screen: hide the Screenshot button with a patch of my lock wallpaper", "On the lock screen only, from any launcher: a piece of your LOCK-screen wallpaper covers the place where the stock power menu shows its Screenshot button, so the menu reads Power off + Restart. The stock menu itself is not changed. For the patch to be invisible it is cut from ONE real screenshot of your lock screen (Calibrate below).",
                    Features.get(this, TjAccessibilityService.LOCK_PATCH_KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, TjAccessibilityService.LOCK_PATCH_KEY, v); rebuild(); } });
        } else note("The lock-screen patch is used only with the side-key options that hide the Screenshot button (choose one above).");
        if (Features.get(this, TjAccessibilityService.LOCK_PATCH_KEY, true) && Features.isCover(mode)) {   // AE3 / AD7b (Tj, 2026-11-06)
            toggle("Keep the patch over the Screenshot spot ALL the time on the lock screen", "Default ON. The patch stays over the place where the stock menu's Screenshot button appears, so the button is covered from the very first frame of the menu (no flicker). It only shows once it can be INVISIBLE: after you calibrate it with one real screenshot of your lock screen (see below). It hides itself while the PIN pad is up. Open the power menu once on the lock screen first, so the app learns where the menu sits. The nudges below work while the menu is CLOSED, so you can line the patch up by looking at it.",
                    Features.get(this, PatchRest.KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, PatchRest.KEY, v); rebuild(); } });
            toggle("Draw my own Power off / Restart card instead of the stock buttons", "Default OFF: on the lock screen the stock menu stays completely stock and only the patch covers the Screenshot spot (on the home screen a card of mine always covers the entries). ON: the old way, my card is drawn over the two stock entries on the lock screen too.",
                    Features.get(this, TjAccessibilityService.POWER_CARD_KEY, false), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, TjAccessibilityService.POWER_CARD_KEY, v); rebuild(); } });
            String shotLast = Features.getString(this, PatchShot.STATUS_KEY, "");
            note("Patch from a real screenshot: " + PatchShot.summary(this) + (shotLast.isEmpty() ? "" : "   last: " + shotLast));
            toggle("Calibrate the patch by itself (default ON)", "When no valid calibration exists for the current wallpaper and screen, the helper takes ONE screenshot of the plain lock screen by itself (waiting for a lock screen with no PIN pad and no power menu; at most 3 tries per lock, 8 per day) and keeps only the crop around the Screenshot spot. Every try leaves one screenshot in the tablet's gallery. OFF = only the button below.",
                    Features.get(this, AutoCalibration.KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, AutoCalibration.KEY, v); rebuild(); } });
            note("Automatic calibration: " + Features.getString(this, "patch_auto_note", "(nothing yet)"));
            action("Calibrate the patch (one screenshot of the lock screen)", "1) Hold the side key once on the lock screen (so the menu spot is known), then close the menu. 2) Tap this button. 3) Lock the tablet and wake it: the lock screen takes the screenshot by itself (or press Power + Volume Down yourself). 4) Unlock: the line above says CALIBRATED. The patch hides while you calibrate so it cannot be in the picture. Do it again if you change the wallpaper or turn the tablet.",
                    new Runnable() { public void run() { Features.setString(ControlActivity.this, AutoCalibration.START_KEY, String.valueOf(System.currentTimeMillis())); Features.setString(ControlActivity.this, PatchShot.CAL_UNTIL_KEY, String.valueOf(System.currentTimeMillis() + PatchShot.WINDOW_MS)); Features.setString(ControlActivity.this, PatchShot.STATUS_KEY, "calibrating: lock the tablet and wake it"); rebuild(); } });   // review of v0.8.1: the window's own start, so an older automatic window's start cannot make an old screenshot count
            action("Forget the patch calibration", "The patch then stays hidden at rest until you calibrate again.", new Runnable() { public void run() { PatchShot.forget(ControlActivity.this); rebuild(); } });
            note("Wallpaper type: " + Features.getString(this, TjAccessibilityService.WALLPAPER_KIND_KEY, "(not read yet: lock the tablet once)"));
        }
        if (Features.POWER_HIDE_SCREENSHOT.equals(mode)) {   // BI1 (Tj, 2026-11-08: "do the same logic and wallpaper patch that you did for the lock screen, but implement it for the unlocked home screen")
            toggle("Home screen: hide the Screenshot button with a patch of the real home screen", "Default ON. On the unlocked custom home screen the stock Power off / Restart stay completely stock, like on the lock screen: a patch of the REAL pixels covers the Screenshot spot. It is made of one calibrated picture of your plain wallpaper (one screenshot, the home screen goes blank for a second) and the home screen's own icons, copied live. It only shows while the home screen is at rest (no finger, no scrolling, no open folder); at any other moment my card covers the buttons as before. Open the power menu once on the home screen first, so the app learns where the menu sits.",
                    Features.get(this, HomePatchRules.KEY, HomePatchRules.defaultOn((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, HomePatchRules.KEY, v); TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.refreshLayersSoon(); rebuild(); } });
            TjAccessibilityService hs = TjAccessibilityService.get();
            note("Home patch: " + (hs != null ? hs.homePatchInfo() : "the accessibility helper is not running (" + Features.getString(this, TjAccessibilityService.HOME_STATUS_KEY, "nothing yet") + ")"));
            action("Calibrate the home patch again (one screenshot of the plain wallpaper)", "Forgets the stored picture of your plain wallpaper. The next time the CUSTOM launcher's home screen is quiet (no finger, no scrolling, a few seconds after the screen came on) it is made again by itself: the home screen goes blank for about a second and the screen flashes once (a normal screenshot: it stays in the gallery). It is also made by itself the first time, when you change the wallpaper, and when you move to another page of the home screen. Needs 'Allow wallpaper access' above. Until it exists my card covers the buttons as before.",
                    new Runnable() { public void run() { HomeShot.invalidateAll(ControlActivity.this); HomeCurtain.invalidateAll(ControlActivity.this); Features.setString(ControlActivity.this, "home_cal_day", ""); Features.setString(ControlActivity.this, TjAccessibilityService.HOME_STATUS_KEY, "calibration forgotten: it is made again by itself the next time the custom launcher's home screen is quiet"); TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.refreshLayersSoon(); rebuild(); } });
        }
        if (Features.POWER_WALLPAPER.equals(mode) || Features.get(this, TjAccessibilityService.LOCK_PATCH_KEY, true) && Features.isCover(mode)) {
            note("Wallpaper patch: " + Features.getString(this, TjAccessibilityService.PATCH_STATUS_KEY, "(not reported yet)"));
            note("Lock-screen patch alignment: " + Features.getString(this, TjAccessibilityService.CAL_STATUS_KEY, "(lock the tablet once with the adb helper running; it photographs the lock screen and finds the right part of the wallpaper by itself)"));
            action("Re-calibrate the lock-screen patch", "Forgets the measured alignment; it is measured again the next time the lock screen shows (needs the adb helper).", new Runnable() { public void run() { Features.setString(ControlActivity.this, TjAccessibilityService.LOCK_CAL_KEY, ""); Features.setString(ControlActivity.this, TjAccessibilityService.CAL_STATUS_KEY, "(will be measured at the next lock)"); rebuild(); } });
            stepper("Patch left / right", TjAccessibilityService.PATCH_DX_KEY, 2, -200, 200, " px");
            stepper("Patch up / down", TjAccessibilityService.PATCH_DY_KEY, 2, -200, 200, " px");
            stepper("Patch size", TjAccessibilityService.PATCH_SCALE_KEY, 1, -30, 30, " %");
            stepper("Patch brightness (lock screen)", TjAccessibilityService.PATCH_DIM_KEY, 2, -40, 40, " %");
            stepper("Menu dimming (patch brightness while the menu is OPEN, lock and home)", TjAccessibilityService.PATCH_SCRIM_KEY, 2, -40, 40, " %");
            toggle("Measure the menu's real dimming on this tablet (default ON)", "The stock power menu darkens the lock screen with its own scrim; the patch has to be dimmed EXACTLY the same to be invisible. ONCE per lock screen, in its normal (light) look; the dark look the lock screen has for a few seconds after Quickstep uses that measurement and is never measured on its own. As long as the wallpaper, the screen size and the rotation stay the same, nothing is ever measured again. The helper takes a frame of the plain lock screen, opens the stock power menu by itself for about two seconds, takes a second frame and closes the menu again: from the two frames it works out the scrim's exact scale and colour offset per channel. You will see two camera flashes and the menu appear and go. It only starts on a plain lock screen that has been on for 10 seconds (no PIN pad, no gesture in progress), at most once per lock, and it stops for good after 2 tries that stored nothing. OFF = the patch uses the generic 0.80 dimming plus your nudge above.",
                    Features.get(this, ScrimMeasure.KEY, ScrimMeasure.defaultOn((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ScrimMeasure.KEY, v); rebuild(); } });
            note("Menu dimming measurement: " + Features.getString(this, ScrimMeasure.NOTE_KEY, "(nothing yet)") + "   stored (light theme): " + ScrimFit.describeStored(Features.getString(this, ScrimMeasure.valueKey(0, false), "")) + "   stored (dark theme): " + ScrimFit.describeStored(Features.getString(this, ScrimMeasure.valueKey(0, true), "")) + "   last started: " + Features.getString(this, ScrimMeasure.WHY_KEY, "(never)"));
            action("Measure the menu's dimming again", "Forgets the stored measurements of the upright lock screen; the helper measures it again, in the light look, on the next plain lock screen that stays on for 10 seconds (two flashes, the menu opens by itself for about two seconds).", new Runnable() { public void run() { for (boolean n : new boolean[] { false, true }) { Features.setString(ControlActivity.this, ScrimMeasure.valueKey(0, n), ""); Features.setString(ControlActivity.this, ScrimMeasure.failKey(0, n), ""); } Features.setString(ControlActivity.this, ScrimMeasure.TRIES_KEY, ""); Features.setString(ControlActivity.this, ScrimMeasure.NOTE_KEY, "(forgotten: measured again at the next plain lock screen)"); rebuild(); } });
            toggle("Fade the patch together with the stock menu (no flicker)", "Default ON. The stock power menu darkens the screen around it with a 0.3 s fade; the patch follows the SAME fade, started the moment the menu appears, so it never shows as a darker or lighter block. OFF = the old way: the patch jumps to its dim in one step (a slight flicker).",
                    Features.get(this, TjAccessibilityService.PATCH_FADE_KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, TjAccessibilityService.PATCH_FADE_KEY, v); } });
            stepper("Fade timing (+ = the patch darkens earlier, - = later; only if the patch still flashes)", TjAccessibilityService.PATCH_FADE_SHIFT_KEY, 10, -100, 100, " ms");
        }
        note("Speed of the last menu: " + Features.getString(this, TjAccessibilityService.LATENCY_KEY, "(open the menu once)"));

        group("recents", "Recents & wallpaper", false);
        toggle("Recents of the custom launcher: only apps of its drawer", "Whenever the Recents screen opens in the custom launcher, the cards of apps that are NOT in the custom app drawer (hidden apps, apps you opened in Quickstep that have no drawer entry) are closed at once. A card that matches no known app is never touched.", Features.get(this, RecentsFilter.KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, RecentsFilter.KEY, v); } });
        toggle("Keep hidden apps out of Recents (invisible helper)", "With the adb helper running, the Recents tasks of every app that is NOT in the custom drawer are removed in the background: when the tablet locks, after a plain unlock, when the Recents screen opens and when such an app leaves the screen. They are gone before any card could be drawn.", RecentsSweep.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, RecentsSweep.KEY, v); } });
        String sweepLast = RecentsSweep.lastResult();
        note("Hidden-app sweep (needs the adb helper: " + (ShellHelper.enabled(this) ? "switch on" : "its switch is off") + "), last: " + (sweepLast.isEmpty() ? "(none since this app started)" : sweepLast));
        String recentsStatus = Features.getString(this, RecentsFilter.STATUS_KEY, "");
        if (!recentsStatus.isEmpty()) note("Recents filter, last: " + recentsStatus);
        toggle("Keep apps from the custom launcher OUT of Recents", "Off = they appear in Recents like normal apps. On = only Solitaire is listed.", RecentsPolicy.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, RecentsPolicy.KEY, v); } });
        toggle("Hide the launcher from Recents completely", "Home-type launcher: never listed. If the launcher ever crashes Android asks you to choose the Home app again (adb guide, step 6, fixes it).", Features.get(this, RouterActivity.HOME_TYPE_KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, RouterActivity.HOME_TYPE_KEY, v); } });
        toggle("Hide \"My photos\" in the wallpaper picker", "Long-press on the home screen > Wallpapers opens the launcher's own copy of the stock wallpaper picker WITHOUT the \"My photos\" tile (nothing else is offered). If the stock picker is opened some other way (Settings), its \"My photos\" tile is covered in the picker's own colour and cannot be tapped. Quickstep's picker stays stock.", Features.get(this, PickerGuard.KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, PickerGuard.KEY, v); } });
        note("Wallpaper picker: the launcher\'s own copy (no \"My photos\"). Stock picker found on this tablet: " + WallpaperPickerChoice.summary(this));
        String pickerStatus = Features.getString(this, PickerGuard.STATUS_KEY, "");
        if (!pickerStatus.isEmpty()) note("Wallpaper picker, last: " + pickerStatus);

        group("settings", "Settings look-alike", false);
        toggle("Settings look-alike in the custom launcher", "On = the custom drawer has a \"Settings\" icon that opens a look-alike with only Battery, Display, Sound, Storage, Accessibility, System and About tablet, and the real Settings is kept out of the custom drawer. Off = the real Settings is the only one. The Quickstep drawer always keeps the real Settings.", SettingsSpoof.switchOn(this), new Chg() { public void on(boolean v) { if (v) SsFuse.rearm(ControlActivity.this); Features.set(ControlActivity.this, SettingsSpoof.KEY, v); SettingsSpoof.syncSoon(ControlActivity.this, "switch " + (v ? "on" : "off")); } });
        String ssStatus = Features.getString(this, SettingsSpoof.STATUS_KEY, "");
        if (!ssStatus.isEmpty()) note("Settings entry, last change: " + ssStatus);
        String ssFuse = SsFuse.blockedWhy(this);
        if (!ssFuse.isEmpty()) note("The Settings look-alike switched itself OFF after a crash in one of its pages (" + ssFuse + "). Switch it on again to retry.");

        group("return", "Return to the custom launcher (from Quickstep)", false);
        toggle("Run the return reset", "Master switch: when you go back to the custom launcher from Quickstep (double Home, the Back-to-custom icon or tile, a plain PIN) the apps you opened there are closed and the notifications cleared.", ReturnReset.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ReturnReset.KEY, v); } });
        if (ReturnReset.enabled(this)) {
            toggle("Clear notifications", "Skipped by itself when the status bar shows none.", ReturnReset.clearNotifications(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ReturnReset.CLEAR_NOTIF_KEY, v); } });
            toggle("Close the apps opened from Quickstep", "Only those apps; apps opened from the custom launcher stay.", ReturnReset.clearRecents(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ReturnReset.CLEAR_RECENTS_KEY, v); } });
            toggle("Black cover while it works", "Hides the shade / Recents flicker.", Features.get(this, ReturnReset.COVER_KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ReturnReset.COVER_KEY, v); } });
            toggle("Home-looking curtain instead of the black cover", "Default ON. While the unlock clean-up works (a plain PIN after a Quickstep session, no adb helper) the cover shows an identical picture of your home screen instead of black. The picture is made of your real wallpaper (kept by the home calibration: ONE extra screenshot flash after this update, once per wallpaper position) and the Launcher's own pixels, and it is re-made by itself when the home changes. No picture, another wallpaper or another rotation: the black cover, as before. Touches on the picture are swallowed once the unlock is confirmed.",
                    Features.get(this, HomeCurtainRules.KEY, HomeCurtainRules.defaultOn((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, HomeCurtainRules.KEY, v); rebuild(); } });
            TjAccessibilityService curtainSvc = TjAccessibilityService.get();
            note("Home curtain (BL1): " + (curtainSvc != null ? curtainSvc.curtainInfo() : "the accessibility helper is not running (last use: " + Features.getString(this, TjAccessibilityService.CURTAIN_STATE_KEY, "none") + ")"));
            toggle("Animations off during the reset", AnimationSpeedup.permitted(this) ? "Put back exactly as they were afterwards." : "Needs the adb permission (not granted yet).", AnimationSpeedup.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, AnimationSpeedup.KEY, v); } });
            note("Last reset:\n" + Features.getString(this, TjAccessibilityService.RESET_REPORT_KEY, "(not run yet)"));
        } else note("The return reset is OFF: nothing is closed or cleared when you come back from Quickstep.");
        toggle("Close Quickstep's apps and notifications the moment the tablet locks", "Needs the adb helper (Advanced > Invisible clean-up): apps, their notifications and their Recents cards are closed at lock, and the custom account is already active when you unlock, so the unlock has nothing left to do. Without the helper the clean-up happens at unlock under a black cover.", ProfileSession.preClean(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ProfileSession.PRECLEAN_KEY, v); } });
        note("Any one of these ends the Quickstep session: it switches to the custom launcher, closes what Quickstep opened, clears notifications, turns Wi-Fi/Bluetooth off and opens Solitaire. They only work while Quickstep is active. Locking the tablet also ends it.");
        for (final ReturnMethods.Method m : ReturnMethods.Method.values()) {
            if (m == ReturnMethods.Method.HOME2) continue;   // the router reads Home presses itself: shown below
            toggle(m.title, m.how, ReturnMethods.on(this, m), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, m.prefKey(), v); } });
        }
        note(ReturnMethods.Method.HOME2.title + ": " + ReturnMethods.Method.HOME2.how + "  (always on, nothing to switch)");
        note("The \"Back to custom\" icon is in Quickstep's app drawer (arrow icon). Put it on the home screen if you like. The tile is added from the tile editor (pencil) - or tick it in the group Quick settings & keyboard.");

        group("battery", "Battery, radios & speed", false);
        toggle("Turn battery saver off in the custom launcher", BatterySaverGuard.permitted(this) ? "When the custom launcher becomes active (Quickstep ends, a PIN unlock, the tablet starts) and battery saver is on, it is switched off at once. Quickstep is never touched: the battery tile in the shade and the Battery saver screen in Settings work exactly as on a stock tablet. (If a tap to turn it ON is ever ignored by Android after this switch-off, the launcher carries that tap out for you.)" : "Needs the adb permission (not granted yet).", BatterySaverGuard.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, BatterySaverPolicy.KEY, v); if (v) BatterySaverGuard.check(ControlActivity.this, "switched on in Launcher control"); } });
        toggle("Strict battery saver (the old way)", "OFF (default): the tile and Settings always answer. ON: while the custom launcher is active a saver that gets turned on is switched off again within a moment (at most 4 times per minute) and the rule is re-applied at every start of the launcher process. Quickstep is never touched either way.", BatterySaverGuard.strict(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, BatterySaverPolicy.STRICT_KEY, v); if (v) BatterySaverGuard.check(ControlActivity.this, "strict switched on in Launcher control"); } });
        note("Battery saver guard: " + Features.getString(this, BatterySaverPolicy.LAST_KEY, "(has not had to act yet)"));
        toggle("Battery saver off on the lock screen (any launcher)", BatterySaverGuard.permitted(this) ? "While the lock screen is up battery saver is always OFF, whichever launcher you locked from (the dark theme and the lock screen's animations then never depend on it). If something turns it on while the lock screen is up it is switched off again within a moment. The custom launcher keeps it off after an unlock." : "Needs the adb permission (not granted yet).", BatterySaverGuard.lockOffEnabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, BatterySaverPolicy.LOCK_OFF_KEY, v); } });
        toggle("Bring Quickstep's battery saver back after a gesture unlock", "When you unlock into Quickstep with your gesture, the saver goes back to what it was the last time you were in Quickstep. Only when Android cannot have lost track of it: not while charging, not after the tablet was plugged in or restarted meanwhile (then the tile in the shade is one tap away). A plain unlock into the custom launcher leaves it off.", BatterySaverGuard.lockRestoreEnabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, BatterySaverPolicy.LOCK_RESTORE_KEY, v); } });
        note("Lock-screen battery saver: " + BatterySaverGuard.lockStatus(this));
        toggle("No Wi-Fi or Bluetooth in the custom launcher", "If something turns them on while the custom launcher is active they are switched off again. Quickstep keeps whatever you set. Both are the exception while \"Keep Wi-Fi and Bluetooth on, hide their symbols and the VPN key\" above is on.", ProfileSession.radiosOff(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ProfileSession.RADIOS_KEY, v); } });
        toggle("Keep the launcher in memory (faster unlock)", "A do-nothing foreground service stops Android from closing the launcher while the tablet sleeps, so an unlock never has to start it again. Cost: it stays in memory and ONE silent notification (\"Launcher is kept ready\") sits in the notification shade. Its status-bar icon is transparent; Android, not the app, decides whether it also shows on the lock screen (hide it under Settings > Notifications if it does). Off = Android may close the launcher when memory is short. OFF by default (version 0.7.2): on one tablet its notification crashed the launcher, so it must be proven first; if Android ever refuses the notification again, this switch turns itself off.", KeepAliveService.enabled(this), new Chg() { public void on(boolean v) {
            KeepAliveService.setUserChoice(ControlActivity.this, v);
            KeepAliveService.ensure(ControlActivity.this);
        } });
        if (KeepAliveService.blocked(this)) note("Keep-alive switched ITSELF off because Android refused its notification (" + KeepAliveService.blockedWhy(this) + "). Nothing else was changed. Switch it on again only to try once more.");
        note("Keep-alive now: " + (KeepAliveService.running() ? "RUNNING (the launcher process is a foreground service)" : "not running") );

        group("qs", "Quick settings & keyboard", false);
        toggle("Hide and disable the pencil (Edit)", "Custom launcher only: a white cover over the pencil while the panel is open. Quickstep never has it.", Features.get(this, TjAccessibilityService.PEN_KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, TjAccessibilityService.PEN_KEY, v); } });
        toggle("Gear opens the Settings look-alike", "Custom launcher, tablet unlocked: the gear in the notification panel opens the look-alike Settings instead of the real one. Needs the pencil cover (above) and the Settings look-alike (Settings group) to be on. A tap within about half a second after you pull the panel or touch it can still reach the real gear; the lock-screen panel always keeps the real gear. Last: " + Features.getString(this, GearRules.STATUS_KEY, "(not used yet)"), Features.get(this, GearRules.KEY, GearRules.defaultOn(ReturnReset.isDebuggable(this))), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, GearRules.KEY, v); } });
        toggle("Hide the status bar in the custom launcher (experimental)", ImmersiveProfile.permitted(this) ? "A quieter look: no clock, battery or notification icons while the custom launcher is active; Quickstep always keeps its bar. NOT a lock: one swipe down from the top edge shows the bar again and a second swipe opens the panel, so the rotation tile still works. Uses Android's own immersive setting (put back for Quickstep)." : "Needs the adb permission (not granted yet).", ImmersiveProfile.enabled(this), new Chg() { public void on(boolean v) {
            Features.set(ControlActivity.this, ImmersiveProfile.KEY, v);
            ImmersiveProfile.apply(ControlActivity.this, Mode.get(ControlActivity.this));
        } });
        toggle("Different tiles for each launcher", TileProfile.permitted(this) ? "Applied the moment you switch launcher." : "Needs the adb permission (not granted yet).", TileProfile.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, TileProfile.KEY, v); } });
        if (TileProfile.enabled(this)) {
            tileChooser("Tiles in the custom launcher", TileProfile.CUSTOM_KEY, TileProfile.DEFAULT_CUSTOM);
            tileChooser("Tiles in Quickstep", TileProfile.QUICKSTEP_KEY, TileProfile.DEFAULT_QUICKSTEP);
        } else note("Per-launcher tiles are OFF: both launchers keep whatever quick-settings tiles Android has.");
        toggle("Stock keyboard in custom, Gboard in Quickstep", KeyboardSwitch.status(this), KeyboardSwitch.enabled(this), new Chg() { public void on(boolean v) {
            Features.set(ControlActivity.this, KeyboardSwitch.KEY, v);
            KeyboardSwitch.apply(ControlActivity.this, Mode.get(ControlActivity.this));
        } });

        group("safety", "Safety & secret ways to Quickstep", false);
        toggle("Keep Quickstep away unless I use a handshake", "Quickstep opens ONLY after a secret unlock handshake (Lock screen section). If its home screen appears any other way in custom mode, send me back.", Features.get(this, StrayHomeGuard.KEY, true), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, StrayHomeGuard.KEY, v); } });
        note("Safety: if the Accessibility helper is OFF no handshake can work, so three quick Home presses in the custom launcher let you out to Quickstep. With the helper ON that does nothing.");
        note("Hidden ways to leave the custom launcher for Quickstep, tablet unlocked. No hardware button and no motion sensor is used; each one is made to be hard to trigger by accident, and each can be switched off here. (The lock-screen handshakes, in their own group, and the ladder below are separate.)");
        for (final SecretEntrances.Method m : SecretEntrances.Method.values()) {
            toggle(m.title, m.how, EntranceWatcher.on(this, m), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, m.prefKey(), v); } });
        }
        note("Ladder, easiest first: 1) the lock-screen handshakes (group Unlock into Quickstep); 2) the key code below; 3) the hidden Solitaire hotspot below; 4) if the Accessibility helper is OFF: press Home three times quickly; 5) Android itself: Settings > Apps > Default apps > Home app, or Safe mode (hold Power off), or adb (see the adb guide).");
        toggle("Key code anywhere in the custom launcher", "Volume Up, Up, Down, Down, Back, Back, Up, Down within 20 seconds, screen unlocked. Needs the helper's key access.", FailsafeKeys.konami(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, FailsafeKeys.KONAMI_KEY, v); } });
        toggle("Hidden Solitaire hotspot", "An invisible 56 dp square in a corner of Solitaire: tap it 5 times quickly, then touch and HOLD it for a second. The game cannot use that corner while this is on.", FailsafeKeys.hotspot(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, FailsafeKeys.HOTSPOT_KEY, v); } });
        radio("Hotspot corner", FailsafeKeys.CORNERS, new String[] { "0", "1", "2", "3" }, String.valueOf(FailsafeKeys.corner(this)), new Str() { public void on(String v) { Features.setInt(ControlActivity.this, FailsafeKeys.CORNER_KEY, Integer.parseInt(v)); } });
        action("Probe Solitaire: what can the helper read?", "Starts a 10-minute probe. Open Solitaire, move a few cards, come back here. Tells us whether a CARD-MOVE code is possible.", new Runnable() { public void run() {
            Features.setString(ControlActivity.this, FailsafeKeys.PROBE_UNTIL_KEY, String.valueOf(System.currentTimeMillis() + 600_000));
            Features.setString(ControlActivity.this, FailsafeKeys.PROBE_REPORT_KEY, "(probe running: open Solitaire and move some cards)");
            rebuild();
        }});
        note("Probe result: " + Features.getString(this, FailsafeKeys.PROBE_REPORT_KEY, "(not run)") + "\nLast failsafe / return: " + Features.getString(this, FailsafeKeys.LAST_KEY, "(none yet)"));

        group("advanced", "Advanced & diagnostics", false);
        final boolean helperUp = ShellHelper.reachable();
        LinearLayout hc = card();
        hc.addView(chipLine("Shell helper", helperUp ? "RUNNING - the clean-up is invisible" : "not running - the black-cover method is used instead", helperUp ? ACCENT : Color.parseColor("#B26A00")));
        box.addView(hc);
        toggle("Use the invisible clean-up when the helper is running", "Quickstep's apps are force-stopped behind Solitaire: their Recents cards, notifications and processes vanish with no black cover, no notification shade and no Recents screen. Off = always the cover method.", ShellHelper.enabled(this), new Chg() { public void on(boolean v) { Features.set(ControlActivity.this, ShellHelper.KEY, v); } });
        action("Copy the adb command that starts the helper", "Run it on the computer with the tablet plugged in. It lasts until the tablet restarts, then run it again. (Two-step version for Windows: adb guide, step 10.)", new Runnable() { public void run() {
            ((android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("adb", ShellProtocol.START_ONE_LINER));
            android.widget.Toast.makeText(ControlActivity.this, "Copied", android.widget.Toast.LENGTH_SHORT).show();
        }});
        note("Link: a local-only connection (127.0.0.1). The launcher declares the Internet permission only so Android lets it open that connection; nothing else in the launcher uses it.");
        note("The helper is a small process the adb command starts as the shell user. It only answers this app, and only knows two commands: \"are you there\" and \"force-stop these apps\" (never the system, the keyboard, this launcher or the stock launcher). Stop it any time with: " + ShellProtocol.STOP_LINE);
        action("Tablet setup & report", "Every adb to-do item on one screen (permissions, battery, accessibility, helper), a \"Do everything\" button, and a report + copies of the system apps for Claude.", new Runnable() { public void run() { startActivity(new Intent(ControlActivity.this, SetupActivity.class)); } });
        action("adb setup guide", "Every command, step by step, with copy buttons.", new Runnable() { public void run() { startActivity(new Intent(ControlActivity.this, AdbGuideActivity.class)); } });
        note("Last unlock: " + Features.getString(this, TjAccessibilityService.UNLOCK_SPEED_KEY, "(unlock once to measure)"));
        note("Last unlock, start to finish (BK1c): " + Features.getString(this, TjAccessibilityService.UNLOCK_SUM_KEY, "(unlock once to measure)"));
        note("Lock screen log (newest last):\n" + (LockLog.tail(this, 8).isEmpty() ? "(nothing yet: lock and unlock the tablet once)" : LockLog.tail(this, 8)));
        note("Stray stock home screens sent back: " + Features.getString(this, StrayHomeGuard.COUNT_KEY, "0") + " (last " + Features.getString(this, StrayHomeGuard.LAST_KEY, "never") + ")"

                + "\nWill open: " + describeTarget());

        box = page;
        section("Restore");
        action("Restore everything", "Turns every change of the launcher off and puts the settings back as they were.", new Runnable() { public void run() {
            new AlertDialog.Builder(ControlActivity.this).setTitle("Restore everything?").setMessage("Every launcher feature is switched off and the original system settings are put back. You stay in Quickstep.")
                    .setPositiveButton("Restore", new DialogInterface.OnClickListener() { public void onClick(DialogInterface d, int w) {
                        RestoreStore.restoreAll(ControlActivity.this);
                        CrashGuard.pause(ControlActivity.this);   // AC1: nothing of ours switches launchers for you until you go back to the custom launcher on purpose
                        Mode.set(ControlActivity.this, Mode.QUICKSTEP);
                        rebuild();
                    } }).setNegativeButton("Cancel", null).show();
        }});
    }

    private String describeTarget() {
        String app = ReturnReset.findAppLabel(this);
        return app == null ? "(no app matching '" + ReturnReset.appLabel(this) + "' found)" : app;
    }

    // ---------------------------------------------------------------- small UI kit
    private interface Chg { void on(boolean v); }
    private interface Str { void on(String v); }

    private TextView text(String t, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    private void title(String t) {
        TextView v = text(t, 26, INK, true);
        v.setPadding(px(4), 0, 0, px(12));
        box.addView(v);
    }


    // ---------------------------------------------------------------- AR1: collapsible groups
    private static final String[] GROUPS = { "everyday", "lock", "handshakes", "power", "recents", "return", "battery", "qs", "safety", "advanced" };

    private void setAllGroups(boolean open) {
        for (String id : GROUPS) Features.set(this, "ctl_open_" + id, open);
        rebuild();
    }

    /** A group: a tappable header (title, "n of m on", an arrow) and the box its rows go into. Open / closed is remembered per group. */
    private void group(final String id, String title, boolean openByDefault) {
        final boolean open = Features.get(this, "ctl_open_" + id, openByDefault);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(px(4), px(18), px(4), px(10));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.addView(text(title, 18, INK, true));
        groupSummary = text("", 13, MUTED, false);
        mid.addView(groupSummary);
        head.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(text(open ? "v" : ">", 22, ACCENT, true));
        head.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { Features.set(ControlActivity.this, "ctl_open_" + id, !open); rebuild(); } });
        page.addView(head);
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#DADCE0"));
        page.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setVisibility(open ? View.VISIBLE : View.GONE);
        page.addView(box);
        groupOn = 0; groupTotal = 0;
    }

    /** AR1: is the group being built closed? (outside any group {@code box} is the page itself: never.) */
    private boolean collapsed() { return box != page && box.getVisibility() == View.GONE; }

    private void countSwitch(boolean on) {
        groupTotal++;
        if (on) groupOn++;
        if (groupSummary != null) groupSummary.setText(groupOn + " of " + groupTotal + " switches on");
    }

    private void section(String t) {
        TextView v = text(t.toUpperCase(java.util.Locale.ROOT), 12, ACCENT, true);
        v.setLetterSpacing(0.08f);
        v.setPadding(px(4), px(26), 0, px(6));
        box.addView(v);
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#DADCE0"));
        box.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private GradientDrawable fieldBackground() {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE); g.setStroke(px(2), ACCENT); g.setCornerRadius(px(8));
        return g;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD); g.setCornerRadius(px(12));
        c.setBackground(g);
        c.setPadding(px(14), px(10), px(14), px(10));
        return c;
    }

    private View chipLine(String label, String value, int color) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, px(6), 0, px(6));
        l.addView(text(label, 12, MUTED, false));
        l.addView(text(value, 15, color, true));
        return l;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(px(4), px(10), px(4), px(10));
        return r;
    }

    private void toggle(String title, String desc, boolean on, final Chg c) {
        countSwitch(on);
        if (collapsed()) return;   // AR1: a closed group builds no rows at all (the page opens and rebuilds faster on the slow tablet); its switches are still counted for the header
        LinearLayout r = row();
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(title, 16, INK, false));
        TextView d = text(desc, 13, MUTED, false);
        left.addView(d);
        r.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final Switch sw = new Switch(this);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean isChecked) { c.on(isChecked); rebuild(); }
        });
        r.addView(sw);
        r.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { sw.toggle(); } });
        box.addView(r);
    }

    private void action(String title, String desc, final Runnable run) {
        if (collapsed()) return;   // AR1: a closed group builds no rows at all (the page opens and rebuilds faster on the slow tablet)
        LinearLayout r = row();
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(text(title, 16, ACCENT, true));
        left.addView(text(desc, 13, MUTED, false));
        r.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(text(">", 20, ACCENT, true));
        r.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { run.run(); } });
        box.addView(r);
    }

    private void note(String t) {
        if (collapsed()) return;   // AR1: a closed group builds no rows at all (the page opens and rebuilds faster on the slow tablet)
        TextView v = text(t, 13, MUTED, false);
        v.setPadding(px(4), px(8), px(4), px(8));
        box.addView(v);
    }

    private Button smallButton(String label, final Runnable r) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { r.run(); } });
        return b;
    }

    private void radio(String title, String[] labels, final String[] values, String current, final Str s) {
        if (collapsed()) return;   // AR1: a closed group builds no rows at all (the page opens and rebuilds faster on the slow tablet)
        box.addView(text(title, 14, MUTED, false));
        RadioGroup g = new RadioGroup(this);
        g.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i]); rb.setTextSize(15); rb.setTextColor(INK);
            rb.setId(1000 + i);
            rb.setPadding(px(6), px(8), 0, px(8));
            g.addView(rb);
            if (values[i].equals(current)) rb.setChecked(true);
        }
        g.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int id) { int i = id - 1000; if (i >= 0 && i < values.length) { s.on(values[i]); rebuild(); } }
        });
        box.addView(g);
    }

    private void stepper(String label, final String key, final int step, final int min, final int max, final String unit) {
        if (collapsed()) return;   // AR1: a closed group builds no rows at all (the page opens and rebuilds faster on the slow tablet)
        LinearLayout r = row();
        r.addView(text(label + ":  " + Features.getInt(this, key, 0) + unit, 15, INK, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(smallButton("-", new Runnable() { public void run() { Features.setInt(ControlActivity.this, key, Math.max(min, Features.getInt(ControlActivity.this, key, 0) - step)); rebuild(); } }));
        r.addView(smallButton("+", new Runnable() { public void run() { Features.setInt(ControlActivity.this, key, Math.min(max, Features.getInt(ControlActivity.this, key, 0) + step)); rebuild(); } }));
        box.addView(r);
    }

    /** A row of tick boxes for the quick-settings tiles of one launcher; stored as the comma list SystemUI wants. */
    private void tileChooser(String title, final String key, String def) {
        if (collapsed()) return;   // AR1: a closed group builds no rows at all (the page opens and rebuilds faster on the slow tablet)
        box.addView(text(title, 14, MUTED, false));
        String cur = TileProfile.sanitize(Features.getString(this, key, def), def);
        final Set<String> on = new HashSet<>(Arrays.asList(cur.split(",")));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout line = null;
        for (int i = 0; i < TileProfile.CHOICES.length; i++) {
            if (i % 3 == 0) { line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL); wrap.addView(line); }
            final String t = TileProfile.CHOICES[i];
            CheckBox cb = new CheckBox(this);
            cb.setText(t); cb.setTextSize(14);
            cb.setChecked(on.contains(t));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                    List<String> out = new ArrayList<>();
                    String now = TileProfile.sanitize(Features.getString(ControlActivity.this, key, "rotation"), "rotation");
                    Set<String> sel = new HashSet<>(Arrays.asList(now.split(",")));
                    if (isChecked) sel.add(t); else sel.remove(t);
                    for (String c : TileProfile.CHOICES) if (sel.contains(c)) out.add(c);
                    for (String c : sel) if (!out.contains(c)) out.add(c);   // keep any custom spec that is not in the list
                    Features.setString(ControlActivity.this, key, out.isEmpty() ? "rotation" : android.text.TextUtils.join(",", out));
                    TileProfile.apply(ControlActivity.this, Mode.get(ControlActivity.this));
                }
            });
            line.addView(cb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        box.addView(wrap);
    }

    private void goHome() {
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
