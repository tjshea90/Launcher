# CHECKPOINT 1189 — read me first, then TASKS.md

**Written:** 2026-11-11T18:58:03Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `7f982df2` (this checkpoint is the commit after it)

## Just done
Docs for the adaptive icon (TASKS BW1b/d, CHANGES, MUSIC_ICON, CLAUDE lesson); SS7 polls 20 s. Waiting: mu shots=true on a0077f5d (dispatched 18:56Z), #559 o,last on head (id 43081771429)

## Do this next
1) get the mu run id (list_workflow_runs in_progress; head_sha a0077f5d..), read INFO 'MU icon:' (ratio ~0.715?) + all FAILs; git fetch origin ci-shots, extract to scratchpad/shots_mu2, LOOK at mud-01-drawer + crop; 2) read #559 (o,last): green = O4/O1 were flakes (tag #558 green); 3) tick TASKS BW1d/BV1d-h, fill V096 draft markers, HANDOFF 0t; 4) bump app/build.gradle.kts to 45 / 0.9.6, bash ship.sh, android.yml full_build=true on main, get_release_by_tag (3 APKs), record-release, answer Tj. Keep main == tip (git ls-remote origin refs/heads/main). Heartbeat trig_01GVcBRvTAzqf97UhMTJszab

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  bf5d5194 ckpt 1188: MAIN WAS STALE 18:33-18:55Z: the other account's heartbeat session pushed 2 
  9886cd58 ckpt 1187: #557 ss shots on head (id 43080291365) GREEN (SS7 passes; failed in #549/#55
  290b6533 ckpt 1186: Music icon is now ADAPTIVE (white bg + speaker fg, 0.477 of layer = 71.5% of
  d3d0554f ckpt 1185: #555 o,last (id 43077433975): 10 PASS 3 FAIL = O4 x2 + emulator went offline
  6e343425 ckpt 1184: #554 mu (id 43077282616, a2ba557d, job 130720504597): 28 PASS 1 FAIL = MU-21
  0cdab391 ckpt 1183: In flight: #554 mu id 43077282616 (a2ba557d, shots), #555 o,last id 43077433
  50b5c78c ckpt 1182: rb solo #552 (id 43076216494) GREEN 15 PASS (RB4 was the flake); ss solo #55
  2f1d6246 ckpt 1181: #550 (id 43071819655, ca64c7f9) cancelled at 33 min (stalled in o after FAIL
  a2ba557d ckpt 1180: MU-14b: drawer opening retried until Contacts is listed (run 551 picture sho
  db15ad86 ckpt 1179: #551 (id 43074338064, f1939f2d, mu shots): 27 PASS 1 FAIL = MU-14b only; MU-
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
