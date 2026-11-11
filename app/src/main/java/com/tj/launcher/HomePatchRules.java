package com.tj.launcher;

/**
 * BI1 (Tj, 2026-11-08 ~04:00 UTC: "For the custom launcher, do the same logic and wallpaper patch that you did for the lock screen, but implement it for the unlocked home screen."). The pure rules (JVM-tested,
 * no Android classes) of the REAL-PIXEL patch over the stock power menu's Screenshot slot on the UNLOCKED custom home screen. Design and the reasons: {@code research/HOME_PATCH_2026-11-08.md}.
 *
 * The lock screen's patch rests over a still picture. The home screen is a live UI (the system wallpaper plus whatever the Launcher draws), so the patch is only correct while nothing under it changes. It is
 * therefore built from two layers (a calibrated wallpaper-only crop and the Launcher's own pixels), shown ONLY while the home is at rest, and everything else falls back to today's replacement panel.
 */
public final class HomePatchRules {
    private HomePatchRules() {}

    /** The switch in Launcher control (shipped default ON, debug builds OFF so the older emulator sections keep their premises, like the other behaviour switches). */
    public static final String KEY = "home_patch";

    public static boolean defaultOn(boolean debuggable) { return !debuggable; }

    /** How long after a finger went down the patch stays away: the press feedback and the scroll start follow within ~200 ms; the stock hide ({@code ACTION_OUTSIDE}) comes 60-70 ms after the touch. */
    public static final long TOUCH_AWAY_MS = 700;
    /** How long the Launcher's view tree must NOT have drawn before the patch may be visible (a draw means something under it may have changed). */
    public static final long DRAW_QUIET_MS = 250;

    // ================================================================ is the patch window wanted at all?

    /** Everything the decision reads, by name. All false = nothing is wanted. */
    public static final class Facts {
        /** the switch in Launcher control */
        public boolean switchOn;
        /** the power-menu mode is "hide_screenshot" (the old "wallpaper" mode keeps its own home patch, "off" and "block" have none) */
        public boolean coverMode;
        /** the custom launcher is the active one (not Quickstep) */
        public boolean customMode;
        /** the keyguard is gone */
        public boolean unlocked;
        public boolean screenOn;
        /** the Launcher is resumed and in its plain home state (not All Apps, not spring-loaded) */
        public boolean homeShowing;
        /** a folder, popup or other floating view is open over the workspace */
        public boolean floatingViewOpen;
        /** an icon drag or a state change is running */
        public boolean busy;
        /** the stock panel's rectangle was learned (it is shared with the lock screen) */
        public boolean rectKnown;
        /** a wallpaper-only crop is stored for the current wallpaper, display size and wallpaper offset */
        public boolean cropValid;
        /** the home calibration (screenshot with the Launcher hidden) is running right now */
        public boolean calibrating;
        /** the crash breaker tripped or "Restore everything" paused the launcher's automation */
        public boolean automationOff;
    }

    /** Why the patch window is NOT wanted now: the first failing condition in plain words, or "" when it is. One function for the decision and the words (Launcher control, the tablet report). */
    public static String whyNot(Facts f) {
        if (!f.switchOn) return "switch off";
        if (!f.coverMode) return "power menu mode is not 'hide Screenshot'";
        if (f.automationOff) return "automation paused";
        if (!f.customMode) return "Quickstep";
        if (!f.unlocked) return "lock screen";
        if (!f.screenOn) return "screen off";
        if (!f.homeShowing) return "not on the plain home screen";
        if (f.floatingViewOpen) return "a folder or popup is open";
        if (f.busy) return "dragging or changing screen";
        if (f.calibrating) return "calibrating";
        if (!f.rectKnown) return "open the power menu once so the menu's place is learned";
        if (!f.cropValid) return "not calibrated for this wallpaper and page yet";
        return "";
    }

    public static boolean wanted(Facts f) { return whyNot(f).isEmpty(); }

