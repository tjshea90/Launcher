package com.tj.launcher;

import android.app.Application;

/**
 * Installs the crash-loop breaker before anything else of ours runs. Does nothing else at start-up (no services, no work at boot, nothing declared in the manifest) except three passive listeners in the MAIN
 * process, each registered once: a content observer on battery saver (AC3), one on the rotation lock and one dynamic power-connected/disconnected receiver (AD4, the hidden entrances; idle unless Tj uses them).
 */
public class TjApplication extends Application {
    /**
     * AD8: how long this process's {@code Application.onCreate} took, and how long after the process was forked it ended (ms). Shown in the tablet report (ReportState), so the start-up cost of the
     * MT8168 / 2 GB tablet is MEASURED there instead of guessed. Plain statics: nothing is written to disk at start-up for them.
     */
    static volatile long startMs = -1, sinceForkMs = -1;

    @Override public void onCreate() {
        long t0 = android.os.SystemClock.uptimeMillis();
        super.onCreate();
        final boolean main = getPackageName().equals(Application.getProcessName());   // the short-lived :wallpaper_chooser / :wallpaper_picker processes have no business with the watchers below
        try { if (main) CrashGuard.install(this); } catch (Throwable ignored) { }   // AO1b: only the launcher's OWN process counts toward the crash breaker: a crash inside the wallpaper picker's process just closes the picker
        try { CrashGuard.clearIfNewer(this); } catch (Throwable ignored) { }   // CBR1: a tripped breaker is cleared by the first start of a NEWER version (a fair try)
        try { if (main) ProcessLife.noteStart(this); } catch (Throwable ignored) { }   // BG1: how often does Android restart this process on the tablet? (one preference write with apply())
        try { if (main) AnimationSpeedup.reconcileAsync(this); } catch (Throwable ignored) { }   // an interrupted reset must never leave the animations off; BF1: and the Quickstep hold is re-decided from the facts (after a kill, a reboot, an update). Main process only: a picker process starting in the middle of a reset must not end it
        try { if (main) SettingsSpoof.syncSoon(this, "process start"); } catch (Throwable ignored) { }   // BN1: the Settings look-alike's drawer entry follows the switch and the mode (after a kill, an update or Clear data); 3 s late, background thread
        try { if (main) SwitchMigration.run(this); } catch (Throwable ignored) { }   // v0.7.3: undo the old breaker reset's "false" values once (all three switches stored as off = its signature)
        try { if (main) EntranceWatcher.start(this); } catch (Throwable ignored) { }   // AD4: the rotation-tile and charger entrances (the page flick is fed by the Launcher touch hook)
        try { if (main) LockQuiet.applyNotificationsAsync(this); } catch (Throwable ignored) { }   // AK1: the lock screen shows no app notifications (setting written / put back by the switch)
        try { if (main) BatterySaverGuard.start(this); } catch (Throwable ignored) { }   // AC3: watch battery saver for the life of the MAIN process (one observer, no polling)
        try { if (main) LockQuiet.applyLockRulesEarly(this); } catch (Throwable ignored) { }   // AW2 / AW3: the first lock screen after a restart is quiet before the accessibility helper has bound (about 1.5 min after boot)
        try {
            startMs = android.os.SystemClock.uptimeMillis() - t0;
            sinceForkMs = android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime();
        } catch (Throwable ignored) { }
    }
}
