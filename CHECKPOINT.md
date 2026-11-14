# CHECKPOINT 1329 — read me first, then TASKS.md

**Written:** 2026-11-14T00:03:49Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9ed1fe56-f77dz2` · **builds on:** `153cca83` (this checkpoint is the commit after it)

## Just done
CD1c done: probe-commands.txt + tablet_intake.py row/flag for the stock launcher's memory (dumpsys meminfo/procstats com.android.launcher3), intake test 64/64; not released

## Do this next
send Tj the CD1a answer (rows, not normal, causes, CD1b screen checks); then look at our flows that touch the stock launcher (Recents open/clear, helper sweeps, alias toggle) for CD1d; release only together with a real change

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  4b92c094 ckpt 1328: CD1 written: Tj asks if Quickstep 450 MB is normal (Memory screen). Row map 
  276cfb51 ckpt 1327: CC1 DONE: all adb grants + ACTIVATE_VPN + helper + Do everything (Tj: It is 
  76823c2d ckpt 1326: CC1e: helper line ran ([1] 16808)
  1b27a9b1 ckpt 1325: CC1c+d done: all 5 grants true, 5 appops allow incl ACTIVATE_VPN, accessibil
  f9f8eb84 ckpt 1324: CC1b done: joined grant line ran, Added: com.tj.launcher + DONE, no errors
  6386b958 ckpt 1323: CC1a done: pm path printed the base.apk (shell works); step 2 = joined grant
  e34553c0 ckpt 1322: CC1 written (Tj: adb cable, all permissions); live cable session started, st
  fdac2f4c ckpt 1321: heartbeat 12:33Z: nothing new (no Tj message since 04:58Z, no other session)
  45fe90fe ckpt 1320: heartbeat 08:32Z+10:33Z: merged main (v0.9.10, CA1/CB1/CB2 of the other sess
  7c05a840 ckpt 1271: stand-down note only, no work: heartbeat 06:33Z rule 2 (other branch ckpt 13
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
