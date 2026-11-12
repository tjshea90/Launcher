# CHECKPOINT 1241 — read me first, then TASKS.md

**Written:** 2026-11-12T21:49:52Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `ea8d0d3b` (this checkpoint is the commit after it)

## Just done
#568 id 43310928575 (fl, 6381a278): 18 PASS 1 FAIL (FL2 menuW1 flashA 2 = 33 ms). seq: W1 fast open age=2 ms, fade lag 3 ms, first draw 19 ms = OUR side fast (W2: lag 35, first 86, no flash) -> residual W1 lag is stock-side timing. NEW: W2 + menuS1 seq 'menu closed' from a 163-174 ms node scan WHILE the menu was open, re-opened by the 2nd Phone_options event 0.2 s later (patch at bright rest look over the dimmed menu = a flash the FL2 check does not count)

## Do this next
find the scan's close rule (evaluateScan -> patchShow(false)); close only when the dialog WINDOW is gone; then reviewer fixes MED1/LOW2-6. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  ea8d0d3b ckpt 1240: lock reviewer DONE (BY1c diff): 1 MED + 5 LOW. MED1 onServiceConnected never
  d05deb09 ckpt 1239: PERF_REVIEW status section written (fixes done + review verdicts). gate.sh v
  95dd29c0 ckpt 1238: suite+lint on 6381a278: 1035 tests 0 fail, lint OK. fl run #568 id 433109285
  13d4e526 ckpt 1237: dispatched emulator fl (shots) on main 6381a278 at 21:30Z to time menuW1 (fa
  6381a278 ckpt 1236: BY1b seq fix: short seq lines were dropped by read (no newline; #567 menuW1 
  80649696 ckpt 1235: BZ1a/b DONE (VPN subagent): SLOT_VPN in RadioIconHold.SLOTS, texts, scenario
  7f6ca49f ckpt 1234: perf-batch reviewer DONE: no HIGH/MED; LOW1 Mode home-press history in-memor
  c131378e ckpt 1233: #567 id 43306712313 (fl, 2f944bae: F1b+B1-B4+C2+D1+subagent batch): 18 PASS 
  b3161105 ckpt 1232: BZ1a/b delegated 21:15Z to a subagent: RadioIconHold.SLOT_VPN='vpn' in SLOTS
  78b5914b ckpt 1231: BZ1 (Tj 21:10Z, new request): hide the VPN key icon everywhere except unlock
```
