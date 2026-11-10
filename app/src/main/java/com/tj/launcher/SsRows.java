package com.tj.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.R;

/**
 * BN1: the row factory of the Settings look-alike. One hand-built row = exact by construction and easy to measure (the AOSP rows are androidx.preference layouts that nobody can read here). Numbers are the
 * AOSP 10 Settings dimens (research/aosp10-settings/*.md): start / end padding 16 dp, icon frame 56 dp (so the text of a row without icon starts at 72 dp too), title 16 sp (textAppearanceListItem) over
 * summary 14 sp (textAppearanceListItemSecondary, secondary colour), vertical padding 16 dp. dp -> px uses the REAL density (170 dpi on the tablet).
 */
final class SsRows {
    private SsRows() {}

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    /** Applies the text appearance the theme names for {@code attr} (android.R.attr.textAppearanceListItem ...). */
    static void appearance(TextView t, int attr) {
        TypedValue tv = new TypedValue();
        if (t.getContext().getTheme().resolveAttribute(attr, tv, true) && tv.resourceId != 0) t.setTextAppearance(tv.resourceId);
    }

    static int themeColor(Context c, int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (c.getTheme().resolveAttribute(attr, tv, true)) {
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) return tv.data;
            try { return c.getColor(tv.resourceId); } catch (Throwable ignored) { }
        }
        return fallback;
    }

    /** The theme's colour STATE LIST for {@code attr} (so a disabled TextView greys itself), or the plain {@code fallback}. */
    static android.content.res.ColorStateList themeColors(Context c, int attr, int fallback) {
        android.content.res.TypedArray ta = c.obtainStyledAttributes(new int[] { attr });
        try {
            android.content.res.ColorStateList l = ta.getColorStateList(0);
            if (l != null) return l;
        } finally { ta.recycle(); }
        return android.content.res.ColorStateList.valueOf(fallback);
    }

    /** Greys a row made by {@link #row} (AOSP Preference.setEnabled(false): title and summary in the disabled text colours, no click, no ripple). */
    static void disable(View row) {
        row.setEnabled(false);
        row.setClickable(false);
        row.setBackground(null);
        setTextEnabled(row, false);
    }

    private static void setTextEnabled(View v, boolean enabled) {
        if (v instanceof TextView) v.setEnabled(enabled);
        if (v instanceof ViewGroup) { ViewGroup g = (ViewGroup) v; for (int i = 0; i < g.getChildCount(); i++) setTextEnabled(g.getChildAt(i), enabled); }
    }

    /** A platform style by name (private ones too: the Settings layouts use styles the public SDK does not list); 0 when this build has none. */
    static int sysStyle(String name) { return android.content.res.Resources.getSystem().getIdentifier(name, "style", "android"); }

    /** AOSP's Widget.Material.SeekBar.Discrete (tick marks) as the Settings sliders use it; the plain seek bar of the theme when the platform has no such style. */
    static android.widget.SeekBar discreteSeekBar(Context c) {
        int style = sysStyle("Widget.Material.SeekBar.Discrete");
        return style != 0 ? new android.widget.SeekBar(c, null, 0, style) : new android.widget.SeekBar(c);
    }

    /** A standard preference row: optional icon (36 dp tile or 24 dp glyph), title, optional summary; {@code click} null = not clickable (no ripple). */
    static LinearLayout row(Context c, View icon, CharSequence title, CharSequence summary, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        r.setMinimumHeight(dp(c, 48));
        FrameLayout frame = new FrameLayout(c);   // the icon frame: 56 dp wide even when there is no icon (the text of every row starts at 72 dp)
        frame.setMinimumWidth(dp(c, 56));
        if (icon != null) {   // an icon that brings its own FrameLayout size (the 36 dp tile, the 32 dp app icon) keeps it
            ViewGroup.LayoutParams own = icon.getLayoutParams();
            frame.addView(icon, own instanceof FrameLayout.LayoutParams ? own : new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));
        }
        r.addView(frame, new LinearLayout.LayoutParams(dp(c, 56), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(c, 16), 0, dp(c, 16));
        TextView t = new TextView(c);
        appearance(t, android.R.attr.textAppearanceListItem);
        t.setText(title);
        col.addView(t);
        if (summary != null) {   // an empty summary keeps its (hidden) view so a page can fill it in later with setSummary
            TextView s = new TextView(c);
            appearance(s, android.R.attr.textAppearanceListItemSecondary);
            s.setTextColor(themeColors(c, android.R.attr.textColorSecondary, Color.GRAY));   // the state list: a disabled row greys its summary too
            s.setText(summary);
            if (summary.length() == 0) s.setVisibility(View.GONE);
            col.addView(s);
        }
        r.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (click != null) {
            TypedValue tv = new TypedValue();
            if (c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) r.setBackgroundResource(tv.resourceId);
            r.setOnClickListener(click);
        }
        return r;
    }

    /** BN1p D2: a summary that is not known yet is one no-break space, not "": the row keeps its two lines, so nothing below it moves when the real text arrives (Tj's screenshots have the line from the first frame). */
    static final String BLANK = "\u00A0";

    /** {@code s}, or {@link #BLANK} when it is null or empty. */
    static String orBlank(String s) { return s == null || s.length() == 0 ? BLANK : s; }

    /** Changes the summary of a row made by {@link #row} (the row must have been created with a non-null summary); an empty text hides the line. */
    static void setSummary(View row, CharSequence text) {
        if (!(row instanceof ViewGroup) || ((ViewGroup) row).getChildCount() < 2) return;
        View col = ((ViewGroup) row).getChildAt(1);
        if (!(col instanceof ViewGroup) || ((ViewGroup) col).getChildCount() < 2) return;
        TextView s = (TextView) ((ViewGroup) col).getChildAt(1);
        s.setText(text);
        s.setVisibility(text == null || text.length() == 0 ? View.GONE : View.VISIBLE);
    }

    /** What a switch row reports. */
    interface Toggle { void onToggle(boolean checked); }

    /** A row with a switch at the right edge (AOSP SwitchPreference: the whole row toggles, the switch itself is not touched separately). */
    static LinearLayout switchRow(Context c, CharSequence title, CharSequence summary, final android.widget.Switch sw, final Toggle t) {
        LinearLayout r = row(c, null, title, summary, new View.OnClickListener() {
            @Override public void onClick(View v) { boolean to = !sw.isChecked(); sw.setChecked(to); t.onToggle(to); }
        });
        sw.setClickable(false);
        sw.setFocusable(false);
        r.addView(sw, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return r;
    }

    /** A row with a value at the right edge (AOSP preference_app + preference_widget_summary: 14 sp, secondary colour, text end 16 dp from the screen edge); {@code value} is the caller's TextView, filled in later. */
    static LinearLayout valueRow(Context c, CharSequence title, TextView value) {
        LinearLayout r = row(c, null, title, null, null);
        value.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
        value.setTextColor(themeColor(c, android.R.attr.textColorSecondary, Color.GRAY));
        value.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        value.setMinWidth(dp(c, 64));
        r.addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return r;
    }

    /** The footer note of a page (AOSP preference_footer: icon frame 56 dp wide with a 24 dp info icon at its top, 14 sp secondary text, at least 72 dp high, not clickable). */
    static LinearLayout footer(Context c, CharSequence text) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        r.setMinimumHeight(dp(c, 72));
        FrameLayout frame = new FrameLayout(c);
        frame.setPadding(0, dp(c, 16), dp(c, 12), dp(c, 4));
        ImageView ic = new ImageView(c);
        ic.setImageResource(R.drawable.ss_ic_info_outline);
        frame.addView(ic, new FrameLayout.LayoutParams(dp(c, 24), dp(c, 24), Gravity.START | Gravity.TOP));
        r.addView(frame, new LinearLayout.LayoutParams(dp(c, 56), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView t = new TextView(c);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTextColor(themeColor(c, android.R.attr.textColorSecondary, Color.GRAY));
        t.setPaddingRelative(0, dp(c, 16), dp(c, 16), dp(c, 16));   // measured: Tj's footer wraps "...based on / usage", the text area ends about 16 dp before the row's own padding
        t.setText(text);
        r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return r;
    }

    /** The coloured square of a home-page row: 36 dp, corner radius 2.9 dp, a white 24 dp glyph inset 6 dp (AOSP dashboard_tile_image_size / _foreground_image_size / _inset). */
    static View tile(Context c, int color, int glyphRes) {
        FrameLayout f = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(c, 2.9f));   // the stock icon mask: 8 % of the 36 dp tile (measured on Tj's screenshot: the corner cuts 3, 2, 1, 0 px from the top row down)
        f.setBackground(bg);
        ImageView g = new ImageView(c);
        g.setImageResource(glyphRes);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(c, 24), dp(c, 24));
        lp.setMargins(dp(c, 6), dp(c, 6), dp(c, 6), dp(c, 6));
        f.addView(g, lp);
        f.setLayoutParams(new FrameLayout.LayoutParams(dp(c, 36), dp(c, 36)));
        return f;
    }

    static Drawable glyph(Context c, int res) { return c.getDrawable(res); }

    /** A themed 24 dp icon (vector with the theme's control tint) for the icon frame of a row. */
    static View icon(Context c, int res) {
        ImageView iv = new ImageView(c);
        iv.setImageDrawable(c.getDrawable(res));
        iv.setLayoutParams(new FrameLayout.LayoutParams(dp(c, 24), dp(c, 24), Gravity.START | Gravity.CENTER_VERTICAL));
        return iv;
    }

    /**
     * A category heading: teal capitals, 11 sp medium, letter spacing 0.0727, text at 72 dp. The block is 32 dp of padding with 8 dp of it BELOW the text (measured 2026-11-09 on Tj's Languages screenshot against
     * the first CI render: the glyphs sit exactly 8 px lower in the stock, the next row does not move; 16 / 16 dp was 8 px too high). The top is "32 dp minus 8 dp" so the block keeps its rounded height at any density.
     */
    static TextView heading(Context c, CharSequence text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setAllCaps(true);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setLetterSpacing(0.072727f);
        t.setTextColor(themeColor(c, android.R.attr.colorAccent, 0xFF008577));
        t.setPaddingRelative(dp(c, 72), dp(c, 32) - dp(c, 8), dp(c, 16), dp(c, 8));
        return t;
    }

    /** A hairline divider: 1 px, #DADCE0 (measured on Tj's screenshots of the Battery and Languages pages). */
    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0xFFDADCE0);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }
}
