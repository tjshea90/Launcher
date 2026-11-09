package com.tj.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BN1: the Battery page (a gray header with the percentage, the estimate and the big battery; "Last full charge", "Screen usage since full charge", the footer note) and its one overflow page, "Battery usage",
 * which holds one permanent Solitaire row and nothing else (Tj, 2026-11-09). Layout numbers are AOSP 10's battery_header.xml / preference_app.xml / preference_footer.xml
 * (research/aosp10-settings/A_battery_display.md sec. 2). The values Android keeps in its hidden BatteryStats come from {@link SsBatteryStats} (a background read); the rest from the public sticky battery intent.
 */
final class SsPagesBattery {
    private SsPagesBattery() {}

    static final String STATUS_KEY = "ss_battery_status";   // what the last read of the statistics gave (Launcher control + tablet report)
    private static final long MIN_RELOAD_MS = 30_000;

    static void battery(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        final Context c = a;
        final LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setBackgroundColor(SsRows.themeColor(c, android.R.attr.colorPrimaryDark, 0xFFE0E0E0));
        head.setPadding(0, SsRows.dp(c, 24), SsRows.dp(c, 16), SsRows.dp(c, 16));
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(SsRows.dp(c, 170), ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMarginStart(SsRows.dp(c, 72));
        clp.setMarginEnd(SsRows.dp(c, 8));
        head.addView(col, clp);

        final TextView pct = new TextView(c);
        pct.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        pct.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pct.setTextColor(SsRows.themeColor(c, android.R.attr.textColorPrimary, 0xFF212121));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = SsRows.dp(c, 12);
        col.addView(pct, plp);
        final TextView summary = new TextView(c);
        summary.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
        summary.setMinLines(2);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = SsRows.dp(c, 8);
        col.addView(summary, slp);
        TextView summary2 = new TextView(c);   // AOSP's second summary line: invisible, but its line is kept
        summary2.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
        summary2.setText(" ");
        summary2.setVisibility(View.INVISIBLE);
        col.addView(summary2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final SsBatteryIcon icon = new SsBatteryIcon(SsRows.themeColor(c, android.R.attr.colorAccent, 0xFF008577), 0xFFFF5722,
                SsRows.themeColor(c, android.R.attr.colorForeground, 0xFF000000), SsRows.dp(c, 66), SsRows.dp(c, 100));
        ImageView meter = new ImageView(c);
        meter.setImageDrawable(icon);
        meter.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SsRows.dp(c, 100));
        mlp.gravity = Gravity.CENTER;
        head.addView(meter, mlp);
        list.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        list.addView(SsRows.divider(c));
        final TextView lastFull = new TextView(c), screenUse = new TextView(c);
        list.addView(SsRows.valueRow(c, "Last full charge", lastFull));
        list.addView(SsRows.valueRow(c, "Screen usage since full charge", screenUse));
        list.addView(SsRows.divider(c));
        list.addView(SsRows.footer(c, "Battery usage data is approximate and can change based on usage"));

        live.add(new Page(a, pct, summary, icon, lastFull, screenUse));
    }

    /** The page's live part: follows the battery broadcast and re-reads the statistics in the background. */
    private static final class Page implements SsPages.Live {
        private final Activity a;
        private final TextView pct, summary, lastFull, screenUse;
        private final SsBatteryIcon icon;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final SsBatteryText.TimeOfDay clock;
        private SsBatteryStats.Data data = new SsBatteryStats.Data();
        private Intent last;
        private boolean started, loading;
        private long lastLoad;
        private final BroadcastReceiver rx = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent i) { last = i; render(); maybeReload(false); }
        };

        Page(Activity a, TextView pct, TextView summary, SsBatteryIcon icon, TextView lastFull, TextView screenUse) {
            this.a = a; this.pct = pct; this.summary = summary; this.icon = icon; this.lastFull = lastFull; this.screenUse = screenUse;
            final DateFormat f = android.text.format.DateFormat.getTimeFormat(a);
            this.clock = new SsBatteryText.TimeOfDay() { @Override public String format(long ms) { return f.format(new Date(ms)); } };
        }

