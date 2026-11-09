package com.tj.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.android.launcher3.R;
import java.util.List;

/**
 * BN1: the Accessibility page with Tj's rows and headings (Screen readers: Text-to-speech output; Display: Font size, Display size, Magnification; Interaction controls: Touch & hold delay; Audio & on-screen
 * text: Mono audio, Audio balance; no search, no other row) and its Magnification page. Every control changes the real system setting (the launcher holds WRITE_SECURE_SETTINGS through the adb grant):
 * Settings.Secure long_press_timeout, Settings.System master_mono and master_balance. Rows whose page is not among Tj's pages (Magnify with triple-tap / shortcut) show the real state and open nothing.
 * Rules: {@link SsAccessRules}; layout numbers: AOSP 10 Settings (research/aosp10-settings/C_access_system_about.md sec. 1) and the measured pixels of Tj's screenshots.
 */
final class SsPagesAccessibility {
    private SsPagesAccessibility() {}

    static final String LONG_PRESS = "long_press_timeout";
    static final String MAG_TRIPLE = "accessibility_display_magnification_enabled";
    static final String MAG_SHORTCUT = "accessibility_display_magnification_navbar_enabled";
    static final String MONO = "master_mono";
    static final String BALANCE = "master_balance";

    private static View.OnClickListener open(final Activity a, final SsPage p) {
        return new View.OnClickListener() { @Override public void onClick(View v) { a.startActivity(SsPageActivity.intent(a, p)); } };
    }

