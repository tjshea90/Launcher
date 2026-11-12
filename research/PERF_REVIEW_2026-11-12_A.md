# Performance review, slice A: start-up, load and the app drawer (2026-11-12)

Read-only review. Nothing was built, run or measured here. Scope: process start to the first home frame, a Home press to the resumed Launcher, LauncherModel load and bind, icon cache, the drawer filter (HiddenApps / AppVisibility / AppFilter), the app's hooks inside launcher3/, the wallpaper offset hook, and what the manifest starts. Platform behaviour marked "(AOSP, from memory)" was not re-read from Android source in this review; verify it before you rely on it.

Measured context from Tj's tablet reports:
- Application.onCreate takes 48 ms, and the time from fork to the end of onCreate is 124 ms (research/tablet/LATEST.md:33).
- The launcher process started 2 times in 24 h (research/tablet/2026-11-10-2/tablet-report.txt:630). Cold starts are rare. Costs paid per resume, per Home press or per mode change matter more.
- From Launcher resume to its first frame: 20 ms in a plain unlock (same report :761) and 188 ms in a return-from-Quickstep unlock (:738).

No HIGH finding in this slice. The heavier costs sit downstream of the hooks, inside the accessibility service, which is slice B (see A7).

| id | sev | where | how often |
| :- | :- | :- | :- |
| A1 | MEDIUM | SettingsSpoof.java:114 full model reload | every custom/Quickstep mode change (2 per Quickstep session) |
| A2 | MEDIUM | TjHooks.java:88-89 -> RecentsGuard | every Launcher resume, before its first frame |
| A3 | MEDIUM | RouterActivity.java:58 -> Mode.java:21-24 | every Home press (fsync on the main thread) |
| A4 | MEDIUM | RecentsPolicy.java:30 | every app launch from the custom home or drawer |
| A5 | LOW | TjApplication.java:16-34 | every process start (about 2 a day; also at boot) |
| A6 | LOW | HiddenApps.java:22, :39 | per app on every drawer load |
| A7 | LOW (to B) | TjHooks.java:87, :163-172, :183 | every resume, pause, drawer open and drawer close |
| A8 | LOW | launcher3/res/values-v26/bools.xml:20 | at model creation, plus a permanent observer |
| A9 | LOW (idea) | AllAppsList.java:83 | the first drawer open after a cold start |

## A1 MEDIUM: a full Launcher3 reload on every mode change (Settings look-alike alias)
**Where:** `SettingsSpoof.syncNow` (SettingsSpoof.java:102-114) posts `HiddenApps.reloadLauncher` at :114, which calls `LauncherModel.forceReload` (HiddenApps.java:102-104). It runs 3 s after every `Mode.set` (Mode.java:73, SettingsSpoof.java:34, :89-96).

**What it does today:** `settings_spoof` defaults to ON in release builds (SettingsSpoof.java:43), and `aliasWanted = switch && custom` (:48). So the alias component flips at every custom/Quickstep change, which is twice per Quickstep session.

Each flip already reaches Launcher3 incrementally. The system sends PACKAGE_CHANGED, which becomes `LauncherModel.onPackageChanged` and then `PackageUpdatedTask` OP_UPDATE (LauncherModel.java:208-212, PackageUpdatedTask.java:114-120). That path adds or removes the alias through `AllAppsList.updatePackage` (AllAppsList.java:178-207), filtered at :77. The comment at SettingsSpoof.java:99-100 says the author knew this; the reload was added as "certain".

After that, `forceReload()` stops the loader and runs a complete LoaderTask (LauncherModel.java:309-333, :344-397; LoaderTask.java:169-237).

**Cost:** the whole first load again:
- a query of the favorites database
- a workspace rebind on the main thread: `startBinding` closes an open folder or popup, cancels a drag and removes every screen (Launcher.java:1933-1942)
- `getActivityList` over all apps, the filter and `bindAllApplications`
- `getInstalledPackages(MATCH_UNINSTALLED_PACKAGES)` inside IconCacheUpdateHandler (launcher3/iconloaderlib/.../IconCacheUpdateHandler.java:87): one large binder reply built under the package manager's lock
- three `waitForIdle` passes

