# Performance review, slice B: the accessibility service and its overlay windows (2026-11-12)

Scope: app/src/main/java/com/tj/launcher/TjAccessibilityService.java ("TAS" below; all of it read), app/src/main/res/xml/tj_accessibility.xml, and the helpers on its event path (EventNoise, Features, Mode, PowerMenuRule, PatchShot, PickerGuard, AnimationSpeedup, LockNumber, LockLog, AutoCalibration, CrashGuard, HomeCurtainRules, UnlockMethods, ReturnMethods, FailsafeKeys). Read-only; nothing was built or run. For PatchRest, PatchFade, HomePatchRules, ScrimFit, ScrimMeasure, PencilRules, HomeLayer and HomeShot only the call sites were checked.

Line numbers: TAS as of commit 809fab9a (5094 lines; it includes the BY1b probe at TAS:62-64). The main session is still editing TAS, so a later edit can shift them.

Facts behind the severities:
- The service gets content-change events from EVERY app, with no throttle (tj_accessibility.xml:7, :12). Every callback runs on the Launcher's main thread (TAS:38).
- Measured on Tj's tablet, 121 minutes (research/tablet/2026-11-10-2/tablet-report.txt:629):
  - 45,521 events, of which 38,643 were dropped as noise;
  - 984 power-menu scans, avg 28 ms, max 563 ms;
  - 834 pencil tree searches, avg 33 ms, max 1305 ms.
  - Together that is about 55 s of main-thread blocking, in bursts.
- Other reports: a 963 ms scan (research/tablet/2026-11-10/tablet-report.txt:629) and a 514 ms pencil search (research/tablet/2026-11-09/tablet-report.txt:628).
- A tree search is a binder call answered by the TARGET window's UI thread. That is SystemUI's UI thread for the shade, the PIN pad and the power menu, and the foreground app's for its own window. The Launcher's thread waits for the answer.
- The PIN-pad scan has no counter, so its cost on the tablet is unknown.

## Findings

**B1 HIGH: the power-menu scan searches every window, the foreground app's included, after any window event or click**
- Where:
  - TAS:89 sets `fullScanUntilMs = now + 300` on every window event.
  - TAS:107-117 runs `evaluate()` at once and again 80 ms later.
  - For each window, TAS:3539-3573 calls `getRoot`, then searches the text for "Power off" and "Screenshot" (3547-3548).
  - EventNoise.java:59-62: right after a window change, every window is searched.
  - EventNoise.java:31: a click in any app also starts a scan.
- How often: every window-state or windows-changed event and every click, from any app, in the custom world (custom mode, or locked). That covers app starts, activity changes, dialogs, the keyboard and returning home. Measured: 984 scans in 121 min.
- Cost:
  - Each window costs two whole-tree text searches, plus `getRoot` with its descendant prefetch.
  - An app that is starting answers late, so the Launcher's thread waits behind that start: avg 28 ms, max 0.5 to 1 s.
  - The Launcher's own window is fetched too. It is skipped only after its `getRoot` (TAS:3543-3545).
  - The foreground app's UI thread walks its own tree twice for every scan.
  - Possible symptom, link unverified: in the 2026-11-10 report, the home fade's first frame came 135 ms late.
- Change:
  1. In `EventNoise.mayHoldPowerMenu`, return `isSystemWindow(type)` in the window-change branch too. The stock menu is SystemUI's `global_actions_view` (2026-11-10-2 report:820-845). In AOSP 10 that dialog is a TYPE_VOLUME_OVERLAY system window (from memory; unverified on the MediaTek build).
  2. Skip type 4 (TYPE_ACCESSIBILITY_OVERLAY, always ours) before calling `getRoot`.
  3. Start a scan on a click only when the click comes from SystemUI or "android", or when the menu is already up.
  4. Optional: on a window event, scan only when a system window id appears that the last scan had not seen. Every opening of the menu is a new dialog window.
- Function kept: the fast path (TAS:126-152) is untouched, and the menu is still found in SystemUI's windows.
- Risk: on a ROM whose power menu is an application-type window, only the fast path would catch it.
- Verify:
  - EventNoise JVM test: a window change plus TYPE_APPLICATION gives false.
  - Emulator sections pt, hp, hf and v.
  - The tablet's perfLine "power-menu scans avg/max" should drop sharply.

**B2 HIGH: a pencil whole-tree search on every SystemUI event, even when the shade is closed**
- Where: `schedulePencil` TAS:2334-2357, `penEval` TAS:2325-2331, `findPencilFull` TAS:2466-2492, EventNoise.java:36.
- How often: every SystemUI event in the custom world while no pencil is known. That includes clock, battery, Wi-Fi and notification icon updates, and the whole lock screen.
  - The first search runs at once: the gap is 200 ms for content changes and 40 ms for window events.
  - `penEval` then waits for the gap to end and searches again, so one isolated event costs up to two searches.
