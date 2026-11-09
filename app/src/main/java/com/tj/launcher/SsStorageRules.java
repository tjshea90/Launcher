package com.tj.launcher;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * BN1: the pure rules of the Storage page of the Settings look-alike (no Android imports; JVM-tested by SsStorageRulesTest). Everything here is a plain function of its arguments: the readers of the
 * system values live in {@link SsPagesStorage}, the drawing in {@link SsDonutView}. Sources are AOSP 10 (android-10.0.0_r47): Settings (research/aosp10-settings/B_sound_storage.md sec. 2) and the
 * framework classes the Settings code calls (android.text.format.Formatter, android.os.FileUtils, StorageStatsService; read from the source on 2026-11-09, quoted in the comments below).
 * Unknown numbers are never invented: every function that needs one answers {@link #UNKNOWN} or "".
 */
final class SsStorageRules {
    private SsStorageRules() {}

    // ------------------------------------------------------------------ the texts (AOSP Settings strings.xml, English)

    /** Preference title and toolbar title: storage_settings (strings.xml l.3092). */
    static final String TITLE = "Storage";
    /** What a row shows while its number is being measured: memory_calculating_size (l.3184; StorageItemPreference sets it in its constructor). */
    static final String CALCULATING = "Calculating…";
    /** The second line inside the donut: storage_percent_full (l.10499). */
    static final String DONUT_LABEL = "used";

    /** Header summary: storage_volume_total "Used of %1$s" (l.10497), the total in {@link #formatShortFileSize}. */
    static String usedOf(long totalBytes, Locale locale) {
        return "Used of " + formatShortFileSize(totalBytes, locale);
    }

    /** The message of the System row's dialog: storage_detail_dialog_system (l.3405), the argument is Build.VERSION.RELEASE. */
    static String systemInfo(String androidRelease) {
        return "System includes files used to run Android version " + androidRelease;
    }

    /** Accessibility text of the donut: join_two_unrelated_items "%1$s, %2$s" (strings.xml l.4412) over the percentage and "used". */
    static String donutDescription(String percentString) {
        return percentString + ", " + DONUT_LABEL;
    }

    /**
     * The text the Settings HOME page shows under "Storage": storage_summary "%1$s used - %2$s free" (l.9503) with NumberFormat.getPercentInstance().format(used / total) and
     * Formatter.formatFileSize(free) (TopLevelStoragePreferenceController.refreshSummary). Blank when a number is unknown (total not above 0, free below 0).
     */
    static String homeSummary(long totalBytes, long freeBytes, Locale locale) {
        if (totalBytes <= 0 || freeBytes < 0) return "";
        double used = (double) used(totalBytes, freeBytes);   // AOSP: info.totalBytes - info.freeBytes, as a double
        return percent(used / totalBytes, locale) + " used - " + formatFileSize(freeBytes, locale) + " free";
    }

    // ------------------------------------------------------------------ the seven rows

    /** The rows in the stock order: storage_dashboard_fragment.xml android:order 2, 3, 4, 5, 6, 7 and 100 (the order the screenshot of Tj's tablet shows). */
    enum Row {
        PHOTOS("Photos & videos"),    // pref_photos_videos, storage_photos_videos
        MUSIC("Music & audio"),       // pref_music_audio, storage_music_audio
        GAMES("Games"),               // pref_games, storage_games
        MOVIES("Movie & TV apps"),    // pref_movies, storage_movies_tv
        OTHER_APPS("Other apps"),     // pref_other_apps, storage_other_apps
        FILES("Files"),               // pref_files, storage_files
        SYSTEM("System");             // pref_system, storage_detail_system

        /** The title of the row. */
        final String label;

        Row(String label) { this.label = label; }
    }

    /** "No number": a row whose inputs could not be read (never 0, never a guess). A real size may legitimately be negative in odd data, so the marker is not -1. */
    static final long UNKNOWN = Long.MIN_VALUE;

    static boolean isKnown(long v) { return v != UNKNOWN; }

    // ------------------------------------------------------------------ sizes as the stock shows them

    /** What Formatter.formatBytes (a hidden class BytesResult) hands back: the number text and the unit text. */
    static final class Bytes {
        final String value;
        final String units;

        Bytes(String value, String units) { this.value = value; this.units = units; }
    }

    private static final String[] UNITS = { "B", "kB", "MB", "GB", "TB", "PB" };   // core/res/res/values/strings.xml l.16-33: byteShort, kilobyteShort ... petabyteShort

    /**
     * android.text.format.Formatter.formatBytes(res, bytes, flags) of android-10.0.0_r47 with the SI flag (decimal: 1 kB = 1000 B; FLAG_IEC_UNITS absent), copied from the source: the value climbs a unit
     * while it is ABOVE 900 (so 900 MB stays "900 MB" and 901 MB is "0.90 GB"); the decimals: whole number for bytes or 100 and up, "%.2f" below 1, then "%.1f" (shorter) or "%.2f" from 1 to 10,
     * then "%.0f" (shorter) or "%.2f" from 10 to 100. The arithmetic is done in FLOAT on purpose, as the platform does (a 32 bit float and a double round a few sizes differently).
     * {@code shorter} is Formatter.FLAG_SHORTER (formatShortFileSize); the page's big number uses flags 0 = not shorter ("30.28"). Not replicated: the bidi marks the platform adds in a right-to-left locale.
     */
    static Bytes formatBytes(long sizeBytes, boolean shorter, Locale locale) {
        final boolean isNegative = sizeBytes < 0;
        float result = isNegative ? -sizeBytes : sizeBytes;
        int unit = 0;
        long mult = 1;
        while (unit < UNITS.length - 1 && result > 900) {   // the five "if (result > 900)" steps of the source, B -> kB -> MB -> GB -> TB -> PB
            unit++;
            mult *= 1000;
            result = result / 1000;
        }
        final String roundFormat;
        if (mult == 1 || result >= 100) roundFormat = "%.0f";
        else if (result < 1) roundFormat = "%.2f";
        else if (result < 10) roundFormat = shorter ? "%.1f" : "%.2f";
        else roundFormat = shorter ? "%.0f" : "%.2f";   // 10 <= result < 100
        if (isNegative) result = -result;
        return new Bytes(String.format(locale == null ? Locale.getDefault() : locale, roundFormat, result), UNITS[unit]);
    }

    /** Formatter.formatFileSize(context, bytes) = formatBytes with FLAG_SI_UNITS, then fileSizeSuffix "%1$s %2$s": "1.57 GB" (the Settings home summary, "95% used - 1.57 GB free"). */
    static String formatFileSize(long sizeBytes, Locale locale) {
        Bytes b = formatBytes(sizeBytes, false, locale);
        return b.value + " " + b.units;
    }

    /** Formatter.formatShortFileSize(context, bytes) = FLAG_SI_UNITS | FLAG_SHORTER: "32 GB" (the header's "Used of 32 GB"). */
    static String formatShortFileSize(long sizeBytes, Locale locale) {
        Bytes b = formatBytes(sizeBytes, true, locale);
        return b.value + " " + b.units;
    }

    private static final long GIGABYTE_IN_BYTES = 1_000_000_000L;   // FileSizeFormatter.GIGABYTE_IN_BYTES (SI, not the 1024 based TrafficStats constant)

    /**
     * What a category row shows on the right: com.android.settings.utils.FileSizeFormatter.formatFileSize(ctx, size, gigabyteShort, GIGABYTE_IN_BYTES) (settings10/src/.../utils/FileSizeFormatter.java l.79-109):
     * ALWAYS gigabytes, "%.2f" below 1 GB, "%.1f" below 10 GB, "%.0f" from 10 GB, float arithmetic, then fileSizeSuffix. Tj's screenshot: "3.3 GB", "0.54 GB", "3.0 GB", "0.00 GB", "3.7 GB", "15 GB", "4.3 GB".
     * Blank for an unknown size.
     */
    static String formatRowSize(long sizeBytes, Locale locale) {
        if (!isKnown(sizeBytes)) return "";
        final boolean isNegative = sizeBytes < 0;
        float result = isNegative ? -sizeBytes : sizeBytes;
        result = result / GIGABYTE_IN_BYTES;
        final String roundFormat;
        if (result < 1) roundFormat = "%.2f";
        else if (result < 10) roundFormat = "%.1f";
        else roundFormat = "%.0f";
        if (isNegative) result = -result;
        return String.format(locale == null ? Locale.getDefault() : locale, roundFormat, result) + " GB";
    }

    /** NumberFormat.getPercentInstance().format(fraction): "95%" (com.android.settingslib.Utils.formatPercentage(double), which the donut and the home summary use; half-even, no decimals). */
    static String percent(double fraction, Locale locale) {
        return NumberFormat.getPercentInstance(locale == null ? Locale.getDefault() : locale).format(fraction);
    }

    /** Used bytes as the page computes them: total minus free (StorageDashboardFragment.onReceivedSizes), never below 0. */
    static long used(long totalBytes, long freeBytes) { return Math.max(0L, totalBytes - freeBytes); }

    /** StorageSummaryDonutPreference.setPercent: used / (double) total; NaN (nothing to draw) when the total is not above 0. */
    static double usedFraction(long usedBytes, long totalBytes) {
        return totalBytes <= 0 ? Double.NaN : usedBytes / (double) totalBytes;
    }

    // ------------------------------------------------------------------ the total ("32 GB")

    /**
     * android.os.FileUtils.roundStorageSize (@hide, android-10.0.0_r47): the smallest value 1, 2, 4, ... 512 times a power of 1000 that is not below {@code size}; this is why the page says "32 GB" for a
     * partition of 28.4 GB. (StorageStatsService.getTotalBytes applies it to StorageManager.getPrimaryStorageSize(), which is itself this function over data + system partition.)
     */
    static long roundStorageSize(long size) {
        long val = 1;
        long pow = 1;
        while ((val * pow) < size) {
            val <<= 1;
            if (val > 512) {
                val = 1;
                pow *= 1000;
            }
        }
        return val * pow;
    }

    /**
     * The total the page shows when StorageStatsManager.getTotalBytes is not available: StorageManager.getPrimaryStorageSize() = roundStorageSize(data partition + system partition) (@hide; the second
     * rounding in StorageStatsService.getTotalBytes changes nothing, the function is idempotent).
     */
    static long marketingTotal(long dataPartitionBytes, long systemPartitionBytes) {
        return roundStorageSize(dataPartitionBytes + systemPartitionBytes);
    }

    // ------------------------------------------------------------------ the bars

    /** StorageItemPreference.setStorageSize (l.49-62): progress = total == 0 ? 0 : (int) (size * 100 / total) (long arithmetic, truncated, ProgressBar max 100). Unknown sizes and totals show an empty bar. */
    static int progress(long sizeBytes, long totalBytes) {
        if (!isKnown(sizeBytes) || totalBytes <= 0) return 0;
        return (int) (sizeBytes * 100 / totalBytes);
    }

    /**
     * The drawable level the platform gives the bar for {@code progress} of 100: ProgressBar.setVisualProgress sets (int) (scale * MAX_LEVEL) with scale = (progress - min) / (float) range, all in float, so
     * 39 gives 3899, not 3900 (the same for 53, 59 and 78). A plain setProgress does not animate, the bar is at this level at once.
     */
    static int barLevel(int progress) {
        int p = Math.max(0, Math.min(100, progress));
        return (int) ((float) p / (float) 100 * 10000);
    }

    /**
     * How many pixels of a bar {@code widthPx} wide the teal part covers for {@code progress} (0..100): the platform's progress_horizontal_material draws the progress layer as a ScaleDrawable (scaleWidth 100%),
     * and ScaleDrawable.onBoundsChange cuts (int) (w * (10000 - level) * 1.0f / 10000) off the width. Checked against the seven bars of Tj's screenshot (506 px wide): progress 10, 1, 9, 0, 11, 48, 13 cover 51, 6,
     * 46, 0, 56, 243, 66 px. For the unit test and the emulator scenario's expected values; the page itself draws with the platform's own ProgressBar, so it is identical by construction.
     */
    static int barFillPx(int widthPx, int progress) {
        int level = barLevel(progress);
        return widthPx - (int) (widthPx * (10000 - level) * 1.0f / 10000);
    }

    // ------------------------------------------------------------------ the donut text

    /**
     * Where the percent sign sits in the formatted percentage, as [start, end) (DonutView.getPercentageStringSpannable): the shrunken part. When the sign is not found the WHOLE text counts (the source
     * "falls back to no small string" by shrinking everything).
     */
    static int[] percentSignRange(String percentString, String sign) {
        int start = percentString.indexOf(sign);
        int end = start + sign.length();
        if (start < 0) {
            start = 0;
            end = percentString.length();
        }
        return new int[] { start, end };
    }

    // ------------------------------------------------------------------ what the apps and the shared storage add up to

    /** ApplicationInfo.category values (API 26, android.content.pm.ApplicationInfo.CATEGORY_*); the page maps the real constants to these. */
    static final int CAT_UNDEFINED = -1;
    static final int CAT_GAME = 0;
    static final int CAT_AUDIO = 1;
    static final int CAT_VIDEO = 2;
    static final int CAT_IMAGE = 3;

    /** StorageStatsService.DEFAULT_QUOTA (64 MiB, android-10.0.0_r47): the cache quota of an app the system has set none for (every app on a device without a CacheQuotaService). */
    static final long DEFAULT_CACHE_QUOTA = 64L * 1024 * 1024;

    /** The floor of the System row: TrafficStats.GB_IN_BYTES (@hide, 1024 ^ 3, android-10.0.0_r47), so the row never shows less than "1.1 GB". */
    static final long SYSTEM_FLOOR = 1024L * 1024 * 1024;

    /** StorageAsyncLoader.AppsStorageResult for ONE user: the bytes the installed apps are blamed for, by category (settings10/src/.../storage/StorageAsyncLoader.java l.97-150). */
    static final class Tally {
        long games;
        long music;
        long video;
        long photos;
        long other;
        /** How many packages were added (diagnostics only). */
        int count;
        private final Set<String> seen = new HashSet<>();

        /**
         * One installed app, as StorageAsyncLoader.getStorageResultForUser counts it: the data bytes, less the cache above the app's quota (the excess counts as free space, see
         * StorageStatsService.getFreeBytes), plus the code bytes once per package name; then by category: game, audio, video (Movie &amp; TV apps), image, else the deprecated FLAG_IS_GAME means a game
         * and everything else is "Other apps".
         */
        void addApp(String packageName, int category, boolean gameFlag, long dataBytes, long cacheBytes, long cacheQuotaBytes, long codeBytes) {
            long blamed = dataBytes;
            if (cacheQuotaBytes < cacheBytes) blamed = blamed - cacheBytes + cacheQuotaBytes;
            if (seen.add(packageName)) blamed += codeBytes;
            switch (category) {
                case CAT_GAME: games += blamed; break;
                case CAT_AUDIO: music += blamed; break;
                case CAT_VIDEO: video += blamed; break;
                case CAT_IMAGE: photos += blamed; break;
                default:
                    if (gameFlag) games += blamed; else other += blamed;
            }
            count++;
        }

        long attributed() { return games + music + video + photos + other; }
    }

    /** StorageStatsSource.ExternalStorageStats: what StorageStatsManager.queryExternalStatsForUser reports about the shared (emulated) storage of the user. */
    static final class External {
        final long total;
        final long audio;
        final long video;
        final long image;
        final long app;

        External(long total, long audio, long video, long image, long app) {
            this.total = total;
            this.audio = audio;
            this.video = video;
            this.image = image;
            this.app = app;
        }
    }

    /**
     * The seven row sizes in {@link Row} order, from StorageItemPreferenceController.onLoadFinished (l.249-290): Photos &amp; videos = photo apps + image + video files; Music &amp; audio = music apps +
     * audio files; Games, Movie &amp; TV apps and Other apps = the app sums; Files = shared storage total minus audio, video, image and app bytes; System = what is left of the used bytes after every
     * attributed byte (apps + shared storage without the app part), at least {@link #SYSTEM_FLOOR}. {@code apps} or {@code ext} null means that part could not be read: every row that needs it is
     * {@link #UNKNOWN}; {@code usedBytes} {@link #UNKNOWN} means the volume numbers are missing and System stays unknown.
     */
    static long[] rowSizes(Tally apps, External ext, long usedBytes) {
        long[] r = new long[Row.values().length];
        Arrays.fill(r, UNKNOWN);
        if (apps != null) {
            r[Row.GAMES.ordinal()] = apps.games;
            r[Row.MOVIES.ordinal()] = apps.video;
            r[Row.OTHER_APPS.ordinal()] = apps.other;
        }
        if (ext != null) r[Row.FILES.ordinal()] = ext.total - ext.audio - ext.video - ext.image - ext.app;
        if (apps != null && ext != null) {
            r[Row.PHOTOS.ordinal()] = apps.photos + ext.image + ext.video;
            r[Row.MUSIC.ordinal()] = apps.music + ext.audio;
            if (isKnown(usedBytes)) {
                long attributed = apps.attributed() + (ext.total - ext.app);
                r[Row.SYSTEM.ordinal()] = Math.max(SYSTEM_FLOOR, usedBytes - attributed);
            }
        }
        return r;
    }
}
