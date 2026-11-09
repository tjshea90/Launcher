# CHECKPOINT 841 — read me first, then TASKS.md

**Written:** 2026-11-09T23:24:03Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `c671a48` (this checkpoint is the commit after it)

## Just done
8 base runs read: TAG v0.8.13 4/4 green (478-481, one healed crash); HEAD 3 of 4 green (482 483 484) but 485 (branch, base,last) FAILED breaker group: diag = tj_crash.xml MISSING after all 3 am crash although pid existed (wait=0s), mode=custom. Head breaker-group FAIL 4 of 11 runs, tag 0 of 5. Hypotheses: crash hit another process / handler did not count. Added procs+logcat diag to scenario

## Do this next
dispatch base x6 on head (main base; main base,last; branch base; branch base,last; main base,x1; branch base,x1) and read 'breaker diag' + 'breaker diag log' INFO of any failing one: which process died (FATAL EXCEPTION Process: name pid) and procs list. If the crash hit a wallpaper process or a stale pid -> test fix (kill/choose pid); if the main process died but nothing recorded -> product bug in CrashGuard.record (SsFuse.fuseHandles runs first; check SsFuse.blocked/Features.get can throw). Do NOT ship until understood. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  53b3d53 ckpt 840: 8 base runs dispatched 23:12Z on ckpt 839 (a06970f): tag v0.8.13 x4 (base; bas
  a06970f ckpt 839: scenario: breaker diag + wait for process; reruns 475 (3 crashes; FAIL recents
  a0359ad ckpt 838: gated v0.9.0 (code 39) and pushed it: the handoff self-test, the full unit sui
  a79ee8e ckpt 837: reruns #475 42715672216 main, #476 42715676675 branch, #477 42715681398 main+l
  3da7cb8 ckpt 836: #474 base,legacy ONE run (306152a): 40P/4F recents,W5x2,W3b + THREE native cra
  098143a ckpt 835: legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
  299085c ckpt 833: #470 base on TAG v0.8.13: 28 PASS 1 FAIL + native crash (released tag crashes 
  a4583b1 ckpt 832: Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
