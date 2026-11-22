# CW1 audit, slice C: RAM / CPU / BATTERY of our own processes (2026-11-22)

Read-only audit of `com.tj.launcher` at v0.9.17 (versionCode 56, unified launcher ON in release). Nothing was edited, built or run.
Question: what does the app cost while the tablet is idle and while it is used, and what can be cut WITHOUT losing speed or function?

Evidence marks: **[V]** verified in the code (file:line), **[I]** reasoned from the code and the platform source, **[T]** only the tablet can show.
Tablet numbers on file are from the report of 2026-11-10 (v0.9.1: PSS 69 MB, Java heap 5 of 128 MB, onCreate 48 ms, 45,521 accessibility events in 121 min = 6.3 per second, keep-alive OFF). Nothing newer exists, so every v0.9.12 .. v0.9.17 cost is [T].

## 0. Verdict

The standing cost is already small. Steady state is event-driven:
- **No** AlarmManager, JobScheduler, WakeLock, `java.util.Timer`, ScheduledExecutor or periodic `Handler` loop in the app or in launcher3 [V: grep over `app/src/main`, `launcher3/src`, `iconloaderlib`; the only WakeLock hit is a lint cache binary].
- The only free-running timer in the APP process is `VpnOff.poll` (30 s), and it exists only while its switch is on AND the world is "quiet"; the only other one (helper BEAT, 30 s) lives in the adb helper process, not in the app.
- One foreground service is declared (`KeepAliveService`) and is OFF by default (`KeepAlivePolicy.DEFAULT_ON=false`, :14).
- `largeHeap` is `@bool/config_largeHeap` = false, `hardwareAccelerated=true` (needed), `:wallpaper_picker` / `:wallpaper_chooser` are separate processes that run only when used.
- Idle wakeups caused by US, screen off: about 0 per hour (only conditional VpnOff polls, and those use uptime so they freeze in suspend). Screen on and idle: the cost is the accessibility event stream (about 6 events per second [V 2026-11-10 report]), 85 % dropped at 5 to 10 microseconds each by `EventNoise.isNoise` [V in code, I on cost].
- **No HIGH finding.** The items below are small wins or decisions for Tj. Most are carried from earlier audits (PERF_AUDIT 2026-11-13, CG1B 2026-11-17) and re-verified here; new ones are marked NEW.

## 1. Findings, ranked by saving

Saving = what the change would remove from the standing / in-use cost. Risk = risk to function if done.

