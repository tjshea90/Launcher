# Performance review, slice C: background work and battery (2026-11-12)

This is reviewer C's slice of TASKS BY1a, done read-only.

**Scope:**
- receivers, observers, timers and heartbeats
- the shell helper and RecentsSweep
- the holds (battery saver, radio icons, banners, animations)
- ReturnReset and KeyboardSwitch
- every file or preference the app appends to

Other slices: A = startup and the drawer, B = the accessibility service's per-event work and overlays, D = memory. C1-C4 and C7 sit in slice B's file, TjAccessibilityService.java, but they are lock, unlock, heartbeat and report-file work, not overlay work. C5 overlaps slice A.

**Evidence:**
- The code at HEAD. Nothing was built or run.
- `TR` = research/tablet/2026-11-10-2/tablet-report.txt; `TR9` = research/tablet/2026-11-09/tablet-report.txt.
- "(AOSP)" = Android 10 platform behaviour stated from AOSP knowledge. I did not re-read that source for this review, and it is unverified on the tablet.
- Section names come from tools/gate-map.txt.

**What the tablet says (this sets the priorities):**
- **The shell helper is NOT running** (TR:620; "not reachable" repeats at TR:907-911; HLP1 is still open, TASKS.md:449). Everything that needs it (RecentsSweep's removals, ShellServer, HelperLife) is idle there. The app-only fallbacks are the paths actually in use.
- **Event volume:** 45521 accessibility events in 121 min, 38643 of them noise (TR:629).
- **Footprint:** PSS 69 MB (TR:627); onCreate 48 ms (TR:628).
- **Usage access:** PACKAGE_USAGE_STATS is reported not granted (TR:612). The app-op state is unverified.

**Nothing in this slice wakes the CPU or keeps it awake while the screen is off.** There is no AlarmManager, JobScheduler, WorkManager or wake lock in app/src/main/java (grep). Handler delays and Thread.sleep do not advance in suspend (AOSP).

**Where the cost actually is:**
- CPU and IPC while the screen is on.
- Main-thread stalls on the lock and unlock paths. The service and Launcher3 share one main thread (TjAccessibilityService.java:38).

## Findings at a glance

| id | sev | where | one line |
| :- | :- | :- | :- |
| C1 | HIGH | RecentsFilter.java:117, 134-157 | every app's label is scanned on the MAIN thread when Recents opens (switch default ON, :24) |
| C2 | MEDIUM | TjAccessibilityService.java:76 | noise events drive the 2 Hz policy heartbeat: about 6 binder calls per tick in Quickstep |
| C3 | MEDIUM | TjAccessibilityService.java:1786, 1861 | a helper ping (join up to 700 ms) and a usage query run eagerly on the main thread, at lock and at every Enter tap |
| C4 | MEDIUM | TjAccessibilityService.java:2827-2831 | lockPrepare runs 3 times per lock and redoes all its work each time |
| C5 | MEDIUM | SettingsSpoof.java:110-114 | every mode change toggles a component AND forces a full Launcher3 model reload |
| C6 | MEDIUM | Mode.java:76-80 | keyboard, tiles, immersive and saver IPC plus prefs commits on the main thread at every mode-changing unlock |
| C7 | MEDIUM | Features.java:28-36 | status and diagnostic strings use commit() (whole file + fsync), several on the main thread |
| C8 | LOW | ReturnReset.java:317-351 | target lookup: one PackageManager IPC per window change, a full label rescan every 10 min |
| C9 | LOW | ShellServer.java:148-169, 236-255 | up to 3 `dumpsys` forks per clean-up (helper only) |
| C10 | LOW | ShellServer.java:408-422 | the logcat watcher reads every main / system / crash line (helper only) |
| C11 | LOW | BatterySaverGuard.java:43-51 | the saver observer body runs on the main thread, twice per switch-off |
| C12 | LOW | CrashGuard.java:92-95 and others | getSharedPreferences is looked up again on every call on per-event paths |
| C13 | LOW | TjHooks.java:88-89 | 1 + 2n task binder calls on every Launcher resume |

**Suggested order:**
1. Measure first: a debug-only lock-log line with main-thread ms and IPC counts per lock cycle and per unlock.
2. C1.
3. C6 + C7, the unlock path.
4. C3.
5. C4.
6. C2.
7. C5.
8. The LOW findings, as clean-up in the same files.

## C1 HIGH: Recents label scan on the main thread
- **Where:**
  - RecentsFilter.java:112-131 (`run`). At :117, `if (now - blockedAt > LABELS_TTL_MS) blocked = blockedNow(svc)`; `blockedNow` is :134-157.
  - The TTL is 60 s (:26). The first look comes 60 ms after the Recents window opens (:35).
  - The handler belongs to the service and runs on the main looper (TjAccessibilityService.java:38, passed in at :237).
- **How often:** almost every Recents open in the custom launcher.
  - The switch is ON by default (:24); `wanted` is at :44-46.
  - The first look rebuilds the set whenever the last build is more than 60 s old.
  - It runs with or without the helper; RecentsSweep is requested as well (:100).
- **What one rebuild does:** queryIntentActivities, then for each app: HiddenApps.isPackageShown, `ri.loadLabel(pm)` and `pm.getApplicationLabel`, then the labels of the apps opened in Quickstep.
- **Cost:**
  - Each label is PackageManager IPC plus resource loading. A scan with ONE label per app took 349 ms for 53 apps on the tablet (TR9:888, ReturnReset.scanTarget). This scan does more per app; its tablet time is unverified.
  - It blocks the thread that draws Launcher3's Recents transition.
  - It delays the look that closes the hidden apps' cards.
- **Change:**
  - Build the set on a worker when the service connects and after each unlock.
  - Mark it stale on package add / remove / change, a locale change, HiddenApps edits and changes to AppOrigin's Quickstep set. Labels change only with those events, so the TTL is not needed.
  - `run` uses the set it already has; a stale set is rebuilt on the worker and the look runs again when the rebuild lands.
  - The same cards are closed.
- **Risk:** an app installed seconds ago could survive one look. A slow scan already lets that happen today.
- **Verify:** a JVM test of the pure "stale? → rebuild" rule; a lock-log INFO line with the build's ms and thread; emulator sections `rg` (AP1) and `cs`.

## C2 MEDIUM: noise events drive the policy heartbeat
- **Where:**
  - TjAccessibilityService.java:76 calls `AnimationSpeedup.tick(this)` BEFORE the noise filter (:81-85).
  - `tick` (AnimationSpeedup.java:163-171) is behind a 500 ms gate. In Quickstep, with any switch on, it calls `syncSoon`, which runs on the worker HandlerThread (:151-157).
  - `syncNow` (:198-233) is static synchronized. It runs NotifQuiet.sync, RadioIconHide.sync and the animation hold.
- **How often:** about 6 events a second reach it on the tablet, and 85 % of them are content changes of other apps (TR:629; EventNoise.java:19-22). In Quickstep, any app that keeps updating (a video, a progress bar, a clock) holds it at 2 syncs a second, up to 7200 an hour.
- **Cost:**
  - In steady state every sync is a no-op: the holds write nothing when unchanged (AnimationHolds.java:43-88, NotifQuietHold.java:34-61, RadioIconHold.java:172-199).
  - Even so, each sync makes about 6 binder calls:
    - up to 4 checkSelfPermission calls: NotifQuiet.java:33 via :70, RadioIconHide.java:42, and AnimationSpeedup.java:38 via :53 and :209. Android 10 has no client cache for these (AOSP).
    - isInteractive and isKeyguardLocked (AnimationSpeedup.java:262-269).
    - plus Settings reads and the prefs lookups in C12.
  - The main-thread syncs (Mode.java:72, AnimationSpeedup.java:132, 141-148) take the same class lock. An unlock can therefore wait behind a worker sync that is running.
- **Change:**
  - Call `tick` only for window events (`EventNoise.windowsJustChanged`, EventNoise.java:45-47), or after the noise filter. Content changes of other apps change none of the facts the policy reads: mode, screen, keyguard, helper.
  - Read the grant once per `syncNow` and pass it to the three holds.
  - Optional: cache the screen and keyguard state from the broadcasts the service already receives (:2804-2838).
- **Risk:** a fact that changes with no window event is re-read later. The other level triggers stay: Mode.set, screen off/on, unlock decided, helper connect/unbind.
- **Verify:** emulator `an`, `nq`, `wf`, `y2`; a debug count of syncs per minute.

## C3 MEDIUM: helper ping and usage query run eagerly on the main thread
- **Where:** two arguments are evaluated eagerly in TjAccessibilityService:
  - :1861 (coverAtEnter) evaluates `ReturnReset.mayHaveWork(app)`.
  - :1786 (lockPrepare) evaluates `ReturnReset.invisibleReady(app)`.
  - The rules that receive them often do not need them (ProfileSession.java:43-55; for example, `plainUnlockEndsQuickstep` = `!customMode && !automationOff`).
- **What the two calls cost:**
  - `invisibleReady` (ReturnReset.java:87-96, 4 s cache) → `ShellHelper.reachable()`. That starts a thread and joins it for up to 700 ms (ShellHelper.java:64-70). The thread tries the unix socket, then the TCP ports 47631-47635 (:27-58, ShellProtocol.java:22). Failures are logged (:109-114; TR:907-911).
  - `mayHaveWork` (ReturnReset.java:400-403) → `recentsNeededCheap` (:62-65) → `AppOrigin.pending` → `mergeUsage` (AppOrigin.java:151, 63-91). That is a UsageStats query guarded only for 1 s (:69).
- **How often:**
  - :1861 runs on every plain Enter tap of the PIN pad, in both modes.
  - :1786 runs at every lockPrepare, 3 per lock (C4).
  - Each pings whenever its 4 s cache is stale.
- **Cost:**
  - Without the helper, each ping is a thread start plus failed connects on the main thread.
  - The connects are normally refused fast. The worst case is 700 ms; the refusal time on the tablet is unverified.
  - The usage query sits between the finger lift and the first frame.
- **Change:**
  - Evaluate lazily (a supplier, or the cheap conditions first).
  - Keep one "helper ready" value, refreshed on a worker at screen-off and when the helper connects or fails. The failure reset already exists at :150 and :178. The main thread only reads it.
  - At the Enter tap, use the cached `pending(custom, quickstep)`. The usage merge belongs on the lock worker, where `refreshUsedCacheAsync` (AppOrigin.java:120-129) already runs it (:1797).
- **Risk:** a stale "ready" could choose the invisible path just after the helper died. That path already falls back on failure (:150, :178).
- **Verify:** the ProfileSession JVM truth tables stay unchanged; emulator `uf`, `cs`, `cs2`; lock-log ms from the Enter tap to `cover shown`.

## C4 MEDIUM: lockPrepare runs three times per lock
- **Where:** TjAccessibilityService.java:2827-2829 calls `lockPrepareLater` at +700 ms and +6500 ms after SCREEN_OFF. SCREEN_ON calls `handleLockedWake` (:2831), which calls `lockPrepare` (:1804). The body is at :1776-1801.
- **How often:** every screen-off: twice while dark, once at the wake.
- **Each run repeats:**
  - `ProcessLife.noteLock` (:1783): `getRunningAppProcesses` on the main thread when its 60 s sample is due (ProcessLife.java:52, 119-139).
  - `lockQuietNow` (:1784 → :1807-1813): two new threads, then `applyLockTiles` on the main thread (:1819-1826, with a commit at TileProfile.java:89).
  - The helper ping (C3).
  - `RecentsSweep.requestAsync` (:1796).
  - `AppOrigin.refreshUsedCacheAsync` (:1797): a thread plus a usage query.
  - `refreshLayers` (:1798).
- **Cost:**
  - The wake run sits between the power button and the first lock-screen frame: about 14 ms of main-thread work on the tablet (TR:722-725, LOCKPREP 45.680 → TILES 45.694).
  - Each dark run costs thread starts, IPC and prefs rewrites (C7).
- **Change:**
  - Keep all three triggers; they cover missed broadcasts.
  - Record per lock, under a lock sequence number, which parts succeeded. The later runs redo only parts that failed or whose input changed (mode, helper state, a tiles value that really differs).
  - Take the ProcessLife sample once per lock, on a worker.
- **Risk:** a part that half-failed must still be retried, so track success per part, not with one flag.
- **Verify:** a JVM test of the pure "what to redo" rule; emulator `o`, `ae`, `bl`, `wf`, `t`; the LOCKPREP / TILES lines per lock.

## C5 MEDIUM: the Settings alias forces a full Launcher3 reload at every mode change
- **Where:** a chain from Mode.java:73:
  1. SettingsSpoof.java:89-96 (`syncSoon`, 3 s delay, :34).
  2. :102-120 (`syncNow`) calls `setComponentEnabledSetting` (:110), then posts `HiddenApps.reloadLauncher` (:114).
  3. HiddenApps.java:102-105.
  4. `LauncherModel.forceReload()` (launcher3/src/com/android/launcher3/LauncherModel.java:309-334): a full LoaderTask (workspace, all apps, deep shortcuts, widgets) and a full rebind.
- **How often:** `aliasWanted` is true only with the switch on AND custom mode (:48). It flips at every Quickstep entry and every return, so there are 2 full reloads per round trip (release default ON).
- **Cost:**
  - A whole model load and rebind about 3 s after the custom home appears, while Tj may already be using it.
  - The component change alone already sends PACKAGE_CHANGED for our package, which stock Launcher3 handles as a per-package update (AOSP). Whether this fork's HiddenApps filter needs more is unverified.
- **Change:**
  - Drop forceReload if PACKAGE_CHANGED alone adds or removes the entry. Otherwise run a per-package update for our package.
  - Skip it entirely in Quickstep (our drawer is not on screen) and check the entry on return.
- **Risk:** zero or two "Settings" entries in the drawer.
- **Verify:** emulator `ss` (exactly one entry after each mode change); a lock-log line per reload.

## C6 MEDIUM: Mode.set does the slow profile switches on the main thread
- **Where:** Mode.java:63-82, called from the unlock body on the main thread (TjAccessibilityService.java:1894 QUICKSTEP, :1913 CUSTOM, and :1790). After the tj_mode commit (:71) and AnimationSpeedup.syncNow (:72), it runs these steps:
  - TaskOrigin.sessionStart (:76).
  - KeyboardSwitch.apply (:77):
    - checkSelfPermission (KeyboardSwitch.java:27);
    - the getEnabledInputMethodList IPC (:38);
    - two getSharedPreferences calls (:44);
    - a Settings read, write and read-back (:43, :56, :57);
    - `say()`, a Features commit plus Log.i, even for "already" (:30, :55). The string contains the mode, so it writes at every change.
  - TileProfile.apply (:78): a keyguard binder call (TileProfile.java:72-74) and an unconditional APPLIED_KEY commit (:89).
  - ImmersiveProfile.apply (:79).
  - BatterySaverGuard.check (:80).
- **How often:** every unlock that changes the mode. The keyboard switch is on, on the tablet (TR:675-676).
- **Cost:**
  - Several IPCs and at least two whole-file prefs writes with fsync (AOSP).
  - All on the thread that must draw the first frame after the unlock.
  - The reset took 912 ms on the tablet (TR:704). How much of that is Mode.set is unverified.
- **Change:**
  - Keep only the tj_mode commit (the router reads it) and the animation sync on the calling thread.
  - Run KeyboardSwitch, TileProfile, ImmersiveProfile and BatterySaverGuard.check, in the same order, on one serial background executor. A late tiles apply is already accepted (it is re-applied after 1.5 s, :1829-1840).
  - Write the status strings with `setStringAsync`, and only when they differ.
- **Risk:** a very fast first key press could go to the previous keyboard. This is unverified; today the switch also lands after the frame.
- **Verify:** emulator `legacy` with `base` (it prints `keyboard: before=... afterCustom=...`) and `t`; KeyboardPolicyTest; the unlock body's ms.

## C7 MEDIUM: diagnostics written with commit()
- **Where:** Features.java:28-36: `set` / `setString` / `setInt` all use `commit()`.
- **Callers on the main thread:**
  - profileNote (TjAccessibilityService.java:1718-1721), called at :1731 (the handshake Enter tap), :1763 (prearm) and :1791.
  - finishReset, which writes RESET_REPORT_KEY BEFORE `done.run()` (:4014-4021).
  - appendReport, once per line (:4032-4035).
  - KeyboardSwitch.java:30, TileProfile.java:89 and RecentsFilter.java:124.
- **Callers on workers, at every lock and unlock:** BatterySaverGuard.java:135, 214, 278-279, 290-291 and LockQuiet.java:120, 123, 158.
- **How often:** about 8-15 whole-file writes per lock/unlock cycle. That is my count from the call sites; it is unverified, and a debug counter would confirm it.
- **Cost:**
  - Each changing write serialises all of tj_features.xml and fsyncs it on the calling thread. On eMMC that is typically several to tens of ms (unverified on this tablet).
  - A commit also waits for any apply() still in flight (AOSP).
  - On the main thread it delays the unlock frame. finishReset even holds the cover removal behind it.
  - Every write also wakes the prefWatch listener (:530-543).
- **Change:**
  - Write status and diagnostic strings with `setStringAsync` / `apply()`. The report reads them; the router never does after a kill.
  - Compare before writing.
  - In finishReset, call `done.run()` first and write afterwards.
  - Write the appendReport lines once.
  - Keep `commit()` only for state that must survive an immediate kill: the mode and the holds' originals.
  - Optional: give diagnostics their own prefs file. ReportState reads `Features.all` (Features.java:41), so it would then read both files.
- **Risk:** a diagnostic line written milliseconds before a process death can be lost.
- **Verify:** emulator `x` (the report sections still show their lines); StrictMode disk-write detection in debug.

## C8 LOW: ReturnReset.targetPackage on frequent paths
- **Where:**
  - ReturnReset.java:317-334 makes an `isInstalled` getApplicationInfo IPC on EVERY call (:323, :336-338).
  - It rescans every 10 min, or every 30 s when there is no target (:324).
  - `scanTarget` (:341-351) loads one label per app and commits (:348).
- **Callers:**
  - trackDirty, on every window-state change in Quickstep (TjAccessibilityService.java:1990).
  - updateHotspot, on every foreground change (:3168-3175).
  - AppOrigin.java:76, RecentsGuard.java:58 and :1968.
  - RecentsPolicy.java:30, evaluated eagerly even with its switch off (default off, :22).
- **Cost:**
  - One IPC per window change, on the main thread for trackDirty and updateHotspot.
  - A 349 ms scan (TR9:888) every 10 min of use.
  - Clean start, the release default, opens no target app anyway.
- **Change:**
  - Cache the installed flag together with the target.
  - Rescan only on package add / remove / change.
  - Evaluate the target in RecentsPolicy only when its switch is on.
- **Risk:** none in practice; every install or removal sends a package event.
- **Verify:** a JVM test of the cache rule; emulator `cs`, `uf`, `legacy`.

## C9 LOW: dumpsys forked up to three times per clean-up (helper only)
- **Where:**
  - ShellServer.java:148-169: LISTTASKS; RMTASKS dumps again.
  - :236-242: `removeRecentTasks` dumps again.
  - :244-255: `recentsDump` forks `dumpsys activity recents`.
  - :276-289: `runShell` starts a thread plus `sh` for every command.
  - RecentsSweep.java:183 and :188.
- **How often:** every sweep (lock, plain unlock, Recents open, a hidden app leaving the screen), but only while the helper runs. It is not running on the tablet today (TR:620).
- **Cost:** each dump is a fork, an exec and a full recents dump, likely tens of ms of CPU on this SoC (unverified).
- **Change:**
  - Let RMTASKS reuse a dump that is less than about 1 s old, or pass the parsed list along.
  - Deduplicate the sweeps of the three lockPrepare runs (C4).
- **Risk:** a task created inside that second is missed once; the next level-triggered sweep catches it.
- **Verify:** emulator `cs2`, `cs`; the fork count in the helper log.

## C10 LOW: the HLP1 logcat watcher reads every log line (helper only)
- **Where:**
  - ShellServer.java:408-422 runs `logcat -b main -b system -b crash -v threadtime -T 1` with a regex per line (:405) and stops after 400 matches.
  - The BEAT writes every 30 s (HelperLife.java:18-19); `rec` opens, appends to and closes the log file (ShellServer.java:386-403).
- **How often:** continuously, while the helper runs.
- **Cost:** every log line of every app is copied into the helper JVM and matched. The 400 cap limits the matches, not the lines read.
- **Change:**
  - Let logcat filter first (`-s ActivityManager:I`, or `-e <regex>`; Android 10's logcat has `-e`, unverified for this build), and keep the Java regex as the second stage.
  - Remove the watcher once HLP1 is answered (TASKS.md:449, 469, 481).
- **Risk:** a filter that is too narrow misses a kill line.
- **Verify:** a forced kill is still recorded. No gate section covers HelperLife (tools/gate-map.txt), so this needs a manual run.

## C11 LOW: the battery-saver observer body runs on the main thread
- **Where:** BatterySaverGuard.java:43-51 runs on the main looper and does all of this:
  - a LockLog write;
  - Settings and Mode reads;
  - `powerSource`, a sticky registerReceiver IPC (:379-390);
  - starting a thread;
  - `check`.
- **How often:** on every low_power change. It fires twice per switch-off, 4 ms apart (TR:726-727). The tablet has counted 93 manual activations (TR:642).
- **Change:** post the body to a worker, and fold changes that come within about 50 ms of each other into one.
- **Risk:** the reaction comes a few ms later.
- **Verify:** emulator `bs` and `bl`.

## C12 LOW: preference handles looked up on every call
- **Where:**
  - CrashGuard.java:92-95: `automationOff` is read on many paths, for example TjAccessibilityService.java:3343 and 3407.
  - AnimationSpeedup.java:42, called on every tick (:168).
  - NotifQuiet and RadioIconHide.
  - HiddenApps.java:22. `allowed()` also copies the set (:32-40), and `isPackageShown` (:73-78) runs per app in C1 and per notification.
  - AppOrigin.java:18, RecentsDirty.java:16 and KeyboardSwitch.java:44.
- **How often:** on every event (through tick, C2), every notification, and every app in C1.
- **Cost:** a global lock plus map lookups per call (AOSP). Each call costs only microseconds, but these paths run per event.
- **Change:** cache one handle per file, as Features.java:19-25 and Mode.java:47-53 already do, and test membership without copying the set.
- **Risk:** none; the cached handle is the same object.
- **Verify:** the unit tests stay unchanged.

## C13 LOW: task binder calls on every Launcher resume
- **Where:** TjHooks.java:88-89 calls into RecentsGuard:
  - :25-35: `getAppTasks`, then `getTaskInfo` and `setExcludeFromRecents` for each task;
  - :53-73: `describeAsTarget`, which uses targetPackage (C8).
- **How often:** every Launcher.onResume, so every Home press and every return to the home screen.
- **Cost:** 1 + 2n binder calls (n = our tasks, usually 1-2) on the main thread, during the resume that draws the home screen.
- **Change:**
  - Remember which task ids are already flagged.
  - Redo the calls only for a new id.
  - Call describeAsTarget only when the target changes.
- **Risk:** none expected; a new task always gets a new id (AOSP).
- **Verify:** emulator `legacy` (Recents flags, EXCLUDE_FROM_RECENTS).

## Checked and FINE

**Wake sources:**
- No AlarmManager, JobScheduler, WorkManager or WakeLock in app code (grep).
- The helper's BEAT thread holds no wake lock (ShellServer.java:354-363).

**Receivers and observers:**
- Manifest receivers are Launcher3's standard ones (AndroidManifest.xml:83-105).
- The service registers each of these once and unregisters it in a matching pair:
  - lock receiver (TjAccessibilityService.java:2840-2857)
  - patch watch (:563-580)
  - close-dialogs receiver (:552-561)
  - tile guard (:3355-3365)
  - radio guard (:3428-3441)
- The radio receiver checks `keepsRadiosOn` first (:3386). The lock-radio chain runs only while a radio is on (:3404-3426).
- The MediaStore observer is live only during a calibration (:4917-4943).
- Registered once or in pairs:
  - BatterySaverGuard.java:38-61
  - EntranceWatcher.java:45-61
  - KeepAliveService.java:59-66 and 101-119 (off on the tablet, TR:622)
  - TjApplication.java:16-34
  - RestoreStore.java:21-26 (snapshots once)
- The look-alike pages register only while visible: SsWatcher.java:28-35, SsHomeSummary.java:44-55, SsPagesBattery.java:119/126, SsPagesSound.java:185-200.
- Launcher3 pairs its registrations: Launcher.java:377/1462, LauncherAppState.java:100/141, ConfigMonitor.java:77/135, WallpaperOffsetInterpolator.java:164-168.
- WallpaperManagerCompat uses the VOMR1 path on API 29 (WallpaperManagerCompat.java:37/43); its JobScheduler variant is unused.

**Holds and bounded waits:**
- The holds write nothing when unchanged: AnimationHolds.java:43-88, NotifQuietHold.java:34-61, RadioIconHold.java:172-199.
- RadioIconHide.offThenSync waits at most 4 s (RadioIconHide.java:153-170). deferRelease uses an explicit state (:125-139).
- AnimationSpeedup schedules 2 follow-ups per unlock (:146-147) and has a 10 s failsafe (:100).

**Logs and writes:**
- LockLog is a 48/32 KB ring on one background executor (LockLog.java:17-18, 51-77).
- HelperLife is capped at 48/24 KB (HelperLife.java:19).
- DIRTY lines are written only on change and rate-limited (RecentsDirty.java:18, 22-31; TjAccessibilityService.java:1997).
- NotifSightings uses a background executor, apply() and a rate-limited log (NotifSightings.java:113-152).
- Radio writes happen only when a radio is on (ReturnReset.java:292-304, LockQuiet.java:115-127).
- RecentsSweep coalesces requests (RecentsSweep.java:139-177).
- SettingsSpoof.syncSoon is coalesced and does nothing when already in state (SettingsSpoof.java:89-96, 109).
- apply() is used on frequent writes: ProcessLife.java:111-116 and Mode.java:20-24.
- TabletReport and the helper's SHOTS / APKS / PROBE run only on demand (TabletReport.java:195-236).

**Not verified in this review:**
- how long a refused helper connect takes on the tablet (C3)
- the tablet's fsync cost (C6, C7)
- whether PACKAGE_USAGE_STATS is really denied (TR:612)
- the real number of prefs writes per lock (C7)

One debug counter line in the lock log answers all four before any fix.
