# CHECKPOINT 1528 — read me first, then TASKS.md

**Written:** 2026-11-21T22:16:20Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `555fc3ef` (this checkpoint is the commit after it)

## Just done
CT1: run #714 (44955075583) sf 12 PASS 1 FAIL: Force stop through Settings WORKS on the emulator (pid 2569 -> 4216 in 1690 ms, OK pressed, custom home back, Recents opens); only SF5 failed (Recents LINES 2 -> 5, a bad premise). Replaced by App-info-gone check + Recents UI dump INFO (SF6b)

## Do this next
dispatch sf once more (read the SF Recents INFO line: settings nodes + dumpsys lines), then /code-review high on the diff vs v0.9.15, then tools/gate.sh v0.9.15; heartbeat trig_01T4TXfU4trvxurWPZHQmijG

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M tools/emulator_scenario.sh

## Last ten checkpoints
```
  000042c4 ckpt 1527: CT1: run #713 (44954330955) sf 10 PASS 3 FAIL: Settings opened, Force stop p
  ab249c64 ckpt 1526: CT1: unit tests 21 green; section sf + gate-map + workflow list written; emu
  b74454f5 ckpt 1525: CT1: SR2 written (StockForceStop rules + service drive sf*, hooks in Mode / 
  3fa0209b ckpt 1524: CT1: audit done, other session finished (v0.9.15), ff to main; TASKS CT1 wri
  832cc7dc ckpt 1523: CS2 release v0.9.15 recorded; HANDOFF 0zc + TASKS CS2 updated (N2 flake note
  884e1f4e ckpt 1522: v0.9.15 (code 54) android.yml full_build DISPATCHED ONCE on main 05f66d1b at
  05f66d1b ckpt 1521: gated v0.9.15 (code 54) and pushed it: the handoff self-test, the full unit 
  612c5fc9 ckpt 1520: CS2 triage: head N2 fails ONLY when a native crash hit base earlier (fail: #
  b636f8e4 ckpt 1519: scenario only: N2 now prints mode/resumed/keyguard/a11y + lock log tail as I
  af0127bc ckpt 1518: CS2 gate on head 37b99dad-code (early-out): #702 id 44948036203 base,n,cu,pf
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
