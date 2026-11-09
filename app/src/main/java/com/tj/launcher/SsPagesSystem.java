package com.tj.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.launcher3.R;

/**
 * BN1 M1: the static pages: System, Languages & input, About tablet, Legal information (AOSP 10 Settings layouts in research/aosp10-settings/C_access_system_about.md; Tj's screenshots decide which rows exist).
 * RULE (research/SETTINGS_BUILD_LOG_2026-11-09.md): a row whose page is not one of Tj's pages does nothing when tapped.
 */
final class SsPagesSystem {
    private SsPagesSystem() {}

    /** The text Tj asked for, verbatim (his message of 2026-11-09 02:06 UTC). */
    static final String CUSTOM_BUILD_VERSION = "vnd_tb8168p1_bsp 1779111601";

    private static View.OnClickListener open(final Activity a, final SsPage p) {
        return new View.OnClickListener() { @Override public void onClick(View v) { a.startActivity(SsPageActivity.intent(a, p)); } };
    }

    /** System: only "Languages & input" (the stock page has five rows; Tj wants the first). */
    static void system(Activity a, LinearLayout list) {
        list.addView(SsRows.row(a, SsRows.icon(a, R.drawable.ss_ic_language), SsPage.LANGUAGES.title, SsSystemInfo.defaultKeyboardLabel(a), open(a, SsPage.LANGUAGES)));
    }

    /**
     * Languages & input as on Tj's screenshot minus "Autofill service" and "Personal dictionary", with "Text-to-speech output" as the LAST row. Dividers sit above every heading and above the unlabelled group
     * (AOSP category rule). Languages, Virtual keyboard, Physical keyboard, Spell checker and Pointer speed open nothing (their pages are not among Tj's pages).
     */
    static void languages(Activity a, LinearLayout list) {
        list.addView(SsRows.row(a, SsRows.icon(a, R.drawable.ss_ic_translate), "Languages", SsSystemInfo.localeSummary(), null));
        list.addView(SsRows.divider(a));
        list.addView(SsRows.heading(a, "Keyboards"));
        list.addView(SsRows.row(a, null, "Virtual keyboard", SsSystemInfo.enabledKeyboards(a), null));
        list.addView(SsRows.row(a, null, "Physical keyboard", SsSystemInfo.physicalKeyboardSummary(a), null));
        list.addView(SsRows.divider(a));
        list.addView(SsRows.heading(a, "Tools"));
        list.addView(SsRows.row(a, null, "Spell checker", SsSystemInfo.spellCheckerLabel(a), null));
        list.addView(SsRows.divider(a));
        list.addView(SsRows.row(a, null, "Pointer speed", null, null));
        list.addView(SsRows.row(a, null, SsPage.TTS.title, null, open(a, SsPage.TTS)));
    }

    /** About tablet: no owner header; Legal information, Model & hardware, Android version, a divider, Custom build version. Only Legal information opens something. */
    static void about(Activity a, LinearLayout list) {
        list.addView(SsRows.row(a, null, "Legal information", null, open(a, SsPage.LEGAL)));
        list.addView(SsRows.row(a, null, "Model & hardware", "Model: " + Build.MODEL, null));   // Tj, 2026-11-09 02:47 UTC: it "can't be opened"
        list.addView(SsRows.row(a, null, "Android version", Build.VERSION.RELEASE, null));       // its page would show the Build number: not a page of Tj's
        list.addView(SsRows.divider(a));
        list.addView(SsRows.row(a, null, "Custom build version", CUSTOM_BUILD_VERSION, null));
    }

    /** Legal information as on Tj's screenshot: two licence screens (the real, read-only ones, through their public intents) and the wallpaper credits (a plain text row). */
    static void legal(final Activity a, LinearLayout list) {
        list.addView(SsRows.row(a, null, "Third-party licenses", null, licence(a, "android.settings.LICENSE")));
        list.addView(SsRows.row(a, null, "System WebView licenses", null, licence(a, "android.settings.WEBVIEW_LICENSE")));
        list.addView(SsRows.row(a, null, "Wallpapers", "Satellite imagery providers:\n©2014 CNES / Astrium, DigitalGlobe, Bluesky", null));
    }

    private static View.OnClickListener licence(final Activity a, final String action) {
        return new View.OnClickListener() { @Override public void onClick(View v) {
            try { a.startActivity(new Intent(action)); }
            catch (ActivityNotFoundException e) { Toast.makeText(a, "There is a problem loading the licenses.", Toast.LENGTH_LONG).show(); }   // AOSP settings_license_activity_unavailable
        } };
    }
}