My estimate is several hundred ms of CPU on the MT8168 (not measured). When Quickstep starts, it rebuilds a hidden Launcher 3 s after the handshake, while Tj starts apps. When Quickstep ends, it rebinds the home screen Tj is looking at, 3 s after the unlock. Whether a visible blink of the icons results was not verified.

Only one drawer entry changes its filter answer with the mode: `com.android.settings` (SettingsSpoof.java:63-66).

**Change:**
- Replace the post at :114 with a targeted model task. `enqueueModelUpdateTask(new BaseModelUpdateTask(){...})` checks whether `com.android.settings` is in `AllAppsList.data` and compares that with `HiddenApps.isPackageShown`. Only on a difference does it call `removePackage` or `addPackage`, then bind through `bindAppInfosRemoved` / `bindAppsAddedOrUpdated`. The pattern exists at LauncherModel.java:409-420 and PackageUpdatedTask.java:157-167 and :343.
- Skip the task entirely while the mode is Quickstep. The custom drawer is not on screen then, and the return re-checks the facts.
- Keep `forceReload` only as a fallback: if the alias is still in the wrong state some seconds after a flip, reload as today.
- Leave HiddenAppsActivity's explicit reloads alone (HiddenAppsActivity.java:45, :119): Tj starts those himself.
- The OP_UPDATE that redraws the app's own icons (PackageUpdatedTask.java:117) comes with the alias approach and stays.

**Risk:** if the difference is computed wrong, the drawer shows two Settings entries or none. A switch flipped during a Quickstep session must still hide the real Settings on return; the task re-reads the facts, so it does.

**Verify:**
- Put the difference (present, shown) in a pure function and add a JVM test.
- Run emulator section `ss` ("ONE Settings in the custom drawer") plus a custom, Quickstep, custom round trip.
- Logcat must show no "loadWorkspace: loading default favorites" (LoaderTask.java:277) 3 s after a mode change.

## A2 MEDIUM: binder calls and a bitmap upload on every Launcher resume, before its first frame
**Where:** Launcher.java:948 calls TjHooks.java:88-89 before `super.onResume()`. That runs RecentsGuard.java:25-35 and :38-44 (exclude) and :53-56 (describe).

**What it does today:** on every resume (every Home press, every return from an app, every unlock):
- `getAppTasks()`, then for each task of the app `getTaskInfo()` and `setExcludeFromRecents(true)`. That is 1 + 2T calls into the activity manager's global lock, in the middle of a transition (AOSP, from memory).
- `setTaskDescription(new TaskDescription(label, bitmap))` (RecentsGuard.java:55). In Android 10, `Activity.setTaskDescription` scales an icon that has no file name with `createScaledBitmap` on the calling thread. The system then stores the icon as a PNG through TaskPersister and notifies the task listeners in SystemUI and Quickstep (AOSP, from memory).

**Cost:** a few ms of main thread (my estimate) on a path that takes about 20 ms on the tablet (tablet-report :761). Each resume also allocates a scaled bitmap and makes the system write a file.

Repeating either step buys nothing:
- The exclude flag sits on the task's base intent and stays for the life of the task.
- The description belongs to the activity instance (RecentsGuard.java:49-52).
- New Launcher tasks are created already excluded (RouterActivity.java:77).
- The other screens exclude their own tasks: ControlActivity.java:62-63, SetupActivity.java:108-109, HiddenAppsActivity.java:48, AdbGuideActivity.java:64.

The first-time branch (RecentsGuard.java:56-71) also runs `targetPackage`, `getApplicationIcon` and a Canvas draw on the main thread, 300 ms after the first resume of a process.

**Change:**
- Run both steps once per Launcher instance. Keep a WeakReference to the instance last handled in TjHooks. A recreated Launcher gets the description again, so the v0.3.2 bug named in the comment stays fixed.
- Do the label and icon work of the first call on a worker thread, and post only `setTaskDescription` back to the main thread.

**Risk:** if something outside the app cleared a task's exclude flag between two resumes, it would not be set again. Nothing in the app clears it; the system side is unverified.

**Verify:** emulator sections `legacy` (Recents flags, the HOME-type stack) and `hp` (it covers `onLauncherResumed`). In the next tablet report, check the UNLOCKSUM gap "Launcher resumed ... first frame".

