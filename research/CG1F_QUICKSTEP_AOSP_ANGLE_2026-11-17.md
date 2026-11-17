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

<!-- CG1F-Q2 -->
