# CHECKPOINT 835 — read me first, then TASKS.md

**Written:** 2026-11-09T22:36:44Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `e796a99` (this checkpoint is the commit after it)

## Just done
legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1): #472 head 17P/3F (reset2 + W5x2 after native crash launcher-loader jemalloc), #473 TAG v0.8.13 19P/1F reset2. gate.sh BIND legacy->base fixed. Dispatched base,legacy ONE run: #474 id 42713242884 on 306152a (22:35Z)

## Do this next
timer 22:47Z: read #474 (get_workflow_run; failed_only tail 75). Green (reset2 passes after base) -> bump 0.9.0/code 39, ship.sh, android.yml full_build main, get_release_by_tag v0.9.0, record-release. A native crash again -> rerun base,legacy; if reset2 fails after base on head but not tag -> real. Audit subagent writes research/BN1M2_AUDIT_2026-11-09.md. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
  299085c ckpt 833: #470 base on TAG v0.8.13: 28 PASS 1 FAIL + native crash (released tag crashes 
  a4583b1 ckpt 832: Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
  e024dae ckpt 830: Solo reruns on eaeb47e: #466 42683210584 base, #467 42683216363 o, #468 426832
  eaeb47e ckpt 829: GATE v0.9.0 on 09215cc: #465 rg OK; #463 (42671984609) 5 FAIL legacy/lq; #464 
  c5dca2d ckpt 828: Gate attempt 2 of #463 (42671984609) and #464 (42671992301) RUNNING since 18:4
  0cf5f0e ckpt 827: Gate #463/#464 (42671984609 / 42671992301) FAILED at +57 s with ZERO jobs (no 
  e676c11 ckpt 826: BO1d PROVEN: heartbeat first fire 18:27:33Z reached this session idle (queued 
  0c91d62 ckpt 825: HANDOFF.md architecture map: Settings look-alike row. Gate #463/#464 running (
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
