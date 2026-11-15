# CHECKPOINT 1366 — read me first, then TASKS.md

**Written:** 2026-11-15T22:23:32Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9028296e-kjveqx` · **builds on:** `94a9c117` (this checkpoint is the commit after it)

## Just done
CE1 gate verdict final on c0d29aae: #610 86 PASS 6 FAIL (VO2b VO3b VO3c VO5 VO5b VO5c, no native crash); #611 GREEN; #612 141 PASS 1 FAIL (SS7 known flake: 'no Settings entry in drawer dump', diag says head #576 failed too, tag #580 found it). Reading of VO fails: the NEW design stops a VPN in the custom launcher at once (process start / a VPN connected), so vo phases B/C/E counts were written for the old screen-off-only design: test design, product behaves as intended

## Do this next
rewrite vo phases B/C/E (clear logcat before the VPN is started; C = process start / reconnect in custom launcher turns it off, E counts 3 stops from a known start), rerun section vo solo, then rerun ss SOLO (SS7), then ship.sh + android.yml

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  94a9c117 ckpt 1365: CE1 gate verdict on c0d29aae: #611 (x1,e1,y2,lq,an,wf) GREEN; #610 (base,ac,
  ed7e4836 ckpt 1364: CE1 gate on c0d29aae dispatched 21:57Z: #610 id 43766533567 base,ac,rb,vo,la
  c0d29aae ckpt 1363: CE1: Launcher unit 1054 tests 0 fail + lint ok after review fixes; dispatchi
  cdfb5bea ckpt 1362: CE1: review fixes 1,3,4,5,6,8 in VpnOff.java + ControlActivity (not yet comp
  ed0d7d93 ckpt 1361: CE1: review of VpnOff diff done, 8 findings saved in research/VPN_FOOTER (ME
  ac285646 ckpt 1360: CE1: LinkMeter docs+HANDOFF done (b9e20d7), Launcher HANDOFF 0y + VPN_FOOTER
  94f95c50 ckpt 1359: CE1b LinkMeter code written (uncommitted-build): FastRules.pauseWhy+PAUSED c
  a0e1c45b ckpt 1358: CE1a code: VpnOff follows NotifQuiet world (custom launcher too), publishes 
  fce33b22 ckpt 1357: CE1 written (Tj: VPN row on lock screen; LinkMeter vs VpnOff fight); CD3a ve
  771b5e32 ckpt 1356: CD3 written (Tj 05:44Z: 3h avg keeps falling, 130); CD2e verified+ticked; re
```
