# CHECKPOINT 874 — read me first, then TASKS.md

**Written:** 2026-11-10T08:31:43Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `ccf4c52` (this checkpoint is the commit after it)

## Just done
BQ1a fix coded: ScrimMeasure settle 10 s, 1 run/lock, EVERY run that stored nothing counts (2 per slot then give up), RUN_KEY last-run line + LockLog SCRIM lines + report line; ScrimMeasureTest +6 tests green. Heartbeat armed trig_013YUvG3qqKTpfyqZn9k2nmE (every 2h at :22)

## Do this next
BQ1b pencil: read updatePencil / penWatch / findPencilFull / penQuicken in TjAccessibilityService.java (grep -n), design hide-at-finger-down rule, JVM test in PencilRulesTest, then emulator b6 + tools/gate.sh v0.9.1. Then /code-review high, ship v0.9.2 (code 41). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE armed 08:22Z (delete when shipped)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  835a7b0 ckpt 873: BQ1a cause found in code: BA1 measurement = REAL screenshots via performGlobal
  fa62839 ckpt 872: Tj sent a v0.9.1 report 08:12Z (tablet intake done: research/tablet/2026-11-10
  4fd44c2 ckpt 871: Merged new-acct ckpts 868-870 + its own release answer (06:41Z) into the old-a
  5b885d6 ckpt 869: Release answer sent to Tj (OUTBOX). Old heartbeat trig_01DhC2W8FUPFeqdGFQCt7yW
  475fd8a ckpt 870: Docs corrected: old account's heartbeat trig_01DhC2W8 is STILL ARMED (its dele
  7e841b1 ckpt 869: Merged the old account's ckpt 868 of 06:33Z (CHECKPOINT.md only; that session 
  c4ca434 ckpt 868: 06:35Z new-acct session: Release v0.9.1 verified, main merged (1de9e99), rb so
  b5dd9eb ckpt 868: v0.9.1 RELEASED (run 42757896306, tag on 2f5831f) + RECORDED (cc6965e) + docs 
  2362a06 ckpt 867: ship.sh v0.9.1 (code 40) green+pushed at 2f5831f; android.yml full_build=true 
  2f5831f ckpt 866: gated v0.9.1 (code 40) and pushed it: the handoff self-test, the full unit sui
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
