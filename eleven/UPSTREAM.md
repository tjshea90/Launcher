# Eleven, vendored from LineageOS (TASKS BV1)

- Upstream: https://github.com/LineageOS/android_packages_apps_Eleven , branch `lineage-17.1` (Android 10), commit `ca22308cfe54b3c4d7200441a3f951f4831fb035` (2024-02-08, "Automatic translation import").
- Licence: Apache License 2.0. The upstream `NOTICE` ("Copyright (c) 2014, The CyanogenMod Project") is kept as `NOTICE`, and every source file keeps its own licence header.
- Why it is here: Tj asked for this music app in the custom launcher's app drawer, unchanged in look and function except that it must NEVER load or display album art (research/ELEVEN_PORT_2026-11-11.md).
- The first commit that contains this folder is the PRISTINE upstream content: only the paths were moved to the standard Gradle layout (`src/**/*.java` to `src/main/java`, the two `.aidl` files to `src/main/aidl`, `res` to `src/main/res`, `AndroidManifest.xml` to `src/main/AndroidManifest.xml`). `git diff <that commit>..HEAD -- eleven` is therefore the complete list of our changes (Apache 2.0 section 4(b)); `CHANGES.md` explains them in words.
- Not vendored: `Android.bp`, the privapp whitelist, upstream's Gradle wrapper and `build.gradle` (replaced by `build.gradle.kts` for this project's AGP 8.13.2), `lint-baseline.xml`, `src_aosp/BuildConfig.java` (Gradle generates `BuildConfig`).
- To re-check against upstream: `git clone --branch lineage-17.1 https://github.com/LineageOS/android_packages_apps_Eleven` somewhere OUTSIDE this repo, check out the commit above, and compare (never commit a clone).
