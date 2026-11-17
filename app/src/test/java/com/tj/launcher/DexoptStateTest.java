package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** CG1c: the "Dexopt state:" block of {@code dumpsys package}, in the shape AOSP 10 prints it. */
public class DexoptStateTest {
    private static final String PKG = "com.tj.launcher";

    private static String dump(String block) {
        return "Packages:\n  Package [com.tj.launcher] (abc123):\n    userId=10090\n    versionCode=50\n\nDexopt state:\n" + block + "\nCompiler stats:\n  [com.tj.launcher]\n     base.apk - 123\n";
    }

    @Test public void speedProfileFromTheIdleJob() {
        DexoptState s = DexoptState.parse(dump("  [com.tj.launcher]\n    path: /data/app/com.tj.launcher-1/base.apk\n      arm: [status=speed-profile] [reason=bg-dexopt]\n"), PKG);
        assertTrue(s.found);
        assertEquals("speed-profile", s.status);
        assertEquals("bg-dexopt", s.reason);
        assertTrue(s.compiled());
        assertFalse(s.full());
        assertEquals("speed-profile (bg-dexopt)", s.line());
    }

    @Test public void shellCompileSpeedIsFull() {
        DexoptState s = DexoptState.parse(dump("  [com.tj.launcher]\n    path: /data/app/x/base.apk\n      arm64: [status=speed] [reason=cmdline]\n"), PKG);
        assertTrue(s.compiled());
        assertTrue(s.full());
        assertEquals("fully compiled", s.advice());
    }

    @Test public void everythingCountsAsFull() {
        assertTrue(DexoptState.parse(dump("  [com.tj.launcher]\n    path: p\n      arm: [status=everything] [reason=cmdline]\n"), PKG).full());
    }

    @Test public void justInstalledIsOnlyVerified() {
        DexoptState s = DexoptState.parse(dump("  [com.tj.launcher]\n    path: /data/app/x/base.apk\n      arm: [status=verify] [reason=install]\n"), PKG);
        assertTrue(s.found);
        assertFalse(s.compiled());
        assertFalse(s.full());
        assertEquals("verify (install)", s.line());
        assertTrue(s.advice().startsWith("not compiled yet"));
    }

    @Test public void runFromApkAndQuickenAreNotCompiled() {
        assertFalse(DexoptState.parse(dump("  [com.tj.launcher]\n    path: p\n      arm: [status=run-from-apk] [reason=unknown]\n"), PKG).compiled());
        assertFalse(DexoptState.parse(dump("  [com.tj.launcher]\n    path: p\n      arm: [status=quicken] [reason=install]\n"), PKG).compiled());
    }

    @Test public void aNeighbouringPackageIsNotMistakenForOurs() {
        String d = dump("  [com.android.settings]\n    path: /system/priv-app/Settings/Settings.apk\n      arm64: [status=speed] [reason=prebuilt]\n"
                + "  [com.tj.launcher]\n    path: /data/app/x/base.apk\n      arm64: [status=verify] [reason=install]\n"
                + "  [com.android.systemui]\n    path: s\n      arm64: [status=speed] [reason=prebuilt]\n");
        DexoptState s = DexoptState.parse(d, PKG);
        assertEquals("verify", s.status);
    }

    @Test public void ourPackageWithoutALineDoesNotBorrowTheNextOnes() {
        String d = dump("  [com.tj.launcher]\n    path: /data/app/x/base.apk\n  [com.android.systemui]\n    path: s\n      arm64: [status=speed] [reason=prebuilt]\n");
        assertSame(DexoptState.NONE, DexoptState.parse(d, PKG));
    }

    @Test public void theFirstCodePathWinsWhenThereAreSplits() {
        String d = dump("  [com.tj.launcher]\n    path: /data/app/x/base.apk\n      arm: [status=speed-profile] [reason=bg-dexopt]\n    path: /data/app/x/split_a.apk\n      arm: [status=verify] [reason=install]\n");
        assertEquals("speed-profile", DexoptState.parse(d, PKG).status);
    }