## A3 MEDIUM: a synchronous preference write on the main thread at every Home press
**Where:** RouterActivity.java:57-58 calls `Mode.recordHomePress`, which writes with `apply()` (Mode.java:21-24). The router finishes inside onCreate (RouterActivity.java:42-43) and is `noHistory` / `NoDisplay` (AndroidManifest.xml:232-247).

**What it does today:** every Home press rewrites tj_mode.xml with `apply()`. (AOSP, from memory) `apply()` waits 100 ms before it writes. `ActivityThread.handleStopActivity` calls `QueuedWork.waitToFinish()`, which runs every pending write on the calling thread. The router passes its stop a few ms after the write is queued. So the write, including its fsync, happens on the main thread during the Home transition, every time.

The same flush happens at the Launcher's own onStop when an app opens. It then writes, for example, `AppOrigin.markCustom`'s `apply()` (AppOrigin.java:29), which only happens the first time an app is launched after a clear.

**Cost:** one fsync of a small file on the main thread per Home press. That is typically 1-10 ms on eMMC and more under I/O load; not measured on the tablet.

**Change:** keep lastHome and lastHome2 in static volatile fields, and drop the per-press write.
- RouterActivity.java:57 is the only reader.
- Double-Home and the L10 failsafe need presses less than 0.8 s apart (ProfileSession.java:103-106), and the process is alive for that.

**Risk:** a sequence of presses that spans a process death is not recognised; Tj presses again.

**Verify:** the JVM tests of HomeDoublePress and `ProfileSession.homeFailsafe` stay unchanged. Run emulator sections `n` (ways back to custom, the master failsafe) and `legacy`.

## A4 MEDIUM: a wasted package-manager call on every app launch, plus a periodic scan of all app labels during launches
**Where:** BaseDraggingActivity.java:152 calls RecentsPolicy.java:25-36. Line :30 evaluates `ReturnReset.targetPackage` (ReturnReset.java:317-338).

**What it does today:** `startActivitySafely` runs `RecentsPolicy.apply` before every `startActivity`. That evaluates `ReturnReset.targetPackage(c)` as an argument even though `recents_only_target` defaults to OFF (RecentsPolicy.java:13, :22), and the result is then thrown away.
- Each call costs one `getApplicationInfo` binder call on the main thread (ReturnReset.java:323, :336-338).
- When the 10-minute TTL has expired (ReturnReset.java:309, :324), the call also starts the thread "tj-target-scan", which loads the label of every launchable app. The comment at ReturnReset.java:313-315 puts that at over 100 ms on the MT8168. It runs exactly while the tapped app is starting cold.

**Change:** after `markCustom` (RecentsPolicy.java:29), return early when the switch is off. In effect: `if (!enabled(c)) return;`.

**Risk:** none for the result, which is unused when the switch is off. The only side effect lost is a warm in-memory cache: the next other caller pays one preference read and one `isInstalled` call.

**Verify:** emulator section `legacy`. Logcat should no longer show "target app for ..." lines after app launches.

## A5 LOW: Application.onCreate (48 ms on the tablet) makes binder calls and blocking preference loads on the main thread
**Where:** TjApplication.java:16-34. Specifically:
- **EntranceWatcher.java:45-61:** a Settings.System read (:49), a content observer (:53) and a receiver (:59).
- **BatterySaverGuard.java:38-60:** two observers (:52, :56), a Settings.Secure read (:54), `unstickOnce` (:57) and `check` (:59).
- **First reads that block on each XML load:** CrashGuard.java:112-113 (tj_crash), ProcessLife.java:113-114 (tj_life) and SwitchMigration.java:28 (tj_features, the largest file; its size was not measured).
- **New threads at every start:** AnimationSpeedup.java:181, SettingsSpoof.java:83 (a HandlerThread kept for the life of the process), LockQuiet.java:101 and :136.

**How often:** every start of the main process, about 2 a day, and at boot, on the way to the first home frame.