    // ================================================================ is the patch's content current, and is it visible?

    /**
     * Is the built patch current? It was requested at {@code builtAtMs} (uptime ms); any draw of the Launcher's view tree after that moment may have changed the pixels under the slot, so the patch is stale. The
     * wallpaper crop it was built with must also still be the one for now ({@code keyMatches}: wallpaper, display size, rotation, wallpaper offset step).
     */
    public static boolean compositeFresh(long builtAtMs, long lastDrawMs, boolean keyMatches) {
        return keyMatches && builtAtMs > 0 && builtAtMs >= lastDrawMs;
    }

    /** Should a new patch be built now? Only when there is none that is fresh, none is being built, and the home has been quiet for {@link #DRAW_QUIET_MS}. */
    public static boolean buildDue(long now, long lastDrawMs, boolean fresh, boolean building) {
        if (fresh || building) return false;
        return lastDrawMs <= 0 || now < lastDrawMs || now - lastDrawMs >= DRAW_QUIET_MS;
    }

    /**
     * Is the resting patch visible? Only when it is fresh and wanted ({@code restable}) AND no finger went down for {@link #TOUCH_AWAY_MS} AND the Launcher has not drawn for {@link #DRAW_QUIET_MS}.
     * (The stock dialog's own window event shows it regardless of the two timers when it is {@code restable}: a fresh patch is correct even a moment after a touch.)
     */
    public static boolean visible(boolean restable, long now, long lastTouchDownMs, long lastDrawMs) {
        if (!restable) return false;
        boolean touchOk = lastTouchDownMs <= 0 || now < lastTouchDownMs || now - lastTouchDownMs >= TOUCH_AWAY_MS;
        boolean drawOk = lastDrawMs <= 0 || now < lastDrawMs || now - lastDrawMs >= DRAW_QUIET_MS;
        return touchOk && drawOk;
    }

    /** Milliseconds until the two timers allow the patch to show (0 = now), for scheduling the next look. */
    public static long waitMs(long now, long lastTouchDownMs, long lastDrawMs) {
        long a = lastTouchDownMs <= 0 || now < lastTouchDownMs ? 0 : TOUCH_AWAY_MS - (now - lastTouchDownMs);
        long b = lastDrawMs <= 0 || now < lastDrawMs ? 0 : DRAW_QUIET_MS - (now - lastDrawMs);
        return Math.max(0, Math.max(a, b));
    }

    // ================================================================ the wallpaper crop: which one is for now

    /**
     * The wallpaper offset as a step number: the Launcher moves the wallpaper only with two or more pages (offset 0 with one page, steps of 1/(max(4, pages) - 1) otherwise), so offsets that differ by less
     * than a thousandth are the same position.
     */
    public static int step(float wallpaperX) {
        if (Float.isNaN(wallpaperX)) return 0;
        return Math.round(Math.max(0f, Math.min(1f, wallpaperX)) * 1000f);
    }

    /** Two offset steps are the same position (the offset is reported as a float that may differ in its last digits). */
    public static boolean sameStep(int a, int b) { return Math.abs(a - b) <= 2; }

    /** What a stored wallpaper crop is valid for: one wallpaper picture (system), one rotation, one display size. Same shape as {@link PatchShot#sig}. */
    public static String sig(long wallpaperId, int rotation, int dispW, int dispH) { return PatchShot.sig(wallpaperId, rotation, dispW, dispH); }

    /**
     * Does a stored crop's signature ({@code id|r<rotation>|WxH}, any rotation) still belong to this wallpaper picture and display size? Used to delete the crops of an old wallpaper when a new one is stored.
     */
    public static boolean sigMatches(String sig, long wallpaperId, int dispW, int dispH) {
        if (sig == null) return false;
        String[] p = sig.split("\\|");
        // the display size swaps in landscape (the real metrics of that rotation): a crop of the OTHER rotation of the same display must survive (code review 2026-11-08)
        return p.length == 3 && p[0].equals(String.valueOf(wallpaperId)) && (p[2].equals(dispW + "x" + dispH) || p[2].equals(dispH + "x" + dispW));
    }