    @Test public void aReasonIsOptional() {
        DexoptState s = DexoptState.parse(dump("  [com.tj.launcher]\n    path: p\n      arm: [status=speed-profile]\n"), PKG);
        assertTrue(s.found);
        assertEquals("", s.reason);
        assertEquals("speed-profile", s.line());
    }

    @Test public void emulatorInstructionSetNamesAreFine() {
        assertEquals("speed-profile", DexoptState.parse(dump("  [com.tj.launcher]\n    path: p\n      x86: [status=speed-profile] [reason=install]\n"), PKG).status);
    }

    @Test public void garbageNullAndMissingBlocksGiveNone() {
        assertSame(DexoptState.NONE, DexoptState.parse(null, PKG));
        assertSame(DexoptState.NONE, DexoptState.parse("", PKG));
        assertSame(DexoptState.NONE, DexoptState.parse("Permission Denial: can't dump PackageManager from pid=1 uid=10090 due to missing android.permission.DUMP", PKG));
        assertSame(DexoptState.NONE, DexoptState.parse("Packages:\n  [com.tj.launcher]\n      arm: [status=speed] [reason=cmdline]\n", PKG));   // no "Dexopt state:" heading: not the block
        assertSame(DexoptState.NONE, DexoptState.parse(dump("  [com.tj.launcher]\n"), null));
        assertFalse(DexoptState.NONE.compiled());
        assertFalse(DexoptState.NONE.full());
        assertTrue(DexoptState.NONE.line().startsWith("not known"));
        assertEquals("", DexoptState.NONE.advice());
    }

    @Test public void windowsLineEndingsAreFine() {
        assertEquals("speed", DexoptState.parse(dump("  [com.tj.launcher]\n    path: p\n      arm: [status=speed] [reason=cmdline]\n").replace("\n", "\r\n"), PKG).status);
    }

    @Test public void staleWhenNothingWasStoredOrTheVersionChanged() {
        long now = 1_000_000_000L, max = 6L * 3600_000L;
        assertTrue(DexoptState.stale(0, 0L, 50, now, max));              // never stored
        assertTrue(DexoptState.stale(49, now - 1000, 50, now, max));     // an update throws the compiled code away: the old words say nothing
        assertFalse(DexoptState.stale(50, now - 1000, 50, now, max));    // same version, fresh
        assertTrue(DexoptState.stale(50, now - max - 1, 50, now, max));  // older than the limit: the idle job may have compiled since
        assertFalse(DexoptState.stale(50, now - max, 50, now, max));     // exactly at the limit is still fresh
        assertTrue(DexoptState.stale(50, now + 5000, 50, now, max));     // the clock went backwards
    }

    @Test public void ofRebuildsAStoredReading() {
        DexoptState s = DexoptState.of("speed", "cmdline");
        assertTrue(s.found);
        assertTrue(s.full());
        assertEquals("speed (cmdline)", s.line());
        assertEquals("verify", DexoptState.of("verify", null).line());
        assertSame(DexoptState.NONE, DexoptState.of("", "install"));
        assertSame(DexoptState.NONE, DexoptState.of(null, "install"));
    }

    @Test public void aLookThatGaveNothingIsNotRepeatedAtEveryStart() {   // CG1 review C
        long fail = 2L * 3600_000L, forced = 30_000L, now = 10_000_000_000L;
        assertTrue("a reading for this version: the age rule decides elsewhere", DexoptState.tryDue(true, now - 1, now, false, fail, forced));
        assertTrue("never tried", DexoptState.tryDue(false, 0, now, false, fail, forced));
        assertFalse("tried a minute ago, nothing found, a plain start", DexoptState.tryDue(false, now - 60_000, now, false, fail, forced));
        assertTrue("two hours later", DexoptState.tryDue(false, now - fail, now, false, fail, forced));
        assertFalse("Launcher control opened twice within 30 s", DexoptState.tryDue(false, now - 10_000, now, true, fail, forced));
        assertTrue("Launcher control opened after 30 s", DexoptState.tryDue(false, now - forced, now, true, fail, forced));
        assertTrue("the clock went backwards", DexoptState.tryDue(false, now + 5_000, now, false, fail, forced));
    }
}
