# CHECKPOINT 785 — read me first, then TASKS.md

**Written:** 2026-11-09T04:34:54Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `3ea693a` (this checkpoint is the commit after it)

## Just done
ship.sh v0.8.13 (code 38) DONE: handoff test, unit suite green, pushed; HEAD=branch=main 3ea693a. Waiting ONLY for the fresh pt solo on ref main: run 450 id 42562320225 (started 04:31:37, ETA 04:46). android.yml NOT dispatched yet

## Do this next
when run 450 (id 42562320225) is SUCCESS (actions_get get_workflow_run): dispatch android.yml full_build=true ref main (mcp__github__actions_run_trigger run_workflow), +3 min get_release_by_tag v0.8.13, bash tools/record-release.sh v0.8.13 with the note from ship.log, link to Tj as plain text. If 450 fails only PT timing yardsticks: pt solo with ref v0.8.12 to compare. Then tick BN2-BN4 boxes, start BN1 M0 (research/SETTINGS_BUILD_PLAN_2026-11-09.md; AppVisibility.ALWAYS_SHOWN_OWN_COMPONENTS is the hook for the drawer entry; switch pattern = policy class with KEY + defaultOn(debuggable))

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3ea693a ckpt 784: gated v0.8.13 (code 38) and pushed it: the handoff self-test, the full unit su
  b466126 ckpt 783: 444 pt solo cancelled at 20 min (emulator died in phase H: 3 FAIL = PT timing 
  53ea1f0 ckpt 782: GATE v0.8.13: ALL GREEN except 444 still running: 445 last, 447 e1,n,ac (id 42
  d6bdfd6 ckpt 781: 448 hp SOLO = SUCCESS (HP14 green). 449 fp,last (fixed scenario, head 4b0cbfc)
  55e5668 ckpt 780: TASKS: gate story v0.8.13 written (441/442/443 triage, fixes: gate.sh solo pt+
  7adb7fa ckpt 779: 446 fp,last FAILED = TEST order problem (picture: lock screen + PIN pad up, st
  437946a ckpt 778: gate triage: 445 last SOLO = SUCCESS (443's last FAIL was chain-dependent); 44
  ae3a6e2 ckpt 777: tools/gate.sh: pt and rg now SOLO runs (+ bins grow when a run would pass 40 m
  19360e7 ckpt 776: gate 6702e72 read: 441=4 FAIL all in pt (BC2 closetap + 3 PB, patch -6.6 level
  cfa0b53 ckpt 775: gate 6702e72: run 441 (x1,v,pt,rb) FAILED, 443 (b6,o,ad,ae,hf,fp,last) FAILED,
```
