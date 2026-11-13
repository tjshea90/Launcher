# CHECKPOINT 1302 — read me first, then TASKS.md

**Written:** 2026-11-13T04:49:11Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `0e70bcf2` (this checkpoint is the commit after it)

## Just done
CB2c section vo written (premise footer row, A switch off, B not allowed, C screen off stops, D reconnect while dark, E cap 3, F unlocked, G lock panel no row), gate-map row, emulator.yml list; VpnOffRulesTest 7/7 green, build OK

## Do this next
dispatch emulator.yml sections=vo on main; meanwhile /code-review high on the diff vs v0.9.9 (VpnOff, service hooks, Control, testnotifier, section vo)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M tools/emulator_scenario.sh

## Last ten checkpoints
```
  61b5eff1 ckpt 1301: CB2b coded: VpnOff.java (worker thread, appops read, getAllNetworks, prepare
  d1b34d9e ckpt 1300: CB2a verified (Vpn.prepare path, Always-on blocks, legacy lockdown throws, C
  f2fed0e9 ckpt 1299: CB2b started: VpnOffRules + VpnOffRulesTest (pure rule: screen dark or lock 
  732dcb95 ckpt 1298: CB2 (Tj: turn the VPN off at screen-off / lock screen) written into TASKS; C
  5c2800f9 ckpt 1297: CB1a done: research/VPN_FOOTER_2026-11-13.md (QSSecurityFooter; no setting; 
  589a9f66 ckpt 1296: CB1a started: web-fetch agent reading AOSP 10 QSSecurityFooter / QSPanel / Q
  211c2419 ckpt 1295: CB1 (Tj: remove the VPN 'Device connected to' row from the panel) written in
  969297d8 ckpt 1294: heartbeat trig_01WL1UmCwAoqLLcPFywmb7Dv DELETED (Tj approved); HANDOFF 0w st
  18ce5b5c ckpt 1293: v0.9.9 RELEASED 03:39Z + RECORDED (android.yml #50 id 43360200878 green on f
  1d0c087b ckpt 1292: ship.sh v0.9.9 green (SHIP_EXIT=0, f43d41ad, main==tip); android.yml full_bu
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
