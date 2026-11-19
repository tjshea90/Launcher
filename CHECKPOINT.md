# CHECKPOINT 1499 — read me first, then TASKS.md

**Written:** 2026-11-19T00:42:19Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `41321437` (this checkpoint is the commit after it)

## Just done
CI1: #685 base,an main FAIL = only 'hidden: REINSTALLED app is forgotten' (AN9b passed this time; #681 had AN9b only, base ok): two different single FAILs in the same chain on identical product code = timing flakes; scenario hardened (hidden check polls up to 16 s + INFO with the wait; no product change, git diff 3e02bf08 -- app launcher3 phoneguide empty); rerun base,an x2 (main + branch ref)

## Do this next
read both reruns; if base,an both green -> gate done (A+C parts covered: base,an,ac,sr,ss green; B green) -> ship.sh; hidden INFO line says the delay

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  1ff86b86 ckpt 1498: CI1: triage: an solo main #683 id 44428928340 SUCCESS; base,an on TAG v0.9.1
  8d1a658d ckpt 1497: CI1: base,an #681 id 44426690735 = 41 PASS 1 FAIL: only AN9b (lock log shows
  681c0607 ckpt 1496: CI1: rerun verdict: ac solo #682 id 44426695151 SUCCESS (AC2 was the native-
  8eca3e05 ckpt 1495: CI1: gate triage (head 3e02bf08, product unchanged since): A #678 6 FAIL (86
  c33f0c26 ckpt 1494: CI1: gate raw verdict (head 3e02bf08): A #678 id 44423199764 (base,x1,t,x,an
  3492984f ckpt 1493: CI1: tier 0 local green (testDebugUnitTest + lintDebug, BUILD SUCCESSFUL, GR
  9f4d3d35 ckpt 1492: CI1: TASKS ticked (CI1a/c/d with proofs; CI1b left open on purpose), HANDOFF
  3e02bf08 ckpt 1491: CI1: VERDICT sr main #676 id 44421271607 = 13 PASS 0 FAIL (stock 32 MB/1 Act
  6c027d6c ckpt 1490: CI1: version 0.9.14 code 53, draft research/CI1_RELEASE_MESSAGE_DRAFT_2026-1
  f47446c4 ckpt 1489: CI1: ss #675 id 44418711509 job 135205705700 = 73 PASS 0 FAIL (head 8a46ec64
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
