# CHECKPOINT 1704 — read me first, then TASKS.md

**Written:** 2026-11-27T23:56:42Z · **tests:** all 12 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/wonderful-franklin-vlm90o` · **builds on:** `b9065095` (this checkpoint is the commit after it)

## Just done
DA1b: findings of run #796 written (research/DA1_PERF_PROBE_2026-11-27.md, TASKS DA1e); run #797 id 46084969954 (sections pr,pr2 shots=true) dispatched 23:53:44Z; trace_top --tree committed

## Do this next
wait for ci-shots commit naming run 46084969954 (background watcher script scratchpad/wait_ci.sh), then git show origin/ci-shots:pr-summary.txt / pr-summary2.txt / pr-profile.txt; decide P1 (class lines in baseline-prof.txt) from the pr2 cold-start numbers; no heartbeat armed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  aefb878e ckpt 1703: DA1b: scenario pr gets the PIN-unlock and power-menu phases; NEW section pr2
  622ba1fb ckpt 1702: DA1 facts so far (all [V] by hand): debug APK resources.arsc + dex are STORE
  44e5756c ckpt 1701: DA1b: dispatched emulator.yml sections=pr shots=true: run #796 id 4608351418
  618215c8 ckpt 1700: DA1b: perf probe built (tools/trace_top.py ART-trace reader + selftest, emul
  35cc3cef ckpt 1699: DA1 written into TASKS (Tj's speed / RAM / CPU request, sixth pass); debug A
  6e22778a ckpt 1698: CZ1 tidy: CLAUDE.md 81K to 31K (lessons to LESSONS.md), dup kotlin skill + o
  05373f92 ckpt 1697: CY1: draft wording fixed (two emulator runs, second green); job complete
  a3301a93 ckpt 1696: CY1: answer draft finalised (link, gate verdict #795/#794); heartbeat trig_0
  2c7b35b0 ckpt 1695: CY1 SHIPPED v0.9.20: Release published 15:13Z (android.yml run 61 id 4542247
  69493ceb ckpt 1694: CY1d: ship.sh OK (handoff, unit suite, lint) v0.9.20 code 59 pushed, main==h
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
