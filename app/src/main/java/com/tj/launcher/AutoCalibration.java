package com.tj.launcher;

/**
 * AW5 (Tj, 2026-11-06 evening, chose A: "I choose A) auto calibrate the screenshot"). The lock-screen patch needs ONE real screenshot of the plain lock screen (see {@link PatchShot}); until now Tj had to start that
 * by hand (Launcher control > Calibrate the patch, lock, wake). Now the helper does it by itself on the first PLAIN lock screen that has no valid crop (a new wallpaper, rotation or screen size makes the stored crop
 * invalid), so there is no step for Tj and no grey cover block. Every automatic calibration leaves one screenshot in the tablet's gallery (Tj accepted that by choosing A; the helper tries to delete it again).
 * This class is the pure part (JVM-tested): when to start, how often, and which screenshots are refused (review AU2: a screenshot taken with the power menu open or the PIN pad up must never be stored).
 * {@link TjAccessibilityService} does the Android side.
 */
public final class AutoCalibration {
    /** Launcher control switch, default ON. OFF = only the manual "Calibrate the patch" button. */
    public static final String KEY = "patch_auto_cal";
    /** A short window: the resting patch is hidden while it runs (it must not be inside the screenshot), so it must not stay open for minutes. */
    public static final long WINDOW_MS = 25_000;
    /** Between two automatic starts (each leaves a screenshot in the gallery). */
    public static final long MIN_GAP_MS = 20_000;
    public static final int MAX_PER_LOCK = 3;
    public static final int MAX_PER_DAY = 8;
    public static final long DAY_MS = 24 * 3_600_000L;
    /** The epoch ms of the recent automatic starts, comma separated (the cap per day). */
    public static final String TRIES_KEY = "patch_auto_tries";
    /** Epoch ms at which the current calibration window opened (so the window length need not be a constant: automatic = short, manual = 4 minutes). */
    public static final String START_KEY = "patch_cal_start";
    /** The system app that writes real screenshots into the media store. */
    public static final String SYSTEM_UI = "com.android.systemui";
    /** A screenshot counts as "during the menu / the PIN pad" when the menu or the pad was seen this close (ms) before or after the capture. */
    public static final long GUARD_MS = 2_500;
    /** ... and as "right before an unlock" when the unlock came this soon after the capture. */
    public static final long UNLOCK_GUARD_MS = 1_500;

    private AutoCalibration() {}

    /** Everything the decision depends on; the service fills it in. */
    public static final class State {
        public boolean switchOn, coverOn, lockPatchOn, paused, permitted, geometryKnown, patchWindow, validCrop, calibrating;
        public boolean interactive, keyguardLocked, pinPadUp, menuOpen;
        public long now, lastStartMs;
        public int startsThisLock, startsToday;
    }

    /** Pure: why NOT to start an automatic calibration now ("" = go). The reasons are shown in Launcher control, so they are plain words. */
    public static String decision(State s) {
        if (!s.switchOn) return "automatic calibration is switched off";
        if (!s.coverOn || !s.lockPatchOn) return "the lock-screen patch is switched off";
        if (s.paused) return "paused (Restore everything / the crash breaker)";
        if (!s.permitted) return "needs permission: Launcher control > Allow wallpaper access";
        if (s.validCrop) return "";   // nothing to do is not a reason to show: callers treat "" + validCrop separately
        if (s.calibrating) return "a calibration is already running";
        if (!s.geometryKnown) return "waiting: hold the side key once on the lock screen so the menu's place is known";
        if (!s.patchWindow) return "waiting for the lock-screen patch window";
        if (!s.interactive) return "waiting: the screen is off";
        if (!s.keyguardLocked) return "waiting for the lock screen";
        if (s.pinPadUp) return "waiting: the PIN pad is up";
        if (s.menuOpen) return "waiting: the power menu is open";
        if (s.lastStartMs > 0 && s.now >= s.lastStartMs && s.now - s.lastStartMs < MIN_GAP_MS) return "waiting: the last automatic screenshot was just now";
        if (s.startsThisLock >= MAX_PER_LOCK) return "gave up for this lock after " + MAX_PER_LOCK + " tries";
        if (s.startsToday >= MAX_PER_DAY) return "gave up for today after " + MAX_PER_DAY + " tries";
        return "";
    }