| Id | Sev | Where | Cost today (period / wakeups per hour / bytes) | Fix | Risk to function |
| :- | :- | :- | :- | :- | :- |
| C1 | MED | `TjAccessibilityService.updateKeyFilter` TAS:1765-1779; `FailsafeKeys.konami` FailsafeKeys:26 (default ON); `res/xml/tj_accessibility.xml` `canRequestFilterKeyEvents` | In the custom launcher `FLAG_REQUEST_FILTER_KEY_EVENTS` stays set ALL DAY [V: `want = ... Mode.isCustom ? FailsafeKeys.konami : ...`]. While it is set system_server routes EVERY input event (touch included) through `AccessibilityInputFilter` and re-injects it: about 0.1 to 0.3 ms of system_server per event and one extra thread hop [I, same as CG1B E12]. Idle cost 0 (no input, no cost); use cost = every touch frame. Not measurable on the emulator [T] | Decision for Tj, not a code fix: (a) keep (current); (b) make the Konami failsafe ARM only while it is needed (see the lock-out risk), e.g. only while the lock screen or the Launcher's own error state is up. Cheapest safe variant: leave as is, ask Tj whether he ever used the Konami key code | HIGH if it is wrong: the Konami code is the "never locked out" failsafe into Quickstep (CLAUDE.md lock-safety set). Do NOT change without Tj |
| C2 | MED | `TjAccessibilityService.onAccessibilityEvent` :67-106 and `tj_accessibility.xml` eventTypes (`typeWindowContentChanged` is the volume driver) | About 6.3 events per second reach the process while the screen is on [V 2026-11-10: 45,521 / 121 min]; each is a binder oneway call plus a main-thread message BEFORE `isNoise` can drop it. 85 % are dropped. Cost of the delivery itself (wakeup of a binder thread, Handler hop on the main thread shared with Launcher3) [I about 30 to 60 microseconds each = about 1 to 2 % of one core while the screen is on]. Screen off: near 0 | Narrow the subscription at RUNTIME with `setServiceInfo` (as `updateKeyFilter` already does): drop `typeWindowContentChanged` while nothing consumes it (custom home or drawer in front with no cover world, no menu, no lock screen) and restore it on window-state change / lock / power key. Same idea as CG1B E1/E2 (still open) | HIGH: the lock-screen patch, pencil cover, PIN-pad and power-menu detection all hang on content events from SystemUI; a race here shows the stock Screenshot entry or a flash. Needs the lock core gate and the 3 patch sections. Recommend NOT now (saving about 1 percent of a core) |
| C3 | MED | `HomeProfile.syncNow` HomeProfile:85-104 -> `HiddenApps.reloadLauncher` HiddenApps:122 -> `LauncherModel.forceReload` LauncherModel:309; `LauncherProvider` keeps BOTH layout DB helpers open (`mProfileHelpers`) | NEW cost of CV1: every mode flip that changes the profile (plain PIN after Quickstep, double Home, lock-time return) reloads the whole workspace + all-apps model: one loader pass, every icon bound again, a burst of about 0.3 to 1 s of the `launcher-loader` thread and several MB of short-lived bitmaps [I]; a few flips a day, so about 0 per hour standing. Standing RAM: two SQLite helpers/connections stay open (about 100 to 300 KB) [I] | (a) measure on the tablet first (CV1h) [T]; (b) if the reload shows in the report, skip it when the next profile's visible item set equals the loaded one (they differ by design, so only the hidden-apps part could be saved); (c) close the INACTIVE profile's helper after a flip | MED to HIGH: the review rounds of CV1 found 7 HIGH defects in this area (ghost widgets, shared pinned shortcuts, stale writes). Do nothing without a tablet number |
| C4 | LOW | `VpnOff.poll` VpnOff:204-211, `VpnOffRules.POLL_MS` = 30,000 | Only with the VPN switch on AND quiet world (level-triggered, self-stops when either goes). Each tick: `AnimationSpeedup.syncSoon` = the whole level pipeline (NotifQuiet, RadioIconHide, animation hold, VpnOff look) re-reads its facts: about 6 to 12 binder / settings reads on the `tj-anim` thread [I]. At most 120 ticks per hour; uptime-based, so 0 in suspend. On the tablet the quiet world is mostly SCREEN OFF with the CPU asleep, so the real count is much lower [T] | Replace the poll by the `registerNetworkCallback` the class ALREADY has (VpnOff:275-289, `cb` on `tj-vpn-off`) and keep the poll only as a 5-minute backstop; or make the tick run only `VpnOff.look`, not the whole pipeline | LOW: an unnoticed VPN would stay up up to 5 min longer if the callback missed it; only matters if Tj uses the VPN switch (v0.9.x feature) |
| C5 | LOW | `TjAccessibilityService.scheduleLayerRefresh` TAS:664-668 -> `refreshLayers` :1458 | On every allowed window event in a patch world (power mode wallpaper, or a patch window exists): one `refreshLayers` at most every 150 ms plus one trailing at 250 ms. PERF_AUDIT S9/B5 counted about 9 binder calls per refresh (rotation, `getRealMetrics`, wallpaper ids) [V in the earlier audit, not re-measured]. Fires in bursts (opening the drawer, shade) not at idle | The earlier audit's S9: cache rotation + metrics + wallpaper id behind the display-change / configuration callbacks that already exist (ConfigMonitor, rotation observer) and refresh in `refreshLayers` only on change | MED: stale metrics would misplace the patch after rotation; needs section `rt`/`hp` |
| C6 | LOW | `launcher3` `Utilities.THREAD_POOL_EXECUTOR` Utilities:165 (core `CPU_COUNT + 1` = 5 on the 4-core MT8168, no `allowCoreThreadTimeOut`) and 5 app threads that never end: `LockLog` `tj-lock-log` (LockLog:51), `NotifSightings` `tj-notif-seen` (:113), `SettingsSpoof` `TjSsSync` (:83), `AnimationSpeedup` `tj-anim` (:183), `VpnOff` `tj-vpn-off` (:65, only with the switch) | Up to 5 + 4 idle threads for the process life. Each costs a native stack and TLS, resident about 20 to 100 KB, virtual 1 MB [I]; no CPU. Same as PERF_AUDIT M7 | `allowCoreThreadTimeOut(true)` with a 10 s keep-alive on the two logging executors (the pattern of `RecentsFilter` :184-191) and on the Launcher3 pool; keep `tj-anim` | LOW: a thread is re-created on the next task (microseconds); the lock log must stay queued and in order (single thread keeps that) |
| C7 | LOW | `TjAccessibilityService` curtain capture TAS:2170-2260 (`scheduleCurtain` / `buildCurtainNow`, `PixelCopy`) | After each home visit at most one every 15 s: a full Launcher window copy, about 2.4 MB native + GPU readback, then a PNG/crop on a worker thread [V in PERF_AUDIT M3/S11, code re-read here]. Only in a patch world; screen-on only | Earlier audit's M3: copy only the strip the curtain needs (the patch region) instead of the whole window | MED: the curtain must match the stock pixels (section `cu`); keep as is unless a tablet report shows battery or jank |
| C8 | LOW | `launcher3/iconloaderlib` `BaseIconCache` (`mCache` HashMap, :102; `addIconToDBAndMemCache` always puts) | Icons of HIDDEN apps stay in the memory cache (16 KB each x the hidden count; PERF_AUDIT M4 estimated 0.1 to 0.6 MB) [I]; trimmed only by Launcher3's own `onTrimMemory` (Launcher.java:1867) | M4: put an icon in the memory cache only if the package is shown (`HiddenApps.isPackageShown`) | LOW: a later unhide re-loads the icon from the DB (one disk read) |
| C9 | LOW | Helper process: `ShellServer.logcatWatch` ShellServer:418-421 (`logcat -b main -b system -b crash -v threadtime`, Java regex per line) and BEAT 30 s :364-368 | Only while the adb helper is alive (the tablet has been running without it since the report; every helper feature degrades). While alive: the whole system log is streamed through a Java process, about 100 to 1,000 lines per minute on a busy tablet [I], plus 120 BEATs per hour (each reads `/proc` and appends a line to a file in `/data/local/tmp`: about 120 small writes per hour) [V]. This is the helper's own process, not the app's | Pass `logcat` the filter specs it needs (`-s` / `*:S tag:level`) so the kernel of logd does the filtering; raise BEAT to 60 s | LOW: the flight recorder (HLP1 evidence) would carry fewer system lines; keep the tags it parses today |
| C10 | LOW | Build: `app/build.gradle.kts` minify / shrink OFF (deliberate, BRIEF), no `resConfigs`, `launcher3` carries unused Launcher3 resources and locales; baseline profile has wildcards | APK size and first-start dex loading only; no standing cost. `onCreate` was 48 ms on the tablet [V 2026-11-10] | Nothing to do for battery. If cold-start time matters later: `resConfigs "en"` (about 1 to 3 MB of strings) and an R8 pass with Launcher3 keep rules | HIGH for R8 (reflection in Launcher3, accessibility service by name): not recommended. `resConfigs` LOW |

