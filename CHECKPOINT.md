# CHECKPOINT 1218 — read me first, then TASKS.md

**Written:** 2026-11-12T20:25:26Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-91599ec3-za40s7` · **builds on:** `be64b364` (this checkpoint is the commit after it)

## Just done
probe #565 id 43298041314: 16 PASS 3 FAIL. PANE TITLES CONFIRMED: shade film 1.68 'Quick settings.' (cct 0x8) .. 5.23 'Lock screen.' (cct 0x8) = flash end; PIN pad 'Pin unlock.' (cct 0x10); menu 'Phone options'. FAILs: shade flashA 138 (F1 again), menuW1 flashA 3 at fade start (0.47-0.52, lag), menuP2 shot 9: S0.68-0.72 + S1.48-1.60 MID-menu (2 fast opens)

## Do this next
BY1c: code F1 = LockShade.kind (pane titles via SystemUI resources + English) -> lockShadeOpen; patchShow lock branch vis needs !lockShadeOpen and shadeHold 350 ms after 'Lock screen.'; reset at SCREEN_OFF/USER_PRESENT; investigate menuP2 mid-menu Screenshot (showWithCause during open menu?). Heartbeat trig_01JYEUXs7JosHyg85f2T1oCK

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  7162ba3c ckpt 1217: BY1c F3: app compiles; PatchRestTest 39/39 green (5 new padCancelled). resea
  a9d7948b ckpt 1216: BY1c F3 coded: PatchRest.padCancelled (PAD_CANCEL_MS 700, PAD_SIGN_QUIET_MS 
  7d0f360c ckpt 1215: BY1b: probe run #565 id 43298041314 (fl, shots) dispatched 20:12Z on ef78ded
  ef78dede ckpt 1214: BY1b: LOOKED at #564 sheets: shade = patch block over the open white QS pane
  c25a9ce5 ckpt 1213: #564 id 43294929647 (fl, fixed analyzer, b3b0c4d7): 17 PASS 2 FAIL. FAIL FL1
  9577dc4d ckpt 1212: BY1a: 4 read-only perf reviewers launched 20:00Z (A startup+drawer, B a11y s
  3f825827 ckpt 1211: BY1b: run #564 id 43294929647 (fl, shots=true) dispatched 19:54Z on b3b0c4d7
  b3b0c4d7 ckpt 1210: BY1b: fl_analyze fixed (2 test bugs that made #563 unreadable): (1) VFR film
  ca1a23e2 ckpt 1209: acct-2 heartbeat ARMED trig_01JYEUXs7JosHyg85f2T1oCK (cron 44 */2, bound to 
  6e3af8cb ckpt 1208: TAKEOVER (account 2, branch ccr-91599ec3-za40s7; acct-1 session cut off, las
```

(14 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