**Change (keeps every function):**
- (a) At the top of onCreate, call `getSharedPreferences` for tj_crash, tj_life, tj_features and tj_mode without reading. Their loads then run in parallel on Android's own loader threads (AOSP, from memory).
- (b) Register EntranceWatcher and BatterySaverGuard from a worker thread, or post them behind the first frame. Both `start` methods are synchronized and idempotent, and their callbacks keep the main-thread Handler they use now. Make `EntranceWatcher.lastRotation` volatile.
- (c) Give the asynchronous starters one shared worker at THREAD_PRIORITY_BACKGROUND, instead of 4-5 new threads that compete with the first frame for the four A53 cores.

**Risk:** a rotation-lock or charger entrance, or a battery-saver change, in the first fraction of a second after a start is missed. The guard re-reads the state in `check()`.

**Verify:** the AD8 line of the tablet report (Application.onCreate ms, fork-to-end ms) before and after. Emulator sections `se`, `bs` and `an`.

## A6 LOW: the drawer filter copies the whole allowed set for every app
**Where:** AllAppsList.java:77 calls AppFilter.java:18-21, which calls `HiddenApps.isShown` (HiddenApps.java:63-71). That calls `allowed()` (:32-40), which copies the set at :39.
- The preferences handle at :22 is not cached. Each call takes ContextImpl's process-wide lock, unlike Features.java:19-25 and Mode.java:47-53.
- For the auto-allowed music app, every call also makes one `getPackageInfo` binder call (HiddenApps.java:46, :55-57).

**How often:** once per app on every `loadAllApps` (LoaderTask.java:818-822) and every package update. Each `isPackageShown` call pays the same; those come from the Recents sweep, which is slice C.

**Cost:** allocations proportional to apps times allowed apps, on the loader thread, on the way to `bindAllApps`. My estimate is about 200 KB of garbage per load (80 apps, 60 allowed). Small.

**Change:**
- Cache the preferences handle.
- On the read-only path, use `sp.getStringSet(ALLOWED, empty)` directly; only `contains()` is called on it.
- Copy only in the write paths: `setShown`, `onRemoved`, `autoAllowIfDue`.

