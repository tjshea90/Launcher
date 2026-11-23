# CX1a (B): the custom-home patch lifecycle — read-only audit, 2026-11-23 (v0.9.18 code)

Source: a read-only subagent, neutral brief. TAS = TjAccessibilityService.java, HPR = HomePatchRules.java, HK = TjHooks.java. [V] read in code, [I] inferred. Tj's ask (CX1 point 3): the wallpaper patch prepared as soon as possible on the custom home so the Screenshot button is always hidden.

## What "ready" means
The first side-key press finds a patch only if the stock dialog's own window event passes `fastDecline` (TAS 161-172): window exists, `patchRestable` (or stale-adoptable per `HPR.openTreatment` 297-302: window + composite kept + same wallpaper step + Launcher focus + plain home + no calibration), overlay not already shown, geometry learned. Otherwise the slow path (node scan 28-563 ms on the tablet; BN2 case: lag 306 ms) and the card covers the whole panel: the Screenshot entry can show for the scan time.

## State table
- window `patchView`: added `addPatch` 1588 (crop decoded on the main thread by `loadHomeWall` 1501; alpha 0); removed `removePatch` 1641-1648 (also drops composite, crop, `homeBuiltAtMs`) by `refreshLayers` when `patchTarget()==null` (1472), flavour / rotation / rect change (1482), wallpaper receiver 620, calibration start 1163.
- `patchTarget` -> `homePatchTarget` 756-769 -> `HPR.whyNot` 53-67: switch, mode, automation, custom, unlocked, screen on, `homeScreenShowing` (HK 157 = resumed && NORMAL), no floating view, not busy, not calibrating, rect learned (per rotation), crop valid (needs `HK.wallpaperReported`).
- crop `homeWallBmp`: `loadHomeWall` 982 (at addPatch and on step change); `HomeShot.findStep` accepts the stored step +-2; one PNG per rotation + step.
- composite `patchShotBmp`/`homeBuiltAtMs`: `startHomeCopy` 1086-1110 (PixelCopy of the Launcher window over the crop). Fresh = builtAt >= `HK.lastHomeDrawMs` and same step.
- build scheduling: `scheduleHomeBuild` 1049-1055 posts at `waitMs(..., buildQuietMs)` + 20; first composite quiet 100 ms (HPR 367-369, CW1 B1a), rebuild 250 ms (HPR 21); the `buildDue` loop re-posts WITHOUT a cap (1062).
- `patchRestable`: true by `homeRestable()` 4850 (fresh + home showing + no foreign guard + Launcher focus); false on ANY Launcher draw (`homeDrew` 1029), foreign window 866, screen off 3031, FULL_CARD verdict 949; true again by STALE adoption 945.
- alpha / resting: `patchShow` 4848-4868; visible only if restable AND 700 ms since touch AND 250 ms since draw (HPR 94-99). Finger: `HK.onTouch` 135 -> `homeTouchDown` 1036 (alpha 0, recheck 730 ms); ACTION_OUTSIDE 4734 (900 ms const, home flavour).
- foreign guard `homeForeignUntilMs`: set 863 (2500 ms) by named SystemUI / other-app WINDOW_STATE events; provisional 400 ms recheck within 3000 ms of the keyguard being seen up; lifted by Launcher focus regain (873-888, `HPR.guardEndsWithFocus` 335).
- every Launcher draw: `HK.watchHomeDraw` 64-76 (DragLayer OnDrawListener, set once per Launcher instance) -> `lastHomeDrawMs`.

