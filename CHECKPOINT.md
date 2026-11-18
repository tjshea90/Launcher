# CHECKPOINT 1437 — read me first, then TASKS.md

**Written:** 2026-11-18T00:58:02Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `8ce06edf` (this checkpoint is the commit after it)

## Just done
00:56 Z: claims in the release draft checked against code: unit tests 1130/0 fail (app xml), StrayCustomGuardTest 28 PatchRestTest 46 QsMemoryTest 21 DexoptStateTest 15 ModeFlipsTest 5, QsMemory GAP 25 min, guard RETRY_BUDGET 20 s, qs_memory+guard release ON / debug OFF, patch_dark_settle default ON; B idle since 21:48:06Z (main==my tip c28cd895). Draft MEASURED line rewritten honestly (only [PENDING #655] left). Waiting for chain rerun #655 id 44211556464 (cs2,hp,cu,last@branch, due ~01:05Z), timer braqy2jnq fires 01:04Z

## Do this next
probe #655 (list_workflow_jobs 44211556464 for status, then failed_only); green => android.yml full_build=true ref main, get_release_by_tag v0.9.12, record-release.sh v0.9.12 with the ship note of ckpt 1429, TASKS ticks CG1a-g CG3c, HANDOFF 0z, delete note research/CG1_SESSION_A_NOTE_2026-11-17.md, answer Tj LAST; red at CU5 => chain on tag v0.9.11 + shots; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (delete after record)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c28cd895 ckpt 1436: 00:54 Z: CU5 triage: cu solo GREEN on head x2 (#652 branch+shots id 44210843
  4b98508b ckpt 1435: 00:51 Z: x@branch #651 id 44210399111 job 134486989957 = SUCCESS (scenario 0
  696add5b ckpt 1434: 00:48 Z: #644 id 44206651719 (cs2,hp,cu,last@main) = 46 PASS 1 FAIL 45 INFO,
  8fd517a8 ckpt 1433: 00:46 Z: VERDICTS pt@main #648 id 44208108238 GREEN; pt@branch #649 id 44208
  b42de57a ckpt 1432: 00:44Z: #643 AD8 FAIL = STALE TEST not product: CG1b G2 put a PSS split '[ja
  ba778629 ckpt 1431: 00:40Z: e1@tag #647 id 44207847156 GREEN (e1: head 3 of 4 green, tag 2 of 2)
  b491662e ckpt 1430: 00:34Z: e1@branch #645 id 44207837374 GREEN; x1,e1@main #646 id 44207842258 
  e96c9bba ckpt 1429: gated v0.9.12 (code 51) and pushed it: the handoff self-test, the full unit 
  cc784e1c ckpt 1428: PT TALLY 00:40Z (all solo): HEAD #631 BC2 only; #632 BA1+PT (1 of 5 fades; B
  31718304 ckpt 1427: PROBES 00:36Z (failed_only): pt@branch #634 id 44205747479 job 134471094065 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
