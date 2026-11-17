package com.tj.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CG1f (Tj, 2026-11-17: "investigate why and how the quickstep launcher slowly builds in ram usage over time ... Figure out what features in the custom launcher app can add to the quickstep ram usage, even if indirectly").
 * A recorder that needs no cable: with the DUMP grant from the adb setup the app reads {@code dumpsys meminfo com.android.launcher3} (the STOCK launcher's process, the one Settings calls "Quickstep") on a
 * short-lived background thread, at most every {@link #GAP_MS} and only while the tablet is in use (30 s after a wake, when Launcher control opens, when a tablet report is written), and keeps a ring of
 * {@link #KEEP} compact samples. Each sample holds what tells the candidate causes of the growth apart (research/CG1F_QUICKSTEP_AOSP_ANGLE_2026-11-17.md): the PSS parts (java heap, native heap, code, graphics,
 * and the GL / EGL / graphics-device rows that the "Graphics" total hides) and the OBJECT counts (Activities, Views, ViewRootImpl: an Activity of the stock launcher that is pinned for good by the Go Recents view's
 * listener shows as one more Activity and ~100-140 more Views every time the stock launcher is re-created). The tablet report prints the ring and a reading of it. Pure parser and ring here (JVM-tested);
 * {@link #refreshAsync} does the dumpsys call.
 */
final class QsMemory {
    /** Switch (hidden): ON in the shipped build, OFF in a debug build (older emulator sections must not see extra dumpsys calls); the section {@code qm} switches it on. */
    static final String KEY = "qs_memory_log";
    static final String RING_KEY = "qs_mem_ring", LAST_AT_KEY = "qs_mem_last_at";
    static final String PACKAGE = "com.android.launcher3";
    static final long GAP_MS = 25L * 60_000L;          // a sample every 25 minutes at most (the process grows over hours and days)
    static final long FORCED_GAP_MS = 30_000L;         // opening Launcher control twice in a row does not call dumpsys twice
    static final int KEEP = 96;                        // about two days of use
    private static final long LIMIT_MS = 10_000L;      // a hung dumpsys costs one thread for 10 s, never more
    private static final int MAX_CHARS = 96 * 1024;
    private static final java.util.concurrent.atomic.AtomicBoolean RUNNING = new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final Object RING_LOCK = new Object();

    private QsMemory() { }

    static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** One reading of the stock launcher's process. Memory in kB, counts as printed, -1 = the line was not in the dump. */
    static final class Sample {
        boolean found;
        long atMs;
        int pid = -1, pssKb = -1, javaKb = -1, nativeKb = -1, codeKb = -1, stackKb = -1, graphicsKb = -1, privOtherKb = -1, systemKb = -1;
        int eglKb = -1, glKb = -1, gfxDevKb = -1, unknownKb = -1;
        int views = -1, viewRoots = -1, activities = -1, appContexts = -1, assets = -1, binders = -1;
    }

    private static final Pattern PID = Pattern.compile("MEMINFO in pid (\\d+)");
    private static final Pattern SUMMARY = Pattern.compile("(?m)^\\s*(Java Heap|Native Heap|Code|Stack|Graphics|Private Other|System|TOTAL):\\s+(\\d+)");
    private static final Pattern ROW = Pattern.compile("(?m)^\\s*(EGL mtrack|GL mtrack|Gfx dev|Unknown)\\s+(\\d+)");
    private static final Pattern OBJ = Pattern.compile("(Views|ViewRootImpl|AppContexts|Activities|Assets|Local Binders):\\s+(\\d+)");

    /** Reads what {@code dumpsys meminfo <package>} printed (Android 10 layout; unknown lines are ignored, a missing one stays -1). {@code found} is false when it is not a meminfo dump of a running process. */
    static Sample parse(String dump, long atMs) {
        Sample s = new Sample();
        s.atMs = atMs;
        if (dump == null) return s;
        Matcher m = PID.matcher(dump);
        if (!m.find()) return s;
        s.pid = toInt(m.group(1));
        m = SUMMARY.matcher(dump);
        while (m.find()) {
            int v = toInt(m.group(2));
            switch (m.group(1)) {
                case "Java Heap": if (s.javaKb < 0) s.javaKb = v; break;
                case "Native Heap": if (s.nativeKb < 0) s.nativeKb = v; break;
                case "Code": if (s.codeKb < 0) s.codeKb = v; break;
                case "Stack": if (s.stackKb < 0) s.stackKb = v; break;
                case "Graphics": if (s.graphicsKb < 0) s.graphicsKb = v; break;
                case "Private Other": if (s.privOtherKb < 0) s.privOtherKb = v; break;
                case "System": if (s.systemKb < 0) s.systemKb = v; break;
                case "TOTAL": if (s.pssKb < 0) s.pssKb = v; break;
                default: break;
            }
        }
        m = ROW.matcher(dump);
        while (m.find()) {
            int v = toInt(m.group(2));
            switch (m.group(1)) {
                case "EGL mtrack": if (s.eglKb < 0) s.eglKb = v; break;
                case "GL mtrack": if (s.glKb < 0) s.glKb = v; break;
                case "Gfx dev": if (s.gfxDevKb < 0) s.gfxDevKb = v; break;
                case "Unknown": if (s.unknownKb < 0) s.unknownKb = v; break;
                default: break;
            }
        }
        m = OBJ.matcher(dump);
        while (m.find()) {
            int v = toInt(m.group(2));
            switch (m.group(1)) {
                case "Views": if (s.views < 0) s.views = v; break;
                case "ViewRootImpl": if (s.viewRoots < 0) s.viewRoots = v; break;
                case "Activities": if (s.activities < 0) s.activities = v; break;
                case "AppContexts": if (s.appContexts < 0) s.appContexts = v; break;
                case "Assets": if (s.assets < 0) s.assets = v; break;
                case "Local Binders": if (s.binders < 0) s.binders = v; break;
                default: break;
            }
        }
        s.found = s.pssKb >= 0;   // a dump without the total is not a reading (the process is gone, or the layout is not the one this reads)
        return s;
    }

    private static int toInt(String v) { try { return Integer.parseInt(v); } catch (NumberFormatException e) { return -1; } }

    /** One compact line for the ring: {@code t=<epoch s>;pid=..;pss=..} with every part, -1 omitted. */
    static String toLine(Sample s) {
        StringBuilder b = new StringBuilder("t=").append(s.atMs / 1000L);
        put(b, "pid", s.pid); put(b, "pss", s.pssKb); put(b, "java", s.javaKb); put(b, "nat", s.nativeKb); put(b, "code", s.codeKb); put(b, "stk", s.stackKb);
        put(b, "gfx", s.graphicsKb); put(b, "oth", s.privOtherKb); put(b, "sys", s.systemKb); put(b, "egl", s.eglKb); put(b, "gl", s.glKb); put(b, "gdev", s.gfxDevKb); put(b, "unk", s.unknownKb);
        put(b, "views", s.views); put(b, "vri", s.viewRoots); put(b, "act", s.activities); put(b, "ctx", s.appContexts); put(b, "ast", s.assets); put(b, "bnd", s.binders);
        return b.toString();
    }

    private static void put(StringBuilder b, String k, int v) { if (v >= 0) b.append(';').append(k).append('=').append(v); }

    /** The inverse of {@link #toLine}; {@code found} is false for a line that is not one. */
    static Sample fromLine(String line) {
        Sample s = new Sample();
        if (line == null) return s;
        for (String kv : line.trim().split(";")) {
            int eq = kv.indexOf('=');
            if (eq <= 0) continue;
            String k = kv.substring(0, eq);
            long v;
            try { v = Long.parseLong(kv.substring(eq + 1)); } catch (NumberFormatException e) { continue; }
            int i = (int) Math.max(-1, Math.min(Integer.MAX_VALUE, v));
            switch (k) {
                case "t": s.atMs = v * 1000L; break;
                case "pid": s.pid = i; break;
                case "pss": s.pssKb = i; break;
                case "java": s.javaKb = i; break;
                case "nat": s.nativeKb = i; break;
                case "code": s.codeKb = i; break;
                case "stk": s.stackKb = i; break;
                case "gfx": s.graphicsKb = i; break;
                case "oth": s.privOtherKb = i; break;
                case "sys": s.systemKb = i; break;
                case "egl": s.eglKb = i; break;
                case "gl": s.glKb = i; break;
                case "gdev": s.gfxDevKb = i; break;
                case "unk": s.unknownKb = i; break;
                case "views": s.views = i; break;
                case "vri": s.viewRoots = i; break;
                case "act": s.activities = i; break;
                case "ctx": s.appContexts = i; break;
                case "ast": s.assets = i; break;
                case "bnd": s.binders = i; break;
                default: break;
            }
        }
        s.found = s.atMs > 0 && s.pssKb >= 0;
        return s;
    }

    /** Appends a line, newest last, keeping at most {@code keep} (the oldest go first). */
    static String addLine(String ring, String line, int keep) {
        String[] old = ring == null || ring.isEmpty() ? new String[0] : ring.split("\n");
        int from = Math.max(0, old.length + 1 - Math.max(1, keep));
        StringBuilder b = new StringBuilder();
        for (int i = from; i < old.length; i++) if (!old[i].isEmpty()) b.append(old[i]).append('\n');
        return b.append(line).toString();
    }

    /** May a sample be taken now? {@code lastAtMs} = wall clock of the last one (0 = never); a clock that went backwards counts as due. */
    static boolean due(long lastAtMs, long nowMs, boolean forced) {
        long gap = forced ? FORCED_GAP_MS : GAP_MS;
        return lastAtMs <= 0 || nowMs < lastAtMs || nowMs - lastAtMs >= gap;
    }

    private static String mb(int kb) { return kb < 0 ? "?" : String.valueOf(Math.round(kb / 1024f)); }
    private static String n(int v) { return v < 0 ? "?" : String.valueOf(v); }
    private static String clock(long ms) { return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date(ms)); }

    /**
     * What the ring says, for the tablet report and Launcher control: how many samples over how long, the newest reading with its parts, the change since the first reading of the SAME process (a new pid = the
     * stock launcher was restarted, so the count starts again), and the reading that goes with each candidate cause.
     */
    static String describe(String ring, long nowMs) {
        String[] lines = ring == null || ring.isEmpty() ? new String[0] : ring.split("\n");
        java.util.ArrayList<Sample> all = new java.util.ArrayList<>();
        for (String l : lines) { Sample s = fromLine(l); if (s.found) all.add(s); }
        if (all.isEmpty()) return "no sample yet (it needs the DUMP grant from the adb setup; the first one is taken 30 s after a wake, when Launcher control opens, or when a report is written)";
        Sample last = all.get(all.size() - 1);
        int firstSame = all.size() - 1;
        while (firstSame > 0 && all.get(firstSame - 1).pid == last.pid) firstSame--;
        Sample first = all.get(firstSame);
        long spanMin = Math.max(0, (last.atMs - first.atMs) / 60_000L);
        StringBuilder b = new StringBuilder();
        b.append(all.size()).append(" sample").append(all.size() == 1 ? "" : "s").append(", newest ").append(Math.max(0, (nowMs - last.atMs) / 60_000L)).append(" min ago; stock launcher process ").append(last.pid)
                .append(": ").append(mb(last.pssKb)).append(" MB (java ").append(mb(last.javaKb)).append(", native ").append(mb(last.nativeKb)).append(", code ").append(mb(last.codeKb))
                .append(", graphics ").append(mb(last.graphicsKb)).append(" [EGL ").append(mb(last.eglKb)).append(", GL ").append(mb(last.glKb)).append(", gfx dev ").append(mb(last.gfxDevKb)).append("], other ").append(mb(last.privOtherKb))
                .append(", system ").append(mb(last.systemKb)).append("); objects: Activities ").append(n(last.activities)).append(", Views ").append(n(last.views)).append(", ViewRootImpl ").append(n(last.viewRoots)).append(", AppContexts ").append(n(last.appContexts));
        if (all.size() - firstSame >= 2) {
            b.append("; since the first sample of this process (").append(clock(first.atMs)).append(", ").append(spanMin).append(" min): PSS ").append(signed(last.pssKb - first.pssKb)).append(" kB");
            if (spanMin >= 30) b.append(" = ").append(signed((int) Math.round((last.pssKb - first.pssKb) * 60.0 / spanMin))).append(" kB per hour");
            b.append(", Activities ").append(first.activities < 0 || last.activities < 0 ? "?" : signed(last.activities - first.activities)).append(", Views ").append(first.views < 0 || last.views < 0 ? "?" : signed(last.views - first.views));
            b.append(". Reading: ").append(reading(first, last));
        } else if (firstSame > 0) {
            b.append("; the process is new since the sample before (pid ").append(all.get(firstSame - 1).pid).append(" -> ").append(last.pid).append(": the stock launcher was restarted)");
        }
        // the last few readings, oldest first, so a growth curve can be read without a graph
        int from = Math.max(0, all.size() - 8);
        b.append("; last readings (time MB act views gfx): ");
        for (int i = from; i < all.size(); i++) { Sample s = all.get(i); b.append(clock(s.atMs).substring(6)).append(' ').append(mb(s.pssKb)).append(' ').append(n(s.activities)).append(' ').append(n(s.views)).append(' ').append(mb(s.graphicsKb)).append(i + 1 < all.size() ? " | " : ""); }
        return b.toString();
    }

    private static String signed(int v) { return (v >= 0 ? "+" : "") + v; }

    /** Which candidate cause the change between two readings of one process points at (CG1F report: R1 pinned activities, R2 graphics memory, R3 heap). A reading, never a proof. */
    static String reading(Sample first, Sample last) {
        int dAct = first.activities < 0 || last.activities < 0 ? 0 : last.activities - first.activities;
        int dViews = first.views < 0 || last.views < 0 ? 0 : last.views - first.views;
        int dPss = last.pssKb - first.pssKb;
        int dGfx = (last.graphicsKb < 0 || first.graphicsKb < 0) ? 0 : last.graphicsKb - first.graphicsKb;
        int dJava = (last.javaKb < 0 || first.javaKb < 0) ? 0 : last.javaKb - first.javaKb;
        int dNat = (last.nativeKb < 0 || first.nativeKb < 0) ? 0 : last.nativeKb - first.nativeKb;
        if (dPss < 4096) return "no real growth (under 4 MB).";
        if (dAct >= 2 || dViews >= 200) return "more stock Activities / Views are alive (" + signed(dAct) + " / " + signed(dViews) + "): the stock launcher was re-created and the old screens were never freed (R1: the Go Recents view's listener pins every Activity).";
        if (dGfx >= dPss / 2) return "mostly graphics memory (" + signed(dGfx) + " kB of " + signed(dPss) + "), objects flat: GPU / EGL buffers that are never trimmed (R2).";
        if (dJava + dNat >= dPss / 2) return "mostly heap (java " + signed(dJava) + ", native " + signed(dNat) + " kB), objects flat: memory the runtime keeps instead of returning (R3).";
        return "grew by " + signed(dPss) + " kB without one dominant part: look at the parts above.";
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the dumpsys call

    /** The stored ring ("" = none). */
    static String ring(Context c) { try { return Features.getString(c, RING_KEY, ""); } catch (Throwable t) { return ""; } }

    /** One sentence for Launcher control and the tablet report. */
    static String describe(Context c) { return describe(ring(c), System.currentTimeMillis()); }

    /** Takes a reading on a background thread when the switch is on, the DUMP grant is there and the gap since the last one allows it. Never throws, never blocks the caller. */
    static void refreshAsync(final Context c, final boolean forced) {
        final Context app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        try {
            if (!Features.get(app, KEY, defaultOn(ReturnReset.isDebuggable(app)))) return;
            if (app.checkSelfPermission(android.Manifest.permission.DUMP) != PackageManager.PERMISSION_GRANTED) return;
            if (!due(Features.getLong(app, LAST_AT_KEY, 0L), System.currentTimeMillis(), forced)) return;
            if (!RUNNING.compareAndSet(false, true)) return;
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    try {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        long now = System.currentTimeMillis();
                        Sample s = parse(read(PACKAGE, LIMIT_MS), now);
                        if (s.found) {
                            synchronized (RING_LOCK) { Features.setStringAsync(app, RING_KEY, addLine(ring(app), toLine(s), KEEP)); }
                            Features.setLongAsync(app, LAST_AT_KEY, now);
                        }
                    } catch (Throwable ignored) {
                    } finally { RUNNING.set(false); }
                }
            }, "TjQsMem");
            t.setDaemon(true);
            t.start();
        } catch (Throwable ignored) { RUNNING.set(false); }
    }

    /** Runs {@code dumpsys meminfo <pkg>}; blocks up to {@code limitMs}; "" when it fails. Never throws. */
    static String read(String pkg, long limitMs) {
        Process proc = null;
        final java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean(false);
        try {
            proc = new ProcessBuilder("dumpsys", "meminfo", pkg).redirectErrorStream(true).start();
            final Process p = proc;
            final long end = SystemClock.elapsedRealtime() + limitMs;
            Thread watchdog = new Thread(new Runnable() {
                @Override public void run() {
                    while (!finished.get() && SystemClock.elapsedRealtime() < end) { try { Thread.sleep(50); } catch (InterruptedException e) { return; } }
                    if (!finished.get()) p.destroy();
                }
            }, "TjQsMemWatch");
            watchdog.setDaemon(true);
            watchdog.start();
            StringBuilder out = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try { String l; while ((l = r.readLine()) != null && out.length() < MAX_CHARS) out.append(l).append('\n'); } finally { r.close(); }
            return out.toString();
        } catch (Throwable t) {
            return "";
        } finally {
            finished.set(true);
            if (proc != null) { try { proc.destroy(); } catch (Throwable ignored) { } }
        }
    }
}
