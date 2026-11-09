# CHECKPOINT 836 — read me first, then TASKS.md

**Written:** 2026-11-09T22:52:07Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `5d55187` (this checkpoint is the commit after it)

## Just done
#474 base,legacy ONE run (306152a): 40P/4F recents,W5x2,W3b + THREE native crashes all inside jemalloc (main: arena_bin_malloc_hard; launcher-loader: a0ialloc; launcher-loader: extent_deregister_impl); base part clean, FAILs all in legacy after crash #3. Version bumped 0.9.0/code 39. heal() now prints 12 frames. Audit BN1M2 done (78 rows, 2 diffs D1 D2, 8 risks)

## Do this next
1) dispatch 3 parallel reruns base,legacy on the release commit: main; ccr-5ef85f7a-ngtgj1; main with base,legacy,last. 2) bash ship.sh (local tier 0, background, read GRADLE/SHIP exit). 3) first run with 0 FAIL -> android.yml full_build main. All crash -> ship on documented evidence (tag crashes too). Then BN1p audit fixes D1 D2 R3-R5 R7 R8 as v0.9.1, BN6 native crash hunt. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  098143a ckpt 835: legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
  299085c ckpt 833: #470 base on TAG v0.8.13: 28 PASS 1 FAIL + native crash (released tag crashes 
  a4583b1 ckpt 832: Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
  e024dae ckpt 830: Solo reruns on eaeb47e: #466 42683210584 base, #467 42683216363 o, #468 426832
  eaeb47e ckpt 829: GATE v0.9.0 on 09215cc: #465 rg OK; #463 (42671984609) 5 FAIL legacy/lq; #464 
  c5dca2d ckpt 828: Gate attempt 2 of #463 (42671984609) and #464 (42671992301) RUNNING since 18:4
  0cf5f0e ckpt 827: Gate #463/#464 (42671984609 / 42671992301) FAILED at +57 s with ZERO jobs (no 
  e676c11 ckpt 826: BO1d PROVEN: heartbeat first fire 18:27:33Z reached this session idle (queued 
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
