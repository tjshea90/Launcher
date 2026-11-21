# CHECKPOINT 1526 — read me first, then TASKS.md

**Written:** 2026-11-21T22:01:01Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `3598f81b` (this checkpoint is the commit after it)

## Just done
CT1: unit tests 21 green; section sf + gate-map + workflow list written; emulator.yml sections=sf DISPATCHED ONCE on main e1d5ab30 at 22:21Z (run id: find with list_workflow_runs emulator.yml branch main event workflow_dispatch perPage 1). heartbeat trig_01T4TXfU4trvxurWPZHQmijG

## Do this next
read run sf: get_job_logs failed_only+tail ~60 (404 = not ended); fix what fails; then code review high, gate, ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b74454f5 ckpt 1525: CT1: SR2 written (StockForceStop rules + service drive sf*, hooks in Mode / 
  3fa0209b ckpt 1524: CT1: audit done, other session finished (v0.9.15), ff to main; TASKS CT1 wri
  832cc7dc ckpt 1523: CS2 release v0.9.15 recorded; HANDOFF 0zc + TASKS CS2 updated (N2 flake note
  884e1f4e ckpt 1522: v0.9.15 (code 54) android.yml full_build DISPATCHED ONCE on main 05f66d1b at
  05f66d1b ckpt 1521: gated v0.9.15 (code 54) and pushed it: the handoff self-test, the full unit 
  612c5fc9 ckpt 1520: CS2 triage: head N2 fails ONLY when a native crash hit base earlier (fail: #
  b636f8e4 ckpt 1519: scenario only: N2 now prints mode/resumed/keyguard/a11y + lock log tail as I
  af0127bc ckpt 1518: CS2 gate on head 37b99dad-code (early-out): #702 id 44948036203 base,n,cu,pf
  37b99dad ckpt 1517: CS2 head b4485d0a: base,n #700 id 44947211442 GREEN, pm #701 id 44947217420 
  b4485d0a ckpt 1516: CS2: primePowerMenu now returns at once when the switch is off / automation 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
