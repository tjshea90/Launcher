# CX1a (C): the pencil cover — read-only audit, 2026-11-23

Source: a read-only subagent with a neutral brief (v0.9.18 code). TAS = TjAccessibilityService.java, PR = PencilRules.java. [V] read in code, [I] inferred. Tj's ask: "make sure the pencil is hidden as well as possible without a flickering white square cover".

## How it works today
- ONE window "TjPencilCover", TYPE_ACCESSIBILITY_OVERLAY, size = the pencil's bounds (<400 px a side), FLAG_NOT_FOCUSABLE | LAYOUT_IN_SCREEN | LAYOUT_NO_LIMITS plus FLAG_WATCH_OUTSIDE_TOUCH (shown) or FLAG_NOT_TOUCHABLE (hidden), TRANSLUCENT, TOP|START [V TAS 2703-2712]. Content: a plain View, `setBackgroundColor(Color.WHITE)` [V 2731-2732]. Hidden = view alpha 0 + `updateViewLayout` to NOT_TOUCHABLE at the old bounds; the window stays attached [V 2778-2779]. A second transparent touchable window "TjGearOverlay" rides on it (draws nothing; unlocked custom home only) [V 2912-2921].
- Wanted when: `coverWorld()` (custom mode OR keyguard locked), switch `hide_qs_pencil` (default ON), no demo profile [V 681, 2522, 2541]. Unlocked Quickstep: never covered by design.
- Triggers: accessibility events windowStateChanged | windowsChanged | windowContentChanged | viewClicked | notificationStateChanged, notificationTimeout 0. A SystemUI event runs `schedulePencil`: a look (`updatePencil`) at most every 40 ms; a full tree search only if >= 40 ms (>= 200 ms for content changes) since the last; a `penEval` is always booked 90 ms later [TAS 2555-2559, EventNoise 76].
- A look = `findPencil` [2649]: refresh the kept node (one binder call); if lost: HIDE first, then a whole-tree search of the tall SystemUI windows for `android:id/edit` [2672-2698]. Show / follow = `showPencilCover` [2721-2746]. `penWatch` re-looks a shown cover every 150 ms, or every 25 ms for 600 ms after a trigger [PR 34, 136-137; TAS 2617-2638]; it stops while the screen is dark.
- PencilRules constants: SlideGuard SLIDE_UP_PX 3, HOLD_MAX_MS 3000, STALE_MS 800, MIN_LOOK_GAP_MS 40, RELOOK_MS 60 (cover held away while the top edge rises). FingerGuard HOLD_MS 350, QUIET_MS 200, MAX_MS 3000 (finger down anywhere = ACTION_OUTSIDE hides the cover at once, a look at +350 ms). staleLook: a look slower than 150 ms neither shows nor moves the cover (3 in a row, the 4th is accepted). B3 (v0.9.18): on the HOME flavour a FULL search with no cover and no known node waits for the burst's quiet work (<= 140 ms).
- Hides: finger down, sliding up, followed pencil gone, node gone, none found. Removal: world change, switch off, onInterrupt / onUnbind / onDestroy, any exception.

## Colour and flicker sources
- Colour: fixed opaque WHITE; nothing reads the panel's pixels, theme, wallpaper or alpha [V 2732]. The dark theme's panel is not white (TASKS BK1b2, open, never answered); no code handles panel alpha while pulled, the brightness slider or rotation.
1. The cover lags a moving pencil (position from a look: binder refresh; tree search avg 33 ms, max 1305 ms on the tablet).
2. A stale place: a slow look is rejected only 3 times, then used.
3. Slow hide path: the cover leaves only after a look or a finger-down; a drag with no finger-down reaching the cover depends on looks (Tj's BN4 / BN8b / BQ1b "white box on the lock screen").
4. Hide and show are each TWO steps (alpha, then binder layout) on a busy main thread [I].
5. Show / hide churn with no hysteresis: a transient refresh failure hides at once and a search re-shows 30-100 ms later = a toggle; finger-down hide / 350 ms hold / re-show; SlideGuard hold / release.
6. Cold window: nothing is pre-created; after every removal the next show is a `wm.addView` at the first sighting ("cover shown (new window)").
7. uiautomator dump / service restart removes the window; the dead instance's window lingers in `dumpsys window` for seconds.
8. The cover survives SCREEN_OFF (no hide call, penWatch paused): the first look after wake takes up to ~150 ms, a white box can show for that long if the panel is gone [V code, I effect].
9. Nothing hides the cover when the stock power dialog opens over an open panel.

## Gaps where the REAL pencil shows (longest the code allows)
- First show per panel opening: look + search (emulator 75-78 ms after the panel opened; tablet unmeasured) + a spacing wait up to 200 ms with no node known + search up to ~1.3 s + stale rejects.
- Finger down on the open panel: pencil visible >= 350 ms (+ 200 ms quiet), up to 3 s on a continuous drag.
- Panel sliding up: held away until two looks agree, up to 3 s (the pencil then sits on a white panel).
- A followed pencil lost while the panel is still open: hidden for the whole search (<= 1305 ms seen).
- A panel pulled down: the pencil at its new place is visible for the look lag [I].
- Never covered: unlocked Quickstep, switch off, demo profile, service not connected, window not yet created.

## Logging a test can use
- logcat tag TjA11y, ONLY on transitions: "PENCIL cover shown [l,t][r,b]", "PENCIL cover hidden (kept warm; why, the pencil was last seen N ms ago)", "PENCIL cover removed", "PENCIL fast watch (why)"; debug builds also "PENCIL look took N ms: its place is stale". Repositions leave no line.
- Lock log (lock-events.log, 48 KB ring, event PENCIL), only while locked, 1 per 250 ms per kind. It undercounts fast toggles; logcat does not.
- `dumpsys window` block "TjPencilCover" (NOT_TOUCHABLE = hidden); emulator helpers `pen_blocks`, `pen_sample`, `pen_tl`, `bn8_shown_after_hide`, `bq3_close_verdict` (section b6).

## Alternatives found in the docs (research/PENCIL_REMOVAL_2026-11-10.md, IDEAS_2026-11-04.md s3, VPN_FOOTER_2026-11-13.md)
- `Settings.Global device_demo_mode=1` removes pencil AND gear natively (emulator runs 55 / 57) but was retired 2026-11-05 (retail-demo service may create a demo user at boot: factory-reset risk).
- DISABLE2_QUICK_SETTINGS hides the whole panel, needs the STATUS_BAR signature permission.
- Runtime resource overlay: NOT possible for a sideloaded APK (runs #617 / #618; idmap2 refuses, targetSdk 28, SystemUI declares nothing overlayable).
- Accessibility has no action that hides or resizes another app's view: the cover is the ceiling. No `sysui_qs_tiles` / `icon_blacklist` slot touches the footer.

## Not determined
Tablet timing of the first show; whether the stock footer / panel alpha animates while pulled; the panel's real colour / alpha on the lock screen and in the dark theme; whether battery saver / dark theme occurs where the cover is active; whether the stock power dialog collapses the panel; whether the lock-screen quiet-burst deferral applies to the shade opening; whether a dead instance's lingering window is visible on a real device.
