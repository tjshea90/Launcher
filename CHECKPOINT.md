# CHECKPOINT 1025 — read me first, then TASKS.md

**Written:** 2026-11-10T23:56:20Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `58adf6f` (this checkpoint is the commit after it)

## Just done
BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) written into TASKS; wf emulator run (Wi-Fi only code, main@8babd57) + review agent still in flight

## Do this next
read the wf run + review findings; then BS2a: rename WifiIcon* -> RadioIcon* (git mv + sed), slots wifi+bluetooth in RadioIconHold (restore removes only added slots), keepsRadiosOn in ReturnReset.radiosOff/LockQuiet/receiver (both radios), LockQuiet.onUnlock never turns a radio on while active (BS2b), Control text, scenario wf + gate-map names. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
  8babd57 ckpt 1018: BS1d: emulator section 'wf' written (scenario + gate-map + emulator.yml names
  dece682 ckpt 1015: BS1c code: WifiIconHold (24 tests green) + WifiIconHide + hooks (syncNow/tick
  e40df78 ckpt 1006: BS1a done: AOSP10 source confirms icon_blacklist=wifi hides the symbol in sta
  1ae6eee ckpt 1004: BS1 (Tj's Wi-Fi symbol question) written into TASKS; v0.9.3 answer is in OUTB
  d35f676 ckpt 1000: v0.9.3 RELEASED + recorded (android.yml run 44 id 42891423582, a542594, publi
  97e99eb ckpt 998: v0.9.3 (code 42) shipped a542594 = main; android.yml full_build DISPATCHED on 
  a542594 ckpt 997: gated v0.9.3 (code 42) and pushed it: the handoff self-test, the full unit sui
  634ed77 ckpt 994: BR1 emu run #518 (id 42889694843, job 130087240598, main@5360869, sections=gea
  44a6455 ckpt 992: BR1 emu run #518 id 42889694843 (sections=gear, shots, main@5360869) DISPATCHE
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
