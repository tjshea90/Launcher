# CHECKPOINT 875 — read me first, then TASKS.md

**Written:** 2026-11-10T08:35:47Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `c934b14` (this checkpoint is the commit after it)

## Just done
BQ1b design written (TASKS BQ1b-DESIGN): FingerGuard = hide cover at finger DOWN, return after 350 ms + 200 ms SystemUI quiet + SlideGuard rest. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Code it: PencilRules.FingerGuard + PencilRulesTest; TjAccessibilityService: penFinger field, penFingerDown(), schedulePencil stamps SystemUI events, updatePencil uses both guards, LockLog show/hide lines, removePencil resets. Then bash tools/gradle.sh :app:testDebugUnitTest --tests '*PencilRules*'. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  c934b14 ckpt 874: BQ1a fix coded: ScrimMeasure settle 10 s, 1 run/lock, EVERY run that stored no
  835a7b0 ckpt 873: BQ1a cause found in code: BA1 measurement = REAL screenshots via performGlobal
  fa62839 ckpt 872: Tj sent a v0.9.1 report 08:12Z (tablet intake done: research/tablet/2026-11-10
  4fd44c2 ckpt 871: Merged new-acct ckpts 868-870 + its own release answer (06:41Z) into the old-a
  5b885d6 ckpt 869: Release answer sent to Tj (OUTBOX). Old heartbeat trig_01DhC2W8FUPFeqdGFQCt7yW
  475fd8a ckpt 870: Docs corrected: old account's heartbeat trig_01DhC2W8 is STILL ARMED (its dele
  7e841b1 ckpt 869: Merged the old account's ckpt 868 of 06:33Z (CHECKPOINT.md only; that session 
  c4ca434 ckpt 868: 06:35Z new-acct session: Release v0.9.1 verified, main merged (1de9e99), rb so
  b5dd9eb ckpt 868: v0.9.1 RELEASED (run 42757896306, tag on 2f5831f) + RECORDED (cc6965e) + docs 
  2362a06 ckpt 867: ship.sh v0.9.1 (code 40) green+pushed at 2f5831f; android.yml full_build=true 
```
