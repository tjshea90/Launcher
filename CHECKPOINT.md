# CHECKPOINT 1588 — read me first, then TASKS.md

**Written:** 2026-11-22T09:15:51Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `60b9303a` (this checkpoint is the commit after it)

## Just done
gated v0.9.17 (code 56) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.17 "CV1: one launcher for both worlds (the unlocked home is the custom launcher in its unlocked profile; stock launcher stays Recents host + emergency exit; switch unified_launcher)"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0ceccc00 ckpt 1587: CV1 RAW: #737 id 45005254638 (unified,base, main) 25P 4F = reset: x4 (Calend
  672c8c8a ckpt 1586: CV1 RAW: #738 id 45005257395 (unified,base, branch ref) GREEN 29P 0F (1 nati
  343f5a01 ckpt 1585: CV1 rerun unified,base on main@048e5f7e: #737 id 45005254638 (main), #738 id
  43467833 ckpt 1584: CV1 RAW: #736 id 45002717972 (full unified core, branch ref) 94P 4F: 'crash 
  048e5f7e ckpt 1583: CV1 RAW: #735 id 45002715538 FULL unified core {unified,base,n,o,ac,e1,x1,rb
  1e9b80df ckpt 1582: CV1: answer draft rewritten to claim only what runs proved (UL10 vacuous, Re
  e9ea49d9 ckpt 1581: CV1 FULL unified lock core x2 on main@e2492d57 (product==a5396e53): #735 id 
  2700ae96 ckpt 1580: CV1 RAW: #734 id 45001887853 (ul solo, main@5c620f40) GREEN 25P 0F, UL24 dou
  292af997 ckpt 1579: CV1: CLAUDE.md lessons for the CV1 session written; #734 ul solo id 45001887
  81d3d4c2 ckpt 1578: CV1: ul solo dispatched: #734 id 45001887853 on main@5c620f40 (UL24 double H
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
