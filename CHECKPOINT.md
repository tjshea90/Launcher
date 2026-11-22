# CHECKPOINT 1593 — read me first, then TASKS.md

**Written:** 2026-11-22T18:08:14Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `b6844bff` (this checkpoint is the commit after it)

## Just done
CW1a: 4 read-only audits launched in background at ~18:10 UTC (A speed, B patch, C ram/cpu/battery, D stock sleep); each writes research/CW1_AUDIT_<X>_*_2026-11-22.md

## Do this next
wait for the 4 completion notices; if a report file is missing after ~40 min or a session restart, relaunch that audit (briefs: neutral, read-only, slice A/B/C/D as in TASKS CW1a); then write research/CW1_FINDINGS_2026-11-22.md (CW1b) ranking every finding, decide fixes; heartbeat trig_015wPvHD1UqrhN1aXkEXR6TC

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b6844bff ckpt 1592: CW1: heartbeat armed trig_015wPvHD1UqrhN1aXkEXR6TC (every 2 h at :04)
  037337ef ckpt 1591: CW1: Tj asks audit+polish (speed, patch preparedness lock+home, ram/cpu/batt
  9327691c ckpt 1590: CV1 CLOSED: v0.9.17 Release verified (get_release_by_tag, 3 APKs), merged ma
  1a02fc09 ckpt 1546: STAND DOWN heartbeat 10:04: other session shipped+recorded v0.9.17 at 09:20 
  f12b6d8d ckpt 1589: CV1: ship.sh green (handoff, version 0.9.17 code 56, unit suite, pushed 4729
  47290141 ckpt 1588: gated v0.9.17 (code 56) and pushed it: the handoff self-test, the full unit 
  0ceccc00 ckpt 1587: CV1 RAW: #737 id 45005254638 (unified,base, main) 25P 4F = reset: x4 (Calend
  672c8c8a ckpt 1586: CV1 RAW: #738 id 45005257395 (unified,base, branch ref) GREEN 29P 0F (1 nati
  343f5a01 ckpt 1585: CV1 rerun unified,base on main@048e5f7e: #737 id 45005254638 (main), #738 id
  43467833 ckpt 1584: CV1 RAW: #736 id 45002717972 (full unified core, branch ref) 94P 4F: 'crash 
```
