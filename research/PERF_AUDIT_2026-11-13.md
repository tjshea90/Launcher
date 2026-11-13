# Performance audit 2026-11-13 (TASKS CA1): second pass after BY1 (v0.9.8)

Tj, 2026-11-13 01:37 UTC: "Review the launcher for code efficiency, ram optimization without sacrificing speed or function, speed of the launcher
and loading from the lock screen, battery and cpu efficiency. Polish it and make sure it is well built and optimized for my hardware."

**This builds on `PERF_REVIEW_2026-11-12.md` (BY1, shipped in v0.9.8).** That review fixed B1-B4, C1, C2, A2-A4, C6/C7, D1 and left a NEXT list
(C3, C4, A1/C5, D2/D3, B5/B7/B8, A5, A6/C12, C11, B9-B15, D4-D8). This pass: three read-only reviewers (neutral briefs; unlock path, steady CPU /
battery, RAM) on v0.9.8 + a build-config look by hand. IDs here: U = unlock path, S = steady state, M = memory, K = build.

Hardware: MediaTek MT8168B (4x Cortex-A53), 32-bit userland, ~1.3 GB free of 2 GB, Android 10 Go, 600x1024 @ 170 dpi.
Tablet facts (report 2026-11-10-2): PSS 69 MB, Java heap 5 of 128 MB, Application.onCreate 48 ms, 2 process starts a day.

## K: build configuration (by hand)
- K1 R8 stays OFF (AD8 decision, `EFFICIENCY_2026-11-05.md`): reflection on hidden APIs, `app_process ... com.tj.launcher.ShellServer` by class
  name, class names in XML; the emulator gate runs the DEBUG build, so a minifier-only break would show only on the tablet. Unloaded classes cost
  no RAM; the gain would be APK size and install time. Verdict NO (unchanged).
- K2 The app has 0 Kotlin files, but kotlin-stdlib 2.3.10 also comes in transitively (androidx core / annotation-experimental / recyclerview), so
  dropping the Kotlin plugin from :app saves nothing without R8. Verdict NO.
- K3 `cmd package compile -m speed` is a manual Setup item per version (`SetupActivity` COMPILE_SELF, "Redo after every update"). Android's idle
  bg-dexopt compiles speed-profile from the JIT profile anyway when the tablet charges idle. Verdict: say it to Tj (one adb line after an update).

## M: memory (reviewer M)
| id | what | size | verdict |
| :- | :- | :- | :- |
| M1 (=D2) | home curtain cached in custom mode where nothing can show it (`HomeCurtain.java:184`, filled at connect `TjAccessibilityService:322`, 2081, 2186, 2200, 2248) | 2.4 MB (+ maybe a GPU texture) | candidate: keep only while `switchOn && (!custom || bootCleanPending)`, evaluated in `prefWatch` + connect; JVM test |
| M2 NEW | lock-model wallpaper stays pinned on the custom home after an unlock: `refreshLayers()` drops `lockBmp` / `dropWallpaper()` only when `want == null`; home patch target keeps them (1258-1259, 1405, 1440-1456); `onTrimMemory` frees nothing while `patchView != null` (3538) | 4.9 MB (1200x1024), up to 12 MB for a separate lock photo; only when no lock crop is stored (after a wallpaper change / new rotation) | candidate: after `patchTarget()`, if the target is not the lock flavour: `lockBmp = null`, `dropWallpaper()` unless power mode "wallpaper"; onTrimMemory `patchView == null || patchHomeMode` |
| M3 (=D3) | each curtain capture copies the whole Launcher window (`HomeLayer.java:33`, 2160-2171) | 2.4 MB native + GPU readback, <= 1 per 15 s | NEXT (pixel-stability risk; section `cu`) |
| M4 NEW | Launcher3 icon cache keeps icons of HIDDEN apps (`BaseIconCache.addIconToDBAndMemCache` 249-273 always puts; `IconCache.updateIconsForPkg` 103-115) | 16 KB each, 0.1-0.6 MB est. | candidate: put into the mem cache only if already there (DB row still written) |
| M5 (=D4) | patch masks ARGB_8888 rebuilt per window (1469, 1586-1608) | 137 KB kept + 137 KB garbage per window | NEXT (ALPHA_8 pixel-equality on MediaTek unproven) |
| M6 NEW LOW | `HomeLayer.compose` (64-72) one extra copy per build | ~128 KB garbage | NO (rounding risk) |
| M7 (=D6) | 4 idle threads for the process life (`LockLog:51`, `NotifSightings:113` executors, `SettingsSpoof:83` TjSsSync, `AnimationSpeedup:157`) | 20-60 KB each est. | candidate: core-thread timeout like `RecentsFilter:184-191`; keep tj-anim |
| M8 (=D5) | Hidden apps screen loads every icon at once, no view reuse (`HiddenAppsActivity:53-60, 90`) | N x 10-110 KB while open | candidate (simple, high confidence) |
| M9 (=D7) | `new SimpleDateFormat` per log line (`LockLog:27`, `TjAccessibilityService.now()` 1767, 19 sites) | garbage only | candidate |
| M10 (=D8) | `HiddenApps.allowed` copies the set per question (`HiddenApps:32-40`) | garbage only | candidate |

