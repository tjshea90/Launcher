# CHECKPOINT 1555 — read me first, then TASKS.md

**Written:** 2026-11-22T06:24:20Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `cd2ed2a3` (this checkpoint is the commit after it)

## Just done
CV1: run #720 id 44992764510 (ul,ul2, main@87bfc562) = 26 PASS 1 FAIL 16 INFO: only UL4 FAILED (drawer dump showed the home with Email, Gallery = swipe did not open the drawer after a default-layout first load). Seed log: 'custom layout holds nothing to copy' = the REAL copy path (ids +1e9, folder links, widget/deep-shortcut exclusion) was NOT exercised. Failsafe variants a/b/c: 3 presses registered each but mode stayed custom (helper probably still bound/re-enabled) -> setmode fallback worked, UL11-15 PASS

## Do this next
scenario ul: (1) uldrawer retry until 'Search apps' is in the dump; (2) INFO both dbs' apps; (3) NEW: inject rows into launcher.db on the host with python3 sqlite3 (app, folder + 2 items, a widget row, a deep-shortcut row) before phase A, then pull launcher_unlocked.db and check ids >= 1e9, folder links, no widget/deep shortcut rows (UL3d); then re-run ul,ul2; then gate; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  5358e822 ckpt 1554: CV1: dispatched emulator #720 id 44992764510 (ul,ul2, main@87bfc562, review-
  87bfc562 ckpt 1553: CV1: review-2 fixes in (BgDataModel unpin skip, allLayoutDbs opens unlocked 
  49db2cb3 ckpt 1552: CV1: run #719 id 44991880109 (ul,ul2, main@9d1d6c06) = 22 PASS 5 FAIL: UL3c 
  bc824069 ckpt 1551: CV1: review round 2 saved (research/CV1_REVIEW2_*): 1 HIGH (BgDataModel unpi
  6625e904 ckpt 1550: CV1: dispatched emulator #719 id 44991880109 (ul,ul2, main@9d1d6c06, fixes) 
  9d1d6c06 ckpt 1549: CV1: review fixes in: launcher3 per-DB empty flag + atomic Java seed (no wid
  eaa7fdd0 ckpt 1548: CV1: run #718 id 44990179693 (ul,ul2 on main@390f5cfb) = FAIL 21 PASS 5 FAIL
  8bcc6704 ckpt 1547: CV1: 2 reviews saved (research/CV1_REVIEW1_*): launcher3 5 HIGH, app 1 HIGH 
  95f3a56d ckpt 1546: CV1: heartbeat armed trig_01NxLAL2jZccAiP8UPYM1X7m (every 2 h at :44). Run #
  2d2b69e4 ckpt 1545: CV1 takeover: new session continues old one (Tj told me to). Emulator run #7
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
