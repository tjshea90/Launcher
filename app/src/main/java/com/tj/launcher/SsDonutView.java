package com.tj.launcher;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.icu.text.DecimalFormatSymbols;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import java.util.Locale;

/**
 * BN1: the donut of the Storage page header, drawn in code (the stock one is com.android.settings.widget.DonutView of AOSP 10, settings10/src/com/android/settings/widget/DonutView.java, read from android-10.0.0_r47;
 * "95%" over "used" inside a thin ring). The arithmetic is the stock one on purpose, so it can be compared pixel for pixel with Tj's screenshot: the ring is the view minus one stroke on every side, starts at 12 o'clock,
 * has square (BUTT) ends and its track is the accent colour at 30 % (meter_background_color); the number is drawn by a StaticLayout over the full width, the label under it on a baseline derived from descent - ascent.
 * What differs on purpose: an unknown percentage (not read yet or not readable) draws the track alone and no text, where the stock one is never drawn before its number is known.
 */
final class SsDonutView extends View {
    private static final int TOP = -90;                  // DonutView.TOP: the arc starts at 12 o'clock
    private static final int LINE_CHARACTER_LIMIT = 10;  // DonutView: "the longest we can go without visual errors" for the label (the shrunken size above it)

    private final float stroke;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint bigPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final float labelSize, shrunkenLabelSize, bigSize, signSize;
    private double percent = Double.NaN;
    private String percentString, fullString;

