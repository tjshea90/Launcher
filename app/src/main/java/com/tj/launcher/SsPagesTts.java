package com.tj.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;

/**
 * BN1: the Text-to-speech output page as on Tj's screenshot (his tablet has no speech engine: Preferred engine without a summary, Language / Speech rate / Pitch / Play greyed out, Reset active), minus the search
 * icon. With an engine installed the sliders and Play work (they store Settings.Secure tts_default_rate / tts_default_pitch like AOSP and Play speaks the sample); the engine picker and the language
 * list are not among Tj's pages and open nothing. Layout numbers: AOSP 10 preference_widget_seekbar_settings, SettingsLib settings_action_buttons (research/aosp10-settings/C_access_system_about.md sec. 1.1).
 */
final class SsPagesTts {
    private SsPagesTts() {}

    static final String RATE = "tts_default_rate", PITCH = "tts_default_pitch", ENGINE = "tts_default_synth";
    private static final String SAMPLE = "This is an example of speech synthesis";   // tts_default_sample_string

    static List<ResolveInfo> engines(Context c) {
        return c.getPackageManager().queryIntentServices(new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0);
    }

    /** The label of the preferred engine: the stored default when it is installed, else the first engine (what TtsEngines.getDefaultEngine settles on); null with no engine. */
    static String engineLabel(Context c) {
        List<ResolveInfo> all = engines(c);
        if (all.isEmpty()) return null;
        String stored = Settings.Secure.getString(c.getContentResolver(), ENGINE);
        PackageManager pm = c.getPackageManager();
        for (ResolveInfo ri : all) if (ri.serviceInfo != null && ri.serviceInfo.packageName.equals(stored)) return String.valueOf(ri.loadLabel(pm));
        return String.valueOf(all.get(0).loadLabel(pm));
    }

