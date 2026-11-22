# CHECKPOINT 1556 — read me first, then TASKS.md

**Written:** 2026-11-22T06:27:28Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `e535d6a3` (this checkpoint is the commit after it)

## Just done
CV1: scenario ul reworked (drawer retry, python3 sqlite3 injection of real custom-layout rows + UL3d verifies the unlocked copy: ids>=1e9, folder links, no widget/deep shortcut); gate-map ul 140 s ul2 75 s; handoff test 53 ok

## Do this next
dispatch emulator.yml ul,ul2 ref main (new run, record id), read with get_job_logs failed_only tail 40 / job logs tail 45; if UL3d fails read 'UL the unlocked layout after the seed' INFO; then 3 gate runs from gate.sh 1d8b6b2d; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  9da41d62 ckpt 1555: CV1: run #720 id 44992764510 (ul,ul2, main@87bfc562) = 26 PASS 1 FAIL 16 INF
  5358e822 ckpt 1554: CV1: dispatched emulator #720 id 44992764510 (ul,ul2, main@87bfc562, review-
  87bfc562 ckpt 1553: CV1: review-2 fixes in (BgDataModel unpin skip, allLayoutDbs opens unlocked 
  49db2cb3 ckpt 1552: CV1: run #719 id 44991880109 (ul,ul2, main@9d1d6c06) = 22 PASS 5 FAIL: UL3c 
  bc824069 ckpt 1551: CV1: review round 2 saved (research/CV1_REVIEW2_*): 1 HIGH (BgDataModel unpi
  6625e904 ckpt 1550: CV1: dispatched emulator #719 id 44991880109 (ul,ul2, main@9d1d6c06, fixes) 
  9d1d6c06 ckpt 1549: CV1: review fixes in: launcher3 per-DB empty flag + atomic Java seed (no wid
  eaa7fdd0 ckpt 1548: CV1: run #718 id 44990179693 (ul,ul2 on main@390f5cfb) = FAIL 21 PASS 5 FAIL
  8bcc6704 ckpt 1547: CV1: 2 reviews saved (research/CV1_REVIEW1_*): launcher3 5 HIGH, app 1 HIGH 
  95f3a56d ckpt 1546: CV1: heartbeat armed trig_01NxLAL2jZccAiP8UPYM1X7m (every 2 h at :44). Run #
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
