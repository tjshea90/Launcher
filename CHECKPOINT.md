# CHECKPOINT 1145 — read me first, then TASKS.md

**Written:** 2026-11-11T08:52:37Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `dad80ef4` (this checkpoint is the commit after it)

## Just done
#541 (id 42988210547, job 130414401792, v,ps,hp on 3e4d1d2): 32 PASS 15 FAIL 45 INFO, ALL 15 FAIL in hp (HP1 calibration never started, HP2, HP4 cardless, HP5-HP7, HP11, HP8, HP13): home patch status 'away: not on the plain home screen' from phase B on; no NATIVE CRASH; HP4 INFO-skip DID fire (stock fit 0.749). v,ps all PASS. Scenario diff vs 3627dbaf is HP4 block only, no product diff

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab STAYS ARMED. Triage: grep 'not on the plain home screen' in app/src to see the state; rerun hp SOLO and v,ps,hp on head side by side (refs main and branch), plus v,ps,hp on tag v0.9.4 (#540 passed HP1/HP2, only HP4 failed); do NOT answer Tj before this is explained

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  dad80ef4 ckpt 1144: CLAUDE.md lessons from the v0.9.5 session added; GATE wording checked: wf/lq
  0bdaac12 ckpt 1143: HANDOFF.md block 0s (v0.9.5 state) written; #541 id 42988210547 still runnin
  78d6cb20 ckpt 1142: BT2 proof run #541 id 42988210547 (v,ps,hp on branch, head 3e4d1d2, dispatch
  3e4d1d26 ckpt 1141: BT2 fix written: HP4 in tools/emulator_scenario.sh now INFO-skips (never sil
  e316fa19 ckpt 1140: v0.9.5 RELEASED and RECORDED (Release v0.9.5 on 6172610, android.yml run 46 
  453ff402 ckpt 1139: android.yml full_build dispatched 08:26Z: Build APK run #46 id 42987042430 o
  6172610b ckpt 1138: gated v0.9.5 (code 44) and pushed it: the handoff self-test, the full unit s
  6f1d0a9d ckpt 1137: tag v0.9.4 chain #540 (id 42984659617, job 130402860592): 47 PASS 1 FAIL = H
  0df075f1 ckpt 1136: tag chain #540 (id 42984659617, job 130402860592, v0.9.4 v,ps,hp) FAILED; FA
  4e8bb812 ckpt 1135: heartbeat 08:14Z: no other session working (other account last commit 70 min
```
