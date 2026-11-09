# CHECKPOINT 783 — read me first, then TASKS.md

**Written:** 2026-11-09T04:33:02Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `1009867` (this checkpoint is the commit after it)

## Just done
444 pt solo cancelled at 20 min (emulator died in phase H: 3 FAIL = PT timing x2 + BE1 with 'no PATCH lines', same dying-emulator sign as 442; the same code passed those PT timing checks in 441). Fresh pt solo = run on ref main dispatched 04:31:30 (id: list_workflow_runs emulator.yml branch main perPage 1). versionCode 38 / 0.8.13 set; ship.sh running, log in scratchpad ship.log (SHIP_EXIT= line)

## Do this next
1) read scratchpad ship.log (SHIP_EXIT=0 and pushed; origin main == HEAD). 2) when the pt run on main is SUCCESS: android.yml full_build=true ref main, +3 min get_release_by_tag v0.8.13, bash tools/record-release.sh v0.8.13 note, link to Tj. 3) if pt fails only the PT timing yardsticks again: run pt solo with ref v0.8.12 and compare; accept only with that evidence in TASKS. 4) then tick BN2-BN4 FIX boxes (tests: HomePatchRulesTest, PencilRulesTest, PowerMenuRuleTest, emulator HP14/BN4) and start BN1 M0

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  53ea1f0 ckpt 782: GATE v0.8.13: ALL GREEN except 444 still running: 445 last, 447 e1,n,ac (id 42
  d6bdfd6 ckpt 781: 448 hp SOLO = SUCCESS (HP14 green). 449 fp,last (fixed scenario, head 4b0cbfc)
  55e5668 ckpt 780: TASKS: gate story v0.8.13 written (441/442/443 triage, fixes: gate.sh solo pt+
  7adb7fa ckpt 779: 446 fp,last FAILED = TEST order problem (picture: lock screen + PIN pad up, st
  437946a ckpt 778: gate triage: 445 last SOLO = SUCCESS (443's last FAIL was chain-dependent); 44
  ae3a6e2 ckpt 777: tools/gate.sh: pt and rg now SOLO runs (+ bins grow when a run would pass 40 m
  19360e7 ckpt 776: gate 6702e72 read: 441=4 FAIL all in pt (BC2 closetap + 3 PB, patch -6.6 level
  cfa0b53 ckpt 775: gate 6702e72: run 441 (x1,v,pt,rb) FAILED, 443 (b6,o,ad,ae,hf,fp,last) FAILED,
  27833a7 ckpt 774: Tj: 'Ship v0.8.13 when the gate is green' written into TASKS; gate 441-443 on 
  2229c7c ckpt 773: BN1c: build plan written (research/SETTINGS_BUILD_PLAN_2026-11-09.md: own row 
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
