# CHECKPOINT 1116 — read me first, then TASKS.md

**Written:** 2026-11-11T06:14:28Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `4ab597fb` (this checkpoint is the commit after it)

## Just done
dispatched at 06:12Z on head 79c74bf: bt run #530 (id 42970805392, main, shots=true) and hp run #531 (id 42970809372, branch claude/dazzling-knuth-f1khhb); also lock settle now keeps the clock across a PIN pad (uncommitted-then-pushed)

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. NEXT: read #530 (job id via list_workflow_jobs; get_job_logs tail 70) and #531; fetch ci-shots (git archive into scratchpad) right after #530 ends; then BT1b from the audit subagent a5f1a8192926714b3 notification; then /code-review high vs base 9676905, gate (tools/gate.sh 9676905), ship v0.9.5

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  79c74bf6 ckpt 1115: BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFr
  e8aba8f8 ckpt 1114: run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
  3c05741c ckpt 1112: BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCC
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
  a409a340 ckpt 1104: BS1e SHIP: android.yml full_build=true dispatched 02:52:12Z on main@9676905 
  96769051 ckpt 1102: gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit s
  b64b0e1a ckpt 1100: BS2d GATE COMPLETE: #527 (id 42947197401, job 130282725555, ss solo) step co
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
