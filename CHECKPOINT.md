# CHECKPOINT 1427 — read me first, then TASKS.md

**Written:** 2026-11-18T00:28:01Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `6085afa9` (this checkpoint is the commit after it)

## Just done
PROBES 00:36Z (failed_only): pt@branch #634 id 44205747479 job 134471094065 = FAILURE; pt@main #635 id 44205751621 job 134471108478 = FAILURE with a SHORT log (ended 00:17:21Z, ~6 min after dispatch: probably infra, being read); pt@TAG #636 id 44205756858 job 134471125874 = FAILURE (so pt also fails on the released tag: noise, not this diff); o@branch #641 and o@tag #642 = both GREEN (O3/O4 + the hang of #628 did not reproduce). Dispatched e1@branch, x1,e1@main, e1@tag (~00:35Z, ids not read yet). Haiku readers launched for #634 #636 #635 (single-job briefs)

## Do this next
read the 3 reader answers (FAIL names per pt run; was #635 infra?), then list ids of the e1 runs (perPage 3) and probe them with failed_only; then #643 44206647208 / #644 44206651719; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6085afa9 ckpt 1426: VERIFIED 00:30Z (cheap probe 'No failed jobs'): base@head #637 green 29/0 (b
  46099daf ckpt 1425: ids of the 00:17Z dispatches: o@branch #641 id 44206636873; o@tag #642 id 44
  e3467532 ckpt 1424: #628 (run1 n6,o,x,ad,ae,pa,uf,cs2,hp,cu,last) CUT at the 2700s step limit (e
  839cc5e8 ckpt 1423: triage runs (dispatched 00:11Z): pt@branch #634 id 44205747479; pt@main #635
  b911a52b ckpt 1422: triage dispatched 00:17Z (all on product code = 1c135985 + version bump): pt
  fd046e2f ckpt 1421: RAW VERDICTS 00:08Z: #629 (base,y2,n,ac,bs,se,fp,rt,gear,fl) 123 PASS 4 FAIL
  688590e5 ckpt 1420: gate ended 00:02Z: #629 (run 44199579604 job 134449895997, base,y2,n,ac,bs,s
  74289af2 ckpt 1419: #631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one 
  6fcc6cbf ckpt 1418: gate #631 pt id 44199590509 job 134449933970 head 1c135985: job conclusion F
  58fd08c3 ckpt 1417: version bump only: app/build.gradle.kts versionCode 51, versionName 0.9.12 (
```
