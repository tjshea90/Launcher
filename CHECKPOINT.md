# CHECKPOINT 1381 — read me first, then TASKS.md

**Written:** 2026-11-17T19:13:21Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `a11f0638` (this checkpoint is the commit after it)

## Just done
CG1e: section ro (RRO acceptance experiment: tools/make_overlay_apk.sh + emulator_scenario.sh ro; framework ic_mode_edit overlay + SystemUI colour overlay, INFO only) written, pushed; not run yet

## Do this next
dispatch emulator.yml sections=ro on ref ccr-16772798-h13w43 (shots=true), read RESULTS block (get_job_logs tail_lines ~80) -> if overlay accepted test the pencil on the LOCK screen; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ; reviewers' reports in research/CG1*_2026-11-17.md (CG1E source report already there)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  7361a436 ckpt 1380: CG1: heartbeat armed trig_01EzvMtE44ag8oMav4DqoqHZ; 7 read-only reviewers la
  dc10ff72 ckpt 1379: CG1 written into TASKS (Tj's 6 asks, verbatim); prior research read (CA1 NEX
  7617cde9 ckpt 1378: handoff manifest saved (.handoff/), CE1/CF1 fully done and answered; nothing
  8201dfdb ckpt 1377: CE1/CF1 answered to Tj (v0.9.11 + LinkMeter v0.2.184 released; restart/no-ca
  659d20ad ckpt 1376: CE1: v0.9.11 RELEASED and RECORDED (android.yml #52 id 43772718433, publishe
  39dc3055 ckpt 1375: CE1a-c ticked, HANDOFF 0y updated; android.yml full_build dispatched on dcea
  dceaa412 ckpt 1374: gated v0.9.11 (code 50) and pushed it: the handoff self-test, the full unit 
  858ec985 ckpt 1373: pre-ship: v0.9.11: no VPN on the lock screen, with the screen off or in the 
  5c176218 ckpt 1372: CE1 GATE COMPLETE on product code c0d29aae (identical at HEAD): #611 id 4376
  c44673fa ckpt 1371: CE1: vo phase E rewritten (cap independent of earlier stop counts); rerun vo
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
