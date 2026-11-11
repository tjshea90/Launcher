# CHECKPOINT 1115 — read me first, then TASKS.md

**Written:** 2026-11-11T06:12:22Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `59bb9e6a` (this checkpoint is the commit after it)

## Just done
BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFreshWhy + guardEndsWithFocus; TAS homeOpenDecide (stale patch adopted + rebuilt in place, else cardArea=full panel), HOMEPRESS log line per press, guard lifted on focus regain; BT1a code: PatchRest.settled (lock patch rests visibly only 1.2 s after it became restable) + onOutsideTouch records finger even when not resting; JVM tests green; bt section rewritten (never/patch/latency checks)

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. NEXT: dispatch emulator.yml sections=bt shots=true on main, and sections=hp (regression of card/patch geometry checks); read RESULTS (get_job_logs tail ~70); an audit subagent (id a5f1a8192926714b3) is reading unlock-time patch/card paths for BT1b; its report arrives as a notification

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e8aba8f8 ckpt 1114: run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
  3c05741c ckpt 1112: BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCC
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
  a409a340 ckpt 1104: BS1e SHIP: android.yml full_build=true dispatched 02:52:12Z on main@9676905 
  96769051 ckpt 1102: gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit s
  b64b0e1a ckpt 1100: BS2d GATE COMPLETE: #527 (id 42947197401, job 130282725555, ss solo) step co
  471266e9 ckpt 1099: BS2d GATE reruns dispatched 02:36Z on main@ccd1024 (app diff vs gated b44f03
```

(7 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
