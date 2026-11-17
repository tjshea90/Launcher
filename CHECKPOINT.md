# CHECKPOINT 1410 — read me first, then TASKS.md

**Written:** 2026-11-17T23:13:10Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `f14ff6d9` (this checkpoint is the commit after it)

## Just done
stray guard review A part 1 compiled+green: StrayCustomGuard.reason() codes (UNDECIDED, RESUMING, ROUTER_BUSY), retryInMs, verdict-first logText; service re-check + customHomeGone + noteOtherWindow + async bump; TjHooks null-helper line; StrayCustomGuardTest 28 green

## Do this next
ad scenario: sed for new STRAYCUSTOM format, non-vacuous AD5b, AD5a negative control (no redirect at first gesture); then full suite + lint; rerun lg + ad; then 4 gate runs; heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3e1bff85 ckpt 1409: lg #625 id 44194958128 job 134433835995 head 46153e5b: 12 PASS 0 FAIL 66 INF
  606d1157 ckpt 1408: full suite 1116 tests 0 fail + lint green (full1.log GRADLE_EXIT=0); lg reru
  f1a1fbfe ckpt 1407: reviews B+C fixed: gate text clocks first, RESTING label = really visible, p
  3c3c09d9 ckpt 1406: HANDOFF 0z (live state of v0.9.12) written; B still silent (tip 69453515), b
  9381967d ckpt 1405: lg: LG1 premise moved to the first learned-menu hold + control check LG7 (OF
  24543478 ckpt 1404: lg #624 id 44190767040 job 134419239929 (head 2702c070): 10 PASS 1 FAIL 65 I
  a3704328 ckpt 1403: lg run #624 id 44190767040 dispatched 22:30:46Z (head 2702c070, sections=lg)
  2702c070 ckpt 1402: QsMemory via helper STOCKMEM + 2h backoff: QsMemoryTest/TaskOriginTest green
  171cc637 ckpt 1401: TAKEOVER done 22:25Z (CG3b): B silent since 21:48Z (31 min), no CI in flight
  63330b2e ckpt 1398: takeover prepared (read-only): git merge-tree dry run of origin/main into my
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
