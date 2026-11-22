# CHECKPOINT 1597 — read me first, then TASKS.md

**Written:** 2026-11-22T18:26:30Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/launcher-performance-audit-39n5do` · **builds on:** `4f20b9c9` (this checkpoint is the commit after it)

## Just done
CW1a: audit D done (research/CW1_AUDIT_D_STOCK_SLEEP_2026-11-22.md): stock process CANNOT sleep (SystemUI binds TouchInteractionService for good, adj 100, a kill is re-bound in seconds); goal = smallest footprint, fewest wakes; plan D3 SR2 debt model stale after CV1, D5 one seeded cleanup after switch, D4 clear-all deadline includes cold stock start, D7 stop 2 refused dumpsys; never disable stock HOME/TouchInteractionService. ALL 4 AUDITS DONE

## Do this next
read the 4 reports (findings tables), write research/CW1_FINDINGS_2026-11-22.md (CW1b) ranking + fix list; ask Tj: hider in unlocked home? Konami filter all day?; heartbeat trig_015wPvHD1UqrhN1aXkEXR6TC

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0223d6e2 ckpt 1596: CW1a: audit C done (research/CW1_AUDIT_C_RESOURCES_2026-11-22.md, 10 finding
  a9305bf9 ckpt 1595: CW1a: audit A done (research/CW1_AUDIT_A_SPEED_2026-11-22.md, 12 findings: A
  3270688c ckpt 1594: CW1a: audit B done (research/CW1_AUDIT_B_PATCH_2026-11-22.md, 17 findings: B
  b304ee67 ckpt 1593: CW1a: 4 read-only audits launched in background at ~18:10 UTC (A speed, B pa
  b6844bff ckpt 1592: CW1: heartbeat armed trig_015wPvHD1UqrhN1aXkEXR6TC (every 2 h at :04)
  037337ef ckpt 1591: CW1: Tj asks audit+polish (speed, patch preparedness lock+home, ram/cpu/batt
  9327691c ckpt 1590: CV1 CLOSED: v0.9.17 Release verified (get_release_by_tag, 3 APKs), merged ma
  1a02fc09 ckpt 1546: STAND DOWN heartbeat 10:04: other session shipped+recorded v0.9.17 at 09:20 
  f12b6d8d ckpt 1589: CV1: ship.sh green (handoff, version 0.9.17 code 56, unit suite, pushed 4729
  47290141 ckpt 1588: gated v0.9.17 (code 56) and pushed it: the handoff self-test, the full unit 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
