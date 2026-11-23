# CHECKPOINT 1656 — read me first, then TASKS.md

**Written:** 2026-11-23T05:25:42Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `75ccd85c` (this checkpoint is the commit after it)

## Just done
CX1c-H3: HomePatchRules.SHADE_CLOSE_SETTLE_MS 350 / settleAsTouchMs: the resting home patch waits 350 ms after the Launcher got the focus back from a foreign window (hmShade2 flashA 16/28/19 frames = patch over the closing shade); 2 JVM tests, HomePatchRulesTest green GRADLE_EXIT=0. gm,fl #775 verdict recorded in ckpt 1655 (only gmFling 233 ms: patch away after pad left + dialog event 181 ms old)

## Do this next
read hm rerun (dispatched 05:17Z on ckpt 1654, no id yet: list_workflow_runs perPage 1): hmApp floating=<class>? hmApp pass with hm_nobounce?; then dispatch hm on H3 head; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  03846a43 ckpt 1655: CX1 RAW VERDICT gm,fl #775 id 45114867392 job 137495293731 (head a5d0d18e H2
  378fd991 ckpt 1654: CX1c: hm #774 analysis: hmApp 'floating=true' = a Launcher3 floating view is
  3361f0d6 ckpt 1653: CX1 RAW VERDICT hm #774 id 45114864876 job 137495285640 (head a5d0d18e H2): 
  8bbf9560 ckpt 1652: CX1c: in flight on a5d0d18e (H2): hm #774 id 45114864876 (04:57Z, ~8 min), g
  a5d0d18e ckpt 1651: CX1c-H2: PowerMenuRule.holdOnMiss (menu text seen but card not located: hold
  d9e77ef6 ckpt 1650: CX1 RAW VERDICT hp #773 id 45112680139 job 137488368103 (head 64dda30f): SUC
  5a786434 ckpt 1649: CX1 RAW VERDICT hm #772 id 45112677186 job 137488357728 (head 64dda30f keep-
  0d28ad66 ckpt 1648: CX1c: in flight on 64dda30f: hm #772 id 45112677186, hp #773 id 45112680139 
  64dda30f ckpt 1647: CX1c-H1: home window KEPT hidden through transient reasons (app/drawer/folde
  a2c1e498 ckpt 1646: CX1: gm baseline 2 #770 id 45109356329 job 137477900783 ran > 25 min (emulat
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