Not reported as findings because they are already minimal (see section 3): everything not listed above.

### Ranked recommendation
1. Nothing in C1 to C10 should be done blind. The standing cost is a few tenths of a percent of battery per day at most [I, T].
2. Cheapest safe wins if Tj wants "polish" now: C6 (core thread timeout), C8 (hidden icons out of the memory cache), C4 (VpnOff poll to a 5 minute backstop). Each is a few lines, JVM-testable (pure rules), low risk.
3. C1, C2, C3 are decisions: they need a fresh tablet report (v0.9.17) with the per-feature numbers, and C1 needs Tj's word on the Konami failsafe.
4. Ask Tj for a fresh `tablet-report.txt` from v0.9.17 before spending on any of these: the numbers on file are from v0.9.1.

## 2. All recurring timers / receivers / observers

Legend: "screen off" = what it does while the screen is off. Triggering: E = event-triggered, L = level-triggered (re-reads the facts), P = periodic. Self-stop: does it end by itself.

### 2a. Periodic or self-re-arming timers (app process unless noted)

| # | Where | Period | Wake cause | Screen off | Trig. | Self-stops |
| :- | :- | :- | :- | :- | :- | :- |
| T1 | `VpnOff.poll` VpnOff:204-211 on `tj-vpn-off` | 30 s | arms after the world is quiet and the VPN switch is on | uptime clock: frozen in suspend, runs while the CPU is awake | P (L inside: `syncSoon`) | yes: stops when the switch goes off or the world is not quiet |
| T2 | helper BEAT `ShellServer` :364 (helper process, not the app) | 30 s | helper alive | same | P | ends with the helper |
| T3 | `PickerGuard` failsafe PickerGuard:114 | `FAILSAFE_MS` re-arm | a share / picker sheet is up | n/a | E | yes: removes itself when the picker is gone |
| T4 | TAS bounded re-arm loops: TAS:463 (quiet wait), :496 (pad calm), :830 (foreign window recheck), :2392 (50 ms), :2416 / :2425 (settle 40 ms, cap 1.5 s), :2489 (pencil wait), :2857 (gear learn retry), :3991 (20 ms), :4043 (30 ms, deadline + 2 s), :4058 / :4192 / :4203 (40 ms, recents clear), :5599 (auto-shot 2 s x 12) | 20 ms to 2 s | a window event or a user action starts it | all end at a deadline or a counter [V: every loop checks a deadline / try count] | E | yes, bounded |
| T5 | other one-shots: `AnimationSpeedup` (5 sites :119-130, :172-173; the tick is driven by window events and gated at 500 ms, no timer of its own), `SettingsSpoof` (2), `RecentsSweep` (2), `HomeProfile` (1), `DexoptWatch` (1, 20 s after start, then at most every 6 h / 2 h after a failure), `BatterySaverGuard`, `RecentsFilter`, `RecentsGuard`, `ReturnReset`, `TjHooks`, `SsPagesSound` | one-shot | an event | n/a | E | yes |
| T6 | TAS one-shots, the rest of the 106 `postDelayed` sites | 20 ms to a few seconds | window events, unlock, lock, power | n/a | E | yes |
| T7 | launcher3: 19 `postDelayed` sites, no `Timer` / alarm | one-shot | user input, model load | n/a | E | yes |

