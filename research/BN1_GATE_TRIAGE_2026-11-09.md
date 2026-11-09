# BN1 gate triage for v0.9.0 (head 09215ccebf53e930944a82df2c93eec827684bb1, base revision edbf66d = v0.8.13), written 2026-11-09 19:30 UTC

Gate plan (tools/gate.sh edbf66d): run 1 `legacy,n6,t,x,ac,bl,lq,ao,an,rt`, run 2 `base,x1,e1,n,o,bs,cs,cs2,rb,ss,last`, run 3 `rg` solo.

| Run | Sections | Id | Result |
| :- | :- | :- | :- |
| #465 | `rg` solo | 42671999660 | SUCCESS (18:16:50Z) |
| #463 | run 1 | 42671984609 (attempt 1 = zero jobs, GitHub-side, 57 s; attempt 2 started 18:48Z) | **149 PASS, 5 FAIL, 97 INFO** + `NATIVE CRASH #1` |
| #464 | run 2 | 42671992301 (attempt 1 = zero jobs; attempt 2 started 18:48Z) | **162 PASS, 6 FAIL, 80 INFO** + `NATIVE CRASH #1`; section `ss` ALL PASS (SS7 passed this time), TIME ss 357 s |

FAIL lines (verbatim, read by a haiku subagent from the job log; scenario line -> section):
* #463: `recents: the target app (Clock) is in Recents` (l.293, section legacy), `recents: an ordinary app launched from the launcher (Messaging) is NOT in Recents` (l.294, legacy), `reset2: the notification was cleared through the real shade (no notification access)` (l.333, legacy), `W3b: a wallpaper patch window exists while the menu is closed` (l.405, legacy), `LQ: a gesture unlock into Quickstep puts Wi-Fi back as it was before the lock` (l.1887, section lq).
* #464: `crash breaker switched to quickstep` (l.224, section base), `after the breaker, Home shows the stock launcher` (l.226, base), `AR1: Launcher control is grouped: every group header is on the page` (l.238, base), `AR1: 'Restore everything' stays reachable (a fixed footer, not inside a closed group)` (l.239, base), `O1: the erase ladder (Delete, digit x3) before the PIN unlocks into Quickstep` (l.714, section o), `O1: and the stock launcher is on screen` (l.715, o).

Both runs printed `INFO NATIVE CRASH #1 of com.tj.launcher` (the known SIGSEGV that clears the default home; checks within ~15 s after it fail). NOT yet known: where the crash sits relative to the failing sections (all 11 FAILs are in the first sections of each chain: legacy, base, o, lq), and whether they would fail without the crash.

**Suspect to rule out FIRST (a real regression is possible):** the crash-breaker FAILs (`crash breaker switched to quickstep`, `after the breaker, Home shows the stock launcher`, section base) touch `CrashGuard.record`, which BN1 changed (`SsFuse.fuseHandles(e, SsFuse.blocked(app))` runs FIRST and returns without counting when the first own frame is an `Ss*` / `SettingsSpoof*` class). If the scenario's forced crashes hit that branch, the breaker never reaches 3. Read `tools/emulator_scenario.sh` l.200-230 (what crash is forced) and `SsFuse.fuseHandles`, and the unit test `SsFuseTest`.

Plan: (1) read the crash-breaker part of the scenario and SsFuse (cheap, decides it); (2) rerun SOLO on the head: `base`, `o`, `legacy`, `lq` (gate.sh / dispatch emulator.yml sections each; different sections run side by side); (3) any section that fails again: run it on the tag `v0.8.13` (`ref` = v0.8.13) to see whether it is pre-existing; (4) fix a real regression, else record the reruns as the proof (`git diff <gated> HEAD -- app launcher3 phoneguide` must stay empty, only version lines may differ); (5) then bump `app/build.gradle.kts` versionName 0.9.0 / versionCode 39 and `bash ship.sh "note"`.

## Solo reruns and the baseline (written 2026-11-09 22:30 UTC, after the cut-off)

Product code is identical to the gated head (`git diff 09215cc HEAD -- app launcher3 phoneguide` is empty; only scenario, docs and CI bookkeeping changed).

| Run | Section / ref | Id | Result |
| :- | :- | :- | :- |
| #466 | `base`, head | 42683210584 | 4 FAIL, 2 native crashes (threads `launcher-loader` and main) |
| #467 | `o`, head | 42683216363 | green |
| #468 | `legacy`, head | (see ckpt 831) | 2 FAIL (reset2, startup) + a native crash |
| #469 | `lq`, head | (see ckpt 831) | green |
| #470 | `base`, TAG v0.8.13 (edbf66d) | 42685125516 | **28 PASS, 1 FAIL** (`lock: Launcher control does not open in custom mode`) + `INFO NATIVE CRASH #1`: the RELEASED tag crashes natively too |
| #471 | `base`, head 4a49b3f | 42685347862 | **29 PASS, 0 FAIL, no native crash** |

