# CG1F: the AOSP android10-release angle on "why would the stock Quickstep process keep growing" (2026-11-17)

Question: on Tj's MT8168 Android 10 Go tablet the stock `com.android.launcher3` process ("Quickstep", MediaTek `MtkLauncher3GoIconRecents`) shows 414-450 MB in "Memory used by apps" (1-day average 445, 3-hour 450, max 427 plateau); the same tablet model with the stock launcher as default home shows about 48 MB; after several force-stops it fell to about 90 MB and is slowly building up again. Emulator runs (plain AOSP, software GL, non-Go Launcher3) never grew (`CD1`, `CD2`, `CD2_EMULATOR_GROWTH`).
This report asks a different question than `CD2_QUICKSTEP_RAM_AUDIT`: not "which of OUR features touch the stock process" but "what in AOSP itself lets a stock launcher process that is NEVER cached and is only a fallback Recents host / started HOME task grow with the number of Recents opens, stock-home starts and configuration changes".

## 0. How to read this (sources, numbering, limits)

- Source: `android.googlesource.com`, `platform/packages/apps/Launcher3` and `platform/frameworks/base`, ref `refs/heads/android10-release` (every file below was fetched on that ref on 2026-11-17 through the WebFetch tool; GitHub mirrors, aospxref, androidxref and gitiles `+log` / `+blame` were NOT reachable from this session).
- **Line numbers are approximate.** WebFetch returns markdown without a line gutter and inserts an extra blank line after every import line, so every `L` below was counted from a transcription: `L=` = from a full-file listing (about +-3 lines), `L~` = estimated by the fetch model (+-10..60; locate by the method name, which is exact). Every quoted block carries file, ref and method name; quotes are verbatim except for indentation (the page drops it) and where marked `[...]`.
- **VERIFIED** = the code is quoted here. **INFERRED** = reasoning from the quoted code. Nothing here is MEASURED on the tablet.
- This is plain AOSP Q. MediaTek's Go build (`MtkLauncher3GoIconRecents`) is not public and may differ. The CI emulator image (`target: default`) is NOT a Go build: it runs the non-Go classes (`quickstep/recents_ui_overrides/...`, `RecentsView`), not the Go classes of section 1. **Nothing in section 1.4-1.6 has ever been exercised by an emulator run of this project.**

## 1. Q1: the fallback RecentsActivity path (Go build)

### 1.1 The earlier audit read the WRONG classes for a Go device (VERIFIED, directory listings)
`CD2_AOSP_Q_LAUNCHER3_FACTS` Q3/Q4 read `quickstep/recents_ui_overrides/.../RecentsActivity.java` and `TouchInteractionService.java`. In the Go build those two classes are REPLACED by Go versions in
`go/quickstep/src/com/android/quickstep/` (listing, branch android10-release):
```
AppToOverviewAnimationProvider.java  ClearAllHolder.java  ContentFillItemAnimator.java
FallbackActivityControllerHelper.java  FallbackRecentsToActivityHelper.java  GoActivityControlHelper.java
IconRecentsFragment.java  LauncherActivityControllerHelper.java  OverviewCommandHelper.java
RecentsActivity.java  RecentsToActivityHelper.java  TaskActionController.java  TaskAdapter.java
TaskHolder.java  TaskListLoader.java  TaskSwipeCallback.java  ThumbnailDrawable.java
TouchInteractionService.java            fallback/GoRecentsActivityRootView.java
views/IconRecentsView.java  views/TaskItemView.java  views/TaskLayerDrawable.java  views/TaskThumbnailIconView.java
```
plus `go/quickstep/src/com/android/launcher3/uioverrides/RecentsUiFactory.java` (`GO_LOW_RAM_RECENTS_ENABLED = true`) and the layouts `go/quickstep/res/layout/{clear_all_button,fallback_recents_activity,icon_recents_root_view,overview_panel,task_item_view}.xml`. The shared classes (`OverviewComponentObserver`, `BaseRecentsActivity`, `RecentsModel`, `RecentTasksList`, `TaskIconCache`, `TaskThumbnailCache`, `RecentsActivityTracker`) are in `quickstep/src/com/android/quickstep/`.

### 1.2 What decides "fallback": `OverviewComponentObserver` (VERIFIED)
`quickstep/src/com/android/quickstep/OverviewComponentObserver.java`, android10-release (L= 76-87 constructor, 102-155 `updateOverviewTargets` in the fetch listing; real file about 10 lines lower because of the import artefact):
```java
    public OverviewComponentObserver(Context context) {
        mContext = context;
        Intent myHomeIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(mContext.getPackageName());
        ResolveInfo info = context.getPackageManager().resolveActivity(myHomeIntent, 0);
        mMyHomeComponent = new ComponentName(context.getPackageName(), info.activityInfo.name);

        mContext.registerReceiver(mUserPreferenceChangeReceiver,
                new IntentFilter(ACTION_PREFERRED_ACTIVITY_CHANGED));
        updateOverviewTargets();
    }
    ...
    private void updateOverviewTargets() {
        ComponentName defaultHome = PackageManagerWrapper.getInstance()
                .getHomeActivities(new ArrayList<>());
        ...
        if ((mSystemUiStateFlags & SYSUI_STATE_HOME_DISABLED) == 0 &&
                (defaultHome == null || mMyHomeComponent.equals(defaultHome))) {
            // User default home is same as out home app. Use Overview integrated in Launcher.
            overviewComponent = mMyHomeComponent;
            mActivityControlHelper = new LauncherActivityControllerHelper();
            mIsHomeAndOverviewSame = true;
            overviewIntentCategory = Intent.CATEGORY_HOME;
            ...
        } else {
            // The default home app is a different launcher. Use the fallback Overview instead.
            overviewComponent = new ComponentName(mContext, RecentsActivity.class);
            mActivityControlHelper = new FallbackActivityControllerHelper();
            mIsHomeAndOverviewSame = false;
            overviewIntentCategory = Intent.CATEGORY_DEFAULT;
            mHomeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(defaultHome);
            ...
            } else if (!defaultHome.getPackageName().equals(mUpdateRegisteredPackage)) {
                ...
                mUpdateRegisteredPackage = defaultHome.getPackageName();
                mContext.registerReceiver(mOtherHomeAppUpdateReceiver, getPackageFilter(
                        mUpdateRegisteredPackage, ACTION_PACKAGE_ADDED, ACTION_PACKAGE_CHANGED,
                        ACTION_PACKAGE_REMOVED));
            }
        }
        mOverviewIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(overviewIntentCategory)
                .setComponent(overviewComponent)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
```
Meaning (VERIFIED + INFERRED): with `com.tj.launcher` as default home, `defaultHome != mMyHomeComponent`, so Recents is NOT the in-Launcher overview but the separate fallback `RecentsActivity` (Go class: `go/quickstep/.../RecentsActivity.java`, section 1.4). The two receivers are registered/unregistered correctly (the old receiver is unregistered before a new one is registered; `onDestroy` unregisters both: L= 160-166), so the observer itself does not leak. Each package-changed broadcast of the default home package re-runs `updateOverviewTargets()` (allocates 2 intents + a helper object; no cache).