        @Override public void start() {
            started = true;
            try { last = a.registerReceiver(rx, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); } catch (Throwable ignored) { }
            render();
            maybeReload(true);
        }

        @Override public void stop() {
            started = false;
            try { a.unregisterReceiver(rx); } catch (Throwable ignored) { }
        }

        private void maybeReload(boolean force) {
            long now = System.currentTimeMillis();
            if (loading || (!force && now - lastLoad < MIN_RELOAD_MS)) return;
            loading = true;
            lastLoad = now;
            final Context app = a.getApplicationContext();
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    final SsBatteryStats.Data d = SsBatteryStats.read(6000);
                    try { Features.setStringAsync(app, STATUS_KEY, new java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "  " + d.status
                            + "  start=" + (d.startClockMs >= 0) + " screen=" + (d.screenOnMs >= 0) + " drain=" + (d.dischargeMs >= 0) + " charge=" + (d.chargeMs >= 0)); } catch (Throwable ignored) { }
                    main.post(new Runnable() { @Override public void run() { loading = false; if (d.any()) data = d; if (started) render(); } });
                }
            }, "TjSsBatt");
            t.setDaemon(true);
            t.start();
        }

        private void render() {
            Intent i = last;
            if (i == null) return;
            int scale = Math.max(1, i.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            int level = Math.round(100f * i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / scale);
            int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            boolean saver = false;
            try { PowerManager pm = (PowerManager) a.getSystemService(Context.POWER_SERVICE); saver = pm != null && pm.isPowerSaveMode(); } catch (Throwable ignored) { }
            icon.set(level, plugged != 0, saver);
            pct.setText(percent(level));
            long now = System.currentTimeMillis();
            summary.setText(SsBatteryText.header(plugged, status, data.dischargeMs, data.chargeMs, now, clock));
            lastFull.setText(data.startClockMs >= 0 ? SsFormat.ago(Math.max(0, now - data.startClockMs)) : "");
            screenUse.setText(data.screenOnMs >= 0 ? SsFormat.elapsed(data.screenOnMs) : "");
        }
    }

    /** AOSP battery_header_title_alternate: the number, then a small gap and a 20 dp "%". */
    static CharSequence percent(int level) {
        SpannableStringBuilder b = new SpannableStringBuilder(String.valueOf(level));
        int gap = b.length();
        b.append(' ');
        b.setSpan(new RelativeSizeSpan(0.8f), gap, gap + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.append('%');
        b.setSpan(new RelativeSizeSpan(0.8f), gap, gap + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        b.setSpan(new AbsoluteSizeSpan(20, true), gap + 1, gap + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return b;
    }

    /** The "Battery usage" page: ONE permanent Solitaire row (its real icon when the app is installed), never anything else. */
    static void usage(final Activity a, LinearLayout list) {
        final ImageView iv = new ImageView(a);
        iv.setImageDrawable(a.getPackageManager().getDefaultActivityIcon());
        iv.setLayoutParams(new android.widget.FrameLayout.LayoutParams(SsRows.dp(a, 32), SsRows.dp(a, 32), Gravity.START | Gravity.CENTER_VERTICAL));
        list.addView(SsRows.row(a, iv, "Solitaire", null, null));
        final Context app = a.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    PackageManager pm = app.getPackageManager();
                    Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                    for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                        if (String.valueOf(ri.loadLabel(pm)).toLowerCase(Locale.ROOT).contains("solitaire")) {
                            final Drawable d = ri.loadIcon(pm);
                            main.post(new Runnable() { @Override public void run() { iv.setImageDrawable(d); } });
                            return;
                        }
                    }
                } catch (Throwable ignored) { }
            }
        }, "TjSsSolitaire");
        t.setDaemon(true);
        t.start();
    }
}
