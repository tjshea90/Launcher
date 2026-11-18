# CHECKPOINT 1438 — read me first, then TASKS.md

**Written:** 2026-11-18T01:05:23Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `d1b82984` (this checkpoint is the commit after it)

## Just done
01:04 Z: GATE COMPLETE on code 1c135985(+version bump): chain cs2,hp,cu,last@branch #655 id 44211556464 job 134490942627 = SUCCESS (scenario 00:55:03-01:04:00Z, 0 FAIL) so #644's CU5 = flake (cu: head 4 of 5 green, tag 1 of 1). Every first-run FAIL of the gate now has a green rerun on the same product code (o #641/#642, AD8 fix + x #651, cu #652-#655, base #637/#638, b6/e1 #639-#647, pt #648/#649); pt also red on tag v0.9.11 (#636). Nothing built yet

## Do this next
dispatch android.yml full_build=true ref ccr-16772798-h13w43 (check android.yml run list first: none in flight), read result at +3 min, get_release_by_tag v0.9.12, then record-release.sh v0.9.12 with the ship note of ckpt 1429; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ae412f3c ckpt 1437: 00:56 Z: claims in the release draft checked against code: unit tests 1130/0
  c28cd895 ckpt 1436: 00:54 Z: CU5 triage: cu solo GREEN on head x2 (#652 branch+shots id 44210843
  4b98508b ckpt 1435: 00:51 Z: x@branch #651 id 44210399111 job 134486989957 = SUCCESS (scenario 0
  696add5b ckpt 1434: 00:48 Z: #644 id 44206651719 (cs2,hp,cu,last@main) = 46 PASS 1 FAIL 45 INFO,
  8fd517a8 ckpt 1433: 00:46 Z: VERDICTS pt@main #648 id 44208108238 GREEN; pt@branch #649 id 44208
  b42de57a ckpt 1432: 00:44Z: #643 AD8 FAIL = STALE TEST not product: CG1b G2 put a PSS split '[ja
  ba778629 ckpt 1431: 00:40Z: e1@tag #647 id 44207847156 GREEN (e1: head 3 of 4 green, tag 2 of 2)
  b491662e ckpt 1430: 00:34Z: e1@branch #645 id 44207837374 GREEN; x1,e1@main #646 id 44207842258 
  e96c9bba ckpt 1429: gated v0.9.12 (code 51) and pushed it: the handoff self-test, the full unit 
  cc784e1c ckpt 1428: PT TALLY 00:40Z (all solo): HEAD #631 BC2 only; #632 BA1+PT (1 of 5 fades; B
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
