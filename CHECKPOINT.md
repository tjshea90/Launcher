# CHECKPOINT 1306 — read me first, then TASKS.md

**Written:** 2026-11-13T05:07:19Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `4b1e7b5a` (this checkpoint is the commit after it)

## Just done
review of CB2 (subagent): 11 findings: MED1 cap reset only at USER_PRESENT; MED2 network callback blind to a VPN that excludes the launcher (poll needed); MED3 VO1 cannot fail (runs before allowed); LOW4 per-lock cap no window; LOW5 double check at connect + stale screenDarkNow; LOW6 refusals flood log; LOW7 describe lacks helper; LOW8 VO6 wrong reason; LOW9 VO7b no premise; LOW10 E no reconnect proof; LOW11 late outcome note

## Do this next
rewrite VpnOffRules (window 3 per 10 min, refused/settling reasons) + VpnOff (worker state, poll 30s while locked via main Look, recheckLater, onLockedWorld/onUnlocked, outcome note immediate + late-safe), service hooks (screenDarkNow at connect, SCREEN_ON unlocked -> onUnlocked), section vo phase order; heartbeat trig_01MRTg3yGjbXMnzqNtcrbLWg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  862ff1ad ckpt 1305: run #594 43367976336 vo: 16 PASS 5 FAIL (VO0 VO1 VO2 VO5b VO6) = the PROBE: 
  f3f19593 ckpt 1304: CB2e (Tj: 'And any time I lock the screen from quickstep') in TASKS; rule ha
  2719a394 ckpt 1303: emulator vo dispatched 04:49Z run #594 id 43367976336 on 64268967; review su
  64268967 ckpt 1302: CB2c section vo written (premise footer row, A switch off, B not allowed, C 
  61b5eff1 ckpt 1301: CB2b coded: VpnOff.java (worker thread, appops read, getAllNetworks, prepare
  d1b34d9e ckpt 1300: CB2a verified (Vpn.prepare path, Always-on blocks, legacy lockdown throws, C
  f2fed0e9 ckpt 1299: CB2b started: VpnOffRules + VpnOffRulesTest (pure rule: screen dark or lock 
  732dcb95 ckpt 1298: CB2 (Tj: turn the VPN off at screen-off / lock screen) written into TASKS; C
  5c2800f9 ckpt 1297: CB1a done: research/VPN_FOOTER_2026-11-13.md (QSSecurityFooter; no setting; 
  589a9f66 ckpt 1296: CB1a started: web-fetch agent reading AOSP 10 QSSecurityFooter / QSPanel / Q
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
