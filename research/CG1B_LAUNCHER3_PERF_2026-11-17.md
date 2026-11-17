# CG1b: Launcher3 fork and resources, performance review (2026-11-17)

TASKS CG1b (Tj, 2026-11-17: audit code and design for CPU, RAM and speed on the MT8168 / 2 GB / Android 10 Go tablet). Read-only review of
`launcher3/` (src, go/src, res, go/res, iconloaderlib), `wallpaperpicker/` and the Tj hook sites inside them. Nothing was built, run or measured.
It continues `PERF_REVIEW_2026-11-12*.md` (BY1) and `PERF_AUDIT_2026-11-13.md` (CA1): ids from those files keep their names (A1, C5, U8, M3 ...),
new ids here are G1 ...

**Flags.** VERIFIED = I read the code at the cited line in this tree (head after CA1b, CG1c baseline profile present). INFERRED = reasoned from the
code path or from AOSP behaviour recalled from memory, no measurement. MEASURED = a number from a tablet report or a CI run (source named).
**Basis of every time estimate:** Cortex-A53 cores at about 1.5 GHz run Java code about 3-5x slower per thread than a current phone core, and code
that is still interpreted or JIT-cold is another 2-3x slower (CG1c's baseline profile attacks that second factor; its rule
`HSPLcom/android/launcher3/**` already covers this whole slice). All "ms" below are counted operations times that cost, never a stopwatch.

## 1. Bottom line

- The fork is lean at idle (VERIFIED by grep over `launcher3/src`, `go/src`, `iconloaderlib`, `wallpaperpicker/src`): no timer, alarm, job,
  WorkManager or repeating Handler; one INFINITE animator (`CellLayout.java:2031`, the reorder hint while an icon is dragged); one hardware layer
  (`CellLayout.java:360`, only while a page moves or an icon is dragged); no software layer; widgets off, shortcuts stubbed; the drawer filter runs
  before any icon lookup (`AllAppsList.java:77`, lookup at :83); the three databases open lazily on the loader / worker thread.
- Launcher3's own data at idle is about 3-6 MB of the 69 MB PSS (INFERRED, section 6). Even a perfect Launcher3 diet moves PSS by at most about
  3 MB (4 %). The RAM question is not in this slice; section 6 says where it probably is and G2 measures it.
- Recurring CPU that is still left here: one full model reload per return to custom (G1) and the Hidden apps screen build (G3). Both are small in
  total but sit where Tj notices them (3 s after an unlock into custom; opening a settings page).
- Worth doing: G1, G2, G3, G4. G2 and G4 are measurements of a few lines each that decide everything else. The rest is marginal or no.

## 2. Findings at a glance

