package com.tj.launcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.List;
import java.util.regex.Pattern;

/**
 * N6 (Tj, 2026-11-04): "make it totally invisible once I have the adb commands entered". The only thing that can remove another app's Recents card AND its notifications without driving any
 * screen is {@code am force-stop}, which only the shell user may run. {@link ShellServer} is a tiny process started ONCE per boot by an adb command; it runs as the shell user, listens on a
 * local socket, and accepts exactly two commands from OUR app only (peer uid is checked). This class is the pure, JVM-tested part: parsing and the deny-list.
 *   PING                      -> OK tj-shell 1
 *   FORCESTOP pkg [pkg ...]   -> OK <n stopped> (packages that fail the deny-list / name check are skipped, never run)
 *   QUIT                      -> OK (the process ends)
 */
public final class ShellProtocol {
    public static final String SOCKET = "tj_shell_helper";
    public static final String SELF = "com.tj.launcher", STOCK = "com.android.launcher3";
    public static final int MAX_PACKAGES = 40;
    /** Fallback transport: loopback TCP (an app may be refused a shell-owned abstract socket by SELinux; loopback TCP is allowed). First free port of this range wins. */
    public static final int TCP_PORT_FIRST = 47631, TCP_PORT_LAST = 47635;
    private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$");

    private ShellProtocol() {}

    /** Hard deny-list, enforced INSIDE the shell process whatever the app asks for: never stop the system, the phone, providers, the keyboards, this app or the stock launcher. */
    public static boolean allowed(String pkg) {
        if (pkg == null || pkg.length() > 150 || !NAME.matcher(pkg).matches()) return false;
        if (!ResetPolicy.shouldKill(pkg, SELF, STOCK, null)) return false;
        return !(pkg.startsWith("com.android.providers.") || pkg.startsWith("com.google.android.gms") || pkg.startsWith("com.google.android.gsf")
                || pkg.startsWith("com.android.server.") || pkg.equals("com.android.bluetooth") || pkg.equals("com.android.nfc") || pkg.startsWith("com.mediatek.")
                || pkg.equals("com.android.keychain") || pkg.equals("com.android.shell") || pkg.equals("com.android.permissioncontroller"));
    }

    /** The packages of a FORCESTOP line that pass {@link #allowed}, de-duplicated, at most MAX_PACKAGES. Null if the line is not a FORCESTOP. */
    public static List<String> forceStopTargets(String line) {
        if (line == null) return null;
        String[] w = line.trim().split("\\s+");
        if (w.length < 2 || !"FORCESTOP".equals(w[0])) return null;
        List<String> out = new ArrayList<>();
        for (int i = 1; i < w.length && out.size() < MAX_PACKAGES; i++) if (allowed(w[i]) && !out.contains(w[i])) out.add(w[i]);
        return out;
    }

    private static final Pattern BALANCE_VALUE = Pattern.compile("^-?(0|1)(\\.[0-9]{1,3})?$");

    /**
     * BN1 (the Settings look-alike): "SETSYS master_mono 1" / "SETSYS master_balance -0.5". Android keeps these two settings PRIVATE (Settings.System.PRIVATE_SETTINGS: an app gets "You cannot change private
     * system settings" even with WRITE_SECURE_SETTINGS; only system, shell and root may write them), so the helper (shell) writes them for the Mono audio switch and the Audio balance slider. A strict allow-list:
     * exactly these two keys, master_mono 0 or 1, master_balance a plain number in -1 .. 1 with at most three decimals. @return {key, value} (value normalised) or null for anything else.
     */
    public static String[] setSystem(String line) {
        if (line == null) return null;
        String[] w = line.trim().split("\\s+");
        if (w.length != 3 || !"SETSYS".equals(w[0])) return null;
        if ("master_mono".equals(w[1])) return ("0".equals(w[2]) || "1".equals(w[2])) ? new String[] { w[1], w[2] } : null;
        if ("master_balance".equals(w[1])) {
            if (!BALANCE_VALUE.matcher(w[2]).matches()) return null;
            float f = Float.parseFloat(w[2]);
            if (f < -1f || f > 1f) return null;
            return new String[] { w[1], w[2] };
        }
        return null;
    }

