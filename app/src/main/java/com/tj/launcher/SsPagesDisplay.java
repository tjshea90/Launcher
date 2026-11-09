package com.tj.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import java.text.NumberFormat;
import java.util.List;

/**
 * BN1: the Display page with Tj's five rows in his order (Brightness level, Screen timeout, Auto-rotate screen, Font size, Display size; no search, no other row). Every control changes the real system setting
 * (the launcher holds WRITE_SECURE_SETTINGS through the adb grant, which also lets it write Settings.System): brightness in the HLG curve of AOSP, the timeout from AOSP's list in a single-choice dialog,
 * auto-rotate as AOSP's RotationPolicy does it, Font size and Display size on their own pages ({@link SsPagesDisplay} only builds the list). Rules: {@link SsDisplayRules}.
 */
final class SsPagesDisplay {
    private SsPagesDisplay() {}

    static void display(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        final View brightness = SsRows.row(a, null, "Brightness level", brightnessSummary(a), new View.OnClickListener() {
            @Override public void onClick(View v) { openBrightness(a); }
        });
        final View timeout = SsRows.row(a, null, "Screen timeout", timeoutSummary(a), new View.OnClickListener() {
            @Override public void onClick(View v) { openTimeout(a); }
        });
        final Switch rotate = new Switch(a);
        rotate.setChecked(autoRotate(a));
        final View rotation = SsRows.switchRow(a, "Auto-rotate screen", null, rotate, new SsRows.Toggle() {
            @Override public void onToggle(boolean checked) { setAutoRotate(a, checked); }
        });
        final View font = SsRows.row(a, null, "Font size", fontSummary(a), new View.OnClickListener() {
            @Override public void onClick(View v) { a.startActivity(SsPageActivity.intent(a, SsPage.FONT_SIZE)); }
        });
        final View size = SsRows.row(a, null, "Display size", sizeSummary(a), new View.OnClickListener() {
            @Override public void onClick(View v) { a.startActivity(SsPageActivity.intent(a, SsPage.DISPLAY_SIZE)); }
        });
        list.addView(brightness);
        list.addView(timeout);
        list.addView(rotation);
        list.addView(font);
        list.addView(size);

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                SsRows.setSummary(brightness, brightnessSummary(a));
                SsRows.setSummary(timeout, timeoutSummary(a));
                SsRows.setSummary(font, fontSummary(a));
                SsRows.setSummary(size, sizeSummary(a));
                boolean on = autoRotate(a);
                if (rotate.isChecked() != on) rotate.setChecked(on);
            }
        };
        live.add(new Watcher(a, refresh, Settings.System.SCREEN_BRIGHTNESS, Settings.System.SCREEN_OFF_TIMEOUT, Settings.System.ACCELEROMETER_ROTATION, Settings.System.FONT_SCALE));
    }

    /** Watches system settings while the page is visible (a ContentObserver per setting, removed when the page stops). */
    static final class Watcher implements SsPages.Live {
        private final Activity a;
        private final Runnable onChange;
        private final String[] names;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final ContentObserver obs = new ContentObserver(main) {
            @Override public void onChange(boolean selfChange) { onChange.run(); }
        };
        Watcher(Activity a, Runnable onChange, String... names) { this.a = a; this.onChange = onChange; this.names = names; }
        @Override public void start() {
            for (String n : names) { try { Uri u = Settings.System.getUriFor(n); if (u != null) a.getContentResolver().registerContentObserver(u, false, obs); } catch (Throwable ignored) { } }
            onChange.run();
        }
        @Override public void stop() { try { a.getContentResolver().unregisterContentObserver(obs); } catch (Throwable ignored) { } }
    }

    // ------------------------------------------------------------------ readers
    static int rawBrightness(Context c) { return Settings.System.getInt(c.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, SsDisplayRules.BRIGHTNESS_MIN); }

    static String brightnessSummary(Context c) {
        return NumberFormat.getPercentInstance().format(SsDisplayRules.brightnessFraction(rawBrightness(c), SsDisplayRules.BRIGHTNESS_MIN, SsDisplayRules.BRIGHTNESS_MAX));
    }

    static long timeoutMs(Context c) { return Settings.System.getLong(c.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 30_000L); }

    static String timeoutSummary(Context c) { return SsDisplayRules.timeoutSummary(timeoutMs(c)); }

    static boolean autoRotate(Context c) { return Settings.System.getInt(c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) != 0; }

    static float fontScale(Context c) { return Settings.System.getFloat(c.getContentResolver(), Settings.System.FONT_SCALE, 1.0f); }

    static String fontSummary(Context c) { return SsDisplayRules.FONT_LABELS[SsDisplayRules.fontIndex(fontScale(c))]; }

    /** The Display size stops for this screen (the default density is the device's own stable one). */
    static SsDisplayRules.Density densityStops(Activity a) {
        DisplayMetrics real = new DisplayMetrics();
        a.getWindowManager().getDefaultDisplay().getRealMetrics(real);
        return SsDisplayRules.densityStops(DisplayMetrics.DENSITY_DEVICE_STABLE, real.widthPixels, real.heightPixels, real.densityDpi);
    }

    static String sizeSummary(Activity a) {
        try { SsDisplayRules.Density d = densityStops(a); return d.labels[d.index]; } catch (Throwable t) { return "Default"; }
    }

    // ------------------------------------------------------------------ writers
    /** A system-setting write that never crashes the page: without the permission the user is told, the page keeps showing the real value. */
    static boolean write(Context c, Runnable w) {
        try { w.run(); return true; }
        catch (SecurityException e) { Toast.makeText(c, "This setting can't be changed from here.", Toast.LENGTH_SHORT).show(); return false; }
        catch (RuntimeException e) { return false; }
    }

    static void setAutoRotate(final Activity a, final boolean on) {
        write(a, new Runnable() {
            @Override public void run() {
                if (on) { Settings.System.putInt(a.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1); return; }
                // AOSP RotationPolicy.setRotationLock(true) freezes the CURRENT rotation: a bare ACCELEROMETER_ROTATION = 0 would lock at whatever USER_ROTATION held
                int rotation = a.getWindowManager().getDefaultDisplay().getRotation();
                Settings.System.putInt(a.getContentResolver(), Settings.System.USER_ROTATION, rotation);
                Settings.System.putInt(a.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            }
        });
    }

    // ------------------------------------------------------------------ dialogs
    /** The system's own brightness dialog (SystemUI, the same one the real Settings opens); a small slider of ours when this build has none. */
    static void openBrightness(final Activity a) {
        try { a.startActivity(new Intent("com.android.intent.action.SHOW_BRIGHTNESS_DIALOG")); return; } catch (Throwable ignored) { }
        final SeekBar bar = new SeekBar(a);
        bar.setMax(SsDisplayRules.GAMMA_MAX);
        bar.setProgress(SsDisplayRules.toGamma(rawBrightness(a), SsDisplayRules.BRIGHTNESS_MIN, SsDisplayRules.BRIGHTNESS_MAX));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, final int p, boolean fromUser) {
                if (!fromUser) return;
                write(a, new Runnable() { @Override public void run() {
                    Settings.System.putInt(a.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, SsDisplayRules.fromGamma(p, SsDisplayRules.BRIGHTNESS_MIN, SsDisplayRules.BRIGHTNESS_MAX));
                } });
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        new AlertDialog.Builder(a).setTitle("Brightness level").setView(bar).setPositiveButton(android.R.string.ok, null).show();
    }

    /** AOSP's TimeoutListPreference dialog: a single-choice list; a tap stores the value and closes it (no OK button), Cancel closes it. */
    static void openTimeout(final Activity a) {
        int checked = SsDisplayRules.timeoutIndex(timeoutMs(a));
        new AlertDialog.Builder(a).setTitle("Screen timeout")
                .setSingleChoiceItems(SsDisplayRules.TIMEOUT_LABELS, checked, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, final int which) {
                        write(a, new Runnable() { @Override public void run() {
                            Settings.System.putInt(a.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, SsDisplayRules.TIMEOUT_VALUES[which]);
                        } });
                        d.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
