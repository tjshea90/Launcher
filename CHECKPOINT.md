# CHECKPOINT 738 — read me first, then TASKS.md

**Written:** 2026-11-09T00:30:07Z · **tests:** all 5 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `49ace28` (this checkpoint is the commit after it)

## Just done
BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact hook vetoes compaction)

## Do this next
write research/COMPACTION_BLOCKED doc; BM1b keepalive+env+compact_guard; BM1c threshold 40; BM1d ctxwatch; BM1e matcher; then read v0.8.12 gate runs (BM1f)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  2ba15f7 ckpt 737: GATE v0.8.12 dispatched 22:26 on 94429c2: A base,x1,e1,n,ac,uf,x,se,b6 / B hp,
  7c914d2 ckpt 736: BL1 docs: research section 5 (status, limits, review), TABLET_CHECKLIST_v0.8.1
  d7cae14 ckpt 735: BL1 review fixes in (9 of 10 findings; frozen status-bar row = disclosed limit
  041aa83 ckpt 734: BL1d evidence: cu #419/#420 id 42528148176/#421 id 42528151261 all PASS (3 gre
  559db2a ckpt 733: BL1d cu #419 id 42527552708 (913947f) = 14 PASS 0 FAIL: wall kept 2 s, curtain
  66783a5 ckpt 732: BL1d first cu run #418 id 42526557769 (922d886): 13 PASS 1 FAIL (CU5). Wall ke
  922d886 ckpt 731: BL1d: emulator section cu written (calibrate with home_patch+home_curtain ON, 
  3791c9b ckpt 730: BL1c steps 3-6 BUILT (compiles; unit suite running): TAS wiring (full wall kep
  b72bb88 ckpt 729: BL1c step 2 DONE: HomeCurtain.java (store: walls per rot+step, composed curtai
  631f26d ckpt 728: BL1c step 1 DONE: HomeCurtainRules (pure: switch default, whyNotUse, whyNotCap
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
