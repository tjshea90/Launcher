# CHECKPOINT 1707 — read me first, then TASKS.md

**Written:** 2026-11-28T00:16:21Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `12357dd7` (this checkpoint is the commit after it)

## Just done
DA1b: dispatched emulator.yml sections=pr2 shots=true: run #798 id 46086858124 (started 00:14:58Z) on head 12357dd7 (release apk built + debug-signed in the workflow, swapped in by the scenario)

## Do this next
wait for ci-shots commit naming run 46086858124 (watcher script scratchpad/wait_ci.sh), then git show origin/ci-shots:pr-summary2.txt (INFO PR2 lines: install, profile written by profileinstaller?, cold start x3 verify / speed-profile / speed with LaunchState, oat file sizes, PSS per state); if a step failed read the job log tail (get_job_logs, tail_lines 60); then decide P1 (leaning SKIP) and write the final DA1 answer; heartbeat trig_01TAXAYoGyEPAhuzh7PzBQm8 to delete when done

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  12357dd7 ckpt 1706: DA1b: run #797 id 46084969954 (sections pr,pr2) read: PIN unlock = 9 ms main
  6a741e60 ckpt 1705: DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
  35cc3cef ckpt 1699: DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug A
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
  05373f92 ckpt 1697: CY1: draft wording fixed (two emulator runs, second green); job complete
```
