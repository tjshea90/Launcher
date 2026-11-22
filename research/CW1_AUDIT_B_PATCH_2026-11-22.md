# CW1 audit B: is the Screenshot-entry hider READY before the first power-menu open? (2026-11-22)

Read-only audit (slice B of CW1a). Nothing was edited, built, run or committed except this file. Requirement from Tj (TASKS CW1, verbatim part):
"preparedness of the screenshot button hider from both home screen and from lock screen so it is hidden before first open of the power menu without lag or flickering".

Tags: **[V]** verified by reading the code (file:line), **[I]** reasoned from code and research docs, **[T]** only Tj's tablet can show (the CI emulator is plain AOSP, not the MediaTek tablet).
Line numbers are `app/src/main/java/com/tj/launcher/TjAccessibilityService.java` (call it TAS) unless another file is named. Sources read: TAS (all the power-menu paths), `HomePatchRules`, `PatchRest`, `PatchFade`, `PatchShot`, `PowerMenuRule`, `PowerMenuPrime`, `ProfileSession`, `UnifiedRules` / `Unified`, `TjHooks`, `EventNoise`, `AutoCalibration`, `ScrimMeasure`, `Features`; research `CG1D_PATCH_LIFECYCLE_2026-11-17`, `HOME_PATCH_2026-11-08`, `FAST_UNLOCK_AND_HOME_POWERMENU_2026-11-08`, `CS2_POWER_MENU_PRIME_2026-11-21`, `CV1_AUDIT_A/B_2026-11-22`, `tablet/2026-11-10-2/tablet-report.txt`; `tools/gate-map.txt` and `tools/emulator_scenario.sh`.

## 0. How the hider works (the facts every finding rests on)

- **The patch** is one `TYPE_ACCESSIBILITY_OVERLAY` window "TjWallpaperPatch" owned by the accessibility service. Translucent, `NOT_FOCUSABLE`, `NOT_TOUCHABLE` until the menu has been open 350 ms, with `FLAG_WATCH_OUTSIDE_TOUCH` on the lock and home flavour. It holds REAL pixels of what is behind the stock panel's Screenshot slot, so at rest it is invisible because it equals the screen. [V addPatch 1487-1583]
- **Two flavours share one code path** (`patchLockMode`; the home flavour also sets `patchHomeMode`): `fastDecline` answers "not the lock screen" when `patchLockMode` is false (TAS:163). So the real-pixel patch exists ONLY on the lock screen and on the custom home screen. Everything else (drawer, an app in front, a home that is not "ready") uses the whole-panel CARD (`cardWanted`, 540-545). [V]
- **The service and the Launcher share ONE main thread.** Anything the service runs on its handler delays the Launcher's frame and the patch's own fade frames. [V]
- **Fast path** (AZ1): the stock dialog announces itself with a `TYPE_WINDOW_STATE_CHANGED` whose class contains "ActionsDialog" (TAS:128-158). `fastDecline` (161-172) decides; `PatchFade` follows the stock 300 ms FAST_OUT_SLOW_IN fade from the event time (OPEN_MS 300, LEAD_MS 10, FRESH_MS 600, SETTLE_MS 330). **Slow path** = node scan (`evaluateScan` 3794) 100-560 ms. A press that takes the slow path shows the stock Screenshot entry (and its first frames) before anything hides it. [V]
- **Who prepares what, when:**

