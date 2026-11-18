# CHECKPOINT 1429 — read me first, then TASKS.md

**Written:** 2026-11-18T00:33:40Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `cc784e1c` (this checkpoint is the commit after it)

## Just done
gated v0.9.12 (code 51) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.9.12 "v0.9.12: no more custom home showing up in a Quickstep session after a gesture unlock (stray-home guard, CG1a); lock-screen patch counted as ready at once when it is prepared with the screen off (CG1d); baseline profile plus compile-state and stock-launcher-memory recorders (CG1c, CG1f); three accessibility hot-path savings (CG1b)"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  cc784e1c ckpt 1428: PT TALLY 00:40Z (all solo): HEAD #631 BC2 only; #632 BA1+PT (1 of 5 fades; B
  31718304 ckpt 1427: PROBES 00:36Z (failed_only): pt@branch #634 id 44205747479 job 134471094065 
  6085afa9 ckpt 1426: VERIFIED 00:30Z (cheap probe 'No failed jobs'): base@head #637 green 29/0 (b
  46099daf ckpt 1425: ids of the 00:17Z dispatches: o@branch #641 id 44206636873; o@tag #642 id 44
  e3467532 ckpt 1424: #628 (run1 n6,o,x,ad,ae,pa,uf,cs2,hp,cu,last) CUT at the 2700s step limit (e
  839cc5e8 ckpt 1423: triage runs (dispatched 00:11Z): pt@branch #634 id 44205747479; pt@main #635
  b911a52b ckpt 1422: triage dispatched 00:17Z (all on product code = 1c135985 + version bump): pt
  fd046e2f ckpt 1421: RAW VERDICTS 00:08Z: #629 (base,y2,n,ac,bs,se,fp,rt,gear,fl) 123 PASS 4 FAIL
  688590e5 ckpt 1420: gate ended 00:02Z: #629 (run 44199579604 job 134449895997, base,y2,n,ac,bs,s
  74289af2 ckpt 1419: #631 pt (id 44199590509, head 1c135985): 30 PASS, 1 FAIL, 124 INFO; the one 
```
