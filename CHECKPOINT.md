# CHECKPOINT 1331 — read me first, then TASKS.md

**Written:** 2026-11-14T02:34:42Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9ed1fe56-f77dz2` · **builds on:** `d9902a94` (this checkpoint is the commit after it)

## Just done
CD1b numbers recorded: Quickstep flat plateau (avg 414/max 427, always running; 1-day 445) = big BEFORE today's adb session, so not the helper; ours 52 MB

## Do this next
wait for Tj: CD1e cable dump (dumpsys meminfo + procstats of com.android.launcher3) or CD1f restart test; no emulator run before the dump; then read App Summary split + Activities/Views and pick: targeted emulator section cd1, or idle restart via helper (ShellServer.forceStop); probe lines CD1c ride in the next version; no heartbeat armed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  f1890526 ckpt 1330: CD1a answer sent to Tj (rows: Quickstep=stock MtkLauncher3Go 450 MB, house '
  8d691f4c ckpt 1329: CD1c done: probe-commands.txt + tablet_intake.py row/flag for the stock laun
  4b92c094 ckpt 1328: CD1 written: Tj asks if Quickstep 450 MB is normal (Memory screen). Row map 
  276cfb51 ckpt 1327: CC1 DONE: all adb grants + ACTIVATE_VPN + helper + Do everything (Tj: It is 
  76823c2d ckpt 1326: CC1e: helper line ran ([1] 16808)
  1b27a9b1 ckpt 1325: CC1c+d done: all 5 grants true, 5 appops allow incl ACTIVATE_VPN, accessibil
  f9f8eb84 ckpt 1324: CC1b done: joined grant line ran, Added: com.tj.launcher + DONE, no errors
  6386b958 ckpt 1323: CC1a done: pm path printed the base.apk (shell works); step 2 = joined grant
  e34553c0 ckpt 1322: CC1 written (Tj: adb cable, all permissions); live cable session started, st
  fdac2f4c ckpt 1321: heartbeat 12:33Z: nothing new (no Tj message since 04:58Z, no other session)
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
