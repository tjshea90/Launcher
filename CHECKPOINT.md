# CHECKPOINT 940 — read me first, then TASKS.md

**Written:** 2026-11-10T11:06:42Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `476dcb7` (this checkpoint is the commit after it)

## Just done
Run #513 b6,n 26 PASS 5 FAIL (id 42802572154, head da7e710): BK1b O1-O3 'shown' x2 modes + BN8b 'cover up before drag' read cover=none while the service LOGGED cover shown (my pid filter in pen_cover_of is wrong on CI); N2 x2 in n after b6 FAIL; pictures 55: NO white box after BACK close in both modes. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Fix pen_cover_of (drop the pid filter: same-pid lingering entries anyway; use log-based checks for BN8b-before and control), find why N2 fails after b6 (picture 46-n2-pinpad.png in scratchpad/shots513), rerun b6,n. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e067780 ckpt 938: Run in flight: #513 b6,n shots=true id 42802572154 head da7e710 since 10:50:52
  f6f2eca ckpt 936: CLAUDE.md lessons v0.9.2 written; b6,n shots run dispatched 10:50Z on branch (
  da7e710 ckpt 934: b6 scenario fixed after #512 read (BN8b=dump restarted service; control=stale 
  c175e39 ckpt 930: BQ3 SCREENSHOTS #512 (42799451859): Quickstep P2 lock screen has the QS panel 
  2d42feb ckpt 929: BQ3 round 3 coded: scenario swipe-close + shots 53/56 + shade-event log + awk 
  a784111 ckpt 924: BQ3 finding: cmd statusbar collapse is a NO-OP on the lock screen (my BQ3 chec
  9bda41c ckpt 923: BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL 
  3ec729c ckpt 922: BQ3 round 2: run #510 b6 on head (42795642177, 14 PASS 6 FAIL): BQ3 closed-pan
  135053e ckpt 913: BQ3 diagnostics coded (debug-only PENCIL probe in service; b6: closed-panel sa
  9c1b18d ckpt 904: Triage 09:50Z: b6 FAILS on head #507 (42792069234) AND tag v0.9.1 #509 (427920
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
