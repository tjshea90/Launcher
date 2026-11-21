# CHECKPOINT 1518 — read me first, then TASKS.md

**Written:** 2026-11-21T21:05:53Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `5f07c6a9` (this checkpoint is the commit after it)

## Just done
CS2 gate on head 37b99dad-code (early-out): #702 id 44948036203 base,n,cu,pf,rb,last = 77 PASS 3 FAIL (N2 x2 + card Restart) + native crash in base; #704 id 44948063907 lq,uf,cs,cs2,pk = 60 PASS 2 FAIL (CS-B2, CS-D2, no crash); #700/#701 base,n and pm green; control: same two chains dispatched on tag v0.9.14

## Do this next
read the two tag chain runs (newest 2 on ref v0.9.14) + haiku comparison of 6 runs; identical FAILs on tag = environment, else bisect hunks

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  37b99dad ckpt 1517: CS2 head b4485d0a: base,n #700 id 44947211442 GREEN, pm #701 id 44947217420 
  b4485d0a ckpt 1516: CS2: primePowerMenu now returns at once when the switch is off / automation 
  b47f4085 ckpt 1515: CS2 triage: cs SOLO green on head (#695 id 44944662421) and tag (#697); base
  3f13c121 ckpt 1514: CS2 gate verdict on 96d0596b: #691 GREEN (x1,e1,o,ac,hp,pm); #692 71 PASS 9 
  b90ed541 ckpt 1513: CS2 gate dispatched on 96d0596b: #691 id 44942545183 x1,e1,o,ac,hp,pm | #692
  96d0596b ckpt 1512: CS2: prime end counts toward the 1-minute rule only when the menu was really
  3f77a1df ckpt 1511: CS2: run #690 pm 20 PASS 1 FAIL (PM4): its premise was wrong (setmode force-
  154d6cf0 ckpt 1510: CS2: run #689 pm: 18 PASS 3 FAIL = test bugs only (PM2 regexes, order check 
  b5f5106e ckpt 1509: CS2: run #688 pm: 13 PASS 8 FAIL = test bug (waited on the OFF run's old end
  1e011ec7 ckpt 1508: CS2: emulator.yml sections=pm dispatched once: run #688 id 44939552500 on br
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
