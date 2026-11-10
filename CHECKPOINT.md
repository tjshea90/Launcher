# CHECKPOINT 922 — read me first, then TASKS.md

**Written:** 2026-11-10T10:13:50Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `d554a7c` (this checkpoint is the commit after it)

## Just done
BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-panel check FAILS in CUSTOM too (cover stays shown after collapse x2 on lock screen = Tj's bug, reproduced); pencil node+ancestors read V with open bounds while panel closed; first-sight probe window was [0,0][600,26]. Added insideItsWindow + penTree sketch. BN8b also failed once in #510 (cover none after dump). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Dispatch b6 solo on branch with tree sketch; haiku-extract BQ3/FAIL lines; find the node that differs open vs closed on the lock screen; implement fix; rerun b6 + n. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  135053e ckpt 913: BQ3 diagnostics coded (debug-only PENCIL probe in service; b6: closed-panel sa
  9c1b18d ckpt 904: Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (427920
  ad4c424 ckpt 901: Triage 09:41Z: base SOLO #506 (42790941782) GREEN = native-crash flake; n SOLO
  a837b79 ckpt 900: Gate 6878395 raw verdict: #504 b6,n,o,x,ac,ad = 98 PASS 4 FAIL (BK1b no-cover 
  4f7d798 ckpt 898: Gate 6878395 verdict 09:27Z: #503 pt GREEN (0 FAIL; BQ1a/BQ2 pass, dark look 0
  bd81eab ckpt 894: Gate dispatched on 6878395 09:10Z: #503 pt (run 42788494486), #504 b6,n,o,x,ac
  6878395 ckpt 893: Review fixes coded: scrim counted at run start (+async end, report w/ sig), pe
  c33b797 ckpt 882: Both reviews read; verdicts saved in research/BQ1_REVIEW_2026-11-10.md. Scrim:
  a04e129 ckpt 881: BQ2a answered in chat (09:00Z); BQ2b coded, not released. Heartbeat trig_013YU
  8d587e9 ckpt 880: BQ2b coded (Tj: measure once, never screenshot again): dark look never measure
```

(8 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
