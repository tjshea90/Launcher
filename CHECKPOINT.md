# CHECKPOINT 1199 — read me first, then TASKS.md

**Written:** 2026-11-12T01:57:07Z · **tests:** all 10 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-393c88ba-k4wifl` · **builds on:** `16b47b65` (this checkpoint is the commit after it)

## Just done
BX1: emulator smoke #562 id 43137670443 on 16b47b65 (base,legacy,x) dispatched 01:54Z; ship.sh v0.9.7 gates running locally

## Do this next
read #562 (list_workflow_jobs then get_job_logs tail 60); when ship.sh logged SHIP_EXIT=0 trigger android.yml full_build=true ref main; then get_release_by_tag v0.9.7, record-release.sh, tick BX1 in TASKS, HANDOFF 0t, answer Tj

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  16b47b65 ckpt 1198: BX1 code done: Quickstep -> Gboard (KeyboardPolicy.pickGboard/noTargetStatus
  89ac6020 ckpt 1197: BX1 written into TASKS (Tj: Quickstep keyboard Gboard instead of SwiftKey, c
  c4d6429b ckpt 1196: heartbeat 20:13Z: no other session working (newest main commit = own OUTBOX 
  ff35152f ckpt 1195: HANDOFF 0t rewritten as v0.9.6 RELEASED (run #47, BUILDLOG 09546785); answer
  46c47738 ckpt 1194: v0.9.6 (code 45) RELEASED+RECORDED: android.yml #47 id 43084983118 green on 
  bd61fbea ckpt 1193: SHIPPED v0.9.6 (code 45) gate green (e2302114, main == tip); android.yml ful
  e2302114 ckpt 1192: gated v0.9.6 (code 45) and pushed it: the handoff self-test, the full unit s
```
