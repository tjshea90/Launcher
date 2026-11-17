# CG1d: the lock-screen patch window: lifecycle, exposures, and a gesture matrix (section `lg`)

Read-only investigation (Claude, 2026-11-17, TASKS CG1d). Nothing in the product, the scenario or the workflows was changed.

Tj's words (TASKS CG1d, verbatim): "Sometimes the wallpaper patch shows up on the lock screen when i pull up the pin pad or when i first go to the lock
screen and hold the side key. Make it prepare immediately when the lock screen is open or when the screen turns off, so the screenshot button is always hidden
with no flashing of the wallpaper patch over it. If it helps, run a github emulator with various simulated presses and gestures on the lockscreen and see when
the patch is visible or slow and improve it based on the findings."

## 0. Legend, sources, and one big caveat

- **[V]** = read in the code at the cited line, or measured in a cited run or tablet report (the source is named). **[I]** = inferred: platform behaviour or a
  combination of facts, not observed. Where a fact comes from a research file I did not re-measure, the file is named and the tag is [V] (documented).
- **TAS** = `app/src/main/java/com/tj/launcher/TjAccessibilityService.java` (5266 lines; last commit 2026-11-15, so the line numbers below are current).
  `PatchRest.java`, `PatchFade.java`, `LockShade.java` are in the same folder. Times are milliseconds. "Uptime" = `SystemClock.uptimeMillis()`.
- Read for this report: every lock-patch path of TAS, PatchRest, the constants of PatchFade / LockShade, emulator sections `pt`, `fp`, `fl`, `ae`
  (`tools/emulator_scenario.sh`), `tools/fl_analyze.py`, `pt_analyze.py`, `pb_probe.py`, `pb_ui.py`, `tools/gate-map.txt`, `.github/workflows/emulator.yml`,
  the research files PATCH_MAP_BN2_BN3, BK1_LOCK_FIRST_PRESS, BY1_FLICKER_FINDINGS + TEST_PLAN, FAST_UNLOCK_AND_HOME_POWERMENU, NOTES_2026-11-04, REEXAMINE_2026-11-05,
  and the newest tablet report in the repo, `research/tablet/2026-11-10-2/tablet-report.txt`. TASKS.md / HANDOFF.md were only grepped.
- **Caveat that shapes everything:** the newest tablet report is from **v0.9.1**. The settle gate (BT1a, `PatchRest.SETTLE_MS`), the pad-left hold (BT1b), the
  shade rule (BY1c F1) and the pad-cancel rule (BY1c F3) all came AFTER it. Tj's CG1d message is about **v0.9.11**. So what the tablet does with the current
  code is **unmeasured**; every statement about it below is [I], and section 11 lists the log lines that would make it measurable.

## 1. Findings in brief

1. There is ONE patch window (title `TjWallpaperPatch`, TYPE_ACCESSIBILITY_OVERLAY, over the Screenshot slot). Its visibility has exactly one owner, `patchShow`
   (TAS 4565-4600), plus four immediate "hide now" writers (finger DOWN 4486, PIN-pad sign 4463, panel open 4441, calibration photo 1344). [V]
2. The window is invisible by construction (alpha 0) in every state except RESTING and OPEN. Since BT1a a NEW window is not allowed to rest for
   **1200 ms of uptime** (TAS 1572, `PatchRest.SETTLE_MS` 155). On the tablet the window is created 0.64-0.77 s BEFORE the SCREEN_OFF broadcast (report v0.9.1),
   so the gate ends 0.45-0.58 s AFTER the screen went off, with the display dark. [V for the numbers; the effect of a dark display is [I]]
3. Every return from "away" to "visible" is a **hard alpha 0 to 1 jump at a clock time** (settleRun, touchBack, padBack, padCancelCheck): none of them knows
   whether a finger is still down; the accessibility config has no touch-interaction events (`res/xml` event types: window state, windows, content, view clicked,
   notification state). A pull that starts after the finger hold has run out is protected only by the PIN-pad scan, which on the tablet came 136-203 ms after the
   finger's DOWN, never earlier. [V]
4. The hide at finger DOWN (`ACTION_OUTSIDE`) and the dialog's own window event are handled on the **same main thread as every node scan**. Tablet scan
   statistics: power-menu scans avg 28 ms, max 563 ms; the pencil cover's tree searches avg 33 ms, max 1305 ms. A DOWN or a dialog event that lands behind a scan
   is handled late by that long. [V numbers, report line 629; the collision frequency is [I]]
5. The Screenshot entry is hidden at the first frames of a menu opening ONLY by the fast path (`fastMenuOpen`, TAS 127-153), which can react no earlier than the
   stock dialog's own window event (no earlier signal exists: NOTES_2026-11-04, REEXAMINE_2026-11-05, run 37). That is at best 1-2 frames late (2 ms handling +
   19 ms first frame on an idle emulator thread, run #568; 12-19 ms on the tablet). Anything that makes the patch "away" or the fast path decline turns those
   frames into a ghost of the entry (away) or into a full scan (100-560 ms, declined). [V]
6. Candidate causes of Tj's two symptoms, ranked in section 7: (H1) main-thread stall behind a scan, (H2) clock-based returns with no finger knowledge,
   (H3/H4) a first lock cycle whose window is not yet rested / warmed, (H5/H6) the fast path being refused or late, (H7) the scan-miss after 2 s of an open menu.

## 2. The cast

### 2.1 Window and threads [V]
- Created by `addPatch` (TAS 1481-1577) only from `refreshLayers` (1468) and `ensurePatch` (1475-1479, called from showStock 4632 / showOverlay 5216).
  Removed by `removePatch` (1626-1633) from `refreshLayers` (1457, 1467), `wallpaperReceiver` (605), and the service teardown (`onInterrupt` / `onUnbind` /
  `onDestroy`, 3557-3582).
- Type TYPE_ACCESSIBILITY_OVERLAY, flags NOT_FOCUSABLE | LAYOUT_IN_SCREEN | LAYOUT_NO_LIMITS | NOT_TOUCHABLE (until the menu has been open 350 ms), plus
  FLAG_WATCH_OUTSIDE_TOUCH for the lock flavour (`overlayParams` TAS 4293-4303, 1559); format TRANSLUCENT (1560, an OPAQUE window ignores alpha 0).
- Rectangle = the Screenshot slot below the learned card plus a fringe (`patchRect` 1286-1295, `PatchRest.slotWindow`, extra bottom 24 dp + 4 dp fringe).
  Pixels = the stored real-pixel crop of the lock screen (`loadStoredShot` at creation, 1487) or the helper's fresh photo; two soft masks built at creation (1485).
- Everything below runs on the service's **main thread** (the accessibility service's Looper): events, timers (`handler`), broadcasts, node scans, the patch's
  touch listener (ACTION_OUTSIDE, 1564-1571), `onDraw`, `setAlpha`. [V: no HandlerThread is used for the patch; only the calibration photo has its own thread, 1347]
- Window flavours: lock (`patchLockMode && !patchHomeMode`) and home (`patchHomeMode`, pixels composed from the Launcher). `refreshLayers` swaps them by
  remove + add (1467); the home flavour is hidden at SCREEN_OFF (2914). This report is about the lock flavour.

