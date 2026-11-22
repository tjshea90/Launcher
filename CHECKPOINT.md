# CHECKPOINT 1558 — read me first, then TASKS.md

**Written:** 2026-11-22T06:37:08Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `82c0e28a` (this checkpoint is the commit after it)

## Just done
CV1: run #721 id 44993809638 (ul,ul2, main@d4f15e86) GREEN: real seed path proven: 6 rows injected into custom layout, unlocked seeded with 4 (app, folder, 2 folder items; widget + deep shortcut excluded), min id 1000000001, folder links ok, no orphans, no low ids; UL11-15 pass via setmode fallback (failsafe variants a/b/c did not flip on the emulator; INFO says so). lintDebug local green, full unit suite green

## Do this next
run bash tools/gate.sh 1d8b6b2d, dispatch its 3 runs on ref main (record run ids in ckpt), read each when ended (~34 min), triage per CLAUDE.md; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  82c0e28a ckpt 1557: CV1: dispatched emulator #721 id 44993809638 (ul,ul2, main@d4f15e86, real se
  d4f15e86 ckpt 1556: CV1: scenario ul reworked (drawer retry, python3 sqlite3 injection of real c
  9da41d62 ckpt 1555: CV1: run #720 id 44992764510 (ul,ul2, main@87bfc562) = 26 PASS 1 FAIL 16 INF
  5358e822 ckpt 1554: CV1: dispatched emulator #720 id 44992764510 (ul,ul2, main@87bfc562, review-
  87bfc562 ckpt 1553: CV1: review-2 fixes in (BgDataModel unpin skip, allLayoutDbs opens unlocked 
  49db2cb3 ckpt 1552: CV1: run #719 id 44991880109 (ul,ul2, main@9d1d6c06) = 22 PASS 5 FAIL: UL3c 
  bc824069 ckpt 1551: CV1: review round 2 saved (research/CV1_REVIEW2_*): 1 HIGH (BgDataModel unpi
  6625e904 ckpt 1550: CV1: dispatched emulator #719 id 44991880109 (ul,ul2, main@9d1d6c06, fixes) 
  9d1d6c06 ckpt 1549: CV1: review fixes in: launcher3 per-DB empty flag + atomic Java seed (no wid
  eaa7fdd0 ckpt 1548: CV1: run #718 id 44990179693 (ul,ul2 on main@390f5cfb) = FAIL 21 PASS 5 FAIL
```
