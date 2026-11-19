# CHECKPOINT 1501 — read me first, then TASKS.md

**Written:** 2026-11-19T00:57:15Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `e529353f` (this checkpoint is the commit after it)

## Just done
CI1: GATE DONE on identical product code (git diff 3e02bf08..: tools only): base,an green x2 (#686 id 44431167364 main, #687 id 44431172990 branch, head a2b5316c); B #679 green; ac solo #682 green; sr #676 + ss #677 green; #678/#680 only flakes beside a native crash; tag v0.9.13 base,an #684 green

## Do this next
bash ship.sh note; then android.yml full_build on the branch; get_release_by_tag v0.9.14; record-release.sh; send Tj research/CI1_RELEASE_MESSAGE_DRAFT (fill gate sentence)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e529353f ckpt 1500: CI1: base,an x2 dispatched 00:45Z (main a2b5316c + branch ref); ids = newest
  a2b5316c ckpt 1499: CI1: #685 base,an main FAIL = only 'hidden: REINSTALLED app is forgotten' (A
  1ff86b86 ckpt 1498: CI1: triage: an solo main #683 id 44428928340 SUCCESS; base,an on TAG v0.9.1
  8d1a658d ckpt 1497: CI1: base,an #681 id 44426690735 = 41 PASS 1 FAIL: only AN9b (lock log shows
  681c0607 ckpt 1496: CI1: rerun verdict: ac solo #682 id 44426695151 SUCCESS (AC2 was the native-
  8eca3e05 ckpt 1495: CI1: gate triage (head 3e02bf08, product unchanged since): A #678 6 FAIL (86
  c33f0c26 ckpt 1494: CI1: gate raw verdict (head 3e02bf08): A #678 id 44423199764 (base,x1,t,x,an
  3492984f ckpt 1493: CI1: tier 0 local green (testDebugUnitTest + lintDebug, BUILD SUCCESSFUL, GR
  9f4d3d35 ckpt 1492: CI1: TASKS ticked (CI1a/c/d with proofs; CI1b left open on purpose), HANDOFF
  3e02bf08 ckpt 1491: CI1: VERDICT sr main #676 id 44421271607 = 13 PASS 0 FAIL (stock 32 MB/1 Act
```
