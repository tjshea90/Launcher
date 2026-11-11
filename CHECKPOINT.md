# CHECKPOINT 1123 — read me first, then TASKS.md

**Written:** 2026-11-11T06:58:23Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `2b793e68` (this checkpoint is the commit after it)

## Just done
review B (HIGH fixed): lock patch return = one level-triggered timer (PatchRest.returnInMs, patchShow posts settleRun while restable+invisible, padLeft padBack not before hold end, padLeft lock-only); PatchRestTest 34 green; new emulator step AE3-short in ae; A still running

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. Wait for review A (aeaeedd027fb69d06), append to research/BT1_REVIEW_2026-11-11.md, fix valid findings in ONE batch + JVM tests, run FULL unit suite (bash tools/gradle.sh :app:testDebugUnitTest, check GRADLE_EXIT), ckpt, then dispatch the 3 gate runs of bash tools/gate.sh 9676905 on the branch (pt SOLO); run 2 must include ae (new step AE3-short) and bt; ckpt the raw verdict before analysing

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  cef15801 ckpt 1122: review C done (7 LOW): fixed #1 #2 (service label + AppLabelTest 3 tests gre
  323f69fd ckpt 1121: BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices
  9a8b660d ckpt 1120: fix compile error (comment swallowed addPatch tail); unit suite green; BT1b:
  c0c1840a ckpt 1119: bt run #532 (id 42972187857, job 130362986310, head eae2cb4) GREEN: ctl/g04/
  eae2cb44 ckpt 1118: hp run #531 (id 42970809372, job 130358595632) GREEN on 79c74bf (0 FAIL). bt
  0565d017 ckpt 1117: run #530 (id 42970805392, job 130358583086, bt on 79c74bf): 7 FAIL (ctl, g04
  fb838402 ckpt 1116: dispatched at 06:12Z on head 79c74bf: bt run #530 (id 42970805392, main, sho
  79c74bf6 ckpt 1115: BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFr
  e8aba8f8 ckpt 1114: run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
