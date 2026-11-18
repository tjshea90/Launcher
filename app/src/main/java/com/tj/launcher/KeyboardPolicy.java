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
     * CH1e (Tj, 2026-11-18: "i switched to swoop keyboard ... an app from my keyboard repo. Make an option in the launcher settings to choose between the stock Android keyboard, gboard, or swoop for the quickstep launcher").
     * Swoop (tjshea90/Keyboard, ime/build.gradle.kts + AndroidManifest.xml): applicationId {@code app.swoop.keyboard}, input method service {@code app.swoop.keyboard.KeysService}; its debug build installs beside it as
     * {@code app.swoop.keyboard.debug}. Matched by PACKAGE only, like Gboard.
     */
    public static final String SWOOP_PKG = "app.swoop.keyboard", SWOOP_DEBUG_PKG = "app.swoop.keyboard.debug";
    public static boolean isSwoop(String pkg) { return SWOOP_PKG.equals(pkg) || SWOOP_DEBUG_PKG.equals(pkg); }

    /** CH1e: the id of an ENABLED Swoop (the release build first, then a debug build), or null. */
    public static String pickSwoop(List<Ime> enabled) {
        String debug = null;
        for (Ime i : enabled) {
            if (SWOOP_PKG.equals(i.pkg)) return i.id;
            if (debug == null && SWOOP_DEBUG_PKG.equals(i.pkg)) debug = i.id;
        }
        return debug;
    }

    /** CH1e: the setting "Keyboard in Quickstep" (Launcher control): which keyboard the Quickstep side gets. Gboard stays the default (BX1: it is what Quickstep has always loaded since 2026-11-12). */
    public static final String CHOICE_KEY = "quickstep_keyboard";
    public static final String CHOICE_STOCK = "stock", CHOICE_GBOARD = "gboard", CHOICE_SWOOP = "swoop";
    /** CH1e (pure): anything stored that is not one of the three names is Gboard, so a damaged value changes nothing. */
    public static String normalizeChoice(String stored) {
        if (CHOICE_STOCK.equals(stored) || CHOICE_SWOOP.equals(stored)) return stored;
        return CHOICE_GBOARD;
    }
    /** CH1e: the keyboard name for the status line and the Control row. */
    public static String choiceName(String choice) {
        String c = normalizeChoice(choice);
        return CHOICE_STOCK.equals(c) ? "the stock keyboard" : CHOICE_SWOOP.equals(c) ? "Swoop" : "Gboard";
    }

    /**
     * BT1e (Tj, 2026-11-11: in the Settings look-alike, Languages and input, "remove the text 'and Microsoft SwiftKey keyboard' even if that keyboard is installed"). Is this input method SwiftKey's? The look-alike is
     * the custom launcher's world, which must not know the keyboard of the Quickstep side, so it never names it. Decided by the package or by the label, in any letter case.
     */
    public static boolean isSwiftKey(String pkg, String label) { return mentionsSwiftKey(pkg) || mentionsSwiftKey(label); }

    private static boolean mentionsSwiftKey(String s) { return s != null && s.toLowerCase(Locale.ROOT).contains("swiftkey"); }

    /** BX1: a keyboard of the Quickstep side that the look-alike should not name: SwiftKey (BT1e, still hidden if it is installed) or Gboard (the Quickstep keyboard since BX1). */
    public static boolean isQuickstepSide(String pkg, String label) { return isSwiftKey(pkg, label) || isGboard(pkg) || isSwoop(pkg); }   // CH1e: Swoop can be the Quickstep side's keyboard too

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
            (isGboard(pkg) || isSwoop(pkg) ? gboard : out).add(label);   // CH1e: Swoop is left out like Gboard (named only when it is the one and only keyboard)
        }
        return out.isEmpty() ? gboard : out;
    }

    /**
     * Which keyboard to use for a mode; null = leave things alone. Quickstep mode returns Gboard (BX1; it was SwiftKey until 2026-11-12), or the keyboard that was the default before we ever switched. The custom
     * launcher's keyboard is {@link #pickStock} and was NOT changed by BX1 (Tj: "make the custom launcher open the same default keyboard it already opens").
     */
    public static String target(String mode, List<Ime> enabled, String originalId) { return target(mode, enabled, originalId, CHOICE_GBOARD); }

    /**
     * CH1e: the same with the user's choice for the Quickstep side. {@code stock}: the stock keyboard (the custom launcher's), {@code gboard}: BX1's rule unchanged, {@code swoop}: an enabled Swoop, and NOTHING else
     * (an explicit choice is never silently replaced by another keyboard: null = left as is, and the status line says why). The custom launcher's keyboard never depends on the choice.
     */
    public static String target(String mode, List<Ime> enabled, String originalId, String choice) {
        if (Mode.QUICKSTEP.equals(mode)) {
            String c = normalizeChoice(choice);
            if (CHOICE_STOCK.equals(c)) return pickStock(enabled);
            if (CHOICE_SWOOP.equals(c)) return pickSwoop(enabled);
            String gb = pickGboard(enabled);
            if (gb != null) return gb;
            return originalId != null && containsId(enabled, originalId) ? originalId : null;
        }
        return pickStock(enabled);
    }

    /**
     * The status line (Launcher control, tablet report) when {@link #target} found nothing to switch to. BX1: for Quickstep it names Gboard AND the packages that ARE enabled, so the line alone shows why Gboard was
     * not found (not installed, not turned on under Virtual keyboard > Manage keyboards, or another package) without anyone having to run adb.
     */
    public static String noTargetStatus(String mode, List<Ime> enabled) { return noTargetStatus(mode, enabled, CHOICE_GBOARD); }

    /** CH1e: the same for the user's choice: names the keyboard that was asked for and the packages that ARE enabled. */
    public static String noTargetStatus(String mode, List<Ime> enabled, String choice) {
        if (!Mode.QUICKSTEP.equals(mode)) return "no enabled stock keyboard found: left as is";
        String c = normalizeChoice(choice);
        if (CHOICE_STOCK.equals(c)) return "no enabled stock keyboard found: left as is";
        StringBuilder b = new StringBuilder();
        if (enabled != null) for (Ime i : enabled) { if (b.length() > 0) b.append(", "); b.append(i.pkg); }
        return choiceName(c) + " is not an enabled keyboard (enabled: " + (b.length() == 0 ? "none" : b.toString()) + "): left as is. Turn it on under Virtual keyboard > Manage keyboards.";
    }

    private static boolean containsId(List<Ime> l, String id) { for (Ime i : l) if (id.equals(i.id)) return true; return false; }
}