### 1.3 The Go `TouchInteractionService` (the service SystemUI binds) (VERIFIED)
`go/quickstep/src/com/android/quickstep/TouchInteractionService.java` (about 188 real lines; the listing's numbers minus 16):
```java
    private static boolean sConnected = false;;
    ...
    @Override
    public void onCreate() {
        super.onCreate();
        if (UserManagerCompat.getInstance(this).isUserUnlocked(Process.myUserHandle())) {
            initWhenUserUnlocked();
        } else {
            mIsUserUnlocked = false;
            registerReceiver(mUserUnlockedReceiver, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
        }
        sConnected = true;
    }

    private void initWhenUserUnlocked() {
        mRecentsModel = RecentsModel.INSTANCE.get(this);
        mOverviewComponentObserver = new OverviewComponentObserver(this);
        mOverviewCommandHelper = new OverviewCommandHelper(this,
                mOverviewComponentObserver);
        mIsUserUnlocked = true;
        Utilities.unregisterReceiverSafely(this, mUserUnlockedReceiver);
    }

    @Override
    public void onDestroy() {
        if (mIsUserUnlocked) {
            mOverviewComponentObserver.onDestroy();
        }
        Utilities.unregisterReceiverSafely(this, mUserUnlockedReceiver);
        sConnected = false;
        super.onDestroy();
    }
```
and in the `IOverviewProxy.Stub`: `onInitialize(Bundle)` -> `mRecentsModel.setSystemUiProxy(iSystemUiProxy)`; `onOverviewToggle/Shown/Hidden/onTip` -> `mOverviewCommandHelper...` only `if (mIsUserUnlocked)`; `onAssistantAvailable`, `onAssistantVisibilityChanged`, `onBackAction`, `onSystemUiStateChanged` are empty ("To be implemented"). Note `onSystemUiStateChanged` is EMPTY here, so `OverviewComponentObserver.onSystemUiStateChanged` is never fed in the Go build.
Meaning: the service creates `RecentsModel` (singleton, section 1.5) and the observer once per service instance. The process-wide singletons are created at the first bind and stay for the life of the process (INFERRED from `MainThreadInitializedObject`).

### 1.4 The Go fallback `RecentsActivity` and its layouts (VERIFIED)
`go/quickstep/src/com/android/quickstep/RecentsActivity.java` (about 95 lines; the only lifecycle override is `onResume`; there is NO `onDestroy` / `onDetachedFromWindow` / cleanup of any kind):
```java
public final class RecentsActivity extends BaseRecentsActivity {
    private GoRecentsActivityRootView mRecentsRootView;
    private IconRecentsView mIconRecentsView;

    @Override
    protected void initViews() {
        setContentView(R.layout.fallback_recents_activity);
        mRecentsRootView = findViewById(R.id.drag_layer);
        mIconRecentsView = findViewById(R.id.overview_panel);
        mIconRecentsView.setRecentsToActivityHelper(new FallbackRecentsToActivityHelper(this));
        mIconRecentsView.setShowStatusBarForegroundScrim(true);
    }
    ...
    @Override
    protected void onResume() {
        mIconRecentsView.onBeginTransitionToOverview();
        super.onResume();
    }
}
```
`go/quickstep/res/layout/fallback_recents_activity.xml`: root `com.android.quickstep.fallback.GoRecentsActivityRootView` (`@+id/drag_layer`) with `<include android:id="@+id/overview_panel" layout="@layout/overview_panel" .../>`.
`go/quickstep/res/layout/overview_panel.xml` (whole file):
```xml
<fragment android:name="com.android.quickstep.IconRecentsFragment"
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/low_ram_recents_fragment"
    android:layout_width="match_parent"
    android:layout_height="match_parent"/>
```
`IconRecentsFragment.onCreateView`: `return inflater.inflate(R.layout.icon_recents_root_view, container, false);` and `icon_recents_root_view.xml` has the root element `<com.android.quickstep.views.IconRecentsView ...>` with a `RecyclerView` (`recent_task_recycler_view`) and an empty-state `TextView`.
So **every creation of the fallback `RecentsActivity` inflates one new `IconRecentsView`**. (INFERRED: the Go `Launcher` gets the same panel, because `res/layout/launcher.xml` includes `@layout/overview_panel` and the Go resource overrides it; not fetched. If true, every creation of the stock `Launcher` activity inflates one too.)

`BaseRecentsActivity` (`quickstep/src/com/android/quickstep/BaseRecentsActivity.java`, L= 45-148, 157 lines):
```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOldConfig = new Configuration(getResources().getConfiguration());
        initDeviceProfile();
        initViews();
        getSystemUiController().updateUiState(SystemUiController.UI_STATE_BASE_WINDOW,
                Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText));
        RecentsActivityTracker.onRecentsActivityCreate(this);
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        int diff = newConfig.diff(mOldConfig);
        if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) {
            onHandleConfigChanged();
        }
        mOldConfig.setTo(newConfig);
        super.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Workaround for b/78520668, explicitly trim memory once UI is hidden
        onTrimMemory(TRIM_MEMORY_UI_HIDDEN);
    }
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        UiFactory.onTrimMemory(this, level);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        RecentsActivityTracker.onRecentsActivityNewIntent(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        RecentsActivityTracker.onRecentsActivityDestroy(this);
    }
    @Override
    public void onBackPressed() {
        // TODO: Launch the task we came from
        startHome();
    }
    public void startHome() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
```
`RecentsActivityTracker` (`quickstep/src/com/android/quickstep/RecentsActivityTracker.java`, L= 33-131): `sCurrentActivity` is a `WeakReference<BaseRecentsActivity>`, cleared in `onRecentsActivityDestroy`; the pending tracker is also a `WeakReference`. No leak there (VERIFIED).
Note: `BaseRecentsActivity.onStop` calls `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` on the ACTIVITY object only, which reaches `UiFactory.onTrimMemory` -> `RecentsModel.onTrimMemory` (section 1.5). It does not go through `ActivityThread.handleTrimMemory`, so it does not reach `WindowManagerGlobal.trimMemory` / HWUI (see 3.x).

### 1.5 KEY FINDING: the Go `IconRecentsView` registers a listener in a process-wide singleton and nothing removes it (VERIFIED code; magnitude INFERRED)
`go/quickstep/src/com/android/quickstep/views/IconRecentsView.java` (about 900 lines; constructor L~221-259, `onFinishInflate` L~262-350; two counts of the fetch model disagree by 60 lines, locate by name):
```java
    private final RecentsModel.TaskThumbnailChangeListener listener = (taskId, thumbnailData) -> {
        ArrayList<TaskItemView> itemViews = getTaskViews();
        for (int i = 0, size = itemViews.size(); i < size; i++) {
            TaskItemView taskView = itemViews.get(i);
            TaskHolder taskHolder = (TaskHolder) mTaskRecyclerView.getChildViewHolder(taskView);
            Optional<Task> optTask = taskHolder.getTask();

            if (optTask.filter(task -> task.key.id == taskId).isPresent()) {
                Task task = optTask.get();

                // Update thumbnail on the task.
                task.thumbnail = thumbnailData;
                taskView.setThumbnail(thumbnailData);
                return task;
            }
        }
        return null;
    };

    public IconRecentsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActivity = BaseActivity.fromContext(context);
        mContext = context;

        mStatusBarForegroundScrim =
                Themes.getAttrDrawable(mContext, R.attr.workspaceStatusBarScrim);

        mTaskLoader = new TaskListLoader(mContext);
        mTaskAdapter = new TaskAdapter(mTaskLoader);
        mTaskAdapter.setOnClearAllClickListener(view -> animateClearAllTasks());
        mTaskActionController = new TaskActionController(mTaskLoader, mTaskAdapter,
                mActivity.getStatsLogManager());
        mTaskAdapter.setActionController(mTaskActionController);

        mTaskLayoutManager = new LinearLayoutManager(mContext, VERTICAL, true /* reverseLayout */);

        RecentsModel.INSTANCE.get(context).addThumbnailChangeListener(listener);
    }
```
Property check by the fetch tool over the whole file (string search, not a line read): `removeThumbnailChangeListener` = no, `onDetachedFromWindow` = no, `onAttachedToWindow` = no (only the RecyclerView child callbacks `onChildViewAttachedToWindow` / `onChildViewDetachedFromWindow`, the latter empty: L~291 `public void onChildViewDetachedFromWindow(@NonNull View view) { }`).
`RecentsModel` (`quickstep/src/com/android/quickstep/RecentsModel.java`, ref android10-release, 239 lines):
```java
public class RecentsModel extends TaskStackChangeListener {                       // L~69
    // We do not need any synchronization for this variable as its only written on UI thread.
    public static final MainThreadInitializedObject<RecentsModel> INSTANCE =
            new MainThreadInitializedObject<>(c -> new RecentsModel(c));          // L~74
    private final List<TaskThumbnailChangeListener> mThumbnailChangeListeners = new ArrayList<>();   // L~77
    ...
    private RecentsModel(Context context) {                                       // L~89
        mContext = context;
        HandlerThread loaderThread = new HandlerThread("TaskThumbnailIconCache",
                Process.THREAD_PRIORITY_BACKGROUND);
        loaderThread.start();
        mTaskList = new RecentTasksList(context);
        mIconCache = new TaskIconCache(context, loaderThread.getLooper());
        mThumbnailCache = new TaskThumbnailCache(context, loaderThread.getLooper());
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this);
    }
    @Override
    public void onTaskSnapshotChanged(int taskId, ThumbnailData snapshot) {        // L~179
        mThumbnailCache.updateTaskSnapShot(taskId, snapshot);
        for (int i = mThumbnailChangeListeners.size() - 1; i >= 0; i--) {
            Task task = mThumbnailChangeListeners.get(i).onTaskThumbnailChanged(taskId, snapshot);
            if (task != null) {
                task.thumbnail = snapshot;
            }
        }
    }
    public void addThumbnailChangeListener(TaskThumbnailChangeListener listener) {  // L~227
        mThumbnailChangeListeners.add(listener);
    }
    public void removeThumbnailChangeListener(TaskThumbnailChangeListener listener) {
        mThumbnailChangeListeners.remove(listener);
    }
```
**What this means (INFERRED from the verified code):** `RecentsModel.INSTANCE` is a process singleton; its `ArrayList` holds a STRONG reference to every `IconRecentsView.listener` lambda; the lambda captures `this` (it calls the instance method `getTaskViews()` and reads `mTaskRecyclerView`); `IconRecentsView` holds `mActivity` / `mContext` (the Activity), the adapter, the `TaskListLoader` (with its `Task` list: icons and thumbnails) and, through the RecyclerView, up to 7 `TaskItemView`s with their `ThumbnailDrawable`s (BitmapShader on `ThumbnailData.thumbnail`). **Each creation of an `IconRecentsView` therefore pins the whole `Activity` (view tree, adapter, task icons, task thumbnails) for the life of the process.** The leak is per ACTIVITY CREATION (fallback `RecentsActivity`, and the Go `Launcher` if section 1.4's inference holds), not per Recents open: a stopped, resumed activity is created once. It grows with every RELAUNCH (configuration change not in `configChanges`: uiMode, fontScale, density, locale) and every process-internal re-creation. Plain AOSP (the emulator image) uses `RecentsView`, a different class (INFERRED, not fetched: it registers in `onAttachedToWindow` and removes in `onDetachedFromWindow`), which is why 80 font-scale relaunches on the CI image (cd3u: Activities 2, Views flat) cannot show it.
Each leaked view is also KEPT UP TO DATE: `onTaskSnapshotChanged` calls every listener for every snapshot change, and a leaked view whose adapter holds that task id receives the new `ThumbnailData` (`taskView.setThumbnail`), so the leaked instances keep pinning recent snapshot Bitmaps (hardware bitmaps over a `GraphicBuffer`, i.e. gralloc memory outside the Java heap, INFERRED from `ThumbnailData` which was not fetched).
Not a Go-only coincidence: the registration is in the constructor, the removal does not exist anywhere in the Go sources that were read (`IconRecentsView`, `RecentsActivity`, `BaseRecentsActivity`, `FallbackRecentsToActivityHelper`, `IconRecentsFragment`).

### 1.6 The bounded caches (VERIFIED), and why they are NOT the answer
- `RecentsModel.onTrimMemory` (L~203-212): `if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) { mThumbnailCache.getHighResLoadingState().setVisible(false); }` and `if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) { mThumbnailCache.clear(); mIconCache.clear(); }` (equality tests, not `>=`: BACKGROUND / MODERATE / COMPLETE clear nothing).
- `TaskIconCache` (L= 47-79): `TaskKeyLruCache<Drawable> mIconCache`, `TaskKeyLruCache<String> mContentDescriptionCache`, `LruCache<ComponentName, ActivityInfo> mActivityInfoCache`, all sized `res.getInteger(R.integer.recentsIconCacheSize)` (12, `CD2_AOSP_Q_LAUNCHER3_FACTS` Q8); `clear()` = `evictAll()`.
- `TaskThumbnailCache` (listing-relative): `mCache = new ThumbnailCache(mCacheSize)` (3 entries, `recentsThumbnailCacheSize`); `HighResLoadingState` constructor: `mIsLowRamDevice = activityManager.isLowRamDevice();` `updateState()`: `mHighResLoadingEnabled = !mIsLowRamDevice && mVisible && !mFlingingFast;` `isPreloadingEnabled()`: `return !mHighResLoadingState.mIsLowRamDevice && mHighResLoadingState.mVisible;` => on a Go (low-RAM) device thumbnails are ALWAYS reduced resolution and there is NO background preloading (`RecentsModel.onTaskStackChangedBackground` returns at once).
- `TaskAdapter` (L= 41-181): `public static final int MAX_TASKS_TO_DISPLAY = 6;` `getItemCount()` = 1 + `Math.min(mLoader.getCurrentTaskList().size(), MAX_TASKS_TO_DISPLAY)`; `onBindViewHolderInternal` binds each visible row and calls `mLoader.loadTaskIconAndLabel(task, ...)` and `mLoader.loadTaskThumbnail(task, ...)`.
- `TaskListLoader` (L= 35-143): `private ArrayList<Task> mTaskList`; `loadTaskList` rebuilds the list from `RecentsModel.getTasks` and copies content from the old list: `task.icon = loadedTask.icon; task.titleDescription = loadedTask.titleDescription; task.thumbnail = loadedTask.thumbnail;` (so a `Task` keeps its thumbnail after it leaves the 3-entry cache, as long as it is in the list); `removeTask` / `clearAllTasks` mutate the list.
- `RecentTasksList.getTasks` (listing-relative L~50-73): `loadTasksInBackground(Integer.MAX_VALUE, loadKeysOnly)` (ALL recent tasks, keys only for those not displayed), result replaces `mTasks`; `onTaskRemoved` removes by id. No growth.
- `TaskActionController` (Go): `removeTask` -> `ActivityManagerWrapper.getInstance().removeTask(task.key.id)`; `clearAllTasks` -> `removeAllRecentTasks()`.
Conclusion: every bounded structure is small (12 icons, 3 thumbnails, <= 6 rows). The unbounded structure is the listener list of 1.5.

### 1.7 What a Recents open does in the Go fallback path (VERIFIED, `go/.../OverviewCommandHelper.java` L= 52-177)
`onOverviewToggle()` -> `mMainThreadExecutor.execute(new RecentsActivityCommand<>())`; the command constructor does `mRecentsModel.getTasks(null)` ("Preload the plan"); `run()` -> `mHelper.switchToRecentsIfVisible(null)` (the Go `FallbackActivityControllerHelper` returns `false`) -> `mListener = mHelper.createActivityInitListener(provider::onActivityReady); mListener.registerAndStartActivity(mOverviewComponentObserver.getOverviewIntent(), provider, mContext, mMainThreadExecutor.getHandler(), provider.getRecentsLaunchDuration());` i.e. `RecentsActivityTracker.register()` + `context.startActivity(intent, options)` where the intent is `ACTION_MAIN` + `CATEGORY_DEFAULT` + component `RecentsActivity` + `FLAG_ACTIVITY_NEW_TASK` (1.2). `mListener.unregister()` runs only in `onWindowAnimationCreated`; the tracker holds only weak references, so a missed unregister leaks nothing. The activity is `singleTask` + `stateNotNeeded` + `excludeFromRecents` (`CD2_AOSP_Q_LAUNCHER3_FACTS` Q1): the second and later opens deliver `onNewIntent` + `onResume` to the SAME instance; a new instance (a new leaked `IconRecentsView`) appears only when the system recreates it.

## 2. Q2: what keeps the process from being trimmed (oom_adj, home process, trim levels, ART)

Fetch limit worth knowing: the fetch tool delivers only the first 86,267 characters of a page, so `ActivityManagerService.java`, `ActivityThread.java` (after `scheduleTrimMemory`) and `Activity.java` could NOT be read past their first ~2000 lines. Statements that depend on those bodies are marked INFERRED.

### 2.1 SystemUI binds the Launcher3 service for ever (VERIFIED)
`frameworks/base/packages/SystemUI/src/com/android/systemui/recents/OverviewProxyService.java`, android10-release, `internalConnectToCurrentUser()`:
```java
mBound = mContext.bindServiceAsUser(launcherServiceIntent,
mOverviewServiceConnection,
Context.BIND_AUTO_CREATE | Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE,
UserHandle.of(mDeviceProvisionedController.getCurrentUser()));
...
private static final long BACKOFF_MILLIS = 1000;
private static final long MAX_BACKOFF_MILLIS = 10 * 60 * 1000;
private static final long DEFERRED_CALLBACK_MILLIS = 5000;
```
(the intent is `ACTION_QUICKSTEP` with the package of `config_recentsComponentName`, i.e. `com.android.launcher3`, whatever the default home is; failures retry with exponential backoff up to 10 minutes; summary of the rest from the fetch tool, quote refused). So on the friend's tablet AND on Tj's tablet the stock process hosts a service bound by the persistent SystemUI. This is NOT the structural difference; the difference is what runs inside that process (section 1).

### 2.2 What the binding does to the process class (VERIFIED code; the arithmetic is INFERRED)
`frameworks/base/services/core/java/com/android/server/am/OomAdjuster.java`, android10-release, `computeOomAdjLocked`, service-connection loop (about 1/3 into the file; listing numbers are restarted at 072 by the fetch):
```java
    ProcessRecord client = cr.binding.client;
    computeOomAdjLocked(client, cachedAdj, TOP_APP, doingAll, now, cycleReEval);
    ...
    int clientAdj = client.getCurRawAdj();
    int clientProcState = client.getCurRawProcState();
    if (clientProcState >= PROCESS_STATE_CACHED_ACTIVITY) {
        // If the other app is cached for any reason, for purposes here
        // we are going to consider it empty. The specific cached state
        // doesn't propagate except under certain conditions.
        clientProcState = PROCESS_STATE_CACHED_EMPTY;
    }
    ...
    if (adj > clientAdj) {
        // If this process has recently shown UI, and
        // the process that is binding to it is less
        // important than being visible, then we don't
        // care about the binding as much as we care
        // about letting this process get into the LRU
        // list to be killed and restarted if needed for
        // memory.
        if (app.hasShownUi && !wpc.isHomeProcess()
                && clientAdj > ProcessList.PERCEPTIBLE_APP_ADJ) {
            if (adj >= ProcessList.CACHED_APP_MIN_ADJ) {
                adjType = "cch-bound-ui-services";
            }
        } else {
            int newAdj;
            if ((cr.flags&(Context.BIND_ABOVE_CLIENT
                    |Context.BIND_IMPORTANT)) != 0) {
                ...
            } else if ((cr.flags & Context.BIND_NOT_PERCEPTIBLE) != 0
                    && clientAdj < ProcessList.PERCEPTIBLE_APP_ADJ
                    && adj > ProcessList.PERCEPTIBLE_LOW_APP_ADJ) {
                newAdj = ProcessList.PERCEPTIBLE_LOW_APP_ADJ;
            } else if ((cr.flags&Context.BIND_NOT_VISIBLE) != 0
                    && clientAdj < ProcessList.PERCEPTIBLE_APP_ADJ
                    && adj > ProcessList.PERCEPTIBLE_APP_ADJ) {
                newAdj = ProcessList.PERCEPTIBLE_APP_ADJ;
            } else if (clientAdj >= ProcessList.PERCEPTIBLE_APP_ADJ) {
                newAdj = clientAdj;
            } else {
                if (adj > ProcessList.VISIBLE_APP_ADJ) {
                    // TODO: Is this too limiting for apps bound from TOP?
                    newAdj = Math.max(clientAdj, ProcessList.VISIBLE_APP_ADJ);
                } else {
                    newAdj = adj;
                }
            }
            if (!client.cached) {
                app.cached = false;
            }
            if (adj > newAdj) {
                adj = newAdj;
                app.setCurRawAdj(adj);
                adjType = "service";
            }
        }
    }
    ...
    if ((cr.flags & (Context.BIND_NOT_FOREGROUND
            | Context.BIND_IMPORTANT_BACKGROUND)) == 0) {
        ...
        if (clientProcState < PROCESS_STATE_TOP) {
            // Special handling for above-top states (persistent
            // processes). These should not bring the current process
            // into the top state, since they are not on top. Instead
            // give them the best bound state after that.
            final int bestState = cr.hasFlag(Context.BIND_INCLUDE_CAPABILITIES)
                    ? PROCESS_STATE_FOREGROUND_SERVICE_LOCATION
                    : PROCESS_STATE_BOUND_FOREGROUND_SERVICE;
            if ((cr.flags & Context.BIND_FOREGROUND_SERVICE) != 0) {
                clientProcState = bestState;
            } else if (mService.mWakefulness
                    == PowerManagerInternal.WAKEFULNESS_AWAKE
                    && (cr.flags & Context.BIND_FOREGROUND_SERVICE_WHILE_AWAKE)
                    != 0) {
                clientProcState = bestState;
            } else {
                clientProcState =
                    PROCESS_STATE_IMPORTANT_FOREGROUND;
            }
        } else if ...
```
`ProcessList.java` constants (android10-release, verbatim): `PERSISTENT_PROC_ADJ = -800`, `SYSTEM_ADJ = -900`, `PERSISTENT_SERVICE_ADJ = -700`, `FOREGROUND_APP_ADJ = 0`, `VISIBLE_APP_ADJ = 100`, `PERCEPTIBLE_APP_ADJ = 200`, `PERCEPTIBLE_LOW_APP_ADJ = 250`, `BACKUP_APP_ADJ = 300`, `HEAVY_WEIGHT_APP_ADJ = 400`, `SERVICE_ADJ = 500`, `HOME_APP_ADJ = 600`, `PREVIOUS_APP_ADJ = 700`, `SERVICE_B_ADJ = 800`, `CACHED_APP_MIN_ADJ = 900`, `CACHED_APP_MAX_ADJ = 999`, `TRIM_CRITICAL_THRESHOLD = 3`, `TRIM_LOW_THRESHOLD = 5`; the lmkd ladder `mOomAdj = { FOREGROUND_APP_ADJ, VISIBLE_APP_ADJ, PERCEPTIBLE_APP_ADJ, PERCEPTIBLE_LOW_APP_ADJ, CACHED_APP_MIN_ADJ, CACHED_APP_LMK_FIRST_ADJ }` with `mOomMinFreeLow = {12288, 18432, 24576, 36864, 43008, 49152}` KB and `mOomMinFreeHigh = {73728, 92160, 110592, 129024, 147456, 184320}` KB.
Reading (INFERRED, arithmetic on the quoted code): the client is SystemUI (persistent, `clientAdj` far below `PERCEPTIBLE_APP_ADJ`, never cached); the flags `BIND_AUTO_CREATE | BIND_FOREGROUND_SERVICE_WHILE_AWAKE` match none of the special cases, so the process takes `newAdj = Math.max(clientAdj, VISIBLE_APP_ADJ)` = **100** and `app.cached = false`; the process state is `PROCESS_STATE_BOUND_FOREGROUND_SERVICE` while the device is awake and `PROCESS_STATE_IMPORTANT_FOREGROUND` while it is not. **The stock Launcher3 process is therefore never a cached process, never enters the LRU cached region, and lmkd only reaches it after every cached, perceptible and service process is gone (the `VISIBLE_APP_ADJ = 100` rung needs free memory below `mOomMinFree[1]`, 18 MB with the low table).** This holds on the friend's tablet as well; it explains why nothing ever shrinks the process from the outside, not why it grows.

### 2.3 Home / previous process handling (VERIFIED) and the two HOME-type launchers
`OomAdjuster.computeOomAdjLocked` (same file, earlier in the method):
```java
        if (wpc.isHomeProcess()) {
            if (adj > ProcessList.HOME_APP_ADJ) {
                // This process is hosting what we currently consider to be the
                // home app, so we don't want to let it go into the background.
                adj = ProcessList.HOME_APP_ADJ;
                schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
                app.cached = false;
                app.adjType = "home";
                ...
            }
            if (procState > ActivityManager.PROCESS_STATE_HOME) {
                procState = ActivityManager.PROCESS_STATE_HOME;
                app.adjType = "home";
                ...
            }
        }

        if (wpc.isPreviousProcess() && app.hasActivities()) {
            if (adj > ProcessList.PREVIOUS_APP_ADJ) {
                // This was the previous process that showed UI to the user.
                // We want to try to keep it around more aggressively, to give
                // a good experience around switching between two apps.
                adj = ProcessList.PREVIOUS_APP_ADJ;
                schedGroup = ProcessList.SCHED_GROUP_BACKGROUND;
                app.cached = false;
                app.adjType = "previous";
                ...
            }
            if (procState > PROCESS_STATE_LAST_ACTIVITY) {
                procState = PROCESS_STATE_LAST_ACTIVITY;
                app.adjType = "previous";
                ...
            }
        }
```
`frameworks/base/services/core/java/com/android/server/wm/WindowProcessController.java`: `public boolean isHomeProcess() { synchronized (mAtm.mGlobalLockWithoutBoost) { return this == mAtm.mHomeProcess; } }` (same shape for `isPreviousProcess()` with `mPreviousProcess`).
`frameworks/base/services/core/java/com/android/server/wm/ActivityStackSupervisor.java`, `realStartActivityLocked`, and `updateHomeProcess`:
```java
        if (r.isActivityTypeHome()) {
            // Home process is the root process of the task.
            updateHomeProcess(task.mActivities.get(0).app);
        }
    ...
    void updateHomeProcess(WindowProcessController app) {
        if (app != null && mService.mHomeProcess != app) {
            if (!mHandler.hasMessages(REPORT_HOME_CHANGED_MSG)) {
                mHandler.sendEmptyMessage(REPORT_HOME_CHANGED_MSG);
            }
            mService.mHomeProcess = app;
        }
    }
```
and `cleanUpRemovedTaskLocked`: `if (proc == mService.mHomeProcess) { // Don't kill the home process along with tasks from the same package. continue;`.
Our own start of the stock home (`app/src/main/java/com/tj/launcher/RouterActivity.java` L108-120, `startStock()`): `new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(stock.getPackageName()).addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)` (no component on purpose: "An implicit HOME intent restricted to the stock package (no component) is a HOME-type task"). INFERRED (the `ActivityRecord` constructor rule was not fetched; the 2-category / no-component / NEW_TASK conditions are from AOSP memory): that task is `ACTIVITY_TYPE_HOME`, so every time a stock `Launcher` instance is really started in its process (`realStartActivityLocked`), `mHomeProcess` points at `com.android.launcher3`; every time our `RouterActivity` instance is started it points back at `com.tj.launcher`. Only ONE of the two processes is "the home process" at any moment, but (2.2) the stock process is held non-cached by the binding either way, so the flipping does not change its class; it does change whether the (never-fetched) `hasShownUi && !isHomeProcess()` exception applies, which needs `clientAdj > PERCEPTIBLE_APP_ADJ` and is false here.

### 2.4 Trim levels the stock process does and does not receive (partly VERIFIED, mostly INFERRED)
VERIFIED fragments: `OomAdjuster.updateOomAdjLocked` counts `if (app.getCurProcState() >= ActivityManager.PROCESS_STATE_HOME && !app.killedByAm) { numTrimming++; }` and calls `mService.updateLowMemStateLocked(numCached, numEmpty, numTrimming)` (so in Android 10 the trim ladder lives in AMS, not in OomAdjuster); `ProcessList.TRIM_CRITICAL_THRESHOLD = 3`, `TRIM_LOW_THRESHOLD = 5`; `ActivityThread.ApplicationThread.scheduleTrimMemory` posts `handleTrimMemory` on the Choreographer commit callback ("to minimize jank-risk"). The body of `updateLowMemStateLocked` was NOT readable (AMS page truncated; one search hit, an e/OS Android 10 diff, shows `if (app.trimMemoryLevel < ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && app.thread != null)` before `scheduleTrimMemory`).
INFERRED from AOSP memory of that method: (a) processes with `curProcState >= PROCESS_STATE_HOME` receive the LRU ladder TRIM_MEMORY_BACKGROUND / MODERATE / COMPLETE; (b) every other process receives `TRIM_MEMORY_UI_HIDDEN` once, only `if ((app.curProcState >= PROCESS_STATE_IMPORTANT_BACKGROUND || app.systemNoUi) && app.pendingUiClean)`, and afterwards only `fgTrimLevel` = `TRIM_MEMORY_RUNNING_MODERATE / LOW / CRITICAL` chosen from `numCached + numEmpty` against the thresholds 3 and 5. With 2.2 (state 6 or 7, adj 100) the stock process is in class (b) but never satisfies its UI_HIDDEN condition, so **AMS never sends it UI_HIDDEN, BACKGROUND, MODERATE or COMPLETE; only RUNNING_*,** and RUNNING_CRITICAL only when the system has at most 3 cached+empty processes (plausible on a 2 GB Go tablet; not measured).
What each level does in the stock process (VERIFIED code):
- `RecentsModel.onTrimMemory` (1.5): UI_HIDDEN -> high-res flag off (already off on a low-RAM device); `== RUNNING_CRITICAL` -> clears the 3 thumbnails and the 12 icons; nothing for BACKGROUND / MODERATE / COMPLETE (equality tests).
- `WindowManagerGlobal.trimMemory` (`core/java/android/view/WindowManagerGlobal.java`, reached from `ActivityThread.handleTrimMemory`, INFERRED):
```java
    public void trimMemory(int level) {
        if (ThreadedRenderer.isAvailable()) {
            if (shouldDestroyEglContext(level)) {
                // Destroy all hardware surfaces and resources associated to
                // known windows
                synchronized (mLock) {
                    for (int i = mRoots.size() - 1; i >= 0; --i) {
                        mRoots.get(i).destroyHardwareResources();
                    }
                }
                // Force a full memory flush
                level = ComponentCallbacks2.TRIM_MEMORY_COMPLETE;
            }

            ThreadedRenderer.trimMemory(level);
            if (ThreadedRenderer.sTrimForeground) {
                doTrimForeground();
            }
        }
    }

    public static boolean shouldDestroyEglContext(int trimLevel) {
        // On low-end gfx devices we trim when memory is moderate;
        // on high-end devices we do this when low.
        if (trimLevel >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            return true;
        }
        if (trimLevel >= ComponentCallbacks2.TRIM_MEMORY_MODERATE
                && !ActivityManager.isHighEndGfx()) {
            return true;
        }
        return false;
    }
```
- HWUI `libs/hwui/renderthread/CacheManager.cpp` (android10-release, L= 53-149): `#define SURFACE_SIZE_MULTIPLIER (12.0f * 4.0f)`, `#define BACKGROUND_RETENTION_PERCENTAGE (0.5f)`; `mMaxResourceBytes(mMaxSurfaceArea * SURFACE_SIZE_MULTIPLIER)` (600 x 1024 = 614,400 px -> about 29.5 MB Skia GPU cache cap; background cap half of it); `trimMemory(mode)`: `Complete` -> `freeGpuResources()` + `SkGraphics::PurgeAllCaches()`; `UiHidden` -> `purgeUnlockedResources(true)` and a limits toggle (the cache keeps up to the cap); `trimStaleResources()` -> `purgeResourcesNotUsedInMs(std::chrono::seconds(30))`. `CanvasContext::trimMemory`: `if (level >= TRIM_MEMORY_COMPLETE) { ... thread.destroyRenderingContext(); }` else `UiHidden` mode from `TRIM_MEMORY_UI_HIDDEN` (20) up; RUNNING_CRITICAL (15) is below both thresholds: **nothing**.
=> In a process that AMS never sends UI_HIDDEN / MODERATE / COMPLETE to, the EGL context and the Skia GL cache (cap about 30 MB on this screen) live as long as the process; nothing releases the GL driver's per-context memory except the process dying (INFERRED).

### 2.5 The activity-level "workaround" trims nothing of HWUI (INFERRED)
`BaseRecentsActivity.onStop` and `Launcher.onStop` call `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)` on the ACTIVITY (1.4, `CD2_AOSP_Q_LAUNCHER3_FACTS` Q5). `WindowManagerGlobal.trimMemory` is reached from `ActivityThread.handleTrimMemory` (not from the activity), so this self-trim reaches only `Launcher.onTrimMemory` (`SQLiteDatabase.releaseMemory()`) and `UiFactory.onTrimMemory` -> `RecentsModel.onTrimMemory`. The per-window path that does run on every stop is `ViewRootImpl.setWindowStopped(true)` (VERIFIED, `core/java/android/view/ViewRootImpl.java`):
```java
    void setWindowStopped(boolean stopped) {
        checkThread();
        if (mStopped != stopped) {
            mStopped = stopped;
            final ThreadedRenderer renderer = mAttachInfo.mThreadedRenderer;
            if (renderer != null) {
                if (DEBUG_DRAW) Log.d(mTag, "WindowStopped on " + getTitle() + " set to " + mStopped);
                renderer.setStopped(mStopped);
            }
            if (!mStopped) {
                mNewSurfaceNeeded = true;
                scheduleTraversals();
            } else {
                if (renderer != null) {
                    renderer.destroyHardwareResources(mView);
                }
            }
            for (int i = 0; i < mWindowStoppedCallbacks.size(); i++) {
                mWindowStoppedCallbacks.get(i).windowStopped(stopped);
            }
            if (mStopped) {
                if (mSurfaceHolder != null && mSurface.isValid()) {
                    notifySurfaceDestroyed();
                }
                destroySurface();
            }
        }
    }
    @AnyThread
    void destroyHardwareResources() {
        final ThreadedRenderer renderer = mAttachInfo.mThreadedRenderer;
        if (renderer != null) {
            // This is called by WindowManagerGlobal which may or may not be on the right thread
            if (Looper.myLooper() != mAttachInfo.mHandler.getLooper()) {
                mAttachInfo.mHandler.postAtFrontOfQueue(this::destroyHardwareResources);
                return;
            }
            renderer.destroyHardwareResources(mView);
            renderer.destroy();
        }
    }
```
So a stopped window drops its Surface (its BufferQueue buffers go with it) and its display lists; `renderer.destroy()` (the `CanvasContext`) only happens for the destroy-EGL-context trim level above or when the window is removed.

### 2.6 ART: why a never-"background" process only grows (VERIFIED ART code, mapping INFERRED)
`art/runtime/gc/heap.cc`, android10-release (verbatim, trimmed):
```cpp
void Heap::UpdateProcessState(ProcessState old_process_state, ProcessState new_process_state) {
  if (old_process_state != new_process_state) {
    const bool jank_perceptible = new_process_state == kProcessStateJankPerceptible;
    ...
    if (jank_perceptible) {
      // Transition back to foreground right away to prevent jank.
      RequestCollectorTransition(foreground_collector_type_, 0);
    } else {
      // Don't delay for debug builds since we may want to stress test the GC.
      // If background_collector_type_ is kCollectorTypeHomogeneousSpaceCompact then we have
      // special handling which does a homogenous space compaction once but then doesn't transition
      // the collector. Similarly, we invoke a full compaction for kCollectorTypeCC but don't
      // transition the collector.
      RequestCollectorTransition(background_collector_type_,
                                 kStressCollectorTransition
                                 ? 0
                                 : kCollectorTransitionWait);
    }
  }
}
void Heap::DoPendingCollectorTransition() {
  CollectorType desired_collector_type = desired_collector_type_;
  // Launch homogeneous space compaction if it is desired.
  if (desired_collector_type == kCollectorTypeHomogeneousSpaceCompact) {
    if (!CareAboutPauseTimes()) {
      PerformHomogeneousSpaceCompact();
    } else {
      VLOG(gc) << "Homogeneous compaction ignored due to jank perceptible process state";
    }
  } else if (desired_collector_type == kCollectorTypeCCBackground) {
    DCHECK(kUseReadBarrier);
    if (!CareAboutPauseTimes()) {
      // Invoke CC full compaction.
      CollectGarbageInternal(collector::kGcTypeFull,
                             kGcCauseCollectorTransition,
                             /*clear_soft_references=*/false);
    } else {
      VLOG(gc) << "CC background compaction ignored due to jank perceptible process state";
    }
  } else {
    TransitionCollector(desired_collector_type);
  }
}
void Heap::Trim(Thread* self) {
  Runtime* const runtime = Runtime::Current();
  if (!CareAboutPauseTimes()) {
    // Deflate the monitors, this can cause a pause but shouldn't matter since we don't care
    // about pauses.
    ...
  }
  TrimIndirectReferenceTables(self);
  TrimSpaces(self);
  // Trim arenas that may have been used by JIT or verifier.
  runtime->GetArenaPool()->TrimMaps();
}
```
INFERRED (not fetched, `ActivityThread.updateProcessState`): the framework tells ART "jank perceptible" for every `processState <= PROCESS_STATE_IMPORTANT_FOREGROUND`. 2.2 puts the stock process at BOUND_FOREGROUND_SERVICE / IMPORTANT_FOREGROUND, both inside that range, so `CareAboutPauseTimes()` stays true for the whole life of the process: the background compaction GC (low-RAM mode relies on it), the homogeneous compaction, monitor deflation and (from AOSP memory of `TrimSpaces`) the native-heap purge (`mallopt(M_PURGE)`, guarded by `!CareAboutPauseTimes()`) never run. Java heap and jemalloc native heap fragmentation can therefore only be reclaimed by ordinary GCs. This amplifies any retention bug (1.5), it does not create one.

<!-- CG1F-Q3 -->