    public static boolean isPing(String line) { return line != null && line.trim().equals("PING"); }
    public static boolean isQuit(String line) { return line != null && line.trim().equals("QUIT"); }

    /** The uid in the output of {@code cmd package list packages -U com.tj.launcher} ("package:com.tj.launcher uid:10123"), or -1. */
    public static int parseUid(String listOutput) {
        if (listOutput == null) return -1;
        for (String l : listOutput.split("\\n")) {
            int i = l.indexOf("uid:");
            if (l.trim().startsWith("package:" + SELF + " ") && i >= 0) {
                try { return Integer.parseInt(l.substring(i + 4).trim().split("[^0-9]")[0]); } catch (NumberFormatException ignored) { }
            }
        }
        return -1;
    }

    /**
     * The uid that owns the CLIENT end of a loopback TCP connection, read from the text of /proc/net/tcp or /proc/net/tcp6 (only the shell user may read those on Android 10): the row whose
     * local port is the client's port and whose remote port is the server's port. -1 if there is no such row.
     */
    public static int uidForPorts(String procNetTcp, int clientPort, int serverPort) {
        if (procNetTcp == null) return -1;
        for (String l : procNetTcp.split("\n")) {
            String[] f = l.trim().split("\\s+");
            if (f.length < 8) continue;
            int lp = portOf(f[1]), rp = portOf(f[2]);
            if (lp == clientPort && rp == serverPort) {
                try { return Integer.parseInt(f[7]); } catch (NumberFormatException ignored) { }
            }
        }
        return -1;
    }

    private static int portOf(String addr) {
        int i = addr == null ? -1 : addr.lastIndexOf(':');
        if (i < 0) return -1;
        try { return Integer.parseInt(addr.substring(i + 1), 16); } catch (NumberFormatException e) { return -1; }
    }

    private static final Pattern TASK_HEADER = Pattern.compile("Recent #\\d+:.*?\\{[0-9a-fA-F]+ #(\\d+) ");
    private static final Pattern COMPONENT_PKG = Pattern.compile("(?:realActivity=|cmp=|affinity=|A=)([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)");

    /**
     * The ids of the Recents tasks that belong to the given packages, read from the text of `dumpsys activity recents`. Force-stop kills an app but Android 10 can leave its Recents card, so the helper
     * removes these tasks too.
     * <p>2026-11-06 (gate run 147 and the bisect runs 153-155): a task belongs to a package ONLY if its ROOT activity does (the rule of {@link #parseTasks}: realActivity=, else cmp=, else I=, else A=).
     * The first version matched any line of a task's text, and the LAST task of the list also collects everything printed after the list, so at every lock clean-up the OLDEST Recents task (the
     * Calendar target in the emulator test, the system's FallbackHome in others) was removed - and its process killed ("remove task") - although the plan never named it.
     */
    public static List<Integer> recentTaskIds(String dump, Collection<String> pkgs) {
        List<Integer> out = new ArrayList<>();
        if (dump == null || pkgs == null || pkgs.isEmpty()) return out;
        for (java.util.Map.Entry<Integer, String> e : parseTasks(dump).entrySet()) if (pkgs.contains(e.getValue())) out.add(e.getKey());
        return out;
    }

    // ------------------------------------------------------------------ AA3 (Tj, 2026-11-05: "Build number 4"): task-precise Recents clean-up
    private static final Pattern REAL_ACTIVITY = Pattern.compile("realActivity=([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)/");
    private static final Pattern INTENT_CMP = Pattern.compile("intent=\\{[^}]*?cmp=([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)/");
    private static final Pattern HEADER_I = Pattern.compile(" I=([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)/");
    private static final Pattern HEADER_A = Pattern.compile(" A=([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+)[ :}]");

