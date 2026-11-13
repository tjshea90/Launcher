# CHECKPOINT 1299 — read me first, then TASKS.md

**Written:** 2026-11-13T04:40:15Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `13792c6a` (this checkpoint is the commit after it)

## Just done
CB2b started: VpnOffRules + VpnOffRulesTest (pure rule: screen dark or lock up, VPN up, allowed, cap 3/lock), testnotifier TestVpnService (routes 10.111.0.0/24 only, TJVPN log) + MainActivity --ez vpn true

## Do this next
wait for web-fetch agent (AOSP 10 Vpn.prepare verify); then VpnOff.java glue (appops check android:activate_vpn, getAllNetworks TRANSPORT_VPN, prepare on bg thread, NetworkCallback), hooks in TjAccessibilityService lockReceiver SCREEN_OFF/ON + lockPrepareLater + USER_PRESENT reset, ControlActivity row + Allow, ReportState line, ACCESS_NETWORK_STATE

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  732dcb95 ckpt 1298: CB2 (Tj: turn the VPN off at screen-off / lock screen) written into TASKS; C
  5c2800f9 ckpt 1297: CB1a done: research/VPN_FOOTER_2026-11-13.md (QSSecurityFooter; no setting; 
  589a9f66 ckpt 1296: CB1a started: web-fetch agent reading AOSP 10 QSSecurityFooter / QSPanel / Q
  211c2419 ckpt 1295: CB1 (Tj: remove the VPN 'Device connected to' row from the panel) written in
  969297d8 ckpt 1294: heartbeat trig_01WL1UmCwAoqLLcPFywmb7Dv DELETED (Tj approved); HANDOFF 0w st
  18ce5b5c ckpt 1293: v0.9.9 RELEASED 03:39Z + RECORDED (android.yml #50 id 43360200878 green on f
  1d0c087b ckpt 1292: ship.sh v0.9.9 green (SHIP_EXIT=0, f43d41ad, main==tip); android.yml full_bu
  f43d41ad ckpt 1291: gated v0.9.9 (code 48) and pushed it: the handoff self-test, the full unit s
  573db729 ckpt 1290: version bumped to 48 / 0.9.9; ship.sh starting in background (scratchpad/shi
  1f37ec66 ckpt 1289: GATE GREEN on 566fc305: #586 #588 green; reruns #589 fl head GREEN (0 Screen
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
