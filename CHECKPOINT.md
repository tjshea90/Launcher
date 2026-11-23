# CHECKPOINT 1626 — read me first, then TASKS.md

**Written:** 2026-11-23T01:00:03Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `f324972c` (this checkpoint is the commit after it)

## Just done
CW1 READ 00:58Z #764 (ao,cs2,hf,ul 2nd): 43P 3F = HF1 (home patch never calibrated/rests), HF4, HF5 (cascade), hf TIME 424 s (vs 261 in #754 where hf passed); UL24 PASSED 3/3 (202/162/163 ms) so the UL24 fail was a flake; no native crash line. Dispatched hf solo: main + branch claude/dazzling-hawking-b7qz0b (same code, 2 samples) + TAG v0.9.17; suspect B1a (HomePatchRules.buildQuietMs 100 ms) only if head fails and tag not

## Do this next
read the 3 hf runs (list perPage 3 + probe); if head red: read scheduleHomeBuild/buildDue logic; heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d7876e0a ckpt 1625: CW1 00:57Z: ship.sh v0.9.18 (code 57) EXIT 0 (suite+lint+handoff green), NOT
  c3f00ded ckpt 1624: gated v0.9.18 (code 57) and pushed it: the handoff self-test, the full unit 
  ef3504c1 ckpt 1623: CW1 RAW 00:54Z: #763 45090010377 main base,e1 GREEN (E1 passes on head). #76
  4de58d41 ckpt 1622: CW1 READ 00:41Z: #762 head base,legacy,e1,y2,b4,lq = 68P 1F: ONLY E1 'Delete
  3987ecf1 ckpt 1621: CW1 RAW 00:38Z: #762 45087708823 (head, base,legacy,e1,y2,b4,lq, 2nd sample)
  ed5ec54e ckpt 1620: CW1 READ 00:33Z: TAG #758 base,legacy,e1,y2,b4,lq = 67P 2F (E1 x2 + NATIVE C
  326d9c17 ckpt 1619: CW1 RAW 00:31Z: #759 TAG ul GREEN; #760 MAIN ul GREEN (UL24 ok solo = chain 
  b3b0dca2 ckpt 1618: CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, u
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
  2fe4dce4 ckpt 1616: CW1 RERUN READ 00:20Z (subagent): #753 base,legacy,e1,y2,b4,lq 63P 6F no nat
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