    static void accessibility(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        list.addView(SsRows.heading(a, "Screen readers"));
        list.addView(SsRows.row(a, null, SsPage.TTS.title, null, open(a, SsPage.TTS)));
        list.addView(SsRows.divider(a));
        list.addView(SsRows.heading(a, "Display"));
        final View font = SsRows.row(a, null, "Font size", SsPagesDisplay.fontSummary(a), open(a, SsPage.FONT_SIZE));
        final View size = SsRows.row(a, null, "Display size", SsPagesDisplay.sizeSummary(a), open(a, SsPage.DISPLAY_SIZE));
        final View magnify = SsRows.row(a, magnificationIcon(a), "Magnification", magnificationSummary(a), open(a, SsPage.MAGNIFICATION));
        list.addView(font);
        list.addView(size);
        list.addView(magnify);
        list.addView(SsRows.divider(a));
        list.addView(SsRows.heading(a, "Interaction controls"));
        final View hold = SsRows.row(a, null, "Touch & hold delay", holdSummary(a), new View.OnClickListener() {
            @Override public void onClick(View v) { openHold(a); }
        });
        list.addView(hold);
        list.addView(SsRows.divider(a));
        list.addView(SsRows.heading(a, "Audio & on-screen text"));
        final Switch mono = new Switch(a);
        mono.setChecked(monoOn(a));
        final Runnable monoRefresh = new Runnable() { @Override public void run() { if (mono.isChecked() != monoOn(a)) mono.setChecked(monoOn(a)); } };
        list.addView(SsRows.switchRow(a, "Mono audio", "Combine channels when playing audio", mono, new SsRows.Toggle() {
            @Override public void onToggle(final boolean checked) {
                applyPrivate(a, MONO, checked ? "1" : "0", monoRefresh);
            }
        }));
        final BalanceBar bar = new BalanceBar(a);
        bar.setProgress(SsAccessRules.balanceProgress(balance(a)));
        final Runnable balanceRefresh = new Runnable() {
            @Override public void run() { int p = SsAccessRules.balanceProgress(balance(a)); if (!bar.tracking && bar.getProgress() != p) bar.setProgress(p); }
        };
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (!fromUser) return;
                final int snapped = SsAccessRules.snapBalance(progress);
                if (snapped != progress) s.setProgress(snapped);
                // while the finger is down the app writes the value itself when this build lets it; when it does not, the shell helper is asked ONCE, when the finger lifts
                if (!privateBlocked) try { Settings.System.putString(a.getContentResolver(), BALANCE, SsAccessRules.balanceText(snapped)); } catch (SecurityException | IllegalArgumentException e) { privateBlocked = true; }
            }
            @Override public void onStartTrackingTouch(SeekBar s) { ((BalanceBar) s).tracking = true; }
            @Override public void onStopTrackingTouch(SeekBar s) {
                ((BalanceBar) s).tracking = false;
                if (privateBlocked) applyPrivate(a, BALANCE, SsAccessRules.balanceText(s.getProgress()), balanceRefresh);
            }
        });
        list.addView(balanceRow(a, bar));

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                SsRows.setSummary(font, SsPagesDisplay.fontSummary(a));
                SsRows.setSummary(magnify, magnificationSummary(a));
                SsRows.setSummary(hold, holdSummary(a));
                monoRefresh.run();
                balanceRefresh.run();
            }
        };
        live.add(new SsWatcher(a, refresh, SsWatcher.sec(LONG_PRESS), SsWatcher.sec(MAG_TRIPLE), SsWatcher.sec(MAG_SHORTCUT),
                SsWatcher.sys(Settings.System.FONT_SCALE), SsWatcher.sys(MONO), SsWatcher.sys(BALANCE)));
    }

    /** The Magnification page: two rows with the real On / Off state; their pages are not among Tj's pages, so they open nothing (as agreed for every row outside his list). */
    static void magnification(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        final View triple = SsRows.row(a, null, "Magnify with triple-tap", SsAccessRules.onOff(flag(a, MAG_TRIPLE)), null);
        final View shortcut = SsRows.row(a, null, "Magnify with shortcut", SsAccessRules.onOff(flag(a, MAG_SHORTCUT)), null);
        list.addView(triple);
        list.addView(shortcut);
        live.add(new SsWatcher(a, new Runnable() {
            @Override public void run() {
                SsRows.setSummary(triple, SsAccessRules.onOff(flag(a, MAG_TRIPLE)));
                SsRows.setSummary(shortcut, SsAccessRules.onOff(flag(a, MAG_SHORTCUT)));
            }
        }, SsWatcher.sec(MAG_TRIPLE), SsWatcher.sec(MAG_SHORTCUT)));
    }

    // ------------------------------------------------------------------ private system settings
    /** Set when Android refused a direct write of master_mono / master_balance (Settings.System.PRIVATE_SETTINGS): from then on the shell helper does it. */
    private static volatile boolean privateBlocked;

    /**
     * Writes master_mono / master_balance: the app itself first (a build that allows it), else the shell helper (new verb SETSYS), on a worker thread. When neither could, the control goes back to the value the
     * system really holds ({@code revert} runs on the main thread) and a short note says so: the page never shows a value Android does not have.
     */
    static void applyPrivate(final Activity a, final String key, final String value, final Runnable revert) {
        new Thread(new Runnable() {
            @Override public void run() {
                boolean ok = false;
                if (!privateBlocked) {
                    try { ok = Settings.System.putString(a.getContentResolver(), key, value); } catch (SecurityException | IllegalArgumentException e) { privateBlocked = true; }
                }
                if (!ok) ok = ShellHelper.enabled(a) && ShellHelper.setSystem(key, value);
                if (ok) return;
                a.runOnUiThread(new Runnable() { @Override public void run() {
                    android.widget.Toast.makeText(a, "This setting can't be changed from here.", android.widget.Toast.LENGTH_SHORT).show();
                    revert.run();
                } });
            }
        }, "ss-private").start();
    }

    // ------------------------------------------------------------------ readers
    static boolean flag(Context c, String secureKey) { return Settings.Secure.getInt(c.getContentResolver(), secureKey, 0) == 1; }

    static String magnificationSummary(Context c) { return SsAccessRules.magnificationSummary(flag(c, MAG_TRIPLE), flag(c, MAG_SHORTCUT)); }

    static int holdMs(Context c) { return Settings.Secure.getInt(c.getContentResolver(), LONG_PRESS, SsAccessRules.LONG_PRESS_VALUES[0]); }

    static String holdSummary(Context c) { return SsAccessRules.longPressSummary(holdMs(c)); }

    static boolean monoOn(Context c) { return Settings.System.getInt(c.getContentResolver(), MONO, 0) == 1; }

    static float balance(Context c) { return Settings.System.getFloat(c.getContentResolver(), BALANCE, 0f); }

    // ------------------------------------------------------------------ pieces
    /** The Magnification row's own icon (AOSP ic_accessibility_magnification: a 32 dp pink pentagon with a magnifier). */
    private static View magnificationIcon(Context c) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(R.drawable.ss_ic_magnification);
        iv.setLayoutParams(new FrameLayout.LayoutParams(SsRows.dp(c, 32), SsRows.dp(c, 32), Gravity.START | Gravity.CENTER_VERTICAL));
        return iv;
    }

    /** AOSP's ListPreference dialog for the touch & hold delay: a single-choice list, a tap stores the value and closes it (no OK button), Cancel closes it. */
    static void openHold(final Activity a) {
        new AlertDialog.Builder(a).setTitle("Touch & hold delay")
                .setSingleChoiceItems(SsAccessRules.LONG_PRESS_LABELS, SsAccessRules.longPressIndex(holdMs(a)), new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, final int which) {
                        SsPagesDisplay.write(a, new Runnable() { @Override public void run() {
                            Settings.Secure.putInt(a.getContentResolver(), LONG_PRESS, SsAccessRules.LONG_PRESS_VALUES[which]);
                        } });
                        d.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * AOSP preference_balance_slider: a horizontal row (min height 48 dp, padding 16 dp) with a 56 dp icon frame left empty, then a column (margin 8 dp top and bottom) holding the title, the slider and the
     * Left / Right labels. The labels are in the SECONDARY text colour (measured on Tj's screenshot: #737373 like the summaries), not the primary one the layout file would suggest.
     */
    private static View balanceRow(Context c, BalanceBar bar) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(SsRows.dp(c, 48));
        r.setPaddingRelative(SsRows.dp(c, 16), 0, SsRows.dp(c, 16), 0);
        r.addView(new View(c), new LinearLayout.LayoutParams(SsRows.dp(c, 56), SsRows.dp(c, 32)));   // the icon frame: 24 dp invisible icon + 4 dp above and below
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.setMargins(0, SsRows.dp(c, 8), 0, SsRows.dp(c, 8));
        r.addView(col, clp);
        TextView title = new TextView(c);
        SsRows.appearance(title, android.R.attr.textAppearanceListItem);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        title.setText("Audio balance");
        col.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout labels = new LinearLayout(c);
        labels.setOrientation(LinearLayout.HORIZONTAL);
        labels.addView(label(c, "Left", View.TEXT_ALIGNMENT_VIEW_START), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        labels.addView(label(c, "Right", View.TEXT_ALIGNMENT_VIEW_END), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(labels, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }

    private static TextView label(Context c, String text, int alignment) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setMaxLines(1);
        t.setTextAlignment(alignment);
        t.setTextColor(SsRows.themeColors(c, android.R.attr.textColorSecondary, Color.GRAY));
        return t;
    }

    /**
     * AOSP BalanceSeekBar: 0..200 with the middle at 100, no coloured fill (progress tint transparent: only the thumb shows the accent) and a black centre marker, 14 dp tall and 1 dp wide, drawn BEFORE the
     * track and thumb (so only its ends show). The snap to the middle is in {@link SsAccessRules#snapBalance}.
     */
    static final class BalanceBar extends SeekBar {
        private final Paint mark = new Paint();
        private final int markW, markH;
        boolean tracking;

        BalanceBar(Context c) {
            super(c);
            setMax(SsAccessRules.BALANCE_MAX);
            setProgressTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            mark.setColor(Color.BLACK);
            markW = Math.max(1, SsRows.dp(c, 1));
            markH = SsRows.dp(c, 14);
        }

        @Override protected void onDraw(Canvas canvas) {
            int left = getWidth() / 2 - markW / 2, top = getHeight() / 2 - markH / 2;
            canvas.drawRect(left, top, left + markW, top + markH, mark);
            super.onDraw(canvas);
        }
    }
}
