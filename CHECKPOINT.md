# CHECKPOINT 1308 — read me first, then TASKS.md

**Written:** 2026-11-13T05:12:53Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `32a96ffd` (this checkpoint is the commit after it)

## Just done
vo rerun dispatched 05:11Z run #595 id 43370382236 on 32a96ffd (review fixes + reordered vo)

## Do this next
~05:19Z: list_workflow_jobs 43370382236 -> get_job_logs tail 60; if vo green: bash tools/gate.sh v0.9.9 -> dispatch its groups (core + vo), then ship.sh (bump versionCode 49 / 0.9.10 in app/build.gradle.kts), android.yml full_build, record-release, answer Tj (draft research/CB2_RELEASE_MESSAGE_DRAFT_2026-11-13.md, write it first); heartbeat trig_01MRTg3yGjbXMnzqNtcrbLWg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  32a96ffd ckpt 1307: CB2 review fixes coded: VpnOffRules window 3/10min + SETTLING + REFUSED_BEFO
  cb4a81fe ckpt 1306: review of CB2 (subagent): 11 findings: MED1 cap reset only at USER_PRESENT; 
  862ff1ad ckpt 1305: run #594 43367976336 vo: 16 PASS 5 FAIL (VO0 VO1 VO2 VO5b VO6) = the PROBE: 
  f3f19593 ckpt 1304: CB2e (Tj: 'And any time I lock the screen from quickstep') in TASKS; rule ha
  2719a394 ckpt 1303: emulator vo dispatched 04:49Z run #594 id 43367976336 on 64268967; review su
  64268967 ckpt 1302: CB2c section vo written (premise footer row, A switch off, B not allowed, C 
  61b5eff1 ckpt 1301: CB2b coded: VpnOff.java (worker thread, appops read, getAllNetworks, prepare
  d1b34d9e ckpt 1300: CB2a verified (Vpn.prepare path, Always-on blocks, legacy lockdown throws, C
  f2fed0e9 ckpt 1299: CB2b started: VpnOffRules + VpnOffRulesTest (pure rule: screen dark or lock 
  732dcb95 ckpt 1298: CB2 (Tj: turn the VPN off at screen-off / lock screen) written into TASKS; C
```
