package com.tj.launcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** CH1: every rule of the pocket return that can be decided without a device. The switch OFF must be the old launcher, exactly. */
public class PocketRulesTest {

    // ---------------------------------------------------------------- the switch: OFF = nothing changes

    @Test public void switchOffIsNeverActive() {
        for (String via : new String[] { PocketRules.VIA_TILE, PocketRules.VIA_ICON, "home2", "volcode", "backs", "flurry", "rshake", "", null }) {
            assertFalse("via " + via, PocketRules.active(false, via));
        }
    }

    @Test public void onlyTheTileAndTheIconCount() {
        assertTrue(PocketRules.active(true, PocketRules.VIA_TILE));
        assertTrue(PocketRules.active(true, PocketRules.VIA_ICON));
        for (String via : new String[] { "home2", "volcode", "backs", "volboth", "flurry", "rshake", "rknock", "", null, "TILE", "plain-pin" }) {
            assertFalse("via " + via, PocketRules.active(true, via));
        }
    }

    @Test public void defaultIsOffAndTheKeyIsStable() {
        assertFalse(PocketRules.defaultOn());
        assertEquals("pocket_return", PocketRules.KEY);   // stored in the preferences: renaming it would reset Tj's choice
    }

    @Test public void lockPrepWithTheSwitchOffIsExactlyTheOldTwoTimes() {
        assertArrayEquals(new long[] { 700L, 6500L }, PocketRules.lockPrepDelaysMs(false));
    }

    @Test public void lockPrepWithTheSwitchOnStartsImmediatelyAndKeepsTheOldTimes() {
        long[] d = PocketRules.lockPrepDelaysMs(true);
        assertArrayEquals(new long[] { 0L, 250L, 700L, 6500L }, d);
        long[] old = PocketRules.lockPrepDelaysMs(false);
        for (long o : old) assertTrue("old time " + o + " still there", contains(d, o));
        for (int i = 1; i < d.length; i++) assertTrue("ascending", d[i] > d[i - 1]);
    }

    @Test public void lockPrepTablesCannotBeChangedByACaller() {
        long[] a = PocketRules.lockPrepDelaysMs(false);
        a[0] = 99999L;
        assertEquals(700L, PocketRules.lockPrepDelaysMs(false)[0]);
        assertNotSame(PocketRules.lockPrepDelaysMs(true), PocketRules.lockPrepDelaysMs(true));
    }

    @Test public void lockNotificationCleanNeedsEveryCondition() {
        assertTrue(PocketRules.lockNotificationClean(true, true, false, true, false, true, false));
        assertFalse("switch off", PocketRules.lockNotificationClean(false, true, false, true, false, true, false));
        assertFalse("quickstep session (its own clean-up)", PocketRules.lockNotificationClean(true, false, false, true, false, true, false));
        assertFalse("paused", PocketRules.lockNotificationClean(true, true, true, true, false, true, false));
        assertFalse("not locked", PocketRules.lockNotificationClean(true, true, false, false, false, true, false));
        assertFalse("handshake armed", PocketRules.lockNotificationClean(true, true, false, true, true, true, false));
        assertFalse("no helper", PocketRules.lockNotificationClean(true, true, false, true, false, false, false));
        assertFalse("already done this lock", PocketRules.lockNotificationClean(true, true, false, true, false, true, true));
    }

    @Test public void lockNotificationCleanIsNeverTrueWithTheSwitchOff() {
        boolean[] b = { false, true };
        for (boolean a : b) for (boolean c : b) for (boolean d : b) for (boolean e : b) for (boolean f : b)
            assertFalse(PocketRules.lockNotificationClean(false, a, c, d, e, f, false));
    }

    // ---------------------------------------------------------------- which apps are stopped

    /** A phone's packages: system apps and what the old rule refuses are named in the sets. */
    private static PocketRules.Facts facts(final Set<String> system, final Set<String> never) {
        return new PocketRules.Facts() {
            @Override public boolean mayClose(String pkg) { return !never.contains(pkg); }
            @Override public boolean isSystemApp(String pkg) { return system.contains(pkg); }
        };
    }
    private static Set<String> set(String... s) { return new HashSet<>(Arrays.asList(s)); }

    @Test public void keepSetIsLinkMeterAndTheKeyboards() {
        assertTrue(PocketRules.keepPackage("com.tj.linkmeter"));
        assertTrue(PocketRules.keepPackage("com.google.android.inputmethod.latin"));
        assertTrue(PocketRules.keepPackage("app.swoop.keyboard"));
        assertTrue(PocketRules.keepPackage("app.swoop.keyboard.debug"));
        assertTrue(PocketRules.keepPackage("com.touchtype.swiftkey"));
        assertFalse(PocketRules.keepPackage("com.whatsapp"));
        assertFalse(PocketRules.keepPackage("com.tj.launcher"));   // the Launcher is kept by the old rule (self), not by this one
        assertFalse(PocketRules.keepPackage(null));
        assertFalse(PocketRules.keepPackage(""));
    }

