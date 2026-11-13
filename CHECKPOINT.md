# CHECKPOINT 1298 — read me first, then TASKS.md

**Written:** 2026-11-13T04:36:24Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `fea4df47` (this checkpoint is the commit after it)

## Just done
CB2 (Tj: turn the VPN off at screen-off / lock screen) written into TASKS; CB1e answered (no cover)

## Do this next
CB2a: web-fetch agent verifies AOSP 10 Vpn.prepare/prepareInternal/isVpnUserPreConsented, ConnectivityService.prepareVpn, VpnService.prepare, vpndialogs ConfirmDialog; meanwhile read SCREEN_OFF handler in TjAccessibilityService + testnotifier manifest

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  5c2800f9 ckpt 1297: CB1a done: research/VPN_FOOTER_2026-11-13.md (QSSecurityFooter; no setting; 
  589a9f66 ckpt 1296: CB1a started: web-fetch agent reading AOSP 10 QSSecurityFooter / QSPanel / Q
  211c2419 ckpt 1295: CB1 (Tj: remove the VPN 'Device connected to' row from the panel) written in
  969297d8 ckpt 1294: heartbeat trig_01WL1UmCwAoqLLcPFywmb7Dv DELETED (Tj approved); HANDOFF 0w st
  18ce5b5c ckpt 1293: v0.9.9 RELEASED 03:39Z + RECORDED (android.yml #50 id 43360200878 green on f
  1d0c087b ckpt 1292: ship.sh v0.9.9 green (SHIP_EXIT=0, f43d41ad, main==tip); android.yml full_bu
  f43d41ad ckpt 1291: gated v0.9.9 (code 48) and pushed it: the handoff self-test, the full unit s
  573db729 ckpt 1290: version bumped to 48 / 0.9.9; ship.sh starting in background (scratchpad/shi
  1f37ec66 ckpt 1289: GATE GREEN on 566fc305: #586 #588 green; reruns #589 fl head GREEN (0 Screen
  a6b44f50 ckpt 1288: reruns 03:14Z: #589 43357592447 fl head; #590 43357596323 fl TAG v0.9.8; #59
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
