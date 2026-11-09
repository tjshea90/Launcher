# CHECKPOINT 750 — read me first, then TASKS.md

**Written:** 2026-11-09T01:47:23Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `d435d9b` (this checkpoint is the commit after it)

## Just done
v0.8.12: gate COMPLETE (uf solo #437 green); ship.sh done a739409; android.yml full_build=true DISPATCHED 01:46Z on main

## Do this next
find the android.yml run id (list_workflow_runs android.yml main workflow_dispatch perPage 1), wait for it (timer, deadline first), get_release_by_tag v0.8.12, bash tools/record-release.sh v0.8.12 with the BL1 note, then tell Tj (release link as plain text on its own line; BM1g already sent)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  d435d9b ckpt 749: v0.8.12 shipped locally (gates green, pushed a739409). uf SOLO #437 id 4254684
  a739409 ckpt 748: gated v0.8.12 (code 37) and pushed it: the handoff self-test, the full unit su
  2ac3ecb ckpt 747: BM1f: #433 id 42545046876 e1,b6,n,x,ac,se,uf = 102 PASS 2 FAIL (UF1 x2 after N
  390c17d ckpt 746: BM1f: #434 id 42545049965 o,hp SUCCESS (hp ok after o = B flake); #433 id 4254
  5634d23 ckpt 745: BM1f: gate v0.8.12 read: A #428 emulator died in e1 (n,ac,uf,x,se,b6 never ran
  c998398 ckpt 744: BM1: LIVE PROOF auto-compact fired 392147 at 00:50:20 (136K after); ctxwatch h
  545161c ckpt 743: BM1: toobig.sh msg, CLAUDE.md rule, HANDOFF 0n written; ctx 390K = 99pct of 39
  3e48e52 ckpt 742: BM1: test_ctxwatch 20 pass, compact_guard+ctxwatch tests wired into test_hando
  304cbaf ckpt 741: BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin s
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
```
