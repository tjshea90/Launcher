# CHECKPOINT 924 — read me first, then TASKS.md

**Written:** 2026-11-10T10:26:16Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `9bda41c` (this checkpoint is the commit after it)

## Just done
BQ3 finding: cmd statusbar collapse is a NO-OP on the lock screen (my BQ3 check invalid; open/closed trees identical). Real defect = Quickstep->lock first-sight stale pencil (P2) + control stale cover. Notes in research/BQ3_STUCK_COVER_2026-11-10.md. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Code (a): in TjAccessibilityService updatePencil/schedulePencil/penWatch hide+dropPenNode when !isScreenOn(), never show while screen off; add debug log of SystemUI TYPE_WINDOW_STATE_CHANGED/ANNOUNCEMENT texts; fix scenario BQ3 (swipe-close instead of collapse; bq3_pick awk done-flag); control: print pid of TjPencilCover owner. Then dispatch b6, read via haiku subagent. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M research/BQ3_STUCK_COVER_2026-11-10.md

## Last ten checkpoints
```
  9bda41c ckpt 923: BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL 
  3ec729c ckpt 922: BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-pan
  135053e ckpt 913: BQ3 diagnostics coded (debug-only PENCIL probe in service; b6: closed-panel sa
  9c1b18d ckpt 904: Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (427920
  ad4c424 ckpt 901: Triage 09:41Z: base SOLO #506 (42790941782) GREEN = native-crash flake; n SOLO
  a837b79 ckpt 900: Gate 6878395 raw verdict: #504 b6,n,o,x,ac,ad = 98 PASS 4 FAIL (BK1b no-cover 
  4f7d798 ckpt 898: Gate 6878395 verdict 09:27Z: #503 pt GREEN (0 FAIL; BQ1a/BQ2 pass, dark look 0
  bd81eab ckpt 894: Gate dispatched on 6878395 09:10Z: #503 pt (run 42788494486), #504 b6,n,o,x,ac
  6878395 ckpt 893: Review fixes coded: scrim counted at run start (+async end, report w/ sig), pe
  c33b797 ckpt 882: Both reviews read; verdicts saved in research/BQ1_REVIEW_2026-11-10.md. Scrim:
```