Totals: **2 free-running periodic timers** (T1 conditional in the app, T2 in the helper), **about 14 bounded re-arm sites** (T4), **130 `postDelayed` sites in the app** (106 in TAS + 24 elsewhere) and **19 in launcher3**. **0** AlarmManager / JobScheduler / WakeLock / `Timer` / ScheduledExecutor. **1** foreground service declared (default OFF).

### 2b. Broadcast receivers

| # | Where | Filter | Lifetime | Screen off |
| :- | :- | :- | :- | :- |
| R1 | TAS `wallpaperReceiver` :633 | `ACTION_WALLPAPER_CHANGED` | service life | idle, wakes only on a wallpaper change |
| R2 | TAS `packageAddedReceiver` :634 | `PACKAGE_ADDED` (scheme package) | service life | idle |
| R3 | TAS `closeDialogsReceiver` :635 | `ACTION_CLOSE_SYSTEM_DIALOGS` | service life | wakes on Home / power key |
| R4 | TAS `lockReceiver` :3027 | screen on / off / user present (conditional `lockReceiverOn`) | while wanted | wakes at screen off / on (the event the lock path needs) |
| R5 | TAS `radioReceiver` :3706 | Wi-Fi / Bluetooth state | service life | wakes only when a radio changes |
| R6 | `EntranceWatcher.powerReceiver` :59 | power / screen | process life | wakes at screen on / off |
| R7 | `SsHomeSummary` :46, R8 `SsPagesBattery` :119, R9 `SsPagesSound` :185 | `BATTERY_CHANGED` / volume | only while that Settings page is open (registered / unregistered by the page) | none |
| R10 | launcher3 `LauncherAppState` mModel :100 | the managed-profile actions (the rest of its filter was not read) | process life | idle |
| R11 | launcher3 `ConfigMonitor` :77 (+ DisplayListener :81 on the background looper) | `CONFIGURATION_CHANGED`, display changes | process life | idle |
| R12 | launcher3 `InvariantDeviceProfile.OverlayMonitor` :583 | `OVERLAY_CHANGED` | process life | idle |
| R13 | launcher3 `Launcher.mScreenOffReceiver` :378, R14 `WorkspaceAndHotseatScrim` :232, R15 `WallpaperOffsetInterpolator` :168 | screen off / wallpaper | while the Launcher activity (or its resumed state) lives | wakes at screen off |
| R16 | launcher3 `LoaderTask` SdCardAvailableReceiver :778 | `BOOT_COMPLETED`, one-shot, only if SD packages were pending | until fired | idle |
| R17-R19 | manifest: `INSTALL_SHORTCUT` (enabled via `enable_install_shortcut_api`), `SESSION_COMMITTED`, `APPWIDGET_HOST_RESTORED` | those actions | declared, started by the system only on the broadcast | idle |

