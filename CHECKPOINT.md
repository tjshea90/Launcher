# CHECKPOINT 748 — read me first, then TASKS.md

**Written:** 2026-11-09T01:45:25Z · **tests:** all 7 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `1de457d` (this checkpoint is the commit after it)

## Just done
gated v0.8.12 (code 37) and pushed it: the handoff self-test, the full unit suite + lint
and the versionCode check all passed here. NOT yet built - GitHub has not been asked.

## Do this next
TRIGGER THE BUILD: mcp__github__actions_run_trigger, method run_workflow, workflow
android.yml, ref main, inputs {"full_build": "true"}. When that run is green,
confirm the Release is published (get_release_by_tag is enough) and then run:
  bash tools/record-release.sh v0.8.12 "BL1: the black cover while Quickstep is cleaned up after an unlock is now a curtain, a saved picture of your current home screen (refreshed when the home changes, no camera flash); touches are swallowed exactly as before"

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  2ac3ecb ckpt 747: BM1f: #433 id 42545046876 e1,b6,n,x,ac,se,uf = 102 PASS 2 FAIL (UF1 x2 after N
  390c17d ckpt 746: BM1f: #434 id 42545049965 o,hp SUCCESS (hp ok after o = B flake); #433 id 4254
  5634d23 ckpt 745: BM1f: gate v0.8.12 read: A #428 emulator died in e1 (n,ac,uf,x,se,b6 never ran
  c998398 ckpt 744: BM1: LIVE PROOF auto-compact fired 392147 at 00:50:20 (136K after); ctxwatch h
  545161c ckpt 743: BM1: toobig.sh msg, CLAUDE.md rule, HANDOFF 0n written; ctx 390K = 99pct of 39
  3e48e52 ckpt 742: BM1: test_ctxwatch 20 pass, compact_guard+ctxwatch tests wired into test_hando
  304cbaf ckpt 741: BM1: compact_guard.py/.sh + test_compact_guard.sh (18 pass incl. REAL plugin s
  08f4480 ckpt 740: BM1: ctxwatch.py/.sh (+--report,--top), posttool.sh (matcher *), keepalive dae
  daacba3 ckpt 739: BM1: evidence doc, keepalive daemon running, env in settings (hot-reload prove
  d6783a5 ckpt 738: BM1: root cause of Prompt is too long found (session-handoff plugin PreCompact
```

(1 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
