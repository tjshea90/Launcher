# CHECKPOINT 1372 — read me first, then TASKS.md

**Written:** 2026-11-15T22:59:39Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9028296e-kjveqx` · **builds on:** `dd414345` (this checkpoint is the commit after it)

## Just done
CE1 GATE COMPLETE on product code c0d29aae (identical at HEAD): #611 id 43766536409 x1,e1,y2,lq,an,wf GREEN; #610 id 43766533567 base,ac,rb,last PASS (its 6 FAILs were all vo test design); #612 id 43766538896 n,o,x,nq PASS + ss SS7 flake; ss solo #614 id 43769209473 GREEN; vo solo #616 id 43771494714 GREEN (all VO checks incl. word publishing, cap, Quickstep stays)

## Do this next
bash ship.sh 'v0.9.11 ...' -> android.yml full_build=true on ccr-9028296e-kjveqx -> get_release_by_tag v0.9.11 -> tools/record-release.sh -> tick TASKS CE1a-c, fill HANDOFF 0y RELEASED line -> answer Tj CE1 (research/CE1_RELEASE_MESSAGE_DRAFT_2026-11-15.md, fill proof) + CF1 (research/RESTART_NO_CABLE_2026-11-15.md)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c44673fa ckpt 1371: CE1: vo phase E rewritten (cap independent of earlier stop counts); rerun vo
  e2bc0169 ckpt 1370: CE1 vo #615 (ec0a14ed+grant): 26 PASS 2 FAIL (VO5b VO5c only: the cap check 
  98824d26 ckpt 1369: CF1 written (Tj: after all releases, review what keeps working / breaks afte
  cd382c45 ckpt 1368: CE1: #613 vo solo 24 PASS 4 FAIL (VO2d VO6b VO8a VO8c: word null = no WRITE_
  ec0a14ed ckpt 1367: CE1: vo scenario B/C rewritten for the new custom-launcher stop (log cleared
  52626f4d ckpt 1366: CE1 gate verdict final on c0d29aae: #610 86 PASS 6 FAIL (VO2b VO3b VO3c VO5 
  94a9c117 ckpt 1365: CE1 gate verdict on c0d29aae: #611 (x1,e1,y2,lq,an,wf) GREEN; #610 (base,ac,
  ed7e4836 ckpt 1364: CE1 gate on c0d29aae dispatched 21:57Z: #610 id 43766533567 base,ac,rb,vo,la
  c0d29aae ckpt 1363: CE1: Launcher unit 1054 tests 0 fail + lint ok after review fixes; dispatchi
  cdfb5bea ckpt 1362: CE1: review fixes 1,3,4,5,6,8 in VpnOff.java + ControlActivity (not yet comp
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
