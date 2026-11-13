package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** BY1a A2: a stored "" (the last full scan found no target app) is a known "none", never a reason to scan all labels on the calling thread. */
public class ReturnResetTargetTest {
    @Test public void whatAStoredAnswerMeans() {
        assertEquals("never scanned for this label: the one synchronous scan", ReturnReset.SAVED_NOTHING, ReturnReset.savedKind(null));
        assertEquals("the scan found no app: known none, re-checked in the background", ReturnReset.SAVED_NO_APP, ReturnReset.savedKind(""));
        assertEquals("a package: checked as installed, then used", ReturnReset.SAVED_PACKAGE, ReturnReset.savedKind("com.example.solitaire"));
    }
}
