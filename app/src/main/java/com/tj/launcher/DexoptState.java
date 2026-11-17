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
