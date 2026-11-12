# CHECKPOINT 1208 — read me first, then TASKS.md

**Written:** 2026-11-12T19:36:29Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `6d895bd7` (this checkpoint is the commit after it)

## Just done
TAKEOVER (account 2, branch ccr-91599ec3-za40s7; acct-1 session cut off, last real work 17:53Z). #563 id 43273214570 (fl on fcc27808): 8 PASS 11 FAIL. FL1 flashA frames: wakeH1 9, wakeH2 8, wakeL 8, shade 135 (+flashB 1), pad 7, tap 0; shadeX too-few-frames. FL2 menu films: menu frames 0 in menuL/W1/W2/S2/P1/P2 (flashA 122/150/48/34/130/18), menuS1 menu=170 flashA 0; shot 0 everywhere. Paths: fast open + fast skip already open.

## Do this next
BY1b: analyse #563: why the menu detector sees 0 menu frames in 6 of 7 menu films (test or product?), what the wake flashes at ~4.8 s are (5 films same worst 69.3); then dispatch fl on main (861a3526+ has PATCHEV lines). Heartbeat acct-1 trig_01DZ15y7gsi1bXpJ7P2KBghc; acct-2: arm one

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  861a3526 ckpt 1207: BY1b: fl BEFORE run #563 id 43273214570 dispatched 17:48Z on fcc27808; fl go
  fcc27808 ckpt 1206: BY1b: section fl written (films: wakeH1/2 wakeL shade shadeX pad tap + menuL
  59478275 ckpt 1205: BY1b: tools/fl_analyze.py (selftest OK) + section fl design in research/BY1_
  4c088a33 ckpt 1204: BY1-F: 4 code findings (lock patch over open shade, dark-rest at wake, fastD
  b83f8177 ckpt 1203: BY1 written into TASKS (Tj 17:26Z: perf review + lock/home patch flicker fil
  44afb137 ckpt 1202: BX1 DONE: v0.9.7 (code 46) RELEASED+RECORDED (android.yml #48 id 43139795817
  30021942 ckpt 1201: BX1 emulator #562 id 43137670443 (base,legacy,x on 16b47b65, product code ==
  3ba1ec6f ckpt 1200: gated v0.9.7 (code 46) and pushed it: the handoff self-test, the full unit s
  3f0de92d ckpt 1199: BX1: emulator smoke #562 id 43137670443 on 16b47b65 (base,legacy,x) dispatch
  16b47b65 ckpt 1198: BX1 code done: Quickstep -> Gboard (KeyboardPolicy.pickGboard/noTargetStatus
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
