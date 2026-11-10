# CHECKPOINT 863 — read me first, then TASKS.md

**Written:** 2026-11-10T04:33:27Z · **tests:** all 9 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-5ef85f7a-ngtgj1` · **builds on:** `d84aec1` (this checkpoint is the commit after it)

## Just done
Old-acct heartbeat 04:28Z took over (new session stalled since 01:52Z; later commits = restart notices); gate #500 green, #501 98P/2F = RB4+RB9 with a native crash (jemalloc/libhwui) in rb

## Do this next
rerun rb SOLO on main (emulator.yml sections rb, id of the run in the next ckpt); read: get_job_logs run_id failed_only tail 60 (No failed jobs = green). Green -> git diff 5bc8db62 HEAD -- app launcher3 phoneguide must be empty -> bash ship.sh; android.yml full_build=true main; get_release_by_tag v0.9.1; record-release; docs; message to Tj (draft research/V091_RELEASE_MESSAGE_DRAFT_2026-11-10.md). Red again -> run rb solo on ref v0.9.0 too. Heartbeats: this session trig_01DhC2W8FUPFeqdGFQCt7yWe (delete when shipped); the other account's trig_01NG4acJZ3CVuQZutyT6rsCC is not mine

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  4c00ae3 ckpt 862: Gate still running at 01:52Z (scenario steps began 01:35:50 / 01:36:26); job i
  0bd19b8 ckpt 861: BP2b draft answer to Tj saved (research/V091_RELEASE_MESSAGE_DRAFT_2026-11-10.
  dba2a24 ckpt 860: BP2 (Tj 01:37Z: save often, fresh account must resume): TASKS BP2, HANDOFF 0p 
  7257d13 ckpt 859: Gate dispatched on main head 5bc8db62: emulator #500 id 42736788932 (e1,o,x,ac
  5bc8db6 ckpt 858: Merged origin/main (old session's BN8f1-3) into claude/bold-gates-5jgbbh; kept
  b6f2d6d ckpt 857: BN8d diag reworked (own report key recents_diag, stock launcher window first, 
  0262a99 ckpt 856: BN8b part 2: pencil FAST watch (25 ms looks for 600 ms after finger-down/Syste
  1dfea9f ckpt 855: Takeover by new account (branch claude/bold-gates-5jgbbh = main): hooks ok, no
  012054e ckpt 855: 2nd BN8 review finished: 3 real findings recorded (TASKS BN8f1-3, brief RESULT
  2ba2208 ckpt 854: BP1: Tj ordered save-all (weekly usage 99%); TASKS BP1, HANDOFF 0p, review bri
```

(3 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
