package com.tj.launcher;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;

public class TimeFmtTest {
    @Test public void sameStringsAsANewFormatterEachTime() {   // CA1 M9
        long t = 1_762_995_430_123L;
        assertEquals(new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date(t)), TimeFmt.hms(t));
        assertEquals(new SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(new Date(t)), TimeFmt.hmsMs(t));
    }

    @Test public void aTimeZoneChangeShowsAtOnce() {
        TimeZone old = TimeZone.getDefault();
        try {
            long t = 0L;
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals("00:00:00", TimeFmt.hms(t));
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"));
            assertEquals("02:00:00", TimeFmt.hms(t));
            assertEquals("02:00:00.000", TimeFmt.hmsMs(t));
        } finally { TimeZone.setDefault(old); }
    }
}
