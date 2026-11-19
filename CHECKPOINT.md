# CHECKPOINT 1497 — read me first, then TASKS.md

**Written:** 2026-11-19T00:27:02Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `681c0607` (this checkpoint is the commit after it)

## Just done
CI1: base,an #681 id 44426690735 = 41 PASS 1 FAIL: only AN9b (lock log shows 'ANIM ON again: accessibility helper not running (process start + 2.5 s)', check wants 'lock screen (process start)'); base + AN7/AN8 PASSED this time; no native crash. Triage dispatched 00:27Z: an solo main, base,an main, base,an ref v0.9.13 (tag)

## Do this next
read the 3 triage runs ~00:40Z (list_workflow_runs emulator.yml workflow_dispatch perPage 3, event only, branch filter drops the tag run); tag also fails AN9b = pre-existing flake, else compare AN9b path with diff (AnimationSpeedup untouched by CI1)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  681c0607 ckpt 1496: CI1: rerun verdict: ac solo #682 id 44426695151 SUCCESS (AC2 was the native-
  8eca3e05 ckpt 1495: CI1: gate triage (head 3e02bf08, product unchanged since): A #678 6 FAIL (86
  c33f0c26 ckpt 1494: CI1: gate raw verdict (head 3e02bf08): A #678 id 44423199764 (base,x1,t,x,an
  3492984f ckpt 1493: CI1: tier 0 local green (testDebugUnitTest + lintDebug, BUILD SUCCESSFUL, GR
  9f4d3d35 ckpt 1492: CI1: TASKS ticked (CI1a/c/d with proofs; CI1b left open on purpose), HANDOFF
  3e02bf08 ckpt 1491: CI1: VERDICT sr main #676 id 44421271607 = 13 PASS 0 FAIL (stock 32 MB/1 Act
  6c027d6c ckpt 1490: CI1: version 0.9.14 code 53, draft research/CI1_RELEASE_MESSAGE_DRAFT_2026-1
  f47446c4 ckpt 1489: CI1: ss #675 id 44418711509 job 135205705700 = 73 PASS 0 FAIL (head 8a46ec64
  328c0636 ckpt 1488: CI1: reviewer ac46f883 read (7 LOW): fixed 1 (no forced reading without help
  73eebb4a ckpt 1487: CI1: sr #674 id 44417744367 job 135202357753 = 12 PASS 1 FAIL: SR2 was a TES
```
