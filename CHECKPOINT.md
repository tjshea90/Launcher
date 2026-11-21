# CHECKPOINT 1522 — read me first, then TASKS.md

**Written:** 2026-11-21T21:37:55Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/linkmeter-keyboard-launcher-fixes-fsk78t` · **builds on:** `05f66d1b` (this checkpoint is the commit after it)

## Just done
v0.9.15 (code 54) android.yml full_build DISPATCHED ONCE on main 05f66d1b at 21:36Z; gate verdict in TASKS CS2g (N2 flake after native crash unexplained)

## Do this next
about 3 min after dispatch: get_release_by_tag v0.9.15; green => bash tools/record-release.sh v0.9.15 with the same note as ship.sh; never dispatch twice

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  05f66d1b ckpt 1521: gated v0.9.15 (code 54) and pushed it: the handoff self-test, the full unit 
  612c5fc9 ckpt 1520: CS2 triage: head N2 fails ONLY when a native crash hit base earlier (fail: #
  b636f8e4 ckpt 1519: scenario only: N2 now prints mode/resumed/keyguard/a11y + lock log tail as I
  af0127bc ckpt 1518: CS2 gate on head 37b99dad-code (early-out): #702 id 44948036203 base,n,cu,pf
  37b99dad ckpt 1517: CS2 head b4485d0a: base,n #700 id 44947211442 GREEN, pm #701 id 44947217420 
  b4485d0a ckpt 1516: CS2: primePowerMenu now returns at once when the switch is off / automation 
  b47f4085 ckpt 1515: CS2 triage: cs SOLO green on head (#695 id 44944662421) and tag (#697); base
  3f13c121 ckpt 1514: CS2 gate verdict on 96d0596b: #691 GREEN (x1,e1,o,ac,hp,pm); #692 71 PASS 9 
  b90ed541 ckpt 1513: CS2 gate dispatched on 96d0596b: #691 id 44942545183 x1,e1,o,ac,hp,pm | #692
  96d0596b ckpt 1512: CS2: prime end counts toward the 1-minute rule only when the menu was really
```
