# CHECKPOINT 1623 — read me first, then TASKS.md

**Written:** 2026-11-23T00:53:27Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `6c9a8bfe` (this checkpoint is the commit after it)

## Just done
CW1 RAW 00:54Z: #763 45090010377 main base,e1 GREEN (E1 passes on head). #764 45090013546 ao,cs2,hf,ul still running (13 min in). Gate picture on head 880732c8-code: #739 n,n6,bs,bt,mu,pk,ul2 green; #740 x1,o,t,pf,rb,sr green, cs red->#748 green, sf red->#761 green after scenario premise fix; #741 bl,uf,an,nq,ss green, ad,ps red->#756 green without unified (AD5a/b old-world premise, tag fails too #751), last red->#757 green; #743 b6,ac,cu,rt,gear,wf,pm ok, hp red->#745/#746 green; #744 rg green; base chain #753 red once (stall), #762 E1+native crash = tag #758 same, #763 green

## Do this next
when #764 ends: probe; then bump app/build.gradle.kts versionCode 57 / versionName 0.9.18, TASKS CW1e/f lines, bash ship.sh 'CW1 ...', android.yml full_build; heartbeat trig_01DN2e1WG9kDhpHMtsFee1Lb

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4de58d41 ckpt 1622: CW1 READ 00:41Z: #762 head base,legacy,e1,y2,b4,lq = 68P 1F: ONLY E1 'Delete
  3987ecf1 ckpt 1621: CW1 RAW 00:38Z: #762 45087708823 (head, base,legacy,e1,y2,b4,lq, 2nd sample)
  ed5ec54e ckpt 1620: CW1 READ 00:33Z: TAG #758 base,legacy,e1,y2,b4,lq = 67P 2F (E1 x2 + NATIVE C
  326d9c17 ckpt 1619: CW1 RAW 00:31Z: #759 TAG ul GREEN; #760 MAIN ul GREEN (UL24 ok solo = chain 
  b3b0dca2 ckpt 1618: CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, u
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
  2fe4dce4 ckpt 1616: CW1 RERUN READ 00:20Z (subagent): #753 base,legacy,e1,y2,b4,lq 63P 6F no nat
  c98e317f ckpt 1615: CW1 RERUN RAW 00:11Z: #756 ad,ps(no unified) GREEN; #757 unified,last GREEN 
  52a775bb ckpt 1614: CW1 RERUNS on main (product diff vs gated 880732c8 EMPTY, only scenario sf l
  d0b4ad2e ckpt 1613: CW1 TRIAGE VERDICT 23:55Z (subagent): #750 main AND #751 TAG unified,ad,ps b
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
