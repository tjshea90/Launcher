# CHECKPOINT 1706 — read me first, then TASKS.md

**Written:** 2026-11-28T00:14:50Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `bb6cf253` (this checkpoint is the commit after it)

## Just done
DA1b: run #797 id 46084969954 (sections pr,pr2) read: PIN unlock = 9 ms main CPU in 9.9 s, power menu 2 ms; runtime profile of a use = 1305 classes + ~3k methods; pr2 compile numbers were NOT usable (debuggable apk caps compile at quicken; force-stop lets the system restart Home so am start -W measured warm starts 29-75 ms); pr2 REWRITTEN around a release apk (emulator.yml builds + debug-signs it only for pr2, scenario swaps it in with install -r, measures profileinstaller result, cold start x3 + files + PSS for verify / speed-profile / speed, restores the debug apk)

## Do this next
dispatch emulator.yml sections=pr2 shots=true on claude/wonderful-franklin-vlm90o, read pr-summary2.txt + pr2-*.txt from origin/ci-shots; heartbeat trig_01TAXAYoGyEPAhuzh7PzBQm8; P1 (class lines in baseline-prof.txt) leaning SKIP: lz4 app image is decompressed into anonymous RAM (reasoned, not measured), cold start happens ~2x/day

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M tools/ci_publish_shots.sh
     M tools/emulator_scenario.sh

## Last ten checkpoints
```
  6a741e60 ckpt 1705: DA1: heartbeat armed (trig_01TAXAYoGyEPAhuzh7PzBQm8, cron 1 */2 * * *, first
  c3a5ca9d ckpt 1704: DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, T
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
  35cc3cef ckpt 1699: DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug A
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
  05373f92 ckpt 1697: CY1: draft wording fixed (two emulator runs, second green); job complete
  a3301a93 ckpt 1696: CY1: answer draft finalised (link, gate verdict #795/#794); heartbeat trig_0
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
