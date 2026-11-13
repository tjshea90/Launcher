package com.tj.launcher;

/**
 * AB2 (efficiency review for the ARMv7 / MT8168 / 2 GB tablet, Tj 2026-11-05). The accessibility helper receives EVERY content-change event of EVERY app (a game or a scrolling list sends hundreds a
 * second), and each one used to start the power-menu scan: two whole-view-tree text searches in every window, which makes the foreground app's own UI thread walk its views. Nothing in another
 * app's content change can open the power menu, the shade or the PIN pad, so those events are dropped right at the door; window changes, clicks and SystemUI's own events still pass.
 * Pure (no Android classes) so the rule is JVM-tested.
 */
public final class EventNoise {
    private EventNoise() {}

    /** android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED */
    public static final int TYPE_WINDOW_CONTENT_CHANGED = 0x00000800;
    public static final String SYSTEM_UI = "com.android.systemui";
    /** The package of windows that system_server itself owns (some ROMs host dialogs there). It is quiet, so it is kept rather than risk missing a system dialog. */
    public static final String ANDROID = "android";

    /** True for the events that cannot matter: a content change inside an app that is neither SystemUI nor the system's own windows. */
    public static boolean isNoise(int eventType, CharSequence packageName) {
        if (eventType != TYPE_WINDOW_CONTENT_CHANGED) return false;
        return packageName == null || !(SYSTEM_UI.contentEquals(packageName) || ANDROID.contentEquals(packageName));
    }

    /**
     * AN1 (Tj, 2026-11-06: "the battery saver tile is mostly unresponsive ... turns on very slow ... I'm almost certain this has something to do with the launcher or adb or accessibility"). Every node search the helper starts
     * is executed by SystemUI's own UI thread, the very thread that handles the tap on a quick-settings tile. While the shade is open SystemUI sends BURSTS of content changes (tiles redrawing, a ripple, the clock) and each
     * one started the power-menu scan (two whole-tree text searches in every system window) plus the pencil search: dozens of tree walks a second on a 2 GB MT8168. A content change cannot OPEN the power menu (that is a
     * new window: the window events start the scan and the next 300 ms are scanned in every window); it matters only while a menu is already up. Pure, so the rule is JVM-tested.
     */
    public static boolean needsPowerMenuScan(int eventType, boolean menuKnownUp, boolean inFullScanWindow) {
        return needsPowerMenuScan(eventType, menuKnownUp, inFullScanWindow, true);
    }

    /** BY1a B1: as above, and a CLICK starts the scan only when it comes from SystemUI or the system ("android") or the menu is already up: a click inside an app cannot open the power menu (984 scans in 121 min on the tablet, avg 28 ms each, the foreground app's tree included). */
    public static boolean needsPowerMenuScan(int eventType, boolean menuKnownUp, boolean inFullScanWindow, boolean fromSystemPackage) {
        if (eventType == TYPE_VIEW_CLICKED) return menuKnownUp || fromSystemPackage;
        if (eventType != TYPE_WINDOW_CONTENT_CHANGED) return true;   // window changes always start the scan, exactly as before
        return menuKnownUp || inFullScanWindow;
    }

    /** android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED */
    public static final int TYPE_VIEW_CLICKED = 0x00000001;

    /** BY1a B1: the packages whose windows can be the power menu or start it (SystemUI's GlobalActionsDialog; the system's legacy one). */
    public static boolean systemPackage(CharSequence pkg) {
        return pkg != null && ("com.android.systemui".contentEquals(pkg) || "android".contentEquals(pkg));
    }

    /**
     * BY1a B2 / B3: a window that can hold the keyguard, its PIN pad, the notification panel or the quick settings is at least a third of the display tall (Android 10's status-bar window is full height whenever the
     * keyguard or the expanded panel is showing; collapsed it is the 24 px bar, the navigation bar is its own short window). Checked BEFORE getRoot, which makes the window's owner walk its whole tree. 0 = unknown display.
     */
    public static boolean tallEnoughForPanel(int windowHeightPx, int displayHeightPx) {
        return displayHeightPx <= 0 || windowHeightPx * 3 >= displayHeightPx;
    }

    /** AN1: the shortest gap between two FULL pencil searches (a whole-tree walk in SystemUI's shade window): 40 ms after a window change or a click, 200 ms inside a burst of content changes. Following a pencil that is already known is one node, not a tree, and is not limited by this. */
    public static long minPencilSearchGapMs(int eventType) { return eventType == TYPE_WINDOW_CONTENT_CHANGED ? 200L : 40L; }

    /** android.view.accessibility.AccessibilityWindowInfo types */
    public static final int WINDOW_TYPE_APPLICATION = 1, WINDOW_TYPE_INPUT_METHOD = 2, WINDOW_TYPE_ACCESSIBILITY_OVERLAY = 4;

    /** android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED / TYPE_WINDOWS_CHANGED: a window appeared, changed or went away. */
    public static final int TYPE_WINDOW_STATE_CHANGED = 0x00000020, TYPE_WINDOWS_CHANGED = 0x00400000;

    /** True when this event can mean "a new window just showed up" (a power menu is a new window). Right after one, the search looks in EVERY window like before; only the quiet follow-up checks are narrowed. */
    public static boolean windowsJustChanged(int eventType) {
        return (eventType & (TYPE_WINDOW_STATE_CHANGED | TYPE_WINDOWS_CHANGED)) != 0;
    }

    /** True for a window that belongs to the system, not to an app or the keyboard (status bar, shade, dialogs of SystemUI, volume, global actions: AccessibilityWindowInfo.TYPE_SYSTEM and friends). */
    public static boolean isSystemWindow(int windowType) {
        return windowType != WINDOW_TYPE_APPLICATION && windowType != WINDOW_TYPE_INPUT_METHOD;
    }

    /**
     * Which windows are searched for the power menu? When a window titled "ActionsDialog" (AOSP's power dialog) exists, only that one; otherwise only system windows: never the foreground app's window
     * (whose tree can be large and whose UI thread would run the search), never the keyboard, never our own overlays (type 4). BY1a B1: right after a window change too (it used to search EVERY window
     * then, "an unusual power menu on another Android build"; Tj's tablet shows SystemUI's global_actions_view, tablet report 2026-11-10-2, and the fast path at the dialog's own event is unchanged).
     */
    public static boolean mayHoldPowerMenu(boolean someWindowIsTheActionsDialog, boolean thisWindowIsTheActionsDialog, int windowType) {
        if (windowType == WINDOW_TYPE_ACCESSIBILITY_OVERLAY) return false;
        if (someWindowIsTheActionsDialog) return thisWindowIsTheActionsDialog;
        return isSystemWindow(windowType);
    }
}
