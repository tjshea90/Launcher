# CHECKPOINT 1646 — read me first, then TASKS.md

**Written:** 2026-11-23T04:30:45Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `e03bdd35` (this checkpoint is the commit after it)

## Just done
CX1: gm baseline 2 #770 id 45109356329 job 137477900783 ran > 25 min (emulator stall?), CANCELLED 04:29Z to print RESULTS; hm #771 read (see ckpt 1645); decision: fix home flap (keep window across transient reasons: !homeShowing / floatingView / busy / crop-step, never remove while a menu is open) + lock lag

## Do this next
read #770 RESULTS block via haiku subagent (job 137477900783, tail 700): header, FAILs, FL gm* metrics, log: lines of cases with shot>0, TIME lines; then implement HomePatchRules.keepWindowTransient + refreshLayers keep branch + JVM test; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  e03bdd35 ckpt 1645: CX1 RAW VERDICT hm #771 id 45109794817 job 137479292087 (head==tag): 15 PASS
  a9b431f6 ckpt 1644: CX1: BASELINE 2 in flight on product code == tag v0.9.18: gm #770 id 4510935
  6ff6b527 ckpt 1643: CX1d/CX1e written into TASKS (baseline findings + fix plan); gm baseline 2 a
  6ca77abe ckpt 1642: CX1b: hm baseline #769 id 45107548707 = geometry bug only (core box empty: h
  bd1d674e ckpt 1641: CX1b: fl_analyze --from (menu judged only from the press; selftest 4b), flfu
  c5dbb2a9 ckpt 1640: CX1b RAW VERDICT gm #768 id 45106026788 (head==tag v0.9.18): 18 PASS 5 FAIL.
  0e3ffaed ckpt 1639: CX1b: BASELINES in flight: gm #768 id 45106026788 (03:26Z), hm #769 id 45107
  36da7375 ckpt 1638: CX1b: section hm (home matrix) written (7 events x 0.2/0.8 s then power menu
  f274afbc ckpt 1637: CX1b: BASELINE gm run #768 id 45106026788 dispatched 03:26Z main ce7c7be7 (p
  ce7c7be7 ckpt 1636: CX1b: section gm (gesture matrix) written: 6 flash cases (pad slow/half/hold
```