    /**
     * id -> package of every task in the text of `dumpsys activity recents`, in the order Android lists them. The package is the one of the task's ROOT activity: realActivity=, else the intent's cmp=,
     * else the header's I=, else (last resort) the affinity A= (many system apps use an affinity that is NOT a package name, e.g. "android.task.contacts": the first research run of 2026-11-05 tripped on that).
     * HOME-type tasks (our launcher, the stock launcher) are listed too: callers filter them with {@link #allowed}.
     */
    public static java.util.LinkedHashMap<Integer, String> parseTasks(String dump) {
        java.util.LinkedHashMap<Integer, String> out = new java.util.LinkedHashMap<>();
        if (dump == null) return out;
        int id = -1;
        String real = null, cmp = null, hdrI = null, hdrA = null;
        for (String line : dump.split("\\n")) {
            Matcher h = TASK_HEADER.matcher(line);
            if (h.find()) {
                put(out, id, real, cmp, hdrI, hdrA);
                try { id = Integer.parseInt(h.group(1)); } catch (NumberFormatException e) { id = -1; }
                real = cmp = hdrI = hdrA = null;
                Matcher i = HEADER_I.matcher(line); if (i.find()) hdrI = i.group(1);
                Matcher a = HEADER_A.matcher(line); if (a.find()) hdrA = a.group(1);
                continue;
            }
            if (id < 0) continue;
            if (real == null) { Matcher m = REAL_ACTIVITY.matcher(line); if (m.find()) real = m.group(1); }
            if (cmp == null) { Matcher m = INTENT_CMP.matcher(line); if (m.find()) cmp = m.group(1); }
        }
        put(out, id, real, cmp, hdrI, hdrA);
        return out;
    }

    private static void put(java.util.Map<Integer, String> out, int id, String real, String cmp, String hdrI, String hdrA) {
        if (id < 0) return;
        String pkg = real != null ? real : cmp != null ? cmp : hdrI != null ? hdrI : hdrA;
        if (pkg != null) out.put(id, pkg);
    }

    public static boolean isListTasks(String line) { return line != null && line.trim().equals("LISTTASKS"); }

    /** AB3: "NOTIFPKGS" -> the helper answers with one package per line: every app that has a notification in the shade right now (read-only). */
    public static boolean isNotifPkgs(String line) { return line != null && line.trim().equals("NOTIFPKGS"); }

    private static final Pattern NOTIF_RECORD = Pattern.compile("NotificationRecord\\(0x[0-9a-fA-F]+: pkg=([A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z0-9_]+)+) user=UserHandle\\{(\\d+)\\}");

