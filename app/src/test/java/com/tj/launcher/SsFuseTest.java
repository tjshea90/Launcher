package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** BN1, review C findings 2 and 3: the look-alike's crash fuse (a page crash never trips the crash-loop breaker) and the Recents label rule. */
public class SsFuseTest {
    private static StackTraceElement f(String cls, String m) { return new StackTraceElement(cls, m, cls.substring(cls.lastIndexOf('.') + 1) + ".java", 10); }

    private static Throwable crash(StackTraceElement... frames) {
        Throwable t = new IllegalStateException("boom");
        t.setStackTrace(frames);
        return t;
    }

    @Test public void classNamesOfTheLookAlike() {
        assertTrue(SsFuse.isLookAlikeClass("com.tj.launcher.SsRows"));
        assertTrue(SsFuse.isLookAlikeClass("com.tj.launcher.SsPagesTts$Writer"));
        assertTrue(SsFuse.isLookAlikeClass("com.tj.launcher.SsPageActivity"));
        assertTrue(SsFuse.isLookAlikeClass("com.tj.launcher.SettingsSpoof"));
        assertTrue(SsFuse.isLookAlikeClass("com.tj.launcher.SettingsSpoof$1"));
        assertFalse(SsFuse.isLookAlikeClass("com.tj.launcher.ShellServer"));
        assertFalse(SsFuse.isLookAlikeClass("com.tj.launcher.TjAccessibilityService"));
        assertFalse(SsFuse.isLookAlikeClass("com.tj.launcher.Settings"));
        assertFalse(SsFuse.isLookAlikeClass("com.tj.launcher.SettingsOther"));
        assertFalse(SsFuse.isLookAlikeClass("com.other.SsRows"));
        assertFalse(SsFuse.isLookAlikeClass("android.widget.TextView"));
        assertFalse(SsFuse.isLookAlikeClass(null));
    }

    @Test public void aCrashInAPageIsTheFusesAndACrashInHomeIsNot() {
        Throwable page = crash(f("android.widget.TextView", "setText"), f("com.tj.launcher.SsRows", "row"), f("com.tj.launcher.SsPageActivity", "onCreate"), f("android.app.Activity", "performCreate"));
        assertTrue(SsFuse.isLookAlikeCrash(page));
        Throwable home = crash(f("android.view.View", "draw"), f("com.android.launcher3.Workspace", "onDraw"), f("com.tj.launcher.TjHooks", "onTouch"));
        assertFalse(SsFuse.isLookAlikeCrash(home));
        Throwable service = crash(f("android.os.Binder", "execTransact"), f("com.tj.launcher.TjAccessibilityService", "onAccessibilityEvent"), f("com.tj.launcher.SsRows", "never"));
        assertFalse("the FIRST frame of our own code decides", SsFuse.isLookAlikeCrash(service));
        assertFalse("no frame of ours at all", SsFuse.isLookAlikeCrash(crash(f("android.os.Handler", "dispatchMessage"))));
        assertFalse("no stack at all", SsFuse.isLookAlikeCrash(crash()));
        assertFalse("no exception", SsFuse.isLookAlikeCrash(null));
    }

    @Test public void theCauseChainIsFollowedOutermostFirst() {
        Throwable inner = crash(f("com.tj.launcher.SsPagesStorage", "load"));
        RuntimeException wrapped = new RuntimeException("wrapped", inner);
        wrapped.setStackTrace(new StackTraceElement[] { f("java.lang.Thread", "run") });   // the outer exception has no frame of ours: its cause decides
        assertTrue(SsFuse.isLookAlikeCrash(wrapped));
        RuntimeException homeWraps = new RuntimeException("wrapped", inner);
        homeWraps.setStackTrace(new StackTraceElement[] { f("com.tj.launcher.TjAccessibilityService", "run") });   // the outer one has an own frame that is not the look-alike's: it decides
        assertFalse(SsFuse.isLookAlikeCrash(homeWraps));
    }

    @Test public void theFuseTakesOnlyTheFirstCrash() {
        Throwable page = crash(f("com.tj.launcher.SsConversation", "column"));
        assertTrue(SsFuse.fuseHandles(page, false));
        assertFalse("a repeat while the fuse is blown goes to the breaker", SsFuse.fuseHandles(page, true));
        assertFalse(SsFuse.fuseHandles(crash(f("com.tj.launcher.Mode", "set")), false));
    }

    /** Review C #2: the look-alike's own Recents card is titled like the real Settings; while it is the wanted entry its label is a drawer label and the label closer leaves the card alone. */
    @Test public void theLookAlikeLabelIsNeverBlockedWhileItIsInTheDrawer() {
        assertEquals("Settings", SettingsSpoof.LABEL);
        Set<String> hidden = new HashSet<>(Arrays.asList("Settings", "Secret"));   // the real Settings is out of the drawer now: its label is a hidden app's
        Set<String> drawer = new HashSet<>(Arrays.asList(SettingsSpoof.LABEL));
        Set<String> out = RecentsFilter.blockedLabels(hidden, new HashSet<String>(), drawer);
        assertFalse(out.contains("settings"));
        assertTrue(out.contains("secret"));
        assertTrue("without the look-alike in the drawer the label stays blocked", RecentsFilter.blockedLabels(hidden, new HashSet<String>(), new HashSet<String>()).contains("settings"));
    }
}
