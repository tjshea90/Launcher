package com.tj.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * BN1l: the chat of the first Display size preview page (screen_zoom_preview_1), drawn to the geometry MEASURED in Tj's five Display size screenshots (2026-11-09; Small 144 .. Largest 254 dpi,
 * research/SETTINGS_BUILD_LOG_2026-11-09.md decision 13). What the shots show: an outgoing message has NO bubble (dark text, a lavender time, a blue 52 dp "A" disc on the right); an incoming message
 * is a green bubble with a pointed tail at its top-left corner, white text, a dark-green time and a green "P" disc on the left. Everything is in dp / sp so the preview scales with the density of the
 * context it is inflated in (every stop of the slider has its own layer).
 * Geometry (dp unless noted): disc 52, letter 32 sp bold; message text 16 sp, time 12 sp; row top margin 18, list padding 10; bubble padding top 16, bottom 12, time 3 below the text and 4 of room after it;
 * incoming padding start 20.4 / end 15, outgoing start 14.3 / end 21.6; bubble 1.5 from its disc; widest bubble = list width - 2 x (52 + 6); the outgoing time starts 3.2 further in than its text;
 * tail 10 wide x 12 high, corner radius 4.
 */
final class SsConversation {
    private SsConversation() {}

    private static final int DISC_OUT = 0xFF4285F4, DISC_IN = 0xFF689F38, BUBBLE_IN = 0xFF689F38;
    private static final int TEXT_OUT = 0xFF323232, TIME_OUT = 0xFF8380AD, TEXT_IN = Color.WHITE, TIME_IN = 0xFF375049;

    /** The four messages of the page (the caller puts them in a scroll view). */
    static LinearLayout column(Context c) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = SsRows.dp(c, 10);
        col.setPadding(p, p, p, 0);
        col.setBackgroundColor(SsRows.themeColor(c, android.R.attr.colorBackgroundFloating, Color.WHITE));
        col.addView(new Row(c, false, "A", "Hi Pete!", "Tue 6:00PM"), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(new Row(c, true, "P", "Hey, want to grab coffee and catch up today?", "Tue 6:01PM"), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(new Row(c, false, "A", "Sounds great. I know of a good place not too far from here.", "Tue 6:02PM"), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(new Row(c, true, "P", "Perfect!", "Tue 6:03PM"), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return col;
    }

    /** One message: a disc at the outer edge, the bubble next to it, both starting at the row's top margin. */
    private static final class Row extends ViewGroup {
        private final boolean incoming;
        private final TextView disc;
        private final LinearLayout bubble;
        private final int discSize, topMargin, gap, reserve;

        Row(Context c, boolean incoming, String letter, String body, String time) {
            super(c);
            this.incoming = incoming;
            discSize = SsRows.dp(c, 52);
            topMargin = SsRows.dp(c, 18);
            gap = SsRows.dp(c, 1.5f);
            reserve = SsRows.dp(c, 6);

            disc = new TextView(c);
            disc.setText(letter);
            disc.setGravity(Gravity.CENTER);
            disc.setIncludeFontPadding(true);
            disc.setTextColor(Color.WHITE);
            disc.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            disc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            GradientDrawable oval = new GradientDrawable();
            oval.setShape(GradientDrawable.OVAL);
            oval.setColor(incoming ? DISC_IN : DISC_OUT);
            disc.setBackground(oval);

            bubble = new LinearLayout(c);
            bubble.setOrientation(LinearLayout.VERTICAL);
            float start = incoming ? 20.4f : 14.3f, end = incoming ? 15f : 21.6f;
            bubble.setPaddingRelative(SsRows.dp(c, start), SsRows.dp(c, 16), SsRows.dp(c, end), SsRows.dp(c, 12));
            if (incoming) bubble.setBackground(new Tail(c));
            TextView msg = new TextView(c);
            msg.setText(body);
            msg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            msg.setTextColor(incoming ? TEXT_IN : TEXT_OUT);
            TextView status = new TextView(c);
            status.setText(time);
            status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            status.setTextColor(incoming ? TIME_IN : TIME_OUT);
            status.setPaddingRelative(0, 0, SsRows.dp(c, 4), 0);
            bubble.addView(msg, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = SsRows.dp(c, 3);
            if (!incoming) slp.setMarginStart(SsRows.dp(c, 3.2f));
            bubble.addView(status, slp);
            addView(disc);
            addView(bubble);
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int w = MeasureSpec.getSize(widthSpec);
            disc.measure(MeasureSpec.makeMeasureSpec(discSize, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(discSize, MeasureSpec.EXACTLY));
            int max = Math.max(0, w - 2 * (discSize + reserve));
            bubble.measure(MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            setMeasuredDimension(w, topMargin + Math.max(discSize, bubble.getMeasuredHeight()));
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int w = r - l, bw = bubble.getMeasuredWidth(), bh = bubble.getMeasuredHeight();
            boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            boolean left = incoming != rtl;   // the disc of an incoming message sits at the start edge
            if (left) {
                disc.layout(0, topMargin, discSize, topMargin + discSize);
                bubble.layout(discSize + gap, topMargin, discSize + gap + bw, topMargin + bh);
            } else {
                disc.layout(w - discSize, topMargin, w, topMargin + discSize);
                bubble.layout(w - discSize - gap - bw, topMargin, w - discSize - gap, topMargin + bh);
            }
        }
    }

    /** The green bubble of an incoming message: a rounded body that starts 10 dp in, and a triangular tail from the top-left corner down to the body's edge 12 dp lower. */
    private static final class Tail extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final float tailW, tailH, radius;

        Tail(Context c) {
            paint.setColor(BUBBLE_IN);
            tailW = SsRows.dp(c, 10);
            tailH = SsRows.dp(c, 12);
            radius = SsRows.dp(c, 4);
        }

        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            path.reset();
            // body: a rounded rectangle from tailW to the right edge
            path.addRoundRect(new RectF(b.left + tailW, b.top, b.right, b.bottom), radius, radius, Path.Direction.CW);
            // tail: tip at the top-left corner, along the top edge to the body, down the body's left edge by tailH
            Path tail = new Path();
            tail.moveTo(b.left, b.top);
            tail.lineTo(b.left + tailW + radius, b.top);
            tail.lineTo(b.left + tailW + radius, b.top + radius);
            tail.lineTo(b.left + tailW, b.top + tailH);
            tail.close();
            path.addPath(tail);
            canvas.drawPath(path, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