### 2.2 The books (state that decides visibility) [V]
| Variable | Meaning | Written at |
| :- | :- | :- |
| `patchView` | the window exists | 1573 / 1631 |
| `patchRestable` | could rest right now (switch on, real pixels, calibration not running, pad not up) | 1572, 4583 |
| `restableSinceMs` | uptime since it became restable (0 = not); drives the settle gate | 1572, 4585 |
| `patchResting` | alpha 1 at rest | 4594 |
| `overlayShown` / `patchOpenFlag` | the stock menu is open for us (open look) | 4635 / 4569 |
| `touchAwayUntilMs`, `lastTouchDownMs` | finger DOWN hold (lock: 1500 ms) | 4483-4484 |
| `pinHiddenUntilMs`, `lastPadSignMs`, `lastBouncer` | PIN-pad hold (2500 ms per sign), last sign, last scan result | 4458-4459, 2970 |
| `lastPadLeftMs` | pad disappeared (hold 1500 ms) | 434-435 |
| `lockShadeOpen`, `lockShadeClosedMs` | notification panel open / closed at (+400 ms) | 4437-4438 |
| `pinHide` | the pad came up OVER an open menu: the open patch stays away | 4465 |
| `fading`, `closing`, `fadeStartMs`, `closeStartMs` | following the stock fade-in / fade-out | 4316-4317, 4342 |
| `lastUserPresentMs` | last unlock (for the pad-cancel rule) | 2926 |
| `patchTouchable` | window touchable (only while the menu is open) | 4660-4678 |

`removePatch` zeroes `touchAwayUntilMs` (1631) and `restableSinceMs`; it keeps `pinHiddenUntilMs`, `lastPadSignMs`, `lastTouchDownMs`. [V]

### 2.3 Who writes the alpha [V]
`patchShow` 4596 (the decision: `vis = can && now >= pinHiddenUntilMs && now >= touchAwayUntilMs && settled && now >= shadeUntil`, 4588; open menu: `vis = !pinHide`,
4576). Immediate hides to 0: `onOutsideTouch` 4486, `pinPadCameUp` 4463, `noteShade` 4441, `maybeCalibrateLock` 1344 (helper photo only), SCREEN_OFF home flavour 2914,
`addPatch` initial value 1572. The home-flavour block (lines 851, 935, 1015, 1024) is out of scope.

## 3. Constants (all VERIFIED)

| Constant | Value | Where | Meaning |
| :- | -: | :- | :- |
| `PatchRest.SETTLE_MS` | 1200 | PatchRest 155 | a (re)restable lock patch rests visibly only after this much uptime |
| settle timer | 1220 | TAS 1572 (`SETTLE_MS + 20`) | first settleRun after window creation |
| `restRecheck` | 400 and 1600 | TAS 1572 | backstop looks after creation |
| `LOCK_TOUCH_AWAY_MS` | 1500 | TAS 420 | lock patch away after any finger DOWN; `touchBack` at +1520 (4487) |
| `TOUCH_AWAY_MS` | 900 | TAS 413 | home flavour only |
| pad sign hold | 2500 (`pinHiddenUntilMs`), padBack at +2600 | TAS 4459, 4460, 4469 | each sign pushes the return out |
| `PAD_CALM_MS` | 1200 | TAS 414, 489 | padBack waits for this much quiet after the last sign |
| `PAD_LEFT_HOLD_MS` | 1500 | TAS 424, 435 | after the pad disappears; padBack at max(1500, hold) + 100 (436) |
| `PatchRest.PAD_CANCEL_MS` / `PAD_SIGN_QUIET_MS` | 700 / 400 | PatchRest 187, 189 | pad cancel rule; `padCancelCheck` at 720 (437) |
| `padSignFresh` | 1500 | TAS 4494 | a pad sign this young makes the fast path decline |
| `LockShade.SETTLE_MS` | 400 | LockShade 20 | return after the panel's "Lock screen." close title |
| `FAST_WAIT_MS` | 120 | TAS 193 | a window burst waits for the dialog's own event; `quietWork` at +140 (186) |
| `PatchFade.OPEN_MS` / `LEAD_MS` / `FRESH_MS` / `SETTLE_MS` | 300 / 10 / 600 / 330 | PatchFade 17, 24, 26, 28 | stock fade-in length, lead, "event still fresh", scan wait after open |
| touchable on / off | 350 / 40 | TAS 4667 / 4672 | `PatchFade.SETTLE_MS + 20` after open; 40 ms after rest |
| close fade | `CLOSE_MS` 300, `CLOSE_START_DELAY_MS` 40, safety 150, slack 100 | PatchFade 81, 108; TAS 399 | fade-out following |
| `dialogGone` ignore | < 800 after open | TAS 4362 | the emulator's double dialog |
| hold-miss guard | first 2000, recheck 250 | TAS 3695-3698 | one scan "no menu" is not believed inside 2 s of opening |
| scan recheck | 1500 | TAS 3685 | `evaluate()` again after a cover |
| `PowerMenuRule.DYING_DIALOG_MS` | 450 | PowerMenuRule 34 | a dialog listed this soon after a close is dying |
| `lockPrepareLater` | 700 and 6500 after SCREEN_OFF | TAS 2921-2922 | creation in the dark; second one for the 5 s lock-after |
| `layerRefresh` | +400 after SCREEN_ON and USER_PRESENT | TAS 2924, 2926 | |
| `lockEval` | now and +400 at each broadcast | TAS 2902 | PIN-pad scan |
| `scheduleLockUpdate` | 60 leading + 120 trailing | TAS 2956-2958 | |
| `scheduleLayerRefresh` | 150 throttle + 250 trailing | TAS 660-662 | |
| `calTrigger` | 1500 after creation, 1200 after a menu close | TAS 1469, 5241, 5248 | helper photo only (`ShellHelper.reachable()`) |
| `keyguardLockedFast` memo | 250 | TAS 1676 | cleared at each SCREEN_ON / OFF / USER_PRESENT (2897) |
| stock card fade | about 10 % at 54 ms, 22 % at 72 ms, 35 % at 88 ms | FAST_UNLOCK_AND_HOME_POWERMENU fact 4 | how long a ghost of the entry is faint |
| key long-press timer | 500 (`getDeviceGlobalActionKeyTimeout`) | BUTTONMAPPER_2026-11-05 line 43 | key DOWN to dialog on a real hold (plus inflate) |

## 4. The lifecycle, state by state, moment by moment

### 4.0 The states and their arcs [V]
```
NONE --addPatch--> NEW-HIDDEN (alpha 0, restable, settling 1220)
NEW-HIDDEN --settleRun / restRecheck / patchShow, books clear--> RESTING (alpha 1, invisible because it equals the lock screen)
RESTING --finger DOWN (onOutsideTouch 4486) / pad sign (4463) / panel open (4441)--> AWAY (alpha 0, still restable)
AWAY --touchBack (+1520) / padBack (+2600) / padCancelCheck (+720 after pad left) / settleRun / shade close +400--> RESTING   (hard 0 to 1, 4596)
RESTING or AWAY --dialog window event, fastDecline() == null (fastMenuOpen 127)--> OPEN (alpha 1, open look follows the stock fade)
RESTING or AWAY --dialog event, fastDecline() != null--> slow path: evaluateScan finds the menu text (100-560 ms) --showStock--> OPEN
OPEN --click / CLOSE_SYSTEM_DIALOGS / own BACK (startClose 4338)--> CLOSING --dialog window removed (dialogGone 4360) / timer--> RESTING (or AWAY if a hold is on)
OPEN --pad sign over the menu (pinPadCameUp, overlayShown)--> OPEN but away (pinHide, untouchable) --menu gone--> RESTING / AWAY
any --refreshLayers sees keyguard unlocked / rect, rotation, flavour change / wallpaper change / service teardown--> removePatch --> NONE (then maybe NEW-HIDDEN again)
```

