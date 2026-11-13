# CHECKPOINT 1314 — read me first, then TASKS.md

**Written:** 2026-11-13T05:59:34Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `7391d7b4` (this checkpoint is the commit after it)

## Just done
GATE GREEN: solo reruns #599 b6 head GREEN, #600 b6 TAG GREEN, #601 43374384544 ac head GREEN, #602 ac TAG GREEN (chain flakes; product code = gated 7d98cc10)

## Do this next
bump app/build.gradle.kts versionCode 49 / 0.9.10; bash ship.sh in background (SHIP_EXIT marker); android.yml full_build=true ref main; heartbeat trig_01MRTg3yGjbXMnzqNtcrbLWg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e7f4655b ckpt 1313: gate FAILs: #597 AC8 (custom home still prepared behind lock) in e1,x,ac,an;
  8f2fa31e ckpt 1312: gate read 05:47Z: #596 43371490476 base,n,o,wf GREEN; #597 job 131708055674 
  7b37b884 ckpt 1311: full unit suite 1051 tests 0 fail + lint green on the gated product code
  969155e7 ckpt 1310: GATE dispatched 05:21Z on 7d98cc10: #596 43371490476 base,n,o,wf; #597 43371
  7d98cc10 ckpt 1309: run #595 43370382236 vo GREEN 24 PASS 0 FAIL on 32a96ffd (VO0-VO8b incl. Qui
  d8b0eaaa ckpt 1308: vo rerun dispatched 05:11Z run #595 id 43370382236 on 32a96ffd (review fixes
  32a96ffd ckpt 1307: CB2 review fixes coded: VpnOffRules window 3/10min + SETTLING + REFUSED_BEFO
  cb4a81fe ckpt 1306: review of CB2 (subagent): 11 findings: MED1 cap reset only at USER_PRESENT; 
  862ff1ad ckpt 1305: run #594 43367976336 vo: 16 PASS 5 FAIL (VO0 VO1 VO2 VO5b VO6) = the PROBE: 
  f3f19593 ckpt 1304: CB2e (Tj: 'And any time I lock the screen from quickstep') in TASKS; rule ha
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
