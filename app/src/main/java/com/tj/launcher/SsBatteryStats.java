package com.tj.launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BN1: the three numbers of the Battery page that Android keeps only in its hidden BatteryStats (the estimate, the start of the statistics = "Last full charge", and the screen-on time since then). The launcher holds
 * the DUMP permission (adb grant), so it reads them the way an adb shell would: {@code dumpsys batterystats --charged}, whose text is written by AOSP 10 BatteryStats.dumpLocked (the format is in
 * research/aosp10-settings/A_battery_display.md sec. 5 and the BN1 build log). {@link #parse} is a pure function of that text (JVM-tested on real output); {@link #read} runs the command on the CALLER's thread
 * (never the main thread) with a hard time limit and stops reading at the first line it does not need.
 */
final class SsBatteryStats {
    private SsBatteryStats() {}

    /** One reading; every field is -1 when unknown. */
    static final class Data {
        long startClockMs = -1;      // wall-clock start of the statistics ("Last full charge" = now - this)
        long screenOnMs = -1;        // screen-on time since then ("Screen usage since full charge")
        long dischargeMs = -1;       // Android's estimate of the time left on battery
        long chargeMs = -1;          // Android's estimate of the time to full while charging
        String status = "none";      // ok | partial | no-dump | exec-failed | timeout | empty (goes into the tablet report)
        boolean any() { return startClockMs >= 0 || screenOnMs >= 0 || dischargeMs >= 0 || chargeMs >= 0; }
    }

    private static final Pattern CLOCK = Pattern.compile("^\\s*Start clock time:\\s*(\\d{4})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{2})\\s*$");
    private static final Pattern TOKEN = Pattern.compile("(\\d+)(ms|d|h|m|s)(?=\\s|$|\\()");

    /** "1d 2h 16m 3s 322ms " (BatteryStats.formatTimeMs: zero leading parts left out) -> milliseconds; -1 when the text holds no time. */
    static long parseTime(String text) {
        if (text == null) return -1;
        Matcher m = TOKEN.matcher(text);
        long total = 0; boolean any = false;
        while (m.find()) {
            long v = Long.parseLong(m.group(1));
            switch (m.group(2)) {
                case "d": total += v * 86_400_000L; break;
                case "h": total += v * 3_600_000L; break;
                case "m": total += v * 60_000L; break;
                case "s": total += v * 1000L; break;
                default: total += v; break;   // ms
            }
            any = true;
        }
        return any ? total : -1;
    }

    /** Feeds a dump line by line; {@link #done} says when everything wanted has been seen (the caller may stop reading). */
    static final class Parser {
        final Data data = new Data();
        private final TimeZone zone;
        private boolean inCharged;
        Parser(TimeZone zone) { this.zone = zone; }

        boolean done() { return data.screenOnMs >= 0; }   // "Screen on:" is the last line wanted; the clock and the estimates come before it

        void line(String l) {
            if (l == null) return;
            if (l.startsWith("Statistics since last charge:")) { inCharged = true; return; }
            if (l.startsWith("Statistics since last unplugged:")) { inCharged = false; return; }
            if (!inCharged) return;
            String t = l.trim();
            if (t.startsWith("Battery time remaining:")) data.dischargeMs = parseTime(t.substring("Battery time remaining:".length()));
            else if (t.startsWith("Charge time remaining:")) data.chargeMs = parseTime(t.substring("Charge time remaining:".length()));
            else if (t.startsWith("Start clock time:")) {
                Matcher m = CLOCK.matcher(l);
                if (m.matches()) {
                    Calendar c = Calendar.getInstance(zone);
                    c.clear();
                    c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
                    data.startClockMs = c.getTimeInMillis();
                }
            } else if (t.startsWith("Screen on:") && data.screenOnMs < 0) {
                int paren = t.indexOf('(');
                data.screenOnMs = parseTime(paren > 0 ? t.substring("Screen on:".length(), paren) : t.substring("Screen on:".length()));
            }
        }
    }

    /** The whole text at once (tests). */
    static Data parse(String dump, TimeZone zone) {
        Parser p = new Parser(zone);
        if (dump != null) for (String l : dump.split("\n")) { p.line(l); if (p.done()) break; }
        p.data.status = p.data.any() ? (p.data.startClockMs >= 0 && p.data.screenOnMs >= 0 ? "ok" : "partial") : "empty";
        return p.data;
    }

    /** Runs {@code dumpsys batterystats --charged}; blocks up to {@code limitMs}. Never throws. */
    static Data read(long limitMs) {
        final AtomicBoolean finished = new AtomicBoolean(false);
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        Process proc = null;
        Parser parser = new Parser(TimeZone.getDefault());
        try {
            proc = new ProcessBuilder("dumpsys", "batterystats", "--charged").redirectErrorStream(true).start();
            final Process p = proc;
            Thread watchdog = new Thread(new Runnable() {
                @Override public void run() {
                    long end = System.currentTimeMillis() + limitMs;
                    while (!finished.get() && System.currentTimeMillis() < end) { try { Thread.sleep(50); } catch (InterruptedException e) { return; } }
                    if (!finished.get()) p.destroy();
                }
            }, "TjSsBattWatch");
            watchdog.setDaemon(true);
            watchdog.start();
            boolean denied = false;
            BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()), 16 * 1024);
            String l; int n = 0;
            while ((l = r.readLine()) != null && ++n < 200_000) {
                if (l.contains("Permission Denial") || l.contains("denied")) { denied = true; }
                parser.line(l);
                if (parser.done()) break;
            }
            finished.set(true);
            Data d = parser.data;
            d.status = denied && !d.any() ? "no-dump" : d.any() ? (d.startClockMs >= 0 && d.screenOnMs >= 0 ? "ok" : "partial") : (System.currentTimeMillis() == 0 ? "timeout" : "empty");
            return d;
        } catch (Throwable t) {
            parser.data.status = "exec-failed";
            return parser.data;
        } finally {
            finished.set(true);
            if (proc != null) { try { proc.destroy(); } catch (Throwable ignored) { } }
        }
    }
}
