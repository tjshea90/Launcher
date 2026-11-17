# CG1f / CG3d — which of OUR writes make the STOCK launcher recreate its Activity (session A, 2026-11-17 22:20Z; read-only audit by grep + reading)

Tags: [V] verified in this repo's code, [I] inferred from AOSP behaviour / earlier lessons, [?] unknown, needs a measurement.

## Why it matters
[V, research/CG1F_QUICKSTEP_AOSP_ANGLE_2026-11-17.md] the Go `IconRecentsView` registers a listener in the process-wide `RecentsModel` in its constructor and nothing removes it:
every NEW Activity of the stock launcher (`com.android.launcher3`) stays pinned for the life of the process. The process cannot be killed by an app and lives at adj 100.
So the growth Tj sees is per ACTIVITY RE-CREATION. [I] Launcher3 (Android 10) lists `keyboard|keyboardHidden|mcc|mnc|navigation|orientation|screenSize|screenLayout|smallestScreenSize` in configChanges,
NOT `uiMode`, `fontScale`, `density`, `locale`: a change of any of those RELAUNCHES the activity (AOSP `ActivityRecord.ensureActivityConfiguration`: a stopped, invisible activity is relaunched too, `relaunchActivityLocked(false, ...)`).

## Every write in `app/src/main` that can change a Configuration (grep: FONT_SCALE, setNightMode, ui_night_mode, UiModeManager, USER_ROTATION, ACCELEROMETER_ROTATION, display_density_forced, wm density/size, updateConfiguration, low_power, setPowerSaveMode, dark_theme, force_dark, night_display)
No code in the app calls `UiModeManager`, writes `ui_night_mode`, a locale, or `wm size`. The candidates:

1. **Battery saver flips (the prime suspect)** [V] writes of `Settings.Global low_power` in `BatterySaverGuard`:
   - `onLock` -> `switchOffForLock` -> `writeOff` (line ~95): at EVERY lock moment (screen off while locked, or the first locked wake) the lock screen gets battery saver OFF if it was ON (BA2, `lockOffEnabled`). One flip.
   - `onUnlock` gesture into Quickstep (line ~277): `low_power = 1` again if Quickstep had it on. A second flip. So one lock + gesture-unlock cycle with the saver on in Quickstep = TWO flips.
   - `check(... "the custom launcher became active")` from `Mode.set` (Mode.java ~113): switches it off whenever the custom launcher takes over (AC3). Heal writes (lines ~347, ~367): `low_power = 1` after a swallowed tap, keep-off while locked (capped).
   - [I from lessons, v0.8.3] on Tj's tablet the saver flips the DARK THEME on and off ("one slot per theme" lesson: `ScrimMeasure` re-measured on every flip). AOSP 10: `UiModeManagerService` follows the battery-saver night-mode policy, so each flip is a `uiMode` configuration change for EVERY process. The stock launcher's activity is relaunched (+1 pinned Activity) at every flip.
   - [?] Frequency on the tablet: the lock log has `BATSAVER low_power is now N` lines (`BatterySaverGuard` line ~47), the tablet report prints the lock log tail. Count them per day.
2. Rotation [V]: `EntranceWatcher` (line ~79, the flick entrance) and `SsPagesDisplay` (lines ~121-125, user action in the look-alike) write `ACCELEROMETER_ROTATION` / `USER_ROTATION`. A rotation change is in the stock launcher's `configChanges` (orientation, screenSize): no relaunch. Not a suspect.
3. Font size / display size [V]: `SsPagesSize` (line ~66 `FONT_SCALE`, `wm density` at ~126): only when Tj changes them on the look-alike Settings page. `fontScale` and `density` ARE relaunch triggers, but only by hand. Not a suspect for slow growth.
4. Not config changes, but extra stock activity [V/I]: our `RouterActivity` start of the stock HOME on every switch to Quickstep (`startRouter("stock")`, the stray-custom guard's redirect, the return tiles). They resume an EXISTING activity unless Android destroyed it; no leak per start unless the Activity was recreated.

## What to measure (cheap, decisive)
- Emulator section `qf` (not written): drive battery saver ON/OFF N=6 times through the REAL Settings screen the way section `bs` does, read the stock launcher with the SHELL uid before and after
  (`dumpsys meminfo com.android.launcher3`: `Activities:` and `TOTAL`; B's `qm` section already prints that INFO line), and a control run of the same length with no flips. If Activities rise by about N per N flips, the mechanism is proven.
  Check first that the CI image's battery saver changes `uiMode` at all (`dumpsys uimode` / `dumpsys activity | grep -i uiMode`): lesson 2026-11-07 says `cmd uimode night` does nothing on the image.
- Tablet: count `BATSAVER` lines in the lock log over a day (report) and read the Quickstep memory ring (the recorder needs the shell helper: an app uid cannot read `dumpsys meminfo`).
- A/B on the tablet: Tj switches the lock-screen battery saver rule off for a day (Launcher control: "lock off") and compares the growth.

## What could be done about it (nothing built yet; Tj decides)
- Fewer flips: keep the saver as it is across a lock when the dark theme is the only reason (not possible if the lock screen needs the light look), or skip the restore when the next unlock is within seconds.
- Make the relaunch cheap: an opt-in idle restart of the stock launcher while the screen is off and locked (`am force-stop com.android.launcher3` through the helper; Tj does this by hand and the tablet survives it) at most once per N hours and only above a PSS threshold. Needs the helper allowlist (`ShellProtocol.allowed` denies it today) and a proof that SystemUI rebinds cleanly.

## Limits of the recorder (QsMemory, review C of v0.9.12; read before trusting a number)
- An APP uid cannot read `dumpsys meminfo com.android.launcher3` (SELinux: "Can't find service: meminfo", emulator runs #621-#623): a reading needs the shell helper (command STOCKMEM, answered through the abstract unix socket first, loopback TCP second). A helper started by an OLDER app version answers `ERR unknown command`: the status line says so in plain words (start the helper again).
- [?] `dumpsys meminfo <pkg>` asks the target process to run a garbage collection before it counts (AOSP `ActivityThread.dumpMemInfo`, from memory, not verified): the Java-heap and PSS parts of a reading are probably post-GC, so a growth that is only uncollected garbage can be hidden. The signal that matters for R1 is the object COUNT (Activities / Views survive a GC when pinned), which the reading names first.
- The recorder looks at most every 25 minutes, only while the tablet is in use (30 s after a wake, when Launcher control opens, when a report is written); a look that gave nothing is not repeated for 2 hours.

