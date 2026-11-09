# CHECKPOINT 741 — read me first, then TASKS.md

**Written:** 2026-11-09T00:43:19Z · **tests:** all 6 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `235ac12` (this checkpoint is the commit after it)

## Just done
BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin script: stale=veto, keepalive clears)

## Do this next
AFTER COMPACTION: (1) bash tools/ctxwatch.sh --report; tail .git/ctxwatch.log; cat .git/hook-env-probe.txt (did compaction at ~392K work, PreCompact hook env) (2) write tools/test_ctxwatch.sh (run python3 -I tools/ctxwatch.py in a temp dir with .git; synthetic transcript levels 1-4, once-only, reset after drop, compact_boundary log, garbage/missing silent) + wire BOTH tests into tools/test_handoff.sh like test_uinode (3) resume.sh: keepalive --start + guard line in BRIEF; capture_inbox.sh: keepalive --once + ctxwatch after INPUT read (4) toobig.sh text (5) CLAUDE.md note, HANDOFF.md pointer, research doc: where context goes (thinking ~half) (6) REMOVE TEMP-PROBE line from autosave.sh (7) BM1f read v0.8.12 gate runs (8) BM1g tell Tj

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
  daacba3 ckpt 739: BM1: evidence doc, keepalive daemon running, env in settings (hot-reload prove
  d6783a5 ckpt 738: BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact
  2ba15f7 ckpt 737: GATE v0.8.12 dispatched 22:26 on 94429c2: A base,x1,e1,n,ac,uf,x,se,b6 / B hp,
  7c914d2 ckpt 736: BL1 docs: research section 5 (status, limits, review), TABLET_CHECKLIST_v0.8.1
  d7cae14 ckpt 735: BL1 review fixes in (9 of 10 findings; frozen status-bar row = disclosed limit
  041aa83 ckpt 734: BL1d evidence: cu #419/#420 id 42528148176/#421 id 42528151261 all PASS (3 gre
  559db2a ckpt 733: BL1d cu #419 id 42527552708 (913947f) = 14 PASS 0 FAIL: wall kept 2 s, curtain
  66783a5 ckpt 732: BL1d first cu run #418 id 42526557769 (922d886): 13 PASS 1 FAIL (CU5). Wall ke
  922d886 ckpt 731: BL1d: emulator section cu written (calibrate with home_patch+home_curtain ON, 
```

(6 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