    @Test public void stopListNeverHasLinkMeterKeyboardsSystemOrOldRuleRefusals() {
        List<String> origin = Arrays.asList("com.whatsapp", "com.tj.linkmeter", "com.tj.launcher");
        List<String> tasks = Arrays.asList("app.swoop.keyboard", "com.android.settings", "com.google.android.apps.chrome", "com.android.systemui");
        List<String> notified = Arrays.asList("com.google.android.inputmethod.latin", "org.telegram.messenger");
        PocketRules.Facts f = facts(set("com.android.settings"), set("com.tj.launcher", "com.android.systemui"));
        List<String> out = PocketRules.stopList(origin, tasks, notified, f);
        assertEquals(Arrays.asList("com.whatsapp", "com.google.android.apps.chrome", "org.telegram.messenger"), out);
    }

    @Test public void stopListHasNoDuplicatesAndKeepsTheOrderOriginTasksNotified() {
        List<String> out = PocketRules.stopList(Arrays.asList("a.one", "a.two"), Arrays.asList("a.two", "a.three"), Arrays.asList("a.three", "a.four", "a.one"), facts(set(), set()));
        assertEquals(Arrays.asList("a.one", "a.two", "a.three", "a.four"), out);
    }

    @Test public void stopListToleratesMissingListsAndBlankNames() {
        PocketRules.Facts f = facts(set(), set());
        assertEquals(Collections.emptyList(), PocketRules.stopList(null, null, null, f));
        assertEquals(Collections.singletonList("a.x"), PocketRules.stopList(null, Arrays.asList(null, "", "a.x"), null, f));
    }

    @Test public void stopListNeverAddsAnAppThatIsOnlyInstalled() {
        // the lists are what is OPEN or notifying: an installed app that is in none of them is never in the answer
        List<String> out = PocketRules.stopList(Collections.<String>emptyList(), Collections.singletonList("a.open"), Collections.<String>emptyList(), facts(set(), set()));
        assertEquals(Collections.singletonList("a.open"), out);
    }

    // ---------------------------------------------------------------- the bridge: whether to start the phone job

    @Test public void helloNoAnswerSkipsThePhoneSteps() {
        assertFalse(PocketRules.helloVerdict(null).start);
        assertFalse(PocketRules.helloVerdict(PocketRules.Hello.none()).start);
        assertTrue(PocketRules.helloVerdict(PocketRules.Hello.none()).text.contains("did not answer"));
    }

    @Test public void helloRefusedSaysWhy() {
        PocketRules.Verdict v = PocketRules.helloVerdict(new PocketRules.Hello(true, false, false, false, ""));
        assertFalse(v.start);
        assertTrue(v.text.contains("signing key"));
    }

    @Test public void helloOnlyStartsForTheLinkedTablet() {
        assertFalse(PocketRules.helloVerdict(new PocketRules.Hello(true, true, false, true, "")).start);   // phone-role LinkMeter
        PocketRules.Verdict unlinked = PocketRules.helloVerdict(new PocketRules.Hello(true, true, true, false, "No phone is linked"));
        assertFalse(unlinked.start);
        assertTrue(unlinked.text.contains("No phone is linked"));
        PocketRules.Verdict ok = PocketRules.helloVerdict(new PocketRules.Hello(true, true, true, true, ""));
        assertTrue(ok.start);
    }

    // ---------------------------------------------------------------- the watcher: when the tablet's Wi-Fi goes off

    private static PocketRules.Look look(String state, boolean sent) { return new PocketRules.Look(true, true, state, sent, ""); }

    @Test public void waitsForTheSleepOrderWhileTheJobRuns() {
        PocketRules.Step s = PocketRules.next(look("RUNNING", false), false, 3_000);
        assertFalse(s.wifiOffNow);
        assertFalse(s.finished);
        assertEquals(PocketRules.FAST_POLL_MS, s.nextMs);
    }

    @Test public void wifiGoesOffWhenThePhoneAcceptedItsSleepOrder() {
        PocketRules.Step a = PocketRules.next(look("RUNNING", true), false, 6_000);
        assertTrue(a.wifiOffNow);
        assertFalse(a.finished);
        assertEquals(PocketRules.SLOW_POLL_MS, a.nextMs);
        PocketRules.Step b = PocketRules.next(look("SENT", true), false, 6_000);
        assertTrue(b.wifiOffNow);
        assertFalse(b.finished);
    }

