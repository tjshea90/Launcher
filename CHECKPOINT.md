# CHECKPOINT 1659 — read me first, then TASKS.md

**Written:** 2026-11-23T05:47:52Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `b5def2cc` (this checkpoint is the commit after it)

## Just done
CX1c-H4 review fixes (subagent a1e2134d: 0 HIGH 2 MEDIUM): calibration trigger before the keep, settle on every focus loss >= 300 ms, kept window = no window for fastDecline / homeOpenDecide / homeForeignWindow while the Launcher is not showing, compact diag suffix; tests green GRADLE_EXIT=0. hp #778 id 45118562300 on 5d9b46c8: SUCCESS (no FAIL). hm #777 id 45118558677 still running

## Do this next
read hm #777 (head 5d9b46c8, before H4): hmShade2 flashA<=1?; then dispatch hm,hp on H4 head, then gate.sh v0.9.18; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  39393a17 ckpt 1658: CX1: in flight on 5d9b46c8 (H3 + HM2 tiering): hm #777 id 45118558677, hp #7
  5d9b46c8 ckpt 1657: CX1c: hm #776 id 45116760550 (head 378fd991 H2+nobounce): 15 PASS 4 FAIL (hm
  716201da ckpt 1656: CX1c-H3: HomePatchRules.SHADE_CLOSE_SETTLE_MS 350 / settleAsTouchMs: the res
  03846a43 ckpt 1655: CX1 RAW VERDICT gm,fl #775 id 45114867392 job 137495293731 (head a5d0d18e H2
  378fd991 ckpt 1654: CX1c: hm #774 analysis: hmApp 'floating=true' = a Launcher3 floating view is
  3361f0d6 ckpt 1653: CX1 RAW VERDICT hm #774 id 45114864876 job 137495285640 (head a5d0d18e H2): 
  8bbf9560 ckpt 1652: CX1c: in flight on a5d0d18e (H2): hm #774 id 45114864876 (04:57Z, ~8 min), g
  a5d0d18e ckpt 1651: CX1c-H2: PowerMenuRule.holdOnMiss (menu text seen but card not located: hold
  d9e77ef6 ckpt 1650: CX1 RAW VERDICT hp #773 id 45112680139 job 137488368103 (head 64dda30f): SUC
  5a786434 ckpt 1649: CX1 RAW VERDICT hm #772 id 45112677186 job 137488357728 (head 64dda30f keep-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
