# CHECKPOINT 1333 — read me first, then TASKS.md

**Written:** 2026-11-14T03:01:22Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9ed1fe56-f77dz2` · **builds on:** `e6d128d1` (this checkpoint is the commit after it)

## Just done
CD1i: emulator section cd1 written (A stock home / B ours no helper / C ours helper; INFO CD1 <tag> lines), registered in emulator.yml; handoff test 53/53

## Do this next
dispatch emulator.yml sections=cd1 on main, wait ~12 min (bounded until loop), read RESULTS INFO CD1 lines; ckpt the raw numbers BEFORE analysing; no product code change; no heartbeat armed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c6d23a07 ckpt 1332: CD1g written: friend's identical tablet has Quickstep at 48 MB; Tj force-sto
  7a65f0b4 ckpt 1331: CD1b numbers recorded: Quickstep flat plateau (avg 414/max 427, always runni
  f1890526 ckpt 1330: CD1a answer sent to Tj (rows: Quickstep=stock MtkLauncher3Go 450 MB, house '
  8d691f4c ckpt 1329: CD1c done: probe-commands.txt + tablet_intake.py row/flag for the stock laun
  4b92c094 ckpt 1328: CD1 written: Tj asks if Quickstep 450 MB is normal (Memory screen). Row map 
  276cfb51 ckpt 1327: CC1 DONE: all adb grants + ACTIVATE_VPN + helper + Do everything (Tj: It is 
  76823c2d ckpt 1326: CC1e: helper line ran ([1] 16808)
  1b27a9b1 ckpt 1325: CC1c+d done: all 5 grants true, 5 appops allow incl ACTIVATE_VPN, accessibil
  f9f8eb84 ckpt 1324: CC1b done: joined grant line ran, Added: com.tj.launcher + DONE, no errors
  6386b958 ckpt 1323: CC1a done: pm path printed the base.apk (shell works); step 2 = joined grant
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
