# CHECKPOINT 960 — read me first, then TASKS.md

**Written:** 2026-11-10T11:40:02Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `0cb99c5` (this checkpoint is the commit after it)

## Just done
b6 #515 (42807044513, 20 PASS 1 FAIL): only BQ3 custom close 3 = STUCK-shown (closes 1,2 hidden in 135/118 ms; quickstep ok). Added pen_tl timeline + pictures 57/58 to b6 (test only). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
Dispatch emulator.yml b6 shots=true on branch ccr-5ef85f7a-ngtgj1, read the 4 'cover timeline' INFO lines + pictures 57-after-drag-custom / 58-after-close3-custom (git fetch origin ci-shots). Cover really up after drag+BACK = product bug (v0.9.3); else test premise. THEN send Tj the v0.9.2 message (BQ5).

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4a046bc ckpt 957: v0.9.2 RELEASED (android.yml run 43 id 42807528403 on 25a184a, published 11:29
  0a66bfd ckpt 954: v0.9.2 gated+pushed (ckpt 953, head 25a184a = main); android.yml full_build DI
  25a184a ckpt 953: gated v0.9.2 (code 41) and pushed it: the handoff self-test, the full unit sui
  c2c75d4 ckpt 952: pre-ship: BQ1a/BQ2: the lock-screen dimming is measured once per wallpaper (li
  bb453ba ckpt 950: b6 BQ3 check now LOG-based (bq3_close_verdict: markers close/open, last 'PENCI
  4996080 ckpt 947: Run #514 b6 solo id 42805284984 head 320582e: 19 PASS 2 FAIL = only 'BQ3 close
  585216b ckpt 943: Run in flight: #514 b6 solo shots id 42805284984 head 320582e since 11:10:29Z 
  320582e ckpt 942: b6 scenario: pen_cover_of stdin bug fixed (adb shell pidof inside a pipe swall
  1857c14 ckpt 940: Run #513 b6,n 26 PASS 5 FAIL (id 42802572154, head da7e710): BK1b O1-O3 'shown
  e067780 ckpt 938: Run in flight: #513 b6,n shots=true id 42802572154 head da7e710 since 10:50:52
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
