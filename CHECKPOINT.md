# CHECKPOINT 1529 — read me first, then TASKS.md

**Written:** 2026-11-21T22:25:05Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `6405188d` (this checkpoint is the commit after it)

## Just done
CT1: run #715 (44955887858) sf 13 PASS 1 FAIL (SF6b = my too-strict dump check; AOSP Recents dump has 5 nodes): Force stop via Settings works again (pid 2564 -> 4208 in 1750 ms, cooldown holds). SF6b now INFO only. Also fixed prime review finding (no cover = no menu open). code-review skill reviewed v0.9.15 itself (prime code), subagent reviewing my diff

## Do this next
read subagent review of v0.9.15..HEAD (adef7b874686e9bfb), fix real findings, then tools/gate.sh v0.9.15 (sf + core), ship.sh, android.yml; heartbeat trig_01T4TXfU4trvxurWPZHQmijG

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  38aaaa6a ckpt 1528: CT1: run #714 (44955075583) sf 12 PASS 1 FAIL: Force stop through Settings W
  000042c4 ckpt 1527: CT1: run #713 (44954330955) sf 10 PASS 3 FAIL: Settings opened, Force stop p
  ab249c64 ckpt 1526: CT1: unit tests 21 green; section sf + gate-map + workflow list written; emu
  b74454f5 ckpt 1525: CT1: SR2 written (StockForceStop rules + service drive sf*, hooks in Mode / 
  3fa0209b ckpt 1524: CT1: audit done, other session finished (v0.9.15), ff to main; TASKS CT1 wri
  832cc7dc ckpt 1523: CS2 release v0.9.15 recorded; HANDOFF 0zc + TASKS CS2 updated (N2 flake note
  884e1f4e ckpt 1522: v0.9.15 (code 54) android.yml full_build DISPATCHED ONCE on main 05f66d1b at
  05f66d1b ckpt 1521: gated v0.9.15 (code 54) and pushed it: the handoff self-test, the full unit 
  612c5fc9 ckpt 1520: CS2 triage: head N2 fails ONLY when a native crash hit base earlier (fail: #
  b636f8e4 ckpt 1519: scenario only: N2 now prints mode/resumed/keyguard/a11y + lock log tail as I
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
