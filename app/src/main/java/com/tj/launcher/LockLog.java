package com.tj.launcher;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;

/**
 * AC1/AC7 (found while reading the code for Tj's 2026-11-05 requests): "sometimes the plain PIN opens Quickstep", "a couple of times the gesture opened the custom launcher". The tablet's logcat only keeps about
 * three minutes, so the cause of a one-off could never be read afterwards. This is a small PERSISTENT log of what the lock screen logic saw and decided (screen on/off, PIN pad shown, the KIND of key tapped,
 * the first two key kinds at Enter, handshake accepted / withdrawn, the unlock decision with the mode before and after, how long Enter to unlock took). It goes into the tablet report.
 *
 * PRIVACY (hard rule, same as the unlock handshakes): never a digit, never how many digits, never the PIN. Callers pass key CLASSES only and at most two of them ({@link UnlockMethods#padLead}).
 * Size: a ring file of at most {@link #MAX_BYTES}; when it grows past that the oldest lines are dropped. Never throws, never blocks the lock screen logic for more than one small append.
 */
public final class LockLog {
    static final String FILE = "lock-events.log";
    public static final int MAX_BYTES = 48 * 1024;
    public static final int KEEP_BYTES = 32 * 1024;
    private static final Object LOCK = new Object();
    private static int sinceTrimCheck = 0;

    private LockLog() {}

    /** Pure: one log line ("HH:mm:ss.SSS EVENT detail"), flattened to a single line and cut to 200 characters. */
    public static String line(long nowMs, String event, String detail) {
        String d = detail == null ? "" : detail.replace('\r', ' ').replace('\n', ' ');
        String l = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(new java.util.Date(nowMs)) + " " + (event == null ? "?" : event) + (d.isEmpty() ? "" : " " + d);
        if (l.length() > 200) l = l.substring(0, 200);
        return l + "\n";
    }

    /** Pure: {@code all} if it fits in {@code max} characters, else its last {@code keep} characters starting at a line boundary. */
    public static String trim(String all, int max, int keep) {
        if (all == null || all.length() <= max) return all == null ? "" : all;
        String t = all.substring(all.length() - keep);
        int nl = t.indexOf('\n');
        return nl >= 0 && nl + 1 < t.length() ? t.substring(nl + 1) : t;
    }

    /** Pure: the last {@code n} lines of {@code all}. */
    public static String tailLines(String all, int n) {
        if (all == null || all.isEmpty() || n <= 0) return "";
        int end = all.length();
        if (all.charAt(end - 1) == '\n') end--;
        int pos = end, seen = 0;
        while (pos > 0 && seen < n) { pos = all.lastIndexOf('\n', pos - 1); seen++; if (pos < 0) break; }
        String t = all.substring(pos < 0 ? 0 : pos + 1);
        return t.endsWith("\n") ? t : t + "\n";
    }

