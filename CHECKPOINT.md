# CHECKPOINT 1070 — read me first, then TASKS.md

**Written:** 2026-11-11T00:33:06Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `a5b52cc` (this checkpoint is the commit after it)

## Just done
BS2d: emulator run #521 (id 42932077928, main@8a93cbf = ckpt 1068, sections=wf shots=true) dispatched 00:31:54Z; code includes review fixes F1/F2/F5

## Do this next
when run 42932077928 ends (~00:40Z): get_job_logs job tail_lines 58 (find job id with list_workflow_jobs latest), ckpt the RAW verdict (PASS/FAIL counts, FAIL names) BEFORE analysing; expect ~38 PASS (new WF12c, WF13, WF13b-d); fetch ci-shots into a NEW scratchpad dir; if green: bump app/build.gradle.kts versionCode 43 + versionName 0.9.4, commit, bash tools/gate.sh a542594 (3 runs: n6,x,lq,uf,nq,rt,pf,rb,ss,wf / base,x1,e1,y2,b4,n,o,ac,cs,cs2,an,last / rg), dispatch all three on main, read each; then bash ship.sh, android.yml full_build=true on main, get_release_by_tag v0.9.4, bash tools/record-release.sh v0.9.4 note, answer Tj (research/V094_RELEASE_MESSAGE_DRAFT_2026-11-11.md), delete heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  8a93cbf ckpt 1068: BS2 review fixes DONE: F1 stale lq_prev forgotten at helper connect, F2 radio
  10f38f1 ckpt 1059: BS2 review fixes IN PROGRESS: done = NotifQuietPolicy.WHY_NO_HELPER, RadioIco
  6e6b2c4 ckpt 1054: BS2d: wf run #520 (id 42930004319, main@ce7f000, code before the review fixes
  52f666f ckpt 1050: BS2d: emulator run #520 (id 42930004319, main@ce7f000, sections=wf shots=true
  ce7f000 ckpt 1049: BS2: docs (TASKS BS1b/2a/2b/2c done, HANDOFF row, research moments walked), s
  65bf8f5 ckpt 1046: BS2a code done: Radio* rename, wifi+bluetooth slots, keepsRadiosOn mirrors th
  d41f968 ckpt 1030: BS2: scenario wf v2 written (Bluetooth list, picture probe, lock control, pha
  b2cfbb6 ckpt 1026: BS1d wf run #519 (id 42926361302, job 130213726774, main@8babd57): 24 PASS 0 
  d1c4de3 ckpt 1025: BS2 (Tj: also Bluetooth, only if already on, never turn on, never show) writt
  8e2f709 ckpt 1021: BS1: TASKS BS1c ticked, HANDOFF row added; wf emulator run dispatched on main
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