    @Test public void wifiGoesOffAtTheCapEvenWithNoSleepOrder() {
        assertFalse(PocketRules.next(look("RUNNING", false), false, PocketRules.SENT_CAP_MS - 1).wifiOffNow);
        PocketRules.Step s = PocketRules.next(look("RUNNING", false), false, PocketRules.SENT_CAP_MS);
        assertTrue(s.wifiOffNow);
        assertFalse(s.finished);
        assertTrue(s.why.contains("anyway"));
    }

    @Test public void wifiIsSwitchedOffOnlyOnce() {
        for (String st : new String[] { "RUNNING", "SENT", "DONE", "FAILED", "IDLE", "???" }) {
            assertFalse("state " + st, PocketRules.next(look(st, true), true, 30_000).wifiOffNow);
        }
        assertFalse(PocketRules.next(PocketRules.Look.none(), true, 1_000).wifiOffNow);
    }

    @Test public void aFinishedJobEndsTheWatchAndFreesTheWifi() {
        for (String st : new String[] { "DONE", "FAILED" }) {
            PocketRules.Step s = PocketRules.next(look(st, false), false, 2_000);
            assertTrue(st, s.wifiOffNow);
            assertTrue(st, s.finished);
        }
    }

    @Test public void noAnswerOrRefusalEndsTheWatchAndFreesTheWifiAtOnce() {
        PocketRules.Step none = PocketRules.next(PocketRules.Look.none(), false, 0);
        assertTrue(none.wifiOffNow);
        assertTrue(none.finished);
        PocketRules.Step refused = PocketRules.next(new PocketRules.Look(true, false, "", false, "refused"), false, 0);
        assertTrue(refused.wifiOffNow);
        assertTrue(refused.finished);
        PocketRules.Step nul = PocketRules.next(null, false, 0);
        assertTrue(nul.wifiOffNow);
        assertTrue(nul.finished);
    }

    @Test public void anUnknownOrIdleStateEndsTheWatch() {
        for (String st : new String[] { "IDLE", "", "WHAT" }) {
            PocketRules.Step s = PocketRules.next(look(st, false), false, 1_000);
            assertTrue(st, s.finished);
            assertTrue(st, s.wifiOffNow);
        }
    }

    @Test public void theDeadlineIsCheckedFirstWhateverTheAnswerSays() {
        // a job that says RUNNING for ever must still end the watch
        PocketRules.Step s = PocketRules.next(look("RUNNING", false), false, PocketRules.TOTAL_CAP_MS);
        assertTrue(s.finished);
        assertTrue(s.wifiOffNow);
        PocketRules.Step s2 = PocketRules.next(look("RUNNING", false), true, PocketRules.TOTAL_CAP_MS + 5_000);
        assertTrue(s2.finished);
        assertFalse(s2.wifiOffNow);
        // an absolute bound, not one relative to the constant: the whole watch is under two minutes
        assertTrue(PocketRules.TOTAL_CAP_MS <= 120_000L);
        assertTrue(PocketRules.SENT_CAP_MS <= 30_000L);
    }

    @Test public void aWatcherThatNeverGetsADoneStillEndsAndFreesTheWifiOnce() {
        // simulate the glue loop: the job says RUNNING for ever, no sleep order. Every step must make progress toward the end.
        boolean wifiOff = false;
        int wifiOffs = 0;
        long t = 0;
        int rounds = 0;
        while (true) {
            PocketRules.Step s = PocketRules.next(look("RUNNING", false), wifiOff, t);
            if (s.wifiOffNow) { assertFalse(wifiOff); wifiOff = true; wifiOffs++; }
            if (s.finished) break;
            assertTrue("a positive wait", s.nextMs > 0);
            t += s.nextMs;
            assertTrue("bounded number of looks", ++rounds < 200);
        }
        assertEquals(1, wifiOffs);
        assertTrue("ends by the total cap", t >= PocketRules.TOTAL_CAP_MS && t < PocketRules.TOTAL_CAP_MS + PocketRules.SLOW_POLL_MS);
    }

    // ---------------------------------------------------------------- the one-line summary

    @Test public void summaryIsOneLineWithTheFacts() {
        String s = PocketRules.summary("12:00:01", "LinkMeter is ready", "DONE: the phone is asleep", true);
        assertEquals("last pocket return 12:00:01; LinkMeter is ready; phone: DONE: the phone is asleep; tablet Wi-Fi off", s);
        assertFalse(s.contains("\n"));
        assertEquals("no pocket return yet; tablet Wi-Fi left as it was", PocketRules.summary(null, null, null, false));
    }

    private static boolean contains(long[] a, long v) { for (long x : a) if (x == v) return true; return false; }
}
