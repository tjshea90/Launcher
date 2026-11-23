# CHECKPOINT 1620 — read me first, then TASKS.md

**Written:** 2026-11-23T00:33:27Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `18819eec` (this checkpoint is the commit after it)

## Just done
CW1 READ 00:33Z: TAG #758 base,legacy,e1,y2,b4,lq = 67P 2F (E1 x2 + NATIVE CRASH #1 jemalloc/libhwui = known flake); breaker/AR1/W5 GREEN on tag. HEAD #753 = 63P 6F: breaker (same pid 5025, no crash delivered), AR1 dump EMPTY, W5 stock home, base 330 s vs tag 213 s, NO native crash = looks like our main thread blocked/slow (am crash and uiautomator both need it) -> NOT cleared; #762 45087708823 (same chain, head) decides

## Do this next
read #762 (probe + haiku). If red again with breaker/AR1: find the main-thread block in diff (A1 isAppsBound mLock? HomeProfile.ready? A4 settle?), else flake; heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  326d9c17 ckpt 1619: CW1 RAW 00:31Z: #759 TAG ul GREEN; #760 MAIN ul GREEN (UL24 ok solo = chain 
  b3b0dca2 ckpt 1618: CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, u
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
  2fe4dce4 ckpt 1616: CW1 RERUN READ 00:20Z (subagent): #753 base,legacy,e1,y2,b4,lq 63P 6F no nat
  c98e317f ckpt 1615: CW1 RERUN RAW 00:11Z: #756 ad,ps(no unified) GREEN; #757 unified,last GREEN 
  52a775bb ckpt 1614: CW1 RERUNS on main (product diff vs gated 880732c8 EMPTY, only scenario sf l
  d0b4ad2e ckpt 1613: CW1 TRIAGE VERDICT 23:55Z (subagent): #750 main AND #751 TAG unified,ad,ps b
  21498a62 ckpt 1612: CW1 TRIAGE RAW 23:50Z: #745 hp(main) GREEN; #746 unified,hp(main) GREEN; #74
  ea6941b0 ckpt 1611: CW1 RAW #742 (sections base,legacy,e1,y2,b4,lq,ao,cs2,hf,ul): EMULATOR DEATH
  41e0bdd3 ckpt 1610: CW1 triage reruns dispatched 23:34Z: #745 45083960910 hp(main) | #746 450839
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
