# CHECKPOINT 746 — read me first, then TASKS.md

**Written:** 2026-11-09T01:32:07Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `6b04c42` (this checkpoint is the commit after it)

## Just done
BM1f: #434 id 42545049965 o,hp SUCCESS (hp ok after o = B flake); #433 id 42545046876 e1,b6,n,x,ac,se,uf still running 01:31Z

## Do this next
~01:35Z list_workflow_runs perPage 1: #433 completed? green -> bash ship.sh + android.yml full_build=true + get_release_by_tag v0.8.12 + record-release; red -> get_job_logs run_id 42545046876 failed_only tail 45, compare with tag v0.8.11 solo

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md
     M TASKS.md

## Last ten checkpoints
```
  5634d23 ckpt 745: BM1f: gate v0.8.12 read: A #428 emulator died in e1 (n,ac,uf,x,se,b6 never ran
  c998398 ckpt 744: BM1: LIVE PROOF auto-compact fired 392147 at 00:50:20 (136K after); ctxwatch h
  545161c ckpt 743: BM1: toobig.sh msg, CLAUDE.md rule, HANDOFF 0n written; ctx 390K = 99pct of 39
  3e48e52 ckpt 742: BM1: test_ctxwatch 20 pass, compact_guard+ctxwatch tests wired into test_hando
  304cbaf ckpt 741: BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin s
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
  daacba3 ckpt 739: BM1: evidence doc, keepalive daemon running, env in settings (hot-reload prove
  d6783a5 ckpt 738: BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact
  2ba15f7 ckpt 737: GATE v0.8.12 dispatched 22:26 on 94429c2: A base,x1,e1,n,ac,uf,x,se,b6 / B hp,
  7c914d2 ckpt 736: BL1 docs: research section 5 (status, limits, review), TABLET_CHECKLIST_v0.8.1
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
