# CHECKPOINT 992 — read me first, then TASKS.md

**Written:** 2026-11-10T19:55:03Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `65dfdaf` (this checkpoint is the commit after it)

## Just done
BR1 emu run #518 id 42889694843 (sections=gear, shots, main@5360869) DISPATCHED 19:53Z; full unit suite+lint green locally; research/PENCIL_REMOVAL written (BR2 verdict: no safe way; demo flag removes gear too)

## Do this next
read #518: actions_list list_workflow_jobs resource_id 42889694843 then get_job_logs tail_lines 60 (RESULTS block; BR1-1..5). Fix failures, rerun gear only. Then bash ship.sh 'BR1 gear -> look-alike', android.yml full_build=true on main, get_release_by_tag v0.9.3, record-release.sh, ONE message to Tj (BR1 result + BR2 verdict + open items). TASKS: tick BR1/BR2 after.

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  5360869 ckpt 990: BR1: emulator section 'gear' written (scenario + gate-map + emulator.yml names
  ce9981c ckpt 987: BR1 code: GearRules(+13 tests green) + gear overlay in TjAccessibilityService 
  dcf71e3 ckpt 974: BR1 (gear -> look-alike) + BR2 (pencil removal research) written into TASKS in
  4f00f29 ckpt 971: Heartbeat trig_013YUvG3qqKTpfyqZn9k2nmE DISABLED 16:34Z (update_trigger enable
  57d90ce ckpt 970: Heartbeat firing 16:22Z: rule 2 no other session working, rule 3 no unanswered
  afe5fc4 ckpt 969: Heartbeat firing 14:24Z: rule 2 no other session working; v0.9.2 shipped/answe
  fbac8ea ckpt 967: b6 rerun #517 (id 42811195897, head 52093c7) GREEN: 21 PASS 0 FAIL; custom clo
  02daec4 ckpt 965: Run in flight: b6 rerun #517 id 42811195897 head 52093c7 since 11:53:51Z (done
  52093c7 ckpt 963: b6 custom flow: the BN8b drag ends at the PIN pad on the CI emulator: first BA
  ba11860 ckpt 962: b6 #516 (id 42809367693, head bbc5283): 20 PASS 1 FAIL = BQ3 custom close 3 ST
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
