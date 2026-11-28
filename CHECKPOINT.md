# CHECKPOINT 1710 — read me first, then TASKS.md

**Written:** 2026-11-28T01:17:09Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `74868746` (this checkpoint is the commit after it)

## Just done
DA1b: run #799 id 46091238924 RESULT: release apk profile pipeline WORKS (profile on device hot=14922 classes=281 after first start); cold start TotalTime install 345/303/321, speed-profile 329/304/383, speed 317/245/310 ms (compile state <= 10 percent, noise 40); PSS 53015 / 52789 / 57809 KB; app image of a 281-class profile = +0.94 MB .art mmap (3.4 KB per class) => P1 class lines SKIPPED; gaps: no true verify baseline (state after install -r was already speed-profile), files: empty (use stat), only 3 runs

## Do this next
run 3 of pr2 (optional, for a complete record): in tools/emulator_scenario.sh section pr2 (a) after the profile read add 'pr2adb shell cmd package compile -m verify -f' then pr2cold verify + pr2mem verify as the TRUE baseline, rename the install-state ones, (b) pr2files via adb shell stat -c '%n %s' on d/oat/x86/base.odex base.vdex base.art, (c) five cold runs per state; dispatch sections=pr2 shots=true, read pr-summary2.txt; then write the final DA1 answer (TASKS DA1b-DA1e ticks, research doc decision table), delete heartbeat trig_01TAXAYoGyEPAhuzh7PzBQm8, answer Tj; no product code changed, no release

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M research/DA1_PERF_PROBE_2026-11-27.md

## Last ten checkpoints
```
  9b11c154 ckpt 1709: DA1: LESSONS.md section for this pass (profiler method, debuggable apk caps 
  7cb3367c ckpt 1708: DA1b: run #798 id 46086858124 FAILED in the workflow step 'Build the release
  13c38769 ckpt 1707: DA1b: dispatched emulator.yml sections=pr2 shots=true: run #798 id 460868581
  12357dd7 ckpt 1706: DA1b: run #797 id 46084969954 (sections pr,pr2) read: PIN unlock = 9 ms main
  6a741e60 ckpt 1705: DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
