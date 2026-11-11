# CHECKPOINT 1147 — read me first, then TASKS.md

**Written:** 2026-11-11T09:00:13Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `ec93f66d` (this checkpoint is the commit after it)

## Just done
BT2c written in TASKS (open: #541 hp all-red, patch never rested). Rerun #542 id 42991071905 (v,ps,hp, shots=true, head a36083d2) dispatched 08:57Z

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab STAYS ARMED. Read #542 id 42991071905 at ~09:10Z: get_job_logs run_id 42991071905 failed_only tail 12 (No failed jobs AND completed = green; first check list_workflow_jobs status). Red: haiku subagent for header+FAILs+'HP B DIAG' line, then git fetch origin ci-shots + git archive into a fresh scratchpad dir (pictures 161-hp-rest, 162-hp-open). Green: tick BT2 + BT2c with rate 1 of 3 chain runs red. Then disable the heartbeat and send the answer (never before BT2c is explained or honestly listed as open)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a36083d2 ckpt 1146: BT2c: #541 (v,ps,hp) was red with 15 FAIL all in hp (patch never rested, 'aw
  c17eac6b ckpt 1145: #541 (id 42988210547, job 130414401792, v,ps,hp on 3e4d1d2): 32 PASS 15 FAIL
  dad80ef4 ckpt 1144: CLAUDE.md lessons from the v0.9.5 session added; GATE wording checked: wf/lq
  0bdaac12 ckpt 1143: HANDOFF.md block 0s (v0.9.5 state) written; #541 id 42988210547 still runnin
  78d6cb20 ckpt 1142: BT2 proof run #541 id 42988210547 (v,ps,hp on branch, head 3e4d1d2, dispatch
  3e4d1d26 ckpt 1141: BT2 fix written: HP4 in tools/emulator_scenario.sh now INFO-skips (never sil
  e316fa19 ckpt 1140: v0.9.5 RELEASED and RECORDED (Release v0.9.5 on 6172610, android.yml run 46 
  453ff402 ckpt 1139: android.yml full_build dispatched 08:26Z: Build APK run #46 id 42987042430 o
  6172610b ckpt 1138: gated v0.9.5 (code 44) and pushed it: the handoff self-test, the full unit s
  6f1d0a9d ckpt 1137: tag v0.9.4 chain #540 (id 42984659617, job 130402860592): 47 PASS 1 FAIL = H
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
