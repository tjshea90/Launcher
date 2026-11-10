# CHECKPOINT 947 — read me first, then TASKS.md

**Written:** 2026-11-10T11:21:15Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `2a0524c` (this checkpoint is the commit after it)

## Just done
Run #514 b6 solo id 42805284984 head 320582e: 19 PASS 2 FAIL = only 'BQ3 closed again' window-sample x2 (Z read shown 5 of 6 while service log says cover hidden ~100 ms after each BACK and pictures 55 show 0% white): sampler reads another touchable block; BN8b PASS (before shown, after hidden, 0 re-shows), BK1b all PASS, control PASS. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Make BQ3 check LOG-based (marker close N -> last 'PENCIL cover' line must be hidden within 1.5 s) + INFO of raw cover blocks; offline test; rerun b6 (#515); IN PARALLEL release routine: bump 41/0.9.2, ship.sh, android.yml full_build. Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  585216b ckpt 943: Run in flight: #514 b6 solo shots id 42805284984 head 320582e since 11:10:29Z 
  320582e ckpt 942: b6 scenario: pen_cover_of stdin bug fixed (adb shell pidof inside a pipe swall
  1857c14 ckpt 940: Run #513 b6,n 26 PASS 5 FAIL (id 42802572154, head da7e710): BK1b O1-O3 'shown
  e067780 ckpt 938: Run in flight: #513 b6,n shots=true id 42802572154 head da7e710 since 10:50:52
  f6f2eca ckpt 936: CLAUDE.md lessons v0.9.2 written; b6,n shots run dispatched 10:50Z on branch (
  da7e710 ckpt 934: b6 scenario fixed after #512 read (BN8b=dump restarted service; control=stale 
  c175e39 ckpt 930: BQ3 SCREENSHOTS #512 (42799451859): Quickstep P2 lock screen has the QS panel 
  2d42feb ckpt 929: BQ3 round 3 coded: scenario swipe-close + shots 53/56 + shade-event log + awk 
  a784111 ckpt 924: BQ3 finding: cmd statusbar collapse is a NO-OP on the lock screen (my BQ3 chec
  9bda41c ckpt 923: BQ3 round 2 read (#511 42797400107: 16 PASS 4 FAIL; BQ3 custom+quickstep FAIL 
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
