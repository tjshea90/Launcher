# CHECKPOINT 1538 — read me first, then TASKS.md

**Written:** 2026-11-22T05:04:43Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `f93bd193` (this checkpoint is the commit after it)

## Just done
CV1: Tj says build the unified launcher (custom launcher only, Quickstep = its unlocked state) in ONE release, no visible difference; written to TASKS CV1a-g

## Do this next
arm heartbeat; launch 3 read-only audit subagents (A mode-dependence, B stock-launcher uses, C app filtering + port gaps); save to research/CV1_*; then design note

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  28ff992c ckpt 1537: CU1: Tj asked whether the custom launcher could replace Quickstep with an un
  6b4b55f8 ckpt 1536: CT1: heartbeat trig_01T4TXfU4trvxurWPZHQmijG DELETED (job shipped); only CT1
  7cfdbb86 ckpt 1535: CT1: v0.9.16 RELEASED + RECORDED (android.yml full_build, target 1d8b6b2d, p
  ccc3d53a ckpt 1534: CT1: gate GREEN: A #716 id 44957361801 (base,n,o,ac,x,pm) + B #717 id 449573
  1d8b6b2d ckpt 1533: CT1: gate run B #717 (id 44957364733, e1,x1,rb,last,sf,bs) GREEN, success at
  25003fc2 ckpt 1532: CT1: ship.sh green (handoff, full unit suite, lint, versionCode 55) tip 6df4
  6df4b740 ckpt 1531: gated v0.9.16 (code 55) and pushed it: the handoff self-test, the full unit 
  767e95aa ckpt 1530: CT1: reviewer (adef7b..., read-only) 7 findings all fixed: SF cover swallows
  25c715ed ckpt 1529: CT1: run #715 (44955887858) sf 13 PASS 1 FAIL (SF6b = my too-strict dump che
  38aaaa6a ckpt 1528: CT1: run #714 (44955075583) sf 12 PASS 1 FAIL: Force stop through Settings W
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