| Piece | Prepared by | When |
| :- | :- | :- |
| Lock patch window | `lockPrepare` 1854 (via `lockReceiver` 2970-3017), `refreshLayers` 1458, `onServiceConnected` 311 | SCREEN_OFF at 0 / 250 / 700 / 6500 ms; SCREEN_ON; USER_PRESENT +400 ms; every window burst (throttled 150 ms + 250 ms trailing) |
| Crop (real pixels) | `PatchShot` (sig = wallpaperId, rotation, WxH; no theme), `loadStoredShot` 4890 | read when the window is made (`addPatch`); calibrated by `AutoCalibration` (screenshot, 3 s after a quiet lock) |
| Scrim fit (open look) | `ScrimMeasure` (per rotation, per theme slot), `reloadScrim` / `setOpenLook` 4980-5020 | one run per lock, 10 s after wake (the stock menu opens by itself, camera flash) |
| Geometry of the stock panel | pref `power_card_rect_r<rot>` (`cachedCard`) | learned on the first scan in that rotation |
| Home patch composite | `scheduleHomeBuild` 1040 / `startHomeCopy` (PixelCopy of the Launcher over the stored wallpaper-only crop) | after the home has been quiet 250 ms (`DRAW_QUIET_MS`) |
| Home crop | home calibration (`maybeHomeCalibrate` 1116, Launcher hidden for a screenshot; per wallpaper id, size, rotation, page step; max 6 a day, 60 s gap) | when the home is at rest and nothing else runs |
| Warm card (whole-panel replacement) | `warmUp` 5714 | attached at alpha 0 and NOT_TOUCHABLE so showing it costs one redraw |
| Stock dialog warm | `PowerMenuPrime` (CS2) | only after `ProfileSession.returnToCustom` with a tile or drawer-icon return; never on the lock screen |

## 1. Findings table

Severity: HIGH = the first open can plausibly show the Screenshot entry or a visible flash in a COMMON, repeatable situation; MED = a rarer situation or a bounded flash; LOW = cost or polish with no visible effect expected.

