# CHECKPOINT 1301 — read me first, then TASKS.md

**Written:** 2026-11-13T04:44:57Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `52467185` (this checkpoint is the commit after it)

## Just done
CB2b coded: VpnOff.java (worker thread, appops read, getAllNetworks, prepare, NetworkCallback, describe), service hooks (connect watch+check, SCREEN_OFF, SCREEN_ON locked +500ms, USER_PRESENT reset, unbind/destroy unwatch), Control toggle + Allow row (startActivityForResult), ReportState line, RestoreStore off, ACCESS_NETWORK_STATE

## Do this next
compile + unit tests: bash tools/gradle.sh :app:testDebugUnitTest :testnotifier:assembleQuickDebug in background (GRADLE_EXIT marker, scratchpad log); then scenario section vo + gate-map row

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M app/src/main/java/com/tj/launcher/ControlActivity.java

## Last ten checkpoints
```
  d1b34d9e ckpt 1300: CB2a verified (Vpn.prepare path, Always-on blocks, legacy lockdown throws, C
  f2fed0e9 ckpt 1299: CB2b started: VpnOffRules + VpnOffRulesTest (pure rule: screen dark or lock 
  732dcb95 ckpt 1298: CB2 (Tj: turn the VPN off at screen-off / lock screen) written into TASKS; C
  5c2800f9 ckpt 1297: CB1a done: research/VPN_FOOTER_2026-11-13.md (QSSecurityFooter; no setting; 
  589a9f66 ckpt 1296: CB1a started: web-fetch agent reading AOSP 10 QSSecurityFooter / QSPanel / Q
  211c2419 ckpt 1295: CB1 (Tj: remove the VPN 'Device connected to' row from the panel) written in
  969297d8 ckpt 1294: heartbeat trig_01WL1UmCwAoqLLcPFywmb7Dv DELETED (Tj approved); HANDOFF 0w st
  18ce5b5c ckpt 1293: v0.9.9 RELEASED 03:39Z + RECORDED (android.yml #50 id 43360200878 green on f
  1d0c087b ckpt 1292: ship.sh v0.9.9 green (SHIP_EXIT=0, f43d41ad, main==tip); android.yml full_bu
  f43d41ad ckpt 1291: gated v0.9.9 (code 48) and pushed it: the handoff self-test, the full unit s
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
