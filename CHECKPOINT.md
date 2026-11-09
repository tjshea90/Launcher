# CHECKPOINT 776 — read me first, then TASKS.md

**Written:** 2026-11-09T04:11:28Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `09820e0` (this checkpoint is the commit after it)

## Just done
gate 6702e72 read: 441=4 FAIL all in pt (BC2 closetap + 3 PB, patch -6.6 levels = scrim 0.75 vs 0.80: SAME signature as BE0e #318, pt must run ALONE); 443=1 FAIL in last (stock launcher3 resumed, no press line); 442 still running. Reruns on 09820e0 (product code = 6702e72): 444 pt id 42560312203, 445 last id 42560315203, 446 fp,last+shots id 42560318046

## Do this next
read 444/445/446 (get_job_logs run_id+failed_only tail 45 = cheap probe) and 442 (id 42557653162, job 128966866909, tail 100); pt solo green + last green (or flake explained with a picture from ci-shots) -> bump versionCode 38/0.8.13, ship.sh, android.yml full_build, get_release_by_tag v0.8.13, record-release, link to Tj. If last fails again: compare fp,last on tag v0.8.12

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  cfa0b53 ckpt 775: gate 6702e72: run 441 (x1,v,pt,rb) FAILED, 443 (b6,o,ad,ae,hf,fp,last) FAILED,
  27833a7 ckpt 774: Tj: 'Ship v0.8.13 when the gate is green' written into TASKS; gate 441-443 on 
  2229c7c ckpt 773: BN1c: build plan written (research/SETTINGS_BUILD_PLAN_2026-11-09.md: own row 
  fde8b51 ckpt 772: BN1j: Tj 03:42Z 'Continue and finish the spoof settings app also' written into
  e90495a ckpt 771: BN2-4: gate runs 441/442/443 on 6702e72 started 03:40Z (x1,v,pt,rb / base,e1,n
  6702e72 ckpt 770: BN2: provisional unlock guard now POLLS (400 ms until guard end) + focus-regai
  14b4971 ckpt 769: BN2-4 review done (research/BN2_BN3_BN4_REVIEW): 6 fixed (provisional unlock g
  91877d8 ckpt 768: BN3 wired (LOCK_TOUCH_AWAY_MS 1500, touchBack pad look, noteReturn lock-log li
  5807012 ckpt 767: BN2 D1 (unlock grace in homeForeignWindow) + D3 (dying dialog) + BN4 (pencil l
  5e77687 ckpt 766: BN2/BN4: pure rules + JVM tests written (HomePatchRules.foreignTreatment/UNLOC
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