| Id | Sev | Situation | Where | Finding | Fix | Risk |
| :- | :- | :- | :- | :- | :- | :- |
| B1 | HIGH | Home, first press after unlock, return from an app, double Home, wallpaper page step | `HomePatchRules.openTreatment`, TAS `homeOpenDecide` 886-944, `scheduleHomeBuild` 1040-1101, `ProfileSession.returnToCustom` 133-148 | The composite is built only after the Launcher has been quiet for 250 ms plus a PixelCopy. A press before that gets OPEN_STALE (the old composite shown as it is: a wrong-looking block if the page or icons changed) or OPEN_FULL_CARD (slow path: the stock entry is visible during `getWindows`, the scan and the card draw). CS2 primes ONLY after a tile / drawer-icon return, not after a plain PIN unlock, a double Home, a key-code return, a boot or a process start. [V] | (a) Start the composite build earlier: at USER_PRESENT +100 ms on the custom home, at Launcher resume, instead of waiting 250 ms of draw quiet (keep the quiet rule only for a rebuild, not for the first one). (b) Keep the previous composite for a re-open on the SAME page step and wallpaper and mark it fresh while the Launcher is resumed without a draw of changed content. (c) Run the CS2 prime also after a plain unlock and a double Home, behind the existing cover, at most once a minute. | Battery: one PixelCopy (a ~1 s event) per unlock; CPU spike while the Launcher draws its first frame (the same main thread). (c) The prime shows a cover for ~1 s: acceptable after an unlock only if nothing is touched meanwhile (the existing guards). |
| B2 | HIGH | Drawer open, an app in front, a home that is not "ready" | `fastDecline` 163 ("not the lock screen"), `cardWanted` 540-545, `showOverlay` 5729-5753 | Only the lock screen and the ready custom home have the real-pixel patch. In the drawer, over an app and on an unready home the first event goes through the SLOW path: `quietBurst` does not wait, `leadingEdge` scans (100-560 ms, tablet scans avg 28 / max 563 ms), and the warm card is shown only after the scan finds the menu. For that time the stock entry and the stock scrim are visible without cover. Never improved since FAST_UNLOCK doc section 4.4. [V mechanism, T size] | Make the card path fast too: on the dialog's own `ActionsDialog` event show the pre-warmed card at once (no scan first), with the same decline rules; the scan then only verifies and re-geometries. The drawer is a custom-launcher surface, so a real-pixel patch from the Launcher's own pixels is possible later (larger work). | The card replaces the panel: a wrong geometry would show a card that does not fit; the geometry is learned (`cachedCard`) so the decline "geometry not learned yet" keeps the old behaviour. A fast card on a window that is not the power dialog = a card flash; the class check "ActionsDialog" makes that rare. |
| B3 | HIGH | Home flavour, every first press | TAS 214-222 (`quietBurst`, line 219) | On the burst's first window event the home flavour calls `schedulePencil(event)` SYNCHRONOUSLY before returning "quiet", i.e. before the dialog's own event is handled. A pencil tree search measured avg 33 ms, max 1305 ms on the tablet (834 searches in 121 min). That time is added to the lag of the fast path (the patch's fade is anchored on the dialog event's time, so a late handling shows as +x % brightness for the first frames). [V mechanism, T size] | On the first event of a burst skip or defer the pencil search when `fastDecline() == null` (run it with `deferWork` after the first patch frame; `quietWork` already calls `schedulePencil(null)`). | The quick-settings pencil cover follows a SystemUI event up to one fast-wait (120 ms) later. The B6 40 ms rule was kept for that reason: pin it with a JVM test of `burstTreatment` and a `hp` / `hf` run. |
| B4 | MED | Card path (warm card shown) | TAS 5748-5752 | `showOverlay` sets alpha 1 and in the same message calls `updateViewLayout` (touchable). That binder call and the relayout land in the traversal of the first visible frame. AZ1 measured 100-200 ms for such a call on the emulator; the PATCH already defers touchability to +350 ms (`setPatchTouchable`), the card does not. [V] | Make the card visible first (alpha) and set it touchable 350 ms later, like the patch. | A tap inside the first 350 ms would hit the stock dialog under it: the stock entries are Power off, Restart, Screenshot; nobody taps within 350 ms of the open, and the card hides Screenshot anyway. Low. |
| B5 | MED | Lock, no crop: after a wallpaper change, a new rotation, "Clear data", first install | `patchTarget` 1264-1285, `fastDecline` 164, `AutoCalibration`, `ScrimMeasure` | Without a crop the patch is not restable: `fastDecline` says "not resting / no valid crop"; the press goes the slow path (100-560 ms with the stock entry visible) and a model patch (a visible block by design) may be drawn. The calibration runs 3 s after a quiet lock (camera flash) and the scrim measurement 10 s after wake (a second flash and the stock menu opening by itself). Until then the open look is the generic 0.80. There is no "never show the entry" fallback on the lock (cardWanted is false on the keyguard). [V] | (a) Start the calibration at the first quiet moment of the first plain lock after the change (not 3 s of waiting plus the quiet rule) so the first press is rarely before it. (b) Optional policy choice for Tj: until a crop exists show the whole-panel card on the lock screen too (it hides the entry fully, but it is not stock-identical). | (a) The camera flash happens earlier, possibly while Tj is looking at the screen. (b) Changes the look. Decision for Tj. |
| B6 | MED | Rotation | `onConfigurationChanged` 3549-3566 | The handler reloads the scrim for a theme change and notes the orientation for the unlock rotation twirl, but does NOT refresh the patch layers. The window for the new rotation is made at the next window event (the throttled `scheduleLayerRefresh`), and a rotation never learned has no geometry (`cachedCard` null: decline, slow path) and no crop (see B5). [V] | Call `refreshLayersSoon()` from the orientation branch (remove + add for the new rotation); learn each rotation once (geometry + crop) the first time it is lit, not at the first press. | A rebuild per rotation: one window remove + add (cheap). Tablet use is mostly portrait [I]. |
| B7 | MED | Process or accessibility-service restart | `onServiceConnected` 311, `startPatchWatch` 629, `warmOpenLookOnce` 1522 | The first window is made at `onServiceConnected` (via `refreshLayers`) but it is hidden for `PatchRest.SETTLE_MS` 1200 ms when the screen is lit; prefs, geometry and the crop are read only now; its first draw is cold (B8). Process starts recorded on the tablet: 9 in the report, 4 in 24 h. After a lock-after-timeout the screen is already off, the uptime timers pause in deep sleep, so the window is created at SCREEN_ON (lit) [I/T]. | Make `onServiceConnected` build the window with the dark-settle rule when the screen is off; at SCREEN_ON of a service that started during the dark, do not apply the 1.2 s settle when the window was prepared dark (CG1d already counts a window prepared in the dark as settled; verify the cold-start path takes that branch). | None expected; a missed branch only costs the first press after a restart. |
| B8 | MED | Cold first draw of the open look (any flavour, first open in the process) | `warmOpenLookOnce` 1522 (only in a RESTING frame), `paintPatch` 1540, CG1d H4 | The open look (a `ColorMatrixColorFilter` path and the paint state) is warmed only when a resting frame is drawn; a window at alpha 0 is not rendered at all, so a patch window that was hidden until the first press draws the open look for the first time AT the fade. CG1d measured 0.194 against 0.04-0.06 on the emulator (BK1a). [V on the emulator, T on the tablet] | Keep the hidden state at a tiny non-zero alpha (alpha 1/255) so RenderThread prepares the layer, or draw one warm frame at creation while the display is on (the existing `warmOpenLookOnce` call, without the "resting" condition). | A drawn window costs a small amount of GPU per update; it never redraws unless invalidated, so a static 1/255 layer is a few tenths of mW [I]. Risk: a ghost of 1/255 is invisible. |
| B9 | MED | Home, wallpaper scroll step (mid-scroll) | `patchTarget` 1264-1285, `onWallpaperOffset` 654, `TjHooks.onWallpaperOffset` 119, `homeDrew` 1005 | While the page steps, a transient step has no crop and `refreshLayers` REMOVES the home window (`patchTarget` null). It is made again after a 200 ms settle with a PNG decode (`PatchShot.load`, `BitmapFactory.decodeFile` on the main thread) and masks, and the composite needs another 250 ms of quiet. A press after a swipe within ~0.5 s takes B1's stale or card treatment. [V] | Keep the window and mark it "not restable" instead of removing it; keep decoded crops for the last N steps (a small LRU, the crops are ~130 x 140 px). | Memory: a few small bitmaps (tens of KB). |
| B10 | LOW | Every fast open | TAS 139-157 | Before `showOverlay` the fast path writes `Log.i` twice and builds `patchGateText` (string work); `logPatch` after the frame is already deferred. [V] | Move both `Log.i` calls and the gate text after `showOverlay`. | None. |
| B11 | LOW | Every opening | `setOpenLook` 5002 (4980-5020) | Per frame: prefs reads (three lookups) and up to ~19 `ColorMatrixColorFilter` allocations per opening (one per distinct q). [V] | Cache the fitted scale / offset in fields when `reloadScrim` runs; reuse one filter and `set()` the matrix. | None; allocation during a 300 ms fade can trigger a GC pause on a Go-edition device, so it is also a smoothness gain [I]. |
| B12 | LOW | Finger down on the menu | `onOutsideTouch` 4664 | The hide at finger DOWN waits behind any node scan on the same thread (H1 in CG1d). Tablet scan avg 28 ms, max 563 ms. The clock-timed returns (`touchAwayUntilMs`) do not know the finger-up (H2). [V mechanism, T frequency] | Do not start a scan while a touch guard is open; pend the scan for 200 ms after any outside touch. | A menu action seen 200 ms later by the scan path. |
| B13 | LOW | Every layer refresh | `patchTarget` 1264-1285, `refreshLayers` 1458-1478, `keyguardLockedFast` 1680, `homeDrew` 1005 | `patchTarget` calls `shotSig()` (two binder `getWallpaperId` calls) and a real `keyguardLocked()` binder read on EVERY refresh; with no window the Launcher's draws trigger `refreshLayersSoon` every 2 s. [V] | Cache the wallpaper id (refresh on the wallpaper-changed broadcast), use the 250 ms keyguard memo for non-decisions. | None. |
| B14 | LOW | Helper on (adb grant) | `maybeCalibrateLock` 1332, `calTrigger` +1500 ms | The helper's screenshot photo hides the RESTING patch for at least 250 ms once per lock session; during it a press has no patch. Helper is normally off on the tablet (HLP1). [V, T for the real window] | Skip the photo when the stored crop is fresh (the photo is only needed without a crop); the photo is also a way around B5. | None. |
| B15 | LOW | Lock, the PIN pad came and went | `fastDecline` 164, `padLeft` 437, `padCancelCheck` 457, `padBack` 492 | A patch merely AWAY (hold) gets the fast path (a 1-2 frame ghost is possible); the fast path DECLINES (the full entry for 100-560 ms) if not resting and (not restable or a pad scan is up or the sign is < 1500 ms old). Hold 1500 ms, cancel check 720 ms. [V] | Already bounded; a sign-fresh decline should return to the fast path as soon as the pad scan reports gone. | Needs the `ae` section; do not touch without it. |
| B16 | LOW | Theme (dark) flip | `onConfigurationChanged` 3551-3556, `ScrimMeasure.valueKey` | The crop has no theme (a wallpaper crop does not change); the scrim has one slot per theme and the dark look uses the LIGHT measurement until a dark one exists (BQ2: one measurement per wallpaper only). The flip schedules `scrimTrigger` +6 s. [V] | None needed beyond a note: the dark open look is a fitted approximation. | The first dark press has the light fit; error is bounded (the research says ~a few levels) [T]. |
| B17 | DECISION | Unlocked profile (Mode QUICKSTEP) | `HomePatchRules.whyNot` 58 ("Quickstep"), `PowerMenuRule.coverWorld`, `patchTarget` null | No patch, no card, no scan runs there; the stock menu shows the Screenshot entry. By design (CV1: "no patches"; `PowerMenuRule`: "Unlocked Quickstep stays untouched"). Whether Tj's requirement "home screen and lock screen" covers the unlocked world is NOT stated; see section 5. | See section 5. | A home patch in the unlocked profile touches three places and has no test. |

