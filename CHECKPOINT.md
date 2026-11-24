# CHECKPOINT 1689 — read me first, then TASKS.md

**Written:** 2026-11-24T14:48:19Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/serene-lamport-wib3wy` · **builds on:** `a8f4eee0` (this checkpoint is the commit after it)

## Just done
CY1c GATE #794 (id 45413130411, job 138512022462, head 0847c754, base,b4,o,cs): 54 PASS 4 FAIL 31 INFO. FAIL: B4 cover shown+removed; B4 Recents untouched skips Recents step; B4 Calendar in front after shorter reset; CS-B2 (BE2) Recents completely cleared. INFO: NATIVE CRASH #1 (SIGSEGV libandroid_runtime DisplayListCanvas reset, jemalloc-type flake); CS-B: RESET recents: no Clear all button and no card with Close found; TIME base 238 b4 29 o 169 cs 150

## Do this next
TRIAGE (B1 touches exactly this reset path, so NOT assumed flake): rerun sections base,b4,cs SOLO on tag v0.9.19 (ref v0.9.19) AND on head in parallel (different refs: tag vs branch); read which B4/CS-B2 checks fail on each; also read lock-log INFO timing of B4 (when was the NATIVE CRASH relative to B4). Do NOT bump version / ship until triaged. Heartbeat trig_01DkASxm8hLzwAL447MmJQnA

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  871f76a2 ckpt 1688: CY1d: answer-to-Tj DRAFT saved research/CY1_RELEASE_MESSAGE_DRAFT_2026-11-24
  fe4528c6 ckpt 1687: CY1: TASKS boxes updated (CY1b done, CY1c batch 1 status); gate #794 still r
  4a058941 ckpt 1686: CY1c: gate run id 45413130411 (#794, emulator.yml base,b4,o,cs, head 0847c75
  0ecbff31 ckpt 1685: CY1c: emulator gate DISPATCHED 14:22Z on head 0847c754 (product diff vs v0.9
  0847c754 ckpt 1684: CY1c batch 1 CODED (not yet tested/released): B1 reset-report writes + Recen
  c8cadcaa ckpt 1683: CY1: reviewer A report SAVED research/CY1_AUDIT_A_2026-11-23.md: new code si
  27dfd2ec ckpt 1682: CY1: reviewer B report SAVED research/CY1_AUDIT_B_2026-11-23.md (22 ids: 5 a
  cb598bc5 ckpt 1681: CY1: manifest checked by hand = clean (largeHeap false on this tablet, no ea
  68ffa23d ckpt 1680: CY1: 14:02Z 11-24: the 3 reviewers (A,B,C) died 11-23 ~08:05Z on the WEEKLY 
  a53835a7 ckpt 1679: CY1: 3 read-only reviewers dispatched 08:01Z (A delta since v0.9.12, B open-
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
