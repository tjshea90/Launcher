# CHECKPOINT 1566 — read me first, then TASKS.md

**Written:** 2026-11-22T07:14:44Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `d3ffb7c3` (this checkpoint is the commit after it)

## Just done
CV1 RAW: #722 id 44994540888 {n,x,bs,se,mu,sf} 96P 1F MU-14b + NATIVE CRASH(libhwui); #723 id 44994543595 {t,ac,bl,ad,an,ss,last} 146P 4F AN7 AN8 AN9 AN9b + NATIVE CRASH(launcher-loader); #724 id 44994546265 {base,legacy,x1,e1,o,lq,cu,ul} 103P 4F W3b x2 Y1 X1 + NATIVE CRASH(GrallocUploadTh) (o PASSED, ul PASSED); #725 green; #726 RED O4x3 O3 AC3x2 (no native crash)

## Do this next
triage: add INFO diag to scenario O4/AC3 (lock log, paused/breaker), rerun unified,base,n,o,ac solo + same chain WITHOUT unified (control) + an,x1,e1 solo on head; non-product edits only; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d3ffb7c3 ckpt 1565: CV1: #725 id 44994548745 {uf,cs,hp,rb,wf,ul2} GREEN 100 PASS 0 FAIL (a5396e5
  46e65265 ckpt 1564: CV1: #726 id 44994708532 (unified,base,n,o,ac,e1,x1,rb,last, switch ON) is R
  a5f2454d ckpt 1563: CV1: TASKS CV1h (tablet checks for Tj) written
  1ca89bee ckpt 1562: CV1: HANDOFF block 0zd written (gate in flight); no product change since a53
  d237db2e ckpt 1561: CV1: release answer draft saved (research/CV1_RELEASE_MESSAGE_DRAFT_2026-11-
  f72c1be9 ckpt 1560: CV1: 5th gate run #726 id 44994708532 {unified,base,n,o,ac,e1,x1,rb,last} = 
  5fb1ba74 ckpt 1559: CV1: GATE dispatched on main@a5396e53 at 06:37: #722 id 44994540888 {n,x,bs,
  a5396e53 ckpt 1558: CV1: run #721 id 44993809638 (ul,ul2, main@d4f15e86) GREEN: real seed path p
  82c0e28a ckpt 1557: CV1: dispatched emulator #721 id 44993809638 (ul,ul2, main@d4f15e86, real se
  d4f15e86 ckpt 1556: CV1: scenario ul reworked (drawer retry, python3 sqlite3 injection of real c
```