    static void tts(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        final String label = engineLabel(a);
        final boolean engine = label != null;
        list.addView(SsRows.row(a, null, "Preferred engine", engine ? label : null, null));
        View language = SsRows.row(a, null, "Language", "Sets the language-specific voice for the spoken text", null);
        if (!engine) SsRows.disable(language);
        list.addView(language);
        list.addView(SsRows.divider(a));

        // BN1m: a PLAIN seek bar. Widget.Material.SeekBar.Discrete draws one tick mark per step, and with 590 / 375 steps the marks run together into a thick dark line (CI darkest grey 0.204);
        // Tj's screenshot has a thin light-grey track (0.843). Look beats the style name; the max stays so the stored value still maps.
        final SeekBar rate = new SeekBar(a), pitch = new SeekBar(a);
        rate.setMax(SsAccessRules.RATE_PROGRESS_MAX);
        pitch.setMax(SsAccessRules.PITCH_PROGRESS_MAX);
        rate.setProgress(SsAccessRules.rateProgress(stored(a, RATE)));
        pitch.setProgress(SsAccessRules.pitchProgress(stored(a, PITCH)));
        rate.setOnSeekBarChangeListener(new Writer(a, RATE, true));
        pitch.setOnSeekBarChangeListener(new Writer(a, PITCH, false));
        list.addView(sliderRow(a, "Speech rate", rate, engine, true, false));
        list.addView(sliderRow(a, "Pitch", pitch, engine, false, true));
        list.addView(SsRows.divider(a));

        final TextToSpeech[] speaker = new TextToSpeech[1];
        Button play = actionButton(a, "Play", engine);
        play.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { speak(a, speaker, rate, pitch); }
        });
        Button reset = actionButton(a, "Reset", true);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SsPagesDisplay.write(a, new Runnable() { @Override public void run() {
                    Settings.Secure.putInt(a.getContentResolver(), RATE, SsAccessRules.TTS_DEFAULT);
                    Settings.Secure.putInt(a.getContentResolver(), PITCH, SsAccessRules.TTS_DEFAULT);
                } });
                rate.setProgress(SsAccessRules.rateProgress(SsAccessRules.TTS_DEFAULT));
                pitch.setProgress(SsAccessRules.pitchProgress(SsAccessRules.TTS_DEFAULT));
            }
        });
        list.addView(buttons(a, play, reset));
        live.add(new SsPages.Live() {
            @Override public void start() { }
            @Override public void stop() { shutdown(speaker); }
        });
    }

    private static int stored(Context c, String key) { return Settings.Secure.getInt(c.getContentResolver(), key, SsAccessRules.TTS_DEFAULT); }

    /** Stores the slider value on every change (AOSP does the same), clamped to the slider's range. */
    private static final class Writer implements SeekBar.OnSeekBarChangeListener {
        private final Activity a;
        private final String key;
        private final boolean rate;
        Writer(Activity a, String key, boolean rate) { this.a = a; this.key = key; this.rate = rate; }
        @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
            if (!fromUser) return;
            final int value = rate ? SsAccessRules.rateValue(progress) : SsAccessRules.pitchValue(progress);
            SsPagesDisplay.write(a, new Runnable() { @Override public void run() { Settings.Secure.putInt(a.getContentResolver(), key, value); } });
        }
        @Override public void onStartTrackingTouch(SeekBar s) { }
        @Override public void onStopTrackingTouch(SeekBar s) { }
    }

    private static void speak(final Context c, final TextToSpeech[] holder, final SeekBar rate, final SeekBar pitch) {
        shutdown(holder);
        holder[0] = new TextToSpeech(c, new TextToSpeech.OnInitListener() {
            @Override public void onInit(int status) {
                TextToSpeech t = holder[0];
                if (t == null || status != TextToSpeech.SUCCESS) return;
                try {
                    t.setLanguage(Locale.getDefault());
                    t.setSpeechRate(SsAccessRules.rateValue(rate.getProgress()) / 100f);
                    t.setPitch(SsAccessRules.pitchValue(pitch.getProgress()) / 100f);
                    t.speak(SAMPLE, TextToSpeech.QUEUE_FLUSH, null, "Sample");
                } catch (RuntimeException ignored) { }
            }
        });
    }

    private static void shutdown(TextToSpeech[] holder) {
        TextToSpeech t = holder[0];
        holder[0] = null;
        if (t != null) { try { t.stop(); t.shutdown(); } catch (RuntimeException ignored) { } }
    }

    /**
     * AOSP preference_widget_seekbar_settings: margin 6 dp top and bottom, at least 72 dp high, padding 16 dp; the title starts 56 dp further in, the 48 dp slider sits right under it (padding 56 dp / 12 dp).
     * Measured on Tj's screenshot against the CI emulator (CI run 455, 600 x 1024 at 170 dpi): the block of two sliders starts 25 px lower than the plain 6 dp (23.5 dp more above the first) and ends 6 px higher (no
     * bottom margin under the last), so the divider under it sits at y 437 as on the tablet (rows pitch 91 px matched already).
     */
    private static View sliderRow(Context c, String title, SeekBar bar, boolean enabled, boolean first, boolean last) {
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
        t.setEnabled(enabled);
        r.addView(t, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        RelativeLayout.LayoutParams sp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SsRows.dp(c, 48));
        sp.addRule(RelativeLayout.ALIGN_PARENT_END);
        sp.addRule(RelativeLayout.BELOW, t.getId());
        bar.setPaddingRelative(SsRows.dp(c, 56), 0, SsRows.dp(c, 12), 0);
        bar.setEnabled(enabled);
        r.addView(bar, sp);
        return r;
    }

    /** AOSP SettingsActionButton: the platform's borderless coloured button with 20 dp above and below its text. */
    private static Button actionButton(Context c, String text, boolean enabled) {
        Button b = new Button(c, null, 0, android.R.style.Widget_DeviceDefault_Button_Borderless_Colored);
        b.setText(text);
        b.setPadding(b.getPaddingLeft(), SsRows.dp(c, 20), b.getPaddingRight(), SsRows.dp(c, 20));
        b.setCompoundDrawablePadding(SsRows.dp(c, 4));
        b.setEnabled(enabled);
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
