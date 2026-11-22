# CHECKPOINT 1541 — read me first, then TASKS.md

**Written:** 2026-11-22T05:20:25Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `37c6650a` (this checkpoint is the commit after it)

## Just done
CV1: design note written (research/CV1_UNIFIED_LAUNCHER_DESIGN_2026-11-22.md)

## Do this next
implement per design sec 3: Unified+UnifiedRules+HomeProfile (+JVM tests) first, then Router/guards/handshake, then LauncherProvider profile db + loader hook, then HiddenApps unlocked rule, CrashGuard emergency, Control switch/report, scenario ul/ul2; heartbeat trig_01SrdLzjP9bFC1W4KzxUT1JL

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  abc9a2c6 ckpt 1540: CV1: audits A/B/C saved in research/CV1_AUDIT_*
  be84f62e ckpt 1539: CV1: heartbeat armed trig_01SrdLzjP9bFC1W4KzxUT1JL (every 2 h at :04)
  e2c41032 ckpt 1538: CV1: Tj says build the unified launcher (custom launcher only, Quickstep = i
  28ff992c ckpt 1537: CU1: Tj asked whether the custom launcher could replace Quickstep with an un
  6b4b55f8 ckpt 1536: CT1: heartbeat trig_01T4TXfU4trvxurWPZHQmijG DELETED (job shipped); only CT1
  7cfdbb86 ckpt 1535: CT1: v0.9.16 RELEASED + RECORDED (android.yml full_build, target 1d8b6b2d, p
  ccc3d53a ckpt 1534: CT1: gate GREEN: A #716 id 44957361801 (base,n,o,ac,x,pm) + B #717 id 449573
  1d8b6b2d ckpt 1533: CT1: gate run B #717 (id 44957364733, e1,x1,rb,last,sf,bs) GREEN, success at
  25003fc2 ckpt 1532: CT1: ship.sh green (handoff, full unit suite, lint, versionCode 55) tip 6df4
  6df4b740 ckpt 1531: gated v0.9.16 (code 55) and pushed it: the handoff self-test, the full unit 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
