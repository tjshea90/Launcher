# CHECKPOINT 1422 — read me first, then TASKS.md

**Written:** 2026-11-18T00:12:38Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `fd046e2f` (this checkpoint is the commit after it)

## Just done
triage dispatched 00:17Z (all on product code = 1c135985 + version bump): pt head-branch, pt main, pt tag v0.9.11, base head-branch, base tag, x1,e1,b6 on main, x1,e1,b6 tag. Evidence so far: #632 BA1 failed by 0.0001 (measured 0.0054 vs generic 0.0003+0.005), PT 1 of 5 fades (tag #633 3 of 5 = bare minimum); old notes record pt BA1 flake on unchanged code (#535); #629 base FAILs = empty dumps + am crash never took (pid 5019 unchanged), no native crash in base; #630 b6 FAILs, native crash was in section t not b6

## Do this next
get run ids (list_workflow_runs emulator.yml, workflow_dispatch, perPage 8), wait ~00:35Z, read each with tail_lines small or a haiku brief; #628 job 134449883426 still running; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  fd046e2f ckpt 1421: RAW VERDICTS 00:08Z: #629 (base,y2,n,ac,bs,se,fp,rt,gear,fl) 123 PASS 4 FAIL
  688590e5 ckpt 1420: gate ended 00:02Z: #629 (run 44199579604 job 134449895997, base,y2,n,ac,bs,s
  74289af2 ckpt 1419: #631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one 
  6fcc6cbf ckpt 1418: gate #631 pt id 44199590509 job 134449933970 head 1c135985: job conclusion F
  58fd08c3 ckpt 1417: version bump only: app/build.gradle.kts versionCode 51, versionName 0.9.12 (
  08955e96 ckpt 1416: gate dispatched 23:27-23:28Z on head 1c135985 (product code = 725270ed): #62
  1c135985 ckpt 1415: VERDICT head 725270ed: ad #626 id 44197894744 job 134444040237 = 14 PASS 0 F
  45127751 ckpt 1414: context ~334K: ad #626 id 44197894744 + lg #627 id 44197899465 (head 725270e
  35ae1d99 ckpt 1413: HANDOFF 0z + answer draft updated (lg #625 result, guard re-look text); ad #
  c8fc7fe2 ckpt 1412: dispatched 23:16Z on head 725270ed: ad #626 id 44197894744 (likely), lg #627
```