| id | verdict | where | one line | gain (how estimated) | in CA1/BY1? |
| :- | :- | :- | :- | :- | :- |
| G1 | worthwhile | SettingsSpoof.java:103-124; LauncherModel.java:309-374 | one targeted Settings-row update instead of a full model reload at each return to custom | ~100-200 ms main thread + ~200-500 ms loader CPU per return, 3 s after unlock (INFERRED, counted operations) | yes: A1 / C5 / U8 (NEXT), S2 did the OFF side |
| G2 | worthwhile | ReportState.java:68-84; tools/emulator_scenario.sh:282-290 | split OUR process's PSS (summary categories + smaps by kind) in the report and in CI section cd2 | none by itself; picks every later RAM change | no |
| G3 | worthwhile (small) | HiddenAppsActivity.java:55-60, 85-126 | rows and icons of every launcher app built on the main thread at open | ~1-3 s freeze (INFERRED) + 1-11 MB transient, Quickstep only | partly: M8 / D5 as RAM only |
| G4 | worthwhile (measure) | Launcher.java:325, 2294; BaseDraggingActivity.java:87-96 | tag the "LAUNCHER created" line: relaunch yes/no, night bit, ms to first bind | none; says how often the Launcher is re-created, and what AOT buys on cold start | no (CA1 U12 wrote the plain line) |
| G5 | marginal | AllAppsContainerView.java:121-123; AlphabeticalAppsList.java:159 | second adapter holder builds a 2nd ICU index + collator for a work profile that cannot exist | 5-25 ms + 0.1-0.3 MB per Launcher creation (INFERRED) | no |
| G6 | marginal | TjHooks.java:175-186 | state hook requests a layer refresh on unchanged state; start-of-open removes the patch window inside the animation | 2 of 4 refreshes per drawer cycle are no-ops; one WM binder call (~3-8 ms) in the first frames of each open | pointer: A7 / B7 (NEXT) |
| G7 | marginal | MainProcessInitializer.java:41; IconShape.java:439-476 | `IconShape.init` (7 region rasterisations) on the main thread in the provider's onCreate | 10-20 ms per process start, 2 a day | no |
| M4 | marginal | BaseIconCache.java:249-269; IconCache.java:103-115 | icons of hidden apps can enter the memory cache | <= 0.5 MB steady | yes (M4) |
| M3 | marginal | HomeLayer.java:33-37 | full-window PixelCopy per curtain build | ~1-2 ms/s on the render thread, 2.4 MB transient | yes (M3 / D3) |
| M5 | no | TjAccessibilityService.java:1361, 1443, 1623 | ARGB_8888 masks | <= 0.1 MB | yes (M5 / D4) |

## 3. Worthwhile findings

### G1  Replace the full reload at the return to custom by one targeted Settings-row update
**Where.** `app/src/main/java/com/tj/launcher/Mode.java:77` -> `SettingsSpoof.syncSoon` (`SettingsSpoof.java:89-96`, delay `SYNC_DELAY_MS = 3000` at
:34) -> `syncNow` (:107-126; reload posted at :123, owed-reload branch :116/:124) -> `HiddenApps.reloadLauncher` (`HiddenApps.java:113-115`) ->
`LauncherModel.forceReload` (`LauncherModel.java:309-334`) -> `startLoader` (:344-374) -> `LoaderTask.run` (`model/LoaderTask.java:166-240`).
**What happens.** Three seconds after every mode change the Settings look-alike alias is flipped, and when the alias goes ON (return to custom) a
COMPLETE LoaderTask is posted on the main thread's behalf: favorites query, `bindWorkspace` (the workspace is torn down and rebuilt:
`Launcher.startBinding` :1929), `getActivityList` for all users + filter + `bindAllApps`, `getInstalledPackages(MATCH_UNINSTALLED_PACKAGES)` for
all 176 packages in the `IconCacheUpdateHandler` constructor (`iconloaderlib/.../IconCacheUpdateHandler.java:77-87`, one large binder reply), the
icon-DB sweep, and three `waitForIdle()` passes (LoaderTask.java:183, 204, 217) that wait for the MAIN looper to go idle, which the accessibility
service shares. CA1 S2 already removed the second reload (alias OFF inside Quickstep), so it is now one per Quickstep session, but it lands in
the first seconds of the custom session. Only ONE drawer row depends on the mode: the real `com.android.settings` (`SettingsSpoof.java:48-56`).
**Evidence.** VERIFIED (all lines above read). New facts compared with A1:
- The cheap shortcut `LauncherModel.onPackageChanged("com.android.settings", user)` would NOT work. `AllAppsList.updatePackage`
  (`AllAppsList.java:178-215`) never re-applies the filter to an entry that is already in `data`; the filter runs only in `add` (:77). A targeted
  task must call `removePackage` / `addPackage` explicitly (`AllAppsList.java:127-150`), as A1 said.
- The alias flip itself already reaches Launcher3 incrementally through the system's package-changed callback -> `PackageUpdatedTask` OP_UPDATE
  (`PackageUpdatedTask.java:114-120`), which also re-renders and rewrites the 2-3 icons of our own package (`IconCache.java:103-115`): about 10-30 ms
  of worker time per flip. Not worth touching.
