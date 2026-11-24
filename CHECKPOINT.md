# CHECKPOINT 1682 — read me first, then TASKS.md

**Written:** 2026-11-24T14:14:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/serene-lamport-wib3wy` · **builds on:** `c5f5255f` (this checkpoint is the commit after it)

## Just done
CY1: reviewer B report SAVED research/CY1_AUDIT_B_2026-11-23.md (22 ids: 5 already fixed, 3 skip, rest risky/deferred; 3 NEW provably-neutral candidates B1 reset-report commits->async, B2 lockPrepare eager invisibleReady, B3 coverAtEnter eager mayHaveWork). NOT yet verified by me

## Do this next
verify B1 B2 B3 in the code myself (TAS:4401/4417, RecentsDirty.java:19, ReturnReset.java:223/237/239/538, TAS:1900, TAS:1997, ProfileSession.java:43), then implement the ones that hold + JVM test; reviewer A still running

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  cb598bc5 ckpt 1681: CY1: manifest checked by hand = clean (largeHeap false on this tablet, no ea
  68ffa23d ckpt 1680: CY1: 14:02Z 11-24: the 3 reviewers (A,B,C) died 11-23 ~08:05Z on the WEEKLY 
  a53835a7 ckpt 1679: CY1: 3 read-only reviewers dispatched 08:01Z (A delta since v0.9.12, B open-
  5bbdf093 ckpt 1678: CY1: findings so far saved (research/CY1_STATE_2026-11-23.md): 5th perf pass
  1e4abffb ckpt 1677: CY1s: Tj narrows scope: launcher only, no Settings look-alike, no music app;
  96a1fa98 ckpt 1676: CY1: Tj asks for a speed / RAM / CPU audit (no behaviour change allowed); TA
  cf06c9bd ckpt 1675: CX1 DONE: heartbeat trig_012EemLoDGk8vy7vUJX1T3Qk DELETED 07:47Z (job shippe
  7b75a283 ckpt 1674: CX1: v0.9.19 RELEASED (android.yml run 60 id 45132390939 on 7db8930b, publis
  ae8caa05 ckpt 1673: CX1: ship.sh for v0.9.19 (code 58) PASSED at 07:36Z on head 7db8930b (== mai
  7db8930b ckpt 1672: gated v0.9.19 (code 58) and pushed it: the handoff self-test, the full unit 
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
