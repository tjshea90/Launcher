# CHECKPOINT 1075 — read me first, then TASKS.md

**Written:** 2026-11-11T00:43:36Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `4d3054a` (this checkpoint is the commit after it)

## Just done
BS2d: wf run #521 (id 42932077928, job 130233048653, main@8a93cbf = code BEFORE Design B): 37 PASS 0 FAIL 19 INFO; pictures in scratchpad shots-run521. Design B edited, NOT compiled yet

## Do this next
1) bash tools/gradle.sh :app:testDebugUnitTest --tests com.tj.launcher.RadioIconHoldTest (+LockQuietTest NotifQuiet*) after adding tests for RadioIconHold.deferRelease; 2) decide review finding 2 (leftover lq_prev); 3) rerun wf shots=true on main; then gate.sh a542594. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c41a4c5 ckpt 1070: BS2d: emulator run #521 (id 42932077928, main@8a93cbf = ckpt 1068, sections=w
  8a93cbf ckpt 1068: BS2 review fixes DONE: F1 stale lq_prev forgotten at helper connect, F2 radio
  10f38f1 ckpt 1059: BS2 review fixes IN PROGRESS: done = NotifQuietPolicy.WHY_NO_HELPER, RadioIco
  6e6b2c4 ckpt 1054: BS2d: wf run #520 (id 42930004319, main@ce7f000, code before the review fixes
  52f666f ckpt 1050: BS2d: emulator run #520 (id 42930004319, main@ce7f000, sections=wf shots=true
  ce7f000 ckpt 1049: BS2: docs (TASKS BS1b/2a/2b/2c done, HANDOFF row, research moments walked), s
  65bf8f5 ckpt 1046: BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors th
  d41f968 ckpt 1030: BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, pha
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
