package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class ShellProtocolTest {
    @Test public void ordinaryAppsAreAllowed() {
        assertTrue(ShellProtocol.allowed("com.android.contacts"));
        assertTrue(ShellProtocol.allowed("com.google.android.apps.chrome"));
        assertTrue(ShellProtocol.allowed("org.mozilla.firefox"));
    }
    @Test public void systemAndOurOwnPackagesAreNeverStopped() {
        for (String p : new String[] { "android", "com.android.systemui", "com.android.phone", "com.tj.launcher", "com.android.launcher3", "com.android.providers.contacts",
                "com.google.android.gms", "com.android.inputmethod.latin", "com.mediatek.ims", "com.android.shell", "com.android.bluetooth", "com.android.server.telecom" })
            assertFalse(p, ShellProtocol.allowed(p));
    }
    @Test public void oddNamesAreRejected() {
        for (String p : new String[] { null, "", "a", "com.x; reboot", "com.x && id", "../x", "com..x", "1com.x", "com.x\nreboot", "$(id).a" }) assertFalse(String.valueOf(p), ShellProtocol.allowed(p));
    }
    @Test public void forceStopLineIsFilteredAndDeduplicated() {
        assertEquals(Arrays.asList("com.android.contacts", "com.android.email"),
                ShellProtocol.forceStopTargets("FORCESTOP com.android.contacts android com.android.email com.android.contacts com.x;reboot"));
    }
    @Test public void otherLinesAreNotForceStop() {
        assertNull(ShellProtocol.forceStopTargets("PING"));
        assertNull(ShellProtocol.forceStopTargets("FORCESTOP"));
        assertNull(ShellProtocol.forceStopTargets("force-stop com.a.b"));
        assertNull(ShellProtocol.forceStopTargets(null));
        assertTrue(ShellProtocol.isPing(" PING ")); assertFalse(ShellProtocol.isPing("PINGX")); assertTrue(ShellProtocol.isQuit("QUIT"));
    }
    @Test public void atMostFortyPackages() {
        StringBuilder b = new StringBuilder("FORCESTOP");
        for (int i = 0; i < 80; i++) b.append(" com.app").append(i).append(".x");
        assertEquals(ShellProtocol.MAX_PACKAGES, ShellProtocol.forceStopTargets(b.toString()).size());
    }
    @Test public void parsesTheUidFromPmOutput() {
        assertEquals(10123, ShellProtocol.parseUid("package:com.tj.launcher uid:10123\n"));
        assertEquals(10123, ShellProtocol.parseUid("package:com.tj.launcherX uid:1\npackage:com.tj.launcher uid:10123"));
        assertEquals(-1, ShellProtocol.parseUid("Error: nothing"));
        assertEquals(-1, ShellProtocol.parseUid(null));
    }
    @Test public void theAdbLineExpandsOnTheDeviceNotTheComputer() {
        assertTrue(ShellProtocol.START_ONE_LINER.contains("adb shell '"));
        assertTrue(ShellProtocol.START_ONE_LINER.contains("com.tj.launcher.ShellServer"));
    }
    @Test public void readsTheClientUidFromProcNetTcp() {
        String t = "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode\n"
                 + "   0: 0100007F:B84F 0100007F:2CCF 01 00000000:00000000 00:00000000 00000000 10123        0 4711 1 0000000000000000 20 4 30 10 -1\n"
                 + "   1: 0100007F:2CCF 0100007F:B84F 01 00000000:00000000 00:00000000 00000000  2000        0 4712 1 0000000000000000 20 4 30 10 -1\n";
        // client port 0xB84F = 47183, server port 0x2CCF = 11471
        assertEquals(10123, ShellProtocol.uidForPorts(t, 0xB84F, 0x2CCF));
        assertEquals(-1, ShellProtocol.uidForPorts(t, 1234, 0x2CCF));
        assertEquals(-1, ShellProtocol.uidForPorts(null, 1, 2));
        assertEquals(-1, ShellProtocol.uidForPorts("garbage", 1, 2));
    }
    @Test public void tcpPortRangeIsSmall() { assertTrue(ShellProtocol.TCP_PORT_LAST - ShellProtocol.TCP_PORT_FIRST <= 5); }
    private static final String DUMP =
            "ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)\n"
          + "  mRecentsUid=-1\n  Recent tasks:\n"
          + "  * Recent #0: TaskRecord{4e6fbae #17 A=com.android.contacts U=0 StackId=1 sz=1}\n"
          + "    userId=0 effectiveUid=u0a15 mCallingUid=u0a1\n"
          + "    affinity=com.android.contacts\n"
          + "    intent={act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.android.contacts/.activities.PeopleActivity}\n"
          + "    realActivity=com.android.contacts/.activities.PeopleActivity\n"
          + "  * Recent #1: TaskRecord{9f1c2aa #18 A=com.android.calendar U=0 StackId=1 sz=1}\n"
          + "    realActivity=com.android.calendar/.AllInOneActivity\n"
          + "  * Recent #2: TaskRecord{77ab11c #19 A=com.android.email U=0 StackId=1 sz=1}\n"
          + "    realActivity=com.android.email/.activity.Welcome\n";
    @Test public void findsTheRecentTasksOfThePackages() {
        assertEquals(Arrays.asList(17, 19), ShellProtocol.recentTaskIds(DUMP, Arrays.asList("com.android.contacts", "com.android.email")));
        assertEquals(Arrays.asList(18), ShellProtocol.recentTaskIds(DUMP, Arrays.asList("com.android.calendar")));
        assertTrue(ShellProtocol.recentTaskIds(DUMP, Arrays.asList("com.nothing.here")).isEmpty());
        assertTrue(ShellProtocol.recentTaskIds(null, Arrays.asList("a.b")).isEmpty());
        assertTrue(ShellProtocol.recentTaskIds(DUMP, Arrays.<String>asList()).isEmpty());
    }

    // 2026-11-06: the OLDEST task was removed at every lock clean-up because the last block of the dump swallows whatever follows the list (gate run 147, bisect runs 153-155)
    private static final String DUMP_WITH_TRAILING_TEXT =
            "ACTIVITY MANAGER RECENT TASKS (dumpsys activity recents)\n"
          + "  Recent tasks:\n"
          + "  * Recent #0: TaskRecord{aa1 #56 A=com.android.email U=0 StackId=10 sz=1}\n"
          + "    realActivity=com.android.email/.activity.Welcome\n"
          + "  * Recent #1: TaskRecord{aa2 #54 A=com.android.gallery3d U=0 StackId=9 sz=1}\n"
          + "    realActivity=com.android.gallery3d/.app.GalleryActivity\n"
          + "  * Recent #2: TaskRecord{aa3 #34 A=android.task.calendar U=0 StackId=6 sz=1}\n"
          + "    affinity=android.task.calendar\n"
          + "    intent={act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10200000 cmp=com.android.calendar/.AllInOneActivity}\n"
          + "    realActivity=com.android.calendar/.AllInOneActivity\n"
          + "    affinityIntent={cmp=com.android.email/.activity.Welcome}\n"
          + "  Visible recent tasks (most recent first):\n"
          + "    * RecentTaskInfo{id=56 A=com.android.email realActivity=com.android.email/.activity.Welcome cmp=com.android.email/.activity.Welcome affinity=com.android.email}\n"
          + "    * RecentTaskInfo{id=54 A=com.android.gallery3d realActivity=com.android.gallery3d/.app.GalleryActivity}\n";
    @Test public void theOldestTaskIsNotSwallowedByTextAfterTheList() {
        assertEquals(Arrays.asList(56, 54), ShellProtocol.recentTaskIds(DUMP_WITH_TRAILING_TEXT, Arrays.asList("com.android.email", "com.android.gallery3d")));
        assertEquals(Arrays.asList(34), ShellProtocol.recentTaskIds(DUMP_WITH_TRAILING_TEXT, Arrays.asList("com.android.calendar")));
    }
    @Test public void aTaskWithoutARealActivityStillMatchesThroughItsIntentOrHeader() {
        String d = "  Recent tasks:\n"
                 + "  * Recent #0: TaskRecord{b1 #21 A=com.x.one U=0 StackId=1 sz=1}\n"
                 + "    intent={act=android.intent.action.MAIN cmp=com.x.one/.Main}\n"
                 + "  * Recent #1: TaskRecord{b2 #22 I=com.x.two/.Main U=0 StackId=1 sz=1}\n";
        assertEquals(Arrays.asList(21, 22), ShellProtocol.recentTaskIds(d, Arrays.asList("com.x.one", "com.x.two")));
    }

    // ---- S2: the allow-listed jobs
    @Test public void onlyListedJobsHaveALine() {
        assertEquals("pm grant com.tj.launcher android.permission.WRITE_SECURE_SETTINGS", ShellProtocol.runLine("GRANT_WSS"));
        assertNull(ShellProtocol.runLine("pm grant com.evil.app android.permission.WRITE_SECURE_SETTINGS"));
        assertNull(ShellProtocol.runLine("GRANT_WSS; reboot"));
        assertNull(ShellProtocol.runLine(null));
    }
    @Test public void everyJobLineOnlyTouchesThisAppOrTheBrowser() {
        for (String[] r : ShellProtocol.RUN) {
            assertTrue(r[0], r[1].contains("com.tj.launcher") || r[1].contains("com.android.browser") || r[1].startsWith("mkdir -p /sdcard/Download/launcher-probe") || r[1].equals("screencap -p /sdcard/Download/launcher-probe/cal.png"));   // CAL_SHOT: the one fixed screenshot path (V4)
            assertFalse(r[0], r[1].contains(";") || r[1].contains("&") || r[1].contains("|") || r[1].contains("`") || r[1].contains("$"));
        }
    }
    @Test public void parsesRunAndProbeLines() {
        assertEquals("OP_BG", ShellProtocol.runId("RUN OP_BG"));
        assertNull(ShellProtocol.runId("PROBE"));
        assertTrue(ShellProtocol.isProbe("PROBE")); assertTrue(ShellProtocol.isA11yOn(" A11YON "));
    }
    @Test public void mergesTheAccessibilityHelperIntoTheList() {
        assertEquals(ShellProtocol.A11Y, ShellProtocol.mergeA11y(null));
        assertEquals(ShellProtocol.A11Y, ShellProtocol.mergeA11y("null"));
        assertEquals("a.b/c.D:" + ShellProtocol.A11Y, ShellProtocol.mergeA11y("a.b/c.D"));
        assertEquals("a.b/c.D:" + ShellProtocol.A11Y, ShellProtocol.mergeA11y("a.b/c.D:" + ShellProtocol.A11Y));
    }

    @Test public void aNestedClassNameIsQuotedSoTheShellNeverExpandsIt() {
        // AB1: unquoted, "$Inner" would become "" and silently disable another accessibility service
        String list = "com.talk.back/com.talk.back.Svc$Inner:" + ShellProtocol.A11Y;
        assertEquals("'" + list + "'", ShellProtocol.quoteForShell(list));
        assertEquals("'" + ShellProtocol.A11Y + "'", ShellProtocol.quoteForShell(ShellProtocol.A11Y));
    }

    @Test public void anUnusualAccessibilityListIsNeverWritten() {
        assertNull(ShellProtocol.quoteForShell(null));
        assertNull(ShellProtocol.quoteForShell(""));
        assertNull(ShellProtocol.quoteForShell("a.b/c.D'; reboot; '"));
        assertNull(ShellProtocol.quoteForShell("a.b/c.D e.f/g.H"));
        assertNull(ShellProtocol.quoteForShell("a.b/c.D\nx"));
        assertNull(ShellProtocol.quoteForShell("a.b/`id`"));
        assertNull(ShellProtocol.quoteForShell("a.b/c.D;x"));
        StringBuilder big = new StringBuilder(); for (int i = 0; i < 4001; i++) big.append('a');
        assertNull(ShellProtocol.quoteForShell(big.toString()));
    }
    @Test public void parsesTheProbeFileAndSkipsItsNotes() {
        String f = "## Read-only probe commands. note\n## Used by tools\n## build and device\ngetprop ro.build.fingerprint\n\n## users\npm list users\n";
        java.util.List<ShellProtocol.ProbeLine> l = ShellProtocol.parseProbeList(f);
        assertEquals(4, l.size());
        assertTrue(l.get(0).title); assertEquals("build and device", l.get(0).text);
        assertFalse(l.get(1).title); assertEquals("getprop ro.build.fingerprint", l.get(1).text);
        assertEquals("pm list users", l.get(3).text);
    }

    @Test public void deviceShellLinesHaveNoAdbPrefixAndStartTheHelper() {
        for (String l : new String[] { ShellProtocol.START_DEVICE_LINE, ShellProtocol.START_DEVICE_SH_C, ShellProtocol.START_DEVICE_TWO_STEP }) {
            assertFalse(l.startsWith("adb"));
            assertTrue(l.contains("app_process") && l.contains("com.tj.launcher.ShellServer"));
        }
        assertTrue(ShellProtocol.START_DEVICE_SH_C.startsWith("sh -c '"));
        assertTrue(ShellProtocol.START_DEVICE_LINE.contains("$(pm path com.tj.launcher"));
    }
    @Test public void shotsAndApksAreFixedRoutines() {
        assertTrue(ShellProtocol.isShots(" SHOTS ")); assertTrue(ShellProtocol.isApks("APKS"));
        assertFalse(ShellProtocol.isShots("SHOTS; reboot")); assertFalse(ShellProtocol.isApks(null));
        for (String p : ShellProtocol.SHOT_PAGES) assertTrue(p, p.matches("[A-Z_]+"));
        for (String p : ShellProtocol.APK_PACKAGES) assertTrue(p, p.matches("[a-z0-9.]+"));
        assertTrue(ShellProtocol.PUBLISH_DIR.startsWith("/sdcard/Download/"));
    }

    // ---- Tj, 2026-11-05: the side-key readiness test ("Power menu v2")
    private static final String[] GETEVENT = {
        "add device 1: /dev/input/event3", "  name:     \"mtk-kpd\"", "add device 2: /dev/input/event0", "  name:     \"mtk-tpd\"",
        "[   45.123456] /dev/input/event3: EV_KEY       KEY_POWER            DOWN", "[   45.123456] /dev/input/event3: EV_SYN       SYN_REPORT           00000000",
        "[   46.612345] /dev/input/event3: EV_KEY       KEY_POWER            UP", "[   46.612345] /dev/input/event3: EV_SYN       SYN_REPORT           00000000",
        "[   47.000000] /dev/input/event0: EV_ABS       ABS_MT_POSITION_X    000001a4", "[   47.000000] /dev/input/event0: EV_KEY       BTN_TOUCH            DOWN" };
    @Test public void theKeyTestIsAFixedRoutineNotAShellLine() {
        assertTrue(ShellProtocol.isKeyTest(" KEYTEST "));
        assertFalse(ShellProtocol.isKeyTest("KEYTEST; reboot"));
        assertFalse(ShellProtocol.isKeyTest(null));
        assertNull("it is not in the simple-line job table", ShellProtocol.runLine("KEYTEST"));
    }
    @Test public void keepsTheDevicesAndTheKeyEventsAndDropsTheTouchNoise() {
        java.util.List<String> kept = ShellProtocol.keyTestLines(Arrays.asList(GETEVENT));
        assertTrue(kept.toString(), kept.contains("add device 1: /dev/input/event3"));
        assertTrue(kept.toString(), kept.contains("name:     \"mtk-kpd\""));
        int power = 0; for (String l : kept) { assertFalse(l, l.contains("ABS_MT") || l.contains("BTN_TOUCH") || l.contains("SYN_REPORT")); if (l.contains("KEY_POWER")) power++; }
        assertEquals(2, power);
        assertTrue(ShellProtocol.keyTestLines(null).isEmpty());
    }
    @Test public void neverKeepsMoreThanFortyLines() {
        java.util.List<String> many = new java.util.ArrayList<>();
        for (int i = 0; i < 500; i++) many.add("[ 1.0] /dev/input/event3: EV_KEY KEY_VOLUMEUP DOWN");
        assertEquals(40, ShellProtocol.keyTestLines(many).size());
    }
    @Test public void manyInputDevicesCannotCrowdOutTheSideKey() {
        // 30 devices = 60 header lines before the first event: with one shared cap the KEY_POWER lines were dropped and the verdict read "side key NOT seen" although it was pressed
        java.util.List<String> raw = new java.util.ArrayList<>();
        for (int i = 0; i < 30; i++) { raw.add("add device " + (i + 1) + ": /dev/input/event" + i); raw.add("  name:     \"device-" + i + "\""); }
        raw.add("[   45.000000] /dev/input/event3: EV_KEY       KEY_POWER            DOWN");
        raw.add("[   46.500000] /dev/input/event3: EV_KEY       KEY_POWER            UP");
        java.util.List<String> kept = ShellProtocol.keyTestLines(raw);
        int power = 0, headers = 0;
        for (String l : kept) { if (l.contains("KEY_POWER")) power++; else headers++; }
        assertEquals(kept.toString(), 2, power);
        assertEquals("the device list is cut at its own cap", 24, headers);
        String verdict = ShellProtocol.keyTestSummary("KEYTEST-START\n" + String.join("\n", kept) + "\nKEYTEST-END\nKEYTEST-LINES 62\ndisable2(0x8) -> OK\ncleared\n");
        assertTrue(verdict, verdict.contains("side key WAS seen (held 1.5 s)"));
    }
    @Test public void summarisesWhatTheHelperFound() {
        String seen = "KEYTEST-START\n" + String.join("\n", ShellProtocol.keyTestLines(Arrays.asList(GETEVENT))) + "\nKEYTEST-END\nuid=2000\ndisable2(0x8) -> OK\ngetDisableFlags -> OK [0, 8]\ncleared\n";
        String s = ShellProtocol.keyTestSummary(seen);
        assertTrue(s, s.contains("side key WAS seen (held 1.5 s)"));
        assertTrue(s, s.contains("MAY switch the normal power menu off"));
        String none = ShellProtocol.keyTestSummary("KEYTEST-START\nKEYTEST-END\nuid=2000\ndisable2(0x8) -> java.lang.SecurityException REFUSED\n");
        assertTrue(none, none.contains("NOT seen"));
        assertTrue(none, none.contains("REFUSED"));
        assertTrue(ShellProtocol.keyTestSummary(null).contains("no answer"));
        assertTrue(ShellProtocol.keyTestSummary("  ").contains("no answer"));
    }
    @Test public void nothingFromGeteventIsNotReportedAsNotPressed() {
        // 0 lines in all means the shell could not read the input devices: "the key was not pressed" would be a false comfort and could end the Power menu v2 idea for the wrong reason
        String s = ShellProtocol.keyTestSummary("KEYTEST-START\nKEYTEST-END\nKEYTEST-LINES 0\nuid=2000\ndisable2(0x8) -> OK\ncleared\n");
        assertTrue(s, s.contains("could NOT read the input devices"));
        assertFalse(s, s.contains("was NOT seen"));
        assertTrue(s, s.contains("MAY switch the normal power menu off"));
        String some = ShellProtocol.keyTestSummary("KEYTEST-START\nadd device 1: /dev/input/event0\nKEYTEST-END\nKEYTEST-LINES 14\nuid=2000\ndisable2(0x8) -> OK\ncleared\n");
        assertTrue(some, some.contains("14 input lines were read, so getevent works"));
        assertTrue(some, some.contains("was NOT seen"));
        String pressed = ShellProtocol.keyTestSummary("KEYTEST-START\n[ 5.0] /dev/input/event2: EV_KEY KEY_POWER DOWN\n[ 6.0] /dev/input/event2: EV_KEY KEY_POWER UP\nKEYTEST-END\nKEYTEST-LINES 9\ndisable2(0x8) -> OK\n");
        assertTrue(pressed, pressed.contains("side key WAS seen (held 1.0 s)"));
    }
    @Test public void aKeyWithoutATimestampStillCounts() {
        assertTrue(ShellProtocol.keyTestSummary("/dev/input/event3: EV_KEY KEY_POWER DOWN").contains("side key WAS seen"));
    }

    @Test public void setSystemAllowListIsStrict() {   // BN1: Mono audio / Audio balance through the helper
        assertArrayEquals(new String[] { "master_mono", "1" }, ShellProtocol.setSystem("SETSYS master_mono 1"));
        assertArrayEquals(new String[] { "master_mono", "0" }, ShellProtocol.setSystem("  SETSYS   master_mono 0 "));
        assertArrayEquals(new String[] { "master_balance", "-0.5" }, ShellProtocol.setSystem("SETSYS master_balance -0.5"));
        assertArrayEquals(new String[] { "master_balance", "1" }, ShellProtocol.setSystem("SETSYS master_balance 1"));
        assertArrayEquals(new String[] { "master_balance", "-1.000" }, ShellProtocol.setSystem("SETSYS master_balance -1.000"));
        assertArrayEquals(new String[] { "master_balance", "0" }, ShellProtocol.setSystem("SETSYS master_balance 0"));
        assertNull(ShellProtocol.setSystem(null));
        assertNull(ShellProtocol.setSystem("SETSYS master_mono 2"));
        assertNull(ShellProtocol.setSystem("SETSYS master_mono 1; reboot"));
        assertNull(ShellProtocol.setSystem("SETSYS master_mono $(id)"));
        assertNull(ShellProtocol.setSystem("SETSYS master_balance 1.5"));
        assertNull(ShellProtocol.setSystem("SETSYS master_balance -1.01"));
        assertNull(ShellProtocol.setSystem("SETSYS master_balance 0.1234"));    // more than three decimals
        assertNull(ShellProtocol.setSystem("SETSYS master_balance NaN"));
        assertNull(ShellProtocol.setSystem("SETSYS master_balance 1e0"));
        assertNull(ShellProtocol.setSystem("SETSYS screen_off_timeout 1000"));   // not on the allow-list
        assertNull(ShellProtocol.setSystem("SETSYS master_mono"));
        assertNull(ShellProtocol.setSystem("setsys master_mono 1"));
        assertNull(ShellProtocol.setSystem("FORCESTOP com.example.app"));
    }
}
