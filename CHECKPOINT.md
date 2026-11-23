# CHECKPOINT 1647 — read me first, then TASKS.md

**Written:** 2026-11-23T04:34:32Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `487e6f51` (this checkpoint is the commit after it)

## Just done
CX1c-H1: home window KEPT hidden through transient reasons (app/drawer/folder/drag/crop step): HomePatchRules.transientReasonsOnly/keepWindowTransient + TAS.homeKeepWindow() called first in refreshLayers want==null; 5 JVM tests (HomePatchRulesTest green, GRADLE_EXIT=0). gm baseline 2 #770 stalled (screenrecord no-video after gmSlow; gmSlow shot=0 this time = not deterministic), cancelled

## Do this next
dispatch hm + hp on main (head has keep-window); read hm: hmApp1/2 must show 'fast open' (stale adoption) not 'no patch window yet'; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a2c1e498 ckpt 1646: CX1: gm baseline 2 #770 id 45109356329 job 137477900783 ran > 25 min (emulat
  e03bdd35 ckpt 1645: CX1 RAW VERDICT hm #771 id 45109794817 job 137479292087 (head==tag): 15 PASS
  a9b431f6 ckpt 1644: CX1: BASELINE 2 in flight on product code == tag v0.9.18: gm #770 id 4510935
  6ff6b527 ckpt 1643: CX1d/CX1e written into TASKS (baseline findings + fix plan); gm baseline 2 a
  6ca77abe ckpt 1642: CX1b: hm baseline #769 id 45107548707 = geometry bug only (core box empty: h
  bd1d674e ckpt 1641: CX1b: fl_analyze --from (menu judged only from the press; selftest 4b), flfu
  c5dbb2a9 ckpt 1640: CX1b RAW VERDICT gm #768 id 45106026788 (head==tag v0.9.18): 18 PASS 5 FAIL.
  0e3ffaed ckpt 1639: CX1b: BASELINES in flight: gm #768 id 45106026788 (03:26Z), hm #769 id 45107
  36da7375 ckpt 1638: CX1b: section hm (home matrix) written (7 events x 0.2/0.8 s then power menu
  f274afbc ckpt 1637: CX1b: BASELINE gm run #768 id 45106026788 dispatched 03:26Z main ce7c7be7 (p
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
