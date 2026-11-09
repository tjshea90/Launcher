# CHECKPOINT 744 — read me first, then TASKS.md

**Written:** 2026-11-09T01:16:54Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `1a654df` (this checkpoint is the commit after it)

## Just done
BM1: LIVE PROOF auto-compact fired 392147 at 00:50:20 (136K after); ctxwatch hardened (fresh-compaction, 2-call alarm, far marker) 26 tests; TEMP-PROBE removed

## Do this next
TASKS tick BM1b-e; research COMPACTION sec 5 result; BM1f gate recovery: list_workflow_runs emulator.yml main workflow_dispatch perPage 8, read v0.8.12 A/B/C (94429c2) + solo hp,hf/cs reruns; BM1g tell Tj

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  545161c ckpt 743: BM1: toobig.sh msg, CLAUDE.md rule, HANDOFF 0n written; ctx 390K = 99pct of 39
  3e48e52 ckpt 742: BM1: test_ctxwatch 20 pass, compact_guard+ctxwatch tests wired into test_hando
  304cbaf ckpt 741: BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin s
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
  daacba3 ckpt 739: BM1: evidence doc, keepalive daemon running, env in settings (hot-reload prove
  d6783a5 ckpt 738: BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact
  2ba15f7 ckpt 737: GATE v0.8.12 dispatched 22:26 on 94429c2: A base,x1,e1,n,ac,uf,x,se,b6 / B hp,
  7c914d2 ckpt 736: BL1 docs: research section 5 (status, limits, review), TABLET_CHECKLIST_v0.8.1
  d7cae14 ckpt 735: BL1 review fixes in (9 of 10 findings; frozen status-bar row = disclosed limit
  041aa83 ckpt 734: BL1d evidence: cu #419/#420 id 42528148176/#421 id 42528151261 all PASS (3 gre
```

(10 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
