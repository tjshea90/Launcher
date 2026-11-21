# CT1 / SR2: the stock launcher's Force stop WITHOUT the shell helper (Tj, 2026-11-21)

Tags: [M] measured (run id named), [V] verified in this repo's code / tests, [I] reasoned, not measured on the tablet.

## Why
Tj: "There is already a fix to force stop quickstep, but it requires the adb helper which i don't have." SR1 (`StockRefresh`, v0.9.14) restarts the stock launcher through the helper only. Cause of the growth, from earlier work
(`CG1F_QUICKSTEP_AOSP_ANGLE`, `CG1F_CONFIG_CHANGES`, `CI1_AUDIT`): the Go `IconRecentsView` pins every NEW Activity of `com.android.launcher3` for the life of its process [V, AOSP code read]; our own battery-saver flips around
every lock re-create the visible stock Activity [I]; the process lives at adj 100, so an app cannot kill it, and an app uid cannot read its size (`dumpsys meminfo` refused, runs #621-#623).

## What it does (`StockForceStop` rules + `TjAccessibilityService.stockForceStop` drive)
1. **Debt** (an estimate, because nothing can read the size without the helper): +1 per change of `Settings.Global low_power` (BatterySaverGuard's observer, ours or the person's) and +1 per ended Quickstep session (`Mode.set`).
2. **Start** (`StockForceStop.blocker`, pure): switch on (release ON, debug OFF), fewer than 2 failures, the shell helper NOT running (SR1's exact reading wins there), debt >= 12, custom mode, custom home in front, screen on and unlocked, automation not
   paused, >= 4 h since the last drive. Checked 45 s after a Quickstep session ends. The service then waits (at most 12 s) for a quiet home (no menu, no cover, no calibration, no prime, no floating view).
3. **Drive** (`StockForceStop.step`, pure; the service executes it every 120 ms): full-screen cover up (cover failsafe re-armed to 15.5 s), Settings' App info page of `com.android.launcher3`
   (`ACTION_APPLICATION_DETAILS_SETTINGS`, package `com.android.settings`, `NEW_TASK | CLEAR_TASK | EXCLUDE_FROM_RECENTS | NO_ANIMATION`), press the "Force stop" button, then the dialog's OK (`android:id/button1`, only when the dialog's title says
   "force stop"), then BACK, BACK, HOME until our own window is in front. Whole run capped at 14 s.
4. **Outcomes**: restarted / the button stays grey for 2.5 s (the process is not running: nothing to give back, counts as done) / failed (a page, button or dialog never came) / aborted (screen off, locked, Quickstep became active: nothing pressed on a lock screen).
   Done clears the debt; two failures in a row switch it off until the person flips the switch; an abort is retried after 30 min and counts no failure.
5. **Switches / views**: Launcher control row (release ON) + a "Force stop the stock launcher now" button (no debt / cooldown rule, quiet-moment rules still hold); the tablet report line "stock launcher Force stop without the helper (CT1 / SR2)".

## What was proven and what was not
- [V] `StockForceStopTest` (22 tests): every blocker reason and its order, cooldown, retry after a failure, the debt, the start verdict, the whole drive (happy path, grey button, no page, retry once, dialog stays, unsafe screen, cap, leaving, nothing pressed on a lock screen).
- [M] emulator section `sf` (see TASKS CT1c for the run id): the real AOSP Settings, the real stock launcher process: pid before / after, lock log, status, custom home back, Settings not in Recents, Recents key still works, cooldown, no crash.
- [I] NOT provable on the CI emulator: that the MediaTek Settings of Tj's tablet has the same labels ("Force stop", "OK") and ids; that the emulator's plain AOSP stock launcher is small, so the memory GAIN is the tablet's to show (the tablet report line says "debt N of 12" and the last result).
- A Settings with different wording makes the drive FAIL SAFE: no button within 7 s = failure (nothing pressed), two failures = off. It never presses anything but a node whose text is "Force stop" and a dialog whose title contains "force stop".

## Limits, so nobody re-derives them
- It cannot know the real size: a healthy stock launcher can be restarted once per 4 h at worst (about one second of cold start the next time Recents / Quickstep opens, SystemUI re-binds by itself).
- The cover hides Settings; a slow tablet may still show a flash between the cover going up and the page covering it (the cover is raised first on purpose).
- With the helper running SR1 decides (its reading is exact); SR2 steps aside.
