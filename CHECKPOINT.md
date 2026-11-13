# CHECKPOINT 1317 — read me first, then TASKS.md

**Written:** 2026-11-13T06:04:53Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `f666bb65` (this checkpoint is the commit after it)

## Just done
ship.sh v0.9.10 green (SHIP_EXIT=0, f666bb65, main==tip); android.yml full_build dispatched 06:03Z

## Do this next
at +3 min: get_release_by_tag v0.9.10 (and android.yml run list perPage 1); then record-release.sh v0.9.10 'v0.9.10: the VPN is turned off when the screen goes off or the lock screen is up, in both launchers (CB2/CB2e)'; answer from research/CB2_RELEASE_MESSAGE_DRAFT_2026-11-13.md; delete heartbeat trig_01MRTg3yGjbXMnzqNtcrbLWg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  f666bb65 ckpt 1316: gated v0.9.10 (code 49) and pushed it: the handoff self-test, the full unit 
  3db81d27 ckpt 1315: version bumped to 49 / 0.9.10; ship.sh starting (scratchpad/ship-cb2.txt, SH
  498e4b60 ckpt 1314: GATE GREEN: solo reruns #599 b6 head GREEN, #600 b6 TAG GREEN, #601 43374384
  e7f4655b ckpt 1313: gate FAILs: #597 AC8 (custom home still prepared behind lock) in e1,x,ac,an;
  8f2fa31e ckpt 1312: gate read 05:47Z: #596 43371490476 base,n,o,wf GREEN; #597 job 131708055674 
  7b37b884 ckpt 1311: full unit suite 1051 tests 0 fail + lint green on the gated product code
  969155e7 ckpt 1310: GATE dispatched 05:21Z on 7d98cc10: #596 43371490476 base,n,o,wf; #597 43371
  7d98cc10 ckpt 1309: run #595 43370382236 vo GREEN 24 PASS 0 FAIL on 32a96ffd (VO0-VO8b incl. Qui
  d8b0eaaa ckpt 1308: vo rerun dispatched 05:11Z run #595 id 43370382236 on 32a96ffd (review fixes
  32a96ffd ckpt 1307: CB2 review fixes coded: VpnOffRules window 3/10min + SETTLING + REFUSED_BEFO
```
