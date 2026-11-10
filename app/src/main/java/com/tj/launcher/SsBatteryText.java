package com.tj.launcher;

/**
 * BN1: the text rules of the Battery page's header, a pure function of its arguments (JVM-tested: SsBatteryTextTest). The thresholds and strings are AOSP 10 (SettingsLib PowerUtil.getBatteryRemainingStringFormatted,
 * BatteryInfo.getBatteryInfo, Utils.getBatteryStatus; research/aosp10-settings/A_battery_display.md sec. 2.3), the tablet variant of the "shut down" string, and no "based on your usage" variants (they need a vendor
 * provider that AOSP does not enable).
 */
final class SsBatteryText {
    private SsBatteryText() {}

    /** Formats a wall-clock time the way the system does ("9:15 AM" or "09:15" by the 12 / 24 hour setting); the Android side gives {@code DateFormat.getTimeFormat}. */
    interface TimeOfDay { String format(long epochMs); }

    static final long SEVEN_MIN = 7 * 60_000L, FIFTEEN_MIN = 15 * 60_000L, ONE_HOUR = 3_600_000L, ONE_DAY = 24 * ONE_HOUR, TWO_DAYS = 2 * ONE_DAY;

    // android.os.BatteryManager constants, repeated here so the class is plain Java
    static final int STATUS_CHARGING = 2, STATUS_DISCHARGING = 3, STATUS_NOT_CHARGING = 4, STATUS_FULL = 5;

    /** AOSP PowerUtil.roundTimeToNearestThreshold: a remainder below half the threshold rounds down, else up. */
    static long roundToNearest(long value, long threshold) {
        long a = Math.abs(value);
        long remainder = a % threshold;
        long down = a - remainder;
        return remainder < threshold / 2 ? down : down + threshold;
    }

    /** The discharging estimate as text, or null when there is none (drain time <= 0). {@code nowMs} = System.currentTimeMillis(). */
    static String discharge(long drainMs, long nowMs, TimeOfDay clock) {
        if (drainMs <= 0) return null;
        if (drainMs <= SEVEN_MIN) return "Tablet may shutdown soon";
        if (drainMs <= FIFTEEN_MIN) return "Less than " + SsFormat.elapsed(FIFTEEN_MIN) + " remaining";
        if (drainMs >= TWO_DAYS) return "More than 2 days remaining";
        if (drainMs >= ONE_DAY) return "About " + SsFormat.elapsed(roundToNearest(drainMs, ONE_HOUR)) + " left";
        return "Should last until about " + clock.format(roundToNearest(nowMs + drainMs, FIFTEEN_MIN));
    }

    /** "Charging", "Not charging", "Plugged in, can't charge right now", "Full", "Unknown" (Utils.getBatteryStatus). */
    static String status(int batteryStatus) {
        switch (batteryStatus) {
            case STATUS_CHARGING: return "Charging";
            case STATUS_DISCHARGING: return "Not charging";
            case STATUS_NOT_CHARGING: return "Plugged in, can’t charge right now";
            case STATUS_FULL: return "Full";
            default: return "Unknown";
        }
    }

    /**
     * The sentence under the percentage. Discharging (plugged == 0): the estimate, else the status. Plugged in: "<time> left until fully charged" when Android knows the time and the battery is not full, else the status.
     * {@code drainMs} / {@code chargeMs} are -1 when unknown.
     */
    static String header(int plugged, int batteryStatus, long drainMs, long chargeMs, long nowMs, TimeOfDay clock) {
        if (plugged == 0) {
            String d = discharge(drainMs, nowMs, clock);
            return d != null ? d : status(batteryStatus);
        }
        if (chargeMs > 0 && batteryStatus != STATUS_FULL) return SsFormat.elapsed(chargeMs) + " left until fully charged";
        return status(batteryStatus);
    }

    /**
     * BN1p D1: {@link #header} for a page that may not have read the statistics yet. While no read has finished ({@code readDone} false) the sentence is EMPTY, never the status word: Tj's screenshots show
     * the estimate from the first frame and no page may flash "Not charging" first. After a read that found nothing the status word is the honest fallback.
     */
    static String headerOrBlank(boolean readDone, int plugged, int batteryStatus, long drainMs, long chargeMs, long nowMs, TimeOfDay clock) {
        return readDone ? header(plugged, batteryStatus, drainMs, chargeMs, nowMs, clock) : "";
    }

    /** The Settings home's one-line summary: "69%" alone while the sentence is not known yet, else "69% - Should last until about 9:15 PM". */
    static String homeLine(String percent, String sentence) {
        return sentence == null || sentence.length() == 0 ? percent : percent + " - " + sentence;
    }
}
