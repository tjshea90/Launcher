# CHECKPOINT 1148 — read me first, then TASKS.md

**Written:** 2026-11-11T09:10:42Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `a82b0b85` (this checkpoint is the commit after it)

## Just done
#542 (id 42991071905, job 130423824216, v,ps,hp on a36083d2, shots=true) job SUCCESS: scenario step green 08:58-09:07. So #541 (15 FAIL in hp, patch never rested) was an intermittent flake of this chain; header/INFO lines to be read next

## Do this next
Heartbeat trig_01GVcBRvTAzqf97UhMTJszab STAYS ARMED until the answer is sent. Next: read #542 RESULTS header + INFO 'HP4: NOT CHECKED' via get_job_logs job_id 130423824216 tail 70; fetch ci-shots (git fetch origin ci-shots; git archive into a fresh scratchpad dir) and LOOK at 161-hp-rest/162-hp-open once; tick BT2 [x] and BT2c as flake-with-rate (1 red of 4 chain runs on this code: #533 #539 HP4 only, #541 hp all-red, #542 green); then disable heartbeat, CLAUDE.md/HANDOFF touch, send the answer

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a82b0b85 ckpt 1147: BT2c written in TASKS (open: #541 hp all-red, patch never rested). Rerun #54
  a36083d2 ckpt 1146: BT2c: #541 (v,ps,hp) was red with 15 FAIL all in hp (patch never rested, 'aw
  c17eac6b ckpt 1145: #541 (id 42988210547, job 130414401792, v,ps,hp on 3e4d1d2): 32 PASS 15 FAIL
  dad80ef4 ckpt 1144: CLAUDE.md lessons from the v0.9.5 session added; GATE wording checked: wf/lq
  0bdaac12 ckpt 1143: HANDOFF.md block 0s (v0.9.5 state) written; #541 id 42988210547 still runnin
  78d6cb20 ckpt 1142: BT2 proof run #541 id 42988210547 (v,ps,hp on branch, head 3e4d1d2, dispatch
  3e4d1d26 ckpt 1141: BT2 fix written: HP4 in tools/emulator_scenario.sh now INFO-skips (never sil
  e316fa19 ckpt 1140: v0.9.5 RELEASED and RECORDED (Release v0.9.5 on 6172610, android.yml run 46 
  453ff402 ckpt 1139: android.yml full_build dispatched 08:26Z: Build APK run #46 id 42987042430 o
  6172610b ckpt 1138: gated v0.9.5 (code 44) and pushed it: the handoff self-test, the full unit s
```
