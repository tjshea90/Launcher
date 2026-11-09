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
}
