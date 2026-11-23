# CX1a (A): the lock-screen patch lifecycle — read-only audit, 2026-11-23 (v0.9.18 code)

Source: a read-only subagent, neutral brief. TAS = TjAccessibilityService.java, PR = PatchRest.java, LS = LockShade.java. [V] read in code, [I] inferred / platform behaviour. Tj's ask (CX1 point 1): "the wallpaper patch often lights up and shows when I pull up the pin pad. The wallpaper patch should be prepared immediately whenever the lock screen is on so the screenshot button is always hidden."

## Fixed facts
The resting patch paints CONSTANT real pixels (`restDim(true)` = 1, TAS 573 / 1554) in a TYPE_ACCESSIBILITY_OVERLAY above the keyguard. Its brightness is only ever binary at rest (alpha hard 0 / 1, TAS 4868); it is NEVER tied to a pull. Time to rest: window made in the dark = about 20 ms (TAS 1587 + PR 212); made lit = 1220 ms (PR.SETTLE_MS 1155-1220). After unlock the window is removed about 400 ms after USER_PRESENT (TAS 3053 -> 1472) and rebuilt at the next SCREEN_OFF. With no / failed crop it never rests.

## State table
- Window `patchView`: CREATED hidden (alpha 0, TAS 1587) by `addPatch` TAS 1496 via `refreshLayers` 1467-1483. Callers: `lockPrepare` 1890 (SCREEN_OFF +700 and +6500, plus 0 / +250 with pocket return, 3047-3049; SCREEN_ON 1914), `layerRefresh` at every screen on / off 3018 and +400 after SCREEN_ON / USER_PRESENT 3051 / 3053, `scheduleLayerRefresh` on each SystemUI burst 673-678 (150 ms throttle + 250 trailing), `onServiceConnected` 335. Needs `patchTarget` 1275-1283: fresh `keyguardLocked()`, switch and cover mode, learned geometry `cachedCard()` for THIS rotation, stored crop or readable wallpaper. REMOVED by `removePatch` 1641: target null (unlocked), rect moved > 2 px / rotation or flavour differs, wallpaper broadcast 619-621, new crop stored 5750, teardown; it zeroes `touchAwayUntilMs` and `restableSinceMs` (1646).
- Crop: loaded inside `addPatch` 1502 (key wallpaper id + rotation + size). Made by `maybeAutoCalibrate` 4996 (AutoCalibration 42-59: screen on, locked, no pad, no menu, geometry known, 20 s gap, 3 per lock, 8 per day; 25 s window), triggered at restRecheck +400 / +1600 (241-248), `autoCalTrigger` +3000 after lockPrepare (1922), retry 5000; then autoShot +2500, ingest +900, `ingestShot` 5707-5750 rebuilds the window.
- `patchRestable` = `PR.visible` (PR 22-26) = switch && keyguardLocked && !calibrating && crop present && !lastBouncer. `restableSinceMs` gate `PR.SETTLE_MS` 1200 (screen dark = settled at once).
- `patchResting` = vis && !menuOpen (4864). vis = can && now >= `pinHiddenUntilMs` && now >= `touchAwayUntilMs` && settled && now >= shade hold (4860). `settleRun` (492) is re-armed at `PR.returnInMs` + 20 by every `patchShow` that leaves a restable patch invisible (4861), except while the panel is open.
- `touchAwayUntilMs`: `onOutsideTouch` 4734 (ACTION_OUTSIDE via FLAG_WATCH_OUTSIDE_TOUCH, 1574 / 1581): alpha 0 at once, hold +1500 (434), `touchBack` +1520 (493: fresh pad scan, shade look, `patchShow`). Ignored if no window, menu open or pinHide (4736). Cleared by menu open, SCREEN_OFF (3040).
- `pinHiddenUntilMs` / `lastPadSignMs`: `pinPadCameUp` 4707 (keyguard PIN / Password / Pattern / SecurityContainer / HostView event 4789, or pad scan 3097): alpha 0, hold +2500, `padBack` +2600 (waits 1200 ms after the last sign, then a fresh scan; 501). Ignored while dark.
- `lastBouncer`: `updateLockNumber` 3088-3097 (scan 3114-3135) at every screen broadcast and +400 and per event (60 ms lead + 120 trailing); false at SCREEN_OFF 3035. Pad leaves: `padLeft` 446 hold +1500, `padBack`, `padCancelCheck` +720 (needs 400 ms since the last sign, still locked, no USER_PRESENT).
- `lockShadeOpen` / `closedMs`: `noteShade` 4686 from pane-title events (4646; LS 22-26) and looks. Open: alpha 0, NO timer (LS 49). Close: back after 400 ms. Survives screen off; cleared at USER_PRESENT.
- Open menu: `fastMenuOpen` 128 on class "ActionsDialog". `fastDecline` 161 declines only if: no window, not lock flavour, [not resting AND (not restable OR pad up OR a pad sign under 1500 ms)], already open, or geometry unknown. Otherwise the node scan `evaluateScan` 3905 takes 100-560 ms. Fade: PatchFade 300 / 10 / 600 ms.

