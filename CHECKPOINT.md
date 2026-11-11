# CHECKPOINT 1095 — read me first, then TASKS.md

**Written:** 2026-11-11T02:08:36Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `bfa1438` (this checkpoint is the commit after it)

## Just done
BS2d GATE: #526 (id 42942938730, job 130268830144, rg solo, main@b44f033): 7 PASS 0 FAIL 7 INFO = GREEN. Local full unit suite 968 tests 0 fail; lint done (abortOnError false). Waiting #524 (id 42942929350) and #525 (id 42942934040)

## Do this next
read #524 + #525 at ~02:34Z (list_workflow_jobs latest -> job id -> get_job_logs tail_lines ~100 for the RESULTS header + FAIL lines; ckpt RAW verdicts before analysing); if both green: bash ship.sh note, android.yml full_build=true on main, get_release_by_tag v0.9.4, record-release.sh, answer Tj. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  f550342 ckpt 1091: BS2d GATE dispatched 02:00Z on main@b44f033 (app diff vs gated wf head 9528dd
  b44f033 ckpt 1090: BS2d: wf run #523 (id 42941618944, job 130264537064, main@9528dd0 = final cod
  52e35b7 ckpt 1087: BS2d: wf run #523 (id 42941618944, main@9528dd0 = Design B v2, sections=wf sh
  9528dd0 ckpt 1086: BS2d: Design B v2 (run #522 flaw fixed): RadioIconHold.releaseDecision state 
  fa797c7 ckpt 1084: BS2d: wf run #522 (id 42934234064, job 130240218023, main@8fbdc7d): 37 PASS 1
  81fd496 ckpt 1081: BS2d: wf run #522 (id 42934234064, main@8fbdc7d, sections=wf shots=true) disp
  8fbdc7d ckpt 1080: BS2 third-review fixes coded: deferred symbol release (RadioIconHide.deferRel
  2f6db42 ckpt 1075: BS2d: wf run #521 (id 42932077928, job 130233048653, main@8a93cbf = code BEFO
  c41a4c5 ckpt 1070: BS2d: emulator run #521 (id 42932077928, main@8a93cbf = ckpt 1068, sections=w
  8a93cbf ckpt 1068: BS2 review fixes DONE: F1 stale lq_prev forgotten at helper connect, F2 radio
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
