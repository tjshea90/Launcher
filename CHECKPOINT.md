# CHECKPOINT 1583 — read me first, then TASKS.md

**Written:** 2026-11-22T08:57:56Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `5ebf21d4` (this checkpoint is the commit after it)

## Just done
CV1 RAW: #735 id 45002715538 FULL unified core {unified,base,n,o,ac,e1,x1,rb,last} on main@e2492d57 (product==a5396e53) GREEN 98 PASS 0 FAIL (1 native crash, harmless); #736 id 45002717972 pending

## Do this next
read #736; then bump app/build.gradle.kts 55->56 / 0.9.16->0.9.17, diff vs a5396e53 version-only, bash ship.sh, android.yml full_build=true, get_release_by_tag v0.9.17, record-release, fill draft placeholders (FULL = #735 98 PASS 0 FAIL), answer Tj; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  1e9b80df ckpt 1582: CV1: answer draft rewritten to claim only what runs proved (UL10 vacuous, Re
  e9ea49d9 ckpt 1581: CV1 FULL unified lock core x2 on main@e2492d57 (product==a5396e53): #735 id 
  2700ae96 ckpt 1580: CV1 RAW: #734 id 45001887853 (ul solo, main@5c620f40) GREEN 25P 0F, UL24 dou
  292af997 ckpt 1579: CV1: CLAUDE.md lessons for the CV1 session written; #734 ul solo id 45001887
  81d3d4c2 ckpt 1578: CV1: ul solo dispatched: #734 id 45001887853 on main@5c620f40 (UL24 double H
  5c620f40 ckpt 1577: CV1: scenario UL24 added (double Home FROM AN APP in unified, 3 rounds + gap
  0055080a ckpt 1576: CV1 RAW: #732 id 45000028314 (unified,base,n,o,ac, main@84de8fa0) GREEN 77P 
  534e9610 ckpt 1575: CV1 RAW: #733 id 45000031118 (unified,o,ac, branch ref) 41P 1F = AC8 'custom
  6f351625 ckpt 1574: CV1 triage runs on main@84de8fa0: #732 id 45000028314 = unified,base,n,o,ac 
  ba6ee6cd ckpt 1573: CV1 RAW: #730 id 44999067540 (unified,ac solo, head) GREEN 31P 0F: AC3 passe
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