## The fifteen situations (R = patch ready at the press)
1. First resume after process start / boot: window needs rect learned + `HK.wallpaperReported` (first interpolator message) + 200 ms settle, then addPatch + 100 ms + 20 + PixelCopy. R = no until the composite; before that card / slow path.
2. Home key from an app: Launcher pause -> `whyNot` "not on the plain home screen" -> window, composite AND crop are FREED (1472, 1645-1646). Resume rebuilds the window (composite not kept), first build after the last draw (return animation) + 120 ms + copy. R = no for that time (FULL_CARD); duration not measured.
3. Double Home: mode flip + `HomeProfile.syncNow` reload under a cover = many draws, composite stale each draw. The CS2 prime runs only after a tile / drawer-icon return, NOT double Home. R = no until the reload settles + 120 ms + copy.
4. Page flip: each step change re-arms `homeOffsetSettled` 200 ms; transient steps have no crop -> `patchTarget` null -> window AND composite removed; new window + PNG decode + 100 ms + copy about 350-500 ms after the last offset change [I]. Mid-flip: R = no (FULL_CARD). CW1 B9 `keepWindowForStep` exists (HPR 386) but is NOT wired. A never-calibrated page: no crop -> calibration trigger 2100 ms.
5. Drawer open / close: window removed at the transition start; at close `setNormal(true)` -> refresh -> addPatch -> 100 ms + copy. R = no during the drawer and about 0.2-0.4 s after [I].
6. Shade down / up: SystemUI FrameLayout window event -> guard 2500 ms, alpha 0, restable false; Launcher loses focus. Focus regain lifts the guard at once (if it began >= loss - 1500 ms); otherwise the guard runs ~2540 ms. During the guard a press is STALE-adopted if the composite is kept and focus held (945), else the full card. A closing event after focus regain can open a fresh 2.5 s guard [I].
7. Quick settings: same code path as 6.
8. Rotation: `onConfigurationChanged` (3591-3606) does NOT refresh layers [V]; the window moves only when a later `refreshLayers` runs; rect / crop / geometry are per rotation: a never-used rotation has no rect and no crop. `buildHomePatchNow` does not test rotation. R = no until refresh + calibration; unmeasured.
9. Wallpaper changed: receiver drops id cache, crop, composite, window; new crop sig -> "not calibrated" -> calibration trigger 2100 ms -> `calDecision` gates (touch quiet 3000, draw quiet 1500, screen on 5000, gap 60 s, 6 per day, permission) -> Launcher hidden 150 ms, screenshot (abort 4500) -> crop saved -> build. About 3.5-5 s best case, per page; R = no throughout (card).
10. Folder open / close: floating view -> `whyNot`; build retries 10 x 400 ms then waits for the next draw; press while open = FULL_CARD; on close 250 ms quiet + 20 + copy.
11. Long-press popup / widgets: as 10 plus finger-down hides at once. R = no while open and ~0.35 s after.
12. Return from lock after a plain PIN: USER_PRESENT sets `lastKeyguardUpMs`, layerRefresh +400; unlock window event -> provisional guard; recheck every 400 ms until Launcher focus. The plain unlock flips the mode a few ms later -> HomeProfile reload + profile cover; draws mark stale. Tablet evidence (v0.8.12, BN2_BN3_BN4_DESIGN:8): Enter tap 57.520, window created 58.232, USER_PRESENT 58.435, composite fresh 59.310 (0.88 s after USER_PRESENT, 1.79 s after Enter). R = no for about 1 s (before B1a).
13. Menu closed then opened at once: a dialog event during the 300 ms close fade -> "already open" decline (168); close re-evaluates restable; a draw DURING the menu makes the composite stale, and a rebuild comes only from a later refresh / draw / focus event (`homeFocusChanged` skips while `overlayShown`). Second press: STALE-adopted if focus held; rebuild at +350 ms.
14. Screen off / on on the home: SCREEN_OFF sets restable / resting false, alpha 0, and the posted layerRefresh REMOVES window + composite; wake: layerRefresh now and +400, Launcher resume / draws; window + build as in 2.
15. A clock / widget / animation draw with a still finger: ANY DragLayer draw anywhere (not only under the slot) = stale (restable false, alpha 0), rebuild 250 ms after the LAST draw. A once-a-minute draw costs about 330-400 ms of not-ready [I]. CONTINUOUS draws postpone for ever: the quiet loop (1062 -> 1049) has NO cap [V]. Whether Tj's home draws continuously: not determined.

