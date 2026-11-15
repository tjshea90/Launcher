# CHECKPOINT 1370 — read me first, then TASKS.md

**Written:** 2026-11-15T22:48:43Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-9028296e-kjveqx` · **builds on:** `97a99888` (this checkpoint is the commit after it)

## Just done
CE1 vo #615 (ec0a14ed+grant): 26 PASS 2 FAIL (VO5b VO5c only: the cap check counted stops across two launcher processes; word publishing now PASS: quiet:custom launcher / quickstep / quiet:screen off); CF1a audit saved research/RESTART_NO_CABLE_2026-11-15.md

## Do this next
rewrite vo phase E to be independent of earlier stop counts (reconnect up to 5x; the reconnect that stays up must be #1-3; pre-ones stopped), rerun vo solo, then ship.sh + android.yml; then answer Tj CE1 and CF1 (list from research/RESTART_NO_CABLE_2026-11-15.md, check TASKS CF1a)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
    ?? research/RESTART_NO_CABLE_2026-11-15.md

## Last ten checkpoints
```
  98824d26 ckpt 1369: CF1 written (Tj: after all releases, review what keeps working / breaks afte
  cd382c45 ckpt 1368: CE1: #613 vo solo 24 PASS 4 FAIL (VO2d VO6b VO8a VO8c: word null = no WRITE_
  ec0a14ed ckpt 1367: CE1: vo scenario B/C rewritten for the new custom-launcher stop (log cleared
  52626f4d ckpt 1366: CE1 gate verdict final on c0d29aae: #610 86 PASS 6 FAIL (VO2b VO3b VO3c VO5 
  94a9c117 ckpt 1365: CE1 gate verdict on c0d29aae: #611 (x1,e1,y2,lq,an,wf) GREEN; #610 (base,ac,
  ed7e4836 ckpt 1364: CE1 gate on c0d29aae dispatched 21:57Z: #610 id 43766533567 base,ac,rb,vo,la
  c0d29aae ckpt 1363: CE1: Launcher unit 1054 tests 0 fail + lint ok after review fixes; dispatchi
  cdfb5bea ckpt 1362: CE1: review fixes 1,3,4,5,6,8 in VpnOff.java + ControlActivity (not yet comp
  ed0d7d93 ckpt 1361: CE1: review of VpnOff diff done, 8 findings saved in research/VPN_FOOTER (ME
  ac285646 ckpt 1360: CE1: LinkMeter docs+HANDOFF done (b9e20d7), Launcher HANDOFF 0y + VPN_FOOTER
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
