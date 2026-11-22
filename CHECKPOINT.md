# CHECKPOINT 1553 — read me first, then TASKS.md

**Written:** 2026-11-22T06:13:05Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `d6da016e` (this checkpoint is the commit after it)

## Just done
CV1: review-2 fixes in (BgDataModel unpin skip, allLayoutDbs opens unlocked helper, insert/bulkInsert id-side guard, cover ownership + only over our home, empty folders in seed) + UL3c tolerant/diag + UL11 variants a/b/c with setmode fallback. Not yet compiled/tested after these edits. #719 id 44991880109 = 22 PASS 5 FAIL (UL3c UL11 UL12 UL13 UL15)

## Do this next
1) bash tools/gradle.sh :app:testDebugUnitTest in background (log in scratchpad, read GRADLE_EXIT=), then lint via ship.sh path; 2) push, dispatch emulator.yml ul,ul2 ref main, read with get_job_logs failed_only tail 40 (UL diag INFO lines: UL seed log, UL C variants); 3) then full gate: bash tools/gate.sh 1d8b6b2d -> 3 runs {n,o,x,bs,se,cs,mu,last} {base,legacy,e1,ac,uf,hp,rb,ss} {x1,t,bl,ad,an,cu,ul,ul2}; 4) ship.sh + android.yml full_build + record-release; heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  49db2cb3 ckpt 1552: CV1: run #719 id 44991880109 (ul,ul2, main@9d1d6c06) = 22 PASS 5 FAIL: UL3c 
  bc824069 ckpt 1551: CV1: review round 2 saved (research/CV1_REVIEW2_*): 1 HIGH (BgDataModel unpi
  6625e904 ckpt 1550: CV1: dispatched emulator #719 id 44991880109 (ul,ul2, main@9d1d6c06, fixes) 
  9d1d6c06 ckpt 1549: CV1: review fixes in: launcher3 per-DB empty flag + atomic Java seed (no wid
  eaa7fdd0 ckpt 1548: CV1: run #718 id 44990179693 (ul,ul2 on main@390f5cfb) = FAIL 21 PASS 5 FAIL
  8bcc6704 ckpt 1547: CV1: 2 reviews saved (research/CV1_REVIEW1_*): launcher3 5 HIGH, app 1 HIGH 
  95f3a56d ckpt 1546: CV1: heartbeat armed trig_01NxLAL2jZccAiP8UPYM1X7m (every 2 h at :44). Run #
  2d2b69e4 ckpt 1545: CV1 takeover: new session continues old one (Tj told me to). Emulator run #7
  390f5cfb ckpt 1544: CV1: sections ul+ul2 written, gate-map + workflow list, handoff test 53 ok
  e18d1484 ckpt 1543: CV1: code done: Unified/UnifiedRules/HomeProfile, provider profile db, loade
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
