# CHECKPOINT 1236 — read me first, then TASKS.md

**Written:** 2026-11-12T21:30:17Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `8049eafb` (this checkpoint is the commit after it)

## Just done
BY1b seq fix: short seq lines were dropped by read (no newline; #567 menuW1 had none); fast open/skip log lines get t=; seq shows 'fast open age=N ms'. LOW2 fixed: ReturnReset.targetForAction (unchecked stored none -> one sync scan in openApp/startTargetBehindLock)

## Do this next
suite+lint running (scratchpad full2.log); then dispatch fl run to see menuW1 'fast open age' + 'fade end first='; lock reviewer pending. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  80649696 ckpt 1235: BZ1a/b DONE (VPN subagent): SLOT_VPN in RadioIconHold.SLOTS, texts, scenario
  7f6ca49f ckpt 1234: perf-batch reviewer DONE: no HIGH/MED; LOW1 Mode home-press history in-memor
  c131378e ckpt 1233: #567 id 43306712313 (fl, 2f944bae: F1b+B1-B4+C2+D1+subagent batch): 18 PASS 
  b3161105 ckpt 1232: BZ1a/b delegated 21:15Z to a subagent: RadioIconHold.SLOT_VPN='vpn' in SLOTS
  78b5914b ckpt 1231: BZ1 (Tj 21:10Z, new request): hide the VPN key icon everywhere except unlock
  9c3350bd ckpt 1230: full suite on combined tree: 1034 tests 0 fail, lint clean. Run #567 id 4330
  2f944bae ckpt 1229: perf subagent DONE (HEAD f858cf55): A4 RecentsPolicy.needsTarget early retur
  63fd15b0 ckpt 1228: BY1 findings doc updated with #566 (F1/F3 fixed, menuW1 lag remains, F1b sha
  cd66faf0 ckpt 1227: BY1c F1b: #566 picture fl-shadeoff-wake = QS panel still OPEN after screen o
  89899c99 ckpt 1226: #566 id 43302204570 (fl, 6973c044, F1+F3): 18 PASS 1 FAIL. F1 FIXED: shade f
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
