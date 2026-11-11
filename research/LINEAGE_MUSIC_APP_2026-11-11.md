# The LineageOS stock music app (Eleven): source for Android 10 (TASKS BU1, 2026-11-11)

Tj, 2026-11-11 15:48 UTC (verbatim): "See if you can find the stock lineage music app source for this os".
Claude's reading (NOT confirmed with Tj): the music app LineageOS ships by default, in the branch that matches Android 10 (LineageOS 17.1).

## Answer
- **Eleven**, package `org.lineageos.eleven`. Upstream: https://github.com/LineageOS/android_packages_apps_Eleven . Android 10 = branch **`lineage-17.1`** (`lineage-17.0` also exists).
- Verified 2026-11-11 with `git ls-remote --heads` and a shallow clone of `lineage-17.1` (the clones were made in the session scratchpad and are NOT kept; nothing of Eleven is copied into this repo).

## Verified facts about `lineage-17.1`
| What | Value |
| :- | :- |
| Branches of the repo | cm-12.0 .. cm-14.1, lineage-15.0 .. lineage-22.0 (so 17.0 and 17.1 are there), plus stable/* and staging/* |
| Default app of LineageOS 17.1 | Verified in the vendor repo (shallow blob-less clone, scratchpad): https://github.com/LineageOS/android_vendor_lineage , branch `lineage-17.1` (tip `e856948`, 2023-12-31), `config/common_mobile.mk`, section `# Lineage packages` of `PRODUCT_PACKAGES`: `AudioFX`, `Backgrounds`, **`Eleven`**, `Etar`, `Jelly`, `LockClock`, `Profiles`, `Seedvault`, `TrebuchetQuickStep`, `WeatherProvider` |
| Tip of lineage-17.1 (Eleven repo) | `ca22308`, 2024-02-08, "Automatic translation import" |
| Size | 4.3 MB without `.git`; 152 Java files, 33,859 Java lines, 270 resource files |
| Manifest | package `org.lineageos.eleven`, versionName 4.0.1, minSdk 28, targetSdk 30; activities `HomeActivity`, `SettingsActivity`, `AudioPreviewActivity`; `MusicPlaybackService`, widgets |
| Build systems | BOTH `Android.bp` (AOSP tree: `privileged: true`, `jni_libs: librsjni`, a privapp whitelist) AND a Gradle build (AGP 7.0.4, compileSdk 30, wrapper included) |
| Libraries (Gradle) | appcompat 1.4.0, cardview 1.0.0, palette 1.0.0, preference 1.1.1, recyclerview 1.2.1, constraintlayout 2.1.2, material 1.4.0 |
| RenderScript | support mode, target API 19; used for the blur (`BlurBitmapWorkerTask`) |
| Licence | Apache 2.0: `NOTICE` says "Copyright (c) 2014, The CyanogenMod Project"; `MODULE_LICENSE_APACHE2` |
| Successor | **Twelve**, https://github.com/LineageOS/android_packages_apps_Twelve , branches `lineage-22.0` and newer only (no Android 10 version). Verified: `android_vendor_lineage` branch `lineage-22.1` (tip `fac22bf`, 2025-02-25) lists `Twelve` in `config/common_mobile_full.mk` and has no `Eleven` anywhere in `config/`, so Twelve replaced Eleven as the default from LineageOS 22.1 (Android 15) |

## What this is NOT
- **Not the music app of Tj's tablet.** The tablet is not LineageOS: `ro.lineage.version` is empty in every tablet report (`research/tablet/LATEST.md`; Alps MT8168, Android 10 Go), and no tablet report names an installed music app. Eleven is the stock player of LineageOS only.
- **AOSP's own "Music"** (`platform/packages/apps/Music`, branch `android10-release`, last commit 2019-06-20): package `com.android.music`, only 4 Java files / 745 lines (`MusicPicker`, `MediaPlaybackService`, `AudioPreview`, `MusicBrowserActivity`), minSdk 19, targetSdk 28: a stub, not a usable player.

## Fit with this project (a later decision, NOT made)
- This repo builds with AGP 8.13.2 / Gradle 8.14.3 (root `build.gradle.kts`, wrapper properties). Eleven's Gradle files use AGP 7.0.4 and RenderScript support mode (deprecated), so building it here would need updating (inference, not tried).
- If the goal is a LOOK-ALIKE music page in the custom drawer (like the Settings look-alike, TASKS BN1), only the look is needed: screenshots of the real thing would be the reference, not this source.
- Apache 2.0 allows reuse with the licence text and copyright notices kept.
- Open question for Tj: what is the source for? (reference for the look, a look-alike page, a player built into the launcher, something else). No code until he says.
