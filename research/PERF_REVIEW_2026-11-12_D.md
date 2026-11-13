# Performance review D: memory (TASKS BY1a), 2026-11-12

**Scope:** the app's own code (`app/src/main/java/com/tj/launcher/`), its hooks inside `launcher3/` (grep `com.tj.launcher` / `TjHooks`) and the manifest.
**Method:** read-only. No build, no test, no device. Tablet facts come from `research/tablet/2026-11-10-2/tablet-report.txt`, cited as `report:LINE`.
**Line numbers:** a snapshot of the working tree at about 20:45 UTC. The main session was editing `TjAccessibilityService.java` during this review, and its lines moved twice (by +2, then by +22), so a number may be off. Every finding also names the method; search for that.

## Bottom line

**No HIGH finding.** Nothing grows without bound and nothing leaks in normal use:
- every list, map and cache I found is bounded or cleared;
- every receiver and observer is unregistered;
- every static holder keeps the application context or a WeakReference.

The process is small: total PSS 69 MB, Java heap 5 of 128 MB (report:627). Most of the weight is native bitmap memory.

Three findings are worth doing:

| Finding | What | Resident memory | Share of the 69 MB PSS |
| :- | :- | :- | :- |
| D1 | the full wallpaper stays pinned in WallpaperManager's static cache after the wallpaper model has run once | 4.9 MB | about 7 % |
| D2 | the home curtain stays in memory in the custom launcher, where nothing can show it | 2.4 MB | about 3.4 % |
| D3 | every curtain capture copies the whole Launcher window, mostly only to compute a hash | 2.4 MB of churn per capture | none (transient) |

D1 and D2 together are about 10 % of the PSS. D4 to D8 are LOW.

**Tablet facts used:**

| Fact | Source |
| :- | :- |
| display 600 x 1024; system wallpaper bitmap 1200 x 1024, cover scale 1.0; power mode `hide_screenshot` | report:714, 691 |
| real-pixel lock crop calibrated, 150 x 163; lock-fit model in use ("the wallpaper model is used meanwhile") | report:689, 677 |
| patch window [474,538][600,674], i.e. 126 x 136; curtain picture 600 x 1024 saved and shown | report:852, 669-670 |
| shell helper not reachable; 2 process starts in 24 h | report:620, 630 |

**Arithmetic:** ARGB_8888 uses 4 B per pixel, ALPHA_8 uses 1 B per pixel. Since Android 8, bitmap pixels live in the native heap, which counts in PSS.

## Findings

### D1 MEDIUM: the full wallpaper stays pinned in WallpaperManager's process-wide cache
- **Where:**
  - `loadWallpaper()`, TjAccessibilityService.java:1395-1408: `getDrawable()` at 1401; the BitmapDrawable's bitmap is kept at 1402.
  - Reached from `loadLockWallpaper()` at 1379 when there is no separate lock picture.
  - Reached from `patchTarget()` at 1227 (lock screen) when no real-pixel crop is stored for this rotation and signature.
  - Reached from `patchTarget()` at 1239 (power mode "wallpaper", which the tablet does not use: report:691).
- **When it loads:** the crop's signature contains the wallpaper id (PatchShot.java:192-195, checked at 171-175). So the model, and with it `getDrawable()`, runs:
  - after every wallpaper change, until the next screenshot calibration;
  - at the first lock in a rotation that has no crop;
  - after a crop is invalidated (`addPatch`, about line 1444).
- **What Android does with it:** Android 10 keeps that bitmap in a process-static field (`WallpaperManager.Globals.mCachedWallpaper`) until the wallpaper changes or `forgetLoadedWallpaper()` is called. This is AOSP behaviour from memory and is unverified in this repo.
- **What the code frees:** only its own references (`refreshLayers` 1419-1420, `onTrimMemory` 3500, `wallpaperReceiver` 568). The comment at 3485-3490 already says that this frees little. Nothing calls `forgetLoadedWallpaper()`: a grep of all `*.java` has no hit.
- **How often:** once per process per wallpaper. After that it is held for the rest of the process's life, even after a calibration has made the model unnecessary (1227 then never asks for it again).
- **Cost:**
  - 1200 x 1024 x 4 = 4,915,200 B, about 7 % of the 69 MB PSS.
  - While the model draws it (1512), the GPU also holds a texture of the same size. Whether the MediaTek driver counts that texture in the process is unverified.
  - Whether it is resident on the tablet right now is unverified: report:714 shows the model path ran at some point, and report:689 shows a crop is stored.
