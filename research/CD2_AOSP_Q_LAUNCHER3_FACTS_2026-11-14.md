# AOSP Android 10 Launcher3 / Quickstep facts for the "why is Quickstep big" question (CD2c, 2026-11-14)

Source: `platform/packages/apps/Launcher3`, ref `refs/heads/android10-release` (every file resolved on this ref), read by the web-fetch agent (gitiles pages, no line numbers).
**Caveat:** this is plain AOSP Q. MediaTek's Go build (`MtkLauncher3GoIconRecents`) is not public; its manifest and overlays may differ. Items marked NOT READ were not read.
Quotes are the agent's verbatim excerpts (short). Treat as data.

## Q1 Manifests
- **Launcher** (`AndroidManifest.xml`): `launchMode="singleTask"`, `taskAffinity=""`, `stateNotNeeded="true"`, `clearTaskOnLaunch="true"`, `resizeableActivity="true"`.
  `configChanges` = `keyboard|keyboardHidden|mcc|mnc|navigation|orientation|screenSize|screenLayout|smallestScreenSize`.
  **uiMode, fontScale and density are NOT in the list**, so the system relaunches (recreates) the activity on those configuration changes.
  Categories HOME, DEFAULT, MONKEY, LAUNCHER_APP. No `process`, `persistent`, `excludeFromRecents`.
- **RecentsActivity** (`quickstep/AndroidManifest.xml`): `exported="true"`, `excludeFromRecents="true"`, `launchMode="singleTask"`, `stateNotNeeded="true"`, `taskAffinity=""`, `clearTaskOnLaunch="true"`, same `configChanges` list. No intent filter.
- **TouchInteractionService**: `permission="android.permission.STATUS_BAR_SERVICE"`, `directBootAware="true"`, action `android.intent.action.QUICKSTEP_SERVICE`. No `process`, no `persistent`.
- NOT READ: `quickstep/AndroidManifest-launcher.xml` (404). `go/quickstep/` has no manifest; `go/AndroidManifest.xml` was read.

## Q2 OverviewComponentObserver (`quickstep/src/com/android/quickstep/`)
- Default home is ANOTHER package (our case): `overviewComponent = new ComponentName(mContext, RecentsActivity.class);`, `mActivityControlHelper = new FallbackActivityControllerHelper();`, `mIsHomeAndOverviewSame = false;`.
- Two receivers, both end in `updateOverviewTargets()`: (1) `ACTION_PREFERRED_ACTIVITY_CHANGED` (default home changed), (2) registered only in the fallback path: `ACTION_PACKAGE_ADDED/CHANGED/REMOVED` for the default HOME package (= `com.tj.launcher`).
  So **every package-changed broadcast of OUR package (the Settings alias flip, an app update) makes the stock observer re-run `updateOverviewTargets()`** (it recomputes the overview component and rebuilds the small controller helper objects; no cache, no list).
- `onDestroy()` unregisters both receivers.

## Q3 TouchInteractionService (read from `quickstep/recents_ui_overrides/src/...`)
- `onCreate`: `initWhenUserUnlocked()` if unlocked, else waits for `ACTION_USER_UNLOCKED`. `initWhenUserUnlocked` creates `RecentsModel`, `OverviewComponentObserver`, `OverviewCommandHelper`, `OverviewInteractionState`, `OverviewCallbacks`, `TaskOverlayFactory`, registers the input consumer.
- `onBind` returns the `IOverviewProxy` stub (`return mMyBinder;`), `onInitialize` -> `initInputMonitor()`.
- `onDestroy`: unregisters the input consumer, `mOverviewComponentObserver.onDestroy();`, disposes the input monitor, removes display / mode listeners.
- Nothing there reacts to `ACTION_PACKAGE_CHANGED`; the overview component is read from the observer on demand.

## Q4 RecentsActivity (read from `quickstep/recents_ui_overrides/...`)
- Overrides `initViews`, `onStart`, `onHandleConfigChanged`, `reapplyUi`, `onTaskLaunched`. `onStart`: `mFallbackRecentsView.setContentAlpha(1); super.onStart(); mFallbackRecentsView.resetTaskVisuals();`. No `finish()` on stop, no uiMode handling here.
- NOT READ: `onCreate`, `onStop`, `onDestroy`, `onNewIntent`, `onConfigurationChanged` (they live in `BaseRecentsActivity`).