    /** Pure: start now? Only when {@link #decision} says go AND there is no valid crop. */
    public static boolean shouldStart(State s) { return !s.validCrop && decision(s).isEmpty(); }

    /** Pure: how many of the recorded starts (comma separated epoch ms) lie within the last day. */
    public static int startsWithinDay(String tries, long now) {
        int n = 0;
        if (tries == null) return 0;
        for (String p : tries.split(",")) {
            try { long t = Long.parseLong(p.trim()); if (t > 0 && now >= t && now - t < DAY_MS) n++; } catch (NumberFormatException ignored) { }
        }
        return n;
    }

    /** Pure: the record with {@code now} added and everything older than a day dropped (at most {@link #MAX_PER_DAY} + 2 entries kept). */
    public static String addTry(String tries, long now) {
        StringBuilder b = new StringBuilder();
        int kept = 0;
        if (tries != null) for (String p : tries.split(",")) {
            try { long t = Long.parseLong(p.trim()); if (t > 0 && now >= t && now - t < DAY_MS && kept < MAX_PER_DAY + 1) { if (b.length() > 0) b.append(','); b.append(t); kept++; } } catch (NumberFormatException ignored) { }
        }
        if (b.length() > 0) b.append(',');
        return b.append(now).toString();
    }

    // ---------------------------------------------------------------- review AU2: which screenshots are refused

    /** Pure: is this the system's own screenshot? A missing owner (an old row, a shell screencap in the tests) is accepted; another app's file is not. */
    public static boolean ownerOk(String ownerPackage) { return ownerPackage == null || ownerPackage.isEmpty() || SYSTEM_UI.equals(ownerPackage); }

    /** Pure: were two moments (uptime ms) closer than {@code slackMs}? 0 = never happened. */
    public static boolean near(long aMs, long bMs, long slackMs) { return aMs > 0 && bMs > 0 && Math.abs(aMs - bMs) <= slackMs; }

    /**
     * Pure: why a screenshot taken at {@code shotUptimeMs} must be refused ("" = fine): the power menu or the PIN pad was showing around that moment, or the tablet was unlocked right after it.
     * {@code menuSeenMs} / {@code padSeenMs} / {@code unlockMs}: the last time (uptime ms) each was seen, 0 = never.
     */
    public static String refusal(long shotUptimeMs, long menuSeenMs, boolean menuOpenNow, long padSeenMs, boolean padUpNow, long unlockMs) {
        return refusal(shotUptimeMs, menuSeenMs, menuOpenNow, padSeenMs, padUpNow, unlockMs, 0, false);
    }

    /** BY1 review LOW5 (2026-11-12): also the notification panel / quick settings: a screenshot taken while it was open (or within the guard of it) shows the panel, not the lock screen. */
    public static String refusal(long shotUptimeMs, long menuSeenMs, boolean menuOpenNow, long padSeenMs, boolean padUpNow, long unlockMs, long shadeSeenMs, boolean shadeOpenNow) {
        if (menuOpenNow || near(shotUptimeMs, menuSeenMs, GUARD_MS)) return "the power menu was open when that screenshot was taken: take it with the menu closed";
        if (padUpNow || near(shotUptimeMs, padSeenMs, GUARD_MS)) return "the PIN pad was showing when that screenshot was taken: take it on the plain lock screen";
        if (shadeOpenNow || near(shotUptimeMs, shadeSeenMs, GUARD_MS)) return "the notification panel was open when that screenshot was taken: take it on the plain lock screen";
        if (unlockMs > 0 && unlockMs >= shotUptimeMs && unlockMs - shotUptimeMs <= UNLOCK_GUARD_MS) return "the tablet was unlocked right after that screenshot, so it may not show the lock screen";
        return "";
    }
}
