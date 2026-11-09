# CHECKPOINT 832 — read me first, then TASKS.md

**Written:** 2026-11-09T19:39:48Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `4a49b3f` (this checkpoint is the commit after it)

## Just done
Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base on main 4a49b3f (frames capture)

## Do this next
timer 19:47: read #470 FIRST (get_workflow_run, then get_job_logs failed_only tail 75 -> RESULTS header, FAIL lines, NATIVE CRASH lines) then #471 (frames: ...). v0.8.13 clean = regression in BN1 (bisect base on older commits of this branch); v0.8.13 same 4 FAIL + crashes = environment -> ship. Heartbeat id: trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
  e024dae ckpt 830: Solo reruns on eaeb47e: #466 42683210584 base, #467 42683216363 o, #468 426832
  eaeb47e ckpt 829: GATE v0.9.0 on 09215cc: #465 rg OK; #463 (42671984609) 5 FAIL legacy/lq; #464 
  c5dca2d ckpt 828: Gate attempt 2 of #463 (42671984609) and #464 (42671992301) RUNNING since 18:4
  0cf5f0e ckpt 827: Gate #463/#464 (42671984609 / 42671992301) FAILED at +57 s with ZERO jobs (no 
  e676c11 ckpt 826: BO1d PROVEN: heartbeat first fire 18:27:33Z reached this session idle (queued 
  0c91d62 ckpt 825: HANDOFF.md architecture map: Settings look-alike row. Gate #463/#464 running (
  1ff741f ckpt 824: Gate #465 (rg solo, 42671999660, 09215cc) = SUCCESS (done 18:16:50). #463/#464
  53ad8fd ckpt 823: scenario heal(): NATIVE CRASH now also prints an INFO detail line (Fatal signa
  009cb18 ckpt 822: GATE dispatched 18:12Z on 09215cc: #463 id 42671984609 (legacy,n6,t,x,ac,bl,lq
```
