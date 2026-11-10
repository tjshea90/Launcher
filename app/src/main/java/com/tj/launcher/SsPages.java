package com.tj.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.android.launcher3.R;
import java.util.List;

/**
 * BN1: builds the content of one {@link SsPage}. Each page is a ScrollView of rows from {@link SsRows}; a page is rebuilt when the activity is created (and by the pages that watch system values).
 * M0 (this file's first version): the home page; the other pages answer a placeholder until their milestone (M1 static pages, M2 Battery and Display, M3 Sound and Storage, M4 Accessibility).
 */
final class SsPages {
    private SsPages() {}

    /** A page part that follows system values: the activity starts it when it becomes visible and stops it when it is hidden (receivers and observers must not outlive the screen). */
    interface Live { void start(); void stop(); }

    static View build(final Activity a, SsPage page, List<Live> live) {
        if (page == SsPage.FONT_SIZE) return SsPagesSize.fontSize(a, live);          // their own scrolling page (preview frame + slider), not a list of rows
        if (page == SsPage.DISPLAY_SIZE) return SsPagesSize.displaySize(a, live);
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        switch (page) {
            case HOME: home(a, list, live); break;
            case BATTERY: SsPagesBattery.battery(a, list, live); break;
            case BATTERY_USAGE: SsPagesBattery.usage(a, list); break;
            case DISPLAY: SsPagesDisplay.display(a, list, live); break;
            case SOUND: SsPagesSound.sound(a, list, live); break;
            case STORAGE: SsPagesStorage.storage(a, list, live); break;
            case ACCESSIBILITY: SsPagesAccessibility.accessibility(a, list, live); break;
            case MAGNIFICATION: SsPagesAccessibility.magnification(a, list, live); break;
            case TTS: SsPagesTts.tts(a, list, live); break;
            case SYSTEM: SsPagesSystem.system(a, list); break;
            case LANGUAGES: SsPagesSystem.languages(a, list); break;
            case ABOUT: SsPagesSystem.about(a, list); break;
            case LEGAL: SsPagesSystem.legal(a, list); break;
            default:
                list.addView(SsRows.row(a, null, page.title, "This page is not built yet.", null));
        }
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.addView(list, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    /** The seven home entries in Tj's order; coloured tiles and the stock summaries (AOSP strings; the page-specific summaries are refined in M1). */
    private static void home(final Activity a, LinearLayout list, List<Live> live) {
        Object[][] e = {
            { SsPage.BATTERY, 0xFF258982, R.drawable.ss_glyph_battery, SsRows.BLANK },   // BN1p D2: the two live summaries start as one blank line, so the row has its final height at once
            { SsPage.DISPLAY, 0xFFFFA727, R.drawable.ss_glyph_display, "Wallpaper, sleep, font size" },
            { SsPage.SOUND, 0xFF01B1AF, R.drawable.ss_glyph_sound, "Volume, vibration, Do Not Disturb" },
            { SsPage.STORAGE, 0xFFC14CE6, R.drawable.ss_glyph_storage, "" },
            { SsPage.ACCESSIBILITY, 0xFF5011C1, R.drawable.ss_glyph_accessibility, "Screen readers, display, interaction controls" },
            { SsPage.SYSTEM, 0xFF9E9E9E, R.drawable.ss_glyph_system, "Languages, gestures, time, backup" },
            { SsPage.ABOUT, 0xFF6F86DA, R.drawable.ss_glyph_about, android.os.Build.MODEL },
        };
        View battery = null, storage = null;
        for (Object[] x : e) {
            final SsPage p = (SsPage) x[0];
            View.OnClickListener go = new View.OnClickListener() { @Override public void onClick(View v) { a.startActivity(SsPageActivity.intent(a, p)); } };
            View row = SsRows.row(a, SsRows.tile(a, (Integer) x[1], (Integer) x[2]), p.title, (String) x[3], go);
            if (p == SsPage.BATTERY) battery = row;
            if (p == SsPage.STORAGE) storage = row;
            list.addView(row);
        }
        live.add(new SsHomeSummary(a, battery, storage));   // the two summaries that follow the system: "69% - ..." and "95% used - ... free"
    }
}
