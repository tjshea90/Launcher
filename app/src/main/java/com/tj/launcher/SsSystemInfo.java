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

    /** The labels of all ENABLED input methods, joined (VirtualKeyboardPreferenceController). */
    static String enabledKeyboards(Context c) {
        try {
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<String> labels = new ArrayList<>();
            if (imm != null) for (InputMethodInfo i : imm.getEnabledInputMethodList()) labels.add(String.valueOf(i.loadLabel(c.getPackageManager())));
            return SsFormat.joinAnd(labels);
        } catch (Throwable t) { return ""; }
    }

    /** The label of the DEFAULT input method (the System page's summary for "Languages & input"). */
    static String defaultKeyboardLabel(Context c) {
        try {
            String flat = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            ComponentName cn = flat == null ? null : ComponentName.unflattenFromString(flat);
            if (cn == null) return "";
            InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) for (InputMethodInfo i : imm.getInputMethodList()) if (i.getPackageName().equals(cn.getPackageName())) return String.valueOf(i.loadLabel(c.getPackageManager()));
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