- Measured: 834 searches, avg 33 ms, max 1305 ms. The code's own comment (TAS:2454) says a whole-tree search takes 0.5 to 1.3 s on the tablet.
- Cost:
  - `getWindows`, then `getRoot` of every system window, including our own overlays and the navigation bar (TAS:2470-2471).
  - Then `findAccessibilityNodeInfosByViewId("android:id/edit")` (TAS:2476). It walks SystemUI's whole status-bar tree on the same thread that handles quick-settings taps.
- Change:
  1. Before `getRoot`, skip type-4 windows and any window whose `getBoundsInScreen` height is under about a third of the display. In AOSP 10 the status-bar window is full height whenever the panel is expanded or the keyguard or a heads-up is showing (from memory; unverified on MediaTek).
  2. Do not re-arm `penEval` when no tall SystemUI window exists.
- Function kept: an expanded panel is full height, so the pencil is still found. The lock screen is unchanged.
- Risk: a ROM that draws quick settings in a short window (none known).
- Verify: emulator section b6, and the pencil counters in perfLine.

**B3 HIGH (size unmeasured): the PIN-pad check runs up to 22 tree searches on lock-screen events**
- Where:
  - `bouncerShown` TAS:2887-2907; the 6 view ids and 5 texts it looks for are at TAS:2883-2884.
  - Callers:
    - every event while locked, through `scheduleLockUpdate` (TAS:2862-2867): at most every 60 ms, plus a trailing call at 120 ms;
    - `lockReceiver`, at screen on, screen off and unlock: at once and again at +400 ms (TAS:2813);
    - the settle, touch and pad returns (TAS:425, 429, 438).
- Cost:
  - `getRoot` is called on every window before the package check (TAS:2891-2895).
  - Then up to 11 searches run for each SystemUI window.
  - The navigation bar belongs to SystemUI and never holds the pad. So when the pad is NOT up, which is the usual state, all 22 searches run.
  - SystemUI's UI thread answers them while it also handles the PIN taps, and the Launcher's thread waits. This plausibly lags PIN entry and the unlock.
  - perfLine (TAS:3491-3497) has no counter for it.
- Change:
  1. Add a count, an average and a maximum for this check to perfLine.
  2. Before `getRoot`, skip windows of type 1, 2 or 4, and windows shorter than about a third of the display.
  3. Remember which id or text matched on this device and try it first. If the learned id is present but not visible, answer "no pad" after that one search. Keep the full 11-search fallback.
- Risk: after a change of lock type, the full fallback runs once.
- Verify: the emulator PIN-pad sections, and the tablet's lock-log line "PIN pad sign ... ms after the finger" (136 to 203 ms today, 2026-11-10-2 report:751-803).

**B4 MEDIUM: the lock-number check makes two binder calls on every event, even while unlocked**
- Where:
  - The early return at TAS:2863 never fires, because `LockNumber.enabled` defaults to true (LockNumber.java:63).
  - TAS:2872-2875 then calls `isInteractive` and `isKeyguardLocked`.
- How often: every event that is not noise and not inside a quiet burst, everywhere (home screen and apps). That is up to once every 60 ms, plus a trailing post.
- Change: `if (lockView == null && !keyguardLockedFast()) return;`. At screen on, screen off and unlock, `lockReceiver` already resets the keyguard memo and posts `lockEval` itself (TAS:2808-2813).
- Risk: none known.
- Verify: the emulator lock-number check ("LOCKNUM shown").

**B5 MEDIUM: `refreshLayers` re-reads display, wallpaper and keyguard facts over binder on every burst of events**
- Where: TAS:598-603 runs it at most every 150 ms, plus a trailing call at 250 ms, whenever a patch exists or the screen is locked. Then `patchTarget` (TAS:1198-1219) and TAS:1402.
- Cost on the lock screen: about 9 or 10 binder calls and one file stat per refresh:
  - `keyguardLocked` (TAS:1200);
  - `rotation()` through `cachedCard` (TAS:1203, 285-294);
  - `PatchShot.has` / `currentSig` (TAS:1205; PatchShot.java:171-176 and :192-203);
  - `getRealMetrics` and `itemKey` (TAS:1228, 524);
  - `rotation()` again (TAS:1402).
- Cost on the home screen with a patch: about 7 binder calls (`homeFacts`, TAS:663-678).
- Extra waste:
  - When nothing is wanted, `warmUp` calls `ensurePatch`, which runs `patchTarget` a second time (TAS:1399, 5028, 1410-1414).
  - Each refresh on the lock screen re-arms `calTrigger` (TAS:1404).
