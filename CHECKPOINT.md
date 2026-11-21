# CHECKPOINT 1506 — read me first, then TASKS.md

**Written:** 2026-11-21T18:57:19Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `7e0bc2a9` (this checkpoint is the commit after it)

## Just done
CS2: PowerMenuPrime (pure) + service glue primePowerMenu + ProfileSession hook + Control switch + section pm; not built yet

## Do this next
build: bash tools/gradle.sh :app:testDebugUnitTest --tests com.tj.launcher.PowerMenuPrimeTest; then assembleRelease; /code-review; run section pm via emulator.yml

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  67e5c9df ckpt 1505: CI1: draft gate sentence made exact (single flakes named, base,an failed twi
  033a2680 ckpt 1504: CI1: v0.9.14 RELEASED + RECORDED (android.yml #55 id 44433461921 green, targ
  091150f9 ckpt 1503: CI1: ship.sh green (handoff, unit suite), v0.9.14 code 53 pushed main a92505
  a92505cd ckpt 1502: gated v0.9.14 (code 53) and pushed it: the handoff self-test, the full unit 
  36359368 ckpt 1501: CI1: GATE DONE on identical product code (git diff 3e02bf08..: tools only): 
  e529353f ckpt 1500: CI1: base,an x2 dispatched 00:45Z (main a2b5316c + branch ref); ids = newest
  a2b5316c ckpt 1499: CI1: #685 base,an main FAIL = only 'hidden: REINSTALLED app is forgotten' (A
  1ff86b86 ckpt 1498: CI1: triage: an solo main #683 id 44428928340 SUCCESS; base,an on TAG v0.9.1
  8d1a658d ckpt 1497: CI1: base,an #681 id 44426690735 = 41 PASS 1 FAIL: only AN9b (lock log shows
  681c0607 ckpt 1496: CI1: rerun verdict: ac solo #682 id 44426695151 SUCCESS (AC2 was the native-
```

(9 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