## 2. Per-situation analysis

### 2.1 Lock screen

- **First long press after boot.** [V] `onServiceConnected` -> `startPatchWatch` -> `refreshLayers` builds the window; `lockPrepare` runs at SCREEN_OFF +0 / 250 / 700 / 6500 ms and only calls `refreshLayers` when the keyguard is locked; after boot the keyguard is up at the first SCREEN_ON. What can be late: the crop does not exist yet (B5), the geometry is not learned (B6), the scrim is the generic 0.80 until the first measurement (10 s after wake, one run per lock, MAX_PER_DAY 4, MIN_GAP 45 s). The very first press of a fresh install therefore always takes the slow path [I]. Boot with an existing crop on disk is fine.
- **First screen-on.** [V] SCREEN_ON -> `handleLockedWake` -> `lockPrepare`; a layer refresh +400 ms after SCREEN_ON and USER_PRESENT. A window prepared while the screen was DARK counts as settled at once (CG1d dark settle; `PatchRest.restableSinceFor`), a window made lit waits 1200 ms of uptime. SCREEN_OFF voids the holds (2994-3001). A press within 1.2 s of a lit creation is the only gap [V]; on the tablet the window was created 0.64-0.77 s before SCREEN_OFF in three recorded lines (that is, while still lit: those windows are NOT dark-prepared) [T report]. [T] whether a press right at wake is realistic: a side key long press needs ~0.5 s.
- **After the app process or the service was killed.** See B7. Prefs and crops survive; the window is rebuilt by `onServiceConnected`; the cold draw is B8. [V]
- **After a wallpaper change.** B5. The sig (wallpaper id, rotation, size) no longer matches, `PatchShot.has` is false, the crop is stale until the next calibration (+3 s after a quiet lock, flash). The scrim slot per wallpaper is kept separately. [V]
- **After a rotation.** B6. [V]
- **After a theme flip.** B16. [V]
- **Helper off / on.** B14. Off: the crop comes from the system screenshot calibration (AutoCalibration, camera flash); on: the photo path, which hides the resting patch >= 250 ms. [V]
- **After the PIN pad came and went.** B15. [V]

