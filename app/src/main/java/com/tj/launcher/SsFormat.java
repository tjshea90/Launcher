package com.tj.launcher;

import java.util.List;

/**
 * BN1: pure text rules of the Settings look-alike (JVM-tested: SsFormatTest). Everything here is a plain function of its arguments; the readers of system values live in {@link SsSystemInfo}.
 */
final class SsFormat {
    private SsFormat() {}

    /** "A", "A and B", "A, B and C": the English list the AOSP summaries use (android.icu ListFormatter, locale en). Empty list = "". */
    static String joinAnd(List<String> items) {
        if (items == null || items.isEmpty()) return "";
        int n = items.size();
        if (n == 1) return items.get(0);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n - 1; i++) { if (i > 0) b.append(", "); b.append(items.get(i)); }
        return b.append(" and ").append(items.get(n - 1)).toString();
    }

    /** First letter upper case (AOSP LocaleHelper.toSentenceCase for a language name), the rest unchanged. */
    static String sentenceCase(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * AOSP StringUtil.formatElapsedTime(ms, withSeconds = false) in ICU's SHORT English: whole seconds + 30 (rounds to the nearest minute), then days / hours / minutes, zero parts dropped, joined by ", ":
     * "59 min", "1 hr, 16 min", "2 hr", "1 day, 3 hr", "2 days", "0 min". A negative duration counts as 0.
     */
    static String elapsed(long ms) {
        long s = Math.max(0L, ms / 1000L) + 30L;
        long days = s / 86400L; s -= days * 86400L;
        long hours = s / 3600L; s -= hours * 3600L;
        long minutes = s / 60L;
        StringBuilder b = new StringBuilder();
        if (days > 0) b.append(days).append(days == 1 ? " day" : " days");
        if (hours > 0) { if (b.length() > 0) b.append(", "); b.append(hours).append(" hr"); }
        if (minutes > 0) { if (b.length() > 0) b.append(", "); b.append(minutes).append(" min"); }
        return b.length() == 0 ? "0 min" : b.toString();
    }

    /**
     * AOSP StringUtil.formatRelativeTime(ms, withSeconds = false) in ICU's LONG English, direction LAST: under 2 h in minutes (rounded), under 2 days in hours (rounded), else days (rounded):
     * "109 minutes ago", "1 minute ago", "2 hours ago", "3 days ago", "0 minutes ago".
     */
    static String ago(long ms) {
        long s = Math.max(0L, ms / 1000L);
        long n;
        String unit;
        if (s < 2 * 3600L) { n = (s + 30L) / 60L; unit = "minute"; }
        else if (s < 2 * 86400L) { n = (s + 1800L) / 3600L; unit = "hour"; }
        else { n = (s + 43200L) / 86400L; unit = "day"; }
        return n + " " + unit + (n == 1 ? "" : "s") + " ago";
    }
}