    SsDonutView(Context c) {
        super(c);
        final Resources res = c.getResources();
        final DisplayMetrics dm = res.getDisplayMetrics();
        // Stock: the constructor reads storage_donut_thickness (4 dp = 4.25 px at 170 dpi) as a float, then the inflate path calls getDimensionPixelSize(DonutView_thickness, (int) thickness); the layout sets no
        // thickness, so the stroke is that dimen cut to whole pixels (4 px). Do not "fix" it to 4.25: the ring would be 0.25 px wider on every side.
        stroke = (int) (4 * dm.density);
        final int foreground = SsRows.themeColor(c, android.R.attr.colorForeground, 0xFF212121);
        final int accent = SsRows.themeColor(c, android.R.attr.colorAccent, 0xFF008577);
        // Stock: both paints carry PorterDuffColorFilter(colorAccent, SRC_IN) over meter_consumed_color (colorForeground, alpha 1.0) and meter_background_color (colorForeground, android:alpha 0.3), i.e. the accent at
        // the foreground's alpha, and at 0.3 of it rounded the way ColorStateList.modulateColorAlpha does ((int) (alpha * 0.3f + 0.5f) = 77 for the opaque colorForeground). Drawing that colour directly is the same pixels.
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.BUTT);
        arc.setStrokeWidth(stroke);
        arc.setColor((Color.alpha(foreground) << 24) | (accent & 0x00FFFFFF));
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.BUTT);
        track.setStrokeWidth(stroke);
        track.setColor(((int) (Color.alpha(foreground) * 0.3f + 0.5f) << 24) | (accent & 0x00FFFFFF));

        // values-sw360dp / values-land of settings10: percent text 30sp, label 14sp, percent sign 20sp, shrunken label 10sp. They are read as FLOATS (Resources.getDimension, no rounding: 38.25 px at 170 dpi and font scale 1.2).
        labelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, dm);
        shrunkenLabelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, dm);
        bigSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 30, dm);
        signSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, dm);

        final Locale locale = res.getConfiguration().getLocales().get(0);
        final int bidiFlags = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_LTR ? Paint.BIDI_LTR : Paint.BIDI_RTL;
        labelPaint.setColor(accent);
        labelPaint.setTextSize(labelSize);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setBidiFlags(bidiFlags);
        bigPaint.setColor(accent);
        bigPaint.setTextSize(bigSize);
        bigPaint.setTypeface(headlineTypeface());
        bigPaint.setBidiFlags(bidiFlags);
    }

    /**
     * Typeface.create(config_headlineFontFamily, NORMAL): the family the stock page names for its headline texts (the donut number and the header's big number). The string is a framework resource the SDK does not
     * list; the build's own value is read by name, and a build without it gets the default family (AOSP's value is empty, which is Roboto).
     */
    static Typeface headlineTypeface() {
        String family = "";
        try {
            final Resources sys = Resources.getSystem();
            final int id = sys.getIdentifier("config_headlineFontFamily", "string", "android");
            if (id != 0) family = sys.getString(id);
        } catch (Throwable ignored) { }
        return Typeface.create(family, Typeface.NORMAL);
    }

    /**
     * Sets the used fraction (0..1). NaN or a negative number means "not known": the track is drawn alone and the text and the content description are cleared. Otherwise the percentage text is
     * Utils.formatPercentage (NumberFormat.getPercentInstance, half-even, no decimals) and the content description reads "95%, used" (join_two_unrelated_items).
     */
    void setPercentage(double fraction) {
        percent = fraction;
        if (Double.isNaN(fraction) || fraction < 0) {
            percentString = null;
            fullString = null;
            setContentDescription(null);
        } else {
            percentString = SsStorageRules.percent(fraction, Locale.getDefault());
            fullString = SsStorageRules.DONUT_LABEL;
            labelPaint.setTextSize(fullString.length() > LINE_CHARACTER_LIMIT ? shrunkenLabelSize : labelSize);
            setContentDescription(SsStorageRules.donutDescription(percentString));
        }
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final float w = getWidth();
        final float h = getHeight();
        canvas.drawArc(stroke, stroke, w - stroke, h - stroke, TOP, 360, false, track);   // the full circle first, then the used part over it
        if (percentString == null) return;
        canvas.drawArc(stroke, stroke, w - stroke, h - stroke, TOP, 360 * (float) percent, false, arc);
        drawInnerText(canvas);
    }

    /** DonutView.drawInnerText, line for line: the number is a StaticLayout across the view width, moved down so both lines are centred; the label hangs under it at startY - descent. */
    private void drawInnerText(Canvas canvas) {
        final float centerX = getWidth() / 2;    // integer division on purpose, as in the stock view: the label centres on a whole pixel
        final float centerY = getHeight() / 2;
        final float totalHeight = textHeight(labelPaint) + textHeight(bigPaint);
        final float startY = centerY + totalHeight / 2;
        final String localizedPercentSign = new DecimalFormatSymbols().getPercentString();

        canvas.save();
        final Spannable percentStringSpan = percentSpan(percentString, localizedPercentSign);
        // The old public constructor (deprecated in API 28) is this builder with its defaults: simple break strategy, no hyphenation, first-strong LTR direction, spacing 1 / 0.
        final StaticLayout percentStringLayout = StaticLayout.Builder.obtain(percentStringSpan, 0, percentStringSpan.length(), bigPaint, getWidth())
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();
        canvas.translate(0, (getHeight() - totalHeight) / 2);
        percentStringLayout.draw(canvas);
        canvas.restore();

        canvas.drawText(fullString, centerX, startY - labelPaint.descent(), labelPaint);   // the second line starts at the bottom plus room for the descender
    }

    /** DonutView.getPercentageStringSpannable: the percent sign (or, when it is not found, the whole text) is shrunk to sign size / number size of the number's size. */
    private Spannable percentSpan(String text, String sign) {
        final int[] range = SsStorageRules.percentSignRange(text, sign);
        final Spannable span = new SpannableString(text);
        span.setSpan(new RelativeSizeSpan(signSize / bigSize), range[0], range[1], Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return span;
    }

    private static float textHeight(TextPaint paint) {
        // The stock comment: "Technically, this should be the cap height, but I can live with the descent - ascent."
        return paint.descent() - paint.ascent();
    }
}
