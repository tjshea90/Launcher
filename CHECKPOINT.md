# CHECKPOINT 1313 — read me first, then TASKS.md

**Written:** 2026-11-13T05:49:37Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `8f2fa31e` (this checkpoint is the commit after it)

## Just done
gate FAILs: #597 AC8 (custom home still prepared behind lock) in e1,x,ac,an; #598 B6 + B6/D4 + BN4 (pencil cover) in x1,b6,..; no NATIVE CRASH; VpnOff cannot reach them (screenDarkNow only read by penWatch). Solo reruns 05:48Z: #599 43374376761 b6 main; #600 43374380692 b6 TAG v0.9.9; ac main + ac TAG v0.9.9 dispatched (ids: list runs status queued/in_progress)

## Do this next
~06:00Z read #599/#600 + the two ac runs (get_job_logs run_id failed_only tail 45; haiku subagent for FAIL lines if red); if head solo green or tag fails the same: ship: bump app/build.gradle.kts versionCode 49 versionName 0.9.10, bash ship.sh, android.yml full_build=true ref main, get_release_by_tag v0.9.10, record-release.sh, answer from research/CB2_RELEASE_MESSAGE_DRAFT_2026-11-13.md; heartbeat trig_01MRTg3yGjbXMnzqNtcrbLWg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  8f2fa31e ckpt 1312: gate read 05:47Z: #596 43371490476 base,n,o,wf GREEN; #597 job 131708055674 
  7b37b884 ckpt 1311: full unit suite 1051 tests 0 fail + lint green on the gated product code
  969155e7 ckpt 1310: GATE dispatched 05:21Z on 7d98cc10: #596 43371490476 base,n,o,wf; #597 43371
  7d98cc10 ckpt 1309: run #595 43370382236 vo GREEN 24 PASS 0 FAIL on 32a96ffd (VO0-VO8b incl. Qui
  d8b0eaaa ckpt 1308: vo rerun dispatched 05:11Z run #595 id 43370382236 on 32a96ffd (review fixes
  32a96ffd ckpt 1307: CB2 review fixes coded: VpnOffRules window 3/10min + SETTLING + REFUSED_BEFO
  cb4a81fe ckpt 1306: review of CB2 (subagent): 11 findings: MED1 cap reset only at USER_PRESENT; 
  862ff1ad ckpt 1305: run #594 43367976336 vo: 16 PASS 5 FAIL (VO0 VO1 VO2 VO5b VO6) = the PROBE: 
  f3f19593 ckpt 1304: CB2e (Tj: 'And any time I lock the screen from quickstep') in TASKS; rule ha
  2719a394 ckpt 1303: emulator vo dispatched 04:49Z run #594 id 43367976336 on 64268967; review su
```
