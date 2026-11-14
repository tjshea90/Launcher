# CHECKPOINT 1339 — read me first, then TASKS.md

**Written:** 2026-11-14T03:38:19Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9ed1fe56-f77dz2` · **builds on:** `f6dd6dbc` (this checkpoint is the commit after it)

## Just done
CD2 written: Tj asks to investigate whether Quickstep sessions make Quickstep keep growing + audit all launcher functions / accessibility features that could raise its RAM

## Do this next
start: (1) hoist cd1 helpers, write emulator sections cd2 + cd2h (many Quickstep sessions, cdmem every 5) and dispatch both; (2) spawn 4 read-only audit subagents (A-D, neutral briefs) + AOSP web-fetch agent; (3) write research/CD2_QUICKSTEP_RAM_AUDIT_2026-11-14.md; no product change; no heartbeat armed

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ac475cae ckpt 1338: CD1i DONE: run 43549429509 7 PASS 0 FAIL: stock launcher 15-43 MB PSS in all
  7714987b ckpt 1337: CD1l: Tj's two Running-services screenshots show NO Quickstep in either list
  70908b8e ckpt 1336: CD1j/CD1k: Tj says restarts + cache clear + stop of our launcher leave Quick
  59d31f8b ckpt 1335: CD1g answer to Tj being sent now (ckpt 1332 claim was wrong: it had not gone
  61192f66 ckpt 1334: CD1i: cd1 dispatched on main: run 43549429509 (#603) 03:01Z, head f1283967
  f1283967 ckpt 1333: CD1i: emulator section cd1 written (A stock home / B ours no helper / C ours
  c6d23a07 ckpt 1332: CD1g written: friend's identical tablet has Quickstep at 48 MB; Tj force-sto
  7a65f0b4 ckpt 1331: CD1b numbers recorded: Quickstep flat plateau (avg 414/max 427, always runni
  f1890526 ckpt 1330: CD1a answer sent to Tj (rows: Quickstep=stock MtkLauncher3Go 450 MB, house '
  8d691f4c ckpt 1329: CD1c done: probe-commands.txt + tablet_intake.py row/flag for the stock laun
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
