# CHECKPOINT 1624 — read me first, then TASKS.md

**Written:** 2026-11-23T00:56:30Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `claude/dazzling-hawking-b7qz0b` · **builds on:** `17af4d97` (this checkpoint is the commit after it)

## Just done
gated v0.9.18 (code 57) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.18 "CW1: speed + resources audit fixes (profile ready earlier, first home patch composite sooner, no pencil search before the dialog event, idle threads time out, hidden-app icons not cached), stock-launcher bookkeeping counts only what can pin; no behaviour removed"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ef3504c1 ckpt 1623: CW1 RAW 00:54Z: #763 45090010377 main base,e1 GREEN (E1 passes on head). #76
  4de58d41 ckpt 1622: CW1 READ 00:41Z: #762 head base,legacy,e1,y2,b4,lq = 68P 1F: ONLY E1 'Delete
  3987ecf1 ckpt 1621: CW1 RAW 00:38Z: #762 45087708823 (head, base,legacy,e1,y2,b4,lq, 2nd sample)
  ed5ec54e ckpt 1620: CW1 READ 00:33Z: TAG #758 base,legacy,e1,y2,b4,lq = 67P 2F (E1 x2 + NATIVE C
  326d9c17 ckpt 1619: CW1 RAW 00:31Z: #759 TAG ul GREEN; #760 MAIN ul GREEN (UL24 ok solo = chain 
  b3b0dca2 ckpt 1618: CW1 dispatched 00:27Z: tag v0.9.17 base,legacy,e1,y2,b4,lq + ul; main: ul, u
  99497a36 ckpt 1617: CW1 SF2/SF3 under unified = test premise: StockForceStop.sessionWeight(unifi
  2fe4dce4 ckpt 1616: CW1 RERUN READ 00:20Z (subagent): #753 base,legacy,e1,y2,b4,lq 63P 6F no nat
  c98e317f ckpt 1615: CW1 RERUN RAW 00:11Z: #756 ad,ps(no unified) GREEN; #757 unified,last GREEN 
  52a775bb ckpt 1614: CW1 RERUNS on main (product diff vs gated 880732c8 EMPTY, only scenario sf l
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
