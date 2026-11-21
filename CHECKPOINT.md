# CHECKPOINT 1516 — read me first, then TASKS.md

**Written:** 2026-11-21T20:36:48Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `46594ad2` (this checkpoint is the commit after it)

## Just done
CS2: primePowerMenu now returns at once when the switch is off / automation off (N2 failed on head 3 of 3, tag green #699 id 44945980772); PM1 expects no PRIME line when off

## Do this next
dispatch emulator.yml sections base,n on branch claude/linkmeter-keyboard-launcher-fixes-fsk78t; if N2 green: gate the rest, ship; if still red: bisect other hunks

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b47f4085 ckpt 1515: CS2 triage: cs SOLO green on head (#695 id 44944662421) and tag (#697); base
  3f13c121 ckpt 1514: CS2 gate verdict on 96d0596b: #691 GREEN (x1,e1,o,ac,hp,pm); #692 71 PASS 9 
  b90ed541 ckpt 1513: CS2 gate dispatched on 96d0596b: #691 id 44942545183 x1,e1,o,ac,hp,pm | #692
  96d0596b ckpt 1512: CS2: prime end counts toward the 1-minute rule only when the menu was really
  3f77a1df ckpt 1511: CS2: run #690 pm 20 PASS 1 FAIL (PM4): its premise was wrong (setmode force-
  154d6cf0 ckpt 1510: CS2: run #689 pm: 18 PASS 3 FAIL = test bugs only (PM2 regexes, order check 
  b5f5106e ckpt 1509: CS2: run #688 pm: 13 PASS 8 FAIL = test bug (waited on the OFF run's old end
  1e011ec7 ckpt 1508: CS2: emulator.yml sections=pm dispatched once: run #688 id 44939552500 on br
  30080d75 ckpt 1507: CS2: PowerMenuPrimeTest 16 green, full unit suite + lint green (GRADLE_EXIT=
  d341b6c3 ckpt 1506: CS2: PowerMenuPrime (pure) + service glue primePowerMenu + ProfileSession ho
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
