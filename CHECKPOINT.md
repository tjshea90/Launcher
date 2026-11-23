# CHECKPOINT 1618 — read me first, then TASKS.md

**Written:** 2026-11-23T00:16:53Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `99497a36` (this checkpoint is the commit after it)

## Just done
CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, unified,sf (scenario fixed), base,legacy,e1,y2,b4,lq (same-code repeat of #753). CrashGuard diff = read-handle only (writes unchanged): #753 breaker FAILs look like the dropped-forced-crash race (same pid 5025 over all tries) but the TAG run decides

## Do this next
wait until 20261123004000 background; list runs perPage 6 resource emulator.yml; read each via failed_only probe + haiku subagent; ckpt raw FIRST; heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
  2fe4dce4 ckpt 1616: CW1 RERUN READ 00:20Z (subagent): #753 base,legacy,e1,y2,b4,lq 63P 6F no nat
  c98e317f ckpt 1615: CW1 RERUN RAW 00:11Z: #756 ad,ps(no unified) GREEN; #757 unified,last GREEN 
  52a775bb ckpt 1614: CW1 RERUNS on main (product diff vs gated 880732c8 EMPTY, only scenario sf l
  d0b4ad2e ckpt 1613: CW1 TRIAGE VERDICT 23:55Z (subagent): #750 main AND #751 TAG unified,ad,ps b
  21498a62 ckpt 1612: CW1 TRIAGE RAW 23:50Z: #745 hp(main) GREEN; #746 unified,hp(main) GREEN; #74
  ea6941b0 ckpt 1611: CW1 RAW #742 (sections base,legacy,e1,y2,b4,lq,ao,cs2,hf,ul): EMULATOR DEATH
  41e0bdd3 ckpt 1610: CW1 triage reruns dispatched 23:34Z: #745 45083960910 hp(main) | #746 450839
  97c4a442 ckpt 1609: CW1 GATE RAW (read by subagent): #740 job 137385027035 84 PASS 6 FAIL: CS-B1
  5a759cb6 ckpt 1608: CW1 GATE RAW 23:31Z main@880732c8: #739 (n,n6,bs,bt,mu,pk,ul2) GREEN; #740 (
```
