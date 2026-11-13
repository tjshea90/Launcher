package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.ref.WeakReference;
import org.junit.Test;

/** BY1a A2/C13: the Recents flag and the task description are sent once per Launcher instance, the flag once per task id. */
public class RecentsGuardTest {
    @Test public void sameInstanceMeansTheVeryObject() {
        Object a = new Object(), b = new Object();
        assertTrue(RecentsGuard.sameInstance(new WeakReference<Object>(a), a));
        assertFalse("a recreated Launcher is a new instance", RecentsGuard.sameInstance(new WeakReference<Object>(a), b));
        assertFalse("nothing handled yet", RecentsGuard.sameInstance(null, a));
        assertFalse(RecentsGuard.sameInstance(new WeakReference<Object>(null), a));   // the old instance is gone
        assertFalse(RecentsGuard.sameInstance(new WeakReference<Object>(null), null));
    }

    @Test public void aKnownTaskIdIsNotFlaggedAgain() {
        RecentsGuard.FlaggedIds f = new RecentsGuard.FlaggedIds();
        assertTrue(f.needsFlag(41, true));
        f.flagged(41);
        assertFalse(f.needsFlag(41, true));
        assertTrue("a new task", f.needsFlag(42, true));
        assertTrue("the other screens still flag every task", f.needsFlag(41, false));
    }

    @Test public void aTaskWithoutInfoIsAlwaysFlagged() {
        RecentsGuard.FlaggedIds f = new RecentsGuard.FlaggedIds();
        f.flagged(-1);
        assertTrue(f.needsFlag(-1, true));
        assertEquals(0, f.size());
    }

    @Test public void theMemoryIsBounded() {
        RecentsGuard.FlaggedIds f = new RecentsGuard.FlaggedIds();
        for (int i = 0; i < RecentsGuard.FlaggedIds.MAX; i++) f.flagged(i);
        assertEquals(RecentsGuard.FlaggedIds.MAX, f.size());
        f.flagged(5);   // known: nothing changes
        assertEquals(RecentsGuard.FlaggedIds.MAX, f.size());
        f.flagged(1000);   // full: starts over
        assertEquals(1, f.size());
        assertFalse(f.needsFlag(1000, true));
        assertTrue("forgotten = flagged once more, harmless", f.needsFlag(5, true));
    }
}
