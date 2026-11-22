# CHECKPOINT 1587 — read me first, then TASKS.md

**Written:** 2026-11-22T09:12:04Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `672c8c8a` (this checkpoint is the commit after it)

## Just done
CV1 RAW: #737 id 45005254638 (unified,base, main) 25P 4F = reset: x4 (Calendar opened, apps closed, notification cleared, Wi-Fi off) beside NATIVE CRASH (GrallocUploadTh); same chain green in #738 (branch ref), #735, #732. GATE VERDICT: product a5396e53 unchanged through 10 emulator runs; every unified-ON section green in at least one clean run (full core #735 98P 0F); reds = native-crash flakes + #726 unexplained once

## Do this next
RELEASE NOW: bump app/build.gradle.kts versionCode 56 / 0.9.17, diff vs a5396e53 version-only, bash ship.sh note, android.yml full_build=true on branch, get_release_by_tag v0.9.17, record-release, fill draft, answer Tj; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  672c8c8a ckpt 1586: CV1 RAW: #738 id 45005257395 (unified,base, branch ref) GREEN 29P 0F (1 nati
  343f5a01 ckpt 1585: CV1 rerun unified,base on main@048e5f7e: #737 id 45005254638 (main), #738 id
  43467833 ckpt 1584: CV1 RAW: #736 id 45002717972 (full unified core, branch ref) 94P 4F: 'crash 
  048e5f7e ckpt 1583: CV1 RAW: #735 id 45002715538 FULL unified core {unified,base,n,o,ac,e1,x1,rb
  1e9b80df ckpt 1582: CV1: answer draft rewritten to claim only what runs proved (UL10 vacuous, Re
  e9ea49d9 ckpt 1581: CV1 FULL unified lock core x2 on main@e2492d57 (product==a5396e53): #735 id 
  2700ae96 ckpt 1580: CV1 RAW: #734 id 45001887853 (ul solo, main@5c620f40) GREEN 25P 0F, UL24 dou
  292af997 ckpt 1579: CV1: CLAUDE.md lessons for the CV1 session written; #734 ul solo id 45001887
  81d3d4c2 ckpt 1578: CV1: ul solo dispatched: #734 id 45001887853 on main@5c620f40 (UL24 double H
  5c620f40 ckpt 1577: CV1: scenario UL24 added (double Home FROM AN APP in unified, 3 rounds + gap
```
