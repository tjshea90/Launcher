# CHECKPOINT 872 — read me first, then TASKS.md

**Written:** 2026-11-10T08:18:30Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `3895a10` (this checkpoint is the commit after it)

## Just done
Tj sent a v0.9.1 report 08:12Z (tablet intake done: research/tablet/2026-11-10-2, LATEST.md read). TASKS BQ1 written: lock screen 'screenshots' x3 + pencil white cover again

## Do this next
Read-only investigation, no release yet: BQ1a = grep TjAccessibilityService maybeMeasureScrim / ScrimMeasure / scrim_why dark theme (what the BA1 measurement does visibly, why the dark slot is not stored); BQ1b = grep updatePencil / penWatch / findPencilFull (main-thread cost, 834 tree searches avg 33 ms max 1305 ms in the AD8 line). Report lines: 629 AD8, 643 BA1, 718-818 lock log, 757 PENCIL held away in research/tablet/2026-11-10-2/tablet-report.txt. Answer Tj in ONE message. No heartbeat armed (arm ONE if the job gets long)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  4fd44c2 ckpt 871: Merged new-acct ckpts 868-870 + its own release answer (06:41Z) into the old-a
  5b885d6 ckpt 869: Release answer sent to Tj (OUTBOX). Old heartbeat trig_01DhC2W8FUPFeqdGFQCt7yW
  475fd8a ckpt 870: Docs corrected: old account's heartbeat trig_01DhC2W8 is STILL ARMED (its dele
  7e841b1 ckpt 869: Merged the old account's ckpt 868 of 06:33Z (CHECKPOINT.md only; that session 
  c4ca434 ckpt 868: 06:35Z new-acct session: Release v0.9.1 verified, main merged (1de9e99), rb so
  b5dd9eb ckpt 868: v0.9.1 RELEASED (run 42757896306, tag on 2f5831f) + RECORDED (cc6965e) + docs 
  2362a06 ckpt 867: ship.sh v0.9.1 (code 40) green+pushed at 2f5831f; android.yml full_build=true 
  2f5831f ckpt 866: gated v0.9.1 (code 40) and pushed it: the handoff self-test, the full unit sui
  ce00536 ckpt 865: Gate green: #500 + rb solo #502 (id 42756634563) on head 520b08c; gated code =
  4afb1ad ckpt 864: rb solo dispatched on main: emulator #502 id 42756634563 (head 520b08c), start
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