## GAPS (first press can show the Screenshot entry, or the patch is not ready)
- G1 No window or no composite after any Launcher pause, drawer, shade, page step, wallpaper / profile change, screen-off: window created, then 100 ms + 20 + PixelCopy (+ draw quiet). Tablet pre-B1a about 1.1 s (BN2); no tablet number after B1a.
- G2 Continuous Launcher draws: no composite for as long as they last (unbounded; 1049-1062, HK 70).
- G3 No crop (new wallpaper, new page step, new rotation, Clear data) or rect not learned: no window until a calibration passes its gates; retry 3 s / 61 s after failure, max 6 a day; permission missing = never. Can be minutes to the whole day.
- G4 Foreign-window guard 2.5 s (+ 40 ms) after a named SystemUI window event; press during it = STALE block if the composite is kept, else full card.
- G5 A press inside a Launcher draw window (stale composite): adopted STALE (wrong pixels possible if the changed element lies under the slot), rebuild at +350 ms.
- G6 FULL_CARD cases (folder / popup open, drag, calibrating, no composite, no focus): fast path declines, slow path scan; entry visible for the scan (tablet avg 28 ms, max 563 ms).
- G7 Cold open look: `warmOpenLookOnce` runs only in a resting frame; a window re-created at every removal draws its open look cold if pressed before its first resting frame (>= 250 ms after the composite). Measured for the lock (BK1a 0.194), not for the home [I].
- G8 Rotation: no refresh trigger from the service; stale rect / crop possible.
- G9 Home-burst blocking now = scans (not measured on the home).
- G10 NOT wired from CW1: B9 keep-window (HPR 386), B2 fast card, B4 card touchable (HPR 340).

## Not determined
Tablet time to the first ready patch after B1a (no report newer than v0.9.1 has home lines); PixelCopy and PNG decode time on the tablet; Launcher resume time after unlock; whether any element on Tj's home draws continuously (widget updates probably fire the DragLayer draw listener [I]); what refreshes layers after a rotation; HP14 passes vacuously when no "window created: resting=false" line exists (research/BN5_V0813_REVIEW_FOLLOWUPS_2026-11-09.md:16); the unlocked profile has no patch BY DECISION (Tj 2026-11-23: keep the Screenshot button there).

## Log lines that show the home patch state
HOMEPATCH: "away: a finger went down" (TAS 1040), "away: another window" (842, 867), "unlock window ignored" (849), "guard lifted" (882), "stale: the Launcher drew" (1031), "patch built, fresh=... step N [(while the menu is open)]" (1103), "layer copy failed" (1096), "crop corrupt / could not be loaded / no longer covers" (989-1002), calibration lines. HOMEPRESS "menu opened (home): fresh patch | patch not fresh, shown as it is...: <why> | no usable patch, the card covers the whole panel: <why>" (955; STALE / FULL_CARD also persisted via `logPatch` 4776). "PATCH fast open" / "PATCH fast skip: <reason>" (141, 153); "PATCH window created / removed / rest on|off"; "PATCH fade start lag= / fade end home first=". Debug only: "HOMEPATCH draw burst" (1020). Status text `homeStatus()` 714-718 / `homePatchInfo` 1263.

## Constants that bound readiness
first-build quiet 100, rebuild / rest quiet 250 (HPR 21, 367), +20 post (TAS 1054), finger 700 (+30) / 900, foreign guard 2500 (+40), unlock grace 3000, recheck 400, focus tolerance 450, restRecheck 400 / 1600, offset settle 200, layer refresh 150 + 250, "not at rest" retries 10 x 400, load fail 6 x 5000, copy fail 5 x 1500, no-window refresh ask 2000, calibration 2100 trigger / 3000 touch / 1500 draw / 5000 screen-on / 60000 gap / 6 per day / hide 150 / abort 4500, stale rebuild after open 350, prime 1200 start, 60 s gap.