- **Change:**
  - (a) In `refreshLayers()`, call `WallpaperManager.getInstance(this).forgetLoadedWallpaper()` once, when all three hold:
    - the tablet is unlocked;
    - the power mode is not "wallpaper";
    - `PatchShot.has(this, rotation(), shotSig())` is true.

    Track "once" with a boolean that `loadWallpaper()` sets when it used `getDrawable()` and that this call clears. The next lock reads the stored crop (1227), so no decode is added.
  - (b) Also call it in `onTrimMemory` (3500) when `patchView == null`. Under real memory pressure, reading the wallpaper again later is the right trade.
  - Optional, bigger: in the model state keep only the part of the wallpaper the patch window can show:
    - the window plus a 16 px margin: (126+32) x (136+32) x 4 = 106,176 B;
    - forget the full picture;
    - the draw at 1512 adds the crop's offset;
    - `maybeCalibrateLock` (1288) then decodes its own full copy on its worker thread. It needs the helper, which is unreachable on the tablet (report:620).
- **Risk:**
  - (a) leaves the look unchanged, because the crop path never reads the model.
  - When the model is needed again (a crop invalidated, a new rotation), that lock pays one main-thread decode. Today that costs nothing only because of the pinned 4.9 MB.
  - The optional crop needs a margin of at least 2 px, or the bilinear filter at its edge differs.
- **Verify:**
  - a log line `WALLPAPER cache forgotten (crop stored)`;
  - on the tablet, `dumpsys meminfo com.tj.launcher` Native Heap after a wallpaper change and a calibration, before and after one lock and unlock (expected about -4.7 MB);
  - emulator sections `pt` / `fp` (lock patch pixels) unchanged.

### D2 MEDIUM: the 2.4 MB home curtain is kept in memory while nothing can show it
- **Where:** HomeCurtain.java:184 (`private static volatile Entry cache`), filled by `preload` (211-223, PNG decode at 217) and `setCache` (204-206).
- **What fills it** (TjAccessibilityService.java):
  - 320: every service connect;
  - 2157: every unchanged capture;
  - 2171: every changed capture;
  - 2052 and 2219: a cover found nothing in memory.
- **What empties it:** only `onTrimMemory` at RUNNING_LOW, CRITICAL or BACKGROUND (3499), and `invalidateAll` (HomeCurtain.java:239). Unbind and destroy do not (3482-3483, 3505).
- **Who uses it:** only `showResetCover()` (2206, read at 2218), which runs:
  - from `coverAtEnter()` at 1886, which needs a current Quickstep session (ProfileSession.java:53-55, which calls 48-50);
  - from `ReturnReset.run` (ReturnReset.java:222), whose callers are:
    - a plain unlock that ends a Quickstep session (1939);
    - the first unlock after a boot (1948);
    - the reconcile of a Quickstep session (1973);
    - the double-Home return and the Home-press reconcile, both only from Quickstep (RouterActivity.java:65-75, 96);
    - `ProfileSession.returnToCustom` (127-135, which returns at once in custom mode at 129);
    - the "Switch to the custom launcher" button (ControlActivity.java:121-125).

  So in the custom launcher, with no boot clean pending, the bitmap waits for the next Quickstep session. That session always starts at least seconds before it can end.
- **Cost:**
  - 600 x 1024 x 4 = 2,457,600 B (report:669 confirms 600 x 1024), about 3.4 % of the PSS, for the whole time the custom launcher is in use.
  - A changed capture also keeps the replaced entry alive until the next GC. `setCache` does not recycle it, on purpose (HomeCurtain.java:203).