**Gain.** Per return to custom: the main thread is spared the workspace unbind / rebind and the re-layout and re-bind of the invisible drawer
RecyclerView (about 30-40 workspace icon views inflated and bound, up to ~48 holders re-bound; ~100-200 ms in 2-3 bursts, INFERRED), and the
loader thread ~200-500 ms CPU plus one large binder reply. Visible symptom today (A1, not verified): the home icons blink 3 s after the unlock.
One per Quickstep session; the count per day is not in any report.
**Risk and proof.** A wrong difference gives two Settings rows or none. Put the decision in a pure function
(`rowAction(presentInModel, shouldShow) -> NONE | ADD | REMOVE`, JVM test with the four cases), run it inside
`LauncherModel.enqueueModelUpdateTask(new BaseModelUpdateTask() ...)` (patterns: `LauncherModel.java:499-508`, `PackageUpdatedTask.java:157-167`),
keep `forceReload` as the fallback when the model still disagrees a few seconds later, and keep `HiddenAppsActivity`'s explicit reloads (Tj starts
those). Prove with emulator section `ss` ("ONE Settings in the custom drawer") plus a custom -> Quickstep -> custom round trip with the switch
flipped while in Quickstep (the `reloadOwed` case), and logcat shows no "loading default favorites" (`LoaderTask.java:277`) 3 s after the mode change.
**In CA1/BY1.** Yes (A1 / C5 / U8, NEXT). **Verdict: worthwhile**; it is the largest recurring Launcher3-side cost left.

### G2  Say where the other 60 MB are: PSS categories and smaps groups in the report, and the same for the emulator
**Where.** `app/.../ReportState.java:68-84` (`costLines` prints `mi[0].getTotalPss()` and the Java heap, nothing else);
`tools/emulator_scenario.sh:282-290` (`cdtotal` reads only TOTAL for our process; `cdmem` keeps the category rows only for the STOCK launcher);
`tools/tablet_intake.py:196-225` already parses the "App Summary" rows of `dumpsys meminfo` for the stock launcher.
**What is known (MEASURED).** Tablet reports 2026-11-09 / 10 / 10-2: total PSS 68 / 71 / 69 MB, Java heap 6 / 8 / 5 MB (`research/tablet/*/tablet-report.txt`
line 626-627). So 60+ MB are code, native heap, graphics or runtime. CI runs `cd2` / `cd2h` (`research/CD2_EMULATOR_GROWTH_2026-11-14.md`): OUR
process 38.1 -> 44.4 MB over 18 Quickstep sessions without the helper (+0.35 MB per session; flat with the helper), and 44.4 -> **69.6 MB after 30
Recents open / close cycles**. That note says "may be GC timing; not investigated". The tablet's 69 MB looks like the same plateau (the tablet runs
without the helper and Tj opens Recents often). Nothing in the repo splits our process's memory, so nobody can say what grew.
**Change.** (1) In `costLines` add the rows of `Debug.MemoryInfo.getMemoryStats()` on the object it already fetches (no extra call): summary
java-heap, native-heap, code, stack, graphics, private-other, system, total-pss, total-swap, printed in the same words as `dumpsys meminfo` "App
Summary" so `tablet_intake.py:212` parses it with the same pattern. (2) Once per report, on its worker thread, read `/proc/self/smaps` and add up Pss
by kind (oat / odex / vdex, art, apk / jar / dex, so, ttf, malloc heap, dalvik spaces by name, gralloc / ION / GPU device nodes, ashmem, other) and
print the top eight kinds without paths. (3) Emulator: in `cd2` / `cd2h` keep `dumpsys meminfo com.tj.launcher` at s0, s24 and sR30 (file plus one
INFO line with the category rows); the helper already does exactly that for the stock launcher. One CI run then says whether the +25 MB is Native,
Graphics or Other, and the s0/s24/sR30 split says whether it is a one-time warm-up or a slope.
**Gain.** None directly; it is the instrument. It decides which of G5 / M4 / the overlay windows / CG1c actually matter.
**Risk and proof.** Report only, inside try / catch. A smaps read is ~0.6-0.9 MB of text for ~1,500 mappings (INFERRED), 30-80 ms on a worker, never
on the unlock path. Privacy: kinds only. Prove with a JVM test of the pure grouper on a canned smaps sample (plus a garbage line) and a fixture in
`tools/test_tablet_intake.sh`.
**In CA1/BY1.** No (D reasoned from code and quoted the same total). **Verdict: worthwhile**, first thing to ship.

