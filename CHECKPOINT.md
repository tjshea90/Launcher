# CHECKPOINT 1240 — read me first, then TASKS.md

**Written:** 2026-11-12T21:45:36Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `8f0b2f49` (this checkpoint is the commit after it)

## Just done
lock reviewer DONE (BY1c diff): 1 MED + 5 LOW. MED1 onServiceConnected never looks at the panel (rebind with QS open -> patch rests over it): fix noteShade(shadeLevel) before startPatchWatch. LOW2 SCREEN_ON look blocks every wake (skip if dark look done since off). LOW3 unlocked pane titles leak OPEN into next lock (drop OPEN unless keyguardLocked). LOW4 occlusion (dialer/camera over keyguard) pre-existing, F3 sooner: padCancel needs no app window. LOW5 autoShot can photograph open panel (ready && !lockShadeOpen). LOW6 test names. Q for Tj: menu over open QS panel

## Do this next
fix MED1+LOW2-6 in TAS; read #568 id 43310928575 menuW1 seq. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  d05deb09 ckpt 1239: PERF_REVIEW status section written (fixes done + review verdicts). gate.sh v
  95dd29c0 ckpt 1238: suite+lint on 6381a278: 1035 tests 0 fail, lint OK. fl run #568 id 433109285
  13d4e526 ckpt 1237: dispatched emulator fl (shots) on main 6381a278 at 21:30Z to time menuW1 (fa
  6381a278 ckpt 1236: BY1b seq fix: short seq lines were dropped by read (no newline; #567 menuW1 
  80649696 ckpt 1235: BZ1a/b DONE (VPN subagent): SLOT_VPN in RadioIconHold.SLOTS, texts, scenario
  7f6ca49f ckpt 1234: perf-batch reviewer DONE: no HIGH/MED; LOW1 Mode home-press history in-memor
  c131378e ckpt 1233: #567 id 43306712313 (fl, 2f944bae: F1b+B1-B4+C2+D1+subagent batch): 18 PASS 
  b3161105 ckpt 1232: BZ1a/b delegated 21:15Z to a subagent: RadioIconHold.SLOT_VPN='vpn' in SLOTS
  78b5914b ckpt 1231: BZ1 (Tj 21:10Z, new request): hide the VPN key icon everywhere except unlock
  9c3350bd ckpt 1230: full suite on combined tree: 1034 tests 0 fail, lint clean. Run #567 id 4330
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