    /** The private file of a crop (per rotation and offset step). */
    public static String fileName(int rotation, int step) { return "homeshot_r" + rotation + "_x" + step + ".png"; }

    /** The preference that holds a crop's meta ({@code sig#left#top}). */
    public static String metaKey(int rotation, int step) { return "home_shot_r" + rotation + "_x" + step; }

    // ================================================================ the home calibration: when may it start by itself?

    /** The calibration hides the Launcher's view tree for about a second and the system takes a screenshot (a camera flash): it only starts when nothing could be disturbed. */
    public static final long CAL_TOUCH_QUIET_MS = 3000, CAL_DRAW_QUIET_MS = 1500, CAL_SCREEN_ON_MS = 5000, CAL_MIN_GAP_MS = 60_000;
    public static final int CAL_MAX_PER_DAY = 6;

    public static final class CalState {
        public boolean switchOn, coverMode, customMode, unlocked, screenOn, homeAtRest, rectKnown, hasCrop, running, menuOpen, permission, automationOff, otherCalibration;
        /** The Launcher has reported its wallpaper offset in this process (until then the offset is only the 0.5 placeholder, and a crop filed under it would be filed under the wrong page position). */
        public boolean positionKnown = true;
        /** ms since a finger went down / since the Launcher last drew / since the screen came on / since the last calibration start; a huge number = never */
        public long msSinceTouch = Long.MAX_VALUE, msSinceDraw = Long.MAX_VALUE, msSinceScreenOn = Long.MAX_VALUE, msSinceLastStart = Long.MAX_VALUE;
        public int startsToday;
    }

    /** Why the home calibration must NOT start now: the first reason in plain words, or "" = go. Every "not yet" reason is shown in Launcher control. */
    public static String calDecision(CalState s) {
        if (!s.switchOn) return "switch off";
        if (!s.coverMode) return "power menu mode is not 'hide Screenshot'";
        if (s.automationOff) return "automation paused";
        if (!s.customMode) return "Quickstep";
        if (!s.unlocked) return "lock screen";
        if (!s.screenOn) return "screen off";
        if (!s.permission) return "needs 'Allow wallpaper access' in Launcher control";
        if (s.running) return "a calibration is running";
        if (s.otherCalibration) return "the lock-screen calibration is running";
        if (!s.positionKnown) return "the wallpaper position is not known yet";   // BEFORE hasCrop: neither "there is a crop" nor "there is none" can be said for a position nobody has reported
        if (s.hasCrop) return "already calibrated for this wallpaper and page";
        if (!s.rectKnown) return "open the power menu once so the menu's place is learned";
        if (!s.homeAtRest) return "the home screen is not at rest";
        if (s.menuOpen) return "the power menu is open";
        if (s.msSinceTouch < CAL_TOUCH_QUIET_MS) return "touched a moment ago";
        if (s.msSinceDraw < CAL_DRAW_QUIET_MS) return "the home screen is still moving";
        if (s.msSinceScreenOn < CAL_SCREEN_ON_MS) return "the screen came on a moment ago";
        if (s.msSinceLastStart < CAL_MIN_GAP_MS) return "tried a moment ago";
        if (s.startsToday >= CAL_MAX_PER_DAY) return "tried " + CAL_MAX_PER_DAY + " times today";
        return "";
    }

    /**
     * How the calibration screenshot is known to be wallpaper-only: NOT by comparing times (a media-store timestamp has a resolution of one second), but by construction. The Launcher's root view is hidden,
     * {@link #HIDE_SETTLE_MS} pass (the compositor shows the hidden state within a frame or two), the screenshot is requested, and the Launcher stays hidden until the media store reports the new file (the capture
     * is over by then) or {@link #HIDE_MAX_MS} pass; a screenshot that arrives after the Launcher was shown again is ignored.
     */
    public static final long HIDE_SETTLE_MS = 150, HIDE_MAX_MS = 4500;

