# CHECKPOINT 1102 — read me first, then TASKS.md

**Written:** 2026-11-11T02:51:57Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `1f69f2f` (this checkpoint is the commit after it)

## Just done
gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.4 "v0.9.4: keep Wi-Fi and Bluetooth on, hide their symbols on the lock screen and in the custom launcher (BS1/BS2)"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b64b0e1 ckpt 1100: BS2d GATE COMPLETE: #527 (id 42947197401, job 130282725555, ss solo) step con
  471266e ckpt 1099: BS2d GATE reruns dispatched 02:36Z on main@ccd1024 (app diff vs gated b44f033
  ccd1024 ckpt 1098: BS2d GATE raw: #524 (id 42942929350, job 130268804149, base,n6,o,x,uf,cs2,an,
  67c16d8 ckpt 1095: BS2d GATE: #526 (id 42942938730, job 130268830144, rg solo, main@b44f033): 7 
  f550342 ckpt 1091: BS2d GATE dispatched 02:00Z on main@b44f033 (app diff vs gated wf head 9528dd
  b44f033 ckpt 1090: BS2d: wf run #523 (id 42941618944, job 130264537064, main@9528dd0 = final cod
  52e35b7 ckpt 1087: BS2d: wf run #523 (id 42941618944, main@9528dd0 = Design B v2, sections=wf sh
  9528dd0 ckpt 1086: BS2d: Design B v2 (run #522 flaw fixed): RadioIconHold.releaseDecision state 
  fa797c7 ckpt 1084: BS2d: wf run #522 (id 42934234064, job 130240218023, main@8fbdc7d): 37 PASS 1
  81fd496 ckpt 1081: BS2d: wf run #522 (id 42934234064, main@8fbdc7d, sections=wf shots=true) disp
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
