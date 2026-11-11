# CHECKPOINT 1146 — read me first, then TASKS.md

**Written:** 2026-11-11T08:56:45Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `73069441` (this checkpoint is the commit after it)

## Just done
BT2c: #541 (v,ps,hp) was red with 15 FAIL all in hp (patch never rested, 'away: not on the plain home screen'); added HP B DIAG info line (who is in front, focus, pid, service log tail) to emulator_scenario.sh; bash -n + test_handoff 48/0. Dispatching v,ps,hp on the branch with shots=true

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab STAYS ARMED. Find the new run (list_workflow_runs resource_id emulator.yml branch ccr-3e66a8c0-ruc8aa event workflow_dispatch perPage 1), read at +14 min: header + FAILs (subagent a082feb7763c08dc0 / new haiku with get_job_logs tail 170). PASS = HP4 INFO-skip proven, #541 = flake (write BT2c). FAIL again = read the HP B DIAG line + fetch ci-shots (git archive) pictures 161-hp-rest/162-hp-open. Do NOT answer Tj before this is explained

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c17eac6b ckpt 1145: #541 (id 42988210547, job 130414401792, v,ps,hp on 3e4d1d2): 32 PASS 15 FAIL
  dad80ef4 ckpt 1144: CLAUDE.md lessons from the v0.9.5 session added; GATE wording checked: wf/lq
  0bdaac12 ckpt 1143: HANDOFF.md block 0s (v0.9.5 state) written; #541 id 42988210547 still runnin
  78d6cb20 ckpt 1142: BT2 proof run #541 id 42988210547 (v,ps,hp on branch, head 3e4d1d2, dispatch
  3e4d1d26 ckpt 1141: BT2 fix written: HP4 in tools/emulator_scenario.sh now INFO-skips (never sil
  e316fa19 ckpt 1140: v0.9.5 RELEASED and RECORDED (Release v0.9.5 on 6172610, android.yml run 46 
  453ff402 ckpt 1139: android.yml full_build dispatched 08:26Z: Build APK run #46 id 42987042430 o
  6172610b ckpt 1138: gated v0.9.5 (code 44) and pushed it: the handoff self-test, the full unit s
  6f1d0a9d ckpt 1137: tag v0.9.4 chain #540 (id 42984659617, job 130402860592): 47 PASS 1 FAIL = H
  0df075f1 ckpt 1136: tag chain #540 (id 42984659617, job 130402860592, v0.9.4 v,ps,hp) FAILED; FA
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
