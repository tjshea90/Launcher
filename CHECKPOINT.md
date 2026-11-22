# CHECKPOINT 1576 — read me first, then TASKS.md

**Written:** 2026-11-22T08:10:21Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `00ed6f69` (this checkpoint is the commit after it)

## Just done
CV1 RAW: #732 id 45000028314 (unified,base,n,o,ac, main@84de8fa0) GREEN 77P 0F (1 native crash launcher-loader, harmless); AC3 passes, home presses 221 ms apart. So #726 (O4x3,O3,AC3x2) and #727 (AC3x3) did not reproduce on same product code in 3 more runs (#730 #732 #733); AC8 failed once in #733 only (logcat PROFILE line race)

## Do this next
add UL24 (double Home FROM AN APP in unified: mode custom + INFO gap between the two ROUTER presses) to section ul as non-product check, run ul solo (and unified,ul2?); then decide release; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  534e9610 ckpt 1575: CV1 RAW: #733 id 45000031118 (unified,o,ac, branch ref) 41P 1F = AC8 'custom
  6f351625 ckpt 1574: CV1 triage runs on main@84de8fa0: #732 id 45000028314 = unified,base,n,o,ac 
  ba6ee6cd ckpt 1573: CV1 RAW: #730 id 44999067540 (unified,ac solo, head) GREEN 31P 0F: AC3 passe
  84de8fa0 ckpt 1572: CV1 RAW: #731 id 44999069824 (an solo, head) GREEN 17P 0F no native crash =>
  13be4537 ckpt 1571: CV1 triage runs on main@9725a905: #730 id 44999067540 = unified,ac; #731 id 
  9725a905 ckpt 1570: CV1 RAW: #728 id 44997503118 control (base,n,o,ac, switch OFF, branch ref) 7
  78ed48e7 ckpt 1569: CV1 RAW: #727 id 44997500491 (unified,base,n,o,ac) 74P 3F = AC3 x3 only (O4/
  7d098702 ckpt 1568: CV1 triage runs on main@3218da2b (product==a5396e53): #727 id 44997500491 = 
  3218da2b ckpt 1567: CV1 triage: scenario INFO diag for O4/AC3 (non-product; product diff vs a539
  a27a72bb ckpt 1566: CV1 RAW: #722 id 44994540888 {n,x,bs,se,mu,sf} 96P 1F MU-14b + NATIVE CRASH(
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
