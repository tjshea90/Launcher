package com.tj.launcher;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.usage.ExternalStorageStats;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.launcher3.R;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * BN1: the Storage page of the Settings look-alike: the donut header ("30.28 GB" over "Used of 32 GB" and the ring) and the seven category rows with their sizes and bars, in the stock order, without the stock
 * "Free up space" button and without the search icon (Tj, 2026-11-09). Source: AOSP 10 Settings (StorageDashboardFragment, storage_dashboard_fragment.xml, layout-sw360dp/storage_summary_donut.xml,
 * layout/storage_item.xml, StorageItemPreferenceController, StorageAsyncLoader; research/aosp10-settings/B_sound_storage.md sec. 2). The rules are in {@link SsStorageRules}, the ring in {@link SsDonutView}.
 *
 * <p>Real numbers, public APIs only. The header (total, free, used, percent) comes from StorageStatsManager and needs no permission. The seven sizes need the usage-access app op (the adb step
 * {@code cmd appops set com.tj.launcher GET_USAGE_STATS allow} that is part of the standard setup): StorageStatsManager.queryStatsForPackage of OTHER packages and queryExternalStatsForUser refuse the caller
 * without it. Without it every size stays blank (never a guess) and the header is still right. The scan runs on a worker thread; the views are touched on the main thread only.
 *
 * <p>Rows whose stock target page is not one of Tj's pages do nothing when tapped (his rule for the whole look-alike); the System row's one-sentence dialog is part of this page, not another page.
 */
final class SsPagesStorage {
    private SsPagesStorage() {}

    /** What the last read gave, one line for the tablet report (written after every read, like the Battery page's key). */
    static final String STATUS_KEY = "ss_storage_status";

    /**
     * AOSP's System row opens a dialog with one sentence (PrivateVolumeSettings.SystemInfoFragment: "System includes files used to run Android version 10", OK). It is no page, so it fits Tj's rule; the switch
     * is here so it can be turned off in one place if he does not want it.
     */
    static final boolean SYSTEM_ROW_OPENS_DIALOG = true;

    private static final long RELOAD_MS = 30_000;          // the per-app scan is not repeated more often than this (the stock page keeps its numbers for five minutes)
    private static final long SCAN_DEADLINE_MS = 45_000;   // an app scan that takes longer than this is dropped, never shown half done

    private static volatile Snapshot cached;               // the last COMPLETE read: a rotation or a quick return shows it at once instead of "Calculating..."
    private static volatile long cachedAt;                 // SystemClock.elapsedRealtime() of that read

    // ------------------------------------------------------------------ the page

