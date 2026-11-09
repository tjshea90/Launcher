# CHECKPOINT 837 — read me first, then TASKS.md

**Written:** 2026-11-09T22:55:01Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `d5d4f94` (this checkpoint is the commit after it)

## Just done
reruns #475 42715672216 main, #476 42715676675 branch, #477 42715681398 main+last (base,legacy) on 3da7cb8; TASKS BN1p BN6 written; ship.sh running

## Do this next
23:05Z timer: read #475-#477 (get_workflow_run; failed_only tail 44); read scratchpad ship.log SHIP_EXIT. First 0-FAIL run or ship on evidence -> android.yml full_build main -> get_release_by_tag v0.9.0 -> record-release. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3da7cb8 ckpt 836: #474 base,legacy ONE run (306152a): 40P/4F recents,W5x2,W3b + THREE native cra
  098143a ckpt 835: legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
  299085c ckpt 833: #470 base on TAG v0.8.13: 28 PASS 1 FAIL + native crash (released tag crashes 
  a4583b1 ckpt 832: Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
  e024dae ckpt 830: Solo reruns on eaeb47e: #466 42683210584 base, #467 42683216363 o, #468 426832
  eaeb47e ckpt 829: GATE v0.9.0 on 09215cc: #465 rg OK; #463 (42671984609) 5 FAIL legacy/lq; #464 
  c5dca2d ckpt 828: Gate attempt 2 of #463 (42671984609) and #464 (42671992301) RUNNING since 18:4
  0cf5f0e ckpt 827: Gate #463/#464 (42671984609 / 42671992301) FAILED at +57 s with ZERO jobs (no 
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
