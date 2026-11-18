# CHECKPOINT 1487 — read me first, then TASKS.md

**Written:** 2026-11-18T23:22:52Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `cef15f62` (this checkpoint is the commit after it)

## Just done
CI1: sr #674 id 44417744367 job 135202357753 = 12 PASS 1 FAIL: SR2 was a TEST fault (pid read after the restart; INFO: stock 38 MB 1 Activities -> 11 MB 0 Activities, lock log + status ok, SR4-SR7 PASS: custom home in front, Recents opens, Quickstep session shows stock home, cooldown holds); SR2 fixed (pid before the return)

## Do this next
read reviewer agent ac46f883e4e9e1e55 + ss #675 id 44418711509; rerun sr solo after fixes; gate by diff vs v0.9.13

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  083eb8f8 ckpt 1486: CI1: sr #674 id 44417744367 finished (result being read by a subagent); ss m
  5c4ef2a0 ckpt 1485: CI1: research/CI1_AUDIT_2026-11-18.md written; ss G1 check added (SS1g); rev
  8a46ec64 ckpt 1484: CI1: VERDICT fl branch #673 id 44416691055 job 135198762832 (head 35e5283d, 
  41c4e9a6 ckpt 1483: CI1: VERDICT fl main #671 (head 91c9b941, before G1/SR1): 18 PASS 1 FAIL 103
  00bd3e68 ckpt 1482: CI1: fl main #671 id 44415811567 job 135195690242 RED (names pending; menuP2
  5c84ffc9 ckpt 1481: CI1: emulator section sr written (SR0-SR8: negative control, restart via STO
  205fa802 ckpt 1480: CI1: G1 (SettingsSpoof checked reload) + SR1 (StockRefresh: STOCKRESTART hel
  9be2b3ca ckpt 1479: CI1: fl solo dispatched 22:53Z on head 91c9b941: main #671 id 44415811567, b
  91c9b941 ckpt 1478: CI1 written into TASKS (Tj full audit request), starting audit; no code yet
  368d1081 ckpt 1477: after worker restart: hooks ok, tree clean, main=6308ef0c=tip; OUTBOX had no
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