- Change:
  1. Cache rotation and the real display metrics. Refresh them from a `DisplayManager.DisplayListener`, which also sees a 180-degree turn, and from `onConfigurationChanged`.
  2. Cache `shotSig` the way `homeWallId` is already cached (TAS:645-651), and clear it in `wallpaperReceiver` (TAS:547).
  3. Pass the "nothing wanted" result into `warmUp` instead of working it out again.
  4. Keep `keyguardLocked` fresh; the AM1 comment at TAS:503 explains why.
- Risk: a stale cache, but the display listener covers every change that matters.
- Verify: a debug counter of binder calls per refresh, and the emulator patch sections.

**B6 MEDIUM: synchronous `commit()` writes on the unlock path**
- Where:
  - `appendReport` (TAS:4034-4037) commits. It is called by `settleHome` right after the cover goes (TAS:2260), and from ReturnReset.java:215 and :455.
  - `finishReset` commits too (TAS:4020).
  - `patchStatus` (TAS:608-610) commits whenever the text changes. The text flips between "lock screen: patch window wanted at ..." (TAS:1207) and "active over ..." (TAS:1510) at every lock and every unlock, so each flip is a real disk write.
- Cost: a file write plus fsync on the main thread, right when the first frames after the unlock are due (size unverified on this tablet). The project has already moved other unlock-path writes to `apply()` (TAS:3004, 3029). LockLog is already asynchronous (LockLog.java:51-65).
- Change: use `Features.setStringAsync` for `RESET_REPORT_KEY` and `PATCH_STATUS_KEY`.
- Risk: an emulator step that reads the preferences file immediately afterwards (unverified).
- Verify: StrictMode disk-write detection in a debug build, and the UNLOCKSUM line (TAS:3070-3088).
- Left as it is: the two `setString` calls on the menu-found path (TAS:3581-3582). A commit with an unchanged value skips the disk write (platform behaviour, from memory), and the comment there wants the value stored before the card is built.

**B7 MEDIUM: the home-patch window is built at the same moment as the Launcher's first frame after an unlock**
- Where:
  - TjHooks.java:87: `onLauncherResumed` calls `refreshLayersSoon`.
  - USER_PRESENT does the same at once and again at +400 ms (TAS:2812, 2834).
  - Together they remove the lock patch and add the home patch (`addPatch`, TAS:1416-1512):
    - `addView` and the masks;
    - `loadHomeWall` (TAS:907-933), which decodes a PNG on the main thread;
    - `reloadScrim`, a binder call.
- Ordering against the Launcher's first frame: unverified.
- Nothing needs this window so early, because the patch cannot rest until its composite is built after the quiet time (TAS:974-1005).
- Change: on the unlocked home screen, create the patch after the Launcher's first frame since USER_PRESENT (`homeDrew` / `sumNoteDraw`, TAS:939-941 and 3063), or at least post it behind one frame. Keep pre-creating the lock patch while the screen is dark (TAS:1800).
- Risk: a side-key press within about 0.5 s of the unlock shows the card. That already happens today while the patch is not fresh (`cardWanted`, TAS:481-486).
- Verify: UNLOCKSUM "first frame +N ms" before and after (emulator sections cs and uf, and the tablet report).

**B8 MEDIUM: the wallpaper-picker refresh runs inside a random event every 10 minutes**
- Where:
  - PickerGuard.java:91 checks `PKG_TTL_MS`, which is 10 minutes (line 38).
  - `refreshPackage` (PickerGuard.java:123-137) then runs:
    - `WallpaperPickerChoice.resolve` (WallpaperPickerChoice.java:113-136);
    - `themeBackground` (PickerGuard.java:140 onward): `getActivityInfo`, `createPackageContext` and theme resolution.
  - It is reached from TAS:79 on every event, noise included.
- Cost: tens of ms (unverified). It can land inside a power-menu burst, because TAS:79 runs after `fastMenuOpen` (TAS:74) but before the dialog's event is handled.
- Change: refresh on a worker thread at connect and then every 10 minutes (or on a package change). Work out the colour only when a cover is about to be shown.
- Verify: the emulator picker test knob (PickerGuard.java:129-134).

**B9 LOW: two keyguard binder calls per Back or volume key, with the key filter on by default**
- Where: TAS:1668-1669. Only Back and volume keys are looked at (comment on TAS:1668).
- The filter is on by default: TAS:1698-1699, FailsafeKeys.java:26, ReturnMethods.java:23 and :49.
- Cost: AOSP holds a filtered key until the service answers (timeout about 0.5 s, from memory). A Back press in any app therefore waits for our main thread and for whatever scan is running on it.
- Change: read `keyguardLocked()` once into a local. Fixing B1 to B3 also removes the stalls that sit in front of the key.
- Keep the filter: removing it would lose the Konami code and the volume codes.