Reading: `base` fails on the released tag with the same native crash (so the crash is the emulator's, not BN1's) and passes clean on the head. The head's earlier `base` failures (#464, #466) were crash-poisoned runs. `legacy` is the one section still to prove: dispatched solo on `main` and on `v0.8.13` side by side (ids in the checkpoint).

## `legacy` (written 2026-11-09 23:00 UTC)

| Run | Sections / ref | Id | Result |
| :- | :- | :- | :- |
| #472 | `legacy` SOLO, head | 42711379205 | 17 PASS, 3 FAIL: `reset2`, `W5` x2; NATIVE CRASH in `launcher-loader` (`extent_deregister_impl` <- `extent_recycle`, fault addr 0x8) |
| #473 | `legacy` SOLO, TAG v0.8.13 | 42711385101 | 19 PASS, 1 FAIL: `reset2`; no crash |
| #474 | `base,legacy` ONE run, head 306152a | 42713242884 | 40 PASS, 4 FAIL: `recents` (Clock in Recents), `W5` x2, `W3b`; THREE native crashes (main `arena_bin_malloc_hard`; `launcher-loader` `a0ialloc`; `launcher-loader` `extent_deregister_impl`); all `base` checks passed, `reset2` PASSED (it passes only after `base`) |

Readings. (1) `reset2` fails on the TAG too when `legacy` runs solo: by design it needs `base` in the same run (HANDOFF 0e RS1). The gate plan had split them (a gap in `gate.sh`, FIXED 2026-11-09: `BIND` now has `legacy -> base`). (2) Every other FAIL (`W5`, `recents`, `W3b`, earlier `startup`) sits in a run with a native crash right before it (`W5 after: resumed: ...com.android.launcher3/.Launcher`: the crash cleared the default home). (3) The crashes are ALL inside jemalloc (bionic libc.so), in three different threads and call sites = a corrupted native heap, the crashing thread is only the victim. The released tag crashes too (#470). New lead for a separate task: TASKS BN6.

Reruns on the release commit (version 0.9.0 / code 39, 3da7cb8): #475 id 42715672216 (`main`, `base,legacy`), #476 id 42715676675 (branch, `base,legacy`), #477 id 42715681398 (`main`, `base,legacy,last`). The first one with 0 FAIL is the proof for `base` + `legacy`; heal() prints up to 12 frames of the crash backtrace now.

Results (read 23:09 UTC; all three FAILED, `ship.sh` passed locally: handoff test, unit suite, lint, commit pushed):
* #475 (`main`, `base,legacy`): 42 PASS, 3 FAIL (`recents` Messaging not in Recents, `B1` our task in the HOME-type stack, `reset2` Calendar opened); THREE native crashes, now with frames: (1) main thread, fault addr 0x1c, `android_view_DisplayListCanvas_reset` called from Java `RecordingCanvas` / `RenderNode.beginRecording`; (2) thread `GrallocUploadTh`, `extent_deregister_impl` <- `je_extents_alloc` <- malloc <- `operator new` <- `libOpenglCodecCommon.so` `GLClientState::init` (the emulator's GL pipe); (3) `launcher-loader`, the same jemalloc frames <- `libOpenglSystemCommon.so` `QemuPipeStream::allocBuffer` <- `HostConnection::connect` <- `HostConnection::get`. = heap corruption inside the EMULATOR'S GRAPHICS EMULATION layer (goldfish GL pipe) hitting random threads; a real ARM tablet has no such layer.
* #476 (branch, `base,legacy`): 41 PASS, 4 FAIL: `crash breaker switched to quickstep`, `after the breaker, Home shows the stock launcher`, `AR1` x2 = the SAME four FAILs as #464 and #466, but with NO native crash line at all in this run. So that group is not (only) a crash artefact: 3 of 7 head runs of `base` failed it (#464, #466, #476), #471 / #474 / #475 / #477 passed it. Needs the tag's rate (the tag's one `base` run #470 failed another check, `lock: Launcher control does not open in custom mode`, with a crash).
* #477 (`main`, `base,legacy,last`): 45 PASS, 1 FAIL (`startup` Clock in front), one crash (`launcher-loader`, `libhwui` `SkRegion::setPath` <- malloc), `base` all PASS.
Next: 4 x `base` on the tag v0.8.13 against 4 x `base` on the head (ckpt 839: the scenario now waits for the process before each `am crash` and prints `breaker diag` with the breaker's file after each crash).
