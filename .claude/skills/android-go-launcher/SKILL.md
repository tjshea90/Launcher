---
name: android-go-launcher
description: Use when changing Launcher's manifest, HOME activity, installed-app listing, icon/bitmap handling, memory behaviour, default-launcher setup, or adding any dependency. Constraints and gotchas for an Android 10 (API 29) Go-edition home-screen launcher.
---

# Android 10 Go launcher — constraints and gotchas

Read `BRIEF.md` first: it holds the decisions and the OPEN questions (do not decide those for Tj).
The app is now a Launcher3 fork (`app/`, `launcher3/`); `HANDOFF.md` has the architecture map. The rules below still hold. Device facts (2 GB, MT8168, what Go switches off): `tj-android-kit:low-ram-android`.

## Target
- Android 10 (API 29), "Go edition": 1-2 GB RAM, slow storage, often 32-bit ARM. A Go launcher is judged on
  **cold-start time, resident memory and APK size** before features.
- `minSdk 29` / `targetSdk 28` (on purpose: the AOSP Launcher3 Go value, it lets the kill switch toggle Wi-Fi / Bluetooth; `BRIEF.md`) / `compileSdk 36`. APIs newer than 29 must be guarded (`Build.VERSION.SDK_INT`) or
  avoided; lint (`bash tools/gradle.sh :app:lintDebug`) will flag `NewApi`.

## Being a launcher
- Manifest: the Home activity has `MAIN` + `HOME` + `DEFAULT`, `launchMode="singleTask"`, `stateNotNeeded`,
  `excludeFromRecents`; Back must do nothing (a launcher that exits shows a black screen).
- List apps with `PackageManager.queryIntentActivities(MAIN/LAUNCHER)`; start with `component` + `FLAG_ACTIVITY_NEW_TASK`.
  Hide the launcher's own package. Re-query in `onResume` and on package add/remove broadcasts (registered at
  runtime — manifest receivers for `PACKAGE_ADDED` are not delivered on API 26+).
- Wallpaper shows through with a `Theme.*.Wallpaper*` theme. `READ_EXTERNAL_STORAGE`/wallpaper permissions are only
  needed for reading the wallpaper file; prefer `WallpaperManager` APIs.
- Android 10 has no `QUERY_ALL_PACKAGES` requirement; the manifest `<queries>` block keeps a later newer-device build working.
- Setting the default: `Settings > Apps > Default apps > Home app` (or the `ACTION_HOME_SETTINGS` intent). Don't try to force it.

## Go-edition discipline
- No heavyweight libraries: every dependency costs APK size, dex methods and RAM. Justify in `BRIEF.md` before adding.
- Decode icons/bitmaps at display size (`inSampleSize`), cache with a small `LruCache` sized from
  `ActivityManager.memoryClass`, and drop it in `onTrimMemory`. Avoid per-frame allocations in lists.
- Prefer `RecyclerView`-free lists only for tiny sets; for hundreds of apps a recycling view is mandatory (note the
  `androidx.recyclerview` cost in BRIEF.md first).
- Do disk and package-manager work off the main thread (the code is Java, no coroutines); keep startup minimal.
- R8 is on for release; test the **release** build for reflection/keep-rule breakage before shipping.

## Verifying
- JVM unit tests for pure logic (`AppList`); keep logic out of Activities so it stays testable.
- Emulator/device checks: the `claude-android-qa-plugin:android-emulator-qa` skill if an emulator is available;
  otherwise the preview APK from the `android.yml` artifact on the real device.
