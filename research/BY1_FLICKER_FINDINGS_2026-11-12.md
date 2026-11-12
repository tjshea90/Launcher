# BY1 flicker films: what the emulator showed (2026-11-12, Claude, account 2 after the takeover)

Tj, 2026-11-12 17:26 UTC (TASKS BY1): "It should cover the screenshot button without flicker or delay, whether from the first lock screen or
after pulling down the notification panel or pulling up the pin pad. It should not flicker or flash either the screenshot button or the
wallpaper patch at any time".

Section `fl` (design: `BY1_FLICKER_TEST_PLAN_2026-11-12.md`) films each case with animations ON and measures every frame with
`tools/fl_analyze.py`. Contact sheets (`fl-sheet-<film>.png`, a thumbnail every 0.1 s, 10 per row = one row per second; red frame = flash
of the patch, yellow = Screenshot entry visible, green = stock menu open) go to the branch `ci-shots` with `shots=true`.

## Run #563 (id 43273214570, fcc27808): unreadable, two ANALYZER bugs (not the product)
1. `screenrecord` writes a frame only when the screen CHANGES: a 10 s film ended at its LAST change (3-5 s). `--ref last` (the median of the
   last 400 ms) was the middle of the last animation, so 6 of 7 menu films found "no menu" and the wake films showed false flashes at the end.
   Fix: the analyzer pads the end with the last frame for 1 s (`tpad`), so REF = the settled end state; `setpts` pins frame n at n / fps.
2. The boxes have odd sizes (core 99 x 83). Cropping the yuv420p video rounds them to the chroma grid, so the raw frames were NOT w x h bytes
   and the stream was cut at the wrong places (shifted pixels). Fix: `format=gray` BEFORE `crop`.
Both pinned by `tools/test_fl_analyze.sh` (a synthetic variable-frame-rate film that ends at its last change; old code: 72 / 124 frames,
spans shifted; new: flashA 78, 139 frames, A1.00-2.30).

## Run #564 (id 43294929647, b3b0c4d7, fixed analyzer): 17 PASS, 2 FAIL, all looked at on the sheets
- **shade (F1, FAIL):** the notification panel (white quick settings on the emulator) is pulled down by a finger; the patch hides at the
  finger's DOWN, comes back ~1.9 s later (`touchBack`, LOCK_TOUCH_AWAY_MS 1.5 s) ON TOP of the still open panel and stays there for 2.3 s
  (A3.03-5.37) until the panel is closed: a block of lock-screen wallpaper (the slot + the bottom extra, a stepped shape) over the panel.
  Nothing in the lock flavour knows about the panel.
- **menuP1 (F3, FAIL):** the power menu opened ~1 s after the PIN pad was dismissed with BACK: the Screenshot entry is visible in 2 thumbnails
  (167 ms, S0.72-0.88) while the menu fades in, then the patch covers it. The patch was still AWAY for the pad's hold (every pad sign holds it
  2.5 s; `padLeft` 1.5 s) and only the dialog's own window event (fast path, about two frames late) brought it back. menuP2 passed (timing).
- **menuPU (diagnostic, menu over the OPEN PIN pad):** 2 thumbnails of the Screenshot entry, then the patch shows the LOCK-screen wallpaper
  (bright orange) over the DARK PIN-pad background for the whole menu: a wrong picture, not a match. Open question for Tj: does he open the
  power menu while the PIN pad is up?
- **wakeH1 / wakeH2 / wakeL (first lock screen after the screen comes on):** no flash on the emulator (the lock screen appears in one step;
  the patch has been restable since the screen went off, so it rests from the first frame). F2 (a keyguard fade-in from black at wake) is NOT
  seen here; the tablet may differ (a slower SystemUI). menuW1 / menuW2 (menu ~0.5 s after the wake): covered from the first frame.
- **pad, tap, menuL, menuS1, menuS2:** clean (0 flash frames, 0 Screenshot frames).

## Fixes (BY1c)
- F3 (pad cancel): `PatchRest.padCancelled` (pure, 5 JVM tests) + `padCancelCheck` in the service: a pad that LEFT while the screen stays
  locked, with no USER_PRESENT since, no new pad sign for 400 ms and no pad in a fresh look, was CANCELLED: the pad hold ends 700 ms after the
  departure (not 2.5 s after the last sign). An unlock's USER_PRESENT follows the departure by about 270 ms on the tablet (BT1b), so 700 ms
  keeps the BT1b protection. Log line `PATCH pin pad cancelled`.
- F1 (shade): `LockShade` (pure, 5 JVM tests) + `lockShadeEvent` / `noteShade` / `shadeLevel` in the service. Probe run #565 proved the signal:
  SystemUI's notification panel is an accessibility pane; TYPE_WINDOW_STATE_CHANGED with CONTENT_CHANGE_TYPE_PANE_TITLE (0x8) says
  "Quick settings." when the panel is open and "Lock screen." at the END of the collapse (also "Notification shade." / "Quick settings editor.").
  While open the lock patch is away (no timer); after "Lock screen." it may rest again 400 ms later (`LockShade.SETTLE_MS`; the scrim still fades
  about 90 ms after the event). A level look (the panel node's pane title, one id search) runs at the finger return and in `lockPrepare`.

## Run #566 (id 43302204570, 6973c044, F1 + F3): 18 PASS, 1 FAIL
- shade: flashA 138 -> **0** (path: shade open, rest off, shade closed, rest on). New diagnostic films: a slow 2.6 s pull (`shadeSlow`): 0 / 0.
- pad: 0 / 0, path "pin pad cancelled"; menuP1 / menuP2 (menu right after the pad): Screenshot frames **0** (were 10 / 9); menuS1 / menuS2: 0.
- menuPU (menu over the OPEN pad, diagnostic): the Screenshot entry shows for the whole menu (the patch stays away over the pad, BC5). Question for Tj.
- **FAIL menuW1** (the first menu after a wake, as in #565): flash A for 3 frames (50 ms, 14.6 levels) just before the menu is detected: the stock
  scrim darkens around the slot before the patch dims (a lag). menuW2 (same sequence) is clean. This fits Tj's "first lock screen". Next run prints
  the patch's own timeline per film (`FL <film> seq:`, including "fade end lock first=N ms").
- **New gap found by the `shadeOff` picture**: the quick-settings panel stays OPEN across a screen-off; the first F1 version reset its state at
  SCREEN_OFF and the patch rested over the open panel at the wake. Fixed (F1b): the state is kept across the screen-off and `lockPrepare` (while
  dark) reads the panel's real state before the patch is shown again.
