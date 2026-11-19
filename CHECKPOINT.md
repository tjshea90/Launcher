# CHECKPOINT 1503 — read me first, then TASKS.md

**Written:** 2026-11-19T01:01:08Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `68877c24` (this checkpoint is the commit after it)

## Just done
CI1: ship.sh green (handoff, unit suite), v0.9.14 code 53 pushed main a92505cd; android.yml full_build=true dispatched 00:59Z on main

## Do this next
at ~01:03Z get_release_by_tag v0.9.14 (target_commitish a92505cd); green = bash tools/record-release.sh v0.9.14 note; tick CI1e; HANDOFF 0zb; send Tj the CI1 draft with the Release link on its own line

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a92505cd ckpt 1502: gated v0.9.14 (code 53) and pushed it: the handoff self-test, the full unit 
  36359368 ckpt 1501: CI1: GATE DONE on identical product code (git diff 3e02bf08..: tools only): 
  e529353f ckpt 1500: CI1: base,an x2 dispatched 00:45Z (main a2b5316c + branch ref); ids = newest
  a2b5316c ckpt 1499: CI1: #685 base,an main FAIL = only 'hidden: REINSTALLED app is forgotten' (A
  1ff86b86 ckpt 1498: CI1: triage: an solo main #683 id 44428928340 SUCCESS; base,an on TAG v0.9.1
  8d1a658d ckpt 1497: CI1: base,an #681 id 44426690735 = 41 PASS 1 FAIL: only AN9b (lock log shows
  681c0607 ckpt 1496: CI1: rerun verdict: ac solo #682 id 44426695151 SUCCESS (AC2 was the native-
  8eca3e05 ckpt 1495: CI1: gate triage (head 3e02bf08, product unchanged since): A #678 6 FAIL (86
  c33f0c26 ckpt 1494: CI1: gate raw verdict (head 3e02bf08): A #678 id 44423199764 (base,x1,t,x,an
  3492984f ckpt 1493: CI1: tier 0 local green (testDebugUnitTest + lintDebug, BUILD SUCCESSFUL, GR
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
