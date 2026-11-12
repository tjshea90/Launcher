# Performance review 2026-11-12 (TASKS BY1a): merged findings and verdicts

Tj, 2026-11-12 17:26 UTC: "Do a full review of the launcher for optimizations in code and to make it load and run as smoothly and fast as possible with
minimal lag and battery waste and minimal ram usage without sacrificing any speed or function".

Four read-only reviewers, one slice each, neutral briefs, nothing built or run by them. Full texts (file:line, cost, change, risk, how to verify):
- `PERF_REVIEW_2026-11-12_A.md`: start-up, Launcher3 load, the drawer, the hooks in launcher3/ (no HIGH).
- `PERF_REVIEW_2026-11-12_B.md`: the accessibility service's per-event work and its overlays (3 HIGH).
- `PERF_REVIEW_2026-11-12_C.md`: background work, battery, holds, helper, files (1 HIGH).
- `PERF_REVIEW_2026-11-12_D.md`: memory (no HIGH, no leak, no unbounded growth).

## What the tablet reports already say (the scale of the problem)
- Main thread blocked by accessibility tree searches (reviewer B, report 2026-11-10-2): 984 power-menu scans (avg 28 ms, max 563 ms) and 834 pencil
  searches (avg 33 ms, max 1305 ms) in 121 minutes = about 55 s of main-thread blocking. The service and Launcher3 share ONE main thread, so every
  one of those milliseconds is a frame the home screen or the lock-screen patch cannot draw. This is the biggest lag item in the whole review.
- 45521 accessibility events in 121 min, 38643 of them noise (other apps' content changes).
- PSS 69 MB; 4.9 MB of it is a full-size copy of the wallpaper pinned in WallpaperManager's static cache (D1), 2.4 MB a home curtain kept in custom mode (D2).
- No wake lock, alarm, job or WorkManager anywhere: nothing wakes the CPU with the screen off (C). Battery cost is CPU + IPC while the screen is on.
- Process starts: 2 a day; Application.onCreate 48 ms (A). Cold start is not where the time goes; per-event and per-unlock work is.

## Verdicts
FIX = in v0.9.8 (BY1); NEXT = worth doing, needs its own emulator proof or tablet data first; NO = not worth the risk or the work.

| id | sev | verdict | why |
| :- | :- | :- | :- |
| B1 | HIGH | FIX | the power-menu scan also walks the foreground APP's windows after any window event or click; the menu is always a system window |
| B2 | HIGH | FIX | the pencil search walks the whole SystemUI tree on every SystemUI event; skip windows that cannot hold the quick-settings footer |
| B3 | HIGH | FIX (part) | the PIN-pad check can run 6 id + 5 text searches per SystemUI window; count it first, then cut |
| B4 | MED | FIX | binder calls (power, keyguard) on every event although nothing is on screen and the screen is not locked |
| C1 | HIGH | FIX | Recents label scan (every app's label) on the main thread when Recents opens |
| C2 | MED | FIX | noise events drive the 2 Hz policy heartbeat (about 6 binder calls a tick in Quickstep) |
| A4 | MED | FIX | a package-manager call (and sometimes a label scan) at every app launch for a switch that is off |
| A3 | MED | FIX | a prefs write + fsync on the main thread at every Home press |
| A2 / C13 | MED / LOW | FIX | task binder calls + a bitmap upload on every Launcher resume, before its first frame |
| C6 / C7 / B6 | MED | FIX (status strings only) | whole-file prefs writes with fsync on the unlock path for diagnostic strings |
| D1 | MED | FIX if verified | 4.9 MB wallpaper copy kept after the crop is stored |
| C3 | MED | NEXT | eager helper ping / usage query at lock and at the Enter tap; the helper is not running on the tablet (HLP1), measure the ping first |
| C4 | MED | NEXT | lockPrepare runs 3 times per lock; per-part success bookkeeping needs its own section run (o, ae, bl, wf, t) |
| A1 / C5 | MED | NEXT | a full Launcher3 reload 3 s after every mode change; the targeted update risks 0 or 2 Settings entries, needs section `ss` + a round trip |
| D2 / D3 | MED | NEXT | the home curtain's memory and its full-window copy; the curtain is lock-safety code (BL1), its own section first |
| B5, B7, B8 | MED / LOW | NEXT | layer refresh binder calls, home patch window at the first frame, PickerGuard per event |
| A5 | LOW | NEXT | onCreate 48 ms, 2 starts a day: small gain |
| A6, C12 | LOW | NEXT | prefs handle lookups and set copies: microseconds per call |
| C8 | LOW | FIX (part, with A4) | the eager evaluation in RecentsPolicy; the per-window-change lookup stays for now |
| C9, C10 | LOW | NO (for now) | helper-only paths; the helper does not run on the tablet (HLP1 open) |
| C11 | LOW | NEXT | battery-saver observer body on the main thread, twice per switch-off |
| A8 | LOW | NO | notification dots off: a component change for a few binder calls at model creation |
| A9 | LOW | NO | costs RAM for a rare cold start |
| B9-B15, D4-D8 | LOW | NEXT | small clean-ups in files touched anyway |

## Status (updated as the fixes land)
- Flicker fixes (BY1c), proven by emulator films (section `fl`): F1 the patch stays away while the notification panel is open on the lock screen
  (pane titles, `LockShade`); F3 a cancelled PIN pad ends its hold 700 ms after it left (`PatchRest.padCancelled`). Details and numbers:
  `BY1_FLICKER_FINDINGS_2026-11-12.md`.
- Perf fixes: in progress (this file is updated with each one's test and run).
