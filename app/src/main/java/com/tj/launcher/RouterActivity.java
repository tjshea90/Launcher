package com.tj.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * The ONLY activity that answers the HOME intent. Invisible: decides at once where Home goes and finishes.
 * CUSTOM -> our Launcher3. QUICKSTEP -> the stock launcher, or (CV1, the one launcher, release default) our Launcher3 in its unlocked profile; anything going wrong -> the stock launcher.
 * Fail-open rule: if our Launcher cannot be started for any reason, the stock launcher is started instead, so
 * pressing Home can never lead to a dead end.
 */
public class RouterActivity extends Activity {
    private static final String TAG = "TjRouter";
    /** N3: the accessibility helper starts the router while the lock screen covers everything, to put a stack in front BEFORE the unlock. Not a Home press: never counted, never a double-Home. */
    public static final String EXTRA_PREARM = "tj_prearm";           // "custom" | "stock"
    public static final String HOME_TYPE_KEY = "home_type_launcher";   // B1 switch, default ON (Tj chose full hiding, 2026-11-04, knowing the crash cost)

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        handleHome();
    }

    /** A second Home press that arrives while this invisible instance is still alive is delivered here, not to onCreate: handle it the same way. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleHome();
    }

    private void handleHome() {
        try {
            route(getIntent());
        } catch (Throwable t) {
            Log.e(TAG, "route failed, falling back to stock launcher", t);
            try { startStock(); } catch (Throwable ignored) { }
        }
        finish();
        overridePendingTransition(0, 0);
    }

    private void route(Intent in) {
        ProfileSession.checkBoot(this);   // N7: a restart ends a Quickstep session
        DemoModeProfile.cleanupOnce(this);   // AB1: the retired retail-demo option never stays switched on (one settings read, once)
        KeepAliveService.ensure(this);    // AA2: the process stays in memory (no-op when it already runs or the switch is off)
        String pre = in == null ? null : in.getStringExtra(EXTRA_PREARM);
        LockLog.add(this, "ROUTER", (pre == null ? "home press" : "prepare " + pre) + ", mode " + Mode.get(this));   // AC1/AC7: the persistent lock-screen log shows whether a start made by the helper really arrived here
        if ("stock".equals(pre)) {   // the Quickstep world's home: the stock launcher, or (CV1, the one launcher) OUR Launcher3 in its unlocked profile
            if (Unified.active(this) && !Mode.isCustom(this) && Unified.home(this) == UnifiedRules.Home.OURS && startOurs(false, false)) return;   // review CV1-7: a late "stock" start after the mode went back to custom is the stock home exactly as before
            startStock();
            return;
        }
        boolean prearm = "custom".equals(pre);
        // Double-press Home while the stock launcher is active = back to the custom launcher (Tj: "a quick way to go back").
        // This router answers every Home press, so no permission or accessibility is needed for it.
        long now = System.currentTimeMillis();
        long last = Mode.lastHomePressMs(this), last2 = Mode.homePressBeforeLastMs(this);
        if (!prearm) Mode.recordHomePress(this, now);
        boolean returnedByDoubleHome = false;
        // L10 failsafe: with the accessibility helper OFF no unlock handshake can work, so three quick Home presses let Tj out to Quickstep. Helper ON: ignored.
        if (!prearm && ProfileSession.homeFailsafe(TjAccessibilityService.get() != null, Mode.isCustom(this), now, last2, last)) {
            Mode.set(this, Mode.QUICKSTEP);
            android.widget.Toast.makeText(getApplicationContext(), "Accessibility helper is off: switched to Quickstep", android.widget.Toast.LENGTH_LONG).show();
        }
        if (!prearm && !Mode.isCustom(this) && HomeDoublePress.isDouble(last, now)) {
            CrashGuard.clearTripped(this);
            Mode.set(this, Mode.CUSTOM);
            returnedByDoubleHome = true;
        }
        // AC1 safety net: a Home press in a Quickstep session that outlived a lock (the helper was away at the unlock, so nobody ended it): the plain-PIN rule applies now.
        if (!prearm && !Mode.isCustom(this) && UsageGap.sessionEndedWhileAway(this)) {
            LockLog.add(this, "RECONCILE", "Home press: the keyguard was shown after the Quickstep session began and no handshake was confirmed: ending the session like a plain unlock");
            Mode.set(this, Mode.CUSTOM);
            returnedByDoubleHome = true;   // the same clean-up as any return: Quickstep's apps closed, notifications cleared, radios off
        }
        if (prearm || Unified.home(this) == UnifiedRules.Home.OURS) {   // CV1: custom always; the Quickstep world too while the one launcher is in force
            if (startOurs(prearm, returnedByDoubleHome && !prearm)) return;
        }
        startStock();
    }

    /**
     * Starts OUR Launcher3 (custom profile in the custom world, unlocked profile in the Quickstep world: the profile follows the mode, see {@link HomeProfile}). Returns false when it could not be started
     * (the caller then falls back to the stock launcher, and the stray guards stand down for 20 s).
     */
    private boolean startOurs(boolean prearm, boolean returnedByDoubleHome) {
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;   // V4: also excluded (belt and braces)
        // B1 (switch, default ON: Tj's decision, 2026-11-04): an IMPLICIT MAIN + SECONDARY_HOME intent (no component) makes Android create a HOME-type task, which is never listed in Recents (not even as the
        // most recent task). COST (found by the crash-breaker emulator test): if the launcher process ever crashes, Android clears our default-home setting
        // (AppErrors -> clearPackagePreferredForHomeActivities) and the next Home press shows the "choose a Home app" dialog. Tj accepted that cost.
        Intent i = Features.get(this, HOME_TYPE_KEY, true)
                ? new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_SECONDARY_HOME).setPackage(getPackageName()).addFlags(flags)
                : new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(this, "com.android.launcher3.Launcher")).addFlags(flags);
        if (TjHooks.launcherWasForeground()) i.putExtra(TjHooks.EXTRA_HOME_WHILE_ON_HOME, true);
        try {
            try {
                startActivity(i);
            } catch (android.content.ActivityNotFoundException nf) {
                // the filter is missing (should not happen): fall back to the explicit launch we always used
                Log.e(TAG, "SECONDARY_HOME launch not resolvable, explicit launch", nf);
                Intent e = new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(this, "com.android.launcher3.Launcher")).addFlags(flags);
                if (TjHooks.launcherWasForeground()) e.putExtra(TjHooks.EXTRA_HOME_WHILE_ON_HOME, true);
                startActivity(e);
            }
            // Tj: coming back by double Home also closes apps, clears notifications, turns Wi-Fi/Bluetooth off and opens Solitaire (ReturnReset).
            if (returnedByDoubleHome) ReturnReset.run(this);
            // O3: the lock-screen pre-arm needs the target app (Solitaire) started AFTER the custom home, from this same activity, so the order cannot flip (starting both from the helper raced: the home won).
            if (prearm && ProfileSession.prearm(this)) ReturnReset.startTargetBehindLock(this);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "our launcher failed to start", t);
            Mode.markFailOpen(this, System.currentTimeMillis() + 20_000);   // the stray-Quickstep guard must not fight this fallback
            return false;
        }
    }

    private void startStock() {
        ComponentName stock = StockLauncher.find(this);
        if (stock == null) { Log.e(TAG, "no stock launcher found"); return; }
        int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;
        // C5 (Tj: "Quickstep must never appear in Recents"): an EXPLICIT component makes Android create an ordinary task for it, which Recents lists as "Quickstep".
        // An implicit HOME intent restricted to the stock package (no component) is a HOME-type task, which is never listed.
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(stock.getPackageName()).addFlags(flags));
        } catch (android.content.ActivityNotFoundException nf) {
            Log.e(TAG, "implicit stock HOME launch failed, explicit launch", nf);
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setComponent(stock).addFlags(flags));
        }
    }
}