### G3  Hidden apps screen: build rows and icons off the main thread
**Where.** `app/.../HiddenAppsActivity.java:55-60` (`queryIntentActivities`, then `ri.loadLabel(pm)` and `ri.loadIcon(pm)` for EVERY launcher app inside
`onCreate`, main thread), `:85-126` (a `ListView` whose `getView` builds a new row view tree on each call, no `convertView` reuse; every decoded
`Drawable` stays in `Row.icon` while the screen lives). The page opens only in Quickstep mode (`:52`).
**What happens.** Opening "Choose the apps shown in the custom launcher" opens a `Resources` per package and decodes the adaptive icon (two layers,
XML + PNG / vector) of every app before the first frame. On this tablet: 176 packages (`research/tablet/2026-11-10-2/tablet-report.txt:708`), so
about 60-100 launcher apps. About 15-40 ms each on an A53 gives ~1-3 s of main-thread time (INFERRED), plus 1-11 MB transient (two 115 px layers of
~53 KB per app, held until the screen closes). The Control screen has a build-time counter (`ControlActivity.java:80-85`, 346 ms on the tablet);
this page has none.
**Change.** Load (package, label) on a worker and show the list when ready; load each icon per visible row (worker + `ImageView` callback, cancel on
row reuse, small LRU of ~24); use `convertView`; add `HiddenAppsActivity.lastBuildMs` to the report like Control's. Look unchanged.
**Gain.** Removes the freeze and cuts the transient to ~25 icons (~1-2.5 MB). Rare screen: this is polish, not battery.
**Risk and proof.** Low (look-only page). Emulator sections `o` (picture `60-hidden-apps-big-switches`) and `mu` (MU-12a0 "the screen is open" check
and the row / switch checks) already drive the real screen; add the counter to the section's INFO line.
**In CA1/BY1.** Partly: M8 / D5 listed it as a RAM item ("N x 10-110 KB while open"). New here: the main-thread time is the bigger cost.
**Verdict: worthwhile, small and simple.**

