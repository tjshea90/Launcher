# CHECKPOINT 1500 — read me first, then TASKS.md

**Written:** 2026-11-19T00:43:42Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `1ff0a972` (this checkpoint is the commit after it)

## Just done
CI1: base,an x2 dispatched 00:45Z (main a2b5316c + branch ref); ids = newest two of list_workflow_runs emulator.yml event workflow_dispatch perPage 2

## Do this next
read both at ~00:57Z (haiku: header, FAILs, INFO 'hidden:' and 'AN9b' lines); both green = ship.sh + android.yml full_build; a repeat FAIL = real: bisect G1 (SettingsSpoof) against the chain

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a2b5316c ckpt 1499: CI1: #685 base,an main FAIL = only 'hidden: REINSTALLED app is forgotten' (A
  1ff86b86 ckpt 1498: CI1: triage: an solo main #683 id 44428928340 SUCCESS; base,an on TAG v0.9.1
  8d1a658d ckpt 1497: CI1: base,an #681 id 44426690735 = 41 PASS 1 FAIL: only AN9b (lock log shows
  681c0607 ckpt 1496: CI1: rerun verdict: ac solo #682 id 44426695151 SUCCESS (AC2 was the native-
  8eca3e05 ckpt 1495: CI1: gate triage (head 3e02bf08, product unchanged since): A #678 6 FAIL (86
  c33f0c26 ckpt 1494: CI1: gate raw verdict (head 3e02bf08): A #678 id 44423199764 (base,x1,t,x,an
  3492984f ckpt 1493: CI1: tier 0 local green (testDebugUnitTest + lintDebug, BUILD SUCCESSFUL, GR
  9f4d3d35 ckpt 1492: CI1: TASKS ticked (CI1a/c/d with proofs; CI1b left open on purpose), HANDOFF
  3e02bf08 ckpt 1491: CI1: VERDICT sr main #676 id 44421271607 = 13 PASS 0 FAIL (stock 32 MB/1 Act
  6c027d6c ckpt 1490: CI1: version 0.9.14 code 53, draft research/CI1_RELEASE_MESSAGE_DRAFT_2026-1
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
