package com.tj.launcher;

import java.util.List;
import java.util.Locale;

/** Pure rules for the keyboard switch (JVM-testable). Only ever picks from keyboards that are ALREADY ENABLED, so a switch can never leave the tablet without a usable keyboard. */
public final class KeyboardPolicy {
    public static final class Ime {
        public final String id, pkg;
        public final boolean system;
        public Ime(String id, String pkg, boolean system) { this.id = id; this.pkg = pkg; this.system = system; }
    }

    private KeyboardPolicy() {}

    /** The stock keyboard: first enabled SYSTEM keyboard, preferring the AOSP / Google ones. */
    public static String pickStock(List<Ime> enabled) {
        Ime best = null; int bestScore = -1;
        for (Ime i : enabled) {
            if (!i.system) continue;
            int score = 1;
            String p = i.pkg == null ? "" : i.pkg.toLowerCase(Locale.ROOT);
            if (p.contains("inputmethod.latin")) score += 3;
            if (p.startsWith("com.android") || p.startsWith("com.google")) score += 1;
            if (p.equals("com.android.inputmethod.latin")) score += 1;   // the AOSP keyboard wins a tie with Gboard
            if (score > bestScore) { bestScore = score; best = i; }
        }
        return best == null ? null : best.id;
    }

    public static String pickSwiftKey(List<Ime> enabled) {
        for (Ime i : enabled) if (i.pkg != null && i.pkg.toLowerCase(Locale.ROOT).contains("swiftkey")) return i.id;
        return null;
    }

    /**
     * BT1e (Tj, 2026-11-11: in the Settings look-alike, Languages and input, "remove the text 'and Microsoft SwiftKey keyboard' even if that keyboard is installed"). Is this input method SwiftKey's? The look-alike is
     * the custom launcher's world, which must not know the keyboard of the Quickstep side, so it never names it. Decided by the package (the same rule as {@link #pickSwiftKey}) or by the label, in any letter case.
     */
    public static boolean isSwiftKey(String pkg, String label) { return mentionsSwiftKey(pkg) || mentionsSwiftKey(label); }

    private static boolean mentionsSwiftKey(String s) { return s != null && s.toLowerCase(Locale.ROOT).contains("swiftkey"); }

    /**
     * BT1e: the keyboard names the look-alike may show. {@code pkgs} and {@code labels} are parallel lists describing the ENABLED input methods (a missing package is fine, the label then decides); SwiftKey's entry and
     * blank labels are left out. Never null; the order is kept.
     */
    public static List<String> visibleLabels(List<String> pkgs, List<String> labels) {
        List<String> out = new ArrayList<>();
        if (labels == null) return out;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            String pkg = pkgs != null && i < pkgs.size() ? pkgs.get(i) : null;
            if (label == null || label.trim().isEmpty() || isSwiftKey(pkg, label)) continue;
            out.add(label);
        }
        return out;
    }

    /** Which keyboard to use for a mode; null = leave things alone. Quickstep mode returns SwiftKey, or the keyboard that was the default before we ever switched. */
    public static String target(String mode, List<Ime> enabled, String originalId) {
        if (Mode.QUICKSTEP.equals(mode)) {
            String sk = pickSwiftKey(enabled);
            if (sk != null) return sk;
            return originalId != null && containsId(enabled, originalId) ? originalId : null;
        }
        return pickStock(enabled);
    }

    private static boolean containsId(List<Ime> l, String id) { for (Ime i : l) if (id.equals(i.id)) return true; return false; }
}
