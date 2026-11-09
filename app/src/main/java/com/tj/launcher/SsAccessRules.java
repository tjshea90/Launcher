package com.tj.launcher;

/**
 * BN1: the pure rules of the Accessibility page and its sub pages (JVM-tested: SsAccessRulesTest). Sources: AOSP 10 Settings (AccessibilitySettings, the TTS settings fragment, BalanceSeekBar),
 * research/aosp10-settings/C_access_system_about.md sec. 1.1, 1.4, 1.5, 1.7. Plain Java: no Android classes.
 */
final class SsAccessRules {
    private SsAccessRules() {}

    // ------------------------------------------------------------------ Touch & hold delay (Settings.Secure long_press_timeout)
    static final int[] LONG_PRESS_VALUES = { 400, 1000, 1500 };   // long_press_timeout_selector_values (ms); the first is also the default
    static final String[] LONG_PRESS_LABELS = { "Short", "Medium", "Long" };

    /** The index of the entry that holds {@code ms}, or -1 (a stored value outside the list selects nothing and the row has no summary, like the AOSP ListPreference). */
    static int longPressIndex(int ms) {
        for (int i = 0; i < LONG_PRESS_VALUES.length; i++) if (LONG_PRESS_VALUES[i] == ms) return i;
        return -1;
    }

    static String longPressSummary(int ms) {
        int i = longPressIndex(ms);
        return i < 0 ? "" : LONG_PRESS_LABELS[i];
    }

    // ------------------------------------------------------------------ Magnification
    /** The Accessibility row's summary (AccessibilitySettings.updateMagnificationSummary) for the triple-tap and the shortcut switches. */
    static String magnificationSummary(boolean tripleTap, boolean shortcut) {
        if (tripleTap && shortcut) return "Magnify with shortcut & triple-tap";
        if (shortcut) return "Magnify with shortcut";
        if (tripleTap) return "Magnify with triple-tap";
        return "Off";
    }

    static String onOff(boolean on) { return on ? "On" : "Off"; }

    // ------------------------------------------------------------------ Audio balance (Settings.System master_balance, -1.0 .. 1.0)
    static final int BALANCE_MAX = 200, BALANCE_CENTER = 100;
    private static final float BALANCE_SNAP = 0.03f;   // BalanceSeekBar.SNAP_TO_PERCENTAGE

    /** The slider position of a stored balance: round (balance * 100) + 100, kept inside the bar. Rounded, not cut: 0.41f * 100f is 40.999996, and the thumb would jump a notch after our own write (review B #1). */
    static int balanceProgress(float balance) {
        int p = Math.round(balance * 100f) + BALANCE_CENTER;
        return p < 0 ? 0 : (p > BALANCE_MAX ? BALANCE_MAX : p);
    }

    /** The value stored for a slider position: (progress - 100) * 0.01. */
    static float balanceValue(int progress) { return (progress - BALANCE_CENTER) * 0.01f; }

    /** The text stored for a slider position: "0" in the middle, else two decimals with a dot ("-0.50", "0.06"); always inside what ShellProtocol.setSystem accepts. */
    static String balanceText(int progress) {
        if (progress == BALANCE_CENTER) return "0";
        return String.format(java.util.Locale.US, "%.2f", balanceValue(progress));
    }

    /** BalanceSeekBar: a position within 3 % of the range around the middle (exclusive) is pulled to the middle. */
    static int snapBalance(int progress) {
        int threshold = (int) (BALANCE_MAX * BALANCE_SNAP);
        if (progress > BALANCE_CENTER - threshold && progress < BALANCE_CENTER + threshold) return BALANCE_CENTER;
        return progress;
    }

    // ------------------------------------------------------------------ Text-to-speech output (Settings.Secure tts_default_rate / tts_default_pitch, both percent)
    static final int TTS_DEFAULT = 100;
    static final int RATE_MIN = 10, RATE_MAX = 600, PITCH_MIN = 25, PITCH_MAX = 400;   // the slider offsets of TextToSpeechSettings (rate progress = value - 10, pitch = value - 25)
    static final int RATE_PROGRESS_MAX = RATE_MAX - RATE_MIN, PITCH_PROGRESS_MAX = PITCH_MAX - PITCH_MIN;   // 590, 375

    static int rateProgress(int value) { return clamp(value, RATE_MIN, RATE_MAX) - RATE_MIN; }
    static int rateValue(int progress) { return clamp(progress + RATE_MIN, RATE_MIN, RATE_MAX); }
    static int pitchProgress(int value) { return clamp(value, PITCH_MIN, PITCH_MAX) - PITCH_MIN; }
    static int pitchValue(int progress) { return clamp(progress + PITCH_MIN, PITCH_MIN, PITCH_MAX); }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
