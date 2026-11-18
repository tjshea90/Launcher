# CHECKPOINT 1491 — read me first, then TASKS.md

**Written:** 2026-11-18T23:43:32Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `6c027d6c` (this checkpoint is the commit after it)

## Just done
CI1: VERDICT sr main #676 id 44421271607 = 13 PASS 0 FAIL (stock 32 MB/1 Act -> 11 MB/0, custom home, Recents, Quickstep, cooldown ok); ss #677 id 44421277617 = 74 PASS 0 FAIL (SS1g: G1 already right, NO full reload); head f47446c4 + bump; dispatching 3-run gate vs v0.9.13

## Do this next
read the 3 gate runs (dispatched ~23:45Z, done ~00:10Z): A base,x1,t,x,an,sr | B e1,bs,bl,rt,rb,last | C n,n6,o,ac,cs2,ss; then ship.sh, android.yml full_build

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6c027d6c ckpt 1490: CI1: version 0.9.14 code 53, draft research/CI1_RELEASE_MESSAGE_DRAFT_2026-1
  f47446c4 ckpt 1489: CI1: ss #675 id 44418711509 job 135205705700 = 73 PASS 0 FAIL (head 8a46ec64
  328c0636 ckpt 1488: CI1: reviewer ac46f883 read (7 LOW): fixed 1 (no forced reading without help
  73eebb4a ckpt 1487: CI1: sr #674 id 44417744367 job 135202357753 = 12 PASS 1 FAIL: SR2 was a TES
  083eb8f8 ckpt 1486: CI1: sr #674 id 44417744367 finished (result being read by a subagent); ss m
  5c4ef2a0 ckpt 1485: CI1: research/CI1_AUDIT_2026-11-18.md written; ss G1 check added (SS1g); rev
  8a46ec64 ckpt 1484: CI1: VERDICT fl branch #673 id 44416691055 job 135198762832 (head 35e5283d, 
  41c4e9a6 ckpt 1483: CI1: VERDICT fl main #671 (head 91c9b941, before G1/SR1): 18 PASS 1 FAIL 103
  00bd3e68 ckpt 1482: CI1: fl main #671 id 44415811567 job 135195690242 RED (names pending; menuP2
  5c84ffc9 ckpt 1481: CI1: emulator section sr written (SR0-SR8: negative control, restart via STO
```