### 2.2 Home screen, custom profile (Mode CUSTOM)

- **First open after unlock / return from an app / double Home.** B1. The first-ever custom-session press is covered by the CS2 prime only if the return was by tile or drawer icon (`ProfileSession.returnToCustom` 133-148 calls `primePowerMenu` only with `pocketVia`). [V]
- **After a profile reload** (`HomeProfile.syncNow`, 84-110: the DB flip with a reset cover): the model is rebuilt, the Launcher draws, the composite is stale until 250 ms of quiet after the last draw -> B1. [V by reading `HomeProfile`, I for the timing]
- **Drawer open.** B2. The drawer is the Launcher's own activity but the home composite does not represent it: card path, slow path. [V]
- **Wallpaper scroll step** (`TjHooks.wallpaperX` / `wallpaperReported`). B9. The earlier "known" flag (BI1 lessons) prevents the stuck-away case, a transient step still removes the window. [V]
- **Home calibration** (the crop): Launcher hidden for a screenshot per wallpaper id, size, rotation, page step; max 6 a day, 60 s gap. A page step that was never calibrated has no crop: the first press there takes the card path (OPEN_FULL_CARD) [V].

### 2.3 Unlocked profile (Mode QUICKSTEP, our Launcher3 with all apps)

- No patch window is created (`patchTarget` null), no scan runs, `coverWorld()` is false, `HomePatchRules.whyNot` returns "Quickstep" (58). The stock menu opens exactly stock, with the Screenshot entry. [V]
- The unified-world tests (`ul`, `ul2`) have NO power-menu checks; HP10 checks only "no patch window in Quickstep" with the stock launcher. [V]
- The CV1 design deliberately says "no patches" there; the only residue is that the patch window may be created for the lock screen and removed at unlock; `homeDrew` can cause a `refreshLayersSoon` every 2 s in that profile to clean up (B13). [V]

