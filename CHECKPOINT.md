# CHECKPOINT 1651 — read me first, then TASKS.md

**Written:** 2026-11-23T04:57:33Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `5e814522` (this checkpoint is the commit after it)

## Just done
CX1c-H2: PowerMenuRule.holdOnMiss (menu text seen but card not located: hold 3x250 ms, was: hide at once), PatchRest.rectMoveRemovesWindow (no window replace on rect wobble while menu open), TjHooks.homeAtRestWhy in homeNotFreshWhy; 3 JVM test groups green GRADLE_EXIT=0

## Do this next
dispatch hm + gm on main (head with H2), compare hmOff1/2 (no 'menu closed' at +0.7 s), hmApp1/2 log now prints the homeAtRest facts {resumed normal floating busy}; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d9e77ef6 ckpt 1650: CX1 RAW VERDICT hp #773 id 45112680139 job 137488368103 (head 64dda30f): SUC
  5a786434 ckpt 1649: CX1 RAW VERDICT hm #772 id 45112677186 job 137488357728 (head 64dda30f keep-
  0d28ad66 ckpt 1648: CX1c: in flight on 64dda30f: hm #772 id 45112677186, hp #773 id 45112680139 
  64dda30f ckpt 1647: CX1c-H1: home window KEPT hidden through transient reasons (app/drawer/folde
  a2c1e498 ckpt 1646: CX1: gm baseline 2 #770 id 45109356329 job 137477900783 ran > 25 min (emulat
  e03bdd35 ckpt 1645: CX1 RAW VERDICT hm #771 id 45109794817 job 137479292087 (head==tag): 15 PASS
  a9b431f6 ckpt 1644: CX1: BASELINE 2 in flight on product code == tag v0.9.18: gm #770 id 4510935
  6ff6b527 ckpt 1643: CX1d/CX1e written into TASKS (baseline findings + fix plan); gm baseline 2 a
  6ca77abe ckpt 1642: CX1b: hm baseline #769 id 45107548707 = geometry bug only (core box empty: h
  bd1d674e ckpt 1641: CX1b: fl_analyze --from (menu judged only from the press; selftest 4b), flfu
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
