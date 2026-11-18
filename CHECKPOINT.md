# CHECKPOINT 1446 — read me first, then TASKS.md

**Written:** 2026-11-18T19:43:50Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-61fe5a34-fw0jax` · **builds on:** `57e21ce8` (this checkpoint is the commit after it)

## Just done
CH1b LinkMeter half DONE+pushed (b27a212: bridge, identity pin, job, PocketRulesTest 35580 checks, release build ok); CH1e keyboard choice code+tests written (KeyboardPolicy/Switch/Control/ReportState), not yet run

## Do this next
1) run: bash tools/gradle.sh :app:testDebugUnitTest --tests com.tj.launcher.KeyboardPolicyTest (GRADLE_EXIT in log). 2) write Launcher PocketRules(pure)+test, PocketReturn glue, LinkMeterBridge client, plumb returnToCustom(why,pocket)->ReturnReset.run(c,pocket) (skip Wi-Fi in radiosOff, widen invisibleTargets to non-system launchable apps minus kept LinkMeter/Gboard/Swoop), BackToCustomActivity+ReturnTile via extra, lock prep delays 0/250 ms when on, Control toggle, ReportState keys. Design: research/CH1_POCKET_RETURN_DESIGN_2026-11-18.md

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a4335781 ckpt 1445: CH1b LinkMeter half in progress (3 files written, wip pushed on LinkMeter br
  f0e9e60c ckpt 1444: CH1a design note written (research/CH1_POCKET_RETURN_DESIGN_2026-11-18.md)
  c4b05681 ckpt 1443: CH1 written into TASKS (pocket return + Quickstep keyboard choice)
  710d7019 ckpt 1442: 01:27 Z: heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ DISABLED (update_trigger en
  31a00b1a ckpt 1441: 01:12 Z: records done after the release: TASKS CG1a-d,f,g + CG3a-d + CG3 tic
  0ec228c9 ckpt 1440: 01:09 Z: v0.9.12 RELEASED and RECORDED: https://github.com/tjshea90/Launcher
  da30c920 ckpt 1439: 01:05 Z: BUILD DISPATCHED: android.yml full_build=true #53 id 44213092334 on
  e3312567 ckpt 1438: 01:04 Z: GATE COMPLETE on code 1c135985(+version bump): chain cs2,hp,cu,last
  ae412f3c ckpt 1437: 00:56 Z: claims in the release draft checked against code: unit tests 1130/0
  c28cd895 ckpt 1436: 00:54 Z: CU5 triage: cu solo GREEN on head x2 (#652 branch+shots id 44210843
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
