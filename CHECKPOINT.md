# CHECKPOINT 1436 — read me first, then TASKS.md

**Written:** 2026-11-18T00:55:47Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `bdb841e7` (this checkpoint is the commit after it)

## Just done
00:54 Z: CU5 triage: cu solo GREEN on head x2 (#652 branch+shots id 44210843037, #653 main id 44210847147) and on tag (#654 id 44210851917); passing pictures: 4 screenshots in the curtain's life identical (MAE 0.001183, clock band only); failing frame of #644 unknown (not saved). Exact chain cs2,hp,cu,last rerun on branch = #655 id 44211556464 (dispatched 00:53:55Z, due ~01:05Z). Wrote research/V0912_GATE_RECORD_2026-11-18.md (gate table)

## Do this next
probe #655 with failed_only + list_workflow_jobs (completed?); if green: android.yml full_build=true ref main, get_release_by_tag v0.9.12, record-release.sh with the ship note of ckpt 1429, fill [PENDING] in research/V0912_RELEASE_MESSAGE_DRAFT_2026-11-17.md, answer Tj LAST; if CU5 fails again: run chain cs2,hp,cu,last on tag v0.9.11 and compare; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4b98508b ckpt 1435: 00:51 Z: x@branch #651 id 44210399111 job 134486989957 = SUCCESS (scenario 0
  696add5b ckpt 1434: 00:48 Z: #644 id 44206651719 (cs2,hp,cu,last@main) = 46 PASS 1 FAIL 45 INFO,
  8fd517a8 ckpt 1433: 00:46 Z: VERDICTS pt@main #648 id 44208108238 GREEN; pt@branch #649 id 44208
  b42de57a ckpt 1432: 00:44Z: #643 AD8 FAIL = STALE TEST not product: CG1b G2 put a PSS split '[ja
  ba778629 ckpt 1431: 00:40Z: e1@tag #647 id 44207847156 GREEN (e1: head 3 of 4 green, tag 2 of 2)
  b491662e ckpt 1430: 00:34Z: e1@branch #645 id 44207837374 GREEN; x1,e1@main #646 id 44207842258 
  e96c9bba ckpt 1429: gated v0.9.12 (code 51) and pushed it: the handoff self-test, the full unit 
  cc784e1c ckpt 1428: PT TALLY 00:40Z (all solo): HEAD #631 BC2 only; #632 BA1+PT (1 of 5 fades; B
  31718304 ckpt 1427: PROBES 00:36Z (failed_only): pt@branch #634 id 44205747479 job 134471094065 
  6085afa9 ckpt 1426: VERIFIED 00:30Z (cheap probe 'No failed jobs'): base@head #637 green 29/0 (b
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
