package com.tj.launcher;

/**
 * BN1: the pure rules of the Display page and its sub pages (JVM-tested: SsDisplayRulesTest). AOSP 10 sources: SettingsLib BrightnessUtils (the HLG curve), Settings arrays.xml (screen timeouts, font sizes),
 * SettingsLib DisplayDensityUtils (the display-size stops); research/aosp10-settings/A_battery_display.md sec. 4. Plain Java: no Android classes, so they run on the JVM.
 */
final class SsDisplayRules {
    private SsDisplayRules() {}

    // ------------------------------------------------------------------ brightness
    static final int BRIGHTNESS_MIN = 10, BRIGHTNESS_MAX = 255, BRIGHTNESS_DEFAULT = 102;   // config_screenBrightnessSettingMinimum / Maximum / Default (AOSP core config.xml)
    static final int GAMMA_MAX = 1023;
    private static final float R = 0.5f, A = 0.17883277f, B = 0.28466892f, C = 0.55991073f;   // the HLG constants of BrightnessUtils

    /** BrightnessUtils.convertLinearToGamma: the raw setting (min..max) on the slider's 0..1023 scale. */
    static int toGamma(int raw, int min, int max) {
        float n = (raw - min) / (float) (max - min) * 12f;   // HLG is normalised to [0, 12]
        float ret = n <= 1f ? (float) Math.sqrt(n) * R : A * (float) Math.log(n - B) + C;
        return Math.round(GAMMA_MAX * ret);
    }

    /** BrightnessUtils.convertGammaToLinear: a slider position (0..1023) as the raw setting. */
    static int fromGamma(int gamma, int min, int max) {
        float n = gamma / (float) GAMMA_MAX;
        float ret = n <= R ? (n / R) * (n / R) : (float) Math.exp((n - C) / A) + B;
        return Math.round(min + (max - min) * (ret / 12f));
    }

    /** The summary's fraction (0..1) of BrightnessLevelPreferenceController: the slider position as a share of 1023. */
    static double brightnessFraction(int raw, int min, int max) {
        double g = toGamma(raw, min, max);
        if (g > GAMMA_MAX) return 1.0;
        if (g < 0) return 0.0;
        return g / GAMMA_MAX;
    }

    // ------------------------------------------------------------------ screen timeout
    static final int[] TIMEOUT_VALUES = { 15_000, 30_000, 60_000, 120_000, 300_000, 600_000, 1_800_000 };   // screen_timeout_values: no "Never"
    static final String[] TIMEOUT_LABELS = { "15 seconds", "30 seconds", "1 minute", "2 minutes", "5 minutes", "10 minutes", "30 minutes" };

    /** The index of the entry whose value is {@code ms}, or -1. */
    static int timeoutIndex(long ms) {
        for (int i = 0; i < TIMEOUT_VALUES.length; i++) if (TIMEOUT_VALUES[i] == ms) return i;
        return -1;
    }

    /** "After 1 minute of inactivity" (screen_timeout_summary); "" for a value that is not in the list. */
    static String timeoutSummary(long ms) {
        int i = timeoutIndex(ms);
        return i < 0 ? "" : "After " + TIMEOUT_LABELS[i] + " of inactivity";
    }

    // ------------------------------------------------------------------ font size
    /**
     * entryvalues_font_size. AOSP 10 has 0.85, 1.0, 1.15, 1.30; Tj's tablet reads "Default" at its own font scale of 1.2, so its table is shifted. PROVISIONAL until Tj sends the tablet's Font size page
     * (BN1 open question): the same 0.15 steps with "Default" at 1.2.
     */
    static final float[] FONT_VALUES = { 1.05f, 1.2f, 1.35f, 1.5f };
    static final String[] FONT_LABELS = { "Small", "Default", "Large", "Largest" };

    /** ToggleFontSizePreferenceFragment / PreviewSeekBarPreferenceFragment.fontSizeValueToIndex: the first step whose midpoint the value is below. */
    static int fontIndex(float scale) {
        float last = FONT_VALUES[0];
        for (int i = 1; i < FONT_VALUES.length; i++) {
            float cur = FONT_VALUES[i];
            if (scale < last + (cur - last) * 0.5f) return i - 1;
            last = cur;
        }
        return FONT_VALUES.length - 1;
    }

    // ------------------------------------------------------------------ display size
    private static final String[] LARGER = { "Large", "Larger", "Largest" };

    /** The stops of the Display size seek bar. */
    static final class Density {
        final int[] values;
        final String[] labels;
        final int index;
        Density(int[] values, String[] labels, int index) { this.values = values; this.labels = labels; this.index = index; }
    }

    /**
     * DisplayDensityUtils: one smaller stop, the default, up to three larger stops (even numbers), limited so the smallest screen side stays at least 320 dp wide at 1.5x. {@code current} that matches no stop is
     * appended as "Custom (N)" and selected, like AOSP.
     */
    static Density densityStops(int defaultDpi, int widthPx, int heightPx, int current) {
        int smallest = Math.min(widthPx, heightPx);
        int maxDensity = 160 * smallest / 320;
        float maxScale = Math.min(1.5f, maxDensity / (float) defaultDpi);
        int numLarger = (int) clamp((maxScale - 1f) / 0.09f, 0f, 3f);
        int numSmaller = (int) clamp((1f - 0.85f) / 0.09f, 0f, 1f);
        int n = numSmaller + 1 + numLarger;
        int[] values = new int[n];
        String[] labels = new String[n];
        int k = 0, cur = -1;
        if (numSmaller > 0) {
            float interval = (1f - 0.85f) / numSmaller;
            for (int i = numSmaller - 1; i >= 0; i--) {
                int d = ((int) (defaultDpi * (1f - (i + 1) * interval))) & ~1;
                if (current == d) cur = k;
                values[k] = d; labels[k] = "Small"; k++;
            }
        }
        if (current == defaultDpi) cur = k;
        values[k] = defaultDpi; labels[k] = "Default"; k++;
        if (numLarger > 0) {
            float interval = (maxScale - 1f) / numLarger;
            for (int i = 0; i < numLarger; i++) {
                int d = ((int) (defaultDpi * (1f + (i + 1) * interval))) & ~1;
                if (current == d) cur = k;
                values[k] = d; labels[k] = LARGER[i]; k++;
            }
        }
        if (cur < 0) {
            int[] v2 = java.util.Arrays.copyOf(values, n + 1);
            String[] l2 = java.util.Arrays.copyOf(labels, n + 1);
            v2[n] = current; l2[n] = "Custom (" + current + ")";
            return new Density(v2, l2, n);
        }
        return new Density(values, labels, cur);
    }

    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
