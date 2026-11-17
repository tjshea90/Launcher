# CHECKPOINT 1419 — read me first, then TASKS.md

**Written:** 2026-11-17T23:49:52Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `6fcc6cbf` (this checkpoint is the commit after it)

## Just done
#631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one FAIL = BC2 'largest patch-versus-surround mismatch around that close within 15%'; no native crash; TIME pt 613 s. Triage reruns dispatched 23:48Z: pt on head #632 id 44202582002 (6fcc6cbf = same code + version bump), pt on tag v0.9.11 #633 id 44202587033 (dceaa412)

## Do this next
read #632 and #633 at ~00:06Z (get FAIL lines + the PTCLOSE closetap number from each; compare). If the tag fails BC2 the same way = environment, not the diff (BC2 flaked on v0.8.8 tag too); if only head fails, run pt twice more on head and look at the lock-screen patch changes (fastMenuOpen wasResting, patchGateText). Read #628-#630 at ~00:08Z. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (next 00:03Z)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  6fcc6cbf ckpt 1418: gate #631 pt id 44199590509 job 134449933970 head 1c135985: job conclusion F
  58fd08c3 ckpt 1417: version bump only: app/build.gradle.kts versionCode 51, versionName 0.9.12 (
  08955e96 ckpt 1416: gate dispatched 23:27-23:28Z on head 1c135985 (product code = 725270ed): #62
  1c135985 ckpt 1415: VERDICT head 725270ed: ad #626 id 44197894744 job 134444040237 = 14 PASS 0 F
  45127751 ckpt 1414: context ~334K: ad #626 id 44197894744 + lg #627 id 44197899465 (head 725270e
  35ae1d99 ckpt 1413: HANDOFF 0z + answer draft updated (lg #625 result, guard re-look text); ad #
  c8fc7fe2 ckpt 1412: dispatched 23:16Z on head 725270ed: ad #626 id 44197894744 (likely), lg #627
  725270ed ckpt 1411: stray guard: ROUTER_BUSY + gave-up log line + ad AD5a/AD5b scenario edits in
  b980302b ckpt 1410: stray guard review A part 1 compiled+green: StrayCustomGuard.reason() codes 
  3e1bff85 ckpt 1409: lg #625 id 44194958128 job 134433835995 head 46153e5b: 12 PASS 0 FAIL 66 INF
```
