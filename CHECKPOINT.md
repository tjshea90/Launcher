# CHECKPOINT 1483 — read me first, then TASKS.md

**Written:** 2026-11-18T23:09:37Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `00bd3e68` (this checkpoint is the commit after it)

## Just done
CI1: VERDICT fl main #671 (head 91c9b941, before G1/SR1): 18 PASS 1 FAIL 103 INFO, only FAIL = FL2 menuP2 (9 Screenshot frames 150 ms); menuP1 was 0 frames this time; no native crash; fl 385 s. With #669/#670/#663 the FL2 failure moves between menuP1/menuP2/menuW1 run to run on identical code = emulator timing noise

## Do this next
read #673 (fl, branch head 35e5283d) and the sr run (branch head 5c84ffc9): list_workflow_runs resource_id emulator.yml branch ccr-3b4e218f-z6cmv4 event workflow_dispatch perPage 2; then haiku subagent for FAIL lines of whichever finished

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  00bd3e68 ckpt 1482: CI1: fl main #671 id 44415811567 job 135195690242 RED (names pending; menuP2
  5c84ffc9 ckpt 1481: CI1: emulator section sr written (SR0-SR8: negative control, restart via STO
  205fa802 ckpt 1480: CI1: G1 (SettingsSpoof checked reload) + SR1 (StockRefresh: STOCKRESTART hel
  9be2b3ca ckpt 1479: CI1: fl solo dispatched 22:53Z on head 91c9b941: main #671 id 44415811567, b
  91c9b941 ckpt 1478: CI1 written into TASKS (Tj full audit request), starting audit; no code yet
  368d1081 ckpt 1477: after worker restart: hooks ok, tree clean, main=6308ef0c=tip; OUTBOX had no
  6308ef0c ckpt 1476: v0.9.13 SHIPPED + recorded (BUILDLOG), LinkMeter v0.2.190; draft links fille
  b753f73f ckpt 1475: ship.sh v0.9.13 (code 52) OK, main=4bea2198; android.yml full_build=true dis
  4bea2198 ckpt 1474: gated v0.9.13 (code 52) and pushed it: the handoff self-test, the full unit 
  541cd5ed ckpt 1473: pre-ship: Pocket return switch (tile and icon, with LinkMeter) and Quickstep
```
