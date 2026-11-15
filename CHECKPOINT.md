# CHECKPOINT 1374 — read me first, then TASKS.md

**Written:** 2026-11-15T23:03:42Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9028296e-kjveqx` · **builds on:** `858ec985` (this checkpoint is the commit after it)

## Just done
gated v0.9.11 (code 50) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.11 "v0.9.11: no VPN on the lock screen, with the screen off or in the custom launcher; Fast Wi-Fi of LinkMeter pauses and comes back by itself in Quickstep (CE1)"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  858ec985 ckpt 1373: pre-ship: v0.9.11: no VPN on the lock screen, with the screen off or in the 
  5c176218 ckpt 1372: CE1 GATE COMPLETE on product code c0d29aae (identical at HEAD): #611 id 4376
  c44673fa ckpt 1371: CE1: vo phase E rewritten (cap independent of earlier stop counts); rerun vo
  e2bc0169 ckpt 1370: CE1 vo #615 (ec0a14ed+grant): 26 PASS 2 FAIL (VO5b VO5c only: the cap check 
  98824d26 ckpt 1369: CF1 written (Tj: after all releases, review what keeps working / breaks afte
  cd382c45 ckpt 1368: CE1: #613 vo solo 24 PASS 4 FAIL (VO2d VO6b VO8a VO8c: word null = no WRITE_
  ec0a14ed ckpt 1367: CE1: vo scenario B/C rewritten for the new custom-launcher stop (log cleared
  52626f4d ckpt 1366: CE1 gate verdict final on c0d29aae: #610 86 PASS 6 FAIL (VO2b VO3b VO3c VO5 
  94a9c117 ckpt 1365: CE1 gate verdict on c0d29aae: #611 (x1,e1,y2,lq,an,wf) GREEN; #610 (base,ac,
  ed7e4836 ckpt 1364: CE1 gate on c0d29aae dispatched 21:57Z: #610 id 43766533567 base,ac,rb,vo,la
```
