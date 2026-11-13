package com.tj.launcher;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
import com.android.launcher3.R;

/**
 * Power-menu helper (L4 / G3b). Only active in custom-launcher mode, only when Tj chose a power mode, never on the lock screen.
 * HIDE_SCREENSHOT: covers the "Screenshot" entry with a panel-coloured overlay (TYPE_ACCESSIBILITY_OVERLAY, needs no overlay permission).
 * BLOCK: closes the whole power menu (Back). Fails open: any problem -> the menu is simply left alone and the overlay removed.
 */
public class TjAccessibilityService extends AccessibilityService {
    private static final String TAG = "TjA11y";
    private static final int PANEL_COLOR = Color.parseColor("#E8F0F1");   // v0.7.6: was #EFF1F3 (AOSP emulator). Tj's side-by-side photo: the stock card is about 4 % less red than ours in the same photo (low reliability: screens differ), so only half way: #E8F0F1

    private static volatile TjAccessibilityService instance;
    /** The running helper, or null when Tj has not turned it on in Accessibility settings. */
    public static TjAccessibilityService get() { return instance; }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View overlay;
    private final Runnable recheck = new Runnable() { @Override public void run() { evaluate(); } };

    private boolean pending = false;
    private final Rect shownAt = new Rect();
    private final Runnable debounced = new Runnable() {
        @Override public void run() {
            pending = false;
            try { evaluate(); } catch (Throwable t) { Log.e(TAG, "evaluate failed", t); removeOverlay(); }
        }
    };

    private int diagLines = 0; private long diagLastMs = 0;
    /** Debug builds only (emulator section pt): the events around the lock-screen patch, with their age on arrival, to find the event that matches the moment the stock scrim becomes VISIBLE. */
    private void diagEvent(AccessibilityEvent e) {
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0 || e == null || (!patchResting && !overlayShown)) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - diagLastMs > 2500) diagLines = 0;   // a new opening
        int type = e.getEventType();
        CharSequence pk = e.getPackageName();
        if (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && (pk == null || !"com.android.systemui".contentEquals(pk))) return;
        if (diagLines >= 200) return;   // BC2: the opening alone logs ~70 lines; the close follows ~2 s later in the same count
        diagLines++; diagLastMs = now;
        String pane = "";   // BY1b: what a SystemUI pane says about itself (the notification panel's pane title on the lock screen: "Lock screen" / "Notification shade" / "Quick settings"?)
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_ANNOUNCEMENT) { CharSequence tx = e.getText() == null || e.getText().isEmpty() ? null : e.getText().get(0); pane = " cct=0x" + Integer.toHexString(e.getContentChangeTypes()) + " txt=" + (tx == null ? "-" : tx.toString().replace(' ', '_').substring(0, Math.min(32, tx.length()))); }
        Log.i(TAG, "PATCHEV t=" + now + " type=0x" + Integer.toHexString(type) + " pkg=" + pk + " cls=" + e.getClassName() + " age=" + Math.max(-1, now - e.getEventTime()) + " rest=" + (patchResting ? 1 : 0) + " open=" + (overlayShown ? 1 : 0) + " wid=" + e.getWindowId() + " dlg=" + dialogWindowId + " wc=" + e.getWindowChanges() + (closing ? " closing=1" : "") + pane);   // BC2: wid / wc show which window an event is about (the dialog's removal ends the fade-out)
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        evSeen++;
        if (event != null && event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {   // BH1: a notification (or a toast) was posted: note WHO posted it (the package, never the title or text) and stop; nothing below is about it (no power-menu scan, no pencil search)
            try { CharSequence cn = event.getClassName(); if (cn != null && "android.app.Notification".contentEquals(cn)) NotifSightings.onNotification(getApplicationContext(), event.getPackageName()); } catch (Throwable t) { Log.e(TAG, "notification sighting failed", t); }
            return;
        }
        try { diagEvent(event); } catch (Throwable ignored) { }
        try { if (event != null && EventNoise.windowsJustChanged(event.getEventType())) { noteWindowEvent(event.getEventTime()); fastMenuOpen(event); } } catch (Throwable t) { Log.e(TAG, "fast menu check failed", t); }   // AZ1: FIRST, before any other work of this event
        try { if (event != null) fastMenuClose(event); } catch (Throwable t) { Log.e(TAG, "fast menu close check failed", t); }
        try { if (event != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) lockShadeEvent(event); } catch (Throwable t) { Log.e(TAG, "shade check failed", t); }   // BY1c F1: the panel opened / closed on the lock screen   // BC2: a click in the stock dialog starts the fade-out, the removal of its window ends it
        try { if (event != null && patchHomeMode) homeForeignWindow(event); } catch (Throwable t) { Log.e(TAG, "home foreign window check failed", t); }   // BI1: the shade, the volume panel ... must not get the home patch painted over them
        try { trackForeground(event); } catch (Throwable t) { Log.e(TAG, "foreground tracking failed", t); }
        try { if (event != null && EventNoise.windowsJustChanged(event.getEventType())) AnimationSpeedup.tick(this); } catch (Throwable t) { Log.e(TAG, "animation heartbeat failed", t); }   // BY1a C2: WINDOW events only (the facts it re-reads, mode / screen / keyguard / helper, change with a window; other apps' content changes drove up to 2 syncs a second, ~6 binder calls each, in Quickstep)   // BF1: self-heal (a lock screen that came up after the wake, a missed broadcast); a volatile read and a time compare in all but two events a second, nothing at all in the custom launcher
        try { picker().onEvent(event); } catch (Throwable t) { Log.e(TAG, "picker guard failed", t); }
        try { recents().onEvent(event); } catch (Throwable t) { Log.e(TAG, "recents guard failed", t); }   // AP1: whenever Recents opens in the custom launcher, the cards of apps that are not in the drawer are closed   // AO1: no "My photos" tile in the system wallpaper picker (one string compare for every other app's event)
        // Never react to our own overlay (adding it fires events: that would be a feedback loop), and coalesce bursts of events into one check.
        if (event != null && getPackageName().contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) return;
        if (event != null && EventNoise.isNoise(event.getEventType(), event.getPackageName())) {   // AB2: another app's content change: it cannot open the power menu, the shade or the PIN pad
            evNoise++;
            if (probeWanted()) { try { probeEvent(event); } catch (Throwable t) { Log.e(TAG, "probe failed", t); } }   // only the (rare, 10-minute) Solitaire probe wants those
            return;
        }
        final boolean quiet = quietBurst(android.os.SystemClock.uptimeMillis(), event);   // AZ1: the heavy work of a window burst on the lock screen waits for the stock dialog's own event
        if (event != null && EventNoise.windowsJustChanged(event.getEventType())) fullScanUntilMs = android.os.SystemClock.uptimeMillis() + 300;   // AB2: look in every window for the next 300 ms (a power menu is a new window)
        final boolean coverOn = !Features.POWER_OFF.equals(Features.powerMode(this)) && coverWorld();
        boolean scanned = false;
        if (overlayShown && !quiet && coverOn) { leadingEdge(event); scanned = true; }   // AZ1: the menu is open: noticing that it CLOSED (the patch must un-dim) comes before the PIN-pad scan and the other per-event work, which can take 100+ ms in a busy SystemUI (run 245: the patch stayed dark 85 ms after the surround was bright again)
        if (!quiet) { scheduleLockUpdate(); scheduleLayerRefresh(); schedulePencil(event); }
        trackDirty(event);
        trackUnlockKey(event);
        if (probeWanted()) { try { probeEvent(event); } catch (Throwable t) { Log.e(TAG, "probe failed", t); } }
        if (event != null) { try { guardStrayHome(event); } catch (Throwable t) { Log.e(TAG, "guard failed", t); } }
        if (!coverOn) {
            if (overlay != null || overlayShown) removeOverlay();
            return;   // nothing to do and nothing scanned: zero cost when the feature is off
        }
        if (quiet || scanned) return;   // AZ1: quietWork runs the scans once, after the dialog's event had its turn (or after FAST_WAIT_MS if it never came)
        leadingEdge(event);
    }

    /** Leading edge: evaluate on the FIRST event at once (the cover must appear as fast as possible), then once more shortly after to catch late content. */
    private void leadingEdge(AccessibilityEvent event) {
        long now = android.os.SystemClock.uptimeMillis();
        // AN1 (Tj: the battery saver tile in the shade is slow / unresponsive): a content change of the shade or the status bar (a tile redrawing, the clock) cannot OPEN the power menu, so it starts no scan unless a menu is
        // already up or a window just changed. Every scan is a pair of whole-tree searches run by SystemUI's own UI thread, the thread that handles the tap on the tile (EventNoise.needsPowerMenuScan).
        if (!EventNoise.needsPowerMenuScan(event == null ? 0 : event.getEventType(), overlayShown || now - menuUpAtMs < 2500, now < fullScanUntilMs, EventNoise.systemPackage(event == null ? null : event.getPackageName()), lastBouncer)) { evSkipped++; return; }   // CA1 U1: lastBouncer = a PIN key starts no scan
        if (now - lastEvalMs >= 40) {
            lastEvalMs = now;
            try { evaluate(); } catch (Throwable t) { Log.e(TAG, "evaluate failed", t); removeOverlay(); }
        }
        if (!pending) { pending = true; handler.postDelayed(debounced, 80); }
    }

    /**
     * AZ1: the earliest possible reaction to the stock power menu opening on the lock screen. Everything else {@link #onAccessibilityEvent} does for an event (the lock-number update with its PIN-pad
     * search, the layer refresh, the pencil, the handshake bookkeeping) used to run BEFORE the menu was looked for, and the node scan that followed blocked the main thread until it was over; the patch's
     * new brightness was only drawn after that. Now: the lock-screen patch is resting, a window event arrives, the window list holds the stock dialog and its geometry is known (it is, after the first use):
     * switch the patch to its open state at once and let the fade start. The node scan then waits for the end of the stock fade ({@link PatchFade#SETTLE_MS}). Fail-open: anything else (no geometry yet, the
     * list does not show the dialog yet, the switch is off) leaves everything to the normal path below.
     */
    private void fastMenuOpen(AccessibilityEvent event) {
        // Run 243 (emulator, animations ON in the real device but not there): the first version asked getWindows() on every window event and found the dialog only ~140 ms after the stock scrim had appeared (a binder call
        // that lists every window, repeated per event, on a thread that was busy with the events before it): the patch stayed LIGHT against the darkening surround for that time = the flicker. The dialog announces itself:
        // a TYPE_WINDOW_STATE_CHANGED event whose class is GlobalActionsDialog$ActionsDialog. No binder call at all, the event's own time is the anchor of the fade.
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cn = event.getClassName();
        if (cn == null || !cn.toString().contains("ActionsDialog")) return;
        long now = android.os.SystemClock.uptimeMillis();
        dialogEventMs = event.getEventTime();
        menuClosedAtMs = 0;                     // BN2: the dialog announced itself: a NEW opening, not the dying one of a close (evaluateScan)
        dialogWindowId = event.getWindowId();   // BC2: the removal of THIS window ends the fade-out (dialogGone)
        String why = fastDecline();
        if (why != null) {
            Log.i(TAG, "PATCH fast skip: " + why + " (the normal path follows) t=" + now);
            // BJ1: the burst's first event may have started a wait for a FAST open on the home screen (quietBurst); the dialog's own event says it is not going to happen (the patch went stale in between): the card path must not sit behind that wait
            if (patchHomeMode && !overlayShown && quietUntilMs != 0) { quietUntilMs = 0; handler.removeCallbacks(quietWork); }
            return;
        }
        Rect cached = cachedCard();
        if (menuSeenMs == 0) menuSeenMs = now;
        menuUpAtMs = now;
        if (!patchResting && !patchHomeMode) logPatch("menu opened while the patch was away (touch " + (touchAwayUntilMs > now) + ", PIN guard " + (pinHiddenUntilMs > now) + "): shown at the dialog's own event");   // BE1: the lock log shows the case behind a Screenshot flash
        touchAwayUntilMs = 0; if (!padSignFresh()) pinHiddenUntilMs = 0;   // a sign of the PIN pad a moment ago is NOT forgotten (code review 2026-11-07: the scan is ~120 ms slower than the first sign)
        Log.i(TAG, "PATCH fast open: the dialog's window event is " + Math.max(0, now - event.getEventTime()) + " ms old, " + Math.max(0, event.getEventTime() - winBurstStartMs) + " ms after the first window event of the burst t=" + now);   // BY1b: t= puts it on the film's timeline (section fl 'seq')
        showOverlay(cached);
        if (fading) deferScan(now); else deferWork(now, 60);   // the patch is drawn (and, while it fades, redrawn every frame) before any heavy work of this burst runs
    }

    /** AZ1: null = the fast path may open the patch now; else why not (the normal scan path then does it, a little later). */
    private String fastDecline() {
        if (patchView == null) return "no patch window yet";
        if (!patchLockMode) return "not the lock screen";
        if (!patchResting && (!patchRestable || lastBouncer || padSignFresh())) {   // BE1: a patch that is merely AWAY (touch / PIN guard) comes back at the dialog's own event
            // BT1c / BT1d: on the home screen a patch that is not fresh (a draw of the Launcher since it was built, a window guard) is shown as it is while the Launcher has the focus (HomePatchRules.openTreatment): never the short card with the Screenshot slot open
            if (!(patchHomeMode && !lastBouncer && !padSignFresh() && homeOpenTreatment() == HomePatchRules.OPEN_STALE)) return "the patch is not resting (PIN pad up, or no valid crop)" + (patchHomeMode ? " " + homeNotFreshWhy() : "");
        }
        if (overlayShown) return "already open";
        if (!Features.isCover(Features.powerMode(this)) || !coverWorld()) return "power menu mode " + Features.powerMode(this);
        if (cachedCard() == null) return "the menu geometry is not learned yet";
        return null;
    }

    /** AZ1: no node scan until the stock fade is over (and one more look then): it blocks this thread for tens of ms, and the patch has to be redrawn on every frame of the fade. */
    private void deferScan(long now) {
        if (!fading) return;   // nothing to follow (switch off, or the scrim was complete already): scan as before
        scanNotBeforeMs = now + PatchFade.SETTLE_MS;
        deferWork(now, PatchFade.SETTLE_MS);
    }

    /**
     * AZ1 (run 245, emulator with animations on): the first window event of a power-menu opening used to start the PIN-pad scan of the lock number ({@link #bouncerShown}: a dozen node searches in SystemUI's
     * windows) and the power-menu scan on this very thread, one after the other. SystemUI is busy inflating and animating the dialog at that moment, so they took 100-300 ms and the dialog's OWN event,
     * queued behind them, was handled 80-280 ms late (event age at handling), and the patch then dimmed 400 ms after the scrim = a light block against the dark surround for that long. Now the heavy
     * per-event work (lock-number/PIN-pad scan, layer refresh, pencil, the power-menu scan) of a window burst on the lock screen waits until {@code now + waitMs} and runs ONCE ({@link #quietWork}): the dialog's event is
     * handled first (fast path) and the patch is drawn before anything else.
     */
    private void deferWork(long now, long waitMs) {
        quietUntilMs = now + waitMs;
        handler.removeCallbacks(recheck); handler.removeCallbacks(quietWork);
        handler.postDelayed(quietWork, waitMs + 20);
    }

    private long quietUntilMs = 0;
    private volatile long menuClosedAtMs = 0;   // BN2: uptime of the last close we were told about (0 = none, or a new dialog announced itself since); see PowerMenuRule.dyingDialog
    private int holdMisses = 0;   // AZ1: scans in a row that found no menu text while the dialog window was still up
    /** AZ1: how long a window burst on the lock screen waits for the stock dialog's own event before the normal scans run (fail-open for a ROM whose dialog does not announce itself). The dialog's event follows the first window event of its burst by 15-45 ms. */
    static final long FAST_WAIT_MS = 120;
    private final Runnable quietWork = new Runnable() { @Override public void run() {
        quietUntilMs = 0;
        try { scheduleLockUpdate(); scheduleLayerRefresh(); schedulePencil(null); } catch (Throwable t) { Log.e(TAG, "deferred event work failed", t); }
        try { evaluate(); } catch (Throwable t) { Log.e(TAG, "evaluate failed", t); removeOverlay(); }
    } };

    /** AZ1: true while the heavy per-event work of the current window burst waits ({@link #deferWork}); the first window event of a burst on a resting lock-screen patch starts the wait ({@link #FAST_WAIT_MS}). */
    private boolean quietBurst(long now, AccessibilityEvent event) {
        // BI1: the wait that the power menu's OWN open event set (fastMenuOpen: deferWork / deferScan) is honoured on the home screen like on the lock screen: the patch is redrawn every frame of the stock 300 ms fade and no
        // layer refresh / pencil work may block that thread meanwhile (code review 2026-11-08). No QS pencil is on screen while the power menu is open.
        // BJ1 (Tj, 2026-11-08 16:21 UTC: "The home screen also must match the dimming just as the lock screen did. Right now the patch looks correct except the dimming speed makes the patch appear"): v0.8.9 started NO wait
        // on the home screen for the FIRST window event of a burst (to keep the QS pencil's 40 ms rule, B6), so the node scan (56 ms, then 207 ms on the CI emulator) ran before the dialog's own event, which then waited in the
        // queue (age 234 ms) while the second scan found the menu itself: the patch opened by the SLOW path 267 ms late and stayed at full brightness while the scrim darkened (+24 %; emulator run #382, section hf: the fast
        // path was used in 1 of 10 films, median open_shape 0.20). The lock screen's wait fixes that (fast path in 10 of 10 films, median 0.054). It is only started when the fast path will be taken, ends after FAST_WAIT_MS
        // at the latest (fail-open: then the scan runs as before), and the pencil is NOT part of it: it follows every event, as before.
        if (patchHomeMode) {
            // `ready` only says "this event could start the wait"; the guards in front of fastDecline() are there so that it is asked only when it can matter (burstTreatment decides, and is what the JVM tests pin)
            boolean ready = !overlayShown && now >= quietUntilMs && event != null && EventNoise.windowsJustChanged(event.getEventType()) && fastDecline() == null;
            int t = HomePatchRules.burstTreatment(overlayShown, now, quietUntilMs, ready);
            if (t == HomePatchRules.BURST_NORMAL) return false;   // with the menu closed and no wait running no event is deferred, so the shade / QS pencil keep their 40 ms rule
            if (!overlayShown) schedulePencil(event);             // a wait on the closed home: the pencil cover still follows every SystemUI event
            if (t == HomePatchRules.BURST_START) deferWork(now, FAST_WAIT_MS);
            return true;
        }
        if (now < quietUntilMs) return true;
        if (event == null || !EventNoise.windowsJustChanged(event.getEventType()) || fastDecline() != null) return false;
        deferWork(now, FAST_WAIT_MS);
        return true;
    }

    private long lastEvalMs = 0;
    private long fullScanUntilMs = 0;
    /** AM1: a patch window that was just created re-decides whether it RESTS after 400 ms and 1.6 s (the stored crop, the keyguard and the PIN pad may only be settled by then). */
    private final Runnable restRecheck = new Runnable() { @Override public void run() {
        try {
            if (patchView == null || !patchLockMode) return;
            if (patchHomeMode) { patchShow(overlayShown); scheduleHomeBuild(); return; }   // BI1: nothing of the lock screen's calibration applies
            if (patchShotBmp == null && PatchShot.has(TjAccessibilityService.this, rotation(), shotSig())) loadStoredShot();
            showWithCause("restRecheck");
            maybeAutoCalibrate("the patch window settled");   // AW5
            restInfo("restRecheck rest=" + patchResting + " menuOpen=" + overlayShown);   // the diagnostic is written here (twice per new window), not on every menu open / close
        } catch (Throwable t) { Log.e(TAG, "rest recheck failed", t); }
    } };
    private PickerGuard pickerGuard;   // AO1
    private PickerGuard picker() { if (pickerGuard == null) pickerGuard = new PickerGuard(this, handler); return pickerGuard; }
    private RecentsFilter recentsGuard;   // AP1
    private RecentsFilter recents() { if (recentsGuard == null) recentsGuard = new RecentsFilter(this, handler); return recentsGuard; }
    String stockPackageOrNull() { return stockPkg(); }
    boolean keyguardLockedNow() { return keyguardLockedFast(); }
    /** AP1: the return reset (or another automation) is driving Recents itself right now: the guard stays out of its way. */
    boolean resetDrivesRecents() { return resetCover != null || System.currentTimeMillis() < guardPausedUntil; }

    /** AP1: presses "Close" on the first Recents card whose description starts with one of {@code blockedLower} (lower-case app labels); returns that label, or null. Main thread. */
    String closeOneRecentsCard(java.util.Set<String> blockedLower) {
        for (AccessibilityNodeInfo card : findTaskCards()) {
            CharSequence d = card.getContentDescription();
            String hit = RecentsFilter.matchingLabel(d == null ? null : d.toString(), blockedLower);
            if (hit == null) continue;
            for (AccessibilityNodeInfo.AccessibilityAction act : card.getActionList()) {
                if (act.getLabel() != null && "close".equalsIgnoreCase(act.getLabel().toString().trim()) && card.performAction(act.getId())) return hit;
            }
        }
        return null;
    }

    /** BE2: presses "Close" on EVERY Recents card whose description starts with one of {@code blockedLower} (lower-case app labels), all found in ONE pass over the tree; returns the labels closed. Main thread. */
    java.util.List<String> closeRecentsCards(java.util.Set<String> blockedLower) {
        java.util.List<String> done = new java.util.ArrayList<>();
        for (AccessibilityNodeInfo card : findTaskCards()) {
            CharSequence d = card.getContentDescription();
            String hit = RecentsFilter.matchingLabel(d == null ? null : d.toString(), blockedLower);
            if (hit == null) continue;
            for (AccessibilityNodeInfo.AccessibilityAction act : card.getActionList()) {
                if (act.getLabel() != null && "close".equalsIgnoreCase(act.getLabel().toString().trim()) && card.performAction(act.getId())) { done.add(hit); break; }
            }
        }
        return done;
    }

    /** AB2: is the Solitaire probe running? Read from the preferences at most once a second instead of on every event. */
    private long probeUntilCache = 0, probeCacheAt = -10_000;
    private boolean probeWanted() {
        long up = android.os.SystemClock.uptimeMillis();
        if (up - probeCacheAt > 1000) {
            probeCacheAt = up;
            try { probeUntilCache = Long.parseLong(Features.getString(this, FailsafeKeys.PROBE_UNTIL_KEY, "0")); } catch (NumberFormatException e) { probeUntilCache = 0; }
        }
        return probeUntilCache != 0;
    }

    private static final String CARD_KEY = "power_card_rect";

    private int rotation() { return ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation(); }
    private String cardKey() { return CARD_KEY + "_r" + rotation(); }   // the menu sits somewhere else in landscape

    private Rect cachedCard() {
        String v = Features.getString(this, cardKey(), "");
        String[] p = v.split(",");
        if (p.length != 4) return null;
        try { return new Rect(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])); }
        catch (NumberFormatException e) { return null; }
    }

    private boolean dialogWindowPresent(List<AccessibilityWindowInfo> windows) {
        if (windows == null) return false;
        for (AccessibilityWindowInfo w : windows) {
            CharSequence t = w.getTitle();
            if (t != null && t.toString().contains("ActionsDialog")) return true;
        }
        return false;
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        connectedAtMs = android.os.SystemClock.elapsedRealtime(); evSeen = 0; evNoise = 0; scanCount = 0; scanTotalMs = 0; scanMaxMs = 0; evSkipped = 0; penFull = 0; penFullMs = 0; penFullMax = 0; penFast = 0;   // AD8: the report's load numbers count from this connect
        try { warmUp(); } catch (Throwable t) { Log.e(TAG, "warm failed", t); }
        try { startLockNumber(); } catch (Throwable t) { Log.e(TAG, "lock number failed", t); }
        try { updateKeyFilter(); } catch (Throwable t) { Log.e(TAG, "key filter failed", t); }
        try { updateMotion(); } catch (Throwable t) { Log.e(TAG, "motion failed", t); }
        try { startTileGuard(); } catch (Throwable t) { Log.e(TAG, "tile guard failed", t); }
        try { startRadioGuard(); } catch (Throwable t) { Log.e(TAG, "radio guard failed", t); }
        try { if (keyguardLocked()) noteShade(shadeLevel(), "look at connect"); } catch (Throwable t) { Log.e(TAG, "shade look failed", t); }   // BY1 review MED1: a re-bound service starts with "closed"; the panel may be open (a dump, an update, a restart)
        try { startPatchWatch(); } catch (Throwable t) { Log.e(TAG, "patch watch failed", t); }
        try { DemoModeProfile.cleanupOnce(this); } catch (Throwable t) { Log.e(TAG, "demo cleanup failed", t); }   // AB1: the retired retail-demo option never stays switched on
        try { KeepAliveService.ensure(this); } catch (Throwable t) { Log.e(TAG, "keep-alive failed", t); }   // AA2: after a restart / update the helper reconnects first: bring the service back
        Log.i(TAG, "connected; power mode=" + Features.powerMode(this) + " custom=" + Mode.isCustom(this));
        LockLog.add(this, "SERVICE", "connected, mode " + Mode.get(this) + (CrashGuard.automationOff(this) ? " PAUSED" : ""));
        curtainPreloadAsync();   // BL1: the stored home curtain is read into memory now (a worker thread), so the cover at the next Enter tap never waits for a disk read
        handler.postDelayed(new Runnable() { @Override public void run() { reconcileAfterGap("the helper (re)connected"); } }, 1500);   // AC1: was the helper away at an unlock? (after the connect has settled)
        handler.postDelayed(new Runnable() { @Override public void run() { try { if (keyguardLocked() && !CrashGuard.automationOff(getApplicationContext())) lockQuietNow("the helper connected on the lock screen"); } catch (Throwable t) { Log.e(TAG, "connect lock rules failed", t); } } }, 2000);   // AW2 / AW3: the first lock screen after a restart (or after the helper was away) gets the same rules
        try { BatterySaverGuard.start(this); } catch (Throwable t) { Log.e(TAG, "battery saver guard failed", t); }
        try { LockQuiet.applyNotificationsAsync(this); } catch (Throwable t) { Log.e(TAG, "lock quiet failed", t); }   // AK1
        try { LockQuiet.forgetStalePrevAsync(this); } catch (Throwable t) { Log.e(TAG, "stale lock memory check failed", t); }   // review of BS2: a lock memory left over from an unlock this helper never saw (a boot unlock before it bound) must not put a radio back much later
        try { if (calibrating()) calibrationChanged(); } catch (Throwable t) { Log.e(TAG, "calibration resume failed", t); }   // AM1
        try { AnimationSpeedup.syncSoon(this, "helper connected"); } catch (Throwable t) { Log.e(TAG, "animation sync failed", t); }   // BF1: a Quickstep session that was already running when the helper (re)connected gets its animations off; the helper is what hears the screen go off
    }

    // ---------------------------------------------------------------- W3b: wallpaper patch under the replacement card (Tj: permanent, custom launcher only)
    public static final String PATCH_STATUS_KEY = "wallpaper_patch_status";
    public static final String PATCH_DX_KEY = "patch_dx", PATCH_DY_KEY = "patch_dy", PATCH_SCALE_KEY = "patch_scale_pct";   // alignment nudges in px / percent
    /** AC6: brightness nudge of the lock-screen patch in percent points (Launcher control). The wallpaper MODEL is drawn darker by {@link #DIM_KEYGUARD} because the real keyguard darkens its wallpaper. */
    public static final String PATCH_DIM_KEY = "patch_dim_pct";
    /** AC6: how much of its brightness the real lock screen keeps: MEASURED on the Android 10 emulator (run 74: (173,96,20) on screen vs (229,122,26) in the wallpaper = 0.755 / 0.787 / 0.77). A first estimate for the tablet's MtkSystemUI. */
    static final float DIM_KEYGUARD = 0.77f;
    /** AC6 / V5: how much brightness the screen around the open stock power menu keeps (the dialog's own scrim). MEASURED on the Android 10 emulator, run 136: the strip next to the menu keeps 0.791 / 0.791 / 0.789 of the closed lock screen. Our patch sits ABOVE that scrim, so it is dimmed the same. */
    static final float DIM_OPEN = 0.80f;   // AZ1c (Tj: "the darkened patch may be slightly too dark"): was 0.79. The AOSP source says the stock scrim is alpha 0.2 of a black-ish colour = exactly 0.80 (ScrimController.GRADIENT_SCRIM_ALPHA); emulator run 242 section v measured R 0.795 / G 0.800 / B 0.798
    /** The brightness factor of the lock-screen patch: the photo already has the keyguard's dim (only the dialog's scrim is missing), the wallpaper MODEL has neither. Tj's nudge (Launcher control) scales it. */
    private float lockPatchDim(boolean photo) {
        float open = openScrim();
        float k = (photo ? open : DIM_KEYGUARD * open) * (1f + (Features.getInt(this, PATCH_DIM_KEY, 0) + (photo ? 0 : PatchProfile.lockDimPct(android.os.Build.DEVICE))) / 100f);   // v0.7.6 AH3: + the per-device lift of the MODEL (not of a photo)
        return Math.max(0.3f, Math.min(1f, k));
    }
    private View patchView;
    private android.graphics.Bitmap wallpaperBmp;
    private boolean wallpaperFromDrawable = false;   // BY1a D1: WallpaperManager.getDrawable() ran: the platform keeps that bitmap in a process-wide cache until forgetLoadedWallpaper

    /** BY1a D1: drop our full-size wallpaper AND the platform's static copy of it (WallpaperManager.Globals keeps the bitmap until forgetLoadedWallpaper(); 1200 x 1024 x 4 = 4.9 MB, about 7 % of the tablet's PSS). The next model use decodes it again. */
    private void dropWallpaper() {
        boolean cached = wallpaperFromDrawable;
        wallpaperBmp = null; wallpaperFromDrawable = false;
        if (cached) { try { android.app.WallpaperManager.getInstance(this).forgetLoadedWallpaper(); } catch (Throwable ignored) { } }
    }
    /** V1: the LOCK-screen wallpaper (may differ from the home wallpaper), only held while the lock screen is up. */
    private android.graphics.Bitmap lockBmp;
    private boolean patchLockMode, targetIsLock;
    /** BI1: the cardless real-pixel patch (patchLockMode) is ALSO used on the unlocked custom home screen; these two say that this window / target is the home flavour (the pixels come from {@link HomeShot} + {@link HomeLayer}, the rest rules from {@link HomePatchRules}). Everything lock-specific stays keyed on the keyguard. */
    private boolean patchHomeMode, targetIsHome;
    /** AE3 / AD7b: the resting state of the lock-screen patch (see {@link PatchRest}): visible over the Screenshot slot while the lock screen is up and the menu is closed; the switch is in Launcher control, default OFF. */
    /** AH1: Tj's nudge (percent points) of how much the stock dialog's scrim darkens the screen around the OPEN menu; the patch is dimmed the same (lock and home). */
    public static final String PATCH_SCRIM_KEY = "patch_scrim_pct";
    private volatile boolean patchOpenHome = false;   // AH1: the HOME-screen patch is dimmed like the dialog's scrim only while the menu is open (closed = invisible seam)
    private float openScrim() { return PatchRest.openScrim(DIM_OPEN, Features.getInt(this, PATCH_SCRIM_KEY, 0)); }
    private volatile boolean patchResting = false;
    /**
     * AZ1 (Tj, 2026-11-06 20:40: "a slight flicker when I press the side key ... can the darkening to match the background be faster or fade"): the patch FADES to its open-menu brightness along the stock dialog's
     * own fade-in instead of jumping (see {@link PatchFade}); Control switch, default ON (OFF = the v0.8.0 jump).
     */
    public static final String PATCH_FADE_KEY = "patch_fade";
    /** AZ1: Tj's nudge of the fade timing in ms (+ = the patch darkens earlier, - = later): the tablet's timing may differ from the emulator's. */
    public static final String PATCH_FADE_SHIFT_KEY = "patch_fade_shift";
    private volatile long lastWinEventMs = 0, winBurstStartMs = 0, dialogEventMs = 0;   // AZ1: event time (uptime ms) of the newest window event, of the FIRST event of the current burst, and of the stock dialog's own window-state event: the stock fade began about then
    /** AZ1: events of one burst (a dialog being added raises several window events within a few ms) are anchored on the first one. */
    private void noteWindowEvent(long eventTime) {
        if (lastWinEventMs == 0 || eventTime - lastWinEventMs > 250 || eventTime < lastWinEventMs) winBurstStartMs = eventTime;
        lastWinEventMs = eventTime;
    }
    // BJ1 (Tj, 2026-11-08 16:21 UTC: on the HOME screen "the dimming speed makes the patch appear"): what the last fade-in really looked like from the patch's side, per frame. The film (section hf) sees it on the
    // emulator; the tablet report (perfLine) is how Tj's tablet tells us. A frame later than 34 ms after the one before is a missed vsync or more = the patch's brightness stood still while the stock scrim moved.
    private long fadeFrameLastMs = 0, fadeWorstGapMs = 0, fadeFirstDrawLateMs = -1;
    private int fadeFrames = 0, fadeSlowFrames = 0;
    private boolean fadeHomeFlavour = false;
    private String fadeSummary = "", fadeFirstSummary = "";
    private boolean fading = false;                    // AZ1: the open-menu brightness is still following the stock fade (main thread only)
    private long fadeStartMs = 0, fadeLagLast = -1, fadeLeadMs = PatchFade.LEAD_MS;    // AZ1: uptime ms at which the stock scrim began (estimated); the lag the last fade started with (the tablet report)
    // BC2 (Tj, 2026-11-07: "when the power menu is open, and I press in the middle of the screen to close it, or pull up from the bottom to type the pin, the wallpaper patch flickers for a split second"): the stock
    // dialog's dismissal fades its card and scrim OUT over 300 ms (LogAccelerate: slow start, fast end) and removes its window at the end; the v0.8.3 patch held its dark look until a node scan noticed that = a dark block.
    private boolean closing = false;                   // the patch's open look is following the stock fade-OUT (main thread only)
    private long closeStartMs = 0;                     // uptime ms at which the stock dismissal began (a sign of it we saw, or our own BACK); may lie a few ms ahead
    private volatile int dialogWindowId = -1;          // the accessibility window id of the newest stock dialog (from its window-state event): the event that removes that window ends the fade-out at once
    static final long CLOSE_SLACK_MS = 100, CLOSE_SAFETY_MS = 150;   // a scan may end the fade-out this long after its predicted end; the clock ends it (and verifies with a scan) this long after, whatever else happens
    private final Runnable closeFinishRun = new Runnable() { @Override public void run() { finishClose("the fade-out time is over"); } };
    // BC5 (Tj, 2026-11-07: "For the pull up for the pin, the patch should disappear immediately, as fast as possible after the pull up from the bottom"): the PIN pad came up while the stock menu was open: the patch is
    // hidden AT ONCE and stays hidden until that menu is gone (no fade-follow here; the stock card's Screenshot slot may show for the rest of its fade-out: Tj chose that over any flicker).
    private boolean pinHide = false;
    private long pinHiddenUntilMs = 0;                 // the resting patch stays away this long after such a hide: the PIN-pad scan (lastBouncer) is ~120 ms slower than the first sign
    // BE1 (Tj, 2026-11-07 20:17: "Every once in a while there is a very quick flicker of the wallpaper patch when I pull up to enter pin. And one time the screenshot button showed for a split second"):
    //  - the pull-up is announced by keyguard view events only AFTER the stock bouncer scrim has started to darken the wallpaper (the patch is a still picture of the PLAIN lock screen): the resting patch now also goes at the
    //    finger's DOWN (ACTION_OUTSIDE, delivered before any MOVE), the earliest sign there is;
    //  - a patch that is away (touch, PIN pad) is not "not resting": the stock dialog's own event brings it back at once (fastDecline used to refuse = the slow scan path = the Screenshot slot for 100-300 ms).
    private long touchAwayUntilMs = 0;                 // the resting patch is away because a finger went down on the lock screen
    private long lastTouchDownMs = 0;                  // uptime of that DOWN (for the lock log: how long before the PIN pad's first sign)
    private long lastPadSignMs = 0;                    // newest sign that the PIN pad is (still) up
    private volatile boolean patchRestable = false;    // the patch COULD rest right now (switch on, real pixels known, PIN pad not up), whatever the time guards say
    static final long TOUCH_AWAY_MS = 900;             // a finger went down: the resting patch stays away this long (a harmless tap costs nothing: the patch is invisible when it matches)
    static final long PAD_CALM_MS = 1200;              // the resting patch returns only this long after the newest sign of the PIN pad
    /**
     * BN3 (Tj, 2026-11-09 02:33 UTC, about v0.8.12: "the wallpaper patch on the lock screen shows up and flashes sometimes when I pull the pin pad up"): on the LOCK screen a finger that went down keeps the patch away for
     * this long (a harmless tap costs nothing: the stock dialog's own event brings an away patch back at once, the lock log says "menu opened while the patch was away"). A slow pull whose first PIN-pad sign comes late
     * must not find the patch back in front of the darkening wallpaper. The home flavour keeps {@link #TOUCH_AWAY_MS} (its own timers decide).
     */
    static final long LOCK_TOUCH_AWAY_MS = 1500;
    private String patchShowCause = "";                // BN3: who asks patchShow() for a RETURN (touchBack, padBack, restRecheck), for the lock log line "patch visible again"
    private long lastReturnLogMs = 0;
    private long lastPadLeftMs = 0;                    // BT1b: uptime of the PIN pad's last disappearance
    static final long PAD_LEFT_HOLD_MS = 1500;         // BT1b: long enough for the keyguard's exit (USER_PRESENT about 270 ms later, the layer refresh within 0.7 s of that), short enough that a cancelled pad gets the patch back quickly
    /**
     * BT1b (Tj, 2026-11-11: "the first time I unlock from the lock screen to the custom launcher the power menu sometimes flickers on and off even without me pressing the side key"). The PIN pad LEAVES about 270 ms before
     * USER_PRESENT (tablet report 2026-11-10-2); the keyguard is still exiting then (the lock screen content slides and brightens). Every pad sign pushes the resting patch's hold out by 2.5 s, but a long PIN entry lets
     * the hold run out, and the pad's disappearance itself called patchShow() with no hold at all: the lock patch (a block of the lock screen's wallpaper over the Screenshot slot) came back in front of the exit for the
     * 0.3-0.7 s until the layer refresh removed it = a ghost of the power menu's shape. The disappearance counts as a sign of the pad: the patch stays away {@link #PAD_LEFT_HOLD_MS} (a pad that was only CANCELLED gets it back then, as every
     * return since BE1 does; a power menu pressed meanwhile still shows it at the dialog's own event). Audit of the unlock path: research/BT1_ANALYSIS_2026-11-11.md.
     */
    private void padLeft() {
        long now = android.os.SystemClock.uptimeMillis();
        lastPadLeftMs = now;
        pinHiddenUntilMs = Math.max(pinHiddenUntilMs, now + PAD_LEFT_HOLD_MS);
        handler.removeCallbacks(padBack); handler.postDelayed(padBack, Math.max(PAD_LEFT_HOLD_MS, pinHiddenUntilMs - now) + 100);   // review B #1: never BEFORE the hold ends (a pad seen for under 0.9 s still holds from its last sign for 2.5 s)
        handler.removeCallbacks(padCancelCheck); handler.postDelayed(padCancelCheck, PatchRest.PAD_CANCEL_MS + 20);   // BY1c / F3: a CANCELLED pad ends the hold early (PatchRest.padCancelled)
        Log.i(TAG, "PATCH pin pad left t=" + now + ": the resting patch stays away " + (pinHiddenUntilMs - now) + " ms");
    }
    private volatile long lastUserPresentMs = 0;   // BY1c: uptime of the last USER_PRESENT (an unlock), for PatchRest.padCancelled
    private volatile boolean lockShadeOpen = false;   // BY1c F1: SystemUI's notification panel / quick settings are open on the lock screen (its pane title, LockShade); the lock patch stays away meanwhile
    private boolean shadeLookedDark = false;            // BY1 review LOW2: a look while the screen was dark read the panel since the last SCREEN_OFF
    private long lastShadeSeenMs = 0;                   // BY1 review LOW5: uptime the panel was last known open (a calibration screenshot around it is refused)
    private long lockShadeClosedMs = 0;                // BY1c F1: uptime of the last "Lock screen." pane title (the panel closed); LockShade.SETTLE_MS later the patch may rest again
    private String shadeClosedTitle = null;            // BY1c F1: SystemUI's own pane titles in the device's language (read once; LockShade has the English ones)
    private String[] shadeOpenTitles = null;
    /**
     * BY1c / F3 (emulator film menuP1, run #564): the PIN pad left, the screen is still locked and no unlock followed: the pad was cancelled, so its hold (2.5 s from the last sign) ends now and the resting patch is
     * back in front of the Screenshot slot before a power menu can open (a menu opened while the patch is away shows the stock entry until the dialog's own event, about two frames late). Re-looks while a pad sign
     * is too recent, gives up once the normal padBack would run anyway. A fresh look at the pad first, as padBack does.
     */
    private final Runnable padCancelCheck = new Runnable() { @Override public void run() {
        try {
            if (patchView == null || !patchLockMode || patchHomeMode || overlayShown || lastPadLeftMs == 0) return;
            long now = android.os.SystemClock.uptimeMillis();
            if (now - lastPadLeftMs > PAD_LEFT_HOLD_MS + 1000) return;   // the normal padBack has it by now
            long quiet = PatchRest.PAD_SIGN_QUIET_MS - (now - lastPadSignMs);
            if (quiet > 0) { handler.postDelayed(this, quiet + 20); return; }
            updateLockNumber();   // a FRESH look at the pad (a pad that is up again calls pinPadCameUp and keeps the patch away)
            now = android.os.SystemClock.uptimeMillis();
            if (!PatchRest.padCancelled(now, lastPadLeftMs, lastPadSignMs, lastBouncer, keyguardLocked(), lastUserPresentMs)) return;
            if (appOverKeyguard()) { Log.i(TAG, "PATCH pin pad left for an app shown over the lock screen t=" + now + ": not a cancel, the pad hold stays"); return; }   // BY1 review LOW4
            if (pinHiddenUntilMs > now) pinHiddenUntilMs = now;
            Log.i(TAG, "PATCH pin pad cancelled t=" + now + ": " + (now - lastPadLeftMs) + " ms after the pad left, still locked, no unlock: the pad hold ends now");
            showWithCause("padCancel");
        } catch (Throwable ignored) { }
    } };
    /** BY1 review LOW4 (2026-11-12): an app shown OVER the lock screen (the emergency dialer from the PIN pad, the lock-screen camera) leaves the keyguard "locked" but hides it. One window list, only when a pad cancel is about to be declared. */
    private boolean appOverKeyguard() {
        try {
            List<AccessibilityWindowInfo> ws = getWindows();
            if (ws != null) for (AccessibilityWindowInfo w : ws) if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive() || w.isFocused())) return true;
        } catch (Throwable ignored) { }
        return false;
    }
    private long restableSinceMs = 0;               // BT1a: uptime of the moment the LOCK patch became restable (0 = it is not); it rests visibly only after PatchRest.SETTLE_MS
    private final Runnable settleRun = new Runnable() { @Override public void run() { try { if (patchView != null && patchLockMode && !patchHomeMode && !overlayShown) { updateLockNumber(); showWithCause("settle"); } } catch (Throwable ignored) { } } };   // review B #1: a FRESH look at the PIN pad first (as touchBack and padBack do: lastBouncer is a scan result and can lag), then decide
    private final Runnable touchBack = new Runnable() { @Override public void run() {
        try {
            if (android.os.SystemClock.uptimeMillis() < touchAwayUntilMs) return;
            if (!patchHomeMode) updateLockNumber();   // BN3: a FRESH look at the PIN pad first (as padBack does): a pad that is up, or being pulled up, calls pinPadCameUp and keeps the patch away
            if (!patchHomeMode && patchLockMode && patchView != null && patchView.getAlpha() < 0.5f) noteShade(shadeLevel(), "look");   // BY1c F1: a finger may have opened or closed the panel without an event we saw
            showWithCause("touchBack");
        } catch (Throwable ignored) { }
    } };
    private final Runnable padBack = new Runnable() { @Override public void run() {
        try {
            long now = android.os.SystemClock.uptimeMillis();
            long wait = lastPadSignMs + PAD_CALM_MS - now;
            if (wait > 0) { handler.postDelayed(this, wait); return; }                  // a newer sign of the pad: wait for calm
            updateLockNumber();                                                          // a FRESH look at the pad (lastBouncer is a scan result and can be stale), then decide
            showWithCause("padBack");
        } catch (Throwable ignored) { }
    } };

    /**
     * BN3: the lock patch went from invisible to VISIBLE (a return after a finger, a PIN pad, a menu). No path wrote this down before, so a flash Tj saw could not be matched to the path that caused it. Logcat always; the
     * persistent lock log (it is in the tablet report) only when a finger or a PIN-pad sign was less than 4 s ago, at most one line per 700 ms.
     */
    /** patchShow with the path that asked for it, for {@link #noteReturn}; the cause never outlives the call (a stale one would name the wrong path for a later return). */
    private void showWithCause(String cause) {
        patchShowCause = cause;
        try { patchShow(overlayShown); } finally { patchShowCause = ""; }
    }

    private void noteReturn() {
        long now = android.os.SystemClock.uptimeMillis();
        String cause = patchShowCause.isEmpty() ? "other" : patchShowCause;
        long f = lastTouchDownMs > 0 ? now - lastTouchDownMs : -1, p = lastPadSignMs > 0 ? now - lastPadSignMs : -1;
        String what = "patch visible again (" + cause + "), " + (f >= 0 && f < 60_000 ? f + " ms after the finger" : "no finger lately") + ", " + (p >= 0 && p < 60_000 ? p + " ms after the last PIN pad sign" : "no PIN pad sign lately") + ", pad scan " + (lastBouncer ? "says UP" : "says gone");
        Log.i(TAG, "PATCH " + what);
        long pl = lastPadLeftMs > 0 ? now - lastPadLeftMs : -1;   // BT1b: a patch that comes back right after the pad left is the unlock-exit ghost's signature
        if (!patchHomeMode && ((f >= 0 && f < 4000) || (p >= 0 && p < 4000) || (pl >= 0 && pl < 5000)) && now - lastReturnLogMs > 700) { lastReturnLogMs = now; logPatch(what + (pl >= 0 && pl < 5000 ? ", the PIN pad left " + pl + " ms ago, keyguard " + (keyguardLockedFast() ? "still locked" : "unlocked") : "")); }
    }
    private boolean patchOpenFlag = false;             // AZ1: the last patchShow() call said "menu open"
    private long scanNotBeforeMs = 0;   // AZ1: the node scan of a freshly opened menu waits for the fade
    private boolean patchFadeOn() { return Features.get(this, PATCH_FADE_KEY, true); }
    private int patchRestTop = 0;   // where the RESTING patch starts to paint, in the patch window's coordinates (the bottom of our card = the top of the Screenshot slot)
    /** AM1 (Tj, 2026-11-06): switch, default OFF: draw my own Power off / Restart card over the stock entries (the old cover). OFF = the stock menu stays completely stock; the patch alone hides the Screenshot spot. */
    public static final String POWER_CARD_KEY = "power_card";
    private android.graphics.Bitmap patchShotBmp; private int patchShotLeft, patchShotTop;   // AM1: the stored REAL-pixel crop of the lock screen for this rotation, and where its top-left corner lies on the screen
    private boolean patchTouchable = false;                                                    // AM1: only while the stock menu is open, the patch swallows a tap on the hidden Screenshot spot
    private android.database.ContentObserver shotObserver;
    private long lastShotId = -1;
    private int autoShotTries = 0;
    private boolean autoShotDone = false;
    private android.graphics.Bitmap patchMaskOpen, patchMaskRest;   // AH3: the soft-edge masks of the slot patch (menu open: hard top under the card; resting: soft top); null = the old hard-edged drawing
    /** What kind of wallpaper the lock screen shows ("still picture" / "LIVE wallpaper <component>"): an animated wallpaper cannot be matched exactly by a still patch. Shown in Launcher control and in the tablet report. */
    public static final String WALLPAPER_KIND_KEY = "wallpaper_kind";
    /**
     * AM1: cardless only on the LOCK screen, where the real-pixel patch hides the Screenshot spot. The HOME screen has no patch in "hide_screenshot" mode (the card stretched over the whole panel is what hides the
     * Screenshot entry there), and the "wallpaper" mode IS "patch + my card": both keep the card, as before. The switch forces the card everywhere.
     */
    private boolean cardWanted() {
        if (Features.get(this, POWER_CARD_KEY, false)) return true;
        if (Features.POWER_WALLPAPER.equals(Features.powerMode(this))) return true;
        if (keyguardLockedFast()) return false;
        return !homePatchReady();   // BI1: the home screen is cardless too, but only while the real-pixel patch is ready (fresh, restable); anything else keeps today's replacement panel
    }
    /** Debug builds only (the emulator tests): the resting patch may also rest on the wallpaper MODEL, which is otherwise forbidden because a model patch is a visible block. */
    private boolean modelRestAllowed() { return (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 && Features.get(this, "patch_rest_model", false); }
    private boolean calibrating() { return PatchShot.calibrating(Features.getString(this, PatchShot.CAL_UNTIL_KEY, "0"), System.currentTimeMillis()); }
    /**
     * AM1: the patch RESTS over the Screenshot spot (switch default ON) only when it can be INVISIBLE: it needs real pixels of this lock screen (a stored crop from a real screenshot, or the helper's fresh photo),
     * and never while a calibration screenshot is being taken. Without them it stays hidden at rest (a model patch is a visible block: Tj's photo of 2026-11-06).
     */
    private String lastRestInfo = "";
    /** AM1: the inputs of the resting decision, newest only, kept in a preference (Launcher control / tablet report / emulator INFO): why does the patch rest, or not? */
    private void restInfo(String what) {
        try {
            String v = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  " + what + " [always=" + lockPatchAlways() + " switch=" + Features.get(this, PatchRest.KEY, true) + " wanted=" + lockPatchWanted() + " calibrating=" + calibrating() + " shot=" + (patchShotBmp != null) + " bouncer=" + lastBouncer + "]";
            String key = v.substring(10);
            if (!key.equals(lastRestInfo)) { lastRestInfo = key; Features.setStringAsync(this, "patch_rest_info", v); }
        } catch (Throwable ignored) { }
    }
    private boolean lockPatchAlways() { return Features.get(this, PatchRest.KEY, true) && Features.get(this, LOCK_PATCH_KEY, true) && Features.isCover(Features.powerMode(this)) && keyguardLocked() && !calibrating() && (patchShotBmp != null || lockShot != null || modelRestAllowed()); }   // AM1 (run 218): a FRESH keyguard read, not the 250 ms cache: a stale 'unlocked' left the new window hidden until the menu opened
    /** AE3: brightness of the RESTING patch: no dialog scrim (the menu is closed), so the photo path keeps its own keyguard dim (k = 1) and the wallpaper MODEL gets only the keyguard's dim; Tj's brightness nudge still applies. */
    private float restDim(boolean photo) {
        if (photo) return 1f;   // v0.7.6 (review of v0.7.5): the photo already has the real keyguard brightness and no dialog scrim is open: exactly 1, whatever the brightness nudge says (it corrects the MODEL's keyguard dim only)
        float k = DIM_KEYGUARD * (1f + (Features.getInt(this, PATCH_DIM_KEY, 0) + PatchProfile.lockDimPct(android.os.Build.DEVICE)) / 100f);   // v0.7.6 AH3: + the per-device lift of the MODEL
        return Math.max(0.3f, Math.min(1f, k));
    }
    public static final String LOCK_PATCH_KEY = "lock_patch";   // Tj V1: wallpaper patch + short card on the lock screen (default ON; needs the wallpaper permission)
    private long lastOffTop = -1, lastItemH = -1;
    private int lastPitch = -1;   // AC6: distance between the tops of neighbouring menu entries (Power off -> Restart), measured on every scan
    private int lastCut = -1;     // AC6: where the Screenshot entry begins (its top), measured on every scan
    // V4: auto-calibration of the lock-screen patch against a real screenshot (shell helper). Final lock-patch scale = lockCalM * (1 + Tj's nudge %); shifts add.
    public static final String LOCK_CAL_KEY = "lock_cal";
    public static final String CAL_STATUS_KEY = "lock_cal_status";
    private float lockCalM = 1f; private int lockCalDx = 0, lockCalDy = 0; private String lockCalSig = "";
    private volatile boolean calBusy = false;
    /** V4, second design (run 74): the keyguard darkens the wallpaper by about 23 %, so no model of the wallpaper can match the lock screen. The patch is therefore a PHOTO of the real lock screen
     *  (shell-helper screenshot with the menu closed, cropped to the patch rectangle): exact by construction (dimming, position, zoom). The wallpaper model below stays as the fallback until the first photo exists. */
    private android.graphics.Bitmap lockShot; private int lockShotDx, lockShotDy; private volatile boolean lockShotFresh = false; private long lastShotTry = 0;
    private final Runnable calTrigger = new Runnable() { @Override public void run() { try { maybeCalibrateLock(); } catch (Throwable t) { Log.e(TAG, "calibrate trigger failed", t); } } };
    private float testSkew() { return 1f + Features.getInt(this, "patch_test_skew", 0) / 100f; }   // emulator test only: a deliberately wrong model the calibration must correct
    private String itemKey() { return "power_geo_r" + rotation(); }   // AC6 (v0.7.3): "pitch/cut" of the stock panel; the old "power_item_r<N>" ("top,height") is no longer read or written
    private final Rect patchAt = new Rect();
    private int patchRot = -1;
    private boolean patchWatching = false;
    private final Runnable layerRefresh = new Runnable() { @Override public void run() { layerRefreshPending = false; refreshLayers(); } };
    private boolean layerRefreshPending = false;
    private long lastLayerMs = 0;
    private final Runnable patchRedraw = new Runnable() { @Override public void run() { if (patchView != null) patchView.invalidate(); } };
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefWatch = new android.content.SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override public void onSharedPreferenceChanged(android.content.SharedPreferences sp, String key) {
            if ("mode".equals(key) || Features.POWER_MODE.equals(key)) { handler.post(layerRefresh); handler.post(lockEval); }
            if ("mode".equals(key) || Features.POWER_MODE.equals(key) || HomePatchRules.KEY.equals(key)) handler.post(new Runnable() { @Override public void run() { homeCalInterrupted("the mode or the switch changed"); } });   // BI1: a calibration needs the custom home screen, the switch on, hide-Screenshot mode, until its end
            if (PatchShot.CAL_UNTIL_KEY.equals(key)) handler.post(new Runnable() { @Override public void run() { try { calibrationChanged(); } catch (Throwable t) { Log.e(TAG, "calibration change failed", t); } } });   // AM1
            if (POWER_CARD_KEY.equals(key)) handler.post(new Runnable() { @Override public void run() { try { removeOverlay(); refreshLayers(); } catch (Throwable t) { Log.e(TAG, "card switch failed", t); } } });   // AM1: the card appears / disappears now
            if (LockQuiet.KEY.equals(key)) { try { LockQuiet.applyNotificationsAsync(getApplicationContext()); } catch (Throwable ignored) { } }   // AK1: the switch changed: hide / put back the lock-screen notification setting now
            if (PatchRest.KEY.equals(key) || LOCK_PATCH_KEY.equals(key) || PATCH_DIM_KEY.equals(key)) handler.post(new Runnable() { @Override public void run() { layerRefresh.run(); patchShow(overlayShown); } });   // AE3: the always-on switch / dim nudge changed: apply it now
            if ("mode".equals(key)) handler.post(new Runnable() { @Override public void run() { updateHotspot(); } });
            if (key != null && (key.startsWith("unlock_m_") || key.startsWith("return_m_") || "mode".equals(key))) handler.post(new Runnable() { @Override public void run() { updateMotion(); } });
            if (key != null && (key.startsWith("unlock_m_") || key.startsWith("return_m_") || "mode".equals(key) || FailsafeKeys.KONAMI_KEY.equals(key))) handler.post(new Runnable() { @Override public void run() { updateKeyFilter(); } });
            if (FailsafeKeys.HOTSPOT_KEY.equals(key) || FailsafeKeys.CORNER_KEY.equals(key)) handler.post(new Runnable() { @Override public void run() { removeHotspot(); updateHotspot(); } });
        }
    };
    private final android.content.BroadcastReceiver wallpaperReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, android.content.Intent i) { dropWallpaper(); lockBmp = null; lockCalSig = ""; lockShot = null; lockShotFresh = false; homeWallId = -1; homeWallBmp = null; homeBuiltAtMs = 0; handler.post(new Runnable() { @Override public void run() { removeOverlay(); removePatch(); refreshLayers(); } }); }
    };

    /**
     * BC2: the stock power dialog dismisses itself on ACTION_CLOSE_SYSTEM_DIALOGS (the Home key, the assistant, a swipe that ends in the Home gesture ...) unless the reason is "recentapps" (its own receiver, Android 10
     * GlobalActionsDialog); "globalactions" is the power menu's own opening. The same broadcast reaches us within milliseconds: it is the start of the stock fade-out when nothing else (a click, our own BACK) told us.
     */
    private final android.content.BroadcastReceiver closeDialogsReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, android.content.Intent i) {
            try {
                String reason = i == null ? null : i.getStringExtra("reason");
                if ("recentapps".equals(reason) || "globalactions".equals(reason)) return;
                if (overlayShown) menuClosedAtMs = android.os.SystemClock.uptimeMillis();   // BN2: the menu we covered is being dismissed: its window lingers while it fades out (see evaluateScan)
                startClose(android.os.SystemClock.uptimeMillis(), "close system dialogs (" + reason + ")");
            } catch (Throwable t) { Log.e(TAG, "close dialogs receiver failed", t); }
        }
    };

    private void startPatchWatch() {
        if (patchWatching) return;
        getSharedPreferences("tj_mode", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefWatch);
        getSharedPreferences("tj_features", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefWatch);
        registerReceiver(wallpaperReceiver, new android.content.IntentFilter(android.content.Intent.ACTION_WALLPAPER_CHANGED));
        try { registerReceiver(closeDialogsReceiver, new android.content.IntentFilter(android.content.Intent.ACTION_CLOSE_SYSTEM_DIALOGS)); } catch (Throwable t) { Log.e(TAG, "close dialogs receiver not registered", t); }   // BC2
        patchWatching = true;
        refreshLayers();
    }

    private void stopPatchWatch() {
        if (!patchWatching) return;
        try { getSharedPreferences("tj_mode", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefWatch); } catch (Throwable ignored) { }
        try { getSharedPreferences("tj_features", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefWatch); } catch (Throwable ignored) { }
        try { unregisterReceiver(wallpaperReceiver); } catch (Throwable ignored) { }
        try { unregisterReceiver(closeDialogsReceiver); } catch (Throwable ignored) { }   // BC2
        patchWatching = false;
    }

    /** Called by the launcher (state changes, resume, pause): re-evaluate the patch at once. */
    public void refreshLayersSoon() { handler.removeCallbacks(layerRefresh); handler.post(layerRefresh); }

    /** Called by the launcher (same process) whenever it moves the wallpaper, on the wallpaper interpolator's BACKGROUND looper, not the main thread (so it only posts): redraw the patch with the new slice (debounced) and look again when the page position (the step) changed. */
    public void onWallpaperOffset() {
        handler.removeCallbacks(patchRedraw); handler.postDelayed(patchRedraw, 40);
        // BI1 (emulator run #346, phase C): a NEW wallpaper position is a different crop. After a restart TjHooks.wallpaperX is 0.5 until the Launcher reports its real offset (0 with one page) a moment after the first
        // evaluation, and that first look judged "no crop for step 500" and nothing looked again. Level-triggered: whenever the reported step changes, look again once it has settled (a page scroll = one look at its end).
        int st = homeStepNow();
        if (st != homeOffsetStep) { homeOffsetStep = st; handler.removeCallbacks(homeOffsetSettled); handler.postDelayed(homeOffsetSettled, 200); }
    }
    private volatile int homeOffsetStep = -1;
    private final Runnable homeOffsetSettled = new Runnable() { @Override public void run() { try { if (homeSwitchOn()) refreshLayersSoon(); } catch (Throwable t) { Log.e(TAG, "home offset refresh failed", t); } } };

    private void scheduleLayerRefresh() {
        if (!Features.POWER_WALLPAPER.equals(Features.powerMode(this)) && patchView == null && !lockPatchWanted()) return;   // not in this mode and nothing to clean up: free
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastLayerMs >= 150) { lastLayerMs = now; refreshLayers(); }
        if (!layerRefreshPending) { layerRefreshPending = true; handler.postDelayed(layerRefresh, 250); }
    }

    /** AD7a: see {@link PowerMenuRule#coverWorld}. Cheap: the keyguard answer is cached for 250 ms. */
    private boolean coverWorld() { return PowerMenuRule.coverWorld(Mode.isCustom(this), keyguardLockedFast()); }

    private void patchStatus(String s) {
        if (!s.equals(Features.getString(this, PATCH_STATUS_KEY, ""))) Features.setString(this, PATCH_STATUS_KEY, s);
    }

    /** All the reasons NOT to cover the system menu with a wallpaper patch; null = go. Each reason is shown in Launcher control. */
    private boolean lockPatchWanted() {
        return Features.get(this, LOCK_PATCH_KEY, true) && Features.isCover(Features.powerMode(this)) && keyguardLockedFast();   // AD7a: any lock screen, whatever the mode flag says
    }

    // ================================================================ BI1: the real-pixel patch on the UNLOCKED HOME screen (research/HOME_PATCH_2026-11-08.md)
    // Tj, 2026-11-08: "For the custom launcher, do the same logic and wallpaper patch that you did for the lock screen, but implement it for the unlocked home screen." The window, its masks, the open / close fade,
    // the tap on the hidden spot and the scrim are the lock screen's (patchLockMode); only the pixels (a calibrated wallpaper-only crop + the Launcher's own pixels, composed) and the rest rules are new.

    public static final String HOME_STATUS_KEY = "home_patch_status";
    private android.graphics.Bitmap homeWallBmp;                 // the wallpaper-only crop for the current offset step (decoded)
    private int homeWallLeft, homeWallTop, homeWallStep = -1;
    private long homeBuiltAtMs = 0;                               // uptime of the REQUEST of the composite that patchShotBmp holds; 0 = none
    private int homeBuiltStep = -1;
    private boolean homeBuilding = false;
    private int homeBuildFails = 0;
    private long homeWallId = -1;                                 // the system wallpaper's id, cached until the wallpaper changes
    private boolean homeNeedsCal = false;
    private String homeLastStatus = "";
    private boolean homeCalRunning = false;
    private long homeCalAskedWall = 0, homeCalLastStartUp = 0;
    private int homeCalStep = 0;
    private Rect homeCalWin;
    private View homeCalGuard;

    boolean homeCalRunning() { return homeCalRunning; }
    private boolean homeSwitchOn() { return Features.get(this, HomePatchRules.KEY, HomePatchRules.defaultOn(debuggable())); }
    private void homeStatus(String s) {
        if (s == null || s.equals(homeLastStatus)) return;
        homeLastStatus = s;
        try { Features.setStringAsync(this, HOME_STATUS_KEY, now() + "  " + s); } catch (Throwable ignored) { }
    }

    private long homeWallpaperId() {
        if (homeWallId >= 0) return homeWallId;
        long id = 0;
        try { id = android.app.WallpaperManager.getInstance(this).getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM); } catch (Throwable ignored) { }
        homeWallId = id < 0 ? 0 : id;   // cached whatever it is (an error or "no wallpaper" must not cost a binder call on every layer refresh); the wallpaper receiver clears it
        return homeWallId;
    }
    private android.util.DisplayMetrics realMetrics() {
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        return dm;
    }
    private String homeSig() { android.util.DisplayMetrics dm = realMetrics(); return HomePatchRules.sig(homeWallpaperId(), rotation(), dm.widthPixels, dm.heightPixels); }
    private int homeStepNow() { return HomePatchRules.step(TjHooks.wallpaperX); }
    private static long since(long upMs) { return upMs <= 0 ? Long.MAX_VALUE : Math.max(0, android.os.SystemClock.uptimeMillis() - upMs); }
    private long homeLastTouchMs() { return Math.max(TjHooks.lastHomeTouchMs, lastTouchDownMs); }

    /** The facts of the home patch right now. Main thread. */
    private HomePatchRules.Facts homeFacts() {
        HomePatchRules.Facts f = new HomePatchRules.Facts();
        f.switchOn = homeSwitchOn();
        f.coverMode = Features.POWER_HIDE_SCREENSHOT.equals(Features.powerMode(this));
        if (!f.switchOn || !f.coverMode) return f;   // nothing else is asked (no binder call) when the feature is off
        f.automationOff = CrashGuard.automationOff(this);
        f.customMode = Mode.isCustom(this);
        f.unlocked = !keyguardLockedFast();
        f.screenOn = isScreenOn();
        f.homeShowing = TjHooks.homeScreenShowing();
        if (f.homeShowing) { f.floatingViewOpen = TjHooks.homeFloatingViewOpen(); f.busy = TjHooks.homeBusy(); }
        f.calibrating = homeCalRunning;
        f.rectKnown = cachedCard() != null;
        f.cropValid = f.rectKnown && TjHooks.wallpaperReported && homeCropValid(homeStepNow(), homeSig());   // BI1: before the Launcher reports its wallpaper offset the step is only a placeholder (a crop looked up under it is for the wrong page position)
        return f;
    }

    /** The home patch's window rectangle (the lock screen's slot window: the stock panel is the same dialog), or null with the reason in {@link #HOME_STATUS_KEY}. */
    private Rect homePatchTarget() {
        homeNeedsCal = false;
        HomePatchRules.Facts f = homeFacts();
        if (!f.switchOn || !f.coverMode) return null;
        String why = HomePatchRules.whyNot(f);
        if (!why.isEmpty()) {
            if (f.customMode && f.unlocked && f.homeShowing && f.rectKnown && !f.cropValid && !f.calibrating) homeNeedsCal = true;
            homeStatus("away: " + why);
            return null;
        }
        Rect r = cachedCard();
        homeStatus("home screen: patch window wanted");
        return patchRect(r, true);
    }

    /** The cardless path may be used on the home screen: the patch exists, its pixels are fresh, and nothing about the calibration is running. */
    private boolean homePatchReady() { return patchView != null && patchHomeMode && patchRestable && !homeCalRunning; }

    /** The built patch is still what the home screen shows there (built after the last draw of the Launcher, for the current wallpaper position). Says nothing about whether it may be SEEN now. */
    private boolean homeCompositeFresh() {
        if (!patchHomeMode || patchShotBmp == null) return false;
        return HomePatchRules.compositeFresh(homeBuiltAtMs, TjHooks.lastHomeDrawMs, HomePatchRules.sameStep(homeBuiltStep, homeStepNow()));
    }

    /** The patch may rest over the slot: its pixels are fresh AND nothing else is on top of the home screen. */
    private boolean homeRestable() {
        if (!homeCompositeFresh() || homeCalRunning || !TjHooks.homeScreenShowing()) return false;
        if (android.os.SystemClock.uptimeMillis() < homeForeignUntilMs) return false;   // the shade, quick settings, the volume panel or a system dialog was just up (see homeForeignWindow)
        com.android.launcher3.Launcher l = TjHooks.launcher();
        if (l == null || !l.hasWindowFocus()) return false;                              // another window has the focus (the shade still open, a dialog): the patch must not paint over it
        return true;
    }

    private long homeForeignUntilMs = 0;
    private volatile int homeForeignCount = 0; private volatile long homeForeignLastAtMs = 0; private volatile String homeForeignNames = "", homeForeignLast = "";
    private boolean homeOpenShown = false;   // the patch was restable when the menu opened (decided ONCE at the opening, so a draw during the open menu cannot take it away)
    private long homeRefreshAskedMs = 0;
    private int homeCropGen = -1; private String homeCropKey = ""; private boolean homeCropOk = false;
    private volatile int homeCalGen = 0;
    private int homeBuildWaits = 0;
    private static final long HOME_FOREIGN_AWAY_MS = 2500;

    /**
     * A window of someone else appeared or changed (the notification shade, quick settings, the volume panel, a system dialog): the home patch is a copy of the Launcher's pixels and must not paint over it. It
     * goes away at once and comes back only after {@link #HOME_FOREIGN_AWAY_MS} AND when the Launcher has the window focus again (the shade can stay open for as long as it likes). The stock power menu's own window
     * is NOT one of these: its event is handled first, by the fast open path.
     */
    private void homeForeignWindow(AccessibilityEvent e) {
        if (patchView == null || !patchHomeMode || overlayShown) return;
        // ONLY a window that announces itself: TYPE_WINDOWS_CHANGED carries no package and no class, so it cannot be told from the stock power menu's own burst, from our own patch window being added or from the
        // Launcher's popup. Emulator run #347 (section hp): it took the patch away at the FIRST event of every power-menu burst (the dialog's own event, 19 ms later, found "the patch is not resting" and the card
        // came: HP4) and during the Launcher's own long-press popup (no 'stale', no rebuild: HP7). The shade and the volume panel announce themselves with a named TYPE_WINDOW_STATE_CHANGED; the Launcher's loss of
        // focus (homeRestable) is the second guard.
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pk = e.getPackageName(), cn = e.getClassName();
        if (pk == null) return;
        if (getPackageName().contentEquals(pk)) return;                                  // our own windows (the patch itself, the cards, the Launcher)
        if (cn != null && cn.toString().contains("ActionsDialog")) return;               // the stock power menu
        final String foreignName = pk + " " + cn;
        // BN2 (Tj, 2026-11-09 03:02 UTC, tablet report): the keyguard's own window (SystemUI's status-bar window, a plain FrameLayout) announces itself while the keyguard GOES AWAY, so every plain unlock started the 2.5 s
        // guard below and a side-key press within 2.5 s of the unlock found "the patch is not resting" (the composite was fresh 0.35 s earlier): the stock Screenshot entry showed. Such an event right after the keyguard was
        // up starts the guard as ever, but PROVISIONALLY: 400 ms later the Launcher's window focus decides (the shade takes the focus, the keyguard's exit does not) and the guard is lifted if the Launcher still has it.
        // Nothing can paint over a real window meanwhile (the guard holds); a Dialog (volume panel, power menu) is never judged this way.
        final boolean provisional = HomePatchRules.foreignTreatment(HomePatchRules.isKeyguardWindow(pk.toString(), cn == null ? "" : cn.toString()), since(lastKeyguardUpMs)) == HomePatchRules.FOREIGN_UNLOCK_CHECK;
        homeForeignApply(foreignName, provisional);
        handler.removeCallbacks(homeForeignRecheck);
        if (provisional) handler.postDelayed(homeForeignRecheck, HomePatchRules.FOREIGN_RECHECK_MS);   // a newer event re-arms it; a final guard (above) cancels it: nothing may lift a guard another window set
    }

    private String homeForeignPendingName = "";
    private long homeForeignProvisionalUntilMs = 0;   // the guard end a PROVISIONAL event set (0 = the guard is final)
    private volatile int homeForeignUnlockIgnored = 0;
    /**
     * BN2: the verdict on a provisional guard, every {@link HomePatchRules#FOREIGN_RECHECK_MS} until the guard's own end: the Launcher has the window focus = it was the keyguard going away (the guard is lifted, the patch
     * comes back at once); still no focus = a real window over the home, or a Launcher that is slow to get the focus back after the unlock (look again); at the guard's end: a real window, counted now.
     */
    private final Runnable homeForeignRecheck = new Runnable() { @Override public void run() {
        try {
            if (patchView == null || !patchHomeMode || overlayShown) return;   // (the guard simply runs its course)
            com.android.launcher3.Launcher l = TjHooks.launcher();
            int verdict = HomePatchRules.recheck(l != null && l.hasWindowFocus(), homeForeignUntilMs, homeForeignProvisionalUntilMs);
            if (verdict == HomePatchRules.RECHECK_NOT_OURS) return;
            if (verdict == HomePatchRules.RECHECK_LIFT) { homeForeignLift(); return; }
            if (HomePatchRules.recheckAgain(android.os.SystemClock.uptimeMillis(), homeForeignProvisionalUntilMs)) { handler.postDelayed(this, HomePatchRules.FOREIGN_RECHECK_MS); return; }
            homeForeignProvisionalUntilMs = 0;
            homeForeignCounted(homeForeignPendingName);
            Log.i(TAG, "HOMEPATCH away: another window (" + homeForeignPendingName + ")");
        } catch (Throwable t) { Log.e(TAG, "home foreign recheck failed", t); }
    } };

    /** BN2: a provisional guard ends early: the Launcher has the focus (the keyguard went away). The patch is looked at again at once (it comes back as soon as its own timers allow). */
    private void homeForeignLift() {
        homeForeignProvisionalUntilMs = 0; homeForeignUntilMs = 0; homeForeignUnlockIgnored++;
        Log.i(TAG, "HOMEPATCH unlock window ignored: the Launcher has the focus (the keyguard went away), guard lifted");
        patchShow(false);   // (the guard's own homeShowCheck stays posted: a harmless second look)
    }

    private void homeForeignCounted(String foreignName) {
        // for the tablet report (BI1): how often does a foreign window take the patch away on THIS tablet, and which ones? (gate runs #356 / #358: a named window a few seconds after the patch first rested made the menu use the card)
        homeForeignCount++; homeForeignLast = foreignName; homeForeignLastAtMs = android.os.SystemClock.uptimeMillis();
        if (homeForeignNames.length() < 260 && !homeForeignNames.contains(foreignName)) homeForeignNames = homeForeignNames.isEmpty() ? foreignName : homeForeignNames + "; " + foreignName;
    }

    /** A window is over the home (or may be): keep the patch away for {@link #HOME_FOREIGN_AWAY_MS}. {@code provisional}: only a possible one (the keyguard going away), counted and logged by the recheck. */
    private void homeForeignApply(String foreignName, boolean provisional) {
        homeForeignPendingName = foreignName;
        if (!provisional) homeForeignCounted(foreignName);
        homeForeignUntilMs = android.os.SystemClock.uptimeMillis() + HOME_FOREIGN_AWAY_MS;
        homeForeignProvisionalUntilMs = provisional ? homeForeignUntilMs : 0;
        if (patchView != null && (patchRestable || patchResting)) {
            patchRestable = false; patchResting = false; patchView.setAlpha(0f);
            Log.i(TAG, provisional ? "HOMEPATCH away (checking): SystemUI window " + foreignName + ", " + since(lastKeyguardUpMs) + " ms after the keyguard was up" : "HOMEPATCH away: another window (" + foreignName + ")");
        }
        handler.removeCallbacks(homeShowCheck); handler.postDelayed(homeShowCheck, HOME_FOREIGN_AWAY_MS + 40);
    }

    /** TjHooks: the Launcher's window got or lost the focus. Regained: the patch may come back (after a shade, a dialog, the power menu). Lost: nothing here (the dialog's own event must find the patch ready). */
    void homeFocusChanged(boolean has) {
        try {
            long up = android.os.SystemClock.uptimeMillis();
            if (!has) { homeFocusLostAtMs = up; return; }   // BT1c: when the Launcher lost the focus (the shade, a dialog, the stock power menu itself): homeOpenTreatment tells those apart
            if (patchView != null && patchHomeMode && !overlayShown) {
                if (HomePatchRules.recheck(true, homeForeignUntilMs, homeForeignProvisionalUntilMs) == HomePatchRules.RECHECK_LIFT) { handler.removeCallbacks(homeForeignRecheck); homeForeignLift(); }   // BN2: the focus is back while the guard was only provisional: the keyguard is gone
                else if (HomePatchRules.guardEndsWithFocus(homeFocusLostAtMs, homeForeignUntilMs, HOME_FOREIGN_AWAY_MS, up)) {   // BT1c: the window that took the focus (the shade) is gone: its 2.5 s guard has nothing left to guard
                    handler.removeCallbacks(homeForeignRecheck);
                    homeForeignProvisionalUntilMs = 0; homeForeignUntilMs = 0;
                    Log.i(TAG, "HOMEPATCH guard lifted: the Launcher has the focus again (lost " + (up - homeFocusLostAtMs) + " ms ago)");
                    patchShow(false);
                } else patchShow(false);
                scheduleHomeBuild();
            }
        } catch (Throwable t) { Log.e(TAG, "home focus failed", t); }
    }

    private long homeFocusLostAtMs = 0;       // uptime of the Launcher window's last loss of the focus
    private boolean homeOpenStale = false;    // BT1c: THIS opening of the stock menu shows a patch that was not fresh (decided once, in homeOpenDecide)
    private String homeOpenLogLine = "";

    /** BT1c: the facts of this moment for {@link HomePatchRules#openTreatment}. Main thread. */
    private int homeOpenTreatment() {
        HomePatchRules.OpenFacts f = new HomePatchRules.OpenFacts();
        com.android.launcher3.Launcher l = TjHooks.launcher();
        long now = android.os.SystemClock.uptimeMillis();
        f.patchWindow = patchView != null && patchHomeMode;
        f.restable = patchRestable;
        f.compositeKept = patchShotBmp != null && homeBuiltAtMs > 0;
        f.stepMatches = HomePatchRules.sameStep(homeBuiltStep, homeStepNow());
        f.launcherFocus = homeLauncherFocus();
        f.homeAtRest = TjHooks.homeScreenShowing() && !TjHooks.homeFloatingViewOpen() && !TjHooks.homeBusy();
        f.calibrating = homeCalRunning;
        return HomePatchRules.openTreatment(f);
    }

    private long homeFocusGapMs = -1;   // review A #1: how long before the dialog's own event (else before now) the Launcher lost the focus; -1 = never lost

    /**
     * BT1c + review A #1: does the Launcher count as having the focus at this opening? The stock dialog takes the focus itself and whether its event or the Launcher's loss arrives first is not fixed, so a loss within
     * {@link HomePatchRules#FOCUS_TOLERANCE_MS} of the dialog's own event is the dialog's, not a shade's. The ONE definition: the decision and the words of the log line both use it.
     */
    private boolean homeLauncherFocus() {
        com.android.launcher3.Launcher l = TjHooks.launcher();
        long now = android.os.SystemClock.uptimeMillis();
        long ref = dialogEventMs > 0 && now - dialogEventMs < 1000 ? dialogEventMs : now;
        homeFocusGapMs = homeFocusLostAtMs > 0 ? ref - homeFocusLostAtMs : -1;
        return l != null && HomePatchRules.launcherFocus(l.hasWindowFocus(), homeFocusLostAtMs, ref);
    }

    /** BT1c: why the home patch is not fresh right now, in words and numbers (the log line of every press that finds it so). */
    private String homeNotFreshWhy() {
        long up = android.os.SystemClock.uptimeMillis();
        boolean focus = homeLauncherFocus();
        return HomePatchRules.notFreshWhy(patchShotBmp != null && homeBuiltAtMs > 0, HomePatchRules.sameStep(homeBuiltStep, homeStepNow()),
                homeBuiltAtMs > 0 ? up - homeBuiltAtMs : -1, TjHooks.lastHomeDrawMs > 0 ? up - TjHooks.lastHomeDrawMs : -1, homeForeignUntilMs - up,
                focus, homeCalRunning) + (homeFocusGapMs >= 0 ? " (focus lost " + homeFocusGapMs + " ms before the dialog)" : "");
    }

    /**
     * BT1c / BT1d: called at the start of every showOverlay, i.e. once per opening on the home screen (the fast path and the scan path both come through it). A patch that is not fresh is ADOPTED when
     * {@link HomePatchRules#openTreatment} says so (shown as it is, rebuilt in place right after the first frame); otherwise the card covers the whole panel ({@link #cardArea}). One persistent log line per press
     * says which, and why (it goes to the tablet report: Tj's "sometimes the patch doesn't work" had no trace before).
     */
    private void homeOpenDecide() {
        if (!patchHomeMode || overlayShown) return;
        int t = homeOpenTreatment();
        homeOpenStale = false;
        homeOpenLogPersist = t != HomePatchRules.OPEN_FRESH;   // review A #5: the tablet report keeps the rare presses (stale / full card), not every normal one
        if (t == HomePatchRules.OPEN_FRESH) { homeOpenLogLine = "menu opened (home): fresh patch"; }
        else if (t == HomePatchRules.OPEN_STALE) {
            homeOpenLogLine = "menu opened (home): patch not fresh, shown as it is and rebuilt after the fade: " + homeNotFreshWhy();
            homeOpenStale = true; patchRestable = true;   // adopted for THIS opening: cardWanted() = false, patchShow(true) makes it visible; patchShow(false) at the close recomputes both
            handler.removeCallbacks(homeRefreshOpenRun); handler.postDelayed(homeRefreshOpenRun, PatchFade.SETTLE_MS + 20);   // review A #6: the rebuild (a PixelCopy callback + a composite on THIS thread) waits for the end of the stock fade, which this thread has to redraw frame by frame
        } else {
            homeOpenLogLine = "menu opened (home): no usable patch, the card covers the whole panel: " + homeNotFreshWhy();
            patchRestable = false;   // review A #4: the verdict is ENFORCED (a fresh patch with a folder open or a calibration running is not trusted): cardWanted() = true, the card covers the whole panel
            if (patchResting) { patchResting = false; if (patchView != null) patchView.setAlpha(0f); }
        }
        handler.post(homeOpenLogRun);   // AFTER the frame of the patch / the card: never before the time-critical part
    }
    private boolean homeOpenLogPersist = false;
    private final Runnable homeOpenLogRun = new Runnable() { @Override public void run() { try { Log.i(TAG, "HOMEPRESS " + homeOpenLogLine); if (homeOpenLogPersist) logPatch(homeOpenLogLine); } catch (Throwable t) { Log.e(TAG, "home press log failed", t); } } };
    private final Runnable homeRefreshOpenRun = new Runnable() { @Override public void run() {
        try {
            if (patchView == null || !patchHomeMode || !overlayShown || !homeOpenStale || homeBuilding || homeCalRunning || homeWallBmp == null) return;
            if (!HomePatchRules.sameStep(homeWallStep, homeStepNow())) return;
            if (homeCompositeFresh()) return;   // review A #6: it became fresh meanwhile (a build that was already running): nothing to rebuild
            startHomeCopy();
        } catch (Throwable t) { Log.e(TAG, "home refresh while open failed", t); }
    } };

    /** "Is there a crop for this wallpaper position?", remembered until a crop is stored or forgotten or the wallpaper or the position changes (the answer is asked on every layer refresh). */
    private boolean homeCropValid(int step, String sig) {
        String key = sig + "#" + step;
        int g = HomeShot.generation();
        if (g != homeCropGen || !key.equals(homeCropKey)) { homeCropOk = HomeShot.has(this, rotation(), step, sig); homeCropGen = g; homeCropKey = key; }
        return homeCropOk;
    }

    /** The calibration is cut short (the Launcher paused, the screen went off, the mode changed): the Launcher is shown again and whatever the screenshot shows is thrown away. */
    void homeCalInterrupted(String why) {
        try { if (homeCalRunning) homeCalFinish(false, why); } catch (Throwable t) { Log.e(TAG, "home calibration interrupt failed", t); }
    }

    private boolean homeVisibleNow() { return HomePatchRules.visible(homeRestable(), android.os.SystemClock.uptimeMillis(), homeLastTouchMs(), TjHooks.lastHomeDrawMs); }

    /** Loads the wallpaper-only crop for the current wallpaper position and checks that it still covers the patch window (the stock panel moved: font size, another entry = calibrate again). */
    private boolean homeLoadFailed = false; private int homeLoadFails = 0;
    private void loadHomeWall(Rect rect) {
        homeWallBmp = null; homeWallStep = -1; homeLoadFailed = false;
        try {
            int step = homeStepNow(); String sig = homeSig();
            PatchShot.Loaded l = HomeShot.load(this, rotation(), step, sig);
            if (l != null) {
                if (!PatchShot.covers(l.left, l.top, l.bmp.getWidth(), l.bmp.getHeight(), rect.left, rect.top, rect.right, rect.bottom)) {
                    Log.i(TAG, "HOMEPATCH the stored wallpaper crop no longer covers the patch window " + rect.toShortString() + ": it will be calibrated again");
                    homeStatus("the menu moved out of the stored crop: calibrating again");
                    HomeShot.invalidateAll(this);
                } else { homeWallBmp = l.bmp; homeWallLeft = l.left; homeWallTop = l.top; homeWallStep = HomeShot.findStep(this, rotation(), step, sig); }
            } else if (HomeShot.has(this, rotation(), step, sig)) {
                // a crop that "exists" but could not be loaded (code review 2026-11-08, twice). REALLY corrupt (an empty file, an unreadable header, judged from the image bounds alone): forget THIS ONE page and calibrate it again,
                // or has() says yes and load() says no for ever and buildHomePatchNow / refreshLayers call each other every ~20 ms. Otherwise a failure of the moment (low memory on a 2 GB tablet): keep EVERY crop
                // (a memory spike must never cost the calibration of all pages) and let buildHomePatchNow try again in a few seconds.
                if (HomeShot.isCorrupt(this, rotation(), step, sig)) {
                    Log.w(TAG, "HOMEPATCH the stored wallpaper crop of this page is corrupt: it will be calibrated again");
                    homeStatus("the stored wallpaper crop of this page could not be read: calibrating this page again");
                    HomeShot.invalidateStep(this, rotation(), step, sig);
                } else {
                    Log.w(TAG, "HOMEPATCH the stored wallpaper crop could not be loaded just now (low memory?): trying again in a few seconds");
                    homeLoadFailed = true;
                }
            }
        } catch (Throwable t) { Log.e(TAG, "home wall load failed", t); }
        reloadScrim();   // the scrim measured on the lock screen applies to anything under the stock dialog
    }

    // ---------------------------------------------------------------- the Launcher's hooks (main thread)

    /** TjHooks: the Launcher is about to draw: whatever is under the slot may change, the built patch is stale from now on. Cheap when the patch is already away. */
    private long homeDrawsSeen = 0, homeDrawLoggedMs = 0;
    void homeDrew() {
        scheduleCurtain();   // BL1: a draw of the Launcher is a sign the home may have changed: the curtain picture is looked at again once the home is quiet (one posted runnable at most, nothing else here)
        if (sumArmed && sumFirstDrawAt == 0) { try { sumNoteDraw(); } catch (Throwable t) { Log.e(TAG, "unlock summary draw failed", t); } }   // BK1c: the first Launcher frame after the keyguard went away
        if (debuggable()) {   // emulator diagnostics only (run #348: HP7 saw no draw at all): one line per burst of draws says whether the listener hears the Launcher and what state the patch was in
            homeDrawsSeen++;
            long up = android.os.SystemClock.uptimeMillis();
            if (up - homeDrawLoggedMs > 1000) { homeDrawLoggedMs = up; Log.i(TAG, "HOMEPATCH draw burst (draw " + homeDrawsSeen + ", patch " + (patchView == null ? "none" : patchResting ? "resting" : patchRestable ? "restable" : "away") + ")"); }
        }
        if (patchView == null) {   // no window (a calibration just ended, a failure removed it, the wallpaper changed): a draw of the Launcher is a sign of life, ask for a layer refresh now and then
            long up = android.os.SystemClock.uptimeMillis();
            if (up - homeRefreshAskedMs >= 2000 && !homeCalRunning && homeSwitchOn()) { homeRefreshAskedMs = up; refreshLayersSoon(); }
            return;
        }
        if (!patchHomeMode || overlayShown || homeCalRunning) return;
        if (!patchRestable && !patchResting) { scheduleHomeBuild(); return; }   // BI1: already away, but a draw says the home screen is changing: a build follows the LAST draw by DRAW_QUIET_MS. The bounded retries of buildHomePatchNow can run out while a popup stays open for long, and nothing else would ask again when it closes
        patchRestable = false;
        if (patchResting) { patchResting = false; patchView.setAlpha(0f); }
        Log.i(TAG, "HOMEPATCH stale: the Launcher drew (the patch is away until it is built again)");
        scheduleHomeBuild();
    }

    /** TjHooks: a finger went down in the Launcher: the patch goes away in this very frame (before the press feedback / a scroll can change what is under it). It stays restable: a tap that changes nothing leaves it fresh. */
    void homeTouchDown() {
        try {
            if (patchView == null || !patchHomeMode || overlayShown || !patchResting) return;
            patchView.setAlpha(0f);
            Log.i(TAG, "HOMEPATCH away: a finger went down");
            handler.removeCallbacks(homeShowCheck); handler.postDelayed(homeShowCheck, HomePatchRules.TOUCH_AWAY_MS + 30);
        } catch (Throwable t) { Log.e(TAG, "home touch hide failed", t); }
    }

    private final Runnable homeShowCheck = new Runnable() { @Override public void run() { try { if (patchHomeMode && patchView != null) patchShow(overlayShown); } catch (Throwable t) { Log.e(TAG, "home show check failed", t); } } };
    private final Runnable homeBuild = new Runnable() { @Override public void run() { try { buildHomePatchNow(); } catch (Throwable t) { homeBuilding = false; Log.e(TAG, "home patch build failed", t); } } };

    /** Builds a fresh patch after the home has been quiet for {@link HomePatchRules#DRAW_QUIET_MS}. */
    private void scheduleHomeBuild() {
        if (patchView == null || !patchHomeMode) return;
        homeBuildWaits = 0;   // a fresh wish to build (a draw, a resume, a focus gain) starts a new bounded wait
        long quiet = HomePatchRules.waitMs(android.os.SystemClock.uptimeMillis(), 0, TjHooks.lastHomeDrawMs);
        handler.removeCallbacks(homeBuild);
        handler.postDelayed(homeBuild, quiet + 20);
    }

    private void buildHomePatchNow() {
        if (patchView == null || !patchHomeMode || overlayShown || homeBuilding || homeCalRunning) return;
        long now = android.os.SystemClock.uptimeMillis();
        boolean fresh = homeCompositeFresh();
        if (fresh) { patchShow(false); return; }
        if (!HomePatchRules.buildDue(now, TjHooks.lastHomeDrawMs, fresh, homeBuilding)) { scheduleHomeBuild(); return; }
        if (!TjHooks.homeScreenShowing() || TjHooks.homeFloatingViewOpen() || TjHooks.homeBusy()) {   // not at rest: look again a few times (the draws that end a folder / a drag start a new wish anyway)
            if (++homeBuildWaits <= 10) { handler.removeCallbacks(homeBuild); handler.postDelayed(homeBuild, 400); }
            return;
        }
        int stepNow = homeStepNow();
        if (homeWallBmp == null || !HomePatchRules.sameStep(homeWallStep, stepNow)) {   // another page (the wallpaper moved) or nothing loaded: the crop of THIS position, or the calibration of it
            loadHomeWall(new Rect(patchAt));
            if (homeWallBmp == null) {
                patchRestable = false;
                if (homeLoadFailed) {   // the crop exists but could not be loaded just now (low memory): a few tries, 5 s apart, never a tight loop; the card covers the buttons meanwhile
                    if (++homeLoadFails <= 6) { handler.removeCallbacks(homeBuild); handler.postDelayed(homeBuild, 5000); }
                } else { refreshLayers(); }   // no crop for this position: the window goes, the calibration comes (refreshLayers -> homeNeedsCal)
                return;
            }
            homeLoadFails = 0;
        }
        startHomeCopy();
    }

    /**
     * The copy of the Launcher's pixels under the stored wallpaper crop, composed into the patch (asynchronous: PixelCopy). Called by the normal build and, BT1c, right after a menu opened with a patch that was not
     * fresh (the open menu hides every later draw, so nothing waits for a quiet home). Needs {@code homeWallBmp}; sets {@code homeBuilding} until the answer comes.
     */
    private void startHomeCopy() {
        homeBuilding = true;
        final long reqAt = android.os.SystemClock.uptimeMillis(); final int builtStep = homeWallStep;
        final android.graphics.Bitmap wall = homeWallBmp; final int l = homeWallLeft, t = homeWallTop;
        Rect crop = new Rect(l, t, l + wall.getWidth(), t + wall.getHeight());
        HomeLayer.copy(crop, handler, new HomeLayer.Done() {
            @Override public void done(android.graphics.Bitmap layer, String problem) {
                homeBuilding = false;
                try {
                    if (patchView == null || !patchHomeMode || wall != homeWallBmp) { if (layer != null) layer.recycle(); return; }
                    if (layer == null) { homeBuildFails++; homeStatus("could not read the Launcher's pixels (" + problem + ")"); Log.i(TAG, "HOMEPATCH layer copy failed: " + problem); if (homeBuildFails < 5) { handler.removeCallbacks(homeBuild); handler.postDelayed(homeBuild, 1500); } return; }
                    android.graphics.Bitmap comp = HomeLayer.compose(wall, layer);
                    layer.recycle();
                    if (comp == null) { homeBuildFails++; homeStatus("could not compose the patch"); return; }
                    homeBuildFails = 0;
                    patchShotBmp = comp; patchShotLeft = l; patchShotTop = t; homeBuiltAtMs = reqAt; homeBuiltStep = builtStep;
                    homeStatus("home patch built; " + (homeCompositeFresh() ? "fresh" : "stale already, building again"));
                    Log.i(TAG, "HOMEPATCH patch built, fresh=" + homeCompositeFresh() + " step " + builtStep + (overlayShown ? " (while the menu is open)" : ""));
                    patchShow(overlayShown);   // BT1c: a build that ends while the stock menu is open must not close the patch's open look (it used to call patchShow(false) whatever the state)
                    patchView.invalidate();
                    if (!homeCompositeFresh() && !overlayShown) scheduleHomeBuild();
                } catch (Throwable th) { Log.e(TAG, "home patch apply failed", th); }
            }
        });
    }

    // ---------------------------------------------------------------- the calibration: the Launcher hidden, one system screenshot of the plain wallpaper

    private final Runnable homeCalTrigger = new Runnable() { @Override public void run() { try { maybeHomeCalibrate("timer"); } catch (Throwable t) { Log.e(TAG, "home calibration trigger failed", t); } } };
    private final Runnable homeCalShoot = new Runnable() { @Override public void run() {
        try {
            if (!homeCalRunning) return;
            if (!isScreenOn() || keyguardLocked() || !TjHooks.homeScreenShowing() || !Mode.isCustom(TjAccessibilityService.this)) { homeCalFinish(false, "the screen or the launcher changed before the screenshot"); return; }   // never photograph a lock screen, an app or the Launcher with its icons
            Log.i(TAG, "HOMEPATCH asking the system for a screenshot of the plain wallpaper"); homeStatus("taking the screenshot of the plain wallpaper ..."); performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        } catch (Throwable t) { Log.e(TAG, "home screenshot failed", t); }
    } };
    private final Runnable homeCalAbort = new Runnable() { @Override public void run() { try { if (homeCalRunning) homeCalFinish(false, "no screenshot appeared in time"); } catch (Throwable t) { Log.e(TAG, "home calibration abort failed", t); } } };

    /** Starts the calibration when the rules say so. Returns "" when it started, else the reason. (Launcher control only opens in Quickstep, where no calibration can run: its button forgets the crops, and this starts by itself the next time the custom home screen is quiet.) */
    private String maybeHomeCalibrate(String why) {
        HomePatchRules.CalState s = new HomePatchRules.CalState();
        s.switchOn = homeSwitchOn();
        s.coverMode = Features.POWER_HIDE_SCREENSHOT.equals(Features.powerMode(this));
        s.automationOff = CrashGuard.automationOff(this);
        s.customMode = Mode.isCustom(this);
        s.unlocked = !keyguardLocked();
        s.screenOn = isScreenOn();
        s.permission = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        s.running = homeCalRunning;
        s.otherCalibration = calibrating() || scrimStage != 0;
        s.positionKnown = TjHooks.wallpaperReported;   // BI1: never file a crop under the placeholder step (code review 2026-11-08)
        s.hasCrop = s.positionKnown && homeCropValid(homeStepNow(), homeSig()) && !curtainWallMissing();   // BL1: a crop made before the curtain existed has no full wallpaper picture next to it: one more calibration (one flash) takes it
        s.rectKnown = cachedCard() != null;
        s.homeAtRest = TjHooks.homeScreenShowing() && !TjHooks.homeFloatingViewOpen() && !TjHooks.homeBusy();
        s.menuOpen = overlayShown || since(menuUpAtMs) < 3000;
        s.msSinceTouch = since(homeLastTouchMs());
        s.msSinceDraw = since(TjHooks.lastHomeDrawMs);
        s.msSinceScreenOn = screenOnEpoch > 0 ? Math.max(0, System.currentTimeMillis() - screenOnEpoch) : Long.MAX_VALUE;
        s.msSinceLastStart = since(homeCalLastStartUp);
        s.startsToday = homeCalStartsToday(false);
        String reason = HomePatchRules.calDecision(s);
        if (!reason.isEmpty()) {
            if (s.hasCrop && s.switchOn && s.coverMode && s.customMode && patchView == null) refreshLayersSoon();   // BI1: the look that posted this timer found no crop (the wallpaper position was not known yet after a restart); this one finds it: look again (one shot: the timer is re-posted only while there is no crop)
            homeStatus("not calibrating yet: " + reason);
            if (s.switchOn && s.coverMode && s.customMode && !s.hasCrop && ("touched a moment ago".equals(reason) || "the home screen is still moving".equals(reason) || "the screen came on a moment ago".equals(reason) || "tried a moment ago".equals(reason) || "the home screen is not at rest".equals(reason))) {   // NOT 'the wallpaper position is not known yet' (review 2026-11-08): no poll for it; the Launcher's first report re-evaluates by itself (onWallpaperOffset -> homeOffsetSettled -> refreshLayersSoon)
                handler.removeCallbacks(homeCalTrigger); handler.postDelayed(homeCalTrigger, 3000);   // a "not yet" reason: look again
            }
            return reason;
        }
        Rect win = patchRect(cachedCard(), true);
        homeCalWin = new Rect(win);
        homeCalStep = homeStepNow();
        homeCalStartsToday(true);
        homeCalGen++;
        homeCalRunning = true; homeCalAskedWall = System.currentTimeMillis(); homeCalLastStartUp = android.os.SystemClock.uptimeMillis();
        Log.i(TAG, "HOMEPATCH calibration starts (" + why + ") window " + win.toShortString() + " step " + homeCalStep);
        homeStatus("calibrating: the home screen is hidden for a second");
        try { removeOverlay(); removePatch(); } catch (Throwable ignored) { }   // the resting patch must never be inside the screenshot
        addHomeCalGuard();
        if (!TjHooks.setLauncherHidden(true)) { homeCalFinish(false, "the Launcher could not be hidden"); return "the Launcher could not be hidden"; }
        startShotWatch();
        handler.removeCallbacks(homeCalShoot); handler.postDelayed(homeCalShoot, HomePatchRules.HIDE_SETTLE_MS);
        handler.removeCallbacks(homeCalAbort); handler.postDelayed(homeCalAbort, HomePatchRules.HIDE_MAX_MS);
        return "";
    }

    private int homeCalStartsToday(boolean add) {
        try {
            String day = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(new java.util.Date());
            String[] p = Features.getString(this, "home_cal_day", "").split(":");
            int n = p.length == 2 && p[0].equals(day) ? Integer.parseInt(p[1]) : 0;
            if (add) { n++; Features.setStringAsync(this, "home_cal_day", day + ":" + n); }
            return n;
        } catch (Throwable t) { return 0; }
    }

    /** A transparent window over everything while the Launcher is hidden: a touch must not land on invisible icons; it ends the calibration at once. */
    private void addHomeCalGuard() {
        try {
            removeHomeCalGuard();
            View g = new View(this);
            g.setOnTouchListener(new View.OnTouchListener() { @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) { handler.post(new Runnable() { @Override public void run() { if (homeCalRunning) homeCalFinish(false, "touched during the calibration"); } }); }
                return true;
            } });
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
            lp.setTitle("TjHomeCalGuard");
            ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(g, lp);
            homeCalGuard = g;
        } catch (Throwable t) { Log.e(TAG, "home calibration guard failed", t); }
    }

    private void removeHomeCalGuard() {
        if (homeCalGuard == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(homeCalGuard); } catch (Throwable ignored) { }
        homeCalGuard = null;
    }

    /** Finishes the calibration of run {@code gen} only if that run is still the current one (a worker thread must never end or save for a run that was aborted). */
    private void homeCalFinishIf(int gen, boolean ok, String msg) { if (gen == homeCalGen && homeCalRunning) homeCalFinish(ok, msg); }

    /** The media store changed while a home calibration runs: is our screenshot in it? Then the capture is over, the Launcher is shown again at once and the crop is cut (worker thread). */
    private void homeShotObserved() {
        if (!homeCalRunning) return;
        final android.content.Context app = getApplicationContext();
        final long asked = homeCalAskedWall; final Rect win = homeCalWin == null ? null : new Rect(homeCalWin); final int rot = rotation(); final int step = homeCalStep; final String sig = homeSig(); final long wid = homeWallpaperId();
        final android.util.DisplayMetrics dm = realMetrics();
        final int gen = homeCalGen;   // this run: an abort / a finish / a new start makes every later step of THIS worker void (code review 2026-11-08)
        new Thread(new Runnable() { @Override public void run() {
            try {
                if (gen != homeCalGen) return;
                final PatchShot.Shot row = PatchShot.newestShot(app, asked);
                if (row == null || !HomePatchRules.shotIsOurs(row.addedSec, asked)) return;   // some other image change: keep waiting
                if (!AutoCalibration.ownerOk(row.owner) && !isSystemPackage(row.owner)) { Log.i(TAG, "HOMEPATCH ignored a screenshot written by " + row.owner); return; }
                if (lastShotId == android.content.ContentUris.parseId(row.uri)) return;
                lastShotId = android.content.ContentUris.parseId(row.uri);
                handler.post(new Runnable() { @Override public void run() { try { if (homeCalRunning && gen == homeCalGen) { TjHooks.setLauncherHidden(false); removeHomeCalGuard(); } } catch (Throwable ignored) { } } });   // the capture is over: the home screen comes back at once
                if (win == null) { handler.post(new Runnable() { @Override public void run() { homeCalFinishIf(gen, false, "the menu rectangle is not learned"); } }); return; }
                android.graphics.Bitmap full = PatchShot.decode(app, row.uri);
                if (full == null) { handler.post(new Runnable() { @Override public void run() { homeCalFinishIf(gen, false, "the screenshot could not be read"); } }); return; }
                int W = full.getWidth(), H = full.getHeight();
                if (W != dm.widthPixels || H != dm.heightPixels) { full.recycle(); handler.post(new Runnable() { @Override public void run() { homeCalFinishIf(gen, false, "the screenshot has another size than the screen"); } }); return; }
                long sr = 0, sg = 0, sb = 0; int n = 0;
                for (int y = Math.max(0, win.top); y < Math.min(H, win.bottom); y += 3) for (int x = Math.max(0, win.left); x < Math.min(W, win.right); x += 3) { int q = full.getPixel(x, y); sr += (q >> 16) & 255; sg += (q >> 8) & 255; sb += q & 255; n++; }
                if (n > 0 && PatchShot.looksLikeMenu(sr / (double) n, sg / (double) n, sb / (double) n)) { full.recycle(); handler.post(new Runnable() { @Override public void run() { homeCalFinishIf(gen, false, "the power menu was in that screenshot"); } }); return; }
                int[] cr = PatchShot.cropRect(win.left, win.top, win.right, win.bottom, PatchShot.MARGIN_PX, W, H);
                android.graphics.Bitmap crop = android.graphics.Bitmap.createBitmap(full, cr[0], cr[1], cr[2] - cr[0], cr[3] - cr[1]);
                if (gen != homeCalGen) { full.recycle(); crop.recycle(); Log.i(TAG, "HOMEPATCH the calibration was cut short: the screenshot is thrown away"); return; }
                final boolean saved = HomeShot.save(app, rot, step, crop, sig, cr[0], cr[1], wid, W, H);
                final String dims = crop.getWidth() + "x" + crop.getHeight();
                if (saved && curtainSwitchOn()) {   // BL1: the WHOLE plain-wallpaper screenshot is kept too (it used to be thrown away after the crop was cut): it is the wallpaper half of the home curtain
                    final boolean wallKept = HomeCurtain.saveWall(app, rot, step, full, sig, wid, W, H);
                    Log.i(TAG, "CURTAIN wallpaper picture " + (wallKept ? "kept" : "NOT kept") + " (" + W + "x" + H + ", wallpaper position " + step + ")");
                }
                full.recycle();
                final boolean deleted = saved && PatchShot.deleteQuietly(app, row.uri);
                handler.post(new Runnable() { @Override public void run() { homeCalFinishIf(gen, saved, saved ? "HOME PATCH CALIBRATED (" + dims + ", wallpaper position " + step + ")" + (deleted ? "; the screenshot was removed from the gallery again" : "; the screenshot itself stays in the gallery") : "the crop could not be stored"); } });
            } catch (Throwable t) { Log.e(TAG, "home shot ingest failed", t); handler.post(new Runnable() { @Override public void run() { homeCalFinishIf(gen, false, "failed: " + t); } }); }
        } }, "tj-homeshot").start();
    }

    private void homeCalFinish(boolean ok, String msg) {
        handler.removeCallbacks(homeCalShoot); handler.removeCallbacks(homeCalAbort);
        homeCalGen++; homeCalRunning = false;
        try { TjHooks.setLauncherHidden(false); } catch (Throwable ignored) { }
        removeHomeCalGuard();
        try { if (!calibrating()) stopShotWatch(); } catch (Throwable ignored) { }
        homeWallBmp = null; homeWallStep = -1; homeBuiltAtMs = 0;
        homeStatus((ok ? "" : "calibration did not finish: ") + msg);
        Log.i(TAG, "HOMEPATCH calibration end: ok=" + ok + " " + msg);
        logPatch("home calibration " + (ok ? "ok" : "failed") + ": " + msg);
        try { refreshLayers(); } catch (Throwable ignored) { }
        if (!ok) { handler.removeCallbacks(homeCalTrigger); handler.postDelayed(homeCalTrigger, HomePatchRules.CAL_MIN_GAP_MS + 1000); }
    }

    /** One line for Launcher control and the tablet report. Never throws. */
    String homePatchInfo() {
        try {
            return "switch " + (homeSwitchOn() ? "on" : "off") + ", power menu mode " + Features.powerMode(this) + ", crops stored " + HomeShot.count(this) + ", window " + (patchView != null && patchHomeMode ? (patchResting ? "resting" : "present, away") : "none")
                    + ", fresh " + homeRestable() + ", calibrating " + homeCalRunning
                    + ", foreign windows seen " + homeForeignCount + (homeForeignCount == 0 ? "" : " (" + homeForeignNames + "; the last " + Math.max(0, (android.os.SystemClock.uptimeMillis() - homeForeignLastAtMs) / 1000) + " s ago)")
                    + (homeForeignUnlockIgnored == 0 ? "" : ", unlock window events ignored " + homeForeignUnlockIgnored)
                    + " | " + Features.getString(this, HOME_STATUS_KEY, "(nothing yet)");
        } catch (Throwable t) { return "(unreadable)"; }
    }

    private Rect patchTarget() {
        targetIsLock = false; targetIsHome = false;
        if (keyguardLocked()) {   // V1: the lock screen has its own rules (the wallpaper there may be another picture, and nothing may be painted over the clock / PIN pad)
            if (!Features.get(this, LOCK_PATCH_KEY, true)) { patchStatus("off: lock-screen patch switched off"); return null; }
            if (!Features.isCover(Features.powerMode(this))) { patchStatus("off: lock screen, power menu cover off"); return null; }   // AD7a: no mode condition on the lock screen
            Rect r = cachedCard();
            if (r == null) { patchStatus("waiting: open the power menu once on the lock screen so the helper learns where it sits"); return null; }
            if (!PatchShot.has(this, rotation(), shotSig()) && loadLockWallpaper() == null) { patchStatus("waiting: the lock-screen wallpaper could not be read (Launcher control > Allow wallpaper access)"); return null; }   // BE1: real pixels stored = no full wallpaper decode on the main thread at every lock
            targetIsLock = true;
            patchStatus("lock screen: patch window wanted at " + r.toShortString());
            return patchRect(r, true);
        }
        Rect homeReal = homePatchTarget();   // BI1: the unlocked custom home screen in "hide Screenshot" mode: the cardless real-pixel patch, when its crop exists
        if (homeReal != null) { targetIsLock = true; targetIsHome = true; return homeReal; }
        if (!Features.POWER_WALLPAPER.equals(Features.powerMode(this))) return null;
        if (!Mode.isCustom(this)) { patchStatus("off: Quickstep mode"); return null; }
        if (!TjHooks.homeScreenShowing()) { patchStatus("off: only shown on the home screen (not over apps, not in All Apps)"); return null; }
        Rect r = cachedCard();
        if (r == null) { patchStatus("waiting: open the power menu once so the helper learns where it sits (this screen orientation)"); return null; }
        if (loadWallpaper() == null) return null;
        return patchRect(r, false);
    }

    /**
     * V3 (before v0.7.6): the patch covered the ENTIRE 3-button menu: the learned rectangle plus a 12 px ring with hard edges. AH3 (v0.7.6, Tj's side-by-side photos: our card is the right size and flush, but a flat
     * bluish block around it shows where the patch does not match the live lock screen): the patch now covers ONLY the Screenshot slot (below our card) plus a soft fringe, see {@link PatchRest#slotWindow}.
     * If the learned geometry is nonsense (the card would be the whole panel) the window is a thin strip at the panel's bottom and the card covers the whole panel, so no Screenshot entry can show.
     */
    private Rect patchRect(Rect r, boolean lock) {
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        if (!lock) {   // v0.7.6: the HOME-screen patch keeps its v0.7.5 geometry (the whole menu rectangle plus a 12 px ring); only the lock-screen patch became a soft slot patch
            int m = (int) (12 * dm.density);
            return new Rect(Math.max(0, r.left - m), Math.max(0, r.top - m), Math.min(dm.widthPixels, r.right + m), Math.min(dm.heightPixels, r.bottom + m));
        }
        int[] w = PatchRest.slotWindow(r.left, cardAreaGeom(r).bottom, r.right, r.bottom, Math.round(PatchRest.EDGE_DP * dm.density), Math.round(PatchRest.FEATHER_DP * dm.density), patchExtraPx(dm), patchBottomFeatherPx(dm), dm.widthPixels, dm.heightPixels);
        return new Rect(w[0], w[1], w[2], w[3]);
    }

    /** BB1: the width of the patch's bottom fringe in px (hidden preference {@code patch_bfeather_dp}, default {@link PatchRest#BOTTOM_FEATHER_DP}). */
    private int patchBottomFeatherPx(android.util.DisplayMetrics dm) {
        return Math.max(0, Math.min(PatchShot.MARGIN_PX, Math.round(Features.getInt(this, PatchRest.BOTTOM_FEATHER_KEY, Math.round(PatchRest.BOTTOM_FEATHER_DP)) * dm.density)));
    }

    /**
     * BB1: how far the opaque core reaches below the stock panel, in px beyond the old window (hidden preference {@code patch_extra_dp}, default {@link PatchRest#EXTRA_BOTTOM_DP}, 0 = the v0.8.2 geometry). Clamped so the
     * window stays inside the stored real-pixel crop (cut for the old window + {@link PatchShot#MARGIN_PX}): existing calibrations stay valid, no new screenshot.
     */
    private int patchExtraPx(android.util.DisplayMetrics dm) {
        int wanted = Math.round(Features.getInt(this, PatchRest.EXTRA_KEY, Math.round(PatchRest.EXTRA_BOTTOM_DP)) * dm.density);
        return PatchRest.clampExtra(wanted, patchBottomFeatherPx(dm), Math.round(PatchRest.FEATHER_DP * dm.density), PatchShot.MARGIN_PX);
    }

    private static int[] rectArr(Rect r) { return new int[] { r.left, r.top, r.right, r.bottom }; }

    // ---------------------------------------------------------------- V4: make the lock-screen patch show the RIGHT part of the wallpaper
    private String lockCalSigFor(Rect r) {
        long id = 0;
        try { android.app.WallpaperManager wm = android.app.WallpaperManager.getInstance(this);
              id = wm.getWallpaperId(android.app.WallpaperManager.FLAG_LOCK); if (id <= 0) id = wm.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM); } catch (Throwable ignored) { }
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        return id + "|r" + rotation() + "|" + dm.widthPixels + "x" + dm.heightPixels + "|" + r.toShortString() + "|k" + Features.getInt(this, "patch_test_skew", 0);
    }

    private void calStatus(String s) { if (!s.equals(Features.getString(this, CAL_STATUS_KEY, ""))) Features.setString(this, CAL_STATUS_KEY, s); }

    /** Runs once per wallpaper / rotation / rectangle: a real screenshot (shell helper) of the lock screen with the menu closed is compared with the wallpaper picture. */
    private void maybeCalibrateLock() {
        if (!patchLockMode || patchHomeMode || patchView == null || calBusy || lockBmp == null) return;   // BI1: the home patch reuses patchLockMode; a LOCK calibration must never run on the unlocked home (it would photograph the home screen, flash, and overwrite the lock-screen fit: code review 2026-11-08)
        if (overlayShown) { handler.postDelayed(calTrigger, 2000); return; }   // never photograph the screen while our menu is up
        final Rect r = new Rect(patchAt);
        final String sig = lockCalSigFor(r);
        if (sig.equals(lockCalSig) && lockShotFresh) return;    // already photographed during this lock
        String saved = Features.getString(this, LOCK_CAL_KEY, "");
        String[] p = saved.split("#");
        if (p.length == 4 && p[0].equals(sig) && !sig.equals(lockCalSig)) {     // model fit measured before for exactly this wallpaper / rotation / rectangle (fallback while there is no photo)
            try { lockCalM = Float.parseFloat(p[1]); lockCalDx = Integer.parseInt(p[2]); lockCalDy = Integer.parseInt(p[3]); lockCalSig = sig; patchView.invalidate(); } catch (NumberFormatException ignored) { }
        }
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null || !pm.isInteractive()) return;
        if (!ShellHelper.reachable()) { calStatus("no photo of the lock screen: start the adb helper (Launcher control > Tablet setup); the wallpaper model is used meanwhile"); return; }
        if (android.os.SystemClock.elapsedRealtime() - lastShotTry < 4000) { handler.postDelayed(calTrigger, 4000); return; }   // never a screenshot storm if a photo keeps failing
        lastShotTry = android.os.SystemClock.elapsedRealtime();
        calBusy = true;
        final boolean hideRest = patchResting;   // AE3: a RESTING patch must not photograph itself: hide it for the screenshot, restore it afterwards
        if (hideRest && patchView != null) patchView.setAlpha(0f);
        final android.graphics.Bitmap wall = lockBmp;
        final float skew = testSkew();
        new Thread(new Runnable() { @Override public void run() {
            String result = "failed";
            try {
                ShellHelper.run("MKDIR_PUBLISH");
                if (hideRest) { try { Thread.sleep(250); } catch (InterruptedException ignored) { } }   // let the window really disappear before the shot
                String out = ShellHelper.run("CAL_SHOT");
                if (out == null) throw new IllegalStateException("the screenshot job did not answer");
                android.graphics.Bitmap shot = android.graphics.BitmapFactory.decodeFile("/sdcard/Download/launcher-probe/cal.png");
                if (shot == null) throw new IllegalStateException("the screenshot could not be read");
                int W = shot.getWidth(), H = shot.getHeight();
                int[] sp = new int[W * H]; shot.getPixels(sp, 0, W, 0, 0, W, H); shot.recycle();
                final int cl = Math.max(0, r.left), ct = Math.max(0, r.top), cr = Math.min(W, r.right), cb = Math.min(H, r.bottom);
                android.graphics.Bitmap crop = null; String mean = "";
                if (cr - cl >= 12 && cb - ct >= 12) {
                    crop = android.graphics.Bitmap.createBitmap(cr - cl, cb - ct, android.graphics.Bitmap.Config.ARGB_8888);
                    crop.setPixels(sp, ct * W + cl, W, 0, 0, cr - cl, cb - ct);
                    long sr = 0, sg = 0, sb = 0; int n = 0;
                    for (int y = ct; y < cb; y += 3) for (int x = cl; x < cr; x += 3) { int q = sp[y * W + x]; sr += (q >> 16) & 255; sg += (q >> 8) & 255; sb += q & 255; n++; }
                    if (n > 0) mean = (sr / n) + "," + (sg / n) + "," + (sb / n);
                }
                final android.graphics.Bitmap photo = crop; final int photoDx = cl - r.left, photoDy = ct - r.top;
                int bw = wall.getWidth(), bh = wall.getHeight();
                int[] wp = new int[bw * bh]; wall.getPixels(wp, 0, bw, 0, 0, bw, bh);
                PatchCalibrator.Result res = PatchCalibrator.solve(wp, bw, bh, sp, W, H, r.left, r.top, r.right, r.bottom, 0.5f, skew);
                final float m = res == null ? 1f : res.scale; final int dx = res == null ? 0 : res.dx, dy = res == null ? 0 : res.dy;
                final String model = res == null ? "wallpaper model: default alignment kept" : "wallpaper model: " + res;
                result = photo != null ? "PHOTO of the lock screen taken (" + photo.getWidth() + "x" + photo.getHeight() + ", mean colour " + mean + "); " + model : model;
                handler.post(new Runnable() { @Override public void run() {
                    if (patchHomeMode || !keyguardLocked()) { Log.i(TAG, "CAL lock patch result discarded: the screen is not the lock screen any more (unlocked while the photo was taken)"); return; }   // BI1 (audit 2026-11-08): a photo taken while Tj unlocked can show the home screen: never fit the LOCK wallpaper to it and never store that fit under the lock signature
                    lockCalM = m; lockCalDx = dx; lockCalDy = dy; lockCalSig = sig;
                    Features.setString(TjAccessibilityService.this, LOCK_CAL_KEY, sig + "#" + m + "#" + dx + "#" + dy);
                    if (photo != null && patchView != null && patchLockMode && patchAt.equals(r) && keyguardLocked()) { lockShot = photo; lockShotDx = photoDx; lockShotDy = photoDy; lockShotFresh = true; }
                    if (patchView != null) patchView.invalidate();
                } });
                Log.i(TAG, "CAL lock patch " + result + " [" + PatchCalibrator.lastNote + "]");
            } catch (Throwable t) { result = "failed: " + t; Log.e(TAG, "lock patch calibration failed", t); }
            finally {
                calBusy = false; calStatus(now() + "  " + result);
                if (hideRest) handler.post(new Runnable() { @Override public void run() { patchShow(overlayShown); } });   // AE3: back to resting (or whatever the state is by now)
            }
        } }, "tj-patch-cal").start();
    }

    /** V1: the picture the lock screen shows: a separate lock wallpaper if Tj set one, else the home wallpaper. Down-sampled to the display so a Go tablet keeps its memory. */
    private android.graphics.Bitmap loadLockWallpaper() {
        if (lockBmp != null) return lockBmp;
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            patchStatus("needs permission: Launcher control > Allow wallpaper access"); return null;
        }
        try {
            android.app.WallpaperManager wm = android.app.WallpaperManager.getInstance(this);
            try {   // AE3 / AE8: is the wallpaper an animated LIVE wallpaper? A still patch can never match one exactly (the specks and streaks move). Shown in Launcher control and in the tablet report (Features dump).
                android.app.WallpaperInfo wi = wm.getWallpaperInfo();
                String kind = wi == null ? "still picture" : "LIVE wallpaper " + wi.getComponent().flattenToShortString() + " (animated: a still patch cannot match it exactly)";
                if (!kind.equals(Features.getString(this, WALLPAPER_KIND_KEY, ""))) Features.setString(this, WALLPAPER_KIND_KEY, kind);
            } catch (Throwable ignored) { }
            android.os.ParcelFileDescriptor pfd = wm.getWallpaperId(android.app.WallpaperManager.FLAG_LOCK) > 0 ? wm.getWallpaperFile(android.app.WallpaperManager.FLAG_LOCK) : null;
            if (pfd != null) {
                try {
                    android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
                    ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
                    android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    android.graphics.BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, o);
                    int sample = 1;
                    while (o.outWidth / (sample * 2) >= dm.widthPixels && o.outHeight / (sample * 2) >= dm.heightPixels) sample *= 2;
                    o = new android.graphics.BitmapFactory.Options(); o.inSampleSize = sample;
                    lockBmp = android.graphics.BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor(), null, o);
                } finally { try { pfd.close(); } catch (Throwable ignored) { } }
            }
            if (lockBmp == null) lockBmp = loadWallpaper();   // no separate lock picture: the lock screen shows the home wallpaper
            try {   // v0.7.6 AH3: the numbers the patch model works with, for the tablet report (so the real engine's placement can be worked out from facts, not photos)
                android.util.DisplayMetrics gdm = new android.util.DisplayMetrics();
                ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(gdm);
                if (lockBmp != null) {
                    float gs = Math.max((float) gdm.heightPixels / lockBmp.getHeight(), (float) gdm.widthPixels / lockBmp.getWidth());
                    String geo = "bitmap " + lockBmp.getWidth() + "x" + lockBmp.getHeight() + ", desired " + wm.getDesiredMinimumWidth() + "x" + wm.getDesiredMinimumHeight() + ", display " + gdm.widthPixels + "x" + gdm.heightPixels
                            + ", cover scale " + Math.round(gs * 1000) / 1000f + " -> shown " + Math.round(lockBmp.getWidth() * gs) + "x" + Math.round(lockBmp.getHeight() * gs) + ", x offset 0.5, profile shift " + PatchProfile.lockShiftPx(android.os.Build.DEVICE, gdm.widthPixels) + " px";
                    if (!geo.equals(Features.getString(this, "wallpaper_geometry", ""))) Features.setString(this, "wallpaper_geometry", geo);
                }
            } catch (Throwable ignored) { }
        } catch (Throwable t) { Log.e(TAG, "lock wallpaper read failed", t); }
        if (lockBmp == null) patchStatus("off: the lock wallpaper could not be read");
        return lockBmp;
    }

    private android.graphics.Bitmap loadWallpaper() {
        if (wallpaperBmp != null) return wallpaperBmp;
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            patchStatus("needs permission: Launcher control > Allow wallpaper access"); return null;
        }
        try {
            android.graphics.drawable.Drawable d = android.app.WallpaperManager.getInstance(this).getDrawable();
            if (d != null) wallpaperFromDrawable = true;   // BY1a D1
            if (d instanceof android.graphics.drawable.BitmapDrawable) wallpaperBmp = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
            else if (d != null && d.getIntrinsicWidth() > 0) {
                android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                d.setBounds(0, 0, b.getWidth(), b.getHeight()); d.draw(new android.graphics.Canvas(b)); wallpaperBmp = b;
            }
        } catch (Throwable t) { Log.e(TAG, "wallpaper read failed", t); }
        if (wallpaperBmp == null) patchStatus("off: the wallpaper could not be read");
        return wallpaperBmp;
    }

    /** Brings the patch (and, above it, the card) in line with the current mode / lock state / rotation. Cheap when nothing changed. */
    private void refreshLayers() {
        try {
            Rect want = patchTarget();
            if (want == null) {
                if (homeNeedsCal) { handler.removeCallbacks(homeCalTrigger); handler.postDelayed(homeCalTrigger, HomePatchRules.CAL_DRAW_QUIET_MS + 600); }   // BI1: the home patch has no wallpaper crop for this position yet: calibrate when everything is quiet
                if (patchView != null) { removeOverlay(); removePatch(); }
                if (!Features.POWER_WALLPAPER.equals(Features.powerMode(this))) dropWallpaper();   // BY1a D1: and the platform's copy
                if (!keyguardLocked()) lockBmp = null;   // V1: not needed off the lock screen (memory)
                if (Features.isCover(Features.powerMode(this)) && coverWorld()) warmUp();   // plain cover mode keeps its pre-built card
                return;
            }
            if (patchView != null && (!PatchRest.sameWindow(rectArr(patchAt), rectArr(want), 2) || patchRot != rotation() || patchLockMode != targetIsLock || patchHomeMode != targetIsHome)) { removeOverlay(); removePatch(); }   // AH3: the slot window follows the card's bottom edge, measured on every scan: a 1-2 px wobble must not rebuild the patch
            if (patchView == null) { removeOverlay(); addPatch(want); warmUp(); }
            if (patchLockMode && !patchHomeMode) { handler.removeCallbacks(calTrigger); handler.postDelayed(calTrigger, 1500); }
            if (patchHomeMode) scheduleHomeBuild();   // BI1
        } catch (Throwable t) { Log.e(TAG, "layer refresh failed", t); removeOverlay(); removePatch(); }
    }

    /** Used by the card creation paths: guarantees the patch exists BEFORE the card is added (later windows sit on top). */
    private void ensurePatch() {
        if (patchView != null) return;
        Rect want = patchTarget();
        if (want != null) addPatch(want);
    }

    private void addPatch(final Rect r) {
        final android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        final Rect rect = new Rect(r);
        if (targetIsLock) buildPatchMasks(rect, dm); else { patchMaskOpen = null; patchMaskRest = null; }
        if (targetIsHome) { patchShotBmp = null; loadHomeWall(rect); }   // BI1: the composite (wallpaper crop + the Launcher's pixels) is built when the home has been quiet, not here
        else if (targetIsLock) { loadStoredShot(); if (patchShotBmp != null && !PatchShot.covers(patchShotLeft, patchShotTop, patchShotBmp.getWidth(), patchShotBmp.getHeight(), rect.left, rect.top, rect.right, rect.bottom)) { Log.i(TAG, "PATCHSHOT the stored crop no longer covers the patch window " + rect.toShortString() + ": calibrating again"); shotStatus(now() + "  the menu moved out of the stored crop (font size / menu layout changed): it is calibrated again"); patchShotBmp = null; PatchShot.invalidate(this, rotation()); } } else patchShotBmp = null;   // AU4   // AM1: the real pixels, if this lock screen was calibrated (must be loaded BEFORE the resting state is decided below)   // AH3: the soft-edge masks belong to the lock patch; the home patch is drawn exactly as in v0.7.5
        View v = new View(this) {
            private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
            private final android.graphics.Matrix m = new android.graphics.Matrix();
            private final android.graphics.Paint maskPaint = newMaskPaint();
            @Override protected void onDraw(android.graphics.Canvas c) {
                boolean rest = patchResting && patchLockMode;   // AE3: the RESTING patch paints only the Screenshot slot (below the card) and without the dialog's scrim
                android.graphics.Bitmap mask = rest ? patchMaskRest : patchMaskOpen;
                if (mask != null && mask.getWidth() == getWidth() && mask.getHeight() == getHeight()) {   // AH3: paint into a layer and fade its edges with the mask (DST_IN), so a residual mismatch is a soft gradient, not a frame
                    int sc = c.saveLayer(0, 0, getWidth(), getHeight(), null);
                    try { paintPatch(c, rest); c.drawBitmap(mask, 0, 0, maskPaint); } finally { c.restoreToCount(sc); }
                    fadeFrameDone(this);   // AZ1: the stock menu is still fading in: draw the next frame
                    if (rest) warmOpenLookOnce(c);
                    return;
                }
                if (rest) { c.save(); c.clipRect(0, patchRestTop, getWidth(), getHeight()); }
                try { paintPatch(c, rest); } finally { if (rest) c.restore(); }
                fadeFrameDone(this);   // AZ1
                if (rest) warmOpenLookOnce(c);
            }
            private int warmedKind = 0;   // per patch window: 1 = the generic dim has been drawn once, 2 = the MEASURED look (a colour matrix) has been
            /**
             * BK1a (Tj, 2026-11-08 18:16 UTC: "The first time I press the side key for the power menu on the lock screen, the patch flickers. Every subsequent press looks fine. I think it just needs to load once. ... Load it
             * immediately when the screen is turned off or when the screen goes to lock screen ... It could also be the dimmer loading for the first time."): MEASURED (emulator section fp, run #389) in a FRESH process the first
             * opening drew 3 frames with one gap of 278 ms while a warm process reads 0.04 - 0.06, because the OPEN look (a colour filter and the open-edge mask in a layer) had never been drawn before the first real opening.
             * Draw it ONCE, now, in the first resting frame of the window, at 1/255 alpha: invisible (at most 0.2 of one brightness level, in one frame), but the GPU work of the first real opening is done before anybody can press
             * the side key. The real code path (paintPatch / setOpenLook / the open mask), no copy of it. Code review 2026-11-08: there are two looks (the generic dim, and the measured colour matrix that appears once the scrim
             * has been measured), so it is repeated when the kind changes, and a draw that could not happen (no picture, no mask) or threw is not counted.
             */
            private void warmOpenLookOnce(android.graphics.Canvas c) {
                int kind = scrimT != null && Features.get(TjAccessibilityService.this, ScrimMeasure.KEY, ScrimMeasure.defaultOn(debuggable())) ? 2 : 1;
                if (warmedKind >= kind) return;
                boolean haveBitmap = patchLockMode && (lockShot != null || patchShotBmp != null) || (patchLockMode ? lockBmp : wallpaperBmp) != null;
                if (!haveBitmap) return;   // nothing to draw yet: try again at the next resting frame
                try {
                    int outer = c.saveLayerAlpha(0, 0, getWidth(), getHeight(), 1);
                    try {
                        android.graphics.Bitmap mo = patchMaskOpen;
                        if (mo != null && mo.getWidth() == getWidth() && mo.getHeight() == getHeight()) {
                            int sc = c.saveLayer(0, 0, getWidth(), getHeight(), null);
                            try { paintPatch(c, false); c.drawBitmap(mo, 0, 0, maskPaint); } finally { c.restoreToCount(sc); }
                        } else paintPatch(c, false);
                    } finally { c.restoreToCount(outer); }
                    warmedKind = kind;
                    Log.i(TAG, "PATCH warm-up: the open look (" + (kind == 2 ? "measured" : "generic") + ") was drawn once, invisibly" + (patchHomeMode ? " (home)" : ""));
                } catch (Throwable t) { Log.e(TAG, "patch warm-up failed", t); }
            }
            private void paintPatch(android.graphics.Canvas c, boolean rest) {
                android.graphics.Bitmap photo = !patchLockMode ? null : lockShot != null ? lockShot : patchShotBmp;   // AM1: the helper's fresh photo wins, else the stored REAL-pixel crop
                if (photo != null) {
                    int px = lockShot != null ? lockShotDx : patchShotLeft - rect.left, py = lockShot != null ? lockShotDy : patchShotTop - rect.top;
                    if (patchHomeMode && debuggable()) px += Features.getInt(TjAccessibilityService.this, "home_patch_test_shift", 0);   // BI1, debug builds only: the emulator shifts the composite on purpose (section hp's negative control: the pixel comparison must notice)
                    if (rest) setDim(p, restDim(true)); else setOpenLook(p, fadeProgress());   // AZ1: the open look follows the stock fade-in (1 = complete); BA1: = the scrim MEASURED on this tablet (per channel scale + offset), else the generic brightness
                    c.drawBitmap(photo, px, py, p); return;
                }   // V4: the photo of the real lock screen (AC6: the paint is shared with the model path, whose dim filter must not darken the photo: run 136 caught that)
                android.graphics.Bitmap b = patchLockMode ? lockBmp : wallpaperBmp;
                if (b == null) return;
                // AC6: the real lock screen is darker than its wallpaper (keyguard scrim), so the MODEL patch is drawn darker too (the photo path above already has the real colours)
                if (patchLockMode) setDim(p, rest ? restDim(false) : PatchFade.mix(restDim(false), lockPatchDim(false), fadeProgress())); else setDim(p, patchOpenHome ? PatchFade.mix(1f, openScrim(), fadeProgress()) : 1f);   // AH1: the home patch was always drawn UNDIMMED = a lighter block while the stock dialog dims the screen; AZ1: both follow the stock fade-in
                // How Android 10's wallpaper engine places the picture: scaled to COVER the display, centred vertically, shifted sideways by the launcher's offset.
                float s = Math.max((float) dm.heightPixels / b.getHeight(), (float) dm.widthPixels / b.getWidth());
                float dw = b.getWidth() * s, dh = b.getHeight() * s;
                float cal = 1f + Features.getInt(TjAccessibilityService.this, PATCH_SCALE_KEY, 0) / 100f;   // Tj's alignment nudges (Launcher control)
                if (patchLockMode) cal *= testSkew() * lockCalM;                                           // V4: the measured fit (1 until calibrated)
                s *= cal; dw = b.getWidth() * s; dh = b.getHeight() * s;
                m.setScale(s, s);
                m.postTranslate(-(dw - dm.widthPixels) * (patchLockMode ? 0.5f : TjHooks.wallpaperX) - rect.left + Features.getInt(TjAccessibilityService.this, PATCH_DX_KEY, 0) + (patchLockMode ? lockCalDx + (lockCalDx == 0 && lockCalDy == 0 && lockCalM == 1f ? PatchProfile.lockShiftPx(android.os.Build.DEVICE, dm.widthPixels) : 0) : 0),   // v0.7.6 AH3: + the per-device shift of the model (Tj's MediaTek tablet: -90 px)
                        -(dh - dm.heightPixels) * 0.5f - rect.top + Features.getInt(TjAccessibilityService.this, PATCH_DY_KEY, 0) + (patchLockMode ? lockCalDy : 0));
                c.drawBitmap(b, m, p);
            }
        };
        WindowManager.LayoutParams lp = overlayParams(r, false);
        if (targetIsLock) lp.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;   // BE1: told (ACTION_OUTSIDE) when a finger goes down anywhere else on the lock screen: the earliest sign of a pull-up for the PIN
        lp.format = targetIsLock ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;   // V1: an OPAQUE window ignores alpha 0 (run 71: the lock patch stayed visible over the notifications and was even photographed by the calibration); TRANSLUCENT can really be hidden. The home patch keeps OPAQUE (v0.7.5 behaviour)
        lp.setTitle("TjWallpaperPatch");
        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(v, lp);
        patchLockMode = targetIsLock; patchHomeMode = targetIsHome; patchTouchable = false;
        v.setOnTouchListener(new View.OnTouchListener() {   // AM1: the window is NOT_TOUCHABLE except while the stock menu is open: then a tap on the hidden Screenshot spot closes the menu (like a tap outside) instead of taking a screenshot
            @Override public boolean onTouch(View view, android.view.MotionEvent e) {
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_OUTSIDE) { onOutsideTouch(); return false; }   // BE1
                if (!patchTouchable) return false;
                if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) { try { startClose(android.os.SystemClock.uptimeMillis(), "a tap on the hidden spot: our own BACK"); performGlobalAction(GLOBAL_ACTION_BACK); } catch (Throwable ignored) { } }   // BC2: the stock dismissal begins ~20 ms after the injected BACK
                return true;
            }
        });
        if (patchLockMode) { if (patchHomeMode) { patchResting = false; patchRestable = false; homeBuiltAtMs = 0; homeBuiltStep = -1; } else { boolean can = !overlayShown && PatchRest.visible(false, lockPatchAlways(), true, lastBouncer); patchRestable = can; patchResting = false; if (can) { restableSinceMs = android.os.SystemClock.uptimeMillis(); handler.removeCallbacks(settleRun); handler.postDelayed(settleRun, PatchRest.SETTLE_MS + 20); } } v.setAlpha(overlayShown ? 1f : 0f); /* BT1a / BT1b: a NEW lock window starts hidden and rests after PatchRest.SETTLE_MS (it used to start visible, ignoring every hold: a window created inside a pull-up or an unlock exit showed at once) */ logPatch("window created, resting=" + patchResting + ", " + (screenOnEpoch > 0 ? Math.max(0, System.currentTimeMillis() - screenOnEpoch) + " ms after the screen came on" : "screen-on time unknown") + ", screen " + (isScreenOn() ? "on" : "OFF (prepared in the dark)")); handler.removeCallbacks(restRecheck); handler.postDelayed(restRecheck, 400); handler.postDelayed(restRecheck, 1600); if (patchResting) Log.i(TAG, "PATCH rest on (new window) top=" + patchRestTop); Log.i(TAG, "PATCH window created: resting=" + patchResting + " [always=" + lockPatchAlways() + " switch=" + Features.get(this, PatchRest.KEY, true) + " wanted=" + lockPatchWanted() + " calibrating=" + calibrating() + " shot=" + (patchShotBmp != null) + " bouncer=" + lastBouncer + " overlayShown=" + overlayShown + "]"); }   // V1: on the lock screen it is only visible while the power menu is open (it must never paint over the clock / PIN pad); AE3: or RESTING (switch on, PIN pad not up)
        patchView = v; patchAt.set(r); patchRot = rotation();
        try { Rect cc = cachedCard(); if (cc != null) { Rect ca = cardAreaGeom(cc); patchRestTop = PatchRest.restTop(r.top, ca.bottom, r.bottom); } } catch (Throwable ignored) { }   // AE3: the resting patch paints only the part BELOW the card = the Screenshot slot
        patchStatus("active over " + r.toShortString());
        Log.i(TAG, "PATCH shown " + r.toShortString());
    }

    /** AC6: draws with brightness {@code k} (1 = unchanged): a black SRC_ATOP layer of alpha 1-k. */
    private float dimKCache = -1f;
    private android.graphics.ColorFilter dimFilterCache;
    private void setDim(android.graphics.Paint p, float k) {
        if (k >= 0.999f) { p.setColorFilter(null); return; }
        if (dimFilterCache == null || Math.abs(k - dimKCache) > 0.0005f) {   // one filter per brightness, not one per draw (a Go tablet's UI thread)
            dimFilterCache = new android.graphics.PorterDuffColorFilter(Color.argb(Math.round((1f - k) * 255), 0, 0, 0), android.graphics.PorterDuff.Mode.SRC_ATOP);
            dimKCache = k;
        }
        p.setColorFilter(dimFilterCache);
    }

    private static android.graphics.Paint newMaskPaint() {
        android.graphics.Paint mp = new android.graphics.Paint();
        mp.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
        return mp;
    }

    /**
     * AH3: the two soft-edge masks of the slot patch for the window {@code r}: OPEN menu = the top edge is hard and reaches just under our card (the card hides it, so the stock panel can never show through a
     * fade), RESTING = the top edge fades too (nothing sits above it). The opaque core = the learned panel plus a couple of px; the fringe fades outward on the other sides. Any failure leaves the masks null = the
     * old hard-edged drawing, never a crash.
     */
    private void buildPatchMasks(Rect r, android.util.DisplayMetrics dm) {
        patchMaskOpen = null; patchMaskRest = null;
        try {
            Rect panel = cachedCard();
            int w = r.width(), h = r.height();
            if (panel == null || w <= 0 || h <= 0 || (long) w * h > 500L * 500L) return;   // a slot window is about 130 x 130 px
            int e = Math.round(PatchRest.EDGE_DP * dm.density), f = Math.round(PatchRest.FEATHER_DP * dm.density), ov = Math.max(1, Math.round(PatchRest.OVERLAP_DP * dm.density));
            int cb = cardAreaGeom(panel).bottom;
            int extra = patchExtraPx(dm), bf = patchBottomFeatherPx(dm);   // BB1: the core reaches further down (the stock panel's shadow), the sides keep their margin
            int cl = panel.left - e - r.left, cr = panel.right + e - r.left, cbot = panel.bottom + e + extra - r.top;
            int fl = Math.min(f, Math.max(0, cl)), fr = Math.min(f, Math.max(0, w - cr)), fb = Math.min(bf, Math.max(0, h - cbot));
            int[] ce = PatchRest.coverDisplayEdges(r.left, r.right, r.bottom, dm.widthPixels, dm.heightPixels, w, h, cl, cr, cbot, fl, fr, fb);   // BC1: a side that lies on the display edge has nothing to blend into: its core runs to the edge (the stock card's edge line showed through the 2 px fringe)
            cl = ce[0]; cr = ce[1]; cbot = ce[2]; fl = ce[3]; fr = ce[4]; fb = ce[5];
            patchMaskOpen = alphaBitmap(PatchRest.mask(w, h, cl, cb - ov - r.top, cr, cbot, fl, 0, fr, fb), w, h);
            patchMaskRest = alphaBitmap(PatchRest.mask(w, h, cl, cb - r.top, cr, cbot, fl, Math.min(f, Math.max(0, cb - r.top)), fr, fb), w, h);
        } catch (Throwable t) { patchMaskOpen = null; patchMaskRest = null; Log.e(TAG, "patch masks failed", t); }
    }

    private static android.graphics.Bitmap alphaBitmap(byte[] a, int w, int h) {
        int[] px = new int[w * h];
        for (int i = 0; i < px.length; i++) px[i] = (a[i] & 0xff) << 24;
        return android.graphics.Bitmap.createBitmap(px, w, h, android.graphics.Bitmap.Config.ARGB_8888);
    }

    private void removePatch() {
        if (patchView == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(patchView); } catch (Throwable ignored) { }
        closing = false; handler.removeCallbacks(closeFinishRun);   // BC2
        handler.removeCallbacks(homeBuild); handler.removeCallbacks(homeShowCheck); patchHomeMode = false; homeBuiltAtMs = 0; homeBuiltStep = -1; homeWallBmp = null;   // BI1
        patchView = null; patchAt.setEmpty(); patchRot = -1; patchLockMode = false; patchResting = false; patchRestable = false; touchAwayUntilMs = 0; handler.removeCallbacks(touchBack); handler.removeCallbacks(padBack); handler.removeCallbacks(padCancelCheck); patchOpenHome = false; restableSinceMs = 0; handler.removeCallbacks(settleRun); lockShot = null; lockShotFresh = false; patchMaskOpen = null; patchMaskRest = null; patchShotBmp = null; patchTouchable = false; handler.removeCallbacks(touchableOn); handler.removeCallbacks(touchableOff); touchableOnPending = false;
        Log.i(TAG, "PATCH removed");
    }

    // ---------------------------------------------------------------- E1 / N2 / N3: the secret handshake picks the launcher; the lock screen covers all the switching
    // Watches WHICH TYPE of PIN-pad key is tapped (by view id), never which digit. Everything is started BEHIND the keyguard, so the unlock itself shows the right screen at once.
    private final UnlockMethods unlock = new UnlockMethods(new UnlockMethods.Enabled() {
        @Override public boolean on(UnlockMethods.Method m) { return methodOn(m); }
    });
    private final Runnable unlockApply = new Runnable() { @Override public void run() { applyUnlockChoice(); } };
    private boolean preArmedThisLock = false;       // the custom stack was already put in front during this lock
    private boolean flippedByHandshake = false;     // Mode was flipped to Quickstep by a handshake that has not unlocked yet
    private boolean lastBouncer = false;
    private final java.util.concurrent.atomic.AtomicBoolean listenerSet = new java.util.concurrent.atomic.AtomicBoolean(false);

    /** A method is on when its switch says so; if Tj switched every one off, the original (double erase) stays on so Quickstep can never become unreachable. */
    private boolean methodOn(UnlockMethods.Method m) {
        boolean any = false;
        for (UnlockMethods.Method x : UnlockMethods.Method.values()) if (Features.get(this, x.prefKey(), x.defaultOn)) { any = true; break; }
        if (!any) return m == UnlockMethods.Method.ERASE2;
        return Features.get(this, m.prefKey(), m.defaultOn);
    }

    /** The stored switches (no fallback): the fallback to double erase lives in {@link UnlockMethods#selected}. */
    private final UnlockMethods.Enabled storedEnabled = new UnlockMethods.Enabled() {
        @Override public boolean on(UnlockMethods.Method m) { return Features.get(TjAccessibilityService.this, m.prefKey(), m.defaultOn); }
    };

    private void ensureUnlockListener() {
        if (!listenerSet.compareAndSet(false, true)) return;
        unlock.setListener(new UnlockMethods.Listener() {
            @Override public void onArmed(UnlockMethods.Method m, boolean padMethodAtEnter) { try { secretAccepted(m); } catch (Throwable t) { Log.e(TAG, "secret accepted failed", t); } }
            @Override public void onDisarmed() { try { secretWithdrawn(); } catch (Throwable t) { Log.e(TAG, "secret withdrawn failed", t); } }
        });
    }

    private boolean keyguardLocked() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    /** AB2: the same answer, remembered for 250 ms. Only for the per-event early-outs (each real call is a binder round trip to system_server); screen on/off and unlock broadcasts re-check at once. */
    private long kgAt = -10_000; private boolean kgVal = false;
    private boolean keyguardLockedFast() {
        long up = android.os.SystemClock.uptimeMillis();
        if (up - kgAt < 250) return kgVal;
        kgVal = keyguardLocked(); kgAt = up;
        if (kgVal) lastKeyguardUpMs = up;
        return kgVal;
    }
    /** BN2: the last time the keyguard was seen up (any caller of {@link #keyguardLockedFast}, and USER_PRESENT): a SystemUI window event soon after it is the keyguard going away, not the shade (HomePatchRules.foreignTreatment). */
    private volatile long lastKeyguardUpMs = 0;

    private long lastPadKeyAt = 0;

    private void trackUnlockKey(AccessibilityEvent e) {
        try {
            if (e == null || e.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED) return;
            CharSequence pkg = e.getPackageName();
            if (pkg == null || !"com.android.systemui".contentEquals(pkg)) return;
            // The Enter tap unlocks the tablet, and its click event can be delivered AFTER the keyguard already reports "unlocked" (race seen in emulator runs 63/64: ten pad taps
            // reached us, the final Enter did not, so the handshake was never completed). A click of the PIN pad within 3 s of the previous pad tap still counts.
            long nowMs = System.currentTimeMillis();
            if (!keyguardLockedFast() && !keyguardLocked() && nowMs - lastPadKeyAt > 3000) return;   // CA1 U1: the 250 ms memo answers "locked" during PIN entry without a binder call per key; "unlocked" is asked again for real (same decision as before)
            ensureUnlockListener();
            AccessibilityNodeInfo n = e.getSource();
            UnlockPick.Key k = null;
            for (int i = 0; i < 3 && n != null && k == null; i++) { k = UnlockPick.classify(n.getViewIdResourceName()); n = n.getParent(); }   // ids only: nothing else of the key is read
            Log.i(TAG, "UNLOCKCLICK class=" + (k == null ? "none" : k.name()));   // diagnostics (full emulator runs 59-61): the KIND of key only, never which digit
            if (k == null) return;
            long now = System.currentTimeMillis();
            lastPadKeyAt = now;
            if (k == UnlockPick.Key.ENTER) {
                String lead = unlock.padLead();   // read BEFORE onEnter clears the pad: the first two key KINDS only (never a digit, never the length)
                long tapLag = Math.max(0, android.os.SystemClock.uptimeMillis() - e.getEventTime());
                lastEnterAt = now - tapLag;   // CA1 U1: the TAP's time, not the time this event was handled (the tablet's Enter lag reached 2164 ms and the summaries did not show it)
                UnlockMethods.Method m = unlock.onEnter(now);
                Log.i(TAG, "UNLOCKPICK enter tapped, handshake=" + (m == null ? "none" : m.id));
                LockLog.add(getApplicationContext(), "ENTER", "lag=" + Math.max(0, android.os.SystemClock.uptimeMillis() - e.getEventTime()) + "ms lead=" + (lead.isEmpty() ? "-" : lead) + " handshake=" + (m == null ? "none" : m.id) + (m == null && unlock.armedMethod() != null ? " (earlier gesture kept for this retry: " + unlock.armedMethod().id + ")" : ""));
                if (m == null) { sumNoteEnter(e.getEventTime()); coverAtEnter(); }
            } else unlock.onKey(k, now);
        } catch (Throwable t) { Log.e(TAG, "unlock key tracking failed", t); }
    }

    private final KeySeq returnVolCode = new KeySeq(ReturnMethods.SEQ_WINDOW_MS, ReturnMethods.VOLCODE);
    private final KeySeq returnBacks = new KeySeq(ReturnMethods.SEQ_WINDOW_MS, ReturnMethods.BACKS);
    private final KeySeq konami = new KeySeq(FailsafeKeys.WINDOW_MS, FailsafeKeys.KONAMI);
    private long lastVolUpKeyAt = 0, lastVolDownKeyAt = 0;

    /**
     * Hardware keys, never consumed. On the lock screen: the volume keys feed the unlock handshakes. Unlocked in QUICKSTEP: the "back to custom" key methods (N4).
     * Unlocked in CUSTOM: the Konami master failsafe (N5). Only key-down, only Back and the volume keys are looked at; nothing is stored.
     */
    @Override protected boolean onKeyEvent(android.view.KeyEvent ev) {
        try {
            if (ev != null && Features.get(this, "raw_key_log", false)) Log.i(TAG, "KEYRAW " + android.view.KeyEvent.keyCodeToString(ev.getKeyCode()) + " action=" + ev.getAction());   // research only (AB5, default off): which keys does Android's key filter ever hand a service?
            if (ev == null || ev.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
            int rc = ev.getRepeatCount();
            int code = ev.getKeyCode();
            boolean vol = code == android.view.KeyEvent.KEYCODE_VOLUME_UP || code == android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
            if (!vol && code != android.view.KeyEvent.KEYCODE_BACK) return false;
            long now = System.currentTimeMillis();
            android.content.Context app = getApplicationContext();
            if (rc == 0) Log.i(TAG, "KEY " + (code == android.view.KeyEvent.KEYCODE_BACK ? "back" : code == android.view.KeyEvent.KEYCODE_VOLUME_UP ? "vol-up" : "vol-down") + " locked=" + keyguardLocked() + " custom=" + Mode.isCustom(app));   // only Back / volume keys are ever looked at
            if (keyguardLocked()) {
                ensureUnlockListener();
                if (rc == 0 && vol && (methodOn(UnlockMethods.Method.VOLUME) || methodOn(UnlockMethods.Method.VOLUP_ERASE))) unlock.onVolume(code == android.view.KeyEvent.KEYCODE_VOLUME_UP, now);
                unlock.onHwKey(code, rc, now);      // O1: both-volume, ping-pong, hold-then-tap, three Backs + Volume Up
                return false;
            }
            if (rc != 0) return false;
            if (Mode.isCustom(app)) {
                if (FailsafeKeys.konami(app) && konami.onKey(code, now)) { buzz(); ProfileSession.goToQuickstep(app, "Konami key code"); }
            } else {
                boolean hit = false;
                if (ReturnMethods.on(app, ReturnMethods.Method.VOLCODE) && returnVolCode.onKey(code, now)) hit = true;
                if (ReturnMethods.on(app, ReturnMethods.Method.BACKS) && returnBacks.onKey(code, now)) hit = true;
                if (vol) {
                    if (code == android.view.KeyEvent.KEYCODE_VOLUME_UP) lastVolUpKeyAt = now; else lastVolDownKeyAt = now;
                    if (ReturnMethods.on(app, ReturnMethods.Method.VOLBOTH) && ReturnMethods.bothVolume(lastVolUpKeyAt, lastVolDownKeyAt, now)) { hit = true; lastVolUpKeyAt = lastVolDownKeyAt = 0; }
                }
                if (hit) { buzz(); ProfileSession.returnToCustom(app, "key code"); }
            }
        } catch (Throwable t) { Log.e(TAG, "key event failed", t); }
        return false;
    }

    /** The key filter is switched on only while a volume handshake is on (it needs the capability declared in tj_accessibility.xml). */
    private void updateKeyFilter() {
        try {
            android.accessibilityservice.AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) return;
            android.content.Context app = getApplicationContext();
            boolean want = UnlockMethods.needsKeys(storedEnabled)
                    || (Mode.isCustom(app) ? FailsafeKeys.konami(app) : ReturnMethods.needsKeys(app));
            boolean has = (info.flags & android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS) != 0;
            Log.i(TAG, "key filter want=" + want + " has=" + has + " custom=" + Mode.isCustom(app));
            if (want == has) return;
            if (want) info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            else info.flags &= ~android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
            Log.i(TAG, "key filter " + (want ? "ON" : "OFF"));
        } catch (Throwable t) { Log.e(TAG, "key filter update failed", t); }
    }

    private void buzz() {
        try {
            if (!ProfileSession.buzz(this)) return;
            android.os.Vibrator v = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Throwable ignored) { }
    }

    private String now() { return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()); }

    private void profileNote(String s) {
        Features.setString(this, ProfileSession.PREARM_REPORT_KEY, now() + "  " + s);
        Log.i(TAG, "PROFILE " + s);
    }

    /** A handshake was accepted while the lock screen is up: Quickstep is the target. Flip the mode and put the stock home in front BEHIND the keyguard. */
    private void secretAccepted(UnlockMethods.Method m) {
        buzz();
        android.content.Context app = getApplicationContext();
        if (Mode.isCustom(app)) { Mode.set(app, Mode.QUICKSTEP); flippedByHandshake = true; qsApp = null; resumedPkg = null; }   // review RV1: the lock began in custom = no live Quickstep session, so nothing to resume (a stale app would be cold-started)
        if (ProfileSession.stockBehindLockOnHandshake()) startQuickstepFront("handshake");   // AD5 (the app that was open, else the stock home) / AC9: Quickstep in front NOW, whatever the pre-arm switch says: the custom stack may already be in front (prepared at the wake) and must be replaced before the unlock reveals it
        preArmedThisLock = true;
        Features.setStringAsync(this, UnlockPick.LAST_KEY, now() + "  handshake '" + m.title + "' accepted -> Quickstep");
        profileNote("handshake '" + m.title + "' accepted: Quickstep in front behind the lock screen");
        Mode.confirmQuickstep(app, System.currentTimeMillis());   // AC1: dates this session after the lock it came from, even if the helper dies before the unlock is processed
        LockLog.add(app, "HANDSHAKE", m.id + " accepted, mode now " + Mode.get(app));
    }

    /** The handshake was withdrawn (plain retry after a wrong PIN, or the screen went dark): back to the custom account. */
    private void secretWithdrawn() {
        if (!flippedByHandshake) return;
        flippedByHandshake = false;
        android.content.Context app = getApplicationContext();
        if (!Mode.isCustom(app) && !CrashGuard.automationOff(app)) Mode.set(app, Mode.CUSTOM);
        preArmedThisLock = false;
        LockLog.add(app, "HANDSHAKE", "withdrawn (screen off or a plain retry too late), mode " + Mode.get(app));
        if (keyguardLocked()) prearmCustom("handshake withdrawn");
    }

    private void startRouter(String which) {
        Log.i(TAG, "ROUTER start " + which);
        LockLog.add(this, "START", "router " + which);
        try {
            startActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME).setPackage(getPackageName())
                    .putExtra(RouterActivity.EXTRA_PREARM, which)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (Throwable t) { Log.e(TAG, "router start (" + which + ") failed", t); }
    }

    /** Custom home + the target app (Solitaire) in front, behind the keyguard. Mode is left alone: the unlock decides. */
    private void prearmCustom(String why) {
        try {
            android.content.Context app = getApplicationContext();
            startRouter("custom");            // the router starts the custom home and THEN the target app (same activity, fixed order)
            preArmedThisLock = true;
            profileNote("lock screen (" + why + "): custom home" + (ReturnReset.autoOpenWanted(app) ? " + " + ReturnReset.appLabel(app) : " (Solitaire NOT started: \"Open Solitaire automatically\" is off)") + " put in front behind the keyguard");
        } catch (Throwable t) { Log.e(TAG, "prearm failed", t); }
    }

    private boolean preCleanedThisLock = false;       // O4: Quickstep's apps were closed at lock during this lock
    private final Runnable lockPrepareLater = new Runnable() { @Override public void run() { lockPrepare("screen off, locked"); } };

    /**
     * The tablet is LOCKED (at screen-off if it locks at once, or at the next wake). Do everything that does not need the screen NOW, so the unlock has nothing left to do:
     *  - O4: with the shell helper, a Quickstep session is closed here (apps + notifications + Recents cards) and Mode flips to custom behind the lock screen (tiles / keyboard follow),
     *  - O3: the custom home + Solitaire are put in front, behind the keyguard.
     * Never when the tablet is NOT locked (a plain wake must keep Quickstep's apps) and never while a handshake is armed.
     */
    private void lockPrepare(String why) {
        try {
            android.content.Context app = getApplicationContext();
            boolean locked = keyguardLocked();
            boolean armed = unlock.armedMethod() != null;
            boolean paused = CrashGuard.automationOff(app);
            LockLog.add(app, "LOCKPREP", why + ": locked=" + locked + " armed=" + armed + " mode=" + Mode.get(app) + (paused ? " PAUSED" : ""));
            if (locked) ProcessLife.noteLock(app);   // BG1: at what priority does Android hold this process while the tablet is locked? (one sample a minute; the report prints it)
            if (locked && !paused) lockQuietNow(why);   // AK1: on the lock screen Wi-Fi and Bluetooth are off, from any launcher (what was on is remembered for a gesture unlock); AW2: and the quick settings show the rotation tile only
            boolean cleanedNow = false;
            if (locked && !armed && ProfileSession.preCleanAtLock(ProfileSession.preClean(app), Mode.isCustom(app), paused, true, false, ReturnReset.invisibleReady(app), preCleanedThisLock)) {
                if (ReturnReset.cleanAtLock(app)) {
                    preCleanedThisLock = true; cleanedNow = true;
                    qsApp = null;     // AD5: the helper closed the apps: nothing is left to come back to
                    Mode.set(app, Mode.CUSTOM);
                    profileNote("lock (" + why + "): Quickstep's apps + notifications closed, custom account active behind the lock screen");
                }
            }
            if (locked && !paused && !armed && !cleanedNow && !Mode.isCustom(app) && !ReturnReset.invisibleReady(app)) LockLog.add(app, "LOCKPREP", "no shell helper: Quickstep's apps stay until the unlock, which then cleans on screen behind the cover (start the helper with the adb line to make the unlock instant)");   // BE3: the report says WHY an unlock was slow
            // BE2 (Tj: "at no time ... an app that is hidden from the app drawer inside the recent apps"): while locked, in the custom launcher, every task of a non-drawer app is removed by id (helper running, invisible)
            if (locked && !paused && !cleanedNow && !armed && Mode.isCustom(app)) RecentsSweep.requestAsync(app, "lock", RecentsSweep.HIDDEN_ONLY, null, 4000);
            if (locked && !paused && ReturnReset.cleanStart(app) && (!Mode.isCustom(app) || ReturnReset.bootCleanPending(app))) AppOrigin.refreshUsedCacheAsync(app);   // BE3: what the unlock needs to know about Recents is read now, not on the unlock path
            boolean dark = !isScreenOn();
            if (locked && !(shadeLookedDark && !dark)) { noteShade(shadeLevel(), "look at " + why); if (dark) shadeLookedDark = true; }   // review LOW2: the wake (SystemUI busy) skips it when the dark look already read the panel   // BY1c F1: the panel's real state BEFORE the patch is (re)shown: while dark it cannot change, so the wake shows the right thing (a missed event or a panel left open at screen-off)
            if (locked) { try { refreshLayers(); } catch (Throwable t) { Log.e(TAG, "patch pre-creation failed", t); } }   // BE1: the lock-screen patch window is created NOW, while the screen is dark, not in the wake-up burst
            if (ProfileSession.prearmCustomStackOnLockedWake(ProfileSession.prearm(app), locked, armed, preArmedThisLock, paused)) prearmCustom(why);
        } catch (Throwable t) { Log.e(TAG, "lock prepare failed", t); }
    }

    /** SCREEN_ON: the lock screen is what the screen shows (if it was not locked at screen-off, this is the first chance). */
    private void handleLockedWake() { lockPrepare("woke locked"); }

    /** AK1 + AW2 + AW3: the quiet lock screen rules, nothing else (no clean-up, no pre-arm): Wi-Fi / Bluetooth off, the rotation tile only. Safe to repeat. */
    private void lockQuietNow(String why) {
        lqLastWrite = 0; lqWrites = 0;
        LockQuiet.onLockAsync(getApplicationContext());
        BatterySaverGuard.onLockAsync(getApplicationContext(), why);   // BA2 (Tj, 2026-11-07): the lock screen always has battery saver OFF, from any launcher; Quickstep's own state is remembered for a gesture unlock
        applyLockTiles(why);
        handler.removeCallbacks(autoCalTrigger); handler.postDelayed(autoCalTrigger, 3000);   // AW5: a lock screen without a valid crop is calibrated once it is plain
    }

    /**
     * AW2 (Tj, 2026-11-06 evening): "The lock screen from any launcher at all times should only have the rotation tile in the quick settings". While the keyguard is up the custom tile list is written whatever
     * the mode says ({@link TileProfile#effectiveMode}); {@link #restoreUnlockTiles} puts the mode's own list back once the lock screen is gone. Idempotent, exception-safe, does nothing while automation is paused.
     */
    private void applyLockTiles(String why) {
        try {
            android.content.Context app = getApplicationContext();
            if (CrashGuard.automationOff(app) || !TileProfile.enabled(app)) return;
            TileProfile.apply(app, Mode.get(app));
            LockLog.add(app, "TILES", "lock screen (" + why + "): rotation list in place, mode " + Mode.get(app));
        } catch (Throwable t) { Log.e(TAG, "lock tiles failed", t); }
    }

    /** AW2: the lock screen is gone: the list of the active mode comes back (a gesture unlock into Quickstep: Tj's Quickstep tiles). Looked at twice: the keyguard may still report "locked" for a moment after USER_PRESENT. */
    private void restoreUnlockTiles() {
        try {
            android.content.Context app = getApplicationContext();
            if (CrashGuard.automationOff(app) || !TileProfile.enabled(app)) return;
            TileProfile.apply(app, Mode.get(app));
            handler.removeCallbacks(restoreTilesLater);
            handler.postDelayed(restoreTilesLater, 1500);
        } catch (Throwable t) { Log.e(TAG, "unlock tiles failed", t); }
    }
    private final Runnable restoreTilesLater = new Runnable() { @Override public void run() {
        try { android.content.Context app = getApplicationContext(); if (!CrashGuard.automationOff(app) && TileProfile.enabled(app) && !keyguardLocked()) TileProfile.apply(app, Mode.get(app)); } catch (Throwable t) { Log.e(TAG, "unlock tiles (later) failed", t); }
    } };

    private final ReturnMethods.Flurry returnFlurry = new ReturnMethods.Flurry();

    /** N4: side-key flurry while Quickstep is active = back to custom. (On a locked screen the lock-screen rules already end the session.) */
    private void flurryChange(boolean on, long t) {
        try {
            android.content.Context app = getApplicationContext();
            if (Mode.isCustom(app) || !ReturnMethods.on(app, ReturnMethods.Method.FLURRY)) { returnFlurry.reset(); return; }
            if (returnFlurry.onChange(on, t)) { buzz(); ProfileSession.returnToCustom(app, "side-key flurry"); }
        } catch (Throwable e) { Log.e(TAG, "flurry failed", e); }
    }

    private final Runnable coverCheck = new Runnable() { @Override public void run() {
        if (keyguardLocked()) hideResetCover();    // the PIN was wrong: the pad is still there, so the cover must not stay
    } };

    /** A PLAIN Enter tap while a Quickstep session is current: raise the black cover now, so the unlock animation never shows what Quickstep left behind. */
    private void coverAtEnter() {
        try {
            android.content.Context app = getApplicationContext();
            if (!ProfileSession.coverAtEnter(Mode.isCustom(app), CrashGuard.automationOff(app), false, ReturnReset.mayHaveWork(app), Features.get(app, ReturnReset.COVER_KEY, true))) return;
            showResetCover();
            handler.removeCallbacks(coverCheck);
            handler.postDelayed(coverCheck, 1800);
        } catch (Throwable t) { Log.e(TAG, "cover at enter failed", t); }
    }

    /** The unlock succeeded (USER_PRESENT). Handshake given = Quickstep (already in front); plain = the custom account (a Quickstep session ends here). */
    private void applyUnlockChoice() {
        lastUnlockUptime = android.os.SystemClock.uptimeMillis();   // AU2: a screenshot taken just before an unlock is refused
        curtainSwallowTouches();   // BL1: the unlock is confirmed: a curtain raised at the Enter tap (touches passed through so a wrong PIN stays retypable) now swallows them until it goes
        try { applyUnlockChoiceBody(); } finally {
            try { AnimationSpeedup.onUnlockDecided(getApplicationContext()); } catch (Throwable t) { Log.e(TAG, "animation sync failed", t); }   // BF1: from here the stored mode is the truth: a gesture unlock = the Quickstep session starts (animations off), a plain PIN = custom (they never went off)
            lqLastWrite = 0; lqWrites = 0; autoCalThisLock = 0; scrimThisLock = 0; restoreUnlockTiles(); }   // AW2 / AW3: whichever way the unlock went, the mode's own tile list is back and the lock-screen radio count starts afresh
    }

    private void applyUnlockChoiceBody() {
        try {
            android.content.Context app = getApplicationContext();
            handler.removeCallbacks(coverCheck);
            lastUnlockAt = System.currentTimeMillis();
            ensureUnlockListener();
            if (unlock.hasPendingPad()) {   // the tablet unlocked but the Enter click never classified (emulator runs 63-66): the unlock is the Enter
                UnlockMethods.Method pm = unlock.finishPendingPad(System.currentTimeMillis());
                Log.i(TAG, "UNLOCKPICK Enter click not recognised; pending pad entry evaluated at the unlock: " + (pm == null ? "none" : pm.id));
            }
            UnlockMethods.Method m = unlock.consume(System.currentTimeMillis());
            preArmedThisLock = false; preCleanedThisLock = false;
            boolean wasFlipped = flippedByHandshake;
            flippedByHandshake = false;
            unlock.reset();
            long sinceEnter = lastEnterAt > 0 ? System.currentTimeMillis() - lastEnterAt : -1;   // AC7: how long the PIN check took (the log is how a slow tablet shows itself)
            if (m != null) {
                if (Mode.isCustom(app)) Mode.set(app, Mode.QUICKSTEP);
                // The stock home was started behind the lock screen; only if the pre-start did not land (not in front now) press the router's Home.
                ComponentName stock = StockLauncher.find(this);
                boolean front = (stock != null && isForeground(stock.getPackageName())) || (resumedPkg != null && System.currentTimeMillis() - resumedAt < 15_000 && isForeground(resumedPkg));   // AD5: the resumed app counts (for this unlock only)
                if (!front) startQuickstepFront("after the unlock");
                Features.setStringAsync(this, UnlockPick.LAST_KEY, now() + "  Quickstep (handshake '" + m.title + "')" + (front ? "" : ", started after the unlock"));
                Log.i(TAG, "UNLOCKPICK quickstep via " + m.id + " front=" + front);
                Mode.confirmQuickstep(app, System.currentTimeMillis());   // AC1: this lock ended in a handshake, so the safety net below must not end this Quickstep session
                LockLog.add(app, "UNLOCK", "handshake " + m.id + " -> Quickstep, stock in front=" + front + ", Enter to unlock " + sinceEnter + " ms");
                LockQuiet.onUnlockAsync(app, true);   // AK1: a gesture unlock into Quickstep puts Wi-Fi / Bluetooth back as they were before the lock
                BatterySaverGuard.onUnlockAsync(app, true);   // BA2: ... and Quickstep's own battery saver state (only when Android's state cannot have been reset: not plugged in, same boot)
                verifyQuickstepFront(0);         // AC9: a late start of the custom home / Solitaire stand-in must not stay on top of the gesture's choice
                return;
            }
            qsApp = null; resumedPkg = null;   // AD5: a plain unlock ends the session: no app to come back to
            LockQuiet.onUnlockAsync(app, false);   // AK1: a plain unlock (custom launcher): the radios stay off; forget the memory of this lock
            BatterySaverGuard.onUnlockAsync(app, false);   // BA2: the custom launcher keeps the saver off; Quickstep's memory waits for a later gesture unlock
            boolean ends = ProfileSession.plainUnlockEndsQuickstep(Mode.isCustom(app), CrashGuard.automationOff(app));
            if (ends) {
                Mode.set(app, Mode.CUSTOM);
                startRouter("custom");           // quiet: the stack is already (or about to be) in front; no Home press, no double-Home
                ReturnReset.run(app);            // closes what Quickstep opened, clears the shade, radios off, Solitaire last (the cover is usually up already)
                Features.setStringAsync(this, UnlockPick.LAST_KEY, now() + "  custom launcher (plain unlock ended the Quickstep session)");
                Log.i(TAG, "UNLOCKPICK custom (session ended)");
                LockLog.add(app, "UNLOCK", "plain -> custom (a Quickstep session ended), Enter to unlock " + sinceEnter + " ms");
            } else if (!CrashGuard.automationOff(app) && ReturnReset.cleanStart(app) && ReturnReset.bootCleanPending(app)) {
                // BE2/BE3: the first unlock after a boot starts the custom session clean: the persisted Recents list of the last boot (Quickstep's apps among them) goes
                Features.setStringAsync(this, UnlockPick.LAST_KEY, now() + "  custom launcher (first unlock after a boot: Recents cleared)");
                Log.i(TAG, "UNLOCKPICK custom (boot clean)");
                LockLog.add(app, "UNLOCK", "plain, first unlock after a boot: the custom session starts clean (Recents cleared), Enter to unlock " + sinceEnter + " ms");
                ReturnReset.run(app);
                handler.postDelayed(new Runnable() { @Override public void run() { ReturnReset.clearBootClean(getApplicationContext()); } }, 10_000);   // whatever run() decided, the flag never repeats the clean-up at every later unlock
            } else {
                hideResetCover();
                Features.setStringAsync(this, UnlockPick.LAST_KEY, now() + "  custom launcher (plain unlock)" + (wasFlipped ? " after a withdrawn handshake" : ""));
                Log.i(TAG, "UNLOCKPICK custom");
                LockLog.add(app, "UNLOCK", "plain, mode " + Mode.get(app) + (CrashGuard.automationOff(app) ? " (automation paused: the mode is left alone)" : "") + ", Enter to unlock " + sinceEnter + " ms");
                sweepSoon("unlock", 700);   // BE2: the drawer-only guarantee after the unlock (helper running, invisible)
            }
        } catch (Throwable t) { Log.e(TAG, "unlock choice failed", t); }
    }

    /**
     * AC1 safety net ({@link UsageGap}): if the helper was not running when the tablet was unlocked, nothing saw the plain PIN and Android showed Quickstep. Now that it is back: the keyguard was shown after the
     * Quickstep session began and no handshake was confirmed, so the session ended at that unlock - the custom launcher takes the tablet, exactly like a plain unlock does. Cheap when nothing is wrong
     * (a few preference reads; the usage query only runs in Quickstep mode).
     */
    private void reconcileAfterGap(String why) {
        try {
            android.content.Context app = getApplicationContext();
            if (!UsageGap.sessionEndedWhileAway(app)) return;
            LockLog.add(app, "RECONCILE", why + ": the keyguard was shown after the Quickstep session began and no handshake was confirmed: ending the session like a plain unlock");
            qsApp = null; resumedPkg = null;
            Mode.set(app, Mode.CUSTOM);
            startRouter("custom");
            ReturnReset.run(app);
            Features.setStringAsync(this, UnlockPick.LAST_KEY, now() + "  custom launcher (the helper was away at the unlock; corrected when it came back)");
        } catch (Throwable t) { Log.e(TAG, "reconcile failed", t); }
    }

    private long lastEnterAt = 0;   // when the last PIN-pad Enter was tapped (for the lock log's "Enter to unlock" time)
    private long lastUnlockAt = 0;  // when the last unlock was handled (applyUnlockChoice): windows that appear in the next moments are what was under the lock screen (ProfileSession.windowIsLockLeftover)

    /**
     * AC9 (Tj: "even though the custom launcher is prepared to open on the lock screen, if I do the gesture for quickstep, always open quickstep"): after an unlock that chose Quickstep, look again at
     * {@link ProfileSession#VERIFY_DELAYS_MS} - if OUR launcher or the Solitaire stand-in is the window in front (a start made at the wake finished late), start Quickstep's home again. Bounded (two looks),
     * and it never touches any OTHER app: someone who opened an app within a second of the unlock is left alone.
     */
    private void verifyQuickstepFront(final int attempt) {
        if (attempt >= ProfileSession.VERIFY_DELAYS_MS.length) return;
        handler.postDelayed(new Runnable() { @Override public void run() {
            try {
                android.content.Context app = getApplicationContext();
                if (Mode.isCustom(app) || keyguardLocked()) return;   // the choice changed meanwhile (a return method, a new lock): nothing to enforce
                String target = ReturnReset.targetPackage(app);
                boolean ours = isForeground(getPackageName()), tgt = target != null && isForeground(target);
                if (ProfileSession.restartQuickstepAfterUnlock(ours, tgt, attempt)) {
                    LockLog.add(app, "VERIFY", "attempt " + (attempt + 1) + ": " + (ours ? "the custom launcher" : "Solitaire") + " is in front after a Quickstep choice: starting Quickstep's home again");
                    startQuickstepFront("verify " + (attempt + 1));
                    verifyQuickstepFront(attempt + 1);
                } else LockLog.add(app, "VERIFY", "attempt " + (attempt + 1) + ": Quickstep (or another app) is in front, nothing to do");
            } catch (Throwable t) { Log.e(TAG, "quickstep verify failed", t); }
        } }, ProfileSession.VERIFY_DELAYS_MS[attempt]);
    }

    // ---------------------------------------------------------------- B4: has another app been opened since Recents was last cleared?
    private final java.util.HashMap<String, Long> dirtyLogged = new java.util.HashMap<String, Long>();   // BN8e: when each package's DIRTY line was last written (event thread only)

    private void trackDirty(AccessibilityEvent e) {
        try {
            if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
            if (Mode.isCustom(this)) return;   // D5: only apps whose window appears while QUICKSTEP is on count as Quickstep-opened (the custom launcher marks its own launches itself)
            String ime = null;
            String cur = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.DEFAULT_INPUT_METHOD);
            if (cur != null && cur.contains("/")) ime = cur.substring(0, cur.indexOf('/'));
            ComponentName stock = stockCached();   // AD8: cached (one lookup a minute), not a package-manager call per window change
            if (RecentsDirty.shouldMark(e.getPackageName().toString(), getPackageName(), stock == null ? null : stock.getPackageName(), ime, ReturnReset.targetPackage(this), true)) {
                // AC2 follow-up: the app that was in front BEHIND the lock screen shows for a moment while the gesture's unlock goes away. Tj did not open it in Quickstep: it stays the custom account's.
                if (ProfileSession.windowIsLockLeftover(keyguardLockedFast(), unlock.armedMethod() != null, System.currentTimeMillis(), lastUnlockAt)) {
                    LockLog.add(getApplicationContext(), "WINDOW", e.getPackageName() + " appeared under the lock screen / during the unlock: NOT counted as opened in Quickstep (window " + e.getClassName() + ")");
                    return;
                }
                RecentsDirty.mark(this); AppOrigin.markQuickstep(this, e.getPackageName().toString());
                if (RecentsDirty.shouldLog(dirtyLogged, e.getPackageName().toString(), System.currentTimeMillis(), RecentsDirty.LOG_GAP_MS))   // BN8e: one line per package per minute
                    LockLog.add(getApplicationContext(), "DIRTY", e.getPackageName() + " was marked as opened in Quickstep (window " + e.getClassName() + ")");
            }
        } catch (Throwable t) { Log.e(TAG, "dirty tracking failed", t); }
    }

    // ---------------------------------------------------------------- BL1: the HOME CURTAIN, the home-looking cover of the no-helper return reset (rules: HomeCurtainRules; store: HomeCurtain; design: research/BL1_HOME_CURTAIN_2026-11-08.md)
    /** Prefs shown in the tablet report: what the curtain did last (shown, or why the plain black cover was used) and what the last capture said. */
    public static final String CURTAIN_STATE_KEY = "home_curtain_state", CURTAIN_CAPTURE_KEY = "home_curtain_capture";
    private volatile boolean curtainPreloading = false;
    private boolean curtainPending = false, curtainBuilding = false;
    private long curtainLastStartUp = 0;
    private int curtainWaits = 0;
    private String curtainCaptureNote = "";

    private boolean curtainSwitchOn() { return Features.get(this, HomeCurtainRules.KEY, HomeCurtainRules.defaultOn(debuggable())); }

    /** The curtain is wanted but the plain-wallpaper picture of THIS page is not stored yet (a calibration made before the curtain existed kept only the crop): the next calibration takes it (one flash). */
    private boolean curtainWallMissing() {
        try { int rot = rotation(), step = homeStepNow(); String sig = homeSig(); return curtainSwitchOn() && !HomeCurtain.hasWall(this, rot, step, sig) && !HomeCurtain.wallFailed(this, rot, step, sig); } catch (Throwable t) { return false; }   // a wallpaper picture that could not be kept (a full disk) is not asked for again and again: that would cost a flash per quiet moment
    }

    /** The facts the rules need to say whether the stored picture may be the cover now. Main thread, NO decoding and no disk read of the picture (two preference reads and a file check). */
    private HomeCurtainRules.UseFacts curtainUseFacts(android.util.DisplayMetrics dm) {
        HomeCurtainRules.UseFacts f = new HomeCurtainRules.UseFacts();
        f.switchOn = curtainSwitchOn();
        f.rotation = rotation();
        f.dispW = dm.widthPixels; f.dispH = dm.heightPixels;
        f.currentSig = homeSig();
        int cr = HomeCurtain.cachedRot(); String cs = HomeCurtain.cachedSig();
        android.graphics.Bitmap pic = cr < 0 ? null : HomeCurtain.cachedFor(cr, cs);
        if (pic == null || cr != f.rotation || !cs.equals(f.currentSig)) curtainPreloadAsync();   // nothing fitting in memory (a restarted process, another rotation or wallpaper): the right one is read for the NEXT unlock (the worker looks at the files)
        f.hasPicture = pic != null;
        f.picRotation = pic != null ? cr : f.rotation;
        if (pic != null) { f.pictureSig = cs; f.picW = pic.getWidth(); f.picH = pic.getHeight(); f.ageMs = System.currentTimeMillis() - HomeCurtain.cachedSavedAt(); }
        return f;
    }

    /** Reads the stored curtain into memory (worker thread, one at a time): at service start, after a trim, and whenever a cover found it stored but not in memory. */
    private void curtainPreloadAsync() {
        if (curtainPreloading || !curtainSwitchOn()) return;
        curtainPreloading = true;
        final android.content.Context app = getApplicationContext();
        new Thread(new Runnable() { @Override public void run() {
            try { android.util.DisplayMetrics dm = realMetrics(); HomeCurtain.preload(app, rotation(), homeSig(), dm.widthPixels, dm.heightPixels); }
            catch (Throwable t) { Log.e(TAG, "curtain preload failed", t); }
            finally { curtainPreloading = false; }
        } }, "tj-curtain-load").start();
    }

    /** What the cover did, for Launcher control, the tablet report and the lock log (written off the main thread). */
    private void curtainNoteUse(boolean curtain, String why, boolean touchable) {
        try {
            String line = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  "
                    + (curtain ? "the curtain was shown (touches " + (touchable ? "swallowed" : "pass through until the unlock is confirmed") + ")" : "black cover: " + why);
            Features.setStringAsync(this, CURTAIN_STATE_KEY, line);
            LockLog.add(getApplicationContext(), "COVER", line);
        } catch (Throwable ignored) { }
    }

    /** USER_PRESENT: the unlock is confirmed, so the curtain now swallows touches until it goes (the black cover never did). Main thread. */
    private void curtainSwallowTouches() {
        try {
            if (resetCover == null || !resetCoverIsCurtain || resetCoverLp == null) return;
            if ((resetCoverLp.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) == 0) return;
            resetCoverLp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(resetCover, resetCoverLp);
            Log.i(TAG, "RESET curtain swallows touches now (the unlock is confirmed)");
        } catch (Throwable t) { Log.e(TAG, "curtain touch switch failed", t); }
    }

    /** Service teardown (unbind, interrupt, destroy): nothing of the curtain's capture may fire later, and nothing may stay marked busy for the next connection (code review 2026-11-08). */
    private void curtainStop() { try { handler.removeCallbacks(curtainBuild); curtainPending = false; curtainBuilding = false; } catch (Throwable ignored) { } }

    private final Runnable curtainBuild = new Runnable() { @Override public void run() { curtainPending = false; try { buildCurtainNow(); } catch (Throwable t) { curtainBuilding = false; Log.e(TAG, "curtain capture failed", t); } } };

    /** From {@link #homeDrew} (every draw of the Launcher): look at the curtain picture again once the home has been quiet. At most ONE posted runnable; nothing else on the draw path. */
    private void scheduleCurtain() {
        if (curtainBuilding && since(curtainLastStartUp) > 30_000) { curtainBuilding = false; curtainCaptureNote = "a capture never reported back (30 s): the next one may start"; }   // a lost callback must not stop every later capture
        if (curtainPending || curtainBuilding) return;
        if (!curtainSwitchOn()) return;
        curtainWaits = 0;   // a fresh wish (a draw after a quiet spell) gets a fresh bounded retry budget; the old budget must not stay spent for good (code review 2026-11-08)
        curtainPending = true;
        handler.postDelayed(curtainBuild, HomeCurtainRules.QUIET_DRAW_MS + 100);
    }

    /** Captures the Launcher's own pixels (no flash: its own window, PixelCopy) when the custom home is quiet, and re-makes the stored picture only when the home really changed. Main thread; the heavy part runs on a worker. */
    private void buildCurtainNow() {
        if (curtainBuilding) return;
        final android.content.Context app = getApplicationContext();
        final HomeCurtainRules.CaptureFacts f = new HomeCurtainRules.CaptureFacts();
        f.switchOn = curtainSwitchOn();
        if (!f.switchOn) return;
        f.customMode = Mode.isCustom(app); f.unlocked = !keyguardLockedFast(); f.screenOn = isScreenOn();
        f.calibrating = homeCalRunning || calibrating() || scrimStage != 0; f.building = curtainBuilding;
        f.positionKnown = TjHooks.wallpaperReported;
        final int rot = rotation(); final int step = homeStepNow(); final String sig = homeSig();
        f.wallKnown = f.positionKnown && HomeCurtain.hasWall(app, rot, step, sig);
        f.homeAtRest = TjHooks.homeScreenShowing() && !TjHooks.homeFloatingViewOpen() && !TjHooks.homeBusy();
        f.msSinceDraw = since(TjHooks.lastHomeDrawMs); f.msSinceTouch = since(homeLastTouchMs()); f.msSinceLastCapture = since(curtainLastStartUp);
        String why = HomeCurtainRules.whyNotCapture(f);
        if (!why.isEmpty()) {
            curtainCaptureNote = "not capturing: " + why;
            boolean quietish = f.customMode && f.unlocked && f.screenOn && f.positionKnown;
            if (quietish && !f.wallKnown) {   // the wallpaper picture of this page comes with a calibration (one flash); the calibration's own rules decide when
                if (!f.calibrating) { handler.removeCallbacks(homeCalTrigger); handler.postDelayed(homeCalTrigger, HomePatchRules.CAL_DRAW_QUIET_MS + 600); }
            } else if (quietish && ++curtainWaits <= 12) { curtainPending = true; handler.postDelayed(curtainBuild, 1500); }   // a "not yet" reason: look again, a bounded number of times (the next draw starts a new wish anyway)
            return;
        }
        curtainWaits = 0;
        final int[] win = TjHooks.launcherWindowBounds();   // {left, top, width, height} of the Launcher window on the screen
        final android.util.DisplayMetrics dm = realMetrics();
        if (win == null || win[2] <= 0 || win[3] <= 0 || win[0] < 0 || win[1] < 0 || win[0] >= dm.widthPixels || win[1] >= dm.heightPixels) { curtainCaptureNote = "no Launcher window to copy"; return; }
        final int left = win[0], top = win[1];
        final int cw = Math.min(win[2], dm.widthPixels - left), ch = Math.min(win[3], dm.heightPixels - top);
        curtainBuilding = true; curtainLastStartUp = android.os.SystemClock.uptimeMillis();
        HomeLayer.copy(new Rect(0, 0, cw, ch), handler, new HomeLayer.Done() {
            @Override public void done(final android.graphics.Bitmap layer, String problem) {
                if (layer == null) { curtainBuilding = false; curtainCaptureNote = "could not read the Launcher's pixels (" + problem + ")"; Log.i(TAG, "CURTAIN layer copy failed: " + problem); return; }
                new Thread(new Runnable() { @Override public void run() { curtainFinishOffMain(app, layer, rot, step, sig, dm.widthPixels, dm.heightPixels, left, top); } }, "tj-curtain").start();
            }
        });
    }

    /** Worker thread: hash the layer, refuse a blank copy, and only when the home changed compose it over the wallpaper picture and store it (PNG) and keep it in memory. Always ends by clearing the busy flag on the main thread. */
    private void curtainFinishOffMain(final android.content.Context app, final android.graphics.Bitmap layer, final int rot, final int step, final String sig, final int dispW, final int dispH, final int left, final int top) {
        String result;
        boolean noteIt = true;
        try {
            int[] opaque = new int[1]; int h = 17; int[] row = new int[layer.getWidth()];
            for (int y = 0; y < layer.getHeight(); y += HomeCurtainRules.SAMPLE_STEP_ROW) { layer.getPixels(row, 0, layer.getWidth(), 0, y, layer.getWidth(), 1); h = HomeCurtainRules.foldRow(h, row, row.length, opaque); }
            if (HomeCurtainRules.blank(opaque)) result = "the copy was blank (the Launcher had not drawn yet)";
            else {
                HomeCurtainRules.Meta m = HomeCurtain.meta(app, rot);
                boolean changed = HomeCurtainRules.changed(m != null, m == null ? "" : m.sig, m == null ? 0 : m.step, m == null ? 0 : m.hash, sig, step, h);
                if (!changed) {
                    HomeCurtain.preload(app, rot, sig, dispW, dispH);
                    if (m != null && HomeCurtainRules.stampDue(System.currentTimeMillis() - m.savedAtMs)) { HomeCurtain.refreshStamp(app, rot); result = "unchanged: the picture's time stamp was renewed"; }   // a still-correct picture must never expire just because the home was not rearranged
                    else result = "unchanged since the last picture";
                    noteIt = false;
                }
                else {
                    android.graphics.Bitmap wall = HomeCurtain.loadWall(app, rot, step, sig);
                    if (wall == null) result = "the wallpaper picture could not be loaded";
                    else {
                        android.graphics.Bitmap comp = HomeLayer.composeInPlace(wall, layer, left, top);   // the layer is drawn straight onto the decoded wallpaper picture (no copy)
                        if (comp == null) wall.recycle();
                        if (comp == null) result = "could not compose the picture";
                        else {
                            long now = System.currentTimeMillis();
                            if (HomeCurtain.saveCurtain(app, rot, comp, sig, step, h, now)) { HomeCurtain.setCache(rot, sig, now, comp); result = "picture saved (" + comp.getWidth() + "x" + comp.getHeight() + ", layer " + layer.getWidth() + "x" + layer.getHeight() + " at " + left + "," + top + ", wallpaper position " + step + ")"; }
                            else { comp.recycle(); result = "the picture could not be written"; }
                        }
                    }
                }
            }
        } catch (Throwable t) { Log.e(TAG, "curtain capture failed", t); result = "failed: " + t; }
        finally { layer.recycle(); }
        final String res = result; final boolean note = noteIt;
        handler.post(new Runnable() { @Override public void run() {
            curtainBuilding = false; curtainCaptureNote = res;
            Log.i(TAG, "CURTAIN " + res);
            if (note) Features.setStringAsync(TjAccessibilityService.this, CURTAIN_CAPTURE_KEY, new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + "  " + res);
        } });
    }

    /** One line for Launcher control: is the curtain ready, and if not why the black cover is used. */
    public String curtainInfo() {
        try {
            if (!curtainSwitchOn()) return "off (the black cover is used)";
            HomeCurtainRules.UseFacts f = curtainUseFacts(realMetrics());
            String use = HomeCurtainRules.whyNotUse(f);
            HomeCurtainRules.Meta m = HomeCurtain.meta(this, rotation());
            String ready = use.isEmpty() ? "ready: a picture of your home screen taken " + Math.max(0, (System.currentTimeMillis() - m.savedAtMs) / 60000) + " min ago" : "the black cover is used (" + use + ")";
            return ready + "; wallpaper pictures kept: " + HomeCurtain.wallCount(this) + "; capture: " + (curtainCaptureNote.isEmpty() ? "(nothing yet)" : curtainCaptureNote) + "; last use: " + Features.getString(this, CURTAIN_STATE_KEY, "(not used yet)");
        } catch (Throwable t) { return "(unreadable: " + t + ")"; }
    }

    // ---------------------------------------------------------------- B4: black cover while the reset drives the shade / Recents (never touchable, always removed)
    private View resetCover;
    private boolean resetCoverIsCurtain;                      // BL1: the cover shows the picture of the home screen (not plain black)
    private WindowManager.LayoutParams resetCoverLp;          // BL1: kept so the curtain can start swallowing touches at USER_PRESENT
    private final Runnable coverFailsafe = new Runnable() { @Override public void run() { hideResetCover(); } };

    /** Full-screen black, untouchable window: hides the shade / Recents flicker. Removed when Solitaire is verified in front, and by a 4.5 s failsafe. */
    public void showResetCover() {
        try {
            if (resetCover != null) return;
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
            // BL1 (Tj, 2026-11-08 21:11 UTC: "Make the home looking curtain instead of the black screen. An identical screenshot of the current home screen"): the picture of the home screen when there is a valid one IN MEMORY
            // (never a disk read here: this runs at the Enter tap on the main thread), the plain black view as before on ANY doubt, so the curtain is never worse than the cover it replaces.
            View v = null; boolean curtain = false; String why = "";
            try {
                HomeCurtainRules.UseFacts uf = curtainUseFacts(dm);
                why = HomeCurtainRules.whyNotUse(uf);
                if (why.isEmpty()) {
                    android.graphics.Bitmap pic = HomeCurtain.cachedFor(uf.rotation, uf.currentSig);
                    if (pic == null) { why = "the picture is not in memory yet"; curtainPreloadAsync(); }
                    else {
                        android.widget.ImageView iv = new android.widget.ImageView(this);
                        iv.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                        iv.setImageBitmap(pic);
                        iv.setOnTouchListener(new View.OnTouchListener() { @Override public boolean onTouch(View view, android.view.MotionEvent ev) {   // a tap on the picture is swallowed (once the window is touchable)
                            if (ev.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) Log.i(TAG, "RESET curtain swallowed a tap at " + (int) ev.getRawX() + "," + (int) ev.getRawY());   // emulator section cu proves the swallowing with this line
                            return true; } });
                        v = iv; curtain = true;
                    }
                }
            } catch (Throwable t) { Log.e(TAG, "curtain picture failed", t); why = "failed: " + t; v = null; curtain = false; }
            if (v == null) { v = new View(this); v.setBackgroundColor(Color.BLACK); }
            // A cover raised at the ENTER tap must not swallow touches before the unlock is confirmed (a wrong PIN has to stay retypable; the black cover never swallowed any touch): it becomes touchable at USER_PRESENT.
            boolean touchable = curtain && HomeCurtainRules.unlockConfirmed(android.os.SystemClock.uptimeMillis(), lastUnlockUptime);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(dm.widthPixels, dm.heightPixels, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.OPAQUE);
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.setTitle("TjResetCover");
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            try { wm.addView(v, lp); }
            catch (Throwable addFailed) {
                Log.e(TAG, "cover window failed", addFailed);
                if (!curtain) { resetCover = null; return; }   // the black view failing is the old, handled failure: no cover, the reset runs without one
                why = "the picture window failed: " + addFailed; curtain = false; touchable = false;   // never worse than before: the same window with the plain black view
                v = new View(this); v.setBackgroundColor(Color.BLACK);
                lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                wm.addView(v, lp);
            }
            resetCover = v; resetCoverIsCurtain = curtain; resetCoverLp = lp; if (sumCoverUpAt == 0) sumCoverUpAt = android.os.SystemClock.uptimeMillis();   // BK1c
            handler.removeCallbacks(coverFailsafe);
            handler.postDelayed(coverFailsafe, 4500);
            curtainNoteUse(curtain, why, touchable);
            Log.i(TAG, "RESET cover shown" + (curtain ? " (curtain)" : ""));
        } catch (Throwable t) { Log.e(TAG, "cover failed", t); resetCover = null; }
    }

    public void hideResetCover() {
        handler.removeCallbacks(coverFailsafe);
        if (resetCover == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(resetCover); } catch (Throwable ignored) { }
        resetCover = null; resetCoverIsCurtain = false; resetCoverLp = null; sumCoverGoneAt = android.os.SystemClock.uptimeMillis();   // BK1c
        Log.i(TAG, "RESET cover removed");
        if (sumArmed) { try { sumEmit(false); } catch (Throwable t) { Log.e(TAG, "unlock summary failed", t); } }   // review 2026-11-08: the line is complete the moment the cover goes (the first frame came earlier, behind it)
    }

    /** Presses Home (used by the reset when there is no app to open, so Recents / the shade are not left on screen). */
    public void goHome() { performGlobalAction(GLOBAL_ACTION_HOME); }

    /**
     * BE3 (clean start): the custom home must be the screen once the clean-up is done (or when there was nothing to clean). Looks every 40 ms for at most 1.5 s: our launcher in front = done, the black cover goes at
     * that very moment (it used to stay until a cold Solitaire start was verified); Recents / the shade still in front after 200 ms = one Home press. Never throws, the cover's own 4.5 s failsafe still exists.
     */
    public void settleHome() {
        final long t0 = android.os.SystemClock.uptimeMillis();
        final boolean[] pressed = { false };
        handler.post(new Runnable() { @Override public void run() {
            try {
                long dt = android.os.SystemClock.uptimeMillis() - t0;
                if (isForeground(getPackageName())) {
                    hideResetCover();
                    AnimationSpeedup.releaseReset(getApplicationContext(), "the custom home is in front");   // BF1 (Tj: animations back on "immediately ... in the custom launcher"): no 700 ms tail once the home is the screen
                    appendReport("clean start: the custom home is on screen (+" + dt + " ms)");
                    LockLog.add(getApplicationContext(), "HOME", "custom home in front +" + dt + " ms after the clean-up" + (pressed[0] ? " (one Home press)" : ""));
                    return;
                }
                if (!pressed[0] && dt >= 200) { pressed[0] = true; goHome(); }
                if (dt < 1500) { handler.postDelayed(this, 40); return; }
                hideResetCover();
                AnimationSpeedup.releaseReset(getApplicationContext(), "settle timed out");
                appendReport("clean start: the custom home was not confirmed in front after " + dt + " ms (cover removed anyway)");
                LockLog.add(getApplicationContext(), "HOME", "custom home NOT confirmed in front after " + dt + " ms; cover removed anyway");
            } catch (Throwable t) { Log.e(TAG, "settle home failed", t); hideResetCover(); }
        } });
    }

    /** BE2: the drawer-only sweep of Recents a moment from now (helper running; skips the app on screen). */
    private void sweepSoon(final String why, long delayMs) {
        handler.postDelayed(new Runnable() { @Override public void run() {
            try {
                String fg = fgPkg;
                java.util.Set<String> skip = (fg == null || fg.isEmpty() || fg.equals(getPackageName())) ? null : java.util.Collections.singleton(fg);
                RecentsSweep.requestAsync(getApplicationContext(), why, RecentsSweep.HIDDEN_ONLY, skip, 3000);
            } catch (Throwable t) { Log.e(TAG, "sweep request failed", t); }
        } }, delayMs);
    }

    private boolean hiddenSeen = false;   // BE2: an app of no drawer has been on screen since the last time the custom home / the Recents screen was shown

    /**
     * BE2: a window of {@code pk} is now in front. A hidden app (not in the drawer, not system-like) on screen is remembered; the moment the CUSTOM HOME or the Recents screen comes up after it, its task is swept (400 ms
     * later, by id, helper running), before a Recents card could be drawn. Never in the middle of its own flow: a share sheet, a file picker or a permission prompt that the hidden app opened does NOT trigger anything
     * (the first version swept at any change of the front window and would have closed the app under such a dialog: code review 2026-11-07).
     */
    private void noteHiddenForSweep(String pk) {
        try {
            android.content.Context app = getApplicationContext();
            if (!Mode.isCustom(app) || !RecentsSweep.enabled(app) || keyguardLockedFast()) { hiddenSeen = false; return; }
            boolean atHome = pk.equals(getPackageName()) || pk.equals(stockPkg());   // the custom home, or the Recents screen
            boolean inDrawer = atHome || hiddenSeen || HiddenApps.isPackageShown(app, pk);   // cheap first tests: a drawer app (the common case) ends here
            boolean mayClose = !atHome && !hiddenSeen && !inDrawer && ReturnReset.guardFor(app).mayClose(pk);
            int step = RecentsSweep.hiddenSeenStep(hiddenSeen, atHome, inDrawer, mayClose, RecentsSweep.systemLike(pk));
            if (step == RecentsSweep.STEP_REMEMBER) hiddenSeen = true;
            else if (step == RecentsSweep.STEP_SWEEP) {
                hiddenSeen = false;
                handler.postDelayed(new Runnable() { @Override public void run() {
                    try { RecentsSweep.requestAsync(getApplicationContext(), "a hidden app was on screen and the home came back", RecentsSweep.HIDDEN_ONLY, null, 1500); } catch (Throwable ignored) { }
                } }, 400);
            }
        } catch (Throwable t) { Log.e(TAG, "hidden app check failed", t); }
    }

    /** Public wrapper for the reset: TRUE = notification icons in the status bar, FALSE = none, null = cannot tell. */
    public Boolean hasNotificationIcons() { return notificationIconsPresent(); }

    // ---------------------------------------------------------------- B6: the "pencil" (Edit order of settings) in the quick-settings panel
    public static final String PEN_KEY = "hide_qs_pencil";   // switch, default ON
    private View penView;
    private final Rect penAt = new Rect();
    private long lastPenMs = 0;
    private boolean penPending = false;
    /** AN1: the pencil found last time (kept un-recycled): following it is ONE node refresh, a whole-tree search in SystemUI's shade window is the expensive part and is spaced out. */
    private AccessibilityNodeInfo penNode;
    private boolean penSearched = false;
    /** Review of BQ1b: the world and the switches the cover depends on, re-read by every DELAYED look (a 350 ms look after a finger could otherwise re-create a cover that was removed in between: the tablet locked, the switch turned off). */
    private boolean pencilWanted() { return coverWorld() && Features.get(this, PEN_KEY, true) && !DemoModeProfile.active(this); }

    private final Runnable penEval = new Runnable() { @Override public void run() {
        penPending = false;
        if (!pencilWanted()) { if (penView != null) removePencil(); return; }
        long wait = EventNoise.minPencilSearchGapMs(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) - (android.os.SystemClock.uptimeMillis() - lastPenFullMs);
        if (penNode == null && wait > 0) { penPending = true; handler.postDelayed(this, wait); return; }   // no known pencil to follow and a full search is not due yet: look again when it is
        updatePencil(true);
    } };

    /** Look at SystemUI events only (and keep checking while a cover is up, so it follows the panel and goes with it). Throttled. */
    private void schedulePencil(AccessibilityEvent e) {
        // N3: Quickstep needs no cover over the pencil. BK1b (Tj, 2026-11-08 18:16 UTC: "Can the pencil in the notification panel be hidden on the lock screen? If so, do it."): the LOCK screen counts as the custom world, exactly as for the
        // power-menu cover (coverWorld, AD7a). Without the shell helper the stored mode stays Quickstep while the tablet is locked after a Quickstep session, so `Mode.isCustom` alone left the pencil uncovered on the lock screen.
        if (!coverWorld() || !Features.get(this, PEN_KEY, true)) {
            if (penView != null) removePencil();
            if (debuggable() && e != null && e.getPackageName() != null && "com.android.systemui".contentEquals(e.getPackageName())) {   // debug builds only, once a second: why no cover (emulator section b6 needs it)
                long up = android.os.SystemClock.uptimeMillis();
                if (up - penSkipLoggedMs > 1000) { penSkipLoggedMs = up; Log.i(TAG, "PENCIL skipped: custom=" + Mode.isCustom(this) + " locked=" + keyguardLockedFast() + " switch=" + Features.get(this, PEN_KEY, true)); }
            }
            return;
        }
        boolean sysui = e != null && e.getPackageName() != null && "com.android.systemui".contentEquals(e.getPackageName());
        if (!sysui && penView == null) return;   // (an event-less follow-up of a deferred burst does NOT start a search: review 2026-11-08, a 100 ms whole-tree search after every lock-screen burst was too much for what it saved; the next SystemUI event starts it)
        if (DemoModeProfile.active(this)) { if (penView != null) removePencil(); return; }   // something else already turned the retail flag on (the pencil is gone natively); read only for SystemUI events (AB2)
        long now = android.os.SystemClock.uptimeMillis();
        int type = e == null ? 0 : e.getEventType();
        if (sysui) penFinger.systemUiEvent(now);   // BQ1b: SystemUI is redrawing: the wait after a finger is not over yet
        if (sysui) penQuicken("SystemUI is redrawing");   // BN8b: a shown cover is watched closely while the panel may be moving (does nothing when no cover is up)
        if (!EventNoise.pencilSearchOnEvent(type, lastBouncer, penShown)) return;   // CA1 U1: a PIN key cannot open the quick settings: no whole-tree search (now or 90 ms later) per key while the pad is up and no cover is shown
        if (now - lastPenMs >= 40) {
            lastPenMs = now; updatePencil(now - lastPenFullMs >= EventNoise.minPencilSearchGapMs(type));   // AN1: following a known pencil every 40 ms is cheap; a full tree search is spaced out in a burst of redraws
            if (debuggable()) { long end = android.os.SystemClock.uptimeMillis(); if (end - now >= 15) Log.i(TAG, "PATCH pencil took " + (end - now) + " ms t=" + end); }   // BJ1 diagnostic (debug builds only), see the scan line in leadingEdge
        }
        if (!penPending) { penPending = true; handler.postDelayed(penEval, 90); }
    }

    /** {@code allowFull}: a whole-tree search may run now if the known pencil cannot be followed. Without it a failed refresh just leaves the cover as it is until the next allowed search. */
    private void updatePencil(boolean allowFull) {
        try {
            penSearched = false; penFollowed = PencilRules.NO_KNOWN;
            final long lookStart = android.os.SystemClock.uptimeMillis();   // review of BQ1b: the pencil's place is as old as the START of the look that read it
            Rect r = findPencil(allowFull);
            final long lookEnd = android.os.SystemClock.uptimeMillis();
            if (r == null) {
                // BN4 (Tj, 2026-11-09 03:02 UTC: "as soon as I slide the notification panel back up I see a white box on the lock screen"): a pencil that was being followed and is gone (hidden, collapsed, INVISIBLE views
                // cannot be refreshed) takes the cover away AT ONCE; before, only a whole-tree search that found nothing did, and that one waits for its spacing gap: the opaque white cover stayed on the lock screen.
                if (PencilRules.hideCover(penFollowed, penSearched)) hidePencil(penFollowed == PencilRules.LOST ? "the followed pencil is gone" : "no pencil found");
                if (penSearched) penFinger.reset();   // review of BQ1b: a whole-tree search found NO pencil = the panel is closed. A closing swipe never gets a look that finds the pencil, so the finger wait outlived the panel and held the cover away from the NEXT pull
                if (PencilRules.searchGapWaived(penFollowed, penSearched)) {   // it may only have been a failed refresh (no search ran in this call): search promptly (the gap is waived once) and cover the pencil again if it is still there
                    lastPenFullMs = 0;
                    if (!penPending) { penPending = true; handler.postDelayed(penEval, 30); }
                } else if (penFinger.waitingAt(lookEnd) && !penPending) {   // review of BQ1b: while a finger wait is on (3 s at most) the hold loop must not depend on the next SystemUI event
                    penPending = true; handler.postDelayed(penEval, PencilRules.SlideGuard.RELOOK_MS);
                }
                return;
            }
            if (PencilRules.staleLook(lookStart, lookEnd, penStaleRun)) {   // review of BQ1b: a SLOW look read the place up to that long ago: it neither shows nor moves the cover, a quick re-look (one node refresh now) decides
                penStaleRun++;
                if (!penPending) { penPending = true; handler.postDelayed(penEval, PencilRules.SlideGuard.RELOOK_MS); }
                if (debuggable()) Log.i(TAG, "PENCIL look took " + (lookEnd - lookStart) + " ms: its place is stale, a quick re-look decides");
                return;
            }
            penStaleRun = 0;
            penConfirmedMs = lookStart;
            // BN8b: a pencil that moves UP is a panel sliding away; a cover that follows it trails it over the lock screen as a white box. Keep the cover away until two looks agree (the panel at rest).
            final boolean fingerWasWaiting = penFinger.waiting();
            boolean restOk = penSlide.allowCover(r.top, penConfirmedMs);       // both guards are fed at EVERY look (no short-circuit): each keeps its own history
            boolean fingerOk = penFinger.allowCover(penConfirmedMs);           // BQ1b: after a finger went down the cover waits until SystemUI is quiet
            if (fingerWasWaiting && fingerOk) penAfterFinger = true;           // review of BQ1b: the cover that comes back after a finger is watched FAST (a finger that rests and then slides is not announced again: ACTION_OUTSIDE fires at DOWN only)
            if (!restOk || !fingerOk) {
                boolean wasUp = penShown;
                if (penShown) hidePencil(!restOk ? "the panel is sliding up" : "a finger went down a moment ago");
                if (wasUp && !restOk && penConfirmedMs - penHeldLoggedMs > 1500) { penHeldLoggedMs = penConfirmedMs; LockLog.add(getApplicationContext(), "PENCIL", "cover held away: the panel is sliding up (the pencil is at a new place at every look)"); }
                if (!penPending) { penPending = true; handler.postDelayed(penEval, PencilRules.SlideGuard.RELOOK_MS); }   // the events stop when the panel stops: look again soon, the cover comes back when two looks agree
                return;
            }
            showPencil(r);
            if (penAfterFinger) { penAfterFinger = false; penQuicken("the cover is back after a finger"); }
        } catch (Throwable t) { Log.e(TAG, "pencil update failed", t); removePencil(); }
    }

    private final PencilRules.SlideGuard penSlide = new PencilRules.SlideGuard();   // BN8b
    private final PencilRules.FingerGuard penFinger = new PencilRules.FingerGuard();   // BQ1b: a finger went down: the cover is away at once and returns when the panel is quiet
    private boolean penAfterFinger = false;   // review of BQ1b: a finger wait has just ended: the next time the cover is shown it is watched fast
    private int penStaleRun = 0;              // review of BQ1b: slow looks in a row (PencilRules.staleLook)
    private long penHeldLoggedMs = 0, penLockShownMs = 0, penLockHiddenMs = 0;

    private int penFollowed = PencilRules.NO_KNOWN;   // BN4: what following the known node gave in the last findPencil
    private long penConfirmedMs = 0;                  // BN4: the last time a visible pencil was seen (for the "hidden N ms after" line)
    private long penLateLoggedMs = 0;
    /** BN4: while the cover is up it is looked at every {@link PencilRules#WATCH_MS} even without an event (the events stop when the panel stops moving, and the last one can come before SystemUI hides the pencil). One node refresh. */
    private final Runnable penWatch = new Runnable() { @Override public void run() {
        try {
            if (penView == null || !penShown) return;
            if (penNode == null) { hidePencil("the pencil node is gone"); return; }   // a cover must always have a pencil it follows
            updatePencil(false);
            if (penView != null && penShown) startPenWatch();   // BN8b: fast or slow (PencilRules.watchGapMs); never two pending looks
        } catch (Throwable t) { Log.e(TAG, "pencil watch failed", t); }
    } };

    private long penFastUntil = 0;   // BN8b: until this uptime the shown cover is looked at every PencilRules.FAST_WATCH_MS

    /** BN8b: something may be about to move the panel ({@code why}: a finger went down somewhere, SystemUI is redrawing, the pencil was just found at a new place): look at the covered pencil every FAST_WATCH_MS for a while. Main thread. */
    private void penQuicken(String why) {
        if (penView == null || !penShown) return;
        long now = android.os.SystemClock.uptimeMillis();
        boolean wasFast = PencilRules.watchGapMs(now, penFastUntil) == PencilRules.FAST_WATCH_MS;
        penFastUntil = now + PencilRules.FAST_FOR_MS;
        if (!wasFast) { Log.i(TAG, "PENCIL fast watch (" + why + ")"); startPenWatch(); }   // the slow look may be up to WATCH_MS away: bring the next one forward
    }

    private void dropPenNode() {
        AccessibilityNodeInfo n = penNode;
        penNode = null;
        if (n != null) { try { n.recycle(); } catch (Throwable ignored) { } }
    }

    private static boolean pencilBoundsOk(Rect b) { return b.width() > 0 && b.height() > 0 && b.width() < 400 && b.height() < 400; }

    /** Bounds of the visible Edit pencil, or null. AN1: first the pencil found last time is refreshed (one node); only when that fails (and a search is allowed) the shade window is searched. */
    private Rect findPencil(boolean allowFull) {
        AccessibilityNodeInfo known = penNode;
        if (known != null) {
            boolean refreshed = false, visible = false, boundsOk = false; Rect b = new Rect();
            try {
                refreshed = known.refresh();
                if (refreshed) { visible = known.isVisibleToUser(); if (visible) { known.getBoundsInScreen(b); boundsOk = pencilBoundsOk(b); } }
            } catch (Throwable ignored) { }
            penFollowed = PencilRules.follow(true, refreshed, visible, boundsOk);   // BN4
            if (penFollowed == PencilRules.FOUND) { penFast++; penSearched = true; return b; }
            dropPenNode();   // gone, hidden or moved to a bad place: the cover goes NOW; search again (below) when allowed
            // Review of BQ1b: the cover is taken away BEFORE the whole-tree search below (0.5-1.3 s of main thread on the tablet), not after it: a panel that collapsed without a touch on the cover (or whose finger-down came while the
            // cover was hidden and untouchable) left the white box on the lock screen for the whole search. A pencil that is still there is covered again when the search has found it.
            if (penShown) hidePencil("the followed pencil is gone");
        }
        if (!allowFull) return null;
        penSearched = true;
        long t0 = android.os.SystemClock.uptimeMillis(); lastPenFullMs = t0;
        try { return findPencilFull(); }
        finally { long d = android.os.SystemClock.uptimeMillis() - t0; penFull++; penFullMs += d; if (d > penFullMax) penFullMax = d; }
    }

    /** The whole-tree search: bounds of the visible Edit pencil (framework id android:id/edit, description "Edit order of settings."), or null. Remembers the node it found (see {@link #penNode}). */
    private Rect findPencilFull() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return null;
        for (AccessibilityWindowInfo w : windows) {
            if (!tallSystemWindow(w)) continue;   // AB2: the pencil is in SystemUI's shade window; never wake an app (or the keyboard) just to read its package name; BY1a B2: nor a short system window (the collapsed bar, the navigation bar) or our overlays
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) continue;
            try {
                CharSequence pkg = root.getPackageName();
                if (pkg == null || !"com.android.systemui".contentEquals(pkg)) continue;
                List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByViewId("android:id/edit");
                if (hit == null) continue;
                for (AccessibilityNodeInfo n : hit) {
                    CharSequence cd = n.getContentDescription();
                    if (!n.isVisibleToUser() || cd == null || !cd.toString().toLowerCase(java.util.Locale.ROOT).contains("edit")) continue;
                    Rect b = new Rect(); n.getBoundsInScreen(b);
                    if (pencilBoundsOk(b)) {
                        dropPenNode();
                        try { penNode = AccessibilityNodeInfo.obtain(n); } catch (Throwable ignored) { penNode = null; }
                        if (penNode == null) return null;   // a cover must always have a node it follows (penWatch hides one that has none: found without a node = no cover, not a 150 ms flicker; code review 2026-11-09)
                        return b;
                    }
                }
            } finally { root.recycle(); }
        }
        return null;
    }

    private boolean penShown = false;
    private long penSkipLoggedMs = 0;

    private WindowManager.LayoutParams penParams(Rect r, boolean touchable) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(r.width(), r.height(), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | (touchable ? WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),   // touchable when shown on purpose: it swallows taps meant for the pencil; BN8b: and it is told (ACTION_OUTSIDE) when a finger goes down anywhere else
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = r.left; lp.y = r.top;
        lp.setTitle("TjPencilCover");
        return lp;
    }

    private void showPencil(Rect r) {
        boolean wasShown = penShown;
        boolean moved = !penAt.equals(r);
        showPencilCover(r);
        if (!wasShown || moved || !gearShown) gearFollow(r);   // BR1: the gear overlay rides on the cover; a look that changes nothing costs nothing here
    }

    private void showPencilCover(Rect r) {
        // D4: the cover window stays attached (invisible, untouchable) once it exists, so showing it is one redraw + one layout update, not a window creation.
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (penView != null) {
            if (!penShown) { penView.setAlpha(1f); penShown = true; wm.updateViewLayout(penView, penParams(r, true)); penAt.set(r); Log.i(TAG, "PENCIL cover shown " + r.toShortString()); penLockLog(true, "cover shown"); startPenWatch(); }
            else if (!penAt.equals(r)) { wm.updateViewLayout(penView, penParams(r, true)); penAt.set(r); penQuicken("the pencil moved"); }   // BN8b: it is moving: the next look is due in FAST_WATCH_MS, not at the next event
            return;
        }
        WindowManager.LayoutParams lp = penParams(r, true);
        if (penView == null) {
            View v = new View(this);
            v.setBackgroundColor(Color.WHITE);   // the stock panel is pure white (255,255,255) in Tj's photo
            v.setOnTouchListener(new View.OnTouchListener() { @Override public boolean onTouch(View view, android.view.MotionEvent ev) {
                if (ev.getActionMasked() == android.view.MotionEvent.ACTION_OUTSIDE) { penFingerDown(); return false; }   // BN8b / BQ1b: the earliest sign that the panel may be about to move
                return true;
            } });
            wm.addView(v, lp);
            penView = v; penAt.set(r); penShown = true;
            Log.i(TAG, "PENCIL cover shown " + r.toShortString());
            penLockLog(true, "cover shown (new window)");
            startPenWatch();
        } else if (!penAt.equals(r)) {
            wm.updateViewLayout(penView, lp);
            penAt.set(r);
        }
    }

    /**
     * BQ1b: a finger went down on the screen while the cover is up (the touch listener, main thread). The panel may be about to slide, and every look at it is slow (a binder call into SystemUI): the cover goes AT ONCE, with
     * no look, and {@link PencilRules.FingerGuard} brings it back when the panel has been quiet. One look is booked for the end of the minimum wait; the looks after it follow the SlideGuard's rhythm.
     */
    private void penFingerDown() {
        long now = android.os.SystemClock.uptimeMillis();
        penFinger.fingerDown(now);
        if (penShown) hidePencil("a finger went down: the panel may be about to move");
        handler.removeCallbacks(penEval); penPending = true; handler.postDelayed(penEval, PencilRules.FingerGuard.HOLD_MS);
    }

    /** One line in the lock-screen log (the tablet report): when the cover came and went, to the millisecond, with the reason. At most one 'shown' and one 'hidden' line every 250 ms (one gate per kind: a hide right after a show must not be dropped, review of BQ1b); only while the tablet is locked. */
    private void penLockLog(boolean shown, String text) {
        try {
            long up = android.os.SystemClock.uptimeMillis();
            if (!keyguardLockedFast()) return;
            if (shown) { if (up - penLockShownMs < 250) return; penLockShownMs = up; }
            else { if (up - penLockHiddenMs < 250) return; penLockHiddenMs = up; }
            LockLog.add(getApplicationContext(), "PENCIL", text);
        } catch (Throwable ignored) { }
    }

    private void startPenWatch() { handler.removeCallbacks(penWatch); handler.postDelayed(penWatch, PencilRules.watchGapMs(android.os.SystemClock.uptimeMillis(), penFastUntil)); }

    /** Panel closed: hide the cover but keep the window (warm) so the next open is instant; a real removal happens on removePencil(). */
    private void hidePencil(String why) {
        gearCoverAway(why);   // BR1: the gear overlay never outlives the cover (cheap when it is not up)
        if (penView == null || !penShown) return;
        handler.removeCallbacks(penWatch);
        try {
            penView.setAlpha(0f); penShown = false;
            ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(penView, penParams(penAt, false));
            long age = penConfirmedMs > 0 ? android.os.SystemClock.uptimeMillis() - penConfirmedMs : -1;
            Log.i(TAG, "PENCIL cover hidden (kept warm; " + why + (age >= 0 ? ", the pencil was last seen " + age + " ms ago" : "") + ")");
            // BN4: for the tablet report: on the lock screen a hide that came late (the white box) shows in the lock log with its age (one line a second at most)
            long up = android.os.SystemClock.uptimeMillis();
            if (age >= 150 && up - penLateLoggedMs > 1000 && keyguardLockedFast()) { penLateLoggedMs = up; LockLog.add(getApplicationContext(), "PENCIL", "cover hidden " + age + " ms after the pencil was last seen (" + why + ")"); }
            else penLockLog(false, "cover hidden (" + why + ")" + (age >= 0 ? ", pencil last seen " + age + " ms ago" : ""));   // BQ1b: a timeline of the cover on the lock screen for the next report
        } catch (Throwable t) { removePencil(); }
    }

    private void removePencil() {
        removeGear();   // BR1
        dropPenNode();
        handler.removeCallbacks(penWatch);
        if (penView == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(penView); } catch (Throwable ignored) { }
        penView = null; penAt.setEmpty(); penShown = false; penSlide.reset(); penFinger.reset(); penAfterFinger = false; penStaleRun = 0;
        Log.i(TAG, "PENCIL cover removed");
    }

    // ---------------------------------------------------------------- BR1: the gear ("Open settings.") in the quick-settings footer opens the Settings look-alike
    // Tj, 2026-11-10 19:27 UTC: "Make the settings gear icon in the custom launcher notification panel open the spoof settings app ... instead of the regular android settings". Design, AOSP facts and known gaps:
    // research/GEAR_TO_LOOKALIKE_2026-11-10.md. A transparent, touchable window over the gear; it is shown exactly while the pencil cover is shown (same find / follow / guards, so it is never over a moving panel), at the pencil's
    // place plus an offset learned once (GearRules). A tap collapses the shade and starts the look-alike; the real gear is untouched everywhere else (Quickstep, the lock screen, a panel that is moving).
    private View gearView;
    private final Rect gearAt = new Rect();
    private boolean gearShown = false, gearLearning = false, gearDown = false;
    private int gearTries = 0;
    private int[] gearOff;                 // the gear's edges minus the pencil's (GearRules.offset); null = not learned yet
    private String gearOffKey = "";        // the screen facts the offset was learned under (GearRules.key)
    private float gearDownX, gearDownY;
    private long gearTapMs = 0;

    /** Cheap exits first: this is asked at every look of the lock-screen cover too. */
    private boolean gearWanted() {
        if (keyguardLockedFast() || !Mode.isCustom(this)) return false;
        return GearRules.wanted(Features.get(this, GearRules.KEY, GearRules.defaultOn(debuggable())), true, false, SettingsSpoof.switchOn(this));
    }

    private String gearKeyNow() {
        try {
            android.view.Display d = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            android.util.DisplayMetrics m = new android.util.DisplayMetrics();
            d.getRealMetrics(m);
            return GearRules.key(m.widthPixels, m.heightPixels, m.densityDpi, d.getRotation());
        } catch (Throwable t) { return "?"; }
    }

    private void gearStatus(String what) {
        try { Features.setStringAsync(this, GearRules.STATUS_KEY, GearRules.statusLine(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date()), what)); } catch (Throwable ignored) { }
    }

    /** The cover was hidden or removed: a new panel visit starts (the learn tries count again) and the overlay goes with it. */
    private void gearCoverAway(String why) { gearTries = 0; gearHide("the cover is away: " + why); }

    /** Called with the cover up at {@code pencil}: shows, moves, hides or removes the overlay, or starts learning where the gear is. */
    private void gearFollow(Rect pencil) {
        try {
            if (gearOff == null && gearView == null && gearTries >= GearRules.MAX_LEARN_TRIES) return;   // gave up for this panel visit: nothing to do until the cover comes back (gearCoverAway resets the count)
            boolean keyOk = gearOff != null && gearKeyNow().equals(gearOffKey);
            switch (GearRules.decide(gearWanted(), penShown, gearOff != null, keyOk)) {
                case GearRules.REMOVE: removeGear(); break;
                case GearRules.HIDE: gearHide("the cover is away"); break;
                case GearRules.LEARN:
                    gearHide("the gear's place has to be learned first");
                    if (!gearLearning && gearTries < GearRules.MAX_LEARN_TRIES) { gearLearning = true; handler.postDelayed(gearLearn, GearRules.LEARN_DELAY_MS); }
                    break;
                default: gearShow(pencil);
            }
        } catch (Throwable t) { Log.e(TAG, "gear follow failed", t); removeGear(); }
    }

    /** Bounds of the visible gear in SystemUI's shade window, or null. One whole-tree view-id search (like the pencil's); it runs only to learn the offset. */
    private int[] findGearFull() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return null;
        for (AccessibilityWindowInfo w : windows) {
            if (!EventNoise.isSystemWindow(w.getType())) continue;
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) continue;
            try {
                CharSequence pkg = root.getPackageName();
                if (pkg == null || !"com.android.systemui".contentEquals(pkg)) continue;
                List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByViewId(GearRules.VIEW_ID);
                if (hit == null) continue;
                int[] found = null;
                for (AccessibilityNodeInfo n : hit) {
                    CharSequence cd = n.getContentDescription();
                    if (found == null && n.isVisibleToUser() && (cd == null || cd.toString().toLowerCase(java.util.Locale.ROOT).contains(GearRules.DESCRIPTION_PART))) {
                        Rect b = new Rect(); n.getBoundsInScreen(b);
                        int[] r = {b.left, b.top, b.right, b.bottom};
                        if (GearRules.boundsOk(r)) found = r;
                    }
                    try { n.recycle(); } catch (Throwable ignored) { }
                }
                if (found != null) return found;
            } finally { root.recycle(); }
        }
        return null;
    }

    /** Learns where the gear sits relative to the pencil (once per screen layout): one gear search while the panel is at rest, checked by reading the pencil again. */
    private final Runnable gearLearn = new Runnable() { @Override public void run() {
        gearLearning = false;
        try {
            if (!penShown || penNode == null || !gearWanted()) return;
            Rect p = new Rect(penAt);
            int[] pencil = {p.left, p.top, p.right, p.bottom};
            int[] gear = findGearFull();
            boolean atRest = false;
            if (gear != null) {   // the panel must not have moved while the search ran: the pencil is read again and must be where it was
                try {
                    AccessibilityNodeInfo pn = penNode;
                    if (pn != null && pn.refresh() && pn.isVisibleToUser()) { Rect now = new Rect(); pn.getBoundsInScreen(now); atRest = GearRules.same(pencil, new int[] {now.left, now.top, now.right, now.bottom}); }
                } catch (Throwable ignored) { }
            }
            if (gear == null || !atRest) {
                gearTries++;
                String why = gear == null ? "the gear was not found in the footer" : "the panel moved while the gear was searched";
                Log.i(TAG, "GEAR learn try " + gearTries + ": " + why);
                if (gearTries < GearRules.MAX_LEARN_TRIES) { gearLearning = true; handler.postDelayed(this, GearRules.LEARN_RETRY_MS); }
                else gearStatus(why + " (gave up for this panel visit)");
                return;
            }
            gearOff = GearRules.offset(pencil, gear);
            gearOffKey = gearKeyNow();
            gearTries = 0;
            Log.i(TAG, "GEAR learned: pencil " + p.toShortString() + ", gear " + new Rect(gear[0], gear[1], gear[2], gear[3]).toShortString());
            gearStatus("learned the gear's place (" + gearOffKey + ")");
            gearFollow(new Rect(penAt));
        } catch (Throwable t) { Log.e(TAG, "gear learn failed", t); }
    } };

    private WindowManager.LayoutParams gearParams(Rect r, boolean touchable) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(r.width(), r.height(), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),   // untouchable while away: taps then reach whatever is under it
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = r.left; lp.y = r.top;
        lp.setTitle("TjGearOverlay");
        return lp;
    }

    private void gearShow(Rect pencil) {
        int[] g = GearRules.place(new int[] {pencil.left, pencil.top, pencil.right, pencil.bottom}, gearOff);
        if (!GearRules.boundsOk(g)) { gearHide("the learned place is not a button any more"); gearOff = null; return; }
        Rect r = new Rect(g[0], g[1], g[2], g[3]);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (gearView == null) {
            View v = new View(this);   // draws nothing: only its touches matter
            v.setOnTouchListener(gearTouch);
            wm.addView(v, gearParams(r, true));
            gearView = v;
        } else if (!gearShown || !gearAt.equals(r)) {
            wm.updateViewLayout(gearView, gearParams(r, true));
        } else return;
        gearAt.set(r);
        if (!gearShown) { gearShown = true; Log.i(TAG, "GEAR overlay shown " + r.toShortString()); }
    }

    /** Untouchable and kept warm, like the pencil cover: the next show is one layout update. */
    private void gearHide(String why) {
        if (gearView == null || !gearShown) return;
        gearShown = false;
        try {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(gearView, gearParams(gearAt, false));
            Log.i(TAG, "GEAR overlay hidden (" + why + ")");
        } catch (Throwable t) { removeGear(); }
    }

    private void removeGear() {
        handler.removeCallbacks(gearLearn); gearLearning = false; gearTries = 0; gearDown = false;
        View v = gearView;
        gearView = null; gearShown = false; gearAt.setEmpty();
        if (v == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(v); } catch (Throwable ignored) { }
        Log.i(TAG, "GEAR overlay removed");
    }

    /** A tap = DOWN and UP close together on the overlay. A DOWN here means the overlay was touchable at that moment, so the UP counts even if the cover has hidden it since (a finger-down elsewhere hides both). */
    private final View.OnTouchListener gearTouch = new View.OnTouchListener() { @Override public boolean onTouch(View view, android.view.MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case android.view.MotionEvent.ACTION_DOWN: gearDown = true; gearDownX = ev.getRawX(); gearDownY = ev.getRawY(); return true;
            case android.view.MotionEvent.ACTION_UP: {
                boolean tap = gearDown && GearRules.isTap(gearDownX, gearDownY, ev.getRawX(), ev.getRawY(), android.view.ViewConfiguration.get(TjAccessibilityService.this).getScaledTouchSlop());
                gearDown = false;
                if (tap) gearTapped();
                return true;
            }
            case android.view.MotionEvent.ACTION_CANCEL: gearDown = false; return true;
            default: return true;
        }
    } };

    /** What the stock gear does is collapse the shade and start Settings; this collapses the shade and starts the look-alike (the intent the custom drawer uses). If that cannot be done the REAL Settings opens, as before. */
    private void gearTapped() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - gearTapMs < 800) return;   // a double tap opens it once
        gearTapMs = now;
        String how;
        boolean collapsed = collapseShade();
        try {
            if (!gearWanted()) throw new IllegalStateException("no longer wanted");
            startActivity(new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    .setComponent(new ComponentName(getPackageName(), SettingsSpoof.ALIAS))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            how = "tap: opened the look-alike (shade collapsed: " + collapsed + ")";
        } catch (Throwable t) {
            how = "tap: the look-alike could not be opened (" + t.getClass().getSimpleName() + "), opened the real Settings";
            try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (Throwable t2) { how += "; that failed too (" + t2.getClass().getSimpleName() + ")"; }
        }
        Log.i(TAG, "GEAR " + how);
        gearStatus(how);
        gearHide("tapped");
    }

    /** StatusBarManager.collapsePanels() (hidden API on the greylist, permission EXPAND_STATUS_BAR is in the manifest): the stock gear dismisses the shade itself, an activity started from outside does not. */
    private boolean collapseShade() {
        try {
            Object sbm = getSystemService("statusbar");
            sbm.getClass().getMethod("collapsePanels").invoke(sbm);
            return true;
        } catch (Throwable t) { Log.w(TAG, "GEAR collapsePanels failed: " + t); return false; }
    }

    // ---------------------------------------------------------------- X1: the number on the lock screen (Tj relaxed the lock-screen rule for this)
    private View lockView;
    private boolean lockReceiverOn = false;
    private long lastLockEvalMs = 0;
    private boolean lockEvalPending = false;
    private final Runnable lockEval = new Runnable() { @Override public void run() { lockEvalPending = false; updateLockNumber(); } };
    private final android.content.BroadcastReceiver lockReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, android.content.Intent i) {
            kgAt = -10_000;   // AB2: screen on/off and unlock change the keyguard: never answer these from the 250 ms memo
            try {   // BF1 (Tj: animations back on "immediately" on the lock screen): the screen going OFF is handled before anything else of this broadcast, so the lock screen never shows with them off (the screen is dark: no patch work waits for it)
                if (android.content.Intent.ACTION_SCREEN_OFF.equals(i == null ? null : i.getAction())) AnimationSpeedup.onScreenOff(getApplicationContext());
            } catch (Throwable t) { Log.e(TAG, "animation sync failed", t); }
            handler.post(layerRefresh);   // BE1: the patch window first (a no-op when it was prepared while the screen was dark); the PIN-pad scan of lockEval comes after it
            handler.post(lockEval); handler.postDelayed(lockEval, 400);
            if (android.content.Intent.ACTION_SCREEN_ON.equals(i == null ? null : i.getAction())) {   // BF1: the wake is handled AFTER the patch work above (code review: the settings writes of a no-lock wake must not delay the patch window); a lock screen needs nothing written (the animations came back at screen off)
                handler.post(new Runnable() { @Override public void run() { try { AnimationSpeedup.onScreenOn(getApplicationContext(), keyguardLocked()); } catch (Throwable t) { Log.e(TAG, "animation sync failed", t); } } });
            }
            String act = i == null ? null : i.getAction();
            long t = System.currentTimeMillis();
            ensureUnlockListener();
            if (android.content.Intent.ACTION_SCREEN_OFF.equals(act) || android.content.Intent.ACTION_SCREEN_ON.equals(act) || android.content.Intent.ACTION_USER_PRESENT.equals(act))
                LockLog.add(getApplicationContext(), android.content.Intent.ACTION_SCREEN_OFF.equals(act) ? "SCREEN_OFF" : android.content.Intent.ACTION_SCREEN_ON.equals(act) ? "SCREEN_ON" : "USER_PRESENT", "locked=" + keyguardLocked() + " mode=" + Mode.get(getApplicationContext()));
            if (android.content.Intent.ACTION_SCREEN_OFF.equals(act)) {
                homeCalInterrupted("the screen went off");   // BI1
                try { if (patchHomeMode && patchView != null) { patchRestable = false; patchResting = false; patchView.setAlpha(0f); } } catch (Throwable ignored) { }   // BI1 (review 2026-11-08): the home patch is a copy of the HOME screen's pixels; with the screen dark it is hidden and stale, and the wake decides again (the lock screen's own patch, or this one rebuilt) so it can never show over the lock screen for the moments before the layer refresh
                shadeLookedDark = false;   // BY1 review LOW2: the next dark look reads the panel again
                lockShotFresh = false;     // a new lock session: photograph the lock screen again after the next wake (notifications may have changed)
                qsAppAt = ResumeTarget.refreshedAt(qsApp, fgPkg, qsAppAt, t);   // review RV2: the app still in front when the screen goes off is the one Tj had open: its 6 h clock starts now
                resumedPkg = null; unlock.onScreen(false, t); preArmedThisLock = false; preCleanedThisLock = false; lastBouncer = false; flurryChange(false, t); updateMotion();   // (BY1c F1: the panel's state is KEPT: an open quick-settings panel survives the screen-off, run #566 picture fl-shadeoff-wake; lockPrepare looks again while dark)
                try { patchShow(overlayShown); } catch (Throwable ignored) { }   // v0.7.6 (review of v0.7.5): lastBouncer is reset here, so a resting patch that was hidden for the PIN pad must come back now; the next wake sees bouncer == lastBouncer and would never re-show it
                handler.removeCallbacks(lockPrepareLater);
                handler.postDelayed(lockPrepareLater, 700);      // O3/O4: if the tablet locks at once, get everything ready while the screen is still dark
                handler.postDelayed(lockPrepareLater, 6500);     // ... and after the default 5 s "lock after" delay
            }
            if (android.content.Intent.ACTION_SCREEN_ON.equals(act)) { screenOnEpoch = t; unlock.onScreen(true, t); handler.removeCallbacks(lockPrepareLater); handleLockedWake(); if (calibrating()) { autoShotTries = 0; handler.removeCallbacks(autoShot); handler.postDelayed(autoShot, 2500); } flurryChange(true, t); updateMotion(); handler.postDelayed(layerRefresh, 400); }
            if (android.content.Intent.ACTION_USER_PRESENT.equals(act)) { lastKeyguardUpMs = android.os.SystemClock.uptimeMillis(); lastUserPresentMs = lastKeyguardUpMs; lockShadeOpen = false; lockShadeClosedMs = 0; handler.removeCallbacks(lockPrepareLater); startUnlockSpeed(); sumNotePresent(); handler.post(unlockApply); updateMotion(); handler.postDelayed(layerRefresh, 400); }
            if (android.content.Intent.ACTION_POWER_CONNECTED.equals(act) || android.content.Intent.ACTION_POWER_DISCONNECTED.equals(act)) {
                try { if (keyguardLocked()) unlock.onPower(android.content.Intent.ACTION_POWER_CONNECTED.equals(act), t); } catch (Throwable e) { Log.e(TAG, "power event failed", e); }
                BatterySaverGuard.onPowerEvent(getApplicationContext(), android.content.Intent.ACTION_POWER_CONNECTED.equals(act));   // BA2: a plug-in resets Android's own saver state: the guard never switches the saver back on from outside after one
            }
        }
    };

    private void startLockNumber() {
        if (!lockReceiverOn) {
            android.content.IntentFilter f = new android.content.IntentFilter();
            f.addAction(android.content.Intent.ACTION_SCREEN_ON);
            f.addAction(android.content.Intent.ACTION_SCREEN_OFF);
            f.addAction(android.content.Intent.ACTION_USER_PRESENT);
            f.addAction(android.content.Intent.ACTION_POWER_CONNECTED);
            f.addAction(android.content.Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(lockReceiver, f);
            lockReceiverOn = true;
        }
        updateLockNumber();
    }

    private void stopLockNumber() {
        if (lockReceiverOn) { try { unregisterReceiver(lockReceiver); } catch (Throwable ignored) { } lockReceiverOn = false; }
        removeLockNumber();
    }

    /** Called on every accessibility event: leading edge at once (the number must go the moment the PIN pad appears), plus one trailing check. */
    private void scheduleLockUpdate() {
        if (lockView == null && !lastBouncer && !keyguardLockedFast()) return;   // BY1a B4: nothing on screen, no PIN pad and not on the lock screen: zero cost, the number on or off (LockNumber.shouldShow needs the keyguard; was: two binder calls per event while unlocked with the number on)
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastLockEvalMs >= 60) { lastLockEvalMs = now; updateLockNumber(); }
        if (!lockEvalPending) { lockEvalPending = true; handler.postDelayed(lockEval, 120); }
    }

    private void updateLockNumber() {
        try {
            boolean enabled = LockNumber.enabled(this);
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            boolean on = pm != null && pm.isInteractive();
            boolean locked = km != null && km.isKeyguardLocked();
            boolean bouncer = (enabled || locked) && on && locked && bouncerShown();   // also needed by the handshakes (a fresh pad, the peek count), not just the number
            if (bouncer) lastPadSeenMs = android.os.SystemClock.uptimeMillis();   // AU2: a screenshot taken around a PIN pad is refused
            if (bouncer != lastBouncer) { lastBouncer = bouncer; ensureUnlockListener(); unlock.onPad(bouncer, System.currentTimeMillis()); LockLog.add(getApplicationContext(), "PAD", bouncer ? "shown" : "hidden"); if (bouncer) pinPadCameUp("the PIN pad scan found it"); else if (!patchHomeMode) padLeft(); patchShow(overlayShown); }   // AE3: the resting patch hide; BT1b: a pad that LEAVES keeps it away toos while the PIN pad is up and comes back after
            if (LockNumber.shouldShow(enabled, on, locked, bouncer)) showLockNumber(); else removeLockNumber();
        } catch (Throwable t) { Log.e(TAG, "lock number update failed", t); removeLockNumber(); }
    }

    private static final String[] BOUNCER_IDS = {"keyguard_pin_view", "keyguard_password_view", "keyguard_pattern_view", "pinEntry", "passwordEntry", "lockPatternView"};
    private static final String[] BOUNCER_TEXTS = {"enter your pin", "enter your password", "draw your pattern", "enter pin", "enter password"};

    /** True when the PIN / password / pattern pad of the lock screen is visible (read-only look at SystemUI's windows). */
    private boolean bouncerShown() {
        long t0 = android.os.SystemClock.uptimeMillis();
        try { return bouncerShownScan(); }
        finally { long d = android.os.SystemClock.uptimeMillis() - t0; padChecks++; padChecksMs += d; if (d > padChecksMax) padChecksMax = d; }   // BY1a B3: counted for perfLine
    }
    private long padChecks = 0, padChecksMs = 0, padChecksMax = 0;

    private boolean bouncerShownScan() {
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return false;
        for (AccessibilityWindowInfo w : windows) {
            if (!tallSystemWindow(w)) continue;   // BY1a B3: the pad lives in the keyguard's full-height window; never the navigation bar (11 searches each), an app, the keyboard or our overlays
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) continue;
            try {
                CharSequence pkg = root.getPackageName();
                if (pkg == null || !"com.android.systemui".contentEquals(pkg)) continue;
                for (String id : BOUNCER_IDS) {
                    List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/" + id);
                    if (hit != null) for (AccessibilityNodeInfo n : hit) if (n.isVisibleToUser()) return true;
                }
                for (String t : BOUNCER_TEXTS) {
                    List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByText(t);
                    if (hit != null) for (AccessibilityNodeInfo n : hit) if (n.isVisibleToUser()) return true;
                }
            } finally { root.recycle(); }
        }
        return false;
    }

    private View buildLockView(final String text, final float em, final float baselineInView) {
        return new View(this) {
            private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.SUBPIXEL_TEXT_FLAG);
            { p.setColor(Color.argb(LockNumber.alphaFor(Features.getInt(TjAccessibilityService.this, LockNumber.ALPHA_KEY, 0)), 255, 255, 255)); p.setTextSize(em);
              p.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, LockNumber.weightFor(Features.getInt(TjAccessibilityService.this, LockNumber.WEIGHT_KEY, 0)), false)); p.setTextAlign(android.graphics.Paint.Align.CENTER); }   // v0.7.6: Tj's nudges on the measured look
            @Override protected void onDraw(android.graphics.Canvas c) { c.drawText(text, getWidth() / 2f + getWidth() * LockNumber.X_NUDGE_PER_WIDTH, baselineInView, p); }
        };
    }

    private void showLockNumber() {
        if (lockView != null) return;
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        float em = LockNumber.emFor(dm.widthPixels, Features.getInt(this, LockNumber.SIZE_KEY, 0));   // v0.7.6: the measured size, times Tj's size nudge
        float baselineY = dm.heightPixels * LockNumber.BASELINE_PER_HEIGHT;
        int h = (int) Math.ceil(em * 2f);
        float baselineInView = em * 1.3f;
        lockView = buildLockView(LockNumber.text(this), em, baselineInView);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                dm.widthPixels, h, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = 0; lp.y = Math.round(baselineY - baselineInView);
        lp.setTitle("TjLockNumber");
        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(lockView, lp);
        Log.i(TAG, "LOCKNUM shown");
    }

    private void removeLockNumber() {
        if (lockView == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(lockView); } catch (Throwable ignored) { }
        lockView = null;
        Log.i(TAG, "LOCKNUM removed");
    }

    // ---------------------------------------------------------------- W5: keep the stock launcher's home screen away unless Tj did the gesture
    private ComponentName stockCache;
    private long stockCacheAt = 0;

    /** AD8: the stock launcher, looked up at most once a minute (a package-manager binder call per window change is too much for a Go tablet); a failed lookup is retried at the next call, never remembered. */
    private ComponentName stockCached() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (stockCache == null || now - stockCacheAt > 60_000) { stockCache = StockLauncher.find(this); stockCacheAt = now; }
        return stockCache;
    }
    private final long[] guardHist = new long[StrayHomeGuard.MAX];
    private int guardIdx = 0;
    private long standDownUntil = 0, lastGuardMs = 0, guardPausedUntil = 0;

    private void guardStrayHome(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pk = e.getPackageName(), cl = e.getClassName();
        if (pk == null || cl == null) return;
        if (!Mode.isCustom(this) || !Features.get(this, StrayHomeGuard.KEY, true)) return;
        long now = System.currentTimeMillis();
        if (now < guardPausedUntil || now - Mode.changedAt(this) < 2500) return;   // a deliberate mode change or the return reset is running: never fight it
        ComponentName stockHome = stockCached();
        if (stockHome == null) return;
        if (!StrayHomeGuard.isStockHome(pk.toString(), cl.toString(), stockHome.getPackageName(), stockHome.getClassName())) return;
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();
        if (!StrayHomeGuard.shouldRedirect(true, true, locked, true, now, Mode.failOpenUntil(this), standDownUntil, guardHist)) {
            if (StrayHomeGuard.countInWindow(guardHist, now) >= StrayHomeGuard.MAX) standDownUntil = now + StrayHomeGuard.COOLDOWN_MS;
            return;
        }
        if (now - lastGuardMs < 700) return;   // one stock-home appearance can raise two events
        lastGuardMs = now;
        guardHist[guardIdx++ % guardHist.length] = now;
        int n = bump(StrayHomeGuard.COUNT_KEY);
        Features.setString(this, StrayHomeGuard.LAST_KEY, new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(new java.util.Date(now)));
        Log.i(TAG, "STRAY stock home shown in custom mode -> pressing Home (#" + n + ")");
        performGlobalAction(GLOBAL_ACTION_HOME);   // the router answers with our Launcher
    }

    private int bump(String key) {
        int n = 1;
        try { n = Integer.parseInt(Features.getString(this, key, "0")) + 1; } catch (NumberFormatException ignored) { }
        Features.setString(this, key, String.valueOf(n));
        return n;
    }

    // ---------------------------------------------------------------- D3: how fast is the card? (measured, shown in Launcher control)
    // There is NO earlier signal on Android 10: the side key is consumed before the key filter (never reaches a service), and PhoneWindowManager no longer broadcasts
    // ACTION_CLOSE_SYSTEM_DIALOGS("globalactions") when it opens the power menu (checked in the android-10.0.0_r47 source; only the shutdown path still does).
    // The first thing an accessibility service can see is the menu window itself, so speed comes from the pre-built (warm) card.
    public static final String LATENCY_KEY = "power_latency";
    private long signalMs = 0, menuSeenMs = 0, cardShownMs = 0;

    /** Called when the card becomes visible: how long after the broadcast / after the real menu window appeared? (negative = the card came FIRST) */
    private void recordLatency() {
        if (cardShownMs == 0) return;
        String line = "card shown " + (signalMs > 0 ? (cardShownMs - signalMs) + " ms after the system broadcast" : "(no broadcast seen)")
                + (menuSeenMs > 0 ? ", " + (cardShownMs - menuSeenMs) + " ms after the real menu appeared (negative = card first)" : ", real menu not seen before the card");
        Features.setStringAsync(this, LATENCY_KEY, line);   // AZ1: a synchronous preferences commit here (run 245: ~75 ms between "fast open" and the first dimmed frame) delayed the patch on the very path that must be fastest
        Log.i(TAG, "POWERLAT " + line);
    }

    public static final String PWR_COUNT_KEY = "power_key_count", PWR_LAST_KEY = "power_key_last";   // kept so old settings files stay readable; the side key never reaches a service

    // ---------------------------------------------------------------- N5: which app is in front (for the hidden hotspot and the probe)
    private String fgPkg = "";

    // ---------------------------------------------------------------- U1 (Tj: "significant delay when loading either launcher from the lock screen"): measure it on the tablet itself
    public static final String UNLOCK_SPEED_KEY = "unlock_speed";
    private long unlockAtMs = 0;
    private final Runnable unlockSpeedTimeout = new Runnable() { @Override public void run() {
        if (unlockAtMs > 0) { unlockAtMs = 0; Log.i(TAG, "UNLOCKSPEED no new window within 3 s: the target was already in front behind the lock screen"); Features.setStringAsync(TjAccessibilityService.this, UNLOCK_SPEED_KEY, now() + "  already in front (nothing to load)"); }
    } };

    /** USER_PRESENT: start the stopwatch. */
    private void startUnlockSpeed() { unlockAtMs = android.os.SystemClock.uptimeMillis(); handler.removeCallbacks(unlockSpeedTimeout); handler.postDelayed(unlockSpeedTimeout, 3000); }

    /** The first real app window after the unlock: how long the launcher / app took to appear. */
    private void noteUnlockSpeed(String pk) {
        if (unlockAtMs <= 0) return;
        long d = android.os.SystemClock.uptimeMillis() - unlockAtMs;
        unlockAtMs = 0; handler.removeCallbacks(unlockSpeedTimeout);
        Log.i(TAG, "UNLOCKSPEED first window after the unlock: " + pk + " +" + d + " ms");
        Features.setStringAsync(this, UNLOCK_SPEED_KEY, now() + "  " + pk + " appeared +" + d + " ms after the unlock");   // BG1: apply(), not commit(): this runs the moment the launcher's window appears, on the thread that is about to draw it
    }

    // ---------------------------------------------------------------- BK1c: ONE figure for "PIN accepted -> the custom home is on screen"
    // Tj, 2026-11-08 18:16 UTC: "the custom launcher still loads from the lock screen on a delay, figure it out how to make it load as quickly as possible, ideally immediately". Nothing was ever measured on the tablet
    // (BG1), and the log had only pieces: 'HOME custom home in front +N ms' counts from the start of settleHome, 'unlock_speed' stops at the first non-SystemUI window. This stamps the Enter tap, the moment the keyguard went away,
    // the Launcher's resume and its first frame, and the black cover's life, and writes ONE line per unlock (lock log 'UNLOCKSUM' + the report key unlock_sum). Main thread, a few additions per event, nothing on the draw path.
    public static final String UNLOCK_SUM_KEY = "unlock_sum";
    private long sumEnterAt = 0, sumPresentAt = 0, sumResumedAt = 0, sumFirstDrawAt = 0, sumCoverUpAt = 0, sumCoverGoneAt = 0;
    private String sumMode = "";
    private boolean sumArmed = false;
    private final Runnable sumTimeout = new Runnable() { @Override public void run() { try { sumEmit(true); } catch (Throwable t) { Log.e(TAG, "unlock summary failed", t); } } };

    /** A plain Enter tap (the PIN is about to be judged). */
    private void sumNoteEnter(long tapUptimeMs) { sumEnterAt = tapUptimeMs > 0 ? tapUptimeMs : android.os.SystemClock.uptimeMillis(); }   // CA1 U1: from the tap (the event's own time, uptime base)

    /** USER_PRESENT: the keyguard is gone. */
    private void sumNotePresent() {
        long n = android.os.SystemClock.uptimeMillis();
        sumPresentAt = n; sumResumedAt = 0; sumFirstDrawAt = 0; sumCoverGoneAt = 0; sumMode = Mode.isCustom(this) ? "custom" : "quickstep"; sumArmed = true;
        if (resetCover == null) sumCoverUpAt = 0;                       // a cover from an earlier, unrelated unlock is gone; one raised at THIS Enter stays counted
        if (sumEnterAt != 0 && n - sumEnterAt > 6000) sumEnterAt = 0;   // an Enter long ago (a wrong PIN) is not this unlock's
        // review 2026-11-08: a Launcher that was already behind the keyguard (the custom-after-custom case) can resume and draw during the keyguard's exit animation, BEFORE this broadcast arrives, and a static home draws no more: look at the
        // last times the Launcher reported, so such an unlock is not mistaken for "no custom-Launcher frame"
        long ld = TjHooks.lastHomeDrawMs, lr = TjHooks.lastResumeMs;
        if (ld > 0 && n - ld <= 2000) sumFirstDrawAt = ld;
        if (lr > 0 && n - lr <= 2000) sumResumedAt = lr;
        handler.removeCallbacks(sumTimeout); handler.postDelayed(sumTimeout, 8000);
        sumEmit(false);   // everything may already be known (no cover, the Launcher already drew)
    }

    /** The custom Launcher's onResume (TjHooks). */
    void sumNoteResume() { if (sumArmed && sumResumedAt == 0 && android.os.SystemClock.uptimeMillis() >= sumPresentAt) sumResumedAt = android.os.SystemClock.uptimeMillis(); }

    private void sumNoteDraw() {
        long n = android.os.SystemClock.uptimeMillis();
        if (n < sumPresentAt) return;
        sumFirstDrawAt = n;
        sumEmit(false);
    }

    private void sumEmit(boolean timeout) {
        if (!sumArmed) return;
        boolean coverWasUp = sumCoverUpAt > 0;
        if (!timeout && (sumFirstDrawAt == 0 || (coverWasUp && sumCoverGoneAt == 0))) return;   // wait for the first frame AND for the cover to go
        sumArmed = false; handler.removeCallbacks(sumTimeout);
        String text;
        if (sumFirstDrawAt == 0) text = "no custom-Launcher frame within 8 s after the keyguard went away (the unlock went to Quickstep or to an app); stored mode then: " + sumMode;
        else {
            long visibleAt = Math.max(sumFirstDrawAt, sumCoverGoneAt);
            boolean fromEnter = sumEnterAt > 0;
            text = "custom home on screen " + (visibleAt - (fromEnter ? sumEnterAt : sumPresentAt)) + " ms after " + (fromEnter ? "the Enter tap" : "the keyguard went away")
                    + " [" + (fromEnter ? "keyguard " + (sumPresentAt - sumEnterAt) + " ms; " : "") + "relative to the keyguard going away: Launcher resumed " + (sumResumedAt > 0 ? (sumResumedAt - sumPresentAt > 0 ? "+" : "") + (sumResumedAt - sumPresentAt) : "?") + ", first frame " + (sumFirstDrawAt - sumPresentAt > 0 ? "+" : "") + (sumFirstDrawAt - sumPresentAt)
                    + (coverWasUp ? ", black cover gone +" + (sumCoverGoneAt - sumPresentAt) + " (up " + (sumCoverGoneAt - sumCoverUpAt) + ")" : ", no cover") + " ms]; stored mode when the keyguard went away (before the unlock choice is applied): " + sumMode;
        }
        sumCoverUpAt = 0; sumCoverGoneAt = 0; sumEnterAt = 0;
        Log.i(TAG, "UNLOCKSUM " + text);
        LockLog.addAsync(getApplicationContext(), "UNLOCKSUM", text);
        Features.setStringAsync(this, UNLOCK_SUM_KEY, now() + "  " + text);
    }

    private void trackForeground(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null || e.getClassName() == null) return;
        String pk = e.getPackageName().toString();
        if ("com.android.systemui".equals(pk) || pk.startsWith("com.google.android.inputmethod") || pk.contains("inputmethod") || pk.contains("keyboard") || pk.contains("swiftkey")) return;
        noteUnlockSpeed(pk);
        if (pk.equals(fgPkg)) return;
        fgPkg = pk;
        noteHiddenForSweep(pk);   // BE2
        try { rememberQuickstepApp(pk); } catch (Throwable t) { Log.e(TAG, "remember app failed", t); }
        updateHotspot();
    }

    // ---------------------------------------------------------------- AD5: a gesture unlock comes back to the app that was open (see ResumeTarget)
    private String qsApp = null;          // the last app window of the Quickstep session while unlocked ("" / null = none: the stock home was the last thing, or no session)
    private long qsAppAt = 0;
    private String resumedPkg = null;     // the app started for the current gesture (so "is Quickstep in front?" accepts it too)
    private long resumedAt = 0;           // ... and only for a few seconds: a stale value must never decide a LATER unlock

    private String stockPkg() { ComponentName s = stockCached(); return s == null ? null : s.getPackageName(); }

    private void rememberQuickstepApp(String pk) {
        android.content.Context app = getApplicationContext();
        if (Mode.isCustom(app)) { qsApp = null; return; }   // review RV1: a custom session means there is no Quickstep session to come back to; a stale app must never be started by a later gesture
        // what appears under the lock screen (or right after the unlock) is the pre-arm / the custom account's last app, not Tj's app (review RV3: the same unit-tested rule trackDirty uses)
        if (ProfileSession.windowIsLockLeftover(keyguardLockedFast(), unlock.armedMethod() != null, System.currentTimeMillis(), lastUnlockAt)) return;
        String stock = stockPkg();
        if (ResumeTarget.isStockHome(pk, stock)) { qsApp = null; return; }    // he went home (or to Recents): there is no app to come back to
        if (!ResumeTarget.remember(pk, getPackageName(), stock, false, false)) return;
        if (!isLaunchable(pk)) return;        // a dialog or system window without an icon (permission controller, installer) is not "the app Tj had open": it neither replaces nor clears it
        qsApp = pk; qsAppAt = System.currentTimeMillis();
    }

    private final java.util.HashMap<String, Boolean> launchableCache = new java.util.HashMap<>();
    private boolean isLaunchable(String pk) {
        Boolean b = launchableCache.get(pk);
        if (b == null) {
            boolean v = false;
            try { v = getPackageManager().getLaunchIntentForPackage(pk) != null; } catch (Throwable ignored) { }
            if (launchableCache.size() > 64) launchableCache.clear();
            launchableCache.put(pk, v); b = v;
        }
        return b;
    }

    /**
     * AD5: Quickstep in front. The Quickstep app that was open when the tablet locked is started like a tap on its icon (its task comes forward as it was: no RESET_TASK_IF_NEEDED); nothing to come back to, the
     * switch off, or any failure = the stock home, as before.
     */
    private void startQuickstepFront(String why) {
        android.content.Context app = getApplicationContext();
        String pkg = ResumeTarget.pick(Features.get(app, ResumeTarget.KEY, true), qsApp, qsAppAt, System.currentTimeMillis());
        if (pkg != null) {
            try {
                android.content.Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
                if (li != null) {
                    li.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    resumedPkg = pkg; resumedAt = System.currentTimeMillis();
                    startActivity(li);
                    Log.i(TAG, "ROUTER resume " + pkg + " (" + why + ")");
                    LockLog.add(app, "START", "resume " + pkg + " (" + why + ")");
                    return;
                }
            } catch (Throwable t) { Log.e(TAG, "resume start failed, stock home instead", t); }
        }
        if (pkg == null) {   // say WHY the stock home came up (the report and the emulator gate show it): switch off / nothing remembered / too old
            String reason = ResumeTarget.whyNot(Features.get(app, ResumeTarget.KEY, true), qsApp, qsAppAt, System.currentTimeMillis()) + " [qsApp=" + qsApp + " fg=" + fgPkg + " mode=" + Mode.get(app) + "]";
            Log.i(TAG, "ROUTER stock home, no resume (" + why + "): " + reason);
            LockLog.add(app, "START", "stock home, no resume: " + reason);
        }
        resumedPkg = null;
        startRouter("stock");
    }

    // ---------------------------------------------------------------- N5: hidden hotspot in Solitaire: 5 quick taps then a hold = master failsafe into Quickstep
    private View hotView;
    private final TapCode tapCode = new TapCode();
    private final Runnable hotHold = new Runnable() { @Override public void run() {
        try { tapCode.reset(); buzz(); removeHotspot(); ProfileSession.goToQuickstep(getApplicationContext(), "Solitaire hotspot"); } catch (Throwable t) { Log.e(TAG, "hotspot failsafe failed", t); }
    } };

    private void updateHotspot() {
        try {
            android.content.Context app = getApplicationContext();
            String target = ReturnReset.targetPackage(app);
            boolean want = FailsafeKeys.hotspot(app) && Mode.isCustom(app) && target != null && target.equals(fgPkg) && !keyguardLocked();
            if (want && hotView == null) showHotspot(); else if (!want && hotView != null) removeHotspot();
        } catch (Throwable t) { Log.e(TAG, "hotspot update failed", t); removeHotspot(); }
    }

    private void showHotspot() {
        float dp = getResources().getDisplayMetrics().density;
        int size = Math.round(56 * dp);
        View v = new View(this) {
            @Override public boolean onTouchEvent(android.view.MotionEvent m) {
                long t = android.os.SystemClock.uptimeMillis();
                switch (m.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        if (tapCode.onDown(t)) handler.postDelayed(hotHold, TapCode.HOLD_MS);
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(hotHold);
                        tapCode.onUp(t);
                        return true;
                    default: return true;
                }
            }
        };
        int corner = FailsafeKeys.corner(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        lp.gravity = (corner < 2 ? Gravity.TOP : Gravity.BOTTOM) | (corner % 2 == 0 ? Gravity.START : Gravity.END);
        lp.setTitle("TjHotspot");
        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(v, lp);
        hotView = v;
        tapCode.reset();
        Log.i(TAG, "HOTSPOT shown corner=" + FailsafeKeys.CORNERS[corner]);
    }

    private void removeHotspot() {
        handler.removeCallbacks(hotHold);
        if (hotView == null) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(hotView); } catch (Throwable ignored) { }
        hotView = null;
        Log.i(TAG, "HOTSPOT removed");
    }

    // ---------------------------------------------------------------- N5 research: what can the helper READ inside Solitaire? (decides whether a card-move code is possible)
    private final java.util.LinkedHashSet<String> probeSamples = new java.util.LinkedHashSet<>();
    private int probeEvents = 0, probeClicks = 0, probeNodes = 0, probeReadable = 0;
    private long probeLastWalk = 0, probeLastWrite = 0;

    private void probeEvent(AccessibilityEvent e) {
        if (e == null) return;
        long wall = System.currentTimeMillis();
        long until = Long.parseLong(Features.getString(this, FailsafeKeys.PROBE_UNTIL_KEY, "0"));
        if (wall > until) { if (until != 0) { Features.setString(this, FailsafeKeys.PROBE_UNTIL_KEY, "0"); probeFlush(true); } return; }
        CharSequence pk = e.getPackageName();
        String target = ReturnReset.targetPackage(this);
        if (pk == null || target == null || !target.contentEquals(pk)) return;
        probeEvents++;
        if (e.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) probeClicks++;
        AccessibilityNodeInfo src = e.getSource();
        if (src != null) probeSample(src);
        long now = android.os.SystemClock.uptimeMillis();
        if (now - probeLastWalk > 1500) {
            probeLastWalk = now;
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) { try { probeNodes = 0; probeReadable = 0; probeWalk(root, 0); } finally { root.recycle(); } }
        }
        probeFlush(false);
    }

    private void probeSample(AccessibilityNodeInfo n) {
        CharSequence d = n.getContentDescription(), t = n.getText();
        String id = n.getViewIdResourceName();
        if ((d == null || d.length() == 0) && (t == null || t.length() == 0) && id == null) return;
        if (probeSamples.size() >= 25) return;
        String x = String.valueOf(n.getClassName()).replace("android.widget.", "").replace("android.view.", "") + "|" + (id == null ? "-" : id.substring(id.indexOf('/') + 1)) + "|"
                + (d == null ? "" : cut(d)) + "|" + (t == null ? "" : cut(t));
        probeSamples.add(x);
    }

    private static String cut(CharSequence c) { String s = c.toString(); return s.length() > 24 ? s.substring(0, 24) : s; }

    private void probeWalk(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 14 || probeNodes > 400) return;
        probeNodes++;
        CharSequence d = n.getContentDescription(), t = n.getText();
        if ((d != null && d.length() > 0) || (t != null && t.length() > 0)) { probeReadable++; probeSample(n); }
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) { try { probeWalk(c, depth + 1); } finally { c.recycle(); } }
        }
    }

    private void probeFlush(boolean force) {
        long now = android.os.SystemClock.uptimeMillis();
        if (!force && now - probeLastWrite < 2000) return;
        probeLastWrite = now;
        Features.setString(this, FailsafeKeys.PROBE_REPORT_KEY, "events=" + probeEvents + ", clicks seen=" + probeClicks + ", nodes in window=" + probeNodes + ", nodes with text/description=" + probeReadable
                + (probeSamples.isEmpty() ? "\nNOTHING readable: the game draws its cards itself, so a card-move code cannot be read (use the hotspot or the key code)." : "\nreadable samples (class|id|description|text):\n" + android.text.TextUtils.join("\n", probeSamples)));
    }

    // ---------------------------------------------------------------- O1: rotation twirl + shake / knock (accelerometer, only while the lock screen is up and one of them is selected)
    private int lastOrientation = -1;
    private int lastNightSeen = -1;   // BB2: -1 = not seen yet, 0 light, 1 dark

    @Override public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        try {   // BB2: battery saver / the dark theme flipped: use (or look for) the measurement of the NEW theme at once, never measure again just because the other theme was measured
            int nightNow = (c != null ? (c.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES : isNightNow()) ? 1 : 0;
            if (lastNightSeen != nightNow) {   // also the first callback: the measurement loaded at start may belong to the other theme
                lastNightSeen = nightNow;
                reloadScrim(); if (patchView != null) patchView.invalidate(); handler.removeCallbacks(scrimTrigger); handler.postDelayed(scrimTrigger, 6000);
            }
        } catch (Throwable t) { Log.e(TAG, "theme change failed", t); }
        try {
            int o = c == null ? -1 : c.orientation;
            boolean first = lastOrientation == -1;
            if (o != lastOrientation) {
                lastOrientation = o;
                if (!first && keyguardLocked()) { ensureUnlockListener(); unlock.onRotation(System.currentTimeMillis()); }
            }
        } catch (Throwable t) { Log.e(TAG, "rotation event failed", t); }
    }

    private final MotionDetector motion = new MotionDetector();
    private boolean motionOn = false;
    private final android.hardware.SensorEventListener sensorListener = new android.hardware.SensorEventListener() {
        @Override public void onSensorChanged(android.hardware.SensorEvent e) {
            try {
                MotionDetector.Kind k = motion.onSample(e.values[0], e.values[1], e.values[2], e.timestamp / 1_000_000L);
                if (k == MotionDetector.Kind.NONE) return;
                android.content.Context app = getApplicationContext();
                if (keyguardLocked()) {
                    ensureUnlockListener();
                    unlock.onMotion(k == MotionDetector.Kind.SHAKE ? UnlockMethods.Method.SHAKE : UnlockMethods.Method.KNOCK, System.currentTimeMillis());
                } else if (!Mode.isCustom(app) && ReturnMethods.on(app, k == MotionDetector.Kind.SHAKE ? ReturnMethods.Method.SHAKE : ReturnMethods.Method.KNOCK)) {
                    buzz();
                    ProfileSession.returnToCustom(app, k == MotionDetector.Kind.SHAKE ? "shake" : "double knock");   // R1: the accelerometer way back to the custom launcher
                }
            } catch (Throwable t) { Log.e(TAG, "motion sample failed", t); }
        }
        @Override public void onAccuracyChanged(android.hardware.Sensor s, int a) { }
    };

    /** The accelerometer is read ONLY while the lock screen is showing with the screen on and Triple shake / Double knock is selected; unregistered otherwise (no battery cost). */
    private void updateMotion() {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            android.content.Context app = getApplicationContext();
            boolean interactive = pm != null && pm.isInteractive();
            boolean lockWant = UnlockMethods.needsMotion(storedEnabled) && keyguardLocked();
            boolean returnWant = !Mode.isCustom(app) && ReturnMethods.needsMotion(app) && !keyguardLocked();     // R1: only while Quickstep is active
            boolean want = interactive && (lockWant || returnWant);
            android.hardware.SensorManager sm = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
            if (sm == null) return;
            if (want && !motionOn) {
                android.hardware.Sensor a = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);
                if (a != null) { motion.reset(); sm.registerListener(sensorListener, a, android.hardware.SensorManager.SENSOR_DELAY_GAME); motionOn = true; Log.i(TAG, "MOTION sensor on"); }
            } else if (!want && motionOn) {
                sm.unregisterListener(sensorListener); motionOn = false; Log.i(TAG, "MOTION sensor off");
            }
        } catch (Throwable t) { Log.e(TAG, "motion sensor update failed", t); }
    }

    // ---------------------------------------------------------------- O5: the custom account keeps its ONE tile (SystemUI's auto-added tiles - hotspot, saver, night light ... - are taken out again)
    private final long[] tileHist = new long[3];
    private int tileIdx = 0;
    private boolean tileGuardOn = false;
    private final android.database.ContentObserver tileObserver = new android.database.ContentObserver(handler) {
        @Override public void onChange(boolean selfChange) {
            try {
                android.content.Context app = getApplicationContext();
                if (!(Mode.isCustom(app) || keyguardLockedFast()) || !TileProfile.enabled(app) || !TileProfile.permitted(app) || CrashGuard.automationOff(app)) return;   // AW2: the custom list is also the lock screen's, whatever the mode says; review of v0.8.1: not while automation is paused ("Restore everything"): applyLockTiles / restoreUnlockTiles stand down too
                String cur = android.provider.Settings.Secure.getString(getContentResolver(), TileProfile.SETTING);
                if (TileProfile.tilesFor(app, Mode.CUSTOM).equals(cur)) return;
                long now = System.currentTimeMillis();
                if (!ProfileSession.enforceRadios(true, true, now, tileHist)) return;     // same "3 tries a minute, never a loop" rule
                tileHist[tileIdx++ % tileHist.length] = now;
                Log.i(TAG, "TILES changed while the custom account / the lock screen is active (" + cur + ") -> putting its list back");
                TileProfile.apply(app, Mode.CUSTOM);
            } catch (Throwable t) { Log.e(TAG, "tile guard failed", t); }
        }
    };

    private void startTileGuard() {
        if (tileGuardOn) return;
        getContentResolver().registerContentObserver(android.provider.Settings.Secure.getUriFor(TileProfile.SETTING), false, tileObserver);
        tileGuardOn = true;
    }

    private void stopTileGuard() {
        if (!tileGuardOn) return;
        try { getContentResolver().unregisterContentObserver(tileObserver); } catch (Throwable ignored) { }
        tileGuardOn = false;
    }

    // ---------------------------------------------------------------- N3: the custom account has no Wi-Fi / Bluetooth (Tj: "no wifi or bluetooth enabled")
    private long lqLastWrite = 0; private int lqWrites = 0;   // AW3: the lock-screen rule: when it last switched a radio off, and how many switch-offs this lock has spent (restarted at every lock and unlock)
    private final long[] radioHist = new long[3];
    private int radioIdx = 0;
    private boolean radioGuardOn = false;
    private final Runnable radioRecheck = new Runnable() { @Override public void run() { lockRadiosCheck("recheck"); } };
    private final android.content.BroadcastReceiver radioReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context c, android.content.Intent i) {
            try {
                String act = i == null ? null : i.getAction();
                boolean turnedOn, fullyOn;   // v0.7.6 (review of AK1): fullyOn = the settled state: the lock-screen rule must not spend a slot on ENABLING and another on ENABLED
                if (android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION.equals(act)) {
                    int st = i.getIntExtra(android.net.wifi.WifiManager.EXTRA_WIFI_STATE, -1);
                    turnedOn = st == android.net.wifi.WifiManager.WIFI_STATE_ENABLED || st == android.net.wifi.WifiManager.WIFI_STATE_ENABLING; fullyOn = st == android.net.wifi.WifiManager.WIFI_STATE_ENABLED;
                } else if (android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED.equals(act)) {
                    int st = i.getIntExtra(android.bluetooth.BluetoothAdapter.EXTRA_STATE, -1);
                    turnedOn = st == android.bluetooth.BluetoothAdapter.STATE_ON || st == android.bluetooth.BluetoothAdapter.STATE_TURNING_ON; fullyOn = st == android.bluetooth.BluetoothAdapter.STATE_ON;
                } else return;
                if (!turnedOn) return;
                if (RadioIconHide.keepsRadiosOn(getApplicationContext())) return;   // BS1 + BS2 (Tj): Wi-Fi and Bluetooth may be on in the custom launcher and on the lock screen while their symbols are hidden (the switch is read first: no binder call when the feature is off)
                long now = System.currentTimeMillis();
                if (!ProfileSession.enforceRadios(ProfileSession.radiosOff(TjAccessibilityService.this), Mode.isCustom(TjAccessibilityService.this), now, radioHist)) {
                    // AK1: not the custom account's rule: is the tablet on the LOCK SCREEN? Then a radio that comes on is switched off again, whatever launcher is active (AW3: and kept off, see lockRadiosCheck)
                    if (fullyOn) lockRadiosCheck("a radio came on");
                    return;
                } else radioHist[radioIdx++ % radioHist.length] = now;
                final android.content.Context app = getApplicationContext();
                new Thread(new Runnable() { @Override public void run() { try { ReturnReset.radiosOff(app); } catch (Throwable t) { Log.e(TAG, "radio guard failed", t); } } }, "tj-radio-guard").start();
                Log.i(TAG, "RADIOS switched on in the custom account -> switching off");
            } catch (Throwable t) { Log.e(TAG, "radio receiver failed", t); }
        }
    };

    /**
     * AW3 (Tj, 2026-11-06 evening): on the lock screen Wi-Fi and Bluetooth stay off for the WHOLE time the keyguard is up, from any launcher. A radio that is on is switched off at once, or as soon as
     * {@link LockQuiet#MIN_GAP_MS} have passed since the last switch-off, and looked at again after that until it stays off (v0.8.0 gave up after 3 switch-offs a minute). The chain only runs while a radio is found on.
     */
    private void lockRadiosCheck(String why) {
        try {
            final android.content.Context app = getApplicationContext();
            if (!keyguardLockedFast() || !LockQuiet.enabled(app) || CrashGuard.automationOff(app)) return;
            new Thread(new Runnable() { @Override public void run() {   // the radio state reads are binder calls
                try {
                    if (!LockQuiet.anyRadioOn(app)) return;
                    final long now = System.currentTimeMillis();
                    final long delay = LockQuiet.retryDelayMs(true, true, false, now, lqLastWrite, lqWrites);
                    handler.post(new Runnable() { @Override public void run() {
                        try {
                            if (delay < 0) { Log.i(TAG, "LOCKRADIOS gave up for this lock: " + lqWrites + " switch-offs spent"); return; }
                            if (delay > 0) { handler.removeCallbacks(radioRecheck); handler.postDelayed(radioRecheck, delay + 100); return; }
                            lqLastWrite = now; lqWrites++;
                            Log.i(TAG, "LOCKRADIOS a radio is on while locked -> switching off (" + why + ", #" + lqWrites + ")");
                            new Thread(new Runnable() { @Override public void run() { try { ReturnReset.radiosOff(app); } catch (Throwable t) { Log.e(TAG, "lock radios failed", t); } } }, "tj-lock-radios").start();
                            handler.removeCallbacks(radioRecheck); handler.postDelayed(radioRecheck, LockQuiet.MIN_GAP_MS + 300);   // did it stay off?
                        } catch (Throwable t) { Log.e(TAG, "lock radios step failed", t); }
                    } });
                } catch (Throwable t) { Log.e(TAG, "lock radios check failed", t); }
            } }, "tj-lock-radios-check").start();
        } catch (Throwable t) { Log.e(TAG, "lock radios check failed", t); }
    }

    private void startRadioGuard() {
        if (radioGuardOn) return;
        android.content.IntentFilter f = new android.content.IntentFilter();
        f.addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(radioReceiver, f);
        radioGuardOn = true;
    }

    private void stopRadioGuard() {
        if (!radioGuardOn) return;
        try { unregisterReceiver(radioReceiver); } catch (Throwable ignored) { }
        radioGuardOn = false;
    }

    /** AU6 (review of v0.8.0): the calibration watcher and the delayed patch jobs must not outlive this service instance (a rebind inside the calibration window leaked the old one through its ContentObserver). */
    private void cancelPatchWork() {
        try {
            stopShotWatch();
            handler.removeCallbacks(restRecheck); handler.removeCallbacks(autoShot); handler.removeCallbacks(shotEnd); handler.removeCallbacks(shotIngest); handler.removeCallbacks(autoCalTrigger);
            handler.removeCallbacks(radioRecheck); handler.removeCallbacks(restoreTilesLater); handler.removeCallbacks(recheck); handler.removeCallbacks(debounced); handler.removeCallbacks(quietWork); handler.removeCallbacks(touchableOn); handler.removeCallbacks(touchableOff); touchableOnPending = false;
            quietUntilMs = 0;
            try { homeCalInterrupted("the helper was stopped"); } catch (Throwable ignored) { }   // BI1 (code review 2026-11-08): a calibration under way ends with the Launcher SHOWN again: the Launcher must never stay hidden because the object that hid it is gone ...
            handler.removeCallbacks(homeBuild); handler.removeCallbacks(homeShowCheck); handler.removeCallbacks(homeCalTrigger); handler.removeCallbacks(homeCalShoot); handler.removeCallbacks(homeCalAbort); handler.removeCallbacks(homeOffsetSettled);   // ... and after it (a failed calibration re-posts its retry timer) the home timers go
            scrimTeardown();   // BA1
        } catch (Throwable ignored) { }
    }

    @Override public void onInterrupt() { scrimTeardown(); removeHotspot(); removeOverlay(); removePatch(); removePencil(); hideResetCover(); curtainStop(); removeLockNumber(); picker().remove("interrupted"); try { stopShotWatch(); handler.removeCallbacks(shotIngest); handler.removeCallbacks(autoShot); handler.removeCallbacks(shotEnd); Features.setString(this, PatchShot.CAL_UNTIL_KEY, "0"); } catch (Throwable ignored) { } }

    @Override public boolean onUnbind(android.content.Intent i) { instance = null; LockLog.add(this, "SERVICE", "unbound"); try { AnimationSpeedup.syncNow(this, "helper unbound"); } catch (Throwable t) { Log.e(TAG, "animation sync failed", t); }   // BF1: the helper is what hears the screen go off: without it the animations are real again
        cancelPatchWork(); removeOverlay(); removePatch(); removePencil(); hideResetCover(); curtainStop(); picker().remove("unbound"); stopPatchWatch(); stopLockNumber(); stopRadioGuard(); stopTileGuard(); removeHotspot(); try { ((android.hardware.SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(sensorListener); motionOn = false; } catch (Throwable ignored) { } return super.onUnbind(i); }

    /**
     * AD8 (Go discipline, Tj: "optimized for my tablet": 2 GB RAM, memory class 128 MB): when Android reports memory pressure, drop the cached wallpaper / lock-screen bitmaps that nothing is drawing right now
     * (a few MB each at 600 x 973); they are rebuilt on demand. Never touches them while the patch window exists (its view draws from them).
     * HONEST SCOPE (review RV5, 2026-11-06): this only drops the references THIS service holds. The home wallpaper bitmap usually aliases WallpaperManager's own cache and the lock bitmaps exist only while the patch window
     * does, so on most devices little memory is actually freed. It is cheap insurance, NOT a measured saving: the tablet report's memory lines (AD8) decide whether anything bigger is worth doing.
     */
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        try {
            // real memory pressure only: RUNNING_LOW (10), RUNNING_CRITICAL (15) and the background levels (40+); NOT UI_HIDDEN (20), which fires whenever the launcher's UI goes away
            boolean pressure = level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND;
            if (!pressure) return;
            handler.post(new Runnable() { @Override public void run() {
                launchableCache.clear();
                HomeCurtain.dropCache();   // BL1: the in-memory curtain (about 2.4 MB) goes under real pressure; it is read back from its file the next time it is needed (a cover that shows it keeps its own reference)
                if (patchView == null) { dropWallpaper(); lockBmp = null; lockShot = null; lockShotFresh = false; Log.i(TAG, "TRIM released the cached wallpaper bitmaps"); }
            } });
        } catch (Throwable ignored) { }
    }

    @Override public void onDestroy() { instance = null; try { AnimationSpeedup.syncNow(this, "helper destroyed"); } catch (Throwable t) { Log.e(TAG, "animation sync failed", t); } cancelPatchWork(); removeOverlay(); removePatch(); removePencil(); hideResetCover(); curtainStop(); picker().remove("destroyed"); stopPatchWatch(); stopLockNumber(); stopRadioGuard(); stopTileGuard(); removeHotspot(); super.onDestroy(); }

    // ---------------------------------------------------------------- AD8: what the helper costs on THIS tablet (plain counters on the main thread; read by the report)
    private long connectedAtMs = 0, evSeen = 0, evNoise = 0, scanCount = 0, scanTotalMs = 0, scanMaxMs = 0;
    /** AN1: shade events that started NO scan; the pencil: full tree searches (count / total ms / slowest) and one-node refreshes; when a power menu was last seen up (uptime ms). */
    private long evSkipped = 0, penFull = 0, penFullMs = 0, penFullMax = 0, penFast = 0, menuUpAtMs = -100_000, lastPenFullMs = -100_000;

    /** One line for the tablet report: how long the helper has been connected, how many events it got, how many were dropped at the door, and how long a power-menu scan takes here. A torn read is only a slightly stale number. */
    String perfLine() {
        long up = connectedAtMs == 0 ? 0 : (android.os.SystemClock.elapsedRealtime() - connectedAtMs) / 60_000;
        long n = scanCount;
        String pen = "; pencil cover: " + penFull + " tree searches" + (penFull == 0 ? "" : " (avg " + (penFullMs / penFull) + " ms, max " + penFullMax + " ms)") + ", " + penFast + " one-node refreshes"   // AN1
                + "; PIN-pad checks: " + padChecks + (padChecks == 0 ? "" : " (avg " + (padChecksMs / padChecks) + " ms, max " + padChecksMax + " ms)");   // BY1a B3
        String fade = "; patch fade (AZ1): " + (patchFadeOn() ? "on" : "off") + ", the last menu opening was noticed " + (fadeLagLast < 0 ? "(no opening yet)" : fadeLagLast + " ms after the stock fade began (" + (fadeLagLast >= PatchFade.OPEN_MS ? "no fade needed" : "the patch followed it") + ")") + "; last fade seen from the patch (BJ1): " + (fadeSummary.isEmpty() ? "(none yet)" : fadeSummary) + "; FIRST fade since this process started (BK1a): " + (fadeFirstSummary.isEmpty() ? "(none yet)" : fadeFirstSummary);
        return "connected " + up + " min, " + evSeen + " events (" + evNoise + " dropped as noise, " + evSkipped + " shade redraws without a scan), " + n + " power-menu scans" + (n == 0 ? "" : ", avg " + (scanTotalMs / n) + " ms, max " + scanMaxMs + " ms") + pen + fade;
    }

    private void evaluate() {
        long t0 = android.os.SystemClock.uptimeMillis();
        try { evaluateScan(); }
        finally {
            long d = android.os.SystemClock.uptimeMillis() - t0; scanCount++; scanTotalMs += d; if (d > scanMaxMs) scanMaxMs = d;
            if (d >= 15 && debuggable()) Log.i(TAG, "PATCH scan took " + d + " ms t=" + (t0 + d));   // BJ1 diagnostic (debug builds only, EVERY caller: the immediate scan, the deferred one, the recheck): how long the main thread was blocked, which delays the dialog's own event and the patch's redraw (review 2026-11-08: the deferred scan must be timed too)
        }
    }

    private void evaluateScan() {
        String mode = Features.powerMode(this);
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();
        boolean custom = PowerMenuRule.coverWorld(Mode.isCustom(this), locked);   // AD7a: "custom" here = the cover world (custom launcher or ANY lock screen)
        Rect shot = new Rect();
        boolean menuOpen = false, found = false, dialogUpNow = false;
        if (custom && !Features.POWER_OFF.equals(mode) && !(locked && Features.POWER_BLOCK.equals(mode))) {   // Y1: the cover also works on the lock screen; BLOCK still stays off it
            List<AccessibilityWindowInfo> windows = getWindows();
            // BN2 (Tj, 2026-11-09 03:02 UTC, tablet report): 143 ms after the menu was closed (Home key) a scan still found the stock dialog's window, with its entries, and drew the TALL card over it for 250 ms. A close we
            // were TOLD about (the system's close-dialogs broadcast) marks the next DYING_DIALOG_MS: a dialog listed in that time is not believed to be a new opening (a new one announces itself: fastMenuOpen clears the mark).
            final boolean listed = dialogWindowPresent(windows);
            final boolean dying = listed && !overlayShown && PowerMenuRule.dyingDialog(android.os.SystemClock.uptimeMillis(), menuClosedAtMs);
            if (dying) Log.i(TAG, "PATCH scan ignored: the stock dialog is still fading out (closed " + (android.os.SystemClock.uptimeMillis() - menuClosedAtMs) + " ms ago)");
            final boolean dialogUp = listed && !dying;
            dialogUpNow = dialogUp;
            if (menuSeenMs == 0 && dialogUp) menuSeenMs = android.os.SystemClock.uptimeMillis();
            if (dialogUp) menuUpAtMs = android.os.SystemClock.uptimeMillis();   // AN1: a menu is up: its content changes are followed
            if (dialogUp && overlayShown && fading && android.os.SystemClock.uptimeMillis() < scanNotBeforeMs) {   // AZ1: the stock menu is still fading in and the patch follows it: no node scan now (it would block the redraws); a closed menu (dialogUp false) is handled at once
                handler.removeCallbacks(recheck);
                handler.postDelayed(recheck, Math.max(20, scanNotBeforeMs - android.os.SystemClock.uptimeMillis() + 20));
                return;
            }
            if (!overlayShown && Features.isCover(mode)) {
                Rect cached = cachedCard();
                if (cached != null && dialogUp) {
                    showOverlay(cached);   // instant: no node scan yet
                    if (fading) { deferScan(android.os.SystemClock.uptimeMillis()); return; }   // AZ1: the fade is running: the scan (and everything after it) waits for its end
                }
            }
            if (windows != null && !dying) {
                for (AccessibilityWindowInfo w : windows) {
                    CharSequence wt = w.getTitle();
                    if (!EventNoise.mayHoldPowerMenu(dialogUp, wt != null && wt.toString().contains("ActionsDialog"), w.getType())) continue;   // AB2 / BY1a B1: never fetch + search the foreground app's own tree, the keyboard or our overlays
                    AccessibilityNodeInfo root = w.getRoot();
                    if (root == null) continue;
                    if (getPackageName().contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) { root.recycle(); continue; }
                    try {
                        List<AccessibilityNodeInfo> off = root.findAccessibilityNodeInfosByText("Power off");
                        List<AccessibilityNodeInfo> sc = root.findAccessibilityNodeInfosByText("Screenshot");
                        if (off != null && !off.isEmpty() && sc != null && !sc.isEmpty()) {
                            menuOpen = true; menuUpAtMs = android.os.SystemClock.uptimeMillis();
                            // the card = the smallest ancestor of "Power off" that also contains the Screenshot entry
                            AccessibilityNodeInfo offItem = clickableAncestor(off.get(0));
                            Rect scr = new Rect(), c = new Rect(), ob = new Rect();
                            offItem.getBoundsInScreen(ob); lastOffTop = ob.top; lastItemH = ob.height();   // V1: the stock item size, so our menu is "stock minus Screenshot"
                            clickableAncestor(sc.get(0)).getBoundsInScreen(scr);
                            // AC6: the entry PITCH (not the height of one clickable node: the entries have margins), so our two-entry card is "the stock panel minus its last entry"
                            lastPitch = -1; lastCut = scr.top > ob.top ? scr.top : -1;
                            List<AccessibilityNodeInfo> rst = root.findAccessibilityNodeInfosByText("Restart");
                            if (rst != null && !rst.isEmpty()) { Rect rb = new Rect(); clickableAncestor(rst.get(0)).getBoundsInScreen(rb); if (rb.top > ob.top) lastPitch = rb.top - ob.top; }
                            if (rst != null) for (AccessibilityNodeInfo rn : rst) { try { rn.recycle(); } catch (Throwable ignored) { } }
                            if (lastPitch <= 0 && scr.top > ob.top) lastPitch = (scr.top - ob.top) / 2;
                            AccessibilityNodeInfo t = offItem;
                            for (int i = 0; i < 6 && t != null; i++) {
                                t.getBoundsInScreen(c);
                                if (c.contains(scr.centerX(), scr.centerY()) && c.height() > scr.height()) { shot.set(c); break; }
                                t = t.getParent();
                            }
                            found = !shot.isEmpty();
                            if (found) dumpMenu(root, locked, shot);   // AC6: what the stock menu really looks like on THIS tablet, for the report (cable-free)
                            break;
                        }
                    } finally { root.recycle(); }
                }
            }
        }
        int action = PowerMenuRule.decide(mode, custom, locked, menuOpen, found);
        if ((overlayShown || menuOpen) && (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) Log.i(TAG, "PATCH scan dialogUp=" + dialogUpNow + " menuOpen=" + menuOpen + " found=" + found + " overlayShown=" + overlayShown + " fading=" + fading + " action=" + action);   // debug builds only (section pt prints it in the timeline)
        if (menuOpen || action != PowerMenuRule.NOTHING) Log.i(TAG, "menuOpen=" + menuOpen + " found=" + found + " bounds=" + shot + " action=" + action);
        if (action == PowerMenuRule.COVER_SCREENSHOT) {
            holdMisses = 0;
            Features.setString(this, cardKey(), shot.left + "," + shot.top + "," + shot.right + "," + shot.bottom);   // AC6: stored BEFORE the card is built, so the very first open already uses the measured panel and pitch
            Features.setString(this, itemKey(), lastPitch > 0 || lastCut > 0 ? lastPitch + "/" + lastCut : "");   // a failed measurement CLEARS the stored value: a stale one from another layout must never be reused
            showOverlay(shot);
            handler.removeCallbacks(recheck);
            if ((Features.POWER_WALLPAPER.equals(mode) || locked || homeSwitchOn()) && patchView == null) refreshLayers();   // BI1: the first menu on the home screen learns where it sits: the home patch (or its calibration) can start now
            handler.postDelayed(recheck, 1500);   // evaluate() again: removes the cover if the menu is gone   // if the menu closed without an event we still clean up
        } else {
            if (closing) {   // BC2: the stock fade-OUT is being followed: the scan stops finding the menu text when the card gets transparent, BEFORE the scrim is gone; the patch ends its fade with the window's removal or the timer, not with this scan
                long left = closeStartMs + PatchFade.CLOSE_MS + CLOSE_SLACK_MS - android.os.SystemClock.uptimeMillis();
                if (left > 0) return;
            }
            // AZ1 (runs 249 / 253, emulator): ~0.5 s after the stock dialog opens, a scan sometimes finds no "Power off" / "Screenshot" text (the emulator's injected long press shows the dialog twice; a busy SystemUI also answers a
            // node search with NOTHING when it is too slow). The patch then fell back to its bright resting state for 20-170 ms = a light block on the darkened menu screen. (The window list cannot confirm the dialog:
            // its title is not "ActionsDialog" there, dialogUp stays false even while the menu is open.) So within 2 s of the opening one "not open" answer is not believed: look again 250 ms later, hide only if that agrees.
            long sinceOpen = android.os.SystemClock.uptimeMillis() - cardShownMs;
            if (custom && overlayShown && !menuOpen && Features.isCover(mode) && sinceOpen < 2000 && holdMisses < 1) {
                holdMisses++;
                Log.i(TAG, "PATCH hold: no menu text found " + sinceOpen + " ms after the menu opened: looking again before the patch goes");
                handler.removeCallbacks(recheck); handler.postDelayed(recheck, 250);
                return;
            }
            holdMisses = 0;
            // Menu not (or no longer) open: keep the invisible, pre-built card ready (W3/W4) when the cover mode is on, else drop it.
            if (custom && Features.isCover(mode)) hideOverlay(); else removeOverlay();
            if (action == PowerMenuRule.CLOSE_MENU) performGlobalAction(GLOBAL_ACTION_BACK);
        }
    }


    // ---------------------------------------------------------------- return reset (RESET2): press "Clear all" in the REAL notification shade and the REAL Recents screen
    public static final String RESET_REPORT_KEY = "reset_last";
    private final StringBuilder resetReport = new StringBuilder();

    /**
     * Runs on the main thread. Calls {@code done} when finished (always, even if nothing could be pressed). Fail-open: a missing button just means that step is skipped.
     * SPEED (V3): no fixed sleeps. After opening the shade / Recents it polls every 40 ms and presses "Clear all" the moment it appears; an EMPTY shade / Recents
     * ("No notifications" / "No recent items") ends that step at once.
     */
    public void runReset(final boolean clearNotifications, final boolean clearRecents, final Runnable done) { runReset(clearNotifications, clearRecents, null, done); }

    /** @param closeLabels null = press "Clear all"; otherwise dismiss ONLY the Recents cards of these app labels (D5: Quickstep-opened apps), falling back to Clear all if the cards cannot be found. */
    public void runReset(final boolean clearNotifications, final boolean clearRecents, final java.util.Set<String> closeLabels, final Runnable done) { runReset(clearNotifications, clearRecents, closeLabels, false, done); }

    /** @param seen a notification was posted since the shade was last cleared ({@link NotifSightings}): the shade is opened even when the status bar shows no icon (a silent notification has none). BH1e: v0.8.8 forgot this second check. */
    public void runReset(final boolean clearNotifications, final boolean clearRecents, final java.util.Set<String> closeLabels, final boolean seen, final Runnable done) {
        resetReport.setLength(0);
        guardPausedUntil = System.currentTimeMillis() + 10_000;   // Recents / the shade are opened on purpose during the reset
        removeOverlay();
        final long t0 = android.os.SystemClock.uptimeMillis();
        Log.i(TAG, "RESET start notifications=" + clearNotifications + " recents=" + clearRecents);
        final Runnable afterShade = new Runnable() { @Override public void run() {
            if (clearRecents) resetRecents(t0, done, closeLabels); else { resetReport.append("recents: skipped (nothing opened from Quickstep, or the switch is OFF)\n"); finishReset(t0, done); }
        } };
        if (!clearNotifications) {
            resetReport.append("notifications: switch is OFF\n");
            afterShade.run();
            return;
        }
        // Y2a: look at the status bar first. No notification icons there = nothing to clear = the whole shade step is skipped. Unknown (bar hidden / not found) = do the step as before.
        Boolean any = notificationIconsPresent();
        if (!NotifSightings.shadeStepWanted(true, any, seen)) {   // BH1e: no icon AND nothing posted since the last clear
            resetReport.append("notifications: none in the status bar, step skipped (+").append(android.os.SystemClock.uptimeMillis() - t0).append(" ms)\n");
            Log.i(TAG, "RESET notifications skipped: status bar shows none");
            afterShade.run();
            return;
        }
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        pollPress("clear all", new String[] {"no notifications"}, new String[] {"com.android.systemui"}, 1000, false, new PollResult() {
            @Override public void done(int result) {
                resetReport.append("notifications: ").append(result == PRESSED ? "cleared" : result == EMPTY ? "none to clear" : "no Clear all button found")
                        .append(" (+").append(android.os.SystemClock.uptimeMillis() - t0).append(" ms)\n");
                Log.i(TAG, "RESET notifications result=" + result + " +" + (android.os.SystemClock.uptimeMillis() - t0) + "ms");
                performGlobalAction(GLOBAL_ACTION_BACK);
                handler.postDelayed(afterShade, ReturnReset.fastActive ? 40 : 90);   // 30 ms was too short on the emulator (Recents opened while the shade was still collapsing); with animations off 40 ms is enough
            }
        });
    }

    /** TRUE = the status bar shows at least one notification icon, FALSE = it shows none, null = cannot tell (then the shade step runs as before). */
    private Boolean notificationIconsPresent() {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return null;
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                try {
                    CharSequence pkg = root.getPackageName();
                    if (pkg == null || !"com.android.systemui".contentEquals(pkg)) continue;
                    List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/notificationIcons");
                    if (hit == null || hit.isEmpty()) continue;
                    AccessibilityNodeInfo box = hit.get(0);
                    if (!box.isVisibleToUser()) return null;   // status bar hidden (full-screen app): the empty list means nothing
                    int n = 0;
                    for (int i = 0; i < box.getChildCount(); i++) {
                        AccessibilityNodeInfo ch = box.getChild(i);
                        if (ch == null) continue;
                        if (!KeepAlivePolicy.isOurIcon(ch.getContentDescription())) n++;   // AA2: our own do-nothing notification is not "something to clear"
                        ch.recycle();
                    }
                    return n > 0;
                } finally { root.recycle(); }
            }
        } catch (Throwable t) { Log.e(TAG, "notification icon scan failed", t); }
        return null;
    }

    private static final int PRESSED = 1, EMPTY = 2, TIMEOUT = 3;
    private interface PollResult { void done(int result); }

    /** Polls every 20 ms (first look immediately): press {@code label} if it is there; end with EMPTY if one of {@code emptyTexts} is visible instead; scroll now and then if asked; TIMEOUT after maxMs. */
    private void pollPress(final String label, final String[] emptyTexts, final String[] packages, final long maxMs, final boolean scroll, final PollResult cb) {
        final long deadline = android.os.SystemClock.uptimeMillis() + maxMs;
        final int[] tick = {0};
        handler.post(new Runnable() { @Override public void run() {
            List<AccessibilityNodeInfo> roots = null;   // BK1c: the windows' roots are fetched ONCE per look and shared by both searches (the notification step took 303 ms on the emulator, run #396)
            try {
                roots = recentsRoots();
                if (pressInRoots(roots, label, packages)) { cb.done(PRESSED); return; }
                if (emptyTexts != null && emptyTexts.length > 0 && anyTextInRoots(roots, emptyTexts, packages)) { cb.done(EMPTY); return; }
                tick[0]++;
                if (scroll && tick[0] % 5 == 0) scrollSomething(packages);
            } catch (Throwable t) { Log.e(TAG, "poll failed", t); }
            finally { if (roots != null) for (AccessibilityNodeInfo r : roots) { try { r.recycle(); } catch (Throwable ignored) { } } }
            if (android.os.SystemClock.uptimeMillis() >= deadline) { cb.done(TIMEOUT); return; }
            handler.postDelayed(this, 20);
        } });
    }

    private boolean anyTextPresent(String[] texts, String[] packages) {
        if (texts == null || texts.length == 0) return false;
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return false;
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                try {
                    String pkg = root.getPackageName() == null ? "" : root.getPackageName().toString();
                    boolean ok = false;
                    for (String p : packages) if (p.equals(pkg)) ok = true;
                    if (!ok) continue;
                    for (String t : texts) {
                        List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByText(t);
                        if (hit != null && !hit.isEmpty()) return true;
                    }
                } finally { root.recycle(); }
            }
        } catch (Throwable t) { Log.e(TAG, "anyTextPresent failed", t); }
        return false;
    }

    private volatile int recentsActionOk = -1;   // BN8d: what performGlobalAction(GLOBAL_ACTION_RECENTS) returned in the last reset (-1 = not asked yet)

    private void resetRecents(final long t0, final Runnable done, final java.util.Set<String> closeLabels) {
        recentsActionOk = performGlobalAction(GLOBAL_ACTION_RECENTS) ? 1 : 0;
        if (closeLabels == null || closeLabels.isEmpty()) {
            if (ReturnReset.cleanStart(this)) recentsEmpty(t0, done); else recentsClearAll(t0, done);   // BE2/BE3: a clean start empties the list whichever way this Recents screen allows
            return;
        }
        // D5: dismiss only the cards of apps opened from Quickstep (each Quickstep card carries a "Close" accessibility action); anything unexpected falls back to Clear all.
        final long deadline = android.os.SystemClock.uptimeMillis() + 2500;
        final int[] closed = {0}, idle = {0};
        final boolean[] sawCards = {false};
        final String[] seen = {null};   // AI1: what the Recents cards looked like (for the lock log)
        handler.post(new Runnable() { @Override public void run() {
            try {
                List<AccessibilityNodeInfo> cards = findTaskCards();
                if (cards.size() > 0) { sawCards[0] = true; if (seen[0] == null) seen[0] = cards.size() + " card(s): " + describeCards(cards, closeLabels); }
                boolean did = false;
                for (AccessibilityNodeInfo card : cards) {
                    if (!AppOrigin.cardMatches(card.getContentDescription() == null ? null : card.getContentDescription().toString(), closeLabels)) continue;
                    for (AccessibilityNodeInfo.AccessibilityAction act : card.getActionList()) {
                        if (act.getLabel() != null && "close".equalsIgnoreCase(act.getLabel().toString().trim()) && card.performAction(act.getId())) { closed[0]++; did = true; break; }
                    }
                    if (did) break;   // the list changed: look again
                }
                if (did && android.os.SystemClock.uptimeMillis() < deadline + 2000) { idle[0] = 0; handler.postDelayed(this, 30); return; }   // never past the deadline (a card that does not go away must not keep this loop alive)
                if (sawCards[0] && ++idle[0] >= 3) {   // cards are there and none of ours is left
                    AppOrigin.clearQuickstep(TjAccessibilityService.this); AppOrigin.setTracked(TjAccessibilityService.this);
                    resetReport.append("recents: closed ").append(closed[0]).append(" card(s) of apps opened from Quickstep, others kept (+").append(android.os.SystemClock.uptimeMillis() - t0).append(" ms)\n");
                    Log.i(TAG, "RESET recents selective closed=" + closed[0]);
                    LockLog.add(getApplicationContext(), "RESET", "recents: closed " + closed[0] + " card(s) of " + closeLabels + "; saw " + seen[0]);
                    finishReset(t0, done);
                    return;
                }
            } catch (Throwable t) { Log.e(TAG, "selective recents failed", t); }
            if (android.os.SystemClock.uptimeMillis() >= deadline) {
                if (!sawCards[0]) { Log.i(TAG, "RESET recents: cards not recognised, falling back to Clear all"); LockLog.add(getApplicationContext(), "RESET", "recents: no card with a 'Close' action found for " + closeLabels + " (this Recents screen is not recognised): falling back to Clear all"); recentsClearAll(t0, done); }
                else { resetReport.append("recents: stopped after ").append(closed[0]).append(" card(s)\n"); LockLog.add(getApplicationContext(), "RESET", "recents: stopped after " + closed[0] + " card(s) of " + closeLabels + "; saw " + seen[0]); finishReset(t0, done); }
                return;
            }
            handler.postDelayed(this, 40);
        } });
    }

    /**
     * AI1: what this tablet's Recents cards look like to accessibility, for the lock log. v0.7.6 (review of v0.7.5, privacy): WITHOUT any text we did not already know. A card description is the app label plus
     * possibly a page title or a conversation name, and the lock log goes into the tablet report. So up to five cards are described only by the matching label from {@code known} (the apps we mean to close),
     * a "+" when the description had more text than that label, or "(other, N chars)" when it matches none.
     */
    private static String describeCards(List<AccessibilityNodeInfo> cards, java.util.Set<String> known) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < cards.size() && i < 5; i++) {
            CharSequence d = cards.get(i).getContentDescription();
            String s = d == null ? "" : d.toString().replace('\n', ' ').trim();
            String hit = null;
            if (known != null) for (String k : known) {
                if (k != null && !k.isEmpty() && s.toLowerCase(java.util.Locale.ROOT).contains(k.toLowerCase(java.util.Locale.ROOT))) { hit = k; break; }
            }
            if (i > 0) b.append(" | ");
            b.append(d == null ? "(no description)" : hit == null ? "(other, " + s.length() + " chars)" : hit + (s.length() > hit.length() ? "+" : ""));
        }
        return b.toString();
    }

    /** Recents task cards: nodes (in the stock launcher / SystemUI windows) that offer a "Close" action. */
    private List<AccessibilityNodeInfo> findTaskCards() {
        List<AccessibilityNodeInfo> out = new java.util.ArrayList<>();
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return out;
        for (AccessibilityWindowInfo w : windows) {
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) continue;
            CharSequence pkg = root.getPackageName();
            if (pkg == null || !(pkg.toString().startsWith("com.android.launcher3") || "com.android.systemui".contentEquals(pkg))) continue;
            collectCards(root, 0, out);
        }
        return out;
    }

    private void collectCards(AccessibilityNodeInfo n, int depth, List<AccessibilityNodeInfo> out) {
        if (n == null || depth > 8) return;
        for (AccessibilityNodeInfo.AccessibilityAction a : n.getActionList()) {
            if (a.getLabel() != null && "close".equalsIgnoreCase(a.getLabel().toString().trim())) { out.add(n); return; }
        }
        for (int i = 0; i < n.getChildCount(); i++) collectCards(n.getChild(i), depth + 1, out);
    }

    // ---------------------------------------------------------------- BK1c: ONE fetch of the Recents windows per look
    // Tj, 2026-11-08 18:16 UTC: "the custom launcher still loads from the lock screen on a delay". Emulator run #393 (section cs, no helper, plain PIN after a Quickstep session): the black cover stayed up 1466 ms after the keyguard went
    // and about 1050 ms of that was the Recents loop, which looked at the screen three times per iteration (pressInSystemUi, anyTextPresent, findTaskCards), each time asking for every window and fetching every window's root
    // (an IPC per window, the status bar and the navigation bar included). Same searches, same order, same package rules, but the roots are fetched ONCE per look and shared.
    private List<AccessibilityNodeInfo> recentsRoots() {
        List<AccessibilityNodeInfo> out = new java.util.ArrayList<>();
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows == null) return out;
        for (AccessibilityWindowInfo w : windows) {
            AccessibilityNodeInfo root = w.getRoot();
            if (root == null) continue;
            CharSequence pkg = root.getPackageName();
            if (pkg != null && (pkg.toString().startsWith("com.android.launcher3") || "com.android.systemui".contentEquals(pkg))) out.add(root); else root.recycle();
        }
        return out;
    }

    private boolean pressInRoots(List<AccessibilityNodeInfo> roots, String label, String... packages) {
        for (AccessibilityNodeInfo root : roots) {
            String pkg = root.getPackageName() == null ? "" : root.getPackageName().toString();
            boolean ok = false;
            for (String p : packages) if (p.equals(pkg)) ok = true;
            if (!ok) continue;
            List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByText(label);
            if (hit == null) continue;
            for (AccessibilityNodeInfo n : hit) {
                CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
                if (t == null || !t.toString().trim().toLowerCase(java.util.Locale.ROOT).startsWith(label)) continue;
                if (clickableAncestor(n).performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
        }
        return false;
    }

    private boolean anyTextInRoots(List<AccessibilityNodeInfo> roots, String[] texts, String[] packages) {
        for (AccessibilityNodeInfo root : roots) {
            String pkg = root.getPackageName() == null ? "" : root.getPackageName().toString();
            boolean ok = false;
            for (String p : packages) if (p.equals(pkg)) ok = true;
            if (!ok) continue;
            for (String t : texts) {
                List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByText(t);
                if (hit != null && !hit.isEmpty()) return true;
            }
        }
        return false;
    }

    private List<AccessibilityNodeInfo> cardsInRoots(List<AccessibilityNodeInfo> roots) {
        List<AccessibilityNodeInfo> out = new java.util.ArrayList<>();
        for (AccessibilityNodeInfo root : roots) collectCards(root, 0, out);
        return out;
    }

    /**
     * BE2/BE3 (clean start, no helper): empties the Recents screen COMPLETELY, whichever way this Recents screen allows. The first CI run of the clean start (emulator run #307) found "Clear all" not reachable in 1.8 s on
     * the AOSP Quickstep screen (the button sits at the far end of the carousel), so nothing was cleared. Every 40 ms: "Clear all" pressable = press it; "No recent items" = done; else EVERY card that offers a "Close" action is
     * closed in one pass; a nudge scrolls the carousel every 5th look (a "Clear all" at the end); three looks in a row without a card after some were seen = done. Gives up after 2.5 s and says what it saw.
     */
    private void recentsEmpty(final long t0, final Runnable done) {
        final long deadline = android.os.SystemClock.uptimeMillis() + 2500;
        final int[] tick = {0}, closed = {0}, idle = {0};
        final boolean[] saw = {false};
        final java.util.HashMap<String, Integer> pressed = new java.util.HashMap<>();   // card description -> how often "Close" was pressed on it
        final String[] pk = {"com.android.launcher3", "com.android.systemui"};
        handler.post(new Runnable() { @Override public void run() {
            // The deadline is checked FIRST, in every branch: the first version returned early after pressing "Close" and could press a card that never went away for ever, starving the main thread
            // (emulator runs #313 / #314: no result line at all, a hung reset). A card is pressed at most three times; only nodes with a description (a card carries its app's name) count as cards.
            final boolean over = android.os.SystemClock.uptimeMillis() >= deadline;
            List<AccessibilityNodeInfo> roots = null;   // BK1c: fetched once for this whole look, recycled in the finally below
            try {
                if (!over) {
                    roots = recentsRoots();
                    if (pressInRoots(roots, "clear all", pk)) { finishRecentsEmpty(t0, done, "Clear all pressed", true, closed[0]); return; }
                    if (anyTextInRoots(roots, new String[] {"no recent items"}, pk)) { finishRecentsEmpty(t0, done, "the list was empty", true, closed[0]); return; }
                    boolean pressedOne = false, leftover = false;
                    for (AccessibilityNodeInfo card : cardsInRoots(roots)) {
                        CharSequence d = card.getContentDescription();
                        String key = d == null ? "" : d.toString().trim();
                        if (key.isEmpty()) continue;
                        saw[0] = true;
                        int n = pressed.containsKey(key) ? pressed.get(key) : 0;
                        if (n >= 3) { leftover = true; continue; }                          // it does not go away: left alone, reported at the end
                        for (AccessibilityNodeInfo.AccessibilityAction act : card.getActionList()) {
                            if (act.getLabel() != null && "close".equalsIgnoreCase(act.getLabel().toString().trim()) && card.performAction(act.getId())) { pressed.put(key, n + 1); closed[0]++; pressedOne = true; break; }
                        }
                    }
                    if (pressedOne) { idle[0] = 0; handler.postDelayed(this, 40); return; }
                    tick[0]++;
                    if (tick[0] % 5 == 0) scrollSomething(pk);
                    if (saw[0] && ++idle[0] >= 3) {
                        finishRecentsEmpty(t0, done, leftover ? "closed " + closed[0] + " card(s), but some cards would not go away: Recents is NOT verified empty" : "closed " + closed[0] + " card(s), none left", !leftover, closed[0]);
                        return;
                    }
                }
            } catch (Throwable t) { Log.e(TAG, "empty recents failed", t); }
            finally { if (roots != null) for (AccessibilityNodeInfo r : roots) { try { r.recycle(); } catch (Throwable ignored) { } } }
            if (over) { finishRecentsEmpty(t0, done, saw[0] ? "STOPPED after " + closed[0] + " card(s), time ran out: Recents is NOT verified empty" : "no Clear all button and no card with a Close action found: nothing was cleared | " + recentsDiag(), false, closed[0]); return; }   // a partial clear never marks Recents clean (code review 2026-11-07): the next return tries again
            handler.postDelayed(this, 40);
        } });
    }

    /**
     * BN8d (Tj, 2026-11-10: "All of my quickstep apps were in the recent apps list in the custom launcher"; the tablet report said "no Clear all button and no card with a Close action found" after the whole 2.5 s): what the
     * screen looked like when the clean-up found nothing, so the next report can say WHY (Recents never opened, a different screen, other labels). Whether the Recents action was accepted; every window with its type, layer and
     * root package (BN8f3: the stock launcher's window FIRST and the others short, because the report cuts a value at 600 characters); for the stock launcher's windows the node classes with counts and whether the fixed words
     * "Clear all" / "No recent items" are on screen (BN8f1: NO node text is copied, see nodeSummary). Never throws.
     */
    private String recentsDiag() {
        String act = recentsActionOk < 0 ? "not asked" : recentsActionOk == 1 ? "accepted" : "REFUSED";
        StringBuilder lead = new StringBuilder(), rest = new StringBuilder();   // lead = the stock launcher's windows, rest = every other window in about 25 characters
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return "recents action " + act + "; no windows";
            int shown = 0;
            for (AccessibilityWindowInfo w : windows) {
                if (shown++ >= 8) break;
                AccessibilityNodeInfo root = w.getRoot();
                String pkg = root == null || root.getPackageName() == null ? "-" : root.getPackageName().toString();
                boolean stock = pkg.startsWith("com.android.launcher3");
                StringBuilder one = stock ? lead : rest;
                one.append(" [t").append(w.getType()).append(" l").append(w.getLayer()).append(w.isActive() ? "a " : " ").append(pkg.startsWith("com.") ? pkg.substring(4) : pkg);
                if (root != null) {
                    try { if (stock) one.append(' ').append(nodeSummary(root)); } finally { root.recycle(); }
                }
                one.append(']');
            }
        } catch (Throwable t) { rest.append(" (diag failed: ").append(t.getClass().getSimpleName()).append(')'); }
        String s = "recents action " + act + ";" + lead + rest;
        return s.length() > 450 ? s.substring(0, 450) : s;
    }

    /**
     * Node classes (simple names with counts), how many nodes have text and how many are clickable, and whether the FIXED words "Clear all" / "No recent items" are on screen, for one window tree, to depth 10 and 250 nodes.
     * BN8f1: NO node text is copied (Recents card text is app labels and conversation titles, and the tablet report is published): only counts and two fixed words leave this method (RecentsDiag).
     */
    private String nodeSummary(AccessibilityNodeInfo root) {
        java.util.LinkedHashMap<String, Integer> classes = new java.util.LinkedHashMap<String, Integer>();
        int[] tally = new int[7];   // 0 nodes with text or description, 1 clickable nodes, 2 "clear all" seen, 3 "no recent items" seen, 4 nodes with a labelled action, 5 nodes with a "Close" action, 6 scrollable nodes
        walkSummary(root, 0, classes, tally, new int[] {250});
        StringBuilder b = new StringBuilder("nodes{");
        boolean first = true;
        for (java.util.Map.Entry<String, Integer> e : classes.entrySet()) { if (!first) b.append(','); first = false; b.append(e.getKey()).append('x').append(e.getValue()); }
        b.append("} text=").append(tally[0]).append(" click=").append(tally[1]).append(" acts=").append(tally[4]).append(" close=").append(tally[5]).append(" scroll=").append(tally[6]);   // the clean-up presses "Clear all" or a card's "Close" action: these counts say which of the two this screen offers
        if (tally[2] > 0) b.append(" CLEARALL");
        if (tally[3] > 0) b.append(" NOITEMS");
        return b.toString();
    }

    private void walkSummary(AccessibilityNodeInfo n, int depth, java.util.Map<String, Integer> classes, int[] tally, int[] budget) {
        if (n == null || depth > 10 || budget[0]-- <= 0) return;
        CharSequence cn = n.getClassName();
        String k = cn == null ? "?" : cn.toString();
        k = k.substring(Math.max(k.lastIndexOf('.'), k.lastIndexOf('$')) + 1);
        Integer c = classes.get(k);
        classes.put(k, c == null ? 1 : c + 1);
        CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
        if (t != null && t.length() > 0) { tally[0]++; int w = RecentsDiag.word(t); if (w == RecentsDiag.CLEAR_ALL) tally[2]++; else if (w == RecentsDiag.NO_ITEMS) tally[3]++; }   // only counts and two fixed words, never the text
        if (n.isClickable()) tally[1]++;
        if (n.isScrollable()) tally[6]++;
        try {   // action LABELS are only matched against the fixed word "Close" (the one recentsEmpty presses): counts leave, never a label
            boolean labelled = false, close = false;
            for (AccessibilityNodeInfo.AccessibilityAction a : n.getActionList()) {
                CharSequence l = a.getLabel();
                if (l != null && l.length() > 0) { labelled = true; if (RecentsDiag.isCloseLabel(l)) close = true; }
            }
            if (labelled) tally[4]++;
            if (close) tally[5]++;
        } catch (Throwable ignored) { }
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo ch = n.getChild(i);
            if (ch != null) { try { walkSummary(ch, depth + 1, classes, tally, budget); } finally { ch.recycle(); } }
        }
    }

    private void finishRecentsEmpty(long t0, Runnable done, String what, boolean ok, int closed) {
        if (ok) { RecentsDirty.clear(this); AppOrigin.clearQuickstep(this); AppOrigin.setTracked(this); ReturnReset.clearBootClean(this); }   // Recents is clean again (and the boot flag is used up)
        resetReport.append("recents: ").append(what).append(" (+").append(android.os.SystemClock.uptimeMillis() - t0).append(" ms)\n");
        Log.i(TAG, "RESET recents (complete) " + what + " +" + (android.os.SystemClock.uptimeMillis() - t0) + "ms");
        LockLog.add(getApplicationContext(), "RESET", "recents (complete clear): " + what + ", " + closed + " card(s) closed one by one");
        finishReset(t0, done);
    }

    private void recentsClearAll(final long t0, final Runnable done) {
        pollPress("clear all", new String[] {"no recent items"}, new String[] {"com.android.launcher3", "com.android.systemui"}, 1800, true, new PollResult() {
            @Override public void done(int result) {
                if (result == PRESSED || result == EMPTY) { RecentsDirty.clear(TjAccessibilityService.this); AppOrigin.clearQuickstep(TjAccessibilityService.this); AppOrigin.setTracked(TjAccessibilityService.this); }   // Recents is clean again
                resetReport.append("recents: ").append(result == PRESSED ? "cleared (apps closed)" : result == EMPTY ? "already empty" : "no Clear all button found | " + recentsDiag())
                        .append(" (+").append(android.os.SystemClock.uptimeMillis() - t0).append(" ms)\n");
                Log.i(TAG, "RESET recents result=" + result + " +" + (android.os.SystemClock.uptimeMillis() - t0) + "ms");
                LockLog.add(getApplicationContext(), "RESET", "recents: Clear all " + (result == PRESSED ? "pressed" : result == EMPTY ? "found the list empty" : "button NOT FOUND: nothing was cleared"));   // AI1: the lock log says what really happened
                finishReset(t0, done);
            }
        });
    }

    private void finishReset(final long t0, final Runnable done) {
        // NO Home press here: on the slow tablet it landed AFTER the app start and covered it.
        handler.postDelayed(new Runnable() { @Override public void run() {
            resetReport.append("took ").append(android.os.SystemClock.uptimeMillis() - t0).append(" ms\n");
            Features.setString(TjAccessibilityService.this, RESET_REPORT_KEY, resetReport.toString());
            try { done.run(); } catch (Throwable t) { Log.e(TAG, "reset done failed", t); }
        } }, ReturnReset.fastActive ? 15 : 40);
    }

    /** True if a window of this package is the active (front) one. */
    public boolean isForeground(String pkg) {
        try {
            AccessibilityNodeInfo r = getRootInActiveWindow();
            if (r == null) return false;
            try { return pkg.contentEquals(r.getPackageName() == null ? "" : r.getPackageName()); } finally { r.recycle(); }
        } catch (Throwable t) { return false; }
    }

    public void appendReport(String line) {
        resetReport.append(line).append("\n");
        Features.setString(this, RESET_REPORT_KEY, resetReport.toString());
    }

    /** Presses the first clickable node whose text starts with {@code label}, but ONLY inside windows of the given system packages (never in another app). */
    private boolean pressInSystemUi(String label, String... packages) {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return false;
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                try {
                    String pkg = root.getPackageName() == null ? "" : root.getPackageName().toString();
                    boolean ok = false;
                    for (String p : packages) if (p.equals(pkg)) ok = true;
                    if (!ok) continue;
                    List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByText(label);
                    if (hit == null) continue;
                    for (AccessibilityNodeInfo n : hit) {
                        CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
                        if (t == null || !t.toString().trim().toLowerCase(java.util.Locale.ROOT).startsWith(label)) continue;
                        if (clickableAncestor(n).performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                    }
                } finally { root.recycle(); }
            }
        } catch (Throwable t) { Log.e(TAG, "pressInSystemUi failed", t); }
        return false;
    }

    private void scrollSomething(String... packages) {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return;
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                try {
                    String pkg = root.getPackageName() == null ? "" : root.getPackageName().toString();
                    boolean ok = false;
                    for (String p : packages) if (p.equals(pkg)) ok = true;
                    if (!ok) continue;
                    AccessibilityNodeInfo sc = findScrollable(root, 0);
                    if (sc != null && sc.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return;
                } finally { root.recycle(); }
            }
        } catch (Throwable t) { Log.e(TAG, "scroll failed", t); }
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 8) return null;
        if (n.isScrollable()) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo r = findScrollable(n.getChild(i), depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    /** AC6: writes the stock menu's node tree + the numbers used into the report file (at most once per 20 s per lock state). Never throws. */
    private void dumpMenu(AccessibilityNodeInfo root, boolean locked, Rect panel) {
        try {
            if (!PowerMenuDump.due(locked)) return;
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
            int[] card = PowerMenuDump.cardFromPanel(panel.left, panel.top, panel.right, panel.bottom, lastPitch, lastCut);
            StringBuilder h = new StringBuilder();
            h.append("written ").append(now()).append(" in the ").append(Mode.isCustom(this) ? "CUSTOM" : "Quickstep").append(" launcher, ").append(locked ? "on the LOCK SCREEN" : "unlocked").append('\n');
            h.append("display ").append(dm.widthPixels).append('x').append(dm.heightPixels).append(" px, rotation ").append(rotation()).append(", density ").append(dm.density).append(" (").append(dm.densityDpi).append(" dpi), font scale ").append(getResources().getConfiguration().fontScale).append('\n');
            h.append("stock panel ").append(panel.toShortString()).append(" (").append(panel.width()).append('x').append(panel.height()).append("), first entry top ").append(lastOffTop).append(" height ").append(lastItemH).append(", entry pitch ").append(lastPitch).append(", Screenshot entry top ").append(lastCut).append('\n');
            h.append("our card would be [").append(card[0]).append(',').append(card[1]).append("][").append(card[2]).append(',').append(card[3]).append("] (").append(card[2] - card[0]).append('x').append(card[3] - card[1]).append(")\n");
            h.append("patch: ").append(Features.getString(this, PATCH_STATUS_KEY, "?")).append(" | calibration: ").append(Features.getString(this, CAL_STATUS_KEY, "?")).append(" | brightness nudge ").append(Features.getInt(this, PATCH_DIM_KEY, 0)).append(" % | patch nudges dx ").append(Features.getInt(this, PATCH_DX_KEY, 0)).append(" dy ").append(Features.getInt(this, PATCH_DY_KEY, 0)).append(" size ").append(Features.getInt(this, PATCH_SCALE_KEY, 0)).append(" %\n");
            h.append("tree of the menu window (class #id 'label' [left,top][right,bottom] click):\n");
            PowerMenuDump.write(this, locked, h.toString(), PowerMenuDump.walk(root));
        } catch (Throwable t) { Log.e(TAG, "power menu dump failed", t); }
    }

    private AccessibilityNodeInfo clickableAncestor(AccessibilityNodeInfo n) {
        AccessibilityNodeInfo t = n;
        for (int i = 0; i < 4 && t != null && !t.isClickable(); i++) t = t.getParent();
        return t != null && t.isClickable() ? t : n;
    }

    /** Presses the real system button with this label (found fresh, never a stale node). Returns false if it could not. */
    private boolean pressReal(String label) {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return false;
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                // never "press" our own card: it is the topmost window and carries the same labels (that was an endless self-click)
                if (getPackageName().contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) { root.recycle(); continue; }
                try {
                    List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByText(label);
                    if (hit != null && !hit.isEmpty()) {
                        boolean ok = clickableAncestor(hit.get(0)).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.i(TAG, "press '" + label + "' -> " + ok);
                        return ok;
                    }
                } finally { root.recycle(); }
            }
        } catch (Throwable t) { Log.e(TAG, "press failed", t); }
        return false;
    }

    /**
     * AH3 (v0.7.6): the layout INSIDE the card follows the stock menu, measured on Tj's side-by-side photos (stock card 110.8 x 208.0 px = 105 x 196 dp): 16 dp top padding, then two entries of 90 dp; in each the
     * icon (24 dp, its glyph 2 dp inside) starts 20 dp down and the 12 dp label starts 5 dp below the icon. The stock label ignores the font scale (its "Power off" is 54 px wide, ours was 64). Before: two equal
     * halves with centred content, which put the first icon about 11 px too high, the entries about 9 px too far apart and the labels 16 % too big. {@code k} scales everything with the card's height.
     */
    private View cardButton(String label, int iconRes, final String realLabel, float d, float k) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER_HORIZONTAL);
        b.setPadding(0, Math.round(20 * d * k), 0, 0);
        b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(90 * d * k)));
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageDrawable(getDrawable(iconRes));
        b.addView(icon, new LinearLayout.LayoutParams(Math.round(24 * d * k), Math.round(24 * d * k)));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 12 * d * k);   // dp, not sp: the stock label does not follow the font scale
        t.setIncludeFontPadding(false);
        t.setTextColor(Color.parseColor("#FF5F6368"));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Math.round(5 * d * k), 0, 0);
        b.addView(t);
        b.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                Log.i(TAG, "card touch " + realLabel + " action=" + e.getActionMasked());
                return false;
            }
        });
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Log.i(TAG, "card click " + realLabel);
                // fail open: if the real button cannot be pressed, take our card away so the original menu is usable
                if (!pressReal(realLabel)) { Log.i(TAG, "press '" + realLabel + "' -> false (revealing original menu)"); removeOverlay(); }
            }
        });
        return b;
    }

    /** {@code heightPx} = the card window's height; the layout (16 dp + 2 x 90 dp = 196 dp at the stock size) scales with it between 0.8 and 1.25, so another panel size still fits. */
    private View buildCard(int heightPx) {
        float d = getResources().getDisplayMetrics().density;
        float k = Math.max(0.8f, Math.min(1.25f, heightPx / (196f * d)));
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(PANEL_COLOR);
        card.setPadding(0, Math.round(16 * d * k), 0, 0);
        card.addView(cardButton("Power off", R.drawable.tj_ic_power, "Power off", d, k));
        card.addView(cardButton("Restart", R.drawable.tj_ic_restart, "Restart", d, k));
        return card;
    }

    private boolean overlayShown = false, overlayTouchable = false;

    private WindowManager.LayoutParams overlayParams(Rect r, boolean touchable) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                r.width(), r.height(), WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = r.left; lp.y = r.top;
        return lp;
    }

    /** V1: on the lock screen the patch is invisible until the power menu opens (and again afterwards). */
    /** AZ1: the stock menu has just opened: start following its fade-in. {@code lastWinEventMs} is the window event that told us (the stock fade began about then); an old one means "complete", i.e. no fade. */
    private void startFade() {
        long now = android.os.SystemClock.uptimeMillis();
        boolean on = patchFadeOn();
        long anchor = dialogEventMs > 0 && now - dialogEventMs <= PatchFade.FRESH_MS && dialogEventMs >= winBurstStartMs ? dialogEventMs : winBurstStartMs;   // the dialog's own event when it is the newest, else the burst's first one
        long lag = on ? PatchFade.lagMs(now, anchor) : PatchFade.OPEN_MS;
        closing = false; handler.removeCallbacks(closeFinishRun);   // BC2: a new opening ends any fade-out that was still being followed
        fadeLeadMs = PatchFade.LEAD_MS;
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) { int v = Features.getInt(this, "patch_fade_lead", Integer.MIN_VALUE); if (v != Integer.MIN_VALUE) fadeLeadMs = v; }   // debug builds only (the emulator sweeps it in section pt; may be negative)
        fadeLeadMs += Features.getInt(this, PATCH_FADE_SHIFT_KEY, 0);
        fadeStartMs = now - lag;
        fading = on && PatchFade.fading(now, fadeStartMs, fadeLeadMs);
        fadeLagLast = lag;
        fadeFrameLastMs = 0; fadeWorstGapMs = 0; fadeFirstDrawLateMs = -1; fadeFrames = 0; fadeSlowFrames = 0; fadeHomeFlavour = patchHomeMode;   // BJ1: a new opening, new frame statistics
        Log.i(TAG, "PATCH fade " + (fading ? "start" : "none") + " lag=" + lag + " ms t=" + now + " lead=" + fadeLeadMs);
    }

    /**
     * AZ1: a frame of the patch was drawn. While the fade runs ask for the next one; the end of the fade is decided by the CLOCK here too (never only by the painter), so a draw path that does not compute the
     * progress (no bitmap to draw) can never leave a redraw loop running: the frame after the last one is drawn with the final brightness and asks for nothing more.
     */
    private void fadeFrameDone(View v) {
        if (closing) { v.postInvalidateOnAnimation(); return; }   // BC2: the fade-OUT is ended by the dialog window's removal or by its own timer (finishClose), never by the painter
        if (!fading) return;
        v.postInvalidateOnAnimation();
        if (!PatchFade.fading(android.os.SystemClock.uptimeMillis(), fadeStartMs, fadeLeadMs)) fading = false;
    }

    /**
     * BC2: the stock dialog began to fade OUT at {@code startMs} (uptime ms): follow its curve ({@link PatchFade#closeProgress}) until its window is removed. Lock-screen patch, open menu, fade switch on, no card of ours
     * (the cardless patch of v0.8.x) - anything else keeps the old behaviour (the node scan ends the open look).
     */
    private void startClose(long startMs, String why) {
        try {
            if (closing || !overlayShown || patchView == null || !patchLockMode || !patchFadeOn() || cardWanted()) return;
            long now = android.os.SystemClock.uptimeMillis();
            closing = true; closeStartMs = startMs + PatchFade.CLOSE_START_DELAY_MS; fading = false;   // SystemUI's animator starts a little after the sign (PatchFade.CLOSE_START_DELAY_MS)
            Log.i(TAG, "PATCH close start (" + why + ") t=" + now + " began=" + (startMs - now) + " ms from now lead=" + fadeLeadMs);
            handler.removeCallbacks(closeFinishRun);
            handler.postDelayed(closeFinishRun, Math.max(0, closeStartMs + PatchFade.CLOSE_MS + CLOSE_SAFETY_MS - now));
            patchView.invalidate();   // the first frame now; fadeFrameDone asks for every following one
        } catch (Throwable t) { closing = false; Log.e(TAG, "close start failed", t); }
    }

    /** BC2: the fade-out time is over and no removal event came (or a sign of a dismissal was wrong): the normal scan decides, the open look returns if the menu is still there. */
    private void finishClose(String why) {
        handler.removeCallbacks(closeFinishRun);
        if (!closing) return;
        Log.i(TAG, "PATCH close end (" + why + ") t=" + android.os.SystemClock.uptimeMillis());
        try { evaluate(); } catch (Throwable t) { Log.e(TAG, "evaluate failed", t); removeOverlay(); }   // `closing` is still set here, so no frame is drawn at the open brightness in between: a menu that is gone ends it (patchShow(false))
        if (closing) { closing = false; if (patchView != null) patchView.invalidate(); }   // the scan says the menu is still open (the sign was wrong): the open look returns
    }

    /** BC2: the stock dialog's window was removed (its removal event, no node scan): the patch goes back to rest in the same frame the stock scrim is gone. */
    private void dialogGone() {
        long since = android.os.SystemClock.uptimeMillis() - cardShownMs;
        if (!overlayShown || since < 800) return;   // not open for us, or the first 0.8 s: the emulator's injected long press shows the dialog twice (the scan path has its own hold for that)
        boolean was = closing;
        closing = false; handler.removeCallbacks(closeFinishRun);
        dialogWindowId = -1;
        Log.i(TAG, "PATCH close end (the dialog window was removed" + (was ? "" : "; no earlier sign of the dismissal") + ") t=" + android.os.SystemClock.uptimeMillis());
        hideOverlay();
    }

    /**
     * BC2: the signs of the stock dismissal. A click in the stock dialog's window (the empty area's click listener, an entry) starts the fade-out at the click's own time (the stock calls dismiss() in the same
     * call); the removal of the dialog's window ends it. Both are window-id checks on the event itself: no binder call, nothing that can block the thread that draws the fade.
     */
    /** BY1c F1: SystemUI's pane titles in the device's language (read once; the English ones in LockShade are the fallback). */
    private void loadShadeTitles() {
        if (shadeOpenTitles != null) return;
        String closed = null;
        String[] open = new String[LockShade.OPEN_RES.length];
        try {
            android.content.res.Resources r = getPackageManager().getResourcesForApplication("com.android.systemui");
            int id = r.getIdentifier(LockShade.CLOSED_RES, "string", "com.android.systemui");
            if (id != 0) closed = r.getString(id);
            for (int i = 0; i < open.length; i++) { int oi = r.getIdentifier(LockShade.OPEN_RES[i], "string", "com.android.systemui"); if (oi != 0) open[i] = r.getString(oi); }
        } catch (Throwable t) { Log.i(TAG, "PATCH shade titles: SystemUI's own strings not readable (" + t + "), the English ones are used"); }
        shadeClosedTitle = closed;
        shadeOpenTitles = open;
    }

    /** BY1c F1: a pane-title event of SystemUI's notification panel (LockShade). Cheap first test: almost every event stops at the change-type bit. */
    private void lockShadeEvent(AccessibilityEvent e) {
        if ((e.getContentChangeTypes() & LockShade.PANE_TITLE) == 0) return;
        CharSequence pk = e.getPackageName();
        if (pk == null || !"com.android.systemui".contentEquals(pk)) return;
        loadShadeTitles();
        CharSequence title = e.getText() == null || e.getText().isEmpty() ? null : e.getText().get(0);
        noteShade(LockShade.kind(true, e.getContentChangeTypes(), title, shadeClosedTitle, shadeOpenTitles), "event");
    }

    private final Rect winBounds = new Rect();
    /** BY1a B2 / B3: a system window (not an app, not the keyboard, not one of our overlays) tall enough to hold the keyguard, the PIN pad or the open panel; asked BEFORE getRoot. Main thread. */
    private boolean tallSystemWindow(AccessibilityWindowInfo w) {
        int t = w.getType();
        if (!EventNoise.isSystemWindow(t) || t == EventNoise.WINDOW_TYPE_ACCESSIBILITY_OVERLAY) return false;
        w.getBoundsInScreen(winBounds);
        return EventNoise.tallEnoughForPanel(winBounds.height(), getResources().getDisplayMetrics().heightPixels);
    }

    /** BY1c F1: a LEVEL look at the panel's pane title (one id search in SystemUI's windows), for a return path that may have missed an event (the service was re-bound, a dump). */
    private int shadeLevel() {
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows == null) return LockShade.UNKNOWN;
            loadShadeTitles();
            for (AccessibilityWindowInfo w : windows) {
                if (!tallSystemWindow(w)) continue;
                AccessibilityNodeInfo root = w.getRoot();
                if (root == null) continue;
                try {
                    CharSequence pkg = root.getPackageName();
                    if (pkg == null || !"com.android.systemui".contentEquals(pkg)) continue;
                    List<AccessibilityNodeInfo> hit = root.findAccessibilityNodeInfosByViewId("com.android.systemui:id/notification_panel");
                    if (hit != null) for (AccessibilityNodeInfo n : hit) { int k = LockShade.kind(true, LockShade.PANE_TITLE, n.getPaneTitle(), shadeClosedTitle, shadeOpenTitles); if (k != LockShade.UNKNOWN) return k; }
                } finally { root.recycle(); }
            }
        } catch (Throwable t) { Log.e(TAG, "shade look failed", t); }
        return LockShade.UNKNOWN;
    }

    /** BY1c F1: the panel is now open (the lock patch goes away in the next frame and stays away, no timer) or closed (it may rest again LockShade.SETTLE_MS later). The home flavour has its own rule (homeForeignWindow). */
    private void noteShade(int kind, String how) {
        if (kind == LockShade.UNKNOWN) return;
        boolean open = kind == LockShade.OPEN;
        long now = android.os.SystemClock.uptimeMillis();
        if (open && !keyguardLocked()) return;   // BY1 review LOW3: a panel opened while UNLOCKED (Quickstep, the custom launcher) is not a lock-screen panel; its "open" must not leak into the next lock
        if (open) lastShadeSeenMs = now;          // BY1 review LOW5: a calibration screenshot around an open panel is refused
        if (open == lockShadeOpen) return;
        lockShadeOpen = open;
        if (!open) lockShadeClosedMs = now;
        Log.i(TAG, "PATCH shade " + (open ? "open" : "closed") + " (" + how + ") t=" + now);
        if (patchView == null || !patchLockMode || patchHomeMode || overlayShown) return;   // an open menu follows its own close; its patchShow(false) asks the books again
        if (open) patchView.setAlpha(0f);   // gone in the next frame, before anything else
        patchShow(false);                   // re-derives the rest from the books
    }

    /**
     * BC5 (Tj, 2026-11-07, twice: "For the pull up for the pin, the patch should disappear immediately, as fast as possible" / "the power menu doesn't need to be opened. Any time I pull up to type the pin number
     * the wallpaper patch should disappear immediately"): the PIN pad came up. A patch that is visible - open (the stock menu is up) or RESTING (invisible over the lock screen, but a window) - goes at once: alpha 0 in the
     * next frame, untouchable so it can never swallow a PIN key. Over an open menu it stays away until that menu is gone; a resting patch stays away for 2.5 s, then the PIN-pad scan (lastBouncer) decides.
     * Called from the fastest sign (an event of the keyguard's own PIN / password / pattern view, no scan) and from the slower PIN-pad scan.
     */
    private void pinPadCameUp(String why) {
        try {
            if (patchView == null || !patchLockMode) return;
            // BI1 (audit + review 2026-11-08): while the screen is really UNLOCKED the home patch ignores keyguard-class events (a screen-unpinning bouncer, the keyguard's teardown): hiding it over an open menu would show the
            // stock Screenshot entry. A LOCKED screen whose window is still the home flavour (the layer refresh follows the wake by about 400 ms) is hidden as before: a stale copy of the home screen must never stay over the lock screen.
            if (patchHomeMode && !keyguardLockedFast()) return;
            long now = android.os.SystemClock.uptimeMillis();
            lastPadSignMs = now;
            pinHiddenUntilMs = Math.max(pinHiddenUntilMs, now + 2500);   // BE1: every sign of the pad pushes the return out (an away patch must not come back over the pad)
            if (pinHide || (!overlayShown && !patchResting)) { Log.i(TAG, "PATCH pin sign (" + why + ") t=" + now + ": the patch is already away"); handler.removeCallbacks(padBack); handler.postDelayed(padBack, 2600); return; }   // nothing visible to hide (already away for the pad)
            Log.i(TAG, "PATCH pin hide (" + why + ") t=" + now + (overlayShown ? " menu open" : " resting") + (lastTouchDownMs > 0 && now - lastTouchDownMs < 3000 ? " " + (now - lastTouchDownMs) + " ms after the finger went down" : ""));
            logPatch("PIN pad sign (" + why + ")" + (lastTouchDownMs > 0 && now - lastTouchDownMs < 3000 ? ", " + (now - lastTouchDownMs) + " ms after the finger went down" : ", no finger-down seen first") + (overlayShown ? ", menu open" : ", resting patch hidden"));
            patchView.setAlpha(0f);                                   // gone in the next frame, before anything else
            if (overlayShown) {
                pinHide = true; closing = false; handler.removeCallbacks(closeFinishRun);
                patchShow(true);                                      // keeps it gone (pinHide) and leaves the rest of the state as it is
                applyPatchTouchable(false);                           // a hidden but touchable window would swallow the PIN keys under it
            } else patchShow(false);                                  // the resting patch: not visible for the next 2.5 s (pinHiddenUntilMs)
            handler.removeCallbacks(padBack); handler.postDelayed(padBack, 2600);   // BE1: ONE return runnable that looks at the pad again before it decides
        } catch (Throwable t) { Log.e(TAG, "pin hide failed", t); }
    }

    /**
     * BE1: a finger went down somewhere on the lock screen while the patch RESTS (menu closed): the patch is gone in the next frame, before a pull-up for the PIN can darken the wallpaper under it. It returns
     * {@link #TOUCH_AWAY_MS} later unless the PIN pad's first sign (pinPadCameUp) pushes the return out. While the stock menu is open this does nothing (the close / PIN cases follow the stock fade-out).
     */
    private void onOutsideTouch() {
        try {
            if (patchView == null || !patchLockMode || overlayShown || pinHide) return;
            if (!patchResting && patchHomeMode) return;      // (the home flavour has its own finger rule: homeTouchDown)
            long now = android.os.SystemClock.uptimeMillis();
            lastTouchDownMs = now;
            final long away = patchHomeMode ? TOUCH_AWAY_MS : LOCK_TOUCH_AWAY_MS;   // BN3: the lock screen holds the patch away longer (see LOCK_TOUCH_AWAY_MS)
            touchAwayUntilMs = now + away;                   // BT1a: recorded EVEN when the patch is not resting yet: a patch that becomes restable during the pull-up must find the finger's DOWN in its books
            final boolean was = patchResting;
            patchView.setAlpha(0f);                          // first: gone in the next frame (patchResting is left alone: the patch is still restable, so the stock dialog's own event brings it back at once, and the PIN-pad logic keeps its log lines)
            handler.removeCallbacks(touchBack); handler.postDelayed(touchBack, away + 20);
            if (now - lastTouchLogMs > 3000) { lastTouchLogMs = now; Log.i(TAG, "PATCH touch hide (a finger went down) t=" + now); logPatch(was ? "resting patch hidden at the finger's DOWN" : "finger DOWN while the patch was not resting (restable " + patchRestable + (restableSinceMs > 0 ? ", for " + (now - restableSinceMs) + " ms" : "") + "), kept away for " + away + " ms"); }
        } catch (Throwable t) { Log.e(TAG, "touch hide failed", t); }
    }
    private long lastTouchLogMs = 0;

    /** A keyguard PIN-pad view announced itself less than 1.5 s ago: the pad is (about to be) up even if the scan has not confirmed it yet. */
    private boolean padSignFresh() { return lastPadSignMs > 0 && android.os.SystemClock.uptimeMillis() - lastPadSignMs < 1500; }

    private boolean isScreenOn() {
        try { android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE); return pm != null && pm.isInteractive(); } catch (Throwable t) { return true; }
    }

    /** BE1: a diagnostic line in the persistent lock log, written OFF the main thread (the patch paths must not wait for a file). */
    private void logPatch(final String what) {
        // BI1 (audit 2026-11-08): the lock log is a 48 KB ring of the PAD / UNLOCK / HANDSHAKE history that unlock debugging needs; the home flavour (patchLockMode is true there too) would write "window created" and
        // a touch line every 3 s of use into it. Only the rare home lines stay: the calibration result and a menu opened while the patch was away.
        if (patchHomeMode && !what.startsWith("home calibration") && !what.startsWith("menu opened")) return;
        final android.content.Context app = getApplicationContext();
        LockLog.addAsync(app, "PATCH", what);
    }

    private void fastMenuClose(AccessibilityEvent event) {
        if (!patchLockMode || patchView == null) return;
        int type = event.getEventType();
        long awayNow = android.os.SystemClock.uptimeMillis();
        if ((overlayShown || patchResting || lastBouncer || touchAwayUntilMs > awayNow || pinHiddenUntilMs > awayNow) && (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) {   // BC5: the keyguard's own PIN pad view announcing itself (BE1: also while the patch is already away: the sign pushes its return out)
            CharSequence cn = event.getClassName(), pk = event.getPackageName();
            if (cn != null && pk != null && "com.android.systemui".contentEquals(pk)) {
                String c = cn.toString();
                if (c.startsWith("com.android.keyguard.") && (c.contains("PINView") || c.contains("PasswordView") || c.contains("PatternView") || c.contains("SecurityContainer") || c.contains("HostView"))) { pinPadCameUp("event " + c.substring(c.lastIndexOf('.') + 1)); return; }
            }
        }
        if (!overlayShown) return;   // a click in the stock dialog / the removal of its window only matter while the menu is open
        if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            CharSequence pk = event.getPackageName();
            int wid = dialogWindowId;
            if (wid != -1 && pk != null && "com.android.systemui".contentEquals(pk) && event.getWindowId() == wid) startClose(event.getEventTime(), "a click in the stock dialog");
        } else if (type == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            int wid = dialogWindowId;
            if (wid != -1 && (event.getWindowChanges() & AccessibilityEvent.WINDOWS_CHANGE_REMOVED) != 0 && event.getWindowId() == wid) dialogGone();
        }
    }

    /** AZ1: how far the stock scrim will be when the frame drawn now is SEEN: 0..1 (1 = complete, also when no fade is running). Main thread (onDraw) only. */
    private float fadeProgress() {
        if (closing) return PatchFade.closeLook(android.os.SystemClock.uptimeMillis(), closeStartMs, fadeLeadMs);   // BC2: the open look runs back to the resting look along the stock fade-OUT
        if (!fading) return 1f;
        long now = android.os.SystemClock.uptimeMillis();
        float p = PatchFade.progressAt(now, fadeStartMs, fadeLeadMs);
        // BJ1: per-frame statistics of this fade (one call per drawn frame; a few additions)
        if (fadeFrames == 0) fadeFirstDrawLateMs = Math.max(0, now - fadeStartMs);
        else { long gap = now - fadeFrameLastMs; if (gap > fadeWorstGapMs) fadeWorstGapMs = gap; if (gap > 34) fadeSlowFrames++; }
        fadeFrameLastMs = now; fadeFrames++;
        if (!PatchFade.fading(now, fadeStartMs, fadeLeadMs)) {   // this frame is the last one: the final brightness
            fading = false;
            fadeEndMs = now;
            handler.post(fadeSummaryRun);   // review 2026-11-08: the summary (a settings read, a string, a log line) is made AFTER this frame has been handed to the renderer, never inside it: the lock screen's last frame must not wait for it
        }
        return p;
    }

    private long fadeEndMs = 0;
    private final Runnable fadeSummaryRun = new Runnable() { @Override public void run() {
        try {
            String scale = animScaleText();
            fadeSummary = (fadeHomeFlavour ? "home" : "lock") + ": the first frame was drawn " + fadeFirstDrawLateMs + " ms after the stock scrim began, " + fadeFrames + " frames, " + fadeSlowFrames + " later than 34 ms after the one before, worst gap " + fadeWorstGapMs + " ms, animator scale " + scale;
            Log.i(TAG, "PATCH fade end " + (fadeHomeFlavour ? "home" : "lock") + " first=" + fadeFirstDrawLateMs + " ms frames=" + fadeFrames + " slow=" + fadeSlowFrames + " worst=" + fadeWorstGapMs + " ms scale=" + scale + " t=" + fadeEndMs);
            if (fadeFirstSummary.isEmpty()) fadeFirstSummary = fadeSummary;   // BK1a: the FIRST fade of this process is kept apart (the report shows whether it still stalls; the later ones overwrite fadeSummary)
        } catch (Throwable t) { Log.e(TAG, "fade summary failed", t); }
    } };

    /** BJ1: the system's animator duration scale as it is now. The stock fade runs 300 ms x this; at 0 ("Remove animations") the stock scrim appears in ONE frame, which a 300 ms patch fade would not match. For the report and the log only. */
    private String animScaleText() {
        try { return String.valueOf(android.provider.Settings.Global.getFloat(getContentResolver(), android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)); }
        catch (Throwable t) { return "?"; }
    }

    private void patchShow(boolean on) {
        if (!on && patchOpenFlag) Log.i(TAG, "PATCH menu closed t=" + android.os.SystemClock.uptimeMillis());
        if (!on) homeOpenStale = false;   // BT1c: the stale patch adopted for the last opening is over with it
        boolean opening = on && !patchOpenFlag;   // AZ1: only the closed -> open transition starts a fade (repeated "still open" calls must not restart it); decided even while the patch window does not exist yet
        patchOpenFlag = on;
        if (opening || !on) pinHide = false;   // BC5: a new menu, or no menu: the PIN-pad hide is over
        if (opening) startFade(); else if (!on) { fading = false; if (closing) { closing = false; handler.removeCallbacks(closeFinishRun); } }   // BC2: the patch goes back to rest: the fade-out (if any) is over
        if (patchView == null) return;
        if (!patchLockMode) { boolean ch = patchOpenHome != on; patchOpenHome = on; if (ch || opening) patchView.invalidate(); return; }   // AH1: the home patch dims like the stock dialog's scrim while the menu is open
        // AE3: "on" = our cover is up for an open menu (the full, dimmed patch); otherwise the patch RESTS visible over the Screenshot slot if the switch is on and the PIN pad is not up, else it stays invisible
        boolean vis;
        if (on) { if (patchHomeMode && opening) homeOpenShown = patchRestable; vis = (!patchHomeMode || homeOpenShown) && !pinHide; }   // BI1: the home patch shows for an open menu only if it was fresh when the menu opened (decided once: a draw during the menu must not take it away); otherwise the card covers the buttons and the stale patch stays hidden   // BC5: an open menu gets its patch unless the PIN pad came up over it   // review of v0.8.1: an open menu always gets its patch (PatchRest.visible says so); lockPatchAlways() asks the keyguard over binder, which has no place on the path that must be fastest
        else if (patchHomeMode) {   // BI1: the home patch rests only while its pixels are fresh, no finger went down for TOUCH_AWAY_MS and the Launcher has not drawn for DRAW_QUIET_MS
            patchRestable = homeRestable();
            vis = homeVisibleNow();
            if (patchRestable && !vis) { handler.removeCallbacks(homeShowCheck); handler.postDelayed(homeShowCheck, HomePatchRules.waitMs(android.os.SystemClock.uptimeMillis(), homeLastTouchMs(), TjHooks.lastHomeDrawMs) + 30); }   // come back when the timers allow
        } else {
            boolean can = PatchRest.visible(false, lockPatchAlways(), true, lastBouncer);
            patchRestable = can;                                                                  // BE1: the fast open path asks THIS, not "is it visible right now"
            long nowMs = android.os.SystemClock.uptimeMillis();
            if (can) { if (restableSinceMs == 0) restableSinceMs = nowMs; } else if (!lastBouncer) restableSinceMs = 0;   // BT1a: since when it has been restable (a PIN pad does not reset it: its own return rule, padBack, waits already)
            boolean settled = PatchRest.settled(nowMs, restableSinceMs);
            long shadeUntil = LockShade.holdUntil(lockShadeOpen, lockShadeClosedMs);              // BY1c F1: away while the notification panel is open, back LockShade.SETTLE_MS after it closed
            vis = can && nowMs >= pinHiddenUntilMs && nowMs >= touchAwayUntilMs && settled && nowMs >= shadeUntil;       // BE1: away for a PIN pad or a finger = not visible, but still restable; BT1a: and not for the first PatchRest.SETTLE_MS after it became restable
            if (can && !vis && !lockShadeOpen) { handler.removeCallbacks(settleRun); handler.postDelayed(settleRun, PatchRest.returnInMs(nowMs, restableSinceMs, pinHiddenUntilMs, Math.max(touchAwayUntilMs, shadeUntil)) + 20); }   // review B #1: restable but not visible = look again when the LONGEST of settle / PIN-pad hold / finger hold is over (never a timer that can run out of order and leave the patch away)
            else if (vis || lockShadeOpen) handler.removeCallbacks(settleRun);   // BY1c F1: no timer while the panel is open: its close event brings the patch back
        }
        boolean rest = vis && !on;
        boolean changed = rest != patchResting;
        patchResting = rest;
        final boolean wasHidden = patchView.getAlpha() < 0.5f;   // BN3: a return (invisible -> visible) is written down (noteReturn)
        patchView.setAlpha(vis ? 1f : 0f);
        if (rest && wasHidden) noteReturn();
        if (!on) setPatchTouchable(false);   // review of v0.8.0: whenever our menu is NOT open the patch can never swallow a touch, whichever path closed the menu (no disk write and no extra binder call in this time-critical path any more)
        if (changed || opening) { patchView.invalidate(); if (changed) Log.i(TAG, "PATCH rest " + (rest ? "on" : "off") + (lastBouncer ? " (PIN pad up)" : "") + " top=" + patchRestTop); }
    }

    /**
     * V1 (Tj): our menu is "the stock menu minus the Screenshot button": same panel, Power off + Restart only. The wallpaper patch underneath fills what the rest of the stock menu
     * (Screenshot, Emergency ...) would have covered. Without a patch below we keep the whole rectangle so a real Screenshot button can never show.
     * AC6 (Tj, 2026-11-05: "the buttons and card are too small"): the card is the PANEL minus one entry pitch ({@link PowerMenuDump#cardFromPanel}); it used to start at the top of the first entry and be
     * twice the height of ONE clickable node, which ignores the panel's padding and the gaps between entries (about a quarter shorter than the stock two-entry area).
     */
    private Rect cardArea(Rect full) {
        if (patchView == null) return full;
        if (patchHomeMode && !patchRestable) return full;   // BT1c: the home patch is not going to cover the Screenshot slot for this menu (not fresh and not adopted, see homeOpenDecide): the card covers the WHOLE panel
        return cardAreaGeom(full);
    }

    /** AH3: the card geometry WITHOUT the "is a patch below" guard of {@link #cardArea}: the patch window itself is derived from it, before any patch exists. */
    private Rect cardAreaGeom(Rect full) {
        try {
            String v = Features.getString(this, itemKey(), "");
            int sl = v.indexOf('/'), pitch = -1, cut = -1;
            if (sl > 0) { pitch = Integer.parseInt(v.substring(0, sl).trim()); cut = Integer.parseInt(v.substring(sl + 1).trim()); }
            int[] c = PowerMenuDump.cardFromPanel(full.left, full.top, full.right, full.bottom, pitch, cut);
            if (c[3] - c[1] < full.height() / 2) return full;                                  // nonsense geometry: keep the whole rectangle
            return new Rect(c[0], c[1], c[2], c[3]);
        } catch (NumberFormatException e) { return full; }
    }

    /**
     * AM1 (Tj, 2026-11-06: "the power menu doesn't need to be rebuilt or changed at all ... it can stay completely stock"): CARDLESS. The stock Power off / Restart entries stay untouched. The patch over the
     * Screenshot spot, resting there since the lock screen came up, only switches to its open-menu brightness and swallows a tap on the hidden Screenshot spot (a tap there closes the menu, like a tap outside).
     * The "CARD area" log line is kept: the emulator sections parse it for the geometry.
     */
    private void showStock(Rect full) {
        ensurePatch();
        final boolean first = !overlayShown;
        if (first) { touchAwayUntilMs = 0; if (!padSignFresh()) pinHiddenUntilMs = 0; }   // BE1: a menu that opens through the scan path also ends every away guard (the PIN pad, if it comes up over the menu, sets its own latch)
        overlayShown = true;
        patchShow(true);   // AZ1: the patch is the time-critical part: it is drawn at the next frame, so nothing slower (a log line, a preferences write) may come before it in this message
        if (first) {
            Log.i(TAG, "CARD area=" + cardAreaGeom(full).toShortString() + " full=" + full.toShortString() + " lockPatch=" + patchLockMode + " stock=true");
            cardShownMs = android.os.SystemClock.uptimeMillis(); recordLatency();
        }
        setPatchTouchable(true);
    }

    private WindowManager.LayoutParams patchParams(boolean touchable) {
        WindowManager.LayoutParams lp = overlayParams(patchAt, touchable);
        if (patchLockMode) lp.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;   // BE1: kept when the window flags are rewritten (touchable on / off)
        lp.format = patchLockMode ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
        lp.setTitle("TjWallpaperPatch");
        return lp;
    }

    /**
     * AZ1 (run 247, emulator with animations on): changing a window flag (here: touchable on/off) makes the window RELAYOUT, a synchronous binder call into system_server made inside the next traversal, i.e. in the same
     * frame as the patch's next draw. While the stock dialog animates it blocked the main thread for 100-200 ms: the patch's first dimmed frame was shown 60-400 ms late, events piled up behind it (age 180 ms),
     * and the same call at the close delayed the un-dim. The flag is only needed for a tap on the hidden Screenshot spot, so it follows the draw: ON after the stock fade is over, OFF 40 ms after the patch went back to rest.
     */
    private boolean touchableOnPending = false;
    private final Runnable touchableOn = new Runnable() { @Override public void run() { touchableOnPending = false; if (overlayShown && !pinHide) applyPatchTouchable(true); } };
    private final Runnable touchableOff = new Runnable() { @Override public void run() { if (!overlayShown) applyPatchTouchable(false); } };
    private void setPatchTouchable(boolean t) {
        if (patchView == null || !patchLockMode) return;   // the home patch never swallows a touch
        if (t && pinHide) return;                           // BC5: hidden for the PIN pad: never touchable
        if (t) {
            handler.removeCallbacks(touchableOff);
            if (patchTouchable || touchableOnPending) return;   // review of v0.8.1: a repeated call (every scan of the open menu) must not restart the wait
            touchableOnPending = true;
            handler.postDelayed(touchableOn, PatchFade.SETTLE_MS + 20);
        } else {
            handler.removeCallbacks(touchableOn); touchableOnPending = false;
            if (!patchTouchable) return;
            handler.removeCallbacks(touchableOff);
            handler.postDelayed(touchableOff, 40);
        }
    }
    private void applyPatchTouchable(boolean t) {
        if (patchView == null || t == patchTouchable || !patchLockMode) return;
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(patchView, patchParams(t)); patchTouchable = t; } catch (Throwable ignored) { }
    }

    // ---------------------------------------------------------------- AM1: the patch from a REAL screenshot of the lock screen (see PatchShot)
    private void loadStoredShot() {
        patchShotBmp = null;
        try {
            PatchShot.Loaded l = PatchShot.load(this, rotation(), shotSig());
            if (l != null) { patchShotBmp = l.bmp; patchShotLeft = l.left; patchShotTop = l.top; }
        } catch (Throwable t) { Log.e(TAG, "stored patch shot failed", t); }
        reloadScrim();   // BA1: the scrim measured for this very lock screen (null = none yet)
    }

    /** What a stored crop is valid for: this wallpaper picture, this rotation, this display size. */
    private String shotSig() { return PatchShot.currentSig(this); }

    private void shotStatus(String s) { try { Features.setString(this, PatchShot.STATUS_KEY, s); } catch (Throwable ignored) { } }

    private long calibrationUntilMs() { try { return Long.parseLong(Features.getString(this, PatchShot.CAL_UNTIL_KEY, "0").trim()); } catch (NumberFormatException e) { return 0; } }
    /** AW5: when the current calibration window opened (an automatic one is short, a manual one lasts {@link PatchShot#WINDOW_MS}); older windows recorded no start, so the old length is the fallback. */
    private long calibrationStartMs() {
        try { long st = Long.parseLong(Features.getString(this, AutoCalibration.START_KEY, "0").trim()); if (st > 0 && st <= calibrationUntilMs()) return st; } catch (NumberFormatException ignored) { }
        return calibrationUntilMs() - PatchShot.WINDOW_MS;
    }

    // ---------------------------------------------------------------- AW5: the helper calibrates the patch BY ITSELF (Tj chose A, 2026-11-06 evening) - see AutoCalibration
    private long autoCalLastMs = 0, lastPadSeenMs = 0, lastUnlockUptime = 0, autoShotAsked = 0;
    private int autoCalThisLock = 0;
    private String lastAutoNote = "";
    private final Runnable autoCalTrigger = new Runnable() { @Override public void run() { try { maybeAutoCalibrate("timer"); } catch (Throwable t) { Log.e(TAG, "auto calibration trigger failed", t); } } };
    private void autoCalNote(String note) {
        if (note == null || note.equals(lastAutoNote)) return;
        lastAutoNote = note;
        try { Features.setStringAsync(this, "patch_auto_note", now() + "  " + note); } catch (Throwable ignored) { }
    }

    /**
     * AW5: on a plain lock screen without a valid crop, open a short calibration window by ourselves; the existing machinery ({@link #calibrationChanged}: hide the resting patch, ask the system for a
     * screenshot when the lock screen is plain, {@link #ingestShot}) does the rest. The pure policy is {@link AutoCalibration#decision}; every "not yet" reason is shown in Launcher control.
     */
    private void maybeAutoCalibrate(String why) {
        android.content.Context app = getApplicationContext();
        AutoCalibration.State st = new AutoCalibration.State();
        st.switchOn = Features.get(app, AutoCalibration.KEY, true);
        st.coverOn = Features.isCover(Features.powerMode(app));
        st.lockPatchOn = Features.get(app, LOCK_PATCH_KEY, true);
        st.paused = CrashGuard.automationOff(app);
        st.permitted = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        st.geometryKnown = cachedCard() != null;
        st.patchWindow = patchView != null && patchLockMode;
        st.validCrop = patchShotBmp != null || PatchShot.has(app, rotation(), shotSig());
        st.calibrating = calibrating();
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        st.interactive = pm != null && pm.isInteractive();
        st.keyguardLocked = keyguardLocked();
        st.pinPadUp = lastBouncer;
        st.menuOpen = overlayShown;
        st.now = System.currentTimeMillis();
        st.lastStartMs = autoCalLastMs;
        st.startsThisLock = autoCalThisLock;
        String tries = Features.getString(app, AutoCalibration.TRIES_KEY, "");
        st.startsToday = AutoCalibration.startsWithinDay(tries, st.now);
        if (st.validCrop) { autoCalNote("not needed: the patch is calibrated for this lock screen"); maybeMeasureScrim("calibrated lock screen"); return; }   // BA1: the real scrim is measured once per lock screen, right after its picture exists
        String reason = AutoCalibration.decision(st);
        if (!reason.isEmpty()) {
            autoCalNote(reason);
            if (reason.startsWith("waiting") && st.switchOn && st.keyguardLocked) { handler.removeCallbacks(autoCalTrigger); handler.postDelayed(autoCalTrigger, 5000); }   // look again in a moment (the PIN pad goes, the screen wakes)
            return;
        }
        autoCalLastMs = st.now; autoCalThisLock++;
        Features.setString(app, AutoCalibration.TRIES_KEY, AutoCalibration.addTry(tries, st.now));
        Features.setString(app, AutoCalibration.START_KEY, String.valueOf(st.now));
        Features.setString(app, PatchShot.CAL_UNTIL_KEY, String.valueOf(st.now + AutoCalibration.WINDOW_MS));   // the preference listener runs calibrationChanged()
        shotStatus(now() + "  automatic calibration started (" + why + "): one screenshot of the plain lock screen");
        autoCalNote("started (" + why + "), try " + autoCalThisLock + " of " + AutoCalibration.MAX_PER_LOCK + " for this lock");
        Log.i(TAG, "PATCHSHOT automatic calibration started (" + why + ")");
    }

    // ================================================================================================================================================================================
    // BA1 (Tj, 2026-11-07: "the final wallpaper patch is very slightly too dark and I can see the square patch, but barely. Try to make the brightness of the square patch match the darkened lock screen after the side
    // key press 100% identical"): the helper MEASURES the stock menu's real scrim on this tablet (the maths: ScrimFit; the policy: ScrimMeasure) and the open patch is drawn with it (setOpenLook).
    // ================================================================================================================================================================================
    /** The scrim measured for the CURRENT lock screen (wallpaper / rotation / size / theme), null = none yet: the patch then uses the generic brightness (0.80 x Tj's nudges). Read by the draw. */
    private volatile ScrimFit.Transform scrimT;
    private float openLookQ = -1f, openLookNudge = -1f; private android.graphics.ColorFilter openLookFilter; private ScrimFit.Transform openLookT;
    private long screenOnEpoch = 0;   // when the screen last came on (wall clock): the lock screen settles before it is measured

    private boolean debuggable() { return (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0; }
    private boolean isNightNow() { return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES; }

    /** Is there a stored measurement valid for the lock screen as it is now? (binder calls; a rare path) */
    private boolean scrimStored() {
        try { return scrimLoad() != null; } catch (Throwable t) { return false; }
    }

    /** BQ2: the stored measurement that answers for the look the lock screen is in now (the dark look uses the light measurement when it has none of its own), or null. Binder calls: not per frame. */
    private ScrimFit.Transform scrimLoad() {
        int rot = rotation(); String sig = shotSig();
        for (boolean n : ScrimMeasure.slotOrder(isNightNow())) {
            ScrimFit.Transform t = ScrimFit.decode(Features.getString(this, ScrimMeasure.valueKey(rot, n), ""), sig, n);
            if (t != null) return t;
        }
        return null;
    }

    /** Loads the measurement stored for the lock screen as it is now (binder calls: not per frame). */
    private void reloadScrim() {
        try { scrimT = scrimLoad(); }   // BB2: one slot per theme; BQ2: the dark look uses the light measurement when it has none
        catch (Throwable t) { scrimT = null; Log.e(TAG, "scrim reload failed", t); }
    }

    /**
     * The look of the patch while the stock menu is open, {@code progress} 0..1 along the stock fade (0 = the screen as it was, 1 = the scrim complete). With a measurement: per channel {@code out = s * in + o},
     * interpolated from the identity exactly as the stock scrim ((1 - a) * L + a * C) grows with its alpha; Tj's two brightness nudges scale the measured s (0 % = exactly what was measured). Without one: the
     * generic brightness of v0.8.1 (a black scrim of 0.80).
     */
    private void setOpenLook(android.graphics.Paint p, float progress) {
        ScrimFit.Transform t = scrimT;
        // the Control switch is also the way back: OFF = the generic brightness again, even when a measurement is stored (it stays stored and applies again when the switch is turned back on)
        if (t == null || !Features.get(this, ScrimMeasure.KEY, ScrimMeasure.defaultOn(debuggable()))) { setDim(p, PatchFade.mix(restDim(true), lockPatchDim(true), progress)); return; }
        float nudge = (1f + Features.getInt(this, PATCH_SCRIM_KEY, 0) / 100f) * (1f + Features.getInt(this, PATCH_DIM_KEY, 0) / 100f);
        float q = Math.round(Math.max(0f, Math.min(1f, progress)) * 255f) / 255f;
        if (openLookFilter == null || q != openLookQ || t != openLookT || nudge != openLookNudge) {   // one filter per step of the fade, not one per draw
            float[] m = new float[20];
            for (int c = 0; c < 3; c++) {
                float sEnd = Math.max(0.3f, Math.min(1f, t.s[c] * nudge));
                m[c * 6] = 1f + (sEnd - 1f) * q;      // the diagonal: 0, 6, 12
                m[c * 5 + 4] = t.o[c] * q;            // the translate column: 4, 9, 14 (levels, 0..255)
            }
            m[18] = 1f;                                // alpha stays
            openLookFilter = new android.graphics.ColorMatrixColorFilter(m); openLookQ = q; openLookT = t; openLookNudge = nudge;
        }
        p.setColorFilter(openLookFilter);
    }

    // ---------------------------------------------------------------- the measurement stage: bare frame -> the stock menu opened by the helper -> second frame -> menu closed
    private boolean scrimNight = false;   // BB2: the theme the running stage started in (its frames and its stored value belong to that theme)
    private int scrimStage = 0;     // 0 idle, 1 waiting for the bare frame, 2 the gap after it, 3 the menu is opening, 4 waiting for the menu frame, 5 closing the menu
    private long scrimBeganAt = 0, scrimStageAt = 0, scrimLastStartMs = 0, scrimAskedAt = 0, scrimMenuAt = 0, scrimPollAt = 0;
    private volatile long scrimLastShotId = -1;   // shared with the worker thread (review nit)
    private int scrimThisLock = 0, scrimCloseTries = 0, scrimGoneTicks = 0;
    private boolean scrimOpenedMenu = false;
    private volatile boolean scrimIngesting = false;   // shared with the worker thread (review nit)
    private int[] scrimPoints, scrimRef;   // the sample points of the bare frame ((y << 16) | x) and its pixels there
    private String lastScrimNote = "", scrimOutcome = "";

    private void scrimNote(String note) {
        if (note == null || note.equals(lastScrimNote)) return;
        lastScrimNote = note;
        try { Features.setStringAsync(this, ScrimMeasure.NOTE_KEY, now() + "  " + note); } catch (Throwable ignored) { }
    }

    private final Runnable scrimTrigger = new Runnable() { @Override public void run() { try { maybeMeasureScrim("timer"); } catch (Throwable t) { Log.e(TAG, "scrim trigger failed", t); } } };
    private final Runnable scrimTick = new Runnable() { @Override public void run() { try { scrimStep(); } catch (Throwable t) { Log.e(TAG, "scrim step failed", t); try { scrimFinish("an error: " + t); } catch (Throwable ignored) { } } } };

    private ScrimMeasure.State scrimState() {
        android.content.Context app = getApplicationContext();
        ScrimMeasure.State st = new ScrimMeasure.State();
        st.switchOn = Features.get(app, ScrimMeasure.KEY, ScrimMeasure.defaultOn(debuggable()));
        st.coverOn = Features.isCover(Features.powerMode(app));
        st.lockPatchOn = Features.get(app, LOCK_PATCH_KEY, true);
        st.paused = CrashGuard.automationOff(app);
        st.permitted = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        st.geometryKnown = cachedCard() != null;
        st.validCrop = patchShotBmp != null || PatchShot.has(app, rotation(), shotSig());   // the same test as the automatic calibration: the picture exists, whether or not the patch window has loaded it yet
        st.haveMeasurement = scrimT != null || scrimStored();                                // the STORED value counts too: right after a restart the memory copy is only loaded when the patch window is built
        st.calibrating = calibrating();
        st.stageRunning = scrimStage != 0;
        st.night = isNightNow();   // BQ2
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        st.interactive = pm != null && pm.isInteractive();
        st.keyguardLocked = keyguardLocked();
        st.pinPadUp = lastBouncer;
        st.menuOpen = overlayShown;
        st.handshakeArmed = unlock.armedMethod() != null;
        st.now = System.currentTimeMillis();
        st.screenOnMs = ScrimMeasure.settleFromMs(screenOnEpoch, st.now, android.os.SystemClock.elapsedRealtime(), connectedAtMs);   // BQ1a: a freshly (re)connected service has not seen the wake: it settles from the connect
        st.lastStartMs = scrimLastStartMs;
        st.startsThisLock = scrimThisLock;
        st.startsToday = AutoCalibration.startsWithinDay(Features.getString(app, ScrimMeasure.TRIES_KEY, ""), st.now);
        st.failures = ScrimMeasure.failures(Features.getString(app, ScrimMeasure.failKey(rotation(), isNightNow()), ""), shotSig(), isNightNow());   // review of BA1: a lock screen that cannot be fitted is not retried every day
        return st;
    }

    /** Starts the measurement when {@link ScrimMeasure#decision} says go; every "not yet" reason is shown in Launcher control, and a "waiting" one is looked at again in a moment. */
    private void maybeMeasureScrim(String why) {
        if (scrimStage != 0) return;
        android.content.Context app = getApplicationContext();
        ScrimMeasure.State st = scrimState();
        if (st.haveMeasurement) { scrimNote("measured for this lock screen: " + ScrimFit.describe(scrimT)); return; }
        String reason = ScrimMeasure.decision(st);
        if (!reason.isEmpty()) {
            scrimNote(reason);
            // look again in a moment, but ONLY while the screen is on: a locked tablet with its screen off must not wake the CPU every few seconds (the next wake / lock re-triggers this by itself)
            if (reason.startsWith("waiting") && st.switchOn && st.keyguardLocked && st.interactive) { handler.removeCallbacks(scrimTrigger); handler.postDelayed(scrimTrigger, 6000); }
            return;
        }
        scrimLastStartMs = st.now; scrimThisLock++;
        scrimNight = isNightNow();
        final String because = ScrimFit.reasonMissing(Features.getString(app, ScrimMeasure.valueKey(rotation(), scrimNight), ""), shotSig(), scrimNight);   // BB2: "why again?" answered on the tablet
        Features.setString(app, ScrimMeasure.WHY_KEY, new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date()) + " started because " + because);
        Features.setString(app, ScrimMeasure.TRIES_KEY, AutoCalibration.addTry(Features.getString(app, ScrimMeasure.TRIES_KEY, ""), st.now));
        // BQ1a: the run is COUNTED NOW, before the first screenshot is asked for (commit, like the line above): a service that is torn down or a process that dies in the middle never reaches the end of the run, and a
        // run that was never counted would start again at the next lock. A run that stores a measurement clears this record (scrimIngest).
        final String failKey = ScrimMeasure.failKey(rotation(), scrimNight);
        Features.setString(app, failKey, ScrimMeasure.addFailure(Features.getString(app, failKey, ""), shotSig(), scrimNight));
        Features.setStringAsync(app, ScrimMeasure.RUN_KEY, ScrimMeasure.runLine(new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()), scrimNight, "started (" + why + "), no end recorded yet"));   // a run that never finishes still shows in the report
        scrimBeganAt = android.os.SystemClock.uptimeMillis(); scrimStageAt = scrimBeganAt;
        scrimStage = 1; scrimOpenedMenu = false; scrimPoints = null; scrimRef = null; scrimCloseTries = 0; scrimMenuAt = 0; scrimPollAt = 0; scrimGoneTicks = 0; scrimOutcome = "";
        scrimNote("started (" + why + "): a bare frame of the lock screen first");
        Log.i(TAG, "SCRIM measurement started (" + why + "): " + because);
        LockLog.add(app, "SCRIM", "run starts (" + why + "), " + (scrimNight ? "dark" : "light") + " theme, screen on " + (st.screenOnMs > 0 ? String.valueOf(Math.max(0, (st.now - st.screenOnMs) / 1000)) : "?") + " s: " + because);   // BQ1a: the 2026-11-10 report could not tell which visit took the screenshots
        scrimAskShot();
        handler.removeCallbacks(scrimTick); handler.postDelayed(scrimTick, 400);
    }

    private void scrimAskShot() {
        scrimAskedAt = System.currentTimeMillis();
        boolean ok = false;
        try { ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT); } catch (Throwable ignored) { }
        Log.i(TAG, "SCRIM asked for a screenshot (stage " + scrimStage + ", accepted=" + ok + ")");
        if (!ok) scrimFinish("Android refused to take a screenshot");
    }

    /** Why the sequence must stop NOW ("" = carry on). Anything but a plain lock screen ends it; the lock screen is never held up. */
    private String scrimInterrupted(long up) {
        if (up - scrimBeganAt > ScrimMeasure.STAGE_LIMIT_MS) return "it took too long";
        android.content.Context app = getApplicationContext();
        if (CrashGuard.automationOff(app)) return "automation was paused";
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null || !pm.isInteractive()) return "the screen went off";
        if (!keyguardLocked()) return "the tablet was unlocked";
        if (isNightNow() != scrimNight) return "the theme changed (battery saver / dark theme) during the measurement";   // BB2: the two frames must be taken in the same theme
        if (lastBouncer) return "the PIN pad came up";
        if (unlock.armedMethod() != null) return "an unlock gesture started";
        if (calibrating()) return "a calibration started";
        if (scrimStage < 3 && overlayShown) return "the power menu was opened by someone else";
        if ((scrimStage == 3 || scrimStage == 4) && scrimMenuAt > 0 && !overlayShown) { if (++scrimGoneTicks >= 3) return "the power menu was closed before the second frame"; }   // three ticks in a row (~0.9 s): the helper's own detection may blink during the menu's window burst
        else scrimGoneTicks = 0;
        return "";
    }

    private void scrimStep() {
        if (scrimStage == 0) return;
        long up = android.os.SystemClock.uptimeMillis();
        if (scrimStage != 5) {
            String stop = scrimInterrupted(up);
            if (!stop.isEmpty()) { scrimFinish("stopped: " + stop); return; }
        }
        switch (scrimStage) {
            case 1:   // the bare frame
                if (up - scrimStageAt > ScrimMeasure.SHOT_WAIT_MS) { scrimFinish("no screenshot appeared"); return; }
                if (up - scrimPollAt >= 700) { scrimPollAt = up; scrimIngest(false); }
                break;
            case 2:   // the gap: the first screenshot's own animation must be over
                if (up - scrimStageAt >= ScrimMeasure.SHOT_GAP_MS) { scrimOpenMenu(up); if (scrimStage == 0 || scrimStage == 5) return; }
                break;
            case 3:   // the menu opens (the helper's own detection), then its fade finishes
                if (overlayShown) {
                    if (scrimMenuAt == 0) scrimMenuAt = up;
                    if (up - scrimMenuAt >= ScrimMeasure.OPEN_SETTLE_MS) { scrimStage = 4; scrimStageAt = up; scrimPollAt = 0; scrimAskShot(); if (scrimStage == 5) return; }
                } else if (up - scrimStageAt > ScrimMeasure.MENU_WAIT_MS) { scrimFinish("the power menu did not open"); return; }
                break;
            case 4:   // the menu frame
                if (up - scrimStageAt > ScrimMeasure.SHOT_WAIT_MS) { scrimFinish("no screenshot appeared with the menu open"); return; }
                if (up - scrimPollAt >= 700) { scrimPollAt = up; scrimIngest(true); }
                break;
            case 5:   // closing the menu again (it is ours to close only if the helper opened it)
                scrimCloseStep();
                return;
        }
        handler.postDelayed(scrimTick, 300);
    }

    private void scrimOpenMenu(long up) {
        if (overlayShown) { scrimFinish("the power menu is already open"); return; }
        scrimStage = 3; scrimStageAt = up; scrimMenuAt = 0; scrimOpenedMenu = true;   // from here on the helper owns an open menu: every way out closes it
        boolean ok = false;
        try { ok = performGlobalAction(GLOBAL_ACTION_POWER_DIALOG); } catch (Throwable ignored) { }
        Log.i(TAG, "SCRIM opened the stock power menu (accepted=" + ok + ")");
        scrimNote("the stock menu is opening by itself (once, for about two seconds) to measure its dimming");
        if (!ok) scrimFinish("Android refused to open the power menu");
    }

    private void scrimFinish(String outcome) {
        scrimOutcome = outcome == null ? "" : outcome;
        scrimNote(scrimOutcome);
        try {   // BQ1a: the run was counted when it STARTED (maybeMeasureScrim); here it is only RECORDED, off the disk path (apply: the unlock path keeps no synchronous writes). The LAST outcome of a run wins: a late MEASURED overwrites an earlier "stopped".
            android.content.Context app = getApplicationContext();
            Features.setStringAsync(app, ScrimMeasure.RUN_KEY, ScrimMeasure.runLine(new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()), scrimNight, scrimOutcome));
            LockLog.add(app, "SCRIM", "run ends: " + scrimOutcome);
        } catch (Throwable ignored) { }
        Log.i(TAG, "SCRIM finished: " + scrimOutcome);
        scrimPoints = null; scrimRef = null;
        scrimStage = 5; scrimStageAt = android.os.SystemClock.uptimeMillis(); scrimCloseTries = 0;
        handler.removeCallbacks(scrimTick); handler.post(scrimTick);   // the closing step runs next
    }

    private void scrimCloseStep() {
        // Review of BA1 (finding 3): BACK goes ONLY to a stock menu that is verifiably still there AND over a lock screen that is still plain. After the person swiped up (PIN pad) or unlocked, a BACK would close
        // the PIN pad or reach the app that just came up (and the system dismisses the menu itself then); a menu that is already gone needs nothing.
        boolean present = overlayShown;
        if (!present) { try { present = dialogWindowPresent(getWindows()); } catch (Throwable ignored) { } }   // the window list is authoritative: the helper's own detection can lag or blink
        if (!scrimOpenedMenu || !present) { scrimStage = 0; scrimOpenedMenu = false; handler.removeCallbacks(scrimTick); try { layerRefresh.run(); } catch (Throwable ignored) { } return; }
        if (!keyguardLocked() || lastBouncer) { scrimNote(scrimOutcome + "; left the menu to the system (the lock screen is no longer plain)"); scrimStage = 0; scrimOpenedMenu = false; handler.removeCallbacks(scrimTick); return; }
        if (scrimCloseTries >= ScrimMeasure.CLOSE_TRIES) { scrimNote(scrimOutcome + "; the power menu would not close by itself: tap outside it"); scrimStage = 0; scrimOpenedMenu = false; return; }
        scrimCloseTries++;
        try { performGlobalAction(GLOBAL_ACTION_BACK); } catch (Throwable ignored) { }
        handler.postDelayed(scrimTick, ScrimMeasure.CLOSE_STEP_MS);
    }

    /** Review of BA1 (finding 7): the service is being interrupted / unbound / destroyed: stop the sequence and, if the helper opened the stock menu and it is still up over a plain lock screen, close it once. */
    private void scrimTeardown() {
        try {
            handler.removeCallbacks(scrimTick); handler.removeCallbacks(scrimTrigger);
            if (scrimStage >= 1 && scrimStage <= 4) {   // BQ1a: a run torn down in the middle (stage 5 = its end is already recorded; the run itself was counted when it started) says so in the report and the lock log
                try {
                    android.content.Context app = getApplicationContext();
                    Features.setStringAsync(app, ScrimMeasure.RUN_KEY, ScrimMeasure.runLine(new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US).format(new java.util.Date()), scrimNight, "stopped: the accessibility service was restarted or switched off"));
                    LockLog.add(app, "SCRIM", "run ends: stopped, the accessibility service was restarted or switched off");
                } catch (Throwable ignored) { }
            }
            if (scrimOpenedMenu && scrimStage != 0 && overlayShown && keyguardLocked() && !lastBouncer) { try { performGlobalAction(GLOBAL_ACTION_BACK); } catch (Throwable ignored) { } }
            scrimOpenedMenu = false; scrimStage = 0; scrimPoints = null; scrimRef = null;
        } catch (Throwable ignored) { }
    }

    /** One look for the screenshot (worker thread): the bare frame fixes the sample points, the menu frame is fitted against them. Nothing here may throw into the lock screen. */
    private void scrimIngest(final boolean open) {
        if (scrimIngesting) return;
        scrimIngesting = true;
        final android.content.Context app = getApplicationContext();
        final long asked = scrimAskedAt;
        final Rect panel = cachedCard(), patch = patchView != null && patchLockMode ? new Rect(patchAt) : null;
        final int rot = rotation(); final String sig = shotSig(); final boolean night = scrimNight;   // BB2: the theme the stage STARTED in (it stops if the theme changes meanwhile)
        final android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        final int shadow = Math.round(18 * dm.density);
        final int[] points = scrimPoints, ref = scrimRef;
        try { new Thread(new Runnable() { @Override public void run() {
            try {
                PatchShot.Shot row = PatchShot.newestShot(app, asked);
                if (row == null) return;   // not there yet: the next tick looks again
                long id = android.content.ContentUris.parseId(row.uri);
                if (id == scrimLastShotId) return;
                if (!AutoCalibration.ownerOk(row.owner) && !isSystemPackage(row.owner)) { postFinish("stopped: the screenshot was written by another app (" + row.owner + ")"); return; }
                android.graphics.Bitmap full = PatchShot.decode(app, row.uri);
                if (full == null) { Log.i(TAG, "SCRIM the screenshot is not readable yet (still being written?): the next poll looks again"); return; }   // the stage has its own time limit; one unreadable look must not waste the try
                scrimLastShotId = id;
                final int W = full.getWidth(), H = full.getHeight();
                if (W != dm.widthPixels || H != dm.heightPixels) { full.recycle(); postFinish("stopped: the screenshot is " + W + "x" + H + " but the screen is " + dm.widthPixels + "x" + dm.heightPixels); return; }
                int[] px = new int[W * H];
                full.getPixels(px, 0, W, 0, 0, W, H); full.recycle();
                PatchShot.deleteQuietly(app, row.uri);   // best effort: Android usually does not allow it without the storage WRITE permission
                if (!open) {
                    int[][] ex = ScrimMeasure.excludeRects(W, H, panel == null ? null : new int[] { panel.left, panel.top, panel.right, panel.bottom }, patch == null ? null : new int[] { patch.left, patch.top, patch.right, patch.bottom }, shadow);
                    final int[] pts = ScrimMeasure.pickPoints(px, W, H, ex);
                    if (pts.length < ScrimFit.MIN_SAMPLES) { postFinish("stopped: too little plain wallpaper on this lock screen to measure (" + pts.length + " usable points)"); return; }
                    final int[] rgb = new int[pts.length];
                    for (int k = 0; k < pts.length; k++) rgb[k] = px[(pts[k] >>> 16) * W + (pts[k] & 0xFFFF)];
                    handler.post(new Runnable() { @Override public void run() {
                        if (scrimStage != 1) return;
                        scrimPoints = pts; scrimRef = rgb; scrimStage = 2; scrimStageAt = android.os.SystemClock.uptimeMillis();
                        scrimNote("bare frame taken (" + pts.length + " sample points); the menu comes next");
                    } });
                } else {
                    if (points == null || ref == null) { postFinish("stopped: no bare frame to compare with"); return; }
                    int n = points.length;
                    int[][] x = new int[3][n], y = new int[3][n];   // x = the bare frame (packed RGB per point), y = the menu frame at the same points
                    int[] shifts = { 16, 8, 0 };
                    for (int c = 0; c < 3; c++) for (int k = 0; k < n; k++) { x[c][k] = (ref[k] >> shifts[c]) & 255; y[c][k] = (px[(points[k] >>> 16) * W + (points[k] & 0xFFFF)] >> shifts[c]) & 255; }
                    ScrimFit.Result r = ScrimFit.fitChannel(x[0], y[0], n), g = ScrimFit.fitChannel(x[1], y[1], n), b = ScrimFit.fitChannel(x[2], y[2], n);
                    String[] why = new String[1];
                    final ScrimFit.Transform t = ScrimFit.combine(r, g, b, why);
                    if (t == null) { postFinish("not measured: " + why[0]); return; }
                    Features.setString(app, ScrimMeasure.valueKey(rot, night), ScrimFit.encode(t, sig, night, System.currentTimeMillis()));
                    Features.setString(app, ScrimMeasure.failKey(rot, night), "");   // BQ1a: the run was counted when it started; it stored a measurement, so it is no failure
                    final String line = "MEASURED " + ScrimFit.describe(t);
                    handler.post(new Runnable() { @Override public void run() {
                        try { reloadScrim(); if (patchView != null) patchView.invalidate(); } catch (Throwable ignored) { }
                        scrimFinish(line);
                    } });
                }
            } catch (Throwable t) { Log.e(TAG, "scrim ingest failed", t); postFinish("stopped: " + t); }
            finally { scrimIngesting = false; }
        } }, "tj-scrim").start(); } catch (Throwable t) { scrimIngesting = false; Log.e(TAG, "scrim worker could not start", t); postFinish("stopped: " + t); }   // a thread that cannot be started (out of memory) ends the try, it never throws into the lock path
    }

    private void postFinish(final String outcome) { handler.post(new Runnable() { @Override public void run() { if (scrimStage != 0 && scrimStage != 5) scrimFinish(outcome); } }); }

    private final Runnable shotIngest = new Runnable() { @Override public void run() { try { ingestShot(); } catch (Throwable t) { Log.e(TAG, "patch shot ingest failed", t); } } };
    private final Runnable shotEnd = new Runnable() { @Override public void run() {
        try {
            if (calibrating()) return;
            if (Features.getString(TjAccessibilityService.this, PatchShot.CAL_UNTIL_KEY, "0").length() > 1) { shotStatus(now() + "  the calibration window ended without a usable screenshot"); Features.setString(TjAccessibilityService.this, PatchShot.CAL_UNTIL_KEY, "0"); }
            calibrationChanged();
        } catch (Throwable t) { Log.e(TAG, "calibration end failed", t); }
    } };
    private final Runnable autoShot = new Runnable() { @Override public void run() {
        try {
            if (!calibrating() || autoShotDone) return;
            if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 && Features.get(TjAccessibilityService.this, "patch_auto_shot_off", false)) return;   // debug builds only: the emulator takes its own screenshot (section pa, AU2)
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            boolean ready = pm != null && pm.isInteractive() && keyguardLockedFast() && !lastBouncer && !overlayShown && !lockShadeOpen && patchView != null;   // BY1 review LOW5: never with the notification panel open
            if (!ready) { if (++autoShotTries < 12) handler.postDelayed(this, 2000); return; }   // wait for the lock screen to be up, the menu closed, no PIN pad
            autoShotDone = true; autoShotAsked = android.os.SystemClock.uptimeMillis();
            Log.i(TAG, "PATCHSHOT asking the system for a screenshot of the lock screen");
            shotStatus(now() + "  taking the screenshot of the lock screen ...");
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        } catch (Throwable t) { Log.e(TAG, "auto screenshot failed", t); }
    } };

    /** The calibration window opened or closed (Launcher control, the end of the window, a service connect): hide / bring back the resting patch, watch the media store, schedule the screenshot. */
    private void calibrationChanged() {
        if (calibrating()) {
            startShotWatch();
            autoShotDone = false; autoShotTries = 0;
            handler.removeCallbacks(autoShot); handler.postDelayed(autoShot, 2500);
            handler.removeCallbacks(shotEnd); handler.postDelayed(shotEnd, Math.max(1000, calibrationUntilMs() - System.currentTimeMillis() + 500));
        } else { stopShotWatch(); handler.removeCallbacks(autoShot); handler.removeCallbacks(shotEnd); }
        layerRefresh.run();
        patchShow(overlayShown);   // the resting patch is hidden while calibrating (it must not be inside the screenshot) and comes back afterwards
    }

    private void startShotWatch() {
        if (shotObserver != null) return;
        shotObserver = new android.database.ContentObserver(handler) {
            @Override public void onChange(boolean selfChange) {
                if (homeCalRunning) { try { homeShotObserved(); } catch (Throwable t) { Log.e(TAG, "home shot observer failed", t); } return; }   // BI1: a home calibration is waiting for ITS screenshot (the Launcher is hidden until it exists)
                handler.removeCallbacks(shotIngest); handler.postDelayed(shotIngest, 900);
            }
        };
        try { getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, shotObserver); } catch (Throwable t) { Log.e(TAG, "media watch failed", t); shotObserver = null; }
    }

    private void stopShotWatch() {
        if (shotObserver == null) return;
        try { getContentResolver().unregisterContentObserver(shotObserver); } catch (Throwable ignored) { }
        shotObserver = null;
    }

    /** A screenshot appeared while calibrating: crop the patch window out of it (worker thread) and keep the crop. Every refusal says why in Launcher control. */
    private void ingestShot() {
        if (!calibrating()) return;
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) { shotStatus(now() + "  needs permission: Launcher control > Allow wallpaper access"); return; }
        final android.content.Context app = getApplicationContext();
        final long start = calibrationStartMs();
        final boolean locked = keyguardLockedFast();
        final long menuSeen = menuUpAtMs, padSeen = lastPadSeenMs, unlockAt = lastUnlockUptime;   // AU2: what was going on around the capture
        final boolean menuNow = overlayShown, padNow = lastBouncer;
        final long shadeSeen = lockShadeOpen ? android.os.SystemClock.uptimeMillis() : lastShadeSeenMs; final boolean shadeNow = lockShadeOpen;   // BY1 review LOW5
        final Rect win = patchView != null && patchLockMode ? new Rect(patchAt) : null;
        final int rot = rotation(); final String sig = shotSig();
        final android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(dm);
        new Thread(new Runnable() { @Override public void run() {
            try {
                final PatchShot.Shot shotRow = PatchShot.newestShot(app, start);
                if (shotRow == null) return;
                final android.net.Uri u = shotRow.uri;
                long id = android.content.ContentUris.parseId(u);
                if (id == lastShotId) return;
                lastShotId = id;
                if (!AutoCalibration.ownerOk(shotRow.owner) && !isSystemPackage(shotRow.owner)) { shotStatus(now() + "  ignored a screenshot written by another app (" + shotRow.owner + "): only screenshots made by the system itself count"); return; }   // AU2
                long shotUptime = android.os.SystemClock.uptimeMillis() - Math.max(0L, System.currentTimeMillis() - shotRow.addedSec * 1000L);
                String refused = AutoCalibration.refusal(shotUptime, menuSeen, menuNow, padSeen, padNow, unlockAt, shadeSeen, shadeNow);   // AU2: the power menu / the PIN pad around the capture, an unlock right after it
                if (!refused.isEmpty()) { shotStatus(now() + "  refused a screenshot: " + refused); autoCalNote("refused a screenshot (" + refused + ")"); return; }
                if (!locked) { shotStatus(now() + "  that screenshot was NOT taken on the lock screen: lock the tablet, wake it and take it there"); return; }
                if (win == null) { shotStatus(now() + "  the power menu rectangle is not learned yet: hold the side key once on the lock screen, then calibrate again"); return; }
                android.graphics.Bitmap full = PatchShot.decode(app, u);
                if (full == null) { shotStatus(now() + "  the screenshot could not be read"); return; }
                int W = full.getWidth(), H = full.getHeight();
                if (W != dm.widthPixels || H != dm.heightPixels) { shotStatus(now() + "  the screenshot is " + W + "x" + H + " but the screen is " + dm.widthPixels + "x" + dm.heightPixels + ": take it in the same orientation"); full.recycle(); return; }
                long sr = 0, sg = 0, sb = 0; int n = 0;
                for (int y = Math.max(0, win.top); y < Math.min(H, win.bottom); y += 3) for (int x = Math.max(0, win.left); x < Math.min(W, win.right); x += 3) { int q = full.getPixel(x, y); sr += (q >> 16) & 255; sg += (q >> 8) & 255; sb += q & 255; n++; }
                if (n > 0 && PatchShot.looksLikeMenu(sr / (double) n, sg / (double) n, sb / (double) n)) { String seen = " [slot mean " + Math.round(sr / (double) n) + "," + Math.round(sg / (double) n) + "," + Math.round(sb / (double) n) + " at " + win.toShortString() + "; 4x4 grid brightness " + gridBrightness(full) + "]"; Log.i(TAG, "PATCHSHOT refused as a menu screenshot" + seen); shotStatus(now() + "  the power menu was open in that screenshot: take it with the lock screen showing and the menu closed" + seen); full.recycle(); return; }
                int[] cr = PatchShot.cropRect(win.left, win.top, win.right, win.bottom, PatchShot.MARGIN_PX, W, H);
                android.graphics.Bitmap crop = android.graphics.Bitmap.createBitmap(full, cr[0], cr[1], cr[2] - cr[0], cr[3] - cr[1]);
                final boolean saved = PatchShot.save(app, rot, crop, sig, cr[0], cr[1]);
                final String dims = crop.getWidth() + "x" + crop.getHeight();
                full.recycle();
                final boolean deleted = saved && PatchShot.deleteQuietly(app, u);   // AW5: the gallery does not fill up where Android allows the removal (it usually does not without the storage WRITE permission)
                handler.post(new Runnable() { @Override public void run() { try {
                    if (!saved) { shotStatus(now() + "  the crop could not be stored"); return; }
                    Features.setString(TjAccessibilityService.this, PatchShot.CAL_UNTIL_KEY, "0");
                    removeOverlay(); removePatch(); refreshLayers();   // rebuild the patch with the stored real pixels
                    calibrationChanged();
                    shotStatus(now() + "  PATCH CALIBRATED (" + dims + "): the patch now shows your real lock screen at the Screenshot spot" + (deleted ? " (the screenshot was removed from the gallery again)" : " (the screenshot itself stays in the gallery)"));
                    autoCalNote("calibrated" + (deleted ? "" : "; the screenshot stays in the gallery"));
                    Log.i(TAG, "PATCHSHOT stored a " + dims + " crop for rotation " + rot);
                    handler.removeCallbacks(scrimTrigger); handler.postDelayed(scrimTrigger, 6000);   // BA1: the scrim of this lock screen is measured next (its own frames; the first flash has passed by then)
                } catch (Throwable t) { Log.e(TAG, "patch shot apply failed", t); } } });
            } catch (Throwable t) { Log.e(TAG, "patch shot ingest failed", t); shotStatus(now() + "  failed: " + t); }
        } }, "tj-patchshot").start();
    }

    /** Diagnostics for a refused calibration screenshot: the mean brightness (0-255) of a 4 x 4 grid of the picture, row by row, so a report shows WHAT was on the screen (a white panel, a PIN pad, the plain wallpaper). */
    private static String gridBrightness(android.graphics.Bitmap b) {
        StringBuilder sb = new StringBuilder();
        try {
            int W = b.getWidth(), H = b.getHeight();
            for (int gy = 0; gy < 4; gy++) for (int gx = 0; gx < 4; gx++) {
                long sum = 0; int n = 0;
                for (int y = gy * H / 4; y < (gy + 1) * H / 4; y += 16) for (int x = gx * W / 4; x < (gx + 1) * W / 4; x += 16) { int q = b.getPixel(x, y); sum += (((q >> 16) & 255) + ((q >> 8) & 255) + (q & 255)) / 3; n++; }
                sb.append(n == 0 ? 0 : sum / n).append(gx == 3 && gy < 3 ? "/" : " ");
            }
        } catch (Throwable ignored) { }
        return sb.toString().trim();
    }

    /** Review of v0.8.1: a vendor ROM (the tablet is a MediaTek one) may save its screenshots under another SYSTEM package than com.android.systemui; any package that ships with the device counts as "the system itself", an app the person installed does not. */
    private boolean isSystemPackage(String pkg) {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            return (ai.flags & (android.content.pm.ApplicationInfo.FLAG_SYSTEM | android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch (Throwable t) { return false; }
    }

    /** W3/W4: the card window can stay attached but invisible and untouchable (alpha 0 + NOT_TOUCHABLE) so showing it costs one redraw, not a window creation. */
    private void warmUp() {
        if (overlay != null || !coverWorld() || !Features.isCover(Features.powerMode(this))) return;
        Rect cached = cachedCard();
        if (cached == null) return;
        ensurePatch();   // the wallpaper patch must be added BEFORE the card so the card is on top
        if (!cardWanted()) return;   // AM1 (Tj): the stock menu stays completely stock: the patch alone hides the Screenshot spot
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Rect area = patchHomeMode ? cardAreaGeom(cached) : cardArea(cached);   // review A #2 (home flavour only; the lock screen and the patch-less case are as before): the short design: the content of a card built for the whole panel (1.25 x) is clipped when a later opening needs only the short window; a short card in a whole-panel window (showOverlay resizes it) still covers everything with its panel colour
        overlay = buildCard(area.height());
        overlay.setAlpha(0f);
        wm.addView(overlay, overlayParams(area, false));
        shownAt.set(area);
        overlayShown = false; overlayTouchable = false;
    }

    private void showOverlay(Rect full) {
        homeOpenDecide();   // BT1c: once per opening on the home screen: fresh patch, a stale one shown as it is, or the card over the whole panel
        if (!cardWanted()) { showStock(full); return; }
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (overlay == null) {
            ensurePatch();
            Rect r0 = cardArea(full);
            Log.i(TAG, "CARD area=" + r0.toShortString() + " full=" + full.toShortString() + " lockPatch=" + patchLockMode);
            patchShow(true);
            overlay = buildCard(r0.height());
            wm.addView(overlay, overlayParams(r0, true));
            shownAt.set(r0);
            overlayShown = true; overlayTouchable = true;
            cardShownMs = android.os.SystemClock.uptimeMillis(); recordLatency();
            return;
        }
        Rect r = cardArea(full);
        if (!overlayShown) Log.i(TAG, "CARD area=" + r.toShortString() + " full=" + full.toShortString() + " lockPatch=" + patchLockMode);
        patchShow(true);
        if (!overlayShown) { overlay.setAlpha(1f); overlayShown = true; cardShownMs = android.os.SystemClock.uptimeMillis(); recordLatency(); }          // visible first (one redraw) ...
        if (!overlayTouchable || !shownAt.equals(r)) {                              // ... then touchable / moved
            wm.updateViewLayout(overlay, overlayParams(r, true));
            shownAt.set(r); overlayTouchable = true;
        }
    }

    /** Back to the warm state: still attached, invisible, untouchable. */
    private void hideOverlay() {
        handler.removeCallbacks(recheck);
        if (overlay == null && !cardWanted()) {   // AM1: cardless: the patch goes back to resting (or hidden)
            if (overlayShown) { overlayShown = false; patchShow(false); setPatchTouchable(false); if (patchLockMode) { handler.removeCallbacks(calTrigger); handler.postDelayed(calTrigger, 1200); } }
            else warmUp();
            return;
        }
        if (overlay == null) { warmUp(); return; }
        if (!overlayShown) return;
        patchShow(false);
        if (patchLockMode) { handler.removeCallbacks(calTrigger); handler.postDelayed(calTrigger, 1200); }
        overlay.setAlpha(0f);
        overlayShown = false;
        try {
            ((WindowManager) getSystemService(WINDOW_SERVICE)).updateViewLayout(overlay, overlayParams(shownAt, false));
            overlayTouchable = false;
        } catch (Throwable t) { removeOverlay(); }
    }

    private void removeOverlay() {
        handler.removeCallbacks(recheck);
        if (overlay == null) { if (overlayShown) { overlayShown = false; try { patchShow(false); setPatchTouchable(false); } catch (Throwable ignored) { } } return; }   // AM1: cardless cover state
        patchShow(false);
        try { ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(overlay); } catch (Throwable ignored) { }
        overlay = null;
        overlayShown = false; overlayTouchable = false;
        shownAt.setEmpty();
    }
}