    private static final java.util.concurrent.ExecutorService BG = java.util.concurrent.Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
        @Override public Thread newThread(Runnable r) { Thread t = new Thread(r, "tj-lock-log"); t.setDaemon(true); return t; }
    });

    /** BE1: like {@link #add} but the file is written on a background thread (in order), with the time of the EVENT: for the patch paths, which must not wait for a disk. */
    public static void addAsync(final Context c, final String event, final String detail) {
        final long t = System.currentTimeMillis();
        try { BG.execute(new Runnable() { @Override public void run() { addAt(c, t, event, detail); } }); } catch (Throwable ignored) { }
    }

    /**
     * Appends one line. Safe from any thread. BG1 (Tj: "open the custom launcher as fast as possible"): the file is written on the log thread, IN ORDER, not by the caller: 46 callers sit on the accessibility
     * service's main thread, which is also the Launcher's main thread, and several of them ran in the middle of an unlock (a file open + append + close each). The time stamp is the caller's.
     */
    public static void add(Context c, String event, String detail) { addAsync(c, event, detail); }

    private static void addAt(Context c, long atMs, String event, String detail) {
        try {
            String l = line(atMs, event, detail);
            File f = c.getApplicationContext().getFileStreamPath(FILE);
            synchronized (LOCK) {
                FileOutputStream o = new FileOutputStream(f, true);
                try { o.write(l.getBytes("UTF-8")); } finally { o.close(); }
                if (++sinceTrimCheck >= 40 || f.length() > MAX_BYTES) { sinceTrimCheck = 0; if (f.length() > MAX_BYTES) rewriteTrimmed(f); }
            }
        } catch (Throwable ignored) { }
    }

    private static void rewriteTrimmed(File f) throws java.io.IOException {
        String all = read(f);
        String t = trim(all, MAX_BYTES, KEEP_BYTES);
        FileOutputStream o = new FileOutputStream(f, false);
        try { o.write(t.getBytes("UTF-8")); } finally { o.close(); }
    }

    private static String read(File f) {
        try {
            if (!f.exists()) return "";
            java.io.FileInputStream in = new java.io.FileInputStream(f);
            try {
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                return bo.toString("UTF-8");
            } finally { in.close(); }
        } catch (Throwable t) { return ""; }
    }

    /** The last {@code n} lines (for the report and Launcher control); "" when there is no log. */
    public static String tail(Context c, int n) {
        synchronized (LOCK) { return tailLines(read(c.getApplicationContext().getFileStreamPath(FILE)), n); }
    }

    /** BN8e: {@link #digest} of the whole log file, for the tablet report. */
    public static String tailDigest(Context c, int n) {
        synchronized (LOCK) { return digest(read(c.getApplicationContext().getFileStreamPath(FILE)), n); }
    }

    private static boolean isDirty(String line) { return line.startsWith("DIRTY ", 13); }   // "HH:mm:ss.SSS DIRTY <package> was marked as opened in Quickstep (window ...)"

    /**
     * BN8e (Tj's report of 2026-11-10): the last {@code n} lines of {@code all}, with every run of consecutive DIRTY lines folded into ONE line ("first time DIRTY 7 lines (com.a x5, com.b x2; newest last time)").
     * A DIRTY line only says that an app was opened in Quickstep; ordinary use writes dozens, and the last 40 lines of the report were all of them, so the whole custom session before them (the lock-screen and side-key events
     * the report exists for) was cut off. Order and every other line are untouched; a run of one stays as it is. Pure.
     */
    public static String digest(String all, int n) {
        if (all == null || all.isEmpty() || n <= 0) return "";
        String[] lines = all.split("\n");
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int i = 0;
        while (i < lines.length) {
            String l = lines[i];
            if (l.isEmpty()) { i++; continue; }
            if (!isDirty(l)) { out.add(l); i++; continue; }
            int j = i;
            java.util.LinkedHashMap<String, Integer> by = new java.util.LinkedHashMap<String, Integer>();
            while (j < lines.length && !lines[j].isEmpty() && isDirty(lines[j])) {
                String rest = lines[j].substring(19);                       // after "HH:mm:ss.SSS DIRTY "
                int sp = rest.indexOf(' ');
                String pkg = sp > 0 ? rest.substring(0, sp) : rest;
                Integer c = by.get(pkg);
                by.put(pkg, c == null ? 1 : c + 1);
                j++;
            }
            int count = j - i;
            if (count == 1) out.add(l);
            else {
                StringBuilder b = new StringBuilder(lines[i].substring(0, 12)).append(" DIRTY ").append(count).append(" lines (");
                boolean first = true;
                for (java.util.Map.Entry<String, Integer> e : by.entrySet()) { if (!first) b.append(", "); first = false; b.append(e.getKey()).append(" x").append(e.getValue()); }
                b.append("; newest ").append(lines[j - 1].substring(0, 12)).append(')');
                out.add(b.length() > 200 ? b.substring(0, 200) : b.toString());
            }
            i = j;
        }
        StringBuilder r = new StringBuilder();
        for (int k = Math.max(0, out.size() - n); k < out.size(); k++) r.append(out.get(k)).append('\n');
        return r.toString();
    }
}
