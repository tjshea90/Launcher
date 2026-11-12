# Performance review, slice C: background work and battery (2026-11-12)

Reviewer C, read-only (TASKS BY1a). **Scope:** receivers, observers, timers and heartbeats, the shell helper, RecentsSweep, the holds (battery saver, radio icons, banners, animations), ReturnReset, KeyboardSwitch, and every file or preference the app appends to. Other slices: A covers startup and the drawer, B the accessibility service's own per-event work and overlays, D memory. C1-C4 and C7 sit in TjAccessibilityService.java, slice B's file, but they cover lock, unlock, heartbeat and report-file work, not overlay work. C5 overlaps slice A.

**Evidence.** I read the code at HEAD; nothing was built or run. Sources:
- `TR` = research/tablet/2026-11-10-2/tablet-report.txt
- `TR9` = research/tablet/2026-11-09/tablet-report.txt
- "(AOSP)" = Android 10 platform behaviour stated from AOSP knowledge; I did not re-read the source for this review, and it is unverified on the tablet.
- Emulator section names come from tools/gate-map.txt.

**What the tablet says (this sets the priorities)**
- **No shell helper.** It is NOT running on the tablet: TR:620, the repeated "not reachable" lines at TR:907-911, and HLP1 is still open (TASKS.md:449). Everything that needs the helper (RecentsSweep's removals, ShellServer, HelperLife) is idle there today; the app-only fallbacks are the live paths.
- **Event volume.** 45521 accessibility events in 121 min, 38643 of them noise (TR:629).
- **Size and start.** PSS is 69 MB (TR:627) and onCreate takes 48 ms (TR:628).
- **Usage access.** PACKAGE_USAGE_STATS is reported as not granted (TR:612). The app-op state is unverified.
- **Nothing in this slice wakes or holds the CPU while the screen is off.** There is no AlarmManager, JobScheduler, WorkManager or wake lock anywhere in app/src/main/java (grep). Every periodic job uses Handler delays or Thread.sleep, and neither advances while the device is suspended (AOSP).
- **Where the cost really is:** CPU and IPC while the screen is on, and main-thread stalls on the lock and unlock paths. The service and Launcher3 share one main thread (TjAccessibilityService.java:38).

## Findings at a glance

| id | sev | where | one line |
| :- | :- | :- | :- |
| C1 | HIGH | RecentsFilter.java:117, 134-157 | every app's label is scanned on the MAIN thread when Recents opens (switch default ON, :24) |
| C2 | MEDIUM | TjAccessibilityService.java:76 | noise events drive the 2 Hz policy heartbeat: about 6 binder calls per tick in Quickstep |
| C3 | MEDIUM | TjAccessibilityService.java:1786, 1861 | a helper ping (join up to 700 ms) and a usage query run eagerly on the main thread, at lock and at every Enter tap |
| C4 | MEDIUM | TjAccessibilityService.java:2827-2831 | lockPrepare runs 3 times per lock and redoes all its work each time |
| C5 | MEDIUM | SettingsSpoof.java:110-114 | every mode change toggles a component AND forces a full Launcher3 model reload |
| C6 | MEDIUM | Mode.java:76-80 | keyboard, tiles, immersive and saver IPC plus prefs commits run on the main thread at every mode-changing unlock |
| C7 | MEDIUM | Features.java:28-36 | status and diagnostic strings use commit() (whole file + fsync), several on the main thread |
| C8 | LOW | ReturnReset.java:317-351 | target lookup: one PackageManager IPC per window change, a full label rescan every 10 min |
| C9 | LOW | ShellServer.java:148-169, 236-255 | up to 3 `dumpsys` forks per clean-up (helper only) |
| C10 | LOW | ShellServer.java:408-422 | the logcat watcher reads every main / system / crash line (helper only) |
| C11 | LOW | BatterySaverGuard.java:43-51 | the saver observer runs its body on the main thread, twice per switch-off |
| C12 | LOW | CrashGuard.java:92-95 and others | getSharedPreferences is looked up again on every call on per-event paths |
| C13 | LOW | TjHooks.java:88-89 | 1 + 2n task binder calls on every Launcher resume |

**Suggested order:**
1. Measure first: a debug-only lock-log line with main-thread ms and IPC counts per lock cycle and per unlock.
2. C1.
3. C6 + C7 (the unlock path).
4. C3, then C4, C2, C5.
5. The LOW findings as clean-up in the same files.

## C1 HIGH: Recents label scan on the main thread
- **Where:**
  - RecentsFilter.java:112-131 (`run`), :117 (`if (now - blockedAt > LABELS_TTL_MS) blocked = blockedNow(svc)`), :134-157 (`blockedNow`).
  - :26 (60 s TTL) and :35 (first look 60 ms after the Recents window).
  - The handler is the service's main-looper handler (TjAccessibilityService.java:38, passed in at :237).
- **How often:** the switch is ON by default (:24). In the custom launcher (`wanted`, :44-46) the first look after Recents opens rebuilds the blocked-label set if the last build is older than 60 s, which in practice is nearly every time Recents opens.
- **What it does:**
  - `blockedNow` calls queryIntentActivities.
  - Then, for each launchable app: HiddenApps.isPackageShown, `ri.loadLabel(pm)` and `pm.getApplicationLabel`.
  - Then the labels of the apps opened in Quickstep.
  - It runs whether or not the helper is up; RecentsSweep is requested as well (:100).
- **Cost:**
  - Each label load is PackageManager IPC plus resource loading, and this scan does two per app.
  - A scan with ONE label per app took 349 ms for 53 apps on the tablet (TR9:888, ReturnReset.scanTarget). This one does more work per app; its tablet time is unverified.
  - It blocks the thread that draws Launcher3's Recents transition.
  - It delays the very look that closes the hidden apps' cards, so those cards stay on screen longer.
- **Change:** keep the set warm and build it off the main thread.
  - Build it on a worker when the service connects and after each unlock.
  - Mark it stale on package add / remove / change, on a locale change, on HiddenApps edits and on changes to AppOrigin's Quickstep set. Labels change only with those events, so correctness does not need the 60 s TTL.
  - In `run`, use the set already built. If it is stale, rebuild it on the worker and look again when it lands. The set of cards closed stays the same.
- **Risk / verify:**
  - Risk: an app installed seconds ago could survive one look. A slow scan has the same effect today.
  - JVM test of the pure "stale? → rebuild" rule.
  - Lock-log INFO line with the build's ms and thread name.
  - Emulator `rg` (AP1) and `cs`.

## C2 MEDIUM: noise events drive the policy heartbeat
- **Where:**
  - TjAccessibilityService.java:76 calls `AnimationSpeedup.tick(this)` BEFORE the noise filter at :81-85.
  - AnimationSpeedup.java:163-171 (`tick`): a 500 ms gate; in Quickstep with any switch on it calls `syncSoon`.
  - :151-157: the worker HandlerThread.
  - :198-233: `syncNow`, static synchronized (NotifQuiet.sync, RadioIconHide.sync, the animation hold).
- **How often:**
  - Every accessibility event reaches it: about 6 a second on the tablet, 85 % of them content changes of other apps (TR:629; EventNoise.java:19-22).
  - In Quickstep, any app that keeps updating its content (video, progress bar, clock, game) keeps the heartbeat at 2 syncs a second while it is on screen: up to 7200 an hour.
- **Cost:**
  - Each sync is a no-op in steady state, because the holds write nothing when the state is already right (AnimationHolds.java:43-88, NotifQuietHold.java:34-61, RadioIconHold.java:172-199).
  - It still makes about 6 binder calls:
    - Up to 4 checkSelfPermission calls (NotifQuiet.java:33 via :70; RadioIconHide.java:42; AnimationSpeedup.java:38 via :53 and :209). On Android 10 these go to ActivityManager with no client cache (AOSP).
    - isInteractive and isKeyguardLocked (AnimationSpeedup.java:262-269).
    - Plus Settings reads and prefs lookups (C12).
  - The main-thread syncs take the same class lock (Mode.java:72, AnimationSpeedup.java:132, 141-148), so an unlock can wait behind a worker sync that is running.
- **Change:**
  - Call `tick` only for window events (`EventNoise.windowsJustChanged`, EventNoise.java:45-47), or move it below the noise filter. Content changes of other apps change none of the facts the policy reads: mode, screen, keyguard, helper.
  - In `syncNow`, read the WRITE_SECURE_SETTINGS grant once and pass it to the three holds.
  - Optionally, cache the screen and keyguard state from the broadcasts the service already receives (lockReceiver, :2804-2838).
- **Risk / verify:**
  - Risk: a fact that changes with no window event would be re-read later. The other level triggers stay: Mode.set, screen off/on, unlock decided, helper connect/unbind.
  - Emulator `an`, `nq`, `wf`, `y2`.
  - A debug counter of syncs per minute in the lock log, before and after.

## C3 MEDIUM: helper ping and usage query run eagerly on the main thread
- **Where:**
  - TjAccessibilityService.java:1861 (coverAtEnter) evaluates `ReturnReset.mayHaveWork(app)` as an argument.
  - :1786 (lockPrepare) evaluates `ReturnReset.invisibleReady(app)` as an argument.
  - The rules that receive these values often do not need them (ProfileSession.java:43-55; for example `plainUnlockEndsQuickstep` = `!customMode && !automationOff`).
  - `invisibleReady` (ReturnReset.java:87-96, 4 s cache) calls `ShellHelper.reachable()`. That starts a thread and joins it for up to 700 ms (ShellHelper.java:64-70). The connect tries the unix socket, then TCP ports 47631-47635 (ShellHelper.java:27-58, ShellProtocol.java:22). Failures are logged (ShellHelper.java:109-114; TR:907-911).
  - `mayHaveWork` (ReturnReset.java:400-403) → `recentsNeededCheap` (:62-65) → `AppOrigin.pending` → `mergeUsage` (AppOrigin.java:151, 63-91). That is a UsageStatsManager query with only a 1 s guard (:69).
- **How often:**
  - :1861 runs on every plain Enter tap of the PIN pad: every unlock, in both modes.
  - :1786 runs at every lockPrepare (3 per lock, C4).
  - Each pings whenever the 4 s cache is stale.
- **Cost:**
  - With no helper (the tablet), every ping is a thread start plus failed connects, on the main thread.
  - The failed connects are normally fast refusals; the worst case is the full 700 ms join. How long a refusal takes on the tablet is unverified.
  - The usage query sits between the finger lift and the cover / first frame, despite the name "Cheap".
- **Change:**
  - Evaluate lazily: pass a supplier, or put the rule's cheap conditions first so they short-circuit.
  - Keep one "helper ready" value. Refresh it on a worker at screen-off and when the helper connects or fails; the failure reset already exists (ReturnReset.java:150, :178). The main thread only reads it.
  - For the Enter tap, use the cached `pending(custom, quickstep)` sets. Merge usage on the lock worker; `AppOrigin.refreshUsedCacheAsync` (AppOrigin.java:120-129) already runs at lock (:1797).
- **Risk / verify:**
  - Risk: a stale "ready" could pick the invisible path just after the helper died. That path already falls back on failure (:150, :178).
  - The ProfileSession JVM tests must keep their truth tables.
  - Emulator `uf`, `cs`, `cs2`.
  - Lock-log ms from the Enter tap to `cover shown`.

## C4 MEDIUM: lockPrepare runs three times per lock
- **Where:**
  - TjAccessibilityService.java:2827-2829: `lockPrepareLater` at +700 ms and +6500 ms after SCREEN_OFF.
  - :2831 → :1804: SCREEN_ON → `handleLockedWake` → `lockPrepare`.
  - The body is at :1776-1801.
- **How often:** every screen-off: two runs while dark, one at the wake.
- **What each run repeats:**
  - `ProcessLife.noteLock` (:1783): `getRunningAppProcesses` on the main thread when its 60 s sample is due (ProcessLife.java:52, 119-139).
  - `lockQuietNow` (:1784 → :1807-1813): two new threads (LockQuiet.onLockAsync, BatterySaverGuard.onLockAsync), then `applyLockTiles` on the main thread (:1819-1826; TileProfile.java:89 commits).
  - The helper ping (C3).
  - `RecentsSweep.requestAsync` in custom mode (:1796).
  - `AppOrigin.refreshUsedCacheAsync` (:1797): a new thread plus a usage query.
  - `refreshLayers` (:1798).
- **Cost:**
  - The wake run sits between the power button and the first lock-screen frame. TR:722-725 shows about 14 ms of main-thread work at the wake (LOCKPREP 45.680 → TILES 45.694).
  - The two dark runs each cost thread starts, IPC and prefs rewrites (C7).
- **Change:**
  - Keep all three triggers; they cover missed broadcasts.
  - Record, per lock (a lock sequence number), which parts succeeded. The later runs redo only parts that failed or whose input changed: the mode, the helper state, or a tiles value that really differs (read it before applying).
  - Take the ProcessLife sample once per lock, on a worker.
- **Risk / verify:**
  - Risk: a part that half-failed in run 1 must still be retried, so record success per part, not as one flag.
  - JVM test of the pure "what to redo" rule.
  - Emulator `o`, `ae`, `bl`, `wf`, `t`.
  - Count the LOCKPREP / TILES lines per lock in the lock log.

## C5 MEDIUM: the Settings alias forces a full Launcher3 reload at every mode change
- **Where:**
  - Mode.java:73 → SettingsSpoof.java:89-96 (`syncSoon`, 3 s delay, :34) → :102-120 (`syncNow`).
  - `syncNow` calls `setComponentEnabledSetting` at :110, then posts `HiddenApps.reloadLauncher` at :114 → HiddenApps.java:102-105 → `LauncherModel.forceReload()` (launcher3/src/com/android/launcher3/LauncherModel.java:309-334).
  - forceReload clears mModelLoaded and runs a full LoaderTask (workspace, all apps, deep shortcuts, widgets), then a full rebind.
- **How often:** `aliasWanted` = switch on AND custom mode (:48). So it flips at every Quickstep entry and every return: two full reloads per round trip (release default ON).
- **Cost:**
  - A whole model load and rebind about 3 s after the custom home appears, when Tj may already be using it.
  - The component change alone already makes the system send PACKAGE_CHANGED for our package, and stock Launcher3 handles that as a per-package update (AOSP). Whether this fork's HiddenApps filter needs more is unverified.
- **Change:**
  - Drop forceReload if PACKAGE_CHANGED alone adds or removes the entry; otherwise run a per-package update for our own package.
  - While in Quickstep, skip the reload entirely (our drawer is not on screen) and check the entry on return.
- **Risk / verify:**
  - Risk: zero or two "Settings" entries in the drawer.
  - Emulator `ss`: exactly one entry after each mode change.
  - A lock-log line for each reload.

## C6 MEDIUM: Mode.set does the slow profile switches on the main thread
- **Where:** Mode.java:63-82, called from the unlock body on the main thread (TjAccessibilityService.java:1894 QUICKSTEP, :1913 CUSTOM, and :1790). In order:
  - The tj_mode commit (:71) and AnimationSpeedup.syncNow (:72).
  - TaskOrigin.sessionStart (:76).
  - KeyboardSwitch.apply (:77). It does:
    - checkSelfPermission (KeyboardSwitch.java:27).
    - getEnabledInputMethodList IPC (:38).
    - 2 getSharedPreferences calls (:44).
    - A Settings read, write and read-back (:43, :56, :57).
    - `say()` = a Features commit + Log.i, even for "already" (:30, :55). The string contains the mode, so it is a real write at every change.
  - TileProfile.apply (:78): a keyguard binder call (TileProfile.java:72-74) and an unconditional APPLIED_KEY commit (:89).
  - ImmersiveProfile.apply (:79) and BatterySaverGuard.check (:80).
- **How often:** every unlock that changes the mode. The keyboard switch is on, on the tablet (TR:675-676).
- **Cost:**
  - Several IPCs and at least two synchronous whole-file prefs writes, each with an fsync on the calling thread (AOSP).
  - All of it runs on the thread that must draw the first frame after the unlock.
  - The reset took 912 ms on the tablet (TR:704); how much of that is Mode.set is unverified.
- **Change:**
  - On the calling thread, keep only the tj_mode commit (the router reads it) and the animation sync.
  - Run KeyboardSwitch, TileProfile, ImmersiveProfile and BatterySaverGuard.check, in the same order, on one serial background executor. The design already accepts a late tiles apply (re-applied after 1.5 s, :1829-1840).
  - Write status strings with `setStringAsync`, and only when they differ.
- **Risk / verify:**
  - Risk: a very fast first key press could reach the previous keyboard. Unverified; today the switch also lands after the frame.
  - Emulator `legacy` with `base` (it prints `keyboard: before=... afterCustom=...`), and `t`.
  - KeyboardPolicyTest.
  - Lock-log ms of the unlock body, before and after.

## C7 MEDIUM: diagnostics written with commit()
- **Where:** Features.java:28-36 (`set` / `setString` / `setInt` all use `commit()`).
  - Main-thread callers:
    - profileNote (TjAccessibilityService.java:1718-1721), called at :1731 (handshake Enter tap), :1763 (prearm) and :1791.
    - finishReset writes RESET_REPORT_KEY BEFORE `done.run()` (:4014-4021).
    - appendReport commits once per line (:4032-4035).
    - KeyboardSwitch.java:30, TileProfile.java:89, RecentsFilter.java:124.
  - Worker callers, at every lock and unlock: BatterySaverGuard.java:135, 214, 278-279, 290-291; LockQuiet.java:120, 123, 158.
- **How often:** about 8-15 whole-file writes per lock/unlock cycle. This is my count from the call sites; unverified, a debug counter would confirm it.
- **Cost:**
  - Each write that changes a value serialises all of tj_features.xml and fsyncs on the calling thread. On eMMC that is typically several to tens of ms (unverified on this tablet).
  - A commit also waits for any apply() still in flight (AOSP).
  - On the main thread it delays the unlock frame; finishReset even holds the cover removal behind it.
  - Every write also wakes the prefWatch listener (:530-543).
- **Change:**
  - Status and diagnostic strings → `setStringAsync` / `apply()`. They are read by the report, never by the router after a kill.
  - Compare before writing.
  - In finishReset, call `done.run()` first, then write the report.
  - Collect the appendReport lines and write them once.
  - Keep `commit()` only for state that must survive an immediate kill (the mode, the holds' originals).
  - Optional: a separate prefs file for diagnostics keeps the state file small. ReportState reads `Features.all` (Features.java:41), so it would then read both files.
- **Risk / verify:**
  - Risk: a diagnostic line written milliseconds before a process death can be lost.
  - Emulator `x`: the report sections still show their lines.
  - StrictMode disk-write detection in the debug build.

## C8 LOW: ReturnReset.targetPackage on frequent paths
- **Where:**
  - ReturnReset.java:317-334: an `isInstalled` getApplicationInfo IPC on EVERY call (:323, :336-338).
  - A full rescan every 10 min, or every 30 s when there is no target (:324).
  - `scanTarget` (:341-351) loads one label per launchable app and commits at :348.
- **Callers:**
  - trackDirty, for every window-state change in Quickstep (TjAccessibilityService.java:1990).
  - updateHotspot, on every foreground change (:3168-3175).
  - AppOrigin.java:76, RecentsGuard.java:58 and :1968.
  - RecentsPolicy.java:30, evaluated eagerly even when its switch is off (default off, :22).
- **Cost:**
  - One IPC per window change, plus a 349 ms scan (TR9:888) every 10 min of use, on whichever thread asks (the main thread for trackDirty and updateHotspot).
  - With clean start (release default) no target app is opened at all.
- **Change:**
  - Cache the installed flag together with the target.
  - Rescan only on package add / remove / change, not by TTL.
  - In RecentsPolicy, evaluate the target only when its switch is on.
- **Risk / verify:**
  - Risk: none in practice; every install or removal sends a package event.
  - JVM test of the cache rule.
  - Emulator `cs`, `uf`, `legacy`.

## C9 LOW: dumpsys forked up to three times per clean-up (helper only)
- **Where:**
  - ShellServer.java:148-169: LISTTASKS; RMTASKS dumps again.
  - :236-242: `removeRecentTasks` dumps again.
  - :244-255: `recentsDump` forks `dumpsys activity recents`.
  - :276-289: `runShell` starts a thread plus `sh` per command.
  - RecentsSweep.java:183 (listTasks), then :188 (removeTasks).
- **How often:** every sweep (lock, plain unlock, Recents open, a hidden app leaving the screen), but only while the helper runs. That is not the case on the tablet today (TR:620).
- **Cost:** each dump is a fork + exec + full recents dump: tens of ms of CPU on this SoC (unverified).
- **Change:**
  - Let RMTASKS reuse a dump less than about 1 s old, or pass the parsed task list along.
  - Deduplicate the sweep requests of the three lockPrepare runs (C4).
- **Risk / verify:**
  - Risk: a task created in that second is missed once; the next level-triggered sweep catches it.
  - Emulator `cs2`, `cs`.
  - Count the forks in the helper log.

## C10 LOW: the HLP1 logcat watcher reads every log line (helper only)
- **Where:**
  - ShellServer.java:408-422: `logcat -b main -b system -b crash -v threadtime -T 1`, one regex per line (:405), stops after 400 matches.
  - The BEAT thread writes the flight recorder every 30 s (HelperLife.java:18-19). `rec` opens the file, appends and closes it (ShellServer.java:386-403).
- **How often:** continuously, while the helper runs.
- **Cost:** every log line of every app is copied into the helper JVM and regex-matched. The cap limits matches, not lines read.
- **Change:**
  - Let logcat filter first: tag filters such as `-s ActivityManager:I`, or `-e <regex>` (Android 10's logcat has `-e`; unverified for this build).
  - Keep the Java regex as the second stage.
  - Remove the watcher once HLP1 is answered (TASKS.md:449, 469, 481).
- **Risk / verify:**
  - Risk: a filter that is too narrow misses a kill line.
  - Check that the flight recorder still records a forced kill.
  - No gate section covers HelperLife (tools/gate-map.txt), so this needs a manual run.

## C11 LOW: the battery-saver observer body runs on the main thread
- **Where:** BatterySaverGuard.java:43-51, the onChange body on the main looper. It:
  - writes LockLog;
  - reads Settings and Mode;
  - calls `powerSource`, a sticky registerReceiver IPC (:379-390);
  - starts a thread and runs `check`.
- **How often:** every low_power change; it fires twice per switch-off, 4 ms apart (TR:726-727). The tablet counts 93 manual saver activations (TR:642).
- **Change:** post the body to a worker, and fold two changes within about 50 ms into one.
- **Risk / verify:**
  - Risk: the reaction to the saver comes a few ms later.
  - Emulator `bs`, `bl`.

## C12 LOW: preference handles looked up on every call
- **Where:**
  - CrashGuard.java:92-95: `automationOff`, read on many paths, including the tile and radio observers (TjAccessibilityService.java:3343, 3407).
  - AnimationSpeedup.java:42: runs on every tick (:168).
  - NotifQuiet and RadioIconHide.
  - HiddenApps.java:22: `allowed()` also copies the set (:32-40), and `isPackageShown` (:73-78) runs per app in C1 and per notification.
  - AppOrigin.java:18, RecentsDirty.java:16, KeyboardSwitch.java:44.
- **How often:** per accessibility event (via tick, C2), per notification, per app in C1.
- **Cost:** a global lock and map lookups per call (AOSP). Only microseconds each, but on per-event paths.
- **Change:**
  - Cache one handle per file, as Features.java:19-25 and Mode.java:47-53 already do.
  - Test membership without copying the set.
- **Risk / verify:**
  - Risk: none; it is the same object.
  - The unit tests stay unchanged.

## C13 LOW: task binder calls on every Launcher resume
- **Where:**
  - TjHooks.java:88-89 → RecentsGuard.java:25-35: `getAppTasks`, then `getTaskInfo` + `setExcludeFromRecents` for each task.
  - RecentsGuard.java:53-73: `describeAsTarget`, which uses targetPackage (C8).
- **How often:** every Launcher.onResume: every Home press and every return to the home screen.
- **Cost:** 1 + 2n binder calls (n = our tasks, usually 1-2) on the main thread, during the resume that draws the home screen.
- **Change:**
  - Remember the task ids already flagged; redo only for a new id.
  - Call describeAsTarget only when the target changes.
- **Risk / verify:**
  - Risk: a new task always has a new id (AOSP), so nothing is missed.
  - Emulator `legacy` (Recents flags, EXCLUDE_FROM_RECENTS).

## Checked and FINE
- No AlarmManager, JobScheduler, WorkManager or WakeLock in app code (grep of app/src/main/java). The helper's BEAT thread holds no wake lock (ShellServer.java:354-363).
- The manifest receivers are Launcher3's standard ones (AndroidManifest.xml:83-105).
- The service's receivers and observers are each registered once and unregistered as a pair:
  - lock receiver (TjAccessibilityService.java:2840-2857)
  - patch watch (:563-580)
  - close-dialogs receiver (:552-561)
  - tile guard (:3355-3365)
  - radio guard (:3428-3441)
- The radio receiver checks `keepsRadiosOn` first (:3386). The lock-radio chain runs only while a radio is on (:3404-3426).
- The MediaStore observer is live only during a calibration (:4917-4943).
- These register once or in pairs:
  - BatterySaverGuard's observer (BatterySaverGuard.java:38-61)
  - EntranceWatcher.java:45-61
  - KeepAliveService.java:59-66, 101-119 (off on the tablet, TR:622)
  - TjApplication.java:16-34
  - RestoreStore, which snapshots once (RestoreStore.java:21-26)
- The look-alike pages register only while they are visible: SsWatcher.java:28-35, SsHomeSummary.java:44-55, SsPagesBattery.java:119/126, SsPagesSound.java:185-200.
- Launcher3's registrations are paired: Launcher.java:377/1462, LauncherAppState.java:100/141, ConfigMonitor.java:77/135, WallpaperOffsetInterpolator.java:164-168.
- WallpaperManagerCompat takes the VOMR1 path on API 29 (WallpaperManagerCompat.java:37/43); its JobScheduler variant is unused.
- The holds write nothing when the state is unchanged: AnimationHolds.java:43-88, NotifQuietHold.java:34-61, RadioIconHold.java:172-199.
- These are bounded:
  - RadioIconHide.offThenSync: 4 s (RadioIconHide.java:153-170); deferRelease is an explicit state (:125-139).
  - AnimationSpeedup: 2 follow-ups per unlock (:146-147) and a 10 s failsafe (:100).
  - LockLog: a 48/32 KB ring, written on one background executor (LockLog.java:17-18, 51-77).
  - HelperLife: 48/24 KB (HelperLife.java:19).
  - DIRTY lines: written only on change and rate-limited (RecentsDirty.java:18, 22-31; TjAccessibilityService.java:1997).
- NotifSightings uses a background executor, apply(), and a rate-limited log (NotifSightings.java:113-152).
- Radio writes happen only when a radio is on (ReturnReset.java:292-304, LockQuiet.java:115-127).
- Coalescing works:
  - RecentsSweep merges requests (RecentsSweep.java:139-177).
  - SettingsSpoof.syncSoon is coalesced and does nothing when already in state (SettingsSpoof.java:89-96, 109).
- apply() is used where it is frequent: ProcessLife.noteStart (ProcessLife.java:111-116), Mode.recordHomePress (Mode.java:20-24).
- TabletReport and the helper's SHOTS / APKS / PROBE commands run only on demand (TabletReport.java:195-236).

**Not verified in this review:**
- How long a refused helper connect takes on the tablet (C3).
- The fsync cost of the tablet's storage (C6, C7).
- Whether PACKAGE_USAGE_STATS is really denied (TR:612).
- The real number of prefs writes per lock (C7).

One debug counter line in the lock log answers all four before any fix.
