# CHECKPOINT 936 — read me first, then TASKS.md

**Written:** 2026-11-10T10:52:58Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `b7478bd` (this checkpoint is the commit after it)

## Just done
CLAUDE.md lessons v0.9.2 written; b6,n shots run dispatched 10:50Z on branch (read ~11:04Z). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
At ~11:04Z list emulator.yml runs (branch ccr-5ef85f7a-ngtgj1, workflow_dispatch, perPage 1), get_job_logs run_id+failed_only tail 20 (probe), haiku for header+FAIL+BK1b/BQ3/BN8b INFO + fetch ci-shots (git fetch origin ci-shots; git archive into fresh scratchpad dir); green => bump 41/0.9.2, ship.sh, android.yml full_build, record-release, message. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M CLAUDE.md

## Last ten checkpoints
```
  da7e710 ckpt 934: b6 scenario fixed after #512 read (BN8b=dump restarted service; control=stale 
  c175e39 ckpt 930: BQ3 SCREENSHOTS #512 (42799451859): Quickstep P2 lock screen has the QS panel 
  2d42feb ckpt 929: BQ3 round 3 coded: scenario swipe-close + shots 53/56 + shade-event log + awk 
  a784111 ckpt 924: BQ3 finding: cmd statusbar collapse is a NO-OP on the lock screen (my BQ3 chec
  9bda41c ckpt 923: BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL 
  3ec729c ckpt 922: BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-pan
  135053e ckpt 913: BQ3 diagnostics coded (debug-only PENCIL probe in service; b6: closed-panel sa
  9c1b18d ckpt 904: Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (427920
  ad4c424 ckpt 901: Triage 09:41Z: base SOLO #506 (42790941782) GREEN = native-crash flake; n SOLO
  a837b79 ckpt 900: Gate 6878395 raw verdict: #504 b6,n,o,x,ac,ad = 98 PASS 4 FAIL (BK1b no-cover 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
