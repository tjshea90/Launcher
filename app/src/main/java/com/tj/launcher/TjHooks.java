package com.tj.launcher;

import android.content.Context;
import android.view.MotionEvent;

/** The few entry points the patched Launcher3 calls. Everything here must be exception-safe: a bug here must never crash the Home screen. */
public final class TjHooks {
    public static final String EXTRA_HOME_WHILE_ON_HOME = "tj_home_while_on_home";
    private static volatile long lastLauncherPauseMs = 0;
    private static volatile boolean launcherResumed = false;

    private TjHooks() {}

    private static boolean startedOnce = false;
    public static final String STARTUP_KEY = "startup_open";   // default ON

    // ---------------------------------------------------------------- BI1: what the real-pixel patch on the home screen needs to know about the Launcher
    private static volatile java.lang.ref.WeakReference<com.android.launcher3.Launcher> launcherRef;
    private static java.lang.ref.WeakReference<com.android.launcher3.Launcher> drawWatchedFor;
    /** Uptime ms of the last draw of the Launcher's view tree / of the last finger-down in the Launcher (0 = never since this process started). Written on the main thread. */
    public static volatile long lastHomeDrawMs = 0, lastHomeTouchMs = 0;

    /** The resumed Launcher instance, or null. Main thread. */
    public static com.android.launcher3.Launcher launcher() {
        java.lang.ref.WeakReference<com.android.launcher3.Launcher> r = launcherRef;
        return r == null ? null : r.get();
    }

    private static volatile boolean launcherHidden = false;

    /**
     * Hides / shows the whole Launcher window (its decor view's alpha): the system wallpaper alone shows through. For the home patch's calibration screenshot only; the service always shows it again (the
     * capture's end, a touch, a timeout), and a resume or a pause of the Launcher does so too. Main thread. Returns false when there is no Launcher window to change.
     */
    public static boolean setLauncherHidden(boolean hidden) {
        try {
            com.android.launcher3.Launcher l = launcher();
            if (l == null || l.getWindow() == null || l.getWindow().peekDecorView() == null) { if (!hidden) launcherHidden = false; return false; }
            l.getWindow().getDecorView().setAlpha(hidden ? 0f : 1f);
            launcherHidden = hidden;
            return true;
        } catch (Throwable t) { android.util.Log.e("TjHooks", "hiding the launcher failed", t); return false; }
    }

    /**
     * A folder, popup or other floating view is open over the workspace (the state stays NORMAL for them). Main thread; false when unknown (the draw timer then keeps the patch away anyway).
     */
    public static boolean homeFloatingViewOpen() {
        try { com.android.launcher3.Launcher l = launcher(); return l != null && com.android.launcher3.AbstractFloatingView.getTopOpenView(l) != null; } catch (Throwable t) { return false; }
    }

    /** An icon drag or a state change is running. Main thread. */
    public static boolean homeBusy() {
        try {
            com.android.launcher3.Launcher l = launcher();
            return l != null && (l.getDragController().isDragging() || l.getWorkspace().isSwitchingState());
        } catch (Throwable t) { return false; }
    }