### 4.1 Where the window is created (all at alpha 0 since BT1a) [V unless marked]
- **A. Going-to-sleep window events (the normal tablet case).** Any non-noise SystemUI event batch while `lockPatchWanted()` (needs `keyguardLockedFast()`) calls
  `scheduleLayerRefresh` (TAS 94, 658-663: immediate `refreshLayers` if 150 ms passed, plus a trailing one 250 ms later) which calls `addPatch` when `patchView == null`
  (1468). Tablet report v0.9.1: "PATCH window created ... screen on" at 03:11:00.970 / 03:11:22.204 / 03:11:38.470, and SCREEN_OFF at :01.741 / :22.844 / :39.153: **created
  0.64-0.77 s before the SCREEN_OFF broadcast, while `isInteractive()` was still true** (three of three lines; the "prepared in the dark" branch of that log text never occurred in the report).
- **B. `lockPrepare` in the dark.** SCREEN_OFF posts `lockPrepareLater` at +700 and +6500 (2921-2922); `lockPrepare` (1844-1871) calls `refreshLayers()` (1868) when `keyguardLocked()`.
  With "lock after 5 s" the keyguard is not locked at +700; the second run at +6500 is on an uptime timer (see 4.2, [I]).
- **C. At the wake.** SCREEN_ON calls `handleLockedWake` -> `lockPrepare("woke locked")` (2924, 1874): creates the window if it is missing (log: "N ms after the screen came on").
- **D. Service (re)connect.** `onServiceConnected` (306-335) -> `startPatchWatch` -> `refreshLayers()` (631); also preference changes (587-592) and `wallpaperReceiver` (605).
- **E. Rebuild.** `refreshLayers` removes and re-adds the window when its rectangle moved by more than 2 px, the rotation changed, or the flavour changed (1467); a rebuilt window
  starts hidden again and forgets `touchAwayUntilMs` (1631).
- On creation: `patchRestable = can`, `patchResting = false`, `restableSinceMs = now`, `settleRun` at 1220, `restRecheck` at 400 and 1600, alpha **0**, not touchable (1572). The log line is
  written (1572) and the window is the finger-DOWN sensor from now on (FLAG_WATCH_OUTSIDE_TOUCH). A finger DOWN that happens while NO window exists is never seen by anybody. [V]

### 4.2 Screen turns off
- [V] SCREEN_OFF broadcast (`lockReceiver`, TAS 2895-2932): `layerRefresh` posted at once (a no-op if the window exists), `lockEval` now and +400, `screenDarkNow`, home patch hidden,
  `lastBouncer = false`, `patchShow(overlayShown)` (2919: a patch whose books are clear is made visible NOW, with the display dark), `lockPrepareLater` +700 and +6500.
- [V] `lockPrepare` (1844): `LockLog LOCKPREP`, quiet rules, one dark look at the panel (`noteShade(shadeLevel(), ...)` 1867, a node scan), `refreshLayers()` (1868).
- [V, numbers from the report] In all three samples the window already existed at SCREEN_OFF (4.1 A; the report does not say whether the power key or the screen timeout turned the screen off)
  and would settle 0.45-0.58 s later under the BT1a rule (the report is from v0.9.1, before it).
- [I] **No vsync reaches an app while the display is off**, so a `setAlpha(1f)` + `invalidate()` made after SCREEN_OFF (the settle completes then) is recorded but not drawn until the first
  vsync after the wake. Before BT1a the first RESTING frame (and with it `warmOpenLookOnce`, 1516) was drawn at window creation, while the display was still on; since BT1a it falls after the display is off.
- [I] **Handler timers use uptime, which stops in deep sleep.** `settleRun`, `restRecheck`, `lockPrepareLater` (+700 / +6500) are `postDelayed`. If the CPU suspends soon after SCREEN_OFF, whatever
  is left of their delay runs after the wake. The lock log has wall-clock stamps only, so the report cannot show it (section 11, L3).

### 4.3 Screen turns on
- [V] SCREEN_ON (2924): `layerRefresh` now and +400; `lockEval` now and +400 (two PIN-pad scans, avg 28 ms each); `handleLockedWake` -> `lockPrepare("woke locked")` runs INSIDE the broadcast
  handling, including `noteShade(shadeLevel())` (a node scan) unless the dark look already ran (1867); `AnimationSpeedup.onScreenOn`; `restRecheck` +400 / +1600 if the window was just made;
  SystemUI's wake events start `scheduleLockUpdate` (60 + 120), `scheduleLayerRefresh` (150 + 250) and the pencil work.
- [V] Nothing at the wake calls `patchShow` directly. A hidden-but-restable window comes back only through `settleRun`, `restRecheck`, a scan that flips `lastBouncer`, or a hold runnable.
- [V] So the first ~2 s after the wake are the busiest time of the lock cycle for the main thread: roughly 6-10 scans / looks are scheduled in that window (list above). The scan duration is
  known (avg 28, max 563); how often a scan is running at a given instant is [I].
- [I] A keyguard fade-in at the wake (BY1-F2) over a patch that is already at alpha 1 would show the patch as a block until the keyguard is settled. Not seen on the emulator (wake films 0 flash
  frames, BY1_FINDINGS); the tablet was asked (BY1g) and the answer is still open.

### 4.4 Lock screen idle and lit
- [V] RESTING = alpha 1, `patchResting`, painted with identity brightness and the soft "rest" mask (onDraw 1492-1506); not touchable; the only protection against a finger is the DOWN hide.
- [V] Looks that can change the state at rest: `settleRun`, `touchBack`, `padBack`, `padCancelCheck`, `restRecheck`, `updateLockNumber` (pad flips), `noteShade`, `hideOverlay`.
  With the shell helper, `maybeCalibrateLock` (1326) hides a resting patch for at least 250 ms to photograph the screen (1344-1362) 1.5 s after window creation and 1.2 s after a menu close; it
  restores through `patchShow` (1385), which honours the books. Tj's tablet usually runs no helper (HLP1).

### 4.5 Side-key hold: the FIRST action after the lock screen appears vs a later one
Platform [V from NOTES_2026-11-04, REEXAMINE_2026-11-05, BUTTONMAPPER]: the key long-press timer is 500 ms; `showGlobalActionsInternal` sends no CLOSE_SYSTEM_DIALOGS (run 37: the receiver never
fired); the side key never reaches an accessibility service. So **nothing precedes the dialog's window event**. A hold while the screen is off does not open the menu (the wake is the first press
and the hold is a second press: [I], Android 10 `interceptPowerKeyDown` arms the long press only for an interactive device).
Our reaction [V]:
1. `onAccessibilityEvent` (TAS 67): `fastMenuOpen` is the FIRST statement for window events (74). It needs a TYPE_WINDOW_STATE_CHANGED whose class contains "ActionsDialog" (131-133).
2. `fastDecline()` (156-167) returns null only when: a window exists, it is the lock flavour, NOT ("not resting" AND ("not restable" OR `lastBouncer` OR `padSignFresh()`)), the menu is not
   already open, the cover mode is on, and the menu geometry is learned. So a patch that is merely AWAY (finger / pad hold / settle / shade) but restable is accepted; a patch that is
   not restable, or a pad that is up or was signalled within 1500 ms while the patch is not resting, is declined. [V]
3. Accepted: `showOverlay` -> `showStock` (4631) -> `patchShow(true)` (alpha 1, `startFade`, invalidate) and `deferScan` / `deferWork` so that scans wait 330 ms (152). The log line
   "menu opened while the patch was away" is written if it was not resting (148, lock log).
