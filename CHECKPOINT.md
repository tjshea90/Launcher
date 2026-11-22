# CHECKPOINT 1548 — read me first, then TASKS.md

**Written:** 2026-11-22T05:55:30Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `e9594287` (this checkpoint is the commit after it)

## Just done
CV1: run #718 id 44990179693 (ul,ul2 on main@390f5cfb) = FAIL 21 PASS 5 FAIL: UL11 failsafe to Quickstep + our launcher still home; UL12 second load of unlocked db; UL13 unlocked drawer back; UL14 Recents key opens stock RecentsActivity; UL15 Launcher control opens in QS world. ul=103s ul2=68s, ul2 all green. Drawer checks pass (unlocked has Launcher control/Settings, custom hides)

## Do this next
triage UL11 (scenario section ul around UL10-UL15 in tools/emulator_scenario.sh + RouterActivity.route:71 log MODE quickstep->custom at 05:40:08); launcher3+app review fixes in progress (research/CV1_REVIEW1_*); heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  8bcc6704 ckpt 1547: CV1: 2 reviews saved (research/CV1_REVIEW1_*): launcher3 5 HIGH, app 1 HIGH 
  95f3a56d ckpt 1546: CV1: heartbeat armed trig_01NxLAL2jZccAiP8UPYM1X7m (every 2 h at :44). Run #
  2d2b69e4 ckpt 1545: CV1 takeover: new session continues old one (Tj told me to). Emulator run #7
  390f5cfb ckpt 1544: CV1: sections ul+ul2 written, gate-map + workflow list, handoff test 53 ok
  e18d1484 ckpt 1543: CV1: code done: Unified/UnifiedRules/HomeProfile, provider profile db, loade
  c583442c ckpt 1542: CV1: rules+HomeProfile+provider profile db+loader hook+router+guards+handsha
  bdfadbd8 ckpt 1541: CV1: design note written (research/CV1_UNIFIED_LAUNCHER_DESIGN_2026-11-22.md
  abc9a2c6 ckpt 1540: CV1: audits A/B/C saved in research/CV1_AUDIT_*
  be84f62e ckpt 1539: CV1: heartbeat armed trig_01SrdLzjP9bFC1W4KzxUT1JL (every 2 h at :04)
  e2c41032 ckpt 1538: CV1: Tj says build the unified launcher (custom launcher only, Quickstep = i
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
