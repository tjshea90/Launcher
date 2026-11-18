# CHECKPOINT 1484 — read me first, then TASKS.md

**Written:** 2026-11-18T23:12:28Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3b4e218f-z6cmv4` · **builds on:** `41c4e9a6` (this checkpoint is the commit after it)

## Just done
CI1: VERDICT fl branch #673 id 44416691055 job 135198762832 (head 35e5283d, has G1): 17 PASS 2 FAIL = FL2 menuW2 (flashA 4 frames) + FL2 menuP2 (12 frames 200 ms). menuP2 failed in BOTH of today's runs (9 and 12 frames): its case = pad pulled up, BACK, then side-key hold within ~0.3 s: the patch is still AWAY (pad-cancel hold), fast path shows it at the dialog event and the emulator draws it 150-200 ms late. No product change for the patch (no tablet data, timing noise); sr #674 id 44417744367 running

## Do this next
read sr #674 (haiku subagent, job via list_workflow_jobs); dispatch ss solo for G1; then code-review high, gate by diff vs v0.9.13

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  41c4e9a6 ckpt 1483: CI1: VERDICT fl main #671 (head 91c9b941, before G1/SR1): 18 PASS 1 FAIL 103
  00bd3e68 ckpt 1482: CI1: fl main #671 id 44415811567 job 135195690242 RED (names pending; menuP2
  5c84ffc9 ckpt 1481: CI1: emulator section sr written (SR0-SR8: negative control, restart via STO
  205fa802 ckpt 1480: CI1: G1 (SettingsSpoof checked reload) + SR1 (StockRefresh: STOCKRESTART hel
  9be2b3ca ckpt 1479: CI1: fl solo dispatched 22:53Z on head 91c9b941: main #671 id 44415811567, b
  91c9b941 ckpt 1478: CI1 written into TASKS (Tj full audit request), starting audit; no code yet
  368d1081 ckpt 1477: after worker restart: hooks ok, tree clean, main=6308ef0c=tip; OUTBOX had no
  6308ef0c ckpt 1476: v0.9.13 SHIPPED + recorded (BUILDLOG), LinkMeter v0.2.190; draft links fille
  b753f73f ckpt 1475: ship.sh v0.9.13 (code 52) OK, main=4bea2198; android.yml full_build=true dis
  4bea2198 ckpt 1474: gated v0.9.13 (code 52) and pushed it: the handoff self-test, the full unit 
```
