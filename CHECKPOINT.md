# CHECKPOINT 1421 — read me first, then TASKS.md

**Written:** 2026-11-18T00:08:12Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `688590e5` (this checkpoint is the commit after it)

## Just done
RAW VERDICTS 00:08Z: #629 (base,y2,n,ac,bs,se,fp,rt,gear,fl) 123 PASS 4 FAIL: crash breaker switched to quickstep; after the breaker Home shows stock; AR1 grouped header; AR1 Restore everything reachable; 2 NATIVE CRASH (GrallocUploadTh jemalloc/EGL). #630 (x1,e1,b6,t,v,bl,ps,an,nq,rb,ss) 183 PASS 3 FAIL: B6 cover over pencil; B6/D4 cover hidden when panel closes; BN4 cover hidden within 600ms; 1 NATIVE CRASH (main thread jemalloc in ART, after TIME b6). #632 pt head 6fcc6cbf 29 PASS 2 FAIL: BA1 (patch not further from surround than generic+0.5%) + PT (fade within 8% in >=3 of 5 variants); no crash. #633 pt tag v0.9.11 31 PASS 0 FAIL. #631 pt head: BC2 only

## Do this next
triage: grep saved #629 log for breaker diag + crash order; run b6 solo on head and tag; pt: more samples head vs tag (read PT INFO of #632 first); #628 job still to read; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  688590e5 ckpt 1420: gate ended 00:02Z: #629 (run 44199579604 job 134449895997, base,y2,n,ac,bs,s
  74289af2 ckpt 1419: #631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one 
  6fcc6cbf ckpt 1418: gate #631 pt id 44199590509 job 134449933970 head 1c135985: job conclusion F
  58fd08c3 ckpt 1417: version bump only: app/build.gradle.kts versionCode 51, versionName 0.9.12 (
  08955e96 ckpt 1416: gate dispatched 23:27-23:28Z on head 1c135985 (product code = 725270ed): #62
  1c135985 ckpt 1415: VERDICT head 725270ed: ad #626 id 44197894744 job 134444040237 = 14 PASS 0 F
  45127751 ckpt 1414: context ~334K: ad #626 id 44197894744 + lg #627 id 44197899465 (head 725270e
  35ae1d99 ckpt 1413: HANDOFF 0z + answer draft updated (lg #625 result, guard re-look text); ad #
  c8fc7fe2 ckpt 1412: dispatched 23:16Z on head 725270ed: ad #626 id 44197894744 (likely), lg #627
  725270ed ckpt 1411: stray guard: ROUTER_BUSY + gave-up log line + ad AD5a/AD5b scenario edits in
```
