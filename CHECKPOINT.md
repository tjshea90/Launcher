# CHECKPOINT 844 — read me first, then TASKS.md

**Written:** 2026-11-10T00:12:44Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `2a92ba5` (this checkpoint is the commit after it)

## Just done
#498 base,legacy main CLEAN 0 FAIL; #499 branch 44P/1F recents + 2 native crashes. android.yml full_build dispatched on main 2a92ba5 at 00:13Z

## Do this next
at +3 min read android.yml run (list_workflow_runs android.yml, branch main, event workflow_dispatch, perPage 1; then jobs/logs); get_release_by_tag v0.9.0; ONLY if green: bash tools/record-release.sh v0.9.0 (note in ship.sh text), HANDOFF 0o, tick TASKS BN1*, final message to Tj. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ba44251 ckpt 843: #497 FAIL = native crash 0.85 s later; #492 emulator offline; breaker group ok
  00bbe0c ckpt 842: 6 head base runs 486-491: 5 green, 489 FAILED breaker: rounds 1+2 recorded, ro
  a6bad9b ckpt 841: 8 base runs read: TAG v0.8.13 4/4 green (478-481, one healed crash); HEAD 3 of
  53b3d53 ckpt 840: 8 base runs dispatched 23:12Z on ckpt 839 (a06970f): tag v0.8.13 x4 (base; bas
  a06970f ckpt 839: scenario: breaker diag + wait for process; reruns 475 (3 crashes; FAIL recents
  a0359ad ckpt 838: gated v0.9.0 (code 39) and pushed it: the handoff self-test, the full unit sui
  a79ee8e ckpt 837: reruns #475 42715672216 main, #476 42715676675 branch, #477 42715681398 main+l
  3da7cb8 ckpt 836: #474 base,legacy ONE run (306152a): 40P/4F recents,W5x2,W3b + THREE native cra
  098143a ckpt 835: legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