## The twelve situations (what the code does)
1. Screen on, menu at once: window exists from SCREEN_OFF, resting; the patch opens at the dialog event (scans deferred 330 ms); entry hidden from the first patch frame (12-19 ms on the tablet per CG1D). Window absent or lit-made: away up to 1220 ms but still accepted by the fast path = 1-2 frame ghost. Not restable (no crop, calibrating, pad up): slow scan, the REAL entry shows 100-560 ms.
2. Slow pull, halfway, released: DOWN -> alpha 0 (4744), hold 1500. The pad sign arrives 136-203 ms later by scan on the tablet. Release back: `padLeft` if a sign happened, otherwise `touchBack` at +1520 runs a scan and shows the patch with HARD alpha 1. After a sign the fast path declines for 1500 ms and the real entry shows 100-560 ms.
3. Fling completes the pad, then BACK / swipe down: DOWN hide, sign hold 2500, `lastBouncer` true so no timer; pad leaves: hold 1500, cancel check 720; returns at the latest of the holds. A menu while the pad is up is declined -> slow scan.
4. Pad up 2 s, screen off, on: SCREEN_OFF zeroes holds and `lastBouncer` and shows the patch alpha 1 IN THE DARK; on wake it rests at once. If the pad still shows after wake the patch stays alpha 1 until the +0 / +400 scan [I whether the pad survives screen off].
5. Shade pull: DOWN hides; the pane title "Notification shade." / "Quick settings." (LS 23) gives alpha 0 with no timer; "Lock screen." brings it back after 400 ms. A half pull with no title: only `touchBack` at +1520 (pane look). Titles depend on SystemUI wording [I for MediaTek].
6. Quick settings: same path (title "Quick settings."); proven on the emulator only.
7. Tap on the lock screen: DOWN -> alpha 0 for 1520 ms, then back. A menu inside that window is covered by the fast path.
8. Rotation: `onConfigurationChanged` does not touch the patch (3591-3608): the old window keeps its old rect and crop until the next refresh (150 / 250 ms after window events); a new rotation needs its own geometry and crop; without them no window and the first menu goes via scan.
9. Menu opened, BACK, opened again: BACK sends no click and no close broadcast; the patch stays at the dimmed OPEN look during the stock 300 ms fade-out until the window-removal event (`dialogGone` ignores the first 800 ms); a dimmer block than the brightening surround. Re-open at once: dying-dialog rule 450 ms.
10. Wallpaper changed while locked: receiver removes and rebuilds the window; new sig -> no crop -> not restable -> fast path declines -> slow scan, REAL entry. Calibration about 6 s total [I].
11. Camera / assistant swipe: DOWN hides 1520 ms, then `touchBack` shows alpha 1 again; no foreground-app check exists in the rest path: while an app is over the keyguard the patch rests over it until the next DOWN [V no check].
12. Notification / call: notification events return early; lock-screen notifications are switched off by LockQuiet; a full-screen call UI behaves like 11.

## GAPS (the Screenshot entry visible, or the patch lights up)
- G1 The hide at DOWN runs on the main thread behind node scans (avg 28, max 563 ms, tablet v0.9.1) [V mechanism, I frequency].
- G2 DOWN unseen when no window exists (4736): during the rebuild after wallpaper / ingest / rect / rotation change, a service rebind (uiautomator dump), and before first creation.
- G3 `touchBack` at DOWN + 1520 is CLOCK-based and knows nothing of the finger (4745, 493-498): it does only a pad scan, then hard alpha 1 at constant pixels. A slow or hesitant pull longer than 1.5 s shows a LIGHTER BLOCK over the partly dimmed wallpaper until the next sign [V mechanism, I whether the scan finds a half-shown pad]. THIS IS THE LIKELY "lights up when I pull up the PIN pad".
- G4 Pad scan false negative (`isVisibleToUser`, 3126) -> the `padLeft` path -> return after 720-1500 ms over a pad that is still up.
- G5 `removePatch` zeroes the finger hold (1646): a rebuild during a pull gives a window that rests after settle, or at once if made dark.
- G6 Fling: the only early signal is DOWN; the first pad sign is a scan later (136-203 ms). If DOWN was missed the patch is bright until then.
- G7 Shade depends on pane titles; a half pull has no title change; only the 1500 ms DOWN hold protects it.
- G8 App over the keyguard (camera, assistant, call): the patch rests over the app.
- G9 Rotation: stale window and crop until the next refresh; an unlearned rotation -> slow path.
- G10 Wallpaper change or failed calibration: no crop -> the entry visible 100-560 ms on EVERY press; limits 3 per lock, 20 s gap, 8 per day; the counter resets at unlock.
- G11 BACK-closed menu: the patch stays at the dimmed open look through the stock fade-out. Also one empty scan after 2000 ms of an open menu calls `hideOverlay`: a bright patch over the dimmed screen.
- G12 A pad over an open menu: pinHide, alpha 0, the real entry shows for the rest of the stock fade-out (4717-4725).
- G13 After SCREEN_OFF the patch is alpha 1 in the dark; the wake's keyguard fade-in may show it as a block [I].
- G14 Lit creation (lock after timeout where the lockPrepare timers are uptime clocks, restart, rotation): a 1220 ms away window; the fast path covers it at the event only (1-2 frame ghost).
- G15 The stored crop is per wallpaper signature, not per lock: content that differs from it (moving or changing content under the slot while the pad slides in) shows as a block [I].
- G16 Calibration with the helper hides a resting patch at least 250 ms (helper normally off).

## Not determinable from the code
Whether ACTION_OUTSIDE reaches the patch on the tablet for every pull start (edge / system-gesture start, multi-touch; emulator-proven only, BE1); the timing of the stock bouncer / shade scrim against DOWN / first MOVE on MediaTek SystemUI; whether the bouncer survives screen off; MediaTek pane titles; whether `isKeyguardLocked()` stays true with the camera / a call over the keyguard; uptime-timer behaviour in deep sleep; the actual DOWN-to-alpha-0 delay and stall frequency on the tablet (newest tablet report v0.9.1); whether the lock screen rotates at all on this tablet; whether an alpha 0 window is drawn by the RenderThread (cold first draw).
