# CHECKPOINT 1621 — read me first, then TASKS.md

**Written:** 2026-11-23T00:38:24Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `3697b88c` (this checkpoint is the commit after it)

## Just done
CW1 RAW 00:38Z: #762 45087708823 (head, base,legacy,e1,y2,b4,lq, 2nd sample) RED, job 137409986453; FAIL lines unread (#753 head red 6F, TAG #758 red only E1 x2 + native crash)

## Do this next
haiku: FAIL/INFO of #762; if breaker/AR1/W5 again = head-specific main-thread stall: bisect diff (git diff v0.9.17 HEAD) e.g. run base only on commits; heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ed5ec54e ckpt 1620: CW1 READ 00:33Z: TAG #758 base,legacy,e1,y2,b4,lq = 67P 2F (E1 x2 + NATIVE C
  326d9c17 ckpt 1619: CW1 RAW 00:31Z: #759 TAG ul GREEN; #760 MAIN ul GREEN (UL24 ok solo = chain 
  b3b0dca2 ckpt 1618: CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, u
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
  2fe4dce4 ckpt 1616: CW1 RERUN READ 00:20Z (subagent): #753 base,legacy,e1,y2,b4,lq 63P 6F no nat
  c98e317f ckpt 1615: CW1 RERUN RAW 00:11Z: #756 ad,ps(no unified) GREEN; #757 unified,last GREEN 
  52a775bb ckpt 1614: CW1 RERUNS on main (product diff vs gated 880732c8 EMPTY, only scenario sf l
  d0b4ad2e ckpt 1613: CW1 TRIAGE VERDICT 23:55Z (subagent): #750 main AND #751 TAG unified,ad,ps b
  21498a62 ckpt 1612: CW1 TRIAGE RAW 23:50Z: #745 hp(main) GREEN; #746 unified,hp(main) GREEN; #74
  ea6941b0 ckpt 1611: CW1 RAW #742 (sections base,legacy,e1,y2,b4,lq,ao,cs2,hf,ul): EMULATOR DEATH
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
