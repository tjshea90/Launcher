# CHECKPOINT 1627 — read me first, then TASKS.md

**Written:** 2026-11-23T01:09:29Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `9c1d777c` (this checkpoint is the commit after it)

## Just done
CW1 RAW 01:09Z: hf SOLO x3 GREEN: #765 45091892552 main, #766 45091895243 branch (same head), #767 45091897619 TAG => #764's HF1/4/5 = chain slowness flake (hf 424 s). Gate complete: product code vs gated 880732c8 differs ONLY by the version bump 0.9.18/57; UL24 passed 3/3 in #764; every red section re-proven green solo (see TASKS CW1e)

## Do this next
dispatch android.yml ref main full_build=true; wait ~3 min; get_release_by_tag v0.9.18; bash tools/record-release.sh v0.9.18 ...; OUTBOX answer; HANDOFF 0zd rewrite; delete heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  7fb952e0 ckpt 1626: CW1 READ 00:58Z #764 (ao,cs2,hf,ul 2nd): 43P 3F = HF1 (home patch never cali
  d7876e0a ckpt 1625: CW1 00:57Z: ship.sh v0.9.18 (code 57) EXIT 0 (suite+lint+handoff green), NOT
  c3f00ded ckpt 1624: gated v0.9.18 (code 57) and pushed it: the handoff self-test, the full unit 
  ef3504c1 ckpt 1623: CW1 RAW 00:54Z: #763 45090010377 main base,e1 GREEN (E1 passes on head). #76
  4de58d41 ckpt 1622: CW1 READ 00:41Z: #762 head base,legacy,e1,y2,b4,lq = 68P 1F: ONLY E1 'Delete
  3987ecf1 ckpt 1621: CW1 RAW 00:38Z: #762 45087708823 (head, base,legacy,e1,y2,b4,lq, 2nd sample)
  ed5ec54e ckpt 1620: CW1 READ 00:33Z: TAG #758 base,legacy,e1,y2,b4,lq = 67P 2F (E1 x2 + NATIVE C
  326d9c17 ckpt 1619: CW1 RAW 00:31Z: #759 TAG ul GREEN; #760 MAIN ul GREEN (UL24 ok solo = chain 
  b3b0dca2 ckpt 1618: CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, u
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
