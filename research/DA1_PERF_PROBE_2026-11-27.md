# DA1 perf probe (Tj, 2026-11-27) — the sixth speed / RAM / CPU pass, this time MEASURED

Tj (verbatim): "Look for any possible ways other than what has already been done, to make this launcher as efficient and fast and
lightweight as possible and as well optimized as possible for my hardware, mt8168 Android 10 go 2gb ram. If you find anything, build it".
Rules: TASKS DA1a (launcher only, no look / function change, every change provable, measure first, R8 stays OFF, write down what was skipped).
Earlier passes (BY1 CA1 CG1 CW1 CX1 CY1) all READ code. Nothing had ever MEASURED which methods of the launcher process burn CPU. This pass does.

## What was built (tools only, no product code yet)
- `tools/trace_top.py` — reader for ART sampling-profiler traces (`am profile start --sampling 1000`): self / inclusive CPU + wall per method and
  thread, short `INFO  PR ...` lines, `--detail` report, `--tree` (call tree of one thread), `--cpu a b` (two `/proc/<pid>/task` snapshots -> idle CPU
  per thread). `tools/test_trace_top.sh` (self-test on a synthetic trace) is part of `tools/test_handoff.sh`.
- emulator.yml section `pr` (research only, zero checks): cold start, 30 s idle screen-on and screen-off, drawer + flings, home pages, app launch and
  back, notification shade, screen off/on, Recents, PIN unlock, stock power menu, PSS split at start and end, thread inventory, compile state.
  Files land in `pr-*.txt`, `pr-*.trace.gz` and `pr-summary.txt` on branch `ci-shots` (dispatch with `shots=true`; `git fetch origin ci-shots`,
  `git show origin/ci-shots:pr-summary.txt`). Section `pr2`: cold-start TotalTime x3 as installed (`verify`) vs after `cmd package compile -m speed-profile`
  from the profile the runtime collected vs `-m speed`, plus the runtime profile text (`pr-profile.txt`) and the oat / art file sizes.

## Results of run #796 (id 46083514184, section `pr`; CI emulator = x86, api 29, software GL, animations off, debug APK status=verify)
Absolute milliseconds do NOT carry to the MT8168 (A53 is several times slower); RANKS and RATIOS do.
| Phase | main-thread CPU | what the profile says |
| :- | :- | :- |
| idle, screen on, 30 s | 10 ms in 30 s total (0.03 % of one core), 0 frames drawn | nothing runs: no polling, no animation, no invalidation loop |
| idle, screen off, 30 s | 10 ms (HeapTaskDaemon only) | same |
| drawer open + 5 flings (9.9 s) | 153 ms, of which 91 ms is `nativePollOnce` (= idle waiting) | real work about 60 ms for ten seconds of gestures |
| home pages, 4 swipes | 104 ms (78 ms idle wait) | `TjHooks$1.onDraw` -> `TjAccessibilityService.get` 2 ms: noise |
| app launch from the drawer + back | 25 ms | `AppOrigin.markCustom` 2 ms, `RecentsPolicy.apply` 2 ms |
| notification shade down / up x2 | 2 ms | the service's scans cost nothing measurable |
| screen off / on | 7 ms | |
| Recents open / close | 20 ms | RecyclerView detach (stock Quickstep host), not ours |
| cold start of the launcher process (7.4 s window) | main 574 ms, launcher-loader 141 ms, HeapTaskDaemon 69 ms | the ONLY sizeable CPU; below |
Cold start, main thread, biggest self times: `DexFile.openDexFileNative` 58, `AllAppsGridAdapter.<init>` 48 (constructor is trivial: class load / link of
what it touches; `AlphabeticIndexCompat` = ICU AlphabeticIndex build 24 of it), `Constructor.newInstance0` 46, `TjAccessibilityService.get` 33 (first load of the
5880-line service class: only when the process was force-stopped, on the tablet the service starts at boot), `Bits.byteOrder` 20; inclusive: `Launcher.onCreate`
278, `AllAppsContainerView.<init>` 85, `RouterActivity.handleHome` 44, `LauncherAppState.getInstance` 29. `am start -W` TotalTime 1371 ms (JIT-cold, verify-only).
All of that is class loading + first-use inflate, i.e. the cost an AOT compile with a GOOD profile (classes + methods) removes. A cold start happens
about twice a day on the tablet (process start count from the v0.9.1 report), so the user-visible gain is small and only at a cold start.

PSS split (emulator, KB): start TOTAL 62 691 = Java heap 8 480, Native heap 23 452 (software GL lands here: not comparable with the tablet), Code 12 972, System 15 335;
end TOTAL 48 101. Views 40, one ViewRootImpl, one Activity, Local Binders 27: no leak signature. Thread inventory at the end: 23 threads, 5 Binder, one each of
tj-vpn-off, tj-lock-log, tj-anim, queued-work-looper, TjSsSync, launcher-loader, UiThreadHelper, Profile Saver (no thread pools left running).
Compile state: `x86: [status=verify] [reason=boot]` (debug APK: no baseline profile is packaged in debug).

## Findings from reading the build, not the run (all [V])
- Packaging is already optimal: dex and resources.arsc are STORED in the APK (no inflate at start), lint `Performance` category is clean, manifest is clean.
- The shipped baseline profile (release `baseline.prof`, 791 B binary): ONE dex of 55 203 methods, about 11.4 k methods marked hot (our wildcards expand to them), but only
  4 CLASSES in the class set and every method flagged HSP with no layout locality. A profile with so few classes yields no useful app image (preloaded classes) when
  bg-dexopt compiles `speed-profile`. This is the one build-input lever the measurement points at; whether it is worth it is what `pr2` measures.

## Candidates, status (DA1b / DA1c; fill in as decided)
| Id | Idea | Status |
| :- | :- | :- |
| P1 | precise class lines (from the runtime profile) in `app/src/main/baseline-prof.txt` so the compile builds an app image | pending the pr2 numbers |
| P2 | HS (startup) flags + P (post-startup) wildcard instead of all-HSP so the layout puts startup code together | pending, neutral by construction, unproven gain |
| P3 | force a `speed-profile` compile on the tablet through the shell helper right after install | not started; needs Tj's tablet to know the compile state first (DexoptWatch prints it) |
| X1 | GPU / EGL surface memory of the overlay windows (patch, pencil cover) | NOT measurable in CI (software GL); needs a tablet report with the PSS split; lock-path code: only if trivially behaviour-preserving |
| X2 | narrow accessibility event types | NO (CY1 decided: the patches depend on them) |
| X3 | R8 / minify | NO (BRIEF + PERF_AUDIT K1: hidden-API reflection, class names in XML and in `app_process`) |
