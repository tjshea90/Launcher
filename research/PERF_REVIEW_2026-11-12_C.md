# Performance review, slice C: background work and battery (2026-11-12)

This is reviewer C's read-only review for TASKS BY1a.

**Scope:**
- receivers, observers, timers and heartbeats
- the shell helper and RecentsSweep
- the holds (battery saver, radio icons, banners, animations)
- ReturnReset and KeyboardSwitch
- every file or preference the app appends to

**Other slices:**
- A: startup and the drawer. C5 overlaps it.
- B: the accessibility service's per-event work and overlays. C1-C4 and C7 are in B's file, TjAccessibilityService.java, but they are lock, unlock, heartbeat and report-file work.
- D: memory.

**Evidence:**
- The code at HEAD; nothing was built or run.
- `TR` = research/tablet/2026-11-10-2/tablet-report.txt; `TR9` = research/tablet/2026-11-09/tablet-report.txt.
- "(AOSP)" = Android 10 platform behaviour from AOSP knowledge, not re-read for this review and unverified on the tablet.
- Section names come from tools/gate-map.txt.

**What the tablet says:**
- **No shell helper:** it is NOT running (TR:620; "not reachable" repeats at TR:907-911; HLP1 is still open, TASKS.md:449). Its paths (RecentsSweep's removals, ShellServer, HelperLife) are idle there; the app-only fallbacks are the paths in use.
- **Event volume:** 45521 accessibility events in 121 min, 38643 of them noise (TR:629).
- **Footprint:** PSS 69 MB (TR:627); onCreate 48 ms (TR:628).
- **Usage stats:** PACKAGE_USAGE_STATS is reported not granted (TR:612). The app-op state is unverified.

**Overall:** nothing in this slice wakes or holds the CPU with the screen off. There is no AlarmManager, JobScheduler, WorkManager or wake lock in app/src/main/java (grep), and Handler delays and Thread.sleep do not advance in suspend (AOSP). The cost is CPU and IPC with the screen on, plus main-thread stalls at lock and unlock. Launcher3 and the service share one main thread (TjAccessibilityService.java:38).

## Findings at a glance

| id | sev | where | one line |
| :- | :- | :- | :- |
| C1 | HIGH | RecentsFilter.java:117, 134-157 | every app's label is scanned on the MAIN thread when Recents opens (switch default ON, :24) |
| C2 | MEDIUM | TjAccessibilityService.java:76 | noise events drive the 2 Hz policy heartbeat: about 6 binder calls per tick in Quickstep |
| C3 | MEDIUM | TjAccessibilityService.java:1786, 1861 | helper ping (join up to 700 ms) and a usage query evaluated eagerly on the main thread at lock and at every Enter tap |
| C4 | MEDIUM | TjAccessibilityService.java:2827-2831 | lockPrepare runs 3 times per lock and redoes all its work each time |
| C5 | MEDIUM | SettingsSpoof.java:110-114 | every mode change toggles a component AND forces a full Launcher3 model reload |
| C6 | MEDIUM | Mode.java:76-80 | keyboard, tiles, immersive and saver IPC plus prefs commits on the main thread at every mode-changing unlock |
| C7 | MEDIUM | Features.java:28-36 | status and diagnostic strings use commit() (whole file + fsync), several on the main thread |
| C8 | LOW | ReturnReset.java:317-351 | target lookup: one PackageManager IPC per window change, full label rescan every 10 min |
| C9 | LOW | ShellServer.java:148-169, 236-255 | up to 3 `dumpsys` forks per clean-up (helper only) |
| C10 | LOW | ShellServer.java:408-422 | logcat watcher reads every main / system / crash line (helper only) |
| C11 | LOW | BatterySaverGuard.java:43-51 | saver observer body on the main thread, twice per switch-off |
| C12 | LOW | CrashGuard.java:92-95 and others | getSharedPreferences looked up on every call on per-event paths |
| C13 | LOW | TjHooks.java:88-89 | 1 + 2n task binder calls on every Launcher resume |

**Suggested order:**
1. Measure first: a debug-only lock-log line with main-thread ms and IPC counts per lock cycle and per unlock.
2. C1.
3. C6 + C7 (the unlock path).
4. C3, then C4, C2 and C5.
5. The LOW findings, as clean-up in the same files.

## C1 HIGH: Recents label scan on the main thread
- **Where:**
  - RecentsFilter.java:112-131 (`run`). At :117, `if (now - blockedAt > LABELS_TTL_MS) blocked = blockedNow(svc)`; `blockedNow` is :134-157.
  - The TTL is 60 s (:26). The first look comes 60 ms after Recents opens (:35).
  - The handler runs on the service's main looper (TjAccessibilityService.java:38, created at :237).
- **How often:** almost every Recents open in the custom launcher. The switch is ON by default (:24; `wanted` at :44-46), and the set is rebuilt whenever it is more than 60 s old. This happens with or without the helper; RecentsSweep is requested as well (:100).
- **Cost:**
  - A rebuild calls queryIntentActivities. Then for each app it calls HiddenApps.isPackageShown, `ri.loadLabel(pm)` and `pm.getApplicationLabel`, and it also loads the labels of the Quickstep-opened apps. Each label load is PackageManager IPC plus resource loading.
  - A scan with one label per app took 349 ms for 53 apps on the tablet (TR9:888, ReturnReset.scanTarget). This scan does more work per app; its own time is unverified.
  - It blocks the thread that draws the Recents transition, and it delays the look that closes the hidden apps' cards.
- **Change:** keep the set warm and build it off the main thread.
  - Build it on a worker at service connect and after each unlock.
  - Mark it stale on package add / remove / change, a locale change, HiddenApps edits and changes to AppOrigin's Quickstep set. Labels only change then, so no TTL is needed.
  - `run` uses the current set. If the set is stale, the worker rebuilds it and the look runs again. The same cards get closed.
- **Risk / verify:**
  - Risk: an app installed seconds ago can survive one look. A slow scan does the same today.
  - Verify: a JVM test of the pure "stale → rebuild" rule; an INFO line with the build's ms and thread; emulator `rg` (AP1) and `cs`.

## C2 MEDIUM: noise events drive the policy heartbeat
- **Where:**
  - TjAccessibilityService.java:76 calls `AnimationSpeedup.tick(this)` BEFORE the noise filter (:81-85).
  - `tick` (AnimationSpeedup.java:163-171) has a 500 ms gate. In Quickstep, with any switch on, it calls `syncSoon` on a worker (:151-157).
  - `syncSoon` runs `syncNow` (:198-233, static synchronized): NotifQuiet.sync, RadioIconHide.sync and the animation hold.
- **How often:** about 6 events a second on the tablet, 85 % of them content changes from other apps (TR:629; EventNoise.java:19-22). In Quickstep, an app that keeps updating (video, progress bar, clock) holds it at 2 syncs a second: up to 7200 an hour.
- **Cost:**
  - Each sync is a no-op in steady state: the holds write nothing when unchanged (AnimationHolds.java:43-88, NotifQuietHold.java:34-61, RadioIconHold.java:172-199).
  - It still makes about 6 binder calls:
    - Up to 4 checkSelfPermission calls (NotifQuiet.java:33 via :70; RadioIconHide.java:42; AnimationSpeedup.java:38 via :53 and :209). Android 10 has no client-side cache for these (AOSP).
    - isInteractive and isKeyguardLocked (:262-269).
  - On top come Settings reads and the prefs lookups in C12.
  - The main-thread syncs (Mode.java:72; AnimationSpeedup.java:132, 141-148) take the same class lock, so an unlock can wait behind a running worker sync.
- **Change:**
  - Call `tick` only for window events (`EventNoise.windowsJustChanged`, EventNoise.java:45-47), or after the filter. Other apps' content changes alter no fact the policy reads (mode, screen, keyguard, helper).
  - Read the permission grant once per `syncNow` and pass it to the three holds.
  - Optional: cache the screen and keyguard state from the broadcasts the service already receives (:2804-2838).
- **Risk / verify:**
  - Risk: a fact that changes without a window event is re-read later. The other level triggers stay (Mode.set, screen off/on, unlock decided, helper connect/unbind).
  - Verify: emulator `an`, `nq`, `wf`, `y2`; a debug count of syncs per minute.

## C3 MEDIUM: helper ping and usage query evaluated eagerly on the main thread
- **Where:** two calls are evaluated as arguments, even when the receiving rules do not need them (ProfileSession.java:43-55; for example `plainUnlockEndsQuickstep` = `!customMode && !automationOff`):
  - `ReturnReset.mayHaveWork(app)` in coverAtEnter (TjAccessibilityService.java:1861). It runs `recentsNeededCheap` (ReturnReset.java:400-403, 62-65), then `AppOrigin.pending`, then `mergeUsage` (AppOrigin.java:151, 63-91): a UsageStats query behind only a 1 s guard (:69).
  - `ReturnReset.invisibleReady(app)` in lockPrepare (:1786). It has a 4 s cache (ReturnReset.java:87-96), then calls `ShellHelper.reachable()`. That starts a thread and joins it for up to 700 ms (ShellHelper.java:64-70). The thread tries the unix socket, then ports 47631-47635 (:27-58; ShellProtocol.java:22), and logs each failure (:109-114; TR:907-911).
- **How often:** :1861 runs on every plain Enter tap of the PIN pad, in both modes. :1786 runs at every lockPrepare, 3 per lock (C4). Each pings whenever its 4 s cache is stale.
- **Cost:**
  - Without the helper, each ping is a thread start plus failed connects, on the main thread. A refused connect is normally fast; the worst case is 700 ms. The refusal time on the tablet is unverified.
  - The usage query runs between the finger lift and the first frame.
- **Change:**
  - Evaluate lazily: pass a supplier, or check the cheap conditions first.
  - Keep one "helper ready" value. Refresh it on a worker at screen-off and on helper connect or failure; the failure reset already exists at :150 and :178. The main thread only reads it.
  - At the Enter tap, use the cached `pending(custom, quickstep)`.
  - Merge usage on the lock worker. `refreshUsedCacheAsync` (AppOrigin.java:120-129) already runs there (:1797).
- **Risk / verify:**
  - Risk: a stale "ready" could pick the invisible path just after the helper died. That path already falls back on failure (:150, :178).
  - Verify: the ProfileSession JVM truth tables must stay unchanged; emulator `uf`, `cs`, `cs2`; the ms from the Enter tap to `cover shown`.

## C4 MEDIUM: lockPrepare runs three times per lock
- **Where:** `lockPrepareLater` at +700 ms and +6500 ms after SCREEN_OFF (TjAccessibilityService.java:2827-2829). SCREEN_ON runs `handleLockedWake` (:2831), which calls `lockPrepare` (:1804). The body is at :1776-1801.
- **How often:** on every screen-off: twice while the screen is dark, once at the wake.
- **Cost:** each run repeats all of this:
  - `ProcessLife.noteLock` (:1783): `getRunningAppProcesses` on the main thread when its 60 s sample is due (ProcessLife.java:52, 119-139).
  - `lockQuietNow` (:1784 → :1807-1813): two new threads, then `applyLockTiles` on the main thread (:1819-1826), which commits (TileProfile.java:89).
  - The helper ping (C3).
  - `RecentsSweep.requestAsync` (:1796).
  - `AppOrigin.refreshUsedCacheAsync` (:1797): a thread plus a usage query.
  - `refreshLayers` (:1798).

  The wake run sits before the first lock-screen frame: about 14 ms of main-thread work on the tablet (TR:722-725). The two dark runs cost thread starts, IPC and prefs rewrites (C7).
- **Change:**
  - Keep the three triggers; they cover missed broadcasts.
  - Record, per lock sequence number, which parts succeeded. The later runs redo only parts that failed or whose input changed: the mode, the helper state, or a tiles value that differs.
  - Take the ProcessLife sample once per lock, on a worker.
- **Risk / verify:**
  - Risk: a half-failed part must still be retried, so track success per part.
  - Verify: a JVM test of the pure "what to redo" rule; emulator `o`, `ae`, `bl`, `wf`, `t`; the LOCKPREP / TILES lines per lock.

## C5 MEDIUM: the Settings alias forces a full Launcher3 reload at every mode change
- **Where:** a chain from Mode.java:73:
  1. SettingsSpoof.java:89-96 (`syncSoon`, 3 s, :34).
  2. :102-120 (`syncNow`): `setComponentEnabledSetting` at :110, then `HiddenApps.reloadLauncher` posted at :114.
  3. HiddenApps.java:102-105.
  4. `LauncherModel.forceReload()` (launcher3/src/com/android/launcher3/LauncherModel.java:309-334): a full LoaderTask (workspace, all apps, deep shortcuts, widgets) and a full rebind.
- **How often:** `aliasWanted` = the switch AND custom mode (:48), so it flips at every Quickstep entry and every return. That is 2 full reloads per round trip (release default ON).
- **Cost:**
  - A whole model load and rebind about 3 s after the custom home appears, while Tj may already be using it.
  - The component change alone already sends PACKAGE_CHANGED for our package, which stock Launcher3 handles per package (AOSP). Whether this fork's HiddenApps filter needs more is unverified.
- **Change:**
  - Drop forceReload if PACKAGE_CHANGED alone adds or removes the entry; otherwise run a per-package update for our package.
  - Skip it in Quickstep, where our drawer is not on screen, and check the entry on return.
- **Risk / verify:**
  - Risk: zero or two "Settings" entries in the drawer.
  - Verify: emulator `ss` (exactly one entry after each mode change); a log line per reload.

## C6 MEDIUM: Mode.set does the slow profile switches on the main thread
- **Where:** Mode.java:63-82, called from the unlock body on the main thread (TjAccessibilityService.java:1894 QUICKSTEP, :1913 CUSTOM, :1790). After the tj_mode commit (:71) and AnimationSpeedup.syncNow (:72) it runs:
  - TaskOrigin.sessionStart (:76).
  - KeyboardSwitch.apply (:77):
    - checkSelfPermission (KeyboardSwitch.java:27)
    - the getEnabledInputMethodList IPC (:38)
    - getSharedPreferences, twice (:44)
    - a Settings read, write and read-back (:43, :56, :57)
    - `say()`, which commits to Features and logs even for "already" (:30, :55). The string contains the mode, so it is a real write at every change.
  - TileProfile.apply (:78): a keyguard binder call (TileProfile.java:72-74) and an unconditional APPLIED_KEY commit (:89).
  - ImmersiveProfile.apply (:79).
  - BatterySaverGuard.check (:80).
- **How often:** every unlock that changes the mode. The keyboard switch is on, on the tablet (TR:675-676).
- **Cost:** several IPCs and at least two whole-file prefs writes with fsync (AOSP), all on the thread that draws the first frame after the unlock. The reset took 912 ms on the tablet (TR:704); Mode.set's share of that is unverified.
- **Change:**
  - Keep only the tj_mode commit (the router reads it) and the animation sync on the calling thread.
  - Run KeyboardSwitch, TileProfile, ImmersiveProfile and BatterySaverGuard.check, in the same order, on one serial background executor. A late tiles apply is already accepted (it is re-applied after 1.5 s, :1829-1840).
  - Write status strings with `setStringAsync`, and only when they differ.
- **Risk / verify:**
  - Risk (unverified): a very fast first key press could reach the previous keyboard. Today the switch also lands after the frame.
  - Verify: emulator `legacy` with `base` (it prints `keyboard: before=... afterCustom=...`) and `t`; KeyboardPolicyTest; the unlock body's ms.

## C7 MEDIUM: diagnostics written with commit()
- **Where:** Features.java:28-36 (`set`, `setString` and `setInt` all use `commit()`).
  - Main-thread callers:
    - profileNote (TjAccessibilityService.java:1718-1721), called at :1731 (handshake Enter tap), :1763 (prearm) and :1791.
    - finishReset, which writes RESET_REPORT_KEY before `done.run()` (:4014-4021).
    - appendReport, once per line (:4032-4035).
    - KeyboardSwitch.java:30, TileProfile.java:89 and RecentsFilter.java:124.
  - Worker callers, at every lock and unlock: BatterySaverGuard.java:135, 214, 278-279, 290-291 and LockQuiet.java:120, 123, 158.
- **How often:** about 8-15 whole-file writes per lock/unlock cycle. This is my count from the call sites (unverified); a debug counter would confirm it.
- **Cost:**
  - Each changing write serialises all of tj_features.xml and fsyncs on the calling thread. On eMMC that is typically several to tens of ms (unverified here).
  - A commit also waits for any in-flight apply() (AOSP).
  - On the main thread this delays the unlock frame. finishReset even holds the cover removal behind it.
  - Every write also wakes the prefWatch listener (:530-543).
- **Change:**
  - Write status and diagnostics with `setStringAsync` / `apply()`. The report reads them; the router never needs them after a kill.
  - Compare before writing.
  - In finishReset, call `done.run()` first and write after it.
  - Write all appendReport lines once.
  - Keep `commit()` only for state that must survive a kill: the mode and the holds' originals.
  - Optional: put diagnostics in their own prefs file. ReportState reads `Features.all` (Features.java:41), so it would then read both files.
- **Risk / verify:**
  - Risk: a line written milliseconds before a process death can be lost.
  - Verify: emulator `x` (the report lines are still there); StrictMode disk-write detection in the debug build.

## C8 LOW: ReturnReset.targetPackage on frequent paths
- **Where:** ReturnReset.java:317-334.
  - `isInstalled`, a getApplicationInfo IPC, runs on EVERY call (:323, :336-338).
  - The target is rescanned after 10 min, or after 30 s when there is none (:324).
  - `scanTarget` (:341-351) loads one label per app, then commits (:348).
- **How often:** callers include:
  - trackDirty, on every window-state change in Quickstep (TjAccessibilityService.java:1990);
  - updateHotspot, on every foreground change (:3168-3175);
  - AppOrigin.java:76, RecentsGuard.java:58 and :1968;
  - RecentsPolicy.java:30, which evaluates eagerly even with its switch off (default off, :22).
- **Cost:**
  - One IPC per window change, on the main thread for trackDirty and updateHotspot.
  - A 349 ms scan every 10 min of use (TR9:888).
  - Clean start, the release default, opens no target app anyway.
- **Change:**
  - Cache the installed flag with the target.
  - Rescan only on package add / remove / change.
  - Evaluate the target in RecentsPolicy only when its switch is on.
- **Risk / verify:**
  - Risk: none in practice. Every install or removal sends a package event.
  - Verify: a JVM test of the cache rule; emulator `cs`, `uf`, `legacy`.

## C9 LOW: dumpsys forked up to three times per clean-up (helper only)
- **Where:**
  - ShellServer.java:148-169: LISTTASKS, then RMTASKS dumps again.
  - :236-242: `removeRecentTasks` dumps again.
  - :244-255: each dump forks `dumpsys activity recents`.
  - :276-289: `runShell` starts a thread plus `sh` per command.
  - The calls come from RecentsSweep.java:183 and :188.
- **How often:** every sweep (lock, plain unlock, Recents open, a hidden app leaving the screen), but only while the helper runs. It is not running on the tablet today (TR:620).
- **Cost:** each dump is a fork, exec and full recents dump, tens of ms of CPU on this SoC (unverified).
- **Change:**
  - Let RMTASKS reuse a dump less than about 1 s old, or pass the parsed list along.
  - Deduplicate the sweeps of the three lockPrepare runs (C4).
- **Risk / verify:**
  - Risk: a task created within that second is missed once. The next level-triggered sweep catches it.
  - Verify: emulator `cs2` and `cs`; the fork count in the helper log.

## C10 LOW: the HLP1 logcat watcher reads every log line (helper only)
- **Where:**
  - ShellServer.java:408-422 runs `logcat -b main -b system -b crash -v threadtime -T 1`, one regex per line (:405), and stops after 400 matches.
  - The BEAT writes every 30 s (HelperLife.java:18-19); `rec` opens, appends and closes each time (ShellServer.java:386-403).
- **How often:** continuously while the helper runs.
- **Cost:** every log line of every app is copied into the helper JVM and matched. The cap limits matches, not lines read.
- **Change:**
  - Let logcat filter first with `-s ActivityManager:I` or `-e <regex>` (Android 10's logcat has `-e`; unverified on this build), and keep the Java regex as a second stage.
  - Remove the watcher once HLP1 is answered (TASKS.md:449, 469, 481).
- **Risk / verify:**
  - Risk: a filter that is too narrow misses a kill line.
  - Verify: a forced kill is still recorded. No gate section covers HelperLife (tools/gate-map.txt), so this needs a manual run.

## C11 LOW: the battery-saver observer body runs on the main thread
- **Where:** BatterySaverGuard.java:43-51, on the main looper. It writes LockLog, reads Settings and Mode, calls `powerSource` (a sticky registerReceiver IPC, :379-390), starts a thread, and calls `check`.
- **How often:** on every low_power change. It fires twice per switch-off, 4 ms apart (TR:726-727). The tablet counts 93 manual activations (TR:642).
- **Change:** post the body to a worker, and fold changes within about 50 ms into one.
- **Risk / verify:**
  - Risk: the reaction comes a few ms later.
  - Verify: emulator `bs` and `bl`.

## C12 LOW: preference handles looked up on every call
- **Where:**
  - CrashGuard.java:92-95: `automationOff`, read on many paths (for example TjAccessibilityService.java:3343 and 3407).
  - AnimationSpeedup.java:42: on every tick (:168).
  - NotifQuiet and RadioIconHide.
  - HiddenApps.java:22. Its `allowed()` also copies the set (:32-40), and `isPackageShown` (:73-78) runs per app in C1 and per notification.
  - AppOrigin.java:18, RecentsDirty.java:16 and KeyboardSwitch.java:44.
- **How often:** on every event (through tick, C2), every notification, and every app in C1.
- **Cost:** each call takes a global lock and does map lookups (AOSP). That is microseconds per call, but on per-event paths.
- **Change:** cache one handle per file, as Features.java:19-25 and Mode.java:47-53 already do, and test membership without copying the set.
- **Risk / verify:**
  - Risk: none; the cached handle is the same object.
  - Verify: the unit tests stay unchanged.

## C13 LOW: task binder calls on every Launcher resume
- **Where:** TjHooks.java:88-89 calls two RecentsGuard methods:
  - :25-35: `getAppTasks`, then `getTaskInfo` and `setExcludeFromRecents` for each task.
  - :53-73: `describeAsTarget`, which uses targetPackage (C8).
- **How often:** every Launcher.onResume, so every Home press and every return to the home screen.
- **Cost:** 1 + 2n binder calls (n = our tasks, usually 1-2) on the main thread, during the resume that draws the home screen.
- **Change:**
  - Remember the task ids already flagged and redo only for a new id.
  - Call describeAsTarget only when the target changes.
- **Risk / verify:**
  - Risk: none expected; a new task always gets a new id (AOSP).
  - Verify: emulator `legacy` (Recents flags, EXCLUDE_FROM_RECENTS).

## Checked and FINE

**Wake sources:**
- There is no AlarmManager, JobScheduler, WorkManager or WakeLock in app code (grep).
- The helper's BEAT thread holds no wake lock (ShellServer.java:354-363).

**Receivers and observers:**
- The manifest receivers are Launcher3's standard ones (AndroidManifest.xml:83-105).
- In TjAccessibilityService.java, each of these is registered once and unregistered as a pair:
  - the lock receiver (:2840-2857)
  - the patch watch (:563-580)
  - the close-dialogs receiver (:552-561)
  - the tile guard (:3355-3365)
  - the radio guard (:3428-3441)
- The radio receiver checks `keepsRadiosOn` first (:3386). The lock chain only runs while a radio is on (:3404-3426).
- The MediaStore observer exists only during a calibration (:4917-4943).
- Also registered once or in pairs: BatterySaverGuard.java:38-61, EntranceWatcher.java:45-61, KeepAliveService.java:59-66 and 101-119 (off on the tablet, TR:622), TjApplication.java:16-34. RestoreStore takes its snapshot once (RestoreStore.java:21-26).
- The look-alike pages register only while they are visible: SsWatcher.java:28-35, SsHomeSummary.java:44-55, SsPagesBattery.java:119/126, SsPagesSound.java:185-200.
- Launcher3's registrations are paired: Launcher.java:377/1462, LauncherAppState.java:100/141, ConfigMonitor.java:77/135, WallpaperOffsetInterpolator.java:164-168.
- WallpaperManagerCompat uses VOMR1 on API 29 (WallpaperManagerCompat.java:37/43). Its JobScheduler variant is unused.

**Holds and bounded waits:**
- The holds write nothing when unchanged: AnimationHolds.java:43-88, NotifQuietHold.java:34-61, RadioIconHold.java:172-199.
- RadioIconHide.offThenSync waits at most 4 s (RadioIconHide.java:153-170). deferRelease is an explicit state (:125-139).
- AnimationSpeedup has two follow-ups per unlock (:146-147) and a 10 s failsafe (:100).

**Logs and writes:**
- LockLog is a 48/32 KB ring written on one background executor (LockLog.java:17-18, 51-77).
- HelperLife is capped at 48/24 KB (HelperLife.java:19).
- DIRTY lines are written only on change and rate-limited (RecentsDirty.java:18, 22-31; TjAccessibilityService.java:1997).
- NotifSightings uses a background executor, apply() and a rate-limited log (NotifSightings.java:113-152).
- Radio writes happen only when a radio is on (ReturnReset.java:292-304, LockQuiet.java:115-127).
- Requests are coalesced in RecentsSweep (RecentsSweep.java:139-177) and in SettingsSpoof.syncSoon, which does nothing when the state is already right (SettingsSpoof.java:89-96, 109).
- Frequent writes use apply(): ProcessLife.java:111-116 and Mode.java:20-24.
- TabletReport and the helper's SHOTS / APKS / PROBE commands run only on demand (TabletReport.java:195-236).

**Not verified:**
- How long a refused helper connect takes on the tablet (C3).
- The fsync cost of the tablet's storage (C6, C7).
- Whether PACKAGE_USAGE_STATS is really denied (TR:612).
- The real number of prefs writes per lock (C7).

One debug counter line in the lock log would answer all four.
