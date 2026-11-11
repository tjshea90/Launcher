# CHECKPOINT 1192 — read me first, then TASKS.md

**Written:** 2026-11-11T19:12:03Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `82dd95c3` (this checkpoint is the commit after it)

## Just done
gated v0.9.6 (code 45) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.6 "BV1 the music app Eleven in the custom drawer, never album art; BW1 it is called Music with the speaker icon"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  9bd909bf ckpt 1191: #561 mu GREEN 29 PASS 0 FAIL (id 43082288224, job 130737757079, a0077f5d, ad
  d4c1bdb1 ckpt 1190: #559 (id 43081771429, o,last on head aa372ce6) GREEN = O1/O4/emulator-offlin
  7de38227 ckpt 1189: Docs for the adaptive icon (TASKS BW1b/d, CHANGES, MUSIC_ICON, CLAUDE lesson
  bf5d5194 ckpt 1188: MAIN WAS STALE 18:33-18:55Z: the other account's heartbeat session pushed 2 
  9886cd58 ckpt 1187: #557 ss shots on head (id 43080291365) GREEN (SS7 passes; failed in #549/#55
  290b6533 ckpt 1186: Music icon is now ADAPTIVE (white bg + speaker fg, 0.477 of layer = 71.5% of
  d3d0554f ckpt 1185: #555 o,last (id 43077433975): 10 PASS 3 FAIL = O4 x2 + emulator went offline
  6e343425 ckpt 1184: #554 mu (id 43077282616, a2ba557d, job 130720504597): 28 PASS 1 FAIL = MU-21
  0cdab391 ckpt 1183: In flight: #554 mu id 43077282616 (a2ba557d, shots), #555 o,last id 43077433
  50b5c78c ckpt 1182: rb solo #552 (id 43076216494) GREEN 15 PASS (RB4 was the flake); ss solo #55
```

(11 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
