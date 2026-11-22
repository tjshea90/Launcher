# CHECKPOINT 1569 — read me first, then TASKS.md

**Written:** 2026-11-22T07:35:05Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `4b33053e` (this checkpoint is the commit after it)

## Just done
CV1 RAW: #727 id 44997500491 (unified,base,n,o,ac) 74P 3F = AC3 x3 only (O4/O3 PASSED this time => #726's O4 was a flake), 1 native crash; #729 id 44997505484 (base,x1,e1,an,mu) 75P 1F = AN9b only + native crash (MU-14b, X1, Y1, W3b all PASS now)

## Do this next
read #728 control (agent a9ced1c2); AC3 fails with unified ON twice: find why the double Home does not return to custom (full AC3 detail line from agent ae788f29); AN9b: rerun an solo head + tag v0.9.16; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  7d098702 ckpt 1568: CV1 triage runs on main@3218da2b (product==a5396e53): #727 id 44997500491 = 
  3218da2b ckpt 1567: CV1 triage: scenario INFO diag for O4/AC3 (non-product; product diff vs a539
  a27a72bb ckpt 1566: CV1 RAW: #722 id 44994540888 {n,x,bs,se,mu,sf} 96P 1F MU-14b + NATIVE CRASH(
  d3ffb7c3 ckpt 1565: CV1: #725 id 44994548745 {uf,cs,hp,rb,wf,ul2} GREEN 100 PASS 0 FAIL (a5396e5
  46e65265 ckpt 1564: CV1: #726 id 44994708532 (unified,base,n,o,ac,e1,x1,rb,last, switch ON) is R
  a5f2454d ckpt 1563: CV1: TASKS CV1h (tablet checks for Tj) written
  1ca89bee ckpt 1562: CV1: HANDOFF block 0zd written (gate in flight); no product change since a53
  d237db2e ckpt 1561: CV1: release answer draft saved (research/CV1_RELEASE_MESSAGE_DRAFT_2026-11-
  f72c1be9 ckpt 1560: CV1: 5th gate run #726 id 44994708532 {unified,base,n,o,ac,e1,x1,rb,last} = 
  5fb1ba74 ckpt 1559: CV1: GATE dispatched on main@a5396e53 at 06:37: #722 id 44994540888 {n,x,bs,
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
