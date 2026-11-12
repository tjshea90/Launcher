package com.tj.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.LocaleList;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BN1: readers of the system values the System, Languages & input and About summaries show (public APIs only; AOSP 10 sources in research/aosp10-settings/C_access_system_about.md sec. 2 and 3). Every reader
 * answers "" on any failure: a summary is never worth a crash.
 */
final class SsSystemInfo {
    private SsSystemInfo() {}

    /** "English (United States)": up to two preferred locales, sentence case, joined as a list (LanguageAndInputSettings / PhoneLanguagePreferenceController). */
    static String localeSummary() {
        try {
            LocaleList ll = LocaleList.getAdjustedDefault();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < Math.min(2, ll.size()); i++) names.add(SsFormat.sentenceCase(ll.get(i).getDisplayName(Locale.getDefault())));
            return SsFormat.joinAnd(names);
        } catch (Throwable t) { return ""; }
    }

    /**
     * The labels of the ENABLED input methods, joined (VirtualKeyboardPreferenceController). BT1e (Tj, 2026-11-11: "remove the text 'and Microsoft SwiftKey keyboard' even if that keyboard is installed"):
     * the look-alike never names SwiftKey, whether or not it is installed and enabled ({@link KeyboardPolicy#visibleLabels}). BX1 (2026-11-12): nor Gboard, the Quickstep side's keyboard now, unless it is the only one.
     */
    static String enabledKeyboards(Context c) {
        try { return SsFormat.joinAnd(visibleKeyboardLabels(c)); } catch (Throwable t) { return ""; }
    }

    /** BT1e: the labels of the enabled input methods the look-alike may show (SwiftKey's left out; BX1: Gboard's too while another keyboard is listed). */
    private static List<String> visibleKeyboardLabels(Context c) {
        InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<String> pkgs = new ArrayList<>(), labels = new ArrayList<>();
        if (imm != null) for (InputMethodInfo i : imm.getEnabledInputMethodList()) { pkgs.add(i.getPackageName()); labels.add(String.valueOf(i.loadLabel(c.getPackageManager()))); }
        return KeyboardPolicy.visibleLabels(pkgs, labels);
    }

    /**
     * The label of the DEFAULT input method (the System page's summary for "Languages & input"). BT1e: never SwiftKey's name; when SwiftKey is the default (the keyboard switch is off, or the custom launcher has not
     * switched yet) the first other enabled keyboard is named instead, as the real page would after SwiftKey was removed from the list. BX1: the same for Gboard (the Quickstep side's keyboard); if it is the only
     * enabled keyboard it is named, as {@link KeyboardPolicy#visibleLabels} does.
     */
    static String defaultKeyboardLabel(Context c) {
        try {
            String flat = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            ComponentName cn = flat == null ? null : ComponentName.unflattenFromString(flat);
            if (cn == null) return "";
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                InputMethodInfo samePackage = null, exact = null;
                for (InputMethodInfo i : imm.getInputMethodList()) {
                    if (cn.equals(i.getComponent())) { exact = i; break; }   // review A #5: the exact input method first
                    if (samePackage == null && i.getPackageName().equals(cn.getPackageName())) samePackage = i;
                }
                InputMethodInfo found = exact != null ? exact : samePackage;   // a package with two input methods: the first one only when the exact one is not listed
                if (found != null) {
                    String label = String.valueOf(found.loadLabel(c.getPackageManager()));
                    if (!KeyboardPolicy.isQuickstepSide(found.getPackageName(), label)) return label;
                    List<String> others = visibleKeyboardLabels(c);   // BT1e, BX1
                    return others.isEmpty() ? "" : others.get(0);
                }
            }
        } catch (Throwable ignored) { }
        return "";
    }

    /** "Not connected" without a physical keyboard, else the keyboards' names (PhysicalKeyboardPreferenceController). */
    static String physicalKeyboardSummary(Context c) {
        try {
            List<String> names = new ArrayList<>();
            for (int id : InputDevice.getDeviceIds()) {
                InputDevice d = InputDevice.getDevice(id);
                if (d != null && !d.isVirtual() && d.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC && (d.getSources() & InputDevice.SOURCE_KEYBOARD) != 0) names.add(d.getName());
            }
            return names.isEmpty() ? "Not connected" : SsFormat.joinAnd(names);
        } catch (Throwable t) { return "Not connected"; }
    }

    /** The label of the selected spell checker service ("Android Spell Checker (AOSP)"). */
    static String spellCheckerLabel(Context c) {
        try {
            String flat = Settings.Secure.getString(c.getContentResolver(), "selected_spell_checker");
            ComponentName cn = flat == null ? null : ComponentName.unflattenFromString(flat);
            if (cn == null) return "";
            PackageManager pm = c.getPackageManager();
            for (ResolveInfo ri : pm.queryIntentServices(new Intent("android.service.textservice.SpellCheckerService"), 0)) {
                if (ri.serviceInfo != null && ri.serviceInfo.packageName.equals(cn.getPackageName()) && ri.serviceInfo.name.equals(cn.getClassName())) return String.valueOf(ri.loadLabel(pm));
            }
        } catch (Throwable ignored) { }
        return "";
    }
}
