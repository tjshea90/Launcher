# CHECKPOINT 743 — read me first, then TASKS.md

**Written:** 2026-11-09T00:48:37Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `3e6ee5a` (this checkpoint is the commit after it)

## Just done
BM1: toobig.sh msg, CLAUDE.md rule, HANDOFF 0n written; ctx 390K = 99pct of 392K compaction point (live test now)

## Do this next
AFTER COMPACTION: (1) bash tools/ctxwatch.sh --report + tail .git/ctxwatch.log (COMPACTION SEEN line?) = did auto-compact at ~392K work; also python3 - grep diag log /tmp/claude-code-*.diag.log for hook_spawn PreCompact exit codes (2) add result + 'where context goes' (thinking ~155K of 332K at 36 calls) to research/COMPACTION_BLOCKED section 5 (3) REMOVE TEMP-PROBE line from tools/autosave.sh (4) tick BM1b-BM1e in TASKS.md (5) BM1f: list_workflow_runs emulator.yml main workflow_dispatch perPage 8, read v0.8.12 gate A/B/C + solo hp,hf and cs reruns, compare with tag v0.8.11 (6) BM1g tell Tj: cause, 4 layers, disable session-handoff plugin, effort max thinking cost

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  3e48e52 ckpt 742: BM1: test_ctxwatch 20 pass, compact_guard+ctxwatch tests wired into test_hando
  304cbaf ckpt 741: BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin s
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
  daacba3 ckpt 739: BM1: evidence doc, keepalive daemon running, env in settings (hot-reload prove
  d6783a5 ckpt 738: BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact
  2ba15f7 ckpt 737: GATE v0.8.12 dispatched 22:26 on 94429c2: A base,x1,e1,n,ac,uf,x,se,b6 / B hp,
  7c914d2 ckpt 736: BL1 docs: research section 5 (status, limits, review), TABLET_CHECKLIST_v0.8.1
  d7cae14 ckpt 735: BL1 review fixes in (9 of 10 findings; frozen status-bar row = disclosed limit
  041aa83 ckpt 734: BL1d evidence: cu #419/#420 id 42528148176/#421 id 42528151261 all PASS (3 gre
  559db2a ckpt 733: BL1d cu #419 id 42527552708 (913947f) = 14 PASS 0 FAIL: wall kept 2 s, curtain
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