### 2.4 Other apps in front (a dialog opens over an app, Mode CUSTOM)

- `patchLockMode` is false (not the lock screen, not the home flavour): the fast path declines "not the lock screen", the normal scan path runs, the warm card (attached at alpha 0 by `warmUp`) is shown after the scan finds the menu: same as B2. [V]
- In Mode QUICKSTEP over an app: untouched stock menu (B17). [V]
- Lock screen with a notification panel / QS open: the shade logic (`LockShade` settle 400 ms) holds the patch away; the menu cannot be opened from there in the stock UI except via the side key [V/I].

## 3. Timing numbers (code or research)

| What | Value | Source |
| :- | :- | :- |
| Stock scrim fade-in | 300 ms FAST_OUT_SLOW_IN, from the dialog's own event | `PatchFade.OPEN_MS` [V] |
| Our lead, fresh window, settle after the fade | LEAD_MS 10, FRESH_MS 600, SETTLE_MS 330 | `PatchFade` [V] |
| Window event -> dialog event (burst) | 15-45 ms | TAS 197 comment [V] |
| Fast wait before the normal scan | FAST_WAIT_MS 120 | TAS 198 [V] |
| Node scan (power menu), tablet | avg 28 ms, max 563 ms (984 scans in 121 min) | tablet report [T] |
| Pencil tree search, tablet | avg 33 ms, max 1305 ms (834 in 121 min) | tablet report [T] |
| Events in 121 min | 45521 | tablet report [T] |
| Patch rest settle | 1200 ms uptime (0 when prepared dark) | `PatchRest.SETTLE_MS` [V] |
| Touch away / PIN hide | lock 1500 ms, home 700-900 ms; pin 2500 per sign; pad hold 1500; cancel check 720; sign fresh 1500 | TAS constants [V] |
| Home composite quiet | DRAW_QUIET_MS 250, then PixelCopy | `HomePatchRules` [V] |
| Touchable delay | 350 ms after open | `setPatchTouchable` [V] |
| Scrim measurement | 10 s after wake, 1 per lock, 4 per day, 45 s gap | `ScrimMeasure` [V] |
| Lock calibration | 3 s after a quiet lock; helper photo hides the patch >= 250 ms | `AutoCalibration`, TAS 1332 [V] |
| CS2 prime | start delay 1200 ms, hold 700 ms, cap 4000 ms, min gap 60 s | `PowerMenuPrime` [V] |
| Home calibration | max 6 a day, 60 s gap | `HomePatchRules` [V] |
| Dialog first-ever draw flash on the emulator | 2-4 frames (BY1 menuW1 flash A) | `CS2` doc [V emulator, T tablet] |
| Process starts on the tablet | 9 recorded, 4 in 24 h | tablet report [T] |
| AZ1 relayout cost (emulator) | 100-200 ms for an `updateViewLayout` in a first-frame traversal | TAS comments [I: emulator only] |