**Risk:** none, as long as the returned set is never modified (Android's contract).

**Verify:** the AppVisibility JVM tests; emulator sections `mu` and `ss`.

## A7 LOW (hand-off to B): what the hooks start before the first frame and at every drawer open
**Where:** the hooks call `TjAccessibilityService.refreshLayersSoon` (TjAccessibilityService.java:583), which runs `refreshLayers` (:1389-1405): `removePatch` (:1394), and `addPatch` (:1401) with its crop decode in `loadHomeWall` (:1414-1419). The callers are:
- TjHooks.java:87, on resume
- TjHooks.java:163-164 and :169-172, at the start and at the end of every state transition, so 2 calls per drawer open and 2 per drawer close
- TjHooks.java:183, on pause

**Reasoning:**
- (AOSP, from memory) A plain `handler.post` from onResume is queued ahead of the sync barrier that the resumed window's first traversal posts. So `refreshLayers` runs before the Launcher's first frame after Home.
- At the start of every drawer open, the patch window is removed with a WindowManager binder call, on the thread that starts the open animation. The patch is already at alpha 0 from the finger-down (TjHooks.java:121-123), so the removal is not needed for correctness at that moment.
- Every burst of draws on the home screen also ends in a quiet-time curtain capture check: TjHooks.java:68-72 calls `homeDrew` (TjAccessibilityService.java:937-939), which leads to `buildCurtainNow` (:2086-2120), a full-window PixelCopy when its rules allow.

**Suggestions for B:**
- Measure `refreshLayers` / `addPatch` per resume.
- Defer a refresh triggered by a transition start until that transition ends.
- Post resume-time refreshes behind the first frame.

## A8 LOW: notification-dot machinery that cannot work on this Go device
**Where:**
- res/values-v26/bools.xml:20 sets `notification_dots_enabled` to true.
- With it, LauncherAppState.java:105-113 registers a secure-settings observer and asks for a listener rebind when the model is created.
- The manifest enables NotificationListener through the same bool (AndroidManifest.xml:124-132), and Launcher.java:923 does listener bookkeeping on every resume.
- launcher3/go/res/values-v26/bools.xml only sets the obsolete `notification_badging_enabled`.

**Cost:** an observer for the life of the process, 2-3 binder calls at model creation, and the per-resume bookkeeping. All of it is for a listener that a low-RAM device never binds (project finding "no notification listener on Go", STEP 17, CLAUDE.md lessons of v0.8.8).

**Change:** set `notification_dots_enabled` to false in launcher3/go/res/values-v26/bools.xml.

**Risk:** none if dots really cannot appear. Confirm that the stock Go home shows none either.

**Verify:** lint, and `dumpsys package com.tj.launcher` shows the listener disabled.

## A9 LOW (idea; costs RAM, D to judge): placeholders on the first drawer open after a cold start
**Where:** at load time the drawer icons are created with `useLowResIcon=true` (AllAppsList.java:83), which gives the LOW_RES_ICON placeholder (BaseIconCache.java:486-487). The real icon of each visible app is loaded the first time it is bound (AOSP `BubbleTextView.verifyHighRes`, from memory).

**Change:** after `bindAllApps`, preload the high-resolution icons of the first drawer page (about 25-30 icons, about 16 KB each, estimated) on the loader thread at background priority.

Process starts are rare (2 a day), so this is only worth doing if Tj notices the icons popping in.

## Checked and fine (do not re-check)
- `TjHooks.onTouch` (TjHooks.java:119-140): per MotionEvent it only switches on the action; ACTION_DOWN does two in-memory preference reads.
- The draw hook (TjHooks.java:64-76, then TjAccessibilityService.java:937-956): microseconds per frame, with at most one pending curtain post. The follow-up work is B's.
- The wallpaper offset hook (WallpaperOffsetInterpolator.java:240, :273) runs on the interpolator's background looper (:202), with one debounced main-thread post per changed offset (TjAccessibilityService.java:586-592).
- The `Launcher.onNewIntent` patch (Launcher.java:1359-1363) costs one Intent extra lookup.
- The LoaderTask patch (LoaderTask.java:394-395) is a string compare after the AOSP check.
- The PackageUpdatedTask patches (:127, :172) run per uninstall or per removed app. `HiddenApps.onRemoved`'s `commit()` runs on the loader thread (HiddenApps.java:96).
- The filter runs before the icon lookup (AllAppsList.java:77, before :83), so hidden apps cost no icon work.
- Go stubs keep widgets and deep shortcuts out of every load: go/.../WidgetsModel.java:58-61, DeepShortcutManager.java:109-124, LoaderResults.java:35-41, LauncherAppWidgetHost.java:74.
- The UiFactory hooks are empty in this build (src_ui_overrides/.../UiFactory.java:51-63).
- There is no dock and no search bar to draw: Hotseat.java:101, DeviceProfile.java:212, BaseFlags.java:70.
- `FeatureFlags.initialize` skips its disk read on user builds (BaseFlags.java:115-124). Promise apps are off (BaseFlags.java:67; LoaderTask.java:826).
- Wallpaper colours use the platform path on API 29 (WallpaperManagerCompat.java:35-37), so the `:wallpaper_chooser` service never starts. The picker process starts only when it is opened (AndroidManifest.xml:109-116).
- The router per Home press, apart from A3:
  - `ProfileSession.checkBoot` (ProfileSession.java:109-113) costs a cached Settings.Global read and one preference read.
  - `DemoModeProfile.cleanupOnce` (DemoModeProfile.java:43-45) costs one preference read.
  - `KeepAliveService.ensure` (KeepAliveService.java:59-66) costs in-memory reads while the service is off.
  - `LockLog.add` is queued to its own thread (LockLog.java:56-65).
- The HiddenApps baseline scan runs once per install (HiddenApps.java:34-37).
- `SettingsSpoof.syncNow` runs off the main thread and calls the package manager only on a real change (SettingsSpoof.java:102-110). Only the reload it triggers is a cost (A1).
- The AnimationSpeedup and LockQuiet start-up work is already asynchronous (AnimationSpeedup.java:178-189; LockQuiet.java:99-102, :134-147). Only the thread count is noted in A5.
- The start-up auto-open does nothing when clean start is on (ReturnReset.java:80; TjHooks.java:91-98).
