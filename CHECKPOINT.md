# CHECKPOINT 1114 — read me first, then TASKS.md

**Written:** 2026-11-11T06:01:22Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-knuth-f1khhb` · **builds on:** `d027fe80` (this checkpoint is the commit after it)

## Just done
run #529 (id 42968657835, job 130351718801, sections=bt on d027fe8): 10 PASS 3 FAIL. BT1f PASS (Recents card = Settings). REPRODUCED BT1c: Screenshot entry visible at control (no shade), gap 0.4 s, gap 1.2 s; covered at gap 4 s (pictures in ci-shots, fetched to scratchpad/shots529). cause: not-ready home patch leaves short card with Screenshot slot uncovered (cardArea uses full only if patchView==null)

## Do this next
Heartbeat trig_01DZ15y7gsi1bXpJ7P2KBghc. NEXT: (1) widen bt INFO diagnostics (full fast-skip reasons, HOMEPATCH away/stale/built timeline); (2) fix: cardArea returns full panel whenever the home patch will not be shown for this menu (never leave the Screenshot slot open) + lift foreign guard on focus regain + per-press diagnostics line; (3) lock side BT1a: onOutsideTouch returns early when !patchResting (touch before resting is not recorded) - record touchAway regardless

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d027fe80 ckpt 1113: section bt added to emulator_scenario.sh + gate-map (BT1f look-alike Recents
  3c05741c ckpt 1112: BT1e+BT1f code done, JVM green (KeyboardPolicyTest, AppLabelTest, BUILD SUCC
  72acd019 ckpt 1111: BT1 first analysis written (research/BT1_ANALYSIS_2026-11-11.md): BT1c/d = B
  3b9848af ckpt 1110: BT1a-f written in TASKS from Tj 05:03Z msg; BS1/BS2/BS2g ticked (answer in O
  eec4700e ckpt 1109: v0.9.4 RELEASED+RECORDED (run #45 id 42948918455 on 9676905). HANDOFF 0r, TA
  a409a340 ckpt 1104: BS1e SHIP: android.yml full_build=true dispatched 02:52:12Z on main@9676905 
  96769051 ckpt 1102: gated v0.9.4 (code 43) and pushed it: the handoff self-test, the full unit s
  b64b0e1a ckpt 1100: BS2d GATE COMPLETE: #527 (id 42947197401, job 130282725555, ss solo) step co
  471266e9 ckpt 1099: BS2d GATE reruns dispatched 02:36Z on main@ccd1024 (app diff vs gated b44f03
  ccd1024c ckpt 1098: BS2d GATE raw: #524 (id 42942929350, job 130268804149, base,n6,o,x,uf,cs2,an
```
