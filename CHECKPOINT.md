# CHECKPOINT 1309 — read me first, then TASKS.md

**Written:** 2026-11-13T05:21:11Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `c468a5fb` (this checkpoint is the commit after it)

## Just done
run #595 43370382236 vo GREEN 24 PASS 0 FAIL on 32a96ffd (VO0-VO8b incl. Quickstep, lock-panel row gone, cap, not allowed, switch off)

## Do this next
bash tools/gate.sh v0.9.9 -> dispatch its groups; then full unit suite via ship.sh; heartbeat trig_01MRTg3yGjbXMnzqNtcrbLWg

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d8b0eaaa ckpt 1308: vo rerun dispatched 05:11Z run #595 id 43370382236 on 32a96ffd (review fixes
  32a96ffd ckpt 1307: CB2 review fixes coded: VpnOffRules window 3/10min + SETTLING + REFUSED_BEFO
  cb4a81fe ckpt 1306: review of CB2 (subagent): 11 findings: MED1 cap reset only at USER_PRESENT; 
  862ff1ad ckpt 1305: run #594 43367976336 vo: 16 PASS 5 FAIL (VO0 VO1 VO2 VO5b VO6) = the PROBE: 
  f3f19593 ckpt 1304: CB2e (Tj: 'And any time I lock the screen from quickstep') in TASKS; rule ha
  2719a394 ckpt 1303: emulator vo dispatched 04:49Z run #594 id 43367976336 on 64268967; review su
  64268967 ckpt 1302: CB2c section vo written (premise footer row, A switch off, B not allowed, C 
  61b5eff1 ckpt 1301: CB2b coded: VpnOff.java (worker thread, appops read, getAllNetworks, prepare
  d1b34d9e ckpt 1300: CB2a verified (Vpn.prepare path, Always-on blocks, legacy lockdown throws, C
  f2fed0e9 ckpt 1299: CB2b started: VpnOffRules + VpnOffRulesTest (pure rule: screen dark or lock 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
