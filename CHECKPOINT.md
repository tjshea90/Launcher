# CHECKPOINT 938 — read me first, then TASKS.md

**Written:** 2026-11-10T10:54:37Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `cd79655` (this checkpoint is the commit after it)

## Just done
Run in flight: #513 b6,n shots=true id 42802572154 head da7e710 since 10:50:52Z. Heartbeat 10:22 fire read (nothing to add). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Timer b6nmy1czh fires 11:03:30Z; then read run 42802572154: get_job_logs run_id+failed_only tail 20 (probe), haiku for RESULTS header + FAIL + BK1b/BN8b/BQ3 INFO lines; fetch ci-shots NOW-ish after the run (git fetch origin ci-shots; git archive FETCH_HEAD | tar -x -C fresh scratchpad dir; RUN.txt). Green => bump 41/0.9.2 + ship. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  f6f2eca ckpt 936: CLAUDE.md lessons v0.9.2 written; b6,n shots run dispatched 10:50Z on branch (
  da7e710 ckpt 934: b6 scenario fixed after #512 read (BN8b=dump restarted service; control=stale 
  c175e39 ckpt 930: BQ3 SCREENSHOTS #512 (42799451859): Quickstep P2 lock screen has the QS panel 
  2d42feb ckpt 929: BQ3 round 3 coded: scenario swipe-close + shots 53/56 + shade-event log + awk 
  a784111 ckpt 924: BQ3 finding: cmd statusbar collapse is a NO-OP on the lock screen (my BQ3 chec
  9bda41c ckpt 923: BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL 
  3ec729c ckpt 922: BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-pan
  135053e ckpt 913: BQ3 diagnostics coded (debug-only PENCIL probe in service; b6: closed-panel sa
  9c1b18d ckpt 904: Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (427920
  ad4c424 ckpt 901: Triage 09:41Z: base SOLO #506 (42790941782) GREEN = native-crash flake; n SOLO
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
