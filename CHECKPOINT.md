# CHECKPOINT 747 — read me first, then TASKS.md

**Written:** 2026-11-09T01:41:47Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `025b3fb` (this checkpoint is the commit after it)

## Just done
BM1f: #433 id 42545046876 e1,b6,n,x,ac,se,uf = 102 PASS 2 FAIL (UF1 x2 after NATIVE CRASH, UF2/3 PASS, e1 ok); #435/#436 cancelled; uf SOLO dispatched 01:42Z on main

## Do this next
find uf solo run id (list in_progress), wait ~01:48Z, get_job_logs run_id failed_only tail 48 (green=No failed jobs); green -> bash ship.sh note, android.yml full_build=true, get_release_by_tag v0.8.12, tools/record-release.sh v0.8.12; red -> same uf on ref v0.8.11

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  390c17d ckpt 746: BM1f: #434 id 42545049965 o,hp SUCCESS (hp ok after o = B flake); #433 id 4254
  5634d23 ckpt 745: BM1f: gate v0.8.12 read: A #428 emulator died in e1 (n,ac,uf,x,se,b6 never ran
  c998398 ckpt 744: BM1: LIVE PROOF auto-compact fired 392147 at 00:50:20 (136K after); ctxwatch h
  545161c ckpt 743: BM1: toobig.sh msg, CLAUDE.md rule, HANDOFF 0n written; ctx 390K = 99pct of 39
  3e48e52 ckpt 742: BM1: test_ctxwatch 20 pass, compact_guard+ctxwatch tests wired into test_hando
  304cbaf ckpt 741: BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin s
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
  daacba3 ckpt 739: BM1: evidence doc, keepalive daemon running, env in settings (hot-reload prove
  d6783a5 ckpt 738: BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact
  2ba15f7 ckpt 737: GATE v0.8.12 dispatched 22:26 on 94429c2: A base,x1,e1,n,ac,uf,x,se,b6 / B hp,
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
