# CHECKPOINT 838 — read me first, then TASKS.md

**Written:** 2026-11-09T22:55:25Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `5abde52` (this checkpoint is the commit after it)

## Just done
gated v0.9.0 (code 39) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.0 "v0.9.0: Settings look-alike in the custom drawer (BN1): Battery, Display, Sound, Storage, Accessibility, System, About tablet, Text-to-speech output; look-only where the app has no permission; own crash fuse"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a79ee8e ckpt 837: reruns #475 42715672216 main, #476 42715676675 branch, #477 42715681398 main+l
  3da7cb8 ckpt 836: #474 base,legacy ONE run (306152a): 40P/4F recents,W5x2,W3b + THREE native cra
  098143a ckpt 835: legacy SOLO fails reset2 by design (needs base in the same run, HANDOFF 0e/RS1
  0f211eb ckpt 834: Read-only audit subagent started 22:29Z (BN1m-2: look-alike state words vs Tj 
  299085c ckpt 833: #470 base on TAG v0.8.13: 28 PASS 1 FAIL + native crash (released tag crashes 
  a4583b1 ckpt 832: Baseline #470 id 42685125516 = base on tag v0.8.13; #471 id 42685347862 = base
  4a49b3f ckpt 831: Solo reruns: #466 base FAIL 4 (2 native crashes: launcher-loader, main), #467 
  e024dae ckpt 830: Solo reruns on eaeb47e: #466 42683210584 base, #467 42683216363 o, #468 426832
  eaeb47e ckpt 829: GATE v0.9.0 on 09215cc: #465 rg OK; #463 (42671984609) 5 FAIL legacy/lq; #464 
  c5dca2d ckpt 828: Gate attempt 2 of #463 (42671984609) and #464 (42671992301) RUNNING since 18:4
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
