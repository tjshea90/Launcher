# CHECKPOINT 848 — read me first, then TASKS.md

**Written:** 2026-11-10T00:39:13Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `00e0a05` (this checkpoint is the commit after it)

## Just done
BN7 written: tablet report 2026-11-10 read (battery no-dump = no PACKAGE_USAGE_STATS, storage ok 664 ms, no crash, helper down, no flash evidence)

## Do this next
finish BN1p: bump app/build.gradle.kts to code 40 / 0.9.1, bash tools/gate.sh v0.9.0, ship.sh, android.yml full_build, record-release, message Tj. Heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
    M  TASKS.md

## Last ten checkpoints
```
  00e0a05 ckpt 847: tablet intake 2026-11-10 (Tj's new report; 2 red flags: helper not running, PA
  3952807 ckpt 846: Heartbeat 00:27Z woke the session; reading code for BN1p (v0.9.1): SsHomeSumma
  bc31d45 ckpt 845: v0.9.0 SHIPPED+RECORDED (Release 439795609, android.yml run 41 id 42726425581 
  413a447 ckpt 844: #498 base,legacy main CLEAN 0 FAIL; #499 branch 44P/1F recents + 2 native cras
  ba44251 ckpt 843: #497 FAIL = native crash 0.85 s later; #492 emulator offline; breaker group ok
  00bbe0c ckpt 842: 6 head base runs 486-491: 5 green, 489 FAILED breaker: rounds 1+2 recorded, ro
  a6bad9b ckpt 841: 8 base runs read: TAG v0.8.13 4/4 green (478-481, one healed crash); HEAD 3 of
  53b3d53 ckpt 840: 8 base runs dispatched 23:12Z on ckpt 839 (a06970f): tag v0.8.13 x4 (base; bas
  a06970f ckpt 839: scenario: breaker diag + wait for process; reruns 475 (3 crashes; FAIL recents
  a0359ad ckpt 838: gated v0.9.0 (code 39) and pushed it: the handoff self-test, the full unit sui
```
