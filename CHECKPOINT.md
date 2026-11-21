# CHECKPOINT 1514 — read me first, then TASKS.md

**Written:** 2026-11-21T20:08:18Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `b90ed541` (this checkpoint is the commit after it)

## Just done
CS2 gate verdict on 96d0596b: #691 GREEN (x1,e1,o,ac,hp,pm); #692 71 PASS 9 FAIL (N2 x2, CU1 CU2 CU3 CU5 CU7 CU8 CU9); #693 60 PASS 2 FAIL (CS-B2, CS-D2); no native crash; triage next

## Do this next
dispatch SOLO reruns: head 'base,n,cu' + 'cs' on branch ref and the same two on ref v0.9.14; compare; a FAIL that is identical on the tag is drift (find the premise), not the diff

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b90ed541 ckpt 1513: CS2 gate dispatched on 96d0596b: #691 id 44942545183 x1,e1,o,ac,hp,pm | #692
  96d0596b ckpt 1512: CS2: prime end counts toward the 1-minute rule only when the menu was really
  3f77a1df ckpt 1511: CS2: run #690 pm 20 PASS 1 FAIL (PM4): its premise was wrong (setmode force-
  154d6cf0 ckpt 1510: CS2: run #689 pm: 18 PASS 3 FAIL = test bugs only (PM2 regexes, order check 
  b5f5106e ckpt 1509: CS2: run #688 pm: 13 PASS 8 FAIL = test bug (waited on the OFF run's old end
  1e011ec7 ckpt 1508: CS2: emulator.yml sections=pm dispatched once: run #688 id 44939552500 on br
  30080d75 ckpt 1507: CS2: PowerMenuPrimeTest 16 green, full unit suite + lint green (GRADLE_EXIT=
  d341b6c3 ckpt 1506: CS2: PowerMenuPrime (pure) + service glue primePowerMenu + ProfileSession ho
  67e5c9df ckpt 1505: CI1: draft gate sentence made exact (single flakes named, base,an failed twi
  033a2680 ckpt 1504: CI1: v0.9.14 RELEASED + RECORDED (android.yml #55 id 44433461921 green, targ
```
