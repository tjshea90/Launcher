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

## Results of run #799 (id 46091238924, section `pr2`, RELEASE apk debug-signed with a throwaway key; CI emulator x86 api 29) - the shipped baseline profile and compiled code
The debug APK cannot be used for this (run #797: a debuggable app is capped at `quicken`), and the wrong cold-start method (force-stop alone) measured warm starts. `pr2` now swaps in the release variant and starts Home cold with Settings in front.
| Fact | Value |
| :- | :- |
| assets in the release apk | `assets/dexopt/baseline.prof` 791 B, `baseline.profm` 55 B |
| profile on the device after the first start + use | hot 14 922, startup 14 908, post-startup 15 127 methods, 281 classes (the shipped rules + what the runtime added): **androidProfileInstaller works on API 29, the CG1c pipeline is real** |
| state right after `install -r` | `speed-profile [reason=install]` (this image's install-time dexopt; the old debug profile matched nothing, so NOT a true verify baseline: fixed in the next run) |
| cold start `am start -W` TotalTime, three runs each, LaunchState COLD | install state 345 / 303 / 321 ms; after `compile -m speed-profile` 329 / 304 / 383; after `-m speed` 317 / 245 / 310. Compile state changes the cold start by at most about 10 percent (noise is +-40 ms) |
| PSS after the same scripted use (KB) | install state TOTAL 53 015 (Native 23 992, Code 8 496); speed-profile 52 789 (Native 18 312, Code 14 080); speed 57 809 (Native 18 376, Code 18 800) |
| app image | `.art mmap` 2 199 KB (no image) -> 3 142 KB after the speed-profile compile with a 281-class profile: **about +0.94 MB, roughly 3.4 KB per class**; `-m speed` builds no image (1 883 KB). A 1 305-class list (everything a use loads) would cost about 4.4 MB, permanently, for a gain that the cold-start numbers say is under 10 percent |
Reading: compiled code moves memory from the native heap (JIT data) into file-backed code pages: speed-profile is memory-neutral (52.8 against 53.0 MB), `-m speed` (the Setup item COMPILE_SELF) is about 5 MB more PSS than speed-profile (clean file pages, reclaimable) and not measurably faster at a cold start.
**Decision P1 (class lines in baseline-prof.txt): SKIPPED.** Measured cost 3.4 KB RAM per class, no measurable cold-start gain, and the existing profile already compiles every method of our code. Nothing to build here.
Known gaps of run #799 (fixed for run 3): no true `verify` baseline, `files:` empty (the oat directory cannot be listed by the shell user: use `stat -c %s` on the known names), three runs per state is noisy (use five).

## FINAL results of run #800 (id 46092484975, section `pr2`, release apk, five cold starts per state, a TRUE verify baseline) - DECISIONS
| State (compile filter, what makes it) | oat / art files | cold start `am start -W` TotalTime, 5 runs (mean) | PSS after one scripted use (KB) |
| :- | :- | :- | :- |
| verify (what a fresh sideload is until Android compiles it) | odex 131 KB, no image | 309 378 331 384 330 (**346 ms**) | TOTAL 49 668: Native heap 21 008, Code 8 564, Java 3 724 |
| speed-profile (what Android's idle job makes from our shipped profile; `cmd package compile -m speed-profile`) | odex 6.8 MB, art 0.54 MB | 317 268 281 286 270 (**284 ms, 18 % faster**) | TOTAL 49 040: Native heap **12 852**, Code 14 304, Java 4 932 |
| speed (Setup item COMPILE_SELF, `-m speed`) | odex 18.6 MB, no image | 330 300 396 277 308 (322 ms, 7 % faster, within noise) | TOTAL 51 645: Native heap 12 376, Code 18 500 |
- The shipped baseline profile WORKS end to end on API 29: after the first start the device holds hot 14 922 / startup 14 908 / post-startup 15 127 methods and 285 classes (shipped rules plus what the runtime recorded). CG1c is verified, not just reasoned.
- A compile turns about 8 MB of ANONYMOUS memory (JIT data in the native heap, not reclaimable) into about 6 MB of file-backed code pages (reclaimable): speed-profile is the best state on every axis measured; `-m speed` costs 2.6 MB more PSS, 11.8 MB more storage and was not faster. The runtime adds its own classes to the profile, so the app image (about 3.4 KB per class: +0.98 MB for 285 classes) appears anyway; adding class lines to `baseline-prof.txt` would change nothing useful.
- The cost of NOT being compiled is the first window after each update (until the idle job runs): about 8 MB more anonymous memory and an 18 % slower cold start. Nothing in the app can start that job earlier (shell only: the Setup item).
| Id | Idea | Decision |
| :- | :- | :- |
| P1 | class lines in baseline-prof.txt | SKIPPED: the runtime records the classes itself (285 after one use), the image costs about 3.4 KB per class, no gain over what already happens |
| P2 | startup (S) / post-startup (P) flags instead of all-HSP | SKIPPED: layout only matters for a verify-state cold start and the numbers show 346 against 284 ms in total; unproven |
| P3 | compile right after install through the shell helper | SKIPPED: the helper only lives during Tj's cable sessions, a compile during use competes for the CPU of a 2 GB tablet, and the Setup item already exists (COMPILE_SELF) |
| P4 | Setup item `-m speed` -> `-m speed-profile` | OBSERVED, NOT CHANGED: speed-profile is 2.6 MB leaner, 11.8 MB smaller, not slower; but without an installed profile it compiles nothing and `compile_done_code` would still be set. Tj asked for a FULL compile in CG1c |
| X1 | GPU / EGL memory of the overlay windows | NOT measurable in CI (software GL); needs a tablet report with the PSS split (v0.9.20 prints it) |
| X2 | a11y event types | NO again: the event delivery is at most about 10 events per second per window (the framework throttles content-change events to 100 ms), our filter drops them at the door, and the measured idle cost is 0.03 % of a core |
| X3 | R8, resource trimming, thread count, receivers | nothing measurable: 23 threads (5 Binder), 40 views, one Activity, no leak signature |
Product code changed in this pass: NONE. What was built: `tools/trace_top.py` + `tools/test_trace_top.sh` (in `tools/test_handoff.sh`), emulator sections `pr` and `pr2`, `ci_publish_shots.sh` globs, the conditional release-apk step of `emulator.yml`. No release needed.
