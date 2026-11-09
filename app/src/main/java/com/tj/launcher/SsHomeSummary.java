package com.tj.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 * BN1: the live summaries of the look-alike's home page: Battery ("69% - Should last until about 9:15 PM", the percentage and the header sentence of the Battery page) and Storage ("95% used - 1.57 GB free").
 * The activity starts it when the home page is visible and stops it when it is hidden. The Battery estimate comes from the same background read as the Battery page ({@link SsBatteryStats}); the sticky
 * battery broadcast gives the rest, so without the usage-stats grant the line still shows the percentage and the status ("Not charging"), never an empty row.
 */
final class SsHomeSummary implements SsPages.Live {
    private static final long MIN_RELOAD_MS = 60_000L;

    private final Activity a;
    private final View batteryRow, storageRow;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SsBatteryText.TimeOfDay clock;
    private SsBatteryStats.Data data = new SsBatteryStats.Data();
    private Intent last;
    private boolean started, loading;
    private long lastLoad;
    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context ctx, Intent i) { last = i; renderBattery(); maybeReload(false); }
    };

    SsHomeSummary(Activity a, View batteryRow, View storageRow) {
        this.a = a; this.batteryRow = batteryRow; this.storageRow = storageRow;
        final DateFormat f = android.text.format.DateFormat.getTimeFormat(a);
        this.clock = new SsBatteryText.TimeOfDay() { @Override public String format(long ms) { return f.format(new Date(ms)); } };
    }

    @Override public void start() {
        started = true;
        try { last = a.registerReceiver(rx, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); } catch (Throwable ignored) { }
        renderBattery();
        maybeReload(true);
        loadStorage();
    }

    @Override public void stop() {
        started = false;
        try { a.unregisterReceiver(rx); } catch (Throwable ignored) { }
    }

    /** The percentage and the header sentence, as one line ("69% - Not charging"). Pure function of the battery intent, the estimate and the clock. */
    static String batteryLine(Intent i, SsBatteryStats.Data d, long nowMs, SsBatteryText.TimeOfDay clock) {
        if (i == null) return "";
        int scale = Math.max(1, i.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
        int level = Math.round(100f * i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / scale);
        int plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        String pct = NumberFormat.getPercentInstance().format(level / 100.0);
        return pct + " - " + SsBatteryText.header(plugged, status, d.dischargeMs, d.chargeMs, nowMs, clock);
    }

    private void renderBattery() {
        if (!started || batteryRow == null) return;
        SsRows.setSummary(batteryRow, batteryLine(last, data, System.currentTimeMillis(), clock));
    }

    private void maybeReload(boolean force) {
        long now = System.currentTimeMillis();
        if (loading || (!force && now - lastLoad < MIN_RELOAD_MS)) return;
        loading = true;
        lastLoad = now;
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                final SsBatteryStats.Data d = SsBatteryStats.read(6000);
                main.post(new Runnable() { @Override public void run() { loading = false; if (d.any()) data = d; if (started) renderBattery(); } });
            }
        }, "TjSsHomeBatt");
        t.setDaemon(true);
        t.start();
    }

    private void loadStorage() {
        if (storageRow == null) return;
        // M3: wired to SsPagesStorage.homeSummary once the Storage page exists
    }
}