    /**
     * Is a screenshot whose media-store time is {@code addedSec} (epoch seconds) the one asked for at {@code askedAtMs} (epoch ms)? One second of slack, exactly the rounding of the media store's whole seconds (a
     * file made at the very moment of the request is stamped at most 999 ms early). Two seconds, as this had, accepted a hardware-key screenshot of the normal home screen taken just before (code review 2026-11-08):
     * its icons would have been stored as "wallpaper only".
     */
    public static boolean shotIsOurs(long addedSec, long askedAtMs) {
        return askedAtMs > 0 && addedSec * 1000L >= askedAtMs - 1000L;
    }

    // ================================================================ BJ1: the FIRST window event of a power-menu burst
    // Tj, 2026-11-08 16:21 UTC, about v0.8.9: "The home screen also must match the dimming just as the lock screen did. Right now the patch looks correct except the dimming speed makes the patch appear".
    // MEASURED (emulator section hf, run #382, animations on, 5 films per treatment): v0.8.9 ran the node scan, the layer refresh and the pencil search on the burst's FIRST window event (56 ms, then 207 ms) on the
    // thread the patch is drawn on. The second scan found the menu itself, so the patch opened by the SLOW path (fade lag 267 ms), the dialog's own event waited 234 ms in the queue and the fast path declined
    // ("already open"); the patch stayed at full brightness for the whole stock fade and dimmed only at its end: +24 % brighter than the darkening surround (open_shape 0.20 against the 0.08 of the lock screen,
    // the fast path used in 1 of 10 films). With the lock screen's deferral the fast path was used in 10 of 10 films and the median fell to 0.054.

    /** The treatment of a window event while the home patch rests or its menu is open. */
    public static final int BURST_NORMAL = 0, BURST_QUIET = 1, BURST_START = 2;

    /**
     * What to do with the heavy per-event work (node scan, layer refresh, lock update) of a window event on the home screen:
     * {@link #BURST_QUIET} = it waits (a wait is running: the one the first event of the burst started, or the one the open menu's own event set); {@link #BURST_START} = this is the first window event of a burst
     * and the stock dialog's own event will open the patch at once (the patch is ready), so start the wait and let that event come first; {@link #BURST_NORMAL} = do the work now, as before.
     *
     * @param menuOpen   our open-menu patch is up (the dialog's own event has been handled)
     * @param now        uptime ms
     * @param quietUntilMs the end of the wait that is running (0 or in the past = none)
     * @param startReady this event can mean "a new window appeared" AND the fast open path is possible (the service's {@code fastDecline() == null}); the caller asks only when the answer can matter
     */
    public static int burstTreatment(boolean menuOpen, long now, long quietUntilMs, boolean startReady) {
        if (now < quietUntilMs) return BURST_QUIET;
        return !menuOpen && startReady ? BURST_START : BURST_NORMAL;
    }

    // ================================================================ BN2: the keyguard's own window is not "a shade over the home"
    // Tj, 2026-11-09 03:02 UTC, about v0.8.12, with a tablet report: the FIRST side-key press after the custom home opened showed the stock Screenshot entry, later ones did not. The report's log: the composite was
    // fresh 1.08 s after the unlock and the press came 0.35 s later, but the patch was "not resting": every plain unlock fires SystemUI's status-bar window event (android.widget.FrameLayout, the keyguard going away)
    // and homeForeignWindow kept the patch away for HOME_FOREIGN_AWAY_MS (2.5 s) after it, as if the shade had been pulled. "foreign windows seen 8 ... the last 35 s ago" = the unlock itself.

