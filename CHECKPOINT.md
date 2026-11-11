# CHECKPOINT 1178 — read me first, then TASKS.md

**Written:** 2026-11-11T18:20:16Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-3e66a8c0-ruc8aa` · **builds on:** `14f47935` (this checkpoint is the commit after it)

## Just done
BW1: MU-21 (tools/music_icon_check.py, numbers from Tj's drawer; synthetic + Tj picture = SPEAKER, old pink icon = no plate) wired after MU-4, gate-map mu 340 s. In flight: #551 mu id 43074338064 (f1939f2d, hand-hide via Control, no icon yet), #550 id 43071819655 (base,legacy,n,o,last), rb + ss solo (a7a89606, ids not yet known)

## Do this next
1) read #551: get_job_logs run_id=43074338064 failed_only tail 60 (MU-12a0/12a/12/13/13b should pass; look at mud-02a picture: fetch ci-shots into scratchpad/shots551); 2) read #550 and the rb + ss solo runs (list_workflow_runs emulator.yml main workflow_dispatch perPage 3 prints ~7 KB per run: or in_progress filter); NATIVE CRASH beside RB4/SS7 = flake, solo green = gate ok (git diff ca64c7f9 HEAD -- app launcher3 phoneguide is empty); 3) dispatch mu shots=true on the new head to prove MU-21 + icon in the drawer (calibrate thresholds from the INFO MU icon line); 4) write research/MUSIC_ICON_2026-11-11.md (BW1a: two searches found nothing, method, numbers), eleven/CHANGES.md icon+label section, tick TASKS BW1a-d BV1d-h with proofs, HANDOFF 0t, CLAUDE.md lesson (am start of an unexported activity from the shell is refused), V096 answer draft with BW1 + two-Music note; 5) bump app/build.gradle.kts 45 / 0.9.6, bash ship.sh, android.yml full_build, Release must carry Launcher+PhoneGuide+Eleven APKs, record-release. Heartbeat trig_01GVcBRvTAzqf97UhMTJszab

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  a7a89606 ckpt 1177: #549 (id 43071814060, ca64c7f9): 113 PASS 2 FAIL = RB4 + SS7 + INFO NATIVE C
  f1939f2d ckpt 1176: #548 e1+cs GREEN. mu: hand-hide now opened via Launcher control (am start of
  b7fac10b ckpt 1175: Run 547 (id 43071778397, head ca64c7f9, mu shots): 23 PASS 4 FAIL = MU-12a M
  203c9ead ckpt 1174: Tj BW1 (music app must be called Music with the speaker icon from his screen
  9b3a56c9 ckpt 1173: In flight on ca64c7f9: #547 id 43071778397 mu shots; #548 id 43071808078 e1,
  ca64c7f9 ckpt 1172: Run 546 (id 43069863937, head 0701e089, mu shots): 23 PASS 1 FAIL = MU-12 + 
  4015d477 ckpt 1171: BV1 drawer rule hardened after the review: auto_done = pkg@firstInstallTime 
  ca99a8b5 ckpt 1170: Review of the drawer rule (agent a44d508, no HIGH): update in place does NOT
  0701e089 ckpt 1169: BV1e: mu control = upstream Eleven (baseline db65f024 + port fix, built by e
  67f04fc9 ckpt 1168: Run 545 (id 43066812780, head ae8cb7e1, mu shots=true): 23 PASS 1 FAIL = MU-
```

(5 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
