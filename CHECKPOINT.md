# CHECKPOINT 1368 — read me first, then TASKS.md

**Written:** 2026-11-15T22:37:07Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9028296e-kjveqx` · **builds on:** `65d67de6` (this checkpoint is the commit after it)

## Just done
CE1: #613 vo solo 24 PASS 4 FAIL (VO2d VO6b VO8a VO8c: word null = no WRITE_SECURE_SETTINGS grant in a solo vo; old VO2b/3x/5x now PASS); #614 ss solo GREEN (SS7 was the flake). Added the grant to vo setup, rerun vo solo

## Do this next
read the vo solo run (list_workflow_runs emulator.yml perPage 1; get_job_logs run_id failed_only tail 38); all PASS -> ship.sh + android.yml full_build + record-release; answer Tj (research/CE1_RELEASE_MESSAGE_DRAFT_2026-11-15.md)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M tools/emulator_scenario.sh

## Last ten checkpoints
```
  ec0a14ed ckpt 1367: CE1: vo scenario B/C rewritten for the new custom-launcher stop (log cleared
  52626f4d ckpt 1366: CE1 gate verdict final on c0d29aae: #610 86 PASS 6 FAIL (VO2b VO3b VO3c VO5 
  94a9c117 ckpt 1365: CE1 gate verdict on c0d29aae: #611 (x1,e1,y2,lq,an,wf) GREEN; #610 (base,ac,
  ed7e4836 ckpt 1364: CE1 gate on c0d29aae dispatched 21:57Z: #610 id 43766533567 base,ac,rb,vo,la
  c0d29aae ckpt 1363: CE1: Launcher unit 1054 tests 0 fail + lint ok after review fixes; dispatchi
  cdfb5bea ckpt 1362: CE1: review fixes 1,3,4,5,6,8 in VpnOff.java + ControlActivity (not yet comp
  ed0d7d93 ckpt 1361: CE1: review of VpnOff diff done, 8 findings saved in research/VPN_FOOTER (ME
  ac285646 ckpt 1360: CE1: LinkMeter docs+HANDOFF done (b9e20d7), Launcher HANDOFF 0y + VPN_FOOTER
  94f95c50 ckpt 1359: CE1b LinkMeter code written (uncommitted-build): FastRules.pauseWhy+PAUSED c
  a0e1c45b ckpt 1358: CE1a code: VpnOff follows NotifQuiet world (custom launcher too), publishes 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
