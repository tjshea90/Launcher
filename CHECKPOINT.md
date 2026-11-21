# CHECKPOINT 1525 — read me first, then TASKS.md

**Written:** 2026-11-21T21:57:02Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `b2e5ab67` (this checkpoint is the commit after it)

## Just done
CT1: SR2 written (StockForceStop rules + service drive sf*, hooks in Mode / BatterySaverGuard / Control / report), JVM StockForceStopTest written, first unit run started. heartbeat trig_01T4TXfU4trvxurWPZHQmijG

## Do this next
read /tmp scratchpad ut1.log for GRADLE_EXIT; fix compile / test failures; then write emulator section sf (model: section sr in tools/emulator_scenario.sh ~5478) + gate-map row; then /code-review high; gate; ship

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3fa0209b ckpt 1524: CT1: audit done, other session finished (v0.9.15), ff to main; TASKS CT1 wri
  832cc7dc ckpt 1523: CS2 release v0.9.15 recorded; HANDOFF 0zc + TASKS CS2 updated (N2 flake note
  884e1f4e ckpt 1522: v0.9.15 (code 54) android.yml full_build DISPATCHED ONCE on main 05f66d1b at
  05f66d1b ckpt 1521: gated v0.9.15 (code 54) and pushed it: the handoff self-test, the full unit 
  612c5fc9 ckpt 1520: CS2 triage: head N2 fails ONLY when a native crash hit base earlier (fail: #
  b636f8e4 ckpt 1519: scenario only: N2 now prints mode/resumed/keyguard/a11y + lock log tail as I
  af0127bc ckpt 1518: CS2 gate on head 37b99dad-code (early-out): #702 id 44948036203 base,n,cu,pf
  37b99dad ckpt 1517: CS2 head b4485d0a: base,n #700 id 44947211442 GREEN, pm #701 id 44947217420 
  b4485d0a ckpt 1516: CS2: primePowerMenu now returns at once when the switch is off / automation 
  b47f4085 ckpt 1515: CS2 triage: cs SOLO green on head (#695 id 44944662421) and tag (#697); base
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
