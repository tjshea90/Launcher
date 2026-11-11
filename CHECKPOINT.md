# CHECKPOINT 1117 — read me first, then TASKS.md

**Written:** 2026-11-11T06:22:02Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `fb838402` (this checkpoint is the commit after it)

## Just done
run #530 (id 42970805392, job 130358583086, bt on 79c74bf): 7 FAIL (ctl, g04/g12/g4-never, g12, g4, lat); BT0b premise says calibration/rest 'never' (probably ended BEFORE the scenario's logcat -c = test-premise flake, to confirm in shots530/bt-02-rest.png); no HOMEPRESS line in any round (patchHomeMode false = no home patch window in those presses), core_mean +118.92 every round. #531 (hp, id 42970809372) still pending

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. NEXT: (1) look at scratchpad shots530/bt-02-rest.png + bt-04-ctl-open.png (ci-shots already fetched); (2) fix bt premise in tools/emulator_scenario.sh section bt: do not hpwait on a log cleared after the event; poll 'dumpsys window windows | grep TjWallpaperPatch' or the home_patch status pref instead, print the HOMEPATCH lines of phase B; (3) read hp #531 (get_job_logs job via list_workflow_jobs, tail 70); (4) BT1b audit subagent a5f1a8192926714b3 report pending (notification); (5) then code-review slices, gate (tools/gate.sh 9676905), ship v0.9.5. Code state: ckpt 1115 + settle-clock-across-pad edit; JVM green

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  fb838402 ckpt 1116: dispatched at 06:12Z on head 79c74bf: bt run #530 (id 42970805392, main, sho
  79c74bf6 ckpt 1115: BT1c/BT1d code: HomePatchRules.openTreatment (fresh/stale/full card) + notFr
  e8aba8f8 ckpt 1114: run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
  3c05741c ckpt 1112: BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCC
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
  a409a340 ckpt 1104: BS1e SHIP: android.yml full_build=true dispatched 02:52:12Z on main@9676905 
  96769051 ckpt 1102: gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit s
```