    /**
     * Registers (once per Launcher instance) a draw listener on the Launcher's view tree: a draw means something on the home screen may have changed (a scroll, a drag, a folder, a bind, press feedback, a new
     * icon), so the patch that rests over the slot is stale from that moment until it is built again. A catch-all that needs no hook in the Launcher's many state paths.
     */
    private static void watchHomeDraw(com.android.launcher3.Launcher l) {
        try {
            java.lang.ref.WeakReference<com.android.launcher3.Launcher> w = drawWatchedFor;
            if (w != null && w.get() == l) return;
            l.getDragLayer().getViewTreeObserver().addOnDrawListener(new android.view.ViewTreeObserver.OnDrawListener() {
                @Override public void onDraw() {
                    lastHomeDrawMs = android.os.SystemClock.uptimeMillis();
                    try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.homeDrew(); } catch (Throwable ignored) { }
                }
            });
            drawWatchedFor = new java.lang.ref.WeakReference<>(l);   // only AFTER the listener is in (code review 2026-11-08): a registration that threw (the observer not alive yet) is tried again at the next resume; marked first it would never be, lastHomeDrawMs would stay 0 and every built patch would count as fresh for ever
        } catch (Throwable t) { android.util.Log.e("TjHooks", "home draw watch failed", t); }
    }

    /** Called by Launcher.onResume. The FIRST resume of each launcher process (boot, or after Android restarted it) puts the target app (Solitaire) in front. */
    /** BK1c: uptime of the Launcher's last onResume (the unlock summary looks at it when the Launcher resumed before the keyguard's broadcast). */
    public static volatile long lastResumeMs = 0;
    /** BY1a A2/C13: the Launcher instance whose tasks are flagged already (main thread). A recreated Launcher is a new instance and flags again. */
    private static java.lang.ref.WeakReference<Context> recentsFlaggedFor;

    public static void onLauncherResumed(final Context c) {
        launcherResumed = true; lastResumeMs = android.os.SystemClock.uptimeMillis();
        try { if (c instanceof com.android.launcher3.Launcher) { launcherRef = new java.lang.ref.WeakReference<>((com.android.launcher3.Launcher) c); watchHomeDraw((com.android.launcher3.Launcher) c); } } catch (Throwable ignored) { }   // BI1
        try { TjAccessibilityService s0 = TjAccessibilityService.get(); if (launcherHidden && (s0 == null || !s0.homeCalRunning())) setLauncherHidden(false); } catch (Throwable ignored) { }   // BI1: a hidden Launcher without a running calibration is a leftover
        try { if (c instanceof com.android.launcher3.Launcher) launcherNormal = ((com.android.launcher3.Launcher) c).isInState(com.android.launcher3.LauncherState.NORMAL); } catch (Throwable ignored) { }
        try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) { s.sumNoteResume(); s.refreshLayersSoon(); } } catch (Throwable ignored) { }   // BK1c: when the Launcher resumed after the keyguard went away
        if (!RecentsGuard.sameInstance(recentsFlaggedFor, c) && RecentsGuard.excludeOurTasks(c, true)) recentsFlaggedFor = new java.lang.ref.WeakReference<>(c);   // W2: also fixes a Launcher task created before the exclude flags existed; BY1a A2/C13: once per Launcher instance, task ids already flagged skipped (it was 1 + 2 binder calls per task at EVERY resume)
        if (c instanceof android.app.Activity) RecentsGuard.describeAsTarget((android.app.Activity) c);   // BY1a A2: returns at once for the instance that already carries the description
        try {
            if (!startedOnce) {
                startedOnce = true;
                if (Mode.isCustom(c) && ReturnReset.autoOpenWanted(c)) {   // BE3: a clean start opens no stand-in app
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override public void run() { try { ReturnReset.openStartupApp(c); } catch (Throwable t) { android.util.Log.e("TjHooks", "startup open failed", t); } }
                    }, 350);
                }
            }
        } catch (Throwable t) { android.util.Log.e("TjHooks", "startup hook failed", t); }
    }

    /** Current wallpaper x offset (0..1) as the launcher last set it; 0.5 until the launcher says otherwise. */
    public static volatile float wallpaperX = 0.5f;
    /** True once the Launcher of THIS process has reported an offset (BI1): until then {@link #wallpaperX} is only the placeholder, and a home-patch crop looked up or filed under it would be for the wrong page position. */
    public static volatile boolean wallpaperReported = false;

    public static void onWallpaperOffset(float x) {
        wallpaperX = x;
        wallpaperReported = true;
        try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.onWallpaperOffset(); } catch (Throwable ignored) { }
    }

    // ---------------------------------------------------------------- AD4: the page-flick entrance watches every single-finger swipe that STARTS on the settled home screen (a passive observer: it never consumes an event)
    private static float downX, downY;
    private static long downAt;
    private static boolean downHome, multiTouch;

    /** Called by Launcher.dispatchTouchEvent before the event is handled as usual. */
    public static void onTouch(android.app.Activity a, MotionEvent ev) {
        try {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {   // BI1: the finger is down: the real-pixel patch over the Screenshot slot goes away in this very frame (before the press feedback and any scroll), ACTION_OUTSIDE is only the second guard
                lastHomeTouchMs = android.os.SystemClock.uptimeMillis();
                try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.homeTouchDown(); } catch (Throwable ignored) { }
            }
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: downHome = homeScreenShowing() && EntranceWatcher.on(a.getApplicationContext(), SecretEntrances.Method.FLICK); multiTouch = false;   // nothing else is done for a touch while the entrance is off or the home screen is not showing
                    if (downHome) { downX = ev.getRawX(); downY = ev.getRawY(); downAt = ev.getEventTime(); } break;
                case MotionEvent.ACTION_POINTER_DOWN: multiTouch = true; break;
                case MotionEvent.ACTION_UP:
                    if (downHome && !multiTouch) {
                        int dir = SecretEntrances.swipeDirection(ev.getRawX() - downX, ev.getRawY() - downY, ev.getEventTime() - downAt, a.getResources().getDisplayMetrics().widthPixels);
                        if (dir != 0) EntranceWatcher.onSwipe(a.getApplicationContext(), dir);
                    }
                    downHome = false;
                    break;
                case MotionEvent.ACTION_CANCEL: downHome = false; break;
                default: break;
            }
        } catch (Throwable t) { android.util.Log.e("TjHooks", "touch hook failed", t); }
    }

    /** True while the plain home screen (workspace, NORMAL state) is the visible, settled screen. */
    public static volatile boolean launcherNormal = false;

    public static boolean homeScreenShowing() { return launcherResumed && launcherNormal; }

    /** BL1: where the Launcher window's decor view is on screen and how big it is: {left, top, width, height}, or null while there is none attached (the home curtain copies the whole window and lays it at its origin). Main thread. */
    public static int[] launcherWindowBounds() {
        try {
            com.android.launcher3.Launcher l = launcher();
            if (l == null || l.getWindow() == null) return null;
            android.view.View d = l.getWindow().peekDecorView();
            if (d == null || !d.isAttachedToWindow()) return null;
            int[] loc = new int[2];
            d.getLocationOnScreen(loc);
            return new int[] { loc[0], loc[1], d.getWidth(), d.getHeight() };
        } catch (Throwable t) { return null; }
    }

    public static void watchState(com.android.launcher3.LauncherStateManager sm) {
        try {
            sm.addStateListener(new com.android.launcher3.LauncherStateManager.StateListener() {
                @Override public void onStateTransitionStart(com.android.launcher3.LauncherState toState) { setNormal(false); }
                @Override public void onStateTransitionComplete(com.android.launcher3.LauncherState finalState) { setNormal(finalState == com.android.launcher3.LauncherState.NORMAL); }
            });
        } catch (Throwable t) { android.util.Log.e("TjHooks", "state watch failed", t); }
    }

    private static void setNormal(boolean v) {
        launcherNormal = v;
        try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.refreshLayersSoon(); } catch (Throwable ignored) { }
    }

    /** The Launcher's window got or lost the focus (a shade or a dialog over the home screen takes it; it comes back when they go). */
    public static void onWindowFocus(boolean has) {
        try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.homeFocusChanged(has); } catch (Throwable ignored) { }
    }

    public static void onLauncherPaused() {
        launcherResumed = false;
        if (launcherHidden) setLauncherHidden(false);   // BI1: never leave the home screen invisible (a calibration that was cut off by a pause)
        try { TjAccessibilityService s1 = TjAccessibilityService.get(); if (s1 != null) s1.homeCalInterrupted("the Launcher was paused"); } catch (Throwable ignored) { }
        try { TjAccessibilityService s = TjAccessibilityService.get(); if (s != null) s.refreshLayersSoon(); } catch (Throwable ignored) { }
        lastLauncherPauseMs = System.currentTimeMillis();
    }

    /** True when Home was pressed while our launcher was the visible screen (it paused a moment ago because the router appeared). */
    public static boolean launcherWasForeground() {
        return launcherResumed || System.currentTimeMillis() - lastLauncherPauseMs < 600;
    }
}