### G4  Count and explain Launcher re-creations, and time the first bind (diagnostic only)
**Where.** `launcher3/.../Launcher.java:289-325` (onCreate; hook call `TjHooks.onLauncherCreated(this)` at :325), `:358` (`startLoader`), `:2294`
(`finishFirstPageBind`); `BaseDraggingActivity.java:81-96` (`updateTheme()` -> `recreate()`), `util/Themes.java:38-44` (theme follows
`uiMode` night and the wallpaper colours); `app/src/main/AndroidManifest.xml:208` (the Launcher's `configChanges` has no `uiMode`, `fontScale`,
`density` or `locale`); `app/.../TjHooks.java:167-173`.
**What happens.** The Launcher activity is destroyed and re-created (full `setupViews` inflate at :335, then `startLoader` takes the "model already
loaded" branch and binds workspace + all apps synchronously, `LauncherModel.java:358-366`) whenever uiMode night flips, font scale / density /
locale changes, or the wallpaper colours change the theme resource. A stopped activity (Quickstep in front) defers it to its next resume, so the
re-creation falls at the unlock into custom, on the main thread the service and the loader also use. This user has more night flips than most:
battery saver forces the dark theme (Android 10 `UiModeManager` power-save rule, from memory) and the app switches the saver off and on around the
lock screen (CA1 S-BA2, `BatterySaverGuard`). Cost per re-creation ~250-600 ms on the main thread (INFERRED: `launcher.xml` inflate 100-200 ms +
bind 100-200 ms + text layouts). CA1 U12 writes "LAUNCHER created (locked=..., process up N ms)" per creation (`TjHooks.java:170`), but the newest
tablet report (2026-11-10) predates it, so there is no count yet.
**Change (diagnostic only).** Pass `savedInstanceState != null` and the night bit to `onLauncherCreated` ("relaunch=yes/no night=yes/no"; "process
up" already separates a restore after a process death), and write "first page bound N ms after onCreate" from `finishFirstPageBind`. One line each
into the existing async lock log. Adding `uiMode` to `configChanges` would NOT help: the theme code recreates the activity itself on the same flip
(`BaseDraggingActivity.java:92-95`), and the look must follow night mode like the stock launcher.
**Gain.** None directly. It answers "how many re-creations per day against 2 process starts" (if the number is large, the next step is to stop the
saver flip from landing while the custom Launcher is resumed) and it gives the tablet its only cold-start number, i.e. the effect of CG1c's baseline
profile. If a "random custom home" report (CG1a) coincides with a "LAUNCHER created relaunch=yes" line, that is a lead.
**Risk and proof.** None for behaviour (try / catch, async log). A JVM test is not needed; check the emulator lock log for the new fields.
**In CA1/BY1.** No. **Verdict: worthwhile as a measurement; act only on its numbers.**

## 4. The four NEXT items of this slice (M3, M4, M5, M8), re-validated

| id | re-validation (current lines) | gain | verdict |
| :- | :- | :- | :- |
| M3 | VERIFIED: `HomeLayer.java:33-37` allocates a full-window ARGB_8888 bitmap and `PixelCopy.request(window, rect, ...)` (window bounds `TjHooks.java:149-161`). Quiet-time only, at most one per ~15 s (CA1). A GPU readback of 2.4 MB on the render thread is ~10-25 ms (INFERRED); the risk is one dropped frame if a touch lands inside it. `PixelCopy` takes a source `Rect`, so a thin strip for the stability hash and the full copy only when the hash changed is possible. | ~1-2 ms/s average, 2.4 MB transient | marginal: keep NEXT with section `cu`; no pixel risk in the strip variant |
| M4 | VERIFIED: `BaseIconCache.java:269` (`addIconToDBAndMemCache` always puts). Reachable from `IconCacheUpdateHandler.updateIcons(allActivityList, ...)` (`LoaderTask.java:196-200`, ALL activities, hidden included) only when a DB row is missing or out of date (first run, Clear data, locale or OS change, an app update), and from `IconCache.updateIconsForPkg` (:103-115) for any changed package. Hidden apps otherwise never get an icon lookup (`AllAppsList.java:77` before :83). Entry = 64x64 ARGB_8888 = 16 KB (`iconImageSize` 60 dp = 64 px at 170 dpi, `go/res/xml/device_profiles.xml:20`). | up to ~1.6 MB after a fresh DB until the next process start (100 hidden apps x 16 KB); steady state 0-0.3 MB | marginal: not before G2 shows Java / native heap matters |
| M5 | VERIFIED: three ARGB_8888 allocations at `TjAccessibilityService.java:1361, 1443, 1623` (crop, drawable copy, mask from pixels). CA1 counts 137 KB kept per patch window. | <= 0.1 MB (0.15 % of PSS) | no: lock-safety pixel code, pixel equality on MediaTek unproven, gain below noise |
| M8 | see G3 (line numbers re-checked: `HiddenAppsActivity.java:55-60, 85-126`) | freeze + transient | worthwhile, small |

## 5. Marginal findings

**G5  A second adapter holder, ICU index and collator for a work profile that cannot exist.** `AllAppsContainerView.java:121-123` builds
`AdapterHolder` MAIN and WORK; each `AlphabeticalAppsList` constructor (`AlphabeticalAppsList.java:156-163`) builds an `AlphabeticIndexCompat`
(an `android.icu.text.AlphabeticIndex` plus `buildImmutableIndex`, `compat/AlphabeticIndexCompat.java:104-119`) and an `AppInfoComparator`
(`Collator.getInstance()`); both lists also register on `AllAppsStore` and re-run `onAppsUpdated()` (an empty filtered sort) on every bind. Cost: 5-25
ms and 0.1-0.3 MB native per Launcher creation (INFERRED; ICU collation data itself is mmapped and shared). Fix: give the WORK list the MAIN
list's index and comparator, or create the WORK holder on first need. Risk: low; proof = emulator `base` / `o` drawer pictures and the
alphabet fast-scroller section letters. Per day it saves well under 0.1 s (2 starts + the G4 count), so it is marginal unless G4 shows many
re-creations.

**G6  The state hook asks for a layer refresh it does not need.** `TjHooks.java:175-186` (`watchState`, `setNormal`): `setNormal(v)` calls `refreshLayersSoon()` at every
transition start AND complete, also when `launcherNormal` did not change (2 of the 4 calls in one drawer open + close are no-ops, ~0.1-0.5 ms each:
nothing). The real cost is the one that changes state: at the START of a drawer open the refresh removes the patch window (a WindowManager binder
call plus `ViewRootImpl` teardown, ~3-8 ms INFERRED) during the first frames of the open animation, although the patch is already at alpha 0
from the finger-down (A7 said the same). Deferring that removal to the transition's end is invisible. This is lock-safety / home-patch code: it
belongs to slice B's B7, needs the films of sections `hp` / `hf`, and should be measured first (log the ms of `refreshLayers` at transition start).

**G7  `IconShape.init` in the provider's onCreate.** `MainProcessInitializer.java:31-41` runs on the main thread BEFORE `Application.onCreate`;
`IconShape.pickBestShape` (`IconShape.java:439-476`) rasterises 7 shapes into 200x200 regions, XORs them with the system mask and adds
`IconNormalizer.normalizeAdaptiveIcon`: ~10-20 ms per process start (INFERRED), 2 starts a day. Could move to the loader thread (the result is
needed by `FolderIcon` / `IconNormalizer` consumers, so every reader would need a "ready" check): not worth the risk.

## 6. Memory at idle

### 6.1 Known facts
- Tablet (MEASURED, 3 reports): total PSS 68-71 MB, Java heap 5-8 MB of 128 (`config_largeHeap` is false below sw720dp:
  `launcher3/res/values/config.xml:3`, so the 128 MB limit is real).
- CI emulator (MEASURED, CD2, x86, software GL so Graphics reads 0): a fresh stock-launcher process 16.8 MB (Java 1.4 / Native 7.4 / System 5.0 /
  Private other 1.0); the same process after its home and Recents were built 36-42 MB (Java 6.1 / Native 15-21 / System 11.3): about half of the
  stock launcher is native heap. Our own process 38-45 MB in sessions, 69.6 MB after 30 Recents cycles, no split.
- Our own bitmaps (CA1b state): Quickstep mode (the long idle) about 2.7 MB (curtain 2.4 MB kept by `HomeCurtainRules.keepInMemory`, patch crop and
  masks ~0.3 MB); custom mode about 0.3 MB. The wallpaper copy (4.9 MB) is dropped since v0.9.8.

### 6.2 What Launcher3 holds (INFERRED; counts from the code, sizes from typical object costs)
| item | size | basis |
| :- | -: | :- |
| Launcher view tree while stopped (Quickstep): workspace icons, drawer holders, chrome | 1.0-1.5 MB Java + 0.2-0.5 MB native | ~130-200 views x 5-6 KB; the window stop discards display lists and hardware resources (AOSP `ViewRootImpl.setWindowStopped`, from memory) |
| Icon memory cache (`BaseIconCache.mCache`, 64x64) | 0.6-1.6 MB | 40-100 entries x 16 KB (M4 adds hidden apps only after a fresh DB) |
| `AllAppsList`, `BgDataModel`, `AppInfo`s, `DeviceProfile`s, receivers | < 0.2 MB | ~100-200 small objects |
| 2 x ICU index + 2 x collator (G5) | 0.1-0.3 MB | native, mostly shared data |
| SQLite connections (`launcher.db`, `app_icons.db`) | 0.3-1.0 MB | page caches; `Launcher.onStop` -> `onTrimMemory(UI_HIDDEN)` -> `SQLiteDatabase.releaseMemory()` (`Launcher.java:890, 1867-1881`) |
| Threads: `launcher-loader`, UiThreadHelper looper, 0-2 pool threads (`Utilities.java:157-169`; users: `Workspace.java:1163`, `SessionCommitReceiver.java:110`) | 0.15-0.3 MB resident | ~50-80 KB each; the 5-core pool never fills |
| Total | about 3-6 MB (5-9 % of 69) | |

### 6.3 The three biggest consumers (likely, INFERRED; G2 turns this into numbers)
1. **Native heap plus graphics buffers and caches** (bursty bitmap work, HWUI / Skia caches, window buffers of the service's overlays). Evidence: native
   is ~50 % of the stock launcher's PSS in CD2, and our process jumps +25 MB under Recents scanning with Java heap flat (5-8 MB on the tablet).
   On the tablet the GPU side (PowerVR, hardware icon bitmaps, EGL surfaces) lands in "Graphics" or in the mtrack rows and was never read.
   Overlay windows are hardware-accelerated because the application sets `hardwareAccelerated=true` (`AndroidManifest.xml:77`; for parentless
   windows `WindowManagerGlobal.addView` copies the application flag, from memory), so each gets an EGL surface and 2-3 buffers; a full-screen cover
   or curtain is 2.4 MB per buffer while it exists. A lever for slices B / D, only after G2.
2. **Mapped code and JIT**: dex, oat / vdex and boot-image pages touched, plus the JIT code cache. The app is one unminified dex (Launcher3 +
   androidx + protobuf-nano; R8 is off by decision K1) and runs interpreted / JIT-compiled until the idle compile (K3). CG1c moves hot code to
   file-backed compiled pages, which are cheaper and reclaimable.
3. **The runtime floor**: zygote-shared pages counted proportionally, ART structures, shared libraries, about 17 MB for a fresh Launcher3-based
   process (CD2 s0 figure; the tablet's 32-bit userland may be a little lower). The app cannot reduce it.
Not in the top three: the Java heap (5-8 MB, MEASURED) and Launcher3's own data (3-6 MB).

### 6.4 What would change the ranking
G2's split. Graphics / EGL mtrack large -> overlay window design (hardware vs software, buffer count) and HWUI cache behaviour. Native heap large
with small Graphics -> allocator fragmentation after bitmap bursts (curtain copies, PixelCopy, crop decodes) and the Recents-scan growth. Code
large -> CG1c's result first. Only if Java heap or `[anon:libc_malloc]` growth tracks Launcher3 objects would G5 / M4 matter.

## 7. Start-up path (cold start of the Launcher activity until the first frame)
| step | where | cost |
| :- | :- | :- |
| process fork -> `Application.onCreate` | `TjApplication` (A5) | 48 ms, fork to end 124 ms (MEASURED, report 2026-11-10-2); not Launcher3 |
| provider onCreate | `LauncherProvider.java:123-133` -> `MainProcessInitializer.java:31-41` | prefs read + `IconShape.init` ~10-20 ms (G7) |
| `LauncherAppState` constructor (first `getInstance`, `Launcher.java:310`) | `LauncherAppState.java:70-114` | IDP XML parse, `IconCache` + default icon decode, `WidgetPreviewLoader` (its `SQLiteCacheHelper` opens no database: `SQLiteCacheHelper.java:24-31`), LauncherApps callback, one receiver: ~30-60 ms (INFERRED) |
| `Launcher.onCreate` -> `setupViews` | `Launcher.java:289-335, 1080-1117` | inflate of `launcher.xml` (DragLayer, Workspace, ScrimView, AllAppsContainerView with 2 holders (G5), DropTargetBar, OverviewPanel): ~100-250 ms cold (INFERRED) |
| loader thread | `LauncherModel.java:344-374`, `LoaderTask.java:166-240` | DB open + favorites query + icon lookups; DBs open here, not on the main thread (`LauncherProvider.java:156-170`) |
| first page bound -> first frame | `Launcher.java:2294` | G4 measures it |
The JIT-cold factor dominates every step; CG1c's profile is the right lever. No further Launcher3-specific start-up change is worth its risk.

## 8. Checked and fine (VERIFIED unless marked)
- Nothing runs while the Launcher is not visible except: the LauncherApps package callback (delivered on the worker looper), one receiver for
  locale / managed-profile broadcasts (`LauncherAppState.java:100`), `ConfigMonitor` (configuration, checks font scale and density only), the
  wallpaper-colour listener, and (A8, decided NO in BY1) the notification-dots observer (`LauncherAppState.java:105-113`).
- Wallpaper offset: `WallpaperOffsetInterpolator` posts to a background looper; the Tj hook (`:240`, `:273`) is one debounced main-thread post per
  changed step (A: checked and fine). A one-page home sends nothing.
- `LauncherIcons` pool (~100 KB), `IconNormalizer` (128x128 A8 = 16 KB per pooled factory), `IconShape` static, `DiscoveryBounce` (one pref read),
  `PageIndicatorDots`, `ScrimView` (one rect), all-apps pool sizing: stock, small.
- Widgets: `GO_DISABLE_WIDGETS = true` (`go/src/.../config/FeatureFlags.java`), `LauncherAppWidgetHost.java:73-77` returns early; shortcuts stubbed.
- Wallpaper picker: `:wallpaper_picker` / `:wallpaper_chooser` processes start only on demand (`AndroidManifest.xml:116, 122`) and the Wallpapers
  entry goes to the stock picker (AO1), so they never start in normal use. The picker's code costs dex size only.
- `Utilities.THREAD_POOL_EXECUTOR` (core 5, no core timeout, `Utilities.java:157-169`): only three call sites, so at most 1-2 threads ever exist.
  `allowCoreThreadTimeOut(true)` would save ~100 KB: no.
- R8 / minify (K1), dropping `androidx.preference` (dex size only, unloaded classes cost no RAM), `android:hardwareAccelerated` on the
  Launcher activity (stock): no.
- Notification-dots bool (A8 / `launcher3/res/values-v26/bools.xml:20`): BY1 said NO; unchanged.

## 9. Order of work (measure first, then change)
1. **G2 + G4 + the cd2 emulator leg** (about 40 lines, no behaviour change): ship with the next release. Read the next tablet report for the PSS
   split and for "LAUNCHER created relaunch=" lines against 2 process starts a day; read one cd2 run for our process's categories at s0 / s24 / sR30.
2. **G1** (its own `ss` + round-trip proof). Count Quickstep sessions per day from the lock log to size the gain.
3. **G3** (small, independent).
4. Decide the rest from G2: Graphics / native dominates -> overlay windows and allocator (slices B / D), Code dominates -> CG1c result,
   Launcher3 marginals (G5, G7, M4, M3) only if Tj wants the last 0.5-1 MB and 30-60 ms per start.
