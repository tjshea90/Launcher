# CHECKPOINT 1428 — read me first, then TASKS.md

**Written:** 2026-11-18T00:29:45Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `31718304` (this checkpoint is the commit after it)

## Just done
PT TALLY 00:40Z (all solo): HEAD #631 BC2 only; #632 BA1+PT (1 of 5 fades; BA1 missed by 0.0001); #634 id 44205747479 BC2 closetap 0.1671 only (PT 5 of 5, BA1 ok); #635 never ran (ffmpeg apt step failed after 4 min = known CI flake). TAG v0.9.11: #633 31/0 pass (PT 3 of 5); #636 id 44205756858 PT only (2 of 5, BA1 ok, BC2 closetap 0.0493). pt fails on the released tag too, every failing check is a hairline metric and differs run to run. Re-dispatched ~00:40Z pt@main, pt@branch, pt@tag (3 more samples, ids not read). Other results: o@head #641 + o@tag #642 green; e1@branch, x1,e1@main, e1@tag dispatched ~00:35Z; #643 id 44206647208 (x,ad,ae,pa,uf) and #644 id 44206651719 (cs2,hp,cu,last) running

## Do this next
list ids of e1 + pt runs (list_workflow_runs emulator.yml workflow_dispatch perPage 6) and probe with failed_only (No failed jobs = green); when #643/#644 end read FAIL lines with ONE single-job haiku brief each (echo the job id); then decide on release; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  31718304 ckpt 1427: PROBES 00:36Z (failed_only): pt@branch #634 id 44205747479 job 134471094065 
  6085afa9 ckpt 1426: VERIFIED 00:30Z (cheap probe 'No failed jobs'): base@head #637 green 29/0 (b
  46099daf ckpt 1425: ids of the 00:17Z dispatches: o@branch #641 id 44206636873; o@tag #642 id 44
  e3467532 ckpt 1424: #628 (run1 n6,o,x,ad,ae,pa,uf,cs2,hp,cu,last) CUT at the 2700s step limit (e
  839cc5e8 ckpt 1423: triage runs (dispatched 00:11Z): pt@branch #634 id 44205747479; pt@main #635
  b911a52b ckpt 1422: triage dispatched 00:17Z (all on product code = 1c135985 + version bump): pt
  fd046e2f ckpt 1421: RAW VERDICTS 00:08Z: #629 (base,y2,n,ac,bs,se,fp,rt,gear,fl) 123 PASS 4 FAIL
  688590e5 ckpt 1420: gate ended 00:02Z: #629 (run 44199579604 job 134449895997, base,y2,n,ac,bs,s
  74289af2 ckpt 1419: #631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one 
  6fcc6cbf ckpt 1418: gate #631 pt id 44199590509 job 134449933970 head 1c135985: job conclusion F
```
