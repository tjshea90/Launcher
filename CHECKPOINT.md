# CHECKPOINT 953 — read me first, then TASKS.md

**Written:** 2026-11-10T11:26:25Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `c2c75d4` (this checkpoint is the commit after it)

## Just done
gated v0.9.2 (code 41) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.2 "BQ1a/BQ2: the lock-screen dimming is measured once per wallpaper (light look only, the dark look reuses it, a run counts when it starts); BQ1b: the pencil cover is hidden the instant a finger touches the open panel"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  c2c75d4 ckpt 952: pre-ship: BQ1a/BQ2: the lock-screen dimming is measured once per wallpaper (li
  bb453ba ckpt 950: b6 BQ3 check now LOG-based (bq3_close_verdict: markers close/open, last 'PENCI
  4996080 ckpt 947: Run #514 b6 solo id 42805284984 head 320582e: 19 PASS 2 FAIL = only 'BQ3 close
  585216b ckpt 943: Run in flight: #514 b6 solo shots id 42805284984 head 320582e since 11:10:29Z 
  320582e ckpt 942: b6 scenario: pen_cover_of stdin bug fixed (adb shell pidof inside a pipe swall
  1857c14 ckpt 940: Run #513 b6,n 26 PASS 5 FAIL (id 42802572154, head da7e710): BK1b O1-O3 'shown
  e067780 ckpt 938: Run in flight: #513 b6,n shots=true id 42802572154 head da7e710 since 10:50:52
  f6f2eca ckpt 936: CLAUDE.md lessons v0.9.2 written; b6,n shots run dispatched 10:50Z on branch (
  da7e710 ckpt 934: b6 scenario fixed after #512 read (BN8b=dump restarted service; control=stale 
  c175e39 ckpt 930: BQ3 SCREENSHOTS #512 (42799451859): Quickstep P2 lock screen has the QS panel 
```
