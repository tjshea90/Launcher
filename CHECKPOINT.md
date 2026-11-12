# CHECKPOINT 1229 — read me first, then TASKS.md

**Written:** 2026-11-12T21:04:55Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `f858cf55` (this checkpoint is the commit after it)

## Just done
perf subagent DONE (HEAD f858cf55): A4 RecentsPolicy.needsTarget early return; A3 Mode lastHome/lastHome2 static volatile; C1 RecentsFilter label set on tj-recents-labels executor (stale never used, look waits for rebuild); A2/C13 TjHooks WeakRef once per Launcher instance + RecentsGuard task-id set + describe once + ReturnReset stored '' = known none; C6/C7 Features.differs/setStringAsyncIfChanged (KeyboardSwitch.say async; TileProfile APPLIED_KEY commit only on change). Its run: 1034 tests 0 fail, lint no new. Open (its notes): RecentsFilter:173 STATUS_KEY commit on main; targetPackage label/package race; RecentsGuard label not refreshed on target change; TAS trackDirty/updateHotspot targetPackage per window change

## Do this next
run full :app:testDebugUnitTest + :app:lintDebug on the combined tree; review subagent diff (git diff 6973c044..HEAD -- app ':!*TjAccessibilityService*'); dispatch fl shots. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  63fd15b0 ckpt 1228: BY1 findings doc updated with #566 (F1/F3 fixed, menuW1 lag remains, F1b sha
  cd66faf0 ckpt 1227: BY1c F1b: #566 picture fl-shadeoff-wake = QS panel still OPEN after screen o
  89899c99 ckpt 1226: #566 id 43302204570 (fl, 6973c044, F1+F3): 18 PASS 1 FAIL. F1 FIXED: shade f
  0f864088 ckpt 1225: BY1b: flend prints 'FL <film> seq:' = the patch's PATCH decisions in film se
  e2e2f237 ckpt 1224: BY1a D1 coded (NOT compiled yet): dropWallpaper() = our wallpaperBmp + Wallp
  866f0343 ckpt 1223: BY1a perf in TAS (NOT yet compiled; subagent owns Gradle): B1 EventNoise.may
  599db918 ckpt 1222: BY1a: perf-fix SUBAGENT launched 20:41Z (A4 RecentsPolicy early return, A3 h
  ebac9824 ckpt 1221: BY1c: run #566 id 43302204570 (fl, shots=true) dispatched 20:37:49Z on 6973c
  6973c044 ckpt 1220: BY1c F1 coded: LockShade (pane titles: SUI cct 0x8 'Quick settings.'/'Notifi
  1842365c ckpt 1219: BY1a: all 4 perf reviews DONE: research/PERF_REVIEW_2026-11-12_{A,B,C,D}.md.
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
