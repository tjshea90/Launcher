package com.tj.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.launcher3.R;
import java.util.List;

/**
 * BN1 / BO2: the Text-to-speech output page, minus the search icon. Tj (2026-11-09 17:5x), who replaced the greyed look of his no-engine screenshot: the page reads like a tablet WITH an engine, and everything on it
 * is INERT: "Preferred engine" says "Pico TTS" and has a settings gear at the very right that does nothing, "Language" says "Use system language", the two sliders move and slide but change no setting (no listener, nothing
 * written, no TextToSpeech instance), PLAY and RESET are active (not grey) and do nothing. The texts are fixed whatever the tablet has. The sliders are the theme's plain seek bar, the one of the Sound page, so the colour
 * is the same. Layout numbers: AOSP 10 preference_widget_seekbar_settings, SettingsLib settings_action_buttons, preference_widget_gear (research/aosp10-settings/C_access_system_about.md sec. 1.1).
 */
final class SsPagesTts {
    private SsPagesTts() {}

    static final String ENGINE_LABEL = "Pico TTS", LANGUAGE_SUMMARY = "Use system language";

    /** Looks like a tap target (the ripple of a stock row) and does nothing. */
    private static final View.OnClickListener INERT = new View.OnClickListener() { @Override public void onClick(View v) { } };

    static void tts(Activity a, LinearLayout list, List<SsPages.Live> live) {
        LinearLayout engine = SsRows.row(a, null, "Preferred engine", ENGINE_LABEL, INERT);
        engine.addView(gearWidget(a), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        list.addView(engine);
        list.addView(SsRows.row(a, null, "Language", LANGUAGE_SUMMARY, INERT));
        list.addView(SsRows.divider(a));

        // the AOSP positions of the default value 100 (rate progress = value - 10 of 590, pitch progress = value - 25 of 375); no listener: they move, nothing is stored
        SeekBar rate = new SeekBar(a), pitch = new SeekBar(a);
        rate.setMax(SsAccessRules.RATE_PROGRESS_MAX);
        pitch.setMax(SsAccessRules.PITCH_PROGRESS_MAX);
        rate.setProgress(SsAccessRules.rateProgress(SsAccessRules.TTS_DEFAULT));
        pitch.setProgress(SsAccessRules.pitchProgress(SsAccessRules.TTS_DEFAULT));
        list.addView(sliderRow(a, "Speech rate", rate, true, false));
        list.addView(sliderRow(a, "Pitch", pitch, false, true));
        list.addView(SsRows.divider(a));

        list.addView(buttons(a, actionButton(a, "Play"), actionButton(a, "Reset")));   // active, no click listener: they do nothing
    }

    /**
     * AOSP GearPreference widget (preference_widget_gear): a thin vertical divider (full height of the row, 16 dp margins) and the settings gear, 16 dp further right, ending at the row's own 16 dp end padding.
     * Inert: nothing is clickable (the stock gear opens the engine's own settings).
     */
    private static View gearWidget(Context c) {
        LinearLayout w = new LinearLayout(c);
        w.setOrientation(LinearLayout.HORIZONTAL);
        w.setGravity(Gravity.CENTER_VERTICAL);
        View divider = new View(c);
        divider.setBackgroundColor(0x1F000000);   // the Material divider (dividerVertical)
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(Math.max(1, SsRows.dp(c, 1)), ViewGroup.LayoutParams.MATCH_PARENT);
        dl.topMargin = SsRows.dp(c, 16);
        dl.bottomMargin = SsRows.dp(c, 16);
        w.addView(divider, dl);
        ImageView gear = new ImageView(c);
        gear.setImageResource(R.drawable.ss_ic_gear);
        gear.setImageTintList(ColorStateList.valueOf(SsRows.themeColor(c, android.R.attr.colorControlNormal, 0xFF757575)));
        gear.setContentDescription("Settings");
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(SsRows.dp(c, 24), SsRows.dp(c, 24));
        gl.setMarginStart(SsRows.dp(c, 16));
        w.addView(gear, gl);
        return w;
    }

    /**
     * AOSP preference_widget_seekbar_settings: margin 6 dp top and bottom, at least 72 dp high, padding 16 dp; the title starts 56 dp further in, the 48 dp slider sits right under it (padding 56 dp / 12 dp).
     * Measured on Tj's screenshot against the CI emulator (CI run 455, 600 x 1024 at 170 dpi): the block of two sliders starts 25 px lower than the plain 6 dp (23.5 dp more above the first) and ends 6 px higher (no
     * bottom margin under the last), so the divider under it sits at y 437 as on the tablet (rows pitch 91 px matched already).
     */
    private static View sliderRow(Context c, String title, SeekBar bar, boolean first, boolean last) {
        RelativeLayout r = new RelativeLayout(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, SsRows.dp(c, first ? 6f + 23.5f : 6f), 0, last ? 0 : SsRows.dp(c, 6));
        r.setLayoutParams(lp);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(SsRows.dp(c, 72));
        r.setPaddingRelative(SsRows.dp(c, 16), 0, SsRows.dp(c, 16), 0);
        TextView t = new TextView(c);
        t.setId(View.generateViewId());
        SsRows.appearance(t, android.R.attr.textAppearanceListItem);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        t.setPaddingRelative(SsRows.dp(c, 56), 0, SsRows.dp(c, 8), 0);
        t.setText(title);
        r.addView(t, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        RelativeLayout.LayoutParams sp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SsRows.dp(c, 48));
        sp.addRule(RelativeLayout.ALIGN_PARENT_END);
        sp.addRule(RelativeLayout.BELOW, t.getId());
        bar.setPaddingRelative(SsRows.dp(c, 56), 0, SsRows.dp(c, 12), 0);
        r.addView(bar, sp);
        return r;
    }

    /** AOSP SettingsActionButton: the platform's borderless coloured button with 20 dp above and below its text. */
    private static Button actionButton(Context c, String text) {
        Button b = new Button(c, null, 0, android.R.style.Widget_DeviceDefault_Button_Borderless_Colored);
        b.setText(text);
        b.setPadding(b.getPaddingLeft(), SsRows.dp(c, 20), b.getPaddingRight(), SsRows.dp(c, 20));
        b.setCompoundDrawablePadding(SsRows.dp(c, 4));
        return b;
    }

    /** AOSP settings_action_buttons: a horizontal row (start padding 8 dp) of equal-weight buttons with 8 dp end margins. */
    private static View buttons(Context c, Button... all) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPaddingRelative(SsRows.dp(c, 8), SsRows.dp(c, 4.7f), 0, 0);   // 4.7 dp = 5 px at 170 dpi: Tj's PLAY / RESET text sits 34 px under the divider, the first build 29
        for (Button b : all) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(SsRows.dp(c, 8));
            r.addView(b, lp);
        }
        return r;
    }
}
