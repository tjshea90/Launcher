# CHECKPOINT 1564 — read me first, then TASKS.md

**Written:** 2026-11-22T07:03:44Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-bf8eda7f-mrlcxc` · **builds on:** `60f9caf4` (this checkpoint is the commit after it)

## Just done
CV1: #726 id 44994708532 (unified,base,n,o,ac,e1,x1,rb,last, switch ON) is RED: seen FAIL O4 x2, O3, AC3 x2 (more above, header via subagent); INFO breaker diag ok, no NATIVE CRASH seen in tail

## Do this next
get exact FAIL list from subagent af40954d; compare with tag v0.9.16 and with switch OFF (o,ac solo on head with/without unified); read #722-725 ~07:12 (ids 44994540888 44994543595 44994546265 44994548745); heartbeat trig_01NxLAL2jZccAiP8UPYM1X7m

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a5f2454d ckpt 1563: CV1: TASKS CV1h (tablet checks for Tj) written
  1ca89bee ckpt 1562: CV1: HANDOFF block 0zd written (gate in flight); no product change since a53
  d237db2e ckpt 1561: CV1: release answer draft saved (research/CV1_RELEASE_MESSAGE_DRAFT_2026-11-
  f72c1be9 ckpt 1560: CV1: 5th gate run #726 id 44994708532 {unified,base,n,o,ac,e1,x1,rb,last} = 
  5fb1ba74 ckpt 1559: CV1: GATE dispatched on main@a5396e53 at 06:37: #722 id 44994540888 {n,x,bs,
  a5396e53 ckpt 1558: CV1: run #721 id 44993809638 (ul,ul2, main@d4f15e86) GREEN: real seed path p
  82c0e28a ckpt 1557: CV1: dispatched emulator #721 id 44993809638 (ul,ul2, main@d4f15e86, real se
  d4f15e86 ckpt 1556: CV1: scenario ul reworked (drawer retry, python3 sqlite3 injection of real c
  9da41d62 ckpt 1555: CV1: run #720 id 44992764510 (ul,ul2, main@87bfc562) = 26 PASS 1 FAIL 16 INF
  5358e822 ckpt 1554: CV1: dispatched emulator #720 id 44992764510 (ul,ul2, main@87bfc562, review-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
