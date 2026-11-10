# CHECKPOINT 867 — read me first, then TASKS.md

**Written:** 2026-11-10T04:46:47Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `2f5831f` (this checkpoint is the commit after it)

## Just done
ship.sh v0.9.1 (code 40) green+pushed at 2f5831f; android.yml full_build=true dispatched on main ~04:45:45Z

## Do this next
~04:49Z: get_release_by_tag v0.9.1 (list_workflow_runs android.yml main if missing); green -> bash tools/record-release.sh v0.9.1 with the ship note; git ls-remote main = tip; docs per HANDOFF 0p step 7; message to Tj from research/V091_RELEASE_MESSAGE_DRAFT_2026-11-10.md (adapt gate story: #501 RB4+RB9 = native crash, rb solo #502 green; this session took over 04:28Z); then delete heartbeat trig_01DhC2W8FUPFeqdGFQCt7yWe

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  2f5831f ckpt 866: gated v0.9.1 (code 40) and pushed it: the handoff self-test, the full unit sui
  ce00536 ckpt 865: Gate green: #500 + rb solo #502 (id 42756634563) on head 520b08c; gated code =
  4afb1ad ckpt 864: rb solo dispatched on main: emulator #502 id 42756634563 (head 520b08c), start
  520b08c ckpt 863: Old-acct heartbeat 04:28Z took over (new session stalled since 01:52Z; later c
  4c00ae3 ckpt 862: Gate still running at 01:52Z (scenario steps began 01:35:50 / 01:36:26); job i
  0bd19b8 ckpt 861: BP2b draft answer to Tj saved (research/V091_RELEASE_MESSAGE_DRAFT_2026-11-10.
  dba2a24 ckpt 860: BP2 (Tj 01:37Z: save often, fresh account must resume): TASKS BP2, HANDOFF 0p 
  7257d13 ckpt 859: Gate dispatched on main head 5bc8db62: emulator #500 id 42736788932 (e1,o,x,ac
  5bc8db6 ckpt 858: Merged origin/main (old session's BN8f1-3) into claude/bold-gates-5jgbbh; kept
  b6f2d6d ckpt 857: BN8d diag reworked (own report key recents_diag, stock launcher window first, 
```