## 4. Main-thread work in the first 300 ms after a power-menu window event

Order per event in `onAccessibilityEvent` (TAS 67-106) [V]:

1. `diagEvent` (cheap, capped at 200 lines) and `noteWindowEvent`.
2. `fastMenuOpen(event)`, FIRST: for the dialog's event: `fastDecline`, `cachedCard` (prefs), two `Log.i`, `patchGateText` (lock only), `showOverlay`: `homeOpenDecide` (home flavour), `patchShow(true)`, `PatchFade` start, then `deferScan`/`deferWork`. (B10)
3. `fastMenuClose`, `lockShadeEvent`, `homeForeignWindow` (home), `trackForeground`, `AnimationSpeedup.tick`, `picker().onEvent`, `recents().onEvent`: all small, one pass per event.
4. `quietBurst`: on the home flavour's burst start `schedulePencil(event)` SYNCHRONOUSLY (B3). On the lock flavour the heavy work is deferred.
5. If not quiet: `scheduleLockUpdate`, `scheduleLayerRefresh`, `schedulePencil`, `trackDirty`, `trackUnlockKey`, `guardStrayHome`, `noteOtherWindow`.
6. During the fade: `patchRedraw` per frame -> `setOpenLook` (B11: prefs lookups + filter allocation per frame).
7. After the fade (`PatchFade.SETTLE_MS` 330 ms) or `deferWork` 60 ms: `quietWork` -> `scheduleLockUpdate` (PIN-pad scan, a dozen node searches), `scheduleLayerRefresh`, `schedulePencil(null)`, `evaluate(EV_QUIET)` -> `evaluateScan` (node scans 28-563 ms). This lands INSIDE the 300 ms window when the fade was skipped (slow path) or finishes early.
8. The Launcher's own frames (home flavour: its draw can invalidate the composite -> `homeDrew`).

## 5. Unlocked profile: what Tj needs to decide (B17)

The audit answer in one sentence: no hider runs in the unlocked profile, so its power menu shows the stock Screenshot entry by design; Tj's wording ("home screen and lock screen") plausibly means the custom world only, but it should be asked once, because the unlocked profile now IS a Launcher-owned home screen (CV1) and could use the same home flavour (same Launcher pixels, same stored crop) if the "Quickstep" guard in `HomePatchRules.whyNot` and `PowerMenuRule.coverWorld` were relaxed. Touches: `whyNot` (58), `coverWorld`, `patchTarget`; no test exists for it; the home calibration is per profile (the unlocked DB has other icon pages).

## 6. Battery and cost of a permanently attached, invisible window

