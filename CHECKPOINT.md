# CHECKPOINT 829 — read me first, then TASKS.md

**Written:** 2026-11-09T19:24:49Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `3611a1b` (this checkpoint is the commit after it)

## Just done
GATE v0.9.0 on 09215cc: #465 rg OK; #463 (42671984609) 5 FAIL legacy/lq; #464 (42671992301) 6 FAIL base/o, ss ALL PASS; both had NATIVE CRASH #1. Triage in research/BN1_GATE_TRIAGE_2026-11-09.md

## Do this next
1) FIRST rule out a regression: crash-breaker FAILs (base, scenario l.224-226) vs CrashGuard.record -> SsFuse.fuseHandles (BN1 change) - read scenario l.200-230 + SsFuse; 2) rerun SOLO on main: base, o, legacy, lq (4 dispatches, side by side), read each (failed_only tail 60, FAIL lines; haiku subagent extract works: tail 260); 3) a section that fails again -> same on ref v0.8.13; 4) then bump app/build.gradle.kts 0.9.0/code 39, bash ship.sh note, android.yml full_build=true main, record-release only after green. Heartbeat id: trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c5dca2d ckpt 828: Gate attempt 2 of #463 (42671984609) and #464 (42671992301) RUNNING since 18:4
  0cf5f0e ckpt 827: Gate #463/#464 (42671984609 / 42671992301) FAILED at +57 s with ZERO jobs (no 
  e676c11 ckpt 826: BO1d PROVEN: heartbeat first fire 18:27:33Z reached this session idle (queued 
  0c91d62 ckpt 825: HANDOFF.md architecture map: Settings look-alike row. Gate #463/#464 running (
  1ff741f ckpt 824: Gate #465 (rg solo, 42671999660, 09215cc) = SUCCESS (done 18:16:50). #463/#464
  53ad8fd ckpt 823: scenario heal(): NATIVE CRASH now also prints an INFO detail line (Fatal signa
  009cb18 ckpt 822: GATE dispatched 18:12Z on 09215cc: #463 id 42671984609 (legacy,n6,t,x,ac,bl,lq
  09215cc ckpt 821: Review A+B fixes done (B1 B4/A3 B5 B6 B7 B8 A1 A2 A4 A5), full unit tests 864 
  2241b0e ckpt 820: BO2 proven: run 462 (42669335036, 0d95241, ss) 69 PASS 1 FAIL; SS26d-g PASS, p
  11c5b53 ckpt 819: Review B done: 8 LOW, no medium/high (research/BN1_REVIEW_B_2026-11-09.md). BO
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
