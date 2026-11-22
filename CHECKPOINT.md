# CHECKPOINT 1568 — read me first, then TASKS.md

**Written:** 2026-11-22T07:18:28Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `3218da2b` (this checkpoint is the commit after it)

## Just done
CV1 triage runs on main@3218da2b (product==a5396e53): #727 id 44997500491 = R1 unified,base,n,o,ac; #728 id 44997503118 = R2 control base,n,o,ac (branch ref, switch OFF); #729 id 44997505484 = R3 base,x1,e1,an,mu; all started 07:16, ~16 min

## Do this next
read #727/#728/#729 at ~07:34 (agent per run: header + FAIL + INFO 'O4 detail'/'AC3 detail'); if R1 red & R2 green = unified defect in lock path (look at CrashGuard.automationOff / lockPrepare / Unified.active); heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3218da2b ckpt 1567: CV1 triage: scenario INFO diag for O4/AC3 (non-product; product diff vs a539
  a27a72bb ckpt 1566: CV1 RAW: #722 id 44994540888 {n,x,bs,se,mu,sf} 96P 1F MU-14b + NATIVE CRASH(
  d3ffb7c3 ckpt 1565: CV1: #725 id 44994548745 {uf,cs,hp,rb,wf,ul2} GREEN 100 PASS 0 FAIL (a5396e5
  46e65265 ckpt 1564: CV1: #726 id 44994708532 (unified,base,n,o,ac,e1,x1,rb,last, switch ON) is R
  a5f2454d ckpt 1563: CV1: TASKS CV1h (tablet checks for Tj) written
  1ca89bee ckpt 1562: CV1: HANDOFF block 0zd written (gate in flight); no product change since a53
  d237db2e ckpt 1561: CV1: release answer draft saved (research/CV1_RELEASE_MESSAGE_DRAFT_2026-11-
  f72c1be9 ckpt 1560: CV1: 5th gate run #726 id 44994708532 {unified,base,n,o,ac,e1,x1,rb,last} = 
  5fb1ba74 ckpt 1559: CV1: GATE dispatched on main@a5396e53 at 06:37: #722 id 44994540888 {n,x,bs,
  a5396e53 ckpt 1558: CV1: run #721 id 44993809638 (ul,ul2, main@d4f15e86) GREEN: real seed path p
```