**B10 LOW: `isForeground` for our own package fetches over binder every 40 ms during `settleHome`**
- Where: TAS:4026-4032, called at TAS:2257 and 1971.
- Change: for our own package, use state the process already has (`TjHooks.launcher()` resumed / `hasWindowFocus()`). Our overlays are not focusable, so the answer means the same. Keep the binder path for other packages.
- Verify: emulator sections cs and cs2, "custom home in front +N ms".

**B11 LOW: AccessibilityNodeInfo objects are not recycled on hot paths**
- Where: TAS:3547-3548, 3552-3567, 2896-2903, 2476-2488, 1631-1633, 3817, 4088.
- Cost: on API 29 these become GC garbage after every scan.
- Change: recycle each node in a `finally` after its last use.
- Risk: none.

**B12 LOW: the curtain polls every 1.5 s instead of waiting out its 15 s gap**
- Where: TAS:2099-2106 retries up to 12 times, each try with binder calls (TAS:2095). The 15 s gap is HomeCurtainRules.java:56.
- Change: post the next look for the end of the gap. The copy itself is reviewer D's slice.

**B13 LOW: `gearKeyNow` makes two binder calls on every follow**
- Where: TAS:2612-2619 (`getRealMetrics` + `getRotation`), called from every `gearFollow` (TAS:2632).
- Change: use the cached display facts from B5.

**B14 LOW: a new SimpleDateFormat for every timestamp**
- Where: TAS:1718, 498, 1099, 2052, 2622.
- Change: one shared formatter for the main thread.

**B15 LOW: automatic calibration re-checks every 5 s while locked, the screen-off time included**
- Where: TAS:4572 re-posts for any reason that starts with "waiting", and "waiting: the screen is off" is one of them (AutoCalibration.java:51).
- Each check costs about six binder calls (TAS:4553-4560).
- It happens only while no valid crop exists, because TAS:4568 returns first once one does. Handler delays count uptime, so they do not wake a CPU that is asleep (platform fact, from memory).
- Change: do not re-post while `!st.interactive`. SCREEN_ON already triggers a fresh look, as the scrim trigger does (TAS:4699-4700); confirm SCREEN_ON reaches `maybeAutoCalibrate` (unverified).

## Considered and rejected
- `notificationTimeout > 0`: it would delay the dialog's own event, which the fast path (TAS:126-152) is anchored on.
- `android:packageNames`: it would drop other apps' window events, which TAS:77-80 and 97 need.

## FINE
- Notification events return first: TAS:69-72.
- About 85 % of events are dropped as noise before any heavy work: TAS:83-87, EventNoise.java:19-22.
- Shade content changes start no power-menu scan: TAS:111, EventNoise.java:30-33.
- The fast open path makes no binder call and defers its heavy work: TAS:126-152, 169-221.
- Touchable-flag changes wait until the fade is over: TAS:4486-4507.
- Paint colour filters are cached: TAS:1517-1524, 4622-4639.
- Frames are requested only while fading: TAS:4228-4233, 4374.
- The keyguard memo is reset by the lock receiver: TAS:1608-1615, 2808.
- Preference handles are cached: Features.java:19-25, Mode.java:47-53.
- `AnimationSpeedup.tick` acts at most twice a second: AnimationSpeedup.java:163-170.
- Cached lookups: `probeWanted` (TAS:274-281), `stockCached` (TAS:2951-2955), `isLaunchable` (TAS:3123-3132).
- Diagnostics use `apply()` or asynchronous logging: TAS:3004, 3029, 642, 500, 4655, 4795, 4331-4337, 3086; LockLog.java:56-65.
- A known pencil is followed with one node refresh, and the watch runs only while a cover is shown: TAS:2443-2452, 2414-2421.
- The cover windows are kept warm instead of being rebuilt: TAS:2515-2540, 2735-2742, 5023-5037.
- The motion sensor runs only on the lock screen with shake or knock chosen (both off by default): TAS:3318-3335, UnlockMethods.java:36-37.
- Screen-on gating and worker threads: TAS:4699-4700, 1140-1170, 4845-4891, 4960-4998, 4919-4945.
- The reset loops are bounded by deadlines: TAS:3692-3708, 3742-3774, 3879-3920.
- `trackDirty` runs only in Quickstep: TAS:1984-1987.
- `homeDrew` posts at most one runnable: TAS:939-958, 2076-2083.
- The wallpaper id is cached (TAS:645-651, 547), and the full lock wallpaper is not read when a stored crop exists (TAS:1205).
- The key filter is switched on only when a feature needs it: TAS:1693-1708.
- `onTrimMemory` acts only under real memory pressure: TAS:3469-3481.

## Suggested order
1. B4 and B6: one line each.
2. B1 and B2: the measured 55 s.
3. B3, starting with its counter.
4. B5 and B7.
