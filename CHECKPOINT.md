# CHECKPOINT 1434 — read me first, then TASKS.md

**Written:** 2026-11-18T00:50:03Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `8fd517a8` (this checkpoint is the commit after it)

## Just done
00:48 Z: #644 id 44206651719 (cs2,hp,cu,last@main) = 46 PASS 1 FAIL 45 INFO, ONLY FAIL = CU5 'curtain up: screen equals bare home (worst MAE<0.01)': screenshot 3 MAE 0.013856 bbox 586x542+10+5 while shots 2/4/5 = 0.00069 (status-bar clock only); no NATIVE CRASH; cs2/hp/cu/last all ran (123/223/110/19 s). Triage dispatched 00:48Z: cu@branch+shots=true #652 (see ci-shots, fetch at once), cu@main #653, cu@tag v0.9.11 #654 id 44210851917 (ids of #652/#653 via list in_progress perPage 4); x@branch #651 id 44210399111 due ~00:53Z

## Do this next
probe #651 (AD8 regex proof) then cu #652-#654 (due ~00:58Z): if head cu green = flake, if head red + tag green = look at ci-shots 171-cu-during-3 vs 170-cu-bare; then android.yml full_build=true; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  8fd517a8 ckpt 1433: 00:46 Z: VERDICTS pt@main #648 id 44208108238 GREEN; pt@branch #649 id 44208
  b42de57a ckpt 1432: 00:44Z: #643 AD8 FAIL = STALE TEST not product: CG1b G2 put a PSS split '[ja
  ba778629 ckpt 1431: 00:40Z: e1@tag #647 id 44207847156 GREEN (e1: head 3 of 4 green, tag 2 of 2)
  b491662e ckpt 1430: 00:34Z: e1@branch #645 id 44207837374 GREEN; x1,e1@main #646 id 44207842258 
  e96c9bba ckpt 1429: gated v0.9.12 (code 51) and pushed it: the handoff self-test, the full unit 
  cc784e1c ckpt 1428: PT TALLY 00:40Z (all solo): HEAD #631 BC2 only; #632 BA1+PT (1 of 5 fades; B
  31718304 ckpt 1427: PROBES 00:36Z (failed_only): pt@branch #634 id 44205747479 job 134471094065 
  6085afa9 ckpt 1426: VERIFIED 00:30Z (cheap probe 'No failed jobs'): base@head #637 green 29/0 (b
  46099daf ckpt 1425: ids of the 00:17Z dispatches: o@branch #641 id 44206636873; o@tag #642 id 44
  e3467532 ckpt 1424: #628 (run1 n6,o,x,ad,ae,pa,uf,cs2,hp,cu,last) CUT at the 2700s step limit (e
```
