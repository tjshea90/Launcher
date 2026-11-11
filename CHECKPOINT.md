# CHECKPOINT 1099 — read me first, then TASKS.md

**Written:** 2026-11-11T02:37:51Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `ccd1024` (this checkpoint is the commit after it)

## Just done
BS2d GATE reruns dispatched 02:36Z on main@ccd1024 (app diff vs gated b44f033 = docs only): #527 id 42947197401 = ss SOLO, #528 id 42947200775 = cs SOLO. Gate so far: rg #526 7/0 green; #524 150 PASS + ss poisoned by native crash; #525 148 PASS + CS-B2 FAIL

## Do this next
read #527 + #528 at ~02:48Z (list_workflow_jobs latest -> job id -> get_job_logs tail_lines 58). Both green => git diff b44f033 HEAD -- app launcher3 phoneguide must be empty, then: bash ship.sh 'v0.9.4: Wi-Fi + Bluetooth stay on, symbols hidden (BS1/BS2)'; android.yml full_build=true on main; get_release_by_tag v0.9.4; bash tools/record-release.sh v0.9.4 note; HANDOFF block 0r; TASKS ticks BS1d/BS1e/BS2d; answer Tj from research/V094_RELEASE_MESSAGE_DRAFT_2026-11-11.md (add gate numbers); delete heartbeat. cs red again => rerun cs on ref v0.9.3 to compare. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M OUTBOX.md

## Last ten checkpoints
```
  ccd1024 ckpt 1098: BS2d GATE raw: #524 (id 42942929350, job 130268804149, base,n6,o,x,uf,cs2,an,
  67c16d8 ckpt 1095: BS2d GATE: #526 (id 42942938730, job 130268830144, rg solo, main@b44f033): 7 
  f550342 ckpt 1091: BS2d GATE dispatched 02:00Z on main@b44f033 (app diff vs gated wf head 9528dd
  b44f033 ckpt 1090: BS2d: wf run #523 (id 42941618944, job 130264537064, main@9528dd0 = final cod
  52e35b7 ckpt 1087: BS2d: wf run #523 (id 42941618944, main@9528dd0 = Design B v2, sections=wf sh
  9528dd0 ckpt 1086: BS2d: Design B v2 (run #522 flaw fixed): RadioIconHold.releaseDecision state 
  fa797c7 ckpt 1084: BS2d: wf run #522 (id 42934234064, job 130240218023, main@8fbdc7d): 37 PASS 1
  81fd496 ckpt 1081: BS2d: wf run #522 (id 42934234064, main@8fbdc7d, sections=wf shots=true) disp
  8fbdc7d ckpt 1080: BS2 third-review fixes coded: deferred symbol release (RadioIconHide.deferRel
  2f6db42 ckpt 1075: BS2d: wf run #521 (id 42932077928, job 130233048653, main@8a93cbf = code BEFO
```
