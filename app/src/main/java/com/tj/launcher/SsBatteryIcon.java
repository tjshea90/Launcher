package com.tj.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * BN1: the big battery of the Battery page, drawn the way AOSP 10 does (SettingsLib ThemedBatteryDrawable, the single-tone branch the Settings page uses; research/aosp10-settings/icons/BATTERY_METER.md): the
 * perimeter, a level rectangle and (charging) the bolt on a 12 x 20 grid scaled to the bounds, one flat colour. The four path strings are the framework's config_batterymeter* values, copied literally.
 * Colour: the theme accent; below 5 % the error colour at 54 %; in battery saver the foreground colour with an error-coloured perimeter and plus.
 */
final class SsBatteryIcon extends Drawable {
    private static final String PERIMETER = "M3.5,2 v0 H1.33 C0.6,2 0,2.6 0,3.33 V13v5.67 C0,19.4 0.6,20 1.33,20 h9.33 C11.4,20 12,19.4 12,18.67 V13V3.33 C12,2.6 11.4,2 10.67,2 H8.5 V0 H3.5 z M2,18v-7V4h8v9v5H2L2,18z";
    private static final String FILL_MASK = "M2,18 v-14 h8 v14 z";
    private static final String BOLT = "M5,17.5 V12 H3 L7,4.5 V10 h2 L5,17.5 z";
    private static final String PLUS = "M9,10l-2,0l0,-2l-2,0l0,2l-2,0l0,2l2,0l0,2l2,0l0,-2l2,0z";
    private static final int CRITICAL_LEVEL = 5;          // config_criticalBatteryWarningLevel
    private static final int LOW_LEVEL = 15;              // ThemedBatteryDrawable.CRITICAL_LEVEL: the level rectangle is painted twice at or under it

    private final Path perimeter = SsPathData.parse(PERIMETER);
    private final Path fillMask = SsPathData.parse(FILL_MASK);
    private final Path bolt = SsPathData.parse(BOLT);
    private final Path plus = SsPathData.parse(PLUS);
    private final Path sPerimeter = new Path(), sFillMask = new Path(), sBolt = new Path(), sPlus = new Path(), unified = new Path(), levelPath = new Path();
    private final RectF fillRect = new RectF(), levelRect = new RectF();
    private final Matrix scale = new Matrix();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), halo = new Paint(Paint.ANTI_ALIAS_FLAG), error = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int accent, errorColor, foreground;
    private int level = 100;
    private boolean charging, powerSave, invertFill;
    private final int w, h;

    /** {@code w} x {@code h} = the intrinsic size in pixels (66 x 100 dp). */
    SsBatteryIcon(int accent, int errorColor, int foreground, int w, int h) {
        this.accent = accent; this.errorColor = errorColor; this.foreground = foreground; this.w = w; this.h = h;
        fill.setStyle(Paint.Style.FILL);
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeJoin(Paint.Join.ROUND);
        halo.setStrokeMiter(5f);
        error.setStyle(Paint.Style.FILL);
        error.setColor(errorColor);
        applyColors();
    }

    /** The state the page last read; the drawable redraws itself. */
    void set(int level, boolean charging, boolean powerSave) {
        this.level = Math.max(0, Math.min(100, level));
        // ThemedBatteryDrawable.setBatteryLevel: the hysteresis of the bolt cut-out (starts false)
        if (this.level >= 67) invertFill = true; else if (this.level <= 33) invertFill = false;
        this.charging = charging;
        this.powerSave = powerSave;
        applyColors();
        invalidateSelf();
    }

    /** BatteryMeterView.updateColorFilter, written as the colour the filter produces: power save = foreground, under 5 % = the error colour at 54 %, else the accent. */
    private void applyColors() {
        int c;
        if (powerSave) c = foreground;
        else if (level < CRITICAL_LEVEL) c = (errorColor & 0x00FFFFFF) | (Math.round(255 * 0.54f) << 24);
        else c = accent;
        fill.setColor(c);
        halo.setColor(c);
    }

    @Override public int getIntrinsicWidth() { return w; }
    @Override public int getIntrinsicHeight() { return h; }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        scale.setScale(b.width() / 12f, b.height() / 20f);
        perimeter.transform(scale, sPerimeter);
        fillMask.transform(scale, sFillMask);
        bolt.transform(scale, sBolt);
        plus.transform(scale, sPlus);
        sFillMask.computeBounds(fillRect, true);

        int save = canvas.saveLayer(b.left, b.top, b.right, b.bottom, null);
        canvas.translate(b.left, b.top);
        float fraction = level / 100f;
        float fillTop = level >= 95 ? fillRect.top : fillRect.top + fillRect.height() * (1f - fraction);
        levelRect.set(fillRect.left, (float) Math.floor(fillTop), fillRect.right, fillRect.bottom);
        levelPath.reset();
        levelPath.addRect(levelRect, Path.Direction.CCW);
        unified.reset();
        unified.op(sPerimeter, levelPath, Path.Op.UNION);

        if (charging) {
            unified.op(sBolt, Path.Op.DIFFERENCE);
            if (!invertFill) canvas.drawPath(sBolt, fill);
        }
        canvas.drawPath(unified, fill);

        if (!charging && level <= LOW_LEVEL) {
            canvas.save();
            canvas.clipPath(sFillMask);
            canvas.drawPath(levelPath, fill);
            canvas.restore();
        }
        if (charging) {
            canvas.save();
            canvas.clipOutPath(sBolt);
            halo.setStrokeWidth(Math.max(b.width() / 12f * 3f, 6f));
            if (invertFill) { halo.setXfermode(null); }
            else halo.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPath(sBolt, halo);
            halo.setXfermode(null);
            canvas.restore();
        }
        if (!charging && powerSave) {
            canvas.drawPath(sPerimeter, error);
            canvas.drawPath(sPlus, error);
        }
        canvas.restoreToCount(save);
    }

    @Override public void setAlpha(int alpha) { fill.setAlpha(alpha); halo.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { fill.setColorFilter(cf); halo.setColorFilter(cf); error.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