Total **19 receivers**: 9 process / service-lifetime (R1-R6 incl. R4 conditional, R10-R12), 7 scoped to an open screen or the Launcher activity (R7-R9, R13-R16), 3 manifest-declared. Two more `registerReceiver(null, ...)` calls in `BatterySaverGuard` (:121, :383) are one-shot sticky reads of `BATTERY_CHANGED`, not registrations.

### 2c. ContentObservers, preference listeners, callbacks

| # | Where | Watches | Lifetime | Screen off |
| :- | :- | :- | :- | :- |
| O1 | TAS `tileObserver` :3630 | `Settings.Secure` `TileProfile.SETTING` | service life | fires only when the setting changes (our own writes) |
| O2 | `BatterySaverGuard` :53 | `Settings.Global` `low_power` | process life | fires on a saver flip |
| O3 | `BatterySaverGuard` :58 | `Settings.Secure` saver count | process life | idle |
| O4 | `EntranceWatcher` rotation observer :53 | `ACCELEROMETER_ROTATION` | process life | idle |
| O5 | TAS `shotObserver` :5627 (descendants on `MediaStore.Images`) | any new picture | ONLY during a patch calibration (`startShotWatch` / `stopShotWatch`, :5619-5633) | not registered |
| O6 | `SsWatcher` :30, `SsPagesSound` :187-188 (descendants on `Settings.System`) | look-alike Settings values | only while that page is open | none |
| O7 | launcher3 `SecureSettingsObserver` :58, `BaseFlags` :251 | Secure / Global flags (dots, debug flags) | per use | idle |
| P1 | TAS `prefWatch` :631-632 | SharedPreferences `tj_mode`, `tj_features` | service life | fires on our own writes only |
| P2 | launcher3 `RotationHelper` :88, `SettingsActivity` :90 | Launcher prefs | home / screen scoped | idle |
| N1 | `VpnOff.registerWatch` :284 `registerNetworkCallback` on `tj-vpn-off` | VPN network | only with the switch on | wakes when a VPN network appears or goes |
| A1 | `LauncherAppsCompatVL` :111 `registerCallback` | package add / remove / suspend | process life | idle |
| A2 | accessibility service `tj_accessibility.xml` | `typeWindowStateChanged | typeWindowsChanged | typeWindowContentChanged | typeViewClicked | typeNotificationStateChanged`, `notificationTimeout=0`, flags `RetrieveInteractiveWindows | IncludeNotImportantViews | ReportViewIds`, `canRequestFilterKeyEvents` | while enabled | screen off: SystemUI produces few events; the keyguard changes still arrive |
| A3 | `NotificationListener` (manifest, `enabled=@bool/notification_dots_enabled`, true in values-v26) | all notification posts, ONLY if Tj granted access | n/a | [T] whether it ever binds on the tablet (no listener on Go per STEP 17) |

