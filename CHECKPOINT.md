# CHECKPOINT 1495 — read me first, then TASKS.md

**Written:** 2026-11-19T00:10:32Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `e1055c7f` (this checkpoint is the commit after it)

## Just done
CI1: gate triage (head 3e02bf08, product unchanged since): A #678 6 FAIL (86 PASS): base crash breaker x2 + AR1 x2 + an AN7/AN8, one NATIVE CRASH #1 (jemalloc RenderThread); C #680 1 FAIL AC2 control + NATIVE CRASH #1 (jemalloc tzload); B #679 green; sr passed in A. Rerun solo dispatched 00:10Z on main c33f0c26: base,an and ac

## Do this next
at ~00:25Z read both reruns (list_workflow_runs emulator.yml main workflow_dispatch perPage 2, haiku subagent); both green = gate done -> ship.sh, android.yml full_build; a FAIL again = compare on tag v0.9.13 (ref) before touching code

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c33f0c26 ckpt 1494: CI1: gate raw verdict (head 3e02bf08): A #678 id 44423199764 (base,x1,t,x,an
  3492984f ckpt 1493: CI1: tier 0 local green (testDebugUnitTest + lintDebug, BUILD SUCCESSFUL, GR
  9f4d3d35 ckpt 1492: CI1: TASKS ticked (CI1a/c/d with proofs; CI1b left open on purpose), HANDOFF
  3e02bf08 ckpt 1491: CI1: VERDICT sr main #676 id 44421271607 = 13 PASS 0 FAIL (stock 32 MB/1 Act
  6c027d6c ckpt 1490: CI1: version 0.9.14 code 53, draft research/CI1_RELEASE_MESSAGE_DRAFT_2026-1
  f47446c4 ckpt 1489: CI1: ss #675 id 44418711509 job 135205705700 = 73 PASS 0 FAIL (head 8a46ec64
  328c0636 ckpt 1488: CI1: reviewer ac46f883 read (7 LOW): fixed 1 (no forced reading without help
  73eebb4a ckpt 1487: CI1: sr #674 id 44417744367 job 135202357753 = 12 PASS 1 FAIL: SR2 was a TES
  083eb8f8 ckpt 1486: CI1: sr #674 id 44417744367 finished (result being read by a subagent); ss m
  5c4ef2a0 ckpt 1485: CI1: research/CI1_AUDIT_2026-11-18.md written; ss G1 check added (SS1g); rev
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
