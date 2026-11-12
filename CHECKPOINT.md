# CHECKPOINT 1210 — read me first, then TASKS.md

**Written:** 2026-11-12T19:54:19Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `c4df18d0` (this checkpoint is the commit after it)

## Just done
BY1b: fl_analyze fixed (2 test bugs that made #563 unreadable): (1) VFR films end at their LAST change, REF now = the settled end frame (tpad clone 1 s); (2) odd-size crops of yuv420p cut the raw stream wrong (gray BEFORE crop). Added --sheet contact sheets (ci-shots) + FLTL timelines; tools/test_fl_analyze.sh OK

## Do this next
BY1b: dispatch emulator.yml sections=fl shots=true on main; read RESULTS (FL lines + tl), then git fetch ci-shots (fl-sheet-*.png) and LOOK; then design fixes F1 (shade) F2 (wake). Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ca1a23e2 ckpt 1209: acct-2 heartbeat ARMED trig_01JYEUXs7JosHyg85f2T1oCK (cron 44 */2, bound to 
  6e3af8cb ckpt 1208: TAKEOVER (account 2, branch ccr-91599ec3-za40s7; acct-1 session cut off, las
  861a3526 ckpt 1207: BY1b: fl BEFORE run #563 id 43273214570 dispatched 17:48Z on fcc27808; fl go
  fcc27808 ckpt 1206: BY1b: section fl written (films: wakeH1/2 wakeL shade shadeX pad tap + menuL
  59478275 ckpt 1205: BY1b: tools/fl_analyze.py (selftest OK) + section fl design in research/BY1_
  4c088a33 ckpt 1204: BY1-F: 4 code findings (lock patch over open shade, dark-rest at wake, fastD
  b83f8177 ckpt 1203: BY1 written into TASKS (Tj 17:26Z: perf review + lock/home patch flicker fil
  44afb137 ckpt 1202: BX1 DONE: v0.9.7 (code 46) RELEASED+RECORDED (android.yml #48 id 43139795817
  30021942 ckpt 1201: BX1 emulator #562 id 43137670443 (base,legacy,x on 16b47b65, product code ==
  3ba1ec6f ckpt 1200: gated v0.9.7 (code 46) and pushed it: the handoff self-test, the full unit s
```

(12 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
