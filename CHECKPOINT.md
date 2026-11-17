# CHECKPOINT 1397 — read me first, then TASKS.md

**Written:** 2026-11-17T22:14:56Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `94bce67d` (this checkpoint is the commit after it)

## Just done
CG3d done: research/CG1F_CONFIG_CHANGES_2026-11-17.md = prime suspect for the Quickstep RAM growth is OUR battery-saver flips (BatterySaverGuard onLock off / gesture-unlock on / custom-mode off -> dark theme flips -> uiMode config change -> stock launcher Activity relaunched -> pinned by the IconRecentsView leak); reasoned, not measured; watcher bso4061al running (tools/watch_branch.sh ccr-b6bbb365-txbcep 8 45 60; B tip 69453515 unchanged since 21:48Z)

## Do this next
wait for the watcher notification (output file /tmp/claude-0/-home-user-Launcher/935296b1-ec15-593b-b259-b93d34c7823d/tasks/bso4061al.output): on WATCH-EVENT QUIET (B silent >=30 min) first list emulator.yml/android.yml runs in_progress+queued (mcp__github__actions_list), then CG3b take over: git fetch origin main; git merge origin/main (conflicts CHECKPOINT/TASKS/OUTBOX: keep both), read B's Do this next (route QsMemory via ShellHelper, rerun qm, lg per research/CG1_SESSION_A_NOTE_2026-11-17.md, section qf per CG1F_CONFIG_CHANGES, /code-review high vs v0.9.11, gate.sh v0.9.11, ship.sh, android.yml full_build, record-release); on TAG or DIGEST just check B and re-arm; always compare git ls-remote origin refs/heads/main with my tip before any dispatch. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ (A, next 00:03Z)

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  7336b488 ckpt 1396: CG3 (Tj 22:06Z: monitor the other session, finish and build the release) wri
  c6e2ebb8 ckpt 1395: heartbeat 22:04Z: stood down again (rule 2): session B ckpt 1400 (21:47Z, br
  6214dc56 ckpt 1394: heartbeat 21:55Z: STOOD DOWN (rule 2): session B (account B, branch ccr-b6bb
  81fd7345 ckpt 1393: CG1a PROVEN on emulator: ad run #620 id 44164339591 (head 9d37c438): 13 PASS
  38a7ec46 ckpt 1392: CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP gran
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
  5d936924 ckpt 1389: CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the serv
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
  50cb0c71 ckpt 1387: CG1e VERDICT run #618 id 44159446669 (9 PASS 0 FAIL): RRO impossible w/o roo
```

(2 automatic checkpoint(s) since the last deliberate one — the
session was still mid-step. `git diff` against it shows what changed.)
