# CHECKPOINT 1054 — read me first, then TASKS.md

**Written:** 2026-11-11T00:25:49Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `274e052` (this checkpoint is the commit after it)

## Just done
BS2d: wf run #520 (id 42930004319, main@ce7f000, code before the review fixes) = 32 PASS 0 FAIL 15 INFO, wf 319 s (lock pixels control 370 vs hold 268, Quickstep icon 102 vs custom 0). BS2 delta review (agent a5b153375eab2951a): 2 major + 3 minor

## Do this next
FIX in one batch then rerun wf: F1 LockQuiet.shouldForgetPrev + forgetStalePrevAsync called from TjAccessibilityService.onServiceConnected (stale lq_prev after a missed unlock); F2 RadioIconHide.sync: hold ended with WHY_NO_HELPER while dark/locked -> LockQuiet.onLockAsync (radios off); F5 Control texts + radios-off before syncNow; tests LockQuietTest + RadioIconHoldTest; scenario: plant w1b1 again in phase N, add phase D2 (uiautomator dump on the lock screen from Quickstep: WF13..13d); gate-map wf 130. F3/F4 documented only. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  52f666f ckpt 1050: BS2d: emulator run #520 (id 42930004319, main@ce7f000, sections=wf shots=true
  ce7f000 ckpt 1049: BS2: docs (TASKS BS1b/2a/2b/2c done, HANDOFF row, research moments walked), s
  65bf8f5 ckpt 1046: BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors th
  d41f968 ckpt 1030: BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, pha
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
  8babd57 ckpt 1018: BS1d: emulator section 'wf' written (scenario + gate-map + emulator.yml names
  dece682 ckpt 1015: BS1c code: WifiIconHold (24 tests green) + WifiIconHide + hooks (syncNow/tick
  e40df78 ckpt 1006: BS1a done: AOSP10 source confirms icon_blacklist=wifi hides the symbol in sta
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
