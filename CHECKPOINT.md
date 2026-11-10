# CHECKPOINT 904 — read me first, then TASKS.md

**Written:** 2026-11-10T09:51:17Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `aa5589a` (this checkpoint is the commit after it)

## Just done
Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (42792080486, +N2 x2) = not v0.9.2 regression; n #508 + base #506 green. Stuck pencil cover at lock transition = likely Tj BQ1b cause (TASKS BQ3). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Root-cause BQ3: read TjAccessibilityService pencil zone (~2195-2470: penWatch, penEval, updatePencil, findPencil) + scenario b6 quickstep phase (tools/emulator_scenario.sh ~572-634) + #507 log; find why the cover is not hidden when the panel collapses on lock; fix + PencilRules test; rerun b6,n then ad; then ship v0.9.2. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ad4c424 ckpt 901: Triage 09:41Z: base SOLO #506 (42790941782) GREEN = native-crash flake; n SOLO
  a837b79 ckpt 900: Gate 6878395 raw verdict: #504 b6,n,o,x,ac,ad = 98 PASS 4 FAIL (BK1b no-cover 
  4f7d798 ckpt 898: Gate 6878395 verdict 09:27Z: #503 pt GREEN (0 FAIL; BQ1a/BQ2 pass, dark look 0
  bd81eab ckpt 894: Gate dispatched on 6878395 09:10Z: #503 pt (run 42788494486), #504 b6,n,o,x,ac
  6878395 ckpt 893: Review fixes coded: scrim counted at run start (+async end, report w/ sig), pe
  c33b797 ckpt 882: Both reviews read; verdicts saved in research/BQ1_REVIEW_2026-11-10.md. Scrim:
  a04e129 ckpt 881: BQ2a answered in chat (09:00Z); BQ2b coded, not released. Heartbeat trig_013YU
  8d587e9 ckpt 880: BQ2b coded (Tj: measure once, never screenshot again): dark look never measure
  15f72bb ckpt 879: Tj asked (BQ2): why not measure once and never screenshot again (wallpaper unc
  b016ab1 ckpt 878: BQ1a: a freshly (re)connected service settles from its connect (ScrimMeasure.s
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
