# CG1B: process start, background cost, disk I/O, APK and ahead-of-time compile (read-only review, 2026-11-17, revision 2)

Slice: everything EXCEPT `TjAccessibilityService.java` internals and the `launcher3/` fork. Built on `PERF_AUDIT_2026-11-13.md` (CA1) and `PERF_REVIEW_2026-11-12*.md` (BY1); items already
there are cited, not repeated. Nothing was built, run or committed by this review; nothing in the repo was edited except this file.

**Revision 2 (same day, about 19:40Z).** The first version was written before any build output existed in this container. At 19:18Z the main session built a release variant locally
(`app/build`, a saved `:app:dependencies` output, a saved Gradle log); this revision reads those outputs (read-only) and replaces four inferences of version 1 by measurements: the dex size,
the presence of `androidx.startup` / `androidx.profileinstaller`, the profile format, and whether wildcard profile rules expand with R8 off. Section 10 reviews the CG1c code the main session had
put in the working tree meanwhile (`DexoptState`, `DexoptWatch`, `baseline-prof.txt`).

Evidence legend: **VERIFIED** = read in code or in a file in this container. **VERIFIED(build)** = read or decoded from the local release build output of 19:18Z (`app/build/...`, file sizes, a
read-only parse of the dex headers and of `baseline.prof`; the build was of the working tree at that moment, so numbers are the tree's, not necessarily the shipped v0.9.11 APK's).
**INFERRED** = reasoned from Android 10 behaviour or from numbers in the tablet report; no tablet counter exists for it. Binder cost is assumed 0.15-0.4 ms per call and a thread hop 0.05-0.1 ms
on the 4x Cortex-A53 (INFERRED, typical; not measured on this tablet).

Citations: `TR:n` = line n of the raw tablet report `research/tablet/2026-11-10-2/tablet-report.txt` (v0.9.1, 121 min of accessibility load); `LATEST.md:n` = line n of `research/tablet/LATEST.md`.
Not possible here: the shipped APK was not downloaded (the project rule forbids working around the GitHub-host curl hook; its size was read through the GitHub MCP tool); no keystore, so no signed
release package was built locally; the tablet report predates the VPN work (CB2 / CE1) and has no dexopt line.

## 0. Headline

1. **This process has exactly ONE periodic timer outside the accessibility service: the VPN look every 30 s** (`VpnOffRules.java:38`, `VpnOff.java:204-211`), armed in every quiet sync
   (`VpnOff.java:139`) and therefore running all day in the custom launcher whenever Android has allowed the VPN app-op. Cost about 2-3 ms CPU per tick, 2 ticks a minute: marginal.
   Everything else (holds, receivers, observers, helper pings, sweeps, resets) is event-driven. There is no WakeLock, AlarmManager, JobScheduler or WorkManager anywhere in
   `app/`, `launcher3/` or `wallpaperpicker/` (grep, 0 hits: VERIFIED, re-validates CA1 S).
2. With the screen dark and the CPU suspended nothing of this slice runs (Handler timers use uptime and stop in suspend: AOSP behaviour, INFERRED). With the CPU awake and the screen
   dark (charging with a wake-lock holder, music) the only repeating item is the same 30 s VPN look.
3. **Process start** (about 2 a day; 48 ms `Application.onCreate`, 124 ms from fork to its end, `LATEST.md:33`): the only structurally avoidable cost is three sequential blocking
   SharedPreferences loads (`tj_crash`, `tj_life`, `tj_features`) plus 5 permanent threads; saving is a few ms, twice a day: **no** (CA1 A5 stands). One fact is new: the merged manifest has a
   SECOND enabled provider, `androidx.startup.InitializationProvider` (section 2.3), installed before `Application.onCreate` (INFERRED 2-5 ms, twice a day: no action).
4. **Disk I/O is already mostly off the main thread.** The remaining fsyncs are the whole-file `commit()` writes (13 app preference files; `tj_features.xml` is estimated at 4-8 KB).
   There is no measurement of how long a commit takes on this tablet. One cheap counter would turn every "estimated 2-15 ms" in CA1 C7/U7 into a number (W4).
5. **The APK is 9,624,960 bytes** (VERIFIED, Release v0.9.11; Eleven is a separate 6,820,150 B APK, PhoneGuide 23,871 B). It is ONE `classes.dex` of **8,048,560 B** (VERIFIED(build): 6,307
   classes, 54,734 method ids, 43,768 methods, 2.89 MB of code) stored uncompressed, plus about 1.6 MB of resources. **About 70 % of the code bytes are libraries** (2.02 of 2.89 MB; the libs-only
   intermediate dex is 5.93 MB); `kotlin-stdlib` alone is 24 % of the code although the repo has 0 Kotlin sources. No native libs; no assets of ours.
