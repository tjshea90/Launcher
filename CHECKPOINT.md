# CHECKPOINT 1552 — read me first, then TASKS.md

**Written:** 2026-11-22T06:10:22Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `bc824069` (this checkpoint is the commit after it)

## Just done
CV1: run #719 id 44991880109 (ul,ul2, main@9d1d6c06) = 22 PASS 5 FAIL: UL3c (provider seed log not found), UL11 failsafe, UL12, UL13, UL15; UL14 now PASS. Router lines in phase C: only ONE home press registered (06:04:56) for three keyevents, no MODE line = failsafe never fired (test problem, helper state or key coalescing); profileCover works (flip UNLOCKED->CUSTOM cover until ready +1409 ms)

## Do this next
fix review-2 items; make UL11 robust (variants + diag); UL3c use app log instead of logcat; then read-only review not needed again, gate gate.sh 1d8b6b2d; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  bc824069 ckpt 1551: CV1: review round 2 saved (research/CV1_REVIEW2_*): 1 HIGH (BgDataModel unpi
  6625e904 ckpt 1550: CV1: dispatched emulator #719 id 44991880109 (ul,ul2, main@9d1d6c06, fixes) 
  9d1d6c06 ckpt 1549: CV1: review fixes in: launcher3 per-DB empty flag + atomic Java seed (no wid
  eaa7fdd0 ckpt 1548: CV1: run #718 id 44990179693 (ul,ul2 on main@390f5cfb) = FAIL 21 PASS 5 FAIL
  8bcc6704 ckpt 1547: CV1: 2 reviews saved (research/CV1_REVIEW1_*): launcher3 5 HIGH, app 1 HIGH 
  95f3a56d ckpt 1546: CV1: heartbeat armed trig_01NxLAL2jZccAiP8UPYM1X7m (every 2 h at :44). Run #
  2d2b69e4 ckpt 1545: CV1 takeover: new session continues old one (Tj told me to). Emulator run #7
  390f5cfb ckpt 1544: CV1: sections ul+ul2 written, gate-map + workflow list, handoff test 53 ok
  e18d1484 ckpt 1543: CV1: code done: Unified/UnifiedRules/HomeProfile, provider profile db, loade
  c583442c ckpt 1542: CV1: rules+HomeProfile+provider profile db+loader hook+router+guards+handsha
```
