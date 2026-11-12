# CHECKPOINT 1227 — read me first, then TASKS.md

**Written:** 2026-11-12T20:57:14Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `371f588f` (this checkpoint is the commit after it)

## Just done
BY1c F1b: #566 picture fl-shadeoff-wake = QS panel still OPEN after screen off+wake and the patch rested OVER it (my SCREEN_OFF reset was wrong). Fix: no reset at SCREEN_OFF; lockPrepare (dark, +0.7 s, +6.5 s, wake) does a level look (shadeLevel) before refreshLayers; USER_PRESENT reset kept

## Do this next
subagent report -> review diff -> :app:testDebugUnitTest + lintDebug -> dispatch fl shots again (expect shadeOff picture without patch, menuW1 seq line 'fade end lock first=N'). Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  89899c99 ckpt 1226: #566 id 43302204570 (fl, 6973c044, F1+F3): 18 PASS 1 FAIL. F1 FIXED: shade f
  0f864088 ckpt 1225: BY1b: flend prints 'FL <film> seq:' = the patch's PATCH decisions in film se
  e2e2f237 ckpt 1224: BY1a D1 coded (NOT compiled yet): dropWallpaper() = our wallpaperBmp + Wallp
  866f0343 ckpt 1223: BY1a perf in TAS (NOT yet compiled; subagent owns Gradle): B1 EventNoise.may
  599db918 ckpt 1222: BY1a: perf-fix SUBAGENT launched 20:41Z (A4 RecentsPolicy early return, A3 h
  ebac9824 ckpt 1221: BY1c: run #566 id 43302204570 (fl, shots=true) dispatched 20:37:49Z on 6973c
  6973c044 ckpt 1220: BY1c F1 coded: LockShade (pane titles: SUI cct 0x8 'Quick settings.'/'Notifi
  1842365c ckpt 1219: BY1a: all 4 perf reviews DONE: research/PERF_REVIEW_2026-11-12_{A,B,C,D}.md.
  959a2388 ckpt 1218: probe #565 id 43298041314: 16 PASS 3 FAIL. PANE TITLES CONFIRMED: shade film
  7162ba3c ckpt 1217: BY1c F3: app compiles; PatchRestTest 39/39 green (5 new padCancelled). resea
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
