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

    /** A standard preference row: optional icon (36 dp tile or 24 dp glyph), title, optional summary; {@code click} null = not clickable (no ripple). */
    static LinearLayout row(Context c, View icon, CharSequence title, CharSequence summary, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        r.setMinimumHeight(dp(c, 48));
        FrameLayout frame = new FrameLayout(c);   // the icon frame: 56 dp wide even when there is no icon (the text of every row starts at 72 dp)
        frame.setMinimumWidth(dp(c, 56));
        if (icon != null) frame.addView(icon, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL));
        r.addView(frame, new LinearLayout.LayoutParams(dp(c, 56), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, dp(c, 16), 0, dp(c, 16));
        TextView t = new TextView(c);
        appearance(t, android.R.attr.textAppearanceListItem);
        t.setText(title);
        col.addView(t);
        if (summary != null && summary.length() > 0) {
            TextView s = new TextView(c);
            appearance(s, android.R.attr.textAppearanceListItemSecondary);
            s.setTextColor(themeColor(c, android.R.attr.textColorSecondary, Color.GRAY));
            s.setText(summary);
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

    /** The coloured square of a home-page row: 36 dp, small rounded corners, a white 24 dp glyph inset 6 dp (AOSP dashboard_tile_image_size / _foreground_image_size / _inset). */
    static View tile(Context c, int color, int glyphRes) {
        FrameLayout f = new FrameLayout(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(c, 6));
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

    /** A category heading: teal capitals, 11 sp medium, letter spacing 0.0727, text at 72 dp, 16 dp above and below (AOSP TextAppearance.CategoryTitle + the Settings category layout). */
    static TextView heading(Context c, CharSequence text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setAllCaps(true);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        t.setLetterSpacing(0.072727f);
        t.setTextColor(themeColor(c, android.R.attr.colorAccent, 0xFF008577));
        t.setPaddingRelative(dp(c, 72), dp(c, 16), dp(c, 16), dp(c, 16));
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