Checked and fine (M): overlays small (<1 MB total), full-screen windows removed after use, Ss* statics tiny and stopped in onStop, Launcher3 Go
widgets off / shortcuts stubbed / AppFilter before icons, all collections bounded, statics hold only the app context or WeakReference, no per-frame
allocations outside a 300 ms fade, the wallpaper-change receiver drops every cached picture.

## U: unlock path (reviewer U; TAS = TjAccessibilityService.java, TR = research/tablet/2026-11-10-2/tablet-report.txt, pre-v0.9.8)
Measured on the tablet: Enter-tap lag (event handled vs tap) 224 ms plain custom unlock (TR:758), 359 ms handshake (TR:776), 2164 ms
Quickstep->custom (TR:729); plain custom unlock otherwise 249 ms (TR:761); Quickstep->custom: 350 ms to the reset plan, Clear all 838 ms, settle 291 ms.
| id | what | verdict |
| :- | :- | :- |
| U1 NEW HIGH | per PIN key: SystemUI click -> power-menu scan (`EventNoise:35-36`, TAS:104/112), pencil search 40 ms after a click (`EventNoise:58`, TAS:2403), `updateLockNumber` 2 binder calls + pad scan (TAS:2914-2933), `trackUnlockKey` fresh `keyguardLocked()` per click (TAS:1678); all before `trackUnlockKey` (TAS:94-96) on the main thread, and each search runs on SystemUI's UI thread too | candidate: while the PIN pad is up and no menu / cover is up, clicks + content changes start no menu scan / pencil search (a menu over the pad still comes by window event; `menuKnownUp` keeps clicks inside an open menu); `keyguardLockedFast()`; measure Enter with `e.getEventTime()` |
| U2 NEW HIGH (Q->custom) | `recentsEmpty` polls every 40 ms from the instant of the Recents action, every window root + 2 text searches + an 8-level card walk (TAS:3939-3980, 3884-3895, 3872-3878, 3752, 3806) | candidate: first look at the stock launcher's window-state event (300 ms fallback), card walk only in the launcher's window + tall system windows |
| U3 NEW | `AppOrigin.pending` -> `mergeUsage` = `UsageStatsManager.queryEvents` on the main thread at the Enter tap (TAS:1914 -> `ReturnReset:425-427`) and in `ReturnReset.run` (208-209) | candidate: merge on a worker; clean start decides from the cheap check + `usedSinceClearCached` |
| U4 NEW | `killApps` (`ReturnReset:193-196, 264-283`): queryIntentActivities + one `killBackgroundProcesses` per app, at the same instant as Recents / Launcher resume | candidate: run after the cover is gone, background thread |
| U5 NEW | USER_PRESENT posts `layerRefresh` + `lockEval` (TAS:2863-2864) before `unlockApply` (2886) | candidate: `postAtFrontOfQueue(unlockApply)` when mode is Quickstep or a cover is up |
| U6 (=C6) | `Mode.set` runs Keyboard / Tile / Immersive / BatterySaver profile switches on the main thread (`Mode:63-82`) | candidate: one serial background executor, same order, after the commit + animation sync |
| U7 (=C7 rest) | commits on the cover-removal path (TAS:4078-4082, `RecentsDirty:19`, `ReturnReset:54`, TAS:2311/4094-4097, `releaseReset` TAS:2310 + `AnimationSpeedup:74`) | candidate: done-runnable first, report async, `apply()` |
| U8 (=A1/C5) | full Launcher3 reload 3 s after every return to custom (`SettingsSpoof:108-114` via `Mode:73`) | NEXT (0 or 2 Settings entries risk, section `ss`) |
| U9 NEW LOW | `settleHome` polls every 40 ms (TAS:2302-2322, 4086-4092) | candidate: also hide on our package's window-state event |
| U10 NEW LOW | `syncNow` twice per unlock (`Mode:72`, TAS:1926), 3 uncached `checkSelfPermission` per sync; `TileProfile.apply` 3x per mode-changing unlock | candidate: grant read once per sync |
| U11 (=C3/C4) | helper ping thread joined up to 700 ms on the main thread at the Enter tap and in every `lockPrepare` (TAS:1837, 1845, 1914) although the helper is not running on the tablet | candidate: cached helper state from a worker |
| U12 NEW | the dark prearm reached the router only 507 ms after SCREEN_ON (TR:719, 728): the custom stack may be (re)created during the wake / PIN entry | measure first: lock-log line at `Launcher.onCreate` |