    static void storage(final Activity a, LinearLayout list, List<SsPages.Live> live) {
        final Header head = new Header(a);
        list.addView(head.root, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final AlertDialog[] dialog = new AlertDialog[1];   // the open System dialog, so the page can close it when it is stopped
        final SsStorageRules.Row[] order = SsStorageRules.Row.values();
        final StorageRow[] rows = new StorageRow[order.length];
        for (int i = 0; i < order.length; i++) {
            rows[i] = new StorageRow(a, order[i], i == 0, dialog);
            list.addView(rows[i].view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        live.add(new Page(a, head, rows, dialog));
    }

    /**
     * The text under "Storage" on the Settings home page: "95% used - 1.57 GB free" (TopLevelStoragePreferenceController.refreshSummary: NumberFormat percent of used / total, Formatter.formatFileSize of free).
     * Two StorageStatsManager calls and no usage access needed; call it OFF the main thread (the free-space call may walk the cache accounting). Blank when the volume could not be read.
     */
    static String homeSummary(Context c) {
        try {
            long[] vol = volume(c.getApplicationContext());
            return SsStorageRules.homeSummary(vol[0], vol[1], Locale.getDefault());
        } catch (Throwable t) {
            return "";
        }
    }

    // ------------------------------------------------------------------ what one read gives

    /** One read of the system. {@code rows} is null while the per-app scan has not finished; a row that could not be read is {@link SsStorageRules#UNKNOWN}. */
    private static final class Snapshot {
        final long total, free;
        final long[] rows;

        Snapshot(long total, long free, long[] rows) { this.total = total; this.free = free; this.rows = rows; }
    }

    /** The app scan's answer: the two parts the seven sizes are built from, and why one of them is missing. */
    private static final class Scan {
        SsStorageRules.Tally apps;        // null = the per-app sizes could not be read completely
        SsStorageRules.External ext;      // null = the shared-storage numbers could not be read
        String why = "ok";                // ok | denied | timeout | cancelled | error
        int packages;
    }

    private interface Cancel { boolean now(); }

    /**
     * {total, free} of the internal storage, each {@link SsStorageRules#UNKNOWN} when it could not be read. The stock page uses StorageStatsManager.getTotalBytes (which already rounds data + system partition to the
     * marketing size, "32 GB") and getFreeBytes (usable space plus clearable cache) for the private volume; both need no permission. When the service refuses, StatFs of /data gives the same figures minus the cache.
     */
    private static long[] volume(Context app) {
        long total = SsStorageRules.UNKNOWN, free = SsStorageRules.UNKNOWN;
        try {
            StorageStatsManager ssm = (StorageStatsManager) app.getSystemService(Context.STORAGE_STATS_SERVICE);
            if (ssm != null) {
                try { long t = ssm.getTotalBytes(StorageManager.UUID_DEFAULT); if (t > 0) total = t; } catch (Throwable ignored) { }
                try { long f = ssm.getFreeBytes(StorageManager.UUID_DEFAULT); if (f >= 0) free = f; } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }
        if (total == SsStorageRules.UNKNOWN || free == SsStorageRules.UNKNOWN) {
            try {
                StatFs data = new StatFs(Environment.getDataDirectory().getPath());
                if (total == SsStorageRules.UNKNOWN) {
                    long system = new StatFs(Environment.getRootDirectory().getPath()).getTotalBytes();
                    total = SsStorageRules.marketingTotal(data.getTotalBytes(), system);   // StorageManager.getPrimaryStorageSize() = roundStorageSize(data + system)
                }
                if (free == SsStorageRules.UNKNOWN) free = data.getAvailableBytes();
            } catch (Throwable ignored) { }
        }
        return new long[] { total, free };
    }

    /**
     * The cache quota the stock page subtracts from an app's cache. It reads it per app through a hidden StorageStatsManager call; the public StorageManager.getCacheQuotaBytes(uuid) answers for the CALLER only.
     * On a build without a CacheQuotaService (AOSP and every Go tablet) every app has the same default, 64 MiB (StorageStatsService.DEFAULT_QUOTA), so one value for all apps is the same number; this is the
     * one place the page approximates when a build does set per-app quotas.
     */
    private static long cacheQuota(Context app) {
        try {
            StorageManager sm = (StorageManager) app.getSystemService(Context.STORAGE_SERVICE);
            long q = sm == null ? 0 : sm.getCacheQuotaBytes(StorageManager.UUID_DEFAULT);
            if (q > 0) return q;
        } catch (Throwable ignored) { }
        return SsStorageRules.DEFAULT_CACHE_QUOTA;
    }

    /** ApplicationInfo.category as the rules know it: only game / audio / video / image count, every other category falls through to the game flag and "Other apps", as in the stock switch. */
    private static int category(ApplicationInfo ai) {
        switch (ai.category) {
            case ApplicationInfo.CATEGORY_GAME: return SsStorageRules.CAT_GAME;
            case ApplicationInfo.CATEGORY_AUDIO: return SsStorageRules.CAT_AUDIO;
            case ApplicationInfo.CATEGORY_VIDEO: return SsStorageRules.CAT_VIDEO;
            case ApplicationInfo.CATEGORY_IMAGE: return SsStorageRules.CAT_IMAGE;
            default: return SsStorageRules.CAT_UNDEFINED;
        }
    }

    /**
     * StorageAsyncLoader.getStorageResultForUser + the external-stats call of StorageAsyncLoader.loadInBackground (settings10, l.97-150) for the current user and the internal volume: one
     * queryExternalStatsForUser, then queryStatsForPackage for every installed application (getInstalledApplications(0), the same list the stock asks for). Both need usage access: the external call is made first
     * and a SecurityException ends the scan at once (every package call would be refused the same way). A package that vanished during the scan is skipped (it really is gone); any other failure drops the whole
     * per-app result instead of showing a sum with a hole in it. Runs on a worker thread and obeys {@code deadline} (elapsedRealtime) and {@code cancel}.
     */
    private static Scan scan(Context app, long deadline, Cancel cancel) {
        final Scan s = new Scan();
        final StorageStatsManager ssm;
        try { ssm = (StorageStatsManager) app.getSystemService(Context.STORAGE_STATS_SERVICE); } catch (Throwable t) { s.why = "error"; return s; }
        if (ssm == null) { s.why = "error"; return s; }
        final UserHandle me = Process.myUserHandle();
        try {
            ExternalStorageStats e = ssm.queryExternalStatsForUser(StorageManager.UUID_DEFAULT, me);
            s.ext = new SsStorageRules.External(e.getTotalBytes(), e.getAudioBytes(), e.getVideoBytes(), e.getImageBytes(), e.getAppBytes());
        } catch (SecurityException e) {
            s.why = "denied";
            return s;
        } catch (Throwable e) {
            s.why = "error";   // the shared-storage part is missing; the per-app part below does not depend on it
        }
        final List<ApplicationInfo> installed;
        try { installed = app.getPackageManager().getInstalledApplications(0); } catch (Throwable t) { s.why = "error"; return s; }
        final long quota = cacheQuota(app);
        final SsStorageRules.Tally tally = new SsStorageRules.Tally();
        final UUID uuid = StorageManager.UUID_DEFAULT;
        for (ApplicationInfo ai : installed) {
            if (cancel.now()) { s.why = "cancelled"; return s; }   // deadline check FIRST in every pass: a loop that cannot end must not run for ever (project rule)
            if (SystemClock.elapsedRealtime() > deadline) { s.why = "timeout"; return s; }
            try {
                StorageStats st = ssm.queryStatsForPackage(uuid, ai.packageName, me);
                tally.addApp(ai.packageName, category(ai), (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0, st.getDataBytes(), st.getCacheBytes(), quota, st.getAppBytes());
                s.packages++;
            } catch (PackageManager.NameNotFoundException e) {
                // removed while we scanned: not installed any more, so it is rightly not counted
            } catch (SecurityException e) {
                s.why = "denied";
                return s;
            } catch (IOException | RuntimeException e) {
                s.why = "error";
                return s;
            }
        }
        s.apps = tally;
        return s;
    }

    // ------------------------------------------------------------------ the views

    /** The header: text block (72 dp / 32 dp / 16 dp / 32 dp padding) with the big number and "Used of 32 GB", the 112 dp donut at its end. Without the stock button the block is 176 px high at 170 dpi. */
    private static final class Header {
        final LinearLayout root;
        private final TextView title, summary;
        private final SsDonutView donut;

        Header(Context c) {
            root = new LinearLayout(c);
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);

            final LinearLayout col = new LinearLayout(c);   // storage_summary_donut.xml: weight 1, vertical, centred; paddingStart = preference_no_icon_padding_start (72 dp)
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_VERTICAL);
            col.setPaddingRelative(SsRows.dp(c, 72), SsRows.dp(c, 32), SsRows.dp(c, 16), SsRows.dp(c, 32));
            root.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            title = new TextView(c);
            final int display1 = SsRows.sysStyle("TextAppearance.DeviceDefault.Display1");   // the platform's style, private in the SDK; the explicit size and colour below hold when a build has none
            if (display1 != 0) title.setTextAppearance(display1);
            title.setTextColor(SsRows.themeColors(c, android.R.attr.textColorSecondary, 0x8A000000));
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, spPx(c, 36));   // android:textSize="36sp" reaches the view as whole pixels (46 at 170 dpi and font scale 1.2): its line is 62 px
            title.setTypeface(SsDonutView.headlineTypeface());
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);   // the stock text marquees; a number this short never overflows, so the two look the same
            col.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            summary = new TextView(c);
            final int body1 = SsRows.sysStyle("TextAppearance.DeviceDefault.Body1");
            if (body1 != 0) summary.setTextAppearance(body1);
            summary.setTextColor(SsRows.themeColors(c, android.R.attr.textColorPrimary, 0xDE000000));
            summary.setTextSize(TypedValue.COMPLEX_UNIT_PX, spPx(c, 14));
            summary.setMaxLines(10);
            summary.setPaddingRelative(0, 0, 0, SsRows.dp(c, 20));
            final LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.setMarginStart(SsRows.dp(c, 4));
            slp.setMarginEnd(SsRows.dp(c, 4));
            col.addView(summary, slp);

            donut = new SsDonutView(c);
            donut.setMinimumWidth(SsRows.dp(c, 58));
            final LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(SsRows.dp(c, 112), SsRows.dp(c, 112));
            dlp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            dlp.setMarginEnd(SsRows.dp(c, 32));
            root.addView(donut, dlp);
        }

        /** The volume numbers, or an empty header (no text, the track of the ring alone) while they are not known; the empty lines keep their height, so the page does not jump when the numbers arrive. */
        void show(long total, long free) {
            if (total > 0 && free >= 0) {
                long used = SsStorageRules.used(total, free);
                Locale l = Locale.getDefault();
                title.setText(largeNumber(SsStorageRules.formatBytes(used, false, l)));
                summary.setText(SsStorageRules.usedOf(total, l));
                donut.setPercentage(SsStorageRules.usedFraction(used, total));
            } else {
                title.setText("");
                summary.setText("");
                donut.setPercentage(Double.NaN);
            }
        }
    }

    /** An sp size the way an XML android:textSize reaches a TextView: whole pixels (round half up), not the unrounded float of setTextSize(SP, v). */
    private static int spPx(Context c, float sp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, c.getResources().getDisplayMetrics()));
    }

    /**
     * storage_size_large_alternate, "^1&lt;small&gt; &lt;font size="20"&gt;^2&lt;/font&gt;&lt;/small&gt;": the number at the title's size, then a space and the unit shrunk to 0.8 of it, the unit itself at an absolute 20 dip
     * (not scaled with the font size: 21 px at 170 dpi). The relative span is set first, so the absolute size wins over it for the unit; that is the order the stock markup produces and the order the Battery page
     * uses for its percent sign. (Whether the stock unit really ends up at 20 dip or at 0.8 of the title is the one thing to compare on Tj's tablet.)
     */
    static CharSequence largeNumber(SsStorageRules.Bytes b) {
        SpannableStringBuilder s = new SpannableStringBuilder(b.value);
        int gap = s.length();
        s.append(' ').append(b.units);
        s.setSpan(new RelativeSizeSpan(0.8f), gap, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new AbsoluteSizeSpan(20, true), gap + 1, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return s;
    }

    /** The drawable of a row (copies of the stock vectors: ic_photo_library, ic_media_stream, ic_videogame, ic_local_movies, ic_storage_apps, ic_folder, ic_system_update). */
    private static int iconFor(SsStorageRules.Row r) {
        switch (r) {
            case PHOTOS: return R.drawable.ss_ic_storage_photos;
            case MUSIC: return R.drawable.ss_ic_storage_music;
            case GAMES: return R.drawable.ss_ic_storage_games;
            case MOVIES: return R.drawable.ss_ic_storage_movies;
            case OTHER_APPS: return R.drawable.ss_ic_storage_apps;
            case FILES: return R.drawable.ss_ic_storage_files;
            default: return R.drawable.ss_ic_storage_system;
        }
    }

    /**
     * One category row, built like layout/storage_item.xml: a vertical root (min height 48 dp, 14 dp above and below, 16 dp at the end) holding a horizontal row (16 dp at the start) with the 56 dp icon frame
     * (24 dp icon, 4 dp above and below) and a RelativeLayout with the title, the size at its end and the 8 dp progress bar 12 dp below the title: 15 + 28 + 13 + 9 + 4 + 15 = 84 px at 170 dpi and font scale 1.2.
     * The bar is the platform's own horizontal ProgressBar (max 100), so its track, fill and square ends are the stock ones by construction. The row does nothing when tapped, except System (see
     * {@link #SYSTEM_ROW_OPENS_DIALOG}). {@code dividerAbove} lays a 1 px hairline over the first pixel row of the row: AOSP's DividerDecoration draws the divider ABOVE the Photos row on the row's own top line
     * (settings:allowDividerAbove), it takes no space of its own.
     */
    private static final class StorageRow {
        final View view;
        private final TextView size;
        private final ProgressBar bar;

        StorageRow(final Activity a, SsStorageRules.Row r, boolean dividerAbove, final AlertDialog[] dialog) {
            final Context c = a;
            final LinearLayout root = new LinearLayout(c);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_VERTICAL);
            root.setMinimumHeight(SsRows.dp(c, 48));
            root.setPaddingRelative(0, SsRows.dp(c, 14), SsRows.dp(c, 16), SsRows.dp(c, 14));
            root.setClipToPadding(false);

            final LinearLayout inner = new LinearLayout(c);
            inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(Gravity.CENTER_VERTICAL);
            inner.setPaddingRelative(SsRows.dp(c, 16), 0, 0, 0);
            root.addView(inner, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final LinearLayout frame = new LinearLayout(c);   // icon_frame: at least 56 dp wide, so the title of every row starts at 16 + 56 = 72 dp
            frame.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            frame.setMinimumWidth(SsRows.dp(c, 56));
            frame.setPadding(0, SsRows.dp(c, 4), 0, SsRows.dp(c, 4));
            final ImageView icon = new ImageView(c);
            icon.setImageDrawable(c.getDrawable(iconFor(r)));
            frame.addView(icon, new LinearLayout.LayoutParams(SsRows.dp(c, 24), SsRows.dp(c, 24)));
            inner.addView(frame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            final RelativeLayout rel = new RelativeLayout(c);
            inner.addView(rel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            final TextView title = new TextView(c);
            title.setId(View.generateViewId());
            SsRows.appearance(title, android.R.attr.textAppearanceListItem);   // DeviceDefault.Subhead: 16sp, textColorPrimary
            title.setText(r.label);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            rel.addView(title, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            size = new TextView(c);   // the summary: right aligned on the title's top line, 14sp, secondary colour
            SsRows.appearance(size, android.R.attr.textAppearanceListItemSecondary);
            size.setTextColor(SsRows.themeColors(c, android.R.attr.textColorSecondary, Color.GRAY));
            size.setMaxLines(10);
            size.setText(SsStorageRules.CALCULATING);   // StorageItemPreference's constructor: memory_calculating_size until the sizes arrive
            final RelativeLayout.LayoutParams slp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.addRule(RelativeLayout.ALIGN_PARENT_END);
            rel.addView(size, slp);

            bar = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);   // ?progressBarStyleHorizontal, as in storage_item.xml
            bar.setMax(100);
            final RelativeLayout.LayoutParams blp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, SsRows.dp(c, 8));
            blp.topMargin = SsRows.dp(c, 12);
            blp.bottomMargin = SsRows.dp(c, 4);
            blp.addRule(RelativeLayout.BELOW, title.getId());
            blp.addRule(RelativeLayout.ALIGN_START, title.getId());
            rel.addView(bar, blp);

            if (r == SsStorageRules.Row.SYSTEM && SYSTEM_ROW_OPENS_DIALOG) {
                TypedValue tv = new TypedValue();
                if (c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) root.setBackgroundResource(tv.resourceId);
                root.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try {
                            // PrivateVolumeSettings.SystemInfoFragment: no title, the message, one OK button
                            dialog[0] = new AlertDialog.Builder(a).setMessage(SsStorageRules.systemInfo(Build.VERSION.RELEASE)).setPositiveButton(android.R.string.ok, null).show();
                        } catch (Throwable ignored) { }
                    }
                });
            }

            if (dividerAbove) {
                final FrameLayout framed = new FrameLayout(c);
                framed.addView(root, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                final View divider = SsRows.divider(c);
                divider.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.TOP));
                framed.addView(divider);
                view = framed;
            } else {
                view = root;
            }
        }

        /** {@code calculating}: the scan is still running (text "Calculating...", empty bar). Otherwise the size as the stock formats it (blank when unknown) and its bar. */
        void show(boolean calculating, long bytes, long total) {
            if (calculating) {
                size.setText(SsStorageRules.CALCULATING);
                bar.setProgress(0);
                return;
            }
            size.setText(SsStorageRules.formatRowSize(bytes, Locale.getDefault()));
            bar.setProgress(SsStorageRules.progress(bytes, total));
        }
    }

    // ------------------------------------------------------------------ the live part

    /**
     * Follows the activity (start / stop). Every start shows what is known at once (the last complete read, else an empty header and "Calculating..." rows), re-reads the volume in the background and, when the
     * last complete scan is older than {@link #RELOAD_MS}, scans the apps again. A worker belongs to ONE start: stop() bumps {@code generation}, the worker stops at its next package and its result is dropped, and a
     * render that was already posted is removed.
     */
    private static final class Page implements SsPages.Live {
        private final Activity a;
        private final Header head;
        private final StorageRow[] rows;
        private final AlertDialog[] dialog;
        private final Handler main = new Handler(Looper.getMainLooper());
        private volatile int generation;
        private boolean started;

        Page(Activity a, Header head, StorageRow[] rows, AlertDialog[] dialog) {
            this.a = a; this.head = head; this.rows = rows; this.dialog = dialog;
        }

        @Override public void start() {
            started = true;
            generation++;
            styleToolbar();
            final Snapshot have = cached;
            apply(have != null ? have : new Snapshot(SsStorageRules.UNKNOWN, SsStorageRules.UNKNOWN, null));
            final boolean scanAgain = have == null || SystemClock.elapsedRealtime() - cachedAt >= RELOAD_MS;
            load(scanAgain, have);
        }

        @Override public void stop() {
            started = false;
            generation++;                           // the running worker stops at its next package, its result is dropped
            main.removeCallbacksAndMessages(null);  // a render that was posted but has not run yet must not touch a stopped page
            final AlertDialog d = dialog[0];
            dialog[0] = null;
            if (d != null) { try { d.dismiss(); } catch (Throwable ignored) { } }   // a dialog must not outlive its activity (rotation)
        }

        /**
         * StorageDashboardFragment.onViewCreated runs EntityHeaderController.styleActionBar: the action bar becomes colorPrimaryDark (#E0E0E0, the status bar's gray) with no elevation, which is the "gray header" of
         * Tj's screenshot. SsPageActivity styles only the Battery page, so the Storage page does it itself.
         */
        private void styleToolbar() {
            try {
                ActionBar bar = a.getActionBar();
                if (bar == null) return;
                bar.setBackgroundDrawable(new ColorDrawable(SsRows.themeColor(a, android.R.attr.colorPrimaryDark, 0xFFE0E0E0)));
                bar.setElevation(0);
            } catch (Throwable ignored) { }
        }

        private void apply(Snapshot s) {
            head.show(s.total, s.free);
            final boolean calculating = s.rows == null;
            for (int i = 0; i < rows.length; i++) rows[i].show(calculating, calculating ? SsStorageRules.UNKNOWN : s.rows[i], s.total);
        }

        private void post(final int gen, final Snapshot s) {
            main.post(new Runnable() {
                @Override public void run() { if (started && gen == generation) apply(s); }
            });
        }

        /** The header first (cheap), then, when asked, the per-app scan; {@code previous} (the last complete read) keeps its rows on screen until the new numbers replace them. */
        private void load(final boolean scanApps, final Snapshot previous) {
            final int gen = generation;
            final Context app = a.getApplicationContext();
            Thread t = new Thread(new Runnable() {
                @Override public void run() {
                    final long t0 = SystemClock.elapsedRealtime();
                    final long[] vol = volume(app);
                    final long[] shown = previous == null ? null : previous.rows;
                    post(gen, new Snapshot(vol[0], vol[1], shown));
                    if (!scanApps) {
                        report(app, vol, "cached", 0, shown, SystemClock.elapsedRealtime() - t0);
                        return;
                    }
                    final Scan s = scan(app, t0 + SCAN_DEADLINE_MS, new Cancel() { @Override public boolean now() { return gen != generation; } });
                    final long used = vol[0] > 0 && vol[1] >= 0 ? SsStorageRules.used(vol[0], vol[1]) : SsStorageRules.UNKNOWN;
                    final long[] sizes = SsStorageRules.rowSizes(s.apps, s.ext, used);
                    final Snapshot full = new Snapshot(vol[0], vol[1], sizes);
                    if (s.apps != null && s.ext != null && vol[0] > 0 && gen == generation) {   // only a complete read is kept for the next start
                        cached = full;
                        cachedAt = SystemClock.elapsedRealtime();
                    }
                    post(gen, full);
                    report(app, vol, s.why, s.packages, sizes, SystemClock.elapsedRealtime() - t0);
                }
            }, "TjSsStor");
            t.setDaemon(true);
            t.start();
        }
    }

    /** One line for the tablet report: when, the volume numbers, how the scan ended, how many packages were counted, how many of the seven rows have a number, and how long it took. */
    private static void report(Context app, long[] vol, String scanState, int packages, long[] rows, long ms) {
        try {
            int known = 0;
            if (rows != null) for (long r : rows) if (SsStorageRules.isKnown(r)) known++;
            Features.setStringAsync(app, STATUS_KEY, new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())
                    + "  total=" + vol[0] + " free=" + vol[1] + " scan=" + scanState + " packages=" + packages + " rows=" + known + "/" + SsStorageRules.Row.values().length + " ms=" + ms);
        } catch (Throwable ignored) { }
    }
}
