# CHECKPOINT 1380 — read me first, then TASKS.md

**Written:** 2026-11-17T19:07:30Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `dc10ff72` (this checkpoint is the commit after it)

## Just done
CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers launched (reports land in research/CG1A_MODE_PATHS, CG1D_PATCH_LIFECYCLE, CG1B_LAUNCHER3_PERF, CG1B_PROCESS_PERF, CG1B_A11Y_PERF, CG1F_QUICKSTEP_AOSP_ANGLE, CG1E_OVERLAY_AOSP10_SOURCE, all _2026-11-17.md)

## Do this next
heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ. While reviewers run: CG1c (baseline-prof.txt + androidx.profileinstaller in app/build.gradle.kts, dexopt-state parser, Setup compile). When a research/CG1*.md report is missing after a cut-off: its reviewer died, rerun that slice. Then read reports -> write findings into TASKS CG1a/b/d/e/f

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
  7617cde9 ckpt 1378: handoff manifest saved (.handoff/), CE1/CF1 fully done and answered; nothing
  8201dfdb ckpt 1377: CE1/CF1 answered to Tj (v0.9.11 + LinkMeter v0.2.184 released; restart/no-ca
  659d20ad ckpt 1376: CE1: v0.9.11 RELEASED and RECORDED (android.yml #52 id 43772718433, publishe
  39dc3055 ckpt 1375: CE1a-c ticked, HANDOFF 0y updated; android.yml full_build dispatched on dcea
  dceaa412 ckpt 1374: gated v0.9.11 (code 50) and pushed it: the handoff self-test, the full unit 
  858ec985 ckpt 1373: pre-ship: v0.9.11: no VPN on the lock screen, with the screen off or in the 
  5c176218 ckpt 1372: CE1 GATE COMPLETE on product code c0d29aae (identical at HEAD): #611 id 4376
  c44673fa ckpt 1371: CE1: vo phase E rewritten (cap independent of earlier stop counts); rerun vo
  e2bc0169 ckpt 1370: CE1 vo #615 (ec0a14ed+grant): 26 PASS 2 FAIL (VO5b VO5c only: the cap check 
```
