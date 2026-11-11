# CHECKPOINT 1030 — read me first, then TASKS.md

**Written:** 2026-11-11T00:02:24Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `64b8fac` (this checkpoint is the commit after it)

## Just done
BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, phase N never-turn-on), research doc renamed RADIO_ICONS_HIDE + Bluetooth part; code NOT yet renamed (review agent aa893cfc6f9ffb41d still reading app/src/main)

## Do this next
when the review agent returns: fix its real findings, THEN BS2a rename WifiIcon* -> RadioIcon* (git mv + sed: classes, tests, keys keep_radios_hide_icons / tj_radio_icons_original / prefs tj_radio_icons, javadoc doc path), slots wifi+bluetooth in RadioIconHold (restore removes only added slots), keepsRadiosOn in ReturnReset.radiosOff both radios / LockQuiet / receiver, LockQuiet.onUnlock never turns a radio on (shouldRestore overload + tests), Control text, gate-map regex names; then unit tests, dispatch wf. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
  8babd57 ckpt 1018: BS1d: emulator section 'wf' written (scenario + gate-map + emulator.yml names
  dece682 ckpt 1015: BS1c code: WifiIconHold (24 tests green) + WifiIconHide + hooks (syncNow/tick
  e40df78 ckpt 1006: BS1a done: AOSP10 source confirms icon_blacklist=wifi hides the symbol in sta
  1ae6eee ckpt 1004: BS1 (Tj's Wi-Fi symbol question) written into TASKS; v0.9.3 answer is in OUTB
  d35f676 ckpt 1000: v0.9.3 RELEASED + recorded (android.yml run 44 id 42891423582, a542594, publi
  97e99eb ckpt 998: v0.9.3 (code 42) shipped a542594 = main; android.yml full_build DISPATCHED on 
  a542594 ckpt 997: gated v0.9.3 (code 42) and pushed it: the handoff self-test, the full unit sui
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
