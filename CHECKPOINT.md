# CHECKPOINT 1404 — read me first, then TASKS.md

**Written:** 2026-11-17T22:41:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `48287708` (this checkpoint is the commit after it)

## Just done
lg #624 id 44190767040 job 134419239929 (head 2702c070): 10 PASS 1 FAIL 65 INFO; FAIL=LG1 only (prelude first menu: no menu-opened line; window created 4729 ms after wake, rest ON +2.4 s). A/B in one build: C0 on=RESTING off=AWAY; C3 on=RESTING off=AWAY + void-in-the-dark line; C1,C2 RESTING; C4/C5 hide at DOWN 1-2 ms

## Do this next
analyse prelude LG1 (why no menu line, who creates the patch window late), decide fix, collect 3 reviewer reports; gate after fixes. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a3704328 ckpt 1403: lg run #624 id 44190767040 dispatched 22:30:46Z (head 2702c070, sections=lg)
  2702c070 ckpt 1402: QsMemory via helper STOCKMEM + 2h backoff: QsMemoryTest/TaskOriginTest green
  171cc637 ckpt 1401: TAKEOVER done 22:25Z (CG3b): B silent since 21:48Z (31 min), no CI in flight
  63330b2e ckpt 1398: takeover prepared (read-only): git merge-tree dry run of origin/main into my
  497a0215 ckpt 1397: CG3d done: research/CG1F_CONFIG_CHANGES_2026-11-17.md = prime suspect for th
  7336b488 ckpt 1396: CG3 (Tj 22:06Z: monitor the other session, finish and build the release) wri
  c6e2ebb8 ckpt 1395: heartbeat 22:04Z: stood down again (rule 2): session B ckpt 1400 (21:47Z, br
  6214dc56 ckpt 1394: heartbeat 21:55Z: STOOD DOWN (rule 2): session B (account B, branch ccr-b6bb
  9d635b14 ckpt 1400: CG1f verdict: app-uid dumpsys meminfo + activity processes refused (qm runs 
  45d8a44d ckpt 1399: QsMemory fallback: dumpsys activity processes -> lastPss+pid of stock launch
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
