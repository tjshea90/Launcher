# CHECKPOINT 1119 — read me first, then TASKS.md

**Written:** 2026-11-11T06:33:19Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `30c1afdf` (this checkpoint is the commit after it)

## Just done
bt run #532 (id 42972187857, job 130362986310, head eae2cb4) GREEN: ctl/g04/g12/g4 core_mean 0.00 (patch covers); ctl+g04 press = 'patch not fresh, shown as it is: a window guard is running' (a SystemUI window event ~190 ms BEFORE the dialog event started the guard = BT1c mechanism on the emulator); g12/g4 fresh; latency 0.5 s after shade close ('guard lifted: the Launcher has the focus'). BT1b audit (subagent): lock patch returned at PAD hidden during keyguard exit -> padLeft hold 1.5 s + new lock window starts hidden (settle)

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. NEXT: confirm full unit suite (g2.log GRADLE_EXIT=0), commit, then tools/gate.sh 9676905 (tiered gate: dispatch the groups it prints; pt/rg solo), read-only review slices (A home-patch decision, B lock hold/settle, C settings look-alike) BEFORE the gate dispatch, then ship v0.9.5

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  eae2cb44 ckpt 1118: hp run #531 (id 42970809372, job 130358595632) GREEN on 79c74bf (0 FAIL). bt
  0565d017 ckpt 1117: run #530 (id 42970805392, job 130358583086, bt on 79c74bf): 7 FAIL (ctl, g04
  fb838402 ckpt 1116: dispatched at 06:12Z on head 79c74bf: bt run #530 (id 42970805392, main, sho
  79c74bf6 ckpt 1115: BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFr
  e8aba8f8 ckpt 1114: run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
  3c05741c ckpt 1112: BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCC
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
