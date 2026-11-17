# CHECKPOINT 1398 — read me first, then TASKS.md

**Written:** 2026-11-17T22:16:36Z · **tests:** all 11 fast checks green (gradle suite: see ship.sh)
**Branch:** `ccr-16772798-h13w43` · **builds on:** `497a0215` (this checkpoint is the commit after it)

## Just done
takeover prepared (read-only): git merge-tree dry run of origin/main into my branch conflicts ONLY in CHECKPOINT.md and OUTBOX.md (TASKS/INBOX/code auto-merge); B idle since 21:48Z, last emulator run #623 (21:44Z) done, no CI in progress; backup wake send_later trig_01BMccPN2w2Qh3tb3n942Ft9 at 22:28Z

## Do this next
at QUIET / 22:28Z backup / 00:03Z heartbeat: if B has no new commit and no CI in flight, take over in ONE bash call: git merge --no-commit --no-ff origin/main; for CHECKPOINT.md take theirs (git checkout --theirs); for OUTBOX.md keep both (git show :1:/:2:/:3: into 3 files, git merge-file --union); git add -A; git commit; then bash tools/ckpt.sh; then B's list: route QsMemory via ShellHelper, rerun qm, lg, qf, /code-review high vs v0.9.11, gate.sh v0.9.11, ship.sh, android.yml full_build, record-release. heartbeat trig_01EzvMtE44ag8oMav4DqoqHZ

*(resuming? CLAUDE.md's "FIRST ACTION OF EVERY SESSION" comes before "Starting a session" — do that one first, or autosave stays off all session.)*

## Uncommitted right now
     M CHECKPOINT.md

## Last ten checkpoints
```
  497a0215 ckpt 1397: CG3d done: research/CG1F_CONFIG_CHANGES_2026-11-17.md = prime suspect for th
  7336b488 ckpt 1396: CG3 (Tj 22:06Z: monitor the other session, finish and build the release) wri
  c6e2ebb8 ckpt 1395: heartbeat 22:04Z: stood down again (rule 2): session B ckpt 1400 (21:47Z, br
  6214dc56 ckpt 1394: heartbeat 21:55Z: STOOD DOWN (rule 2): session B (account B, branch ccr-b6bb
  81fd7345 ckpt 1393: CG1a PROVEN on emulator: ad run #620 id 44164339591 (head 9d37c438): 13 PASS
  38a7ec46 ckpt 1392: CG1f: QsMemory recorder (dumpsys meminfo of the stock launcher via DUMP gran
  5ff672be ckpt 1391: CG1a: ad run #619 id 44161930288: 12 PASS 1 FAIL=AD5b (custom home in front 
  12775bb7 ckpt 1390: CG1a: ad run #619 id 44161930288 started 19:45Z head 5d936924 shots=true (un
  5d936924 ckpt 1389: CG1a P0+P1: StrayCustomGuard (pure, 13 tests) + guardStrayCustom in the serv
  7b0f5893 ckpt 1388: CG1: TASKS holds the reviewer findings (CG1A B1 stray-custom, CG1B x2, CG1D 
```