- **Change:**
  - One pure rule, `HomeCurtainRules.keepInMemory(switchOn, customMode, bootCleanPending)` = `switchOn && (!customMode || bootCleanPending)`, with a JVM test. Do not include "automation paused": a double Home in a paused Quickstep session still runs the reset.
  - Preload on the worker, as today, when the rule turns true:
    - at every lock in `lockPrepare`, next to the similar condition at 1821;
    - when the mode is set to Quickstep (1751, 1918, ControlActivity.java:388, ProfileSession.java:141, CrashGuard.java:65), or once inside `Mode.set`;
    - at connect (320), only if the rule holds;
    - in `ControlActivity.onResume`, for the rare button press in custom mode.
  - Drop it (null the entry, never recycle it) when the rule turns false:
    - in `hideResetCover()` (2257), after the cover window is gone (the cover's ImageView keeps its own reference while it is shown, 2223);
    - at 2157 / 2171, where a capture saves the PNG but does not cache it (captures run only in custom mode, HomeCurtainRules.java:67);
    - at unbind and destroy.
- **Risk:** a missed preload means the plain black cover once. That is the existing fallback (2218-2229), which also starts a preload for the next time, so it is never worse than the black cover. Every path that has a preload before it keeps the same look.
- **Verify:**
  - the JVM test of `keepInMemory`;
  - emulator section `cu` stays green after a Quickstep session (log `RESET cover shown (curtain)`, about 2251; the MAE check);
  - a new log line `CURTAIN dropped from memory (custom mode)`, and `HomeCurtain.cachedRot() == -1` in custom mode.
- **Not recommended now:** decoding the curtain as `Bitmap.Config.HARDWARE` would avoid the second copy (the texture) and its upload at the Enter tap. But the emulator's GL path has a history of native crashes (CLAUDE.md, v0.9.0 gate lessons).

### D3 MEDIUM: every curtain capture copies the whole Launcher window only to hash 1/12 of it
- **Where:**
  - TjAccessibilityService.java:2137 calls HomeLayer.java:33 (`createBitmap(src.width(), src.height(), ARGB_8888)`), then PixelCopy (34).
  - The copy is hashed on a worker at 2150-2151, sampling every 4th row and every 3rd column (HomeCurtainRules.java:94-105).
- **How often:**
  - `homeDrew()` (961-962) calls `scheduleCurtain()` (2098-2105) at every Launcher draw.
  - The capture runs once the home has been quiet for 1.5 s, at most once per 15 s (HomeCurtainRules.java:56, 65-79). In practice that is about once after every visit to the home screen.
  - In the usual "unchanged" result (2154-2159) the copy is thrown away (2178).
- **Cost:**
  - Each capture allocates 600 x 1024 x 4 = 2,457,600 B and does a full-window GPU readback.
  - The hash reads only 256 x 200 = 51,200 of the 614,400 pixels.
  - On a "changed" result the peak is about 7.4 MB: the layer, the decoded wallpaper picture (`HomeCurtain.loadWall` at 2163, 600 x 1024 x 4), and the old cache entry until GC. D2 removes the old cache entry.
  - It is not HIGH: it runs at most once per 15 s and is freed at once (2178).
- **Change:**
  - First hash a scaled copy. PixelCopy into a 150 x 256 bitmap = 153,600 B; Android's documentation says PixelCopy scales the source to the destination bitmap, but this was not tried here. Hash every pixel of it.
  - Store that hash in the meta (`HomeCurtainRules.metaString`, 126).
  - Only when it differs, take the full-size copy and compose as today.
  - The first capture after the update re-makes the picture once, because the hash definition changed.
- **Risk:**
  - If the GPU scaling is not bit-exact for identical content (unverified), every capture would count as "changed", with a PNG write each time. The `cu` run must show "unchanged since the last picture" on the second quiet capture.
  - The two copies are a few ms apart; if the home changed in between, the next capture simply re-makes the picture.
  - A change smaller than about 4 px can be missed, as the 4 x 3 sampling can already miss it today.
- **Verify:**
  - a JVM test of the decision (small hash equal means no full copy);
  - emulator `cu` and its log lines `CURTAIN unchanged ...` / `picture saved`;
  - `curtainInfo()` in Launcher control.

### D4 LOW: the patch masks are ARGB_8888 where ALPHA_8 draws the same
- **Where:** `alphaBitmap()` at TjAccessibilityService.java:1577-1581 goes byte[], then int[w*h], then an ARGB_8888 bitmap. Two masks per patch window (1572-1573) are built at every creation of a lock or home patch window (`addPatch` 1442). The mask paint uses only the source alpha (DST_IN, 1548-1552).
- **Cost** per window, for the tablet's 126 x 136 window (17,136 px):
  - retained: 2 x 68,544 = 137,088 B (ALPHA_8 would be 34,272 B);
  - plus 137,088 B of int[] garbage on the Java heap at each creation;
  - at the 500 x 500 cap (1564): 2,000,000 B retained.
- **Change:** use `Bitmap.createBitmap(w, h, ALPHA_8)` and `copyPixelsFromBuffer(ByteBuffer.wrap(a))` when `getRowBytes() == w` (otherwise fill row by row). No int[]. The drawing call stays the same.
- **Risk:** an A8 bitmap is drawn as the paint's colour times its alpha. DST_IN reads only the alpha, so the pixels should be identical; this is unverified on the MediaTek GPU.
- **Verify:** the emulator `pt` / `fp` / `hp` pixel checks of the edge fringe (`pb_probe`) are unchanged. A JVM test cannot create the bitmap against android.jar.

### D5 LOW: the "Choose the apps" screen loads every icon at once and never reuses rows
- **Where:** HiddenAppsActivity.java:55-61 calls `ri.loadIcon(pm)` for every launchable app in onCreate. `getView` (90-124) builds a new row each time and ignores the recycled view `v`.
- **How often:** only when Tj opens this screen, and only in Quickstep mode (52).
- **Cost:**
  - N icons of about 10 to 110 KB each, held until the screen closes:
    - a legacy icon is about 51 x 51 x 4 = 10,404 B;
    - an adaptive icon has two layers of about 115 x 115 x 4 = 52,900 B each.

    N and the icon types are unverified.
  - Scrolling allocates new rows.
- **Change:** keep the ResolveInfo, load the icon in `getView` when the row is first shown (cache it in the Row), and reuse `v` when it is a row.
- **Risk:** an icon appears one frame late while scrolling.
- **Verify:** emulator section `o` (O2 picture) and `mu` (MU-12) still pass.

### D6 LOW: four idle background threads live for the whole process
- **Where:** LockLog.java:51 (executor), NotifSightings.java:113 (executor), AnimationSpeedup.java:157 (HandlerThread `tj-anim`), SettingsSpoof.java:83 (HandlerThread `TjSsSync`, started at every process start through TjApplication.java:24).
- **Cost:** each idle thread keeps its touched stack pages and its native thread state, tens of KB each (unverified). The 53 `new Thread(` sites in 23 files (grep) end with their job, so they cost start-up time, not memory.
- **Change:** let LockLog and NotifSightings share ONE single-thread executor; both only append to files, and each keeps its own order. Leave `tj-anim` and `TjSsSync` alone: `TjSsSync` makes package-manager calls, and `tj-anim` must not wait behind them.
- **Risk:** a NotifSightings write can delay a LockLog append (both are short).
- **Verify:** the LockLog / NotifSightings JVM tests; the lock log keeps every line in order (emulator `nq`).

### D7 LOW: a new SimpleDateFormat for every log line and status string
- **Where:** LockLog.java:27 (every lock-log line), TjAccessibilityService.java:1740 (`now()`), 2074, 2181 and more. `SimpleDateFormat(` appears in 19 files.
- **Cost:** each construction builds a Calendar and parses the pattern, a few KB of short-lived objects (size unverified). It happens at every `LockLog.add`, dozens of times per lock cycle.
- **Change:** one formatter per pattern per thread (ThreadLocal). Call `setTimeZone(TimeZone.getDefault())` before each use, so a time-zone change is still seen.
- **Risk:** a formatter shared between two threads produces wrong text, which is why it must be ThreadLocal.
- **Verify:** the LockLog JVM tests (line format).

### D8 LOW: HiddenApps copies the allowed set at every question
- **Where:** HiddenApps.java:39 (`new HashSet<>(sp.getStringSet(...))`), used by `isShown` (63-71) and `isPackageShown` (73-78). The callers:
  - AppFilter.java:18-21, for every app component at every Launcher3 load;
  - NotifSightings (about line 138), for every notification;
  - the Recents sweep.
- **Cost:** one HashSet copy of the allowed set per call, a few hundred bytes to a few KB of garbage.
- **Change:** a read-only path for these two questions that reads `sp.getStringSet(...)` directly. SharedPreferences forbids changing the returned set, and these questions only read it. `setShown` and `autoAllowIfDue` keep the copy.
- **Risk:** a future caller that changes the set. None does today (AppVisibility.isShown only reads; not checked line by line).
- **Verify:** the AppVisibility JVM tests; emulator `mu` / `ss`.

## Outside this slice (pointers for A / B / C, not judged here)
- **B:** `rotation()` (285) and `realMetrics()` / getRealMetrics are binder calls on Android 10 (DisplayManagerGlobal caching is off there; unverified). They run per event through `cachedCard()` (288).
- **B:** TjAccessibilityService.java has 13 `findAccessibilityNodeInfosBy*` call sites, and not every returned node is recycled. That is GC churn, not a leak (unverified per site).
- **C:** `RecentsGuard.describeAsTarget` (RecentsGuard.java:53-73) runs at every Launcher resume (TjHooks.java:89) and calls `setTaskDescription` with an icon. Android 10 writes such an icon to disk in system_server (unverified).
- **A:** `HiddenApps.setShown` commits on the main thread and reloads the Launcher per tap (HiddenApps.java:80-88, HiddenAppsActivity.java:113-121).

## Checked and FINE (no action)

**Manifest and processes**
- largeHeap is false on the tablet: AndroidManifest.xml:79 points to launcher3/res/values/config.xml:3. `values-sw720dp` sets true, but the tablet is about 564 dp wide (600 px at 170 dpi).
- The `:wallpaper_picker` process starts only when the picker is opened: AndroidManifest.xml:109-116.
- `:wallpaper_chooser` is not used on API 29: WallpaperManagerCompat.java:35-43 takes the VOMR1 path on API 27 and later.
- The NotificationListener is declared (AndroidManifest.xml:124-132) but never bound on Go.
- KeepAliveService holds no state and is off on the tablet: AndroidManifest.xml:340-344, report:622.

**Leaks and lifecycles**
- The service's static `instance` is cleared at unbind and destroy: TjAccessibilityService.java:34, 3482, 3505.
- The wallpaper receiver and the preference listeners are unregistered: TjAccessibilityService.java:587-602.
- Posted patch, curtain and calibration runnables are removed at teardown: TjAccessibilityService.java:3468-3478, 3480, `curtainStop` 2091.
- TjHooks holds the Launcher by WeakReference: TjHooks.java:18-27.
- Static observers keep the application context: BatterySaverGuard.java:38-61, EntranceWatcher.java:45-61, SettingsSpoof.java:74.
- SetupActivity's worker threads hold the Activity only until their job ends, with no delayed posts: SetupActivity.java:68, 241-364.

**Bounded collections**
- launchableCache is cleared above 64 (TjAccessibilityService.java:3150), probeSamples is capped at 25 (3269), and NotifSightings LAST_LOGGED is cleared above 60 (NotifSightings.java:146).
- LockLog is a 48 KB file ring, not memory: LockLog.java:17-18.

**Bitmaps**
- The lock patch's real-pixel crop is small: 150 x 163 x 4 = 97,800 B (report:689). The home composite is of the same order (`startHomeCopy` 1033-1048).
- `lockBmp` is dropped when unlocked (1420), and `removePatch()` drops every patch bitmap (1583-1588).
- Rare full-screen decodes recycle at once:
  - `ingestShot` (4970);
  - `homeShotObserved` (1156);
  - `scrimIngest` (4856; int[W*H] at 4879);
  - `maybeCalibrateLock` (1288; int[W*H] at 1319), which needs the helper (report:620).
- RecentsGuard's cached task icon is 48 dp at 170 dpi, 51 x 51 x 4 = 10,404 B: RecentsGuard.java:46-47, 53-73.
- These bitmaps must stay ARGB_8888: the curtain and the crops (RGB_565 would band the wallpaper gradients), and the Launcher layer, which needs alpha (HomeLayer.java:55-61).

**Drawing**
- Fade filters are cached: `setDim` keeps one per brightness (1539-1546), `setOpenLook` one per 1/255 step (4650). A new filter is made per frame only during a 300 ms fade.
- The patch's `onDraw` allocates nothing per frame except those filters (1449-1513). Its `saveLayer` is a pooled GPU layer of the window's size.
- SsDonutView.java:110-146 and SsConversation.java:137-151 allocate in onDraw, mirroring the stock views. The pages are static and rarely redrawn.

**Look-alike Settings pages (Ss\*)**
- Live items start in onStart and stop in onStop, and dialogs are dismissed in onDestroy: SsPageActivity.java:57-83.
- Observers and battery receivers are paired (SsWatcher.java:28-35, SsPagesBattery.java:119/126, SsHomeSummary.java:46/54); the sound threads quit (SsPagesSound.java:175-177, 205-208, 520-529, 619-629); the static snapshots are tiny (SsPagesStorage.java:72-73, SsBatteryStats.java:122-140).

**Launcher3 and memory pressure**
- AppFilter filters apps before Launcher3 loads their icons: AppFilter.java:18-21 (the AOSP AllAppsList order is unverified).
- The Go variant of Launcher3 is compiled (app/build.gradle.kts:57-70), so there is no Quickstep thumbnail cache.
- `onTrimMemory` ignores UI_HIDDEN, which fires at every app switch, and reacts only to real pressure: TjAccessibilityService.java:3491-3502.

**Not audited:** ShellServer / ShellHelper (the helper is unreachable on the tablet, report:620), the `eleven/` module, and `phoneguide/`.
