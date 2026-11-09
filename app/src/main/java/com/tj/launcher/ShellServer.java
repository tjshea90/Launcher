package com.tj.launcher;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * N6: the shell-user helper. NOT part of the app's own process: started by `adb shell ... app_process ... com.tj.launcher.ShellServer` (see {@link ShellProtocol#START_ONE_LINER}),
 * so it runs with the shell's powers (force-stop). It dies with the next reboot (nothing here survives a restart: that is Android's rule for adb-started processes) and can be stopped with
 * {@link ShellProtocol#STOP_LINE}. Only our app may talk to it: every connection's peer uid is compared with our app's uid, and the commands are the two in {@link ShellProtocol}.
 * No Context, no Looper: plain sockets and `am`.
 */
public final class ShellServer {
    private ShellServer() {}

    public static void main(String[] args) throws Exception {
        final int appUid = lookupUid();
        if (appUid < 0) { System.out.println("tj-shell: cannot find the uid of " + ShellProtocol.SELF + " (is it installed?)"); return; }
        startRecorder();   // HLP1: a helper that dies leaves no trace (its output goes to /dev/null): this writes one
        boolean any = false;
        try {
            final LocalServerSocket server = new LocalServerSocket(ShellProtocol.SOCKET);
            any = true;
            System.out.println("tj-shell: unix socket up for uid " + appUid);
            new Thread(new Runnable() { @Override public void run() {
                try { while (true) { final LocalSocket c = server.accept(); new Thread(new Runnable() { @Override public void run() { serveUnix(c, appUid); } }, "tj-shell-conn").start(); } }
                catch (Throwable t) { System.out.println("tj-shell: unix accept ended: " + t); }
            } }, "tj-shell-unix").start();
        } catch (java.io.IOException e) { System.out.println("tj-shell: unix socket not available (" + e.getMessage() + ")"); }
        java.net.ServerSocket tcp = null;
        for (int port = ShellProtocol.TCP_PORT_FIRST; port <= ShellProtocol.TCP_PORT_LAST && tcp == null; port++) {
            try { tcp = new java.net.ServerSocket(port, 4, java.net.InetAddress.getByName("127.0.0.1")); } catch (java.io.IOException ignored) { }
        }
        if (tcp != null) { any = true; System.out.println("tj-shell: tcp up on 127.0.0.1:" + tcp.getLocalPort()); }
        if (!any) { System.out.println("tj-shell: nothing to listen on (already running?)"); return; }
        if (tcp == null) { Thread.sleep(Long.MAX_VALUE); return; }
        while (true) {
            final java.net.Socket c = tcp.accept();
            final int sport = tcp.getLocalPort();
            new Thread(new Runnable() { @Override public void run() { serveTcp(c, appUid, sport); } }, "tj-shell-tcp").start();
        }
    }

    private static void serveUnix(LocalSocket c, int appUid) {
        try {
            c.setSoTimeout(5000);
            int peer = c.getPeerCredentials().getUid();
            handle(c.getInputStream(), c.getOutputStream(), peer == appUid);
        } catch (Throwable t) {
            System.out.println("tj-shell: unix connection failed: " + t);
        } finally {
            try { c.close(); } catch (Throwable ignored) { }
        }
    }

    /** Loopback TCP has no peer-credential call; the owner uid of the client socket is read from /proc/net/tcp(6), which only the shell user can read on Android 10. */
    private static void serveTcp(java.net.Socket c, int appUid, int serverPort) {
        try {
            c.setSoTimeout(5000);
            int uid = ShellProtocol.uidForPorts(readFile("/proc/net/tcp") + "\n" + readFile("/proc/net/tcp6"), c.getPort(), serverPort);
            handle(c.getInputStream(), c.getOutputStream(), uid == appUid);
        } catch (Throwable t) {
            System.out.println("tj-shell: tcp connection failed: " + t);
        } finally {
            try { c.close(); } catch (Throwable ignored) { }
        }
    }

    private static String readFile(String path) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new java.io.FileReader(path));
            String l;
            while ((l = r.readLine()) != null) sb.append(l).append('\n');
            r.close();
            return sb.toString();
        } catch (Throwable t) { return ""; }
    }

    private static void handle(java.io.InputStream is, java.io.OutputStream os, boolean authorised) throws Exception {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(os), true);
        if (!authorised) { out.println("ERR denied"); return; }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        if (ShellProtocol.isPing(line)) { out.println("OK tj-shell 1"); return; }
        if (ShellProtocol.isQuit(line)) { out.println("OK"); out.flush(); System.exit(0); return; }
        String id = ShellProtocol.runId(line);
        if (id != null) {
            String sh = ShellProtocol.runLine(id);
            if (sh == null) { out.println("ERR unknown job"); return; }
            out.println("OK"); out.print(runShell(sh, 20_000)); out.flush(); return;
        }
        if (ShellProtocol.isA11yOn(line)) {
            String cur = runShell("settings get secure enabled_accessibility_services", 5_000).trim();
            String merged = ShellProtocol.mergeA11y(cur);
            String quoted = ShellProtocol.quoteForShell(merged);   // AB1: single-quoted, so "$Inner" in another service's name is never expanded by the shell; an odd list is left alone
            if (quoted == null) { out.println("ERR the accessibility list has an unusual character; nothing was changed"); out.flush(); return; }
            runShell("settings put secure enabled_accessibility_services " + quoted, 5_000);
            runShell("settings put secure accessibility_enabled 1", 5_000);
            out.println("OK"); out.println("accessibility services now: " + runShell("settings get secure enabled_accessibility_services", 5_000).trim()); out.flush(); return;
        }
        if (ShellProtocol.isShots(line)) {
            out.println("OK");
            runShell("mkdir -p " + ShellProtocol.PUBLISH_DIR + "/shots", 5_000);
            int n = 0;
            for (String page : ShellProtocol.SHOT_PAGES) {
                runShell("am start -a android.settings." + page, 8_000);
                try { Thread.sleep(2500); } catch (InterruptedException ignored) { }
                runShell("screencap -p " + ShellProtocol.PUBLISH_DIR + "/shots/" + page + ".png", 15_000);
                runShell("uiautomator dump " + ShellProtocol.PUBLISH_DIR + "/shots/" + page + ".xml", 15_000);
                out.println("shot " + page); out.flush(); n++;
            }
            runShell("input keyevent KEYCODE_HOME", 5_000);
            out.println("done: " + n + " pages"); out.flush(); return;
        }
        if (ShellProtocol.isApks(line)) {
            out.println("OK");
            runShell("mkdir -p " + ShellProtocol.PUBLISH_DIR + "/apk", 5_000);
            for (String pkg : ShellProtocol.APK_PACKAGES) {
                String path = runShell("pm path " + pkg + " | head -1 | cut -d: -f2", 8_000).trim();
                if (path.isEmpty()) { out.println("no apk for " + pkg); continue; }
                String r = runShell("cp " + path + " " + ShellProtocol.PUBLISH_DIR + "/apk/" + pkg + ".apk", 60_000).trim();
                out.println("copied " + pkg + (r.isEmpty() ? "" : " (" + r + ")")); out.flush();
            }
            out.println("done"); out.flush(); return;
        }
        if (ShellProtocol.isKeyTest(line)) {
            out.println("OK");
            keyTest(out);
            return;
        }
        if (ShellProtocol.isProbe(line)) {
            out.println("OK");
            java.io.InputStream ris = ShellServer.class.getClassLoader().getResourceAsStream("probe-commands.txt");
            StringBuilder sb = new StringBuilder();
            if (ris != null) { BufferedReader rr = new BufferedReader(new InputStreamReader(ris)); String l; while ((l = rr.readLine()) != null) sb.append(l).append('\n'); rr.close(); }
            for (ShellProtocol.ProbeLine pl : ShellProtocol.parseProbeList(sb.toString())) {
                if (pl.title) out.println("\n===== " + pl.text);
                else { out.println("$ " + pl.text); out.print(runShell(pl.text, 15_000)); }
            }
            out.flush(); return;
        }
        if (ShellProtocol.isListTasks(line)) {
            out.println("OK");
            for (java.util.Map.Entry<Integer, String> e : ShellProtocol.parseTasks(recentsDump()).entrySet()) out.println(e.getKey() + " " + e.getValue());
            out.flush(); return;
        }
        if (ShellProtocol.isNotifPkgs(line)) {
            out.println("OK");
            for (String pkg : ShellProtocol.parseNotifPackages(runShell("dumpsys notification | grep 'NotificationRecord('", 15_000))) out.println(pkg);
            out.flush(); return;
        }
        List<Integer> rmIds = ShellProtocol.removeTaskIds(line);
        if (rmIds != null) {
            // the deny-list is enforced HERE, on the package the system itself reports for each id: an id can never be used to close the system, the keyboard, this app or the stock launcher
            java.util.Map<Integer, String> now = ShellProtocol.parseTasks(recentsDump());
            int removed = 0, skipped = 0;
            for (int tid : rmIds) {
                String pkg = now.get(tid);
                if (pkg == null || !ShellProtocol.allowed(pkg)) { skipped++; continue; }
                if (removeTask(tid)) removed++; else skipped++;
            }
            out.println("OK " + removed + " " + skipped); return;
        }
        String[] sys = ShellProtocol.setSystem(line);
        if (sys != null) {   // BN1: Mono audio / Audio balance of the Settings look-alike (the allow-list is in ShellProtocol.setSystem; both arguments are validated constants)
            String r = runShell("settings put system " + sys[0] + " " + sys[1], 5_000).trim();
            out.println(r.isEmpty() ? "OK" : "ERR " + r);
            return;
        }
        List<String> pkgs = ShellProtocol.forceStopTargets(line);
        if (pkgs == null) { out.println("ERR unknown command"); return; }
        int stopped = forceStop(pkgs);
        int removed = removeRecentTasks(pkgs);
        out.println("OK " + stopped + " " + removed);
    }

    /**
     * KEYTEST (see ShellProtocol.isKeyTest): a fixed routine, every argument is a constant. ShellProbe holds the "no power menu" status-bar flag for a few seconds (Android clears it by itself when that
     * process ends) while getevent reads the key events of every input device; the person holds the side key for about a second in that window. Prints the key lines between KEYTEST-START and
     * KEYTEST-END, then what ShellProbe answered. Takes about 13 seconds.
     */
    private static void keyTest(PrintWriter out) {
        final List<String> raw = java.util.Collections.synchronizedList(new java.util.ArrayList<String>());
        final StringBuilder probeOut = new StringBuilder();
        Process probe = null, ev = null;
        try {
            String apk = runShell("pm path " + ShellProtocol.SELF + " | head -1 | cut -d: -f2", 5_000).trim();
            if (apk.isEmpty()) { out.println("KEYTEST-START"); out.println("KEYTEST-END"); out.println("REFUSED: the launcher's apk was not found"); out.flush(); return; }
            ProcessBuilder pb = new ProcessBuilder("app_process", "/system/bin", "com.tj.launcher.ShellProbe", "statusbar", "0", "0x8", String.valueOf(ShellProtocol.KEYTEST_FLAG_SECONDS));
            pb.environment().put("CLASSPATH", apk);
            pb.redirectErrorStream(true);
            probe = pb.start();
            final BufferedReader pr = new BufferedReader(new InputStreamReader(probe.getInputStream()));
            Thread pt = new Thread(new Runnable() { @Override public void run() {
                try { String l; while ((l = pr.readLine()) != null) synchronized (probeOut) { if (probeOut.length() < 1500) probeOut.append(l).append('\n'); } } catch (Throwable ignored) { }
            } }, "tj-keytest-probe");
            pt.start();
            Thread.sleep(1000);
            ev = new ProcessBuilder("getevent", "-lt").redirectErrorStream(true).start();
            final BufferedReader er = new BufferedReader(new InputStreamReader(ev.getInputStream()));
            Thread et = new Thread(new Runnable() { @Override public void run() {
                try { String l; while ((l = er.readLine()) != null) { if (raw.size() < 4000) raw.add(l); } } catch (Throwable ignored) { }
            } }, "tj-keytest-events");
            et.start();
            Thread.sleep(ShellProtocol.KEYTEST_READ_SECONDS * 1000L);
            ev.destroy();
            et.join(1000);
            // the flag holder prints its last line when its seconds are over: wait for it (at most a few seconds more)
            for (int i = 0; i < 40; i++) { try { probe.exitValue(); break; } catch (IllegalThreadStateException running) { Thread.sleep(150); } }
            pt.join(500);
            out.println("KEYTEST-START");
            List<String> copy; synchronized (raw) { copy = new java.util.ArrayList<>(raw); }
            for (String l : ShellProtocol.keyTestLines(copy)) out.println(l);
            out.println("KEYTEST-END");
            out.println("KEYTEST-LINES " + copy.size());   // everything getevent printed (touch noise included): 0 means the shell could not read the devices at all, which "not seen" must not hide
            synchronized (probeOut) { out.print(probeOut); }
            out.flush();
        } catch (Throwable t) {
            out.println("KEYTEST-START"); out.println("KEYTEST-END"); out.println("REFUSED: " + t); out.flush();
        } finally {
            if (ev != null) ev.destroy();
            if (probe != null) { try { probe.exitValue(); } catch (IllegalThreadStateException running) { probe.destroy(); } }   // destroying it also clears the flag at once
        }
    }

    private static Object atmService;
    private static boolean atmFailedOnce = false;

    /** Removes the Recents cards of these packages: ids from `dumpsys activity recents` (the shell user may run it), removed through IActivityTaskManager.removeTask (the shell holds REMOVE_TASKS). */
    private static synchronized int removeRecentTasks(List<String> pkgs) {
        int n = 0;
        try {
            for (int id : ShellProtocol.recentTaskIds(recentsDump(), pkgs)) if (removeTask(id)) n++;
        } catch (Throwable t) { System.out.println("tj-shell: removing recents failed: " + t); }
        return n;
    }

    /** The text of `dumpsys activity recents` (the shell user may run it). Empty on any problem. */
    private static String recentsDump() {
        try {
            Process pr = new ProcessBuilder("dumpsys", "activity", "recents").redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String l;
            while ((l = r.readLine()) != null) sb.append(l).append('\n');
            pr.waitFor();
            return sb.toString();
        } catch (Throwable t) { System.out.println("tj-shell: dumpsys failed: " + t); return ""; }
    }

    private static boolean removeTask(int id) {
        if (atmFailedOnce) return false;
        try {
            if (atmService == null) {
                Class<?> sm = Class.forName("android.os.ServiceManager");
                android.os.IBinder b = (android.os.IBinder) sm.getMethod("getService", String.class).invoke(null, "activity_task");
                Class<?> stub = Class.forName("android.app.IActivityTaskManager$Stub");
                atmService = stub.getMethod("asInterface", android.os.IBinder.class).invoke(null, b);
            }
            Object ok = atmService.getClass().getMethod("removeTask", int.class).invoke(atmService, id);
            return !(ok instanceof Boolean) || ((Boolean) ok).booleanValue();
        } catch (Throwable t) {
            atmFailedOnce = true;
            System.out.println("tj-shell: removeTask unavailable (" + t + ")");
            return false;
        }
    }

    /** Runs one FIXED shell line (never built from caller input) and returns its output, capped at 60 KB and cut off after the timeout. */
    private static String runShell(String sh, long timeoutMs) {
        try {
            Process pr = new ProcessBuilder("/system/bin/sh", "-c", sh).redirectErrorStream(true).start();
            final StringBuilder sb = new StringBuilder();
            final BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            Thread t = new Thread(new Runnable() { @Override public void run() {
                try { String l; while ((l = r.readLine()) != null) { if (sb.length() < 60_000) sb.append(l).append('\n'); } } catch (Throwable ignored) { }
            } });
            t.start();
            t.join(timeoutMs);
            if (t.isAlive()) { pr.destroy(); sb.append("(cut off after ").append(timeoutMs / 1000).append(" s)\n"); }
            return sb.toString();
        } catch (Throwable e) { return "(failed: " + e + ")\n"; }
    }

    private static Object amService;
    private static boolean binderFailedOnce = false;

    /** Force-stops through the activity manager's binder (no new JVM per app: cheap on a low-RAM tablet). The shell user holds FORCE_STOP_PACKAGES. False if the hidden API is not callable. */
    private static synchronized boolean viaBinder(String pkg) {
        if (binderFailedOnce) return false;
        try {
            if (amService == null) {
                Class<?> sm = Class.forName("android.os.ServiceManager");
                android.os.IBinder b = (android.os.IBinder) sm.getMethod("getService", String.class).invoke(null, "activity");
                Class<?> stub = Class.forName("android.app.IActivityManager$Stub");
                amService = stub.getMethod("asInterface", android.os.IBinder.class).invoke(null, b);
            }
            amService.getClass().getMethod("forceStopPackage", String.class, int.class).invoke(amService, pkg, 0);
            return true;
        } catch (Throwable t) {
            binderFailedOnce = true;
            System.out.println("tj-shell: binder force-stop unavailable, using `am` (" + t + ")");
            return false;
        }
    }

    /** One package at a time (a low-RAM tablet must not start several JVMs at once): the binder call first, `am force-stop` as the fallback. */
    private static int forceStop(List<String> pkgs) {
        int done = 0;
        for (String p : pkgs) {
            try {
                if (viaBinder(p)) { done++; continue; }
                Process pr = new ProcessBuilder("/system/bin/am", "force-stop", p).redirectErrorStream(true).start();
                pr.getInputStream().close();
                if (pr.waitFor() == 0) done++;
            } catch (Throwable ignored) { }
        }
        return done;
    }

    // ---------------------------------------------------------------- HLP1: the flight recorder (never part of any job; every step is guarded, a failure here must not touch the helper)

    private static volatile java.io.File recFile;
    private static final Object REC_LOCK = new Object();
    private static volatile long recT0;

    /**
     * HLP1: the helper is gone a few minutes after it is started on Tj's tablet and nobody knows why. This leaves evidence in a file the launcher can read (its own external files directory; fallback
     * /data/local/tmp/tj-life.txt for a cable session): START, a BEAT every 30 s (seconds alive, VmRSS, oom_score_adj, usb state), SHUTDOWN (the VM was asked to exit), EXCEPTION, and the kill-related lines
     * of the system log as they appear. See {@link HelperLife}. A kill by SIGKILL writes nothing at the end: the last BEAT and the LOGCAT lines before it are the answer.
     */
    private static void startRecorder() {
        try {
            java.io.File f = pickRecFile();
            if (f == null) return;
            recFile = f;
            recT0 = System.currentTimeMillis();
            rec(HelperLife.line("START", recT0, null, "pid", String.valueOf(android.os.Process.myPid()), "uid", String.valueOf(android.os.Process.myUid()), "file", f.getPath()));
            final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { @Override public void uncaughtException(Thread t, Throwable e) {
                rec(HelperLife.line("EXCEPTION", System.currentTimeMillis(), t.getName() + ": " + e));
                if (prev != null) prev.uncaughtException(t, e);
            } });
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { @Override public void run() {
                long now = System.currentTimeMillis();
                rec(HelperLife.line("SHUTDOWN", now, "the VM was asked to exit (SIGTERM, SIGHUP or SIGINT)", "up", String.valueOf((now - recT0) / 1000L)));
            } }, "tj-shell-hook"));
            Thread hb = new Thread(new Runnable() { @Override public void run() {
                while (true) {
                    try { Thread.sleep(HelperLife.BEAT_MS); } catch (InterruptedException e) { return; }
                    long now = System.currentTimeMillis();
                    rec(HelperLife.line("BEAT", now, null, "up", String.valueOf((now - recT0) / 1000L), "rss", procValue("/proc/self/status", "VmRSS:"), "oom", procFirstLine("/proc/self/oom_score_adj"),
                            "usb", sysprop("sys.usb.state"), "adbd", sysprop("init.svc.adbd")));
                }
            } }, "tj-shell-beat");
            hb.setDaemon(true);
            hb.start();
            Thread lc = new Thread(new Runnable() { @Override public void run() { logcatWatch(); } }, "tj-shell-logcat");
            lc.setDaemon(true);
            lc.start();
        } catch (Throwable t) { System.out.println("tj-shell: flight recorder not started: " + t); }
    }

    private static java.io.File pickRecFile() {
        String[] dirs = { "/sdcard/Android/data/" + ShellProtocol.SELF + "/files", "/data/local/tmp" };
        String[] names = { "helper-life.txt", "tj-life.txt" };
        for (int i = 0; i < dirs.length; i++) {
            try {
                java.io.File d = new java.io.File(dirs[i]);
                if (!d.isDirectory() && !d.mkdirs()) continue;
                java.io.File f = new java.io.File(d, names[i]);
                java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
                o.close();
                return f;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private static void rec(String line) {
        try {
            java.io.File f = recFile;
            if (f == null) return;
            synchronized (REC_LOCK) {
                java.io.FileOutputStream o = new java.io.FileOutputStream(f, true);
                try { o.write((line + "\n").getBytes("UTF-8")); } finally { o.close(); }
                if (f.length() > HelperLife.MAX_FILE_BYTES) {
                    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                    java.io.FileInputStream in = new java.io.FileInputStream(f);
                    try { byte[] buf = new byte[4096]; int n; while ((n = in.read(buf)) > 0) bo.write(buf, 0, n); } finally { in.close(); }
                    String t = HelperLife.trim(bo.toString("UTF-8"), HelperLife.MAX_FILE_BYTES, HelperLife.KEEP_FILE_BYTES);
                    java.io.FileOutputStream w = new java.io.FileOutputStream(f, false);
                    try { w.write(t.getBytes("UTF-8")); } finally { w.close(); }
                }
            }
        } catch (Throwable ignored) { }
    }

    private static final java.util.regex.Pattern KILLISH = java.util.regex.Pattern.compile("(?i)lmkd|lowmemorykiller|killing |killed|sigkill|sigterm|sighup|libprocessgroup|tj-shell|shellserver"   /* not plain "app_process": every process the helper starts logs "Core platform API reporting enabled" with that name (emulator run #334), which would fill the 400-line quota; a real kill line names lmkd / killed / Killing anyway */);

    /** Copies the kill-related lines of the system log into the recorder as they appear (at most 400 per run, so a noisy log cannot fill the storage). Ends quietly if logcat cannot be started. */
    private static void logcatWatch() {
        Process p = null;
        try {
            p = new ProcessBuilder("logcat", "-b", "main", "-b", "system", "-b", "crash", "-v", "threadtime", "-T", "1").redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String l;
            int kept = 0;
            while ((l = r.readLine()) != null) {
                if (!KILLISH.matcher(l).find()) continue;
                if (++kept > 400) { rec(HelperLife.line("LOGCAT", System.currentTimeMillis(), "(limit of 400 lines reached for this run)")); break; }
                rec(HelperLife.line("LOGCAT", System.currentTimeMillis(), l));
            }
        } catch (Throwable ignored) {
        } finally { if (p != null) try { p.destroy(); } catch (Throwable ignored) { } }
    }

    private static String procFirstLine(String path) {
        try { BufferedReader r = new BufferedReader(new java.io.FileReader(path)); try { String l = r.readLine(); return l == null ? "?" : l.trim(); } finally { r.close(); } } catch (Throwable t) { return "?"; }
    }

    private static String procValue(String path, String key) {
        try {
            BufferedReader r = new BufferedReader(new java.io.FileReader(path));
            try { String l; while ((l = r.readLine()) != null) if (l.startsWith(key)) return l.substring(key.length()).trim().replace(" kB", "").replace(' ', '_'); } finally { r.close(); }
        } catch (Throwable ignored) { }
        return "?";
    }

    /** android.os.SystemProperties is hidden API; a shell-started app_process may call it, and anything else answers "?". */
    private static String sysprop(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Object v = c.getMethod("get", String.class, String.class).invoke(null, key, "?");
            return v == null || String.valueOf(v).isEmpty() ? "-" : String.valueOf(v);
        } catch (Throwable t) { return "?"; }
    }

    private static int lookupUid() {
        try {
            Process pr = new ProcessBuilder("cmd", "package", "list", "packages", "-U", ShellProtocol.SELF).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String l;
            while ((l = r.readLine()) != null) sb.append(l).append('\n');
            pr.waitFor();
            return ShellProtocol.parseUid(sb.toString());
        } catch (Throwable t) { return -1; }
    }
}