Totals: **content observers 4 process-lifetime + 1 calibration-only + about 4 scoped**, **2 SharedPreferences listeners in TAS** (+ 2 scoped in launcher3), **1 network callback**, **1 display listener**, **1 LauncherApps callback**, **1 accessibility service**. **Grand count of timers + receivers + observers/listeners/callbacks: about 55** (2 + 14 timers + 19 receivers + about 11 content observers + 4 pref listeners + 3 callbacks + 3 service-level hooks, counted from the greps; exact to within a few), of which only **2 are free-running** and **0 wake the CPU on their own** in the app process.

## 3. Checked and already minimal (do not touch)

- **Accessibility event path**: `onAccessibilityEvent` TAS:67-106 drops noise first (`EventNoise.isNoise`, `needsPowerMenuScan`, `tallEnoughForPanel`, `pencilSearchOnEvent`); the heavy scans run on a worker thread or behind a quiet gate [V]. PickerGuard / RecentsFilter per-event cost is one string compare; their timers are one-shot and the label pool times out its core thread after 10 s (RecentsFilter:187).
- **Level pipeline** `AnimationSpeedup.syncNow` -> NotifQuiet / RadioIconHide / VpnOff / animation hold: shared `Look` with a 3 s grant memo; the heartbeat tick runs only on window events with a 500 ms gate; every trigger re-reads, nothing polls [V].
- **`TjApplication.onCreate`** (CrashGuard, ProcessLife, `AnimationSpeedup.reconcileAsync`, `SettingsSpoof.syncSoon`, `SwitchMigration`, `EntranceWatcher.start`, `LockQuiet`, `BatterySaverGuard.start`, `DexoptWatch.refreshSoon`): all asynchronous; 48 ms on the tablet [V 2026-11-10].
- **`DexoptWatch`**: one `dumpsys package` spawn 20 s after start, then at most every 6 h (2 h after a failure) [V DexoptWatch:28-47, 89].
- **`LockLog`**: one daemon executor, queued, 48 KB ring, trimmed; writes happen at events (lock, unlock, mode change), not on a clock [V LockLog:51-84]. Disk writes in steady state: none. SharedPreferences are rewritten whole at a preference write, and the writes are at events (a mode flip, a hold, a measurement), a few to a few dozen a day [I].
- **`HelperLife` flight recorder**: only in the helper process; the app's files are untouched.
- **Services**: `KeepAliveService` is not exported and OFF by default; the quick-settings tile service and `ReturnTile` run only on bind.
- **Manifest**: `largeHeap` off, `LauncherProvider` exported only with permissions, the Grid and TestInfo providers disabled, `SettingsAlias` disabled by default, `:wallpaper_*` in their own processes (created on use). The hot path (Launcher3 workspace) uses hardware layers only where Launcher3 asks.
- **Launcher3 caches**: `BaseIconCache` initial capacity hint 50 is fine (only C8's hidden icons add to it); `ConfigMonitor` / `OverlayMonitor` are single registrations; `FileLog` is disabled on user builds [V].
- **Bitmaps**: patch masks ARGB_8888 per window are PERF_AUDIT M5 (kept NEXT: ALPHA_8 pixel equality on MediaTek unproven; 137 KB kept + 137 KB garbage per window, a few windows a day); the curtain is C7. No unbounded static Bitmap found in the app [I].
- **Key-event hop** is the only per-input cost we add (C1).

## 4. Idle budget (estimates, all [I] unless marked)

| State | Our wakeups / hour | Disk writes / hour | Spawns / hour | Notes |
| :- | :- | :- | :- | :- |
| Screen off, CPU asleep | about 0 to 10 (receivers R3-R6 at screen off / on and the occasional system broadcast; T1 conditional, uptime-clocked) | 0 to 2 (a SharedPreferences write at a lock-cycle fact) | 0 | the process mostly stays cached; nothing of ours holds a wakelock |
| Screen on, idle on the home screen | event delivery about 6 per second (22,000 per hour [V rate]); 85 % dropped | 0 | 0 | the cost is C2; no timer ticks |
| Screen on, in use | the same plus one `refreshLayers` per burst (C5), the occasional curtain copy (C7) | a few small preference writes | 0 | C1 adds a hop per input event |
| Helper alive (optional) | 120 BEAT per hour + the logcat stream (C9) | about 120 small appends | 0 per hour (one logcat process for the helper's life) | not in the app process |
| Dexopt / package look | at most 1 `dumpsys package` per 6 h | 0 | 0.17 per hour | `DexoptWatch` |

## 5. Memory picture (what is held and why it is fine)

- Process: PSS 69 MB, Java heap 5 of 128 MB on the tablet [V v0.9.1]. Launcher3's workspace and all-apps icons dominate; the icon cache has no size bound but is trimmed by `onTrimMemory`.
- Statics that survive the process life: the per-process SharedPreferences handles (`Mode.cached`, `Features.p`), the loaded `HomeProfile` state, the lock log ring (48 KB), `AppOrigin` / `TaskOrigin` maps (package keyed, small), `QsMemory`, the pure-rule objects. No static holds an Activity or a View on purpose [I; the activity-scoped receivers are unregistered in their pages, `SsHomeSummary:54`, `SsPagesBattery:126`, `SsPagesSound:199`].
- Threads: C6. TAS `onTrimMemory` :3740 drops the patch and curtain bitmaps; the instance is nulled at :3731 / :3754 so a dead service does not hold windows.
- Bitmaps held while the screen is on in a patch world: the lock patch crop, the home patch masks (M5) and the curtain frame (C7), together about 0.5 to 3 MB [I]; none while the custom home is in front without a patch world.

## 6. Could NOT be checked (needs the tablet or a run)

1. **Real CPU / battery per feature on v0.9.17** (no tablet report since v0.9.1): `dumpsys batterystats --checkin`, `dumpsys procstats`, `top -H -d 1 -n 60` per thread, with the unified switch on. [T]
2. The per-event cost of the key filter (C1) and of event delivery (C2): the emulator's `AccessibilityInputFilter` path is the same code but its CPU is not the MT8168's. [T]
3. The size of one unified reload (C3): loader-thread time and heap peak at a profile flip. [T]
4. Whether `NotificationListener` ever binds (A3), whether the helper is alive in daily use, and the Bluetooth radio path (no BT on the emulator). [T]
5. Exact resident size of the idle threads (C6) and of the second DB helper (C3): estimated from stack and page sizes, not measured. [T]
6. Deep-sleep behaviour of `VpnOff.poll`: it uses `uptimeMillis`, which stops in suspend; how often the quiet world has the CPU awake is [T].
7. Not read line by line: `AppOrigin`, `TaskOrigin`, `RecentsSweep`, `QsMemory` and `ProcessLife` internals (only their thread / timer sites were checked); the ~60 `Ss*` look-alike pages (they register only while open, spot-checked 4); `eleven/` (a separate app, not our process).
8. The counts in section 2 come from `grep` of `postDelayed`, `registerReceiver`, `registerContentObserver`, `registerOnSharedPreferenceChangeListener`, `Thread` / `HandlerThread` / `Executors` over `app/src/main` and the launcher3 sources; a registration made through a helper class that the grep patterns do not name would be missed (none seen).

## 7. Suggested next step

Ask Tj for a fresh `tablet-report.txt` from v0.9.17 (it already carries PSS, heap, event counts and the radio / mode lines), then decide C1 (Konami) with him. Until then do only the three no-risk polish items (C6, C8, C4) if CW1b wants any code from this slice.
