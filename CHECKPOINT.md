# CHECKPOINT 962 — read me first, then TASKS.md

**Written:** 2026-11-10T11:51:26Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `f13b817` (this checkpoint is the commit after it)

## Just done
b6 #516 (id 42809367693, head bbc5283): 20 PASS 1 FAIL = BQ3 custom close 3 STUCK-shown = TEST PREMISE, not the app: pic 57 (after BN8b drag) = PIN pad up; pic 58 (after close-3 BACK) = QS panel OPEN with the cover correctly over the pencil; timeline: hid at finger down +96ms, SHOWN 221 ms after the BACK (bouncer dismissed, panel back). Closes 1,2 hidden 155/202 ms (quickstep 160/196). Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE

## Do this next
b6 custom flow: before 'BQ3 marker close 3' add one BACK (dismisses the PIN pad the drag brings up) + sleep 2; rerun b6 solo; tick TASKS BQ3; HANDOFF 0q note; send Tj the v0.9.2 message (BQ5, draft research/V092_RELEASE_MESSAGE_DRAFT_2026-11-10.md); send_later read of the rerun; delete heartbeat after that.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  f13b817 ckpt 961: Run in flight: b6 solo shots #516 id 42809367693 head bbc5283 since 11:40:20Z 
  bbc5283 ckpt 960: b6 #515 (42807044513, 20 PASS 1 FAIL): only BQ3 custom close 3 = STUCK-shown (
  4a046bc ckpt 957: v0.9.2 RELEASED (android.yml run 43 id 42807528403 on 25a184a, published 11:29
  0a66bfd ckpt 954: v0.9.2 gated+pushed (ckpt 953, head 25a184a = main); android.yml full_build DI
  25a184a ckpt 953: gated v0.9.2 (code 41) and pushed it: the handoff self-test, the full unit sui
  c2c75d4 ckpt 952: pre-ship: BQ1a/BQ2: the lock-screen dimming is measured once per wallpaper (li
  bb453ba ckpt 950: b6 BQ3 check now LOG-based (bq3_close_verdict: markers close/open, last 'PENCI
  4996080 ckpt 947: Run #514 b6 solo id 42805284984 head 320582e: 19 PASS 2 FAIL = only 'BQ3 close
  585216b ckpt 943: Run in flight: #514 b6 solo shots id 42805284984 head 320582e since 11:10:29Z 
  320582e ckpt 942: b6 scenario: pen_cover_of stdin bug fixed (adb shell pidof inside a pipe swall
```
