# CHECKPOINT 1595 — read me first, then TASKS.md

**Written:** 2026-11-22T18:20:24Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `c3edaf31` (this checkpoint is the commit after it)

## Just done
CW1a: audit A done (research/CW1_AUDIT_A_SPEED_2026-11-22.md, 12 findings: A1 reload 'ready' waits the whole LoaderTask, A2 double Home fan-out on main before Launcher starts, A3 ReturnReset plans on main, A4 settleHome root fetch every 40 ms, A5 plain-PIN reload starts late, A6 handshake eager lookups, A7 3 fsync commits); B done; C, D still running

## Do this next
wait for audits C and D notices; then write research/CW1_FINDINGS_2026-11-22.md (CW1b); ask Tj whether the hider must also cover the unlocked home; heartbeat trig_015wPvHD1UqrhN1aXkEXR6TC

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3270688c ckpt 1594: CW1a: audit B done (research/CW1_AUDIT_B_PATCH_2026-11-22.md, 17 findings: B
  b304ee67 ckpt 1593: CW1a: 4 read-only audits launched in background at ~18:10 UTC (A speed, B pa
  b6844bff ckpt 1592: CW1: heartbeat armed trig_015wPvHD1UqrhN1aXkEXR6TC (every 2 h at :04)
  037337ef ckpt 1591: CW1: Tj asks audit+polish (speed, patch preparedness lock+home, ram/cpu/batt
  9327691c ckpt 1590: CV1 CLOSED: v0.9.17 Release verified (get_release_by_tag, 3 APKs), merged ma
  1a02fc09 ckpt 1546: STAND DOWN heartbeat 10:04: other session shipped+recorded v0.9.17 at 09:20 
  f12b6d8d ckpt 1589: CV1: ship.sh green (handoff, version 0.9.17 code 56, unit suite, pushed 4729
  47290141 ckpt 1588: gated v0.9.17 (code 56) and pushed it: the handoff self-test, the full unit 
  0ceccc00 ckpt 1587: CV1 RAW: #737 id 45005254638 (unified,base, main) 25P 4F = reset: x4 (Calend
  672c8c8a ckpt 1586: CV1 RAW: #738 id 45005257395 (unified,base, branch ref) GREEN 29P 0F (1 nati
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
