package com.tj.launcher;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.android.launcher3.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * BN1: the Font size and Display size pages, built from AOSP 10 (font_size_activity.xml, screen_zoom_activity.xml, preview_seek_bar_view_pager.xml; research/aosp10-settings/A_battery_display.md sec. 4.4 - 4.6).
 * One preview frame (grey border, white card, a 56 dp "Preview" bar) over a label, a plus / minus (or small / large "A") button pair around a stepped slider, and a sentence. Every stop of the slider has its own
 * preview layer, built in a context with THAT stop's font scale or density (like AOSP's PreviewPagerAdapter), so the preview shows what the choice would do; only the current layer is visible.
 * Applying: Font size writes Settings.System font_scale; Display size runs {@code wm density} (the app holds WRITE_SECURE_SETTINGS, which is all the window manager asks for). Choosing the stop that is
 * already active changes nothing; opening the page writes nothing (the slider's listener is attached after its first position, as in AOSP). Open: the exact tablet table of Font size steps and the Display size
 * preview pages wait for Tj's screenshots of those two pages (BN1 open question).
 */
final class SsPagesSize {
    private SsPagesSize() {}

    private interface PageBuilder { View build(Context stopContext); }
    private interface Commit { void commit(int stop); }
    private interface StopContext { Context forStop(int stop); }

    // ------------------------------------------------------------------ Font size
    static View fontSize(final Activity a, List<SsPages.Live> live) {
        final float[] values = SsDisplayRules.FONT_VALUES;
        StopContext ctx = new StopContext() {
            @Override public Context forStop(int stop) {
                Configuration cfg = new Configuration(a.getResources().getConfiguration());
                cfg.fontScale = values[stop];
                return new ContextThemeWrapper(a.createConfigurationContext(cfg), R.style.SsTheme);
            }
        };
        PageBuilder[] pages = { new PageBuilder() { @Override public View build(Context c) { return fontPreview(c); } } };
        return page(a, SsDisplayRules.FONT_LABELS, SsDisplayRules.fontIndex(SsPagesDisplay.fontScale(a)), false, ctx, pages, new Commit() {
            @Override public void commit(final int stop) {
                if (SsDisplayRules.fontIndex(SsPagesDisplay.fontScale(a)) == stop) return;
                SsPagesDisplay.write(a, new Runnable() { @Override public void run() { Settings.System.putFloat(a.getContentResolver(), Settings.System.FONT_SCALE, values[stop]); } });
            }
        }, "Make the text on screen smaller or larger.");
    }

    // ------------------------------------------------------------------ Display size
    static View displaySize(final Activity a, List<SsPages.Live> live) {
        final SsDisplayRules.Density d = SsPagesDisplay.densityStops(a);
        final int def = android.util.DisplayMetrics.DENSITY_DEVICE_STABLE;
        StopContext ctx = new StopContext() {
            @Override public Context forStop(int stop) {
                Configuration cfg = new Configuration(a.getResources().getConfiguration());
                cfg.densityDpi = d.values[stop];
                return new ContextThemeWrapper(a.createConfigurationContext(cfg), R.style.SsTheme);
            }
        };
        PageBuilder[] pages = {
            new PageBuilder() { @Override public View build(Context c) { return conversationPreview(c); } },
            new PageBuilder() { @Override public View build(Context c) { return appsPreview(c); } },
            new PageBuilder() { @Override public View build(Context c) { return settingsPreview(c); } },
        };
        return page(a, d.labels, d.index, true, ctx, pages, new Commit() {
            @Override public void commit(final int stop) {
                if (d.values[stop] == a.getResources().getConfiguration().densityDpi) return;
                final int dpi = d.values[stop];
                new Thread(new Runnable() { @Override public void run() { setDensity(dpi == def ? null : String.valueOf(dpi)); } }, "ss-density").start();
            }
        }, "Make the items on your screen smaller or larger. Some apps on your screen may change position.");
    }

    /** {@code wm density N} / {@code wm density reset} as this app (it holds WRITE_SECURE_SETTINGS); never blocks for ever, never throws. */
    static void setDensity(String dpiOrNull) {
        try {
            Process p = new ProcessBuilder("wm", "density", dpiOrNull == null ? "reset" : dpiOrNull).redirectErrorStream(true).start();
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                try { p.exitValue(); return; } catch (IllegalThreadStateException running) { try { Thread.sleep(50); } catch (InterruptedException e) { return; } }
            }
            p.destroy();
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ the shared page
    private static View page(final Activity a, final String[] labels, int start, boolean zoom, final StopContext stops, final PageBuilder[] pages, final Commit commit, String summary) {
        final int n = labels.length;
        final int pad8 = SsRows.dp(a, 8);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);

        // the preview frame: solid colorPrimary (the layer-list's stroke and fill both resolve to it), 8 dp padding, a white card, a 56 dp bar with the word "Preview"
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad8, pad8, pad8, pad8);
        box.setBackgroundColor(SsRows.themeColor(a, android.R.attr.colorPrimary, 0xFFF5F5F5));
        final ViewFlipper flipper = new ViewFlipper(a);
        flipper.setBackgroundColor(SsRows.themeColor(a, android.R.attr.colorBackgroundFloating, Color.WHITE));
        flipper.setContentDescription("Preview");
        final FrameLayout[] frames = new FrameLayout[pages.length];
        final View[][] layers = new View[pages.length][n];
        for (int i = 0; i < pages.length; i++) { frames[i] = new FrameLayout(a); flipper.addView(frames[i], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)); }
        box.addView(flipper, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SsRows.dp(a, 100), 1f));
        TextView bar = new TextView(a);
        bar.setText("Preview");
        bar.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        bar.setPaddingRelative(SsRows.dp(a, 32), 0, 0, 0);
        // the same text appearance as the toolbar title (AOSP: TextAppearance.DeviceDefault.Widget.ActionBar.Title): Tj's shots show the word regular, ~20 px, and unchanged at every font size (it is not an sp text)
        bar.setTextAppearance(a, android.R.style.TextAppearance_DeviceDefault_Widget_ActionBar_Title);
        bar.setTextColor(SsRows.themeColors(a, android.R.attr.textColorPrimary, Color.BLACK));
        bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        box.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SsRows.dp(a, 56)));
        root.addView(box, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // controls
        LinearLayout controls = new LinearLayout(a);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPaddingRelative(SsRows.dp(a, 16), SsRows.dp(a, 16), SsRows.dp(a, 16), 0);
        final Dots dots = new Dots(a, pages.length);
        if (pages.length > 1) {
            dots.setPadding(SsRows.dp(a, 6), SsRows.dp(a, 6), SsRows.dp(a, 6), SsRows.dp(a, 6));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.gravity = Gravity.CENTER_HORIZONTAL;
            controls.addView(dots, dlp);
        }
        final TextView current = new TextView(a);
        current.setPadding(SsRows.dp(a, 6), SsRows.dp(a, 6), SsRows.dp(a, 6), SsRows.dp(a, 6));
        current.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        current.setTextColor(SsRows.themeColors(a, android.R.attr.textColorPrimary, Color.BLACK));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.CENTER_HORIZONTAL;
        controls.addView(current, clp);
        LinearLayout row = new LinearLayout(a);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (!zoom) row.setPadding(0, SsRows.dp(a, 8), 0, 0);
        final ImageView smaller = stepButton(a, zoom ? R.drawable.ss_ic_remove : R.drawable.ss_ic_font_16, "Make smaller");
        final ImageView larger = stepButton(a, zoom ? R.drawable.ss_ic_add : R.drawable.ss_ic_font_24, "Make larger");
        final SeekBar seek = SsRows.discreteSeekBar(a);
        seek.setMax(n - 1);
        seek.setProgress(start);
        row.addView(smaller, new LinearLayout.LayoutParams(SsRows.dp(a, 48), SsRows.dp(a, 48)));
        row.addView(seek, new LinearLayout.LayoutParams(0, SsRows.dp(a, 48), 1f));
        row.addView(larger, new LinearLayout.LayoutParams(SsRows.dp(a, 48), SsRows.dp(a, 48)));
        controls.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView note = new TextView(a);
        SsRows.appearance(note, android.R.attr.textAppearanceListItem);   // Subhead: 16 sp, primary colour
        note.setText(summary);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nlp.bottomMargin = SsRows.dp(a, 16);
        controls.addView(note, nlp);
        root.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Runnable show = new Runnable() {
            @Override public void run() {
                int stop = seek.getProgress();
                current.setText(labels[stop]);
                smaller.setEnabled(stop > 0);
                larger.setEnabled(stop < n - 1);
                for (int i = 0; i < pages.length; i++) {
                    if (layers[i][stop] == null) {
                        layers[i][stop] = pages[i].build(stops.forStop(stop));
                        frames[i].addView(layers[i][stop], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    }
                    for (int s = 0; s < n; s++) if (layers[i][s] != null) layers[i][s].setVisibility(s == stop ? View.VISIBLE : View.INVISIBLE);
                }
            }
        };
        show.run();   // the first position: nothing is written for it (the listener is attached below)
        final boolean[] dragging = new boolean[1];
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                show.run();
                if (!dragging[0]) commit.commit(progress);   // buttons and keys apply at once; a drag applies when the finger lifts
            }
            @Override public void onStartTrackingTouch(SeekBar s) { dragging[0] = true; }
            @Override public void onStopTrackingTouch(SeekBar s) { dragging[0] = false; commit.commit(s.getProgress()); }
        });
        smaller.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { seek.setProgress(seek.getProgress() - 1); } });
        larger.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { seek.setProgress(seek.getProgress() + 1); } });

        if (pages.length > 1) {   // the preview pages of Display size swipe sideways
            final GestureDetector swipe = new GestureDetector(a, new GestureDetector.SimpleOnGestureListener() {
                @Override public boolean onDown(MotionEvent e) { return true; }
                @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                    if (e1 == null || Math.abs(vx) < Math.abs(vy) || Math.abs(vx) < 400) return false;
                    int to = flipper.getDisplayedChild() + (vx < 0 ? 1 : -1);
                    if (to < 0 || to >= pages.length) return true;
                    flipper.setDisplayedChild(to);
                    dots.select(to);
                    return true;
                }
            });
            flipper.setOnTouchListener(new View.OnTouchListener() { @Override public boolean onTouch(View v, MotionEvent e) { return swipe.onTouchEvent(e); } });
            dots.select(0);
        }

        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.addView(root, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return sv;
    }

    private static ImageView stepButton(Context c, int res, String description) {
        ImageView b = new ImageView(c);
        b.setImageResource(res);
        b.setImageTintList(SsRows.themeColors(c, android.R.attr.textColorPrimary, Color.BLACK));
        b.setImageTintMode(PorterDuff.Mode.SRC_IN);
        b.setScaleType(ImageView.ScaleType.CENTER);
        b.setFocusable(true);
        b.setContentDescription(description);
        android.util.TypedValue tv = new android.util.TypedValue();
        if (c.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true)) b.setBackgroundResource(tv.resourceId);
        return b;
    }

    /** The page indicator of the Display size preview (AOSP DotsPageIndicator with the style of the page: 8 dp dots, 8 dp apart, 54 % grey, the current one in the accent colour). */
    private static final class Dots extends View {
        private final int count, dot, gap;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int idle, active;
        private int current;
        Dots(Context c, int count) {
            super(c);
            this.count = count;
            dot = SsRows.dp(c, 8);
            gap = SsRows.dp(c, 8);
            idle = SsRows.themeColor(c, android.R.attr.colorControlNormal, 0x8A000000);
            active = SsRows.themeColor(c, android.R.attr.colorControlActivated, 0xFF008577);
        }
        void select(int i) { current = i; invalidate(); }
        @Override protected void onMeasure(int w, int h) {
            setMeasuredDimension(getPaddingLeft() + getPaddingRight() + count * dot + (count - 1) * gap, getPaddingTop() + getPaddingBottom() + dot);
        }
        @Override protected void onDraw(Canvas canvas) {
            float r = dot / 2f;
            for (int i = 0; i < count; i++) {
                paint.setColor(i == current ? active : idle);
                canvas.drawCircle(getPaddingLeft() + r + i * (dot + gap), getPaddingTop() + r, r, paint);
            }
        }
    }

    // ------------------------------------------------------------------ preview contents
    private static final String OZ_1 = "Even with eyes protected by the green spectacles Dorothy and her friends were at first dazzled by the brilliancy of the wonderful City. The streets were lined with beautiful houses all built of green marble and studded everywhere with sparkling emeralds. They walked over a pavement of the same green marble, and where the blocks were joined together were rows of emeralds, set closely, and glittering in the brightness of the sun. The window panes were of green glass; even the sky above the City had a green tint, and the rays of the sun were green.";
    private static final String OZ_2 = "There were many people, men, women and children, walking about, and these were all dressed in green clothes and had greenish skins. They looked at Dorothy and her strangely assorted company with wondering eyes, and the children all ran away and hid behind their mothers when they saw the Lion; but no one spoke to them. Many shops stood in the street, and Dorothy saw that everything in them was green. Green candy and green pop-corn were offered for sale, as well as green shoes, green hats and green clothes of all sorts. At one place a man was selling green lemonade, and when the children bought it Dorothy could see that they paid for it with green pennies.";
    private static final String OZ_3 = "There seemed to be no horses nor animals of any kind; the men carried things around in little green carts, which they pushed before them. Everyone seeemed happy and contented and prosperous.";

    private static TextView text(Context c, CharSequence t, float sp, String family, int style) {
        TextView v = new TextView(c);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTypeface(Typeface.create(family, style));
        v.setTextColor(SsRows.themeColors(c, android.R.attr.textColorPrimary, Color.BLACK));
        return v;
    }

    private static View scroll(Context c, LinearLayout content) {
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        sv.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        return sv;
    }

    /** font_size_preview.xml: four lines (headline 24 sp, title 20 sp, subhead 16 sp, body 14 sp), padding 16 dp top and sides. */
    private static View fontPreview(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPaddingRelative(SsRows.dp(c, 16), SsRows.dp(c, 16), SsRows.dp(c, 16), 0);
        col.addView(text(c, "Sample text", 24, "sans-serif", Typeface.NORMAL));
        col.addView(text(c, "The Wonderful Wizard of Oz", 20, "sans-serif", Typeface.NORMAL));   // regular weight on Tj's tablet (his four Font size shots), not medium
        col.addView(text(c, "Chapter 11: The Wonderful Emerald City of Oz", 16, "sans-serif", Typeface.NORMAL));
        col.addView(text(c, OZ_1 + "\n\n" + OZ_2 + "\n\n" + OZ_3, 14, "sans-serif", Typeface.NORMAL));
        return scroll(c, col);
    }

    /** screen_zoom_preview_1: four chat messages (outgoing: no bubble and a blue "A" disc; incoming: a green bubble with a tail and a green "P" disc), drawn to Tj's measured geometry (SsConversation). */
    private static View conversationPreview(Context c) {
        return scroll(c, SsConversation.column(c));
    }

    /** screen_zoom_preview_2: the first six launcher apps by label in a grid of three columns (icon 64 dp, label 12 sp). */
    private static View appsPreview(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> all = new ArrayList<>(pm.queryIntentActivities(main, 0));
        List<ResolveInfo> six = new ArrayList<>();
        for (ResolveInfo ri : all) { if (six.size() == 6) break; if (ri.loadLabel(pm) != null && ri.loadLabel(pm).length() > 0) six.add(ri); }
        final PackageManager fpm = pm;
        Collections.sort(six, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo x, ResolveInfo y) { return String.valueOf(x.loadLabel(fpm)).compareToIgnoreCase(String.valueOf(y.loadLabel(fpm))); }
        });
        LinearLayout grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);
        int p = SsRows.dp(c, 32);
        grid.setPadding(p, p, p, p);
        LinearLayout line = null;
        for (int i = 0; i < six.size(); i++) {
            if (i % 3 == 0) { line = new LinearLayout(c); grid.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); }
            LinearLayout cell = new LinearLayout(c);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            cell.setPadding(0, SsRows.dp(c, 8), 0, 0);
            ImageView icon = new ImageView(c);
            icon.setImageDrawable(six.get(i).loadIcon(pm));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(SsRows.dp(c, 64), SsRows.dp(c, 64));
            ilp.bottomMargin = SsRows.dp(c, 4);
            cell.addView(icon, ilp);
            TextView label = text(c, six.get(i).loadLabel(pm), 12, "sans-serif", Typeface.NORMAL);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(label);
            line.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        return scroll(c, grid);
    }

    /** screen_zoom_preview_settings: five Settings rows (48 dp accent icon, title 16 sp, summary 14 sp). */
    private static View settingsPreview(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        Object[][] rows = {
            { R.drawable.ss_ic_wireless, "Wi‑Fi", "Unavailable" },
            { R.drawable.ss_glyph_system, "Data usage", "No data used" },
            { R.drawable.ss_glyph_display, "Display", "Sleep after 10 minutes of inactivity" },
            { R.drawable.ss_glyph_sound, "Sound", "Ring volume at 80%" },
            { R.drawable.ss_glyph_storage, "App info", "24 apps installed" },
        };
        ColorStateList accent = ColorStateList.valueOf(SsRows.themeColor(c, android.R.attr.colorAccent, 0xFF008577));
        for (Object[] r : rows) {
            LinearLayout line = new LinearLayout(c);
            line.setPaddingRelative(SsRows.dp(c, 8), SsRows.dp(c, 16), 0, 0);
            ImageView icon = new ImageView(c);
            icon.setImageResource((Integer) r[0]);
            icon.setImageTintList(accent);
            icon.setScaleType(ImageView.ScaleType.CENTER);
            line.addView(icon, new LinearLayout.LayoutParams(SsRows.dp(c, 48), SsRows.dp(c, 48)));
            LinearLayout text = new LinearLayout(c);
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPaddingRelative(SsRows.dp(c, 16), 0, 0, 0);
            text.addView(text(c, (String) r[1], 16, "sans-serif", Typeface.NORMAL));
            TextView summary = text(c, (String) r[2], 14, "sans-serif", Typeface.NORMAL);
            summary.setTextColor(SsRows.themeColors(c, android.R.attr.textColorSecondary, Color.GRAY));
            text.addView(summary);
            line.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            col.addView(line);
        }
        return scroll(c, col);
    }
}
