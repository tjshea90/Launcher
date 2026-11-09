# CHECKPOINT 784 — read me first, then TASKS.md

**Written:** 2026-11-09T04:33:43Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `413d52c` (this checkpoint is the commit after it)

## Just done
gated v0.8.13 (code 38) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.8.13 "BN2 first side-key press covered on the custom home (the unlock's status-bar window no longer keeps the patch away), BN2 no tall menu flash after Home, BN4 no white box when the shade slides up, BN3 touch-away 1.5 s + lock-log lines naming each return"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  b466126 ckpt 783: 444 pt solo cancelled at 20 min (emulator died in phase H: 3 FAIL = PT timing 
  53ea1f0 ckpt 782: GATE v0.8.13: ALL GREEN except 444 still running: 445 last, 447 e1,n,ac (id 42
  d6bdfd6 ckpt 781: 448 hp SOLO = SUCCESS (HP14 green). 449 fp,last (fixed scenario, head 4b0cbfc)
  55e5668 ckpt 780: TASKS: gate story v0.8.13 written (441/442/443 triage, fixes: gate.sh solo pt+
  7adb7fa ckpt 779: 446 fp,last FAILED = TEST order problem (picture: lock screen + PIN pad up, st
  437946a ckpt 778: gate triage: 445 last SOLO = SUCCESS (443's last FAIL was chain-dependent); 44
  ae3a6e2 ckpt 777: tools/gate.sh: pt and rg now SOLO runs (+ bins grow when a run would pass 40 m
  19360e7 ckpt 776: gate 6702e72 read: 441=4 FAIL all in pt (BC2 closetap + 3 PB, patch -6.6 level
  cfa0b53 ckpt 775: gate 6702e72: run 441 (x1,v,pt,rb) FAILED, 443 (b6,o,ad,ae,hf,fp,last) FAILED,
  27833a7 ckpt 774: Tj: 'Ship v0.8.13 when the gate is green' written into TASKS; gate 441-443 on 
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
