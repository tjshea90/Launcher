package com.tj.launcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CG1c (Tj, 2026-11-17: "make android compile/precompile it fully on install or update"): how is THIS app's code compiled right now?
 *
 * Android 10 has no manifest switch for "compile at install". A sideloaded APK is only VERIFIED when it is installed; the idle maintenance job
 * compiles it later ("speed-profile": only the methods named in a profile), and every update throws that code away. The baseline profile in the
 * APK ({@code app/src/main/baseline-prof.txt}) gives the job its profile from the first start; {@code cmd package compile -m speed -f} (shell
 * only: the Setup item COMPILE_SELF) compiles everything at once. This class turns the text of {@code dumpsys package <pkg>} (the "Dexopt state:"
 * block; the app holds the DUMP grant) into the two words Android prints there, so Launcher control and the tablet report can SAY which one it is
 * instead of guessing. Pure: text in, facts out (JVM test: DexoptStateTest).
 *
 * AOSP 10 prints, per code path and instruction set: {@code arm: [status=speed-profile] [reason=bg-dexopt]}.
 */
final class DexoptState {
    final boolean found;
    /** speed, speed-profile, everything, quicken, verify, run-from-apk, extract, assume-verified ... or "" when not found. */
    final String status;
    /** install, bg-dexopt, cmdline, first-boot, boot, ab-ota, inactive, shared ... or "" when not printed. */
    final String reason;

    static final DexoptState NONE = new DexoptState(false, "", "");

    private static final Pattern STATUS = Pattern.compile("\\[status=([A-Za-z0-9_.\\-]+)\\](?:\\s*\\[reason=([A-Za-z0-9_.\\-]+)\\])?");
    private static final int LOOK_AHEAD_LINES = 14;   // header line, "path:" line, one line per instruction set; a split APK adds more paths

    private DexoptState(boolean found, String status, String reason) { this.found = found; this.status = status; this.reason = reason; }

    /**
     * Reads the "Dexopt state:" block of {@code dumpsys package} (or of {@code dumpsys package dexopt}) for {@code pkg}. The BASE code path's first
     * status line wins (split APKs print their own lines after it). Never throws; unknown or missing text gives {@link #NONE}.
     */
    static DexoptState parse(String dump, String pkg) {
        if (dump == null || pkg == null || pkg.isEmpty()) return NONE;
        String[] lines = dump.split("\r?\n");
        boolean inBlock = false;
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (!inBlock) { if (l.contains("Dexopt state:")) inBlock = true; continue; }
            if (l.trim().equals("[" + pkg + "]")) {
                for (int j = i + 1; j < lines.length && j <= i + LOOK_AHEAD_LINES; j++) {
                    String m = lines[j];
                    if (m.trim().startsWith("[") && m.trim().endsWith("]") && !m.contains("status=")) break;   // the next package's header
                    Matcher mm = STATUS.matcher(m);
                    if (mm.find()) return new DexoptState(true, mm.group(1), mm.group(2) == null ? "" : mm.group(2));
                }
                return NONE;
            }
        }
        return NONE;
    }

    /**
     * Should the stored reading be taken again? Yes when none was ever stored, when it belongs to another version (an update throws the compiled
     * code away, so an old "speed" says nothing), when it is older than {@code maxAgeMs} (the idle job may have compiled since) or when the clock
     * went backwards. Pure (JVM test).
     */
    static boolean stale(int storedCode, long storedAtMs, int currentCode, long nowMs, long maxAgeMs) {
        if (storedAtMs <= 0 || storedCode != currentCode) return true;
        long age = nowMs - storedAtMs;
        return age < 0 || age > maxAgeMs;
    }

    /**
     * CG1 review C: may a look be tried now? With a reading for THIS version the age rule above decides. WITHOUT one (an app uid is refused: no DUMP / PACKAGE_USAGE_STATS grant) the same refusal would be repeated at every
     * process start and every Launcher control open, a dumpsys process and a watchdog thread each: wait {@code failGapMs} after the last try, {@code forcedGapMs} when the person opened Launcher control. A clock that went
     * backwards allows the try. Pure (JVM test).
     */
    static boolean tryDue(boolean haveReading, long lastTryMs, long nowMs, boolean forced, long failGapMs, long forcedGapMs) {
        if (haveReading) return true;
        long gap = forced ? forcedGapMs : failGapMs;
        return lastTryMs <= 0 || nowMs < lastTryMs || nowMs - lastTryMs >= gap;
    }

    /** Rebuilds a reading from the two stored words (see DexoptWatch). */
    static DexoptState of(String status, String reason) {
        if (status == null || status.isEmpty()) return NONE;
        return new DexoptState(true, status, reason == null ? "" : reason);
    }

    /** True when the profiled (hot) code is compiled: speed-profile, speed or everything. verify / quicken / run-from-apk are not. */
    boolean compiled() { return found && (status.equals("speed-profile") || status.equals("speed") || status.equals("everything")); }

    /** True when EVERY method is compiled (the shell compile "speed" / "everything"), not just the profiled ones. */
    boolean full() { return found && (status.equals("speed") || status.equals("everything")); }

    /** One line for Launcher control and the tablet report, e.g. "speed-profile (bg-dexopt)". */
    String line() {
        if (!found) return "not known (no DUMP grant, or Android printed no dexopt state)";
        return reason.isEmpty() ? status : status + " (" + reason + ")";
    }

    /** What to tell the user, in plain words. */
    String advice() {
        if (!found) return "";
        if (full()) return "fully compiled";
        if (compiled()) return "compiled from the profile; the Setup compile makes it complete";
        return "not compiled yet: it runs in the interpreter until Android's idle job (device idle + charging) or the Setup compile does it";
    }
}