    /**
     * The packages (of user 0) that have an ACTIVE notification, from the text of `dumpsys notification` (Android 10 prints each as "NotificationRecord(0x...: pkg=com.x user=UserHandle{0} id=..."). The "last removed"
     * archive uses another shape ("StatusBarNotification(...)") and is not matched on purpose. Works on the whole dump or on just the lines that contain "NotificationRecord(".
     */
    public static java.util.Set<String> parseNotifPackages(String dump) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (dump == null) return out;
        java.util.regex.Matcher m = NOTIF_RECORD.matcher(dump);
        while (m.find()) if ("0".equals(m.group(2)) && out.size() < 200) out.add(m.group(1));
        return out;
    }

    /** "RMTASKS 12 14" -> [12, 14]: positive numbers only, at most MAX_PACKAGES, no duplicates; null if the line is not an RMTASKS line. Anything that is not a plain number is dropped. */
    public static List<Integer> removeTaskIds(String line) {
        if (line == null) return null;
        String[] w = line.trim().split("\\s+");
        if (w.length < 1 || !"RMTASKS".equals(w[0])) return null;
        List<Integer> out = new ArrayList<>();
        for (int i = 1; i < w.length && out.size() < MAX_PACKAGES; i++) {
            if (!w[i].matches("[0-9]{1,9}")) continue;
            int v = Integer.parseInt(w[i]);
            if (v > 0 && !out.contains(v)) out.add(v);
        }
        return out;
    }

    // ------------------------------------------------------------------ S2 (Tj, 2026-11-04: "do everything in one session"): a fixed list of jobs the helper runs FOR the launcher
    /** id -> the exact shell line. Nothing from the caller is ever put into a line: only these ids exist. Each one is either a grant for THIS app or a read-only look. */
    public static final String[][] RUN = {
        {"GRANT_WSS", "pm grant com.tj.launcher android.permission.WRITE_SECURE_SETTINGS"},
        {"GRANT_DUMP", "pm grant com.tj.launcher android.permission.DUMP"},
        {"GRANT_CFG", "pm grant com.tj.launcher android.permission.CHANGE_CONFIGURATION"},
        {"GRANT_STORAGE", "pm grant com.tj.launcher android.permission.READ_EXTERNAL_STORAGE"},
        {"OP_USAGE", "cmd appops set com.tj.launcher GET_USAGE_STATS allow"},
        {"OP_WRITE", "cmd appops set com.tj.launcher WRITE_SETTINGS allow"},
        {"OP_BG", "cmd appops set com.tj.launcher RUN_IN_BACKGROUND allow"},
        {"OP_ANYBG", "cmd appops set com.tj.launcher RUN_ANY_IN_BACKGROUND allow"},
        {"IDLE_WL", "dumpsys deviceidle whitelist +com.tj.launcher"},
        // U2 (Tj: "significant delay when loading either launcher"): sideloaded apps run half-optimised until Android compiles them in idle time; this compiles our launcher fully now
        {"COMPILE_SELF", "cmd package compile -m speed -f com.tj.launcher"},
        {"BROWSER_OFF", "pm disable-user --user 0 com.android.browser"},
        {"BROWSER_ON", "pm enable com.android.browser"},
        // T1: publishing to a folder a file manager can see (the shell user may write /sdcard/Download)
        {"MKDIR_PUBLISH", "mkdir -p /sdcard/Download/launcher-probe/shots /sdcard/Download/launcher-probe/apk"},
        // V4: a screenshot of the CURRENT screen for the wallpaper-patch auto-calibration (run MKDIR_PUBLISH first)
        {"CAL_SHOT", "screencap -p /sdcard/Download/launcher-probe/cal.png"},
        {"PUBLISH_REPORT", "cp /sdcard/Android/data/com.tj.launcher/files/probe/tablet-report.txt /sdcard/Download/launcher-probe/tablet-report.txt"},
    };

    /** SHOTS and APKS are fixed routines inside the helper (loops over fixed lists, nothing from the caller). */
    public static boolean isShots(String line) { return line != null && line.trim().equals("SHOTS"); }
    public static boolean isApks(String line) { return line != null && line.trim().equals("APKS"); }

    // ------------------------------------------------------------------ "Power menu v2" readiness test (Tj, 2026-11-05: "add whatever is needed to the phone guide")
    /**
     * "KEYTEST": a fixed routine inside the helper (nothing from the caller). It holds the status-bar flag that keeps Android's power menu away for {@link #KEYTEST_FLAG_SECONDS} seconds (the same
     * ShellProbe call the guide's 3-second card uses; Android clears the flag by itself when that process ends) and, meanwhile, reads the key events of every input device for
     * {@link #KEYTEST_READ_SECONDS} seconds. Two questions are answered at once: may the shell read the side key on THIS tablet, and does the flag keep the normal menu away.
     */
    public static boolean isKeyTest(String line) { return line != null && line.trim().equals("KEYTEST"); }
    public static final int KEYTEST_FLAG_SECONDS = 12, KEYTEST_READ_SECONDS = 9;
    private static final Pattern KEY_TIME = Pattern.compile("^\\[\\s*([0-9]+\\.[0-9]+)\\]");

    /** Caps for {@link #keyTestLines}, counted SEPARATELY: getevent lists every input device (two lines each) before any event, so on a tablet with many of them a shared cap would be used up by the list and the
     *  KEY_POWER lines dropped, which would read as a false "side key NOT seen" and could end the Power menu v2 idea for the wrong reason (code review of 2026-11-05). */
    static final int KEYTEST_MAX_DEVICE_LINES = 24, KEYTEST_MAX_KEY_LINES = 40;

    /** From the raw lines of {@code getevent -lt}: the device list (at most 24 lines) and every KEY_ event (not the touch-screen noise; at most 40 lines). */
    public static List<String> keyTestLines(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        int devices = 0, keys = 0;
        for (String l : raw) {
            if (l == null) continue;
            String t = l.trim();
            if (t.isEmpty()) continue;
            boolean device = t.startsWith("add device") || t.startsWith("name:");
            if (device && devices < KEYTEST_MAX_DEVICE_LINES) devices++;
            else if (!device && t.contains("KEY_") && keys < KEYTEST_MAX_KEY_LINES) keys++;
            else continue;
            out.add(t.length() > 160 ? t.substring(0, 160) : t);
        }
        return out;
    }

    /** One plain sentence about the side key from the helper's answer (the key events and the flag line). Never throws. */
    public static String keyTestSummary(String output) {
        if (output == null || output.trim().isEmpty()) return "no answer from the helper (is it running?)";
        double down = -1, up = -1;
        boolean seen = false;
        int lines = -1;   // how many lines getevent printed in all ("KEYTEST-LINES n"); 0 = it printed nothing, so "not seen" would be a false comfort
        for (String l : output.split("\\n")) {
            if (l.startsWith("KEYTEST-LINES ")) { try { lines = Integer.parseInt(l.substring(14).trim()); } catch (NumberFormatException ignored) { } continue; }
            if (!l.contains("KEY_POWER")) continue;
            seen = true;
            Matcher m = KEY_TIME.matcher(l.trim());
            double ts = m.find() ? Double.parseDouble(m.group(1)) : -1;
            if (l.contains("DOWN") && down < 0) down = ts;
            else if (l.contains("UP") && down >= 0 && up < 0) up = ts;
        }
        String key = lines == 0 ? "the shell could NOT read the input devices (getevent printed nothing), so this test cannot say whether the side key is readable"
                : !seen ? "the side key was NOT seen (" + (lines > 0 ? lines + " input lines were read, so getevent works, but " : "") + "no KEY_POWER event in " + KEYTEST_READ_SECONDS + " s: it was not pressed in time, or the shell cannot read it)"
                : (down >= 0 && up >= down ? String.format(java.util.Locale.US, "the side key WAS seen (held %.1f s)", up - down) : "the side key WAS seen");
        String flag = output.contains("disable2(0x8) -> OK") ? "the shell MAY switch the normal power menu off" : output.contains("REFUSED") ? "the shell was REFUSED when it tried to switch the normal power menu off" : "no answer about switching the normal power menu off";
        return key + "; " + flag;
    }
    public static final String PUBLISH_DIR = "/sdcard/Download/launcher-probe";
    /** The Settings pages photographed (public android.settings.* actions). */
    public static final String[] SHOT_PAGES = { "SETTINGS", "WIFI_SETTINGS", "BLUETOOTH_SETTINGS", "DISPLAY_SETTINGS", "SOUND_SETTINGS", "APPLICATION_SETTINGS", "INTERNAL_STORAGE_SETTINGS",
            "BATTERY_SAVER_SETTINGS", "DATE_SETTINGS", "LOCALE_SETTINGS", "ACCESSIBILITY_SETTINGS", "SECURITY_SETTINGS", "PRIVACY_SETTINGS", "DEVICE_INFO_SETTINGS", "INPUT_METHOD_SETTINGS" };
    public static final String[] APK_PACKAGES = { "com.android.settings", "com.android.systemui", "com.android.launcher3", "android", "com.android.providers.settings", "com.android.permissioncontroller", "com.google.android.permissioncontroller", "com.android.packageinstaller", "com.android.inputmethod.latin" };   // W2: + settings storage, permission UI, installer, stock keyboard

    /** The shell line for a job id, or null (unknown ids are refused). */
    public static String runLine(String id) {
        if (id == null) return null;
        for (String[] r : RUN) if (r[0].equals(id.trim())) return r[1];
        return null;
    }

    /** "RUN <id>" -> the id, else null. */
    public static String runId(String line) {
        if (line == null) return null;
        String t = line.trim();
        return t.startsWith("RUN ") ? t.substring(4).trim() : null;
    }

    public static boolean isProbe(String line) { return line != null && line.trim().equals("PROBE"); }
    public static boolean isA11yOn(String line) { return line != null && line.trim().equals("A11YON"); }

    /** The component name of the accessibility helper. */
    public static final String A11Y = SELF + "/" + SELF + ".TjAccessibilityService";

    /** The new value of enabled_accessibility_services with our helper added (kept as it is when already there). */
    public static String mergeA11y(String current) {
        String c = current == null ? "" : current.trim();
        if (c.isEmpty() || c.equals("null")) return A11Y;
        for (String part : c.split(":")) if (part.equals(A11Y)) return c;
        return c + ":" + A11Y;
    }

    /**
     * AB1 (safety): a value to be put on a `sh -c` command line inside single quotes, or null if it cannot be done safely. The list of accessibility services can hold nested class names
     * ("pkg/pkg.Outer$Inner"): unquoted, the shell would expand "$Inner" to nothing and the write would silently disable ANOTHER accessibility service (TalkBack, a switch-access app ...).
     * Only the characters Android itself uses in component names are accepted; anything else (a quote, a space, a semicolon, a newline, a backtick) returns null and the caller writes nothing.
     */
    public static String quoteForShell(String value) {
        if (value == null || value.isEmpty() || value.length() > 4000) return null;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '.' || ch == '_' || ch == '/' || ch == ':' || ch == '$' || ch == '-';
            if (!ok) return null;
        }
        return "'" + value + "'";
    }

    /** One line of probe-commands.txt: a section title ("## ...") or a command; blank lines are dropped. */
    public static final class ProbeLine {
        public final boolean title; public final String text;
        ProbeLine(boolean title, String text) { this.title = title; this.text = text; }
    }

    /** Parses the probe list (the file bundled INSIDE the APK: the helper only ever runs lines from its own signed APK). The first "## " block of notes at the top is skipped. */
    public static List<ProbeLine> parseProbeList(String text) {
        List<ProbeLine> out = new ArrayList<>();
        if (text == null) return out;
        boolean started = false;
        for (String raw : text.split("\\n")) {
            String l = raw.replace("\r", "").trim();
            if (l.isEmpty()) continue;
            if (l.startsWith("## ")) {
                String t = l.substring(3);
                // the first lines are notes about the file itself (they contain 'Read-only probe' / 'Used by' / 'NO double quotes'): skip until the first real section title
                if (!started && (t.startsWith("Read-only") || t.startsWith("Used by"))) continue;
                started = true;
                out.add(new ProbeLine(true, t));
            } else if (started) out.add(new ProbeLine(false, l));
        }
        return out;
    }

    /** The one adb line that starts the helper (single quotes: the DEVICE shell expands the $(...)). */
    public static final String START_ONE_LINER =
            "adb shell 'CLASSPATH=$(pm path " + SELF + " | head -1 | cut -d: -f2) setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &'";
    public static final String START_STEP1 = "adb shell pm path " + SELF;
    public static final String START_STEP2 = "adb shell \"CLASSPATH=<the path printed above, without 'package:'> setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &\"";
    // ---- T1 (Tj, 2026-11-04: "I have adb with another android phone to the tablet ... an app such as adb otg"): such an app gives a DEVICE shell, so there is no "adb shell" prefix
    /** Paste into the phone's adb app terminal (it is already the tablet's shell). */
    public static final String START_DEVICE_LINE = "CLASSPATH=$(pm path " + SELF + " | head -1 | cut -d: -f2) setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &";
    /** For adb apps that run each line directly instead of through a shell: wrap it in sh -c. */
    public static final String START_DEVICE_SH_C = "sh -c '" + START_DEVICE_LINE + "'";
    /** Two steps for apps that cannot substitute $(...): first `pm path com.tj.launcher`, then this with the printed path (without "package:") in place of PATH. */
    public static final String START_DEVICE_TWO_STEP = "sh -c 'CLASSPATH=PATH setsid nohup app_process /system/bin com.tj.launcher.ShellServer >/dev/null 2>&1 &'";
    public static final String STOP_LINE = "adb shell pkill -f com.tj.launcher.ShellServer";
}