    /** A SystemUI window event this soon after the keyguard was last seen up is (most likely) the keyguard going away, not the shade. */
    public static final long UNLOCK_GRACE_MS = 3000;
    /** Such an event is judged again this long later, by the Launcher's window focus (the shade takes the focus from the Launcher, the keyguard's exit does not). */
    public static final long FOREIGN_RECHECK_MS = 400;
    /** {@link #FOREIGN_NOW}: a window over the home (a shade long after an unlock, a volume panel, a dialog): the away guard is final. {@link #FOREIGN_UNLOCK_CHECK}: the guard starts as ever but is PROVISIONAL: it can be lifted at {@link #FOREIGN_RECHECK_MS}. */
    public static final int FOREIGN_NOW = 0, FOREIGN_UNLOCK_CHECK = 1;

    /** The window the keyguard and the shade live in is SystemUI's status-bar window, announced as a plain FrameLayout; every SystemUI Dialog (the volume panel, the stock power menu) announces itself with its own class. */
    public static boolean isKeyguardWindow(String pkg, String cls) {
        return "com.android.systemui".equals(pkg) && "android.widget.FrameLayout".equals(cls);
    }

    /**
     * @param keyguardWindow {@link #isKeyguardWindow}: only that window can be "the keyguard going away"; a Dialog never is
     * @param sinceKeyguardUpMs ms since the keyguard was last seen up ({@code Long.MAX_VALUE} = never)
     */
    public static int foreignTreatment(boolean keyguardWindow, long sinceKeyguardUpMs) {
        return keyguardWindow && sinceKeyguardUpMs >= 0 && sinceKeyguardUpMs < UNLOCK_GRACE_MS ? FOREIGN_UNLOCK_CHECK : FOREIGN_NOW;
    }

    /** What the recheck of a provisional guard does: lift it (the keyguard went away), keep it (a real window: it is counted now), or nothing (another window set or extended the guard since: it is not ours to lift). */
    public static final int RECHECK_LIFT = 0, RECHECK_KEEP = 1, RECHECK_NOT_OURS = 2;

    /**
     * A recheck that finds the Launcher WITHOUT the focus looks again this long later while the guard has at least that much to run: after an unlock the Launcher's resume (its window gets the focus at the end of it)
     * can take longer than {@link #FOREIGN_RECHECK_MS} on the tablet, and the keyguard's window event comes at the START of the exit. At the guard's own end the window is counted as a real one.
     */
    public static boolean recheckAgain(long nowMs, long provisionalUntilMs) { return nowMs + FOREIGN_RECHECK_MS < provisionalUntilMs; }

    /** @param guardUntilMs the guard end now; @param provisionalUntilMs the guard end the deferred event set (0 = none) */
    public static int recheck(boolean launcherHasFocus, long guardUntilMs, long provisionalUntilMs) {
        if (provisionalUntilMs == 0 || guardUntilMs != provisionalUntilMs) return RECHECK_NOT_OURS;
        return launcherHasFocus ? RECHECK_LIFT : RECHECK_KEEP;
    }

    // ================================================================ BT1c / BT1d: the stock menu opens and the patch is not fresh
    // Tj, 2026-11-11 (v0.9.4): "after I pull down from the notification panel then return back to the home screen and hold the side key, the full power menu with screenshot button shows up the first time, then fixes
    // after that" and "sometimes the home screen wallpaper patch doesn't work". Emulator run #529 (section bt) showed the same picture after a plain return to the home screen: a patch that was not fresh at the
    // press (a draw of the Launcher since it was built, the guard of a window that was just up) left the replacement card SHORT (Power off / Restart only) and nothing over the Screenshot slot. Rule: the
    // stock Screenshot entry is never left uncovered.

    /** What the stock menu's own window event gets on the home screen: the fresh patch, a built-before (stale) patch shown as it is, or the replacement card over the WHOLE panel. */
    public static final int OPEN_FRESH = 0, OPEN_STALE = 1, OPEN_FULL_CARD = 2;

