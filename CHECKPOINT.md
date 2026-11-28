# CHECKPOINT 1711 — read me first, then TASKS.md

**Written:** 2026-11-28T01:18:56Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `75a3e48e` (this checkpoint is the commit after it)

## Just done
DA1b: pr2 run 3 prepared (true verify baseline via compile -m verify -f, stat-based oat file sizes, five cold runs per state)

## Do this next
dispatch emulator.yml sections=pr2 shots=true ref claude/wonderful-franklin-vlm90o; find run id (list_workflow_runs resource_id emulator.yml workflow_runs_filter {branch,event workflow_dispatch} perPage 1); background watcher scratchpad/wait_ci.sh <runid> 1500 AND list_workflow_jobs at +6 min; then git show origin/ci-shots:pr-summary2.txt; write final DA1 answer, delete heartbeat trig_01TAXAYoGyEPAhuzh7PzBQm8; no product code changed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  15eb44cd ckpt 1710: DA1b: run #799 id 46091238924 RESULT: release apk profile pipeline WORKS (pr
  9b11c154 ckpt 1709: DA1: LESSONS.md section for this pass (profiler method, debuggable apk caps 
  7cb3367c ckpt 1708: DA1b: run #798 id 46086858124 FAILED in the workflow step 'Build the release
  13c38769 ckpt 1707: DA1b: dispatched emulator.yml sections=pr2 shots=true: run #798 id 460868581
  12357dd7 ckpt 1706: DA1b: run #797 id 46084969954 (sections pr,pr2) read: PIN unlock = 9 ms main
  6a741e60 ckpt 1705: DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
