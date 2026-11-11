# BT1 (Tj, 2026-11-11 05:03 UTC, v0.9.4 on the tablet): six reports, first analysis

Written while reading, so a cut-off session does not re-derive it. Sources: TASKS BT1a-f, `research/PATCH_MAP_BN2_BN3_2026-11-09.md` (the map of the patch code; read its sections 1-6 before touching anything),
`research/BN2_BN3_BN4_DESIGN_2026-11-09.md` (what v0.8.13 already did), `research/tablet/2026-11-10-2/` (the newest tablet report, v0.9.1, a lock-screen test, no home-screen press in it).
NOTHING below is measured on the tablet after v0.9.1; Tj sent no new report with this message.

## Family tree of the six items
- BT1a = BN3 again (lock patch visible while the PIN pad is pulled up). v0.8.13 added: hide at the finger DOWN (`ACTION_OUTSIDE`, since v0.8.x), touch-away 900 -> 1500 ms, `touchBack` looks at the pad, every return logged (`PATCH visible again (...)`). Cause never proven. Open candidate: the frame latency of `setAlpha(0)` (2-3 frames) against the first frames of the stock bouncer scrim; "the first time" = the first pull-up of a lock cycle.
- BT1c + BT1d = BN2 again (home patch NOT ready at a side-key press). v0.8.13 fixed the unlock case only (provisional foreign-window guard after the keyguard, dying-dialog ignore). The shade case (pull the notification panel, close it, hold the side key) and "sometimes" are other not-ready reasons.
- BT1b = new (a power-menu-looking thing flickers on/off after the first unlock into the custom launcher with NO key press). Candidates: the scrim measuring stage (`maybeMeasureScrim`: the helper opens the REAL stock menu by itself with GLOBAL_ACTION_POWER_DIALOG and closes it with BACK), the home calibration (Launcher hidden ~1 s = the camera flash), a window-event burst of the unlock read as a menu burst (card path), the patch itself re-showing.
- BT1e, BT1f = look-alike text items (see below), small.

## Fact that explains BT1c / BT1d / BN2 (from BN2d in the 2026-11-09 report and the map, section 2)
When the home patch is NOT ready at the press (window missing, composite stale = `patchRestable` false, or the foreign-window guard), `fastDecline` refuses the fast path and the SLOW path shows the CARD (`cardWanted()` = `!homePatchReady()`). The card in the home flavour is the SHORT card `[485,339][596,547]` that covers only the first two entries (Power off, Restart). NOTHING covers the Screenshot slot `[547..642]`: the stock Screenshot entry is visible for the whole first press ("the full power menu with screenshot button"). So every "patch not ready" state = Screenshot button visible.

## BT1f (Recents name): cause found by reading AOSP 10 (to be confirmed by `web-fetch`, see TASKS BT1f)
Android 10's `ActivityManagerWrapper.getBadgedContentDescription(info, userId, td)` returns `applicationLabel` if it EQUALS the activity label, else `applicationLabel + " " + activityLabel`. Our application label is "Launcher" (`launcher3/res/values/strings.xml: app_name`), the look-alike activity's label is "Settings" (`@string/ss_label`), so the Recents card is titled "Launcher Settings" = Tj's "launcher settings". A TaskDescription label cannot fix it (it only replaces the ACTIVITY label side). Only `applicationLabel == activityLabel` gives plain "Settings".

## BT1e (keyboard text)
`SsSystemInfo.enabledKeyboards` joins the labels of ALL enabled input methods (`SsFormat.joinAnd`), so "Gboard and Microsoft SwiftKey keyboard". Fix = leave SwiftKey out (`KeyboardPolicy.pickSwiftKey` already identifies it by package containing "swiftkey").

## Plan (order)
1. BT1e fix + JVM test. 2. BT1f decision (see TASKS). 3. Emulator reproduction of BT1c (new section: shade down, close, long-press power at +0.5 / +1 / +2 s, picture + PATCH log) and the readiness timeline; 4. fix the not-ready windows (shade close, stale composite) and give the not-ready case a fallback that never shows the Screenshot entry; 5. BT1a: film the pull-up (frame analysis) and shorten / remove the hide latency; 6. BT1b: find the trigger with a quiet-home film after a plain unlock.
