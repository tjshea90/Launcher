# CHECKPOINT 1384 — read me first, then TASKS.md

**Written:** 2026-11-17T19:26:53Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `4fd2c291` (this checkpoint is the commit after it)

## Just done
CG1c: DexoptState/DexoptWatch wired (Control, report, Setup, intake), unit test DexoptStateTest green; CG1e: ro run #617 id 44156308504 = overlay install REFUSED by PackageManager (Overlay tj.ro.fw and target android..., same for systemui), pencil pixels unchanged (0), overlays left 0

## Do this next
CG1e: rerun ro with the FULL pm install failure text + variants (android:targetName, targetSdk 28) via tools/make_overlay_apk.sh; read research/CG1E_OVERLAY_AOSP10_SOURCE_2026-11-17.md for the rule. Then reviewer reports research/CG1*_2026-11-17.md (CG1A, CG1B x3, CG1D, CG1F). heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  35dd7fe1 ckpt 1383: CG1: ro run = #617 id 44156308504 (started 19:13Z, head 0da1d72b, shots=true
  d6a7c074 ckpt 1382: CG1: emulator.yml sections=ro (shots=true) DISPATCHED on ccr-16772798-h13w43
  0da1d72b ckpt 1381: CG1e: section ro (RRO acceptance experiment: tools/make_overlay_apk.sh + emu
  7361a436 ckpt 1380: CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers la
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
  7617cde9 ckpt 1378: handoff manifest saved (.handoff/), CE1/CF1 fully done and answered; nothing
  8201dfdb ckpt 1377: CE1/CF1 answered to Tj (v0.9.11 + LinkMeter v0.2.184 released; restart/no-ca
  659d20ad ckpt 1376: CE1: v0.9.11 RELEASED and RECORDED (android.yml #52 id 43772718433, publishe
  39dc3055 ckpt 1375: CE1a-c ticked, HANDOFF 0y updated; android.yml full_build dispatched on dcea
  dceaa412 ckpt 1374: gated v0.9.11 (code 50) and pushed it: the handoff self-test, the full unit 
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
