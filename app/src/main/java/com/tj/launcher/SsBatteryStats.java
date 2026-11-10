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
        /** BN1p D1: true once a read has FINISHED, even one that found nothing. Until then a page shows no status word ("Not charging"), because Tj's screenshots never show one. */
        boolean finished() { return !"none".equals(status); }

        long readAtElapsed = -1;     // SystemClock.elapsedRealtime() when the read finished; -1 = not from a read (parse() in tests)

        /**
         * BN1p D1: Android's discharge estimate as of {@code nowElapsed}: the figure of the read minus the time that has passed since it ({@code -1}: none, or used up). A page may start from a reading of any age, and
         * "now + the figure of an hour-old read" would put the end time an hour too late; counting down keeps the END time where Android put it.
         */
        long drainAt(long nowElapsed) { return remaining(dischargeMs, nowElapsed); }
        long chargeAt(long nowElapsed) { return remaining(chargeMs, nowElapsed); }

        /**
         * BN1p D1 (review finding 1): may a page show the sentence from THIS reading before its own read has finished? Only when the reading has an estimate that still applies to the plug state NOW: a seed from a
         * charging session has no discharge figure, and the header would fall back to the status word ("Not charging") in the first frame, which is the flash D1 removes.
         */
        boolean usableFor(boolean pluggedIn, long nowElapsed) { return pluggedIn ? chargeAt(nowElapsed) > 0 : drainAt(nowElapsed) > 0; }

        /** True when the reading HAD an estimate that has run out by now: a page must not start from it (the refresh a second later has the real one). */
        boolean expired(long nowElapsed) { return (dischargeMs > 0 && drainAt(nowElapsed) < 0) || (chargeMs > 0 && chargeAt(nowElapsed) < 0); }

        private long remaining(long ms, long nowElapsed) {
            if (ms < 0) return -1;
            if (readAtElapsed < 0 || nowElapsed < readAtElapsed) return ms;   // unknown read time, or a clock that went backwards: as read
            long left = ms - (nowElapsed - readAtElapsed);
            return left > 0 ? left : -1;
        }
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

    // ------------------------------------------------------------------ one shared reading (review A #3, review B #4)
    // The Settings home and the Battery page both need the dump, and every start of either used to spawn its own `dumpsys batterystats` (a lock in system_server, up to 6 s, twice in a row for home -> Battery).
    // A page that starts within its throttle of the last GOOD read takes that one. Time = SystemClock.elapsedRealtime() (counts sleep, never jumps with the clock), passed in so the rule is a JVM test.
    private static Data lastGood;
    private static long lastGoodAt;

    static synchronized void remember(Data d, long nowMs) { if (d != null && d.any()) { lastGood = d; lastGoodAt = nowMs; } }

    /** The last good reading when it is younger than {@code maxAgeMs}, else null (a failed read is never remembered: the next start tries again). */
    static synchronized Data recent(long maxAgeMs, long nowMs) {
        long age = nowMs - lastGoodAt;
        return lastGood != null && age >= 0 && age < maxAgeMs ? lastGood : null;
    }

    static synchronized void forget() { lastGood = null; lastGoodAt = 0; }

    /**
     * BN1p D1: where a page starts from. The newest good reading WHATEVER ITS AGE (the throttle in {@link #recent} only decides whether to read again) as long as its estimate has not run out
     * ({@link Data#expired}; the pages count the estimate down with {@link Data#drainAt}), so the first frame already carries the sentence and the two value rows instead of a status word and empty cells; a read
     * refreshes it a moment later. Before the very first good read, or after an expired one: a blank reading that is not {@link Data#finished()}.
     */
    static synchronized Data seed(long nowElapsed) { Data g = lastGood; return g != null && !g.expired(nowElapsed) ? g : new Data(); }

    /** What a page keeps after a read: the new reading when it has numbers, else the old one if THAT has numbers, else the (finished, empty) new one so the status word may show. */
    static Data keep(Data old, Data fresh) { return fresh.any() || old == null || !old.any() ? fresh : old; }

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
                    long end = android.os.SystemClock.elapsedRealtime() + limitMs;   // not the wall clock: a clock set backwards must not give a hung dumpsys more time
                    while (!finished.get() && android.os.SystemClock.elapsedRealtime() < end) { try { Thread.sleep(50); } catch (InterruptedException e) { return; } }
                    if (!finished.get()) { timedOut.set(true); p.destroy(); }
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
            d.status = d.any() ? (d.startClockMs >= 0 && d.screenOnMs >= 0 ? "ok" : "partial") : denied ? "no-dump" : timedOut.get() ? "timeout" : "empty";
            d.readAtElapsed = android.os.SystemClock.elapsedRealtime();
            remember(d, d.readAtElapsed);
            return d;
        } catch (Throwable t) {
            parser.data.status = "exec-failed";
            parser.data.readAtElapsed = android.os.SystemClock.elapsedRealtime();   // review: a partial reading still counts its estimate down
            return parser.data;
        } finally {
            finished.set(true);
            if (proc != null) { try { proc.destroy(); } catch (Throwable ignored) { } }
        }
    }
}
