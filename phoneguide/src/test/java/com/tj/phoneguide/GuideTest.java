package com.tj.phoneguide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class GuideTest {
    @Test public void switchesOnWhenNothingIsOn() {
        assertEquals("settings put secure enabled_accessibility_services " + A11yLine.COMPONENT, A11yLine.build("null"));
        assertEquals("settings put secure enabled_accessibility_services " + A11yLine.COMPONENT, A11yLine.build(""));
        assertEquals("settings put secure enabled_accessibility_services " + A11yLine.COMPONENT, A11yLine.build(null));
    }
    @Test public void keepsTheServicesThatAreAlreadyOn() {
        assertEquals("settings put secure enabled_accessibility_services a.b/c.D:" + A11yLine.COMPONENT, A11yLine.build("a.b/c.D"));
        assertEquals("settings put secure enabled_accessibility_services a.b/c.D:e.f/g.H:" + A11yLine.COMPONENT, A11yLine.build(" a.b/c.D:e.f/g.H \n"));
    }
    @Test public void doesNotAddItTwice() {
        String on = "x.y/z.Z:" + A11yLine.COMPONENT;
        assertEquals("settings put secure enabled_accessibility_services " + on, A11yLine.build(on));
    }

    private static java.util.List<String> lines() throws Exception {
        java.util.List<String> out = new java.util.ArrayList<>();
        File f = new File("src/main/assets/steps.txt");
        BufferedReader r = new BufferedReader(new FileReader(f));
        for (String l; (l = r.readLine()) != null; ) out.add(l);
        r.close();
        return out;
    }
    @Test public void everyCommandIsTheBareDeviceShellForm() throws Exception {
        int n = 0;
        for (String l : lines()) {
            if (!(l.startsWith("cmd:") || l.startsWith("alt:"))) continue;
            String c = l.substring(4).trim(); n++;
            assertFalse(l, c.isEmpty());
            assertFalse("no adb prefix in the OTG form: " + l, c.equals("adb") || c.startsWith("adb "));
        }
        assertTrue("the list has real content", n >= 40);
    }
    @Test public void noCommandIsDestructive() throws Exception {
        String[] banned = { "pm uninstall", "pm clear", "wipe", "factory", "rm -", "reboot", "setprop", "locksettings", "pm hide", "pm suspend", "svc wifi", "svc data", "airplane", "adb_enabled", "format" };
        for (String l : lines()) {
            if (!(l.startsWith("cmd:") || l.startsWith("alt:") || l.startsWith("undo:"))) continue;
            for (String b : banned) assertFalse(l + " contains " + b, l.contains(b));
        }
    }
    @Test public void coversEverythingTjAskedFor() throws Exception {
        StringBuilder all = new StringBuilder();
        for (String l : lines()) all.append(l).append('\n');
        String s = all.toString();
        String[] must = { "WRITE_SECURE_SETTINGS", "DUMP", "CHANGE_CONFIGURATION", "READ_EXTERNAL_STORAGE", "PACKAGE_USAGE_STATS", "GET_USAGE_STATS", "WRITE_SETTINGS", "RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND",
                "deviceidle whitelist +com.tj.launcher", "enabled_accessibility_services", "accessibility_enabled", "com.tj.launcher.ShellServer", "cmd package compile -m speed -f com.tj.launcher",
                "pm disable-user --user 0 com.android.browser", "pm get-max-users", "pm list users", "ro.crypto.type", "power_menu_actions", "device_demo_mode", "pkill -f com.tj.launcher.ShellServer", "pm enable com.android.browser" };
        for (String m : must) assertTrue("missing: " + m, s.contains(m));
    }

    // ------------------------------------------------------------------ Tj, 2026-11-05: "simple and intuitive and in a logical order in case I have limited time with the OTG cable"
    private static GuideParser.Guide guide() throws Exception {
        BufferedReader r = new BufferedReader(new FileReader(new File("src/main/assets/steps.txt")));
        try { return GuideParser.parse(r); } finally { r.close(); }
    }
    private static GuideParser.Group part(GuideParser.Guide g, String label) {
        for (GuideParser.Group x : g.groups) if (x.label().equals(label)) return x;
        throw new AssertionError("no group " + label);
    }
    private static boolean hasCmd(GuideParser.Card c) { return !c.cmds.isEmpty() || !c.alts.isEmpty(); }

    @Test public void thePartsComeInTheOrderOfALimitedCableSession() throws Exception {
        GuideParser.Guide g = guide();
        List<String> labels = new ArrayList<>();
        for (GuideParser.Group x : g.groups) labels.add(x.label());
        assertEquals(Arrays.asList("PART 1", "PART 2", "PART 3", "FALLBACK", "EXTRAS", "UNDO"), labels);
        for (int i = 0; i < 3; i++) assertFalse("the first three parts are open: " + labels.get(i), g.groups.get(i).collapsed);
        for (int i = 3; i < 6; i++) assertTrue("the rest is closed until tapped: " + labels.get(i), g.groups.get(i).collapsed);
        assertEquals("the plan box has one line per part", 4, g.plan.size());
    }
    @Test public void onlyPartTwoNeedsTheCableAndItsOneMustTypeLineStartsTheHelper() throws Exception {
        GuideParser.Guide g = guide();
        for (GuideParser.Card c : part(g, "PART 1").cards) assertFalse("PART 1 is tablet-only, no line to type: " + c.title, hasCmd(c));
        for (GuideParser.Card c : part(g, "PART 3").cards) assertFalse("PART 3 is buttons on the tablet, no line to type: " + c.title, hasCmd(c));
        GuideParser.Group cable = part(g, "PART 2");
        assertTrue("short enough to do in a few minutes", cable.cards.size() <= 6);
        GuideParser.Card start = null;
        for (GuideParser.Card c : cable.cards) if (c.must && !c.cmds.isEmpty() && c.cmds.get(0).contains("com.tj.launcher.ShellServer")) start = c;
        assertNotNull("the helper-start card is marked as the one that matters", start);
        assertFalse("it has a second form for adb apps that dislike the first", start.alts.isEmpty());
        for (GuideParser.Group x : g.groups) {
            if (x == cable) continue;
            for (GuideParser.Card c : x.cards) for (String l : c.cmds) assertFalse("the helper is started only in PART 2: " + c.title, l.contains("setsid") && l.contains("ShellServer"));
        }
    }
    @Test public void theFastPathHasNothingToCopyByHandAndTheTabletReportIsMarked() throws Exception {
        GuideParser.Guide g = guide();
        boolean report = false, send = false;
        for (GuideParser.Card c : part(g, "PART 3").cards) {
            if (c.must && c.title.contains("Collect the tablet report")) report = true;
            if (c.must && c.title.contains("Send the files")) send = true;
        }
        assertTrue("collecting the report is a must-do", report);
        assertTrue("sending the files is a must-do", send);
    }
    @Test public void openPartsAreShortAndPlain() throws Exception {
        GuideParser.Guide g = guide();
        for (int i = 0; i < 3; i++) for (GuideParser.Card c : g.groups.get(i).cards) {
            assertNotNull("every card says what it does: " + c.title, c.why);
            assertTrue(c.title + " is too long to read in a hurry", c.why.length() <= 520);
            assertTrue("title without a number (the app numbers the cards): " + c.title, !Character.isDigit(c.title.charAt(0)));
        }
    }
    @Test public void theByHandFallbackAndTheUndoLinesAreStillThere() throws Exception {
        GuideParser.Guide g = guide();
        GuideParser.Card perms = part(g, "FALLBACK").cards.get(0);
        assertEquals("one card, five grants (the fifth, PACKAGE_USAGE_STATS, is for the Settings look-alike's Battery numbers)", 5, perms.cmds.size());
        assertEquals("each grant keeps its own undo line", 5, perms.undos.size());
        assertTrue(part(g, "UNDO").cards.size() >= 5);
        assertTrue(part(g, "EXTRAS").cards.size() >= 15);
    }
    @Test public void theNewNotificationAndPowerKeySamplesAreAskedFor() throws Exception {
        StringBuilder all = new StringBuilder();
        for (String l : lines()) all.append(l).append('\n');
        String s = all.toString();
        assertTrue(s.contains("NotificationRecord("));
        assertTrue(s.contains("getevent -pl"));
        assertTrue(s.contains("Side-key test"));
    }
    @Test public void aTitleWithoutAColonKeepsItsWholeNameOnTheJumpButton() {
        GuideParser.Group x = new GuideParser.Group();
        x.title = "Steps";
        assertEquals("Steps", x.label());
        x.title = "PART 2: WITH the cable";
        assertEquals("PART 2", x.label());
    }
}