- The lock patch is a ~135 x 145 px translucent layer at rest (plus a 24 px margin and a 4 dp feather). It is composited only when the screen updates; it is drawn only when invalidated. [V size, I cost] Negligible: the compositor's blend of a small region on each frame while the screen is on is a fraction of the base cost.
- The pre-attached card (`warmUp`, alpha 0) is attached once and never redrawn at alpha 0; showing it costs one redraw. [V]
- An alpha 1/255 window (B8) IS drawn once and then static: a few tenths of mW while the screen is on and nothing while it is off [I].
- The real cost is main-thread scanning: tablet report 984 power-menu scans (avg 28 ms) and 834 pencil searches (avg 33 ms) in 121 min = ~55 s, roughly 0.8 % duty, clustered at events. [T report] Anything that moves a scan after the first frame (B3, B10) does not cost battery, only reorders.
- Earlier creation (B1(a), B7) costs one PixelCopy / window creation per unlock or restart; both are single events.

## 7. Test coverage and the first-open cases NO test covers

What exists (tools/emulator_scenario.sh, tools/gate-map.txt):

| Section | What it proves | Limits |
| :- | :- | :- |
| `pt` (solo, 305 s) | the lock patch follows the stock fade (phases A-D); the first opening is SKIPPED on purpose | the CI emulator's scrim is 0.75 / 0.80 by state; skips the cold first draw (B8) |
| `fp` | the first opening after a lock cycle (lock only) | lock screen only; no home |
| `fl` | film metrics of the flash / shot, per frame | emulator timing |
| `ae` | PIN-pad hold and return (4 s hold) | not the short pad session (PatchRest.returnInMs) |
| `hp`, `hf` | home patch: phases, HP4 the fresh patch, HP7 log, `hf` films 3 treatments x 10, HF4 fast path in >= 9 of 10 | the home is quiet before the press; a drawn Launcher, a fresh composite |
| `bt` | the stale / fresh rules of BT1c / BT1d (stale shown as it is) | |
| `pm` | the CS2 prime: lines, no menu or cover left behind, a real side-key menu still opens | does NOT film the first press after the prime |
| `lg` | the lock-patch lifecycle A/B for dark settle | data section |
| `ul`, `ul2` | the unified profile, DB copy, cover bridging | NO power-menu or patch checks |

**First-open cases that NO test covers** [V by grep of the sections]:

1. The first home press right after a PLAIN PIN unlock, a double Home or a key-code return (B1), with the composite not yet built.
2. The first press after a wallpaper PAGE STEP, a swipe within 500 ms of the press (B9).
3. The first press after a process kill of the app or the service (home and lock) (B7); the cold open look (B8).
4. The first press in a NEW rotation (geometry and crop unknown) (B6).
5. The first press after a wallpaper change on the lock screen or the home (B5).
6. A press with the DRAWER open or over another app: the card path's first frame (B2) and the card's relayout (B4).
7. A press right after SCREEN_ON of a window made lit (the 1.2 s settle), and the lock-after-timeout creation (B7).
8. A theme flip then the first press (B16).
9. The power menu in the unified (unlocked) profile (B17).
10. The helper-on photo window (B14).

## 8. What I could not check

- Real timings on the tablet: the scan collision frequency, the MediaTek keyguard's fade, whether the real stock dialog announces itself earlier or later than on AOSP, deep-sleep timer behaviour of the settle (uptime pauses). [T]
- Whether a 1/255 alpha window is truly drawn by the tablet's compositor and what it costs. [T]
- The exact bytes of the pre-made crops (ids, sizes): only the code and the reports were read, no preference files.
- Nothing was run (read-only task); no section was executed.

## 9. Order of value (my suggestion for CW1b)

1. B3 (pencil before the dialog's event on the home): one small change, no new state.
2. B1(a)+(b) (build the home composite earlier, keep it for the same page step) and B1(c) (prime after a plain unlock).
3. B2 (fast card on the dialog's event).
4. B4, B10, B11 (cheap, zero risk).
5. B6, B7, B8, B9 (state and window lifecycle: need a section each).
6. B5 and B17 are decisions for Tj.