6. **AOT: whether the app is compiled on the tablet is UNKNOWN** (`LATEST.md:35` "Compiled ahead of time? (not in the report)"). Version 1 said a baseline profile needed a new
   `androidx.profileinstaller` dependency: **wrong**. `profileinstaller 1.3.0` and `startup-runtime 1.1.1` are ALREADY in the APK (core -> lifecycle-runtime -> profileinstaller), so the APK
   very probably already installed a library-only profile (672 androidx rules in today's merge; INFERRED for the shipped APKs) and will install the app's own once `baseline-prof.txt` is built in. Wildcard rules DO expand with R8 off (section 6.3).

## 1. Idle cost table

Rows are mechanisms outside `TjAccessibilityService`; the service's own loops are listed last for completeness (reviewer B owns them). "per min" = callbacks this process runs on its own
with nobody touching the tablet. Per-tick cost is INFERRED (calls x 0.15-0.4 ms + hops x 0.05-0.1 ms).

| mechanism (file:line) | thread | custom launcher, screen ON | Quickstep unlocked, screen ON | screen OFF, CPU suspended | screen OFF, CPU awake | per tick |
| :- | :- | :- | :- | :- | :- | :- |
| VPN look poll: `VpnOff.java:204-211` (30 s, `VpnOffRules.java:38`), armed `:139` only if app-op `activate_vpn` is allowed (`:73-78`) | tj-vpn-off, tj-anim | **2 /min** (quiet world = custom mode, `fillWorld` :100-113) | 0 (`endQuiet` :145-154 removes the callback) | 0 | **2 /min** | 3-4 thread hops, 4-6 binder (grant memo expired -> checkSelfPermission; app-op; getAllNetworks; getNetworkCapabilities per network), 1.5-3 ms |
| Policy heartbeat `AnimationSpeedup.tick` `:182-189`, called per accessibility event, 500 ms gate | main -> tj-anim | 0 (returns after 2-3 prefs reads, `:187`) | up to 2 /s on window changes only (CA1 C2 fix), typically 10-30 /min while apps are used, 0 when still | 0 | 0 | about 3 binder (isInteractive, isKeyguardLocked via the shared `Look` :274-281, grant memo 3 s `:55`), 1-2 ms |
| Content observers `BatterySaverGuard.java:52,56`, `EntranceWatcher.java:53`; dynamic receiver `EntranceWatcher.java:59` | main | 0 (fire on change only) | 0 | 0 | 0 | none at idle |
| `KeepAliveService` (`KeepAlivePolicy.java:142` default OFF; off on the tablet, TR:622) | main | 0 | 0 | 0 | 0 | does no work even when on (`KeepAliveService.java:101-119`) |
| One-shots per PROCESS START (2 a day): `SettingsSpoof` +3 s (`SettingsSpoof.java:34,94`); `DexoptWatch` +20 s (`DexoptWatch.java:28`, only if the stored reading is stale); `ProfileInstallerInitializer` first frame +5 s (library, INFERRED) | TjSsSync, TjDexopt, a library executor | once per start | once per start | n/a | n/a | 1 PackageManager binder; 1 fork+exec of `dumpsys`; a file check (and a profile write once per APK version) |
| `AnimationSpeedup.onUnlockDecided` follow-ups `:165-166` (+400 ms, +1.5 s) | tj-anim | per unlock x2 | per unlock x2 | n/a | n/a | one-shot |
| Shell-helper ping `ShellHelper.java:64-70` (thread + join <= 700 ms) | new thread, main waits | 0 at idle; per `lockPrepare` x3 per lock + Enter tap (CA1 U11/S8) | same | n/a | n/a | refused connects, 1 audit line each |
| Helper process, only if Tj starts it: BEAT every 30 s `HelperLife.java:173`; logcat stream `ShellServer.java:354-364,408-422` | separate JVM | 2 /min + a continuous log parse | same | n/a | same | not on the tablet (HLP1) |
| Service loops (reviewer B): `penWatch` 150/25 ms while quick settings are open (paused when dark since CA1), `recentsEmpty` 40 ms, `settleHome` 40 ms, `scrimTick` | main | 0 at steady idle | 0 at steady idle | 0 | 0 | event-bounded |
| Accessibility events themselves (service slice): 45,521 in 121 min = **376 /min**, 85 % noise (TR:629) | main | same | same | none | none | about 10 us each when dropped |

Reading the table: a quiet tablet in the custom launcher wakes this process about **2 times a minute from its own timer**; everything else is a reaction to events. The event stream (376 /min)
is by far the larger background cost and belongs to reviewer B.

## 2. Process start

### 2.1 What runs before the first frame (VERIFIED, order of execution)
1. Zygote fork, `LoadedApk`, dex open (not measurable here; the 124 - 48 = 76 ms before `onCreate`).
2. Content providers are installed BEFORE `Application.onCreate`, on the main thread: `LauncherProvider` (`AndroidManifest.xml:158-163`) -> `LauncherProvider.onCreate`
   (`launcher3/src/com/android/launcher3/LauncherProvider.java:123-133`) -> `MainProcessInitializer.init` (`FileLog.setDir`, `FeatureFlags.initialize`, `SessionCommitReceiver.applyDefaultUserPrefs`,
   `IconShape.init`; fork area, reviewer A), and **`androidx.startup.InitializationProvider`** (VERIFIED(build), 2.3): `AppInitializer.discoverAndInitialize` reads the provider's meta-data through
   PackageManager (one binder call), instantiates `ProfileInstallerInitializer` by reflection and posts a Choreographer frame callback (INFERRED from the library; est. 2-5 ms, part of the 76 ms).
   The other two providers are `android:enabled="false"` (`:169-173`, `:190-196`): free.
3. `TjApplication.onCreate` (`TjApplication.java:16-35`; lines 20-30 are the `try` blocks and all but line 21 are guarded by `if (main)`, so the `:wallpaper_picker` / `:wallpaper_chooser` processes stop after
   `CrashGuard.clearIfNewer`): `CrashGuard.install` (:20, a handler, no I/O) -> `CrashGuard.clearIfNewer` (:21, **blocks on loading `tj_crash`**, `CrashGuard.java:112`) -> `ProcessLife.noteStart`
   (:22, **blocks on `tj_life`**, then `apply()`, `ProcessLife.java:111-116`) -> `AnimationSpeedup.reconcileAsync` (:23, starts thread `tj-anim-start`, which creates the `tj-anim` HandlerThread,
   `AnimationSpeedup.java:197-208`) -> `SettingsSpoof.syncSoon` (:24, creates HandlerThread `TjSsSync`, 3 s delayed) -> `SwitchMigration.run` (:25, **blocks on `tj_features`**,
   `SwitchMigration.java:28`, then returns because the done-marker is set) -> `EntranceWatcher.start` (:26, one Settings.System read, one observer, one receiver) ->
   `LockQuiet.applyNotificationsAsync` (:27, a thread, `LockQuiet.java:101`) -> `BatterySaverGuard.start` (:28, two observers, one Settings read, `unstickOnce`, `check`) ->
   **`DexoptWatch.refreshSoon` (:29, new in the tree: one 20 s delayed post)** -> `LockQuiet.applyLockRulesEarly` (:30, a thread, `LockQuiet.java:136`).
4. The three blocking loads run one after another because each file is first touched at its first read (INFERRED from `SharedPreferencesImpl`: one loader thread per file, `awaitLoadedLocked` on first read).
   Overlapping them (CA1 A5 idea a) would save roughly the two shorter loads: INFERRED 3-8 ms of the 48 ms, twice a day. **Verdict: no.** Nothing else in `onCreate` is worth moving: the observers are single binder calls.

### 2.2 SharedPreferences inventory (VERIFIED by grep of every `getSharedPreferences` / `PREFS =`)
13 app files: `tj_features` (everything: switches + status strings), `tj_mode`, `tj_crash`, `tj_life`, `tj_anim`, `tj_notif_quiet`, `tj_notif_seen`, `tj_radio_icons`, `tj_restore`, `tj_origin`, `tj_task_origin`,
`tj_dirty`, `tj_apps` (HiddenApps allow list), plus `tj_kb` (KeyboardSwitch) and Launcher3's own. Each file is a whole-XML rewrite on every `commit()` / `apply()` batch (AOSP), so file size sets the cost.

| file | size (est.) | loaded | writes |
| :- | :- | :- | :- |
| `tj_features` | **4-8 KB** (the 2026-11-10 report lists 35 booleans + 35 allow-listed strings = 3.4 KB at v0.9.1, plus un-reported strings such as the scrim fits and card geometry; CG1c adds 4 keys) | `Application.onCreate` (blocking) | `Features.set/setString/setInt/remove/allOff` all `commit()` (`Features.java:28-45`); async variants `:32,36,40`; 47 write sites in the service, 66 elsewhere |
| `tj_mode` | < 1 KB | first `Mode.get` | `Mode.set` `commit()` at the unlock (`Mode.java:75`, needed: the router reads it after a kill); Home-press times are memory only since BY1 A3 (`:25-28`) |
| `tj_crash`, `tj_life`, `tj_dirty`, `tj_origin`, `tj_notif_*`, `tj_radio_icons`, `tj_anim` | < 1 KB each | lazily | rare; `RecentsDirty.mark` commits only on clean -> dirty (`RecentsDirty.java:18`), `AnimationSpeedup.PrefsState.save` commits only when a hold changes (`:89-94`) |

`commit()` / `apply()` counts outside the service and UI classes (VERIFIED, grep): CrashGuard 6/0, RestoreStore 5/0 (restore-all only), Features 6/3, Mode 3/0, HiddenApps 3/1, RecentsDirty 2/0, TaskOrigin 2/0,
AnimationSpeedup 1/1, KeyboardSwitch 1/0, AppOrigin 0/4, NotifSightings 0/2, ProcessLife 0/2, NotifQuiet 0/1, RadioIconHide 0/1.

### 2.3 Manifest components (VERIFIED from `AndroidManifest.xml`; library additions VERIFIED(build) from the merged release manifest `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`, lines 394-421)
| component | line | cost at idle | verdict |
| :- | :- | :- | :- |
| `InstallShortcutReceiver`, `SessionCommitReceiver`, `AppWidgetsRestoredReceiver` | 84-106 | none; start the process only when their broadcast arrives (a session commit, a legacy shortcut) | fine |
| `LauncherProvider` (exported, permission-guarded) | 158-163 | runs at every process start (2.1 step 2) | fork area |
| `GridOptionsProvider`, `TestInformationProvider` | 169-173, 190-196 | disabled | fine |
| `WallpaperPickerActivity` `:wallpaper_picker`, `ColorExtractionService` `:wallpaper_chooser` | 110-123 | separate processes, only on demand; the service is the pre-API-26 path and is never bound on API 29 | fine (dead code, no cost) |
| `NotificationListener` (`android:enabled="@bool/notification_dots_enabled"`) | 125-133 | the system binds it only with notification access, which a Go build does not grant | fine (CA1/BY1 A8: NO) |
| `ReturnTile` | 287-296 | bound by SystemUI only when its tile is shown | fine |
| `KeepAliveService` | 342-345 | off | fine |
| `TjAccessibilityService` | 348-359 | the event stream (section 1) | reviewer B |
| **`androidx.startup.InitializationProvider`** (authority `com.tj.launcher.androidx-startup`, `exported=false`, one meta-data: `androidx.profileinstaller.ProfileInstallerInitializer`) | merged manifest 394-401 | one provider install per process start; the initializer posts work 5 s after the first frame (INFERRED) | keep: it is what installs the baseline profile on API 29 (6.2) |
| **`androidx.profileinstaller.ProfileInstallReceiver`** (`exported=true`, `permission=android.permission.DUMP`; actions INSTALL_PROFILE, SKIP_FILE, SAVE_PROFILE, BENCHMARK_OPERATION) | merged manifest 403-421 | none; only a holder of DUMP (adb shell, or this app itself) can send to it | fine; usable from adb to install the profile now |
| `android:largeHeap="@bool/config_largeHeap"` | 80 | `launcher3/res/values/config.xml:3` is `false`; `values-sw720dp` is `true`, and 600x1024 at 170 dpi is 564 dp wide: **false on this tablet** | fine |

### 2.4 Threads that live for the whole process (VERIFIED)
`tj-anim` (`AnimationSpeedup.java:176`), `TjSsSync` (`SettingsSpoof.java:83`), `tj-vpn-off` (`VpnOff.java:65`, created by the first quiet sync with the release default ON), `tj-lock-log` (`LockLog.java:51-53`),
`tj-notif-seen` (`NotifSightings.java:113-115`). That is **five** (CA1 M7 counted four: `tj-vpn-off` arrived with CE1). INFERRED RSS 40-100 KB each, 0.2-0.5 MB total. `RecentsFilter.java:185-191` already
shows the better pattern (core thread that times out after 10 s). Short-lived `new Thread(...)` is used at about 40 sites (every lock, unlock and reset task starts its own). The new dexopt look adds two short-lived
threads (`TjDexopt`, `TjDexoptWatch`, at most 10 s, section 10).

## 3. Background work: what each timer-like construct does (VERIFIED by reading; grep list of 122 sites reduced to these)
- **Polls / self-rearming:** `VpnOff.poll` (30 s, section 1); `RadioIconHide.offThenSync` polls radios every 150 ms for at most 4 s, deadline checked first (`RadioIconHide.java:153-170`); `LockQuiet` retry sleeps
  3 s / 9 s inside its own thread (`LockQuiet.java:136-147`); `BatterySaverGuard` sleeps 300-400 ms x5 inside worker threads (`:259,287`); `PickerGuard` failsafe re-arm only while the wallpaper picker overlay is shown
  (`PickerGuard.java:102-114`). All bounded and event-started.
- **One-shot delays:** `AnimationSpeedup` +10 s reset failsafe (`:119`), `RecentsSweep` 250 ms drain (`:148,170`), `RecentsGuard` 300 ms (`:89`), `TjHooks` (`:96`), `BatterySaverGuard` heal delay (`:326`), `DexoptWatch` +20 s.
- **Observers / receivers (registered once, no polling):** `BatterySaverGuard.java:52,56`, `EntranceWatcher.java:53,59`, `VpnOff.registerWatch` network callback (`VpnOff.java:275-289`; a failed registration retries after 60 s,
  not per sync), plus the service's own (reviewer B). `BatterySaverGuard.batteryLevel` / `powerSource` use a sticky `registerReceiver(null, ...)` read, one binder call, only on lock / unlock events.
- **Shell helper (not running on the tablet, HLP1):** `ShellHelper.reachable` costs a thread and a join of up to 700 ms on the caller; with no helper each ping is a failed abstract-socket connect plus a refused
  TCP connect (`ShellHelper.java:27-58`). Back-off is CA1 S8/U11 (NEXT).
- **Per-sync cost of the central pipeline** (`AnimationSpeedup.syncNow` `:217-254`, serialised on the class monitor): `NotifQuiet.sync`, `RadioIconHide.sync`, `VpnOff.sync`, then the animation rule. In the custom launcher
  `fillFacts` makes no binder call (`NotifQuiet.java:73-82`); the binder calls per sync are the grant memo (3 s), and in `VpnOff` one app-op check posted on EVERY quiet sync (`VpnOff.java:139`) plus the network look
  when the poll asked for it. In an unlocked Quickstep session add `isInteractive` and `isKeyguardLocked` (shared `Look`).

## 4. File and disk I/O

| item | what happens | evidence | cost | verdict |
| :- | :- | :- | :- | :- |
| `LockLog` | one log thread; per line: `getFileStreamPath`, open (append), write, close, plus a `f.length()` stat on every line because `++sinceTrimCheck >= 40` is false first (`LockLog.java:67-77`); no fsync; when the file passes 48 KB it is read whole and rewritten at 32 KB (`:79-84`), about every 150 lines | VERIFIED | about 50-100 us per line, off every UI thread | fine; no change |
| `NotifSightings` | worker thread, `apply()` of a 40-line ring | VERIFIED | negligible | fine |
| `Features` commits | whole-XML rewrite + fsync on the CALLING thread; sites on the unlock path are CA1 U7 / S12 | VERIFIED code, INFERRED cost (fsync 2-15 ms on eMMC) | unmeasured | **W4: measure first** |
| `AnimationSpeedup.PrefsState.save` | `commit()` inside `setHold`, which `Mode.set` runs synchronously on the main thread at a mode change (`Mode.java:76`, `AnimationSpeedup.java:69-77,89-94`) | VERIFIED | one fsync per Quickstep begin / end | part of CA1 U7 |
| `HomeCurtain` PNG | `Bitmap.compress(PNG)` of a full 600x1024 picture (`HomeCurtain.java:35-46`) but only when the home layout CHANGED; an unchanged capture writes nothing and renews the stamp only when `stampDue` (`TjAccessibilityService.java:2218-2224`); captures are >= 15 s apart (`HomeCurtainRules.java:56`) | VERIFIED | rare | fine (the capture copy is CA1 M3/S11) |
| System-side writes we cause | every `Settings.Global/Secure.putString` (animation scales x3, `heads_up_notifications_enabled`, `icon_blacklist`, the two marker keys, `tj_launcher_world` about 4 per lock cycle, `low_power`) makes SettingsProvider rewrite its XML (delayed about 200 ms, fsync) in `system_server` | INFERRED (AOSP `SettingsState`) | a few file rewrites per lock / unlock cycle, outside our process | no: all are safety state, and `VpnOff.publish` already skips unchanged words (`VpnOff.java:166`) |
| ProfileInstaller write | once per APK version, background thread, tens of KB into the app's current-profile file under `/data/misc/profiles/cur/0/` | INFERRED from the library | one file | fine |
| `DexoptWatch` bookkeeping | 4 keys in `tj_features` via `apply()` after each look (`DexoptWatch.java:43-46`); the int and long variants always write | VERIFIED | one coalesced rewrite of a 4-8 KB file, about 2-3 times a day | fine (section 10 note c) |
| `ReportState` / `TabletReport` | on demand only (button / report) | VERIFIED | none at idle | fine |

## 5. APK composition

### 5.1 Known (VERIFIED)
- Release asset sizes, v0.9.11: `Launcher-v0.9.11.apk` **9,624,960 B**, `Eleven-v0.9.11.apk` 6,820,150 B, `PhoneGuide-v0.9.11.apk` 23,871 B (GitHub MCP `get_latest_release`).
- `app/build.gradle.kts`: `minSdk 29`, `targetSdk 28` (:18-19); release `isMinifyEnabled = false`, `isShrinkResources = false` (:42-43; deliberate, not re-argued); Java 17, 0 Kotlin sources (find, 0 files);
  dependencies (:113-123): `:iconloaderlib`, `dynamicanimation:1.0.0`, `recyclerview:1.3.2`, `preference:1.1.1`, `protobuf-javanano:3.0.0-alpha-7`.
- Source volume that becomes dex: `app` 128 files / 23,838 lines / 1.71 MB; `launcher3/src` 335 / 76,784 / 2.92 MB (+ `src_ui_overrides` 12, `go/src` 4); `wallpaperpicker/src` 39 / 6,310 / 0.23 MB; protos 2.
- Source resources: `launcher3/res` 1.70 MB of XML in 255 files with **82 locale folders (1.34 MB of translated strings)**, `wallpaperpicker/res` 38 KB, `app/src/main/res` 140 KB. No `assets/`, no `jniLibs` in the repo.

### 5.2 Measured composition (VERIFIED(build), local release build of the working tree, 19:18Z)
- **One dex.** `app/build/intermediates/dex/release/mergeDexRelease/classes.dex` = **8,048,560 B**: 6,307 classes, 54,734 method ids (under the 65,536 limit, so no second dex), 43,768 methods with a body,
  **2.89 MB of code** (summed `code_item` sizes). The libs-only intermediate (`external_libs_dex/.../mergeExtDexRelease/classes.dex`) is 5,928,116 B, 4,308 classes, 32,971 methods, 2.01 MB of code. That intermediate is NOT
  added to the other: version 1's "8.05 + 5.93" reading would double count.
- **Resources:** linked resources `linked-resources-binary-format-release.ap_` = 1,612,852 B zipped (1,705,271 B, 687 files): `resources.arsc` 1,034,232 B, the largest raster 41,878 B
  (`work_tab_user_education.png`), `AndroidManifest.xml` 20,860 B.
- **The APK arithmetic works only with the dex stored uncompressed:** 8,048,560 + about 1,612,852 = about 9.66 MB against the shipped 9.62 MB (a deflated dex would give about 5-6 MB). So AGP's default for `minSdk >= 28`
  (stored, page-aligned dex) is in effect (VERIFIED(build) by arithmetic; `unzip -lv` of the real APK would show it directly, W3). Consequence: the APK is large, but the dex is mapped straight from the file.
- **Who is in the code** (grouped by package from the dex class data; code = summed `code_item` bytes; groups beyond the first twelve omitted):

| group (merged dex) | classes | methods | code | share of code |
| :- | -: | -: | -: | -: |
| `kotlin/` stdlib | 1,058 | 10,485 | 684 KB | 24.3 % |
| Launcher3 fork (`com/android/launcher3/`, without `icons/`) | 1,017 | 6,482 | 460 KB | 16.3 % |
| app (`com/tj/launcher/`) | 639 | 3,321 | 336 KB | 11.9 % |
| `androidx/core` | 1,072 | 6,703 | 325 KB | 11.5 % |
| `kotlinx/coroutines` | 857 | 4,440 | 250 KB | 8.9 % |
| `androidx/appcompat` | 384 | 4,047 | 229 KB | 8.1 % |
| `androidx` other | 500 | 2,657 | 164 KB | 5.8 % |
| `androidx/recyclerview` | 208 | 2,115 | 154 KB | 5.4 % |
| `androidx/fragment` | 102 | 1,016 | 72 KB | 2.6 % |
| `androidx/preference` | 112 | 775 | 43 KB | 1.5 % |
| `androidx/lifecycle` | 132 | 537 | 30 KB | 1.1 % |
| protobuf-nano | 19 | 295 | 21 KB | 0.7 % |

  Libraries are about **70 % of the code (2.02 of 2.89 MB)**; the rest is the launcher's own (Launcher3 fork, app, icon library, wallpaper picker, generated R classes: 1,999 classes, 10,797 methods, 0.88 MB). The
  dex file is 2.8x its code because of strings, ids, class data and debug info (no R8). Unloaded classes cost no RAM (K1): this is APK size, install time and, for a full AOT compile, oat size (6.2), not memory.
- **Where the libraries come from** (VERIFIED(build), the saved `:app:dependencies --configuration releaseRuntimeClasspath`, 51 artifacts): `kotlin-stdlib 2.3.10` + `kotlin-stdlib-jdk7/jdk8 1.8.0`, `kotlinx-coroutines-core/android 1.6.4`
  (through `androidx.lifecycle 2.6.2`), `androidx.core 1.13.1` (through `:iconloaderlib`), `appcompat 1.1.0`, `fragment 1.2.4`, `activity 1.1.0`, `preference 1.1.1`, `recyclerview 1.3.2`, `savedstate 1.2.1`, `startup-runtime 1.1.1`,
  `profileinstaller 1.3.0`, `protobuf-javanano 3.0.0-alpha-7`. `lifecycle-process` and `emoji2` are NOT present.
- `androidx.preference` fan-in in our sources is small (`SettingsActivity`, `DeveloperOptionsFragment`, `NotificationDotsPreference`, `FlagTogglerPrefUi`, `PluginManagerWrapper`, grep). Dropping the preference / appcompat /
  fragment / activity subtree would remove about 0.4 MB of code (13 % of 2.89 MB) but NOT kotlin or coroutines (they come with `androidx.core` / `lifecycle`): marginal (M5), size only.

### 5.3 `androidx.startup` and `androidx.profileinstaller`: PRESENT (corrects version 1)
- Chain (saved dependency tree): `:iconloaderlib` -> `androidx.core:core:1.13.1` -> `androidx.lifecycle:lifecycle-runtime:2.6.2` -> `androidx.profileinstaller:profileinstaller:1.3.0` -> `androidx.startup:startup-runtime:1.1.1`
  (+ `tracing 1.0.0`, `concurrent-futures 1.1.0`, `listenablefuture 1.0`). The manifest merger report names it: `app/build/outputs/logs/manifest-merger-release-report.txt:698-701` ("ADDED from
  [androidx.profileinstaller:profileinstaller:1.3.0] ... MERGED from [androidx.startup:startup-runtime:1.1.1]").
- The merged release manifest therefore holds the provider and the receiver of 2.3. The initializer is the ONLY entry of the provider (no `lifecycle-process` initializer, no emoji2).
- The earlier APKs very probably carried the same machinery and a library-only profile (INFERRED: the dependency declarations are unchanged; the shipped APK, read by W3, would show it).

## 6. Ahead-of-time compile (Android 10 only)

### 6.1 What the repo does today
- `ShellProtocol.java:209-210`: allow-listed job `COMPILE_SELF` = `cmd package compile -m speed -f com.tj.launcher` (comment "U2: sideloaded apps run half-optimised until Android compiles them in idle time").
- `SetupActivity.java:88` lists it ("about a minute ... Redo after every update"); the done-check (`:152`) compares `compile_done_code` with the current `versionCode`; `:159-163` stores that after a job whose output has no
  "error" / "fail". It runs only through the adb shell helper (`ShellHelper.run`), which is **not running on the tablet** (TR:620; HLP1) and must be started by Tj with an adb line after every restart.
- Before CG1c nothing read the result back: `LATEST.md:35` "Compiled ahead of time? (not in the report)". The working tree now reads it (section 10).

### 6.2 Facts
- **No manifest or API switch exists** for the compile filter. The filter comes from the installer (`pm.dexopt.install`, OEM-set; a fresh sideload has no profile, so `speed-profile` degrades to `verify`) and from the
  nightly `bg-dexopt` job (`pm.dexopt.bg-dexopt`, default `speed-profile`; runs when the device is idle AND charging, about once a day; it merges the JIT's current profile into the reference profile and compiles
  when enough new methods appeared). An app cannot start it: `IPackageManager.performDexOptMode` is hidden and answers only shell / system / root (the brief; AOSP, not re-read). Replacing the APK on an update discards the oat.
- **Baseline-profile route, now VERIFIED(build) up to the binary profile files that AGP packages into the APK:** AGP 8.13.2 ran `mergeReleaseStartupProfile`, `prepareReleaseArtProfile` (iconloaderlib), `mergeReleaseArtProfile` and `compileReleaseArtProfile` in the 19:18Z
  build (`BUILD SUCCESSFUL`, the task list of the saved log). **No wildcard-expansion task ran** (that task serves R8); with R8 off the rules go to profgen as they are. The result is `assets/dexopt/baseline.prof` +
  `baseline.profm` material under `binary_art_profile/` and `binary_art_profile_metadata/` (APK packaging itself was not run locally: no keystore; W3 asserts it in CI).
- **The profile installer is in the APK (5.3).** On API 24-33 `ProfileInstaller.writeProfile` (called by `ProfileInstallerInitializer` about 5 s after the first frame, INFERRED from the library's documented behaviour)
  writes the profile into the app's current-profile file once per APK version. **Nothing is compiled at that moment:** the next `bg-dexopt` (idle + charging) merges and compiles it, or
  `cmd package compile -f -m speed-profile com.tj.launcher` does it at once. `ProfileInstallReceiver` (DUMP-protected, 2.3) lets adb start the write: `adb shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE
  com.tj.launcher/androidx.profileinstaller.ProfileInstallReceiver`. **No manual `writeProfile` call and no new dependency are needed** (version 1's W2 proposed both).
- **Oat size (INFERRED, typical dex2oat ratios 2-4x of the dex file; not measured):** `speed` of the whole 8.05 MB dex is about **16-32 MB** in the oat directory (plus the vdex); a profile limited to our own packages is
  about a third of the methods and a third of the code, est. **5-9 MB**. It is disk, not RAM: only touched pages become resident, as clean file-backed pages. `speed` also compiles the 70 % that are libraries nobody runs; a
  profile-guided compile also lays the hot methods out together (fewer page-cache pages touched on a 2 GB device).
- **Does the 69 MB PSS change?** Little: AOT trades JIT-cache and profiling memory (anonymous, dirty) for file-backed clean oat pages. No claim of a RAM gain is made; the gain is CPU in the interpreted first minutes after a start or an update.
- **Where AOT helps this process:** the 2 cold starts a day and the first minutes after each update (the tablet's "first fade since process start" is 19 ms to first frame against 12 ms warm, `LATEST.md:33`: a small cold penalty).
  It does not help the warm, JIT-compiled steady state. Expected benefit is moderate and concentrated on "first unlock after a restart or an update".

### 6.3 What the 19:18Z build's profile contains (decoded read-only from `binary_art_profile/release/compileReleaseArtProfile/baseline.prof`)
- Format `pro\0 010\0` = the Android 9-11 ("P") format, which API 29 reads as it is (`baseline.profm` is `prm\0 002\0`, 55 B). 1,482 B compressed, 48,291 B inflated.
- **One dex** (`classes.dex`, 54,734 method ids: the same one as in 5.2), **8,643 method entries and only 4 class entries.**
- The merged text it came from (`merged_art_profile/.../baseline-prof.txt`, 689 lines) holds 672 explicit androidx rules (recyclerview, lifecycle, startup, core ...: the libraries' own profiles) and **ONE app rule,
  `HSPLcom/android/launcher3/**;->**(**)**`**, not the five rules of the current `app/src/main/baseline-prof.txt`. So these artifacts come from an uncommitted intermediate state of that file (the committed versions are 19:15:52 five rules, 19:18:40 none, 19:18:53 five; none has one rule).
  Do not read sizes from them for the final profile; rebuild first.
- **What it proves:** a single wildcard rule produced 8,643 method entries (the fork's 6,482 methods + icon library + the library rules): **wildcards expand with R8 off.**
- **What is missing:** the rules are method rules (`HSPL...;->**(**)**`), so the profile lists almost no classes. Class lines (`Lcom/tj/launcher/**;`, `Lcom/android/launcher3/**;`, `Lcom/android/wallpaperpicker/**;`) add the class list
  that dex2oat uses for class verification and the app image of a `speed-profile` compile (INFERRED from the ART design; cheap, no downside seen).
- With the five rules the profile should name about 13k methods (ours 10.8k + recyclerview 2.1k + dynamicanimation) of 43.8k: expected file size 5-15 KB (INFERRED); check after the rebuild.

## 7. Findings

Gain figures are estimates, with the method named. "CA1?" = already in `PERF_AUDIT_2026-11-13.md` / BY1.

| id | file:line | what happens | evidence | estimated gain | risk and proof that makes it safe | CA1? | verdict |
| :- | :- | :- | :- | :- | :- | :- | :- |
| **W1** dexopt line in Launcher control and the tablet report | `DexoptState.java:36-54`, `DexoptWatch.java:26-55,80-112`, `TjApplication.java:29` (working tree, CG1c), `LATEST.md:35` | the report could not say whether the app is compiled, so every compile decision was blind | the gap VERIFIED; DUMP is granted on the tablet (TR:609), declared at `AndroidManifest.xml:33`; the new code reviewed in section 10 | decides W2 / W5 with data; cost two or three `dumpsys package` runs a day | safe: a failed look keeps the old reading and says "not known"; proof: the line in the next report and `DexoptStateTest` | no (K3 mentions the manual compile only) | **worthwhile, ship it** |
| **W2** baseline profile for our code | `app/src/main/baseline-prof.txt` (working tree); `app/build.gradle.kts`; the installer is already in the APK (5.3) | after every update the app runs interpreted / JIT-cold until a nightly job has a profile; today only `COMPILE_SELF` over adb fixes that | AGP tasks and the profile experiment VERIFIED(build); on-device install INFERRED | no adb after updates; first minutes after each of 2 starts a day run compiled code; APK +5-15 KB. **Remaining work: add class rules, rebuild, assert the asset in CI (W3), read the result with W1** | low; the library is documented for API 24+; proof: report shows `speed-profile (bg-dexopt)` after a night on the charger | no (K3 only) | **worthwhile** (small) |
| **W3** APK and profile check in CI | `.github/workflows/android.yml` (a step after the build) | the dex storage mode, the real APK composition and the presence of `assets/dexopt/baseline.prof` are only inferred; nothing packages a release locally (no keystore) | VERIFIED gap | turns 5.2 arithmetic and 6.2 into facts; guards W2 against a silent regression | none; read-only step: `unzip -lv` of the signed APK (`classes.dex` shows method `Stored`; `assets/dexopt/baseline.prof` and `baseline.profm` present); fail the build when the asset is missing or under 4 KB | no | **worthwhile** (10 lines of CI) |
| **W4** `commit()` duration counter | `Features.java:28-45`, `Mode.java:75`, `CrashGuard.java:60`, report line | all "estimated 2-15 ms fsync" figures in CA1 C7 / U7 are unmeasured | VERIFIED call sites (2.2) | gives the number that decides whether more commits move off the main thread | none if it only times and records the worst / count per file into the report | extends CA1 U7 | **worthwhile** (measure) |
| W5 `COMPILE_SELF` filter | `ShellProtocol.java:210` | `-m speed -f` compiles the whole 8.05 MB dex, 70 % of it libraries nobody runs; oat est. 16-32 MB | VERIFIED | after W2 is proven: `-m speed-profile -f` gives a 5-9 MB oat, a shorter compile and clustered hot code; same hot code | the Setup item would then show "speed-profile" as done: `DexoptState.compiled()` already knows both; do it only when W1 shows the profile working | K3 | marginal (after W2) |
| M1 VPN poll trims | `VpnOff.java:139` (app-op check on every quiet sync), `:204-211` | the poll runs the whole sync pipeline (3 holds) to answer one network look; `allowed()` is a binder call per quiet sync | VERIFIED | about 2-3 ms per tick, 2 ticks a minute: 0.005 % CPU, battery not measurable | the VPN row must never show (Tj, CE1): any change needs section `vo` | no | marginal: add a syncs-per-hour counter to the report first; trim only if it reads more than 4 per minute |
| M2 preload the three prefs files | `TjApplication.java:20-25` | three sequential blocking loads | VERIFIED | INFERRED 3-8 ms per start, 2 starts a day | low | A5 | no |
| M3 five permanent threads | `AnimationSpeedup.java:176`, `SettingsSpoof.java:83`, `VpnOff.java:65`, `LockLog.java:51`, `NotifSightings.java:113` | one more than CA1 M7 counted | VERIFIED | 0.2-0.5 MB INFERRED; merging risks head-of-line blocking behind 100+ ms PackageManager calls | `allowCoreThreadTimeOut` on the two executors like `RecentsFilter.java:185-191` is safe | M7 | no (marginal) |
| M4 English-only resources | `app/build.gradle.kts` (`androidResources.localeFilters`) | 82 locale folders; `resources.arsc` is 1,034,232 B | VERIFIED(build) | APK about -0.6 to -0.8 MB (INFERRED: most of the arsc is translations), faster install; no CPU or RAM effect | low for one owner; framework strings unaffected | no | marginal |
| M5 drop the `androidx.preference` subtree | `launcher3/.../settings/SettingsActivity.java` + 4 files (5.2) | `preference` pulls appcompat / fragment / activity / lifecycle | VERIFIED fan-in and sizes | about 0.4 MB of code (13 %); kotlin and coroutines stay; APK size only | needs the fork reviewer to confirm `SettingsActivity` is unreachable; lint + gate | no | marginal |
| M7 accessibility config | `app/src/main/res/xml/tj_accessibility.xml` | `flagIncludeNotImportantViews`, `notificationTimeout=0`, no `packageNames` filter set the tree size and the IPC volume the service pays for | VERIFIED file; effect INFERRED | possibly smaller trees (scans avg 28-33 ms); no known gain on event volume (CONTENT_CHANGED bypasses the timeout; S10 stands) | high: an id lookup silently skips a not-important view; needs node-count telemetry and the lock / PIN sections first | S10 covers event types only | marginal, for reviewer B |
| N1 thread-per-task to a pool | about 40 `new Thread(` sites (`LockQuiet.java:101,136,151,170,185`, `ReturnReset.java:143,171,193,384`, `AppOrigin.java:122`, `RecentsSweep.java:158`, `BatterySaverGuard.java:164`) | 15-20 thread starts per lock / unlock cycle | VERIFIED sites | INFERRED 1-3 ms on the unlock path | in lock-safety code; below the 0.5 s unlock noise | no | no |
| N2 `LockLog` per-line open / stat | `LockLog.java:67-77` | cheap, async, no fsync | VERIFIED | none worth having | n/a | BY1 (fine) | no |
| N3 SettingsProvider rewrites | section 4 | a few system-side file rewrites per lock cycle | INFERRED | n/a | all safety state | no | no |
| N4 `largeHeap`, KeepAlive, minify, Kotlin plugin, `InitializationProvider` | `config.xml:3`, `KeepAlivePolicy.java:142`, `build.gradle.kts:42`, merged manifest 394 | already off / deliberate / needed for the profile | VERIFIED | n/a | n/a | K1, K2 | no |

## 8. Checked and fine (do not re-check)
- No wake source (WakeLock, alarms, jobs, WorkManager): grep over `app/`, `launcher3/src`, `launcher3/go`, `wallpaperpicker/src`, 0 hits.
- Process start does no disk write except one `apply()` (`ProcessLife.java:114`); no service starts; observers are single registrations.
- Holds write nothing when unchanged (`NotifQuiet.java:90-108`, `RadioIconHide.java:91-111`, `AnimationSpeedup.java:246-252` log only on change); the grant memo is 3 s (`AnimationSpeedup.java:55`).
- `VpnOff.watch` registers one network callback and retries after 60 s on failure, not per sync (`VpnOff.java:268-289`); `unwatch` is a no-op by design.
- `HomeCurtain` rewrites its PNG only when the layout changed (`TjAccessibilityService.java:2218-2224`); stamp renewal is gated by `stampDue`.
- `LockLog` is bounded 48 / 32 KB and written on its own thread; `HelperLife` is bounded 48 / 24 KB and exists only when the helper runs.
- `largeHeap` is false at 564 dp; `KeepAliveService` is off and does no work when on.
- The exported `ProfileInstallReceiver` is guarded by DUMP; no other app can reach it.

## 9. Open measurements (cheapest first)
1. **W1** is in the tree: after it ships, read the line once after a night on the charger (expect `speed-profile (bg-dexopt)` if the profile works, `verify` / `quicken` if not) and once after `COMPILE_SELF` (`speed (cmdline)`).
2. **W2 / W3** rebuild with the five rules plus class rules, then `unzip -lv` the APK: `assets/dexopt/baseline.prof` and `.profm` present, `classes.dex` stored (not deflated); expect the profile at 5-15 KB (the one-rule experiment was 1.5 KB).
3. **W4** commit timing per file in the report. 4. A per-hour count of `AnimationSpeedup.syncNow` runs by trigger (settles M1 and the Quickstep heartbeat cost).
5. Ask Tj for the Launcher control "VPN" line (`VpnOff.describe`, `VpnOff.java:295-304`): "allowed by Android: yes/NO" tells whether the 30 s poll runs at all on the tablet.
6. After the next release: does the first-fade-since-start figure (`LATEST.md:33`, 19 ms against 12 ms warm) move once the profile is compiled?

## 10. The in-flight CG1c code (working tree at about 19:25Z, read-only review)
Files: `DexoptState.java` (92 lines, pure parser), `DexoptWatch.java` (113 lines), `TjApplication.java:29`, two lines in `ControlActivity`, one in `ReportState`, the `COMPILE_SELF` done-check in `SetupActivity`
(`|| DexoptWatch.full(this)`), three small `Features` methods, `DexoptStateTest.java` (97 lines), and `app/src/main/baseline-prof.txt` (five wildcard rules).

- **What a look costs.** `DexoptWatch.read` (`:80-112`) forks `dumpsys package com.tj.launcher` (one `ProcessBuilder` start from a 69 MB process, INFERRED 5-10 ms of our CPU), reads the output until the
  "Dexopt state:" block (tens of KB, about 1 ms of parsing), and stops 16 lines after the package header. System-side it is a package dump (INFERRED tens of ms in `system_server`, the global package lock only for the
  "Packages:" part). It runs on thread `TjDexopt` at background priority, with a watchdog thread (`TjDexoptWatch`, polls every 50 ms, at most 10 s; the same pattern as `SsBatteryStats.java:152-160`).
- **How often.** `refreshSoon` posts one 20 s delayed task per main-process start (`TjApplication.java:29` is inside `if (main)`: no cost in the picker processes); the look happens only when the stored reading
  belongs to another version or is older than 6 h (`DexoptWatch.java:19,35`, `DexoptState.stale`, tested), so about 2 a day; plus one per opening of Launcher control (forced, guarded by `RUNNING`). Fine.
- **No wrong claim is possible:** a failed or unparsable look returns `NONE`, the stored reading is kept with its age, and the UI says "not known yet". `compiled()` / `full()` only accept `speed-profile`, `speed`, `everything`.
- **Premises that nothing in this container can prove (ask for the first report line):** (i) an app-uid `dumpsys package` runs on the tablet (DUMP is granted, TR:609; the SELinux rule for an app executing `dumpsys` and the output
  format of this MediaTek build are unverified; the same technique is used by `SsBatteryStats.read`, whose tablet result is not in the saved report); (ii) on a 32-bit Go build the first status line is the `arm:` one (the parser takes the first
  line after the package header: right for either ABI).
- **Notes, all small:** (a) put the four words in a file of their own or use `IfChanged` for the int / long (`Features.setIntAsync` / `setLongAsync` always write, `DexoptWatch.java:45-46`): negligible at 2-3 a day;
  (b) `Process.waitFor(long, TimeUnit)` (API 26) would replace the 50 ms polling watchdog: cosmetic; (c) the 6 h age limit means a reading taken just before the nightly job shows the old state until Launcher control is opened (it forces a look): by design.
- **Profile file:** keep the five rules, ADD three class lines (6.3), rebuild, and decode again before reading any size. Keep `androidx.startup` / `profileinstaller` as they are (a manual `writeProfile` or removing the
  provider would lose the install on API 29 for no gain). For a "compile now" button without waiting for the idle job: adb `INSTALL_PROFILE` broadcast, then `cmd package compile -f -m speed-profile` (shell only: `COMPILE_SELF`
  could use that line after W2 is proven, W5).