    /** Everything {@link #openTreatment} reads, by name. */
    public static final class OpenFacts {
        /** the home patch window exists (the stored wallpaper crop is valid for this page, the panel's place is learned) */
        public boolean patchWindow;
        /** fresh and nothing on top of the home screen: the patch may rest (what the fast path has always required) */
        public boolean restable;
        /** a built composite is in memory */
        public boolean compositeKept;
        /** it was built for the wallpaper position of now */
        public boolean stepMatches;
        /** the Launcher's window has the focus: no shade, no dialog, no other app's window is over the home screen */
        public boolean launcherFocus;
        /** the plain home screen is showing: no folder, no popup, no drag, not All Apps */
        public boolean homeAtRest;
        /** the calibration (the Launcher hidden for a screenshot) is running */
        public boolean calibrating;
    }

    /**
     * The decision for ONE opening of the stock menu. {@link #OPEN_FRESH}: today's path. {@link #OPEN_STALE}: the composite was built before the Launcher's last draw (or while a window guard was running), but
     * the Launcher has the focus on the plain home screen for the same wallpaper position: what is under the slot has almost always NOT changed (a draw is mostly a blinking clock, a press feedback or a returning
     * shade), so the patch is shown as it is and rebuilt in place at once; a wrong patch under the stock scrim is far less than the stock Screenshot entry. {@link #OPEN_FULL_CARD}: nothing trustworthy to
     * paint (another window has the focus, a folder is open, no composite, the calibration runs): our card covers the WHOLE panel, so the Screenshot entry still never shows.
     */
    public static int openTreatment(OpenFacts f) {
        if (!f.patchWindow || f.calibrating || !f.homeAtRest) return OPEN_FULL_CARD;
        if (f.restable) return OPEN_FRESH;
        if (f.compositeKept && f.stepMatches && f.launcherFocus) return OPEN_STALE;
        return OPEN_FULL_CARD;
    }

    /**
     * The words for the persistent log: why the patch was not fresh at the press (first failing reason, then the numbers). {@code builtAgoMs} / {@code drawAgoMs} / {@code guardLeftMs}: milliseconds, -1 = never / none.
     */
    public static String notFreshWhy(boolean compositeKept, boolean stepMatches, long builtAgoMs, long drawAgoMs, long guardLeftMs, boolean launcherFocus, boolean calibrating) {
        String why;
        if (calibrating) why = "calibrating";
        else if (!compositeKept) why = "no composite built yet";
        else if (!stepMatches) why = "built for another wallpaper position";
        else if (builtAgoMs >= 0 && drawAgoMs >= 0 && drawAgoMs < builtAgoMs) why = "the Launcher drew " + (builtAgoMs - drawAgoMs) + " ms after the build";
        else if (guardLeftMs > 0) why = "a window guard is running";
        else if (!launcherFocus) why = "the Launcher has no window focus";
        else why = "not restable (other)";
        return why + " [built " + (builtAgoMs < 0 ? "never" : builtAgoMs + " ms ago") + ", last draw " + (drawAgoMs < 0 ? "never" : drawAgoMs + " ms ago") + ", guard " + Math.max(0, guardLeftMs) + " ms left, focus " + launcherFocus + "]";
    }

    /**
     * BT1c: the Launcher's window has the focus again. The guard a foreign window started (it runs until {@code guardUntilMs}, {@code guardLenMs} long, so it began at the difference) belongs to the window that took
     * the focus when it began no earlier than 1.5 s before the Launcher lost it, or later (the shade's own opening events, or a closing event of that same window): that window is gone now and its guard has
     * nothing left to guard. A guard that began long BEFORE the loss (a volume panel that does not take the focus) is not ended by it. {@code focusLostAtMs} 0 = never lost.
     */
    public static boolean guardEndsWithFocus(long focusLostAtMs, long guardUntilMs, long guardLenMs, long nowMs) {
        if (focusLostAtMs <= 0 || guardUntilMs <= nowMs) return false;
        return guardUntilMs - guardLenMs >= focusLostAtMs - 1500;
    }
}