Checked and fine (U): LockLog writes on its own thread; plain custom unlock prepared behind the keyguard, light after USER_PRESENT; resume hooks once per
instance; curtain copy async; wake run ~18 ms; cold start rare and 48 ms.
## S: steady CPU / battery (reviewer S)
No wake lock / alarm / job / WorkManager anywhere; KeepAliveService runs nothing; ~6 events/s, 85 % dropped as noise (~10 us each).
| id | what | verdict |
| :- | :- | :- |
| S1 (=U1) | every PIN tap: power-menu scan now + 80 ms later, full pencil search + penEval retry, PIN-pad check, `refreshLayers` (~9 binder calls); est. 100-150 ms per digit | FIX (U1) |
| S2 (=C5/U8) | two full Launcher3 reloads per Quickstep round trip (`SettingsSpoof:102-120` -> `HiddenApps:102-105` -> `LauncherModel:318-333`); the one at Quickstep start is invisible | candidate: when the alias goes OFF (Quickstep) skip `forceReload`, set a pending flag; the next custom sync does the one reload |
| S3 | screen dark but CPU awake (music, charging): each SystemUI content change runs pencil search, `refreshLayers` x2, `calTrigger` (`lockCalSigFor` 4 binder calls), `updateLockNumber` x2; `penWatch` polls 150 / 25 ms with no screen check if QS was open at screen-off (TAS:2465-2472, 2615) | candidate: `screenDark` memo set by the screen broadcasts; early return in schedulePencil / penWatch / scheduleLayerRefresh / scheduleLockUpdate; lockPrepare's dark preparation stays |
| S4 (=B3 rest) | lit idle lock screen: `bouncerShownScan` 6 id + 5 text searches per SystemUI event, twice (TAS:2946-2966) | NEXT (medium risk: lock number) |
| S5 | `lockPrepare` 6.5 s rerun repeats `shadeLevel` while dark (TAS:2882-2883, 1850) | candidate: `if (locked && !shadeLookedDark)` |
| S6 (=C2 rest/U10) | each sync tick ~6 binder calls, 4 of them `checkSelfPermission(WRITE_SECURE_SETTINGS)` (`AnimationSpeedup:198-233`, `NotifQuiet:33, 72-87`, `RadioIconHide:42`) | candidate: one read per `syncNow` |
| S7 (=C8 rest) | Solitaire lookup per window-state event in Quickstep (TAS:2043, 3233; `ReturnReset:319-327`); if Solitaire is uninstalled the label scan reruns every 30 s | candidate: cheap checks first |
| S8 (=C3) | shell-helper pings never back off (`ShellHelper:64-70, 109-114`; TAS:1327 no cache, joins up to 700 ms) | candidate: shared back-off 4 s -> 60 s |
| S9 (=B5) | ~9 binder calls per `refreshLayers` (rotation, getRealMetrics, wallpaper ids) | NEXT |
| S10 | narrowing event types at runtime: gains ~nothing on Android 10 (cache consistency, apps send anyway) | NO |
| S11 (=D3/M3) | curtain full-window copy after every home visit (<= 1 / 15 s) | NEXT |
| S12 (=C7 rest/U7) | `patchStatus` / `calStatus` / `shotStatus` whole-file commits on the main thread (TAS:656-658, 1255, 1559, 1311, 4654) | candidate: `setStringAsyncIfChanged` |
| S-BA2 | battery saver is forced OFF while locked (`BatterySaverGuard:199-223`), losing its standby savings, and flips the dark theme twice per lock after a Quickstep session with saver on | a decision for Tj, not a fix |
Checked and fine (S): noise events cheap; heartbeat window events only; receivers rare; notification work on a worker; LockLog async; retry loops
bounded; key filter only when used; motion sensor off by default (if ever on: 50 Hz on the main thread for the whole Quickstep session); Launcher3
idle draws nothing.

## Design notes for the fixes (Claude, while waiting for reviewer S)
- U1 design: (a) `EventNoise.needsPowerMenuScan` gets a `padUp` input: a CLICK starts a scan only when the menu is known up, or it comes from the
  system AND the PIN pad is not up (a power menu is a NEW window: its window event always scans, plus the 300 ms full-scan window); (b)
  `schedulePencil`: a click while `lastBouncer` and no pencil cover is shown starts no FULL pencil search (content changes still may, 200 ms gap);
  (c) `trackUnlockKey`: proceed if `keyguardLockedFast()` says locked, else ask the real `keyguardLocked()` (identical decisions, no binder call per
  key during PIN entry); (d) handle `trackUnlockKey` before the schedule* calls of the same event; (e) Enter timing from `e.getEventTime()`.
  Per key today: click -> power-menu scan (evaluate) + full pencil search (40 ms gap) + updateLockNumber (60 ms gap) + 1 binder call; the content
  changes after the key already drive updateLockNumber, so the pad-hide detection keeps its triggers.