4. Declined: the burst's heavy work runs after `FAST_WAIT_MS` (quietWork, 194-198) -> `evaluate` -> `evaluateScan` (3608) finds the menu by text ("Power off" + "Screenshot"), then `showOverlay`.
   100-560 ms after the event on the tablet scan statistics. By then the stock card is opaque: the entry is fully visible for that long. [V mechanism, scan sizes from the report]
- Time to visible pixels, fast path [V]: handled 2 ms after the dialog event, first patch frame 19 ms after it (#568 menuW1, idle thread); 34 ms late and 86 ms (#568 menuW2, busy thread);
  tablet 12-19 ms after the stock scrim began, 19-20 frames, worst gap 26-28 ms (report v0.9.1). The stock card is 10 % opaque at 54 ms and 22 % at 72 ms, so a 2-frame reaction leaves a faint ghost.
- **What differs for the first hold after the lock screen appears** [V code, effects [I]]: (a) the patch may still be inside its settle (4.1), so it is AWAY at the dialog event and relies on the
  fast path; (b) `warmOpenLookOnce` (1516) draws the open look only in a RESTING frame, so a window that has not rested yet has never drawn it: the first opening is cold (BK1a measured a cold first
  opening: open_shape 0.194 against 0.04-0.06 warm, 3 frames, one 278 ms gap, run #389); (c) the stock dialog's own first inflate after a wake is slower (BY1: it "announces itself about when its first
  frame is drawn"); (d) the wake burst keeps the main thread busy (4.3).

### 4.6 Menu closes [V]
`fastMenuClose` (4509-4529): a click in the dialog window -> `startClose` (4338; follows the stock fade-out, starts 40 ms after the click); `closeDialogsReceiver` (612-621, reasons other than
"recentapps" / "globalactions"); the dialog's window removal -> `dialogGone` (4360; ignored in the first 800 ms); a tap on the hidden slot -> our own BACK. End: `hideOverlay` (5238) ->
`patchShow(false)` -> RESTING at once if the books are clear, else AWAY; `setPatchTouchable(false)` 40 ms later; `calTrigger` +1200. Scan fallback: `evaluateScan` finds no menu -> `hideOverlay`
(3703); the first miss inside 2000 ms of the opening is not believed (3695-3700). A close while `closing` is held until the fade-out time is over (3687-3690).

### 4.7 PIN pad pulled up, slid down (cancel), pulled up again [V unless marked]
1. **Finger DOWN** anywhere outside the patch: `ACTION_OUTSIDE` (the BE1 check in section `pt` proves delivery on the emulator) -> `onOutsideTouch` (4477-4490): only when the patch is not
   open and `pinHide` is false; `lastTouchDownMs`, `touchAwayUntilMs = now + 1500`, **alpha 0**, `touchBack` at +1520. `patchResting` is left true on purpose. Emulator: hidden 64-73 ms after the DOWN
   (CLAUDE.md, BC5 decision). The stock bouncer scrim starts to darken the wallpaper with the first MOVE; keyguard view events come later (TAS 405-408, BE1 comment). [V as documented]
2. **First sign of the pad.** Either a keyguard view event (class under `com.android.keyguard.` containing PINView / PasswordView / PatternView / SecurityContainer / HostView, `fastMenuClose`
   4513-4517) or the pad scan (`updateLockNumber` 2961 -> `bouncerShownScan` 2986: view ids `keyguard_pin_view` ... or texts "enter your pin" ...). `pinPadCameUp` (4451): `lastPadSignMs`,
   `pinHiddenUntilMs = +2500`, alpha 0, `patchShow(false)`, `padBack` +2600. **On the tablet (report v0.9.1) all four logged signs say "the PIN pad scan found it", 136-203 ms after the finger's
   DOWN: no view event ever came first.** [V]
3. **Pad up.** `lastBouncer = true` makes `can` false (`PatchRest.visible`, 22-26), so no return timer exists (`settleRun` is armed only when `can`). A menu opened now is declined by the fast path.
4. **Slide down / cancel.** The pad disappears -> `updateLockNumber` -> `padLeft` (432-439): `lastPadLeftMs`, `pinHiddenUntilMs >= now + 1500`, `padBack` at max(1500, hold) + 100,
   `padCancelCheck` at +720. `padCancelCheck` (452-467) takes a fresh pad look; `PatchRest.padCancelled` (192-194: 700 ms since the pad left, 400 ms since the last sign, no pad, locked, no unlock
   since) ends the hold: `showWithCause("padCancel")`. The patch is back at the latest of: pad hold end, `touchAwayUntilMs` (last DOWN + 1500), settle, panel. `patchShow` arms `settleRun`
   at that time + 20 (4589, `PatchRest.returnInMs`). Tablet v0.9.1 (before F3): "patch visible again (padBack), 3012 ms after the finger, 2809 ms after the last PIN pad sign".
5. **Re-pull.** Each new DOWN repeats step 1 (alpha 0, hold restarts); each sign pushes `pinHiddenUntilMs`.
6. **Unlock exit** (4.9).

### 4.8 Quick-settings / notification panel pulled down on the lock screen [V]
DOWN hides as in 4.7.1. The panel names itself through accessibility pane titles (`lockShadeEvent` 4390-4397, `LockShade`): "Quick settings." = open -> `noteShade(OPEN)` -> alpha 0 and **no timer**
(`shadeUntil = Long.MAX_VALUE` while open, 4587-4590); "Lock screen." = closed -> the patch may rest again 400 ms later. Level looks (`shadeLevel` 4409-4427, one id search) run at `touchBack`, in
`lockPrepare` (once while dark) and at service connect (320). The state survives a screen-off (BY1 F1b). Proven on the emulator (runs #566-#568: flash A 138 -> 0). **Not proven on the tablet** (BY1g still
waiting); the pane titles are AOSP SystemUI strings, MediaTek's SystemUI is [I]. The pencil cover's tree searches (avg 33 ms, max 1305 ms on the tablet) also run on the main thread.

### 4.9 Unlock [V]
The pad leaves about 270 ms before USER_PRESENT (BT1b, report 2026-11-10-2); `padLeft` holds the patch away 1500 ms. USER_PRESENT (2926): `lastUserPresentMs`, panel state reset, `lockPrepareLater`
cancelled, `layerRefresh` at +400 -> `patchTarget` (1258) sees an unlocked keyguard -> `refreshLayers` removes the window (1457) about 0.4 s after USER_PRESENT; the home flavour may be created instead.

### 4.10 Teardown and restart [V]
`onInterrupt`, `onUnbind`, `onDestroy` (3557-3582) remove the windows; `onTrimMemory` (3568-3580) drops cached bitmaps only when no window exists or in the home flavour. `uiautomator dump` unbinds
accessibility services while it runs (CLAUDE.md BK lessons): the window is gone until the service reconnects (`onServiceConnected` 306) and is recreated hidden (4.1 D).

## 5. Where the patch can be VISIBLE when it should not be (rest state, menu not open)

"Visible" matters only when the live lock screen differs from the stored crop at that moment: the scrim of a pull (bouncer or panel), a keyguard that is still animating, changed content under the slot.

| ID | Place | Window | Exposure | Tag |
| :- | :- | :- | :- | :- |
| V1 | `onOutsideTouch` hide (4486) waits behind a node scan or a timer on the main thread | the DOWN is queued; alpha 0 reaches the screen a few vsyncs after the handler runs ([I]; the emulator measured 64-73 ms from the DOWN) | stall length: avg 28 ms, max 563 ms (scans), pencil max 1305 ms; the scrim starts with the first MOVE, so a stall over about 100 ms exposes a darkening wallpaper behind a light block | mechanism [V], frequency [I] |
| V2 | `touchBack` (478-485) at DOWN + 1520, `padBack` (486-494), `padCancelCheck` (452-467), `settleRun` (477), shade +400: hard 0 to 1 at a clock time | they take a fresh pad scan first (`updateLockNumber`), but have no finger-up knowledge | a pull that starts later than 1.5 s after the last DOWN (a thumb resting on the screen, a hesitant start) darkens the wallpaper under a visible patch until the pad scan fires, 136-203 ms after the pull makes the pad visible (tablet, from the DOWN) | mechanism [V], pattern frequency [I] |
| V3 | `lastBouncer` false while the pad is still up (scan false negative: `getWindows` stale, `isVisibleToUser` false mid-animation) -> `padLeft` -> returns after 700-1500 ms | the patch over the pad | at least the hold time | [I] |
| V4 | A menu that has been open longer than 2 s and ONE scan answers "no menu text" (3695-3703: the guard covers only the first 2000 ms) -> `hideOverlay` -> the patch goes back to its bright resting look over the still dimmed screen | light block | until the next scan or event finds the menu (recheck 1500 ms) | mechanism [V] (the emulator saw it inside 2 s, comment at 3691-3693), tablet [I] |
| V5 | Wake: alpha 1 from the first frame while the keyguard may still fade in (F2) | the patch as a block | the keyguard's settle time | [I] |
| V6 | `removePatch` zeroes `touchAwayUntilMs` (1631): a rebuild during or after a DOWN forgets it; the new window rests at +1.22 s even if the finger is still down | small window | rare | mechanism [V] |
| V7 | A menu opened over the open PIN pad: the patch shows lock-screen wallpaper over the dark pad (#564 menuPU), or the entry shows (#566 menuPU); the two runs disagree and Tj's answer (BY1g (3)) is open | | the whole menu | [V as filmed], design question |
| V8 | Content under the slot that differs from the stored crop (charging text, clock minute, notification icons, a live wallpaper) | the resting patch is a block | until the content matches | not a lifecycle matter; no live pixel check exists on API 29 (`takeScreenshot` is Android 11+, NOTES_2026-11-04) |
| V9 | SCREEN_OFF: `patchShow` may set alpha 1 with the display dark (2919) | harmless | | [V] |

## 6. Where the stock menu can be open WITHOUT the patch (the Screenshot entry shows)

| ID | Case | Mechanism | Exposure | Tag |
| :- | :- | :- | :- | :- |
| M1 | No window: service not connected / unbound (`uiautomator dump`), process killed, first lock before calibration (`cachedCard() == null`, `PatchShot` missing, TAS 1263-1265), lock patch switch off | `fastDecline` "no patch window yet" (157) -> slow path -> `showStock` -> `ensurePatch` (4632) creates it after the scan | scan time, up to 560 ms; with no service: the whole menu | [V] |
| M2 | Patch AWAY at the dialog event (settle not over, finger hold, pad hold, panel) | fast path shows it at the dialog's own event | 1-2 frames: faint ghost (10-22 % card opacity) plus a light patch for the first stock frames; emulator menuW1: 2-4 flash-A frames in runs #565-#568 | [V] |
| M3 | Fast path declined: not restable (calibration running, no crop, pad up `lastBouncer`) or `padSignFresh()` (a sign within 1500 ms) while the patch is not resting | slow path (100-560 ms) | full entry for the scan time | [V] |
| M4 | Dialog event handled late (main thread busy): the event waits behind a scan | | the stall length, up to 563 ms on the tablet scan statistics | mechanism [V], frequency [I] |
| M5 | Cold first opening of a window that never rested (no warm-up drawn) | fade starts late and with gaps (BK1a: 3 frames, 278 ms gap) | the stock scrim darkens around a patch that is still bright; the entry stays covered | [V] measured in BK1a, tablet [I] |
| M6 | Pad hold after a cancel before `padCancelCheck` (0-700 ms after the pad left): not resting and `padSignFresh()` -> declined | slow path | up to 560 ms | [V], real-life frequency low (needs the key held BEFORE the cancel) |
| M7 | Menu opened over the open pad (BC5): by design the patch stays away | | whole menu | [V] design, open question for Tj |
| M8 | The hide at the fade's end (V4) | the patch dims correctly but then un-dims: the entry stays covered | none | the entry is NOT exposed; only the brightness is wrong |

## 7. Hypotheses, ranked

Ranking = how well each fits "sometimes", the two moments Tj names, and the evidence we have. Candidate fixes are sketches for the lead; none is written.

| Rank | ID | Where | When | Why Tj sees it | Tag | Candidate fix |
| :- | :- | :- | :- | :- | :- | :- |
| 1 | H1 | TAS 4477-4490 (hide at DOWN), 127-153 (dialog event) vs the scans at 2979-3007, 3599-3706, 4409-4427, pencil | a DOWN or a dialog event within about 0.2-2 s after a wake, or any burst of SystemUI events | the hide and the fast path are queued behind a node scan on the one main thread; the wallpaper darkens (pull) or the stock fade starts (hold) under a still-bright patch | mechanism [V], frequency [I] | (a) log the age of every ACTION_OUTSIDE and dialog event at handling (L2) to measure it first; (b) move the patch window, its touch listener and alpha writes to their own HandlerThread Looper so a scan cannot delay a hide; (c) no scheduled scan inside 300 ms of a wake or of a DOWN |
| 2 | H2 | TAS 478-494, 452-467, 477, 4483, 4588-4596 | pull-up begins after the finger hold ran out (thumb resting; hesitant or slow start), or any return timer firing while a pull is in progress | return is a hard alpha jump at a clock time; no finger-up event exists; the first sign of the pad is a scan 136-203 ms after the pull begins | mechanism [V], pattern [I] | (a) a return needs "no SystemUI event for 250 ms" plus a fresh negative pad scan, else re-arm 300 ms; (b) returns ramp alpha over 150 ms (a mismatch becomes a faint tint); (c) measure with a sendevent "touch and hold, then pull" case (G7 in `lg`) |
| 3 | H3 | PatchRest 155-166; TAS 1572, 4585-4589 | first hold after the lock screen appears: window created less than 1.2 s of uptime before (timeout lock, quick off/on within about 0.5 s, rebuild, service restart, lock-after delay), or the settle clock paused in deep sleep | the patch is AWAY at the dialog event, so the entry is covered only by the fast path 1-2 frames late; with a late creation, 1.2 s of hidden window | gate [V], pause and creation cases [I] | replace the clock gate by the books: persist `touchAwayUntilMs`, `lastPadLeftMs`, `lastUserPresentMs`, shade across rebuilds and let a window created within 2 s before SCREEN_OFF, or while dark, rest at once; if a clock stays, use `elapsedRealtime` and call `patchShow` at SCREEN_ON |
| 4 | H4 | TAS 1499, 1505, 1516-1533, 4592-4599 | first opening of a lock cycle | the open look is drawn for the first time during the real opening (cold GPU path) because `warmOpenLookOnce` runs only in a RESTING frame, and since BT1a that frame falls after the display went off; a node with alpha 0 is probably not drawn by the RenderThread | code [V], effect [I] | keep a hidden window at alpha 1/255 (not 0) so every frame is drawn and warmed from creation; warm at creation while the display is still on |
| 5 | H5 | TAS 156-167 | menu opened while the pad is up, within 1500 ms of a pad sign, or not restable | declined fast path -> slow scan -> the Screenshot entry fully visible for 100-560 ms | [V] | accept the fast path when `lastBouncer` is false and `lastPadLeftMs` is recent (the pad is gone) instead of the 1500 ms sign age; a pad scan is not allowed on that path |
| 6 | H6 | TAS 4451-4471, 2961-2973 | pad pull-up | the only early sign of the pad on the tablet is a scan; any scan that misses the pad (stale window list, mid-animation visibility) starts `padLeft` and a 700-1500 ms return over a pad that is still up | [I] | confirm the pad with two looks before `padLeft`; add a second cheap sign (the keyguard window's bounds / root class from the event, not a scan) |
| 7 | H7 | TAS 3695-3703 | menu held open longer than 2 s | one empty node search (busy SystemUI) -> `hideOverlay`: the patch un-dims over the dimmed screen = a bright block; the entry stays covered | mechanism [V] | `dialogWindowId` / window-list confirmation before ending an open look at any time, or two consecutive misses regardless of age |
| 8 | H8 | TAS 2914-2924, 4596 | wake | patch at alpha 1 at the first wake frame while the keyguard may fade in | [I] | log first, then a 250 ms wake ramp (hidden at alpha 1/255 for 250 ms after SCREEN_ON) |
| 9 | H9 | TAS 4390-4443, LockShade | notification panel on the tablet | if MediaTek's SystemUI does not send the AOSP pane titles, the patch returns over the open panel at DOWN + 1.5 s (the original F1 bug) | [I] | tablet check (BY1g (2)); the level look at touchBack covers part of it |
| 10 | H10 | TAS 1631; scrim fit (`maybeMeasureScrim` 4862) | rebuild during a pull; a wallpaper or theme whose scrim was never measured | forgotten finger; or a wrong end level for the whole menu | [V] mechanism / [I] | keep the finger book across rebuilds; ask for the `scrim_why` line of the report |

Dead ends that must not be re-derived [V, repo notes]: no signal before the dialog window (NOTES_2026-11-04 D3, REEXAMINE_2026-11-05, run 37: no CLOSE_SYSTEM_DIALOGS from `showGlobalActionsInternal`; the
side key is consumed before the accessibility input filter); a live pixel check of the lock screen (`takeScreenshot` is Android 11+; MediaProjection needs a foreground service and captures our own overlay);
an overlay that could see finger MOVE / UP (a touchable overlay would steal the touches of the keyguard; ACTION_OUTSIDE is delivered for DOWN only, `InputDispatcher` `maskedAction == DOWN`, [I]); an RRO
for `config_globalActionsList` (signature / bootloader).

## 8. How the emulator machinery works today

CI: `.github/workflows/emulator.yml` (manual dispatch, `sections` input; emulator-runner, API 29 x86 default target = plain AOSP, `swiftshader_indirect`, 600x1024 at 170 dpi, font scale 1.2, step
timeout 2700 s, job limit 50 min). The ffmpeg install step runs only for pt, hf, fp, fl. Wall time of a run is about 3 min boot + 2.5 x the sum of the table seconds in `tools/gate-map.txt` (+ a quarter),
e.g. `fl` (280 s in the table) takes about 12-14 min. `pt` and `rg` are always SOLO runs (gate.sh). Never dispatch an empty `sections`.

### 8.1 Section `pt` (`tools/emulator_scenario.sh` 2341-2619; table 305 s) [V]
- Setup: a PIN is set (`locksettings set-pin 1234`), the SetupActivity photographs the lock screen (`--ez test_lock_wallpaper true`) so a real-pixel crop is stored, `palock` locks and wakes, one
  unfilmed power menu opens to learn the panel (`CARD area=... full=[..][..]` log line), animations are forced ON (`ptanim 1.0`: the CI emulator runs them off, which would hide every fade).
- **Hold the power key:** `adb shell input keyevent --longpress KEYCODE_POWER` (a synthetic DOWN + long-press-flagged DOWN + UP, not a real 500 ms hold; the emulator sometimes shows the dialog twice
  about 0.9 s apart, TAS 4362 and BY1 findings). **Close:** `input keyevent KEYCODE_BACK`, a tap at 150,700 (`tap`), `wm dismiss-keyguard` (`pin`), or the CLOSE_SYSTEM_DIALOGS broadcast (`dlg`).
  **PIN pad:** `wm dismiss-keyguard` in `ptrun` (the emulator swallows a swipe-up while a modal dialog is open); `input swipe 300 900 300 300 300` in the `fl` / `ae` sections.
- **Films:** `ptrun <label>` records `screenrecord --time-limit 6 /sdcard/pt.mp4`, then runs `tools/pt_analyze.py <mp4> <label> --disp 600x1024 --p PX,PY --s "SX,SY;SX2,SY"` (ffmpeg): per frame
  the mean brightness P of the patch probe box and S of two surround boxes; `dev = (P / S) / r0 - 1` with r0 the resting ratio, so 0 = invisible patch, -0.10 = a block 10 % darker than its surround.
  Metrics: `onset` (first dimming, threshold 0.006), `rest_noise`, `open_max`, `open_final`, `open_shape` (normalised timing-only difference, end level excluded), `close_max`, `close_final`, `hold_max`.
  Every `ptinput` writes a `PATCH input <name> t=<uptime ms>` log line in the SAME shell call, on the clock of the app's own `PATCH ... t=` lines; `pt_analyze.py --events` prints the timeline
  relative to the dialog's own window event (`PATCHEV`, debug builds only).
- **Phases:** A the old one-step dim (`patch_fade` false); B the fade at leads 25, 0, 50, -25, 80 ms; C (BA1) the helper measures the stock scrim itself (bare frame, stock menu opened by the helper, second
  frame, closed; `ScrimFit`), then films the measured look; BC2 closes (back / tap / dlg / pin); BC5 / BE1 (pad over an open menu, resting patch hidden at DOWN, "touch hide" before the first pad sign);
  D (BB1) `pb_probe.py` shadow / geometry probe: screenshot pairs (`bare.png`, `shot.png`) with and without the patch, `pb_ui.py` names the nodes of a `uiautomator dump` (the emulator's 4th Emergency card);
  E (BB2 / BQ2) theme and measurement-count checks.
- **Thresholds** (the `check` lines): open_shape < 8 % (the fade closer to the stock scrim than the one-step dim, or within 8 %, at least 3 of 5 leads); measured look ends within 1.2 % of the surround and no
  further than the generic look + 0.5 %; scale 0.74-0.86 in all channels, 3000+ samples, rms <= 3; close mismatch within 15 % (20 % when the patch waited for a scan); PB: the geometry matches 24 px below
  the panel, slot interior mean deviation < 1.5 levels, last columns <= 3 levels, shadow line <= 2 levels. The emulator's stock scrim fits 0.75 or 0.80 by state, so checks read the FITTED value.
- Limits [V]: the stock scrim on the software renderer updates in about 100 ms steps (single film means read 0.03-0.17 even for a correct patch: judge counts such as HF4, not means); `pt` skips the first
  opening of a lock cycle on purpose (cold caches), which is exactly Tj's "first hold".

### 8.2 Sections `fp`, `fl`, `ae`, `ps`, `pa`, `hp`, `hf` [V]
- **`fp`** (BK1a; 120 s): films the FIRST opening after a lock cycle and the second, in a fresh process (cold) and in four warm cycles, with the same `pt_analyze.py` metrics plus the app's own
  `PATCH fade end ... first=<ms> frames=N slow=N worst=<ms>` statistics (the first patch frame ms after the stock scrim began). Proves the warm-up of `warmOpenLookOnce`.
- **`fl`** (BY1; 280 s; design in BY1_FLICKER_TEST_PLAN, findings in BY1_FLICKER_FINDINGS): the model for `lg`. Setup like `ps` (no helper, `lock_screen_show_notifications 0`, `flfeat`, calibration window
  240 s, check FL0 CALIBRATED), animations ON (`flanim 1`). Helpers `flrec <secs> [pre-sleep]` (screenrecord in the background), `flend <label> <flash|menu>` (pull, run `fl_analyze.py`, keep the `PATCH` log lines,
  print `FL <label>:` INFO lines: FLMETRIC, timeline `FLTL`, SystemUI pane events, the ORDER of the app's own decisions with film seconds (`seq`), event counts), `flpath` (counts of fast open / fast skip / pin hide /
  touch hide / rest on|off / visible again / pin pad left|cancelled / shade open|closed).
  `fl_analyze.py`: boxes from the learned panel (CORE = inside the hidden slot, RING = just outside the patch window left and below, TOP = the panel's upper part). REF = per-pixel median of the settled end of
  the film (`--ref last`; the analyzer pads the end with the last frame for 1 s because `screenrecord` writes a frame only when the screen changes). Per frame `core_d` / `ring_d` = mean |frame - REF|.
  **flashA** = core_d <= 6 and ring_d >= 15 (the slot shows the resting look while everything around it changed); **flashB** = core_d >= 15 and ring_d <= 6 (only the slot changed: a wrong patch);
  **menu** = TOP brighter than REF by >= 40; **shot** = a menu frame whose CORE is brighter than REF by >= 35 (the Screenshot entry shows). Output line `FLMETRIC label= mode= frames= flashA= flashA_ms= flashB= menu= shot=
  shot_ms= first_menu= worst= spans=`. Mode `menu` judges frames before the first menu frame as mode `flash` does.
  Cases of `fl`: wakeH1/H2/L (first lock screen after the custom home, and from the lock screen), shade, shadeX, shadeSlow, shadeOff, pad, tap, menuL, menuW1/W2 (menu about 0.5-1 s after a wake), menuS1/S2
  (after the panel), menuP1/P2 (after the pad), menuPU (menu over the pad). Checks: FL1 flashA <= 1 and flashB <= 1; FL2 menu >= 10 frames, shot == 0, flashA <= 1 (<= 4 for menuW1: runs 3, 3, 4, 2).
- **`ae`** (AE3; 90 s): the resting patch with a deliberately wrong wallpaper model (skew 30 %) so that it is VISIBLE: proves it paints only below the card, is not touchable, hides while the pad is up.
- **`ps`** (70 s) / **`pa`** (75 s): stored real-pixel crop and automatic calibration. **`hp`** (215 s) / **`hf`** (90 s): the home flavour and its film. **`v`** (57 s): geometry and dimming.
- Fidelity limits of all of them: see section 10.

## 9. Section `lg` ("lock gestures"): sketch (not written)

Goal: a matrix of gestures and timings around the lock screen that prints, for every cell, (1) how long the patch is visible when it should not be, (2) how long the stock menu is open without the patch,
(3) the time from the stock dialog's window to the patch being ready, and the PATH taken. It reuses `fl`'s setup, `flrec` / `flend` / `flpath`, `fl_analyze.py` (modes `menu` and `flash` already cover it) and adds a
compact state log from the app. It is an opt section (`tools/gate-map.txt` row, e.g. `lg :: 420 :: opt :: PatchRest|fl_analyze|lg_ :: patchResting|onOutsideTouch|touchAwayUntilMs|padBack|touchBack|settleRun|pinPadCameUp|padLeft|fastDecline|lockPrepare|restableSinceMs|warmOpenLookOnce :: CG1d`;
add `lg` to the sections text and to the ffmpeg step condition in `emulator.yml`). A full sweep is about 25 cases x 25 s plus setup: dispatch it BY HAND (about 20-25 min wall, inside the 45 min step limit);
the gate gets a quick subset (`lgq`: the cases marked q, about 8 min).

### 9.1 Cases (nominal delays; the REAL delay is an output)
Delays are nominal because every `adb shell input ...` is a new Java process (150-400 ms start-up each) and the emulator's `--longpress` is injected at once while a real hold takes 500 ms. So each case
prints the MEASURED interval "SCREEN_ON broadcast to dialog window event" (from the app's `PATCH screen on t=` and `PATCHEV ActionsDialog t=`) and the script injects the hold 0.2 s earlier than
the target. A hold at "d" means the dialog appears d after the screen came on; on the tablet add 0.5 s for the physical hold when reading the results.

| Group | Cases | What it tests |
| :- | :- | :- |
| G1 wake, then hold (q: 0.3, 1.0 with long off) | screen off 6 s: dialog at 0.3 / 0.8 / 1.5 / 3.5 s after SCREEN_ON; screen off 0.4 s (quick toggle): 0.3 / 1.0 | settle gate, warm-up, creation timing (H3, H4, M2, M5); also `power` pressed from the home (flavour flip) and from the lock screen |
| G2 pad pull, hold during and after (q) | swipe 400 ms with the hold injected 0.15 s into it; pad up then hold at +1.0 s | fast path declined by the pad (M3, M7), BC5 |
| G3 pad cancel, then hold | swipe, BACK at +1.0, dialog at +0.1 / 0.5 / 1.0 / 1.8 s after the BACK (q: 0.5, 1.8) | `padCancelCheck` (700), `padLeft` (1500), `padSignFresh` (1500), finger hold (1500) boundaries (M6) |
| G4 pad re-pull | swipe, BACK, swipe again at +0.3 / 1.0 / 1.7 s; film mode `flash` | hide at DOWN after a return (H1, H2) |
| G5 double press | `power`, `wakeup` 0.15 s apart, dialog at 0.4 s; and `power` x3 | racing off/on transitions |
| G6 panel | pull down, dialog while open at +0.5 s; BACK, dialog at +0.2 / +0.6 s (q: 0.2) | LockShade 400 ms settle, V2 over an open panel |
| G7 touch and hold, then pull (`sendevent`; optional) | finger down, wait 1.2 s and 1.8 s, then pull up | H2: a pull that starts after `touchBack` |
| G8 tap, then hold | tap at 150,700 then dialog at +0.2 / +1.2 s | finger hold path (1500) |
| G9 hold twice | dialog, BACK, dialog again 0.3 s later | dying-dialog rule (450), `dialogGone` ignore (800), re-open with the patch still closing |
| G10 long hold | dialog open 4 s; one injected "empty scan" (debug pref) after 2.5 s | H7 (a miss after 2000 ms hides the open look) |

### 9.2 What each cell prints (`LG <case>: ...`, INFO lines; a few checks)
- `vis_ms` = patch visible when it should not be: (film) frames of flashA + flashB outside the menu-open interval x frame interval, from `fl_analyze` (`flashA_ms` + flashB); (app) the sum of
  intervals with alpha 1 between a hold start (DOWN, pad sign, panel open) and the end of the hold, from the new `PATCHSTATE` lines (section 11, L1).
- `shot_ms` = menu open without the patch: `fl_analyze` `shot_ms` and `first_menu`.
- `ready_ms` = dialog window event to the patch's first frame: fast path = `PATCH fast open: the dialog's window event is N ms old` + `PATCH fade end lock first=M ms` (M counts from the stock scrim);
  slow path = the scan time (`PATCH scan took`, debug builds); `none` if the patch never came.
- `hide_ms` (pad / panel / tap cases) = finger DOWN to alpha 0, from the new `touch hide age=` line (L2) plus the first drawn frame.
- `path` = `flpath` counts (fast open / fast skip with its reason / pin hide / touch hide / rest on|off / visible again (cause) / pin pad left|cancelled / shade) and the ordered `seq` line.
- Checks: LG1 `shot == 0` in every hold case; LG2 flashA <= 1 frame (<= 4 for the first hold after a wake, as FL2 menuW1); LG3 `vis_ms` <= 50 in the no-menu cases; LG4 negative controls: a debug pref that
  turns the patch off must give `shot_ms > 0` in one hold case (the probe sees the entry), and the existing `patch_test_skew` must give flashB > 0 in one idle case (the probe sees a wrong patch).
  Everything else INFO. Always run the same sections on the released tag (`ref` = v0.9.11) in a second run and compare the cell values, as the BY1 / BT2 runs did.
- A/B inside ONE build with debug-only preferences (removed again afterwards, `git diff <gated head> -- app launcher3 phoneguide` empty): `lg_settle_ms`, `lg_touch_away_ms`, `lg_gate` (clock gate vs the book
  gate), `lg_hide_thread`. The result of an A/B on the emulator is a ranking of variants, not an absolute number (section 10).

### 9.3 Harness details
- Each gesture in ONE `adb shell "..."` call with `sleep` between the injections, preceded by a `log -t TjA11y "PATCH input <name> t=$(uptime)"` marker (the `ptinput` pattern), so the script's own uptime
  and the app's `t=` agree. `input keyevent KEYCODE_WAKEUP KEYCODE_POWER` injects two keys in one process; `input swipe x1 y1 x2 y2 <ms>` is a linear drag (no stationary phase).
- A stationary finger needs `getevent -p` to find the touch device and `sendevent` lines (DOWN, wait, MOVE...); fidelity of the emulator's virtual touchscreen is unknown: keep G7 optional and INFO-only.
- State between cases: `flunlock`, `home`, `logcat -c`, a settled 2 s; the first case of every group is a throw-away only when the fresh-process cold case is NOT the target.
- Cost: `fl` is 280 s in the table; `lg` full about 420-500, `lgq` about 200.

## 10. What the CI emulator can and cannot measure faithfully

| Quantity | Emulator | Notes |
| :- | :- | :- |
| Order and path of the patch's decisions (fast / slow, away / rest, which timer returned it) | faithful | same code, same event stream shape; the PATCH lines are the ground truth |
| Whether the Screenshot entry is visible in any frame (`shot`), and for how many frames | faithful at the frame level (about 16.7 ms) | the emulator's stock menu has a 4th Emergency entry and a second white card (`pb_ui.py`): `fl` measures from the learned panel (CORE below the card bottom); TOP / CORE boxes must come from the learned geometry, not from pixels of the tablet |
| flashA / flashB frames of the patch against its surround | faithful for relative comparisons | the stock scrim steps in ~100 ms jumps on the software renderer; the scrim fits 0.75 or 0.80 by state (never hard-code it); count frames, do not read means |
| Time from the dialog window to the patch's first frame | partly | 2 ms + 19 ms idle (run #568); the tablet showed 12-19 ms; the emulator's software GL makes first-draw costs very different |
| Absolute main-thread blocking by scans | not faithful | emulator scans measured 56-207 ms (BJ1 notes), tablet avg 28 / max 563 ms with a different SystemUI (MediaTek); H1 needs the tablet log (L2) |
| Deep sleep: uptime timers pausing, no vsync with the display off, a window created in the dark drawing at the wake | cannot | the emulator's screen-off does not suspend the CPU the way the MT8168 does (H3 / H4 effects) [I] |
| Cold GPU first draw / warm-up size (BK1a) | qualitative only | the 0.194 / 278 ms numbers are software-renderer numbers |
| A real 500 ms hold, the stock dialog's first inflate after a wake | no | `input keyevent --longpress` is injected at once; two dialog events appear sometimes (AZ1 / BY1) |
| Real fingers: a stationary thumb, finger DOWN to first MOVE, bouncer scrim curve | no | `input swipe` is a perfect linear drag; G7 via `sendevent` is the only approximation |
| Keyguard wake fade-in (F2), MediaTek SystemUI pane titles / class names / view ids | no | the emulator is AOSP SystemUI; pad detection by `keyguard_pin_view` ids and the LockShade titles are AOSP names |
| Wall-clock effects of a lock-after-timeout (alarm wake, CPU asleep when the keyguard locks) | no | needs the tablet |

## 11. What to log so the next tablet report answers this (candidate, small, release-safe)

- **L1 `PATCHSTATE` (lock log, one line per alpha CHANGE, 48 KB ring: only around finger / pad / menu activity):** `t(uptime) rt(elapsedRealtime) alpha=0|1 why=settle|touchBack|padBack|padCancel|shade|dialog|screenoff|finger|pad screen=on|off`.
- **L2 age lines:** `touch hide age=<ms>` (`uptimeMillis - MotionEvent.getEventTime()` in `onOutsideTouch`), `dialog event age=<ms> path=fast|slow patch=resting|away(<cause>)|none first=<ms>` for EVERY menu opening
  (today only "menu opened while the patch was away" reaches the lock log; the fast-open and fade-end lines are logcat only). This turns H1 / M2 / M3 into numbers on the tablet.
- **L3 window life:** `window created` with uptime AND elapsedRealtime, `first draw` (first `onDraw`, with `isInteractive`), `warm-up drawn` (today logcat only, 1531), `settled` (when `settleRun` runs), and
  uptime + elapsedRealtime at SCREEN_OFF / SCREEN_ON. This answers H3 / H4 / the sleep question.
- **L4 main-thread watchdog:** the longest gap between handler messages per lock cycle (a `Looper` message-logging counter) in the report's load line.
- Questions for Tj (short): (1) is the screen turned off by the side key or by the timeout, and what is "lock after"? (2) at the flash, was a finger or thumb already on the screen? (3) how soon after the
  screen came on was the hold, and was the flash a light block on the menu's lower part or the Screenshot entry itself? (4) light or dark theme? (5) the open BY1g items (panel, pad, menu over the pad).

## 12. Unverified items (not seen in code or a run)

1. Whether a window created or updated while the display is off is drawn before the wake (vsync behaviour of an accessibility overlay).
2. How soon after SCREEN_OFF the MT8168 suspends the CPU (uptime timers pausing).
3. Whether the RenderThread skips a view whose alpha is 0 (the basis of H4's "alpha 1/255" idea).
4. The keyguard's behaviour in the first frames after the wake on the tablet (F2).
5. The real DOWN to first MOVE time and the bouncer scrim curve on the tablet (how many ms of stall are visible).
6. The size of main-thread blocks at the instant of a finger DOWN (no log line records the age of an ACTION_OUTSIDE event).
7. MediaTek SystemUI: pane titles for the panel, class names for the pad, whether keyguard view events ever precede the scan.
8. Timeout-lock creation timing on the tablet (every window-created line in the newest report follows a power-key-like lock).
9. How often the pad scan misses a visible pad.
10. Whether a real 500 ms hold shows the dialog once (the emulator sometimes shows it twice).
