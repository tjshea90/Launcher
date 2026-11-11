# CHECKPOINT 1172 — read me first, then TASKS.md

**Written:** 2026-11-11T17:48:53Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `d14fe2ec` (this checkpoint is the commit after it)

## Just done
Run 546 (id 43069863937, head 0701e089, mu shots): 23 PASS 1 FAIL = MU-12 + a NATIVE CRASH (libhwui, com.tj.launcher; flake family). MU-2 CONTROL PASSES: upstream Eleven (album-art code, baseline db65f024) shows the magenta cover on songs tab 2544 px, albums tab 60187, player 374545, shade 133205; OUR app 0 px in 19 pictures; pictures: scratchpad shots546, sheet-control.png vs sheet-ours.png (placeholders green note/letter tiles); TIME mu 290 s. Review fixes in (ckpt 1171); MU-12a/13b/14b new, NOT yet run

## Do this next
1) dispatch emulator.yml ref main sections=mu shots=true on the current tip (check git ls-remote origin refs/heads/main = tip first); read RESULTS (list_workflow_runs emulator.yml main workflow_dispatch perPage 1 -> get_job_logs run_id failed_only tail 60); a FAIL right after an INFO NATIVE CRASH = rerun solo. 2) tick TASKS BV1e/BV1f; update HANDOFF 0t; CLAUDE.md lessons. 3) gate: bash tools/gate.sh v0.9.5 (runs e1,cs / x1,ac,rb,ss / base,legacy,n,o,last + mu solo), then bash ship.sh 'BV1 music app' (v0.9.6), android.yml full_build=true, check Release has Launcher + Eleven + PhoneGuide APKs, record-release.sh, answer Tj last (draft research/V096_RELEASE_MESSAGE_DRAFT_2026-11-11.md not yet written). Heartbeat trig_01GVcBRvTAzqf97UhMTJszab; never update_trigger/delete_trigger

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4015d477 ckpt 1171: BV1 drawer rule hardened after the review: auto_done = pkg@firstInstallTime 
  ca99a8b5 ckpt 1170: Review of the drawer rule (agent a44d508, no HIGH): update in place does NOT
  0701e089 ckpt 1169: BV1e: mu control = upstream Eleven (baseline db65f024 + port fix, built by e
  67f04fc9 ckpt 1168: Run 545 (id 43066812780, head ae8cb7e1, mu shots=true): 23 PASS 1 FAIL = MU-
  ae8cb7e1 ckpt 1167: BV1: audit items 3/4/5 done (dead BitmapWithColors ctor + Palette call remov
  cde47d6c ckpt 1166: BV1: audit report saved (research/ELEVEN_AUDIT_2026-11-11.md: no HIGH; TO DO
  25bb7e5a ckpt 1165: Run 544 (id 43063412826, head ff924adf, sections=mu shots=true): 18 PASS 4 F
  ff924adf ckpt 1164: Run 543 (id 43062112175, mu on 21315c73) FAILED before the scenario: step Im
  fa3436e5 ckpt 1163: BV1: TASKS BV1a/b/c ticked with proof; mu extended (artist/album popup menus
  a6de0e7d ckpt 1162: BV1c/e/d: guard test 10 tests green + AppVisibilityTest 9 green (IoUtils dea
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
