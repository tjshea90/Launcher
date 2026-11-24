# CHECKPOINT 1684 — read me first, then TASKS.md

**Written:** 2026-11-24T14:20:07Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/serene-lamport-wib3wy` · **builds on:** `266ac4db` (this checkpoint is the commit after it)

## Just done
CY1c batch 1 CODED (not yet tested/released): B1 reset-report writes + RecentsDirty.clear -> async (TAS finishReset/appendReport, ReturnReset 237/239/538, RecentsDirty), B2 lockPrepare guard !customAtLock before invisibleReady, B3 coverAtEnter plainUnlockEndsQuickstep first; new exhaustive test in ProfileSessionTest; both reviewer reports saved

## Do this next
read /tmp.../scratchpad/cy1_unit1.log for GRADLE_EXIT= (targeted ProfileSessionTest RecentsDirtyTest ReportStateTest); if green: /code-review high on git diff v0.9.19 HEAD -- app, then bash ship.sh (full suite+lint+handoff), bash tools/gate.sh v0.9.19 (expect base,ac,cs,ul-ish), release v0.9.20 only if gate is green; if the session died: git diff v0.9.19 HEAD -- app is the whole change. Heartbeat trig_01DkASxm8hLzwAL447MmJQnA

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c8cadcaa ckpt 1683: CY1: reviewer A report SAVED research/CY1_AUDIT_A_2026-11-23.md: new code si
  27dfd2ec ckpt 1682: CY1: reviewer B report SAVED research/CY1_AUDIT_B_2026-11-23.md (22 ids: 5 a
  cb598bc5 ckpt 1681: CY1: manifest checked by hand = clean (largeHeap false on this tablet, no ea
  68ffa23d ckpt 1680: CY1: 14:02Z 11-24: the 3 reviewers (A,B,C) died 11-23 ~08:05Z on the WEEKLY 
  a53835a7 ckpt 1679: CY1: 3 read-only reviewers dispatched 08:01Z (A delta since v0.9.12, B open-
  5bbdf093 ckpt 1678: CY1: findings so far saved (research/CY1_STATE_2026-11-23.md): 5th perf pass
  1e4abffb ckpt 1677: CY1s: Tj narrows scope: launcher only, no Settings look-alike, no music app;
  96a1fa98 ckpt 1676: CY1: Tj asks for a speed / RAM / CPU audit (no behaviour change allowed); TA
  cf06c9bd ckpt 1675: CX1 DONE: heartbeat trig_012EemLoDGk8vy7vUJX1T3Qk DELETED 07:47Z (job shippe
  7b75a283 ckpt 1674: CX1: v0.9.19 RELEASED (android.yml run 60 id 45132390939 on 7db8930b, publis
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
