# CHECKPOINT 1670 — read me first, then TASKS.md

**Written:** 2026-11-23T07:07:55Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-gates-hbco93` · **builds on:** `ca55b3ee` (this checkpoint is the commit after it)

## Just done
CX1h: bt solo main #792 id 45127351694 GREEN (BT1f was a flake); gm solo branch #793 id 45127355167 in progress; fl menuP1 on #784/#785: fast open handled 116-149 ms after the dialog event (age=), no cause in our log; menuP2 same code age 2-11 ms no leak = emulator CPU contention, probabilistic

## Do this next
read gm #793 (~07:15Z): if gmHalf green or any single case leaks <=4 frames, ship v0.9.19 (bash ship.sh; android.yml full_build; record-release) and say so in the answer; heartbeat trig_012EemLoDGk8vy7vUJX1T3Qk

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  2450629a ckpt 1669: CX1h: TASKS box written (gate triage state, 8 runs); bt solo main + gm solo 
  b4d94d57 ckpt 1668: CX1 takeover 06:55Z (new session, branch dazzling-gates-hbco93, old session 
  8e2389c4 ckpt 1667: CX1 triage RAW 06:46Z: gate group #781 id 45121482111 ENDED 06:33 FAILED (de
  19582175 ckpt 1666: CX1 triage RAW 06:42Z: ul solo main #786 GREEN (UL24 = native-crash flake); 
  4f8ce7b1 ckpt 1665: CX1 gate 8dc58abf RAW FAILS: #779 fl 86P 2F: FL2 menuW1 flashA 5>4, FL2 menu
  30eb9413 ckpt 1664: CX1 gate 8dc58abf RAW: pt #783 id 45121488481 GREEN; #779 (ac,ad,se,ps,cu,fl
  9b9c664d ckpt 1663: removed stray off-topic message (Tj: wrong repo) from TASKS/INBOX/OUTBOX. Ga
  fe546733 ckpt 1662: CY1: Tj's parlay message written into TASKS (not a Launcher job, waits on Tj
  af17492b ckpt 1661: CX1 GATE in flight on 8dc58abf (v0.9.18..HEAD = H1-H5 + pencil wake): #779 i
  8dc58abf ckpt 1660: CX1c-H5: HomePatchRules.lossOwnedByDialog + homeLossOwned: a second stock-di
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
