# CHECKPOINT 1112 — read me first, then TASKS.md

**Written:** 2026-11-11T05:49:38Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `5466ee9d` (this checkpoint is the commit after it)

## Just done
BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCCESSFUL). Cause of BT1f proven from AOSP10 ActivityManagerWrapper.getBadgedContentDescription (appLabel+' '+activityLabel unless equal)

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. NEXT: write emulator section 'bt' in tools/emulator_scenario.sh (+ tools/gate-map.txt line; helpers hpkey/hplog/hpwait/hpopen at scenario lines 118-129): BT1f = open look-alike, KEYCODE_APP_SWITCH, uiautomator dump, card text must equal Settings; BT1c repro = custom mode home_patch true, wait 'PATCH rest on', cmd statusbar expand-notifications, sleep 1.5, collapse, hold power at +0.3/+1/+2.5 s, shot + PATCH log (code facts: TAS homeRestable ~line 645 needs fresh composite + now>=homeForeignUntilMs + Launcher.hasWindowFocus; card path covers only 2 entries so a not-ready patch leaves the Screenshot entry visible)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
  a409a340 ckpt 1104: BS1e SHIP: android.yml full_build=true dispatched 02:52:12Z on main@9676905 
  96769051 ckpt 1102: gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit s
  b64b0e1a ckpt 1100: BS2d GATE COMPLETE: #527 (id 42947197401, job 130282725555, ss solo) step co
  471266e9 ckpt 1099: BS2d GATE reruns dispatched 02:36Z on main@ccd1024 (app diff vs gated b44f03
  ccd1024c ckpt 1098: BS2d GATE raw: #524 (id 42942929350, job 130268804149, base,n6,o,x,uf,cs2,an
  67c16d80 ckpt 1095: BS2d GATE: #526 (id 42942938730, job 130268830144, rg solo, main@b44f033): 7
  f550342c ckpt 1091: BS2d GATE dispatched 02:00Z on main@b44f033 (app diff vs gated wf head 9528d
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
