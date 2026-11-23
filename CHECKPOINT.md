# CHECKPOINT 1665 — read me first, then TASKS.md

**Written:** 2026-11-23T06:30:23Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `d1992b64` (this checkpoint is the commit after it)

## Just done
CX1 gate 8dc58abf RAW FAILS: #779 fl 86P 2F: FL2 menuW1 flashA 5>4, FL2 menuW2 flashA 7>1 (wake then menu); #780 83P 1F: UL24 (double Home from app 2 of 3 custom) beside 2 jemalloc native crashes 06:17:52/06:18:28; #782 46P 2F: HF5 median open_shape, HM2 hmShade1 417 ms (fast path 1, limit 400); #783 pt GREEN; #781 base,e1,b6,n,o,bt,gm in_progress

## Do this next
triage: menuW vs tag v0.9.18 (fl solo on tag), UL24 solo rerun unified,ul, hmShade1 log + HF5 median; #781 cancel 06:42Z; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  30eb9413 ckpt 1664: CX1 gate 8dc58abf RAW: pt #783 id 45121488481 GREEN; #779 (ac,ad,se,ps,cu,fl
  9b9c664d ckpt 1663: removed stray off-topic message (Tj: wrong repo) from TASKS/INBOX/OUTBOX. Ga
  fe546733 ckpt 1662: CY1: Tj's parlay message written into TASKS (not a Launcher job, waits on Tj
  af17492b ckpt 1661: CX1 GATE in flight on 8dc58abf (v0.9.18..HEAD = H1-H5 + pencil wake): #779 i
  8dc58abf ckpt 1660: CX1c-H5: HomePatchRules.lossOwnedByDialog + homeLossOwned: a second stock-di
  cf0c9213 ckpt 1659: CX1c-H4 review fixes (subagent a1e2134d: 0 HIGH 2 MEDIUM): calibration trigg
  39393a17 ckpt 1658: CX1: in flight on 5d9b46c8 (H3 + HM2 tiering): hm #777 id 45118558677, hp #7
  5d9b46c8 ckpt 1657: CX1c: hm #776 id 45116760550 (head 378fd991 H2+nobounce): 15 PASS 4 FAIL (hm
  716201da ckpt 1656: CX1c-H3: HomePatchRules.SHADE_CLOSE_SETTLE_MS 350 / settleAsTouchMs: the res
  03846a43 ckpt 1655: CX1 RAW VERDICT gm,fl #775 id 45114867392 job 137495293731 (head a5d0d18e H2
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
