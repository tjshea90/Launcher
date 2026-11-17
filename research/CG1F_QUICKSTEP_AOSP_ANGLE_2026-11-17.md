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
**What this means (INFERRED from the verified code):** `RecentsModel.INSTANCE` is a process singleton; its `ArrayList` holds a STRONG reference to every `IconRecentsView.listener` lambda; the lambda captures `this` (it calls the instance method `getTaskViews()` and reads `mTaskRecyclerView`); `IconRecentsView` holds `mActivity` / `mContext` (the Activity), the adapter, the `TaskListLoader` (with its `Task` list: icons and thumbnails) and, through the RecyclerView, up to 7 `TaskItemView`s with their `ThumbnailDrawable`s (BitmapShader on `ThumbnailData.thumbnail`). **Each creation of an `IconRecentsView` therefore pins the whole `Activity` (view tree, adapter, task icons, task thumbnails) for the life of the process.** The leak is per ACTIVITY CREATION (fallback `RecentsActivity`, and the Go `Launcher` if section 1.4's inference holds), not per Recents open: a stopped, resumed activity is created once. It grows with every RELAUNCH (configuration change not in `configChanges`: uiMode, fontScale, density, locale) and every process-internal re-creation. Plain AOSP (the emulator image) uses `RecentsView`, a different class (VERIFIED later in 4.3: it registers in `onAttachedToWindow` and removes in `onDetachedFromWindow`), which is why 80 font-scale relaunches on the CI image (cd3u: Activities 2, Views flat) cannot show it.
Each leaked view is also KEPT UP TO DATE: `onTaskSnapshotChanged` calls every listener for every snapshot change, and a leaked view whose adapter holds that task id receives the new `ThumbnailData` (`taskView.setThumbnail`), so the leaked instances keep pinning recent snapshot Bitmaps (hardware bitmaps over a `GraphicBuffer`, i.e. gralloc memory outside the Java heap; the type is VERIFIED in 3.3, the size stays INFERRED).
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

## 3. Q3: per-window costs, GPU / gralloc memory, and what "Memory used by apps" counts

### 3.1 What a window does to the process: what IS given back at stop (VERIFIED) and what stays (INFERRED)
`libs/hwui/renderthread/CanvasContext.cpp` (frameworks/base, android10-release; the fetch model refused long quotes, so these are short verbatim fragments plus its description of each body; no line numbers available):
- `void CanvasContext::setStopped(bool stopped) {` ... `mRenderThread.removeFrameCallback(this);` `mRenderPipeline->onStop();` (stopping bumps `mGenerationID`, removes the frame callback, calls `onStop()` on the pipeline).
- `void CanvasContext::destroy() {` ... `stopDrawing();` `setSurface(nullptr);` `freePrefetchedLayers();` `destroyHardwareResources();`.
- `setSurface`: `mNativeSurface = new ReliableSurface{std::move(surface)};` (create) and `mNativeSurface = nullptr;` (clear). "The EGL-level work happens in `mRenderPipeline->setSurface(...)`", which was not in the text read.
- `CanvasContext::trimMemory(RenderThread& thread, int level)`: `if (level >= TRIM_MEMORY_COMPLETE) {` ... `thread.destroyRenderingContext();` (COMPLETE: cache trim in Complete mode, then the rendering context is destroyed; UI_HIDDEN: cache trim in UiHidden mode; nothing below UI_HIDDEN).
- `destroyHardwareResources`: `if (mRenderPipeline->isContextReady()) {` ... `mRenderPipeline->onDestroyHardwareResources();`.

Together with `ViewRootImpl.setWindowStopped(true)` (2.5, VERIFIED: `renderer.destroyHardwareResources(mView)` and `destroySurface()`): **a stopped window gives back its Surface (its BufferQueue buffers) and its display lists. Nothing per-window stays behind except what is process-global.**
INFERRED (not fetched: `EglManager`, `SkiaOpenGLPipeline`, `RenderThread::requireGlContext`): one EGL context and one Skia `GrContext` exist per process (per RenderThread), created for the first hardware-accelerated window and destroyed only by `destroyRenderingContext` (trim COMPLETE, or MODERATE on a not-high-end-gfx device, 2.4) or by process death. A visible full-screen window holds its BufferQueue (typically 3 buffers; one 600 x 1024 RGBA_8888 buffer = 2.34 MiB, so about 7 MiB per visible window) in gralloc memory, which the memtrack HAL reports as "EGL mtrack" (3.5).

### 3.2 The HWUI caches are bounded by the screen area and are only trimmed by levels this process never gets (VERIFIED)
`libs/hwui/renderthread/CacheManager.cpp` (frameworks/base, android10-release; the listing numbers are the fetch model's best effort, L~53-148; comment lines and unrelated lines dropped, `...` = dropped):
```cpp
 57: #define SURFACE_SIZE_MULTIPLIER (12.0f * 4.0f)
 58: #define BACKGROUND_RETENTION_PERCENTAGE (0.5f)
 60: CacheManager::CacheManager(const DisplayInfo& display)
 61:         : mMaxSurfaceArea(display.w * display.h)
 62:         , mMaxResourceBytes(mMaxSurfaceArea * SURFACE_SIZE_MULTIPLIER)
 63:         , mBackgroundResourceBytes(mMaxResourceBytes * BACKGROUND_RETENTION_PERCENTAGE)
 67:         , mMaxGpuFontAtlasBytes(GrNextSizePow2(mMaxSurfaceArea))
 70:         , mMaxCpuFontCacheBytes(std::max(mMaxGpuFontAtlasBytes*4, SkGraphics::GetFontCacheLimit()))
 71:         , mBackgroundCpuFontCacheBytes(mMaxCpuFontCacheBytes * BACKGROUND_RETENTION_PERCENTAGE) {
 ...
 78: void CacheManager::reset(sk_sp<GrContext> context) {
 ...
 84:         mGrContext->getResourceCacheLimits(&mMaxResources, nullptr);
 85:         mGrContext->setResourceCacheLimits(mMaxResources, mMaxResourceBytes);
 ...
116: void CacheManager::trimMemory(TrimMemoryMode mode) {
 ...
122:         case TrimMemoryMode::Complete:
123:             mVectorDrawableAtlas = new skiapipeline::VectorDrawableAtlas(mMaxSurfaceArea / 2);
124:             mGrContext->freeGpuResources();
125:             SkGraphics::PurgeAllCaches();
126:             break;
127:         case TrimMemoryMode::UiHidden:
131:             mGrContext->purgeUnlockedResources(true);
132:             mGrContext->setResourceCacheLimits(mMaxResources, mBackgroundResourceBytes);
133:             mGrContext->setResourceCacheLimits(mMaxResources, mMaxResourceBytes);
 ...
143: void CacheManager::trimStaleResources() {
 ...
148:     mGrContext->purgeResourcesNotUsedInMs(std::chrono::seconds(30));
```
Arithmetic for a 600 x 1024 panel (INFERRED that `display` is the physical display; the CI emulator's physical size is 320 x 640, `CD1_EMULATOR`): area 614,400 px; resource cache cap 614,400 x 48 = 29,491,200 B (28.1 MiB); background cap 14.1 MiB; GPU font atlas `GrNextSizePow2(614400)` = 1,048,576 B (four atlases); CPU font cache at least 4 MiB. The fetch answer says `CacheManager::onFrameCompleted` and `onThreadIdle` do not exist in this file, so WHO calls `trimStaleResources` was not read.
Reading (INFERRED from the quoted code): HWUI's own caches are capped at roughly 32-36 MiB on this screen. They cannot by themselves make 400 MB. A Graphics figure far above that is (a) gralloc buffers this process still maps (3.3) or (b) vendor-driver memory outside Skia's accounting.

### 3.3 The thumbnails are graphic buffers, not Java bitmaps (VERIFIED field list; constructor body from the fetch model's summary)
`frameworks/base/packages/SystemUI/shared/src/com/android/systemui/shared/recents/model/ThumbnailData.java` (android10-release), fields verbatim: `public final Bitmap thumbnail; public int orientation; public Rect insets; public boolean reducedResolution; public boolean isRealSnapshot; public boolean isTranslucent; public int windowingMode; public int systemUiVisibility; public float scale;`. The constructor `ThumbnailData(TaskSnapshot snapshot)` (summarised by the fetch model because of its quote limit): "wraps the snapshot's hardware buffer into `thumbnail` with `Bitmap.wrapHardwareBuffer`, using the snapshot's color space" and copies the other fields from the snapshot.
So every `Task.thumbnail` that `IconRecentsView`, `TaskListLoader` or `ThumbnailDrawable` references is a hardware Bitmap over a `GraphicBuffer`: its pixels live in gralloc memory (reported under Graphics, "EGL mtrack"), NOT in the Dalvik heap. The 3-entry `TaskThumbnailCache` (1.6) does not cap them: a `Task` keeps the reference it was handed (`TaskListLoader` copies `task.thumbnail = loadedTask.thumbnail`, 1.6), and a leaked `IconRecentsView` (1.5) keeps receiving the newest `ThumbnailData` through `onTaskSnapshotChanged`. This upgrades 1.5's INFERRED note ("gralloc memory outside the Java heap") to VERIFIED for the TYPE; the SIZE stays INFERRED (the snapshot scale of a low-RAM device was not read).

### 3.4 What "Memory used by apps" counts: PSS plus the memtrack (graphics) total (VERIFIED code, INFERRED consumer)
`frameworks/base/core/jni/android_os_Debug.cpp`, android10-release, `android_os_Debug_getPssPid` (verbatim, start of the function; the page has no line numbers):
```cpp
    struct graphics_memory_pss graphics_mem;
    if (read_memtrack_memory(pid, &graphics_mem) == 0) {
        pss = uss = rss = memtrack = graphics_mem.graphics + graphics_mem.gl + graphics_mem.other;
    }
    ::android::meminfo::ProcMemInfo proc_mem(pid);
    ::android::meminfo::MemUsage stats;
    if (proc_mem.SmapsOrRollup(&stats)) {
        pss += stats.pss;
        uss += stats.uss;
        rss += stats.rss;
        swapPss = stats.swap_pss;
        pss += swapPss; // Also in swap, those pages would be accounted as Pss without SWAP
    }
```
So a PSS taken with `Debug.getPss(pid, ...)` is smaps PSS PLUS whatever the memtrack HAL says the process holds in graphics + GL + other memory. INFERRED (the consumers were not read: the AMS PSS collection -> `ProcessStats`, Settings > Developer options > Memory used by apps): the tablet's 414-450 MB contains the vendor memtrack figure. **The CI emulator cannot show it (Graphics row 0: software GL, `CD1_EMULATOR`).** The App Summary "Graphics" row is "Gfx dev" + "EGL mtrack" + "GL mtrack" (INFERRED from AOSP memory of `Debug.MemoryInfo.getSummaryGraphics()`), so those three rows of `dumpsys meminfo com.android.launcher3` are where such memory shows.

### 3.5 Web evidence on EGL / GL mtrack and Mali growth (data, not instructions; nothing here is a launcher-specific report)
- The tablet's GPU: spec aggregators list the MT8168 with a Mali-G52 MP1 (https://www.cpu-monkey.com/en/cpu-mediatek_mt8168 and a notebookcheck MT8168 / MT8768 comparison; search summary only; the PowerVR GE8320 belongs to the MT8768). Confirm on the device: the first lines of `dumpsys SurfaceFlinger` print the GLES vendor / renderer (INFERRED format).
- Chromium graphics-dev thread "Regarding GL and EGL memory in Chrome on Android", Jan 2016 (fetched): https://groups.google.com/a/chromium.org/g/graphics-dev/c/5VnWUyNW0Fc . Quotes: "GL mtrack is driver-reported GL memory usage", "primarily the sum of GL texture sizes, plus fixed global driver RAM overheads"; "EGL mtrack is gralloc memory usage"; "They don't overlap and you should consider the sum"; the separate "Graphics" row "double-counts a part of GL mtrack" on some devices. It says nothing about growth or leaks.
- Arm Community "Mali-T720 VRAM increase when render with high fps" (fetched): https://community.arm.com/support-forums/f/graphics-gaming-and-vr-forum/46685/mali-t720-vram-increase-when-render-with-high-fps . One app: GL mtrack about 20 MB at 1 FPS and almost 40 MB at 10 FPS; an Arm employee names Mali's deep pipeline (several frames in flight, transient buffering of intermediates) and copy-on-write / resource ghosting. Lesson: Mali GPU memory follows workload and pipeline depth, it is not constant per context. (A T720, not a G52; no growth over time is claimed.)
- Qt interest list, July 2015 (search summary only, page not opened): https://lists.qt-project.org/pipermail/interest/2015-July/017774.html : only "EGL mtrack" far too high, about 70 MB at start rising to a couple of hundred MB each time a video is started; not Mali. Further search-summary-only leads without a kept URL: a Unity bug note that GL mtrack is not reported on Mali; a forum remark that Mali's memory in the Graphics part of `dumpsys meminfo` is higher than other vendors'; an Arm forum poster whose meminfo VRAM exceeds `/d/mali/mem/<pid>`. The search advice was: check whether GL mtrack is reported at all on the device, else track EGL mtrack.
- NOT FOUND after three searches: any published report of MediaTek / Mali per-surface memory growth in a launcher or Recents process. That is a statement about the search, not about the platform.

### 3.6 Q3 in one paragraph
VERIFIED: stopping a window destroys its Surface and display lists (2.5); HWUI's own GPU caches are capped by the screen area (3.2); Q's `Debug.getPss` adds the memtrack total to PSS (3.4); the stock thumbnails are hardware bitmaps (3.3). INFERRED: per-window costs do not accumulate by themselves; what CAN accumulate in graphics memory is (a) hardware-bitmap thumbnails pinned by the 1.5 leak and (b) vendor GL-driver memory that follows the number and size of surfaces and textures and is never released because the process never gets a trim above RUNNING_* (2.4).

## 4. Q4: events that rebuild UI or model in the stock process, and known leaks

### 4.1 `Launcher.onNewIntent` (the HOME intent) is a state reset: no creation, no registration (VERIFIED)
`src/com/android/launcher3/Launcher.java` (platform/packages/apps/Launcher3, android10-release; the page has no line numbers, locate by method name), verbatim, trimmed (`...` = dropped):
```java
    protected void onNewIntent(Intent intent) {
        ...
        boolean alreadyOnHome = hasWindowFocus() && ((intent.getFlags() &
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        // Check this condition before handling isActionMain, as this will get reset.
        boolean shouldMoveToDefaultScreen = alreadyOnHome && isInState(NORMAL)
                && AbstractFloatingView.getTopOpenView(this) == null;
        boolean isActionMain = Intent.ACTION_MAIN.equals(intent.getAction());
        boolean internalStateHandled = InternalStateHandler
                .handleNewIntent(this, intent, isStarted());
        if (isActionMain) {
            if (!internalStateHandled) {
                // In all these cases, only animate if we're already on home
                AbstractFloatingView.closeAllOpenViews(this, isStarted());
                if (!isInState(NORMAL)) {
                    mStateManager.goToState(NORMAL);
                }
                // Reset the apps view
                if (!alreadyOnHome) {
                    mAppsView.reset(isStarted() /* animate */);
                }
                if (shouldMoveToDefaultScreen && !mWorkspace.isHandlingTouch()) {
                    mWorkspace.post(mWorkspace::moveToDefaultScreen);
                }
            }
            // Handle HOME_INTENT
            ... UiThreadHelper.hideKeyboardAsync(this, v.getWindowToken());
            if (mLauncherCallbacks != null) { mLauncherCallbacks.onHomeIntent(internalStateHandled); }
        }
    }
```
A HOME intent delivered to a live `Launcher` (singleTask) is a reset of the existing instance. So a "stock-home start" costs memory only when it creates a NEW instance (4.4).

### 4.2 When a `Launcher` instance is destroyed and re-created (VERIFIED for Launcher; the Go panel is 1.5)
Same file, verbatim, trimmed. `onStop`: `mAppWidgetHost.setListenIfResumed(false); NotificationListener.removeNotificationsChangedListener(); getStateManager().moveToRestState(); UiFactory.onLauncherStateOrResumeChanged(this); // Workaround for b/78520668, explicitly trim memory once UI is hidden` then `onTrimMemory(TRIM_MEMORY_UI_HIDDEN);`. `onTrimMemory`: `if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) { SQLiteDatabase.releaseMemory(); ... } ... UiFactory.onTrimMemory(this, level);`. `onConfigurationChanged`: `if ((diff & CONFIG_LOCALE) != 0) { Folder.setLocaleDependentFields(getResources(), true /* force */); } if ((diff & (CONFIG_ORIENTATION | CONFIG_SCREEN_SIZE)) != 0) { onIdpChanged(mDeviceProfile.inv); }` (fontScale, density and uiMode are not handled: the system recreates the activity for them, `CD2_AOSP_Q_LAUNCHER3_FACTS` Q1 / Q5). `onDestroy`:
```java
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOffReceiver);
        mWorkspace.removeFolderListeners();
        if (mCancelTouchController != null) {
            mCancelTouchController.run();
            mCancelTouchController = null;
        }
        if (mModel.isCurrentCallbacks(this)) {
            mModel.stopLoader();
            LauncherAppState.getInstance(this).setLauncher(null);
        }
        mRotationHelper.destroy();
        try { mAppWidgetHost.stopListening(); } catch (NullPointerException ex) { ... }
        TextKeyListener.getInstance().release();
        clearPendingBinds();
        LauncherAppState.getIDP(this).removeOnChangeListener(this);
        if (mLauncherCallbacks != null) { mLauncherCallbacks.onDestroy(); }
    }
```
Registration audit (the fetch model matched every registration in `onCreate` / `setupViews` against its undo): the IDP listener, the screen-off receiver, `setLauncher`, the widget host, the rotation helper, the callbacks and the `enableLiveUIChanges` closure ALL have an undo in `onDestroy`; only `mStateManager.addStateListener(...)` and `mDragController.addDragListener(mWorkspace)` have none, and both objects belong to the activity (they die with it). **The stock `Launcher` activity does not leak through its own registrations.** What the Go build adds is the `IconRecentsView` of 1.5, whose registration sits in a process singleton (INFERRED that the Go `Launcher` inflates one, 1.4).
`UiFactory` (`quickstep/src/com/android/launcher3/uioverrides/UiFactory.java`, listing L= 85-98): `onEnterAnimationComplete(Context)` -> `RecentsModel.INSTANCE.get(context).getThumbnailCache().getHighResLoadingState().setVisible(true);` (irrelevant on a low-RAM device, 1.6) and `onTrimMemory(Context, int)` -> `model.onTrimMemory(level)` (equality tests only, 1.6).

### 4.3 The non-Go `RecentsView` is symmetric: the contrast that explains the emulator (VERIFIED; supersedes the INFERRED note in 1.5)
`quickstep/recents_ui_overrides/src/com/android/quickstep/views/RecentsView.java`, android10-release, verbatim (no line numbers on the page):
```java
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateTaskStackListenerState();
        mModel.getThumbnailCache().getHighResLoadingState().addCallback(this);
        mActivity.addMultiWindowModeChangedListener(mMultiWindowModeChangedListener);
        ActivityManagerWrapper.getInstance().registerTaskStackListener(mTaskStackListener);
        mSyncTransactionApplier = new SyncRtSurfaceTransactionApplierCompat(this);
        RecentsModel.INSTANCE.get(getContext()).addThumbnailChangeListener(this);
        mIdp.addOnChangeListener(this);
    }
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateTaskStackListenerState();
        mModel.getThumbnailCache().getHighResLoadingState().removeCallback(this);
        mActivity.removeMultiWindowModeChangedListener(mMultiWindowModeChangedListener);
        ActivityManagerWrapper.getInstance().unregisterTaskStackListener(mTaskStackListener);
        mSyncTransactionApplier = null;
        RecentsModel.INSTANCE.get(getContext()).removeThumbnailChangeListener(this);
        mIdp.removeOnChangeListener(this);
    }
```
Every line of the file that contains `ThumbnailChangeListener` (fetch answer): the import, the `implements ... TaskThumbnailChangeListener`, the add above, the remove above. **The plain-AOSP view that the CI emulator runs removes its registration at detach; the Go view of 1.5 does not.** The 80 relaunches of section cd3u (Activities 2, Views flat) therefore say nothing about the Go class.
`go/quickstep/src/com/android/quickstep/fallback/GoRecentsActivityRootView.java` (listing L= 1-31, whole file): a `BaseDragLayer` subclass that only handles insets; no registrations.

### 4.4 Events that make the stock process work, and which of them add memory for good
Cited from `CD2_AOSP_Q_LAUNCHER3_FACTS_2026-11-14.md` (Q2, Q6, Q7; not re-fetched here), plus one INFERRED rule:
- Package added / changed / removed for the DEFAULT HOME package (our alias flips, app updates): `OverviewComponentObserver` re-runs `updateOverviewTargets()` (small helper objects, no list); `LauncherModel.onPackageChanged` enqueues `PackageUpdatedTask(OP_UPDATE)` for ANY package (re-decodes that package's icons, rewrites its icon-cache rows; same keys, replaced not added). Bounded.
- `ACTION_PREFERRED_ACTIVITY_CHANGED` (default home changed): the same observer path. Bounded.
- `ConfigMonitor` on fontScale / density: `InvariantDeviceProfile.onConfigChanged` in process (`APPLY_CONFIG_AT_RUNTIME` true), no process kill. Bounded.
- A HOME intent or a Recents open delivered to a live singleTask instance: `onNewIntent` + `onResume` (4.1, 1.7). No creation.
- **Configuration changes that are NOT in `configChanges`** (uiMode, fontScale, density for `Launcher` and `RecentsActivity`; locale for `RecentsActivity`): the system recreates the activity. INFERRED (AOSP `ActivityRecord.ensureActivityConfiguration`, not fetched): a stopped, invisible activity is not relaunched at the moment of the change but once, when it next becomes visible; the number of new instances is therefore "number of shows after at least one change", at most one per Recents open / Quickstep session. Each new instance of the Go build inflates a new `IconRecentsView` (1.4) and registers it (1.5). This is the one event class of which every occurrence adds permanent memory.
- Process restart (force-stop, kill, crash) empties `RecentsModel.INSTANCE` and everything it pinned. This fits "about 90 MB after several force-stops".

### 4.5 Known leaks and leads (what was opened, what is only a search summary)
| Item | Status |
| :- | :- |
| Go `IconRecentsView` thumbnail listener never removed (1.5) | VERIFIED in android10-release code; no upstream bug number found |
| e/OS BlissLauncher3 commits https://gitlab.e.foundation/e/os/BlissLauncher3/-/commit/025799ba9c3b38b9c5dd613d81bf0cff062dfa86 and .../de6770a2ec88ee11c89b46fe3bd7dc0f3fba4cb9 | search summary only (gitlab answered 429, not opened). Summary: `IconRecentsView` registers `addThumbnailChangeListener(listener)` at the end of its constructor with no matching removal (the pre-fix state); a later commit makes it implement `TaskVisualsChangeListener` and adds `removeThumbnailChangeListener(this)`. Consistent with 1.5, unverified. The same summary says `TaskListLoader`'s constructor registered a listener too in that fork; in android10-release it does NOT (read: its constructor only stores `RecentsModel.INSTANCE.get(context)`) |
| OmniROM Launcher3 merge "Remove listener in destroy() that caused a memory leak" | search summary only; the summary itself says it concerns a thumbnail view, not `IconRecentsView`; host not reachable |
| AOSP 60f21a8 "Fix ViewTreeObserver crash on Recents Go" (2019-04-12, Bug 130740246) | OPENED: `go/quickstep/src/com/android/quickstep/views/IconRecentsView.java`, `scheduleFadeInLayoutAnimation()` now asks `mTaskRecyclerView.getViewTreeObserver()` again before `removeOnGlobalLayoutListener(this)`. Not a leak fix; shows the Go view was still being patched during the Q cycle |
| AOSP a035646ea8 (TaskbarOverlayContext leak, 2025) and fb2affac5f (LauncherWidgetHolder NPE, 2023) | OPENED because a search listed them: unrelated |
| "Destroy dep tree on RecentsView detached. This prevents leaks." (Launcher3) and a framework change "references to thumbnails so things can be freed ... Test: Go Recents, go home, take heapdump" (https://android.googlesource.com/platform/frameworks/base/+/3e966e2996bd) | search-summary hits, not opened |
| android11-release `IconRecentsView` | path answered 404: whether upstream fixed the Go leak later could not be read |

## 5. Ranked mechanisms for a never-cached stock process that grows slowly, and how to CONFIRM each on the tablet

### 5.0 Reading protocol (all read-only, run by the shell helper or over the cable; "A" to "D" are used below)
- **A** `dumpsys meminfo com.android.launcher3`: App Summary (Java Heap, Native Heap, Graphics, Private Other, System, TOTAL); the table rows `Dalvik Heap`, `Native Heap`, `Gfx dev`, `EGL mtrack`, `GL mtrack`, `Ashmem` with their Heap Size / Alloc / Free columns; the `Objects` block (Views, ViewRootImpl, AppContexts, Activities).
- **B** `dumpsys gfxinfo com.android.launcher3`: the HWUI cache lines ("Caches" / "Total memory usage" in third-party samples; Q's Skia field names may differ) and the "View hierarchy" counts (Total ViewRootImpl / Views / DisplayList).
- **C** `dumpsys activity processes` (the ProcessRecord block of the package: adj, process state, cached flag, `trimMemoryLevel`, `lastPss`; field names INFERRED) and `dumpsys activity activities` (the system's real ActivityRecords of the package, to be compared with `Objects: Activities` of A).
- **D** `dumpsys procstats --hours 24 com.android.launcher3` (the numbers of the Settings screen).
- **Protocol:** (1) force-stop the stock launcher once; enter a Quickstep session, open Recents once; take A + B + C = t0. (2) Do 10 plain Recents open / close cycles with NO setting change; A = t1. (3) Change ONE uiMode / font setting, open Recents once, repeat 5 times; A + C = t2. t0 -> t1 is "per open", t1 -> t2 is "per relaunch", and A's Graphics rows against B's cache totals separate "HWUI cache" from "driver / gralloc". Garbage of a just-destroyed activity can still be counted until the next GC: a count that stays raised across two readings with activity in between is a leak.

### 5.1 Ranking
| Rank | Mechanism | Evidence | Scales with |
| :- | :- | :- | :- |
| R1 | Go `IconRecentsView` listener pins every created `Launcher` / `RecentsActivity` instance (view tree, adapter, icons, hardware-bitmap thumbnails) for the life of the process (1.5, 3.3, 4.3) | VERIFIED code; magnitude INFERRED | stock activity CREATIONS (relaunch on uiMode / font / density / locale change at the next show, process-internal recreation), not Recents opens |
| R2 | Graphics-side memory (gralloc buffers of pinned thumbnails, vendor GL-driver memory) that is never trimmed because AMS keeps the process at foreground-like state (2.1-2.5) and `Debug.getPss` counts it (3.4) | partly VERIFIED, mapping INFERRED | surfaces / textures created (Recents opens, stock-home starts); bounded by what the driver keeps, so it plateaus |
| R3 | Java and native heap retention in the "jank perceptible" ART state: no background compaction, no `Heap::Trim`, no native purge (2.6) | VERIFIED ART code, mapping INFERRED | any allocation churn; amplifier of R1 / R2, creates no growth by itself |
| R4 | Task objects keep their icon / thumbnail beyond the 12 / 3 entry caches (`TaskListLoader` copy-over, 1.6) | VERIFIED code | list size, at most 6 rows + 1; bounded, small |
| R5 | Per-flip package-changed rebuilds (`PackageUpdatedTask`, `updateOverviewTargets`, 4.4) | VERIFIED by `CD2` facts | number of alias flips; replaced not added, bounded |
| R6 | MediaTek changes to the Go build (`MtkLauncher3GoIconRecents`) | UNKNOWN (sources not public) | unknown |
Not it (VERIFIED): a Recents open does not create an activity (singleTask: `onNewIntent`, 1.7); the HOME intent is a state reset (4.1); the caches are bounded (1.6); AMS never kills the process (2.1-2.2).

### 5.2 How to confirm or refute each
- **R1.** Expect in A: `Objects: Activities` and `Views` (and `AppContexts`) rise in steps at t2 (about +1 Activity and +100..140 Views per pinned instance, the per-activity numbers of `CD1` / `CD2` tables; `ViewRootImpl` may not rise because its parent link is cleared at detach, INFERRED), while C lists only 1-2 live ActivityRecords of the package: the difference IS the leak. Java Heap Private Dirty rises a few MB per step; Graphics rises if thumbnails are pinned. REFUTED if Activities stays at 1-2 after the 5 changes at t2. Sanity on magnitude: the emulator showed about +12-15 MB PSS for creating one stock `Launcher` activity (`CD1_EMULATOR`); a pinned instance probably weighs a few MB, so tens of instances would be needed for hundreds of MB. R1 alone fits a STEP pattern, not a flat plateau at 414-450 MB.
- **R2.** Expect the growth in A's `EGL mtrack` / `GL mtrack` / `Gfx dev` rows while `Objects` stay flat (t0 -> t1, no relaunch involved). In B the HWUI cache total should stay near or below the 3.2 cap (about 30 MB); a Graphics row of hundreds of MB then lies OUTSIDE HWUI = gralloc buffers or driver memory. In C the ProcessRecord should show adj 100 (VISIBLE) and no trim level above the RUNNING_* ones; `trimMemoryLevel` absent or 0 supports "never trimmed". A plateau at the observed 414-450 MB is the signature of a bounded, never-trimmed structure (R2 / R3), not of a leak (R1). Cheap experiment: `am send-trim-memory com.android.launcher3 HIDDEN` and `... RUNNING_CRITICAL` (the second reaches `RecentsModel.onTrimMemory`'s cache clear, 1.6) then A again: the delta says how much is purgeable by trims the process does receive on its own. INFERRED from a search summary of AMS `setProcessMemoryTrimLevel` (android10-release) and two issue reports: a COMPLETE / BACKGROUND / MODERATE request on a process in foreground-like state is refused ("Unable to set a background trim level on a foreground process"), HIDDEN is accepted. A refusal would itself confirm that AMS treats the process as foreground, which is R2's premise.
- **R3.** In A, the `Dalvik Heap` and `Native Heap` rows: Heap Free large against Heap Alloc (retained but unused) while PSS stays high; in C `curProcState` at or below IMPORTANT_FOREGROUND (INFERRED mapping to ART's "jank perceptible"). If Heap Free is small, R3 is not the explanation.
- **R4.** A: Java Heap and Graphics move with the number of Recents rows (open Recents with 1 task, then with 7 tasks): a step of a few MB, not growth over time.
- **R5.** Count the alias flips between t1 and t2 and compare A's TOTAL: a flat reading refutes it (the emulator showed flat counts).
- **R6.** Cannot be tested from outside. If R1-R5 are all refuted by the readings above and the growth persists, the cause is in the MediaTek build or in the driver (R2 mechanism).

## 6. What was NOT read or verified (so nobody re-derives it or trusts it)
- AMS `updateLowMemStateLocked` (the trim ladder), `ActivityThread.updateProcessState` / `handleTrimMemory` (only `scheduleTrimMemory` was read, in `core/java/android/app/ActivityThread.java`), `Activity.onTrimMemory`; the pages are cut at 86,267 characters by the fetch tool.
- `ActivityRecord.ensureActivityConfiguration` (the "relaunch when next visible" rule in 4.4), the rule that makes an implicit HOME intent a HOME-type task (only the `ActivityStackSupervisor` `isActivityTypeHome` block was read), `EglManager`, `SkiaOpenGLPipeline`, the consumers of `Debug.getPss` (AMS PSS collection, procstats, Settings), the scale of low-RAM `TaskSnapshot`s, Go `res/layout/launcher.xml` (so "the Go `Launcher` inflates an `IconRecentsView`" is INFERRED).
- MediaTek's changes (the `MtkLauncher3GoIconRecents` sources are not public) and every file after android10-release; android11-release `IconRecentsView` (404).
- `am send-trim-memory` rules: only a search summary of the AMS page plus two issue reports; the AMS body was not read.
- Unreachable from this session: GitHub mirrors (404 through the proxy), aospxref, androidxref (503), gitiles `+log` / `+blame` (403 / 401), Gerrit REST message searches (empty), gitlab.e.foundation (429), gerrit.omnirom.org (not resolvable). Their content appears here only as search-summary leads and is marked so.
- Line numbers: see section 0. Excerpts of sections 3 and 4 come from pages without a line gutter; only `CacheManager.cpp`, `UiFactory.java` and `GoRecentsActivityRootView.java` carried listing numbers, and those are the fetch model's own counts.
- Nothing in this report was MEASURED on the tablet. The three readings of 5.0 are what turns R1-R5 into a verdict.

## 7. What the readings would change (suggestions, not decided here)
- If R1 holds: the trigger is the number of stock activity creations, so the controllable side is how many configuration changes reach the stock activities while they exist (a uiMode flip while the stock `Launcher` / `RecentsActivity` instance is alive) and when the stock process is restarted for free (not inside a Quickstep session). A per-session recorder line `Activities= Views= Graphics=` from reading A at every Quickstep session start and end would show the staircase directly.
- If R2 / R3 hold: nothing in the stock code releases the memory in this state; only a process restart or a trim the AMS refuses to send does. The `HIDDEN` / `RUNNING_CRITICAL` experiment of 5.2 sizes what the trims the process can still receive are worth.
- If all are refuted: ask for the 5.0 readings plus `dumpsys SurfaceFlinger` (GLES line) before building anything.

