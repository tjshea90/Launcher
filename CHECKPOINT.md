# CHECKPOINT 1708 — read me first, then TASKS.md

**Written:** 2026-11-28T01:04:24Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `e974336d` (this checkpoint is the commit after it)

## Just done
DA1b: run #798 id 46086858124 FAILED in the workflow step 'Build the release APK' (gradle BUILD SUCCESSFUL, then apksigner: FileNotFoundException /home/runner/.android/debug.keystore: a runner that restores the debug apk from the Gradle build cache never creates a debug.keystore and that apk carries another runner's key). Fixed in emulator.yml: keytool makes ONE throwaway key that signs pr2-release.apk AND re-signs app-debug.apk before the scenario installs it. LESSON: my ci-shots watcher waited its whole 45 min deadline because a failure BEFORE the scenario never publishes: after dispatch also check list_workflow_jobs at +6 min

## Do this next
1) dispatch emulator.yml sections=pr2 shots=true ref claude/wonderful-franklin-vlm90o (mcp__github__actions_run_trigger), find the run id with list_workflow_runs resource_id emulator.yml workflow_runs_filter {branch,event workflow_dispatch} perPage 2; 2) start the watcher 'scratchpad/wait_ci.sh <runid> 1500' in the background AND check list_workflow_jobs at +6 min (step 'Build the release APK' must be success); 3) read git show origin/ci-shots:pr-summary2.txt (INFO PR2 lines) + pr2-*.txt; 4) decide: if compile gives a clear cold-start win keep the Setup compile item as THE lever (tell Tj to run it after each update) and write P1 (class lines in baseline-prof.txt) as SKIPPED (lz4 app image = anonymous RAM, reasoned) unless numbers say otherwise; 5) write research/DA1_PERF_PROBE final section + TASKS DA1b-DA1e ticks + LESSONS entry (debuggable apk caps compile at quicken; force-stop restarts Home so use Settings in front; keystore on cached runners); 6) NO product code changed so far: no release needed; 7) delete_trigger trig_01TAXAYoGyEPAhuzh7PzBQm8 when done; 8) answer Tj: PROVEN (idle 0.03 percent of a core, 0 frames, unlock 9 ms, power menu 2 ms, drawer/pages/app launch/shade/recents tiny; only cold start has CPU) vs ONLY REASONED; ask for a fresh tablet report (PSS split + compile state)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  13c38769 ckpt 1707: DA1b: dispatched emulator.yml sections=pr2 shots=true: run #798 id 460868581
  12357dd7 ckpt 1706: DA1b: run #797 id 46084969954 (sections pr,pr2) read: PIN unlock = 9 ms main
  6a741e60 ckpt 1705: DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
  35cc3cef ckpt 1699: DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug A
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
