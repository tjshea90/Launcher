# CHECKPOINT 782 — read me first, then TASKS.md

**Written:** 2026-11-09T04:28:22Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `e264f27` (this checkpoint is the commit after it)

## Just done
GATE v0.8.13: ALL GREEN except 444 still running: 445 last, 447 e1,n,ac (id 42560860094), 448 hp, 449 fp,last (id 42561306209, fixed scenario) = SUCCESS; 441/443 reds explained (pt chain 0.75/0.80; last after fp = locked emulator, test fixed). Waiting ONLY for 444 pt solo id 42560312203 (started 04:10:26, slow)

## Do this next
when 444 is success (actions_get get_workflow_run 42560312203; if failure: get_job_logs run_id+failed_only tail 160, compare pt solo on tag v0.8.12): edit app/build.gradle.kts versionCode 38 / versionName 0.8.13, bash ship.sh 'BN2 first press covered on the custom home, BN2 dying dialog flash, BN4 white box after the shade, BN3 touch-away 1.5 s + lock log lines', check origin main == HEAD, android.yml full_build=true ref main, +3 min get_release_by_tag v0.8.13, bash tools/record-release.sh v0.8.13 note, give Tj the link, tick TASKS BN2-BN4 FIX boxes with test names, then BN1 M0

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d6bdfd6 ckpt 781: 448 hp SOLO = SUCCESS (HP14 green). 449 fp,last (fixed scenario, head 4b0cbfc)
  55e5668 ckpt 780: TASKS: gate story v0.8.13 written (441/442/443 triage, fixes: gate.sh solo pt+
  7adb7fa ckpt 779: 446 fp,last FAILED = TEST order problem (picture: lock screen + PIN pad up, st
  437946a ckpt 778: gate triage: 445 last SOLO = SUCCESS (443's last FAIL was chain-dependent); 44
  ae3a6e2 ckpt 777: tools/gate.sh: pt and rg now SOLO runs (+ bins grow when a run would pass 40 m
  19360e7 ckpt 776: gate 6702e72 read: 441=4 FAIL all in pt (BC2 closetap + 3 PB, patch -6.6 level
  cfa0b53 ckpt 775: gate 6702e72: run 441 (x1,v,pt,rb) FAILED, 443 (b6,o,ad,ae,hf,fp,last) FAILED,
  27833a7 ckpt 774: Tj: 'Ship v0.8.13 when the gate is green' written into TASKS; gate 441-443 on 
  2229c7c ckpt 773: BN1c: build plan written (research/SETTINGS_BUILD_PLAN_2026-11-09.md: own row 
  fde8b51 ckpt 772: BN1j: Tj 03:42Z 'Continue and finish the spoof settings app also' written into
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
