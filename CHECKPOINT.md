# CHECKPOINT 1671 — read me first, then TASKS.md

**Written:** 2026-11-23T07:31:42Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-gates-hbco93` · **builds on:** `84f639e2` (this checkpoint is the commit after it)

## Just done
CX1 release prep: version bumped to 0.9.19 code 58; gm solo #793 cancelled at 07:25 (emulator stall in gmHold 'no-video' after gmHalf at 07:03: fast open age 6 ms, no new FAIL; chain #781 had gm complete); product code == gated 8dc58abf (git diff empty); decision: ship v0.9.19 with FL2 menuP1 / GM2 gmHalf residual written down

## Do this next
run bash ship.sh note (local gate ~10+ min, background), then android.yml full_build on branch, get_release_by_tag, record-release; heartbeat trig_012EemLoDGk8vy7vUJX1T3Qk

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M app/build.gradle.kts

## Last ten checkpoints
```
  f152f218 ckpt 1670: CX1h: bt solo main #792 id 45127351694 GREEN (BT1f was a flake); gm solo bra
  2450629a ckpt 1669: CX1h: TASKS box written (gate triage state, 8 runs); bt solo main + gm solo 
  b4d94d57 ckpt 1668: CX1 takeover 06:55Z (new session, branch dazzling-gates-hbco93, old session 
  8e2389c4 ckpt 1667: CX1 triage RAW 06:46Z: gate group #781 id 45121482111 ENDED 06:33 FAILED (de
  19582175 ckpt 1666: CX1 triage RAW 06:42Z: ul solo main #786 GREEN (UL24 = native-crash flake); 
  4f8ce7b1 ckpt 1665: CX1 gate 8dc58abf RAW FAILS: #779 fl 86P 2F: FL2 menuW1 flashA 5>4, FL2 menu
  30eb9413 ckpt 1664: CX1 gate 8dc58abf RAW: pt #783 id 45121488481 GREEN; #779 (ac,ad,se,ps,cu,fl
  9b9c664d ckpt 1663: removed stray off-topic message (Tj: wrong repo) from TASKS/INBOX/OUTBOX. Ga
  fe546733 ckpt 1662: CY1: Tj's parlay message written into TASKS (not a Launcher job, waits on Tj
  af17492b ckpt 1661: CX1 GATE in flight on 8dc58abf (v0.9.18..HEAD = H1-H5 + pencil wake): #779 i
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
