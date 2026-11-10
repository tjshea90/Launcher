# CHECKPOINT 871 — read me first, then TASKS.md

**Written:** 2026-11-10T08:08:40Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `6772046` (this checkpoint is the commit after it)

## Just done
Merged new-acct ckpts 868-870 + its own release answer (06:41Z) into the old-acct branch; BOTH heartbeats deleted (new 06:34Z, old ~07:24Z); docs corrected (TASKS BP1c/BP3d/BP3f, HANDOFF 0p, CLAUDE.md, HEARTBEAT_PROMPT). Tj has the v0.9.1 answer twice (OUTBOX)

## Do this next
NOTHING Claude-owned left. No heartbeat armed on either account. WAIT for Tj: after he installs v0.9.1 and sends a tablet report -> bash tools/tablet-intake.sh <file>, read research/tablet/LATEST.md, decide BN8b/c/d + BN7a. Only on request, low priority: BN1p R6-R8, BN5, BN6. NEVER ship v0.9.1 again. Arm exactly ONE heartbeat at the start of the next long job

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  5b885d6 ckpt 869: Release answer sent to Tj (OUTBOX). Old heartbeat trig_01DhC2W8FUPFeqdGFQCt7yW
  475fd8a ckpt 870: Docs corrected: old account's heartbeat trig_01DhC2W8 is STILL ARMED (its dele
  7e841b1 ckpt 869: Merged the old account's ckpt 868 of 06:33Z (CHECKPOINT.md only; that session 
  c4ca434 ckpt 868: 06:35Z new-acct session: Release v0.9.1 verified, main merged (1de9e99), rb so
  b5dd9eb ckpt 868: v0.9.1 RELEASED (run 42757896306, tag on 2f5831f) + RECORDED (cc6965e) + docs 
  2362a06 ckpt 867: ship.sh v0.9.1 (code 40) green+pushed at 2f5831f; android.yml full_build=true 
  2f5831f ckpt 866: gated v0.9.1 (code 40) and pushed it: the handoff self-test, the full unit sui
  ce00536 ckpt 865: Gate green: #500 + rb solo #502 (id 42756634563) on head 520b08c; gated code =
  4afb1ad ckpt 864: rb solo dispatched on main: emulator #502 id 42756634563 (head 520b08c), start
  520b08c ckpt 863: Old-acct heartbeat 04:28Z took over (new session stalled since 01:52Z; later c
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