## Q5 Launcher.java
- `onConfigurationChanged`: reacts to `CONFIG_LOCALE` (folder fields) and `CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE` (`onIdpChanged(mDeviceProfile.inv)`). **Not to fontScale, density or uiMode** (they are not in `configChanges`, so the system recreates the activity for them).
- `onNewIntent` (`ACTION_MAIN`): `if (!isInState(NORMAL)) mStateManager.goToState(NORMAL);`, then the workspace goes to the default screen when appropriate.
- `onStop`: `getStateManager().moveToRestState(); onTrimMemory(TRIM_MEMORY_UI_HIDDEN);` ("Workaround for b/78520668, explicitly trim memory once UI is hidden").
- `onTrimMemory`: `if (level >= TRIM_MEMORY_UI_HIDDEN) SQLiteDatabase.releaseMemory(); UiFactory.onTrimMemory(this, level);`
- `onDestroy`: unregisters the screen-off receiver, `mModel.stopLoader()`, `LauncherAppState.getInstance(this).setLauncher(null);`.
- `recreate()`: none in the portions read. NOT READ: the file's tail after `onProvideKeyboardShortcuts`.

## Q6 LauncherModel.onPackageChanged / PackageUpdatedTask
- `onPackageChanged` enqueues `PackageUpdatedTask(OP_UPDATE)` for ANY package: no check against the launcher's own package, **no check of `mModelLoaded`**.
- `OP_UPDATE`: `iconCache.updateIconsForPkg(...)`, `appsList.updatePackage(...)`, `app.getWidgetCache().removePackage(...)`; rewrites workspace rows that belong to the package (`updateItemInDatabase`); `bindAppsAddedOrUpdated`.
  So each alias flip makes the stock Launcher3 re-decode the icons of `com.tj.launcher` and rewrite its icon-cache rows (a few icons; same key every time = replaced, not added).

## Q7 ConfigMonitor / InvariantDeviceProfile / flags
- `ConfigMonitor.onReceive`: `if (mFontScale != config.fontScale || mDensity != config.densityDpi) {`. Locale and uiMode are not checked.
- `APPLY_CONFIG_AT_RUNTIME` default **true** (`config/BaseFlags.java`): IDP uses `onConfigChanged` (in-process `initGrid`, listeners notified), **not** `killProcess`. (Caveat: `config/FeatureFlags.java` returned 404, so a redeclaration is not ruled out.)

## Q8 Recents caches (Go)
- `recentsIconCacheSize` = **12** entries, `recentsThumbnailCacheSize` = **3** entries (`quickstep/res/values/config.xml`; `go/quickstep/res/values/override.xml` does not override them). Entry counts, no byte budget.
- `RecentsModel.onTrimMemory`: at `TRIM_MEMORY_RUNNING_CRITICAL` clears both caches. `TaskIconCache.clear()` = `evictAll()`.
- Go recents DOES load thumbnails per visible row (`mLoader.loadTaskThumbnail(...)` in `TaskAdapter.onBindViewHolderInternal`), reduced resolution unless high-res loading is on (`reducedResolution = !mHighResLoadingState.isEnabled();`).
- NOT READ: `TaskActionController.java`, `TaskHolder.java`, `ThumbnailDrawable.java`, the Go `RecentsActivity.java`.

## What this means for the question (reasoned, not measured)
1. Everything the stock side keeps for Recents is bounded by design (12 icons + 3 reduced-resolution thumbnails; trimmed when the UI is hidden). That cannot add up to hundreds of MB by itself.
2. A uiMode / font change relaunches the Launcher activity (singleTask, not in `configChanges`): a transient rebuild of one view tree, not an accumulation (section cd3n looks for any growth anyway).
3. The package-changed path (our alias flips) costs the stock process a few icon decodes and a controller rebuild per flip.
4. None of this applies to MediaTek's own Go changes, to graphics (GPU) memory, or to a process that lives for days: only the tablet's `dumpsys meminfo` can show those.
