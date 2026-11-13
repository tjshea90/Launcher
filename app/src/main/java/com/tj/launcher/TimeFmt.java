package com.tj.launcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * CA1 M9: the clock strings of the lock log and the status lines. A new SimpleDateFormat per line (it parses its pattern and builds a Calendar each time) was garbage on every log line, many per lock
 * cycle; one formatter per pattern per thread is reused. The time zone is set before each use, so a zone change (travel, the settings) shows at once, exactly as a new formatter did.
 */
public final class TimeFmt {
    private TimeFmt() {}

    private static final ThreadLocal<SimpleDateFormat> HMS = new ThreadLocal<SimpleDateFormat>() {
        @Override protected SimpleDateFormat initialValue() { return new SimpleDateFormat("HH:mm:ss", Locale.US); }
    };
    private static final ThreadLocal<SimpleDateFormat> HMS_MS = new ThreadLocal<SimpleDateFormat>() {
        @Override protected SimpleDateFormat initialValue() { return new SimpleDateFormat("HH:mm:ss.SSS", Locale.US); }
    };

    /** "HH:mm:ss" of a wall-clock time in milliseconds. */
    public static String hms(long wallMs) { return fmt(HMS.get(), wallMs); }

    /** "HH:mm:ss.SSS" of a wall-clock time in milliseconds. */
    public static String hmsMs(long wallMs) { return fmt(HMS_MS.get(), wallMs); }

    private static String fmt(SimpleDateFormat f, long wallMs) {
        f.setTimeZone(TimeZone.getDefault());
        return f.format(new Date(wallMs));
    }
}
