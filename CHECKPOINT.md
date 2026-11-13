# CHECKPOINT 1291 — read me first, then TASKS.md

**Written:** 2026-11-13T03:37:09Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-6e656bc6-wl2rhu` · **builds on:** `573db729` (this checkpoint is the commit after it)

## Just done
gated v0.9.9 (code 48) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.9 "v0.9.9: performance pass 2 (CA1): no searches per PIN key, 4.9-12 MB wallpaper + 2.4 MB curtain dropped where unused, no Launcher3 reload at the Quickstep switch, fewer binder calls and main-thread writes"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  573db729 ckpt 1290: version bumped to 48 / 0.9.9; ship.sh starting in background (scratchpad/shi
  1f37ec66 ckpt 1289: GATE GREEN on 566fc305: #586 #588 green; reruns #589 fl head GREEN (0 Screen
  a6b44f50 ckpt 1288: reruns 03:14Z: #589 43357592447 fl head; #590 43357596323 fl TAG v0.9.8; #59
  0555261c ckpt 1287: gate FAILs: #584 MU-12 beside NATIVE CRASH libhwui (known flake); #585 SS7 (
  b38c8e7a ckpt 1286: gate read 03:10Z: #586 y2..hp GREEN, #588 rg GREEN; #584 (job 131648084482) 
  6e3e0224 ckpt 1285: CA1b status in TASKS; answer draft research/CA1_RELEASE_MESSAGE_DRAFT_2026-1
  2d37f372 ckpt 1284: gate dispatched 02:33Z on 566fc305: #584 43352934985 x1,se,nq,rt,gear,wf,mu;
  566fc305 ckpt 1283: CA1b review fixes all in (MED1 reloadOwed, MED2 grant memo 3s+forget, LOW3/5
  d283250f ckpt 1282: CA1b review fixes: MED1 S2 reloadOwed (SettingsSpoof), MED2 grant memo 3 s +
  4f356f77 ckpt 1281: CA1b verdicts written in PERF_AUDIT (FIXED / REJECTED / NEXT)
```
