# CHECKPOINT 1121 — read me first, then TASKS.md

**Written:** 2026-11-11T06:38:24Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `62055754` (this checkpoint is the commit after it)

## Just done
BT1b boxed [~], BT1 findings written to research/BT1_ANALYSIS; review slices A/B/C running (read-only subagents)

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. Read the 3 review reports (A home patch, B lock hold, C label/keyboard), fix valid findings in ONE batch + JVM test (bash tools/gradle.sh :app:testDebugUnitTest), ckpt, then dispatch the 3 gate runs from bash tools/gate.sh 9676905 on branch claude/dazzling-knuth-f1khhb (run 3 pt SOLO); ckpt the raw verdict BEFORE analysing

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  9a8b660d ckpt 1120: fix compile error (comment swallowed addPatch tail); unit suite green; BT1b:
  c0c1840a ckpt 1119: bt run #532 (id 42972187857, job 130362986310, head eae2cb4) GREEN: ctl/g04/
  eae2cb44 ckpt 1118: hp run #531 (id 42970809372, job 130358595632) GREEN on 79c74bf (0 FAIL). bt
  0565d017 ckpt 1117: run #530 (id 42970805392, job 130358583086, bt on 79c74bf): 7 FAIL (ctl, g04
  fb838402 ckpt 1116: dispatched at 06:12Z on head 79c74bf: bt run #530 (id 42970805392, main, sho
  79c74bf6 ckpt 1115: BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFr
  e8aba8f8 ckpt 1114: run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
  3c05741c ckpt 1112: BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCC
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
