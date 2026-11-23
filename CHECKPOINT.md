# CHECKPOINT 1666 — read me first, then TASKS.md

**Written:** 2026-11-23T06:40:45Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `4f8ce7b1` (this checkpoint is the commit after it)

## Just done
CX1 triage RAW 06:42Z: ul solo main #786 GREEN (UL24 = native-crash flake); hf solo main #787 id 45124602963 FAIL HF5 only (median open_shape 0.1407 over 10, limit 0.14; chain run #782 had 0.17); hf solo TAG v0.9.18 #788 id 45124605974 GREEN; fl main #784 id 45124590829, fl TAG #785 id 45124594693, hm main #789 id 45124609597, gate group #781 id 45121482111 still in_progress

## Do this next
read #788 hf median (haiku a89d2c4a863cffa0a), then fl/hm; if hf head is systematically above tag, look for the home patch change that shifts open_shape; cancel #781 at 06:52Z; heartbeat trig_01GoGsAi9y4N2PpYgaAmbk6A

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4f8ce7b1 ckpt 1665: CX1 gate 8dc58abf RAW FAILS: #779 fl 86P 2F: FL2 menuW1 flashA 5>4, FL2 menu
  30eb9413 ckpt 1664: CX1 gate 8dc58abf RAW: pt #783 id 45121488481 GREEN; #779 (ac,ad,se,ps,cu,fl
  9b9c664d ckpt 1663: removed stray off-topic message (Tj: wrong repo) from TASKS/INBOX/OUTBOX. Ga
  fe546733 ckpt 1662: CY1: Tj's parlay message written into TASKS (not a Launcher job, waits on Tj
  af17492b ckpt 1661: CX1 GATE in flight on 8dc58abf (v0.9.18..HEAD = H1-H5 + pencil wake): #779 i
  8dc58abf ckpt 1660: CX1c-H5: HomePatchRules.lossOwnedByDialog + homeLossOwned: a second stock-di
  cf0c9213 ckpt 1659: CX1c-H4 review fixes (subagent a1e2134d: 0 HIGH 2 MEDIUM): calibration trigg
  39393a17 ckpt 1658: CX1: in flight on 5d9b46c8 (H3 + HM2 tiering): hm #777 id 45118558677, hp #7
  5d9b46c8 ckpt 1657: CX1c: hm #776 id 45116760550 (head 378fd991 H2+nobounce): 15 PASS 4 FAIL (hm
  716201da ckpt 1656: CX1c-H3: HomePatchRules.SHADE_CLOSE_SETTLE_MS 350 / settleAsTouchMs: the res
```
