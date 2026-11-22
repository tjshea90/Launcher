# CHECKPOINT 1544 — read me first, then TASKS.md

**Written:** 2026-11-22T05:36:54Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `c1d5c814` (this checkpoint is the commit after it)

## Just done
CV1: sections ul+ul2 written, gate-map + workflow list, handoff test 53 ok

## Do this next
dispatch emulator.yml sections=ul,ul2 on branch claude/launcher-performance-audit-39n5do (ref), read result with get_job_logs; in parallel start read-only review subagents (delta: git diff be84f62e HEAD -- app launcher3); heartbeat trig_01SrdLzjP9bFC1W4KzxUT1JL

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e18d1484 ckpt 1543: CV1: code done: Unified/UnifiedRules/HomeProfile, provider profile db, loade
  c583442c ckpt 1542: CV1: rules+HomeProfile+provider profile db+loader hook+router+guards+handsha
  bdfadbd8 ckpt 1541: CV1: design note written (research/CV1_UNIFIED_LAUNCHER_DESIGN_2026-11-22.md
  abc9a2c6 ckpt 1540: CV1: audits A/B/C saved in research/CV1_AUDIT_*
  be84f62e ckpt 1539: CV1: heartbeat armed trig_01SrdLzjP9bFC1W4KzxUT1JL (every 2 h at :04)
  e2c41032 ckpt 1538: CV1: Tj says build the unified launcher (custom launcher only, Quickstep = i
  28ff992c ckpt 1537: CU1: Tj asked whether the custom launcher could replace Quickstep with an un
  6b4b55f8 ckpt 1536: CT1: heartbeat trig_01T4TXfU4trvxurWPZHQmijG DELETED (job shipped); only CT1
  7cfdbb86 ckpt 1535: CT1: v0.9.16 RELEASED + RECORDED (android.yml full_build, target 1d8b6b2d, p
  ccc3d53a ckpt 1534: CT1: gate GREEN: A #716 id 44957361801 (base,n,o,ac,x,pm) + B #717 id 449573
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
