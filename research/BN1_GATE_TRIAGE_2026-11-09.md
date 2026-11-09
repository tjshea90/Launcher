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
