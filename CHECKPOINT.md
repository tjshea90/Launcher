# CHECKPOINT 943 — read me first, then TASKS.md

**Written:** 2026-11-10T11:12:07Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `320582e` (this checkpoint is the commit after it)

## Just done
Run in flight: #514 b6 solo shots id 42805284984 head 320582e since 11:10:29Z (timer bjn108lc2 11:22). TASKS BQ3-RESULT + BQ4 (N2 after b6) written, HANDOFF 0q heading updated. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
At 11:22Z read run 42805284984: list_workflow_runs emulator.yml branch ccr-5ef85f7a-ngtgj1 workflow_dispatch perPage 1 (status/conclusion); get_job_logs run_id failed_only tail 20 ('No failed jobs' = green); if red a haiku subagent for header+FAIL+BK1b/BN8b/BQ3 INFO; fetch ci-shots (RUN.txt must name the run). Green => bump app/build.gradle.kts 41/0.9.2, bash ship.sh 'note', dispatch android.yml full_build=true on ccr-5ef85f7a-ngtgj1, get_release_by_tag v0.9.2, bash tools/record-release.sh v0.9.2 'note', tick TASKS BQ1a/BQ1b/BQ2b (ScrimMeasureTest 34/35, PencilRulesTest 35), rewrite HANDOFF 0q, fill the release message draft, ONE message to Tj, delete heartbeat. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M HANDOFF.md
     M TASKS.md

## Last ten checkpoints
```
  320582e ckpt 942: b6 scenario: pen_cover_of stdin bug fixed (adb shell pidof inside a pipe swall
  1857c14 ckpt 940: Run #513 b6,n 26 PASS 5 FAIL (id 42802572154, head da7e710): BK1b O1-O3 'shown
  e067780 ckpt 938: Run in flight: #513 b6,n shots=true id 42802572154 head da7e710 since 10:50:52
  f6f2eca ckpt 936: CLAUDE.md lessons v0.9.2 written; b6,n shots run dispatched 10:50Z on branch (
  da7e710 ckpt 934: b6 scenario fixed after #512 read (BN8b=dump restarted service; control=stale 
  c175e39 ckpt 930: BQ3 SCREENSHOTS #512 (42799451859): Quickstep P2 lock screen has the QS panel 
  2d42feb ckpt 929: BQ3 round 3 coded: scenario swipe-close + shots 53/56 + shade-event log + awk 
  a784111 ckpt 924: BQ3 finding: cmd statusbar collapse is a NO-OP on the lock screen (my BQ3 chec
  9bda41c ckpt 923: BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL 
  3ec729c ckpt 922: BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-pan
```
