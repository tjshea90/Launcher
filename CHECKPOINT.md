# CHECKPOINT 1187 — read me first, then TASKS.md

**Written:** 2026-11-11T18:54:11Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `b096ca6b` (this checkpoint is the commit after it)

## Just done
#557 ss shots on head (id 43080291365) GREEN (SS7 passes; failed in #549/#553, tag #556 green): SS7 now polls up to 20 s for the real Settings (cold start). #558 o,last on TAG v0.9.5 (id 43080297232) GREEN. Head o,last rerun dispatched 18:5xZ

## Do this next
dispatch mu shots=true on main (ci-shots already fetched to scratchpad/shots557); read it: INFO MU icon (speaker ratio ~0.715), mud-01-drawer picture; read the head o,last rerun. Heartbeat trig_01GVcBRvTAzqf97UhMTJszab

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  290b6533 ckpt 1186: Music icon is now ADAPTIVE (white bg + speaker fg, 0.477 of layer = 71.5% of
  d3d0554f ckpt 1185: #555 o,last (id 43077433975): 10 PASS 3 FAIL = O4 x2 + emulator went offline
  6e343425 ckpt 1184: #554 mu (id 43077282616, a2ba557d, job 130720504597): 28 PASS 1 FAIL = MU-21
  0cdab391 ckpt 1183: In flight: #554 mu id 43077282616 (a2ba557d, shots), #555 o,last id 43077433
  50b5c78c ckpt 1182: rb solo #552 (id 43076216494) GREEN 15 PASS (RB4 was the flake); ss solo #55
  2f1d6246 ckpt 1181: #550 (id 43071819655, ca64c7f9) cancelled at 33 min (stalled in o after FAIL
  a2ba557d ckpt 1180: MU-14b: drawer opening retried until Contacts is listed (run 551 picture sho
  db15ad86 ckpt 1179: #551 (id 43074338064, f1939f2d, mu shots): 27 PASS 1 FAIL = MU-14b only; MU-
  6846e2cc ckpt 1178: BW1: MU-21 (tools/music_icon_check.py, numbers from Tj's drawer; synthetic +
  a7a89606 ckpt 1177: #549 (id 43071814060, ca64c7f9): 113 PASS 2 FAIL = RB4 + SS7 + INFO NATIVE C
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
