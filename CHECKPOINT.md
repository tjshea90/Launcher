# CHECKPOINT 1046 — read me first, then TASKS.md

**Written:** 2026-11-11T00:12:50Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `4d7fcdb` (this checkpoint is the commit after it)

## Just done
BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors the hold policy (review f1), BT in radiosOff/onLock/receiver, Control texts + radios off on switch-off (f2-f4); RadioIconHoldTest 29 + LockQuiet/NotifQuiet tests green

## Do this next
docs (TASKS BS2 boxes, HANDOFF row, research known gaps), bash tools/gate.sh --check + tools/test_handoff.sh, then push, verify main==tip, dispatch emulator.yml sections=wf shots=true on main, read RESULTS, fetch ci-shots. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d41f968 ckpt 1030: BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, pha
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
  8babd57 ckpt 1018: BS1d: emulator section 'wf' written (scenario + gate-map + emulator.yml names
  dece682 ckpt 1015: BS1c code: WifiIconHold (24 tests green) + WifiIconHide + hooks (syncNow/tick
  e40df78 ckpt 1006: BS1a done: AOSP10 source confirms icon_blacklist=wifi hides the symbol in sta
  1ae6eee ckpt 1004: BS1 (Tj's Wi-Fi symbol question) written into TASKS; v0.9.3 answer is in OUTB
  d35f676 ckpt 1000: v0.9.3 RELEASED + recorded (android.yml run 44 id 42891423582, a542594, publi
  97e99eb ckpt 998: v0.9.3 (code 42) shipped a542594 = main; android.yml full_build DISPATCHED on 
```

(15 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
