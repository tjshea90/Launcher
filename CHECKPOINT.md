# CHECKPOINT 840 — read me first, then TASKS.md

**Written:** 2026-11-09T23:20:32Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `2b616bb` (this checkpoint is the commit after it)

## Just done
8 base runs dispatched 23:12Z on ckpt 839 (a06970f): tag v0.8.13 x4 (base; base,last; base,x1; base,e1), main x2 (base; base,last), branch ccr-5ef85f7a-ngtgj1 x2 (base; base,last). Question: breaker check fails 3 of 7 on head (464,466,476) - regression or flake?

## Do this next
NOW: haiku subagent extracts the 8 runs (list_workflow_runs emulator.yml event workflow_dispatch perPage 10, created after 23:11Z; per run head_branch, INFO sections line, RESULTS header, FAIL lines, 'breaker diag' INFO, NATIVE CRASH count). Verdict: breaker FAILs on tag too/ head diag shows crashes recorded < 3 -> read wait/t1-t3; tag never fails + head fails -> REAL regression: bisect (suspects Mode.set/RestoreStore/TjApplication syncSoon, SsFuse in CrashGuard.record). Both flake -> android.yml full_build main (ship.sh already passed, version 0.9.0 code 39, main=a06970f), record-release, final message. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a06970f ckpt 839: scenario: breaker diag + wait for process; reruns 475 (3 crashes; FAIL recents
  a0359ad ckpt 838: gated v0.9.0 (code 39) and pushed it: the handoff self-test, the full unit sui
  a79ee8e ckpt 837: reruns #475 42715672216 main, #476 42715676675 branch, #477 42715681398 main+l
  3da7cb8 ckpt 836: #474 base,legacy ONE run (306152a): 40P/4F recents,W5x2,W3b + THREE native cra
  098143a ckpt 835: legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
  299085c ckpt 833: #470 base on TAG v0.8.13: 28 PASS 1 FAIL + native crash (released tag crashes 
  a4583b1 ckpt 832: Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
  e024dae ckpt 830: Solo reruns on eaeb47e: #466 42683210584 base, #467 42683216363 o, #468 426832
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
