# BQ3 - the pencil cover that stays up after the panel closes on the lock screen (2026-11-10)

Tj's BQ1b / BN8b / BN4 ("a white box on the lock screen when I slide the notification panel back up") is REPRODUCED on the CI emulator, deterministically, in CUSTOM mode too.

## Runs (emulator.yml, section b6 unless noted)
- #507 (42792069234) head 6878395: 16 PASS 2 FAIL (BK1b quickstep: lock screen P2 `cover=shown`, control `cover=shown`).
- #509 (42792080486) TAG v0.9.1, b6,n: 24 PASS 4 FAIL (same 2 + N2 x2) = not a v0.9.2 regression; the v0.9.1 gate (#501, 01:44Z) had b6 green (pencil at y=286 there; y=564 in today's runs).
- #510 (42795642177) head + debug probe + new check BQ3 (cover must be gone 2 s after `cmd statusbar collapse` on the lock screen): BQ3 FAILS in CUSTOM mode (cover=shown after the 1st and 2nd closing) and in quickstep. BN8b failed once (cover none after the uiautomator dump): flake, it passed in #511.
- #511 (42797400107) head + tree sketch + insideItsWindow: 16 PASS 4 FAIL (BQ3 custom, BK1b quickstep P2, BQ3 quickstep, control). The full INFO text: job log saved by the extraction agent in /root/.claude/projects/-home-user-Launcher/f57c5b9c-bf62-5be7-8f0c-bd772cf12bbe/tool-results/mcp-github-get_job_logs-1794306018529.txt (211 BQ3 lines; /tmp/scratch_full_BQ3.txt if it still exists).

## Facts
1. The followed pencil node (and all its ancestors up to quick_settings_container) keeps reading `isVisibleToUser = true` with the OPEN panel's bounds [4,564][55,615] while the panel is CLOSED on the lock screen (window is full screen there). Accessibility visibility is therefore NOT a reliable "the panel is open" signal on the lock screen. (In the unlocked shade the window shrinks to 26 px and the node is lost: BN4's hide works there.)
2. Quickstep phase first sight (probe, #510): at the POWER key the node's window was `type=3 layer=1 [0,0][600,26]` (not active, screen=false) yet vis=true [4,564][55,615]; 70 ms later the window was `[0,0][600,976] act foc` and the node still vis=true. `insideItsWindow` (pencil must lie inside its own window) rejects the first sight but not the later ones.
3. Tree sketch of the shade window (custom, panel just OPENED on the lock screen, #511): V = visible, h = hidden: keyguard_status_view V235-367, clock_view V235-316 (still visible while the panel is open!), qs_frame V0-976, quick_settings_container V0-615, header V0-187, quick_settings_panel V51-564, qs_footer V564-615, edit V564-615, notification_stack_scroller V0-944; hidden: keyguard_indication_text, quick_qs_status_icons, centered_icon_area, qs_drag_handle_view, date, statusIcons. So the keyguard clock is NOT (at least not yet at +1.5 s) a closed/open discriminator. NEXT: compare with the CLOSED-state tree (after `BQ3 marker collapse 1`).
4. v0.9.2 BQ1b (finger down hides the cover, FingerGuard brings it back when the node reads visible) cannot fix this alone: the stale node says visible again, so the cover returns. The root fix needs an independent open/closed signal.

## Ideas for the discriminator (to test with the sketch lines of "C closed" vs "B open")
- a node that flips V/h between open and closed on the lock screen (quick_settings_panel? header? qs_drag_handle_view? keyguard clock alpha?);
- node bounds that differ (the closed lock screen may report the same bounds, then not usable);
- fall back to the old behaviour when the discriminator node is not found (no regression on another SystemUI).

## State of the code (branch ccr-5ef85f7a-ngtgj1)
Debug-only: `penProbe` / `penTree` in TjAccessibilityService (log lines `PENCIL probe`, `PENCIL tree`), scenario b6 samples BK_Z1..3, check BQ3, `bq3_pick` (BUG: its first-probe branch does not stop after one probe: add a done flag). Real: `insideItsWindow` in findPencil / findPencilFull.
