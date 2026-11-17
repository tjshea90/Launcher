package com.tj.launcher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * CG1c: how has Android compiled THIS app? Reads the "Dexopt state:" block of {@code dumpsys package com.tj.launcher} (the app holds the DUMP grant
 * from the adb setup) on a short-lived background thread and remembers the two words it prints, so Launcher control and the tablet report can show
 * them without anyone running adb. See {@link DexoptState} for why this matters (a sideloaded APK is only verified at install; the idle job compiles
 * it later; every update starts again). No polling: one look 20 s after a process start when the stored one is stale, one when Launcher control opens.
 */
final class DexoptWatch {
    private static final String K_STATUS = "dexopt_status", K_REASON = "dexopt_reason", K_CODE = "dexopt_code", K_AT = "dexopt_at";
    private static final long MAX_AGE_MS = 6L * 3600_000L;     // the idle job runs about once a night: a look every 6 h at most
    private static final long LIMIT_MS = 10_000L;               // a hung dumpsys costs one thread for 10 s, never more
    private static final java.util.concurrent.atomic.AtomicBoolean RUNNING = new java.util.concurrent.atomic.AtomicBoolean(false);

    private DexoptWatch() { }

    /** Called from {@code Application.onCreate} of the MAIN process: a look 20 s later, off the main thread, only when the stored one is stale. */
    static void refreshSoon(final Context c) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { @Override public void run() { refreshAsync(app, false); } }, 20_000L);
    }

    /** Takes a new reading on a background thread unless one is running or (when not forced) the stored one is still fresh. */
    static void refreshAsync(final Context c, final boolean force) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        try {
            if (!force && !DexoptState.stale(Features.getInt(app, K_CODE, 0), Features.getLong(app, K_AT, 0L), versionCode(app), System.currentTimeMillis(), MAX_AGE_MS)) return;
            if (!RUNNING.compareAndSet(false, true)) return;
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        DexoptState s = read(app.getPackageName(), LIMIT_MS);
                        if (s.found) {   // a failed look keeps the old reading (and its age), so the next start tries again
                            Features.setStringAsyncIfChanged(app, K_STATUS, s.status);
                            Features.setStringAsyncIfChanged(app, K_REASON, s.reason);
                            Features.setLongAsync(app, K_AT, System.currentTimeMillis());
                            Features.setIntAsync(app, K_CODE, versionCode(app));
                        }
                    } catch (Throwable ignored) {
                    } finally { RUNNING.set(false); }
                }
            }, "TjDexopt");
            t.setDaemon(true);
            t.start();
        } catch (Throwable ignored) { RUNNING.set(false); }
    }

    /** The stored reading; {@link DexoptState#NONE} when none belongs to THIS version. */
    static DexoptState stored(Context c) {
        try {
            if (Features.getInt(c, K_CODE, 0) != versionCode(c)) return DexoptState.NONE;
            return DexoptState.of(Features.getString(c, K_STATUS, ""), Features.getString(c, K_REASON, ""));
        } catch (Throwable t) { return DexoptState.NONE; }
    }

    /** True when the shell compile ("speed") or "everything" is what Android reports for this version: the Setup item then counts as done. */
    static boolean full(Context c) { return stored(c).full(); }

    /** One sentence for Launcher control and the tablet report. */
    static String describe(Context c) {
        DexoptState s = stored(c);
        if (!s.found) return "not known yet (needs the DUMP grant from the adb setup; it is read again 20 s after a start and whenever this screen opens)";
        return s.line() + ": " + s.advice();
    }

    private static int versionCode(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode; } catch (Throwable t) { return -1; }
    }

    /** Runs {@code dumpsys package <pkg>} and parses its dexopt block. Blocks up to {@code limitMs}. Never throws. */
    static DexoptState read(String pkg, long limitMs) {
        Process proc = null;
        final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);
        try {
            proc = new ProcessBuilder("dumpsys", "package", pkg).redirectErrorStream(true).start();
            final Process p = proc;
            final long end = SystemClock.elapsedRealtime() + limitMs;
            Thread watchdog = new Thread(new Runnable() {
                @Override public void run() {
                    while (!finished.get() && SystemClock.elapsedRealtime() < end) { try { Thread.sleep(50); } catch (InterruptedException e) { return; } }
                    if (!finished.get()) p.destroy();
                }
            }, "TjDexoptWatch");
            watchdog.setDaemon(true);
            watchdog.start();
            StringBuilder block = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()), 16 * 1024);
            String l; int n = 0, after = 0; boolean inBlock = false, seenPkg = false;
            while ((l = r.readLine()) != null && ++n < 60_000) {
                if (!inBlock) { if (l.contains("Dexopt state:")) inBlock = true; else continue; }
                block.append(l).append('\n');
                if (l.trim().equals("[" + pkg + "]")) seenPkg = true;
                if (seenPkg && ++after > 16) break;
            }
            finished.set(true);
            return DexoptState.parse(block.toString(), pkg);
        } catch (Throwable t) {
            return DexoptState.NONE;
        } finally {
            finished.set(true);
            if (proc != null) { try { proc.destroy(); } catch (Throwable ignored) { } }
        }
    }
}
