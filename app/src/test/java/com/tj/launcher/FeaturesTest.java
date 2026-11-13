package com.tj.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** BY1a C6/C7: a status / bookkeeping string is written only when it changes. */
public class FeaturesTest {
    @Test public void aRepeatedValueIsNotWrittenAgain() {
        assertFalse(Features.differs("custom: already x", "custom: already x"));
        assertFalse(Features.differs(null, null));   // nothing stored, nothing to store
    }

    @Test public void aNewOrChangedValueIsWritten() {
        assertTrue(Features.differs(null, "custom"));          // first write
        assertTrue(Features.differs("custom", "quickstep"));
        assertTrue(Features.differs("custom", ""));
        assertTrue(Features.differs("", "custom"));
        assertTrue(Features.differs("custom", null));          // a removal is a change
    }
}
