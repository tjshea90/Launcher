# CHECKPOINT 1200 — read me first, then TASKS.md

**Written:** 2026-11-12T01:57:50Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-393c88ba-k4wifl` · **builds on:** `3f0de92d` (this checkpoint is the commit after it)

## Just done
gated v0.9.7 (code 46) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.7 "BX1 Quickstep keyboard is now Gboard instead of SwiftKey (custom launcher unchanged); the Settings look-alike keeps Gboard unnamed like SwiftKey"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3f0de92d ckpt 1199: BX1: emulator smoke #562 id 43137670443 on 16b47b65 (base,legacy,x) dispatch
  16b47b65 ckpt 1198: BX1 code done: Quickstep -> Gboard (KeyboardPolicy.pickGboard/noTargetStatus
  89ac6020 ckpt 1197: BX1 written into TASKS (Tj: Quickstep keyboard Gboard instead of SwiftKey, c
  c4d6429b ckpt 1196: heartbeat 20:13Z: no other session working (newest main commit = own OUTBOX 
  ff35152f ckpt 1195: HANDOFF 0t rewritten as v0.9.6 RELEASED (run #47, BUILDLOG 09546785); answer
  46c47738 ckpt 1194: v0.9.6 (code 45) RELEASED+RECORDED: android.yml #47 id 43084983118 green on 
  bd61fbea ckpt 1193: SHIPPED v0.9.6 (code 45) gate green (e2302114, main == tip); android.yml ful
  e2302114 ckpt 1192: gated v0.9.6 (code 45) and pushed it: the handoff self-test, the full unit s
```
