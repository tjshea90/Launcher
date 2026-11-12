package com.tj.launcher;

import java.util.ArrayList;
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

    /**
     * BX1 (Tj, 2026-11-12: "I no longer use Microsoft Swiftkey. I use Google Gboard ... make quickstep open Gboard as the default keyboard"). Gboard's package; the Play Store app and its beta share it. Matched by
     * PACKAGE only, never by label: the stock AOSP keyboard may carry a label like "Gboard" on some builds and must not be taken for it.
     */
    public static final String GBOARD_PKG = "com.google.android.inputmethod.latin";

    /** BX1: the id of an ENABLED Gboard (by package), or null. It does not matter whether Gboard is a system app or a Play Store app. */
    public static String pickGboard(List<Ime> enabled) {
        for (Ime i : enabled) if (isGboard(i.pkg)) return i.id;
        return null;
    }

    /** BX1: is this package Gboard? Exact match; Google's other input methods (Indic, Pinyin, voice typing, ...) have other packages and are not Gboard. */
    public static boolean isGboard(String pkg) { return GBOARD_PKG.equals(pkg); }

    /**
     * BT1e (Tj, 2026-11-11: in the Settings look-alike, Languages and input, "remove the text 'and Microsoft SwiftKey keyboard' even if that keyboard is installed"). Is this input method SwiftKey's? The look-alike is
     * the custom launcher's world, which must not know the keyboard of the Quickstep side, so it never names it. Decided by the package or by the label, in any letter case.
     */
    public static boolean isSwiftKey(String pkg, String label) { return mentionsSwiftKey(pkg) || mentionsSwiftKey(label); }

    private static boolean mentionsSwiftKey(String s) { return s != null && s.toLowerCase(Locale.ROOT).contains("swiftkey"); }

    /** BX1: a keyboard of the Quickstep side that the look-alike should not name: SwiftKey (BT1e, still hidden if it is installed) or Gboard (the Quickstep keyboard since BX1). */
    public static boolean isQuickstepSide(String pkg, String label) { return isSwiftKey(pkg, label) || isGboard(pkg); }

    /**
     * BT1e: the keyboard names the look-alike may show. {@code pkgs} and {@code labels} are parallel lists describing the ENABLED input methods (a missing package is fine, the label then decides); SwiftKey's entry and
     * blank labels are left out. BX1: Gboard is the Quickstep side's keyboard now, so it is left out the same way, but only while another keyboard is listed (on a device whose one and only keyboard is Gboard the
     * row names it instead of going blank). Never null; the order is kept.
     */
    public static List<String> visibleLabels(List<String> pkgs, List<String> labels) {
        List<String> out = new ArrayList<>(), gboard = new ArrayList<>();
        if (labels == null) return out;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            String pkg = pkgs != null && i < pkgs.size() ? pkgs.get(i) : null;
            if (label == null || label.trim().isEmpty() || isSwiftKey(pkg, label)) continue;
            (isGboard(pkg) ? gboard : out).add(label);
        }
        return out.isEmpty() ? gboard : out;
    }

    /**
     * Which keyboard to use for a mode; null = leave things alone. Quickstep mode returns Gboard (BX1; it was SwiftKey until 2026-11-12), or the keyboard that was the default before we ever switched. The custom
     * launcher's keyboard is {@link #pickStock} and was NOT changed by BX1 (Tj: "make the custom launcher open the same default keyboard it already opens").
     */
    public static String target(String mode, List<Ime> enabled, String originalId) {
        if (Mode.QUICKSTEP.equals(mode)) {
            String gb = pickGboard(enabled);
            if (gb != null) return gb;
            return originalId != null && containsId(enabled, originalId) ? originalId : null;
        }
        return pickStock(enabled);
    }

    private static boolean containsId(List<Ime> l, String id) { for (Ime i : l) if (id.equals(i.id)) return true; return false; }
}
