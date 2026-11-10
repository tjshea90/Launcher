# CHECKPOINT 873 — read me first, then TASKS.md

**Written:** 2026-11-10T08:20:49Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `fa62839` (this checkpoint is the commit after it)

## Just done
BQ1a cause found in code: BA1 measurement = REAL screenshots via performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT); interrupted tries are never counted (ScrimMeasure.measurementFailure), dark slot retried each lock visit, MAX_PER_DAY 4

## Do this next
Implement BQ1a fix (TASKS BQ1a-FINDING plan): ScrimMeasure.decision + new State fields (themeSinceMs, count of interrupted tries per slot), TjAccessibilityService.scrimState/isNightNow/scrimFinish (record interruptions), constants MAX_PER_LOCK 1 / MAX_PER_DAY 2; JVM tests in ScrimMeasureTest (bash tools/gradle.sh :app:testDebugUnitTest --tests '*ScrimMeasure*'). Then BQ1b pencil (updatePencil/penWatch/findPencilFull). One release for both, gate via tools/gate.sh v0.9.1. No heartbeat armed: arm ONE now (long job)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  fa62839 ckpt 872: Tj sent a v0.9.1 report 08:12Z (tablet intake done: research/tablet/2026-11-10
  4fd44c2 ckpt 871: Merged new-acct ckpts 868-870 + its own release answer (06:41Z) into the old-a
  5b885d6 ckpt 869: Release answer sent to Tj (OUTBOX). Old heartbeat trig_01DhC2W8FUPFeqdGFQCt7yW
  475fd8a ckpt 870: Docs corrected: old account's heartbeat trig_01DhC2W8 is STILL ARMED (its dele
  7e841b1 ckpt 869: Merged the old account's ckpt 868 of 06:33Z (CHECKPOINT.md only; that session 
  c4ca434 ckpt 868: 06:35Z new-acct session: Release v0.9.1 verified, main merged (1de9e99), rb so
  b5dd9eb ckpt 868: v0.9.1 RELEASED (run 42757896306, tag on 2f5831f) + RECORDED (cc6965e) + docs 
  2362a06 ckpt 867: ship.sh v0.9.1 (code 40) green+pushed at 2f5831f; android.yml full_build=true 
  2f5831f ckpt 866: gated v0.9.1 (code 40) and pushed it: the handoff self-test, the full unit sui
  ce00536 ckpt 865: Gate green: #500 + rb solo #502 (id 42756634563) on head 520b08c; gated code =
```
