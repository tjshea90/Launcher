# CHECKPOINT 1184 — read me first, then TASKS.md

**Written:** 2026-11-11T18:41:32Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `66f61472` (this checkpoint is the commit after it)

## Just done
#554 mu (id 43077282616, a2ba557d, job 130720504597): 28 PASS 1 FAIL = MU-21 only; MU-14b now PASS, art probe 0 magenta in 19. Real drawer shows our speaker at ~62% of the plate (his 71.5%): Launcher3 wraps a circular legacy icon at <=64%

## Do this next
switch the music icon to an ADAPTIVE icon (white background + speaker foreground, speaker 0.477 of the 108dp layer = 71.5% of the plate): tools/make_music_icon.py writes mipmap-*/ic_launcher_foreground.png + mipmap-anydpi/ic_launcher.xml, MusicIdentityTest + tools/music_icon_check.py (scale-free + ratio check) follow; then mu shots again. #555 (43077433975) and #556 (ss on tag, 43077963673) still to read. Heartbeat trig_01GVcBRvTAzqf97UhMTJszab

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  0cdab391 ckpt 1183: In flight: #554 mu id 43077282616 (a2ba557d, shots), #555 o,last id 43077433
  50b5c78c ckpt 1182: rb solo #552 (id 43076216494) GREEN 15 PASS (RB4 was the flake); ss solo #55
  2f1d6246 ckpt 1181: #550 (id 43071819655, ca64c7f9) cancelled at 33 min (stalled in o after FAIL
  a2ba557d ckpt 1180: MU-14b: drawer opening retried until Contacts is listed (run 551 picture sho
  db15ad86 ckpt 1179: #551 (id 43074338064, f1939f2d, mu shots): 27 PASS 1 FAIL = MU-14b only; MU-
  6846e2cc ckpt 1178: BW1: MU-21 (tools/music_icon_check.py, numbers from Tj's drawer; synthetic +
  a7a89606 ckpt 1177: #549 (id 43071814060, ca64c7f9): 113 PASS 2 FAIL = RB4 + SS7 + INFO NATIVE C
  f1939f2d ckpt 1176: #548 e1+cs GREEN. mu: hand-hide now opened via Launcher control (am start of
  b7fac10b ckpt 1175: Run 547 (id 43071778397, head ca64c7f9, mu shots): 23 PASS 4 FAIL = MU-12a M
  203c9ead ckpt 1174: Tj BW1 (music app must be called Music with the speaker icon from his screen
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
