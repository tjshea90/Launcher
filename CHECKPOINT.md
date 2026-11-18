# CHECKPOINT 1424 — read me first, then TASKS.md

**Written:** 2026-11-18T00:20:21Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `17f8e826` (this checkpoint is the commit after it)

## Just done
#628 (run1 n6,o,x,ad,ae,pa,uf,cs2,hp,cu,last) CUT at the 2700s step limit (exit 124): 19 PASS 2 FAIL (O4 AT LOCK force-stopped missing; O3 Calendar in front 1.5s after Enter), last output 23:44:10, hang inside section o; x,ad,ae,pa,uf,cs2,hp,cu,last NEVER RAN; no native crash. Dispatched ~00:17Z: o@branch, o@tag v0.9.11, x,ad,ae,pa,uf@main, cs2,hp,cu,last@main (ids not read yet)

## Do this next
get ids with list_workflow_runs perPage 4; read o@branch vs o@tag first (O3/O4 real or hang flake?), then the two remainder runs; earlier 7 triage runs #634-#640 (ids in ckpt 1423) due 00:22-00:29Z; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  839cc5e8 ckpt 1423: triage runs (dispatched 00:11Z): pt@branch #634 id 44205747479; pt@main #635
  b911a52b ckpt 1422: triage dispatched 00:17Z (all on product code = 1c135985 + version bump): pt
  fd046e2f ckpt 1421: RAW VERDICTS 00:08Z: #629 (base,y2,n,ac,bs,se,fp,rt,gear,fl) 123 PASS 4 FAIL
  688590e5 ckpt 1420: gate ended 00:02Z: #629 (run 44199579604 job 134449895997, base,y2,n,ac,bs,s
  74289af2 ckpt 1419: #631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one 
  6fcc6cbf ckpt 1418: gate #631 pt id 44199590509 job 134449933970 head 1c135985: job conclusion F
  58fd08c3 ckpt 1417: version bump only: app/build.gradle.kts versionCode 51, versionName 0.9.12 (
  08955e96 ckpt 1416: gate dispatched 23:27-23:28Z on head 1c135985 (product code = 725270ed): #62
  1c135985 ckpt 1415: VERDICT head 725270ed: ad #626 id 44197894744 job 134444040237 = 14 PASS 0 F
  45127751 ckpt 1414: context ~334K: ad #626 id 44197894744 + lg #627 id 44197899465 (head 725270e
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
