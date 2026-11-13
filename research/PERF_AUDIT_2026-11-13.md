# Performance audit 2026-11-13 (TASKS CA1): second pass after BY1 (v0.9.8)

Tj, 2026-11-13 01:37 UTC: "Review the launcher for code efficiency, ram optimization without sacrificing speed or function, speed of the launcher
and loading from the lock screen, battery and cpu efficiency. Polish it and make sure it is well built and optimized for my hardware."

**This builds on `PERF_REVIEW_2026-11-12.md` (BY1, shipped in v0.9.8).** That review fixed B1-B4, C1, C2, A2-A4, C6/C7, D1 and left a NEXT list
(C3, C4, A1/C5, D2/D3, B5/B7/B8, A5, A6/C12, C11, B9-B15, D4-D8). This pass: three read-only reviewers (neutral briefs; unlock path, steady CPU /
battery, RAM) on v0.9.8 + a build-config look by hand. IDs here: U = unlock path, S = steady state, M = memory, K = build.

Hardware: MediaTek MT8168B (4x Cortex-A53), 32-bit userland, ~1.3 GB free of 2 GB, Android 10 Go, 600x1024 @ 170 dpi.
Tablet facts (report 2026-11-10-2): PSS 69 MB, Java heap 5 of 128 MB, Application.onCreate 48 ms, 2 process starts a day.

## K: build configuration (by hand)
- K1 R8 stays OFF (AD8 decision, `EFFICIENCY_2026-11-05.md`): reflection on hidden APIs, `app_process ... com.tj.launcher.ShellServer` by class
  name, class names in XML; the emulator gate runs the DEBUG build, so a minifier-only break would show only on the tablet. Unloaded classes cost
  no RAM; the gain would be APK size and install time. Verdict NO (unchanged).
- K2 The app has 0 Kotlin files, but kotlin-stdlib 2.3.10 also comes in transitively (androidx core / annotation-experimental / recyclerview), so
  dropping the Kotlin plugin from :app saves nothing without R8. Verdict NO.
- K3 `cmd package compile -m speed` is a manual Setup item per version (`SetupActivity` COMPILE_SELF, "Redo after every update"). Android's idle
  bg-dexopt compiles speed-profile from the JIT profile anyway when the tablet charges idle. Verdict: say it to Tj (one adb line after an update).

## M: memory (reviewer M)
| id | what | size | verdict |
| :- | :- | :- | :- |
| M1 (=D2) | home curtain cached in custom mode where nothing can show it (`HomeCurtain.java:184`, filled at connect `TjAccessibilityService:322`, 2081, 2186, 2200, 2248) | 2.4 MB (+ maybe a GPU texture) | candidate: keep only while `switchOn && (!custom || bootCleanPending)`, evaluated in `prefWatch` + connect; JVM test |
| M2 NEW | lock-model wallpaper stays pinned on the custom home after an unlock: `refreshLayers()` drops `lockBmp` / `dropWallpaper()` only when `want == null`; home patch target keeps them (1258-1259, 1405, 1440-1456); `onTrimMemory` frees nothing while `patchView != null` (3538) | 4.9 MB (1200x1024), up to 12 MB for a separate lock photo; only when no lock crop is stored (after a wallpaper change / new rotation) | candidate: after `patchTarget()`, if the target is not the lock flavour: `lockBmp = null`, `dropWallpaper()` unless power mode "wallpaper"; onTrimMemory `patchView == null || patchHomeMode` |
| M3 (=D3) | each curtain capture copies the whole Launcher window (`HomeLayer.java:33`, 2160-2171) | 2.4 MB native + GPU readback, <= 1 per 15 s | NEXT (pixel-stability risk; section `cu`) |
| M4 NEW | Launcher3 icon cache keeps icons of HIDDEN apps (`BaseIconCache.addIconToDBAndMemCache` 249-273 always puts; `IconCache.updateIconsForPkg` 103-115) | 16 KB each, 0.1-0.6 MB est. | candidate: put into the mem cache only if already there (DB row still written) |
| M5 (=D4) | patch masks ARGB_8888 rebuilt per window (1469, 1586-1608) | 137 KB kept + 137 KB garbage per window | NEXT (ALPHA_8 pixel-equality on MediaTek unproven) |
| M6 NEW LOW | `HomeLayer.compose` (64-72) one extra copy per build | ~128 KB garbage | NO (rounding risk) |
| M7 (=D6) | 4 idle threads for the process life (`LockLog:51`, `NotifSightings:113` executors, `SettingsSpoof:83` TjSsSync, `AnimationSpeedup:157`) | 20-60 KB each est. | candidate: core-thread timeout like `RecentsFilter:184-191`; keep tj-anim |
| M8 (=D5) | Hidden apps screen loads every icon at once, no view reuse (`HiddenAppsActivity:53-60, 90`) | N x 10-110 KB while open | candidate (simple, high confidence) |
| M9 (=D7) | `new SimpleDateFormat` per log line (`LockLog:27`, `TjAccessibilityService.now()` 1767, 19 sites) | garbage only | candidate |
| M10 (=D8) | `HiddenApps.allowed` copies the set per question (`HiddenApps:32-40`) | garbage only | candidate |

Checked and fine (M): overlays small (<1 MB total), full-screen windows removed after use, Ss* statics tiny and stopped in onStop, Launcher3 Go
widgets off / shortcuts stubbed / AppFilter before icons, all collections bounded, statics hold only the app context or WeakReference, no per-frame
allocations outside a 300 ms fade, the wallpaper-change receiver drops every cached picture.

## U: unlock path (reviewer U) - pending
## S: steady CPU / battery (reviewer S) - pending
