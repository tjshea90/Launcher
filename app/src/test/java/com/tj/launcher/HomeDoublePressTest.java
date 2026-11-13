package com.tj.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HomeDoublePressTest {
    @Test public void twoQuickPressesAreADoublePress() { assertTrue(HomeDoublePress.isDouble(1_000, 1_400)); }
    @Test public void slowSecondPressIsNot() { assertFalse(HomeDoublePress.isDouble(1_000, 2_100)); }
    @Test public void firstEverPressIsNot() { assertFalse(HomeDoublePress.isDouble(0, 1_000)); }
    @Test public void clockGoingBackwardsIsNot() { assertFalse(HomeDoublePress.isDouble(5_000, 4_000)); }

    /** BY1a A3: the router's order (read the two last presses, then record this one) with the times kept in memory: the double press and the L10 triple press are still recognised. */
    @Test public void thePressesKeptInMemoryFeedTheSameRules() {
        long t = 1_000_000;
        Mode.recordHomePress(null, t - 100_000);
        Mode.recordHomePress(null, t - 90_000);   // an old burst: a known start
        assertEquals(t - 90_000, Mode.lastHomePressMs(null));
        assertEquals(t - 100_000, Mode.homePressBeforeLastMs(null));
        long last = Mode.lastHomePressMs(null);
        Mode.recordHomePress(null, t);
        assertFalse(HomeDoublePress.isDouble(last, t));
        last = Mode.lastHomePressMs(null);
        long last2 = Mode.homePressBeforeLastMs(null);
        Mode.recordHomePress(null, t + 400);
        assertTrue(HomeDoublePress.isDouble(last, t + 400));
        assertFalse(ProfileSession.homeFailsafe(false, true, t + 400, last2, last));   // two presses only
        last = Mode.lastHomePressMs(null);
        last2 = Mode.homePressBeforeLastMs(null);
        Mode.recordHomePress(null, t + 800);
        assertTrue(ProfileSession.homeFailsafe(false, true, t + 800, last2, last));
        assertEquals(t + 800, Mode.lastHomePressMs(null));
        assertEquals(t + 400, Mode.homePressBeforeLastMs(null));
    }
}
