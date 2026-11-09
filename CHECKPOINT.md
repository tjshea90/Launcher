# CHECKPOINT 775 — read me first, then TASKS.md

**Written:** 2026-11-09T04:04:35Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `d4061da` (this checkpoint is the commit after it)

## Just done
gate 6702e72: run 441 (x1,v,pt,rb) FAILED, 443 (b6,o,ad,ae,hf,fp,last) FAILED, 442 (base,e1,n,ac,hp) still running; FAIL lines not yet read (RESULTS block header is 150+ lines above the tail)

## Do this next
find the FAIL lines: suspect LOCK_TOUCH_AWAY_MS 1500 in sections pt/ae (touch-away timing); read job 128966857897 (441) / 128966874402 (443) with a deep tail (400) ONCE or rerun the sections solo; compare tag v0.8.12; do NOT ship red

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  27833a7 ckpt 774: Tj: 'Ship v0.8.13 when the gate is green' written into TASKS; gate 441-443 on 
  2229c7c ckpt 773: BN1c: build plan written (research/SETTINGS_BUILD_PLAN_2026-11-09.md: own row 
  fde8b51 ckpt 772: BN1j: Tj 03:42Z 'Continue and finish the spoof settings app also' written into
  e90495a ckpt 771: BN2-4: gate runs 441/442/443 on 6702e72 started 03:40Z (x1,v,pt,rb / base,e1,n
  6702e72 ckpt 770: BN2: provisional unlock guard now POLLS (400 ms until guard end) + focus-regai
  14b4971 ckpt 769: BN2-4 review done (research/BN2_BN3_BN4_REVIEW): 6 fixed (provisional unlock g
  91877d8 ckpt 768: BN3 wired (LOCK_TOUCH_AWAY_MS 1500, touchBack pad look, noteReturn lock-log li
  5807012 ckpt 767: BN2 D1 (unlock grace in homeForeignWindow) + D3 (dying dialog) + BN4 (pencil l
  5e77687 ckpt 766: BN2/BN4: pure rules + JVM tests written (HomePatchRules.foreignTreatment/UNLOC
  b284914 ckpt 765: BN2/BN3/BN4 causes read from the 03:02Z tablet report; design in research/BN2_
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
