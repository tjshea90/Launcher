# CW1 audit D: the stock launcher's sleep (read-only audit, 2026-11-22)

Slice D of TASKS CW1a. Question (owner, verbatim intent): "proper sleep of the no longer used quickstep launcher so that it uses as little ram cpu and battery as possible because it is no longer needed".
Head read: `b6844bff` (v0.9.17 code, `Unified` ON by default in release). Nothing was edited, built or run; every number below is quoted from a repo document, a commit message or a report, or is reasoned.

Tags: **[V]** verified in this repo's code or docs (file:line named); **[M]** measured (run / report named); **[I]** reasoned, not measured; **[T]** only the tablet can show.
Naming trap kept from CV1 audit B: `com.android.launcher3.*` classes inside our APK are OUR port; "the stock launcher" here is the system app `com.android.launcher3` (MediaTek `MtkLauncher3GoIconRecents`, priv-app, uid 10083 in the 2026-11-10 report) [V research/tablet/2026-11-10-2/tablet-report.txt:33].

---------------------------------------------------------------------------------------------------------

## 0. Bottom line

1. **The stock process cannot be put to sleep, only made small.** SystemUI binds the stock `TouchInteractionService` with `BIND_AUTO_CREATE | BIND_FOREGROUND_SERVICE_WHILE_AWAKE` for ever [V, AOSP read in research/CG1F_QUICKSTEP_AOSP_ANGLE_2026-11-17.md section 2.1-2.2]: the process is never cached, sits at oom adj 100, state BOUND_FOREGROUND_SERVICE (awake) / IMPORTANT_FOREGROUND (asleep), is not `persistent`, and gets no BACKGROUND / MODERATE / COMPLETE trims. The lowmemorykiller reaches it only after every cached, perceptible and service process is gone [I, arithmetic on the quoted code]. A killed process is re-created by SystemUI within seconds [M, run #715: pid 2564 -> 4208 read about 3 s after the drive with nothing opened, commit 25c715ed; run #714: 2569 -> 4216, commit 38aaaa6a]. So "sleep" = the smallest possible footprint (the floor: service + RecentsModel, no Activity, no GL context, probably no `LauncherAppState` [I]) plus the fewest wake-ups.
2. **CV1 (v0.9.17) already removed the biggest cause.** In the one-launcher world the stock HOME activity is no longer started in normal operation [V RouterActivity.java:52-54, 80-83; `startStock()` only runs in the emergency exit, on a routing failure and on a late "stock" pre-arm]. What is left is the Go `IconRecentsView` listener pin per `RecentsActivity` re-creation (R1 of CG1F) [V code read, magnitude I], created by Recents opens after a configuration change (battery-saver flips) and by our own Recents clear-all at returns.
3. **The existing machinery (SR1 helper restart, SR2 Settings Force stop) is mechanically right and still the only cure, but its trigger is stale.** The SR2 debt counts every ended Quickstep session and every `low_power` flip; neither creates stock Activities any more unless a Recents instance exists. It will still reach 12 within a day and fire up to every 4 h: a cover, a cold Settings start, a respawn to the same floor and a cold next Recents, for little gain (D3).
4. **Hard NOs** (D2): disabling the stock HOME activity or `TouchInteractionService` kills Recents (the `OverviewComponentObserver` constructor dereferences the resolved home activity) and the emergency exit; `pm clear`, standby buckets, `RUN_ANY_IN_BACKGROUND`, `always_finish_activities` give nothing or harm.
5. **App-only** (no helper): Force stop through Settings (exists), every wake-reduction fix below, all trigger changes. **Helper-only**: exact size, invisible restart, `send-trim-memory`, task removal by id (which also removes the main wake source, the on-screen Recents clear-all).
6. **Biggest unknown, [T]:** nobody has a stock-process size from the tablet since CV1. The 3-hour / 1-day average on Settings > Developer options > Memory after a day of v0.9.17 decides whether anything beyond D3-D7 is worth building.

---------------------------------------------------------------------------------------------------------

## 1. Why the stock process is alive, what wakes it, what it costs

### 1.1 Who keeps it alive

| # | Mechanism | Evidence | Tag |
| :- | :- | :- | :- |
| K1 | SystemUI `OverviewProxyService` binds the stock `TouchInteractionService` (action QUICKSTEP, `directBootAware`, permission STATUS_BAR_SERVICE) with BIND_AUTO_CREATE + BIND_FOREGROUND_SERVICE_WHILE_AWAKE; the target package comes from `config_recentsComponentName` = stock Quickstep whatever the default home is; failures retry with backoff up to 10 min | CG1F 2.1; CD2_AOSP_Q_LAUNCHER3_FACTS Q1/Q3; CV1 audit B (3) | V |
| K2 | `OomAdjuster`: client SystemUI is persistent, so the service host gets `max(clientAdj, VISIBLE_APP_ADJ)` = **100**, `cached=false`; process state 6 awake / 7 asleep. Screen off does not demote it | CG1F 2.2 (quoted code, arithmetic I) | V/I |
| K3 | NOT `persistent` (stock manifest has no such attribute); MediaTek's own manifest is not public | CD2 facts Q1 | V for AOSP, T for MediaTek |
| K4 | Home-process rule (`HOME_APP_ADJ` 600, `PROCESS_STATE_HOME`) is irrelevant: 100 is already better than 600, and AMS flips `mHomeProcess` to whichever process last started a HOME-type activity. In the one-launcher world that is OURS; the stock process would only become it in the emergency exit. Being the home app gave the stock process nothing extra before either | CG1F 2.3 | V/I |
| K5 | It is started at boot (direct-boot aware service) and at every respawn by SystemUI's death recipient / rebind | CG1F 2.1, 1.3; SF/SR runs | V/M |
| K6 | Our process is at the SAME rung (adj 100: accessibility service bound by system_server) and the home role adds only a `HOME_APP_ADJ` 600 cap; `KeepAliveService` (default OFF, OFF on the tablet in the last report) measured adj 50 on the emulator and does nothing for the stock process | KeepAliveService.java:19-22; KeepAlivePolicy.java:14; tablet-report 2026-11-10-2:622-623, 672 | V/M |

Consequence [I]: when memory is short, lmkd picks the largest process of the lowest rung it has to reach; at rung 100 that would be the stock process first, then it would be re-bound by SystemUI. That "free sleep" only happens under real pressure; the tablet showed 830 MB - 1.0 GB free at the last reading (TASKS CD1l), so it does not happen in practice.

### 1.2 Who starts or wakes it now (our code, unified world, switch ON, breaker not tripped)

| # | Waker | Where | How often | What the stock process must do | Cost / verdict |
| :- | :- | :- | :- | :- | :- |
| W1 | Recents clear-all drive without the helper (`GLOBAL_ACTION_RECENTS`, then Close / Clear all with accessibility) | TjAccessibilityService.java:4020-4023 `resetRecents`, 4164 `recentsEmpty`, 4288 `recentsClearAll`; started by `ReturnReset.run` when `recentsNeeded` (ReturnReset.java:60-65) | at every return to custom after a session that used an app (clean start), a few per day [I] | start or re-show `RecentsActivity` (inflate Go layout, load tasks, icons, thumbnails), answer node queries, remove cards | **the main remaining app-caused wake**; one warm sample on the tablet: plan 03:10:51.402 -> "Clear all pressed" 03:10:52.240 = 0.84 s [M tablet-report 2026-11-10-2:734-736, one sample, old world]. With the helper this does not happen at all (invisible `RMTASKS`, ShellServer.java:~170) |
| W2 | Recents watcher: tree walks on every stock window event in custom mode | RecentsFilter.java:86-121; CD2 R3 | only when Recents opens in custom mode; none when the hidden-label set is empty | build a root + prefetch per walk, up to 5 in 3.9 s | transient, small; stays (function) |
| W3 | Package-changed broadcasts of OUR package from the Settings look-alike alias flip | SettingsSpoof.java:48, 167-179 (the only `setComponentEnabledSetting` in the app); CD2 R7 | 2 per Quickstep session (alias OFF on entry, ON on return) | observer re-runs `updateOverviewTargets()` (2 intents, one helper object); `LauncherModel.onPackageChanged` -> `PackageUpdatedTask` icon re-decode only if a `LauncherAppState` exists | tiny; see D8 |
| W4 | Battery-saver flips (`low_power` 1 <-> 0 around every lock) -> uiMode night changes | BatterySaverGuard.java:46 + the guard's own writes; CG1F_CONFIG_CHANGES | several per lock cycle (tablet log: 4 writes in 66 s [M]) | every running process gets the configuration; a RecentsActivity that exists is re-created at its NEXT show (pin) | cannot be removed (AC3 / BA2 lock rule); only the pin effect is manageable (D3) |
| W5 | Deliberate restarts: SR1 `STOCKRESTART` (helper), SR2 Settings Force stop | StockRefresh.java:122-156; StockForceStop.java:252-289; TjAccessibilityService.java:5305-5500 | SR2: up to every 4 h once debt >= 12 | process dies, SystemUI re-creates the floor | section 3 |
| W6 | `startStock()` fall-through: emergency (breaker tripped), our launcher failed to start, route threw, LATE "stock" pre-arm after the mode went back to custom | RouterActivity.java:38-41, 52-54, 80-83, 121-133 | rare | create a full `Launcher` activity: `LauncherAppState`, model, icon cache, a pinned Go view | the late pre-arm case is avoidable (D6) |
| W7 | Task-stack callbacks: every app switch / snapshot / task removal is pushed to the stock `RecentsModel` (a `TaskStackChangeListener`); a task snapshot arrives as a hardware GraphicBuffer wrapped in a `ThumbnailData`, 3 kept in an LRU | CG1F 1.5, 1.6, 3.3 | proportional to app usage, not to our code | binder callback on its main / loader thread; gralloc memory counted in the process | unavoidable while Recents lives there; CPU tiny [I] |
| W8 | SystemUI state callbacks (`onSystemUiStateChanged`, `onOverviewShown/Hidden`, `onBackAction`) | CG1F 1.3 (Go: empty bodies) | per keyguard / shade change | one-way binder, empty | negligible [I] |
| W9 | Our reading of its size (`dumpsys meminfo`) | QsMemory.java:292-322; StockRefresh.java:108-109 | helper: after each ended session + every 25 min of use; no helper: see D7 | shell `dumpsys` walks its smaps | helper only; fine |

Not wakers (checked): `StockLauncher.find` is a package-manager query in OUR process (ReturnReset.java:100, 125, 277; AppOrigin.java:72, 104; TjAccessibilityService.java:3139 cached 60 s); `WallpaperPickerChoice.stockHint` reads the stock APK's resources in OUR process (WallpaperPickerChoice.java:100-108, 158-166); `ResetPolicy.shouldKill` excludes the stock package from every kill (ResetPolicy.java:10-13); `killApps` never touches it (ReturnReset.java:272-290); no `AlarmManager`, `JobScheduler`, `WorkManager`, `Timer` or periodic handler exists in `app/src/main` (CI1 section 1) so nothing of ours schedules work in the stock uid.

### 1.3 What it does while idle, and what each state costs

**Idle work (stock process alive, no Activity shown)** [I, AOSP Q Go sources; MediaTek manifest not readable, so receivers / jobs of the MTK build are [T]]: the bound `TouchInteractionService` with `RecentsModel` (a HandlerThread "TaskThumbnailIconCache", the task-stack listener) and `OverviewComponentObserver` (two dynamic receivers: preferred-activity change, package events of the default home package); no alarms, no JobScheduler job, no widget listening (the host stops listening in `Launcher.onStop`), no content observer that I could find in the Q sources read. If a `LauncherAppState` exists in the process (it does after any stock HOME activity ran: all of the v0.9.16 era), it also holds `LauncherApps` / `PackageInstaller` callbacks, a `ConfigMonitor` receiver, the model with every app icon and the icon-cache database, and re-decodes icons on package events. Widgets that were placed on the stock home screen stay bound to the stock host id after it stopped being used; their providers keep their own periodic updates [I; whether any exist is [T], TASKS CD1h(c) never got an answer].

**Resident size by state** (PSS incl. graphics memtrack where the device reports it):

| State | Size | Tag |
| :- | :- | :- |
| fresh process, service only (the floor) | 10-17 MB on the CI emulator (plain AOSP 10); Go build + MediaTek + Mali: my estimate 20-40 MB | M (CD2 cd4 d0 10.5 MB; CD1 15-17 MB) / I |
| plus one shown-and-stopped Recents activity (warm) | 36-42 MB emulator (flat over 24-80 relaunches there because the plain-AOSP view removes its listener); tablet estimate 40-70 MB incl. an EGL context and up to 7 snapshot buffers | M / I |
| plus the stock HOME activity + model (the v0.9.16 Quickstep sessions) | +12-15 MB per creation on the emulator | M (CD1) |
| Tj's tablet before CV1, "Memory used by apps" | 414 MB 3-h average, 445 MB 1-day, maximum 427 MB, always running 100 %; 280 MB after several force stops (average of a window that still held the old time) | M (TASKS CD1b-result, CD1j) |
| the same tablet model with the stock launcher as default home | 48 MB (reported by the owner) | T |
| one pinned Go activity | a few MB (views, adapter, <= 7 hardware thumbnails); hundreds of MB therefore needs many instances or driver-side graphics memory | I (CG1F 5.2) |

The growth is a step pattern of pinned Activity instances (CG1F R1) plus never-trimmed graphics memory (R2) and a never-compacting heap because the process is "jank perceptible" for life (R3) [V code, I magnitude]. A restart is the only cure, and the 280-450 MB cannot be reproduced on the emulator (every emulator run was flat, CD2 conclusion).

**CPU and battery** [I]: the idle stock process costs essentially nothing (no timers, a few one-way binder callbacks per app switch and per lock / unlock). The energy that is ours to reduce is not its idle draw but (a) each SR2 drive (cover + cold Settings + 3 clicks, roughly 2-14 s of two busy cores, order of 1 mWh each), (b) each Recents clear-all (W1), (c) the respawn after each kill. None is large; the real lever is RAM (a 2 GB tablet with 0.8-1.0 GB free).

---------------------------------------------------------------------------------------------------------

## 2. Every lever, with privilege, effect, verdict, risk

"Helper" = the optional adb shell helper (a process with shell rights). "App-only" = accessibility service, overlay windows, manifest permissions, adb grants already used (the 2026-11-10 report shows WRITE_SECURE_SETTINGS and DUMP granted, PACKAGE_USAGE_STATS not).

| # | Lever | Needs | Effect on the stock process | Verdict | Risk |
| :- | :- | :- | :- | :- | :- |
| L1 | Settings > App info > **Force stop** by accessibility (SR2, exists) | app-only | process killed: every Activity, pin, EGL context, pending alarm / job / intent gone, package marked stopped; SystemUI re-binds and the floor process is back in seconds [M #714 / #715] | **KEEP, re-aim the trigger** (D3, D5) | visible cover 2-14 s, cold Settings (real Settings was 103 MB, TASKS CD1l), next Recents cold; MediaTek Settings labels [T] |
| L2 | `am force-stop` through the helper (SR1 `STOCKRESTART`, ShellServer.java:163) | helper | same as L1, invisible, exact size reading decides | KEEP as is | none new |
| L3 | `ActivityManager.killBackgroundProcesses` / `restartPackage` (KILL_BACKGROUND_PROCESSES, normal permission, declared at AndroidManifest.xml:18) | app-only | kills only processes at adj >= 500; the stock process is at 100 [V CI1 section 3, CG1F 2.2] | **useless for this**; the app uses it only for other apps | none |
| L4 | `am send-trim-memory com.android.launcher3 HIDDEN / RUNNING_CRITICAL` | helper (shell) | HIDDEN: HWUI `trimMemory(UiHidden)` purges the Skia cache (cap about 28 MB on 600x1024), RecentsModel high-res flag off; RUNNING_CRITICAL clears its 3 thumbnails + 12 icons. BACKGROUND / MODERATE / COMPLETE are refused for a foreground-class process [I, search summary in CG1F 5.2]. Does NOT free pinned activities | optional later, unproven; only after a tablet before / after number | low |
| L5 | App standby bucket (`am set-standby-bucket`), `cmd appops set RUN_ANY_IN_BACKGROUND ignore`, Settings Battery > "Background restriction" by automation | helper / Settings UI | limits jobs, alarms, background-started services; the stock process has none, and its service is BOUND by a persistent client, so nothing is saved [I] | **no gain** | unknown side effects on the bound overview service; do not |
| L6 | `pm disable-user` of the stock **HOME activity** | helper | no HOME activity = `OverviewComponentObserver` constructor: `resolveActivity(myHomeIntent)` returns null, then `info.activityInfo.name` throws in `TouchInteractionService` creation [V quoted AOSP, CG1F 1.2]: **Recents dies**; also removes the emergency exit and Safe-mode fallback | **FORBIDDEN** | catastrophic |
| L7 | `pm disable-user` of `TouchInteractionService` / `RecentsActivity` | helper | no overview service: Android 10 SystemUI has no legacy Recents fallback [I] | **FORBIDDEN** | Recents key dead |
| L8 | `pm clear` / Settings "Clear storage" of the stock package | helper / Settings UI | wipes its workspace DB, icon DB, widget host ids (stops any widget providers it still keeps awake [I]) and kills the process like L1; no RAM gain beyond L1 | optional ONE-TIME manual step if a cable session finds widgets bound to uid 10083 | the emergency exit would then show the default layout |
| L9 | `Settings.Global always_finish_activities` ("Don't keep activities") with the granted WRITE_SECURE_SETTINGS | app-only | destroys EVERY app's stopped activities at once; would destroy RecentsActivity on leave, but the listener leak still pins the dead object | **NO** (system-wide damage) | high |
| L10 | `Settings.Global activity_manager_constants`, background process limit | app-only / signature | global process policy; no per-package effect | NO | high |
| L11 | Cancel its jobs / alarms (JobScheduler, AlarmManager) | impossible | both APIs act on the caller's uid only [V AOSP API] | n/a | n/a |
| L12 | Let lmkd do it | none | happens only after every cached / perceptible / service process (K2) | not a plan | n/a |
| L13 | Move the Recents host (`config_recentsComponentName` overlay) | system signature | would remove the stock process entirely | impossible (BRIEF: "Quickstep recents layer not installable without system signature") | n/a |
| L14 | Make fewer wakes: one launcher (done), D6 late pre-arm, D8 alias, helper task removal (no Recents open at returns), D4 so a cold start never fails the clear | app-only (helper for removal) | fewer creations = fewer pins | **DO** (D4, D6; D8 only if measured) | low |
| L15 | Fewer config flips while a Recents instance exists | impossible | the saver flips are Tj's lock rule (AC3, BA2) | no | n/a |
| L16 | Read its size app-only | n/a | `ActivityManager.getProcessMemoryInfo` / `getRunningAppProcesses` cannot see another uid on Android 10 [I]; `dumpsys meminfo` and `dumpsys activity processes` answered nothing to an app uid (emulator runs #621-#623: "Can't find service: meminfo", 32 chars) [V TASKS CG1f FINDING, OUTBOX.md:1409]; scraping Settings > Developer options > Memory by accessibility gives a lagging 3-h average and is fragile | **no**; the size stays an estimate without the helper (D10) | n/a |

---------------------------------------------------------------------------------------------------------

## 3. The current machinery, judged

### 3.1 StockForceStop (SR2, helper-free) and StockRefresh (SR1, helper)

What triggers them [V]:
- `Mode.java:109-110`: at every Quickstep -> custom change `StockForceStop.afterQuickstepSession` (+1 debt, then a daemon thread sleeps 45 s and calls `consider`, StockForceStop.java:229-249) and `StockRefresh.afterQuickstepSession` (45 s thread; returns at once when no helper runs, StockRefresh.java:108).
- `BatterySaverGuard.java:46, 54` -> `StockForceStop.noteLowPower`: +1 per CHANGE of `low_power` (StockForceStop.java:83-90).
- SR1 also from the memory recorder (`QsMemory.refreshAsync` -> `StockRefresh.consider`, QsMemory.java:321; 30 s after each screen-on at most every 25 min, TjAccessibilityService.java:481, 3009).
- Conditions (all pure, StockForceStop.java:98-125): switch ON, fewer than 2 failures, helper NOT running, debt >= `MIN_DEBT` 12, custom mode, custom home in front, screen on and unlocked, automation not paused, >= 4 h since the last drive (`COOLDOWN_MS`, line 31); the service then waits at most 12 s for a quiet home and covers the screen (TjAccessibilityService.java:5410-5444).
- SR1: helper-read size >= 200 MB or >= 6 Activities, custom home in front, >= 6 h since the last (StockRefresh.java:33-36, 62-73).

Is it still correct? **Mechanically yes** [M: `sf` SF1-SF8 and `sr` SR0-SR8 pass; #714 / #715, gate #716 / #717 green on 767e95aa]. Does it still match the cause? **Only half.**
- SR1 is self-calibrating (it reads the real size and Activities), so after CV1 it simply rarely fires. Right behaviour, no change needed; its 200 MB line is generous for a 2 GB tablet (a healthy Go launcher with Recents is 40-70 MB [I]); lower it to about 120 MB / 4 Activities only if the tablet numbers show a plateau below 200 MB (cost of a restart: one cold Recents, about a second [I]).
- SR2 estimates with a debt that was designed for the two-launcher world, where every Quickstep session created a stock HOME activity and every saver flip re-created the VISIBLE stock activity. Now the Quickstep home is ours; the stock process only has a `RecentsActivity` to pin, and a stopped activity is re-created once at its next show, not at each flip (CG1F 4.4, CI1 section 3). So: session ends overcount (they create nothing), flips overcount (they pin nothing unless a Recents instance exists AND is shown afterwards), and a process that nobody opened Recents in since its last restart cannot have pinned anything (nothing to give back). Debt 12 is reached in about 4-6 lock cycles, so the drive can fire up to every 4 h of use.
- Cost per SR2 drive [V code, I numbers]: a full-screen cover (picture of the home, else black) raised first, touches swallowed, the real Settings process cold-started (about 100 MB, then cached), App info page, two clicks, BACK / HOME until our window is back; capped at 14 s + leave, cover failsafe 20.5 s (StockForceStop.java:38-42). Emulator drive time 1.7 s [M #715]; tablet unknown; CT1 doc admits "a slow tablet may still show a flash". Afterwards the stock process respawns at the floor (RAM about the same as before when it had not grown) and the NEXT Recents open or reset is cold.

What it should do now (answer to "once after each Recents use? never? only on debt?"):
- **Not after each Recents use**: it would make every Recents open cold, cost a cover + Settings per use and free almost nothing (one instance).
- **Not never**: the pin (R1) is real in the Go code and the owner's own force stops took 450 -> 280 -> 130 MB (3-h average) [M]; the pins from the v0.9.16 era stay until the process dies.
- **Only when something can have been pinned**: (1) ONE seeded drive the first time the unified launcher is in force on a device that ran v0.9.16 (D5); (2) afterwards debt counts only Recents shows whose configuration signature (night bit, font scale, density, locale) differs from the previous show of the same process (each such show = one new pinned instance), reset at every restart and at every boot, never counted without a stock window seen since the last restart (D3); threshold about 4-6 pins, cooldown as now; (3) with the helper, the exact reading decides as today.

### 3.2 QsMemory (the recorder)
Helper present: right and cheap. No helper: it still spawns `dumpsys meminfo com.android.launcher3` and then `dumpsys activity processes` (QsMemory.java:313-316) every `FAIL_GAP_MS` = 2 h of use and at every Launcher-control open (forced, 30 s gap): both are known to be refused to an app uid (D7).

### 3.3 Everything else that "pokes" it
Stray-home guard (`guardStrayHome`, TjAccessibilityService.java:3146-3169) now also fights the stock HOME window in the Quickstep world (UnifiedRules.strayStockApplies) - correct, free when nothing appears. `StrayCustomGuard` is off in the unified world (TjHooks.java:88-92). Recents scans (W2) and the helper sweeps are function and stay.

---------------------------------------------------------------------------------------------------------

## 4. Recents when the stock process sleeps: cold vs warm

States the user can meet [I unless marked]:

| Recents press arrives when... | What happens | Estimated first-open cost |
| :- | :- | :- |
| the instance is alive and stopped (warm) | `onNewIntent` + `onResume` on the same `RecentsActivity`, list reload through the task list, thumbnails from the 3-entry cache | 0.15-0.35 s |
| the process is up but never showed Recents since its (re)start (the floor, what SR1 / SR2 leave behind) | cold activity creation: class loading, inflate `GoRecentsActivityRootView` + `IconRecentsFragment` + `IconRecentsView` + RecyclerView, EGL / Skia init on the first frame, `getRecentTasks` and icon loads | 0.5-1.0 s (CT1 doc says "about one second") |
| the process is dead and SystemUI has not re-bound yet | the key does nothing: `onOverviewToggle` needs the bound proxy | a few seconds after a kill ("the Recents button may be dead for some seconds", TASKS CD1f) [M: back within about 3 s on the emulator] |

One real warm tablet sample: the Recents step of a return took 0.84 s from the plan line to "Clear all pressed" [M tablet-report 2026-11-10-2:734-736, old world, stock process alive]. The emulator is not a measure of this (software GL, x86): its "list was empty" step is 0.4-0.6 s warm (CD2 cd4).

**What the code does today about warmth:** nothing keeps it warm (no pre-warm, no keep-alive of the stock side). Two things make it cold on purpose: SR1 / SR2 (kill 45 s after a return, so the NEXT return's clear-all finds a cold stock process: the biggest real consequence), and the process simply being re-created after a kill. The return's own clear-all warms it again as a side effect.

**Hidden coupling (D4):** the clear-all gives itself 2.5 s from the moment it presses RECENTS (`recentsEmpty`: TjAccessibilityService.java:4165; the Close-by-label variant 4027; non-clean-start `recentsClearAll` polls 1800 ms, line 4289). A cold start inside that window leaves about 1.5 s margin against the one warm sample [I]. On a miss the result is "no Clear all button and no card ... found: nothing was cleared" and Recents is NOT marked clean (the BN8d history: "All of my quickstep apps were in the recent apps list in the custom launcher"). So "more sleep" must come with a cold-start-tolerant clock, or the clean-start guarantee gets weaker.

---------------------------------------------------------------------------------------------------------

## 5. Findings

Severity: HIGH = changes what can be promised or can break Recents / the exit; MED = real cost or risk in normal use; LOW = small waste or hygiene.

**D1 (HIGH, decision) A sleeping stock process is not reachable; only a small one.**
File: none (platform). Finding: K1-K3: the SystemUI bind keeps the process alive and un-trimmed, a kill is undone by a re-bind in seconds [M #715]. Fix: tell the owner plainly and define "asleep" as "at the floor, nothing pinned, no Activity, woken only by Recents"; measure that, not "stopped". Risk: none (expectation).

**D2 (HIGH, do-not) Never disable the stock HOME activity or the overview service as a "sleep".**
Finding: L6 / L7 break Recents (constructor dereference, CG1F 1.2) and remove the emergency exit and the Safe-mode / uninstall fallbacks that CV1 audit B (1) lists. `ShellProtocol.allowed()` already refuses the stock package for general commands (ShellProtocol.java:167-181); keep it that way. Fix: one line in HANDOFF.md / BRIEF.md ("never disable or pm-clear com.android.launcher3 automatically"). Risk: none.

**D3 (MED) SR2's trigger is stale after CV1.**
File: StockForceStop.java:29-30, 78-90, 229-249; Mode.java:109; BatterySaverGuard.java:46.
Finding: debt +1 per ended session and +1 per saver flip, drive at 12, every 4 h (section 3.1). In the one-launcher world sessions create nothing in the stock process and flips pin only a Recents instance that exists and is shown later. Result: the drive fires about as often as in v0.9.16 for a floor-sized process: cover + cold Settings + cold next Recents for ~0-30 MB.
Fix (pure rule + JVM tests, `sf` section): (a) keep the old weights only when `!Unified.active(c)` (emergency exit = v0.9.16 world); (b) with `Unified.active`: ignore session ends and plain flips; keep a persisted "stock window seen since last restart" flag set from `RecentsFilter.onEvent` (RecentsFilter.java:106-110, any stock window event) and cleared by `finished(restarted | not running)` and at a new boot (`ProfileSession.checkBoot` already knows boots); keep a persisted signature of the configuration at the last stock window (night bit, fontScale, densityDpi, locale; our own `Configuration` is the global one) and add 1 pin per show whose signature differs; drive at >= 4-6 pins. With no stock window since the last restart nothing can have pinned: blocker "nothing to give back". Risk: LOW (a missed Recents event only delays a drive; the helper path keeps its exact reading).

**D4 (MED) The Recents clear-all clock includes a cold stock start.**
File: TjAccessibilityService.java:4027, 4165 (2500 ms), 4289 (1800 ms); history BN8d.
Finding: with SR1 / SR2 firing 45 s after returns and the stock process at the floor, the next return opens a cold `RecentsActivity` inside a fixed budget [I]. A miss leaves cards in the custom Recents (function the owner cares about). Fix: start the 2.5 s deadline at the first window of the stock package (or at the first node found) with a hard overall cap (about 6 s); log "cold start +N ms" in the existing `RESET recents` line so the tablet report gives the first real cold number. Risk: LOW-MED (reset path: sections `rb` / `cs`; the deadline is checked FIRST in every branch, keep that rule from the BE3 lesson).

**D5 (MED) One cleanup after the switch to the one launcher.**
File: StockForceStop.java (seed), Unified.java.
Finding: pinned stock Launcher activities, a `LauncherAppState` + model and a stale stopped stock HOME task from the v0.9.16 era live until the process dies [I]. If debt was already >= 12 since 2026-11-21 22:59 (v0.9.16 install) a drive may already have happened (the report line "stock launcher Force stop without the helper (CT1 / SR2)" shows the last result) [T]. Fix: when `Unified.active` first becomes true on an install, set `sf_debt` to `MIN_DEBT` once (flag `sf_unified_seed`); the existing quiet-moment drive does the rest. Risk: LOW (same drive as v0.9.16, proven by `sf`).

**D6 (LOW) A late "stock" pre-arm in custom mode wakes the heavy path.**
File: RouterActivity.java:52-54.
Finding: `"stock".equals(pre)` with `Mode.isCustom` true falls to `startStock()` (comment: "exactly as before"): creates a stock `Launcher` activity + model + a pin, then the stray guard presses Home. With `Unified.active` the right target is our custom home. Fix: `if (Unified.active(this)) { if (startOurs(false, false)) return; }` for every "stock" pre-arm. Risk: LOW (`ul` / `ul2`; the emergency path stays as is because `Unified.active` is false there).

**D7 (LOW) The recorder runs two known-refused `dumpsys` spawns without the helper.**
File: QsMemory.java:292-316, FAIL_GAP_MS line 28.
Finding: app-uid `dumpsys meminfo` and `dumpsys activity processes` answer nothing (runs #621-#623); on the tablet DUMP is granted so both still run every 2 h of use and at each Launcher-control open (an exec of `dumpsys`, system_server walking all processes). Fix: without a running helper do not spawn them (keep the status line "needs the helper"); keep the forced look only when the helper runs. Risk: none (no data is lost: it never produced any).

**D8 (LOW) The alias flip is no longer needed in the Quickstep world.**
File: SettingsSpoof.java:48 (`aliasWanted`), 167-179; UnifiedRules.java:67-71 (`shownInUnlocked` already hides the alias in the unlocked drawer).
Finding: the alias exists OFF in the Quickstep world only to keep it out of the STOCK drawer, which is now emergency-only. Two PACKAGE_CHANGED broadcasts per session reach the stock observer and model (W3) and our own model. Fix: a separate predicate "alias enabled" = `switchOn && (customMode || Unified.active)`; `pageAllowed` / `hidesRealSettings` keep `customMode`. Risk: MED-LOW (look-alike gates; sections `ss`, `ul`); gain small. Defer until a tablet number says the flips matter.

**D9 (LOW, [I]) With no preferred home Recents uses the integrated overview.**
File: n/a (platform); B1 comment RouterActivity.java:92-94.
Finding: after a crash of our process Android clears our default-home preference; then `defaultHome == null` makes `OverviewComponentObserver` pick the stock HOME as the overview (CG1F 1.2 condition `defaultHome == null || mMyHomeComponent.equals(defaultHome)`), so the Recents key creates the full stock `Launcher` activity again until the owner picks our launcher. Fix: none cheap; add the default home component to the tablet report line next to the SR2 status so a fat stock process can be explained. Risk: none.

**D10 (INFO) The size cannot be read app-only; the trigger remains an estimate.**
File: StockRefresh.java:122-130 accepts only `viaHelper` readings. Fix: none; give the owner the manual number path (Settings > Developer options > Memory > Memory used by apps > "Quickstep", 3-h and 1-day average) and add the missing cable probe lines (section 7). Risk: none.

**D11 (LOW, nit) Two sleeping threads per ended session.**
File: StockForceStop.java:236-247, StockRefresh.java:100-114. Each `afterQuickstepSession` starts a daemon thread that sleeps 45 s (StockRefresh then returns at once when no helper runs). Fix: one `Handler.postDelayed` on an existing looper. Risk: none; tiny gain.

---------------------------------------------------------------------------------------------------------

## 6. Recommended plan, in order

0. **No code, [T]:** ask the owner for (a) Settings > Developer options > Memory > Memory used by apps, "Quickstep" 3-h and 1-day average after about a day of v0.9.17 (the old-world values were 414-450 MB); (b) the tablet report lines for SR2 / SR1 and the recorder. Everything below is cheap enough to build before the answer, but only (a) can say whether D8 / L4 / a lower SR1 line are worth anything.
1. **App-only, one small change set (pure rules + JVM tests, then sections `sf`, `sr` and the lock core the diff pulls in):** D3 (re-aimed debt + "stock window seen" flag + config signature), D5 (one seeded drive), D4 (cold-start tolerant clock + a "cold start +N ms" number in the log), D6, D7. Run the review with a neutral brief on the delta only.
2. **Keep SR1 as it is**; revisit its 200 MB / 6 Activities line after the tablet number (L4 `send-trim-memory` is the only unproven helper add-on; skip it until a number says what it frees).
3. **Defer D8** (alias) and D11 until a number shows they matter.
4. **Write D2 into HANDOFF.md / BRIEF.md** so no later session tries `pm disable-user`.
5. If the owner ever runs the helper: the invisible task removal makes the stock process truly idle (no Recents open at returns). Without it the on-screen clear-all (W1) stays the one daily wake.

**App-only:** L1, D3-D8, the manual memory read. **Helper-only:** exact size (SR1), `STOCKRESTART`, `send-trim-memory`, task removal by id, any `pm` / `cmd appops` / bucket command.

---------------------------------------------------------------------------------------------------------

## 7. Not checked, and cable lines worth adding

Could not check from the repo (all [T] unless noted): the MediaTek manifest of `MtkLauncher3GoIconRecents` (receivers, services, jobs, `persistent`, `android:process`), real SystemUI re-bind timing and Recents-dead window after a kill on the tablet, cold vs warm Recents timing on the MT8168, whether Tj's old stock home holds widgets, the MediaTek Settings labels for the Force stop drive (CT1f), whether `Unified` really leaves the stock HOME activity uncreated over a day on the tablet (CV1h waits on the owner), the real size of one pinned Go activity, and the claim that `LauncherAppState` is not created by the service path alone (read from memory of AOSP Q [I]; the Go classes were read in CG1F, `LauncherAppState` callers were not enumerated). Emulator proof for the SystemUI re-bind is a pid pair in two commit messages; the check text (`[ -n "$SF_PB" ] && [ "$SF_PB" != "$SF_PA" ]`, tools/emulator_scenario.sh:5596) would also pass with an empty "after" pid, so adding an explicit "after pid not empty" check to SF2 and an INFO line with the seconds until a new pid exists would make that evidence permanent.

Probe lines missing from `app/src/main/resources/probe-commands.txt` (existing ones already cover `dumpsys meminfo`, procstats, `ps`, `dumpsys activity lru / processes`, dexopt state of the stock launcher): `dumpsys package com.android.launcher3 | grep -iE "persistent|process=|Receiver|Service|Provider" | head -40` (MTK components), `dumpsys appwidget | grep -B2 -A6 launcher3` (widgets still bound to the stock host), `dumpsys jobscheduler | grep -c launcher3`, `dumpsys alarm | grep -c launcher3`, `dumpsys usagestats | grep -i "launcher3" | head -4` (bucket), `dumpsys activity services com.android.launcher3`, `dumpsys activity processes | grep -A14 "ProcessRecord.*launcher3"` (adj, state, `trimMemoryLevel`, `lastPss`).

---------------------------------------------------------------------------------------------------------

## 8. Sources read

research/CG1F_QUICKSTEP_AOSP_ANGLE_2026-11-17.md (all), CD2_QUICKSTEP_RAM_AUDIT_2026-11-14.md, CD2_AOSP_Q_LAUNCHER3_FACTS_2026-11-14.md, CT1_FORCE_STOP_WITHOUT_HELPER_2026-11-21.md, CI1_AUDIT_2026-11-18.md, CV1_UNIFIED_LAUNCHER_DESIGN_2026-11-22.md, CV1_AUDIT_B_2026-11-22.md; TASKS.md (CD1, CT1, CU1, CV1, CW1), OUTBOX.md:1409, git log messages of commits 25c715ed and 38aaaa6a; research/tablet/2026-11-10-2/tablet-report.txt; code: Unified, UnifiedRules, Mode, RouterActivity, StockLauncher, StockForceStop, StockRefresh, QsMemory, KeepAliveService / KeepAlivePolicy, SettingsSpoof, ResetPolicy, RecentsFilter, ShellProtocol / ShellServer, BatterySaverGuard (observer), ReturnReset, WallpaperPickerChoice, TjHooks (resume hooks), TjAccessibilityService (guards, stock lookups, Recents clear-all, SR2 drive 5305-5500), AndroidManifest permissions, tools/emulator_scenario.sh `sr` / `sf` sections, tools/gate-map.txt, `tj_accessibility.xml`.
