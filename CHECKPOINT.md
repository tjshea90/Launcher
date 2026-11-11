# CHECKPOINT 1111 — read me first, then TASKS.md

**Written:** 2026-11-11T05:43:11Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `d201f5d1` (this checkpoint is the commit after it)

## Just done
BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = BN2 family (not-ready patch => Screenshot entry visible, short card covers only 2 entries); BT1f cause = AOSP joins appLabel+activityLabel; BT1e = enabledKeyboards joins all IMEs

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. (1) fix BT1e in SsSystemInfo.enabledKeyboards (skip swiftkey pkg, pure rule + JVM test) (2) confirm BT1f AOSP source with web-fetch agent (Go recents label source) then choose fix (3) read TAS home-patch readiness code ~590-840 + HomePatchRules, then emulator repro of shade case

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
  a409a340 ckpt 1104: BS1e SHIP: android.yml full_build=true dispatched 02:52:12Z on main@9676905 
  96769051 ckpt 1102: gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit s
  b64b0e1a ckpt 1100: BS2d GATE COMPLETE: #527 (id 42947197401, job 130282725555, ss solo) step co
  471266e9 ckpt 1099: BS2d GATE reruns dispatched 02:36Z on main@ccd1024 (app diff vs gated b44f03
  ccd1024c ckpt 1098: BS2d GATE raw: #524 (id 42942929350, job 130268804149, base,n6,o,x,uf,cs2,an
  67c16d80 ckpt 1095: BS2d GATE: #526 (id 42942938730, job 130268830144, rg solo, main@b44f033): 7
  f550342c ckpt 1091: BS2d GATE dispatched 02:00Z on main@b44f033 (app diff vs gated wf head 9528d
  b44f0331 ckpt 1090: BS2d: wf run #523 (id 42941618944, job 130264537064, main@9528dd0 = final co
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
