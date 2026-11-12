# CHECKPOINT 1220 — read me first, then TASKS.md

**Written:** 2026-11-12T20:37:30Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `3317733e` (this checkpoint is the commit after it)

## Just done
BY1c F1 coded: LockShade (pane titles: SUI cct 0x8 'Quick settings.'/'Notification shade.'/'Quick settings editor.' = OPEN, 'Lock screen.' = CLOSED; SystemUI's own strings + English), lockShadeEvent/noteShade/shadeLevel (level look at touchBack), patchShow lock vis needs shade hold (open = away, no timer; closed + 400 ms settle); reset at SCREEN_OFF/USER_PRESENT. LockShadeTest 5 + PatchRestTest green. fl: + shadeSlow + shadeOff diagnostic films

## Do this next
dispatch emulator.yml sections=fl shots=true on main (expect shade flashA<=1, menuP1/P2 shot 0, 'PATCH pin pad cancelled'/'PATCH shade open' in paths); meanwhile write research/PERF_REVIEW_2026-11-12.md merge. Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  1842365c ckpt 1219: BY1a: all 4 perf reviews DONE: research/PERF_REVIEW_2026-11-12_{A,B,C,D}.md.
  959a2388 ckpt 1218: probe #565 id 43298041314: 16 PASS 3 FAIL. PANE TITLES CONFIRMED: shade film
  7162ba3c ckpt 1217: BY1c F3: app compiles; PatchRestTest 39/39 green (5 new padCancelled). resea
  a9d7948b ckpt 1216: BY1c F3 coded: PatchRest.padCancelled (PAD_CANCEL_MS 700, PAD_SIGN_QUIET_MS 
  7d0f360c ckpt 1215: BY1b: probe run #565 id 43298041314 (fl, shots) dispatched 20:12Z on ef78ded
  ef78dede ckpt 1214: BY1b: LOOKED at #564 sheets: shade = patch block over the open white QS pane
  c25a9ce5 ckpt 1213: #564 id 43294929647 (fl, fixed analyzer, b3b0c4d7): 17 PASS 2 FAIL. FAIL FL1
  9577dc4d ckpt 1212: BY1a: 4 read-only perf reviewers launched 20:00Z (A startup+drawer, B a11y s
  3f825827 ckpt 1211: BY1b: run #564 id 43294929647 (fl, shots=true) dispatched 19:54Z on b3b0c4d7
  b3b0c4d7 ckpt 1210: BY1b: fl_analyze fixed (2 test bugs that made #563 unreadable): (1) VFR film
```

(4 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
